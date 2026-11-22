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
 * A step tempo curve: T_i on [E_i, E_{i+1}), T_0 before E_0 and the last tempo after
 * the last breakpoint. It integrates to a piecewise linear onset map (Mazzola,
 * <i>The Topos of Music II</i>, Example 49, p. 552).
 */
public final class StepTempo implements TempoCurve {

    private final double[] breaks;
    private final double[] tempi;
    private final double[] cumulative;

    public StepTempo(double[] breaks, double[] tempi) {
        if (breaks.length == 0 || breaks.length != tempi.length) {
            throw new IllegalArgumentException("a step tempo needs as many tempi as breakpoints, at least one");
        }
        for (int i = 0; i < breaks.length; i++) {
            TempoCurves.requirePositive(tempi[i], breaks[i]);
            if (i > 0 && !(breaks[i] > breaks[i - 1])) {
                throw new IllegalArgumentException("breakpoints must increase");
            }
        }
        this.breaks = breaks.clone();
        this.tempi = tempi.clone();
        cumulative = new double[breaks.length];
        for (int i = 1; i < breaks.length; i++) {
            cumulative[i] = cumulative[i - 1] + (breaks[i] - breaks[i - 1]) / tempi[i - 1];
        }
    }

    private int piece(double e) {
        int i = 0;
        while (i + 1 < breaks.length && breaks[i + 1] <= e) {
            i++;
        }
        return i;
    }

    public double tempo(double e) {
        return e < breaks[0] ? tempi[0] : tempi[piece(e)];
    }

    private double antiderivative(double e) {
        if (e <= breaks[0]) {
            return (e - breaks[0]) / tempi[0];
        }
        int i = piece(e);
        return cumulative[i] + (e - breaks[i]) / tempi[i];
    }

    public double integral(double a, double b) {
        return antiderivative(b) - antiderivative(a);
    }
}
