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

package org.rubato.math.performance.test;

import junit.framework.TestCase;

import org.rubato.math.performance.Polygon;
import org.rubato.math.performance.StepTempo;
import org.rubato.math.performance.Tempo;
import org.rubato.math.performance.TempoCurve;
import org.rubato.math.performance.TempoCurves;

/**
 * Tempo curves and onset performance (Mazzola, <i>The Topos of Music II</i>,
 * &sect;33.1.1, eq. (33.1) p. 562, Exercise 76 p. 563; Example 49 p. 552;
 * &sect;34.2, Prop. 56 p. 576). Closed forms are compared with the adaptive
 * Simpson rule of {@link TempoCurves#simpson}, an independent evaluation.
 */
public class TempoCurveTest extends TestCase {

    private static final double CLOSED = 1e-12;
    private static final double NUMERIC = 1e-10;

    private static void assertRel(String what, double expected, double actual, double tol) {
        double scale = Math.max(1.0, Math.abs(expected));
        if (Math.abs(expected - actual) > tol * scale) {
            fail(what + ": expected " + expected + " got " + actual);
        }
    }

    private static void checkAgainstSimpson(String what, TempoCurve t, double a, double b) {
        double numeric = TempoCurves.simpson(x -> 1.0 / t.tempo(x), a, b, 1e-13);
        assertRel(what + " [" + a + ", " + b + "]", numeric, t.integral(a, b), NUMERIC);
    }

    /** p. 562: M.M. quarter = 120 means a quarter lasts 0.5 s. */
    public void testMetronome120() {
        TempoCurve mm120 = TempoCurves.constant(120);
        assertEquals(0.5, 60 * mm120.integral(0, 1), CLOSED);
        assertEquals(0.5, Tempo.seconds(mm120, 0, 0, 1), CLOSED);
    }

    /** Constant tempo gives linear onsets: e(E) = e0 + (E - E0)/q0. */
    public void testConstantTempoIsLinear() {
        TempoCurve t = TempoCurves.constant(80);
        for (double e = -3; e <= 12; e += 0.75) {
            assertRel("onset " + e, 2.0 + (e - 1.0) / 80, Tempo.onset(t, 1.0, 2.0, e), CLOSED);
        }
    }

    /** A linear ritardando q0 + q1 X, q1 &lt; 0, gives logarithmic onsets. */
    public void testLinearRitardandoIsLogarithmic() {
        double q0 = 120, q1 = -6;
        TempoCurve t = TempoCurves.linear(q0, q1);
        for (double e = 0; e <= 15; e += 0.5) {
            double closed = Math.log((q0 + q1 * e) / q0) / q1;
            assertRel("rit " + e, closed, t.integral(0, e), CLOSED);
            checkAgainstSimpson("rit", t, 0, e);
        }
        // an accelerando too, and q1 = 0 falls back to the constant tempo
        checkAgainstSimpson("accel", TempoCurves.linear(60, 4), 0, 20);
        assertRel("q1=0", 3.0 / 90, TempoCurves.linear(90, 0).integral(0, 3), CLOSED);
    }

    /** Exercise 76: quadratic tempo in the three discriminant cases. */
    public void testQuadraticTempo() {
        double[][] qs = { { 1, 0.2, 0.05 }, { 1, 2, 1 }, { 1, 3, 1 }, { 2, -0.3, 0.02 } };
        for (double[] q : qs) {
            TempoCurve t = TempoCurves.quadratic(q[0], q[1], q[2]);
            for (double b = 0.5; b <= 12; b += 1.5) {
                checkAgainstSimpson("quadratic " + q[0] + "," + q[1] + "," + q[2], t, 0, b);
            }
        }
    }

    /** Exercise 76: tempo e^X and its scaled form c e^(kX). */
    public void testExponentialTempo() {
        TempoCurve t = TempoCurves.exponential(1, 1);
        for (double b = 0; b <= 10; b += 0.5) {
            assertRel("e^X", 1 - Math.exp(-b), t.integral(0, b), CLOSED);
        }
        checkAgainstSimpson("3e^(0.2X)", TempoCurves.exponential(3, 0.2), -2, 9);
    }

