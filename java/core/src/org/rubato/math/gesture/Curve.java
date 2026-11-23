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

import java.util.TreeSet;
import java.util.function.UnaryOperator;

/**
 * A continuous curve c : [0, 1] -> R^n, an arrow of the spatial digraph of
 * R^n with tail c(0) and head c(1) (Mazzola, The Topos of Music, vol. III,
 * p. 914). Two kinds exist: {@link PolylineCurve}, piecewise linear, whose
 * sup distances are computed exactly at the merged knots, and
 * {@link FunctionCurve}, any function, whose sup distances are sampled.
 */
public abstract class Curve {

    /** Sampling intervals for sup distances of non-polyline curves. */
    public static final int SAMPLES = 1024;

    public abstract int dimension();

    /** The point at parameter s in [0, 1]. */
    public abstract double[] at(double s);

    /** The Euclidean distance. */
    public static double distance(double[] p, double[] q) {
        if (p.length != q.length) {
            throw new IllegalArgumentException("dimensions " + p.length + " and " + q.length);
        }
        double sum = 0;
        for (int i = 0; i < p.length; i++) {
            double d = p[i] - q[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    /**
     * sup over s of |a(s) - b(s)|. For two polylines the difference is
     * piecewise linear on the merged knots and its norm convex on each
     * piece, so the maximum over the knots is the sup. Otherwise the
     * curves are compared at SAMPLES + 1 equally spaced parameters.
     */
    public static double supDistance(Curve a, Curve b) {
        double max = 0;
        if (a instanceof PolylineCurve && b instanceof PolylineCurve) {
            TreeSet<Double> knots = new TreeSet<Double>();
            for (double k : ((PolylineCurve) a).knots()) {
                knots.add(k);
            }
            for (double k : ((PolylineCurve) b).knots()) {
                knots.add(k);
            }
            for (double s : knots) {
                max = Math.max(max, distance(a.at(s), b.at(s)));
            }
            return max;
        }
        for (int i = 0; i <= SAMPLES; i++) {
            double s = (double) i / SAMPLES;
            max = Math.max(max, distance(a.at(s), b.at(s)));
        }
        return max;
    }

    /**
     * The concatenation, e at double speed on [0, 1/2] and then f on
     * [1/2, 1] (vol. III, p. 920). It needs e(1) = f(0).
     *
     * @throws IllegalArgumentException if e(1) and f(0) differ by more
     *         than {@link Gesture#TOLERANCE}
     */
    public static Curve concat(Curve e, Curve f) {
        double gap = distance(e.at(1), f.at(0));
        if (gap > Gesture.TOLERANCE) {
            throw new IllegalArgumentException("e(1) and f(0) differ by " + gap);
        }
        if (e instanceof PolylineCurve && f instanceof PolylineCurve) {
            return PolylineCurve.concat((PolylineCurve) e, (PolylineCurve) f);
        }
        return new FunctionCurve(e.dimension(), s -> s <= 0.5 ? e.at(2 * s) : f.at(2 * s - 1));
    }

    /**
     * The curve h o c. A polyline maps to the polyline through the images
     * of its knots, which equals h o c when h is affine.
     */
    public abstract Curve map(UnaryOperator<double[]> h);
}
