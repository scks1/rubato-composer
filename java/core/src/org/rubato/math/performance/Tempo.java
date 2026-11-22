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
 * Onset performance from a tempo curve and the initial-value constructions of
 * Mazzola, <i>The Topos of Music II</i>, &sect;34.2, p. 576: Prop. 56 (the polygonal
 * tempo adapted to an initial performance) and Cor. 21 (stretching a polygon to a
 * prescribed total duration).
 */
public final class Tempo {

    private Tempo() { /* static utility */ }

    /** Eq. (33.1): e(E) = e0 + the integral of 1/T from E0 to E, in minutes. */
    public static double onset(TempoCurve t, double e0Onset, double e0, double onset) {
        return e0 + t.integral(e0Onset, onset);
    }

    /** {@link #onset} with the physical onsets in seconds (departure D1). */
    public static double seconds(TempoCurve t, double symbolic0, double seconds0, double onset) {
        return seconds0 + 60 * t.integral(symbolic0, onset);
    }

    /**
     * The integral of 1/T over a linear piece from (e0, t0) to (e1, t1):
     * (e1 - e0)(ln t1 - ln t0)/(t1 - t0), tending to (e1 - e0)/t0 as t1 tends to t0.
     */
    public static double segmentIntegral(double e0, double e1, double t0, double t1) {
        double r = (t1 - t0) / t0;
        if (Math.abs(r) < 1e-8) {
            return (e1 - e0) / t0 * (1 - r / 2 + r * r / 3);
        }
        return (e1 - e0) * Math.log1p(r) / (t1 - t0);
    }

    /**
     * The tempo x at the far end of a linear piece of length len that starts at the known
     * tempo t and lasts d minutes. The integral is strictly decreasing in x from infinity to
     * 0 (the logarithmic mean increases), so x is unique; bisection in log scale.
     */
    static double solveSegment(double len, double t, double d) {
        double lo = t, hi = t;
        while (segmentIntegral(0, len, t, lo) < d) {
            lo /= 2;
        }
        while (segmentIntegral(0, len, t, hi) > d) {
            hi *= 2;
        }
        for (int i = 0; i < 200 && hi - lo > 1e-15 * hi; i++) {
            double mid = Math.sqrt(lo * hi);
            if (mid <= lo || mid >= hi) {
                mid = 0.5 * (lo + hi);
            }
            if (segmentIntegral(0, len, t, mid) > d) {
                lo = mid;
            }
            else {
                hi = mid;
            }
        }
        return 0.5 * (lo + hi);
    }

    /**
     * Prop. 56: the unique continuous tempo curve, linear on each [E_i, E_{i+1}] and constant
     * outside, with T(E_k) = tk, whose integrals between the onsets equal the differences of
     * the performed onsets (eq. (34.1)).
     */
    public static Polygon adapt(double[] onsets, double[] performed, int k, double tk) {
        if (onsets.length != performed.length || onsets.length == 0) {
            throw new IllegalArgumentException("onsets and performed onsets differ in number");
        }
        if (k < 0 || k >= onsets.length) {
            throw new IllegalArgumentException("index of the prescribed tempo out of range: " + k);
        }
        TempoCurves.requirePositive(tk, onsets[k]);
        for (int i = 1; i < onsets.length; i++) {
            if (!(onsets[i] > onsets[i - 1]) || !(performed[i] > performed[i - 1])) {
                throw new IllegalArgumentException("onsets and performed onsets must both increase (index " + i + ")");
            }
        }
        double[] t = new double[onsets.length];
        t[k] = tk;
        for (int i = k + 1; i < t.length; i++) {
            t[i] = solveSegment(onsets[i] - onsets[i - 1], t[i - 1], performed[i] - performed[i - 1]);
        }
        for (int i = k - 1; i >= 0; i--) {
            t[i] = solveSegment(onsets[i + 1] - onsets[i], t[i + 1], performed[i + 1] - performed[i]);
        }
        return new Polygon(onsets, t);
    }

    private static Polygon scaled(Polygon p, double tau) {
        double[] t = p.getTempi();
        for (int i = 1; i < t.length - 1; i++) {
            t[i] *= tau;
        }
        return new Polygon(p.getOnsets(), t);
    }

    /**
     * Cor. 21: the factor tau for the inner vertices such that the polygon's integral over
     * [E_0, E_f] becomes sigma times the old one. Needs an inner vertex (f &gt;= 2); the
     * total is strictly decreasing in tau from infinity to 0, so tau is unique.
     */
    public static double stretchFactor(Polygon p, double sigma) {
        double[] e = p.getOnsets();
        if (e.length < 3) {
            throw new IllegalArgumentException("Cor. 21 needs at least one inner vertex; this polygon has "
                    + e.length + " vertices");
        }
        if (!(sigma > 0) || Double.isInfinite(sigma)) {
            throw new IllegalArgumentException("sigma must be positive: " + sigma);
        }
        double a = e[0], b = e[e.length - 1];
        double target = sigma * p.integral(a, b);
        double lo = 1, hi = 1;
        while (scaled(p, lo).integral(a, b) < target) {
            lo /= 2;
        }
        while (scaled(p, hi).integral(a, b) > target) {
            hi *= 2;
        }
        for (int i = 0; i < 200 && hi - lo > 1e-15 * hi; i++) {
            double mid = 0.5 * (lo + hi);
            if (scaled(p, mid).integral(a, b) > target) {
                lo = mid;
            }
            else {
                hi = mid;
            }
        }
        return 0.5 * (lo + hi);
    }

    /** Cor. 21: the polygon with inner vertices scaled by {@link #stretchFactor}. */
    public static Polygon stretch(Polygon p, double sigma) {
        return scaled(p, stretchFactor(p, sigma));
    }
}
