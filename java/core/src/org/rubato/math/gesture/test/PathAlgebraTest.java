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

import junit.framework.TestCase;

import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.DigraphTopos;
import org.rubato.math.gesture.PathAlgebra;

/**
 * Path algebras modulo homogeneous relations: Example 65 (formulas) and
 * the tangent and formoid facts of Examples 66-68 (Mazzola, The Topos of
 * Music, vol. III, sections 61.10-61.11, pp. 925-929).
 */
public class PathAlgebraTest extends TestCase {

    /** Example 65: one vertex, loops X, Y; relations X^2 - YX, Y^2 - XY, XY + YX, X^3. */
    static PathAlgebra example65() {
        PathAlgebra a = new PathAlgebra(Digraph.of(1, new int[] { 0, 0 }, new int[] { 0, 0 }), "XY");
        a.addRelation(1, "XX", -1, "YX");
        a.addRelation(1, "YY", -1, "XY");
        a.addRelation(1, "XY", 1, "YX");
        a.addRelation(1, "XXX");
        return a;
    }

    public void testExample65Dimensions() {
        // p. 926: the radical Rx + Ry + Rxy has dimension 3; graded 1, 2, 1, 0, ...
        PathAlgebra a = example65();
        int[] expected = { 1, 2, 1, 0, 0, 0, 0 };
        for (int d = 0; d < expected.length; d++) {
            assertEquals("degree " + d, expected[d], a.dimension(0, 0, d));
        }
        assertEquals(3, a.radicalDimension(0, 0, 6));
    }

    public void testExample65Relations() {
        PathAlgebra a = example65();
        // x^2 = yx = -y^2, xy = -yx, x^3 = 0
        assertTrue(a.inIdeal(1, "XX", -1, "YX"));
        assertTrue(a.inIdeal(1, "XX", 1, "YY"));
        assertTrue(a.inIdeal(1, "XY", 1, "YX"));
        assertTrue(a.inIdeal(1, "XXX"));
        assertTrue(a.inIdeal(1, "YYY"));
        assertFalse(a.inIdeal(1, "XY"));
        assertFalse(a.inIdeal(1, "XX", -1, "XY"));
    }

    public void testExample65Formula() {
        // the formula xy + 3y^2 is not zero, equals 4xy, and squares to 0
        PathAlgebra a = example65();
        assertFalse(a.inIdeal(1, "XY", 3, "YY"));
        assertTrue(a.inIdeal(1, "XY", 3, "YY", -4, "XY"));
        // (xy + 3yy)^2 = xyxy + 3xyyy + 3yyxy + 9yyyy
        assertTrue(a.inIdeal(1, "XYXY", 3, "XYYY", 3, "YYXY", 9, "YYYY"));
    }

    public void testExample65Tangent() {
        // T_S(x, x) = R + Rad/Rad^2: dimension 1 + 2
        assertEquals(3, 1 + example65().radicalSquareQuotientDimension(0, 0));
    }

    public void testExample66DualNumbers() {
        // one loop modulo its square: R[e], dimensions 1, 1, 0
        PathAlgebra a = new PathAlgebra(Digraph.loop(), "E");
        a.addRelation(1, "EE");
        assertEquals(1, a.dimension(0, 0, 0));
        assertEquals(1, a.dimension(0, 0, 1));
        assertEquals(0, a.dimension(0, 0, 2));
        assertEquals(1, a.radicalSquareQuotientDimension(0, 0));
    }

    public void testExample68OmegaModuloLengthTwo() {
        // pp. 928-929: R Omega modulo paths of length >= 2 gives f@f = R[e], t@t = R[e1, e2]
        PathAlgebra a = new PathAlgebra(DigraphTopos.omega(), "FNPQT");
        int f = DigraphTopos.FALSE, t = DigraphTopos.TRUE;
        assertEquals(2, a.dimension(f, f, 0) + a.dimension(f, f, 1));
        assertEquals(3, a.dimension(t, t, 0) + a.dimension(t, t, 1));
        assertEquals(1, a.dimension(f, t, 0) + a.dimension(f, t, 1));
        assertEquals(1, a.dimension(t, f, 0) + a.dimension(t, f, 1));
        // formoid ranks: Rad/Rad^2 has dimension 1 at f and 2 at t, matching Z and Z^2
        assertEquals(1, a.radicalSquareQuotientDimension(f, f));
        assertEquals(2, a.radicalSquareQuotientDimension(t, t));
        // the free path algebra keeps growing: closed paths of length 2 at t
        assertEquals(5, a.dimension(t, t, 2));
    }

    public void testRejectsNonPaths() {
        PathAlgebra a = new PathAlgebra(DigraphTopos.omega(), "FNPQT");
        try {
            a.addRelation(1, "NQ");
            fail("N ends at f, Q starts at t");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        try {
            example65().addRelation(1, "XY", 1, "X");
            fail("relation not homogeneous");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }
}
