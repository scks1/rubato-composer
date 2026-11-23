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

import java.util.Random;

import junit.framework.TestCase;

import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.Gesture;
import org.rubato.math.gesture.Hypergesture;

/**
 * Hypergestures and the Escher exchange (Mazzola, The Topos of Music,
 * vol. III, section 61.6, pp. 915-918, Prop. 61; section 61.14.1,
 * pp. 931-933, Thm. 38).
 */
public class HypergestureTest extends TestCase {

    static final int[] CANTUS = { 62, 65, 64, 62 };
    static final int[] DISCANTUS = { 69, 69, 67, 74 };

    public void testFirstSpeciesShape() {
        Hypergesture h = Hypergesture.firstSpecies(CANTUS, DISCANTUS);
        assertEquals(Digraph.arrowN(3), h.outer());
        assertEquals(Digraph.arrowN(1), h.inner());
        assertEquals(2, h.dimension());
        assertTrue(h.endpointGap() <= 1e-12);
        // at the second note the inner gesture is the interval from the cantus tone to the discantus tone
        Gesture g = h.innerGesture(1);
        assertEquals(1.0, g.vertex(0)[0], 0);
        assertEquals(65.0, g.vertex(0)[1], 0);
        assertEquals(69.0, g.vertex(1)[1], 0);
        assertEquals(67.0, g.curve(0).at(0.5)[1], 1e-12);
    }

    public void testEscherIsInvolutiveAndValid() {
        Hypergesture h = Hypergesture.firstSpecies(CANTUS, DISCANTUS);
        Hypergesture e = h.escher();
        assertEquals(h.inner(), e.outer());
        assertEquals(h.outer(), e.inner());
        assertTrue(e.endpointGap() <= 1e-12);
        assertEquals(h, e.escher());
        assertFalse(h.equals(e));
    }

    public void testEscherExchangesCoordinates() {
        // Prop. 61: the exchanged hypergesture takes the same value at (inner point, outer point)
        Hypergesture h = Hypergesture.firstSpecies(CANTUS, DISCANTUS);
        Hypergesture e = h.escher();
        Random r = new Random(61);
        for (int k = 0; k < 200; k++) {
            int oc = r.nextInt(h.outer().vertexCount() + h.outer().arrowCount());
            int ic = r.nextInt(h.inner().vertexCount() + h.inner().arrowCount());
            double s = r.nextDouble(), t = r.nextDouble();
            double[] a = h.at(oc, s, ic, t);
            double[] b = e.at(ic, t, oc, s);
            assertEquals(a[0], b[0], 1e-12);
            assertEquals(a[1], b[1], 1e-12);
        }
    }

    public void testEscherGivesTheTwoMelodies() {
        // section 61.14.1: the exchanged reading is a curve of melodies from the cantus firmus to the discantus
        Hypergesture e = Hypergesture.firstSpecies(CANTUS, DISCANTUS).escher();
        Gesture cf = e.innerGesture(0);
        Gesture dis = e.innerGesture(1);
        assertEquals(Digraph.arrowN(3), cf.skeleton());
        for (int i = 0; i < CANTUS.length; i++) {
            assertEquals((double) i, cf.vertex(i)[0], 0);
            assertEquals((double) CANTUS[i], cf.vertex(i)[1], 0);
            assertEquals((double) DISCANTUS[i], dis.vertex(i)[1], 0);
        }
        // halfway along the exchanged arrow, the melody of interval midpoints
        Gesture mid = e.innerGestureAt(0, 0.5);
        assertEquals(65.5, mid.vertex(0)[1], 1e-12);
        assertEquals(68.0, mid.vertex(3)[1], 1e-12);
    }

    public void testEscherMetricBounds() {
        // derived (spec section 5): D1 <= |A_outer| D2 and D2 <= |A_inner| D1; not an isometry
        int[] c2 = { 64, 62, 64 };
        int[] d2 = { 67, 67, 69 };
        int[] c1 = { 62, 62, 64 };
        int[] d1 = { 67, 67, 67 };
        Hypergesture a = Hypergesture.firstSpecies(c1, d1);
        Hypergesture b = Hypergesture.firstSpecies(c2, d2);
        // differences: +2 at the first cantus tone and +2 at the last discantus tone
        double dOuter = a.distance(b);
        double dExchanged = a.escher().distance(b.escher());
        assertEquals(4.0, dOuter, 1e-12);
        assertEquals(2.0, dExchanged, 1e-12);
        assertTrue(dOuter <= a.outer().arrowCount() * dExchanged + 1e-12);
        assertTrue(dExchanged <= a.inner().arrowCount() * dOuter + 1e-12);
        Random r = new Random(23);
        for (int k = 0; k < 100; k++) {
            int n = 2 + r.nextInt(4);
            Hypergesture x = Hypergesture.firstSpecies(random(r, n), random(r, n));
            Hypergesture y = Hypergesture.firstSpecies(random(r, n), random(r, n));
            double p = x.distance(y), q = x.escher().distance(y.escher());
            assertTrue(p <= x.outer().arrowCount() * q + 1e-9);
            assertTrue(q <= x.inner().arrowCount() * p + 1e-9);
        }
    }

    private static int[] random(Random r, int n) {
        int[] x = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = 55 + r.nextInt(25);
        }
        return x;
    }

    public void testBodyMustGlue() {
        // a body whose arrow face misses the vertex values is not a hypergesture
        Hypergesture.Body bad = (oc, s, ic, t) -> new double[] { oc == 2 ? 2 * s : oc, ic == 2 ? t : ic };
        Hypergesture h = new Hypergesture(Digraph.arrowN(1), Digraph.arrowN(1), 2, 2, 2, bad);
        assertEquals(1.0, h.endpointGap(), 1e-15);
        Hypergesture.Body good = (oc, s, ic, t) -> new double[] { oc == 2 ? s : oc, ic == 2 ? t : ic };
        assertEquals(0.0, new Hypergesture(Digraph.arrowN(1), Digraph.arrowN(1), 2, 2, 2, good).endpointGap(), 0);
    }

    public void testRejectsLengthMismatch() {
        try {
            Hypergesture.firstSpecies(new int[] { 60, 62 }, new int[] { 64 });
            fail("different lengths");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }
}
