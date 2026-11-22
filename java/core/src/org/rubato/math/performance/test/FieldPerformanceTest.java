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

import org.rubato.math.performance.Field;
import org.rubato.math.performance.Fields;
import org.rubato.math.performance.InitialSet;
import org.rubato.math.performance.PerformanceCell;
import org.rubato.math.performance.TempoCurve;
import org.rubato.math.performance.TempoCurves;

/**
 * Performance from a field and an initial set (Mazzola, <i>The Topos of Music II</i>,
 * eq. (33.12) p. 568; &sect;34.5, eqs. (34.2)-(34.3) p. 579; Def. 101 p. 586). The RK4
 * integral curves must reproduce the closed-form performance maps to 1e-8.
 */
public class FieldPerformanceTest extends TestCase {

    private static final double TOL = 1e-8;

    /** A one-dimensional tempo field performs eq. (33.1). */
    public void testTempoFieldPerformsEquation331() {
        TempoCurve t = TempoCurves.linear(2, -0.1);
        PerformanceCell cell = PerformanceCell.onset(t, 0, 0.1);
        for (double e = 0; e <= 12; e += 1.5) {
            PerformanceCell.Performed p = cell.perform(new double[] { e });
            assertEquals("onset " + e, 0.1 + t.integral(0, e), p.value()[0], TOL);
            assertEquals("curve parameter", -t.integral(0, e), p.parameter(), TOL);
            assertEquals("hit point", 0.0, p.point()[0], 1e-12);
        }
    }

    /**
     * The parallel articulation field with the initial set of &sect;34.5 (left line E = Ea and
     * bottom line D = Da = 0) performs F(E, D) = (f(E), f(E+D) - f(E)), whichever line the
     * curve meets.
     */
    public void testArticulationFieldMatchesClosedForm() {
        TempoCurve t = TempoCurves.sine(1, 0.4);
        double ea = 0.25;
        PerformanceCell cell = PerformanceCell.articulation(t, 0, ea, 0);
        int left = 0, bottom = 0;
        double[][] points = { { 0.5, 0.5 }, { 2.0, 1.0 }, { 4.0, 0.25 }, { 6.0, 2.0 }, { 1.0, 3.0 }, { 0.2, 5.0 },
                              { 7.5, 0.75 }, { 3.0, 3.0 } };
        for (double[] x : points) {
            PerformanceCell.Performed p = cell.perform(x);
            double fe = ea + t.integral(0, x[0]);
            double fd = t.integral(x[0], x[0] + x[1]);
            assertEquals("onset at " + x[0] + "," + x[1], fe, p.value()[0], TOL);
            assertEquals("duration at " + x[0] + "," + x[1], fd, p.value()[1], TOL);
            if (Math.abs(p.point()[0]) < 1e-12) {
                left++;
            }
            else if (Math.abs(p.point()[1]) < 1e-12) {
                bottom++;
            }
            else {
                fail("hit point off the initial set: " + p.point()[0] + "," + p.point()[1]);
            }
        }
        assertTrue("some curves meet the left line (34.3): " + left, left > 0);
        assertTrue("some curves meet the bottom line (34.2): " + bottom, bottom > 0);
    }

    /** Dilated articulation (Example 51) against F_lambda(E, D) = (f(E), lambda (f(E+D) - f(E))). */
    public void testDilatedArticulation() {
        TempoCurve t = TempoCurves.linear(1.5, 0.05);
        double lambda = 0.6;
        Field z = Fields.parallel(t, lambda);
        PerformanceCell cell = new PerformanceCell(z, InitialSet.lines(new int[] { 0, 1 }, new double[] { 0, 0 }),
                x -> new double[] { t.integral(0, x[0]), lambda * t.integral(x[0], x[0] + x[1]) });
        for (double[] x : new double[][] { { 1, 0.5 }, { 3, 2 }, { 0.5, 4 } }) {
            PerformanceCell.Performed p = cell.perform(x);
            assertEquals(t.integral(0, x[0]), p.value()[0], TOL);
            assertEquals(lambda * t.integral(x[0], x[0] + x[1]), p.value()[1], TOL);
        }
    }

    /**
     * Def. 101 condition 5 with the sign of (33.12): for curve points a, b at parameters
     * alpha &lt; beta, p(b) - p(a) = (beta - alpha) Delta. The book prints (alpha - beta)
     * (error E5 in PERFORMANCE-SPEC.md).
     */
    public void testConsistencySign() {
        TempoCurve t = TempoCurves.sine(1, 0.4);
        PerformanceCell cell = PerformanceCell.articulation(t, 0, 0, 0);
        double[] x = { 5.0, 1.0 };
        double alpha = -0.9, beta = -0.3;
        double[] a = cell.curve(x, alpha);
        double[] b = cell.curve(x, beta);
        double[] pa = cell.perform(a).value();
        double[] pb = cell.perform(b).value();
        for (int i = 0; i < 2; i++) {
            assertEquals("component " + i, beta - alpha, pb[i] - pa[i], TOL);
        }
        assertTrue("the later curve point is performed later", pb[0] > pa[0]);
    }

    /** A point whose backward curve never meets the initial set is refused. */
    public void testUnreachablePointRefused() {
        Field z = new Field() {
            public int dimension() { return 2; }
            public double[] at(double[] x) { return new double[] { 1, 1 }; }
        };
        PerformanceCell cell = new PerformanceCell(z, InitialSet.lines(new int[] { 0 }, new double[] { 0 }),
                x -> x.clone()).withMaxParameter(50);
        // on or behind the line: performed by the initial performance itself
        assertEquals(-1.0, cell.perform(new double[] { -1, 0 }).value()[0], 0);
        Field away = new Field() {
            public int dimension() { return 2; }
            public double[] at(double[] x) { return new double[] { -1, 0 }; }
        };
        PerformanceCell lost = new PerformanceCell(away, InitialSet.lines(new int[] { 0 }, new double[] { 0 }),
                x -> x.clone()).withMaxParameter(50);
        try {
            lost.perform(new double[] { 1, 0 });
            fail("a curve that moves away from the initial set was performed");
        }
        catch (IllegalArgumentException expected) { /* K must lie in I^(int -), Def. 101 condition 4 */ }
    }
}
