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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The topos of presheaves on a finite address category: a finite stand-in
 * for Mod@ (Mazzola, The Topos of Music, vol. I, p. 54 and p. 138; vol. IV,
 * appendix G.3, pp. 1428-1434). The book gives no finite example, so this
 * class is a departure (sources/FINITE-MODAT-SPEC.md, D1). It computes:
 * <ul>
 * <li>the subobject classifier Omega: Omega(X) holds the sieves on X, the
 *     sets of arrows into X closed under precomposition, and f : Y -> X
 *     sends a sieve S to f*(S) = {g : f after g in S} (Example 127 and
 *     p. 1430); true picks the maximal sieve;</li>
 * <li>characteristic maps chi_S(X)(x) = {f : x.f in S} and pullbacks of
 *     true (Def. 169);</li>
 * <li>all subpresheaves and all natural transformations of small
 *     presheaves, by enumeration;</li>
 * <li>negation on Omega and the test for a Boolean topos (Prop. 124,
 *     Def. 177, Topos I p. 335).</li>
 * </ul>
 * Enumerations stop with an IllegalStateException once a result passes the
 * limit given to the constructor (D9).
 */
public final class FiniteModAt {

    /** The default limit on enumerated results. */
    public static final int DEFAULT_LIMIT = 100000;

    private final FiniteCategory category;
    private final int limit;
    private final List<List<BitSet>> sieves;
    private final List<Map<BitSet, Integer>> sieveIndex;
    private final FinitePresheaf omega;

    public FiniteModAt(FiniteCategory category) {
        this(category, DEFAULT_LIMIT);
    }

    /**
     * @param category the address category
     * @param limit the most results an enumeration may return
     */
    public FiniteModAt(FiniteCategory category, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.category = category;
        this.limit = limit;
        this.sieves = new ArrayList<List<BitSet>>();
        this.sieveIndex = new ArrayList<Map<BitSet, Integer>>();
        for (int x = 0; x < category.objectCount(); x++) {
            List<BitSet> list = enumerateSieves(x);
            Map<BitSet, Integer> index = new HashMap<BitSet, Integer>();
            for (int i = 0; i < list.size(); i++) {
                index.put(list.get(i), i);
            }
            sieves.add(Collections.unmodifiableList(list));
            sieveIndex.add(index);
        }
        this.omega = buildOmega();
    }

    public FiniteCategory category() {
        return category;
    }

    /** The principal sieve of f : Y -> X, all f after g. */
    private BitSet principal(int f) {
        BitSet s = new BitSet();
        for (int g : category.arrowsInto(category.dom(f))) {
            s.set(category.compose(f, g));
        }
        return s;
    }

    /**
     * The sieves on x, starting with the empty one: every sieve is a union
     * of principal sieves, so closing under adding one principal sieve at a
     * time reaches them all.
     */
    private List<BitSet> enumerateSieves(int x) {
        List<Integer> into = category.arrowsInto(x);
        List<BitSet> principals = new ArrayList<BitSet>();
        for (int f : into) {
            principals.add(principal(f));
        }
        Set<BitSet> seen = new LinkedHashSet<BitSet>();
        Deque<BitSet> todo = new ArrayDeque<BitSet>();
        BitSet empty = new BitSet();
        seen.add(empty);
        todo.add(empty);
        while (!todo.isEmpty()) {
            BitSet s = todo.poll();
            for (int i = 0; i < into.size(); i++) {
                if (s.get(into.get(i))) {
                    continue;
                }
                BitSet t = (BitSet) s.clone();
                t.or(principals.get(i));
                if (seen.add(t)) {
                    if (seen.size() > limit) {
                        throw new IllegalStateException("more than " + limit + " sieves on " + category.objectName(x));
                    }
                    todo.add(t);
                }
            }
        }
        List<BitSet> out = new ArrayList<BitSet>(seen);
        Collections.sort(out, (a, b) -> {
            int c = Integer.compare(a.cardinality(), b.cardinality());
            if (c != 0) {
                return c;
            }
            for (int i = 0; i < Math.max(a.length(), b.length()); i++) {
                if (a.get(i) != b.get(i)) {
                    return a.get(i) ? -1 : 1;
                }
            }
            return 0;
        });
        return out;
    }

