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
package org.rubato.math.gesture;

import java.util.function.UnaryOperator;

/**
 * A piecewise-linear curve through given points at increasing knots
 * 0 = k_0 < k_1 < ... < k_m = 1.
 */
public final class PolylineCurve extends Curve {

    private final double[] knots;
    private final double[][] points;

    /**
     * @throws IllegalArgumentException unless the knots start at 0, end at
     *         1 and increase strictly, and all points have one dimension
     */
    public PolylineCurve(double[] knots, double[][] points) {
        if (knots.length < 2 || knots.length != points.length) {
            throw new IllegalArgumentException("need at least two knots, one point per knot");
        }
        if (knots[0] != 0 || knots[knots.length - 1] != 1) {
            throw new IllegalArgumentException("knots must run from 0 to 1");
        }
        for (int i = 1; i < knots.length; i++) {
            if (!(knots[i] > knots[i - 1])) {
                throw new IllegalArgumentException("knots must increase: " + knots[i - 1] + ", " + knots[i]);
            }
            if (points[i].length != points[0].length) {
                throw new IllegalArgumentException("points of different dimensions");
            }
        }
        this.knots = knots.clone();
        this.points = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            this.points[i] = points[i].clone();
        }
    }

    /** The straight segment from p to q. */
    public static PolylineCurve line(double[] p, double[] q) {
        return new PolylineCurve(new double[] { 0, 1 }, new double[][] { p, q });
    }

    /** The polyline through the points at equally spaced knots. */
    public static PolylineCurve through(double[]... points) {
        double[] k = new double[points.length];
        for (int i = 0; i < k.length; i++) {
            k[i] = (double) i / (k.length - 1);
        }
        k[k.length - 1] = 1;
        return new PolylineCurve(k, points);
    }

    static PolylineCurve concat(PolylineCurve e, PolylineCurve f) {
        int m = e.knots.length, n = f.knots.length;
        double[] k = new double[m + n - 1];
        double[][] p = new double[m + n - 1][];
        for (int i = 0; i < m; i++) {
            k[i] = e.knots[i] / 2;
            p[i] = e.points[i];
        }
        for (int j = 1; j < n; j++) {
            k[m - 1 + j] = 0.5 + f.knots[j] / 2;
            p[m - 1 + j] = f.points[j];
        }
        k[k.length - 1] = 1;
        return new PolylineCurve(k, p);
    }

    public double[] knots() {
        return knots.clone();
    }

    /** The point at knot i. */
    public double[] point(int i) {
        return points[i].clone();
    }

    @Override
    public int dimension() {
        return points[0].length;
    }

    @Override
    public double[] at(double s) {
        if (s <= 0) {
            return points[0].clone();
        }
        if (s >= 1) {
            return points[points.length - 1].clone();
        }
        int i = 1;
        while (knots[i] < s) {
            i++;
        }
        double u = (s - knots[i - 1]) / (knots[i] - knots[i - 1]);
        double[] a = points[i - 1], b = points[i];
        double[] r = new double[a.length];
        for (int d = 0; d < r.length; d++) {
            r[d] = a[d] + u * (b[d] - a[d]);
        }
        return r;
    }

    @Override
    public Curve map(UnaryOperator<double[]> h) {
        double[][] q = new double[points.length][];
        for (int i = 0; i < q.length; i++) {
            q[i] = h.apply(points[i].clone());
        }
        return new PolylineCurve(knots, q);
    }
}
