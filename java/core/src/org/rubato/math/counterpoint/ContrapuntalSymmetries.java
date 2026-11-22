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
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rubato.math.arith.NumberTheory;

/**
 * Contrapuntal symmetries and admitted successors of first-species
 * counterpoint intervals, for a strong dichotomy (X/Y) of Z_n.
 * <p>
 * Definitions followed (zero address):
 * <ul>
 * <li>The induced counterpoint dichotomy X[&epsilon;] = Z_n + &epsilon;.X
 * (CCW Example 2.4, p. 11) and the induced polarity
 * p_x[&epsilon;] = T^{x(1&minus;v)+&epsilon;.u} &middot; v for the polarity
 * T^u &middot; v (CCW Prop. 2.2, p. 11; Topos I Prop. 51, p. 523).</li>
 * <li>A contrapuntal symmetry of &xi; = x + &epsilon;.k &isin; X[&epsilon;] is a
 * symmetry g with &xi; &notin; gX[&epsilon;], p_x[&epsilon;] a polarity of
 * (gX[&epsilon;]/gY[&epsilon;]), and |gX[&epsilon;] &cap; X[&epsilon;]|
 * maximal among such g (CCW Def. 2.5, p. 15; Topos I Def. 95, p. 532).</li>
 * <li>&eta; is an admitted successor of &xi; if &eta; &isin;
 * gX[&epsilon;] &cap; X[&epsilon;] for some contrapuntal symmetry g
 * (Topos I Def. 96, p. 533; CCW Def. 2.6, p. 15).</li>
 * <li>The search runs over the group H = T^{&epsilon;.Z_n} &middot;
 * GL(Z_n[&epsilon;]) with cantus firmus 0 and translates by T^x afterwards,
 * as CCW Thm. 2.2 (p. 19) permits. {@link #bruteForce} searches the whole
 * affine group without that reduction and serves as the test oracle.</li>
 * </ul>
 * <p>
 * <b>Two counts.</b> {@link Result#getMaxIntersection()} is the size of one
 * contrapuntal symmetry's intersection gX[&epsilon;] &cap; X[&epsilon;];
 * {@link Result#getSuccessorCount()} is the size of the union of those
 * intersections over all contrapuntal symmetries, which is the set Def. 96
 * describes. The books' bounds disagree on which count they bound: CCW
 * Thm. 3.1 (p. 35) says every consonance of Z_12 has at least 42 admissible
 * successors, Topos I Thm. 33 (p. 538) says at least 36, and CCW Thm. 2.3
 * (p. 23) gives k&sup2; &le; N &le; 2k&sup2; &minus; k. For Z_12 this code
 * computes maximal intersections from 42 to 60 and unions from 48 to 70; 70
 * exceeds 2k&sup2; &minus; k = 66, so the books' N matches the
 * single-symmetry count. Both counts are reported and neither book's bound
 * is asserted.
 */
public final class ContrapuntalSymmetries {

    private static final Map<String, Result> CACHE = new HashMap<String, Result>();

    private ContrapuntalSymmetries() { /* static utility */ }

    /** The analysis of a counterpoint interval &xi;: its symmetries and successors. */
    public static final class Result {

        private final Dichotomy dichotomy;
        private final int cantus;
        private final int interval;
        private final int candidateCount;
        private final int maxIntersection;
        private final List<DualAffine> symmetries;
        private final BitSet successors;

        Result(Dichotomy d, int cantus, int interval, int candidateCount, int maxIntersection,
               List<DualAffine> symmetries, BitSet successors) {
            this.dichotomy = d;
            this.cantus = cantus;
            this.interval = interval;
            this.candidateCount = candidateCount;
            this.maxIntersection = maxIntersection;
            this.symmetries = Collections.unmodifiableList(symmetries);
            this.successors = successors;
        }

        public Dichotomy getDichotomy() { return dichotomy; }

        /** The cantus firmus x of &xi; = x + &epsilon;.k. */
        public int getCantus() { return cantus; }

        /** The interval k of &xi; = x + &epsilon;.k. */
        public int getInterval() { return interval; }

