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

import java.util.function.Function;

/**
 * Constructions of performance fields (Mazzola, <i>The Topos of Music II</i>,
 * &sect;33.2): the tempo field, the product field (33.7), the parallel field of
 * articulation (33.9) and its generic form (33.10), the parallel field with dilated
 * duration (Example 51, p. 591), and (33.11) evaluated by central differences.
 */
public final class Fields {

    private Fields() { /* static utility */ }

    /** The one-dimensional field T(E) of a tempo curve (&sect;33.1.1). */
    public static Field tempo(final TempoCurve t) {
        return new Field() {
            public int dimension() { return 1; }
            public double[] at(double[] x) { return new double[] { t.tempo(x[0]) }; }
        };
    }

    /** (33.7): the product of fields on disjoint coordinate blocks, in the given order. */
    public static Field product(final Field... fields) {
        int n = 0;
        for (Field f : fields) {
            n += f.dimension();
        }
        final int dim = n;
        return new Field() {
            public int dimension() { return dim; }
            public double[] at(double[] x) {
                double[] z = new double[dim];
                int k = 0;
                for (Field f : fields) {
                    double[] part = new double[f.dimension()];
                    System.arraycopy(x, k, part, 0, part.length);
                    double[] v = f.at(part);
                    System.arraycopy(v, 0, z, k, v.length);
                    k += v.length;
                }
                return z;
            }
        };
    }

    /**
     * (33.10): the parallel field on B + P of a basis field Z on B, with the alteration
     * a(b, p) = b + p: dZ(b, p) = (Z(b), 2 Z(b + p) - Z(b)).
     */
    public static Field parallel(final Field basis) {
        return dilated(basis, 1);
    }

    /**
     * Example 51 (p. 591), the answer to Exercise 77 (p. 567): for durations dilated by
     * lambda &gt; 0, dZ(b, p) = (Z(b), ((1 + lambda)/lambda) Z(b + p) - Z(b)). Only for a
     * one-dimensional basis, as in the book.
     */
    public static Field parallel(final Field basis, final double lambda) {
        if (basis.dimension() != 1) {
            throw new IllegalArgumentException("the dilated parallel field is defined for one basis coordinate, not "
                    + basis.dimension());
        }
        return dilated(basis, lambda);
    }

    private static Field dilated(final Field basis, final double lambda) {
        if (!(lambda > 0) || Double.isInfinite(lambda)) {
            throw new IllegalArgumentException("lambda must be positive: " + lambda);
        }
        final int k = basis.dimension();
        final double c = (1 + lambda) / lambda;
        return new Field() {
            public int dimension() { return 2 * k; }
            public double[] at(double[] q) {
                double[] b = new double[k], s = new double[k];
                for (int i = 0; i < k; i++) {
                    b[i] = q[i];
                    s[i] = q[i] + q[k + i];
                }
                double[] zb = basis.at(b), zs = basis.at(s);
                double[] z = new double[2 * k];
                for (int i = 0; i < k; i++) {
                    z[i] = zb[i];
                    z[k + i] = c * zs[i] - zb[i];
                }
                return z;
            }
        };
    }

    /** (33.9): the parallel articulation field (T(E), 2 T(E + D) - T(E)). */
    public static Field parallel(TempoCurve t) {
        return parallel(tempo(t));
    }

    /** Example 51 for a tempo curve. */
    public static Field parallel(TempoCurve t, double lambda) {
        return parallel(tempo(t), lambda);
    }

    /**
     * (33.11): (J p)^-1 Delta at x, with the Jacobian of p by central differences of step h
     * and the linear system solved by Gaussian elimination with partial pivoting.
     */
    public static double[] inverseJacobian(Function<double[], double[]> p, double[] x, double h) {
        int n = x.length;
        double[][] a = new double[n][n + 1];
        for (int j = 0; j < n; j++) {
            double[] xp = x.clone(), xm = x.clone();
            xp[j] += h;
            xm[j] -= h;
            double[] fp = p.apply(xp), fm = p.apply(xm);
            for (int i = 0; i < n; i++) {
                a[i][j] = (fp[i] - fm[i]) / (2 * h);
            }
        }
        for (int i = 0; i < n; i++) {
            a[i][n] = 1;
        }
        return solve(a);
    }

    /** Solves the augmented n x (n+1) system in place. */
    static double[] solve(double[][] a) {
        int n = a.length;
        for (int c = 0; c < n; c++) {
            int piv = c;
            for (int r = c + 1; r < n; r++) {
                if (Math.abs(a[r][c]) > Math.abs(a[piv][c])) {
                    piv = r;
                }
            }
            if (a[piv][c] == 0) {
                throw new IllegalArgumentException("singular Jacobian");
            }
            double[] tmp = a[c];
            a[c] = a[piv];
            a[piv] = tmp;
            for (int r = c + 1; r < n; r++) {
                double m = a[r][c] / a[c][c];
                for (int k = c; k <= n; k++) {
                    a[r][k] -= m * a[c][k];
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
