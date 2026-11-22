/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.rubato.io.musicxml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.rubato.io.PartInfo;
import org.rubato.io.ScoreNote;
import org.rubato.math.arith.Rational;
import org.rubato.math.yoneda.Denotator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Reads a partwise MusicXML file, uncompressed or compressed (.mxl), into a
 * denotator of form Score. It reverses {@link MusicXMLWriter}'s mapping and
 * reads files from other programs as follows:
 * <ul>
 * <li>Voice: a part with id {@code V<n>} (as the writer names them) gets
 *     Voice n; otherwise the midi-channel of its first midi-instrument,
 *     minus 1; otherwise the part's index in the part-list, from 0.</li>
 * <li>Time: the position in each part starts at 0 and moves by duration
 *     divided by the current divisions: forward for notes, rests and
 *     forward elements, back for backup elements. A note with a chord
 *     element starts where the previous note did; a grace note has
 *     duration 0 and does not move the position. Each measure starts where
 *     the previous one started plus the length of the current time
 *     signature, so a voice that stops before the barline does not move
 *     later notes; an implicit measure (a pickup), a part without a time
 *     signature or a senza-misura one ends where its furthest voice ends.
 *     Repeats, endings and jumps are not expanded: the result is the
 *     written order.</li>
 * <li>Parts: the part-name and the first midi-instrument's program,
 *     volume and pan of each part, in MIDI units, from
 *     {@link #getParts()}.</li>
 * <li>Tempo: the first sound element with a tempo attribute, available
 *     from {@link #getTempo()} in microseconds per quarter note; later
 *     tempo changes are reported in the warnings and not read.</li>
 * <li>Ties: a note whose tie stop continues an open tie with the same
 *     MusicXML voice and pitch, ending where it starts, lengthens that
 *     note.</li>
 * <li>Loudness: the note's dynamics attribute, else the last sound element
 *     with a dynamics attribute in the part, else 100; either way the
 *     velocity is the percentage times 0.9, rounded (90 for 100%).</li>
 * <li>Cue notes, which the specification says do not sound, and unpitched
 *     notes, which have no Pitch, move the position but make no note.</li>
 * </ul>
 * The parser loads no external DTD or entity, so the DOCTYPE that most
 * MusicXML files carry needs no network.
 */
public final class MusicXMLReader {

    private final List<String> warnings = new ArrayList<String>();
    private Integer tempo;
    private final TreeMap<Integer, PartInfo> parts = new TreeMap<Integer, PartInfo>();
    private boolean tempoChangeReported;

    /**
     * The first tempo in the last file read, in microseconds per quarter
     * note (the unit of {@code ScoreMidi.setTempo} and
     * {@link MusicXMLWriter#setTempo}), from the first sound element with a
     * tempo attribute; {@link MusicXMLWriter#DEFAULT_TEMPO} when the file
     * gives none. Use {@link #hasTempo()} to tell the two apart.
     */
    public int getTempo() {
        return this.tempo == null ? MusicXMLWriter.DEFAULT_TEMPO : this.tempo;
    }

    /**
     * The name and MIDI settings of each part in the last file read, by
     * Voice: the part-name, and from the part's first midi-instrument the
     * midi-program minus 1 (MusicXML counts programs from 1), the volume
     * and the pan converted to MIDI units ({@link PartInfo#panFromDegrees},
     * {@link PartInfo#volumeFromPercent}). Settings the file does not give
     * are {@link PartInfo#UNSET}; a part without a part-name has a null
     * name. When two parts read as one Voice, the first one's entry is kept.
     */
    public Map<Integer, PartInfo> getParts() {
        return Collections.unmodifiableMap(this.parts);
    }

    /** The part names of the last file read, by Voice; see {@link #getParts()}. */
    public Map<Integer, String> getPartNames() {
        TreeMap<Integer, String> names = new TreeMap<Integer, String>();
        for (Map.Entry<Integer, PartInfo> e : this.parts.entrySet()) {
            names.put(e.getKey(), e.getValue().getName());
        }
        return Collections.unmodifiableMap(names);
    }

    /** Whether the last file read gave a tempo. */
    public boolean hasTempo() {
        return this.tempo != null;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(this.warnings);
    }

    public Denotator readScore(InputStream in) throws IOException {
        return ScoreNote.toScore(this.read(in));
    }

    public List<ScoreNote> read(InputStream input) throws IOException {
        this.warnings.clear();
        this.tempo = null;
        this.tempoChangeReported = false;
        this.parts.clear();
        BufferedInputStream in = new BufferedInputStream(input);
        in.mark(4);
        int b0 = in.read(), b1 = in.read();
        in.reset();
        byte[] xml = (b0 == 'P' && b1 == 'K') ? readCompressed(in) : in.readAllBytes();
        Document doc = parse(xml);
        Element root = doc.getDocumentElement();
        if ("score-timewise".equals(root.getTagName())) {
            throw new IOException("Timewise MusicXML is not supported; convert it to partwise first (the MusicXML schema ships timepart.xsl for this)");
        }
        if (!"score-partwise".equals(root.getTagName())) {
            throw new IOException("Not a MusicXML score: the root element is <" + root.getTagName() + ">");
        }
        Map<String, Integer> voices = this.partVoices(root);
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (Element part : children(root, "part")) {
            Integer voice = voices.get(part.getAttribute("id"));
            if (voice == null) {
                this.warnings.add("Part " + part.getAttribute("id") + " is not in the part-list; it is read as voice " + voices.size());
                voice = voices.size();
                voices.put(part.getAttribute("id"), voice);
                if (!this.parts.containsKey(voice)) {
                    this.parts.put(voice, PartInfo.NONE);
                }
            }
            this.readPart(part, voice, notes);
        }
        return notes;
    }

    private static final Pattern WRITER_PART_ID = Pattern.compile("V(-?\\d+)");

    private Map<String, Integer> partVoices(Element root) throws IOException {
        Map<String, Integer> voices = new HashMap<String, Integer>();
        Element partList = child(root, "part-list");
        if (partList == null) {
            throw new IOException("The score has no part-list");
        }
        int index = 0;
        for (Element sp : children(partList, "score-part")) {
            String id = sp.getAttribute("id");
            Matcher m = WRITER_PART_ID.matcher(id);
            Integer voice = null;
            if (m.matches()) {
                try {
                    voice = Integer.valueOf(m.group(1));
                }
                catch (NumberFormatException e) {
                    voice = null;
                }
            }
            if (voice == null) {
                Element mi = child(sp, "midi-instrument");
                Element ch = mi == null ? null : child(mi, "midi-channel");
                if (ch != null) {
                    voice = Integer.parseInt(ch.getTextContent().trim()) - 1;
                }
            }
            if (voice == null) {
                voice = index;
            }
            voices.put(id, voice);
            if (!this.parts.containsKey(voice)) {
                this.parts.put(voice, this.partInfo(sp));
            }
            index++;
        }
        return voices;
    }

    private PartInfo partInfo(Element scorePart) {
        Element nameElement = child(scorePart, "part-name");
        String name = nameElement == null ? null : nameElement.getTextContent().trim();
        int program = PartInfo.UNSET, pan = PartInfo.UNSET, volume = PartInfo.UNSET;
        Element mi = child(scorePart, "midi-instrument");
        if (mi != null) {
            try {
                Element e = child(mi, "midi-program");
                if (e != null) {
                    program = Integer.parseInt(e.getTextContent().trim()) - 1;
                    if (program < 0 || program > 127) {
                        this.warnings.add("Part " + scorePart.getAttribute("id") + " has midi-program " + (program + 1) + ", outside 1..128; it is not read");
                        program = PartInfo.UNSET;
                    }
                }
                e = child(mi, "volume");
                if (e != null) {
                    volume = PartInfo.volumeFromPercent(new BigDecimal(e.getTextContent().trim()));
                }
                e = child(mi, "pan");
                if (e != null) {
                    pan = PartInfo.panFromDegrees(new BigDecimal(e.getTextContent().trim()));
                }
            }
            catch (NumberFormatException e) {
                this.warnings.add("Part " + scorePart.getAttribute("id") + " has a midi-instrument value that cannot be read: " + e.getMessage());
            }
        }
        return new PartInfo(name, program, pan, volume);
    }

    /** A note held open by a tie start, keyed by MusicXML voice and pitch. */
    private static final class Open {
        final Frac onset;
        Frac end;
        final Rational pitch;
        final int loudness;

        Open(Frac onset, Frac end, Rational pitch, int loudness) {
            this.onset = onset;
            this.end = end;
            this.pitch = pitch;
            this.loudness = loudness;
        }
    }

    private void readPart(Element part, int voice, List<ScoreNote> notes) throws IOException {
        Frac divisions = Frac.of(1, 1);
        Frac measureLength = null;
        Frac measureStart = Frac.ZERO;
        Frac pos = Frac.ZERO;
        Frac lastOnset = Frac.ZERO;
        int partLoudness = 90;
        Map<String, Open> open = new HashMap<String, Open>();
        List<Open> done = new ArrayList<Open>();
        for (Element measure : children(part, "measure")) {
            pos = measureStart;
            lastOnset = measureStart;
            Frac reached = measureStart;
            for (Node n = measure.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (!(n instanceof Element)) {
                    continue;
                }
                Element e = (Element)n;
                String tag = e.getTagName();
                if ("attributes".equals(tag)) {
                    Element d = child(e, "divisions");
                    if (d != null) {
                        divisions = decimal(d, "divisions");
                        if (divisions.signum() <= 0) {
                            throw new IOException("divisions must be positive: " + d.getTextContent());
                        }
                    }
                    Element t = child(e, "time");
                    if (t != null) {
                        measureLength = timeLength(t);
                    }
                }
                else if ("backup".equals(tag)) {
                    pos = pos.subtract(duration(e, divisions));
                    if (pos.signum() < 0) {
                        this.warnings.add("A backup in measure " + measure.getAttribute("number") + " of part " + part.getAttribute("id") + " goes before the start; the position is set to 0");
                        pos = Frac.ZERO;
                    }
                }
                else if ("forward".equals(tag)) {
                    pos = pos.add(duration(e, divisions));
                    if (pos.compareTo(reached) > 0) {
                        reached = pos;
                    }
                }
                else if ("sound".equals(tag)) {
                    partLoudness = this.soundLoudness(e, partLoudness);
                    this.soundTempo(e);
                }
                else if ("direction".equals(tag)) {
                    Element s = child(e, "sound");
                    if (s != null) {
                        partLoudness = this.soundLoudness(s, partLoudness);
                        this.soundTempo(s);
                    }
                }
                else if ("note".equals(tag)) {
                    boolean chord = child(e, "chord") != null;
                    boolean grace = child(e, "grace") != null;
                    boolean cue = child(e, "cue") != null;
                    Frac length = grace ? Frac.ZERO : duration(e, divisions);
                    Frac onset = chord ? lastOnset : pos;
                    if (!chord) {
                        lastOnset = pos;
                        pos = pos.add(length);
                        if (pos.compareTo(reached) > 0) {
                            reached = pos;
                        }
                    }
                    Element pitch = child(e, "pitch");
                    if (cue || pitch == null) {
                        if (!cue && child(e, "unpitched") != null) {
                            this.warnings.add("An unpitched note in measure " + measure.getAttribute("number") + " of part " + part.getAttribute("id") + " is skipped");
                        }
                        continue;
                    }
                    Rational p = pitch(pitch);
                    int loudness = partLoudness;
                    if (e.hasAttribute("dynamics")) {
                        loudness = velocity(new BigDecimal(e.getAttribute("dynamics").trim()));
                    }
                    Element mv = child(e, "voice");
                    String key = (mv == null ? "1" : mv.getTextContent().trim()) + "|" + p;
                    boolean tieStart = false, tieStop = false;
                    for (Element t : children(e, "tie")) {
                        if ("start".equals(t.getAttribute("type"))) tieStart = true;
                        if ("stop".equals(t.getAttribute("type"))) tieStop = true;
                    }
                    Frac end = onset.add(length);
                    Open o = tieStop ? open.get(key) : null;
                    if (o != null && o.end.equals(onset)) {
                        o.end = end;
                        if (!tieStart) {
                            open.remove(key);
                        }
                    }
                    else {
                        o = new Open(onset, end, p, loudness);
                        done.add(o);
                        if (tieStart) {
                            open.put(key, o);
                        }
                    }
                }
            }
            measureStart = this.nextMeasure(measure, part, measureStart, measureLength, reached);
        }
        for (Open o : done) {
            notes.add(new ScoreNote(o.onset.doubleValue(), o.pitch, o.loudness, o.end.subtract(o.onset).doubleValue(), voice));
        }
    }

    /**
     * Where the measure after this one starts: its start plus the length the
     * time signature gives, so that a voice that stops short of the barline
     * moves nothing after it. An implicit measure (a pickup), a part with no
     * time signature or one without measure (senza-misura) ends where its
     * furthest voice ends instead, and so does a measure that runs past its
     * time signature, with a warning.
     */
    private Frac nextMeasure(Element measure, Element part, Frac start, Frac length, Frac reached) {
        if (length == null || "yes".equals(measure.getAttribute("implicit"))) {
            return reached;
        }
        Frac end = start.add(length);
        if (reached.compareTo(end) > 0) {
            this.warnings.add("Measure " + measure.getAttribute("number") + " of part " + part.getAttribute("id") + " is longer than its time signature; the next measure starts after its last note");
            return reached;
        }
        return end;
    }

    /** The length of a measure in quarter notes, or null for senza-misura or a time signature that cannot be read. */
    private Frac timeLength(Element time) {
        if (child(time, "senza-misura") != null) {
            return null;
        }
        List<Element> beats = children(time, "beats");
        List<Element> types = children(time, "beat-type");
        if (beats.isEmpty() || beats.size() != types.size()) {
            return null;
        }
        Frac total = Frac.ZERO;
        try {
            for (int i = 0; i < beats.size(); i++) {
                long count = 0;
                for (String s : beats.get(i).getTextContent().trim().split("\\+")) {
                    count += Long.parseLong(s.trim());
                }
                long type = Long.parseLong(types.get(i).getTextContent().trim());
                if (count <= 0 || type <= 0) {
                    return null;
                }
                total = total.add(Frac.of(4 * count, type));
            }
        }
        catch (NumberFormatException e) {
            this.warnings.add("The time signature " + time.getTextContent().trim().replaceAll("\\s+", " ") + " cannot be read; measures end after their last note");
            return null;
        }
        return total;
    }

    /** Takes the first sound tempo of the file and reports later ones that differ. */
    private void soundTempo(Element sound) {
        if (!sound.hasAttribute("tempo")) {
            return;
        }
        BigDecimal bpm;
        try {
            bpm = new BigDecimal(sound.getAttribute("tempo").trim());
        }
        catch (NumberFormatException e) {
            this.warnings.add("The tempo " + sound.getAttribute("tempo") + " cannot be read");
            return;
        }
        if (bpm.signum() <= 0) {
            return;
        }
        long us = BigDecimal.valueOf(60000000L).divide(bpm, 0, RoundingMode.HALF_UP).longValue();
        int micros = (int)Math.max(1, Math.min(0xFFFFFF, us));
        if (this.tempo == null) {
            this.tempo = micros;
        }
        else if (this.tempo != micros && !this.tempoChangeReported) {
            this.tempoChangeReported = true;
            this.warnings.add("The file changes tempo; only the first tempo, " + this.tempo + " microseconds per quarter note, is read");
        }
    }

    private int soundLoudness(Element sound, int current) {
        if (sound.hasAttribute("dynamics")) {
            return velocity(new BigDecimal(sound.getAttribute("dynamics").trim()));
        }
        return current;
    }

    /** MusicXML dynamics are a percentage of the forte velocity 90. */
    static int velocity(BigDecimal dynamics) {
        return dynamics.multiply(new BigDecimal("0.9")).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private static Frac duration(Element e, Frac divisions) throws IOException {
        Element d = child(e, "duration");
        if (d == null) {
            throw new IOException("<" + e.getTagName() + "> has no duration");
        }
        return decimal(d, "duration").divide(divisions);
    }

    private static Frac decimal(Element e, String what) throws IOException {
        try {
            return Frac.of(new BigDecimal(e.getTextContent().trim()));
        }
        catch (NumberFormatException ex) {
            throw new IOException("bad " + what + ": " + e.getTextContent());
        }
    }

    private static final int[] STEP_PC = { 9, 11, 0, 2, 4, 5, 7 }; // A..G

    static Rational pitch(Element pitch) throws IOException {
        Element step = child(pitch, "step");
        Element octave = child(pitch, "octave");
        if (step == null || octave == null) {
            throw new IOException("A pitch needs step and octave");
        }
        String s = step.getTextContent().trim();
        if (s.length() != 1 || s.charAt(0) < 'A' || s.charAt(0) > 'G') {
            throw new IOException("bad step " + s);
        }
        int semitone = (Integer.parseInt(octave.getTextContent().trim()) + 1) * 12 + STEP_PC[s.charAt(0) - 'A'];
        Rational p = new Rational(semitone);
        Element alter = child(pitch, "alter");
        if (alter != null) {
            Frac a = Frac.of(new BigDecimal(alter.getTextContent().trim()));
            if (a.num.bitLength() < 31 && a.den.bitLength() < 31) {
                p = p.sum(new Rational(a.num.intValue(), a.den.intValue()));
            }
            else {
                p = p.sum(new Rational(a.doubleValue()));
            }
        }
        return p;
    }

    //
    // XML
    //

    private static Document parse(byte[] xml) throws IOException {
        try {
            // the JDK's own parser: the application's class path carries an
            // old Xerces that the factory lookup would otherwise find
            DocumentBuilderFactory f = DocumentBuilderFactory.newDefaultInstance();
            f.setNamespaceAware(false);
            f.setValidating(false);
            f.setExpandEntityReferences(false);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return b.parse(new InputSource(new ByteArrayInputStream(xml)));
        }
        catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        catch (SAXException e) {
            throw new IOException("The file is not well-formed XML: " + e.getMessage(), e);
        }
    }

    /** The root file of a compressed MusicXML file (MusicXML 4.0, "Compressed MusicXML files"). */
    private static byte[] readCompressed(InputStream in) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ZipInputStream zip = new ZipInputStream(in);
        ZipEntry entry;
        long total = 0;
        while ((entry = zip.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                byte[] data = zip.readAllBytes();
                total += data.length;
                if (total > 256L * 1024 * 1024) {
                    throw new IOException("The compressed file expands past 256 MB");
                }
                entries.put(entry.getName(), data);
            }
        }
        byte[] container = entries.get("META-INF/container.xml");
        if (container != null) {
            Document c = parse(container);
            Element rootfiles = child(c.getDocumentElement(), "rootfiles");
            Element rootfile = rootfiles == null ? null : child(rootfiles, "rootfile");
            if (rootfile != null) {
                byte[] data = entries.get(rootfile.getAttribute("full-path"));
                if (data != null) {
                    return data;
                }
            }
        }
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String name = e.getKey();
            if (!name.startsWith("META-INF/") && (name.endsWith(".musicxml") || name.endsWith(".xml"))) {
                return e.getValue();
            }
        }
        throw new IOException("The compressed file holds no MusicXML score");
    }

    static Element child(Element parent, String tag) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && tag.equals(((Element)n).getTagName())) {
                return (Element)n;
            }
        }
        return null;
    }

    static List<Element> children(Element parent, String tag) {
        List<Element> list = new ArrayList<Element>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && tag.equals(((Element)n).getTagName())) {
                list.add((Element)n);
            }
        }
        return list;
    }
}