        /**
         * The number of symmetries that meet the first two conditions of
         * Def. 2.5 (&xi; outside gX[&epsilon;], p_x[&epsilon;] a polarity of
         * the deformed dichotomy), before maximality is applied.
         */
        public int getCandidateCount() { return candidateCount; }

        /** |gX[&epsilon;] &cap; X[&epsilon;]| for any one contrapuntal symmetry g. */
        public int getMaxIntersection() { return maxIntersection; }

        /**
         * The contrapuntal symmetries. From {@link ContrapuntalSymmetries#analyse}
         * these are the symmetries of &epsilon;.k in H (CCW Thm. 2.2); from
         * {@link ContrapuntalSymmetries#bruteForce} they are all symmetries of &xi;.
         */
        public List<DualAffine> getSymmetries() { return symmetries; }

        /** The number of admitted successors, the union over all contrapuntal symmetries. */
        public int getSuccessorCount() { return successors.cardinality(); }

        /** True if x2 + &epsilon;.k2 is an admitted successor of &xi;. */
        public boolean isSuccessor(int x2, int k2) {
            int n = dichotomy.getModulus();
            return successors.get(index(NumberTheory.mod(x2, n), NumberTheory.mod(k2, n), n));
        }

        /** The admitted successors as pairs {cantus, interval}, ordered by cantus, then interval. */
        public List<int[]> getSuccessors() {
            int n = dichotomy.getModulus();
            List<int[]> list = new ArrayList<int[]>();
            for (int i = successors.nextSetBit(0); i >= 0; i = successors.nextSetBit(i + 1)) {
                list.add(new int[] { i / n, i % n });
            }
            return list;
        }

        /**
         * The consonances l for which (x + step) + &epsilon;.l is not an
         * admitted successor, one cell of CCW Table 3.1 (p. 36).
         */
        public int[] forbiddenAfterStep(int step) {
            List<Integer> f = new ArrayList<Integer>();
            for (int l : dichotomy.getConsonances()) {
                if (!isSuccessor(cantus + step, l)) {
                    f.add(l);
                }
            }
            int[] r = new int[f.size()];
            for (int i = 0; i < r.length; i++) {
                r[i] = f.get(i);
            }
            return r;
        }
    }

    /**
     * Analyses &xi; = x + &epsilon;.k by the reduction of CCW Thm. 2.2:
     * the symmetries are those of &epsilon;.k in H, the successors those of
     * &epsilon;.k translated by T^x.
     *
     * @throws IllegalArgumentException if the dichotomy is not strong or k is not a consonance
     */
    public static Result analyse(Dichotomy d, int x, int k) {
        int n = d.getModulus();
        x = NumberTheory.mod(x, n);
        Result atZero = analyseAtZero(d, NumberTheory.mod(k, n));
        if (x == 0) {
            return atZero;
        }
        BitSet moved = new BitSet(n * n);
        for (int i = atZero.successors.nextSetBit(0); i >= 0; i = atZero.successors.nextSetBit(i + 1)) {
            moved.set(index(NumberTheory.mod(i / n + x, n), i % n, n));
        }
        return new Result(d, x, atZero.interval, atZero.candidateCount, atZero.maxIntersection,
                new ArrayList<DualAffine>(atZero.symmetries), moved);
    }

    /** Analyses &epsilon;.k, the interval k over cantus firmus 0. */
    public static Result analyse(Dichotomy d, int k) {
        return analyse(d, 0, k);
    }

    /** True if x2 + &epsilon;.k2 is an admitted successor of x + &epsilon;.k. */
    public static boolean isAdmitted(Dichotomy d, int x, int k, int x2, int k2) {
        return analyse(d, x, k).isSuccessor(x2, k2);
    }

    /**
     * Def. 2.5 and Def. 2.6 applied directly over the whole group
     * GL(Z_n[&epsilon;]) of affine symmetries T^{a+&epsilon;.b} &middot; (u + &epsilon;.v),
     * without the reduction to H. Slow (n&sup3;&middot;&phi;(n) symmetries);
     * meant for tests.
     */
    public static Result bruteForce(Dichotomy d, int x, int k) {
        requireStrongConsonance(d, k);
        int n = d.getModulus();
        x = NumberTheory.mod(x, n);
        k = NumberTheory.mod(k, n);
        List<DualAffine> group = new ArrayList<DualAffine>();
        for (int a = 0; a < n; a++) {
            group.addAll(hElements(n, a));
        }
        return search(d, x, k, group);
    }

