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
package org.rubato.math.performance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Algorithmic extraction of performance fields (Mazzola, <i>The Topos of Music II</i>,
 * &sect;47.3, pp. 759-765, by Stefan M&uuml;ller). From a score and a matched performance,
 * given as points of R<sup>n</sup>, it computes a discrete performance field on the score
 * notes:
 * <ol>
 * <li>a basis U<sub>X</sub> of difference vectors N - X of nearby notes, chosen by the least
 * w<sub>loc</sub> |det U<sub>X</sub>| + w<sub>orth</sub> / |det U<sub>X</sub><sup>Norm</sup>|
 * (&sect;47.3.4.1, pp. 762-763);</li>
 * <li>its image V<sub>X</sub> under the performance and the field vector
 * Z(X) = U<sub>X</sub> V<sub>X</sub><sup>-1</sup> &Delta; = det(V<sub>X</sub>)<sup>-1</sup>
 * U<sub>X</sub> Adj(V<sub>X</sub>) &Delta; with &Delta; = (1, ..., 1), in the three cases of
 * p. 762: regular, direction only, no information;</li>
 * <li>a C<sup>1</sup> interpolation of the vectors on chosen axes, recursive over the last
 * axis with zero-slope cubics and a frame of diagonal vectors (&sect;47.3.5.1 p. 764,
 * &sect;32.3.2.1 pp. 553-554).</li>
 * </ol>
 * Matching is only the book's trivial case (p. 761). Departures are numbered as in
 * sources/FIELD-EXTRACTION-SPEC.md: D2 matching, D3 distance, D4 candidates, D5 and D6
 * constructed basis vectors, D7 tolerances, D8 the direction case, D9 interpolation.
 */
public final class FieldExtraction {

    /** The three cases of p. 762. */
    public enum Case {
        /** V<sub>X</sub> is regular; the field vector is defined. */
        REGULAR,
        /** det V<sub>X</sub> = 0 and Adj(V<sub>X</sub>) &ne; 0: only the direction is given. */
        DIRECTION,
        /** det V<sub>X</sub> = 0 and Adj(V<sub>X</sub>) = 0: no information. */
        NONE
    }

    /** Parameters of the basis search and of the case decision. */
    public static final class Settings {
        private final double maxDist;
        private final int maxNeighbors;
        private final double wLoc;
        private final double wOrth;
        private final double tolerance;
        private final double regularBasis;

        private Settings(double maxDist, int maxNeighbors, double wLoc, double wOrth, double tolerance,
                         double regularBasis) {
            this.maxDist = maxDist;
            this.maxNeighbors = maxNeighbors;
            this.wLoc = wLoc;
            this.wOrth = wOrth;
            this.tolerance = tolerance;
            this.regularBasis = regularBasis;
        }

        /**
         * No distance limit, the 8 nearest notes, both weights 1, case tolerance 1e-9, and a
         * candidate basis counted regular when |det U<sup>Norm</sup>| &gt; 1e-6 (D3, D7).
         */
        public static Settings defaults() {
            return new Settings(Double.POSITIVE_INFINITY, 8, 1, 1, 1e-9, 1e-6);
        }

        /** Notes farther than d from X are not neighbours (the book's maxDist, p. 763). */
        public Settings withMaxDist(double d) {
            if (!(d > 0)) {
                throw new IllegalArgumentException("the largest neighbour distance must be positive: " + d);
            }
            return new Settings(d, maxNeighbors, wLoc, wOrth, tolerance, regularBasis);
        }

        /** At most k nearest notes are neighbours (D3). */
        public Settings withMaxNeighbors(int k) {
            if (k < 1) {
                throw new IllegalArgumentException("the number of neighbours must be at least 1: " + k);
            }
            return new Settings(maxDist, k, wLoc, wOrth, tolerance, regularBasis);
        }

        /** The weights w<sub>loc</sub> and w<sub>orth</sub> of p. 763, both positive. */
        public Settings withWeights(double loc, double orth) {
            if (!(loc > 0) || !(orth > 0) || Double.isInfinite(loc) || Double.isInfinite(orth)) {
                throw new IllegalArgumentException("the weights must be positive and finite: " + loc + ", " + orth);
            }
            return new Settings(maxDist, maxNeighbors, loc, orth, tolerance, regularBasis);
        }

