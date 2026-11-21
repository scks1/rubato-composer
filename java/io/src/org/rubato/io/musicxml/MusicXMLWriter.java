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
 *     "Voice &lt;voice&gt;". Voices 0 to 15 also get a midi-instrument with
 *     midi-channel voice+1, since MidiReader reads the channel as Voice and
 *     MusicXML numbers channels from 1.</li>
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
 *     separated by backup elements. Gaps in the first voice become rests,
 *     gaps in the others forward elements.</li>
 * <li>Pitch: the MIDI key number p gives octave floor(p/12)-1 and a step
 *     spelled with sharps; a fractional part of p goes into alter as a
 *     decimal, exact when its denominator has only the factors 2 and 5 and
 *     rounded to 10 places (with a warning) otherwise.</li>
 * <li>Loudness: the note's dynamics attribute, which the specification
 *     defines as a percentage of the MIDI 1.0 forte velocity 90; it is
 *     loudness/0.9 rounded to 4 places. A negative loudness is written as
 *     0, with a warning.</li>
 * <li>A note of duration 0 becomes a grace note.</li>
 * <li>The first measure of the first part has a sound element with tempo
 *     120, the tempo the MIDI writers use.</li>
 * </ul>
 * MusicXML has no position before the first measure, so a negative onset is
 * an error.
 */
public final class MusicXMLWriter {

    public static final long MAX_DENOMINATOR = 3840;
    static final double TOLERANCE = 1e-9;

    private int beats = 4;
    private int beatType = 4;
    private final List<String> warnings = new ArrayList<String>();

