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

package org.rubato.math.topos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The truncated semisimplicial category C_n = (&Delta;<sup>&le;n</sup><sub>inj</sub>)<sup>op</sup>
 * and the presentation of the truncated master monoid M_n = Mon(C_n)
 * (arXiv 2611.32655, section 4.1, Def. 4.3, Prop. 4.4).
 * <p>
 * The objects are [0] ... [n], object k standing for [k]. An arrow
 * [k] -&gt; [l] is an injective monotone map [l] -&gt; [k], stored as the
 * sorted set of the l + 1 vertices of [k] it keeps. The face
 * d^k_i : [k] -&gt; [k-1] keeps every vertex but i, and the faces satisfy
 * d^{k-1}_i d^k_j = d^{k-1}_{j-1} d^k_i for i &lt; j.
 * <p>
 * Arrow names follow one choice the paper leaves open: an arrow that
 * drops two or more vertices is named by its factorization that drops
 * the vertices in descending order, written with the first-acting face
 * on the right, so the arrow [2] -&gt; [0] keeping vertex 0 is
 * "d^1_1 d^2_2".
 */
public final class SemisimplicialCategory {

    /** The relation d^{k-1}_i d^k_j = d^{k-1}_{j-1} d^k_i of Prop. 4.4, as generator pairs {k, i}. */
    public static final class Relation {
        public final int[][] left;
        public final int[][] right;

        Relation(int[][] left, int[][] right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return word(left) + " = " + word(right);
        }
    }

    private final int n;
    private final FiniteCategory category;
    private final int[][] kept;
    private final int[][] faces;
    private final List<String> generators;
    private final List<Relation> relations;
    private UniversalMonoid monoid;

    private SemisimplicialCategory(int n) {
        this.n = n;
        List<int[]> keep = new ArrayList<int[]>();
        List<Integer> dom = new ArrayList<Integer>();
        List<Integer> cod = new ArrayList<Integer>();
        for (int k = 0; k <= n; k++) {
            keep.add(range(k + 1));
            dom.add(k);
            cod.add(k);
        }
        faces = new int[n + 1][];
        List<String> gens = new ArrayList<String>();
        for (int k = 1; k <= n; k++) {
            faces[k] = new int[k + 1];
            for (int i = 0; i <= k; i++) {
                faces[k][i] = keep.size();
                keep.add(drop(range(k + 1), i));
                dom.add(k);
                cod.add(k - 1);
                gens.add(name(k, i));
            }
        }
        for (int k = 2; k <= n; k++) {
            for (int l = k - 2; l >= 0; l--) {
                for (int[] s : subsets(k + 1, l + 1)) {
                    keep.add(s);
                    dom.add(k);
                    cod.add(l);
                }
            }
        }
        int m = keep.size();
        kept = keep.toArray(new int[m][]);
        int[] d = new int[m];
        int[] c = new int[m];
        String[] names = new String[m];
        for (int a = 0; a < m; a++) {
            d[a] = dom.get(a);
            c[a] = cod.get(a);
            names[a] = arrowName(d[a], kept[a]);
        }
        String[] objects = new String[n + 1];
        int[] identity = new int[n + 1];
        for (int k = 0; k <= n; k++) {
            objects[k] = "[" + k + "]";
            identity[k] = k;
        }
        int[][] compose = new int[m][m];
        for (int g = 0; g < m; g++) {
            for (int f = 0; f < m; f++) {
                compose[g][f] = -1;
                if (c[f] == d[g]) {
                    int[] s = new int[kept[g].length];
                    for (int b = 0; b < s.length; b++) {
                        s[b] = kept[f][kept[g][b]];
                    }
                    compose[g][f] = find(kept, d, d[f], s);
                }
            }
        }
        category = FiniteCategory.of(objects, d, c, names, identity, compose);
        generators = Collections.unmodifiableList(gens);
        List<Relation> rels = new ArrayList<Relation>();
        for (int k = 2; k <= n; k++) {
            for (int i = 0; i < k; i++) {
                for (int j = i + 1; j <= k; j++) {
                    rels.add(new Relation(new int[][] { { k - 1, i }, { k, j } },
                                          new int[][] { { k - 1, j - 1 }, { k, i } }));
                }
            }
        }
        relations = Collections.unmodifiableList(rels);
    }

    /** C_n for n &ge; 0. */
    public static SemisimplicialCategory truncated(int n) {
        if (n < 0 || n > 7) {
            throw new IllegalArgumentException("n out of range 0..7: " + n);
        }
        return new SemisimplicialCategory(n);
    }

    /** n(n+3)/2, the number of generators of M_n (Prop. 4.4). */
    public static int generatorCount(int n) {
        return n * (n + 3) / 2;
    }