        /** The relative tolerance of D7 for the case decision. */
        public Settings withTolerance(double tol) {
            if (!(tol > 0) || !(tol < 1)) {
                throw new IllegalArgumentException("the tolerance must lie in ]0, 1[: " + tol);
            }
            return new Settings(maxDist, maxNeighbors, wLoc, wOrth, tol, regularBasis);
        }

        public double getMaxDist() { return maxDist; }

        public int getMaxNeighbors() { return maxNeighbors; }

        public double getLocalityWeight() { return wLoc; }

        public double getOrthogonalityWeight() { return wOrth; }

        public double getTolerance() { return tolerance; }
    }

    /** A basis U<sub>X</sub> at a note. */
    public static final class Basis {
        private final int[] neighbors;
        private final double[][] matrix;
        private final double cost;
        private final int constructed;

        Basis(int[] neighbors, double[][] matrix, double cost, int constructed) {
            this.neighbors = neighbors;
            this.matrix = matrix;
            this.cost = cost;
            this.constructed = constructed;
        }

        /** For each column, the index of the note N with column N - X, or -1 for a constructed column. */
        public int[] neighbors() { return neighbors.clone(); }

        /** U<sub>X</sub>, row-major, the basis vectors as columns. */
        public double[][] matrix() { return copy(matrix); }

        /** w<sub>loc</sub> |det U| + w<sub>orth</sub> / |det U<sup>Norm</sup>|. */
        public double cost() { return cost; }

        /** The number of constructed columns (D5). */
        public int constructed() { return constructed; }
    }

    /** The outcome of the field formula for one basis and its image. */
    public static final class Local {
        private final Case kase;
        private final double[] value;

        Local(Case kase, double[] value) {
            this.kase = kase;
            this.value = value;
        }

        public Case getCase() { return kase; }

        /** Z(X) when regular, the unit direction when only the direction is known, else null. */
        public double[] value() { return value == null ? null : value.clone(); }
    }

    /** The field at one score note. */
    public static final class Vector {
        /** The note's index in the score. */
        public final int index;
        private final Basis basis;
        private final Local local;

        Vector(int index, Basis basis, Local local) {
            this.index = index;
            this.basis = basis;
            this.local = local;
        }

        public Case getCase() { return local.getCase(); }

        /** As {@link Local#value()}. */
        public double[] value() { return local.value(); }

        public Basis basis() { return basis; }
    }

    /** The discrete field of a whole score. */
    public static final class Result {
        private final List<Vector> vectors;

        Result(List<Vector> vectors) {
            this.vectors = Collections.unmodifiableList(vectors);
        }

        /** One vector per score note, in score order. */
        public List<Vector> vectors() { return vectors; }

        public int count(Case c) {
            int k = 0;
            for (Vector v : vectors) {
                if (v.getCase() == c) {
                    k++;
                }
            }
            return k;
        }

        /** The number of notes whose basis has a constructed column. */
        public int constructedNotes() {
            int k = 0;
            for (Vector v : vectors) {
                if (v.basis().constructed() > 0) {
                    k++;
                }
            }
            return k;
        }
    }

    private FieldExtraction() { /* static utility */ }

    // ------------------------------------------------------------------
    // Linear algebra

    /** The determinant by Gaussian elimination with partial pivoting. */
    public static double det(double[][] m) {
        int n = m.length;
        double[][] a = copy(m);
        double d = 1;
        for (int c = 0; c < n; c++) {
            int p = c;
            for (int r = c + 1; r < n; r++) {
                if (Math.abs(a[r][c]) > Math.abs(a[p][c])) {
                    p = r;
                }
            }
            if (a[p][c] == 0) {
                return 0;
            }
            if (p != c) {
                double[] t = a[c];
                a[c] = a[p];
                a[p] = t;
                d = -d;
            }
            d *= a[c][c];
            for (int r = c + 1; r < n; r++) {
                double f = a[r][c] / a[c][c];
                for (int k = c; k < n; k++) {
                    a[r][k] -= f * a[c][k];
                }
            }
        }
        return d;
    }

