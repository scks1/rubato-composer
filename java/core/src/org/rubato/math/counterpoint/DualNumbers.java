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
import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.ZnFreeModule;
import org.rubato.math.module.ZnProperFreeElement;
import org.rubato.math.module.ZnProperFreeModule;

/**
 * The dual numbers Z_n[&epsilon;] = Z_n[X]/(X<sup>2</sup>) on top of Rubato's
 * existing module classes.
 * <p>
 * A counterpoint interval x + &epsilon;.y has cantus firmus x and interval
 * y (CCW section 2.2, p. 10). As a Z_n-module, Z_n[&epsilon;] is the free
 * module Z_n<sup>2</sup>, so this class uses {@link ZnProperFreeModule} of
 * dimension 2 with coordinates (x, y), and the symmetries of
 * {@link DualAffine} become {@code ZnFreeAffineMorphism}s on it. Only the
 * ring multiplication, (a + &epsilon;.b)(c + &epsilon;.d) = ac + &epsilon;.(ad + bc),
 * is defined here; the module layer has no ring class for it, because
 * {@code ModularPolynomialRing} requires a field of coefficients and Z_12
 * is not one.
 */
public final class DualNumbers {

    private DualNumbers() { /* static utility */ }

    /** The module Z_n<sup>2</sup> that carries Z_n[&epsilon;]. */
    public static ZnFreeModule module(int n) {
        return ZnProperFreeModule.make(2, n);
    }

    /** The counterpoint interval x + &epsilon;.y as an element of {@link #module(int)}. */
    public static ModuleElement interval(int x, int y, int n) {
        return ZnProperFreeElement.make(new int[] { NumberTheory.mod(x, n), NumberTheory.mod(y, n) }, n);
    }

    /** The cantus firmus coordinate x of x + &epsilon;.y. */
    public static int cantus(ModuleElement e) {
        return coordinates(e)[0];
    }

    /** The interval coordinate y of x + &epsilon;.y. */
    public static int interval(ModuleElement e) {
        return coordinates(e)[1];
    }

    /** The product (a + &epsilon;.b)(c + &epsilon;.d) = ac + &epsilon;.(ad + bc). */
    public static ModuleElement product(ModuleElement p, ModuleElement q) {
        int[] s = coordinates(p);
        int[] t = coordinates(q);
        int n = modulus(p);
        if (modulus(q) != n) {
            throw new IllegalArgumentException("Moduli differ: " + n + " and " + modulus(q));
        }
        return interval(NumberTheory.mod(s[0] * t[0], n), NumberTheory.mod(s[0] * t[1] + s[1] * t[0], n), n);
    }

    /** True if a + &epsilon;.b is a unit, that is, a is a unit mod n (CCW p. 10). */
    public static boolean isUnit(ModuleElement e) {
        return ZnAffine.isUnit(cantus(e), modulus(e));
    }

    /** The sweeping discantus x + y, CCW's &alpha;+ (p. 13). */
    public static int sweepingDiscantus(int x, int y, int n) {
        return NumberTheory.mod(x + y, n);
    }

    /** The hanging discantus x &minus; y, CCW's &alpha;&minus; (p. 13). */
    public static int hangingDiscantus(int x, int y, int n) {
        return NumberTheory.mod(x - y, n);
    }

    private static int[] coordinates(ModuleElement e) {
        if (!(e instanceof ZnProperFreeElement) || ((ZnProperFreeElement) e).getLength() != 2) {
            throw new IllegalArgumentException(e + " is not an element of Z_n^2");
        }
        return ((ZnProperFreeElement) e).getValue();
    }

    private static int modulus(ModuleElement e) {
        return ((ZnProperFreeElement) e).getModulus();
    }
}
