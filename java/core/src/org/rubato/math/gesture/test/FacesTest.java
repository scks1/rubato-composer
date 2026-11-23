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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.Faces;

/**
 * Faces and boundaries of hypergestures (Mazzola, The Topos of Music,
 * vol. III, section 63.3, pp. 968-971: Def. 112, Prop. 64, Example 76).
 */
public class FacesTest extends TestCase {

    static final Digraph FORK = Digraph.of(3, new int[] { 0, 1 }, new int[] { 0, 2 });
    static final Digraph JOIN = Digraph.of(3, new int[] { 1, 0 }, new int[] { 2, 0 });
    static final Digraph PARALLEL = Digraph.of(2, new int[] { 0, 1 }, new int[] { 0, 1 });
    static final Digraph CYCLE2 = Digraph.of(2, new int[] { 0, 1 }, new int[] { 1, 0 });

    private static void assertCoefficients(long[] expected, Digraph d) {
        long[] c = Faces.coefficients(d);
        assertEquals(d.toString(), expected.length, c.length);
        for (int i = 0; i < c.length; i++) {
            assertEquals(d + " vertex " + i, expected[i], c[i]);
        }
    }

    public void testExample76() {
        // p. 971: on the up-arrow the face is g(h) - g(t) (by Def. 112; the example prints g(t) - g(h), error E6)
        assertCoefficients(new long[] { -1, 1 }, Digraph.arrowN(1));
        // on the final digraph 1 the face is 0
        assertCoefficients(new long[] { 0 }, Digraph.loop());
    }

    public void testDiscreteAndEmpty() {
        // Def. 112: a discrete digraph gives the sum of the values
        assertCoefficients(new long[] { 1 }, Digraph.point());
        assertCoefficients(new long[] { 1, 1, 1 }, Digraph.discrete(3));
        assertCoefficients(new long[0], Digraph.empty());
    }

    public void testComputedConsequences() {
        // computed from Def. 112 (prototype G00 and this test), not stated in the book
        assertCoefficients(new long[] { -2, 2 }, PARALLEL);
        assertCoefficients(new long[] { 0, 0 }, CYCLE2);
        assertCoefficients(new long[] { 2, 1, 1 }, FORK);
        assertCoefficients(new long[] { 2, -3, -3 }, JOIN);
        assertCoefficients(new long[] { 0, 0 }, org.rubato.math.gesture.DigraphTopos.omega());
    }

    public void testUpArrowGivesBinomialDifferences() {
        // on up-arrow n the face is the n-th forward difference: (-1)^(n-i) C(n, i)
        for (int n = 1; n <= 6; n++) {
            long[] c = Faces.coefficients(Digraph.arrowN(n));
            long binom = 1;
            for (int i = 0; i <= n; i++) {
                long sign = (n - i) % 2 == 0 ? 1 : -1;
                assertEquals("n = " + n + ", i = " + i, sign * binom, c[i]);
                binom = binom * (n - i) / (i + 1);
            }
        }
    }

    static Digraph[] pool() {
        return new Digraph[] { Digraph.arrowN(1), Digraph.loop(), Digraph.arrowN(2), PARALLEL, CYCLE2, FORK,
                               Digraph.discrete(2) };
    }

    public void testBoundaryOfTopCube() {
        // up-arrow x up-arrow: the four edges with signs
        Digraph[] seq = { Digraph.arrowN(1), Digraph.arrowN(1) };
        Map<List<Integer>, Long> d = Faces.boundary(seq, Faces.topCube(seq));
        assertEquals(4, d.size());
        assertEquals(Long.valueOf(-1), d.get(cube(0, -1)));
        assertEquals(Long.valueOf(1), d.get(cube(1, -1)));
        assertEquals(Long.valueOf(1), d.get(cube(-1, 0)));
        assertEquals(Long.valueOf(-1), d.get(cube(-1, 1)));
    }

    static List<Integer> cube(int... fixed) {
        List<Integer> l = new ArrayList<Integer>();
        for (int f : fixed) {
            l.add(f);
        }
        return l;
    }

    public void testBoundarySquaredIsZero() {
        // Prop. 64 (pp. 969-970) on every sequence of two and three digraphs from the pool
        Digraph[] p = pool();
        int checked = 0;
        for (Digraph a : p) {
            for (Digraph b : p) {
                assertTrue(dd(new Digraph[] { a, b }).isEmpty());
                checked++;
                for (Digraph c : p) {
                    Digraph[] seq = { a, b, c };
                    assertTrue(dd(seq).isEmpty());
                    // and on every face of the top cube separately
                    for (List<Integer> face : Faces.boundary(seq, Faces.topCube(seq)).keySet()) {
                        Map<List<Integer>, Long> one = new java.util.HashMap<List<Integer>, Long>();
                        one.put(face, 1L);
                        assertTrue(Faces.boundary(seq, Faces.boundary(seq, one)).isEmpty());
                    }
                    checked++;
                }
            }
        }
        assertEquals(49 + 343, checked);
    }

    private static Map<List<Integer>, Long> dd(Digraph[] seq) {
        return Faces.boundary(seq, Faces.boundary(seq, Faces.topCube(seq)));
    }

    public void testForkPointIsTorsion() {
        // a constant fork gesture at a point has boundary 4 times that point (coefficients sum to 4)
        Digraph[] seq = { FORK };
        Map<List<Integer>, Long> d = Faces.boundary(seq, Faces.topCube(seq));
        long sum = 0;
        for (Long v : d.values()) {
            sum += v;
        }
        assertEquals(4, sum);
    }
}
