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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * The gesture of a pianist's hand after Example 60 (Mazzola, The Topos of
 * Music, vol. III, p. 915) and section 61.4: five fingertip curves and the
 * carpus in R^4 with coordinates (pitch, height above the key, front
 * position from the key face line, time). The curve parameter is not the
 * time.
 * <p>
 * Construction, following Mazzola and Mueller, "Constraint-based Shaping
 * of Gestural Performance", ICMC 2003 (reference [772] of the book):
 * <ol>
 * <li>The symbolic gesture of each finger chains Example 59's down, hold
 * and up arrows of its notes, joined by a wait arrow (lifted, time
 * advancing) and a shift arrow (lifted, time constant).</li>
 * <li>Geometric shaping walks through the regions between consecutive
 * onsets and ends. Held fingers are checked pairwise; each free finger,
 * in index order, keeps its position when it satisfies every constraint
 * with the fingers already fixed in the region and otherwise moves to the
 * nearest feasible pitch (a reshape). The constraints are a maximum pitch
 * distance per pair and a pitch order of the non-thumb fingers (settings,
 * departures D2 and D3 of HAND-MODEL-SPEC).</li>
 * <li>Physical shaping turns each instantaneous move into a rest-to-rest
 * straight move with the velocity profile of section 78.2.5 (ramp up,
 * ramp down), lasting sqrt(6 d / A) for distance d under the acceleration
 * limit A = K/m of section 61.4.1. Moves are anticipated to end at their
 * instants; a release that has to come earlier shortens the note, and a
 * shortening beyond {@code maxShortening} of the duration makes the score
 * not performable.</li>
 * </ol>
 * Fingers are numbered 1 (thumb) to 5 (little finger).
 */
public final class HandGesture {

    /** Which hand: the right hand's non-thumb fingers ascend in pitch with their number, the left hand's descend. */
    public enum Hand { RIGHT, LEFT }

    /** Hand geometry, units and limits; every default is a setting, not a value from the book. */
    public static final class Settings {
        public final Hand hand;
        /** Maximum pitch distance in semitones, indexed [i - 1][j - 1], symmetric. */
        private final double[][] span;
        /** Minimum pitch gap per finger-number step between non-thumb fingers, in semitones. */
        public final double orderGap;
        /** Height of a lifted fingertip above the pressed key, in cm. */
        public final double lift;
        /** Width of one semitone on the keyboard, in cm. */
        public final double semitone;
        /** Front position of black keys from the key face line, in cm (white keys at 0). */
        public final double blackFront;
        public final double carpusHeight;
        public final double carpusFront;
        /** Acceleration limit K_i / m_i per fingertip, in cm/s^2. */
        public final double[] accel;
        /** Physical seconds per score time unit. */
        public final double secondsPerUnit;
        /** Largest allowed shortening, as a fraction of the note's duration. */
        public final double maxShortening;

