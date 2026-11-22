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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.rubato.io.PartInfo;
import org.rubato.io.ScoreNote;
import org.rubato.math.arith.Rational;
import org.rubato.math.yoneda.Denotator;

/**
 * Writes a denotator of form Score as a partwise MusicXML 4.0 file.
 * <p>
 * The mapping (MusicXML 4.0, W3C Music Notation Community Group Report,
 * https://www.w3.org/2021/06/musicxml40/):
 * <ul>
 * <li>Each Voice value becomes a part with id {@code V<voice>} and name
 *     "Voice &lt;voice&gt;", or the name given with {@link #setPart}.
 *     Voices 0 to 15 also get a midi-instrument with midi-channel
 *     voice+1, since MidiReader reads the channel as Voice and MusicXML
 *     numbers channels from 1; a part given a program, volume or pan gets
 *     them in its midi-instrument.</li>
 * <li>Onset and Duration count quarter notes, and the divisions element
 *     gives divisions per quarter note, so a position in divisions is
 *     onset times divisions. The writer picks the smallest divisions that
 *     makes every onset, duration and the measure length whole, with
 *     denominators up to {@link #MAX_DENOMINATOR}; a value that needs more
 *     is rounded to 1/{@link #MAX_DENOMINATOR} of a quarter and reported in
 *     {@link #getWarnings()}.</li>
 * <li>Measures are 4/4 by default ({@link #setTime}). A note that crosses
 *     a barline is split into tied notes (tie elements, plus tied
 *     notations for display). Notes with equal onset and duration in a part
 *     form a chord; overlapping notes go to further MusicXML voices,
 *     separated by backup elements. Every voice written in a measure fills
 *     it to the barline: gaps in the first voice of a staff become rests,
 *     gaps in the others forward elements, and a staff with nothing in a
 *     measure gets a measure rest.</li>
 * <li>Note values: each note is split into tied plain or dotted values,
 *     longest first, and each rest into values that follow the beats, or
 *     either into triplet values (time-modification 3:2) when only those
 *     fit, so it carries a type element. A length that
 *     neither expresses is written as one note without a type, and the
 *     write reports how many in {@link #getWarnings()}.</li>
 * <li>Clef: a part with no pitch below F3 (53) gets a treble clef, else
 *     one with no pitch above G4 (67) a bass clef, else a grand staff:
 *     pitches from middle C (60) up on staff 1 in treble clef, the rest on
 *     staff 2 in bass clef, in MusicXML voices from 5.</li>
 * <li>Pitch: the MIDI key number p gives octave floor(p/12)-1 and a step
 *     spelled with sharps; a fractional part of p goes into alter as a
 *     decimal, exact when its denominator has only the factors 2 and 5 and
 *     rounded to 10 places (with a warning) otherwise.</li>
 * <li>Loudness: the note's dynamics attribute, which the specification
 *     defines as a percentage of the MIDI 1.0 forte velocity 90; it is
 *     loudness/0.9 rounded to 4 places. A negative loudness is written as
 *     0, with a warning. For display, each measure of a MusicXML voice
 *     gets a dynamic mark (ppp to fff, by bands of velocity,
 *     {@link #dynamicMark}) from the mean velocity of the notes that start
 *     in it, written before its first such note when it differs from the
 *     voice's last mark, with a sound dynamics of the mark's velocity
 *     ({@link #setDynamicMarks} turns this off). A mark per note would
 *     follow every accent: the Convergences flute line, whose accents run
 *     from velocity 66 to 100, got a mark on almost every note.</li>
 * <li>A note of duration 0 becomes a grace note.</li>
 * <li>The first measure of the first part has a metronome mark and a sound
 *     tempo, 120 quarter notes per minute unless {@link #setTempo} says
 *     otherwise; 120 is the tempo the MIDI writers use by default.</li>
 * </ul>
 * MusicXML has no position before the first measure, so a negative onset is
 * an error.
 */
public final class MusicXMLWriter {

    public static final long MAX_DENOMINATOR = 3840;
    static final double TOLERANCE = 1e-9;

    /** Microseconds per quarter note, as in ScoreMidi: 500000 is 120 quarter notes per minute. */
    public static final int DEFAULT_TEMPO = 500000;

    private int beats = 4;
    private int beatType = 4;
    private int tempo = DEFAULT_TEMPO;
    private final List<String> warnings = new ArrayList<String>();
    private int untyped = 0;
    private final TreeMap<Integer, PartInfo> partInfos = new TreeMap<Integer, PartInfo>();
    private boolean dynamicMarks = true;

