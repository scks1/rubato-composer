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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A finite abstract simplicial complex on labelled vertices 0 &hellip; v&minus;1
 * (Mazzola, <i>The Topos of Music</i>, App. H.2.1, p. 1445): a set of
 * non-empty vertex sets, the simplexes, that holds every singleton and every
 * non-empty subset of a simplex.
 * <p>
 * A simplex is stored as a bit mask over at most 62 vertices. The class
 * computes face counts, the Euler characteristic, integral simplicial
 * homology, boundaries and orientability of pseudomanifolds, simplicial
 * maps, contiguity (App. H.2.3, p. 1447) and automorphisms. Homology and
 * orientability are standard topology applied to the nerve; the book's
 * classification uses other invariants (see the package documentation).
 * Instances are immutable.
 */
public final class SimplicialComplex {

    /** The largest number of vertices a complex can have. */
    public static final int MAX_VERTICES = 62;

    private final List<String> labels;
    private final Set<Long> simplexes;
    private final int dimension;

    private SimplicialComplex(List<String> labels, Set<Long> simplexes) {
        this.labels = Collections.unmodifiableList(new ArrayList<String>(labels));
        this.simplexes = Collections.unmodifiableSet(simplexes);
        int d = -1;
        for (long s : simplexes) {
            d = Math.max(d, Long.bitCount(s) - 1);
        }
        this.dimension = d;
    }

    /**
     * Creates the smallest complex on the given vertices that contains the
     * listed simplexes (each a set of vertex indexes) and every vertex.
     *
     * @throws IllegalArgumentException for more than {@link #MAX_VERTICES}
     *         vertices, an empty simplex or an index out of range
     */
    public static SimplicialComplex generatedBy(List<String> labels, List<int[]> simplexes) {
        int n = labels.size();
        if (n > MAX_VERTICES) {
            throw new IllegalArgumentException(n + " vertices; at most " + MAX_VERTICES + " are supported");
        }
        Set<Long> all = new HashSet<Long>();
        for (int i = 0; i < n; i++) {
            all.add(1L << i);
        }
        for (int[] s : simplexes) {
            long m = mask(s, n);
            addFaces(m, all);
        }
        return new SimplicialComplex(labels, all);
    }

    static SimplicialComplex ofMasks(List<String> labels, Set<Long> closedSimplexes) {
        return new SimplicialComplex(labels, new HashSet<Long>(closedSimplexes));
    }

    private static long mask(int[] s, int n) {
        if (s.length == 0) {
            throw new IllegalArgumentException("A simplex must not be empty");
        }
        long m = 0;
        for (int v : s) {
            if (v < 0 || v >= n) {
                throw new IllegalArgumentException("Vertex " + v + " is out of range 0.." + (n - 1));
            }
            m |= 1L << v;
        }
        return m;
    }

    private static void addFaces(long m, Set<Long> into) {
        // enumerate the non-empty submasks of m
        for (long sub = m; sub != 0; sub = (sub - 1) & m) {
            into.add(sub);
        }
    }

    /** The number of vertices. */
    public int vertexCount() {
        return labels.size();
    }

    /** The label of vertex i. */
    public String label(int i) {
        return labels.get(i);
    }

    /** The vertex labels in index order. */
    public List<String> labels() {
        return labels;
    }

    /** The dimension, the largest simplex size minus one (&minus;1 if empty). */
    public int dimension() {
        return dimension;
    }

    /** Returns true if the vertex set is a simplex. */
    public boolean contains(int... vertexes) {
        return simplexes.contains(mask(vertexes, labels.size()));
    }

    /** The total number of simplexes. */
    public int size() {
        return simplexes.size();
    }

    /** The q-simplexes as sorted vertex arrays, in lexicographic order. */
    public List<int[]> simplexes(int q) {
        List<int[]> out = new ArrayList<int[]>();
        for (long s : sortedMasks()) {
            if (Long.bitCount(s) == q + 1) {
                out.add(vertices(s));
            }
        }
        return out;
    }

