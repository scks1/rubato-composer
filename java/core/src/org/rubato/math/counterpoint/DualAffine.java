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

package org.rubato.math.counterpoint;

import org.rubato.math.arith.NumberTheory;
import org.rubato.math.matrix.ZnMatrix;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.ZnFreeAffineMorphism;

/**
 * An affine symmetry T^{a+&epsilon;.b} &middot; (u + &epsilon;.v) of the dual
 * numbers Z_n[&epsilon;], with u a unit (CCW section 2.2, pp. 10&ndash;11).
 * It sends the counterpoint interval x + &epsilon;.y to
 * (u&middot;x + a) + &epsilon;.(v&middot;x + u&middot;y + b).
 * <p>
 * {@link DualNumbers} represents Z_n[&epsilon;] as the Rubato module
 * Z_n<sup>2</sup> with coordinates (cantus firmus x, interval y). On that
 * module this symmetry is the affine map with matrix
 * [[u, 0], [v, u]] and translation (a, b), which {@link #toMorphism()}
 * returns. The class keeps the four parameters so that symmetries print in
 * the book's notation and compose without building matrices.
 */
public final class DualAffine {

    private final int a;
    private final int b;
    private final int u;
    private final int v;
    private final int n;

    /**
     * Creates T^{a+&epsilon;.b} &middot; (u + &epsilon;.v) over Z_n[&epsilon;].
     *
     * @throws IllegalArgumentException if u is not a unit mod n
     */
    public DualAffine(int a, int b, int u, int v, int n) {
        if (n < 2) {
            throw new IllegalArgumentException("Modulus must be at least 2, got " + n);
        }
        this.n = n;
        this.a = NumberTheory.mod(a, n);
        this.b = NumberTheory.mod(b, n);
        this.u = NumberTheory.mod(u, n);
        this.v = NumberTheory.mod(v, n);
        if (!ZnAffine.isUnit(this.u, n)) {
            throw new IllegalArgumentException(u + " is not a unit modulo " + n);
        }
    }

    /** The element T^{&epsilon;.t} &middot; (u + &epsilon;.v) of the group H (CCW p. 17). */
    public static DualAffine ofH(int t, int u, int v, int n) {
        return new DualAffine(0, t, u, v, n);
    }

    public int getA() { return a; }

    public int getB() { return b; }

    public int getU() { return u; }

    public int getV() { return v; }

    public int getModulus() { return n; }

    /** True if this symmetry lies in H, that is, a = 0. */
    public boolean isInH() {
        return a == 0;
    }

    /** The cantus firmus coordinate of the image of x + &epsilon;.y. */
    public int applyCantus(int x, int y) {
        return NumberTheory.mod(u * x + a, n);
    }

    /** The interval coordinate of the image of x + &epsilon;.y. */
    public int applyInterval(int x, int y) {
        return NumberTheory.mod(v * x + u * y + b, n);
    }

    /** Returns this &#8728; other. */
    public DualAffine compose(DualAffine o) {
        if (o.n != n) {
            throw new IllegalArgumentException("Moduli differ: " + n + " and " + o.n);
        }
        return new DualAffine(u * o.a + a, u * o.b + v * o.a + b, u * o.u, u * o.v + v * o.u, n);
    }

    /** The same map as a Rubato module morphism Z_n<sup>2</sup> &#8594; Z_n<sup>2</sup>. */
    public ModuleMorphism toMorphism() {
        ZnMatrix m = new ZnMatrix(new int[][] { { u, 0 }, { v, u } }, n);
        return ZnFreeAffineMorphism.make(m, new int[] { a, b });
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DualAffine)) {
            return false;
        }
        DualAffine d = (DualAffine) o;
        return d.a == a && d.b == b && d.u == u && d.v == v && d.n == n;
    }

    @Override
    public int hashCode() {
        return (((a * 31 + b) * 31 + u) * 31 + v) * 31 + n;
    }

    /** The book's notation, for example "T^{ε.8}·(5+ε.4)". */
    @Override
    public String toString() {
        String shift = a == 0 ? "\u03b5." + b : (b == 0 ? Integer.toString(a) : a + "+\u03b5." + b);
        String lin = v == 0 ? Integer.toString(u) : "(" + u + "+\u03b5." + v + ")";
        return "T^{" + shift + "}\u00b7" + lin;
    }
}