    /** Sets the time signature written in the first measure. */
    public void setTime(int beats, int beatType) {
        if (beats <= 0 || beatType <= 0 || Integer.bitCount(beatType) != 1) {
            throw new IllegalArgumentException("time signature " + beats + "/" + beatType);
        }
        this.beats = beats;
        this.beatType = beatType;
    }

    /**
     * Sets the tempo in microseconds per quarter note, the unit of
     * {@code ScoreMidi.setTempo} and of the MIDI Set Tempo event (1 to
     * 16777215). The first measure carries it as a metronome mark and a
     * sound tempo in quarter notes per minute, 60000000 divided by this
     * value, written to 6 decimal places.
     */
    public void setTempo(int microsecondsPerQuarter) {
        if (microsecondsPerQuarter <= 0 || microsecondsPerQuarter > 0xFFFFFF) {
            throw new IllegalArgumentException("tempo out of range: " + microsecondsPerQuarter);
        }
        this.tempo = microsecondsPerQuarter;
    }

    /**
     * Sets the name and MIDI settings of the part of a Voice: the name goes
     * into part-name and instrument-name, the program (plus 1, since
     * MusicXML numbers programs from 1), volume (as a percentage of 127)
     * and pan (in degrees, see {@link PartInfo#panDegrees}) into the
     * part's midi-instrument. Settings for a Voice with no notes are not
     * written. {@code null} removes the settings.
     */
    public void setPart(int voice, PartInfo part) {
        if (part == null) {
            this.partInfos.remove(voice);
        }
        else {
            this.partInfos.put(voice, part);
        }
    }

    /**
     * Sets whether the writer adds dynamic marks (on by default): a
     * direction with a dynamics mark from ppp to fff ({@link #dynamicMark})
     * and a sound dynamics, at the first note of a measure whose mean
     * velocity in a MusicXML voice falls in another band than the voice's
     * previous mark. The per-note dynamics attribute, which carries the
     * exact loudness, is written either way.
     */
    public void setDynamicMarks(boolean dynamicMarks) {
        this.dynamicMarks = dynamicMarks;
    }

    /** Quarter notes per minute as written in the file. */
    String tempoText() {
        return BigDecimal.valueOf(60000000L).divide(BigDecimal.valueOf(this.tempo), 6, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString();
    }

    /** Messages about values the last write had to round or clamp. */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(this.warnings);
    }

    public void writeScore(Denotator score, OutputStream out) throws IOException {
        this.write(ScoreNote.fromScore(score), out);
    }

    public void write(List<ScoreNote> notes, OutputStream out) throws IOException {
        Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        w.write(this.toXML(notes));
        w.flush();
    }

