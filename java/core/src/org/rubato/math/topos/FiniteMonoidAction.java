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

import java.util.List;

/**
 * A finite set S = {0, ..., size - 1} with an action of the universal
 * monoid Mon(C) of a finite category (arXiv 2611.32655, Def. 2.1). By
 * the presentation of Def. 2.1 the action is given by one map per
 * non-identity arrow, with [g][f] acting as [g after f] for every
 * composable pair and every identity acting trivially; the constructor
 * checks this.
 * <p>
 * The homotopy quotient S//Mon(C) is computed through the finite
 * semisimplicial complex in the proof of Thm. 4.12, and, as a cross-check,
 * through Lemma 4.10 with B&int;&iota;*S taken as a semisimplicial nerve
 * (Remark 4.14).
 */
public final class FiniteMonoidAction {

    private final UniversalMonoid monoid;
    private final int size;
    private final int[][] act;

    /**
     * An action from one map per arrow.
     *
     * @param monoid Mon(C)
     * @param size the number of elements of S
     * @param letterAct letterAct[f][s] = [f].s for each arrow f; the rows
     *        of identities may be null
     * @throws IllegalArgumentException if the maps do not respect composition
     */
    public FiniteMonoidAction(UniversalMonoid monoid, int size, int[][] letterAct) {
        this.monoid = monoid;
        this.size = size;
        FiniteCategory c = monoid.category();
        if (letterAct.length != c.arrowCount()) {
            throw new IllegalArgumentException("one map per arrow is needed");
        }
        act = new int[c.arrowCount()][];
        for (int f = 0; f < act.length; f++) {
            boolean id = f == c.identity(c.dom(f));
            if (id && letterAct[f] == null) {
                act[f] = identityMap(size);
                continue;
            }
            if (letterAct[f].length != size) {
                throw new IllegalArgumentException("the map of " + c.arrowName(f) + " has the wrong length");
            }
            act[f] = letterAct[f].clone();
            for (int s = 0; s < size; s++) {
                if (act[f][s] < 0 || act[f][s] >= size || (id && act[f][s] != s)) {
                    throw new IllegalArgumentException("bad map for " + c.arrowName(f));
                }
            }
        }
        for (int g = 0; g < act.length; g++) {
            for (int f = 0; f < act.length; f++) {
                if (c.cod(f) != c.dom(g)) {
                    continue;
                }
                int gf = c.compose(g, f);
                for (int s = 0; s < size; s++) {
                    if (act[g][act[f][s]] != act[gf][s]) {
                        throw new IllegalArgumentException("[" + c.arrowName(g) + "][" + c.arrowName(f) + "] != ["
                                                           + c.arrowName(gf) + "] on " + s);
                    }
                }
            }
        }
    }

    /**
     * A finite M_n-set from the maps of the generators d^k_i (Prop. 4.4).
     * Each other arrow of C_n acts through its descending face
     * factorization. The relations of Prop. 4.4 are checked first.
     *
     * @param generators generators[k][i][s] = d^k_i . s for 1 &le; k &le; n
     * @throws IllegalArgumentException if a relation fails
     */
    public static FiniteMonoidAction ofGenerators(SemisimplicialCategory c, int size, int[][][] generators) {
        for (SemisimplicialCategory.Relation r : c.relations()) {
            for (int s = 0; s < size; s++) {
                if (apply(generators, r.left, s) != apply(generators, r.right, s)) {
                    throw new IllegalArgumentException("relation " + r + " fails on " + s);
                }
            }
        }
        FiniteCategory cat = c.category();
        int[][] a = new int[cat.arrowCount()][];
        for (int f = 0; f < a.length; f++) {
            int k = cat.dom(f);
            int[] dropped = SemisimplicialCategory.droppedDescending(k, c.kept(f));
            a[f] = new int[size];
            for (int s = 0; s < size; s++) {
                int x = s;
                int level = k;
                for (int v : dropped) {
                    x = generators[level][v][x];
                    level--;
                }
                a[f][s] = x;
            }
        }
        return new FiniteMonoidAction(c.monoid(), size, a);
    }

    private static int apply(int[][][] g, int[][] word, int s) {
        int x = s;
        for (int t = word.length - 1; t >= 0; t--) {
            x = g[word[t][0]][word[t][1]][x];
        }
        return x;
    }

    private static int[] identityMap(int size) {
        int[] m = new int[size];
        for (int s = 0; s < size; s++) {
            m[s] = s;
        }
        return m;
    }

    public int size() {
        return size;
    }

    public UniversalMonoid monoid() {
        return monoid;
    }

    /** m . s, the last letter of the word acting first. */
    public int act(int[] word, int s) {
        int x = s;
        for (int t = word.length - 1; t >= 0; t--) {
            x = act[word[t]][x];
        }
        return x;
    }

    /** The restriction &iota;*S: the functor on C with S at every object and f acting as [f]. */
    public FiniteFunctor restriction() {
        FiniteCategory c = monoid.category();
        int[] sizes = new int[c.objectCount()];
        for (int o = 0; o < sizes.length; o++) {
            sizes[o] = size;
        }
        return new FiniteFunctor(c, sizes, act);
    }

