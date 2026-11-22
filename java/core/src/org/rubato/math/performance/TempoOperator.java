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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * The tempo operator (Mazzola, <i>The Topos of Music II</i>, &sect;39.5, pp. 653-654):
 * the weighted tempo w T (39.32), its action Q_w on the articulation field (39.33)-(39.36),
 * and the iteration that keeps the duration of an onset interval.
 */
public final class TempoOperator {

    private TempoOperator() { /* static utility */ }

    /** (39.32): the tempo curve w T for a strictly positive weight w. */
    public static TempoCurve apply(final TempoCurve t, final DoubleUnaryOperator w) {
        return new TempoCurve() {
            public double tempo(double e) { return w.applyAsDouble(e) * t.tempo(e); }
            public double integral(double a, double b) {
                return TempoCurves.simpson(x -> {
                    double wx = w.applyAsDouble(x);
                    if (!(wx > 0)) {
                        throw new IllegalArgumentException("the tempo operator needs a strictly positive weight; w(" + x + ") = " + wx);
                    }
                    double v = wx * t.tempo(x);
                    TempoCurves.requirePositive(v, x);
                    return 1 / v;
                }, a, b, TempoCurves.SIMPSON_TOLERANCE);
            }
        };
    }

    /** (39.36): Q_w at (E, D) = [[w(E), 0], [w(E+D) - w(E), w(E+D)]]. */
    public static double[][] q(DoubleUnaryOperator w, double e, double d) {
        double we = w.applyAsDouble(e), ws = w.applyAsDouble(e + d);
        return new double[][] { { we, 0 }, { ws - we, ws } };
    }

    /** (39.35): the articulation field Q_w Z_ED. */
    public static Field articulation(final Field ed, final DoubleUnaryOperator w) {
        if (ed.dimension() != 2) {
            throw new IllegalArgumentException("an articulation field has dimension 2, not " + ed.dimension());
        }
        return new Field() {
            public int dimension() { return 2; }
            public double[] at(double[] x) {
                double[][] m = q(w, x[0], x[1]);
                double[] z = ed.at(x);
                return new double[] { m[0][0] * z[0], m[1][0] * z[0] + m[1][1] * z[1] };
            }
        };
    }

    /**
     * The support function of p. 654: 0 outside [e0, e1], 1 on [e0 + beta, e1 - beta], and the
     * cubic 3u^2 - 2u^3 (zero slopes at both ends) on the two boundary intervals.
     */
    public static double support(double t, double e0, double e1, double beta) {
        if (t <= e0 || t >= e1) {
            return 0;
        }
        if (t < e0 + beta) {
            double u = (t - e0) / beta;
            return u * u * (3 - 2 * u);
        }
        if (t > e1 - beta) {
            double u = (e1 - t) / beta;
            return u * u * (3 - 2 * u);
        }
        return 1;
    }

    /** The outcome of {@link #preserveDuration}. */
    public static final class Preservation {
        private final double factor;
        private final List<Double> ratios;
        private final boolean converged;
        private final TempoCurve curve;

        Preservation(double factor, List<Double> ratios, boolean converged, TempoCurve curve) {
            this.factor = factor;
            this.ratios = Collections.unmodifiableList(ratios);
            this.converged = converged;
            this.curve = curve;
        }

        /** c = the product of the ratios, so that the final weight is c w. */
        public double getFactor() { return factor; }

        /** r(T, w_k) for k = 0, 1, ... */
        public List<Double> getRatios() { return ratios; }

        /** Whether the last ratio was within the tolerance of 1 (else the cap stopped it). */
        public boolean isConverged() { return converged; }

        /** s(T, c w, t) = T(t)(1 + supp(t)(c w(t) - 1)). */
        public TempoCurve getCurve() { return curve; }
    }

    private static TempoCurve supported(final TempoCurve t, final DoubleUnaryOperator w, final double c,
                                        final double e0, final double e1, final double beta) {
        return new TempoCurve() {
            public double tempo(double x) {
                return t.tempo(x) * (1 + support(x, e0, e1, beta) * (c * w.applyAsDouble(x) - 1));
            }
            private double piece(double a, double b) {
                if (b <= e0 || a >= e1) {
                    return t.integral(a, b);
                }
                return TempoCurves.simpson(x -> {
                    double v = tempo(x);
                    TempoCurves.requirePositive(v, x);
                    return 1 / v;
                }, a, b, TempoCurves.SIMPSON_TOLERANCE);
            }
            public double integral(double a, double b) {
                if (b < a) {
                    return -integral(b, a);
                }
                double[] cuts = { e0, e0 + beta, e1 - beta, e1 };
                double sum = 0, lo = a;
                for (double cut : cuts) {
                    if (cut > lo && cut < b) {
                        sum += piece(lo, cut);
                        lo = cut;
                    }
                }
                return sum + piece(lo, b);
            }
        };
    }

    /**
     * p. 653-654: iterates w_(k+1) = r(T, w_k) w_k with r(T, w) = J(T, w)/J(T, 1) and
     * J(T, w) = the integral of 1/s(T, w, t) over [e0, e1], until |r - 1| &lt;= tol or
     * maxIter ratios have been computed. The book does not know whether this converges
     * (footnote 7, p. 654); the result says which way it stopped (departure D7).
     */
    public static Preservation preserveDuration(TempoCurve t, DoubleUnaryOperator w, double e0, double e1,
                                                double beta, double tol, int maxIter) {
        if (!(e1 > e0) || !(beta > 0) || 2 * beta > e1 - e0) {
            throw new IllegalArgumentException("need e0 < e1 and 0 < beta <= (e1 - e0)/2");
        }
        double j1 = t.integral(e0, e1);
        double c = 1;
        List<Double> ratios = new ArrayList<Double>();
        boolean converged = false;
        for (int k = 0; k < maxIter; k++) {
            double r = supported(t, w, c, e0, e1, beta).integral(e0, e1) / j1;
            ratios.add(r);
            c *= r;
            if (Math.abs(r - 1) <= tol) {
                converged = true;
                break;
            }
        }
        return new Preservation(c, ratios, converged, supported(t, w, c, e0, e1, beta));
    }
}
