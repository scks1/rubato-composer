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

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import junit.framework.TestCase;

import org.rubato.math.performance.Weight;
import org.rubato.math.performance.WeightWatcher;

/**
 * Weights and the Weight Watcher (Mazzola, <i>The Topos of Music II</i>, &sect;32.3.2.1
 * pp. 553-554, &sect;39.1.2 pp. 639-640, &sect;41.4 p. 681 and Fig. 41.14, Fig. 42.9 p. 696).
 */
public class WeightWatcherTest extends TestCase {

    private static final double[] XS = { 1, 2, 4 };
    private static final double[] FS = { 0.5, 1.5, 0.8 };

    private static double slope(DoubleUnaryOperator f, double x, double h) {
        return (f.applyAsDouble(x + h) - f.applyAsDouble(x - h)) / (2 * h);
    }

    private static DoubleUnaryOperator ext() {
        return t -> WeightWatcher.extend(XS, FS, 0, 5, t);
    }

    /** W1: the given values at the points, 1 at and beyond the frame, zero slope at every point. */
    public void testExtensionTakesTheValuesAndIsOneOutsideTheFrame() {
        DoubleUnaryOperator f = ext();
        for (int i = 0; i < XS.length; i++) {
            assertEquals("value at " + XS[i], FS[i], f.applyAsDouble(XS[i]), 1e-15);
            assertEquals("slope at " + XS[i], 0, slope(f, XS[i], 1e-6), 1e-5);
        }
        for (double t : new double[] { -100, -1, 0, 5, 6, 1e6 }) {
            assertEquals("1 at " + t, 1, f.applyAsDouble(t), 0);
        }
        assertEquals("slope at u", 0, slope(f, 0, 1e-6), 1e-5);
        assertEquals("slope at v", 0, slope(f, 5, 1e-6), 1e-5);
        assertTrue("cubic, not constant, between the frame and the first point", f.applyAsDouble(0.5) < 1);
    }

    /** W2: between two neighbours the extension stays between their values. */
    public void testExtensionAddsNoExtrema() {
        DoubleUnaryOperator f = ext();
        double[] xs = { 0, 1, 2, 4, 5 };
        double[] fs = { 1, 0.5, 1.5, 0.8, 1 };
        for (int i = 0; i + 1 < xs.length; i++) {
            double lo = Math.min(fs[i], fs[i + 1]), hi = Math.max(fs[i], fs[i + 1]);
            double prev = f.applyAsDouble(xs[i]);
            for (int k = 1; k <= 200; k++) {
                double t = xs[i] + (xs[i + 1] - xs[i]) * k / 200.0;
                double y = f.applyAsDouble(t);
                assertTrue("between neighbours at " + t + ": " + y, y >= lo - 1e-15 && y <= hi + 1e-15);
                assertTrue("monotone on each piece at " + t, fs[i + 1] >= fs[i] ? y >= prev - 1e-15 : y <= prev + 1e-15);
                prev = y;
            }
        }
    }

    /** W3: the slope is continuous, also across the glue points. */
    public void testExtensionIsC1() {
        DoubleUnaryOperator f = ext();
        double h = 1e-5;
        for (double t = -0.5; t <= 5.5; t += 0.001) {
            double s0 = slope(f, t, h), s1 = slope(f, t + 0.001, h);
            assertTrue("slope jump near " + t + ": " + s0 + " vs " + s1, Math.abs(s1 - s0) < 0.02);
        }
    }

    /** W4: delta(0, 1, 0) is the identity; delta(0, 1, tau) fixes 0 and 1 and increases. */
    public void testDeformationFixesTheEndsAndIncreases() {
        for (double t = 0; t <= 1; t += 0.01) {
            assertEquals(t, WeightWatcher.deform(0, 1, 0, t), 1e-15);
        }
        for (double tau : new double[] { -1, -0.75, -0.3, 0.3, 1, 2 }) {
            assertEquals(0, WeightWatcher.deform(0, 1, tau, 0), 1e-15);
            assertEquals(1, WeightWatcher.deform(0, 1, tau, 1), 1e-15);
            double prev = -1;
            for (int k = 0; k <= 100; k++) {
                double y = WeightWatcher.deform(0, 1, tau, k / 100.0);
                assertTrue("increasing for tau " + tau, y > prev);
                prev = y;
            }
        }
        // p. 640: t / (e^{2 tau} - t (e^{2 tau} - 1)) for tau >= 0
        double c = Math.exp(2 * 0.3);
        assertEquals(0.4 / (c - 0.4 * (c - 1)), WeightWatcher.deform(0, 1, 0.3, 0.4), 1e-15);
        assertTrue("positive tau pulls values down", WeightWatcher.deform(0, 1, 0.3, 0.4) < 0.4);
    }

