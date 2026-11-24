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

package org.rubato.math.rhythm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.rubato.math.arith.Rational;

/**
 * An exact rational number with arbitrary-size numerator and denominator,
 * kept in lowest terms with a positive denominator. Rhythm-tree lengths
 * multiply along every path, so they outgrow Rubato's int-based
 * {@link Rational}; a value converts to it only when a denotator is built
 * (sources/RHYTHM-TREES-SPEC.md D8).
 */
public final class Fraction implements Comparable<Fraction> {

    public static final Fraction ZERO = new Fraction(BigInteger.ZERO, BigInteger.ONE);
    public static final Fraction ONE = new Fraction(BigInteger.ONE, BigInteger.ONE);

    private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);

    private final BigInteger num;
    private final BigInteger den;

    private Fraction(BigInteger num, BigInteger den) {
        this.num = num;
        this.den = den;
    }

    /** num/den in lowest terms; den must not be 0. */
    public static Fraction of(BigInteger num, BigInteger den) {
        if (den.signum() == 0) {
            throw new IllegalArgumentException("denominator 0");
        }
        if (den.signum() < 0) {
            num = num.negate();
            den = den.negate();
        }
        BigInteger g = num.gcd(den);
        if (g.signum() != 0 && !g.equals(BigInteger.ONE)) {
            num = num.divide(g);
            den = den.divide(g);
        }
        return new Fraction(num, den);
    }

    public static Fraction of(long num, long den) {
        return of(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }

    public static Fraction of(long n) {
        return of(n, 1);
    }

    public static Fraction of(Rational r) {
        return of(r.getNumerator(), r.getDenominator());
    }

    /**
     * Reads an integer ("5", "-3"), a decimal ("2.5", "0.125") or a fraction
     * of integers ("19/2") exactly.
     *
     * @throws IllegalArgumentException if s is none of these
     */
    public static Fraction parse(String s) {
        if (s == null || !s.matches("[+-]?[0-9]+(\\.[0-9]+)?|[+-]?[0-9]+/[+-]?[0-9]+")) {
            throw new IllegalArgumentException("not a number: \"" + s + "\"");
        }
        int slash = s.indexOf('/');
        if (slash >= 0) {
            BigInteger d = new BigInteger(s.substring(slash + 1));
            if (d.signum() == 0) {
                throw new IllegalArgumentException("denominator 0 in \"" + s + "\"");
            }
            return of(new BigInteger(s.substring(0, slash)), d);
        }
        BigDecimal b = new BigDecimal(s);
        if (b.scale() <= 0) {
            return of(b.toBigIntegerExact(), BigInteger.ONE);
        }
        return of(b.unscaledValue(), BigInteger.TEN.pow(b.scale()));
    }

    public BigInteger numerator() {
        return num;
    }

    public BigInteger denominator() {
        return den;
    }

    public int signum() {
        return num.signum();
    }

    public Fraction add(Fraction o) {
        return of(num.multiply(o.den).add(o.num.multiply(den)), den.multiply(o.den));
    }

    public Fraction subtract(Fraction o) {
        return of(num.multiply(o.den).subtract(o.num.multiply(den)), den.multiply(o.den));
    }

    public Fraction multiply(Fraction o) {
        return of(num.multiply(o.num), den.multiply(o.den));
    }

    /** @throws IllegalArgumentException if o is 0 */
    public Fraction divide(Fraction o) {
        if (o.num.signum() == 0) {
            throw new IllegalArgumentException("division by 0");
        }
        return of(num.multiply(o.den), den.multiply(o.num));
    }

    /** The nearest double (to 16 significant digits). */
    public double toDouble() {
        return new BigDecimal(num).divide(new BigDecimal(den), MathContext.DECIMAL64).doubleValue();
    }

    /** Whether numerator and denominator both fit an int, as Rubato's {@link Rational} needs. */
    public boolean fitsRational() {
        return num.compareTo(INT_MIN) > 0 && num.compareTo(INT_MAX) <= 0 && den.compareTo(INT_MAX) <= 0;
    }

    /** @throws IllegalArgumentException if the value does not fit (D8) */
    public Rational toRational() {
        if (!fitsRational()) {
            throw new IllegalArgumentException(this + " is too large for Rubato's rationals (int numerator and denominator)");
        }
        return new Rational(num.intValue(), den.intValue());
    }

    public int compareTo(Fraction o) {
        return num.multiply(o.den).compareTo(o.num.multiply(den));
    }

    public boolean equals(Object o) {
        return o instanceof Fraction && num.equals(((Fraction) o).num) && den.equals(((Fraction) o).den);
    }

    public int hashCode() {
        return 31 * num.hashCode() + den.hashCode();
    }

    /** "19/2", or "5" for an integer. */
    public String toString() {
        return den.equals(BigInteger.ONE) ? num.toString() : num + "/" + den;
    }
}
