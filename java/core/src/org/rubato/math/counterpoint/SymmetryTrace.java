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

package org.rubato.math.counterpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rubato.math.arith.NumberTheory;

/**
 * The contrapuntal symmetries that admit each step of a sequence of
 * counterpoint intervals, as tabulated by Agust&iacute;n-Aquino and
 * Curiel-L&oacute;pez (arXiv 2512.18862, &sect;5.1, pp. 8&ndash;11).
 * <p>
 * The paper does not define the set it lists against the step
 * &xi;<sub>i</sub> &#8594; &xi;<sub>i+1</sub>. This class uses the reading
 * that reproduces all 41 rows of its six tables: translate the step by
 * T<sup>&minus;x<sub>i</sub></sup> (the reduction of CCW Thm. 2.2, p. 19), take
 * the contrapuntal symmetries g of &epsilon;.k<sub>i</sub> in H that
 * {@link ContrapuntalSymmetries} finds, and keep those whose own intersection
 * gX[&epsilon;] &cap; X[&epsilon;] contains the translated successor
 * (x<sub>i+1</sub> &minus; x<sub>i</sub>) + &epsilon;.k<sub>i+1</sub>. CCW
 * Def. 2.6 (p. 15) takes the union of the same intersections, so a step has a
 * non-empty set exactly when it is admitted.
 */
public final class SymmetryTrace {

    private SymmetryTrace() { /* static utility */ }

    /** One step &xi;<sub>i</sub> &#8594; &xi;<sub>i+1</sub> and the symmetries that admit it. */
    public static final class Step {

        private final int index;
        private final int[] from;
        private final int[] to;
        private final boolean consonantPair;
        private final List<DualAffine> symmetries;

        Step(int index, int[] from, int[] to, boolean consonantPair, List<DualAffine> symmetries) {
            this.index = index;
            this.from = from.clone();
            this.to = to.clone();
            this.consonantPair = consonantPair;
            this.symmetries = Collections.unmodifiableList(symmetries);
        }

        /** The position i of &xi;<sub>i</sub>, counted from 0. */
        public int getIndex() { return index; }

        /** &xi;<sub>i</sub> as {cantus firmus, interval}. */
        public int[] getFrom() { return from.clone(); }

        /** &xi;<sub>i+1</sub> as {cantus firmus, interval}. */
        public int[] getTo() { return to.clone(); }

        /** True if both intervals are consonances. */
        public boolean isConsonantPair() { return consonantPair; }

        /** The symmetries that admit the step, in the order the search finds them. */
        public List<DualAffine> getSymmetries() { return symmetries; }

        /** The number of symmetries; 0 if the step is not admitted. */
        public int cardinality() { return symmetries.size(); }
    }

    /**
     * The steps of a sequence of intervals {x, y}.
     *
     * @throws IllegalArgumentException if the dichotomy is not strong or fewer than two intervals are given
     */
    public static List<Step> trace(Dichotomy d, int[][] intervals) {
        d.polarity();
        if (intervals.length < 2) {
            throw new IllegalArgumentException("A trace needs at least two intervals, got " + intervals.length);
        }
        int n = d.getModulus();
        List<Step> out = new ArrayList<Step>();
        for (int i = 0; i + 1 < intervals.length; i++) {
            int[] a = { NumberTheory.mod(intervals[i][0], n), NumberTheory.mod(intervals[i][1], n) };
            int[] b = { NumberTheory.mod(intervals[i + 1][0], n), NumberTheory.mod(intervals[i + 1][1], n) };
            boolean consonant = d.isConsonant(a[1]) && d.isConsonant(b[1]);
            out.add(new Step(i, a, b, consonant, consonant ? stepSymmetries(d, a[0], a[1], b[0], b[1])
                                                            : new ArrayList<DualAffine>()));
        }
        return out;
    }

