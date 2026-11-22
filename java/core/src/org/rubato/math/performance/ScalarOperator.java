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

import java.util.function.ToDoubleFunction;

/**
 * The scalar operator (Mazzola, <i>The Topos of Music II</i>, &sect;39.6, pp. 654-655) on a
 * field Z_BP = (Z_B, Z_P) over a basis space B and its pianola space P of the same dimension
 * k, with a weight w on B evaluated at p_B(Q). The switches B and P say which components the
 * weight acts on; the parallel flag refers the result to the parallel field d of (33.10).
 * The tempo operator is the parallel case B = P = yes with B = onset, P = duration.
 */
public final class ScalarOperator {

    private ScalarOperator() { /* static utility */ }

    /**
     * @param basis    the basis field Z_B on B (dimension k), used by the parallel cases
     * @param field    the field Z_BP on B + P (dimension 2k)
     * @param w        a positive weight on B
     * @param parallel whether the parallel structure is installed
     * @param onB      the switch B
     * @param onP      the switch P
     */
    public static Field apply(final Field basis, final Field field, final ToDoubleFunction<double[]> w,
                              final boolean parallel, final boolean onB, final boolean onP) {
        final int k = basis.dimension();
        if (field.dimension() != 2 * k) {
            throw new IllegalArgumentException("the field must have twice the basis dimension: " + field.dimension()
                    + " vs " + k);
        }
        if (!parallel) {
            return new Field() {
                public int dimension() { return 2 * k; }
                public double[] at(double[] q) {
                    double[] z = field.at(q);
                    double wb = w.applyAsDouble(head(q, k));
                    for (int i = 0; i < k; i++) {
                        if (onB) z[i] *= wb;
                        if (onP) z[k + i] *= wb;
                    }
                    return z;
                }
            };
        }
        if (!onB && !onP) {
            return Fields.parallel(basis);                      // (39.37)
        }
        if (onB && onP) {
            return new Field() {                                // (39.38): Q_w Z_BP
                public int dimension() { return 2 * k; }
                public double[] at(double[] q) {
                    double[] z = field.at(q);
                    double[] b = head(q, k), s = new double[k];
                    for (int i = 0; i < k; i++) s[i] = q[i] + q[k + i];
                    double wb = w.applyAsDouble(b), ws = w.applyAsDouble(s);
                    double[] r = new double[2 * k];
                    for (int i = 0; i < k; i++) {
                        r[i] = wb * z[i];
                        r[k + i] = (ws - wb) * z[i] + ws * z[k + i];
                    }
                    return r;
                }
            };
        }
        if (onB) {
            return Fields.parallel(new Field() {                // d(w Z_B)
                public int dimension() { return k; }
                public double[] at(double[] b) {
                    double[] z = basis.at(b);
                    double wb = w.applyAsDouble(b);
                    for (int i = 0; i < k; i++) z[i] *= wb;
                    return z;
                }
            });
        }
        final Field d = Fields.parallel(basis);
        return new Field() {                                    // w dZ_B
            public int dimension() { return 2 * k; }
            public double[] at(double[] q) {
                double[] z = d.at(q);
                double wb = w.applyAsDouble(head(q, k));
                for (int i = 0; i < z.length; i++) z[i] *= wb;
                return z;
            }
        };
    }

    static double[] head(double[] q, int k) {
        double[] b = new double[k];
        System.arraycopy(q, 0, b, 0, k);
        return b;
    }
}
