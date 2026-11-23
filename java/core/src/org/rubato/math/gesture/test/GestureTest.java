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
package org.rubato.math.gesture.test;

import java.util.function.UnaryOperator;

import junit.framework.TestCase;

import org.rubato.math.gesture.Curve;
import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.DigraphMorphism;
import org.rubato.math.gesture.FunctionCurve;
import org.rubato.math.gesture.Gesture;
import org.rubato.math.gesture.NaturalGesture;
import org.rubato.math.gesture.PolylineCurve;

/**
 * Curves, gestures and gesture morphisms in R^n (Mazzola, The Topos of
 * Music, vol. III, sections 61.5-61.8, pp. 914-920).
 */
public class GestureTest extends TestCase {

    static double[] p(double... x) {
        return x;
    }

    public void testPolylineEvaluation() {
        PolylineCurve c = PolylineCurve.through(p(0, 0), p(2, 0), p(2, 4));
        assertEquals(2, c.dimension());
        assertEquals(0.0, c.at(0)[0], 0);
        assertEquals(1.0, c.at(0.25)[0], 1e-15);
        assertEquals(2.0, c.at(0.75)[1], 1e-15);
        assertEquals(4.0, c.at(1)[1], 0);
        try {
            new PolylineCurve(new double[] { 0, 0.7, 0.5, 1 }, new double[][] { p(0), p(1), p(2), p(3) });
            fail("knots must increase");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testGestureEndpoints() {
        Digraph up = Digraph.arrowN(1);
        Gesture g = new Gesture(up, new double[][] { p(0, 0), p(1, 2) }, new Curve[] { PolylineCurve.line(p(0, 0), p(1, 2)) });
        assertEquals(0.0, g.endpointGap(), 0);
        try {
            new Gesture(up, new double[][] { p(0, 0), p(1, 2) }, new Curve[] { PolylineCurve.line(p(0, 0), p(1, 2.001)) });
            fail("curve end is not the head's point");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        // a loop's curve must be closed
        Gesture loop = new Gesture(Digraph.loop(), new double[][] { p(3, 3) },
                                   new Curve[] { PolylineCurve.through(p(3, 3), p(4, 3), p(3, 4), p(3, 3)) });
        assertEquals(1, loop.skeleton().arrowCount());
    }

    public void testConcatenationNeedsEndToStart() {
        // p. 920 prints the condition e(0) = f(1); the formula needs e(1) = f(0) (error E2)
        Curve e = PolylineCurve.line(p(0, 0), p(1, 0));
        Curve f = PolylineCurve.line(p(1, 0), p(1, 1));
        Curve ef = Curve.concat(e, f);
        assertEquals(0.5, ef.at(0.25)[0], 1e-15);
        assertEquals(0.5, ef.at(0.75)[1], 1e-15);
        assertEquals(1.0, ef.at(1)[1], 0);
        Curve g = PolylineCurve.line(p(5, 5), p(0, 0));   // g(1) = e(0), the printed condition
        try {
            Curve.concat(e, g);
            fail("e(1) != g(0)");
        }
        catch (IllegalArgumentException ex) {
            // expected
        }
        Curve sampled = Curve.concat(new FunctionCurve(2, s -> p(s, 0)), f);
        assertEquals(0.5, sampled.at(0.75)[1], 1e-15);
    }

    public void testSupDistanceExactForPolylines() {
        // the maximum sits at a knot of the second curve only
        Curve a = PolylineCurve.line(p(0, 0), p(1, 0));
        Curve b = new PolylineCurve(new double[] { 0, 0.3, 1 }, new double[][] { p(0, 0), p(0.3, 1), p(1, 0) });
        assertEquals(1.0, Curve.supDistance(a, b), 1e-15);
        assertEquals(1.0, Curve.supDistance(b, a), 1e-15);
        // sampled sup of the same curves as functions: within L/2048 (spec section 4)
        Curve fa = new FunctionCurve(2, a::at);
        Curve fb = new FunctionCurve(2, b::at);
        double d = Curve.supDistance(fa, fb);
        assertTrue(d <= 1.0 + 1e-15);
        assertTrue(d >= 1.0 - 4.0 / 2048);
    }

    public void testBookMetricIgnoresIsolatedVertices() {
        // p. 916: the metric sums over arrows, so two different point gestures are at distance 0 (error E3)
        Gesture x = new Gesture(Digraph.point(), new double[][] { p(0, 0) }, new Curve[0]);
        Gesture y = new Gesture(Digraph.point(), new double[][] { p(3, 4) }, new Curve[0]);
        assertEquals(0.0, x.bookDistance(y), 0);
        assertEquals(5.0, x.distance(y), 1e-15);
    }

    public void testBookMetricSumsArrowSups() {
        Digraph two = Digraph.arrowN(2);
        Gesture g = new Gesture(two, new double[][] { p(0, 0), p(1, 0), p(2, 0) },
                                new Curve[] { PolylineCurve.line(p(0, 0), p(1, 0)), PolylineCurve.line(p(1, 0), p(2, 0)) });
        Gesture h = new Gesture(two, new double[][] { p(0, 1), p(1, 0), p(2, 2) },
                                new Curve[] { PolylineCurve.line(p(0, 1), p(1, 0)), PolylineCurve.line(p(1, 0), p(2, 2)) });
        assertEquals(3.0, g.bookDistance(h), 1e-15);
        assertEquals(3.0, g.distance(h), 1e-15);
    }

    public void testGestureMorphism() {
        // p. 915: f with h such that h o delta = gamma o f
        Digraph up = Digraph.arrowN(1);
        Digraph two = Digraph.arrowN(2);
        Gesture src = new Gesture(up, new double[][] { p(0, 0), p(1, 0) }, new Curve[] { PolylineCurve.line(p(0, 0), p(1, 0)) });
        Gesture tgt = new Gesture(two, new double[][] { p(5, 5), p(5, 7), p(6, 7) },
                                  new Curve[] { PolylineCurve.line(p(5, 5), p(5, 7)), PolylineCurve.line(p(5, 7), p(6, 7)) });
        UnaryOperator<double[]> shift = x -> p(x[0] + 5, x[1] + 7);
        UnaryOperator<double[]> wrong = x -> p(x[0] + 5, x[1] + 6);
        DigraphMorphism onSecond = new DigraphMorphism(up, two, new int[] { 1, 2 }, new int[] { 1 });
        DigraphMorphism onFirst = new DigraphMorphism(up, two, new int[] { 0, 1 }, new int[] { 0 });
        assertTrue(src.isMorphism(onSecond, shift, tgt));
        assertFalse(src.isMorphism(onSecond, wrong, tgt));
        assertFalse(src.isMorphism(onFirst, shift, tgt));
        // composition: the image under shift is a gesture and the identity is a morphism onto it
        Gesture img = src.map(shift);
        assertTrue(src.isMorphism(DigraphMorphism.identity(up), shift, img));
        assertTrue(img.isMorphism(onSecond, x -> x, tgt));
    }

    public void testNaturalGestureAsPrinted() {
        // p. 919 draws arrow a as ((1-s)e_t + s e_h, exp(2 pi i s) e_a); its ends miss the vertex points by 1 (error E4)
        for (Digraph d : new Digraph[] { Digraph.arrowN(1), Digraph.loop(), Digraph.of(2, new int[] { 0, 1 }, new int[] { 0, 1 }) }) {
            Gesture g = NaturalGesture.literal(d);
            assertEquals(d.vertexCount() + 2 * d.arrowCount(), g.dimension());
            assertEquals(1.0, g.endpointGap(), 1e-12);
        }
    }

    public void testNaturalGestureCorrected() {
        Digraph par = Digraph.of(2, new int[] { 0, 1 }, new int[] { 0, 1 });
        Gesture g = NaturalGesture.of(par);
        assertTrue(g.endpointGap() <= 1e-12);
        assertEquals(6, g.dimension());
        // at s = 1/2 both arrows sit at the midpoint of e_0, e_1 with complex parts -2 e_a and -2 e_b
        double[] mid = g.curve(0).at(0.5);
        assertEquals(0.5, mid[0], 1e-15);
        assertEquals(0.5, mid[1], 1e-15);
        assertEquals(-2.0, mid[2], 1e-15);
        assertEquals(0.0, mid[4], 1e-15);
        assertTrue(Curve.supDistance(g.curve(0), g.curve(1)) >= 2 * Math.sqrt(2) - 1e-12);
        // a loop is drawn as a circle of radius 1 through its vertex point
        Gesture l = NaturalGesture.of(Digraph.loop());
        assertTrue(l.endpointGap() <= 1e-12);
        double[] q = l.curve(0).at(0.25);
        assertEquals(1.0, q[0], 1e-15);
        assertEquals(-1.0, q[1], 1e-12);
        assertEquals(1.0, q[2], 1e-12);
    }
}
