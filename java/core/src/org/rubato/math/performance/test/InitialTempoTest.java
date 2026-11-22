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
import org.rubato.math.performance.Tempo;
import org.rubato.math.performance.TempoCurves;

/**
 * Tempo curves adapted to initial performances (Mazzola, <i>The Topos of
 * Music II</i>, &sect;34.2, eq. (34.1), Prop. 56 and Cor. 21, p. 576).
 */
public class InitialTempoTest extends TestCase {

    private static final double[] E = { 0, 1, 2, 3, 4 };
    private static final double[] T = { 2.0, 1.6, 1.3, 1.5, 2.2 };

    private static double[] performed(double[] onsets, double[] tempi, double e0) {
        Polygon p = new Polygon(onsets, tempi);
        double[] e = new double[onsets.length];
        e[0] = e0;
        for (int i = 1; i < e.length; i++) {
            e[i] = e[i - 1] + p.integral(onsets[i - 1], onsets[i]);
        }
        return e;
    }

    /** Prop. 56 with the first tempo prescribed: the solver recovers the polygon. */
    public void testProp56RecoversPolygonFromFirstTempo() {
        double[] e = performed(E, T, 0.25);
        Polygon p = Tempo.adapt(E, e, 0, T[0]);
        for (int i = 0; i < T.length; i++) {
            assertEquals("T" + i, T[i], p.getTempi()[i], 1e-10 * T[i]);
        }
        // adapted: (34.1) holds for every pair
        for (int i = 0; i < E.length; i++) {
            for (int j = i + 1; j < E.length; j++) {
                assertEquals(e[j] - e[i], p.integral(E[i], E[j]), 1e-12);
            }
        }
    }

    /** Prop. 56 with an inner tempo prescribed (the "general case" of the proof). */
    public void testProp56FromInnerTempo() {
        double[] e = performed(E, T, 0);
        Polygon p = Tempo.adapt(E, e, 2, T[2]);
        for (int i = 0; i < T.length; i++) {
            assertEquals("T" + i, T[i], p.getTempi()[i], 1e-10 * T[i]);
        }
    }

    /**
     * Prop. 56, proof: q(x) = (E1 - E0)(log x - log T(E0))/(x - T(E0)) is the integral of
     * the linear tempo through (E0, T(E0)) and (E1, x). The printed curve multiplies by
     * (E1 - E0) where it must divide (book error E1 in PERFORMANCE-SPEC.md); only the
     * divided form integrates to q.
     */
    public void testProp56ProofCurve() {
        double E0 = 1, E1 = 3.5, T0 = 1.7, x = 0.6;
        double q = (E1 - E0) * (Math.log(x) - Math.log(T0)) / (x - T0);
        double divided = TempoCurves.simpson(s -> 1 / ((x - T0) * (s - E0) / (E1 - E0) + T0), E0, E1, 1e-13);
        assertEquals(q, divided, 1e-11);
        double printedAtE1 = (x - T0) * (E1 - E0) * (E1 - E0) + T0;
        assertTrue("the printed curve misses x at E1", Math.abs(printedAtE1 - x) > 0.1);
    }

    /** q is strictly decreasing, which the uniqueness claim needs (gap E2). */
    public void testSegmentIntegralStrictlyDecreasing() {
        double prev = Double.POSITIVE_INFINITY;
        for (double x = 0.01; x < 100; x *= 1.3) {
            double q = Tempo.segmentIntegral(0, 2, 1.5, x);
            assertTrue("decreasing at " + x, q < prev);
            prev = q;
        }
    }

    /** Prop. 56 refuses data that are not increasing in either coordinate. */
    public void testProp56Refusals() {
        try {
            Tempo.adapt(new double[] { 0, 1, 1 }, new double[] { 0, 1, 2 }, 0, 1);
            fail("non-increasing onsets accepted");
        }
        catch (IllegalArgumentException expected) { /* refused */ }
        try {
            Tempo.adapt(new double[] { 0, 1, 2 }, new double[] { 0, 1, 1 }, 0, 1);
            fail("non-increasing performed onsets accepted");
        }
        catch (IllegalArgumentException expected) { /* refused */ }
    }

    /** Cor. 21: scaling the inner vertices by tau reaches sigma * Delta. */
    public void testCor21StretchesTotalDuration() {
        Polygon p = new Polygon(E, T);
        double delta = p.integral(E[0], E[4]);
        for (double sigma : new double[] { 0.5, 0.9, 1.0, 1.25, 3.0 }) {
            Polygon q = Tempo.stretch(p, sigma);
            assertEquals("sigma " + sigma, sigma * delta, q.integral(E[0], E[4]), 1e-11);
            assertEquals("T0 kept", T[0], q.getTempi()[0], 0);
            assertEquals("Tf kept", T[4], q.getTempi()[4], 0);
            double tau = q.getTempi()[1] / T[1];
            for (int i = 1; i < 4; i++) {
                assertEquals("inner vertex " + i + " scaled by tau", tau * T[i], q.getTempi()[i], 1e-12);
            }
        }
        assertEquals(1.0, Tempo.stretchFactor(p, 1.0), 1e-12);
    }

    /** Cor. 21 needs an inner vertex (gap E3): with f = 1 only sigma = 1 is reachable. */
    public void testCor21NeedsInnerVertex() {
        Polygon p = new Polygon(new double[] { 0, 2 }, new double[] { 1, 2 });
        try {
            Tempo.stretch(p, 1.5);
            fail("Cor. 21 applied without inner vertices");
        }
        catch (IllegalArgumentException expected) { /* refused */ }
        // f = 2: one inner vertex suffices
        Polygon r = new Polygon(new double[] { 0, 1, 2 }, new double[] { 1, 1.5, 2 });
        double d = r.integral(0, 2);
        assertEquals(0.7 * d, Tempo.stretch(r, 0.7).integral(0, 2), 1e-11);
    }
}
