/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */
package org.rubato.rubettes.gesture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.rubato.io.ScoreNote;
import org.rubato.math.gesture.HandGesture;
import org.rubato.math.yoneda.Denotator;

/**
 * Headless access to the hand gesture of a Score (Mazzola, The Topos of
 * Music, vol. III, Example 60, p. 915), built by
 * {@link org.rubato.math.gesture.HandGesture} from a fingering the user
 * supplies, as in Mazzola and Mueller, ICMC 2003.
 */
public final class HandGestures {

    private HandGestures() { /* static utility */ }

    /** The gesture with the text the rubette shows. */
    public static final class Report {
        private final HandGesture gesture;

        Report(HandGesture gesture) {
            this.gesture = gesture;
        }

        public HandGesture getGesture() {
            return gesture;
        }

        public String text() {
            HandGesture h = gesture;
            HandGesture.Settings s = h.settings();
            StringBuilder b = new StringBuilder();
            b.append("Hand gesture of ").append(h.noteCount()).append(" notes")
             .append(" (Topos of Music III, Example 60, p. 915; section 61.4)\n");
            b.append(s.hand == HandGesture.Hand.LEFT ? "left hand" : "right hand");
            b.append(String.format(Locale.ROOT, ", acceleration limit %s cm/s^2, %s s per time unit, lift %s cm,"
                                   + " maximum shortening %s%n", num(s.accel[0]), num(s.secondsPerUnit),
                                   num(s.lift), num(s.maxShortening)));
            b.append("fingering in onset order:");
            for (int f : h.fingering()) {
                b.append(' ').append(f);
            }
            b.append('\n');
            b.append("performable: ").append(h.performable() ? "yes" : "no").append('\n');
            for (String p : h.problems()) {
                b.append("  ").append(p).append('\n');
            }
            b.append("\nreshapes: ").append(h.reshapes().size())
             .append(" (a free finger moved because a span or the finger order required it)\n");
            for (HandGesture.Reshape r : h.reshapes()) {
                b.append("  ").append(r).append('\n');
            }
            b.append("\nshortened notes: ").append(h.shortenings().size()).append('\n');
            double[][] notes = h.notes();
            for (HandGesture.Shortening x : h.shortenings()) {
                b.append(String.format(Locale.ROOT, "  note at E = %s (pitch %s, finger %d) ends at %s instead of %s%n",
                                       num(notes[x.noteIndex][0]), num(notes[x.noteIndex][2]), x.finger,
                                       num(x.newEnd), num(x.originalEnd)));
            }
            b.append("\nfinger  moves  peak acceleration (cm/s^2)\n");
            for (int f = 1; f <= 5; f++) {
                b.append(String.format(Locale.ROOT, "%6d %6d  %.4g%n", f, h.moves(f).size(), h.peakAcceleration(f)));
            }
            b.append("\nre-check of the physical curves: ").append(h.recheckViolations())
             .append(" sample times break a span or the order\n");
            for (String e : h.recheckExamples()) {
                b.append("  ").append(e).append('\n');
            }
            b.append("\nregion start  pitches of fingers 1 to 5\n");
            double[] starts = h.regionStarts();
            List<double[]> pitches = h.regionPitches();
            for (int r = 0; r < starts.length; r++) {
                double[] p = pitches.get(r);
                b.append(String.format(Locale.ROOT, "%12s ", num(starts[r])));
                for (double x : p) {
                    b.append(String.format(Locale.ROOT, " %6s", num(x)));
                }
                b.append('\n');
            }
            return b.toString();
        }
    }

    private static String num(double x) {
        if (Math.abs(x - Math.rint(x)) < 1e-9) {
            return String.format(Locale.ROOT, "%d", (long) Math.rint(x));
        }
        String s = String.format(Locale.ROOT, "%.4g", x);
        if (s.indexOf('e') < 0 && s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    /**
     * Reads a fingering: one finger (1 to 5) per line in onset order, notes with the same
     * onset by ascending pitch; text after {@code #} and blank lines are ignored.
     */
    public static int[] parseFingering(String text) {
        List<Integer> f = new ArrayList<Integer>();
        String[] lines = (text == null ? "" : text).split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int x;
            try {
                x = Integer.parseInt(line);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("line " + (i + 1) + " of the fingering is not a finger: " + line);
            }
            if (x < 1 || x > 5) {
                throw new IllegalArgumentException("line " + (i + 1) + " of the fingering: finger " + x
                                                   + " is not between 1 and 5");
            }
            f.add(x);
        }
        if (f.isEmpty()) {
            throw new IllegalArgumentException("The fingering has no fingers; give one finger (1 to 5) per note.");
        }
        int[] out = new int[f.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = f.get(i);
        }
        return out;
    }

    /** The hand gesture of a zero-addressed Score with a fingering in the text form of {@link #parseFingering}. */
    public static Report analyze(Denotator score, String fingering, HandGesture.Settings settings) {
        if (score == null) {
            throw new IllegalArgumentException("Input denotator is null.");
        }
        List<ScoreNote> notes = ScoreNote.fromScore(score);
        if (notes.isEmpty()) {
            throw new IllegalArgumentException("The score has no notes.");
        }
        int[] f = parseFingering(fingering);
        double[][] rows = new double[notes.size()][];
        for (int i = 0; i < rows.length; i++) {
            ScoreNote n = notes.get(i);
            rows[i] = new double[] { n.onset, n.duration, n.pitch.doubleValue() };
        }
        if (f.length != rows.length) {
            throw new IllegalArgumentException("The fingering has " + f.length + " fingers for " + rows.length
                                               + " notes.");
        }
        // the fingering lists notes by onset, then pitch: sort the rows the same way first
        Integer[] order = new Integer[rows.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> {
            int c = Double.compare(rows[a][0], rows[b][0]);
            return c != 0 ? c : Double.compare(rows[a][2], rows[b][2]);
        });
        double[][] sorted = new double[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            sorted[i] = rows[order[i]];
        }
        return new Report(HandGesture.of(sorted, f, settings));
    }
}