    /** W5: delta(0, 1, tau) and delta(0, 1, -tau) are reflections at the diagonal. */
    public void testNegativeDeformationIsTheDiagonalReflection() {
        for (double tau : new double[] { 0.1, 0.75, 1, 2.5 }) {
            for (int k = 0; k <= 50; k++) {
                double t = k / 50.0;
                assertEquals(t, WeightWatcher.deform(0, 1, tau, WeightWatcher.deform(0, 1, -tau, t)), 1e-12);
                assertEquals(t, WeightWatcher.deform(0, 1, -tau, WeightWatcher.deform(0, 1, tau, t)), 1e-12);
            }
        }
    }

    /** W6: delta(amin, amax, tau) is the identity outside [amin, amax] and fixes its ends. */
    public void testDeformationIsTheIdentityOutsideItsInterval() {
        for (double t : new double[] { -3, 0.1999, 1.6001, 9 }) {
            assertEquals(t, WeightWatcher.deform(0.2, 1.6, -0.75, t), 0);
        }
        assertEquals(0.2, WeightWatcher.deform(0.2, 1.6, -0.75, 0.2), 1e-15);
        assertEquals(1.6, WeightWatcher.deform(0.2, 1.6, -0.75, 1.6), 1e-15);
        // the affine image of delta(0, 1, tau)
        double t = 0.9, s = (t - 0.2) / 1.4;
        assertEquals(0.2 + 1.4 * WeightWatcher.deform(0, 1, -0.75, s), WeightWatcher.deform(0.2, 1.6, -0.75, t), 1e-15);
    }

    private static Weight fig4114() {
        // nine values with Min 973, Max 22138 and Mean 76442/9 = 8493.555..., as Fig. 41.14 shows
        double[] v = { 973, 5000, 6000, 22138, 7000, 8000, 9000, 9331, 9000 };
        double[] e = new double[v.length];
        for (int i = 0; i < e.length; i++) {
            e[i] = i;
        }
        return new Weight("Metro", e, v);
    }

    /** W7: the affine normalization reproduces the Limits box of Fig. 41.14. */
    public void testNormalizationReproducesFigure41_14() {
        Weight w = fig4114();
        assertEquals(973, w.min(), 0);
        assertEquals(22138, w.max(), 0);
        assertEquals(76442.0 / 9, w.mean(), 1e-9);
        WeightWatcher.Entry e = new WeightWatcher.Entry(w, 1.2, 0.8, 0.3, false, 1, 1);
        assertEquals("Norm. Mean of Fig. 41.14", 0.9421319263, e.normMean(), 1e-10);
        double[] n = e.normalized();
        assertEquals(0.8, n[0], 1e-15);
        assertEquals(1.2, n[3], 1e-15);
    }

    /** D3: inversion maps the minimum to High Norm and the maximum to Low Norm. */
    public void testInversionSwapsTheEnds() {
        WeightWatcher.Entry e = new WeightWatcher.Entry(fig4114(), 1.2, 0.8, 0, true, 1, 1);
        double[] n = e.normalized();
        assertEquals(1.2, n[0], 1e-15);
        assertEquals(0.8, n[3], 1e-15);
        assertEquals(2.0 - 0.9421319263, e.normMean(), 1e-10);
    }

    /** D3: a weight with one distinct value maps to High Norm, or Low Norm when inverted. */
    public void testAConstantWeightMapsToAnEnd() {
        Weight w = new Weight("flat", new double[] { 0, 1 }, new double[] { 3, 3 });
        assertEquals(1.4, new WeightWatcher.Entry(w, 1.4, 0.7, 0, false, 1, 1).normalized()[1], 0);
        assertEquals(0.7, new WeightWatcher.Entry(w, 1.4, 0.7, 0, true, 1, 1).normalized()[1], 0);
    }