    private FinitePresheaf buildOmega() {
        int k = category.objectCount();
        int[] sizes = new int[k];
        for (int x = 0; x < k; x++) {
            sizes[x] = sieves.get(x).size();
        }
        int[][] restrict = new int[category.arrowCount()][];
        for (int f = 0; f < category.arrowCount(); f++) {
            int x = category.cod(f);
            int y = category.dom(f);
            restrict[f] = new int[sizes[x]];
            for (int s = 0; s < sizes[x]; s++) {
                BitSet sieve = sieves.get(x).get(s);
                BitSet pulled = new BitSet();
                for (int g : category.arrowsInto(y)) {
                    if (sieve.get(category.compose(f, g))) {
                        pulled.set(g);
                    }
                }
                Integer i = sieveIndex.get(y).get(pulled);
                if (i == null) {
                    throw new IllegalStateException("a pullback sieve is missing");
                }
                restrict[f][s] = i;
            }
        }
        return new FinitePresheaf(category, sizes, restrict);
    }

    /** The sieves on x as sets of arrow indices, empty first, maximal last. */
    public List<BitSet> sievesOn(int x) {
        return sieves.get(x);
    }

    /** The index in Omega(x) of the maximal sieve, the value of true. */
    public int trueAt(int x) {
        return sieves.get(x).size() - 1;
    }

    /** The subobject classifier. The same instance on every call. */
    public FinitePresheaf omega() {
        return omega;
    }

    /** false : 1 -> Omega, the empty sieve at each object. */
    public int[] falseArrow() {
        int[] f = new int[category.objectCount()];
        for (int x = 0; x < f.length; x++) {
            f[x] = sieveIndex.get(x).get(new BitSet());
        }
        return f;
    }

    /**
     * Negation on Omega from Prop. 125 (iv) applied to sieves: not S holds
     * the arrows g with g after h outside S for every h.
     */
    public int[][] negation() {
        int k = category.objectCount();
        int[][] neg = new int[k][];
        for (int x = 0; x < k; x++) {
            List<BitSet> list = sieves.get(x);
            neg[x] = new int[list.size()];
            for (int s = 0; s < list.size(); s++) {
                BitSet sieve = list.get(s);
                BitSet out = new BitSet();
                for (int g : category.arrowsInto(x)) {
                    boolean ok = true;
                    for (int h : category.arrowsInto(category.dom(g))) {
                        if (sieve.get(category.compose(g, h))) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        out.set(g);
                    }
                }
                neg[x][s] = sieveIndex.get(x).get(out);
            }
        }
        return neg;
    }

    /** Every subpresheaf of p, the empty one first. */
    public List<Subpresheaf> subobjects(FinitePresheaf p) {
        FiniteCategory c = p.category();
        List<int[]> elements = new ArrayList<int[]>();
        for (int x = 0; x < c.objectCount(); x++) {
            for (int e = 0; e < p.size(x); e++) {
                elements.add(new int[] { x, e });
            }
        }
        List<boolean[][]> principals = new ArrayList<boolean[][]>();
        for (int[] el : elements) {
            boolean[][] in = Subpresheaf.empty(p);
            in[el[0]][el[1]] = true;
            Deque<int[]> todo = new ArrayDeque<int[]>();
            todo.add(el);
            Subpresheaf.close(p, in, todo);
            principals.add(in);
        }
        Set<Subpresheaf> seen = new LinkedHashSet<Subpresheaf>();
        Deque<Subpresheaf> todo = new ArrayDeque<Subpresheaf>();
        Subpresheaf bottom = Subpresheaf.bottom(p);
        seen.add(bottom);
        todo.add(bottom);
        while (!todo.isEmpty()) {
            Subpresheaf s = todo.poll();
            for (int i = 0; i < elements.size(); i++) {
                int[] el = elements.get(i);
                if (s.contains(el[0], el[1])) {
                    continue;
                }
                boolean[][] in = Subpresheaf.empty(p);
                boolean[][] pr = principals.get(i);
                for (int x = 0; x < in.length; x++) {
                    for (int e = 0; e < in[x].length; e++) {
                        in[x][e] = s.contains(x, e) || pr[x][e];
                    }
                }
                Subpresheaf t = new Subpresheaf(p, in);
                if (seen.add(t)) {
                    if (seen.size() > limit) {
                        throw new IllegalStateException("more than " + limit + " subobjects");
                    }
                    todo.add(t);
                }
            }
        }
        return new ArrayList<Subpresheaf>(seen);
    }

    /** chi_S(X)(x) = {f : Y -> X with x.f in S}, as indices into Omega. */
    public int[][] characteristic(Subpresheaf s) {
        FinitePresheaf p = s.ambient();
        int k = category.objectCount();
        int[][] chi = new int[k][];
        for (int x = 0; x < k; x++) {
            chi[x] = new int[p.size(x)];
            for (int e = 0; e < p.size(x); e++) {
                BitSet sieve = new BitSet();
                for (int f : category.arrowsInto(x)) {
                    if (s.contains(category.dom(f), p.restrict(f, e))) {
                        sieve.set(f);
                    }
                }
                chi[x][e] = sieveIndex.get(x).get(sieve);
            }
        }
        return chi;
    }