    /** Exercise 76: tempo 2 + sin X across several periods (branch of atan). */
    public void testSineTempo() {
        TempoCurve t = TempoCurves.sine(2, 1);
        for (double b = -7; b <= 25; b += 0.7) {
            checkAgainstSimpson("2+sin", t, 0, b);
        }
        checkAgainstSimpson("1+0.4 sin", TempoCurves.sine(1, 0.4), 1.3, 17.9);
        // one full period: 2 pi / sqrt(3)
        assertRel("period", 2 * Math.PI / Math.sqrt(3), t.integral(0, 2 * Math.PI), CLOSED);
    }

    /** Example 49: a step tempo integrates to a piecewise linear onset map. */
    public void testStepTempoPiecewiseLinear() {
        StepTempo s = new StepTempo(new double[] { 0, 4, 8 }, new double[] { 60, 120, 90 });
        assertRel("bar 1", 4.0 / 60, s.integral(0, 4), CLOSED);
        assertRel("bars 1-2", 4.0 / 60 + 4.0 / 120, s.integral(0, 8), CLOSED);
        assertRel("beyond", 4.0 / 60 + 4.0 / 120 + 2.0 / 90, s.integral(0, 10), CLOSED);
        assertRel("before", 1.0 / 60, s.integral(-1, 0), CLOSED);
        assertEquals(120.0, s.tempo(5), 0);
        assertEquals(60.0, s.tempo(-2), 0);
        // linear between breakpoints
        double mid = s.integral(0, 6);
        assertRel("linear piece", (s.integral(0, 4) + s.integral(0, 8)) / 2, mid, CLOSED);
    }

    /** Prop. 56: a linear piece integrates to (E1-E0)(ln T1 - ln T0)/(T1 - T0). */
    public void testPolygonSegment() {
        Polygon p = new Polygon(new double[] { 1, 3 }, new double[] { 2, 7 });
        assertRel("segment", 2 * (Math.log(7) - Math.log(2)) / 5, p.integral(1, 3), CLOSED);
        assertRel("segment helper", p.integral(1, 3), Tempo.segmentIntegral(1, 3, 2, 7), CLOSED);
        // T1 -> T0 limit is (E1 - E0)/T0
        assertRel("limit", 2.0 / 2, Tempo.segmentIntegral(1, 3, 2, 2 * (1 + 1e-10)), 1e-9);
        assertRel("equal", 1.0, Tempo.segmentIntegral(1, 3, 2, 2), CLOSED);
        // constant outside [E0, Ef]
        assertRel("left", 1.0 / 2, p.integral(0, 1), CLOSED);
        assertRel("right", 2.0 / 7, p.integral(3, 5), CLOSED);
        Polygon q = new Polygon(new double[] { 0, 1, 2.5, 4 }, new double[] { 2.0, 1.6, 1.3, 2.2 });
        checkAgainstSimpson("polygon", q, -1, 5);
        checkAgainstSimpson("polygon inner", q, 0.3, 3.1);
        assertRel("reverse", -q.integral(0.3, 3.1), q.integral(3.1, 0.3), CLOSED);
    }

    /** Numeric curves and refusals. */
    public void testNumericCurveAndRefusals() {
        TempoCurve f = TempoCurves.function(x -> 1 + 0.4 * Math.sin(x));
        assertRel("numeric = closed", TempoCurves.sine(1, 0.4).integral(0, 9), f.integral(0, 9), NUMERIC);
        try {
            TempoCurves.constant(0);
            fail("zero tempo accepted");
        }
        catch (IllegalArgumentException expected) { /* refused */ }
        try {
            TempoCurves.sine(1, 1);
            fail("tempo 1 + sin X reaches 0 and must be refused");
        }
        catch (IllegalArgumentException expected) { /* refused */ }
        try {
            TempoCurves.linear(10, -1).integral(0, 12);
            fail("tempo 10 - X is not positive on [0, 12]");
        }
        catch (IllegalArgumentException expected) { /* refused */ }
        try {
            new Polygon(new double[] { 0, 1 }, new double[] { 1, -1 });
            fail("negative vertex accepted");
        }
        catch (IllegalArgumentException expected) { /* refused */ }
    }
}
