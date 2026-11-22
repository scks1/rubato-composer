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

import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;

import junit.framework.TestCase;

import org.rubato.math.performance.BasisPianolaOperator;
import org.rubato.math.performance.Field;
import org.rubato.math.performance.Fields;
import org.rubato.math.performance.InitialSet;
import org.rubato.math.performance.PerformanceCell;
import org.rubato.math.performance.ScalarOperator;
import org.rubato.math.performance.TempoCurve;
import org.rubato.math.performance.TempoCurves;
import org.rubato.math.performance.TempoOperator;

/**
 * Field operators (Mazzola, <i>The Topos of Music II</i>, &sect;39.5 tempo operator,
 * eqs. (39.32)-(39.36) and duration preservation, pp. 653-654; &sect;39.6 scalar
 * operator, (39.37)-(39.38), pp. 654-655; &sect;39.7 basis-pianola operators, (39.43),
 * (39.44), Lemma 53 and Example 55, pp. 655-657).
 */
public class OperatorTest extends TestCase {

    private static final TempoCurve T = TempoCurves.sine(1, 0.4);
    private static final DoubleUnaryOperator W = x -> 1 + 0.3 * Math.cos(0.7 * x);

    private static void assertVec(String what, double[] expected, double[] actual, double tol) {
        assertEquals(what + " dimension", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actual[i]) > tol * Math.max(1, Math.abs(expected[i]))) {
                fail(what + "[" + i + "]: expected " + expected[i] + " got " + actual[i]);
            }
        }
    }

    /** (39.32): the weighted tempo w T. */
    public void testTempoOperator() {
        TempoCurve tw = TempoOperator.apply(T, W);
        for (double e = 0; e < 9; e += 0.7) {
            assertEquals(W.applyAsDouble(e) * T.tempo(e), tw.tempo(e), 1e-15);
        }
        double numeric = TempoCurves.simpson(x -> 1 / (W.applyAsDouble(x) * T.tempo(x)), 0, 9, 1e-13);
        assertEquals(numeric, tw.integral(0, 9), 1e-10);
        try {
            TempoOperator.apply(T, x -> x - 3).integral(0, 9);
            fail("a weight that is not strictly positive was accepted");
        }
        catch (IllegalArgumentException expected) { /* (39.32) needs w > 0 */ }
    }

    /** (39.35)-(39.36): Q_w acts on the articulation field, and Q_w dT = d(wT). */
    public void testQwIsParallelFieldOfWeightedTempo() {
        double[][] q = TempoOperator.q(W, 1.0, 0.5);
        assertEquals(W.applyAsDouble(1.0), q[0][0], 0);
        assertEquals(0.0, q[0][1], 0);
        assertEquals(W.applyAsDouble(1.5) - W.applyAsDouble(1.0), q[1][0], 1e-15);
        assertEquals(W.applyAsDouble(1.5), q[1][1], 0);
        Field deformed = TempoOperator.articulation(Fields.parallel(T), W);
        Field parallelOfWT = Fields.parallel(TempoOperator.apply(T, W));
        for (double e = 0.2; e < 6; e += 1.1) {
            for (double d = 0.5; d < 3; d += 1) {
                double[] x = { e, d };
                assertVec("Q_w dT at " + e + "," + d, parallelOfWT.at(x), deformed.at(x), 1e-14);
            }
        }
    }

    /**
     * p. 653-654: iterating w_(k+1) = r(T, w_k) w_k keeps the duration of [E0, E1]. The book
     * does not know whether this converges; here it does, and every ratio is reported.
     */
    public void testDurationPreservation() {
        // the first two ratios of the p. 654 iteration (proto: 1.152724470, 1.005756744); two steps only,
        // so that a wrong update of c fails here before the 50-step run below
        TempoOperator.Preservation two = TempoOperator.preserveDuration(T, W, 0, 8, 0.5, 1e-12, 2);
        assertEquals("second ratio (proto 1.005756744)", 1.005756744, two.getRatios().get(1), 1e-8);
        TempoOperator.Preservation p = TempoOperator.preserveDuration(T, W, 0, 8, 0.5, 1e-12, 50);
        assertTrue("converged", p.isConverged());
        assertEquals("first ratio (proto 1.152724470)", 1.152724470, p.getRatios().get(0), 1e-8);
        TempoCurve s = p.getCurve();
        assertEquals("duration kept", T.integral(0, 8), s.integral(0, 8), 1e-10);
        assertEquals("unchanged outside", T.tempo(-1), s.tempo(-1), 0);
        assertEquals("unchanged outside", T.tempo(9), s.tempo(9), 0);
        double c = p.getFactor();
        assertEquals("c w T inside", c * W.applyAsDouble(4) * T.tempo(4), s.tempo(4), 1e-12);
        assertEquals("support", 1.0, TempoOperator.support(4, 0, 8, 0.5), 0);
        assertEquals("support", 0.5, TempoOperator.support(0.25, 0, 8, 0.5), 1e-15);
        assertEquals("support", 0.0, TempoOperator.support(8.5, 0, 8, 0.5), 0);
        TempoOperator.Preservation capped = TempoOperator.preserveDuration(T, W, 0, 8, 0.5, 1e-12, 1);
        assertFalse("the cap is reported", capped.isConverged());
        assertEquals(1, capped.getRatios().size());
    }

    /** &sect;39.6: the eight cases of the scalar operator, with w evaluated at p_B(Q). */
    public void testScalarOperatorCases() {
        Field zb = Fields.tempo(T);
        Field zbp = new Field() {
            public int dimension() { return 2; }
            public double[] at(double[] x) { return new double[] { T.tempo(x[0]), 0.5 + 0.1 * x[0] + 0.2 * x[1] }; }
        };
        ToDoubleFunction<double[]> w = b -> W.applyAsDouble(b[0]);
        double e = 1.3, d = 0.8;
        double[] x = { e, d };
        double we = W.applyAsDouble(e), ws = W.applyAsDouble(e + d);
        double[] z = zbp.at(x);
        double te = T.tempo(e), ts = T.tempo(e + d);
        assertVec("no/no", z, ScalarOperator.apply(zb, zbp, w, false, false, false).at(x), 1e-15);
        assertVec("yes/yes", new double[] { we * z[0], we * z[1] }, ScalarOperator.apply(zb, zbp, w, false, true, true).at(x), 1e-15);
        assertVec("yes/no", new double[] { we * z[0], z[1] }, ScalarOperator.apply(zb, zbp, w, false, true, false).at(x), 1e-15);
        assertVec("no/yes", new double[] { z[0], we * z[1] }, ScalarOperator.apply(zb, zbp, w, false, false, true).at(x), 1e-15);
        assertVec("parallel no/no (39.37)", new double[] { te, 2 * ts - te },
                ScalarOperator.apply(zb, zbp, w, true, false, false).at(x), 1e-15);
        assertVec("parallel yes/yes (39.38)", new double[] { we * z[0], (ws - we) * z[0] + ws * z[1] },
                ScalarOperator.apply(zb, zbp, w, true, true, true).at(x), 1e-15);
        assertVec("parallel yes/no", new double[] { we * te, 2 * ws * ts - we * te },
                ScalarOperator.apply(zb, zbp, w, true, true, false).at(x), 1e-15);
        assertVec("parallel no/yes", new double[] { we * te, we * (2 * ts - te) },
                ScalarOperator.apply(zb, zbp, w, true, false, true).at(x), 1e-15);
        // the tempo operator is the parallel yes/yes case on B = E, P = D
        assertVec("tempo operator", TempoOperator.articulation(zbp, W).at(x),
                ScalarOperator.apply(zb, zbp, w, true, true, true).at(x), 1e-15);
    }

    private static final double A = 1.3, B = 4.0;
    private static final DoubleUnaryOperator LAMBDA = x -> 0.4 * Math.sin(0.5 * x);
    private static final DoubleUnaryOperator DLAMBDA = x -> 0.2 * Math.cos(0.5 * x);

    private static Field mother() {
        return Fields.product(Fields.tempo(T), Fields.tempo(TempoCurves.constant(1 / A)));
    }

    /** (39.43): the deformed field is (J p_Lambda)^-1 Delta for p_Lambda = (x(X), e^Lambda (a Y + b)). */
    public void testBasisPianolaFieldIsInverseJacobian() {
        Field z = BasisPianolaOperator.loudness(mother(), 0, 1, LAMBDA, DLAMBDA, A, B);
        Field basisOnly = BasisPianolaOperator.loudnessBasisTerm(mother(), 0, 1, DLAMBDA, A, B);
        for (double e : new double[] { 0.3, 2.0, 5.0 }) {
            for (double y : new double[] { 40, 80, 110 }) {
                double[] x = { e, y };
                double[] fd = Fields.inverseJacobian(v -> new double[] { T.integral(0, v[0]),
                        Math.exp(LAMBDA.applyAsDouble(v[0])) * (A * v[1] + B) }, x, 1e-5);
                assertVec("(39.43) at " + e + "," + y, fd, z.at(x), 1e-7);
                assertEquals("loudness closed form", Math.exp(LAMBDA.applyAsDouble(e)) * (A * y + B),
                        BasisPianolaOperator.performedLoudness(LAMBDA.applyAsDouble(e), A, B, y), 1e-12);
            }
        }
        // the book's small-Lambda reading (p. 656) drops the pianola term; at these values it is off
        double[] x = { 2.0, 80 };
        assertTrue(Math.abs(basisOnly.at(x)[1] - z.at(x)[1]) > 0.01);
    }

    /** The loudness field performed by RK4 from {E = 0} reproduces e^Lambda(E) (a L + b). */
    public void testLoudnessPerformanceByField() {
        Field z = BasisPianolaOperator.loudness(mother(), 0, 1, LAMBDA, DLAMBDA, A, B);
        PerformanceCell cell = new PerformanceCell(z, InitialSet.lines(new int[] { 0 }, new double[] { 0 }),
                p -> new double[] { 0, Math.exp(LAMBDA.applyAsDouble(p[0])) * (A * p[1] + B) });
        for (double[] x : new double[][] { { 3, 64 }, { 7, 100 }, { 1, 20 } }) {
            double[] v = cell.perform(x).value();
            assertEquals("onset", T.integral(0, x[0]), v[0], 1e-8);
            assertEquals("loudness", Math.exp(LAMBDA.applyAsDouble(x[0])) * (A * x[1] + B), v[1], 1e-8);
        }
    }

    /** (39.44) and Lemma 53: linear in the field, additive in the weight for disjoint U, V. */
    public void testLemma53() {
        Field z1 = mother();
        Field z2 = Fields.product(Fields.tempo(TempoCurves.linear(2, 0.1)), Fields.tempo(TempoCurves.constant(0.5)));
        int[] u = { 0 }, v = { 1 };
        java.util.function.Function<double[], double[]> g1 = p -> new double[] { 0.2 * Math.cos(0.5 * p[0]) };
        java.util.function.Function<double[], double[]> g2 = p -> new double[] { 0.1 * p[0] };
        java.util.function.Function<double[], double[]> g12 = p -> new double[] { g1.apply(p)[0] + g2.apply(p)[0] };
        java.util.function.UnaryOperator<double[]> dir = y -> new double[] { 0.5 * y[0] + 3 };
        double mu = 0.7, nu = -1.2;
        Field combo = new Field() {
            public int dimension() { return 2; }
            public double[] at(double[] x) {
                double[] a = z1.at(x), b = z2.at(x);
                return new double[] { mu * a[0] + nu * b[0], mu * a[1] + nu * b[1] };
            }
        };
        double[] x = { 1.7, 70 };
        double[] lhs = BasisPianolaOperator.basis(combo, u, g1, v, dir).at(x);
        double[] a = BasisPianolaOperator.basis(z1, u, g1, v, dir).at(x);
        double[] b = BasisPianolaOperator.basis(z2, u, g1, v, dir).at(x);
        assertVec("Lemma 53 (i)", new double[] { mu * a[0] + nu * b[0], mu * a[1] + nu * b[1] }, lhs, 1e-14);
        Field twice = BasisPianolaOperator.basis(BasisPianolaOperator.basis(z1, u, g1, v, dir), u, g2, v, dir);
        assertVec("Lemma 53 (ii)", BasisPianolaOperator.basis(z1, u, g12, v, dir).at(x), twice.at(x), 1e-14);
    }

    /** Example 55: U = V = E, Dir = 1 and dLambda/dE = 1 - gamma give the tempo field gamma T. */
    public void testExample55() {
        DoubleUnaryOperator gamma = W;
        Field z = BasisPianolaOperator.basis(Fields.tempo(T), new int[] { 0 },
                p -> new double[] { 1 - gamma.applyAsDouble(p[0]) }, new int[] { 0 }, y -> new double[] { 1 });
        for (double e = 0; e < 6; e += 0.9) {
            assertEquals(gamma.applyAsDouble(e) * T.tempo(e), z.at(new double[] { e })[0], 1e-14);
        }
    }
}
