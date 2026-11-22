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
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Basis-pianola operators (Mazzola, <i>The Topos of Music II</i>, &sect;39.7, pp. 655-657).
 * For p_lambda(X, Y) = (x(X), lambda(X) y(X, Y)) and Lambda = ln lambda, eq. (39.43) gives
 * Z_Lambda = Z - L_Z(Lambda) i_V C^-1 y - (1 - e^-Lambda) i_V C^-1 Delta_Y. Implemented for
 * one basis coordinate u (onset) and one deformed coordinate v (loudness) with an affine
 * mother map y = a Y + b, where C^-1 y = Y + b/a and C^-1 Delta_Y = 1/a (&sect;39.7.1).
 * Also the generalized basis operator (39.44) Z - L_(Z_U)(Lambda) i_V Dir.
 */
public final class BasisPianolaOperator {

    private BasisPianolaOperator() { /* static utility */ }

    /** (39.43) for U = {u}, V = {v}, y = a Y + b; dLambda is dLambda/dx_u. */
    public static Field loudness(final Field z, final int u, final int v, final DoubleUnaryOperator lambda,
                                 final DoubleUnaryOperator dLambda, final double a, final double b) {
        checkAffine(a);
        return new Field() {
            public int dimension() { return z.dimension(); }
            public double[] at(double[] x) {
                double[] r = z.at(x);
                double lie = dLambda.applyAsDouble(x[u]) * r[u];
                r[v] -= lie * (x[v] + b / a) + (1 - Math.exp(-lambda.applyAsDouble(x[u]))) / a;
                return r;
            }
        };
    }

    /**
     * The basis term of (39.43) alone, Z - L_Z(Lambda) i_V C^-1 y: the reading the book uses
     * for small Lambda (p. 656). Not the field of p_Lambda when Lambda is not small.
     */
    public static Field loudnessBasisTerm(final Field z, final int u, final int v, final DoubleUnaryOperator dLambda,
                                          final double a, final double b) {
        checkAffine(a);
        return new Field() {
            public int dimension() { return z.dimension(); }
            public double[] at(double[] x) {
                double[] r = z.at(x);
                r[v] -= dLambda.applyAsDouble(x[u]) * r[u] * (x[v] + b / a);
                return r;
            }
        };
    }

    /** The performed loudness e^Lambda (a L + b) of p_Lambda. */
    public static double performedLoudness(double lambdaAtOnset, double a, double b, double loudness) {
        return Math.exp(lambdaAtOnset) * (a * loudness + b);
    }

    /**
     * (39.44): Z - L_(Z_U)(Lambda) i_V Dir(x_V), with the Lie derivative
     * L_(Z_U)(Lambda) = sum over i in U of dLambda/dx_i Z_i.
     *
     * @param gradient the gradient of Lambda on the U coordinates, as a function of x_U
     * @param dir      the directional endomorphism of V, as a function of x_V
     */
    public static Field basis(final Field z, final int[] u, final Function<double[], double[]> gradient,
                              final int[] v, final UnaryOperator<double[]> dir) {
        return new Field() {
            public int dimension() { return z.dimension(); }
            public double[] at(double[] x) {
                double[] r = z.at(x);
                double[] xu = new double[u.length], xv = new double[v.length];
                for (int i = 0; i < u.length; i++) xu[i] = x[u[i]];
                for (int j = 0; j < v.length; j++) xv[j] = x[v[j]];
                double[] g = gradient.apply(xu);
                double lie = 0;
                for (int i = 0; i < u.length; i++) {
                    lie += g[i] * r[u[i]];
                }
                double[] d = dir.apply(xv);
                for (int j = 0; j < v.length; j++) {
                    r[v[j]] -= lie * d[j];
                }
                return r;
            }
        };
    }

    private static void checkAffine(double a) {
        if (a == 0 || Double.isNaN(a) || Double.isInfinite(a)) {
            throw new IllegalArgumentException("the affine map y = a Y + b needs a finite a != 0");
        }
    }
}
