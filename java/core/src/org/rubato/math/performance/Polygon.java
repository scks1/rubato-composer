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
 * A continuous tempo curve, linear between its vertices (E_i, T_i) and constant
 * outside [E_0, E_f] (Mazzola, <i>The Topos of Music II</i>, Prop. 56, p. 576).
 * A linear piece from (E0, T0) to (E1, T1) integrates to
 * (E1 - E0)(ln T1 - ln T0)/(T1 - T0), the reciprocal of the logarithmic mean.
 */
public final class Polygon implements TempoCurve {

    private final double[] onsets;
    private final double[] tempi;
    private final double[] cumulative;

    public Polygon(double[] onsets, double[] tempi) {
        if (onsets.length == 0 || onsets.length != tempi.length) {
            throw new IllegalArgumentException("a polygon needs as many tempi as onsets, at least one");
        }
        for (int i = 0; i < onsets.length; i++) {
            TempoCurves.requirePositive(tempi[i], onsets[i]);
            if (i > 0 && !(onsets[i] > onsets[i - 1])) {
                throw new IllegalArgumentException("polygon onsets must increase: " + onsets[i - 1] + ", " + onsets[i]);
            }
        }
        this.onsets = onsets.clone();
        this.tempi = tempi.clone();
        cumulative = new double[onsets.length];
        for (int i = 1; i < onsets.length; i++) {
            cumulative[i] = cumulative[i - 1] + Tempo.segmentIntegral(onsets[i - 1], onsets[i], tempi[i - 1], tempi[i]);
        }
    }

    public double[] getOnsets() { return onsets.clone(); }

    public double[] getTempi() { return tempi.clone(); }

    public double tempo(double e) {
        int n = onsets.length;
        if (e <= onsets[0]) {
            return tempi[0];
        }
        if (e >= onsets[n - 1]) {
            return tempi[n - 1];
        }
        int i = segment(e);
        return tempi[i] + (tempi[i + 1] - tempi[i]) * (e - onsets[i]) / (onsets[i + 1] - onsets[i]);
    }

    private int segment(double e) {
        int lo = 0, hi = onsets.length - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (onsets[mid] <= e) {
                lo = mid;
            }
            else {
                hi = mid;
            }
        }
        return lo;
    }

    /** The integral of 1/T from E_0 to e. */
    private double antiderivative(double e) {
        int n = onsets.length;
        if (e <= onsets[0]) {
            return (e - onsets[0]) / tempi[0];
        }
        if (e >= onsets[n - 1]) {
            return cumulative[n - 1] + (e - onsets[n - 1]) / tempi[n - 1];
        }
        int i = segment(e);
        return cumulative[i] + Tempo.segmentIntegral(onsets[i], e, tempi[i], tempo(e));
    }

    public double integral(double a, double b) {
        return antiderivative(b) - antiderivative(a);
    }

    public String toString() {
        StringBuilder s = new StringBuilder("polygon");
        for (int i = 0; i < onsets.length; i++) {
            s.append(i == 0 ? " " : ", ").append(onsets[i]).append(':').append(tempi[i]);
        }
        return s.toString();
    }
}