    /** The sum of C(k+1, 2) over k = 2 .. n, the number of relations of M_n (Prop. 4.4). */
    public static int relationCount(int n) {
        int r = 0;
        for (int k = 2; k <= n; k++) {
            r += (k + 1) * k / 2;
        }
        return r;
    }

    public int n() {
        return n;
    }

    public FiniteCategory category() {
        return category;
    }

    /** The arrow d^k_i : [k] -&gt; [k-1]. */
    public int face(int k, int i) {
        if (k < 1 || k > n || i < 0 || i > k) {
            throw new IllegalArgumentException("no face d^" + k + "_" + i + " in C_" + n);
        }
        return faces[k][i];
    }

    /** The vertices of [dom] that the arrow keeps, in increasing order. */
    public int[] kept(int arrow) {
        return kept[arrow].clone();
    }

    /** "d^1_0", "d^1_1", "d^2_0", ...: the generators of M_n in order. */
    public List<String> generators() {
        return generators;
    }

    /** The relations of Prop. 4.4, by k, then i, then j. */
    public List<Relation> relations() {
        return relations;
    }

    public List<String> relationStrings() {
        List<String> out = new ArrayList<String>();
        for (Relation r : relations) {
            out.add(r.toString());
        }
        return out;
    }

    /** "M_2 = &lt; d^1_0, ... | d^1_0 d^2_1 = d^1_0 d^2_0, ... &gt;". */
    public String presentation() {
        StringBuilder b = new StringBuilder("M_").append(n).append(" = < ");
        join(b, generators);
        if (!relations.isEmpty()) {
            b.append(" | ");
            join(b, relationStrings());
        }
        return b.append(" >").toString();
    }

    /** M_n = Mon(C_n) (Def. 4.3). */
    public UniversalMonoid monoid() {
        if (monoid == null) {
            monoid = new UniversalMonoid(category);
        }
        return monoid;
    }

    /**
     * The reduced word in M_n of a product of generators, given as pairs
     * {k, i} for d^k_i with the first-acting generator last.
     */
    public int[] element(int[][] generatorPairs) {
        int[] w = new int[generatorPairs.length];
        for (int t = 0; t < w.length; t++) {
            int k = generatorPairs[t][0];
            int i = generatorPairs[t][1];
            if (k < 1 || k > n || i < 0 || i > k) {
                throw new IllegalArgumentException("d^" + k + "_" + i + " is not a generator of M_" + n);
            }
            w[t] = faces[k][i];
        }
        return monoid().normalize(w);
    }

    private static void join(StringBuilder b, List<String> items) {
        for (int t = 0; t < items.size(); t++) {
            if (t > 0) {
                b.append(", ");
            }
            b.append(items.get(t));
        }
    }

    static String name(int k, int i) {
        return "d^" + k + "_" + i;
    }

    static String word(int[][] pairs) {
        StringBuilder b = new StringBuilder();
        for (int[] p : pairs) {
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(name(p[0], p[1]));
        }
        return b.toString();
    }

    /** The dropped vertices of [k] in descending order: the canonical face factorization, first-acting face first. */
    static int[] droppedDescending(int k, int[] keep) {
        int[] out = new int[k + 1 - keep.length];
        int t = 0;
        for (int v = k; v >= 0; v--) {
            if (Arrays.binarySearch(keep, v) < 0) {
                out[t++] = v;
            }
        }
        return out;
    }

    private static String arrowName(int k, int[] keep) {
        int[] dropped = droppedDescending(k, keep);
        if (dropped.length == 0) {
            return "1_[" + k + "]";
        }
        StringBuilder b = new StringBuilder();
        for (int t = dropped.length - 1; t >= 0; t--) {
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(name(k - t, dropped[t]));
        }
        return b.toString();
    }

    private static int find(int[][] kept, int[] dom, int d, int[] s) {
        for (int a = 0; a < kept.length; a++) {
            if (dom[a] == d && Arrays.equals(kept[a], s)) {
                return a;
            }
        }
        throw new IllegalStateException("missing arrow");
    }

    private static int[] range(int m) {
        int[] r = new int[m];
        for (int i = 0; i < m; i++) {
            r[i] = i;
        }
        return r;
    }

    private static int[] drop(int[] a, int i) {
        int[] out = new int[a.length - 1];
        for (int j = 0, t = 0; j < a.length; j++) {
            if (j != i) {
                out[t++] = a[j];
            }
        }
        return out;
    }

    private static List<int[]> subsets(int m, int size) {
        List<int[]> out = new ArrayList<int[]>();
        int[] s = range(size);
        while (true) {
            out.add(s.clone());
            int i = size - 1;
            while (i >= 0 && s[i] == m - size + i) {
                i--;
            }
            if (i < 0) {
                return out;
            }
            s[i]++;
            for (int j = i + 1; j < size; j++) {
                s[j] = s[j - 1] + 1;
            }
        }
    }
}
