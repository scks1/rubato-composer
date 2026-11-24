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

import java.util.Random;

import junit.framework.TestCase;

import org.rubato.math.performance.FieldExtraction;

/**
 * Field interpolation (Mazzola, <i>The Topos of Music II</i>, &sect;47.3.5.1 p. 764, with the
 * recursive algorithm of &sect;32.3.2.1 pp. 553-554): zero-slope cubics between neighbouring
 * hyperplanes of the last axis, a frame of diagonal vectors at a fixed margin, and the
 * departures of sources/FIELD-EXTRACTION-SPEC.md D9.
 */
public class FieldInterpolationTest extends TestCase {

    private static final double TOL = 1e-12;

    private static FieldExtraction.Interpolation line() {
        return new FieldExtraction.Interpolation(new double[][] { { 0 }, { 1 } }, new double[][] { { 2 }, { 4 } }, 1);
    }

    /** Spec section 6: points 0 -> 2 and 1 -> 4, frame ]-1, 2[. */
    public void testOneDimensionalValues() {
        FieldExtraction.Interpolation f = line();
        assertEquals(2.0, f.at(new double[] { 0 })[0], TOL);
        assertEquals(4.0, f.at(new double[] { 1 })[0], TOL);
        assertEquals(3.0, f.at(new double[] { 0.5 })[0], TOL);
        assertEquals(1.5, f.at(new double[] { -0.5 })[0], TOL);
        assertEquals(2.5, f.at(new double[] { 1.5 })[0], TOL);
        assertEquals(1.0, f.at(new double[] { 2 })[0], TOL);
        assertEquals(1.0, f.at(new double[] { -1 })[0], TOL);
        assertEquals(1.0, f.at(new double[] { 17 })[0], TOL);
        assertEquals(-1.0, f.frameLow()[0], TOL);
        assertEquals(2.0, f.frameHigh()[0], TOL);
    }

    /** The cubic has zero slope at every point (Exercise 71), so the spline is C^1 there. */
    public void testZeroSlopeAtThePoints() {
        FieldExtraction.Interpolation f = line();
        double h = 1e-6;
        for (double x : new double[] { -1, 0, 1, 2 }) {
            double left = (f.at(new double[] { x })[0] - f.at(new double[] { x - h })[0]) / h;
            double right = (f.at(new double[] { x + h })[0] - f.at(new double[] { x })[0]) / h;
            assertEquals("left slope at " + x, 0, left, 1e-4);
            assertEquals("right slope at " + x, 0, right, 1e-4);
        }
    }

    /** &sect;32.3.2.1: no extrema beyond the values at the points and the frame value 1. */
    public void testNoNewExtrema() {
        Random r = new Random(553);
        double[][] xs = new double[7][1], vs = new double[7][1];
        double lo = 1, hi = 1;
        for (int i = 0; i < 7; i++) {
            xs[i][0] = i * 0.7 + r.nextDouble() * 0.3;
            vs[i][0] = 3 * r.nextGaussian();
            lo = Math.min(lo, vs[i][0]);
            hi = Math.max(hi, vs[i][0]);
        }
        FieldExtraction.Interpolation f = new FieldExtraction.Interpolation(xs, vs, 0.5);
        for (double x = -1; x < 6; x += 0.001) {
            double y = f.at(new double[] { x })[0];
            assertTrue("value " + y + " at " + x, y >= lo - 1e-12 && y <= hi + 1e-12);
        }
    }

    /**
     * Two dimensions, last axis outermost (p. 764): points (0, 0) -> a, (1, 0) -> b, (0, 1) -> c.
     * At (0.5, 0.5): on y = 0 the cubic gives (a + b)/2; on y = 1 the only point is x = 0, so
     * between 0 and the frame at 2, s = 0.25, c + (1 - c) * 0.15625; then halfway in y.
     */
    public void testTwoDimensionalRecursion() {
        double[][] pts = { { 0, 0 }, { 1, 0 }, { 0, 1 } };
        double[][] vals = { { 2, 0 }, { 4, 1 }, { 0, 3 } };
        FieldExtraction.Interpolation f = new FieldExtraction.Interpolation(pts, vals, 1);
        double[] v = f.at(new double[] { 0.5, 0.5 });
        for (int k = 0; k < 2; k++) {
            double p0 = (vals[0][k] + vals[1][k]) / 2;
            double p1 = vals[2][k] + (1 - vals[2][k]) * 0.15625;
            assertEquals("component " + k, p0 + (p1 - p0) * 0.5, v[k], TOL);
        }
        for (int i = 0; i < pts.length; i++) {
            double[] at = f.at(pts[i]);
            assertEquals(vals[i][0], at[0], TOL);
            assertEquals(vals[i][1], at[1], TOL);
        }
        double[] outside = f.at(new double[] { 0.5, 2.5 });
        assertEquals(1.0, outside[0], TOL);
        assertEquals(1.0, outside[1], TOL);
        double[] left = f.at(new double[] { -1.5, 0 });
        assertEquals("outside the frame on the first axis", 1.0, left[0], TOL);
    }

    /** The coordinate order matters (p. 554), so swapping the axes changes values off the points. */
    public void testOrderDependence() {
        double[][] pts = { { 0, 0 }, { 1, 0 }, { 0, 1 } };
        double[][] swapped = { { 0, 0 }, { 0, 1 }, { 1, 0 } };
        double[][] vals = { { 2 }, { 4 }, { 0 } };
        double a = new FieldExtraction.Interpolation(pts, vals, 1).at(new double[] { 0.5, 0.5 })[0];
        double b = new FieldExtraction.Interpolation(swapped, vals, 1).at(new double[] { 0.5, 0.5 })[0];
        assertTrue(a + " vs " + b, Math.abs(a - b) > 1e-3);
    }

    /** D9: points that coincide on the chosen axes are averaged. */
    public void testCoincidentPointsAreAveraged() {
        FieldExtraction.Interpolation f = new FieldExtraction.Interpolation(
                new double[][] { { 0 }, { 0 }, { 1 } }, new double[][] { { 2 }, { 4 }, { 5 } }, 1);
        assertEquals(3.0, f.at(new double[] { 0 })[0], TOL);
        assertEquals(1, f.coincident());
    }

    public void testRefusals() {
        refuse(new double[0][], new double[0][], 1, "no points");
        refuse(new double[][] { { 0 } }, new double[][] { { 1 }, { 2 } }, 1, "same number");
        refuse(new double[][] { { 0 } }, new double[][] { { 1 } }, 0, "margin");
        refuse(new double[][] { { 0 }, { 0, 1 } }, new double[][] { { 1 }, { 2 } }, 1, "dimension");
    }

    private static void refuse(double[][] p, double[][] v, double margin, String word) {
        try {
            new FieldExtraction.Interpolation(p, v, margin);
            fail(word);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(word));
        }
    }
}