    /**
     * The finite semisimplicial complex of the proof of Thm. 4.12 for an
     * M_n-set: the semisimplicial set Z = &iota;_n*S, with Z_k = S for
     * k = 0 .. n, plus a new vertex s&#772; for each s and, for each object
     * [k] and each s, an edge joining the vertex v(k, s) = Z(&epsilon;)(s)
     * to s&#772;, where &epsilon; : [k] -&gt; [0] keeps vertex 0. That gives
     * 2|S| vertices and (n + 1)|S| extra edges. Its realization is
     * homotopy equivalent to S//M_n.
     * <p>
     * The proof takes each extra edge "from v(s) to s&#772;"; this class
     * reads that as d_1 = v(k, s) and d_0 = s&#772;. The numbering is a
     * choice: the vertices s&#772; follow the vertices of Z, and the extra
     * edges follow the edges of Z, ordered by k and then s.
     */
    public SemisimplicialSet homotopyQuotientComplex(SemisimplicialCategory c) {
        checkCategory(c);
        int n = c.n();
        SemisimplicialSet z = SemisimplicialSet.ofFunctor(c, restriction());
        FiniteCategory cat = c.category();
        int[] counts = new int[Math.max(n, 1) + 1];
        for (int k = 0; k <= n; k++) {
            counts[k] = size;
        }
        counts[0] = 2 * size;
        counts[1] = (n >= 1 ? size : 0) + (n + 1) * size;
        int[][][] faces = new int[counts.length][][];
        for (int k = 2; k <= n; k++) {
            faces[k] = new int[size][k + 1];
            for (int s = 0; s < size; s++) {
                for (int i = 0; i <= k; i++) {
                    faces[k][s][i] = z.face(k, s, i);
                }
            }
        }
        faces[1] = new int[counts[1]][];
        int e = 0;
        if (n >= 1) {
            for (int s = 0; s < size; s++) {
                faces[1][e++] = new int[] { z.face(1, s, 0), z.face(1, s, 1) };
            }
        }
        for (int k = 0; k <= n; k++) {
            int eps = vertexArrow(c, k);
            for (int s = 0; s < size; s++) {
                faces[1][e++] = new int[] { size + s, act[eps][s] };
            }
        }
        if (cat.objectCount() != n + 1) {
            throw new IllegalStateException("C_n has n + 1 objects");
        }
        return new SemisimplicialSet(counts, faces);
    }

    /** The homology of S//M_n through {@link #homotopyQuotientComplex(SemisimplicialCategory)}. */
    public List<ChainComplex.Homology> homotopyQuotientHomology(SemisimplicialCategory c) {
        return homotopyQuotientComplex(c).homology();
    }

    /**
     * Lemma 4.10 read directly: S//Mon(C) is the homotopy pushout of
     * B&int;&iota;*S &lt;- Ob(C) x S -&gt; S. Here B&int;&iota;*S is the
     * semisimplicial nerve of the category of elements (Remark 4.14; C
     * must be acyclic), each object (c, s) of the category of elements is
     * joined by an edge to a new vertex s&#772;, with d_0 = s&#772; and
     * d_1 = (c, s). This replaces the realization of Z in Thm. 4.12 by a
     * nerve, so it checks the vertex choice v(k, s) and the edge count of
     * that proof; it rests on the same Lemma 4.10.
     */
    public SemisimplicialSet homotopyQuotientByNerve() {
        CategoryOfElements el = new CategoryOfElements(restriction());
        SemisimplicialSet nerve = SemisimplicialSet.nerve(el.category());
        int objects = nerve.count(0);
        int top = Math.max(nerve.dimension(), 1);
        int[] counts = new int[top + 1];
        int[][][] faces = new int[top + 1][][];
        for (int k = 0; k <= top; k++) {
            counts[k] = nerve.count(k);
        }
        counts[0] = objects + size;
        counts[1] = nerve.count(1) + objects;
        for (int k = 2; k <= top; k++) {
            faces[k] = new int[counts[k]][k + 1];
            for (int s = 0; s < counts[k]; s++) {
                for (int i = 0; i <= k; i++) {
                    faces[k][s][i] = nerve.face(k, s, i);
                }
            }
        }
        faces[1] = new int[counts[1]][];
        for (int s = 0; s < nerve.count(1); s++) {
            faces[1][s] = new int[] { nerve.face(1, s, 0), nerve.face(1, s, 1) };
        }
        for (int o = 0; o < objects; o++) {
            faces[1][nerve.count(1) + o] = new int[] { objects + el.element(o), o };
        }
        return new SemisimplicialSet(counts, faces);
    }

    private void checkCategory(SemisimplicialCategory c) {
        FiniteCategory a = c.category();
        FiniteCategory b = monoid.category();
        if (a.objectCount() != b.objectCount() || a.arrowCount() != b.arrowCount()) {
            throw new IllegalArgumentException("the action is not an action of M_" + c.n());
        }
        for (int f = 0; f < a.arrowCount(); f++) {
            if (a.dom(f) != b.dom(f) || a.cod(f) != b.cod(f) || !a.arrowName(f).equals(b.arrowName(f))) {
                throw new IllegalArgumentException("the action is not an action of M_" + c.n());
            }
        }
    }

    /** The arrow &epsilon; : [k] -&gt; [0] keeping vertex 0, whose action gives the 0-th vertex. */
    private static int vertexArrow(SemisimplicialCategory c, int k) {
        for (int f : c.category().hom(k, 0)) {
            if (c.kept(f)[0] == 0) {
                return f;
            }
        }
        throw new IllegalStateException("no arrow [" + k + "] -> [0] keeping 0");
    }
}
