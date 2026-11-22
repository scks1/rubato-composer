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
import org.rubato.math.performance.TempoCurve;
import org.rubato.math.performance.TempoCurves;

/**
 * Performance fields (Mazzola, <i>The Topos of Music II</i>, &sect;33.2: eq. (33.7)
 * p. 565, eq. (33.9) and Fig. 33.2 p. 567, eqs. (33.10)-(33.11) p. 568; Example 51
 * p. 591, which answers Exercise 77 p. 567). Each closed-form field is compared with
 * (J F)^-1 Delta computed by central differences from the performance map F.
 */
public class FieldTest extends TestCase {

    private static final TempoCurve T = TempoCurves.sine(1, 0.4);
    private static final double FD = 1e-7;

    private static double f(double e) {
        return T.integral(0, e);
    }

    private static void assertVec(String what, double[] expected, double[] actual, double tol) {
        assertEquals(what + " dimension", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actual[i]) > tol * Math.max(1, Math.abs(expected[i]))) {
                fail(what + "[" + i + "]: expected " + expected[i] + " got " + actual[i]);
            }
        }
    }

    /** (33.9): the parallel articulation field is (J F)^-1 Delta for F(E, D) = (f(E), f(E+D) - f(E)). */
    public void testParallelFieldIsInverseJacobian() {
        Field z = Fields.parallel(T);
        assertEquals(2, z.dimension());
        for (double e = 0.3; e < 7; e += 1.3) {
            for (double d = 0.25; d < 3; d += 0.8) {
                double[] x = { e, d };
                double[] fd = Fields.inverseJacobian(y -> new double[] { f(y[0]), f(y[0] + y[1]) - f(y[0]) }, x, 1e-5);
                assertVec("(33.9) at " + e + "," + d, fd, z.at(x), FD);
                assertVec("(33.9) closed", new double[] { T.tempo(e), 2 * T.tempo(e + d) - T.tempo(e) }, z.at(x), 1e-15);
            }
        }
    }

    /** Example 51 / Exercise 77: duration dilated by lambda gives ((1+l)/l) T(E+D) - T(E). */
    public void testDilatedParallelField() {
        for (double lambda : new double[] { 0.5, 1.0, 2.0, 3.5 }) {
            Field z = Fields.parallel(T, lambda);
            for (double e = 0.5; e < 6; e += 1.7) {
                for (double d = 0.3; d < 2.5; d += 0.9) {
                    double[] x = { e, d };
                    final double l = lambda;
                    double[] fd = Fields.inverseJacobian(y -> new double[] { f(y[0]), l * (f(y[0] + y[1]) - f(y[0])) }, x, 1e-5);
                    assertVec("lambda " + lambda + " at " + e + "," + d, fd, z.at(x), FD);
                }
            }
        }
        assertVec("lambda 1 = (33.9)", Fields.parallel(T).at(new double[] { 1.1, 0.7 }),
                Fields.parallel(T, 1).at(new double[] { 1.1, 0.7 }), 1e-15);
    }

    /** (33.10): the generic parallel operator on a basis field reproduces (33.9) for B = E. */
    public void testGenericParallelOperator() {
        Field generic = Fields.parallel(Fields.tempo(T));
        Field direct = Fields.parallel(T);
        for (double e = -1; e < 5; e += 0.9) {
            double[] x = { e, 0.6 };
            assertVec("(33.10) = (33.9)", direct.at(x), generic.at(x), 1e-15);
        }
        // two basis coordinates (onset, pitch) with their pianola partners (duration, glissando)
        Field b = Fields.product(Fields.tempo(T), Fields.tempo(TempoCurves.constant(3)));
        Field pb = Fields.parallel(b);
        assertEquals(4, pb.dimension());
        double[] q = { 1.0, 60.0, 0.5, 2.0 };
        assertVec("(33.10) on EH+DG", new double[] { T.tempo(1.0), 3, 2 * T.tempo(1.5) - T.tempo(1.0), 3 }, pb.at(q), 1e-15);
    }

    /** Fig. 33.2: the direction of the parallel field lies in ]-pi/4, pi/2[. */
    public void testFig332DirectionRange() {
        Field z = Fields.parallel(T);
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double e = 0; e <= 7; e += 0.05) {
            for (double d = 0; d <= 3; d += 0.05) {
                double[] v = z.at(new double[] { e, d });
                double a = Math.atan2(v[1], v[0]);
                min = Math.min(min, a);
                max = Math.max(max, a);
            }
        }
        assertTrue("above -pi/4: " + min, min > -Math.PI / 4);
        assertTrue("below pi/2: " + max, max < Math.PI / 2);
        // for T = 1 + 0.4 sin the lower bound is atan(2*0.6/1.4 - 1) = atan(-1/7)
        assertTrue("near atan(-1/7)", min >= Math.atan(-1.0 / 7) - 1e-12 && min < Math.atan(-1.0 / 7) + 0.01);
    }

    /** (33.7): the product field of tempo and intonation is (J F)^-1 Delta of (f(E), g(H)). */
    public void testProductField() {
        final TempoCurve i = TempoCurves.linear(12, 0.05);
        Field z = Fields.product(Fields.tempo(T), Fields.tempo(i));
        double[] x = { 2.2, 64 };
        double[] fd = Fields.inverseJacobian(y -> new double[] { f(y[0]), i.integral(60, y[1]) }, x, 1e-4);
        assertVec("(33.7)", fd, z.at(x), FD);
        try {
            Fields.parallel(Fields.product(Fields.tempo(T), Fields.tempo(i)), 2);
            fail("dilation of a two-dimensional basis accepted");
        }
        catch (IllegalArgumentException expected) { /* only one basis coordinate */ }
    }
}