    /** D2, D4, D5, D6: normalize, extend with the tolerance frame, deform on the image, scale. */
    public void testAnEntryNormalizesExtendsDeformsAndScales() {
        Weight w = new Weight("w", new double[] { 2, 3, 5 }, new double[] { 10, 30, 20 });
        WeightWatcher.Entry e = new WeightWatcher.Entry(w, 1.6, 0.4, -0.75, false, 0.5, 2);
        assertEquals(0, e.frameStart(), 0);
        assertEquals(7, e.frameEnd(), 0);
        assertEquals(0.4, e.imageMin(), 0);
        assertEquals(1.6, e.imageMax(), 0);
        double[] n = e.normalized();
        assertEquals(0.4, n[0], 1e-15);
        assertEquals(1.6, n[1], 1e-15);
        assertEquals(1.0, n[2], 1e-15);
        DoubleUnaryOperator f = e.function();
        for (int i = 0; i < 3; i++) {
            double expect = 0.5 * WeightWatcher.deform(0.4, 1.6, -0.75, n[i]);
            assertEquals(expect, f.applyAsDouble(w.getOnsets()[i]), 1e-12);
        }
        double t = 4.2;
        double fe = WeightWatcher.extend(w.getOnsets(), n, 0, 7, t);
        assertEquals(fe, e.extended(t), 0);
        assertEquals(0.5 * WeightWatcher.deform(0.4, 1.6, -0.75, fe), f.applyAsDouble(t), 1e-12);
        // outside the frame the extension is 1, and 1 lies inside [0.4, 1.6], so the deformation moves it
        assertEquals(0.5 * WeightWatcher.deform(0.4, 1.6, -0.75, 1), f.applyAsDouble(-10), 1e-12);
        assertEquals(0.5 * WeightWatcher.deform(0.4, 1.6, -0.75, 1), f.applyAsDouble(100), 1e-12);
    }

    /** The image interval includes 1 when both norms lie on one side of it. */
    public void testTheImageIncludesOne() {
        Weight w = new Weight("w", new double[] { 0, 1 }, new double[] { 1, 2 });
        WeightWatcher.Entry e = new WeightWatcher.Entry(w, 1.6, 1.2, 0.5, false, 1, 1);
        assertEquals(1, e.imageMin(), 0);
        assertEquals(1.6, e.imageMax(), 0);
        assertEquals("outside the frame the value 1 is an end of the image and stays", 1, e.function().applyAsDouble(-5), 1e-15);
    }

    /** W8: sum (39.5) and product (39.7) are pointwise. */
    public void testSumAndProductArePointwise() {
        Weight a = new Weight("a", new double[] { 0, 2, 4 }, new double[] { 1, 3, 2 });
        Weight b = new Weight("b", new double[] { 1, 3 }, new double[] { 5, 4 });
        WeightWatcher.Entry ea = new WeightWatcher.Entry(a, 1.3, 0.7, 0.3, false, 0.6, 1);
        WeightWatcher.Entry eb = new WeightWatcher.Entry(b, 1.1, 0.9, -0.5, true, 0.4, 0.5);
        List<WeightWatcher.Entry> both = new ArrayList<WeightWatcher.Entry>();
        both.add(ea);
        both.add(eb);
        DoubleUnaryOperator sum = new WeightWatcher(both, false).combined();
        DoubleUnaryOperator prod = new WeightWatcher(both, true).combined();
        for (double t = -2; t <= 6; t += 0.05) {
            double x = ea.function().applyAsDouble(t), y = eb.function().applyAsDouble(t);
            assertEquals(x + y, sum.applyAsDouble(t), 1e-15);
            assertEquals(x * y, prod.applyAsDouble(t), 1e-15);
        }
        WeightWatcher w = new WeightWatcher(both, false);
        assertEquals(-1, w.frameStart(), 0);
        assertEquals(5, w.frameEnd(), 0);
    }

