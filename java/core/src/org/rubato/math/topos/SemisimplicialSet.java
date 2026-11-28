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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A finite semisimplicial set X_0, ..., X_top with face maps
 * d_i : X_k -&gt; X_{k-1} satisfying d_i d_j = d_{j-1} d_i for i &lt; j
 * (arXiv 2611.32655, section 4.1). Simplices of each dimension are
 * numbered from 0, and faces[k][s][i] holds d_i of simplex s of dimension
 * k. A simplex may meet one face several times, so the homology runs
 * through {@link ChainComplex}.
 * <p>
 * As a functor on C_n (Def. 4.3) it sends [k] to X_k and the face
 * d^k_i to d_i; {@link #toFunctor(SemisimplicialCategory)} and
 * {@link #ofFunctor(SemisimplicialCategory, FiniteFunctor)} convert.
 */
public final class SemisimplicialSet {

    private final int[] counts;
    private final int[][][] faces;
    private final String[][] labels;

    /**
     * A semisimplicial set without labels.
     *
     * @param counts the number of simplices of each dimension 0 .. top
     * @param faces faces[k][s][i] = d_i of simplex s of dimension k, for k &ge; 1
     * @throws IllegalArgumentException if a face leaves its range or an identity fails
     */
    public SemisimplicialSet(int[] counts, int[][][] faces) {
        this(counts, faces, null);
    }

    /** A semisimplicial set with a label for each simplex, labels[k][s], or null. */
    public SemisimplicialSet(int[] counts, int[][][] faces, String[][] labels) {
        this.counts = counts.clone();
        this.faces = new int[counts.length][][];
        for (int k = 1; k < counts.length; k++) {
            if (faces[k].length != counts[k]) {
                throw new IllegalArgumentException("dimension " + k + " needs " + counts[k] + " face lists");
            }
            this.faces[k] = new int[counts[k]][];
            for (int s = 0; s < counts[k]; s++) {
                if (faces[k][s].length != k + 1) {
                    throw new IllegalArgumentException("a " + k + "-simplex needs " + (k + 1) + " faces");
                }
                for (int f : faces[k][s]) {
                    if (f < 0 || f >= counts[k - 1]) {
                        throw new IllegalArgumentException("face out of range in dimension " + k);
                    }
                }
                this.faces[k][s] = faces[k][s].clone();
            }
        }
        this.labels = labels;
        for (int k = 2; k < counts.length; k++) {
            for (int s = 0; s < counts[k]; s++) {
                for (int j = 1; j <= k; j++) {
                    for (int i = 0; i < j; i++) {
                        if (face(k - 1, face(k, s, j), i) != face(k - 1, face(k, s, i), j - 1)) {
                            throw new IllegalArgumentException("d_" + i + " d_" + j + " != d_" + (j - 1) + " d_" + i
                                                               + " on simplex " + s + " of dimension " + k);
                        }
                    }
                }
            }
        }
    }

    /** The top dimension. */
    public int dimension() {
        return counts.length - 1;
    }

    /** The number of k-simplices, 0 above the top dimension. */
    public int count(int k) {
        return k < counts.length ? counts[k] : 0;
    }

    /** d_i of simplex s of dimension k. */
    public int face(int k, int s, int i) {
        return faces[k][s][i];
    }

    /** The label of simplex s of dimension k, or "k:s" without labels. */
    public String label(int k, int s) {
        return labels == null ? k + ":" + s : labels[k][s];
    }

    public int eulerCharacteristic() {
        int chi = 0;
        for (int k = 0; k < counts.length; k++) {
            chi += (k % 2 == 0 ? 1 : -1) * counts[k];
        }
        return chi;
    }

    /** The chain complex with boundary of s the sum of (-1)^i d_i s, repeated faces adding up. */
    public ChainComplex chainComplex() {
        long[][][] b = new long[counts.length][][];
        for (int k = 1; k < counts.length; k++) {
            b[k] = new long[counts[k - 1]][counts[k]];
            for (int s = 0; s < counts[k]; s++) {
                for (int i = 0; i <= k; i++) {
                    b[k][faces[k][s][i]][s] += i % 2 == 0 ? 1 : -1;
                }
            }
        }
        return new ChainComplex(counts, b);
    }

    /** The integral homology H_0 ... H_top of the geometric realization. */
    public List<ChainComplex.Homology> homology() {
        return chainComplex().homology();
    }

    /**
     * X as a functor on C_n: X([k]) = X_k (empty above the top dimension),
     * and an arrow acts through its faces in the descending order that
     * names it.
     */
    public FiniteFunctor toFunctor(SemisimplicialCategory c) {
        if (dimension() > c.n()) {
            throw new IllegalArgumentException("dimension " + dimension() + " above n = " + c.n());
        }
        FiniteCategory cat = c.category();
        int[] sizes = new int[c.n() + 1];
        for (int k = 0; k <= c.n(); k++) {
            sizes[k] = count(k);
        }
        int[][] act = new int[cat.arrowCount()][];
        for (int a = 0; a < act.length; a++) {
            int k = cat.dom(a);
            int[] dropped = SemisimplicialCategory.droppedDescending(k, c.kept(a));
            act[a] = new int[sizes[k]];
            for (int s = 0; s < sizes[k]; s++) {
                int x = s;
                int level = k;
                for (int v : dropped) {
                    x = face(level, x, v);
                    level--;
                }
                act[a][s] = x;
            }
        }
        return new FiniteFunctor(cat, sizes, act);
    }

    /** The semisimplicial set of a functor on C_n: X_k = F([k]) and d_i = F(d^k_i). */
    public static SemisimplicialSet ofFunctor(SemisimplicialCategory c, FiniteFunctor f) {
        int n = c.n();
        int[] counts = new int[n + 1];
        int[][][] faces = new int[n + 1][][];
        for (int k = 0; k <= n; k++) {
            counts[k] = f.size(k);
        }
        for (int k = 1; k <= n; k++) {
            faces[k] = new int[counts[k]][k + 1];
            for (int s = 0; s < counts[k]; s++) {
                for (int i = 0; i <= k; i++) {
                    faces[k][s][i] = f.act(c.face(k, i), s);
                }
            }
        }
        return new SemisimplicialSet(counts, faces);
    }

    /**
     * The semisimplicial nerve N+C of a finite acyclic category (arXiv
     * 2611.32655, Remark 4.14): its k-simplices are the chains
     * x0 -&gt; x1 -&gt; ... -&gt; xk of k non-identity arrows; d_0 and d_k drop
     * an end and d_i for 0 &lt; i &lt; k composes the arrows at x_i. Its
     * realization is homotopy equivalent to BC.
     *
     * @throws IllegalArgumentException if C is not acyclic
     */
    public static SemisimplicialSet nerve(FiniteCategory c) {
        if (!c.isAcyclic()) {
            throw new IllegalArgumentException("the semisimplicial nerve needs an acyclic category");
        }
        List<List<int[]>> chains = new ArrayList<List<int[]>>();
        List<Map<String, Integer>> index = new ArrayList<Map<String, Integer>>();
        List<int[]> level = new ArrayList<int[]>();
        for (int o = 0; o < c.objectCount(); o++) {
            level.add(new int[] { o });
        }
        chains.add(level);
        index.add(indexOf(level));
        List<int[]> arrows = new ArrayList<int[]>();
        for (int f = 0; f < c.arrowCount(); f++) {
            if (f != c.identity(c.dom(f))) {
                arrows.add(new int[] { f });
            }
        }
        level = arrows;
        while (!level.isEmpty()) {
            chains.add(level);
            index.add(indexOf(level));
            List<int[]> next = new ArrayList<int[]>();
            for (int[] ch : level) {
                int last = ch[ch.length - 1];
                for (int f = 0; f < c.arrowCount(); f++) {
                    if (f != c.identity(c.dom(f)) && c.dom(f) == c.cod(last)) {
                        int[] e = Arrays.copyOf(ch, ch.length + 1);
                        e[ch.length] = f;
                        next.add(e);
                    }
                }
            }
            level = next;
        }
        int top = chains.size() - 1;
        int[] counts = new int[top + 1];
        int[][][] faces = new int[top + 1][][];
        String[][] labels = new String[top + 1][];
        for (int k = 0; k <= top; k++) {
            List<int[]> lk = chains.get(k);
            counts[k] = lk.size();
            labels[k] = new String[counts[k]];
            for (int s = 0; s < counts[k]; s++) {
                int[] ch = lk.get(s);
                StringBuilder b = new StringBuilder();
                for (int t = 0; t < ch.length; t++) {
                    if (t > 0) {
                        b.append(" ; ");
                    }
                    b.append(k == 0 ? c.objectName(ch[t]) : c.arrowName(ch[t]));
                }
                labels[k][s] = b.toString();
            }
            if (k == 0) {
                continue;
            }
            faces[k] = new int[counts[k]][k + 1];
            for (int s = 0; s < counts[k]; s++) {
                int[] ch = lk.get(s);
                for (int i = 0; i <= k; i++) {
                    int[] f;
                    if (k == 1) {
                        f = new int[] { i == 0 ? c.cod(ch[0]) : c.dom(ch[0]) };
                    } else if (i == 0) {
                        f = Arrays.copyOfRange(ch, 1, k);
                    } else if (i == k) {
                        f = Arrays.copyOfRange(ch, 0, k - 1);
                    } else {
                        f = new int[k - 1];
                        for (int t = 0, u = 0; t < k; t++) {
                            if (t == i - 1) {
                                f[u++] = c.compose(ch[i], ch[i - 1]);
                                t++;
                            } else {
                                f[u++] = ch[t];
                            }
                        }
                    }
                    faces[k][s][i] = index.get(k - 1).get(Arrays.toString(f));
                }
            }
        }
        return new SemisimplicialSet(counts, faces, labels);
    }

    private static Map<String, Integer> indexOf(List<int[]> level) {
        Map<String, Integer> m = new HashMap<String, Integer>();
        for (int s = 0; s < level.size(); s++) {
            m.put(Arrays.toString(level.get(s)), s);
        }
        return m;
    }
}
