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

package org.rubato.math.topos.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.rubato.math.topos.FiniteCategory;
import org.rubato.math.topos.FiniteFunctor;
import org.rubato.math.topos.FiniteMonoidAction;
import org.rubato.math.topos.FinitePresheaf;
import org.rubato.math.topos.SemisimplicialCategory;
import org.rubato.math.topos.UniversalMonoid;

/**
 * Small categories, functors and actions from arXiv 2611.32655 used by
 * the master-monoid tests, and the brute-force rewriting the tests compare
 * the normal forms with.
 */
final class MonoidFixtures {

    private MonoidFixtures() { }

    /** Arrow indices of {@link #categoryP()}. */
    static final int F1 = 3, F2 = 4, G1 = 5, G2 = 6, H = 7, K = 8;

    /**
     * The category P of Example 2.2(2): objects x, y, z, arrows f1, f2 :
     * x -> y and g1, g2 : y -> z with g1 f1 = g2 f2 = h and g2 f1 = g1 f2 = k.
     */
    static FiniteCategory categoryP() {
        String[] objects = { "x", "y", "z" };
        String[] names = { "1_x", "1_y", "1_z", "f1", "f2", "g1", "g2", "h", "k" };
        int[] dom = { 0, 1, 2, 0, 0, 1, 1, 0, 0 };
        int[] cod = { 0, 1, 2, 1, 1, 2, 2, 2, 2 };
        int[][] c = table(dom, cod, new int[] { 0, 1, 2 });
        c[G1][F1] = H;
        c[G2][F2] = H;
        c[G2][F1] = K;
        c[G1][F2] = K;
        return FiniteCategory.of(objects, dom, cod, names, new int[] { 0, 1, 2 }, c);
    }

    /** Arrow indices of {@link #retraction()}. */
    static final int S = 2, R = 3, E = 4;

    /**
     * Remark 3.2's retraction: objects a, b, arrows s : a -> b and r : b -> a
     * with r s = 1_a, and the idempotent e = s r on b.
     */
    static FiniteCategory retraction() {
        String[] objects = { "a", "b" };
        String[] names = { "1_a", "1_b", "s", "r", "e" };
        int[] dom = { 0, 1, 0, 1, 1 };
        int[] cod = { 0, 1, 1, 0, 1 };
        int[][] c = table(dom, cod, new int[] { 0, 1 });
        c[R][S] = 0;
        c[S][R] = E;
        c[E][E] = E;
        c[E][S] = S;
        c[R][E] = R;
        return FiniteCategory.of(objects, dom, cod, names, new int[] { 0, 1 }, c);
    }

    /** A table holding only the composites with an identity; -1 elsewhere. */
    private static int[][] table(int[] dom, int[] cod, int[] identity) {
        int n = dom.length;
        int[][] c = new int[n][n];
        for (int g = 0; g < n; g++) {
            for (int f = 0; f < n; f++) {
                c[g][f] = -1;
                if (cod[f] == dom[g]) {
                    if (g == identity[dom[g]]) {
                        c[g][f] = f;
                    } else if (f == identity[cod[f]]) {
                        c[g][f] = g;
                    }
                }
            }
        }
        return c;
    }

    /**
     * Example 2.7's quiver X, two edges e1, e2 from a to b, as a presheaf on
     * the fork's digraph site and then as a functor on its opposite, the
     * Kronecker category K = {E => V} with s = t^op and t = h^op.
     */
    static FiniteFunctor crown() {
        FiniteCategory site = FiniteCategory.digraphSite();
        int[][] restrict = new int[site.arrowCount()][];
        restrict[site.identity(FiniteCategory.V)] = new int[] { 0, 1 };
        restrict[site.identity(FiniteCategory.A)] = new int[] { 0, 1 };
        restrict[FiniteCategory.TAIL] = new int[] { 0, 0 };
        restrict[FiniteCategory.HEAD] = new int[] { 1, 1 };
        FinitePresheaf p = new FinitePresheaf(site, new int[] { 2, 2 }, restrict);
        return FiniteFunctor.ofPresheaf(p);
    }

