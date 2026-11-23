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

import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.DigraphMorphism;

/**
 * Digraphs and their morphisms (Mazzola, The Topos of Music, vol. III,
 * section 61.5, p. 914; vol. IV, appendix J.1.1, p. 1457).
 */
public class DigraphTest extends TestCase {

    /** Small digraphs used throughout. */
    static Digraph[] samples() {
        return new Digraph[] {
            Digraph.empty(),
            Digraph.point(),
            Digraph.arrowN(1),
            Digraph.loop(),
            Digraph.arrowN(2),
            Digraph.of(2, new int[] { 0, 1 }, new int[] { 0, 1 }),
            Digraph.of(2, new int[] { 0, 1 }, new int[] { 1, 0 }),
            Digraph.of(3, new int[] { 0, 1 }, new int[] { 1, 2 }, new int[] { 2, 0 }, new int[] { 1, 1 }),
        };
    }

    public void testTailIsFirstProjection() {
        // Topos IV p. 1457 defines tail = pr1, head = pr2 (the text then swaps the names; error E1)
        Digraph d = Digraph.of(3, new int[] { 2, 0 });
        assertEquals(2, d.tail(0));
        assertEquals(0, d.head(0));
    }

    public void testArrowN() {
        // Example 135: up-arrow n has n+1 vertices and arrows i -> i+1
        Digraph d = Digraph.arrowN(3);
        assertEquals(4, d.vertexCount());
        assertEquals(3, d.arrowCount());
        for (int i = 0; i < 3; i++) {
            assertEquals(i, d.tail(i));
            assertEquals(i + 1, d.head(i));
        }
        assertEquals(1, Digraph.point().vertexCount());
        assertEquals(0, Digraph.point().arrowCount());
        assertEquals(1, Digraph.loop().arrowCount());
        assertEquals(0, Digraph.loop().tail(0));
        assertEquals(0, Digraph.loop().head(0));
    }

    public void testDualIsInvolution() {
        // Example 136: the dual digraph swaps tail and head
        for (Digraph d : samples()) {
            Digraph e = d.dual();
            for (int a = 0; a < d.arrowCount(); a++) {
                assertEquals(d.tail(a), e.head(a));
                assertEquals(d.head(a), e.tail(a));
            }
            assertEquals(d, e.dual());
        }
    }

    public void testIsolatedVertices() {
        assertTrue(Digraph.point().isIsolated(0));
        assertFalse(Digraph.loop().isIsolated(0));
        Digraph d = Digraph.of(3, new int[] { 0, 1 });
        assertFalse(d.isIsolated(0));
        assertFalse(d.isIsolated(1));
        assertTrue(d.isIsolated(2));
    }

    public void testRejectsBadArrow() {
        try {
            Digraph.of(2, new int[] { 0, 2 });
            fail("vertex 2 does not exist");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testMorphismLaw() {
        Digraph up = Digraph.arrowN(1);
        Digraph two = Digraph.arrowN(2);
        // 0->1 onto the second arrow 1->2
        DigraphMorphism m = new DigraphMorphism(up, two, new int[] { 1, 2 }, new int[] { 1 });
        assertEquals(two, m.target());
        assertFalse(DigraphMorphism.isMorphism(up, two, new int[] { 0, 2 }, new int[] { 0 }));
        assertFalse(DigraphMorphism.isMorphism(up, two, new int[] { 1, 0 }, new int[] { 0 }));
        try {
            new DigraphMorphism(up, two, new int[] { 2, 1 }, new int[] { 1 });
            fail("reversed arrow is not a morphism");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testHomCounts() {
        // Hom(up, D) = arrows of D; Hom(point, D) = vertices of D; Hom(D, loop) = 1
        for (Digraph d : samples()) {
            assertEquals(d.arrowCount(), DigraphMorphism.all(Digraph.arrowN(1), d).size());
            assertEquals(d.vertexCount(), DigraphMorphism.all(Digraph.point(), d).size());
            assertEquals(1, DigraphMorphism.all(d, Digraph.loop()).size());
            assertEquals(1, DigraphMorphism.all(Digraph.empty(), d).size());
        }
        // morphisms from up-arrow 2 into the 2-cycle: paths of length 2
        Digraph cyc = Digraph.of(2, new int[] { 0, 1 }, new int[] { 1, 0 });
        assertEquals(2, DigraphMorphism.all(Digraph.arrowN(2), cyc).size());
        // into a vertex with two loops: 4
        Digraph twoLoops = Digraph.of(1, new int[] { 0, 0 }, new int[] { 0, 0 });
        assertEquals(4, DigraphMorphism.all(Digraph.arrowN(2), twoLoops).size());
    }

    public void testCategoryLaws() {
        Digraph[] s = samples();
        for (Digraph a : s) {
            for (Digraph b : s) {
                List<DigraphMorphism> ab = DigraphMorphism.all(a, b);
                for (DigraphMorphism f : ab) {
                    assertEquals(f, f.then(DigraphMorphism.identity(b)));
                    assertEquals(f, DigraphMorphism.identity(a).then(f));
                }
            }
        }
        Digraph x = Digraph.arrowN(1);
        Digraph y = Digraph.of(2, new int[] { 0, 1 }, new int[] { 1, 0 });
        Digraph z = Digraph.of(3, new int[] { 0, 1 }, new int[] { 1, 2 }, new int[] { 2, 0 }, new int[] { 1, 1 });
        Digraph w = Digraph.loop();
        int checked = 0;
        for (DigraphMorphism f : DigraphMorphism.all(x, y)) {
            for (DigraphMorphism g : DigraphMorphism.all(y, z)) {
                for (DigraphMorphism h : DigraphMorphism.all(z, w)) {
                    assertEquals(f.then(g).then(h), f.then(g.then(h)));
                    checked++;
                }
            }
        }
        assertTrue(checked > 0);
    }

    public void testComposeMapsArrows() {
        Digraph up = Digraph.arrowN(1);
        Digraph cyc = Digraph.of(2, new int[] { 0, 1 }, new int[] { 1, 0 });
        DigraphMorphism f = new DigraphMorphism(up, cyc, new int[] { 1, 0 }, new int[] { 1 });
        DigraphMorphism g = new DigraphMorphism(cyc, cyc, new int[] { 1, 0 }, new int[] { 1, 0 });
        DigraphMorphism fg = f.then(g);
        assertEquals(0, fg.vertex(0));
        assertEquals(1, fg.vertex(1));
        assertEquals(0, fg.arrow(0));
    }
}