    /** The adjugate, Adj(M)<sub>ij</sub> = (-1)<sup>i+j</sup> times the minor of M without row j and column i. */
    public static double[][] adjugate(double[][] m) {
        int n = m.length;
        double[][] adj = new double[n][n];
        if (n == 1) {
            adj[0][0] = 1;
            return adj;
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double[][] minor = new double[n - 1][n - 1];
                for (int r = 0, rr = 0; r < n; r++) {
                    if (r == j) {
                        continue;
                    }
                    for (int c = 0, cc = 0; c < n; c++) {
                        if (c == i) {
                            continue;
                        }
                        minor[rr][cc++] = m[r][c];
                    }
                    rr++;
                }
                adj[i][j] = ((i + j) % 2 == 0 ? 1 : -1) * det(minor);
            }
        }
        return adj;
    }

    /**
     * The field formula of p. 762 for a basis U and its image V (both row-major, columns as
     * vectors). Regular when |det V| &gt; tol times the product of V's column lengths; then
     * Z = U y with V y = &Delta;. Otherwise the direction of U Adj(V) &Delta; when some entry of
     * Adj(V) exceeds tol times its Hadamard bound and that vector is not 0 (D7, D8); else no
     * information.
     */
    public static Local field(double[][] u, double[][] v, double tol) {
        int n = v.length;
        double[] c = new double[n];
        double all = 1;
        for (int k = 0; k < n; k++) {
            double s = 0;
            for (int i = 0; i < n; i++) {
                s += v[i][k] * v[i][k];
            }
            c[k] = Math.sqrt(s);
            all *= c[k];
        }
        double d = det(v);
        if (all > 0 && Math.abs(d) > tol * all) {
            double[][] a = new double[n][n + 1];
            for (int i = 0; i < n; i++) {
                System.arraycopy(v[i], 0, a[i], 0, n);
                a[i][n] = 1;
            }
            return new Local(Case.REGULAR, apply(u, solve(a)));
        }
        double[][] adj = adjugate(v);
        boolean nonzero = false;
        double boundSum = 0;
        for (int i = 0; i < n; i++) {
            double bound = 1;
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    bound *= c[k];
                }
            }
            boundSum += bound;
            for (int j = 0; j < n; j++) {
                if (bound > 0 && Math.abs(adj[i][j]) > tol * bound) {
                    nonzero = true;
                }
            }
        }
        if (!nonzero) {
            return new Local(Case.NONE, null);
        }
        double[] w = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                w[i] += adj[i][j];
            }
        }
        double[] z = apply(u, w);
        double uMax = 0;
        for (int k = 0; k < n; k++) {
            double s = 0;
            for (int i = 0; i < n; i++) {
                s += u[i][k] * u[i][k];
            }
            uMax = Math.max(uMax, Math.sqrt(s));
        }
        double len = norm(z);
        if (!(len > tol * boundSum * uMax * Math.sqrt(n))) {
            return new Local(Case.NONE, null);
        }
        for (int i = 0; i < n; i++) {
            z[i] /= len;
        }
        return new Local(Case.DIRECTION, z);
    }

    // ------------------------------------------------------------------
    // Bases

    /**
     * The basis at note i (&sect;47.3.4.1): the neighbours are the other notes at Euclidean
     * distance in ]0, maxDist], nearest first (ties by index), at most maxNeighbors (D3); every
     * n-subset of their difference vectors is a candidate (D4); a candidate with
     * |det U<sup>Norm</sup>| above 1e-6 is regular, and the least cost wins, the first in
     * candidate order on ties. Without a regular candidate the basis is constructed (D5).
     */
    public static Basis basis(double[][] points, int i, Settings s) {
        double[] x = points[i];
        int n = x.length;
        List<double[]> near = new ArrayList<double[]>();
        for (int j = 0; j < points.length; j++) {
            if (j == i) {
                continue;
            }
            double d = norm(subtract(points[j], x));
            if (d > 0 && d <= s.maxDist) {
                near.add(new double[] { d, j });
            }
        }
        Collections.sort(near, (p, q) -> p[0] != q[0] ? Double.compare(p[0], q[0]) : Double.compare(p[1], q[1]));
        if (near.size() > s.maxNeighbors) {
            near = new ArrayList<double[]>(near.subList(0, s.maxNeighbors));
        }
        int m = near.size();
        int[] idx = new int[m];
        double[][] diff = new double[m][];
        for (int k = 0; k < m; k++) {
            idx[k] = (int) near.get(k)[1];
            diff[k] = subtract(points[idx[k]], x);
        }
        Basis best = null;
        if (m >= n) {
            int[] comb = new int[n];
            for (int k = 0; k < n; k++) {
                comb[k] = k;
            }
            while (true) {
                double[][] u = columns(diff, comb, n);
                double du = det(u);
                double prod = 1;
                for (int k = 0; k < n; k++) {
                    prod *= norm(diff[comb[k]]);
                }
                double dn = du / prod;
                if (Math.abs(dn) > s.regularBasis) {
                    double cost = s.wLoc * Math.abs(du) + s.wOrth / Math.abs(dn);
                    if (best == null || cost < best.cost) {
                        int[] nb = new int[n];
                        for (int k = 0; k < n; k++) {
                            nb[k] = idx[comb[k]];
                        }
                        best = new Basis(nb, u, cost, 0);
                    }
                }
                if (!next(comb, m)) {
                    break;
                }
            }
        }
        return best != null ? best : construct(idx, diff, n, s);
    }

    /**
     * D5: neighbour vectors nearest first while each adds rank, then unit coordinate vectors
     * orthogonalized against them, largest residual first, scaled to length 1.
     */
    private static Basis construct(int[] idx, double[][] diff, int n, Settings s) {
        List<double[]> cols = new ArrayList<double[]>();
        List<double[]> ortho = new ArrayList<double[]>();
        List<Integer> nb = new ArrayList<Integer>();
        for (int k = 0; k < diff.length && cols.size() < n; k++) {
            double[] r = residual(diff[k], ortho);
            double len = norm(r);
            if (len > s.regularBasis * norm(diff[k])) {
                cols.add(diff[k]);
                ortho.add(scale(r, 1 / len));
                nb.add(idx[k]);
            }
        }
        int kept = cols.size();
        while (cols.size() < n) {
            double[] bestR = null;
            double bestLen = -1;
            for (int j = 0; j < n; j++) {
                double[] e = new double[n];
                e[j] = 1;
                double[] r = residual(e, ortho);
                double len = norm(r);
                if (len > bestLen + 1e-15) {
                    bestLen = len;
                    bestR = r;
                }
            }
            double[] q = scale(bestR, 1 / bestLen);
            cols.add(q);
            ortho.add(q);
            nb.add(-1);
        }
        double[][] u = new double[n][n];
        int[] nbs = new int[n];
        double prod = 1;
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                u[i][k] = cols.get(k)[i];
            }
            nbs[k] = nb.get(k);
            prod *= norm(cols.get(k));
        }
        double du = det(u);
        double cost = s.wLoc * Math.abs(du) + s.wOrth / Math.abs(du / prod);
        return new Basis(nbs, u, cost, n - kept);
    }

    // ------------------------------------------------------------------
    // Extraction

    /**
     * The discrete field of a matched score and performance: point i of the performance is
     * the image of point i of the score. A constructed column has itself as image (D6).
     */
    public static Result extract(double[][] score, double[][] performed, Settings s) {
        check(score, performed);
        List<Vector> out = new ArrayList<Vector>();
        int n = score[0].length;
        for (int i = 0; i < score.length; i++) {
            Basis b = basis(score, i, s);
            double[][] v = new double[n][n];
            for (int k = 0; k < n; k++) {
                int j = b.neighbors[k];
                for (int r = 0; r < n; r++) {
                    v[r][k] = j >= 0 ? performed[j][r] - performed[i][r] : b.matrix[r][k];
                }
            }
            out.add(new Vector(i, b, field(b.matrix, v, s.tolerance)));
        }
        return new Result(out);
    }

    private static void check(double[][] score, double[][] performed) {
        if (score.length == 0) {
            throw new IllegalArgumentException("there are no notes");
        }
        if (score.length != performed.length) {
            throw new IllegalArgumentException("the score and the performance need the same number of notes: "
                    + score.length + " and " + performed.length);
        }
        int n = score[0].length;
        if (n == 0) {
            throw new IllegalArgumentException("the points have dimension 0");
        }
        for (int i = 0; i < score.length; i++) {
            if (score[i].length != n || performed[i].length != n) {
                throw new IllegalArgumentException("note " + i + " has dimension " + score[i].length + " in the score and "
                        + performed[i].length + " in the performance, not " + n);
            }
            for (int k = 0; k < n; k++) {
                if (!Double.isFinite(score[i][k]) || !Double.isFinite(performed[i][k])) {
                    throw new IllegalArgumentException("note " + i + " has a coordinate that is not finite");
                }
            }
        }
    }

    /**
     * The trivial matching of p. 761 (D2): every note is played exactly once. Notes of one
     * pitch are paired in onset order (then duration, then index). Returns, for each score
     * note, the index of its performed note.
     *
     * @throws IllegalArgumentException if a pitch has different note counts
     */
    public static int[] matchByPitch(double[][] score, double[][] performed, int pitch, int onset, int duration) {
        Map<Double, List<Integer>> s = group(score, pitch), p = group(performed, pitch);
        for (Map.Entry<Double, List<Integer>> e : s.entrySet()) {
            List<Integer> q = p.get(e.getKey());
            int k = q == null ? 0 : q.size();
            if (k != e.getValue().size()) {
                throw new IllegalArgumentException("pitch " + number(e.getKey()) + " has " + e.getValue().size()
                        + " notes in the score and " + k + " in the performance; matching needs every note"
                        + " played exactly once");
            }
        }
        for (Map.Entry<Double, List<Integer>> e : p.entrySet()) {
            if (!s.containsKey(e.getKey())) {
                throw new IllegalArgumentException("pitch " + number(e.getKey()) + " has 0 notes in the score and "
                        + e.getValue().size() + " in the performance; matching needs every note played exactly once");
            }
        }
        int[] m = new int[score.length];
        for (Map.Entry<Double, List<Integer>> e : s.entrySet()) {
            List<Integer> a = sorted(e.getValue(), score, onset, duration);
            List<Integer> b = sorted(p.get(e.getKey()), performed, onset, duration);
            for (int k = 0; k < a.size(); k++) {
                m[a.get(k)] = b.get(k);
            }
        }
        return m;
    }

    private static Map<Double, List<Integer>> group(double[][] pts, int pitch) {
        Map<Double, List<Integer>> g = new TreeMap<Double, List<Integer>>();
        for (int i = 0; i < pts.length; i++) {
            g.computeIfAbsent(pts[i][pitch], k -> new ArrayList<Integer>()).add(i);
        }
        return g;
    }

    private static List<Integer> sorted(List<Integer> ix, final double[][] pts, final int onset, final int duration) {
        List<Integer> l = new ArrayList<Integer>(ix);
        Collections.sort(l, (a, b) -> {
            int c = Double.compare(pts[a][onset], pts[b][onset]);
            if (c == 0) {
                c = Double.compare(pts[a][duration], pts[b][duration]);
            }
            return c != 0 ? c : Integer.compare(a, b);
        });
        return l;
    }

    private static String number(double x) {
        return x == Math.rint(x) ? String.valueOf((long) x) : String.format(Locale.ROOT, "%s", x);
    }

    // ------------------------------------------------------------------
    // Interpolation

    /**
     * The interpolated field of &sect;47.3.5.1 (p. 764) after &sect;32.3.2.1 (pp. 553-554):
     * hyperplanes perpendicular to the last axis through every point, the recursively
     * interpolated values on the two neighbouring hyperplanes joined by the cubic
     * f<sub>a</sub> + (f<sub>b</sub> - f<sub>a</sub>) s<sup>2</sup>(3 - 2s), and the diagonal
     * vector (1, ..., 1) on and outside a frame one margin beyond the points' bounding box (D9).
     * Points that coincide are averaged (D9).
     */
    public static final class Interpolation {
        private final int dim;
        private final int components;
        private final double[] low;
        private final double[] high;
        private final Node root;
        private final int coincident;

        private static final class Node {
            double[] knots;
            Node[] children;
            double[] value;
        }

        public Interpolation(double[][] points, double[][] values, double margin) {
            if (points.length == 0) {
                throw new IllegalArgumentException("there are no points to interpolate");
            }
            if (points.length != values.length) {
                throw new IllegalArgumentException("points and values need the same number of entries: " + points.length
                        + " and " + values.length);
            }
            if (!(margin > 0) || Double.isInfinite(margin)) {
                throw new IllegalArgumentException("the frame margin must be positive and finite: " + margin);
            }
            dim = points[0].length;
            components = values[0].length;
            if (dim == 0 || components == 0) {
                throw new IllegalArgumentException("points and values need dimension at least 1");
            }
            for (int i = 0; i < points.length; i++) {
                if (points[i].length != dim || values[i].length != components) {
                    throw new IllegalArgumentException("entry " + i + " has dimension " + points[i].length + " and "
                            + values[i].length + " components, not " + dim + " and " + components);
                }
            }
            Map<String, double[]> sums = new TreeMap<String, double[]>();
            Map<String, double[]> where = new TreeMap<String, double[]>();
            Map<String, Integer> counts = new TreeMap<String, Integer>();
            for (int i = 0; i < points.length; i++) {
                String key = Arrays.toString(points[i]);
                double[] sum = sums.computeIfAbsent(key, k -> new double[components]);
                for (int c = 0; c < components; c++) {
                    sum[c] += values[i][c];
                }
                where.put(key, points[i]);
                counts.merge(key, 1, Integer::sum);
            }
            List<double[]> pts = new ArrayList<double[]>();
            List<double[]> vals = new ArrayList<double[]>();
            for (String key : sums.keySet()) {
                double[] sum = sums.get(key);
                int k = counts.get(key);
                for (int c = 0; c < components; c++) {
                    sum[c] /= k;
                }
                pts.add(where.get(key));
                vals.add(sum);
            }
            coincident = points.length - pts.size();
            low = new double[dim];
            high = new double[dim];
            for (int c = 0; c < dim; c++) {
                double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
                for (double[] p : pts) {
                    lo = Math.min(lo, p[c]);
                    hi = Math.max(hi, p[c]);
                }
                low[c] = lo - margin;
                high[c] = hi + margin;
            }
            List<Integer> all = new ArrayList<Integer>();
            for (int i = 0; i < pts.size(); i++) {
                all.add(i);
            }
            root = build(pts, vals, all, dim);
        }

        private static Node build(List<double[]> pts, List<double[]> vals, List<Integer> ix, int m) {
            Node node = new Node();
            if (m == 0) {
                node.value = vals.get(ix.get(0));
                return node;
            }
            int c = m - 1;
            TreeMap<Double, List<Integer>> g = new TreeMap<Double, List<Integer>>();
            for (int i : ix) {
                g.computeIfAbsent(pts.get(i)[c], k -> new ArrayList<Integer>()).add(i);
            }
            node.knots = new double[g.size()];
            node.children = new Node[g.size()];
            int k = 0;
            for (Map.Entry<Double, List<Integer>> e : g.entrySet()) {
                node.knots[k] = e.getKey();
                node.children[k] = build(pts, vals, e.getValue(), m - 1);
                k++;
            }
            return node;
        }

        /** The interpolated vector at x. */
        public double[] at(double[] x) {
            if (x.length != dim) {
                throw new IllegalArgumentException("the point has dimension " + x.length + ", not " + dim);
            }
            return eval(root, dim, x).clone();
        }

        private double[] ones() {
            double[] d = new double[components];
            Arrays.fill(d, 1);
            return d;
        }

        private double[] eval(Node node, int m, double[] x) {
            if (m == 0) {
                return node.value;
            }
            int c = m - 1;
            double t = x[c];
            if (t <= low[c] || t >= high[c]) {
                return ones();
            }
            double[] ks = node.knots;
            double a, b;
            double[] fa, fb;
            if (t < ks[0]) {
                a = low[c];
                b = ks[0];
                fa = ones();
                fb = eval(node.children[0], m - 1, x);
            }
            else if (t >= ks[ks.length - 1]) {
                a = ks[ks.length - 1];
                b = high[c];
                fa = eval(node.children[ks.length - 1], m - 1, x);
                fb = ones();
            }
            else {
                int i = Arrays.binarySearch(ks, t);
                if (i < 0) {
                    i = -i - 2;
                }
                a = ks[i];
                b = ks[i + 1];
                fa = eval(node.children[i], m - 1, x);
                fb = eval(node.children[i + 1], m - 1, x);
            }
            double s = (t - a) / (b - a);
            double h = s * s * (3 - 2 * s);
            double[] out = new double[components];
            for (int k = 0; k < components; k++) {
                out[k] = fa[k] + (fb[k] - fa[k]) * h;
            }
            return out;
        }

        /** The frame's lower corner: the points' minima less the margin. */
        public double[] frameLow() { return low.clone(); }

        /** The frame's upper corner: the points' maxima plus the margin. */
        public double[] frameHigh() { return high.clone(); }

        /** How many points coincided with an earlier one and were averaged in. */
        public int coincident() { return coincident; }
    }

    // ------------------------------------------------------------------
    // Helpers

    private static boolean next(int[] comb, int m) {
        int n = comb.length;
        int i = n - 1;
        while (i >= 0 && comb[i] == m - n + i) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        comb[i]++;
        for (int j = i + 1; j < n; j++) {
            comb[j] = comb[j - 1] + 1;
        }
        return true;
    }

    private static double[][] columns(double[][] vecs, int[] comb, int n) {
        double[][] u = new double[n][n];
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                u[i][k] = vecs[comb[k]][i];
            }
        }
        return u;
    }

    private static double[] residual(double[] v, List<double[]> ortho) {
        double[] r = v.clone();
        for (double[] q : ortho) {
            double d = 0;
            for (int i = 0; i < r.length; i++) {
                d += r[i] * q[i];
            }
            for (int i = 0; i < r.length; i++) {
                r[i] -= d * q[i];
            }
        }
        return r;
    }

    private static double[] scale(double[] v, double f) {
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = v[i] * f;
        }
        return r;
    }

    private static double[] subtract(double[] a, double[] b) {
        double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i] - b[i];
        }
        return r;
    }

    private static double norm(double[] v) {
        double s = 0;
        for (double x : v) {
            s += x * x;
        }
        return Math.sqrt(s);
    }

    private static double[] apply(double[][] a, double[] x) {
        double[] y = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < x.length; j++) {
                y[i] += a[i][j] * x[j];
            }
        }
        return y;
    }

    private static double[][] copy(double[][] m) {
        double[][] c = new double[m.length][];
        for (int i = 0; i < m.length; i++) {
            c[i] = m[i].clone();
        }
        return c;
    }

    /** Solves the augmented n x (n+1) system in place by partial pivoting. */
    private static double[] solve(double[][] a) {
        int n = a.length;
        for (int c = 0; c < n; c++) {
            int p = c;
            for (int r = c + 1; r < n; r++) {
                if (Math.abs(a[r][c]) > Math.abs(a[p][c])) {
                    p = r;
                }
            }
            double[] t = a[c];
            a[c] = a[p];
            a[p] = t;
            for (int r = c + 1; r < n; r++) {
                double f = a[r][c] / a[c][c];
                for (int k = c; k <= n; k++) {
                    a[r][k] -= f * a[c][k];
                }
            }
        }
        double[] z = new double[n];
        for (int r = n - 1; r >= 0; r--) {
            double s = a[r][n];
            for (int k = r + 1; k < n; k++) {
                s -= a[r][k] * z[k];
            }
            z[r] = s / a[r][r];
        }
        return z;
    }
}