    private static synchronized Result analyseAtZero(Dichotomy d, int k) {
        String key = d.toString() + "|" + d.getModulus() + "|" + k;
        Result r = CACHE.get(key);
        if (r == null) {
            requireStrongConsonance(d, k);
            r = search(d, 0, k, hElements(d.getModulus(), 0));
            CACHE.put(key, r);
        }
        return r;
    }

    /** The elements T^{a+&epsilon;.t} &middot; (u + &epsilon;.v) for a fixed a; a = 0 gives H. */
    private static List<DualAffine> hElements(int n, int a) {
        List<DualAffine> h = new ArrayList<DualAffine>();
        for (int t = 0; t < n; t++) {
            for (int u = 1; u < n; u++) {
                if (ZnAffine.isUnit(u, n)) {
                    for (int v = 0; v < n; v++) {
                        h.add(new DualAffine(a, t, u, v, n));
                    }
                }
            }
        }
        return h;
    }

    private static Result search(Dichotomy d, int x, int k, List<DualAffine> group) {
        int n = d.getModulus();
        DualAffine px = inducedPolarity(d, x);
        BitSet xe = induced(d, true);
        BitSet ye = induced(d, false);
        int xi = index(x, k, n);
        int best = -1;
        int candidates = 0;
        List<DualAffine> symmetries = new ArrayList<DualAffine>();
        List<BitSet> intersections = new ArrayList<BitSet>();
        for (DualAffine g : group) {
            BitSet gx = image(g, xe, n);
            if (gx.get(xi)) {
                continue;
            }
            if (!image(px, gx, n).equals(image(g, ye, n))) {
                continue;
            }
            candidates++;
            BitSet meet = (BitSet) gx.clone();
            meet.and(xe);
            int size = meet.cardinality();
            if (size > best) {
                best = size;
                symmetries.clear();
                intersections.clear();
            }
            if (size == best) {
                symmetries.add(g);
                intersections.add(meet);
            }
        }
        BitSet union = new BitSet(n * n);
        for (BitSet b : intersections) {
            union.or(b);
        }
        return new Result(d, x, k, candidates, Math.max(best, 0), symmetries, union);
    }

    /**
     * The induced polarity p_x[&epsilon;] = T^{x(1&minus;v)+&epsilon;.u} &middot; v of
     * X[&epsilon;] at cantus firmus x, for the polarity T^u &middot; v of the
     * dichotomy (CCW Prop. 2.2, p. 11; Topos I Prop. 51, p. 523).
     */
    public static DualAffine inducedPolarity(Dichotomy d, int x) {
        ZnAffine p = d.polarity();
        return new DualAffine(x * (1 - p.getFactor()), p.getTranslation(), p.getFactor(), 0, d.getModulus());
    }

    private static void requireStrongConsonance(Dichotomy d, int k) {
        d.polarity();
        if (!d.isConsonant(k)) {
            throw new IllegalArgumentException(k + " is not a consonance of " + d);
        }
    }

    /** X[&epsilon;] (consonant = true) or Y[&epsilon;] as a set of indices. */
    private static BitSet induced(Dichotomy d, boolean consonant) {
        int n = d.getModulus();
        BitSet s = new BitSet(n * n);
        for (int c = 0; c < n; c++) {
            for (int i = 0; i < n; i++) {
                if (d.isConsonant(i) == consonant) {
                    s.set(index(c, i, n));
                }
            }
        }
        return s;
    }

    private static BitSet image(DualAffine g, BitSet s, int n) {
        BitSet r = new BitSet(n * n);
        for (int i = s.nextSetBit(0); i >= 0; i = s.nextSetBit(i + 1)) {
            int c = i / n;
            int y = i % n;
            r.set(index(g.applyCantus(c, y), g.applyInterval(c, y), n));
        }
        return r;
    }

    private static int index(int cantus, int interval, int n) {
        return cantus * n + interval;
    }
}
