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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rubato.math.matrix.ZnMatrix;
import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.ZnElement;
import org.rubato.math.module.ZnProperFreeElement;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.ZnFreeAffineMorphism;

/**
 * A zero-addressed objective local composition in the free module
 * Z<sub>n</sub><sup>k</sup>: a finite set of points (Mazzola, <i>The Topos of
 * Music</i>, &sect;7, Def. 8, p. 90).
 * <p>
 * Points are kept as sorted integer coordinate vectors with entries in
 * [0, n). {@link #fromModuleElements} and {@link #toModuleElements} convert
 * to and from Rubato's {@link ZnElement} and {@link ZnProperFreeElement}.
 * {@link #morphism(int[], LocalComposition)} decides Def. 23 (p. 129): a set
 * map between two compositions is a morphism if it is the restriction of
 * an affine map of the ambient modules, which is a module morphism in
 * Rubato's sense ({@link AffineMap#toMorphism()}). Instances are immutable.
 */
public final class LocalComposition {

    private final int modulus;
    private final int dim;
    private final List<int[]> points;

    /**
     * Creates the composition of the given points in Z_n^k; duplicates are
     * removed and the points are sorted.
     *
     * @throws IllegalArgumentException if n &lt; 2, k &lt; 1 or a point has the wrong length
     */
    public LocalComposition(int modulus, int dim, Collection<int[]> pts) {
        if (modulus < 2) {
            throw new IllegalArgumentException("Modulus must be at least 2, got " + modulus);
        }
        if (dim < 1) {
            throw new IllegalArgumentException("Dimension must be at least 1, got " + dim);
        }
        this.modulus = modulus;
        this.dim = dim;
        List<int[]> ps = new ArrayList<int[]>();
        for (int[] p : pts) {
            if (p.length != dim) {
                throw new IllegalArgumentException("Point " + Arrays.toString(p) + " is not in Z_" + modulus + "^" + dim);
            }
            int[] q = new int[dim];
            for (int i = 0; i < dim; i++) {
                q[i] = Math.floorMod(p[i], modulus);
            }
            boolean dup = false;
            for (int[] r : ps) {
                if (Arrays.equals(r, q)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                ps.add(q);
            }
        }
        Collections.sort(ps, LEX);
        this.points = Collections.unmodifiableList(ps);
    }

    /** A pitch-class set or other subset of Z_n, as a composition in Z_n^1. */
    public static LocalComposition ofClasses(int modulus, int... values) {
        List<int[]> ps = new ArrayList<int[]>();
        for (int v : values) {
            ps.add(new int[] { v });
        }
        return new LocalComposition(modulus, 1, ps);
    }

    /**
     * Converts Rubato module elements of Z_n or Z_n^k (all of one modulus
     * and dimension) into a composition.
     *
     * @throws IllegalArgumentException for an element of another kind
     */
    public static LocalComposition fromModuleElements(Collection<? extends ModuleElement> elements) {
        int n = -1, k = -1;
        List<int[]> ps = new ArrayList<int[]>();
        for (ModuleElement e : elements) {
            int[] v;
            int m;
            if (e instanceof ZnElement) {
                v = new int[] { ((ZnElement) e).getValue() };
                m = ((ZnElement) e).getModulus();
            }
            else if (e instanceof ZnProperFreeElement) {
                v = ((ZnProperFreeElement) e).getValue().clone();
                m = ((ZnProperFreeElement) e).getModulus();
            }
            else {
                throw new IllegalArgumentException("Only elements of Z_n and Z_n^k are supported, got " + e);
            }
            if (n < 0) {
                n = m;
                k = v.length;
            }
            else if (n != m || k != v.length) {
                throw new IllegalArgumentException("Elements of Z_" + n + "^" + k + " and Z_" + m + "^" + v.length + " mixed");
            }
            ps.add(v);
        }
        if (n < 0) {
            throw new IllegalArgumentException("A local composition needs at least one point here");
        }
        return new LocalComposition(n, k, ps);
    }

    /** The points as Rubato module elements, in the composition's order. */
    public List<ModuleElement> toModuleElements() {
        List<ModuleElement> out = new ArrayList<ModuleElement>();
        for (int[] p : points) {
            out.add(ZnProperFreeElement.make(p.clone(), modulus));
        }
        return out;
    }

    public int getModulus() { return modulus; }

    public int getDimension() { return dim; }

    /** The number of points. */
    public int size() { return points.size(); }

    /** Point i, a fresh copy. */
    public int[] point(int i) { return points.get(i).clone(); }

    /** The index of a point, or &minus;1. */
    public int indexOf(int... p) {
        for (int i = 0; i < points.size(); i++) {
            if (Arrays.equals(points.get(i), normalize(p))) {
                return i;
            }
        }
        return -1;
    }

    private int[] normalize(int[] p) {
        int[] q = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            q[i] = Math.floorMod(p[i], modulus);
        }
        return q;
    }

    /** For a composition in Z_n^1, its values in increasing order. */
    public int[] classes() {
        if (dim != 1) {
            throw new IllegalStateException("Not a subset of Z_n");
        }
        int[] v = new int[points.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = points.get(i)[0];
        }
        return v;
    }

    /** The subcomposition on the points with the given indexes. */
    public LocalComposition sub(Collection<Integer> indexes) {
        List<int[]> ps = new ArrayList<int[]>();
        for (int i : indexes) {
            ps.add(points.get(i));
        }
        return new LocalComposition(modulus, dim, ps);
    }