    /** W9: the four rows of Fig. 42.9 give weights within [Low, High] at the points. */
    public void testFigure42_9ParameterSets() {
        double[][] rows = { { 1.6, 0.2, 1, -0.75 }, { 1.5, 0.3, 1, -0.75 }, { 1.5, 0.4, 1, -0.5 }, { 1.4, 0.6, 1, -1 } };
        Weight w = new Weight("motif", new double[] { 0, 1, 2, 3, 4 }, new double[] { 2, 7, 1, 4, 9 });
        for (double[] r : rows) {
            WeightWatcher.Entry e = new WeightWatcher.Entry(w, r[0], r[1], r[3], true, r[2], 1);
            for (double t : w.getOnsets()) {
                double y = e.function().applyAsDouble(t);
                assertTrue("in [Low, High]: " + y, y >= r[1] - 1e-12 && y <= r[0] + 1e-12);
            }
            assertEquals("inverted: the maximum gets Low Norm", r[1], e.function().applyAsDouble(4), 1e-12);
            assertEquals("inverted: the minimum gets High Norm", r[0], e.function().applyAsDouble(2), 1e-12);
        }
    }

    public void testParserReadsEntriesDefaultsAndComments() {
        String text = "# weights\n"
                    + "\n"
                    + "Metro high=1.2 low=0.8 deformation=0.3 influence=0.5 tolerance=2 points=0:973,1:22138,2:5000\n"
                    + "Motif high=1.6 low=0.2 inverted=yes points=0.5:2, 1.5:7\n";
        WeightWatcher w = WeightWatcher.parse(text, true);
        assertTrue(w.isProduct());
        assertEquals(2, w.getEntries().size());
        WeightWatcher.Entry a = w.getEntries().get(0), b = w.getEntries().get(1);
        assertEquals("Metro", a.getWeight().getName());
        assertEquals(1.2, a.getHigh(), 0);
        assertEquals(0.8, a.getLow(), 0);
        assertEquals(0.3, a.getDeformation(), 0);
        assertFalse(a.isInverted());
        assertEquals(0.5, a.getInfluence(), 0);
        assertEquals(2, a.getTolerance(), 0);
        assertEquals(3, a.getWeight().size());
        assertEquals("Motif", b.getWeight().getName());
        assertTrue(b.isInverted());
        assertEquals("default deformation", 0, b.getDeformation(), 0);
        assertEquals("default influence", 1, b.getInfluence(), 0);
        assertEquals("default tolerance, one quarter (D4)", 1, b.getTolerance(), 0);
        assertEquals(0.5, b.getWeight().getOnsets()[0], 0);
        assertEquals(7, b.getWeight().getValues()[1], 0);
        assertTrue(WeightWatcher.parse("", false).isEmpty());
        assertTrue(WeightWatcher.parse("  \n# none\n", false).isEmpty());
    }

    private static void refused(String text, String expect) {
        try {
            WeightWatcher.parse(text, false);
            fail("accepted: " + text);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expect));
        }
    }

    public void testParserRefusals() {
        refused("\nA low=0.8 points=0:1", "line 2");
        refused("A low=0.8 points=0:1", "high");
        refused("A high=1.2 points=0:1", "low");
        refused("A high=1.2 low=0.8", "points");
        refused("A high=1.2 low=0.8 points=", "points");
        refused("A high=x low=0.8 points=0:1", "high");
        refused("A high=0.7 low=0.8 points=0:1", "Low Norm");
        refused("A high=1 low=0 points=0:1", "positive");
        refused("A high=1.2 low=0.8 influence=0 points=0:1", "influence");
        refused("A high=1.2 low=0.8 tolerance=0 points=0:1", "tolerance");
        refused("A high=1.2 low=0.8 inverted=maybe points=0:1", "inverted");
        refused("A high=1.2 low=0.8 colour=red points=0:1", "colour");
        refused("A high=1.2 low=0.8 points=1:1,1:2", "increasing");
        refused("A high=1.2 low=0.8 points=0:1,1:NaN", "finite");
        refused("A high=1.2 low=0.8 points=0-1", "E:w");
        refused("high=1.2 low=0.8 points=0:1", "name");
    }

    public void testEntryRefusals() {
        Weight w = new Weight("w", new double[] { 0 }, new double[] { 1 });
        try {
            new WeightWatcher.Entry(w, 1, 0.5, 0, false, 1, -1);
            fail();
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("tolerance"));
        }
        try {
            new Weight("w", new double[] { 0, 1 }, new double[] { 1 });
            fail();
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("same number"));
        }
        try {
            new Weight("w", new double[0], new double[0]);
            fail();
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("point"));
        }
        try {
            new WeightWatcher(new ArrayList<WeightWatcher.Entry>(), false).combined();
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no weights"));
        }
    }
}