    public String toXML(List<ScoreNote> notes) {
        this.warnings.clear();
        this.untyped = 0;
        for (ScoreNote n : notes) {
            if (n.onset < 0) {
                throw new IllegalArgumentException("MusicXML has no time before the first measure, so a note cannot start at " + n.onset + ": " + n);
            }
            if (n.duration < 0 || Double.isNaN(n.duration) || Double.isInfinite(n.duration) || Double.isNaN(n.onset) || Double.isInfinite(n.onset)) {
                throw new IllegalArgumentException("onset and duration must be finite and the duration not negative: " + n);
            }
        }
        // exact times in quarter notes
        Frac measure = Frac.of(4L * this.beats, this.beatType);
        List<Frac[]> times = new ArrayList<Frac[]>(notes.size());
        BigInteger lcm = measure.den;
        for (ScoreNote n : notes) {
            Frac on = this.exact(n.onset, n, "onset");
            Frac end = on.add(this.exact(n.duration, n, "duration"));
            times.add(new Frac[] { on, end });
            lcm = lcm(lcm, on.den);
            lcm = lcm(lcm, end.den);
        }
        if (lcm.bitLength() > 31) {
            this.warnings.add("The onsets and durations need " + lcm + " divisions per quarter note; rounded to 1/" + MAX_DENOMINATOR);
            lcm = BigInteger.valueOf(MAX_DENOMINATOR);
            for (Frac[] t : times) {
                t[0] = round(t[0], MAX_DENOMINATOR);
                t[1] = round(t[1], MAX_DENOMINATOR);
            }
        }
        long divisions = lcm.longValue();
        long measureLength = measure.num.multiply(BigInteger.valueOf(divisions)).divide(measure.den).longValueExact();

        // parts by voice
        TreeMap<Integer, List<Placed>> parts = new TreeMap<Integer, List<Placed>>();
        for (int i = 0; i < notes.size(); i++) {
            ScoreNote n = notes.get(i);
            long on = toDivisions(times.get(i)[0], divisions);
            long end = toDivisions(times.get(i)[1], divisions);
            List<Placed> list = parts.get(n.voice);
            if (list == null) {
                list = new ArrayList<Placed>();
                parts.put(n.voice, list);
            }
            list.add(new Placed(n, on, end));
        }
        long lastEnd = 0;
        for (List<Placed> list : parts.values()) {
            for (Placed p : list) {
                lastEnd = Math.max(lastEnd, Math.max(p.end, p.on + 1));
            }
        }
        long measures = Math.max(1, (lastEnd + measureLength - 1) / measureLength);

        StringBuilder x = new StringBuilder(4096);
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        x.append("<!DOCTYPE score-partwise PUBLIC \"-//Recordare//DTD MusicXML 4.0 Partwise//EN\" \"http://www.musicxml.org/dtds/partwise.dtd\">\n");
        x.append("<score-partwise version=\"4.0\">\n");
        x.append("  <identification>\n    <encoding>\n      <software>Rubato Composer</software>\n    </encoding>\n  </identification>\n");
        x.append("  <part-list>\n");
        for (int voice : parts.keySet()) {
            String id = partId(voice);
            PartInfo info = this.partInfo(voice);
            String name = info.getName() != null ? escape(info.getName()) : "Voice " + voice;
            x.append("    <score-part id=\"").append(id).append("\">\n");
            x.append("      <part-name>").append(name).append("</part-name>\n");
            boolean channel = voice >= 0 && voice < 16;
            if (channel || info.hasMidiSettings()) {
                x.append("      <score-instrument id=\"").append(id).append("-I1\">\n");
                x.append("        <instrument-name>").append(name).append("</instrument-name>\n");
                x.append("      </score-instrument>\n");
                x.append("      <midi-instrument id=\"").append(id).append("-I1\">\n");
                if (channel) {
                    x.append("        <midi-channel>").append(voice + 1).append("</midi-channel>\n");
                }
                if (info.hasProgram()) {
                    // MusicXML numbers programs from 1, MIDI messages from 0
                    x.append("        <midi-program>").append(info.getProgram() + 1).append("</midi-program>\n");
                }
                if (info.hasVolume()) {
                    x.append("        <volume>").append(PartInfo.volumePercent(info.getVolume()).toPlainString()).append("</volume>\n");
                }
                if (info.hasPan()) {
                    x.append("        <pan>").append(PartInfo.panDegrees(info.getPan()).toPlainString()).append("</pan>\n");
                }
                x.append("      </midi-instrument>\n");
            }
            x.append("    </score-part>\n");
        }
        if (parts.isEmpty()) {
            x.append("    <score-part id=\"V0\">\n      <part-name>Voice 0</part-name>\n    </score-part>\n");
            parts.put(0, new ArrayList<Placed>());
        }
        x.append("  </part-list>\n");
        boolean firstPart = true;
        for (Map.Entry<Integer, List<Placed>> part : parts.entrySet()) {
            this.writePart(x, partId(part.getKey()), part.getValue(), divisions, measureLength, measures, firstPart);
            firstPart = false;
        }
        x.append("</score-partwise>\n");
        if (this.untyped > 0) {
            this.warnings.add(this.untyped + " notes or rests have lengths that no plain, dotted or triplet note value expresses; they are written without a note type");
        }
        return x.toString();
    }

    private PartInfo partInfo(int voice) {
        PartInfo info = this.partInfos.get(voice);
        return info != null ? info : PartInfo.NONE;
    }

