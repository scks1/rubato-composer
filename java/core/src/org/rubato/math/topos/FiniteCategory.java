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
 * A finite category given by its objects, arrows and composition table,
 * used as a finite address category in place of Mod (Mazzola, The Topos of
 * Music, vol. I, section 6.2.2, p. 54; sources/FINITE-MODAT-SPEC.md, D1).
 * Objects are 0..k-1 and arrows 0..n-1. The table entry compose[g][f] is
 * the index of g after f, or -1 when the codomain of f is not the domain
 * of g. The constructor checks the category laws and refuses a table that
 * breaks them.
 *
 * <p>{@link #affineCyclic} builds the full subcategory of Mod on cyclic
 * Z-modules Z_n with every affine map, each arrow given by its value
 * table. {@link #affineAutomorphisms} and {@link #digraphSite} are not
 * full subcategories of Mod; they serve the groupoid case of Topos I
 * p. 335 and the comparison with the topos of digraphs.</p>
 */
public final class FiniteCategory {

    /** The vertex object of {@link #digraphSite()}. */
    public static final int V = 0;
    /** The arrow object of {@link #digraphSite()}. */
    public static final int A = 1;
    /** The arrow t : V -> A of {@link #digraphSite()}; it restricts to tails. */
    public static final int TAIL = 2;
    /** The arrow h : V -> A of {@link #digraphSite()}; it restricts to heads. */
    public static final int HEAD = 3;

    private final String[] objectNames;
    private final String[] arrowNames;
    private final int[] dom;
    private final int[] cod;
    private final int[] identity;
    private final int[][] compose;
    private final int[] moduli;
    private final int[][] tables;

    private FiniteCategory(String[] objectNames, int[] dom, int[] cod, String[] arrowNames,
                           int[] identity, int[][] compose, int[] moduli, int[][] tables) {
        this.objectNames = objectNames.clone();
        this.dom = dom.clone();
        this.cod = cod.clone();
        this.arrowNames = arrowNames.clone();
        this.identity = identity.clone();
        this.compose = new int[compose.length][];
        for (int i = 0; i < compose.length; i++) {
            this.compose[i] = compose[i].clone();
        }
        this.moduli = moduli;
        this.tables = tables;
        check();
    }

    /**
     * A finite category from its table.
     *
     * @param objectNames one name per object
     * @param dom the domain of each arrow
     * @param cod the codomain of each arrow
     * @param arrowNames one name per arrow
     * @param identity the identity arrow of each object
     * @param compose compose[g][f] = g after f, or -1 when not composable
     * @throws IllegalArgumentException if the data break a category law
     */
    public static FiniteCategory of(String[] objectNames, int[] dom, int[] cod, String[] arrowNames,
                                    int[] identity, int[][] compose) {
        return new FiniteCategory(objectNames, dom, cod, arrowNames, identity, compose, null, null);
    }

    private void check() {
        int k = objectNames.length;
        int n = dom.length;
        if (cod.length != n || arrowNames.length != n || compose.length != n || identity.length != k) {
            throw new IllegalArgumentException("sizes do not match");
        }
        for (int f = 0; f < n; f++) {
            if (dom[f] < 0 || dom[f] >= k || cod[f] < 0 || cod[f] >= k || compose[f].length != n) {
                throw new IllegalArgumentException("arrow " + f + " has a bad end or table row");
            }
        }
        for (int x = 0; x < k; x++) {
            int i = identity[x];
            if (i < 0 || i >= n || dom[i] != x || cod[i] != x) {
                throw new IllegalArgumentException("identity of " + objectNames[x] + " is not an endomorphism");
            }
        }
        for (int g = 0; g < n; g++) {
            for (int f = 0; f < n; f++) {
                int gf = compose[g][f];
                if (cod[f] != dom[g]) {
                    if (gf != -1) {
                        throw new IllegalArgumentException(arrowNames[g] + " after " + arrowNames[f] + " is defined but not composable");
                    }
                    continue;
                }
                if (gf < 0 || gf >= n || dom[gf] != dom[f] || cod[gf] != cod[g]) {
                    throw new IllegalArgumentException(arrowNames[g] + " after " + arrowNames[f] + " has the wrong ends");
                }
            }
        }
        for (int f = 0; f < n; f++) {
            if (compose[identity[cod[f]]][f] != f || compose[f][identity[dom[f]]] != f) {
                throw new IllegalArgumentException("an identity is not neutral for " + arrowNames[f]);
            }
        }
        for (int h = 0; h < n; h++) {
            for (int g = 0; g < n; g++) {
                if (cod[g] != dom[h]) {
                    continue;
                }
                for (int f = 0; f < n; f++) {
                    if (cod[f] != dom[g]) {
                        continue;
                    }
                    if (compose[compose[h][g]][f] != compose[h][compose[g][f]]) {
                        throw new IllegalArgumentException("composition is not associative at "
                                                           + arrowNames[h] + ", " + arrowNames[g] + ", " + arrowNames[f]);
                    }
                }
            }
        }
    }

    /**
     * The full subcategory of Mod on the Z-modules Z_n for the given n
     * (n = 1 is the zero module) with all affine maps. The arrows are found
     * by testing every function Z_m -> Z_n for affineness, so their number
     * is not an input.
     */
    public static FiniteCategory affineCyclic(int... moduli) {
        return build(moduli, false);
    }

    /** The one-object category of affine bijections of Z_n, a group. */
    public static FiniteCategory affineAutomorphisms(int n) {
        return build(new int[] { n }, true);
    }

    private static FiniteCategory build(int[] moduli, boolean bijectionsOnly) {
        if (moduli.length == 0) {
            throw new IllegalArgumentException("no modules");
        }
        for (int m : moduli) {
            if (m < 1 || m > 12) {
                throw new IllegalArgumentException("modulus out of range 1..12: " + m);
            }
        }
        int k = moduli.length;
        List<Integer> doms = new ArrayList<Integer>();
        List<Integer> cods = new ArrayList<Integer>();
        List<int[]> tabs = new ArrayList<int[]>();
        for (int x = 0; x < k; x++) {
            for (int y = 0; y < k; y++) {
                for (int[] t : affineTables(moduli[x], moduli[y])) {
                    if (bijectionsOnly && !isBijection(t, moduli[y])) {
                        continue;
                    }
                    doms.add(x);
                    cods.add(y);
                    tabs.add(t);
                }
            }
        }
        int n = tabs.size();
        int[] dom = new int[n];
        int[] cod = new int[n];
        int[][] tables = new int[n][];
        String[] arrowNames = new String[n];
        String[] objectNames = new String[k];
        for (int x = 0; x < k; x++) {
            objectNames[x] = "Z" + moduli[x];
        }
        for (int f = 0; f < n; f++) {
            dom[f] = doms.get(f);
            cod[f] = cods.get(f);
            tables[f] = tabs.get(f);
            arrowNames[f] = formula(tables[f], moduli[dom[f]], moduli[cod[f]])
                            + " : " + objectNames[dom[f]] + " -> " + objectNames[cod[f]];
        }
        int[] identity = new int[k];
        for (int x = 0; x < k; x++) {
            int[] id = new int[moduli[x]];
            for (int i = 0; i < id.length; i++) {
                id[i] = i;
            }
            identity[x] = indexOf(dom, cod, tables, x, x, id);
        }
        int[][] compose = new int[n][n];
        for (int g = 0; g < n; g++) {
            for (int f = 0; f < n; f++) {
                if (cod[f] != dom[g]) {
                    compose[g][f] = -1;
                    continue;
                }
                int[] gf = new int[tables[f].length];
                for (int i = 0; i < gf.length; i++) {
                    gf[i] = tables[g][tables[f][i]];
                }
                compose[g][f] = indexOf(dom, cod, tables, dom[f], cod[g], gf);
                if (compose[g][f] < 0) {
                    throw new IllegalStateException("affine maps not closed under composition");
                }
            }
        }
        return new FiniteCategory(objectNames, dom, cod, arrowNames, identity, compose, moduli.clone(), tables);
    }

    /** Every affine map Z_m -> Z_n as a value table, by testing all n^m functions. */
    private static List<int[]> affineTables(int m, int n) {
        List<int[]> out = new ArrayList<int[]>();
        int[] t = new int[m];
        long total = 1;
        for (int i = 0; i < m; i++) {
            total *= n;
        }
        for (long code = 0; code < total; code++) {
            long c = code;
            for (int i = 0; i < m; i++) {
                t[i] = (int) (c % n);
                c /= n;
            }
            if (isAffine(t, m, n)) {
                out.add(t.clone());
            }
        }
        return out;
    }

    private static boolean isAffine(int[] t, int m, int n) {
        int b = t[0];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < m; y++) {
                int lhs = Math.floorMod(t[(x + y) % m] - b, n);
                int rhs = Math.floorMod(t[x] - b + t[y] - b, n);
                if (lhs != rhs) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isBijection(int[] t, int n) {
        if (t.length != n) {
            return false;
        }
        boolean[] seen = new boolean[n];
        for (int v : t) {
            if (seen[v]) {
                return false;
            }
            seen[v] = true;
        }
        return true;
    }

    /** "ax+b" with a = f(1) - f(0) and b = f(0), dropping zero and unit terms. */
    private static String formula(int[] t, int m, int n) {
        int b = t[0];
        int a = m >= 2 ? Math.floorMod(t[1] - t[0], n) : 0;
        if (a == 0) {
            return Integer.toString(b);
        }
        String lin = a == 1 ? "x" : a + "x";
        return b == 0 ? lin : lin + "+" + b;
    }

    private static int indexOf(int[] dom, int[] cod, int[][] tables, int x, int y, int[] t) {
        for (int f = 0; f < tables.length; f++) {
            if (dom[f] == x && cod[f] == y && Arrays.equals(tables[f], t)) {
                return f;
            }
        }
        return -1;
    }

    /**
     * The site of digraphs: objects V and A, their identities and the two
     * arrows t, h : V -> A (Topos IV appendix J.1, pp. 1457-1460). A
     * presheaf on it is a digraph: its value at A holds the arrows and its
     * restrictions along t and h give tails and heads.
     */
    public static FiniteCategory digraphSite() {
        return of(new String[] { "V", "A" },
                  new int[] { V, A, V, V },
                  new int[] { V, A, A, A },
                  new String[] { "1_V", "1_A", "t", "h" },
                  new int[] { 0, 1 },
                  new int[][] {
                      { 0, -1, -1, -1 },
                      { -1, 1, 2, 3 },
                      { 2, -1, -1, -1 },
                      { 3, -1, -1, -1 },
                  });
    }

    public int objectCount() {
        return objectNames.length;
    }

    public String objectName(int o) {
        return objectNames[o];
    }

    public int arrowCount() {
        return dom.length;
    }

    public String arrowName(int f) {
        return arrowNames[f];
    }

    public int dom(int f) {
        return dom[f];
    }

    public int cod(int f) {
        return cod[f];
    }

    public int identity(int o) {
        return identity[o];
    }

    /**
     * The arrow g after f.
     *
     * @throws IllegalArgumentException if the codomain of f is not the domain of g
     */
    public int compose(int g, int f) {
        int gf = compose[g][f];
        if (gf < 0) {
            throw new IllegalArgumentException(arrowNames[g] + " after " + arrowNames[f] + " is not composable");
        }
        return gf;
    }

    /** The arrows x -> y, in index order. */
    public List<Integer> hom(int x, int y) {
        List<Integer> out = new ArrayList<Integer>();
        for (int f = 0; f < dom.length; f++) {
            if (dom[f] == x && cod[f] == y) {
                out.add(f);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** The arrows with codomain x, in index order. */
    public List<Integer> arrowsInto(int x) {
        List<Integer> out = new ArrayList<Integer>();
        for (int f = 0; f < dom.length; f++) {
            if (cod[f] == x) {
                out.add(f);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** The arrows with domain x, in index order. */
    public List<Integer> arrowsFrom(int x) {
        List<Integer> out = new ArrayList<Integer>();
        for (int f = 0; f < dom.length; f++) {
            if (dom[f] == x) {
                out.add(f);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * The arrow x -> y with the given value table, for categories built by
     * {@link #affineCyclic} or {@link #affineAutomorphisms}; null if there
     * is none or the category has no tables.
     */
    public Integer find(int x, int y, int[] table) {
        if (tables == null) {
            return null;
        }
        int f = indexOf(dom, cod, tables, x, y, table);
        return f < 0 ? null : Integer.valueOf(f);
    }

    /** The value table of an arrow of an affine category, or null. */
    public int[] table(int f) {
        return tables == null ? null : tables[f].clone();
    }

    /**
     * The opposite category: the same objects and arrows, each arrow with
     * its ends swapped, and g after f in the opposite equal to f after g
     * here. Value tables are not carried over.
     */
    public FiniteCategory opposite() {
        int n = dom.length;
        int[][] op = new int[n][n];
        for (int g = 0; g < n; g++) {
            for (int f = 0; f < n; f++) {
                op[g][f] = compose[f][g];
            }
        }
        return of(objectNames, cod, dom, arrowNames, identity, op);
    }

    /**
     * True iff the only endomorphisms and the only isomorphisms are the
     * identities (arXiv 2611.32655, Remark 4.14). It is enough to check
     * that no non-identity arrow is an endomorphism and that no composite
     * of two non-identity arrows is an identity.
     */
    public boolean isAcyclic() {
        for (int f = 0; f < dom.length; f++) {
            if (f != identity[dom[f]] && dom[f] == cod[f]) {
                return false;
            }
        }
        for (int g = 0; g < dom.length; g++) {
            for (int f = 0; f < dom.length; f++) {
                int gf = compose[g][f];
                if (gf >= 0 && f != identity[dom[f]] && g != identity[dom[g]] && gf == identity[dom[gf]]) {
                    return false;
                }
            }
        }
        return true;
    }

    /** True iff every arrow has an inverse. */
    public boolean isGroupoid() {
        for (int f = 0; f < dom.length; f++) {
            boolean inverse = false;
            for (int g : hom(cod[f], dom[f])) {
                if (compose[g][f] == identity[dom[f]] && compose[f][g] == identity[cod[f]]) {
                    inverse = true;
                    break;
                }
            }
            if (!inverse) {
                return false;
            }
        }
        return true;
    }

    /** The lowest-numbered terminal object, or -1 if there is none. */
    public int terminalObject() {
        for (int t = 0; t < objectNames.length; t++) {
            boolean terminal = true;
            for (int x = 0; x < objectNames.length; x++) {
                if (hom(x, t).size() != 1) {
                    terminal = false;
                    break;
                }
            }
            if (terminal) {
                return t;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "FiniteCategory" + Arrays.toString(objectNames) + " with " + dom.length + " arrows";
    }
}
