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

package org.rubato.math.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A zero-addressed objective global composition (Mazzola, <i>The Topos of
 * Music</i>, &sect;13.2, Def. 36, p. 252): a support set G, here the points
 * of a {@link LocalComposition} in Z<sub>n</sub><sup>k</sup>, a finite non-empty
 * covering I of G by named subsets, and an atlas of charts. Each chart t
 * has a local composition K<sub>t</sub>, a covering set I<sub>t</sub> and a
 * bijection &phi;<sub>t</sub> : K<sub>t</sub> &rarr; I<sub>t</sub>; the map t &#8614;
 * I<sub>t</sub> must reach every covering set (condition iv).
 * {@link #gluingViolations()} checks condition (vi): on every overlap the
 * transition &phi;<sub>t</sub><sup>&minus;1</sup> &middot; &phi;<sub>s</sub> is an
 * isomorphism of local compositions.
 * <p>
 * {@link #interpretation} builds the interpretation K<sup>I</sup> of Def. 39
 * (p. 258), whose charts are the covering sets themselves.
 * {@link #nerve()} is the nerve n(G<sup>I</sup>) of &sect;13.2.1 (pp. 253&ndash;254).
 * Non-zero addresses and functorial global compositions are not modelled.
 * Instances are immutable.
 */
public final class GlobalComposition {

    /** A chart: a local composition K, its covering set, and &phi; : K &rarr; I_t. */
    public static final class Chart {
        public final LocalComposition domain;
        public final int coveringSet;
        private final int[] phi;

        /**
         * @param domain      the chart's local composition K_t
         * @param coveringSet the index of I_t in the covering
         * @param phi         phi[i] is the support index of the image of point i of K_t
         */
        public Chart(LocalComposition domain, int coveringSet, int[] phi) {
            this.domain = domain;
            this.coveringSet = coveringSet;
            this.phi = phi.clone();
        }

        /** The support index of the image of point i. */
        public int phi(int i) {
            return phi[i];
        }

        int[] phi() {
            return phi.clone();
        }
    }

    private final LocalComposition support;
    private final List<String> names;
    private final List<int[]> covering;
    private final List<Chart> atlas;

    /**
     * Creates a global composition from its support, covering and atlas.
     *
     * @throws IllegalArgumentException if the covering is empty, has an empty
     *         set, does not cover the support, or an atlas chart is not a
     *         bijection onto its covering set, or some covering set has no chart
     */
    public GlobalComposition(LocalComposition support, List<String> names, List<int[]> covering, List<Chart> atlas) {
        if (covering.isEmpty()) {
            throw new IllegalArgumentException("The covering must not be empty");
        }
        if (names.size() != covering.size()) {
            throw new IllegalArgumentException(names.size() + " names for " + covering.size() + " covering sets");
        }
        if (new HashSet<String>(names).size() != names.size()) {
            throw new IllegalArgumentException("Covering set names must be distinct: " + names);
        }
        Set<Integer> covered = new HashSet<Integer>();
        List<int[]> cov = new ArrayList<int[]>();
        for (int i = 0; i < covering.size(); i++) {
            int[] c = covering.get(i);
            if (c.length == 0) {
                throw new IllegalArgumentException("Covering set " + names.get(i) + " is empty");
            }
            TreeSet<Integer> s = new TreeSet<Integer>();
            for (int x : c) {
                if (x < 0 || x >= support.size()) {
                    throw new IllegalArgumentException("Covering set " + names.get(i) + " has index " + x
                            + " outside the support of " + support.size() + " points");
                }
                s.add(x);
            }
            covered.addAll(s);
            cov.add(toArray(s));
        }
        if (covered.size() != support.size()) {
            throw new IllegalArgumentException("The covering misses " + (support.size() - covered.size()) + " support points");
        }
        boolean[] reached = new boolean[cov.size()];
        for (Chart ch : atlas) {
            if (ch.coveringSet < 0 || ch.coveringSet >= cov.size()) {
                throw new IllegalArgumentException("A chart names covering set " + ch.coveringSet + ", which does not exist");
            }
            int[] target = cov.get(ch.coveringSet);
            if (ch.domain.size() != target.length || ch.phi.length != target.length) {
                throw new IllegalArgumentException("A chart for " + names.get(ch.coveringSet) + " is not a bijection: "
                        + ch.domain.size() + " points onto " + target.length);
            }
            int[] sorted = ch.phi.clone();
            Arrays.sort(sorted);
            if (!Arrays.equals(sorted, target)) {
                throw new IllegalArgumentException("A chart for " + names.get(ch.coveringSet) + " does not map onto it");
            }
            reached[ch.coveringSet] = true;
        }
        for (int i = 0; i < reached.length; i++) {
            if (!reached[i]) {
                throw new IllegalArgumentException("Covering set " + names.get(i) + " has no chart (Def. 36 (iv))");
            }
        }
        this.support = support;
        this.names = Collections.unmodifiableList(new ArrayList<String>(names));
        this.covering = Collections.unmodifiableList(cov);
        this.atlas = Collections.unmodifiableList(new ArrayList<Chart>(atlas));
    }

    /**
     * The interpretation K<sup>I</sup> of a local composition by a covering,
     * with the obvious atlas of inclusions (Def. 39, p. 258).
     */
    public static GlobalComposition interpretation(LocalComposition k, List<String> names, List<int[]> covering) {
        List<Chart> atlas = new ArrayList<Chart>();
        for (int i = 0; i < covering.size(); i++) {
            TreeSet<Integer> s = new TreeSet<Integer>();
            for (int x : covering.get(i)) {
                s.add(x);
            }
            atlas.add(new Chart(k.sub(s), i, toArray(s)));
        }
        return new GlobalComposition(k, names, covering, atlas);
    }

    public LocalComposition getSupport() { return support; }

    /** The covering set names, in covering order. */
    public List<String> names() { return names; }

    /** The number of covering sets. */
    public int coveringSize() { return covering.size(); }

    /** The support indexes of covering set i, sorted. */
    public int[] coveringSet(int i) { return covering.get(i).clone(); }

    /** Covering set i as a subcomposition of the support. */
    public LocalComposition coveringComposition(int i) {
        List<Integer> idx = new ArrayList<Integer>();
        for (int x : covering.get(i)) {
            idx.add(x);
        }
        return support.sub(idx);
    }

    /** The atlas. */
    public List<Chart> atlas() { return atlas; }

    /**
     * The support indexes in the intersection of the covering sets listed,
     * sorted; empty if they do not meet.
     */
    public int[] intersection(int... sets) {
        Set<Integer> s = null;
        for (int i : sets) {
            Set<Integer> c = new TreeSet<Integer>();
            for (int x : covering.get(i)) {
                c.add(x);
            }
            if (s == null) {
                s = c;
            }
            else {
                s.retainAll(c);
            }
        }
        return s == null ? new int[0] : toArray(s);
    }

    /** The intersection X_&sigma; of the covering sets of a simplex, as a local composition. */
    public LocalComposition intersectionComposition(int... sets) {
        List<Integer> idx = new ArrayList<Integer>();
        for (int x : intersection(sets)) {
            idx.add(x);
        }
        if (idx.isEmpty()) {
            throw new IllegalArgumentException("The covering sets do not meet");
        }
        return support.sub(idx);
    }

    /**
     * The nerve n(G<sup>I</sup>): its vertices are the covering sets, its
     * simplexes the sets of covering sets with a common point (&sect;13.2.1,
     * p. 253; App. H.2.1, Ex. 131, p. 1445).
     */
    public SimplicialComplex nerve() {
        int n = covering.size();
        if (n > SimplicialComplex.MAX_VERTICES) {
            throw new IllegalArgumentException(n + " covering sets; the nerve supports at most "
                    + SimplicialComplex.MAX_VERTICES);
        }
        // for each support point, the covering sets that contain it; every
        // simplex is a subset of one such star
        Set<Long> simplexes = new HashSet<Long>();
        for (int p = 0; p < support.size(); p++) {
            long star = 0;
            for (int i = 0; i < n; i++) {
                if (Arrays.binarySearch(covering.get(i), p) >= 0) {
                    star |= 1L << i;
                }
            }
            for (long sub = star; sub != 0; sub = (sub - 1) & star) {
                simplexes.add(sub);
            }
        }
        return SimplicialComplex.ofMasks(names, simplexes);
    }

    /**
     * Checks Def. 36 (vi): for every two charts s, t whose covering sets
     * meet, the transition &phi;<sub>t</sub><sup>&minus;1</sup> &middot; &phi;<sub>s</sub>
     * on the preimages of the overlap is an isomorphism of local
     * compositions (Def. 26). Returns one line per failing pair; an empty
     * list means the atlas glues.
     */
    public List<String> gluingViolations() {
        List<String> out = new ArrayList<String>();
        for (int s = 0; s < atlas.size(); s++) {
            for (int t = 0; t < atlas.size(); t++) {
                if (s == t) {
                    continue;
                }
                Chart cs = atlas.get(s), ct = atlas.get(t);
                int[] overlap = intersection(cs.coveringSet, ct.coveringSet);
                if (overlap.length == 0) {
                    continue;
                }
                List<Integer> ds = new ArrayList<Integer>(), dt = new ArrayList<Integer>();
                for (int x : overlap) {
                    ds.add(preimage(cs, x));
                    dt.add(preimage(ct, x));
                }
                LocalComposition ks = cs.domain.sub(ds);
                LocalComposition kt = ct.domain.sub(dt);
                int[] f = new int[ks.size()];
                for (int i = 0; i < ks.size(); i++) {
                    int x = cs.phi(cs.domain.indexOf(ks.point(i)));
                    f[i] = kt.indexOf(ct.domain.point(preimage(ct, x)));
                }
                if (!ks.isIsomorphism(f, kt)) {
                    out.add("charts " + s + " (" + names.get(cs.coveringSet) + ") and " + t + " ("
                            + names.get(ct.coveringSet) + "): the transition on " + ks + " is not an isomorphism onto " + kt);
                }
            }
        }
        return out;
    }

    private static int preimage(Chart c, int supportIndex) {
        for (int i = 0; i < c.phi.length; i++) {
            if (c.phi[i] == supportIndex) {
                return i;
            }
        }
        throw new IllegalArgumentException("Support point " + supportIndex + " is not in the chart");
    }

    static int[] toArray(Set<Integer> s) {
        int[] a = new int[s.size()];
        int k = 0;
        for (int x : s) {
            a[k++] = x;
        }
        return a;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < covering.size(); i++) {
            b.append(names.get(i)).append(" = ").append(coveringComposition(i)).append('\n');
        }
        return b.toString();
    }
}