    static String escape(String text) {
        StringBuilder b = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '"': b.append("&quot;"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    static String partId(int voice) {
        return "V" + voice;
    }

    private Frac exact(double value, ScoreNote n, String what) {
        Frac f = Frac.approximate(value, MAX_DENOMINATOR, TOLERANCE * Math.max(1.0, Math.abs(value)));
        if (f == null) {
            f = Frac.of(Math.round(value * MAX_DENOMINATOR), MAX_DENOMINATOR);
            this.warnings.add("The " + what + " " + value + " of " + n + " is rounded to " + f + " quarter notes");
        }
        return f;
    }

    private static Frac round(Frac f, long den) {
        BigDecimal v = new BigDecimal(f.num).multiply(BigDecimal.valueOf(den)).divide(new BigDecimal(f.den), 0, RoundingMode.HALF_EVEN);
        return new Frac(v.toBigIntegerExact(), BigInteger.valueOf(den));
    }

    private static long toDivisions(Frac f, long divisions) {
        Frac d = f.multiply(Frac.of(divisions, 1));
        if (!d.den.equals(BigInteger.ONE)) {
            throw new IllegalStateException("not whole in divisions: " + f);
        }
        return d.num.longValueExact();
    }

    private static BigInteger lcm(BigInteger a, BigInteger b) {
        return a.divide(a.gcd(b)).multiply(b);
    }

    /** A note with its position in divisions. */
    private static final class Placed {
        final ScoreNote note;
        final long on;
        final long end;

        Placed(ScoreNote note, long on, long end) {
            this.note = note;
            this.on = on;
            this.end = end;
        }
    }

    /** Notes of one part with equal onset and end. */
    private static final class Chord {
        final long on;
        final long end;
        final List<ScoreNote> notes = new ArrayList<ScoreNote>();

        Chord(long on, long end) {
            this.on = on;
            this.end = end;
        }
    }

    /** Notes of one part that go on one staff in one MusicXML voice, none overlapping. */
    private static final class Layer {
        /** 0 when the part has one staff, else 1 (treble) or 2 (bass). */
        final int staff;
        final int voice;
        /** The first layer of a staff fills its gaps with rests; the others use forward elements. */
        final boolean primary;
        final List<Chord> chords = new ArrayList<Chord>();
        int next = 0;
        /** The dynamic mark last written in this layer, null before the first. */
        String dynamic;

        Layer(int staff, int voice, boolean primary) {
            this.staff = staff;
            this.voice = voice;
            this.primary = primary;
        }
    }

    static final int TREBLE = 0, BASS = 1, GRAND = 2;
    private static final Rational MIDDLE_C = new Rational(60);
    private static final Rational TREBLE_LOWEST = new Rational(53);
    private static final Rational BASS_HIGHEST = new Rational(67);

    /**
     * The clef of a part: treble when no pitch is below F3 (53, three ledger
     * lines under the treble staff), else bass when no pitch is above G4
     * (67, three ledger lines over the bass staff), else a grand staff.
     */
    static int clef(List<Placed> placed) {
        if (placed.isEmpty()) {
            return TREBLE;
        }
        Rational min = null, max = null;
        for (Placed p : placed) {
            if (min == null || p.note.pitch.compareTo(min) < 0) min = p.note.pitch;
            if (max == null || p.note.pitch.compareTo(max) > 0) max = p.note.pitch;
        }
        if (min.compareTo(TREBLE_LOWEST) >= 0) {
            return TREBLE;
        }
        if (max.compareTo(BASS_HIGHEST) <= 0) {
            return BASS;
        }
        return GRAND;
    }

    /** Groups notes with equal onset and end into chords and puts the chords into non-overlapping layers. */
    private static void layers(List<Placed> placed, int staff, int firstVoice, List<Layer> out) {
        TreeMap<Long, TreeMap<Long, Chord>> byOnset = new TreeMap<Long, TreeMap<Long, Chord>>();
        for (Placed p : placed) {
            TreeMap<Long, Chord> byEnd = byOnset.get(p.on);
            if (byEnd == null) {
                byEnd = new TreeMap<Long, Chord>();
                byOnset.put(p.on, byEnd);
            }
            Chord c = byEnd.get(p.end);
            if (c == null) {
                c = new Chord(p.on, p.end);
                byEnd.put(p.end, c);
            }
            c.notes.add(p.note);
        }
        List<Layer> layers = new ArrayList<Layer>();
        List<Long> layerEnds = new ArrayList<Long>();
        // the primary layer exists even for an empty staff, so the staff gets measure rests
        layers.add(new Layer(staff, firstVoice, true));
        layerEnds.add(0L);
        for (TreeMap<Long, Chord> byEnd : byOnset.values()) {
            for (Chord c : byEnd.values()) {
                Collections.sort(c.notes);
                int layer = -1;
                for (int i = 0; i < layers.size(); i++) {
                    if (layerEnds.get(i) <= c.on) {
                        layer = i;
                        break;
                    }
                }
                if (layer < 0) {
                    layers.add(new Layer(staff, firstVoice + layers.size(), false));
                    layerEnds.add(0L);
                    layer = layers.size() - 1;
                }
                layers.get(layer).chords.add(c);
                layerEnds.set(layer, Math.max(c.on, c.end));
            }
        }
        out.addAll(layers);
    }

    private void writePart(StringBuilder x, String id, List<Placed> placed, long divisions, long measureLength, long measures, boolean firstPart) {
        int clef = clef(placed);
        List<Layer> layers = new ArrayList<Layer>();
        if (clef == GRAND) {
            List<Placed> upper = new ArrayList<Placed>();
            List<Placed> lower = new ArrayList<Placed>();
            for (Placed p : placed) {
                (p.note.pitch.compareTo(MIDDLE_C) >= 0 ? upper : lower).add(p);
            }
            layers(upper, 1, 1, layers);
            // MusicXML voices 1-4 for the upper staff and 5-8 for the lower, as notation programs number them
            layers(lower, 2, Math.max(5, layers.size() + 1), layers);
        }
        else {
            layers(placed, 0, 1, layers);
        }

        x.append("  <part id=\"").append(id).append("\">\n");
        for (long m = 0; m < measures; m++) {
            long start = m * measureLength;
            long stop = start + measureLength;
            x.append("    <measure number=\"").append(m + 1).append("\">\n");
            if (m == 0) {
                x.append("      <attributes>\n");
                x.append("        <divisions>").append(divisions).append("</divisions>\n");
                x.append("        <key>\n          <fifths>0</fifths>\n        </key>\n");
                x.append("        <time>\n          <beats>").append(this.beats).append("</beats>\n          <beat-type>").append(this.beatType).append("</beat-type>\n        </time>\n");
                if (clef == GRAND) {
                    x.append("        <staves>2</staves>\n");
                    x.append("        <clef number=\"1\">\n          <sign>G</sign>\n          <line>2</line>\n        </clef>\n");
                    x.append("        <clef number=\"2\">\n          <sign>F</sign>\n          <line>4</line>\n        </clef>\n");
                }
                else if (clef == BASS) {
                    x.append("        <clef>\n          <sign>F</sign>\n          <line>4</line>\n        </clef>\n");
                }
                else {
                    x.append("        <clef>\n          <sign>G</sign>\n          <line>2</line>\n        </clef>\n");
                }
                x.append("      </attributes>\n");
                if (firstPart) {
                    String bpm = this.tempoText();
                    x.append("      <direction placement=\"above\">\n        <direction-type>\n          <metronome>\n");
                    x.append("            <beat-unit>quarter</beat-unit>\n            <per-minute>").append(bpm).append("</per-minute>\n");
                    x.append("          </metronome>\n        </direction-type>\n");
                    if (clef == GRAND) {
                        x.append("        <staff>1</staff>\n");
                    }
                    x.append("        <sound tempo=\"").append(bpm).append("\"/>\n      </direction>\n");
                }
            }
            // every layer that is written fills the whole measure, so each one after the first backs up a measure
            boolean wroteAny = false;
            for (Layer layer : layers) {
                List<Chord> chords = layer.chords;
                // skip chords that ended before this measure
                while (layer.next < chords.size() && chords.get(layer.next).end <= start && !(isGrace(chords.get(layer.next)) && chords.get(layer.next).on >= start)) {
                    layer.next++;
                }
                boolean has = layer.next < chords.size() && chords.get(layer.next).on < stop;
                if (!has && !layer.primary) {
                    continue;
                }
                if (wroteAny) {
                    x.append("      <backup>\n        <duration>").append(measureLength).append("</duration>\n      </backup>\n");
                }
                wroteAny = true;
                if (!has) {
                    x.append("      <note>\n        <rest measure=\"yes\"/>\n        <duration>").append(measureLength).append("</duration>\n        <voice>").append(layer.voice).append("</voice>\n");
                    staff(x, layer);
                    x.append("      </note>\n");
                    continue;
                }
                long cursor = start;
                String mark = this.measureDynamic(chords, layer.next, start, stop);
                for (int i = layer.next; i < chords.size() && chords.get(i).on < stop; i++) {
                    Chord c = chords.get(i);
                    long from = Math.max(c.on, start);
                    if (from > cursor) {
                        this.gap(x, layer, cursor - start, from - cursor, divisions);
                        cursor = from;
                    }
                    if (mark != null && c.on >= start) {
                        this.dynamic(x, mark, layer);
                        mark = null;
                    }
                    if (isGrace(c)) {
                        this.notes(x, c, layer, new Piece(0, "eighth", false, false), false, false, true);
                        continue;
                    }
                    long to = Math.min(c.end, stop);
                    List<Piece> pieces = this.pieces(to - from, divisions);
                    for (int k = 0; k < pieces.size(); k++) {
                        boolean tieStop = k > 0 || c.on < start;
                        boolean tieStart = k < pieces.size() - 1 || c.end > stop;
                        this.notes(x, c, layer, pieces.get(k), tieStop, tieStart, false);
                    }
                    cursor = to;
                    if (c.end > stop) {
                        break;
                    }
                }
                if (cursor < stop) {
                    this.gap(x, layer, cursor - start, stop - cursor, divisions);
                }
            }
            x.append("    </measure>\n");
        }
        x.append("  </part>\n");
    }

    /** Dynamic marks from ppp to fff, and the velocity each one stands for. */
    static final String[] DYNAMICS = { "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff" };
    static final int[] DYNAMIC_VELOCITIES = { 16, 33, 49, 64, 80, 96, 112, 126 };
    /** The highest velocity of each mark but fff: the bands meet halfway between the marks' velocities. */
    static final int[] DYNAMIC_UPPER = { 24, 40, 56, 71, 87, 103, 118 };

    /** The dynamic mark of a velocity: ppp up to 24, pp to 40, p to 56, mp to 71, mf to 87, f to 103, ff to 118, fff above. */
    public static String dynamicMark(int velocity) {
        for (int i = 0; i < DYNAMIC_UPPER.length; i++) {
            if (velocity <= DYNAMIC_UPPER[i]) {
                return DYNAMICS[i];
            }
        }
        return DYNAMICS[DYNAMICS.length - 1];
    }

    /**
     * The dynamic mark of the notes that start in a measure in one layer:
     * the band of their mean velocity, rounded half up; null when marks are
     * off or no note starts in the measure.
     */
    private String measureDynamic(List<Chord> chords, int first, long start, long stop) {
        if (!this.dynamicMarks) {
            return null;
        }
        long sum = 0;
        int count = 0;
        for (int i = first; i < chords.size() && chords.get(i).on < stop; i++) {
            Chord c = chords.get(i);
            if (c.on >= start) {
                for (ScoreNote n : c.notes) {
                    sum += Math.max(0, n.loudness);
                    count++;
                }
            }
        }
        return count == 0 ? null : dynamicMark((int)((2 * sum + count) / (2 * count)));
    }

    /** Writes a dynamic mark when it differs from the last mark of the layer. */
    private void dynamic(StringBuilder x, String mark, Layer layer) {
        if (mark.equals(layer.dynamic)) {
            return;
        }
        layer.dynamic = mark;
        int velocity = DYNAMIC_VELOCITIES[java.util.Arrays.asList(DYNAMICS).indexOf(mark)];
        x.append("      <direction placement=\"below\">\n        <direction-type>\n          <dynamics>\n            <").append(mark).append("/>\n");
        x.append("          </dynamics>\n        </direction-type>\n        <voice>").append(layer.voice).append("</voice>\n");
        staff(x, layer);
        x.append("        <sound dynamics=\"").append(percent(velocity)).append("\"/>\n      </direction>\n");
    }

    /** A velocity as MusicXML's dynamics percentage of forte (velocity 90), to 4 places. */
    static String percent(int velocity) {
        return BigDecimal.valueOf(velocity).multiply(BigDecimal.TEN).divide(BigDecimal.valueOf(9), 4, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString();
    }

    private static boolean isGrace(Chord c) {
        return c.end == c.on;
    }

    private static void staff(StringBuilder x, Layer layer) {
        if (layer.staff > 0) {
            x.append("        <staff>").append(layer.staff).append("</staff>\n");
        }
    }

    /** Rests in a primary layer, a forward element in the others. */
    private void gap(StringBuilder x, Layer layer, long offset, long length, long divisions) {
        if (layer.primary) {
            for (Piece p : this.pieces(length, divisions, offset)) {
                x.append("      <note>\n        <rest/>\n        <duration>").append(p.length).append("</duration>\n        <voice>").append(layer.voice).append("</voice>\n");
                this.type(x, p);
                staff(x, layer);
                x.append("      </note>\n");
            }
        }
        else {
            x.append("      <forward>\n        <duration>").append(length).append("</duration>\n        <voice>").append(layer.voice).append("</voice>\n");
            staff(x, layer);
            x.append("      </forward>\n");
        }
    }

    private void type(StringBuilder x, Piece p) {
        if (p.type == null) {
            return;
        }
        x.append("        <type>").append(p.type).append("</type>\n");
        if (p.dot) {
            x.append("        <dot/>\n");
        }
        if (p.triplet) {
            x.append("        <time-modification>\n          <actual-notes>3</actual-notes>\n          <normal-notes>2</normal-notes>\n        </time-modification>\n");
        }
    }

    private void notes(StringBuilder x, Chord c, Layer layer, Piece piece, boolean tieStop, boolean tieStart, boolean grace) {
        boolean first = true;
        for (ScoreNote n : c.notes) {
            int loudness = n.loudness;
            if (loudness < 0) {
                this.warnOnce("Loudness " + loudness + " of " + n + " is written as dynamics 0");
                loudness = 0;
            }
            String dynamics = percent(loudness);
            x.append("      <note dynamics=\"").append(dynamics).append("\">\n");
            if (grace) {
                x.append("        <grace/>\n");
            }
            if (!first) {
                x.append("        <chord/>\n");
            }
            this.pitch(x, n);
            if (!grace) {
                x.append("        <duration>").append(piece.length).append("</duration>\n");
            }
            if (tieStop) {
                x.append("        <tie type=\"stop\"/>\n");
            }
            if (tieStart) {
                x.append("        <tie type=\"start\"/>\n");
            }
            x.append("        <voice>").append(layer.voice).append("</voice>\n");
            this.type(x, piece);
            staff(x, layer);
            if (tieStop || tieStart) {
                x.append("        <notations>\n");
                if (tieStop) {
                    x.append("          <tied type=\"stop\"/>\n");
                }
                if (tieStart) {
                    x.append("          <tied type=\"start\"/>\n");
                }
                x.append("        </notations>\n");
            }
            x.append("      </note>\n");
            first = false;
        }
    }

    /** A written note or rest: its length in divisions and its note type, dot and triplet mark. */
    static final class Piece {
        final long length;
        final String type;
        final boolean dot;
        final boolean triplet;

        Piece(long length, String type, boolean dot, boolean triplet) {
            this.length = length;
            this.type = type;
            this.dot = dot;
            this.triplet = triplet;
        }
    }

    /** Note values from the dotted breve down to the 1024th, longest first, in quarter notes. */
    private static final List<Frac> VALUES = new ArrayList<Frac>();
    private static final List<String> VALUE_TYPES = new ArrayList<String>();
    private static final List<Boolean> VALUE_DOTS = new ArrayList<Boolean>();
    static {
        String[] types = { "breve", "whole", "half", "quarter", "eighth", "16th", "32nd", "64th", "128th", "256th", "512th", "1024th" };
        Frac v = Frac.of(8, 1);
        for (String t : types) {
            if (!"1024th".equals(t)) {
                VALUES.add(v.multiply(Frac.of(3, 2)));
                VALUE_TYPES.add(t);
                VALUE_DOTS.add(Boolean.TRUE);
            }
            VALUES.add(v);
            VALUE_TYPES.add(t);
            VALUE_DOTS.add(Boolean.FALSE);
            v = v.multiply(Frac.of(1, 2));
        }
    }

    /**
     * Splits a length into tied note values, longest first, so that every
     * note and rest carries a note type: plain or dotted values when the
     * length in quarter notes is a multiple of a 1024th note, triplet
     * values (time-modification 3:2) when two thirds of it is. Any other
     * length stays one piece without a type and is counted for a warning.
     */
    List<Piece> pieces(long length, long divisions) {
        return this.pieces(length, divisions, -1);
    }

    /**
     * As {@link #pieces(long, long)}, for a rest that starts {@code offset}
     * divisions after the barline: each value starts on a multiple of its
     * own undotted length and ends by the next multiple of twice that, so
     * rests follow the beats (an eighth, a quarter and a half after an
     * eighth note in 4/4, not a dotted half and an eighth). Where the offset
     * is off the grid of 1024th notes, the rest is split as a note is.
     */
    List<Piece> pieces(long length, long divisions, long offset) {
        List<Piece> out = offset >= 0 ? aligned(length, divisions, offset) : null;
        if (out == null) {
            out = greedy(length, divisions, false);
        }
        if (out == null) {
            out = greedy(length, divisions, true);
        }
        if (out == null) {
            this.untyped++;
            out = new ArrayList<Piece>();
            out.add(new Piece(length, null, false, false));
        }
        return out;
    }

    /** Splits a rest at the beats, as described at {@link #pieces(long, long, long)}; null when the rest or its offset is off the grid of 1024th notes. */
    private static List<Piece> aligned(long length, long divisions, long offset) {
        Frac rest = Frac.of(length, divisions);
        Frac at = Frac.of(offset, divisions);
        Frac grid = Frac.of(256, 1);
        if (!rest.multiply(grid).den.equals(BigInteger.ONE) || !at.multiply(grid).den.equals(BigInteger.ONE)) {
            return null;
        }
        Frac scale = Frac.of(divisions, 1);
        List<Piece> out = new ArrayList<Piece>();
        while (rest.signum() > 0) {
            int found = -1;
            for (int i = 0; i < VALUES.size(); i++) {
                Frac v = VALUES.get(i);
                if (v.compareTo(rest) > 0) {
                    continue;
                }
                Frac unit = VALUE_DOTS.get(i) ? v.multiply(Frac.of(2, 3)) : v;
                // at is a multiple of unit, and at + v stays within the next multiple of 2 * unit
                Frac k = at.divide(unit);
                if (!k.den.equals(BigInteger.ONE)) {
                    continue;
                }
                Frac limit = Frac.of(k.num.longValueExact() / 2 * 2 + 2, 1).multiply(unit);
                if (at.add(v).compareTo(limit) <= 0) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                return null;
            }
            Frac d = VALUES.get(found).multiply(scale);
            if (!d.den.equals(BigInteger.ONE)) {
                return null;
            }
            out.add(new Piece(d.num.longValueExact(), VALUE_TYPES.get(found), VALUE_DOTS.get(found), false));
            rest = rest.subtract(VALUES.get(found));
            at = at.add(VALUES.get(found));
        }
        return out;
    }

    private static List<Piece> greedy(long length, long divisions, boolean triplet) {
        // the length in written quarter notes: a triplet value sounds two thirds of what it is written as
        Frac rest = Frac.of(length, divisions);
        if (triplet) {
            rest = rest.multiply(Frac.of(3, 2));
        }
        if (!rest.multiply(Frac.of(256, 1)).den.equals(BigInteger.ONE)) {
            return null;
        }
        Frac scale = triplet ? Frac.of(2 * divisions, 3) : Frac.of(divisions, 1);
        List<Piece> out = new ArrayList<Piece>();
        while (rest.signum() > 0) {
            int i = 0;
            while (VALUES.get(i).compareTo(rest) > 0) {
                i++;
            }
            Frac d = VALUES.get(i).multiply(scale);
            if (!d.den.equals(BigInteger.ONE)) {
                return null;
            }
            out.add(new Piece(d.num.longValueExact(), VALUE_TYPES.get(i), VALUE_DOTS.get(i), triplet));
            rest = rest.subtract(VALUES.get(i));
        }
        return out;
    }

    /** A note split into tied pieces is written several times but reported once. */
    private void warnOnce(String message) {
        if (!this.warnings.contains(message)) {
            this.warnings.add(message);
        }
    }

    private static final String[] STEPS  = { "C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B" };
    private static final int[]    ALTERS = {  0,   1,   0,   1,   0,   0,   1,   0,   1,   0,   1,   0  };

    private void pitch(StringBuilder x, ScoreNote n) {
        Rational p = n.pitch;
        int num = p.getNumerator();
        int den = p.getDenominator();
        int semitone = Math.floorDiv(num, den);
        int rest = num - semitone * den; // 0 <= rest < den
        int pc = Math.floorMod(semitone, 12);
        int octave = Math.floorDiv(semitone, 12) - 1;
        BigDecimal alter = BigDecimal.valueOf(ALTERS[pc]);
        if (rest != 0) {
            BigDecimal fraction;
            try {
                fraction = BigDecimal.valueOf(rest).divide(BigDecimal.valueOf(den));
            }
            catch (ArithmeticException e) {
                fraction = BigDecimal.valueOf(rest).divide(BigDecimal.valueOf(den), 10, RoundingMode.HALF_EVEN);
                this.warnOnce("The pitch " + p + " of " + n + " has alter rounded to 10 decimal places");
            }
            alter = alter.add(fraction);
        }
        x.append("        <pitch>\n          <step>").append(STEPS[pc]).append("</step>\n");
        if (alter.signum() != 0) {
            x.append("          <alter>").append(alter.stripTrailingZeros().toPlainString()).append("</alter>\n");
        }
        x.append("          <octave>").append(octave).append("</octave>\n        </pitch>\n");
        if (octave < 0 || octave > 9) {
            this.warnOnce("The pitch " + p + " of " + n + " is in octave " + octave + ", outside MusicXML's 0 to 9");
        }
    }
}
