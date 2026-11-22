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
 * An affine symmetry T^u &middot; v of Z_n, the map x &#8614; u + v&middot;x
 * with v a unit (Agust&iacute;n-Aquino, Junod, Mazzola, <i>Computational
 * Counterpoint Worlds</i>, 2015, section 2.1, p. 6).
 * <p>
 * The class holds the two parameters so that symmetries can be listed,
 * compared and printed in the book's notation; {@link #toMorphism()} gives
 * the same map as a Rubato {@link ModuleMorphism} on Z_n.
 */
public final class ZnAffine {

    private final int u;
    private final int v;
    private final int n;

    /**
     * Creates T^u &middot; v on Z_n.
     *
     * @throws IllegalArgumentException if n &lt; 2 or v is not a unit mod n
     */
    public ZnAffine(int u, int v, int n) {
        if (n < 2) {
            throw new IllegalArgumentException("Modulus must be at least 2, got " + n);
        }
        this.n = n;
        this.u = NumberTheory.mod(u, n);
        this.v = NumberTheory.mod(v, n);
        if (NumberTheory.gcd(this.v, n) != 1) {
            throw new IllegalArgumentException(v + " is not a unit modulo " + n);
        }
    }

    public int getTranslation() { return u; }

    public int getFactor() { return v; }

    public int getModulus() { return n; }

    /** Returns u + v&middot;x mod n. */
    public int apply(int x) {
        return NumberTheory.mod(u + v * x, n);
    }

    /** Returns this &#8728; other, the map x &#8614; this(other(x)). */
    public ZnAffine compose(ZnAffine other) {
        checkModulus(other.n);
        return new ZnAffine(u + v * other.u, v * other.v, n);
    }

    /** The same map as a Rubato module morphism Z_n &#8594; Z_n. */
    public ModuleMorphism toMorphism() {
        return ZnFreeAffineMorphism.make(new ZnMatrix(new int[][] { { v } }, n), new int[] { u });
    }

    /** Returns true if v is invertible mod n. */
    static boolean isUnit(int v, int n) {
        return NumberTheory.gcd(NumberTheory.mod(v, n), n) == 1;
    }

    private void checkModulus(int m) {
        if (m != n) {
            throw new IllegalArgumentException("Moduli differ: " + n + " and " + m);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZnAffine)) {
            return false;
        }
        ZnAffine a = (ZnAffine) o;
        return a.u == u && a.v == v && a.n == n;
    }

    @Override
    public int hashCode() {
        return (u * 31 + v) * 31 + n;
    }

    /** The book's notation, for example "T^5·11". */
    @Override
    public String toString() {
        return "T^" + u + "\u00b7" + v;
    }
}
