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
package org.rubato.math.gesture;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The finger gesture of a melody after Example 59 (Mazzola, The Topos of
 * Music, vol. III, p. 915). FingerSpace is R^3 with coordinates height
 * above the key, time and pitch. Each note contributes the digraph Pi of
 * three arrows: down onto the key (ending at the onset), hold for the
 * duration, and up (starting at the note's end). One finger plays all
 * notes in onset order, so a move arrow joins the end of each note's up
 * segment to the start of the next note's down segment, and the skeleton
 * is the up-arrow of length 4n - 1. The book gives no lift height or
 * approach and release times; they are settings here (departure D4).
 * All segments are straight.
 */
public final class FingerGesture {

    /** Lift height and the approach and release times, in the score's time unit. */
    public static final class Settings {
        public final double lift;
        public final double approach;
        public final double release;

        public Settings(double lift, double approach, double release) {
            if (!(lift > 0) || !(approach >= 0) || !(release >= 0)
                || Double.isInfinite(lift) || Double.isInfinite(approach) || Double.isInfinite(release)) {
                throw new IllegalArgumentException("need lift > 0 and finite approach, release >= 0");
            }
            this.lift = lift;
            this.approach = approach;
            this.release = release;
        }

        /** Lift 1, approach and release 1/8 of a time unit. */
        public static Settings defaults() {
            return new Settings(1, 0.125, 0.125);
        }
    }

    private final Gesture gesture;
    private final int noteCount;
    private final int backwardMoves;

    private FingerGesture(Gesture gesture, int noteCount, int backwardMoves) {
        this.gesture = gesture;
        this.noteCount = noteCount;
        this.backwardMoves = backwardMoves;
    }

    /**
     * The finger gesture of notes given as rows {onset, duration, pitch}.
     *
     * @throws IllegalArgumentException if there are no notes, a value is
     *         not finite or a duration is negative
     */
    public static FingerGesture of(double[][] notes, Settings settings) {
        if (notes.length == 0) {
            throw new IllegalArgumentException("no notes");
        }
        double[][] sorted = notes.clone();
        for (double[] n : sorted) {
            if (n.length != 3) {
                throw new IllegalArgumentException("a note is {onset, duration, pitch}");
            }
            for (double x : n) {
                if (Double.isNaN(x) || Double.isInfinite(x)) {
                    throw new IllegalArgumentException("note value " + x + " is not finite");
                }
            }
            if (n[1] < 0) {
                throw new IllegalArgumentException("negative duration " + n[1]);
            }
        }
        Arrays.sort(sorted, Comparator.<double[]>comparingDouble(n -> n[0]).thenComparingDouble(n -> n[2]));
        int n = sorted.length;
        double[][] points = new double[4 * n][];
        for (int i = 0; i < n; i++) {
            double onset = sorted[i][0], end = sorted[i][0] + sorted[i][1], pitch = sorted[i][2];
            points[4 * i] = new double[] { settings.lift, onset - settings.approach, pitch };
            points[4 * i + 1] = new double[] { 0, onset, pitch };
            points[4 * i + 2] = new double[] { 0, end, pitch };
            points[4 * i + 3] = new double[] { settings.lift, end + settings.release, pitch };
        }
        Digraph skeleton = Digraph.arrowN(4 * n - 1);
        Curve[] curves = new Curve[4 * n - 1];
        int backward = 0;
        for (int a = 0; a < curves.length; a++) {
            curves[a] = PolylineCurve.line(points[a], points[a + 1]);
            if (a % 4 == 3 && points[a + 1][1] < points[a][1]) {
                backward++;
            }
        }
        return new FingerGesture(new Gesture(skeleton, points, curves), n, backward);
    }

    public Gesture gesture() {
        return gesture;
    }

    public int noteCount() {
        return noteCount;
    }

    /** The move arrows that run backward in time: the next note starts before the finger is free. */
    public int backwardMoves() {
        return backwardMoves;
    }

    /** The Euclidean length of the whole curve in FingerSpace. */
    public double pathLength() {
        double sum = 0;
        for (int a = 0; a < gesture.skeleton().arrowCount(); a++) {
            sum += Curve.distance(gesture.curve(a).at(0), gesture.curve(a).at(1));
        }
        return sum;
    }
}
