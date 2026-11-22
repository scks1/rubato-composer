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

/**
 * A performance cell (Mazzola, <i>The Topos of Music II</i>, Def. 101, pp. 585-586) reduced
 * to its field Z, initial set I and initial performance p_I, with signature epsilon = -.
 * A point X is performed by eq. (33.12), p. 568: follow the integral curve of Z backward
 * from X until it meets I at X0, at curve parameter t &lt;= 0; then p(X) = p_I(X0) - t Delta.
 * <p>
 * Integration: classical RK4 on the reversed field, step h = h0/max(1, |Z|_inf) with h0 =
 * 1/256 by default, the last step bisected so that the end point lies on I (departure D6;
 * the book's PerformanceRUBETTE used Runge-Kutta-Fehlberg, p. 652 and p. 683). Def. 101
 * condition 5 is used with the sign (beta - alpha) that (33.12) gives (book error E5).
 */
public final class PerformanceCell {

    /** The result of performing one point. */
    public static final class Performed {
        private final double[] point;
        private final double parameter;
        private final double[] value;

        Performed(double[] point, double parameter, double[] value) {
            this.point = point;
            this.parameter = parameter;
            this.value = value;
        }

        /** X0, where the backward curve met the initial set. */
        public double[] point() { return point.clone(); }

        /** The curve parameter t &lt;= 0 of X0 on the curve through X. */
        public double parameter() { return parameter; }

        /** p(X) = p_I(X0) - t Delta. */
        public double[] value() { return value.clone(); }
    }

    private final Field field;
    private final InitialSet initial;
    private final InitialPerformance initialPerformance;
    private final double h0;
    private final double maxParameter;

    public PerformanceCell(Field field, InitialSet initial, InitialPerformance initialPerformance) {
        this(field, initial, initialPerformance, 1.0 / 256, 1e6);
    }

    private PerformanceCell(Field field, InitialSet initial, InitialPerformance p, double h0, double maxParameter) {
        this.field = field;
        this.initial = initial;
        this.initialPerformance = p;
        this.h0 = h0;
        this.maxParameter = maxParameter;
    }

    /** The same cell with base step h0. */
    public PerformanceCell withStep(double step) {
        if (!(step > 0)) {
            throw new IllegalArgumentException("step must be positive: " + step);
        }
        return new PerformanceCell(field, initial, initialPerformance, step, maxParameter);
    }

    /** The same cell refusing curves longer than this parameter length. */
    public PerformanceCell withMaxParameter(double m) {
        return new PerformanceCell(field, initial, initialPerformance, h0, m);
    }

    public Field getField() { return field; }

    private double[] vec(double[] x) {
        double[] z = field.at(x);
        for (double v : z) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new IllegalArgumentException("the field is not finite at this point");
            }
        }
        return z;
    }

    /** One RK4 step of length h along the reversed field (curve parameter decreasing). */
    private double[] backStep(double[] x, double h) {
        int n = x.length;
        double[] k1 = vec(x);
        double[] y = new double[n];
        for (int i = 0; i < n; i++) y[i] = x[i] - h / 2 * k1[i];
        double[] k2 = vec(y);
        for (int i = 0; i < n; i++) y[i] = x[i] - h / 2 * k2[i];
        double[] k3 = vec(y);
        for (int i = 0; i < n; i++) y[i] = x[i] - h * k3[i];
        double[] k4 = vec(y);
        double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            r[i] = x[i] - h / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        }
        return r;
    }

    private double step(double[] x) {
        double m = 1;
        for (double v : vec(x)) {
            m = Math.max(m, Math.abs(v));
        }
        return h0 / m;
    }

    private void checkDimension(double[] x) {
        if (x.length != field.dimension()) {
            throw new IllegalArgumentException("point of dimension " + x.length + " for a field of dimension "
                    + field.dimension());
        }
    }

    /** The point of the integral curve through x at parameter s &lt;= 0. */
    public double[] curve(double[] x, double s) {
        checkDimension(x);
        if (s > 0) {
            throw new IllegalArgumentException("epsilon = - curves have parameters <= 0: " + s);
        }
        double[] y = x.clone();
        double left = -s;
        while (left > 0) {
            double h = Math.min(step(y), left);
            y = backStep(y, h);
            left -= h;
        }
        return y;
    }

    /** Eq. (33.12): the performance of x. */
    public Performed perform(double[] x) {
        checkDimension(x);
        double[] y = x.clone();
        double t = 0;
        if (initial.reach(y) > 0) {
            while (true) {
                double h = step(y);
                double[] next = backStep(y, h);
                if (initial.reach(next) <= 0) {
                    double lo = 0, hi = h;
                    double[] hit = next;
                    for (int i = 0; i < 60; i++) {
                        double mid = 0.5 * (lo + hi);
                        double[] m = backStep(y, mid);
                        if (initial.reach(m) > 0) {
                            lo = mid;
                        }
                        else {
                            hi = mid;
                            hit = m;
                        }
                    }
                    t -= hi;
                    y = hit;
                    initial.project(y);
                    break;
                }
                y = next;
                t -= h;
                if (-t > maxParameter) {
                    throw new IllegalArgumentException("the backward curve does not meet the initial set within parameter "
                            + maxParameter + " (Def. 101 condition 4)");
                }
            }
        }
        double[] p0 = initialPerformance.at(y);
        double[] v = new double[p0.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = p0[i] - t;
        }
        return new Performed(y, t, v);
    }

    /** A one-dimensional onset cell: tempo field, initial set {E = Ea}, p_I(Ea) = ea. */
    public static PerformanceCell onset(final TempoCurve t, final double ea0, final double ea) {
        return new PerformanceCell(Fields.tempo(t), InitialSet.lines(new int[] { 0 }, new double[] { ea0 }),
                x -> new double[] { ea + t.integral(ea0, x[0]) });
    }

    /**
     * The articulation cell of &sect;34.5 (p. 579): the parallel field (33.9), the initial set
     * of the left line E = Ea and the bottom line D = Da, and the initial performance
     * (34.2)/(34.3) p_I(E, D) = (p_E(E), p_E(E + D) - p_E(E)) with p_E(Ea) = ea.
     */
    public static PerformanceCell articulation(TempoCurve t, double ea0, double ea, double da) {
        return new PerformanceCell(Fields.parallel(t), InitialSet.lines(new int[] { 0, 1 }, new double[] { ea0, da }),
                articulationInitial(t, ea0, ea));
    }

    /** The initial performance (34.2)/(34.3) built on the onset performance of t. */
    public static InitialPerformance articulationInitial(final TempoCurve t, final double ea0, final double ea) {
        return x -> new double[] { ea + t.integral(ea0, x[0]), t.integral(x[0], x[0] + x[1]) };
    }
}