    /** The elements chi sends to true, a subpresheaf when chi is natural. */
    public Subpresheaf pullbackOfTrue(FinitePresheaf p, int[][] chi) {
        boolean[][] in = Subpresheaf.empty(p);
        for (int x = 0; x < in.length; x++) {
            for (int e = 0; e < in[x].length; e++) {
                in[x][e] = chi[x][e] == trueAt(x);
            }
        }
        return new Subpresheaf(p, in);
    }

    /** Whether m (m[x][e] in q at x) commutes with every restriction. */
    public boolean isNatural(FinitePresheaf p, FinitePresheaf q, int[][] m) {
        FiniteCategory c = p.category();
        for (int f = 0; f < c.arrowCount(); f++) {
            int x = c.cod(f);
            int y = c.dom(f);
            for (int e = 0; e < p.size(x); e++) {
                if (m[y][p.restrict(f, e)] != q.restrict(f, m[x][e])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Every natural transformation p -> q, found by backtracking over the
     * elements of p; each assignment is checked against the restrictions
     * between elements already assigned.
     */
    public List<int[][]> naturalTransformations(FinitePresheaf p, FinitePresheaf q) {
        FiniteCategory c = p.category();
        int k = c.objectCount();
        List<int[]> elements = new ArrayList<int[]>();
        Map<Long, Integer> order = new HashMap<Long, Integer>();
        for (int x = 0; x < k; x++) {
            for (int e = 0; e < p.size(x); e++) {
                order.put(((long) x << 32) | e, elements.size());
                elements.add(new int[] { x, e });
            }
        }
        // constraints[i]: {j, f, iIsSource}: element i = elements[j].f or elements[j] = element i .f
        List<List<int[]>> constraints = new ArrayList<List<int[]>>();
        for (int i = 0; i < elements.size(); i++) {
            constraints.add(new ArrayList<int[]>());
        }
        for (int f = 0; f < c.arrowCount(); f++) {
            int x = c.cod(f);
            int y = c.dom(f);
            for (int e = 0; e < p.size(x); e++) {
                int a = order.get(((long) x << 32) | e);
                int b = order.get(((long) y << 32) | p.restrict(f, e));
                int later = Math.max(a, b);
                constraints.get(later).add(new int[] { a, b, f });
            }
        }
        int[][] m = new int[k][];
        for (int x = 0; x < k; x++) {
            m[x] = new int[p.size(x)];
        }
        List<int[][]> out = new ArrayList<int[][]>();
        search(0, elements, constraints, q, m, out);
        return out;
    }

    private void search(int i, List<int[]> elements, List<List<int[]>> constraints,
                        FinitePresheaf q, int[][] m, List<int[][]> out) {
        if (i == elements.size()) {
            if (out.size() >= limit) {
                throw new IllegalStateException("more than " + limit + " natural transformations");
            }
            int[][] copy = new int[m.length][];
            for (int x = 0; x < m.length; x++) {
                copy[x] = m[x].clone();
            }
            out.add(copy);
            return;
        }
        int[] el = elements.get(i);
        for (int v = 0; v < q.size(el[0]); v++) {
            m[el[0]][el[1]] = v;
            boolean ok = true;
            for (int[] con : constraints.get(i)) {
                int[] a = elements.get(con[0]);
                int[] b = elements.get(con[1]);
                if (m[b[0]][b[1]] != q.restrict(con[2], m[a[0]][a[1]])) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                search(i + 1, elements, constraints, q, m, out);
            }
        }
    }

    /** The global elements 1 -> Omega, each given by its value at every object. */
    public List<int[]> globalTruthValues() {
        List<int[]> out = new ArrayList<int[]>();
        for (int[][] n : naturalTransformations(FinitePresheaf.terminal(category), omega)) {
            int[] v = new int[n.length];
            for (int x = 0; x < n.length; x++) {
                v[x] = n[x][0];
            }
            out.add(v);
        }
        return out;
    }

    /** Whether not not S = S for every subpresheaf S of p (Prop. 124). */
    public boolean isBoolean(FinitePresheaf p) {
        for (Subpresheaf s : subobjects(p)) {
            if (!s.negation().negation().equals(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the topos is Boolean: not not = id on Omega(X) for every X,
     * that is, Sub(@X) is Boolean for every representable (D6).
     */
    public boolean isBooleanTopos() {
        int[][] neg = negation();
        for (int x = 0; x < neg.length; x++) {
            for (int s = 0; s < neg[x].length; s++) {
                if (neg[x][neg[x][s]] != s) {
                    return false;
                }
            }
        }
        return true;
    }
}