    /** The contrapuntal symmetries of x1 + &epsilon;.k1 that admit x2 + &epsilon;.k2. */
    public static List<DualAffine> stepSymmetries(Dichotomy d, int x1, int k1, int x2, int k2) {
        int n = d.getModulus();
        List<DualAffine> out = new ArrayList<DualAffine>();
        if (!d.isConsonant(NumberTheory.mod(k1, n)) || !d.isConsonant(NumberTheory.mod(k2, n))) {
            return out;
        }
        int dx = NumberTheory.mod(x2 - x1, n);
        for (DualAffine g : ContrapuntalSymmetries.analyse(d, 0, k1).getSymmetries()) {
            if (inImageOfConsonances(g, d, dx, k2)) {
                out.add(g);
            }
        }
        return out;
    }

    /** True if x + &epsilon;.y lies in gX[&epsilon;], the image of the consonances under g. */
    public static boolean inImageOfConsonances(DualAffine g, Dichotomy d, int x, int y) {
        int n = g.getModulus();
        int uInv = 1;
        while (NumberTheory.mod(uInv * g.getU(), n) != 1) {
            uInv++;
        }
        int x0 = NumberTheory.mod(uInv * (x - g.getA()), n);
        int y0 = NumberTheory.mod(uInv * (y - g.getB() - g.getV() * x0), n);
        return d.isConsonant(y0);
    }

    /** True if g leaves the cantus firmus coordinate unchanged up to translation: linear part u = 1. */
    public static boolean fixesCantus(DualAffine g) {
        return g.getU() == 1;
    }

    /** The paper's notation, for example "e^{&epsilon;.8}&#8728;(5+&epsilon;.4)". */
    public static String name(DualAffine g) {
        String lin = g.getV() == 0 ? Integer.toString(g.getU()) : "(" + g.getU() + "+\u03b5." + g.getV() + ")";
        String shift = g.getA() == 0 ? "\u03b5." + g.getB() : g.getA() + "+\u03b5." + g.getB();
        return "e^{" + shift + "}\u2218" + lin;
    }

    /**
     * Parses intervals such as "0+&epsilon;.7, 10+e.4; e.3". Terms are separated
     * by commas or semicolons; "e" may stand for &epsilon;; a missing cantus is 0.
     *
     * @throws IllegalArgumentException if a term has no &epsilon; part or a number does not parse
     */
    public static int[][] parse(String text) {
        List<int[]> out = new ArrayList<int[]>();
        for (String raw : text.split("[,;]")) {
            String t = raw.replace(" ", "").replace('\u03b5', 'e').replace('\u03f5', 'e');
            if (t.isEmpty()) {
                continue;
            }
            int e = t.indexOf("e.");
            if (e < 0) {
                throw new IllegalArgumentException("\"" + raw.trim() + "\" is not of the form x+\u03b5.y");
            }
            String xs = t.substring(0, e);
            if (xs.endsWith("+")) {
                xs = xs.substring(0, xs.length() - 1);
            }
            try {
                int x = xs.isEmpty() ? 0 : Integer.parseInt(xs);
                int y = Integer.parseInt(t.substring(e + 2));
                out.add(new int[] { x, y });
            }
            catch (NumberFormatException ex) {
                throw new IllegalArgumentException("\"" + raw.trim() + "\" is not of the form x+\u03b5.y");
            }
        }
        return out.toArray(new int[0][]);
    }

    /** A plain-text table: one line per step with its symmetries and their number. */
    public static String report(List<Step> steps) {
        StringBuilder b = new StringBuilder();
        for (Step s : steps) {
            b.append(String.format("%3d  %2d+\u03b5.%-2d -> %2d+\u03b5.%-2d  ", s.index + 1,
                    s.from[0], s.from[1], s.to[0], s.to[1]));
            if (!s.consonantPair) {
                b.append("dissonant interval, not checked\n");
                continue;
            }
            if (s.symmetries.isEmpty()) {
                b.append("not admitted (0)\n");
                continue;
            }
            List<String> names = new ArrayList<String>();
            for (DualAffine g : s.symmetries) {
                names.add(name(g) + (fixesCantus(g) ? " [fixes cantus]" : ""));
            }
            b.append("{").append(String.join(", ", names)).append("}  (").append(s.symmetries.size()).append(")\n");
        }
        return b.toString();
    }
}