        private Settings(Hand hand, double[][] span, double orderGap, double lift, double semitone, double blackFront,
                         double carpusHeight, double carpusFront, double[] accel, double secondsPerUnit,
                         double maxShortening) {
            if (hand == null) {
                throw new IllegalArgumentException("no hand");
            }
            positive("lift", lift);
            positive("semitone width", semitone);
            positive("seconds per unit", secondsPerUnit);
            finite("black key front", blackFront);
            finite("carpus height", carpusHeight);
            finite("carpus front", carpusFront);
            if (!(orderGap >= 0) || Double.isInfinite(orderGap)) {
                throw new IllegalArgumentException("need a finite order gap >= 0");
            }
            if (!(maxShortening >= 0 && maxShortening < 1)) {
                throw new IllegalArgumentException("need 0 <= maximum shortening < 1");
            }
            for (double a : accel) {
                positive("acceleration limit", a);
            }
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    if (i != j && !(span[i][j] >= 0)) {
                        throw new IllegalArgumentException("need spans >= 0");
                    }
                }
            }
            this.hand = hand;
            this.span = span;
            this.orderGap = orderGap;
            this.lift = lift;
            this.semitone = semitone;
            this.blackFront = blackFront;
            this.carpusHeight = carpusHeight;
            this.carpusFront = carpusFront;
            this.accel = accel;
            this.secondsPerUnit = secondsPerUnit;
            this.maxShortening = maxShortening;
        }

        private static void positive(String what, double x) {
            if (!(x > 0) || Double.isInfinite(x)) {
                throw new IllegalArgumentException("need a finite " + what + " > 0");
            }
        }

        private static void finite(String what, double x) {
            if (Double.isNaN(x) || Double.isInfinite(x)) {
                throw new IllegalArgumentException(what + " is not finite");
            }
        }

        /**
         * Right hand; spans (semitones) 1-2 8, 1-3 10, 1-4 11, 1-5 12, 2-3 4, 2-4 6, 2-5 8, 3-4 3,
         * 3-5 5, 4-5 4; order gap 1; lift 1 cm; semitone 1.375 cm; black keys 4 cm deep; carpus
         * 5 cm high and 6 cm in front of the key face line; acceleration limit 5000 cm/s^2;
         * 0.5 s per time unit; maximum shortening 0.5.
         */
        public static Settings defaults() {
            double[][] s = new double[5][5];
            double[] v = { 8, 10, 11, 12, 4, 6, 8, 3, 5, 4 };
            int k = 0;
            for (int i = 0; i < 5; i++) {
                for (int j = i + 1; j < 5; j++) {
                    s[i][j] = v[k];
                    s[j][i] = v[k];
                    k++;
                }
            }
            return new Settings(Hand.RIGHT, s, 1, 1, 1.375, 4, 5, -6, new double[] { 5000, 5000, 5000, 5000, 5000 },
                                0.5, 0.5);
        }

        private Settings copy(Hand h, double[][] sp, double gap, double l, double[] a, double spu, double ms) {
            return new Settings(h, sp, gap, l, semitone, blackFront, carpusHeight, carpusFront, a, spu, ms);
        }

        public Settings withHand(Hand h) {
            return copy(h, span, orderGap, lift, accel, secondsPerUnit, maxShortening);
        }

        /** A copy with the maximum pitch distance of fingers i and j set to s semitones. */
        public Settings withSpan(int i, int j, double s) {
            checkFinger(i);
            checkFinger(j);
            if (i == j) {
                throw new IllegalArgumentException("a span joins two different fingers");
            }
            double[][] sp = new double[5][];
            for (int k = 0; k < 5; k++) {
                sp[k] = span[k].clone();
            }
            sp[i - 1][j - 1] = s;
            sp[j - 1][i - 1] = s;
            return copy(hand, sp, orderGap, lift, accel, secondsPerUnit, maxShortening);
        }

        public Settings withOrderGap(double g) {
            return copy(hand, span, g, lift, accel, secondsPerUnit, maxShortening);
        }

        public Settings withLift(double l) {
            return copy(hand, span, orderGap, l, accel, secondsPerUnit, maxShortening);
        }

        /** A copy with the same acceleration limit for every fingertip. */
        public Settings withAccel(double a) {
            return copy(hand, span, orderGap, lift, new double[] { a, a, a, a, a }, secondsPerUnit, maxShortening);
        }

        public Settings withSecondsPerUnit(double spu) {
            return copy(hand, span, orderGap, lift, accel, spu, maxShortening);
        }

        public Settings withMaxShortening(double ms) {
            return copy(hand, span, orderGap, lift, accel, secondsPerUnit, ms);
        }

        public double span(int i, int j) {
            return span[i - 1][j - 1];
        }

        /** The lowest (or, if negative, highest) pitch difference x_j - x_i the order requires; NaN without order. */
        double minOrderedDifference(int i, int j) {
            if (i == 1 || j == 1) {
                return Double.NaN;
            }
            return (hand == Hand.RIGHT ? j - i : i - j) * orderGap;
        }

        /** Whether finger i at pitch xi and finger j at pitch xj satisfy both constraints. */
        public boolean allows(int i, double xi, int j, double xj) {
            return violation(i, xi, j, xj) == null;
        }

        /** "span" or "order" for a failed pair, null if the pair is allowed. */
        String violation(int i, double xi, int j, double xj) {
            if (Math.abs(xi - xj) > span(i, j) + EPS) {
                return "span";
            }
            double m = minOrderedDifference(i, j);
            if (!Double.isNaN(m)) {
                double d = xj - xi;
                if (m > 0 ? d < m - EPS : d > m + EPS) {
                    return "order";
                }
            }
            return null;
        }

        /** Front position of the key at a pitch (pitch classes 1, 3, 6, 8, 10 are black). */
        public double front(double pitch) {
            int pc = Math.floorMod((int) Math.round(pitch), 12);
            return pc == 1 || pc == 3 || pc == 6 || pc == 8 || pc == 10 ? blackFront : 0;
        }
    }

    /** A free finger moved at a region start because the constraints required it. */
    public static final class Reshape {
        public final int finger;
        public final double time;
        public final double from;
        public final double to;
        public final String reason;

        Reshape(int finger, double time, double from, double to, String reason) {
            this.finger = finger;
            this.time = time;
            this.from = from;
            this.to = to;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "E = %s: finger %d moved from %s to %s (%s)", num(time), finger,
                                 num(from), num(to), reason);
        }
    }

    /** A note whose end was moved earlier so that the finger can reach its next key. */
    public static final class Shortening {
        /** Index in onset order. */
        public final int noteIndex;
        public final int finger;
        public final double originalEnd;
        public final double newEnd;

        Shortening(int noteIndex, int finger, double originalEnd, double newEnd) {
            this.noteIndex = noteIndex;
            this.finger = finger;
            this.originalEnd = originalEnd;
            this.newEnd = newEnd;
        }
    }

    /** A rest-to-rest straight move of a fingertip; points are (pitch, height, front), times in score units. */
    public static final class Move {
        public final int finger;
        public final double start;
        public final double end;
        public final double[] from;
        public final double[] to;

        Move(int finger, double start, double end, double[] from, double[] to) {
            this.finger = finger;
            this.start = start;
            this.end = end;
            this.from = from;
            this.to = to;
        }

        double[] at(double t) {
            double s = end > start ? (t - start) / (end - start) : 1;
            double f = fraction(Math.max(0, Math.min(1, s)));
            return new double[] { from[0] + f * (to[0] - from[0]), from[1] + f * (to[1] - from[1]),
                                  from[2] + f * (to[2] - from[2]) };
        }
    }

    static final double EPS = 1e-9;

    private final Settings settings;
    private final double[][] notes;
    private final int[] fingers;
    private final double[] breaks;
    private final List<double[]> regionPitches = new ArrayList<double[]>();
    private final List<Reshape> reshapes = new ArrayList<Reshape>();
    private final List<Shortening> shortenings = new ArrayList<Shortening>();
    private final List<String> problems = new ArrayList<String>();
    @SuppressWarnings("unchecked")
    private final List<Move>[] moves = new List[5];
    private final double[][] initial = new double[5][];
    private final double[] peak = new double[5];
    private final Gesture[] symbolic = new Gesture[5];
    private final Gesture[] physical = new Gesture[5];
    private Gesture symbolicCarpus;
    private Gesture physicalCarpus;
    private int recheckViolations;
    private final List<String> recheckExamples = new ArrayList<String>();

    private HandGesture(double[][] notes, int[] fingers, Settings settings) {
        this.notes = notes;
        this.fingers = fingers;
        this.settings = settings;
        TreeSet<Double> b = new TreeSet<Double>();
        for (double[] n : notes) {
            b.add(n[0]);
            b.add(n[0] + n[1]);
        }
        breaks = new double[b.size()];
        int k = 0;
        for (double x : b) {
            breaks[k++] = x;
        }
    }

    /**
     * The hand gesture of notes given as rows {onset, duration, pitch}, with fingering[i] the
     * finger (1 to 5) of notes[i].
     *
     * @throws IllegalArgumentException if there are no notes, the fingering does not match,
     *         a finger is outside 1 to 5, a value is not finite, a duration is negative, or one
     *         finger would hold two notes that overlap
     */
    public static HandGesture of(double[][] notes, int[] fingering, Settings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("no settings");
        }
        if (notes.length == 0) {
            throw new IllegalArgumentException("no notes");
        }
        if (fingering == null || fingering.length != notes.length) {
            throw new IllegalArgumentException("the fingering has " + (fingering == null ? 0 : fingering.length)
                                               + " fingers for " + notes.length + " notes");
        }
        Integer[] order = new Integer[notes.length];
        for (int i = 0; i < notes.length; i++) {
            double[] n = notes[i];
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
            checkFinger(fingering[i]);
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> {
            int c = Double.compare(notes[a][0], notes[b][0]);
            return c != 0 ? c : Double.compare(notes[a][2], notes[b][2]);
        });
        double[][] sorted = new double[notes.length][];
        int[] f = new int[notes.length];
        for (int i = 0; i < notes.length; i++) {
            sorted[i] = notes[order[i]].clone();
            f[i] = fingering[order[i]];
        }
        double[] lastEnd = new double[6];
        Arrays.fill(lastEnd, Double.NEGATIVE_INFINITY);
        for (int i = 0; i < sorted.length; i++) {
            if (sorted[i][0] < lastEnd[f[i]] - EPS) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "finger %d would hold notes that overlap at E = %s", f[i], num(sorted[i][0])));
            }
            lastEnd[f[i]] = sorted[i][0] + sorted[i][1];
        }
        HandGesture h = new HandGesture(sorted, f, settings);
        h.shapeGeometrically();
        h.buildSymbolic();
        h.shapePhysically();
        h.buildPhysical();
        h.recheck();
        return h;
    }

    static void checkFinger(int finger) {
        if (finger < 1 || finger > 5) {
            throw new IllegalArgumentException("finger " + finger + " is not between 1 and 5");
        }
    }

    // --- geometric shaping ---

    private boolean held(int note, int region) {
        double a = breaks[region];
        return notes[note][0] <= a + EPS && notes[note][0] + notes[note][1] > a + EPS;
    }

    private double firstPitchOrNaN(int finger) {
        for (int i = 0; i < notes.length; i++) {
            if (fingers[i] == finger) {
                return notes[i][2];
            }
        }
        return Double.NaN;
    }

    private void shapeGeometrically() {
        double[] pos = new double[5];
        boolean[] placed = new boolean[5];
        for (int f = 1; f <= 5; f++) {
            double p = firstPitchOrNaN(f);
            placed[f - 1] = !Double.isNaN(p);
            pos[f - 1] = placed[f - 1] ? p : notes[0][2];
        }
        for (int r = 0; r + 1 < breaks.length; r++) {
            double time = breaks[r];
            boolean[] fixed = new boolean[5];
            for (int i = 0; i < notes.length; i++) {
                if (held(i, r)) {
                    pos[fingers[i] - 1] = notes[i][2];
                    fixed[fingers[i] - 1] = true;
                }
            }
            for (int i = 1; i <= 5; i++) {
                for (int j = i + 1; j <= 5; j++) {
                    if (fixed[i - 1] && fixed[j - 1]) {
                        String v = settings.violation(i, pos[i - 1], j, pos[j - 1]);
                        if (v != null) {
                            problems.add(String.format(Locale.ROOT, "E = %s: fingers %d and %d cannot hold %s and %s (%s)",
                                                       num(time), i, j, num(pos[i - 1]), num(pos[j - 1]),
                                                       v.equals("span") ? "span " + num(settings.span(i, j)) : "order"));
                        }
                    }
                }
            }
            boolean[] done = fixed.clone();
            for (int j = 1; j <= 5; j++) {
                if (fixed[j - 1]) {
                    continue;
                }
                double lo = Double.NEGATIVE_INFINITY, hi = Double.POSITIVE_INFINITY;
                String loWhy = null, hiWhy = null;
                for (int i = 1; i <= 5; i++) {
                    if (i == j || !done[i - 1]) {
                        continue;
                    }
                    double x = pos[i - 1], s = settings.span(i, j);
                    if (x - s > lo) {
                        lo = x - s;
                        loWhy = "span with finger " + i;
                    }
                    if (x + s < hi) {
                        hi = x + s;
                        hiWhy = "span with finger " + i;
                    }
                    double m = settings.minOrderedDifference(i, j);
                    if (!Double.isNaN(m)) {
                        if (m > 0 && x + m > lo) {
                            lo = x + m;
                            loWhy = "order with finger " + i;
                        }
                        if (m < 0 && x + m < hi) {
                            hi = x + m;
                            hiWhy = "order with finger " + i;
                        }
                    }
                }
                done[j - 1] = true;
                if (lo > hi + EPS) {
                    problems.add(String.format(Locale.ROOT, "E = %s: no position for finger %d (needs %s to %s)",
                                               num(time), j, num(lo), num(hi)));
                    continue;
                }
                double old = pos[j - 1];
                double now = old < lo ? lo : old > hi ? hi : old;
                if (Math.abs(now - old) > EPS) {
                    pos[j - 1] = now;
                    if (placed[j - 1]) {
                        reshapes.add(new Reshape(j, time, old, now, old < lo ? loWhy : hiWhy));
                    }
                }
                placed[j - 1] = true;
            }
            if (r == 0) {
                setInitial(pos);
            }
            regionPitches.add(pos.clone());
        }
        if (breaks.length < 2) {
            setInitial(pos);
        }
    }

    /**
     * Every finger starts where the first region's shaping placed it, so the starting hand
     * satisfies the constraints: a finger that is free in the first region and was reshaped
     * there starts at its reshaped place, so that reshape costs no move.
     */
    private void setInitial(double[] pos) {
        for (int f = 1; f <= 5; f++) {
            double p = pos[f - 1];
            initial[f - 1] = new double[] { p, settings.lift, settings.front(p) };
        }
    }

    // --- symbolic gesture ---

    private double[] point(double pitch, double height, double time) {
        return new double[] { pitch, height, settings.front(pitch), time };
    }

    private void buildSymbolic() {
        double lift = settings.lift;
        for (int f = 1; f <= 5; f++) {
            List<double[]> pts = new ArrayList<double[]>();
            for (int i = 0; i < notes.length; i++) {
                if (fingers[i] != f) {
                    continue;
                }
                double on = notes[i][0], end = on + notes[i][1], p = notes[i][2];
                if (!pts.isEmpty()) {
                    double[] last = pts.get(pts.size() - 1);
                    pts.add(point(last[0], lift, on));
                }
                pts.add(point(p, lift, on));
                pts.add(point(p, 0, on));
                pts.add(point(p, 0, end));
                pts.add(point(p, lift, end));
            }
            if (pts.isEmpty()) {
                double[] q = initial[f - 1];
                symbolic[f - 1] = new Gesture(Digraph.point(), new double[][] { { q[0], q[1], q[2], notes[0][0] } },
                                              new Curve[0]);
            }
            else {
                symbolic[f - 1] = chain(pts);
            }
        }
        // carpus: mean of the symbolic fingertip pitches, which change only at the breakpoints
        List<double[]> pts = new ArrayList<double[]>();
        for (int k = 0; k < breaks.length; k++) {
            if (k > 0) {
                double[] last = pts.get(pts.size() - 1);
                pts.add(new double[] { last[0], settings.carpusHeight, settings.carpusFront, breaks[k] });
            }
            pts.add(new double[] { symbolicMean(breaks[k]), settings.carpusHeight, settings.carpusFront, breaks[k] });
        }
        symbolicCarpus = pts.size() == 1
            ? new Gesture(Digraph.point(), new double[][] { pts.get(0) }, new Curve[0]) : chain(pts);
    }

    /** The symbolic pitch of a finger just after time t: its current or last key, or its first key before it plays. */
    private double symbolicPitch(int finger, double t) {
        double p = Double.NaN;
        for (int i = 0; i < notes.length; i++) {
            if (fingers[i] != finger) {
                continue;
            }
            if (Double.isNaN(p) || notes[i][0] <= t + EPS) {
                p = notes[i][2];
            }
        }
        return Double.isNaN(p) ? initial[finger - 1][0] : p;
    }

    private double symbolicMean(double t) {
        double s = 0;
        for (int f = 1; f <= 5; f++) {
            s += symbolicPitch(f, t);
        }
        return s / 5;
    }

    private static Gesture chain(List<double[]> pts) {
        int n = pts.size() - 1;
        double[][] v = pts.toArray(new double[0][]);
        Curve[] c = new Curve[n];
        for (int a = 0; a < n; a++) {
            c[a] = PolylineCurve.line(v[a], v[a + 1]);
        }
        return new Gesture(Digraph.arrowN(n), v, c);
    }

    // --- physical shaping ---

    /** Minimal duration in seconds of a rest-to-rest move over d under acceleration limit a: sqrt(6 d / a). */
    public static double minimalDuration(double d, double a) {
        return d <= 0 ? 0 : Math.sqrt(6 * d / a);
    }

    /**
     * Position fraction at s in [0, 1] of a rest-to-rest move whose velocity ramps up and then
     * down with the cubic of section 78.2.5, (t - t0)^2 (2t + t0 - 3t1) W / (t0 - t1)^3: for
     * s <= 1/2 it is u^3 - u^4 / 2 with u = 2s, mirrored after.
     */
    public static double fraction(double s) {
        if (s <= 0.5) {
            double u = 2 * s;
            return u * u * u - u * u * u * u / 2;
        }
        double u = 2 - 2 * s;
        return 1 - (u * u * u - u * u * u * u / 2);
    }

    private double distanceCm(double[] p, double[] q) {
        double dx = (q[0] - p[0]) * settings.semitone, dy = q[1] - p[1], dz = q[2] - p[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** A move before scheduling. */
    private static final class Plan {
        static final int UP = 0, LATERAL = 1, DOWN = 2;
        final int kind;
        final double instant;
        final double[] from;
        final double[] to;
        final int note;

        Plan(int kind, double instant, double[] from, double[] to, int note) {
            this.kind = kind;
            this.instant = instant;
            this.from = from;
            this.to = to;
            this.note = note;
        }
    }

    private void shapePhysically() {
        double lift = settings.lift;
        for (int f = 1; f <= 5; f++) {
            List<Reshape> mine = new ArrayList<Reshape>();
            for (Reshape r : reshapes) {
                if (r.finger == f) {
                    mine.add(r);
                }
            }
            List<Plan> plans = new ArrayList<Plan>();
            double[] cur = initial[f - 1];
            int next = 0;
            for (int i = 0; i <= notes.length; i++) {
                if (i < notes.length && fingers[i] != f) {
                    continue;
                }
                double until = i < notes.length ? notes[i][0] : Double.POSITIVE_INFINITY;
                while (next < mine.size() && mine.get(next).time < until - EPS) {
                    Reshape r = mine.get(next++);
                    double[] to = { r.to, lift, settings.front(r.to) };
                    plans.add(new Plan(Plan.LATERAL, r.time, cur, to, -1));
                    cur = to;
                }
                if (i == notes.length) {
                    break;
                }
                double on = notes[i][0], end = on + notes[i][1], p = notes[i][2];
                double[] over = { p, lift, settings.front(p) };
                if (distanceCm(cur, over) > EPS) {
                    plans.add(new Plan(Plan.LATERAL, on, cur, over, -1));
                }
                double[] down = { p, 0, settings.front(p) };
                plans.add(new Plan(Plan.DOWN, on, over, down, i));
                plans.add(new Plan(Plan.UP, end, down, over, i));
                cur = over;
            }
            moves[f - 1] = schedule(f, plans);
        }
    }

    /**
     * Schedules one finger's plans group by group. A group ends with a down move or the last
     * plan and starts after the previous group; moves are anticipated backward from the group's
     * last instant, and a leading up move starts at its note's end or earlier.
     */
    private List<Move> schedule(int finger, List<Plan> plans) {
        List<Move> out = new ArrayList<Move>();
        double a = settings.accel[finger - 1], spu = settings.secondsPerUnit;
        int k = 0;
        while (k < plans.size()) {
            int j = k;
            while (j < plans.size() - 1 && plans.get(j).kind != Plan.DOWN) {
                j++;
            }
            List<Plan> g = plans.subList(k, j + 1);
            int n = g.size();
            double[] start = new double[n], end = new double[n], dur = new double[n];
            for (int m = 0; m < n; m++) {
                dur[m] = minimalDuration(distanceCm(g.get(m).from, g.get(m).to), a) / spu;
            }
            for (int m = n - 1; m >= 0; m--) {
                Plan p = g.get(m);
                if (m == 0 && p.kind == Plan.UP) {
                    start[m] = n > 1 ? Math.min(p.instant, start[1] - dur[m]) : p.instant;
                    end[m] = start[m] + dur[m];
                }
                else {
                    end[m] = m == n - 1 ? p.instant : Math.min(p.instant, start[m + 1]);
                    start[m] = end[m] - dur[m];
                }
            }
            if (g.get(0).kind == Plan.UP) {
                Plan up = g.get(0);
                double origEnd = up.instant, on = notes[up.note][0], d = notes[up.note][1];
                if (start[0] < origEnd - EPS) {
                    double newEnd = start[0];
                    boolean compressed = false;
                    if (newEnd < on - EPS) {
                        // the whole note does not leave enough time: compress the group into [onset, deadline]
                        double total = 0;
                        for (double x : dur) {
                            total += x;
                        }
                        double scale = (end[n - 1] - on) / total, t = on;
                        for (int m = 0; m < n; m++) {
                            start[m] = t;
                            t += dur[m] * scale;
                            end[m] = t;
                        }
                        newEnd = on;
                        compressed = true;
                    }
                    shortenings.add(new Shortening(up.note, finger, origEnd, newEnd));
                    if (compressed || origEnd - newEnd > settings.maxShortening * d + EPS) {
                        problems.add(String.format(Locale.ROOT,
                            "E = %s: finger %d would have to shorten the note at E = %s (pitch %s) by %s of %s",
                            num(origEnd), finger, num(on), num(notes[up.note][2]),
                            compressed ? "more than all" : num(origEnd - newEnd), num(d)));
                    }
                }
            }
            for (int m = 0; m < n; m++) {
                Plan p = g.get(m);
                if (dur[m] <= 0) {
                    continue;
                }
                out.add(new Move(finger, start[m], end[m], p.from, p.to));
                double sec = (end[m] - start[m]) * spu;
                peak[finger - 1] = Math.max(peak[finger - 1], 6 * distanceCm(p.from, p.to) / (sec * sec));
            }
            k = j + 1;
        }
        Collections.sort(out, (x, y) -> Double.compare(x.start, y.start));
        return out;
    }

    // --- physical gesture ---

    private void buildPhysical() {
        double t0 = breaks[0], t1 = breaks[breaks.length - 1];
        for (int f = 1; f <= 5; f++) {
            for (Move m : moves[f - 1]) {
                t0 = Math.min(t0, m.start);
                t1 = Math.max(t1, m.end);
            }
        }
        for (int f = 1; f <= 5; f++) {
            List<double[]> v = new ArrayList<double[]>();
            List<Curve> c = new ArrayList<Curve>();
            double[] q = initial[f - 1];
            v.add(new double[] { q[0], q[1], q[2], t0 });
            for (Move m : moves[f - 1]) {
                double[] last = v.get(v.size() - 1);
                double[] s = { m.from[0], m.from[1], m.from[2], m.start };
                c.add(PolylineCurve.line(last, s));
                v.add(s);
                final Move mm = m;
                c.add(new FunctionCurve(4, x -> {
                    double t = mm.start + x * (mm.end - mm.start);
                    return withTime(mm.at(t), t);
                }));
                v.add(new double[] { m.to[0], m.to[1], m.to[2], m.end });
            }
            double[] last = v.get(v.size() - 1);
            double[] fin = { last[0], last[1], last[2], t1 };
            c.add(PolylineCurve.line(last, fin));
            v.add(fin);
            physical[f - 1] = new Gesture(Digraph.arrowN(c.size()), v.toArray(new double[0][]),
                                          c.toArray(new Curve[0]));
        }
        final double a = t0, b = t1;
        physicalCarpus = new Gesture(Digraph.arrowN(1),
            new double[][] { withTime(carpus(a), a), withTime(carpus(b), b) },
            new Curve[] { new FunctionCurve(4, x -> withTime(carpus(a + x * (b - a)), a + x * (b - a))) });
    }

    private static double[] withTime(double[] p, double t) {
        return new double[] { p[0], p[1], p[2], t };
    }

    // --- re-check of G on the physical curves ---

    private void recheck() {
        TreeSet<Double> ts = new TreeSet<Double>();
        for (int r = 0; r + 1 < breaks.length; r++) {
            ts.add((breaks[r] + breaks[r + 1]) / 2);
        }
        for (int f = 0; f < 5; f++) {
            for (Move m : moves[f]) {
                for (int k = 0; k <= 4; k++) {
                    ts.add(m.start + k * (m.end - m.start) / 4);
                }
            }
        }
        for (double t : ts) {
            String bad = null;
            for (int i = 1; i <= 5 && bad == null; i++) {
                for (int j = i + 1; j <= 5 && bad == null; j++) {
                    String v = settings.violation(i, position(i, t)[0], j, position(j, t)[0]);
                    if (v != null) {
                        bad = String.format(Locale.ROOT, "E = %s: fingers %d and %d (%s)", num(t), i, j, v);
                    }
                }
            }
            if (bad != null) {
                recheckViolations++;
                if (recheckExamples.size() < 12) {
                    recheckExamples.add(bad);
                }
            }
        }
    }

    // --- accessors ---

    public Settings settings() {
        return settings;
    }

    public int noteCount() {
        return notes.length;
    }

    /** The notes in onset order as {onset, duration, pitch}. */
    public double[][] notes() {
        double[][] c = new double[notes.length][];
        for (int i = 0; i < notes.length; i++) {
            c[i] = notes[i].clone();
        }
        return c;
    }

    /** The fingering in onset order. */
    public int[] fingering() {
        return fingers.clone();
    }

    /** Whether no constraint failed: every held pair fits, every free finger found a place and no note shortened too much. */
    public boolean performable() {
        return problems.isEmpty();
    }

    public List<String> problems() {
        return Collections.unmodifiableList(problems);
    }

    public List<Reshape> reshapes() {
        return Collections.unmodifiableList(reshapes);
    }

    public List<Shortening> shortenings() {
        return Collections.unmodifiableList(shortenings);
    }

    public List<Move> moves(int finger) {
        checkFinger(finger);
        return Collections.unmodifiableList(moves[finger - 1]);
    }

    /** The region start times, one per entry of {@link #regionPitches()}. */
    public double[] regionStarts() {
        return Arrays.copyOf(breaks, Math.max(0, breaks.length - 1));
    }

    /** The five fingertip pitches of each region after geometric shaping. */
    public List<double[]> regionPitches() {
        return Collections.unmodifiableList(regionPitches);
    }

    public Gesture symbolic(int finger) {
        checkFinger(finger);
        return symbolic[finger - 1];
    }

    public Gesture physical(int finger) {
        checkFinger(finger);
        return physical[finger - 1];
    }

    public Gesture symbolicCarpus() {
        return symbolicCarpus;
    }

    public Gesture physicalCarpus() {
        return physicalCarpus;
    }

    /** Six curves: five fingertips and the carpus. */
    public int curveCount() {
        return 6;
    }

    /** The largest acceleration of the finger's moves in cm/s^2, 6 d / T^2 per move. */
    public double peakAcceleration(int finger) {
        checkFinger(finger);
        return peak[finger - 1];
    }

    /** The physical fingertip at score time t as (pitch, height, front). */
    public double[] position(int finger, double t) {
        checkFinger(finger);
        double[] p = initial[finger - 1].clone();
        for (Move m : moves[finger - 1]) {
            if (t < m.start) {
                return p;
            }
            if (t <= m.end) {
                return m.at(t);
            }
            p = m.to.clone();
        }
        return p;
    }

    /** The physical fingertip at score time t in cm: (pitch * semitone, height, front). */
    public double[] spaceCm(int finger, double t) {
        double[] p = position(finger, t);
        return new double[] { p[0] * settings.semitone, p[1], p[2] };
    }

    /** The carpus at score time t: mean fingertip pitch, fixed height and front. */
    public double[] carpus(double t) {
        double s = 0;
        for (int f = 1; f <= 5; f++) {
            s += position(f, t)[0];
        }
        return new double[] { s / 5, settings.carpusHeight, settings.carpusFront };
    }

    /** Sample times at which the physical curves break a constraint (region midpoints and move quarters). */
    public int recheckViolations() {
        return recheckViolations;
    }

    /** Up to twelve of the re-check violations. */
    public List<String> recheckExamples() {
        return Collections.unmodifiableList(recheckExamples);
    }

    static String num(double x) {
        if (Math.abs(x - Math.rint(x)) < 1e-9) {
            return String.format(Locale.ROOT, "%d", (long) Math.rint(x));
        }
        String s = String.format(Locale.ROOT, "%.4g", x);
        if (s.indexOf('e') < 0 && s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}
