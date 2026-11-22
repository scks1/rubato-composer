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

import java.util.function.DoubleUnaryOperator;

/**
 * Tempo curves with closed-form integrals of 1/T (Mazzola, <i>The Topos of
 * Music II</i>, Exercise 76, p. 563: q0 + q1 X, q0 + q1 X + q2 X^2, e^X,
 * 2 + sin X, here with parameters), and curves given by a function, whose
 * integral is computed by the adaptive Simpson rule.
 */
public final class TempoCurves {

    /** Absolute tolerance of the adaptive Simpson rule for numeric curves. */
    public static final double SIMPSON_TOLERANCE = 1e-12;

    private TempoCurves() { /* static utility */ }

    static void requirePositive(double t, double at) {
        if (!(t > 0) || Double.isInfinite(t)) {
            throw new IllegalArgumentException("tempo must be positive and finite; T(" + at + ") = " + t);
        }
    }

    /** Constant tempo q0: linear onsets. */
    public static TempoCurve constant(final double q0) {
        requirePositive(q0, 0);
        return new TempoCurve() {
            public double tempo(double e) { return q0; }
            public double integral(double a, double b) { return (b - a) / q0; }
            public String toString() { return "constant " + q0; }
        };
    }

    /** Linear tempo q0 + q1 X: the integral is (1/q1) ln(T(b)/T(a)). */
    public static TempoCurve linear(final double q0, final double q1) {
        if (q1 == 0) {
            return constant(q0);
        }
        return new TempoCurve() {
            public double tempo(double e) { return q0 + q1 * e; }
            public double integral(double a, double b) {
                double ta = tempo(a), tb = tempo(b);
                requirePositive(ta, a);
                requirePositive(tb, b);
                return Math.log(tb / ta) / q1;
            }
            public String toString() { return "linear " + q0 + " + " + q1 + " X"; }
        };
    }

    /**
     * Quadratic tempo q0 + q1 X + q2 X^2, with the antiderivative of
     * 1/(q2 x^2 + q1 x + q0) in the three cases of the discriminant.
     */
    public static TempoCurve quadratic(final double q0, final double q1, final double q2) {
        if (q2 == 0) {
            return linear(q0, q1);
        }
        final double d = q1 * q1 - 4 * q0 * q2;
        return new TempoCurve() {
            public double tempo(double e) { return q0 + q1 * e + q2 * e * e; }
            private double g(double x) {
                double u = 2 * q2 * x + q1;
                if (d < 0) {
                    double s = Math.sqrt(-d);
                    return 2 / s * Math.atan(u / s);
                }
                if (d == 0) {
                    return -2 / u;
                }
                double s = Math.sqrt(d);
                return Math.log(Math.abs((u - s) / (u + s))) / s;
            }
            public double integral(double a, double b) {
                double lo = Math.min(a, b), hi = Math.max(a, b);
                requirePositive(tempo(lo), lo);
                requirePositive(tempo(hi), hi);
                double v = -q1 / (2 * q2);
                if (v > lo && v < hi) {
                    requirePositive(tempo(v), v);
                }
                return g(b) - g(a);
            }
            public String toString() { return "quadratic " + q0 + " + " + q1 + " X + " + q2 + " X^2"; }
        };
    }

    /** Exponential tempo c e^(kX); the book's e^X is c = k = 1. */
    public static TempoCurve exponential(final double c, final double k) {
        requirePositive(c, 0);
        if (k == 0) {
            return constant(c);
        }
        return new TempoCurve() {
            public double tempo(double e) { return c * Math.exp(k * e); }
            public double integral(double a, double b) {
                return (Math.exp(-k * a) - Math.exp(-k * b)) / (c * k);
            }
            public String toString() { return "exponential " + c + " e^(" + k + " X)"; }
        };
    }

    /**
     * Sine tempo p + r sin X with p &gt; |r|; the book's 2 + sin X is p = 2, r = 1. The
     * antiderivative (2/s) atan((p tan(x/2) + r)/s), s = sqrt(p^2 - r^2), is made continuous
     * by adding 2 pi/s for each period.
     */
    public static TempoCurve sine(final double p, final double r) {
        if (!(p > Math.abs(r))) {
            throw new IllegalArgumentException("p + r sin X needs p > |r| to stay positive: p = " + p + ", r = " + r);
        }
        final double s = Math.sqrt(p * p - r * r);
        return new TempoCurve() {
            public double tempo(double e) { return p + r * Math.sin(e); }
            private double g(double x) {
                double k = Math.floor((x + Math.PI) / (2 * Math.PI));
                double y = x - 2 * Math.PI * k;
                return 2 / s * Math.atan((p * Math.tan(y / 2) + r) / s) + k * 2 * Math.PI / s;
            }
            public double integral(double a, double b) { return g(b) - g(a); }
            public String toString() { return "sine " + p + " + " + r + " sin X"; }
        };
    }

    /** A tempo curve given by a function; its integral is computed numerically. */
    public static TempoCurve function(final DoubleUnaryOperator t) {
        return new TempoCurve() {
            public double tempo(double e) { return t.applyAsDouble(e); }
            public double integral(double a, double b) {
                return simpson(x -> {
                    double v = t.applyAsDouble(x);
                    requirePositive(v, x);
                    return 1 / v;
                }, a, b, SIMPSON_TOLERANCE);
            }
            public String toString() { return "numeric tempo"; }
        };
    }

    /**
     * Adaptive Simpson rule with Richardson correction for the integral of f from a to b;
     * negative when b &lt; a.
     */
    public static double simpson(DoubleUnaryOperator f, double a, double b, double tol) {
        if (a == b) {
            return 0;
        }
        if (b < a) {
            return -simpson(f, b, a, tol);
        }
        double fa = f.applyAsDouble(a), fb = f.applyAsDouble(b), m = 0.5 * (a + b), fm = f.applyAsDouble(m);
        double whole = (b - a) / 6 * (fa + 4 * fm + fb);
        return simpson(f, a, b, fa, fm, fb, whole, tol, 50);
    }

    private static double simpson(DoubleUnaryOperator f, double a, double b, double fa, double fm, double fb,
                                  double whole, double tol, int depth) {
        double m = 0.5 * (a + b), lm = 0.5 * (a + m), rm = 0.5 * (m + b);
        double flm = f.applyAsDouble(lm), frm = f.applyAsDouble(rm);
        double left = (m - a) / 6 * (fa + 4 * flm + fm);
        double right = (b - m) / 6 * (fm + 4 * frm + fb);
        double delta = left + right - whole;
        if (depth <= 0 || Math.abs(delta) <= 15 * tol) {
            return left + right + delta / 15;
        }
        return simpson(f, a, m, fa, flm, fm, left, tol / 2, depth - 1)
             + simpson(f, m, b, fm, frm, fb, right, tol / 2, depth - 1);
    }
}