    /**
     * An affine map Z_n^k &rarr; Z_n^l, x &#8614; M&middot;x + b. Immutable.
     */
    public static final class AffineMap {
        public final int modulus;
        private final int[][] m;
        private final int[] b;

        AffineMap(int modulus, int[][] m, int[] b) {
            this.modulus = modulus;
            this.m = m;
            this.b = b;
        }

        public int[] apply(int[] x) {
            int[] y = new int[b.length];
            for (int i = 0; i < b.length; i++) {
                long s = b[i];
                for (int j = 0; j < x.length; j++) {
                    s += (long) m[i][j] * x[j];
                }
                y[i] = (int) Math.floorMod(s, (long) modulus);
            }
            return y;
        }

        /** The same map as a Rubato module morphism Z_n^k &rarr; Z_n^l. */
        public ModuleMorphism toMorphism() {
            return ZnFreeAffineMorphism.make(new ZnMatrix(m, modulus), b.clone());
        }

        public String toString() {
            return "x -> " + Arrays.deepToString(m) + " x + " + Arrays.toString(b) + " (mod " + modulus + ")";
        }
    }

    /**
     * Decides whether the set map f (point i of this composition to point
     * f[i] of target) is a morphism of local compositions (Def. 23, p. 129)
     * with the identity address change, and returns an underlying affine map
     * if so, null otherwise. Both compositions must have the same modulus.
     */
    public AffineMap morphism(int[] f, LocalComposition target) {
        if (target.modulus != modulus) {
            throw new IllegalArgumentException("Moduli differ: " + modulus + " and " + target.modulus);
        }
        if (f.length != points.size()) {
            throw new IllegalArgumentException("The map has " + f.length + " values for " + points.size() + " points");
        }
        int r = points.size();
        int l = target.dim;
        // unknowns: one row of M and one entry of b per output coordinate
        long[][] a = new long[r][dim + 1];
        for (int j = 0; j < r; j++) {
            for (int i = 0; i < dim; i++) {
                a[j][i] = points.get(j)[i];
            }
            a[j][dim] = 1;
        }
        int[][] mm = new int[l][dim];
        int[] bb = new int[l];
        for (int c = 0; c < l; c++) {
            long[] y = new long[r];
            for (int j = 0; j < r; j++) {
                y[j] = target.points.get(f[j])[c];
            }
            long[] z = IntMatrices.solveMod(a, y, modulus);
            if (z == null) {
                return null;
            }
            for (int i = 0; i < dim; i++) {
                mm[c][i] = (int) z[i];
            }
            bb[c] = (int) z[dim];
        }
        AffineMap h = new AffineMap(modulus, mm, bb);
        for (int j = 0; j < r; j++) {
            if (!Arrays.equals(h.apply(points.get(j)), target.points.get(f[j]))) {
                throw new IllegalStateException("Solver returned a map that does not restrict to f: " + h);
            }
        }
        return h;
    }

    /** Returns true if f is a morphism (Def. 23, p. 129). */
    public boolean isMorphism(int[] f, LocalComposition target) {
        return morphism(f, target) != null;
    }

    /**
     * Returns true if f is an isomorphism in the category ObLoc (Def. 26,
     * p. 132): a bijection that is a morphism with an inverse morphism.
     */
    public boolean isIsomorphism(int[] f, LocalComposition target) {
        if (target.size() != size() || f.length != size()) {
            return false;
        }
        int[] inv = new int[size()];
        Arrays.fill(inv, -1);
        for (int i = 0; i < f.length; i++) {
            if (f[i] < 0 || f[i] >= size() || inv[f[i]] >= 0) {
                return false;
            }
            inv[f[i]] = i;
        }
        return isMorphism(f, target) && target.isMorphism(inv, this);
    }

    /**
     * Returns true if some bijection between the two compositions is an
     * isomorphism. Brute force over all bijections, so at most 8 points.
     */
    public boolean isIsomorphicTo(LocalComposition other) {
        return findIsomorphism(other) != null;
    }

    /** An isomorphism onto other, as a point map, or null. At most 8 points. */
    public int[] findIsomorphism(LocalComposition other) {
        if (other.size() != size() || other.modulus != modulus) {
            return null;
        }
        if (size() > 8) {
            throw new IllegalArgumentException("Isomorphism search is brute force; " + size() + " points is too many");
        }
        int[] p = new int[size()];
        for (int i = 0; i < p.length; i++) {
            p[i] = i;
        }
        do {
            if (isIsomorphism(p, other)) {
                return p.clone();
            }
        } while (nextPermutation(p));
        return null;
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

    static final Comparator<int[]> LEX = (x, y) -> {
        for (int i = 0; i < Math.min(x.length, y.length); i++) {
            if (x[i] != y[i]) {
                return x[i] - y[i];
            }
        }
        return x.length - y.length;
    };

    public boolean equals(Object o) {
        if (!(o instanceof LocalComposition)) {
            return false;
        }
        LocalComposition c = (LocalComposition) o;
        if (c.modulus != modulus || c.dim != dim || c.points.size() != points.size()) {
            return false;
        }
        for (int i = 0; i < points.size(); i++) {
            if (!Arrays.equals(points.get(i), c.points.get(i))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = modulus * 31 + dim;
        for (int[] p : points) {
            h = h * 31 + Arrays.hashCode(p);
        }
        return h;
    }

    public String toString() {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                b.append(", ");
            }
            int[] p = points.get(i);
            b.append(dim == 1 ? Integer.toString(p[0]) : Arrays.toString(p));
        }
        return b.append("} in Z_").append(modulus).append(dim == 1 ? "" : "^" + dim).toString();
    }
}