    /** Sets the time signature written in the first measure. */
    public void setTime(int beats, int beatType) {
        if (beats <= 0 || beatType <= 0 || Integer.bitCount(beatType) != 1) {
            throw new IllegalArgumentException("time signature " + beats + "/" + beatType);
        }
        this.beats = beats;
        this.beatType = beatType;
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
            x.append("    <score-part id=\"").append(id).append("\">\n");
            x.append("      <part-name>Voice ").append(voice).append("</part-name>\n");
            if (voice >= 0 && voice < 16) {
                x.append("      <score-instrument id=\"").append(id).append("-I1\">\n");
                x.append("        <instrument-name>Voice ").append(voice).append("</instrument-name>\n");
                x.append("      </score-instrument>\n");
                x.append("      <midi-instrument id=\"").append(id).append("-I1\">\n");
                x.append("        <midi-channel>").append(voice + 1).append("</midi-channel>\n");
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
        return x.toString();
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

    private void writePart(StringBuilder x, String id, List<Placed> placed, long divisions, long measureLength, long measures, boolean firstPart) {
        // chords, ordered by onset, then duration (grace notes first)
        TreeMap<Long, TreeMap<Long, Chord>> byOnset = new TreeMap<Long, TreeMap<Long, Chord>>();
        int lowPitches = 0;
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
            if (p.note.pitch.compareTo(new Rational(60)) < 0) {
                lowPitches++;
            }
        }
        // assign chords to MusicXML voices so that no two overlap
        List<List<Chord>> layers = new ArrayList<List<Chord>>();
        List<Long> layerEnds = new ArrayList<Long>();
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
                    layers.add(new ArrayList<Chord>());
                    layerEnds.add(0L);
                    layer = layers.size() - 1;
                }
                layers.get(layer).add(c);
                layerEnds.set(layer, Math.max(c.on, c.end));
            }
        }
        boolean bass = lowPitches * 2 > placed.size();

        x.append("  <part id=\"").append(id).append("\">\n");
        int[] next = new int[layers.size()];
        for (long m = 0; m < measures; m++) {
            long start = m * measureLength;
            long stop = start + measureLength;
            x.append("    <measure number=\"").append(m + 1).append("\">\n");
            if (m == 0) {
                x.append("      <attributes>\n");
                x.append("        <divisions>").append(divisions).append("</divisions>\n");
                x.append("        <key>\n          <fifths>0</fifths>\n        </key>\n");
                x.append("        <time>\n          <beats>").append(this.beats).append("</beats>\n          <beat-type>").append(this.beatType).append("</beat-type>\n        </time>\n");
                if (bass) {
                    x.append("        <clef>\n          <sign>F</sign>\n          <line>4</line>\n        </clef>\n");
                }
                else {
                    x.append("        <clef>\n          <sign>G</sign>\n          <line>2</line>\n        </clef>\n");
                }
                x.append("      </attributes>\n");
                if (firstPart) {
                    x.append("      <sound tempo=\"120\"/>\n");
                }
            }
            long pos = start;
            boolean any = false;
            for (int l = 0; l < layers.size(); l++) {
                List<Chord> layer = layers.get(l);
                // skip chords that ended before this measure
                while (next[l] < layer.size() && chordEnd(layer.get(next[l])) <= start && !(isGrace(layer.get(next[l])) && layer.get(next[l]).on >= start)) {
                    next[l]++;
                }
                int i = next[l];
                boolean wrote = false;
                long cursor = start;
                while (i < layer.size() && layer.get(i).on < stop) {
                    Chord c = layer.get(i);
                    if (!wrote) {
                        if (pos > start) {
                            x.append("      <backup>\n        <duration>").append(pos - start).append("</duration>\n      </backup>\n");
                        }
                        pos = start;
                        wrote = true;
                    }
                    long from = Math.max(c.on, start);
                    if (from > cursor) {
                        this.gap(x, l, from - cursor);
                        cursor = from;
                    }
                    if (isGrace(c)) {
                        this.notes(x, c, l, 0, false, false, true, divisions);
                        i++;
                        continue;
                    }
                    long to = Math.min(c.end, stop);
                    this.notes(x, c, l, to - from, c.on < start, c.end > stop, false, divisions);
                    cursor = to;
                    if (c.end > stop) {
                        break;
                    }
                    i++;
                }
                if (wrote) {
                    any = true;
                    if (l == 0 && cursor < stop) {
                        this.gap(x, 0, stop - cursor);
                        cursor = stop;
                    }
                    pos = cursor;
                }
            }
            if (!any) {
                x.append("      <note>\n        <rest measure=\"yes\"/>\n        <duration>").append(measureLength).append("</duration>\n        <voice>1</voice>\n      </note>\n");
            }
            x.append("    </measure>\n");
        }
        x.append("  </part>\n");
    }

    private static boolean isGrace(Chord c) {
        return c.end == c.on;
    }

    private static long chordEnd(Chord c) {
        return c.end;
    }

    private void gap(StringBuilder x, int layer, long length) {
        if (layer == 0) {
            x.append("      <note>\n        <rest/>\n        <duration>").append(length).append("</duration>\n        <voice>1</voice>\n      </note>\n");
        }
        else {
            x.append("      <forward>\n        <duration>").append(length).append("</duration>\n        <voice>").append(layer + 1).append("</voice>\n      </forward>\n");
        }
    }

    private void notes(StringBuilder x, Chord c, int layer, long length, boolean tieStop, boolean tieStart, boolean grace, long divisions) {
        String type = grace ? null : noteType(Frac.of(length, divisions));
        boolean dotted = type != null && type.endsWith(".");
        if (dotted) {
            type = type.substring(0, type.length() - 1);
        }
        boolean first = true;
        for (ScoreNote n : c.notes) {
            int loudness = n.loudness;
            if (loudness < 0) {
                this.warnings.add("Loudness " + loudness + " of " + n + " is written as dynamics 0");
                loudness = 0;
            }
            String dynamics = BigDecimal.valueOf(loudness).multiply(BigDecimal.TEN).divide(BigDecimal.valueOf(9), 4, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString();
            x.append("      <note dynamics=\"").append(dynamics).append("\">\n");
            if (grace) {
                x.append("        <grace/>\n");
            }
            if (!first) {
                x.append("        <chord/>\n");
            }
            this.pitch(x, n);
            if (!grace) {
                x.append("        <duration>").append(length).append("</duration>\n");
            }
            if (tieStop) {
                x.append("        <tie type=\"stop\"/>\n");
            }
            if (tieStart) {
                x.append("        <tie type=\"start\"/>\n");
            }
            x.append("        <voice>").append(layer + 1).append("</voice>\n");
            if (type != null) {
                x.append("        <type>").append(type).append("</type>\n");
                if (dotted) {
                    x.append("        <dot/>\n");
                }
            }
            else if (grace) {
                x.append("        <type>eighth</type>\n");
            }
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
                this.warnings.add("The pitch " + p + " of " + n + " has alter rounded to 10 decimal places");
            }
            alter = alter.add(fraction);
        }
        x.append("        <pitch>\n          <step>").append(STEPS[pc]).append("</step>\n");
        if (alter.signum() != 0) {
            x.append("          <alter>").append(alter.stripTrailingZeros().toPlainString()).append("</alter>\n");
        }
        x.append("          <octave>").append(octave).append("</octave>\n        </pitch>\n");
        if (octave < 0 || octave > 9) {
            this.warnings.add("The pitch " + p + " of " + n + " is in octave " + octave + ", outside MusicXML's 0 to 9");
        }
    }

    private static final String[] TYPES = {
        "1024th", "512th", "256th", "128th", "64th", "32nd", "16th", "eighth", "quarter", "half", "whole", "breve", "long", "maxima"
    };

    /** The note type of a length in quarter notes, with a trailing "." when dotted, or null. */
    static String noteType(Frac quarters) {
        Frac base = Frac.of(1, 256);
        Frac two = Frac.of(2, 1);
        Frac dot = Frac.of(3, 2);
        for (String t : TYPES) {
            if (quarters.equals(base)) {
                return t;
            }
            if (quarters.equals(base.multiply(dot))) {
                return t + ".";
            }
            base = base.multiply(two);
        }
        return null;
    }
}