    /** Leftmost-first rewriting of a word of non-identity arrows (uk acts first). */
    static List<Integer> rewriteLeftmost(FiniteCategory c, List<Integer> word) {
        List<Integer> w = new ArrayList<Integer>(word);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i + 1 < w.size(); i++) {
                if (step(c, w, i)) {
                    changed = true;
                    break;
                }
            }
        }
        return w;
    }

    /** Rightmost-first rewriting of the same rules. */
    static List<Integer> rewriteRightmost(FiniteCategory c, List<Integer> word) {
        List<Integer> w = new ArrayList<Integer>(word);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = w.size() - 2; i >= 0; i--) {
                if (step(c, w, i)) {
                    changed = true;
                    break;
                }
            }
        }
        return w;
    }

    /** Rewrites u v at position i to u after v, or to nothing for an identity. */
    private static boolean step(FiniteCategory c, List<Integer> w, int i) {
        int u = w.get(i);
        int v = w.get(i + 1);
        if (c.dom(u) != c.cod(v)) {
            return false;
        }
        int uv = c.compose(u, v);
        w.remove(i + 1);
        if (uv == c.identity(c.dom(uv))) {
            w.remove(i);
        } else {
            w.set(i, uv);
        }
        return true;
    }

    /** Every word of non-identity arrows of length 1..maxLength. */
    static List<int[]> allWords(UniversalMonoid m, int maxLength) {
        List<int[]> out = new ArrayList<int[]>();
        List<Integer> letters = m.letters();
        List<int[]> level = new ArrayList<int[]>();
        level.add(new int[0]);
        for (int l = 1; l <= maxLength; l++) {
            List<int[]> next = new ArrayList<int[]>();
            for (int[] w : level) {
                for (int u : letters) {
                    int[] v = java.util.Arrays.copyOf(w, l);
                    v[l - 1] = u;
                    next.add(v);
                }
            }
            out.addAll(next);
            level = next;
        }
        return out;
    }

    static List<Integer> list(int[] w) {
        List<Integer> l = new ArrayList<Integer>();
        for (int u : w) {
            l.add(u);
        }
        return l;
    }

    /**
     * A random finite M_2-set: a random 2-dimensional semisimplicial set X
     * together with a set T on which every d^2_j acts as one map phi and
     * every d^1_i as one map psi. A letter an element of X cannot absorb
     * sends it into T, inside one fibre of psi for the d^2_j, so that the
     * three relations of Prop. 4.4 hold. This generator is the tests' own
     * construction; the paper gives no random family.
     */
    static FiniteMonoidAction randomM2Set(SemisimplicialCategory c2, Random rnd) {
        int nv = 1 + rnd.nextInt(4);
        List<int[]> edges = new ArrayList<int[]>(); // {head, tail}
        int ne0 = rnd.nextInt(5);
        for (int i = 0; i < ne0; i++) {
            edges.add(new int[] { rnd.nextInt(nv), rnd.nextInt(nv) });
        }
        List<int[]> tris = new ArrayList<int[]>();
        int nt = rnd.nextInt(4);
        for (int i = 0; i < nt; i++) {
            int x0 = rnd.nextInt(nv), x1 = rnd.nextInt(nv), x2 = rnd.nextInt(nv);
            int e0 = edgeFor(edges, x2, x1, rnd); // x1 -> x2
            int e1 = edgeFor(edges, x2, x0, rnd); // x0 -> x2
            int e2 = edgeFor(edges, x1, x0, rnd); // x0 -> x1
            tris.add(new int[] { e0, e1, e2 });
        }
        int ne = edges.size();
        int ntr = tris.size();
        int nT = 1 + rnd.nextInt(3);
        int base = nv + ne + ntr;
        int size = base + nT;
        int[] phi = new int[nT], psi = new int[nT];
        for (int t = 0; t < nT; t++) {
            phi[t] = rnd.nextInt(nT);
            psi[t] = rnd.nextInt(nT);
        }
        int[][][] g = new int[3][][];
        g[1] = new int[2][size];
        g[2] = new int[3][size];
        for (int s = 0; s < size; s++) {
            int fibreTarget = rnd.nextInt(nT);
            for (int j = 0; j < 3; j++) {
                int v;
                if (s >= nv + ne && s < base) {
                    v = nv + tris.get(s - nv - ne)[j];
                } else if (s >= base) {
                    v = base + phi[s - base];
                } else {
                    v = base + inFibre(psi, fibreTarget, rnd);
                }
                g[2][j][s] = v;
            }
            for (int i = 0; i < 2; i++) {
                int v;
                if (s >= nv && s < nv + ne) {
                    v = edges.get(s - nv)[i];
                } else if (s >= base) {
                    v = base + psi[s - base];
                } else {
                    v = base + rnd.nextInt(nT);
                }
                g[1][i][s] = v;
            }
        }
        return FiniteMonoidAction.ofGenerators(c2, size, g);
    }

    private static int inFibre(int[] psi, int t, Random rnd) {
        List<Integer> f = new ArrayList<Integer>();
        for (int u = 0; u < psi.length; u++) {
            if (psi[u] == psi[t]) {
                f.add(u);
            }
        }
        return f.get(rnd.nextInt(f.size()));
    }

    private static int edgeFor(List<int[]> edges, int head, int tail, Random rnd) {
        List<Integer> found = new ArrayList<Integer>();
        for (int e = 0; e < edges.size(); e++) {
            if (edges.get(e)[0] == head && edges.get(e)[1] == tail) {
                found.add(e);
            }
        }
        if (found.isEmpty() || rnd.nextInt(3) == 0) {
            edges.add(new int[] { head, tail });
            return edges.size() - 1;
        }
        return found.get(rnd.nextInt(found.size()));
    }
}
