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

import java.util.List;
import java.util.Locale;

import org.rubato.io.ScoreNote;
import org.rubato.math.gesture.FingerGesture;
import org.rubato.math.gesture.Gesture;
import org.rubato.math.yoneda.Denotator;

/**
 * Headless access to the finger gesture of a Score (Mazzola, The Topos of
 * Music, vol. III, Example 59, p. 915), built by
 * {@link org.rubato.math.gesture.FingerGesture}.
 */
public final class FingerGestures {

    private FingerGestures() { /* static utility */ }

    /** The gesture with the text the rubette shows. */
    public static final class Report {
        private final FingerGesture gesture;
        private final FingerGesture.Settings settings;

        Report(FingerGesture gesture, FingerGesture.Settings settings) {
            this.gesture = gesture;
            this.settings = settings;
        }

        public FingerGesture getGesture() {
            return gesture;
        }

        public String text() {
            Gesture g = gesture.gesture();
            StringBuilder b = new StringBuilder();
            b.append("Finger gesture of ").append(gesture.noteCount()).append(" notes")
             .append(" (Topos of Music III, Example 59, p. 915)\n");
            b.append(String.format(Locale.ROOT, "lift %.3g, approach %.3g, release %.3g%n",
                                   settings.lift, settings.approach, settings.release));
            b.append("skeleton: up-arrow with ").append(g.skeleton().vertexCount()).append(" vertices, ")
             .append(g.skeleton().arrowCount()).append(" arrows\n");
            b.append(gesture.backwardMoves()).append(" backward moves")
             .append(" (the next note starts before the finger is free)\n");
            b.append(String.format(Locale.ROOT, "path length in (height, time, pitch): %.6g%n", gesture.pathLength()));
            b.append("\nvertex  height      time     pitch\n");
            for (int v = 0; v < g.skeleton().vertexCount(); v++) {
                double[] p = g.vertex(v);
                b.append(String.format(Locale.ROOT, "%6d %7.3f %9.4f %9.4f%n", v, p[0], p[1], p[2]));
            }
            return b.toString();
        }
    }

    /** The finger gesture of a zero-addressed Score. */
    public static Report analyze(Denotator score, FingerGesture.Settings settings) {
        if (score == null) {
            throw new IllegalArgumentException("Input denotator is null.");
        }
        List<ScoreNote> notes = ScoreNote.fromScore(score);
        if (notes.isEmpty()) {
            throw new IllegalArgumentException("The score has no notes.");
        }
        double[][] rows = new double[notes.size()][];
        for (int i = 0; i < rows.length; i++) {
            ScoreNote n = notes.get(i);
            rows[i] = new double[] { n.onset, n.duration, n.pitch.doubleValue() };
        }
        return new Report(FingerGesture.of(rows, settings), settings);
    }
}