    /** All simplexes as sorted vertex arrays, by dimension, then lexicographically. */
    public List<int[]> allSimplexes() {
        List<int[]> out = new ArrayList<int[]>();
        for (int q = 0; q <= dimension; q++) {
            out.addAll(simplexes(q));
        }
        return out;
    }

    /** The simplexes that are not proper faces of another simplex. */
    public List<int[]> maximalSimplexes() {
        List<int[]> out = new ArrayList<int[]>();
        for (int q = dimension; q >= 0; q--) {
            for (int[] s : simplexes(q)) {
                long m = mask(s, labels.size());
                boolean maximal = true;
                for (int v = 0; v < labels.size() && maximal; v++) {
                    if ((m & (1L << v)) == 0 && simplexes.contains(m | (1L << v))) {
                        maximal = false;
                    }
                }
                if (maximal) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    /** The face counts f_0, f_1, ..., f_dim. */
    public int[] fVector() {
        int[] f = new int[dimension + 1];
        for (long s : simplexes) {
            f[Long.bitCount(s) - 1]++;
        }
        return f;
    }

    /** The Euler characteristic, the alternating sum of the face counts. */
    public int eulerCharacteristic() {
        int chi = 0;
        int[] f = fVector();
        for (int q = 0; q < f.length; q++) {
            chi += (q % 2 == 0 ? 1 : -1) * f[q];
        }
        return chi;
    }

    /** The integral homology group H_q as a rank and a list of torsion coefficients. */
    public static final class HomologyGroup {
        public final int rank;
        public final List<Long> torsion;

        HomologyGroup(int rank, List<Long> torsion) {
            this.rank = rank;
            this.torsion = Collections.unmodifiableList(torsion);
        }

        public boolean isFree() {
            return torsion.isEmpty();
        }

        public String toString() {
            StringBuilder b = new StringBuilder();
            if (rank == 0 && torsion.isEmpty()) {
                return "0";
            }
            if (rank > 0) {
                b.append(rank == 1 ? "Z" : "Z^" + rank);
            }
            for (long t : torsion) {
                if (b.length() > 0) {
                    b.append(" + ");
                }
                b.append("Z/").append(t);
            }
            return b.toString();
        }
    }

    /**
     * The integral simplicial homology H_0 &hellip; H_dim, from the Smith normal
     * forms of the boundary matrices.
     */
    public List<HomologyGroup> homology() {
        List<HomologyGroup> out = new ArrayList<HomologyGroup>();
        if (dimension < 0) {
            return out;
        }
        List<List<int[]>> byDim = new ArrayList<List<int[]>>();
        List<Map<Long, Integer>> index = new ArrayList<Map<Long, Integer>>();
        for (int q = 0; q <= dimension; q++) {
            List<int[]> sq = simplexes(q);
            byDim.add(sq);
            Map<Long, Integer> idx = new HashMap<Long, Integer>();
            for (int i = 0; i < sq.size(); i++) {
                idx.put(mask(sq.get(i), labels.size()), i);
            }
            index.add(idx);
        }
        // diag[q] = Smith diagonal of the boundary C_q -> C_{q-1}
        long[][] diag = new long[dimension + 2][];
        diag[0] = new long[0];
        diag[dimension + 1] = new long[0];
        for (int q = 1; q <= dimension; q++) {
            List<int[]> cols = byDim.get(q);
            long[][] m = new long[byDim.get(q - 1).size()][cols.size()];
            for (int j = 0; j < cols.size(); j++) {
                int[] s = cols.get(j);
                for (int t = 0; t < s.length; t++) {
                    int[] face = remove(s, t);
                    m[index.get(q - 1).get(mask(face, labels.size()))][j] += (t % 2 == 0) ? 1 : -1;
                }
            }
            diag[q] = IntMatrices.smith(m).diagonal;
        }
        for (int q = 0; q <= dimension; q++) {
            int rank = byDim.get(q).size() - diag[q].length - diag[q + 1].length;
            List<Long> torsion = new ArrayList<Long>();
            for (long d : diag[q + 1]) {
                if (d > 1) {
                    torsion.add(d);
                }
            }
            out.add(new HomologyGroup(rank, torsion));
        }
        return out;
    }

    /** The Betti numbers b_0 &hellip; b_dim. */
    public int[] bettiNumbers() {
        List<HomologyGroup> h = homology();
        int[] b = new int[h.size()];
        for (int q = 0; q < b.length; q++) {
            b[q] = h.get(q).rank;
        }
        return b;
    }

    /**
     * Returns true if the complex is a pure d-dimensional pseudomanifold
     * with boundary: every simplex is a face of a d-simplex, and every
     * (d&minus;1)-simplex is a face of at most two d-simplexes.
     */
    public boolean isPseudomanifold() {
        if (dimension < 1) {
            return false;
        }
        Map<Long, Integer> cofaces = facetCounts();
        for (int c : cofaces.values()) {
            if (c > 2) {
                return false;
            }
        }
        for (int[] s : maximalSimplexes()) {
            if (s.length != dimension + 1) {
                return false;
            }
        }
        return true;
    }

    /** For each (d&minus;1)-simplex, the number of d-simplexes it lies in. */
    private Map<Long, Integer> facetCounts() {
        Map<Long, Integer> counts = new HashMap<Long, Integer>();
        for (int[] s : simplexes(dimension - 1)) {
            counts.put(mask(s, labels.size()), 0);
        }
        for (int[] s : simplexes(dimension)) {
            for (int t = 0; t < s.length; t++) {
                long f = mask(remove(s, t), labels.size());
                counts.put(f, counts.get(f) + 1);
            }
        }
        return counts;
    }

    /**
     * The boundary of a pseudomanifold: the subcomplex generated by the
     * (d&minus;1)-simplexes that lie in exactly one d-simplex.
     *
     * @throws IllegalStateException if the complex is not a pseudomanifold
     */
    public SimplicialComplex boundary() {
        requirePseudomanifold();
        Set<Long> b = new HashSet<Long>();
        for (Map.Entry<Long, Integer> e : facetCounts().entrySet()) {
            if (e.getValue() == 1) {
                addFaces(e.getKey(), b);
            }
        }
        // the boundary keeps all labels; vertices off the boundary are isolated points
        for (int i = 0; i < labels.size(); i++) {
            b.add(1L << i);
        }
        return new SimplicialComplex(labels, b);
    }

    /** The vertices that lie on some simplex of dimension at least 1. */
    public List<Integer> nonIsolatedVertices() {
        List<Integer> out = new ArrayList<Integer>();
        for (int v = 0; v < labels.size(); v++) {
            for (long s : simplexes) {
                if (Long.bitCount(s) > 1 && (s & (1L << v)) != 0) {
                    out.add(v);
                    break;
                }
            }
        }
        return out;
    }

    /**
     * If the edges of this complex form one closed cycle through all its
     * non-isolated vertices, returns that cycle starting at {@code start}
     * and continuing to its smaller-indexed neighbour first; otherwise null.
     */
    public List<Integer> cycle(int start) {
        Map<Integer, List<Integer>> nb = new HashMap<Integer, List<Integer>>();
        for (int[] e : simplexes(1)) {
            add(nb, e[0], e[1]);
            add(nb, e[1], e[0]);
        }
        if (dimension != 1 || !nb.containsKey(start)) {
            return null;
        }
        for (List<Integer> l : nb.values()) {
            if (l.size() != 2) {
                return null;
            }
        }
        List<Integer> out = new ArrayList<Integer>();
        int prev = -1, cur = start;
        do {
            out.add(cur);
            List<Integer> l = nb.get(cur);
            int next = prev < 0 ? Math.min(l.get(0), l.get(1)) : (l.get(0) == prev ? l.get(1) : l.get(0));
            prev = cur;
            cur = next;
        } while (cur != start);
        return out.size() == nb.size() ? out : null;
    }

    private static void add(Map<Integer, List<Integer>> nb, int a, int b) {
        List<Integer> l = nb.get(a);
        if (l == null) {
            l = new ArrayList<Integer>();
            nb.put(a, l);
        }
        l.add(b);
    }

    /**
     * Decides whether a pseudomanifold is orientable: the d-simplexes can be
     * oriented so that every shared (d&minus;1)-face gets opposite induced
     * orientations. Each strongly connected piece is oriented separately.
     *
     * @throws IllegalStateException if the complex is not a pseudomanifold
     */
    public boolean isOrientable() {
        requirePseudomanifold();
        List<int[]> top = simplexes(dimension);
        Map<Long, List<Integer>> byFace = new HashMap<Long, List<Integer>>();
        for (int i = 0; i < top.size(); i++) {
            int[] s = top.get(i);
            for (int t = 0; t < s.length; t++) {
                long f = mask(remove(s, t), labels.size());
                List<Integer> l = byFace.get(f);
                if (l == null) {
                    l = new ArrayList<Integer>();
                    byFace.put(f, l);
                }
                l.add(i);
            }
        }
        int[] sign = new int[top.size()];
        for (int root = 0; root < top.size(); root++) {
            if (sign[root] != 0) {
                continue;
            }
            sign[root] = 1;
            List<Integer> stack = new ArrayList<Integer>();
            stack.add(root);
            while (!stack.isEmpty()) {
                int i = stack.remove(stack.size() - 1);
                int[] s = top.get(i);
                for (int t = 0; t < s.length; t++) {
                    long f = mask(remove(s, t), labels.size());
                    for (int j : byFace.get(f)) {
                        if (j == i) {
                            continue;
                        }
                        int tj = position(top.get(j), f);
                        int want = -sign[i] * parity(t) * parity(tj);
                        if (sign[j] == 0) {
                            sign[j] = want;
                            stack.add(j);
                        }
                        else if (sign[j] != want) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void requirePseudomanifold() {
        if (!isPseudomanifold()) {
            throw new IllegalStateException("The complex is not a pure pseudomanifold; boundary and"
                    + " orientability are only defined here for pseudomanifolds");
        }
    }

    private int position(int[] s, long face) {
        for (int t = 0; t < s.length; t++) {
            if ((face & (1L << s[t])) == 0) {
                return t;
            }
        }
        throw new IllegalStateException("not a face");
    }

    private static int parity(int t) {
        return t % 2 == 0 ? 1 : -1;
    }

    /**
     * Returns true if the vertex map f (f[i] a vertex of target) carries
     * every simplex of this complex to a simplex of target.
     */
    public boolean isSimplicialMap(int[] f, SimplicialComplex target) {
        if (f.length != labels.size()) {
            return false;
        }
        for (int x : f) {
            if (x < 0 || x >= target.vertexCount()) {
                return false;
            }
        }
        for (long s : simplexes) {
            if (!target.simplexes.contains(image(s, f))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the simplicial maps f, g into target are contiguous:
     * f(s) &cup; g(s) is a simplex of target for every simplex s (App. H.2.3,
     * p. 1447).
     */
    public boolean contiguous(int[] f, int[] g, SimplicialComplex target) {
        if (!isSimplicialMap(f, target) || !isSimplicialMap(g, target)) {
            return false;
        }
        for (long s : simplexes) {
            if (!target.simplexes.contains(image(s, f) | image(s, g))) {
                return false;
            }
        }
        return true;
    }

    private static long image(long s, int[] f) {
        long m = 0;
        for (int v = 0; v < f.length; v++) {
            if ((s & (1L << v)) != 0) {
                m |= 1L << f[v];
            }
        }
        return m;
    }

    /**
     * Returns true if the vertex bijection p is an isomorphism onto target:
     * it carries the simplexes of this complex exactly onto those of target.
     */
    public boolean isIsomorphism(int[] p, SimplicialComplex target) {
        if (target.vertexCount() != vertexCount() || target.size() != size()) {
            return false;
        }
        Set<Integer> seen = new HashSet<Integer>();
        for (int x : p) {
            seen.add(x);
        }
        return seen.size() == p.length && isSimplicialMap(p, target);
    }

    /**
     * The automorphisms, as vertex permutations, that also preserve the given
     * weights (one per simplex, keyed by sorted vertex list); weights may be
     * null. Brute force over all permutations, so at most 9 vertices.
     *
     * @throws IllegalArgumentException for more than 9 vertices
     */
    public List<int[]> automorphisms(Map<List<Integer>, ?> weights) {
        int n = labels.size();
        if (n > 9) {
            throw new IllegalArgumentException("Automorphism search is brute force; " + n + " vertices is too many");
        }
        List<int[]> out = new ArrayList<int[]>();
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = i;
        }
        do {
            if (isIsomorphism(p, this) && preserves(p, weights)) {
                out.add(p.clone());
            }
        } while (nextPermutation(p));
        return out;
    }

    private boolean preserves(int[] p, Map<List<Integer>, ?> weights) {
        if (weights == null) {
            return true;
        }
        for (long s : simplexes) {
            Object a = weights.get(asList(vertices(s)));
            Object b = weights.get(asList(vertices(image(s, p))));
            if (a == null ? b != null : !a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    private static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        int j = a.length - 1;
        while (a[j] <= a[i]) {
            j--;
        }
        int t = a[i]; a[i] = a[j]; a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) {
            t = a[l]; a[l] = a[r]; a[r] = t;
        }
        return true;
    }

    /** A sorted vertex array as a list, the key type for weights. */
    public static List<Integer> asList(int[] s) {
        List<Integer> l = new ArrayList<Integer>();
        for (int v : s) {
            l.add(v);
        }
        return l;
    }

    /** A simplex printed with vertex labels, e.g. {I, III, V}. */
    public String format(int[] s) {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < s.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(labels.get(s[i]));
        }
        return b.append('}').toString();
    }

    private List<Long> sortedMasks() {
        List<int[]> arrs = new ArrayList<int[]>();
        for (long s : simplexes) {
            arrs.add(vertices(s));
        }
        Collections.sort(arrs, (x, y) -> {
            if (x.length != y.length) {
                return x.length - y.length;
            }
            for (int i = 0; i < x.length; i++) {
                if (x[i] != y[i]) {
                    return x[i] - y[i];
                }
            }
            return 0;
        });
        List<Long> out = new ArrayList<Long>();
        for (int[] a : arrs) {
            out.add(mask(a, labels.size()));
        }
        return out;
    }

    static int[] vertices(long s) {
        int[] v = new int[Long.bitCount(s)];
        int k = 0;
        for (int i = 0; i < 64 && k < v.length; i++) {
            if ((s & (1L << i)) != 0) {
                v[k++] = i;
            }
        }
        return v;
    }

    private static int[] remove(int[] s, int t) {
        int[] out = new int[s.length - 1];
        for (int i = 0, k = 0; i < s.length; i++) {
            if (i != t) {
                out[k++] = s[i];
            }
        }
        return out;
    }

    public boolean equals(Object o) {
        if (!(o instanceof SimplicialComplex)) {
            return false;
        }
        SimplicialComplex c = (SimplicialComplex) o;
        return labels.equals(c.labels) && simplexes.equals(c.simplexes);
    }

    public int hashCode() {
        return labels.hashCode() * 31 + simplexes.hashCode();
    }

    /** One line per dimension, simplexes listed by label. */
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int q = 0; q <= dimension; q++) {
            List<String> parts = new ArrayList<String>();
            for (int[] s : simplexes(q)) {
                parts.add(format(s));
            }
            b.append(q).append(": ").append(String.join(" ", parts)).append('\n');
        }
        return b.toString();
    }
}
