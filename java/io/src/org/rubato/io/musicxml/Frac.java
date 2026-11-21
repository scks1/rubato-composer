/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.rubato.io.musicxml;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An exact fraction with BigInteger parts, for time positions in MusicXML.
 * Rubato's {@link org.rubato.math.arith.Rational} uses int parts, which a
 * long piece with fine divisions can overflow.
 */
final class Frac implements Comparable<Frac> {

    static final Frac ZERO = new Frac(BigInteger.ZERO, BigInteger.ONE);

    final BigInteger num;
    final BigInteger den;

    Frac(BigInteger n, BigInteger d) {
        if (d.signum() == 0) {
            throw new ArithmeticException("zero denominator");
        }
        if (d.signum() < 0) {
            n = n.negate();
            d = d.negate();
        }
        BigInteger g = n.gcd(d);
        if (g.signum() != 0 && !g.equals(BigInteger.ONE)) {
            n = n.divide(g);
            d = d.divide(g);
        }
        this.num = n;
        this.den = d;
    }

    static Frac of(long n, long d) {
        return new Frac(BigInteger.valueOf(n), BigInteger.valueOf(d));
    }

    static Frac of(BigDecimal value) {
        BigDecimal v = value.stripTrailingZeros();
        if (v.scale() <= 0) {
            return new Frac(v.toBigIntegerExact(), BigInteger.ONE);
        }
        return new Frac(v.unscaledValue(), BigInteger.TEN.pow(v.scale()));
    }

    Frac add(Frac o) {
        return new Frac(this.num.multiply(o.den).add(o.num.multiply(this.den)), this.den.multiply(o.den));
    }

    Frac subtract(Frac o) {
        return new Frac(this.num.multiply(o.den).subtract(o.num.multiply(this.den)), this.den.multiply(o.den));
    }

    Frac multiply(Frac o) {
        return new Frac(this.num.multiply(o.num), this.den.multiply(o.den));
    }

    Frac divide(Frac o) {
        return new Frac(this.num.multiply(o.den), this.den.multiply(o.num));
    }

    int signum() {
        return this.num.signum();
    }

    /** The nearest double, exact as a quotient when both parts fit in 53 bits. */
    double doubleValue() {
        if (this.num.bitLength() <= 53 && this.den.bitLength() <= 53) {
            return this.num.doubleValue() / this.den.doubleValue();
        }
        return new BigDecimal(this.num).divide(new BigDecimal(this.den), java.math.MathContext.DECIMAL128).doubleValue();
    }

    public int compareTo(Frac o) {
        return this.num.multiply(o.den).compareTo(o.num.multiply(this.den));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Frac && this.num.equals(((Frac)o).num) && this.den.equals(((Frac)o).den);
    }

    @Override
    public int hashCode() {
        return this.num.hashCode() * 31 + this.den.hashCode();
    }

    @Override
    public String toString() {
        return this.den.equals(BigInteger.ONE) ? this.num.toString() : this.num + "/" + this.den;
    }

    /**
     * The first continued-fraction convergent of x that lies within
     * {@code tolerance} of x, or null if none has a denominator up to
     * {@code maxDen}.
     */
    static Frac approximate(double x, long maxDen, double tolerance) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            return null;
        }
        long p0 = 0, q0 = 1, p1 = 1, q1 = 0;
        double r = x;
        for (int i = 0; i < 64; i++) {
            double fl = Math.floor(r);
            if (Math.abs(fl) > 1e15) {
                return null;
            }
            long a = (long)fl;
            long p2 = a * p1 + p0;
            long q2 = a * q1 + q0;
            if (q2 > maxDen) {
                return null;
            }
            if (Math.abs(x - (double)p2 / q2) <= tolerance) {
                return of(p2, q2);
            }
            p0 = p1; q0 = q1; p1 = p2; q1 = q2;
            double frac = r - fl;
            if (frac == 0) {
                return null;
            }
            r = 1.0 / frac;
        }
        return null;
    }
}
