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
import org.rubato.math.gesture.DigraphHomotopy;
import org.rubato.math.gesture.DigraphMorphism;
import org.rubato.math.gesture.DigraphTopos;
import org.rubato.math.gesture.ScaleNetworks;
import org.rubato.math.gesture.Subdigraph;

/**
 * The topos of digraphs: subobject classifier, Heyting algebra of
 * subdigraphs, Example 69 and the homotopy facts of Example 68
 * (Mazzola, The Topos of Music, vol. III, sections 61.11-61.13,
 * pp. 928-931; vol. IV, appendix J.1.3, pp. 1459-1460, and Thm. 72,
 * p. 1433).
 */
public class DigraphToposTest extends TestCase {

    public void testOmegaShape() {
        // Topos IV p. 1460: vertices f, t; loop F at f; P: f -> t; N: t -> f; loops Q, T at t
        Digraph o = DigraphTopos.omega();
        assertEquals(2, o.vertexCount());
        assertEquals(5, o.arrowCount());
        int f = DigraphTopos.FALSE, t = DigraphTopos.TRUE;
        assertArrow(o, DigraphTopos.F, f, f);
        assertArrow(o, DigraphTopos.P, f, t);
        assertArrow(o, DigraphTopos.N, t, f);
        assertArrow(o, DigraphTopos.Q, t, t);
        assertArrow(o, DigraphTopos.T, t, t);
        assertEquals(DigraphTopos.T, DigraphTopos.trueArrow());
    }

    private static void assertArrow(Digraph d, int a, int tail, int head) {
        assertEquals("tail of " + a, tail, d.tail(a));
        assertEquals("head of " + a, head, d.head(a));
    }

    public void testSubdigraphCounts() {
        // independent counts (prototype G00): 1, 2, 5, 3, 13, 7, 7, 31
        int[] expected = { 1, 2, 5, 3, 13, 7, 7, 31 };
        Digraph[] s = DigraphTest.samples();
        for (int i = 0; i < s.length; i++) {
            assertEquals(s[i].toString(), expected[i], Subdigraph.all(s[i]).size());
            assertEquals(s[i].toString(), expected[i], DigraphMorphism.all(s[i], DigraphTopos.omega()).size());
        }
    }

    public void testClassifierBijection() {
        // Def. 199 / G.3.1: S is the pullback of true along chi_S, and chi is unique
        for (Digraph d : DigraphTest.samples()) {
            for (Subdigraph s : Subdigraph.all(d)) {
                DigraphMorphism chi = DigraphTopos.characteristic(s);
                assertEquals(d, chi.source());
                assertEquals(DigraphTopos.omega(), chi.target());
                assertEquals(s, DigraphTopos.pullbackOfTrue(chi));
            }
            for (DigraphMorphism chi : DigraphMorphism.all(d, DigraphTopos.omega())) {
                assertEquals(chi, DigraphTopos.characteristic(DigraphTopos.pullbackOfTrue(chi)));
            }
        }
    }

    public void testCharacteristicOfArrowParts() {
        // the arrow 0 -> 1 with only its tail: arrow goes to N (t -> f); only its head: P; both ends, no arrow: Q
        Digraph up = Digraph.arrowN(1);
        assertEquals(DigraphTopos.N, DigraphTopos.characteristic(sub(up, "10", "0")).arrow(0));
        assertEquals(DigraphTopos.P, DigraphTopos.characteristic(sub(up, "01", "0")).arrow(0));
        assertEquals(DigraphTopos.Q, DigraphTopos.characteristic(sub(up, "11", "0")).arrow(0));
        assertEquals(DigraphTopos.T, DigraphTopos.characteristic(sub(up, "11", "1")).arrow(0));
        assertEquals(DigraphTopos.F, DigraphTopos.characteristic(sub(up, "00", "0")).arrow(0));
    }

    static Subdigraph sub(Digraph d, String v, String a) {
        boolean[] vs = new boolean[v.length()];
        for (int i = 0; i < vs.length; i++) {
            vs[i] = v.charAt(i) == '1';
        }
        boolean[] as = new boolean[a.length()];
        for (int i = 0; i < as.length; i++) {
            as[i] = a.charAt(i) == '1';
        }
        return new Subdigraph(d, vs, as);
    }

    public void testSubdigraphRejectsDanglingArrow() {
        try {
            sub(Digraph.arrowN(1), "10", "1");
            fail("arrow without its head");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testYoneda() {
        // Hom(y(A), X) = X(A): morphisms up-arrow -> X are arrows; point -> X are vertices (G.2, p. 1423)
        for (Digraph d : DigraphTest.samples()) {
            List<DigraphMorphism> h = DigraphMorphism.all(Digraph.arrowN(1), d);
            boolean[] seen = new boolean[d.arrowCount()];
            for (DigraphMorphism m : h) {
                assertFalse(seen[m.arrow(0)]);
                seen[m.arrow(0)] = true;
            }
            assertEquals(d.arrowCount(), h.size());
        }
    }

    public void testHeytingAdjunction() {
        // Thm. 72: S meet T <= U iff S <= (T => U), on every triple
        for (Digraph d : new Digraph[] { Digraph.arrowN(1), Digraph.loop(), Digraph.arrowN(2),
                                         Digraph.of(2, new int[] { 0, 1 }, new int[] { 1, 0 }) }) {
            List<Subdigraph> all = Subdigraph.all(d);
            for (Subdigraph s : all) {
                for (Subdigraph t : all) {
                    Subdigraph imp = t.implies(s);
                    for (Subdigraph u : all) {
                        assertEquals(u.meet(t).leq(s), u.leq(imp));
                    }
                    assertTrue(s.leq(s.join(t)));
                    assertTrue(s.meet(t).leq(t));
                }
                assertEquals(s.implies(Subdigraph.bottom(d)), s.negation());
            }
        }
    }

    public void testBookImplicationDescription() {
        // Topos III p. 930 describes the arrows of the implication in four parts; it equals the presheaf formula
        for (Digraph d : DigraphTest.samples()) {
            List<Subdigraph> all = Subdigraph.all(d);
            for (Subdigraph g : all) {
                for (Subdigraph s : all) {
                    assertEquals(g.implies(s), g.bookImplies(s));
                }
            }
        }
    }

    public void testNotBoolean() {
        // the negation of the tail-only part of an arrow is the head; double negation gives the whole arrow
        Digraph up = Digraph.arrowN(1);
        Subdigraph tail = sub(up, "10", "0");
        assertEquals(sub(up, "01", "0"), tail.negation());
        assertEquals(sub(up, "10", "0"), tail.negation().negation());
        Subdigraph both = sub(up, "11", "0");
        assertEquals(Subdigraph.bottom(up), both.negation());
        assertEquals(Subdigraph.top(up), both.negation().negation());
    }

    public void testExample69Networks() {
        ScaleNetworks c = ScaleNetworks.cMajor(false);
        assertEquals(7, c.digraph().vertexCount());
        assertEquals(17, c.digraph().arrowCount());
        assertEquals(16, ScaleNetworks.cMajor(true).digraph().arrowCount());
        assertDegree(c, "I", new int[] { 0, 4, 7 }, new int[][] { { 0, 7 }, { 7, 4 }, { 4, 7 } });
        assertDegree(c, "VII", new int[] { 2, 5, 11 }, new int[][] { { 2, 5 }, { 5, 11 }, { 11, 11 } });
        assertDegree(c, "IV", new int[] { 0, 5, 9 }, new int[][] { { 5, 0 }, { 0, 9 }, { 9, 0 } });
    }

    private static void assertDegree(ScaleNetworks c, String name, int[] pcs, int[][] arrows) {
        Subdigraph s = c.degree(name);
        assertEquals(name, pcs.length, s.vertexCount());
        for (int p : pcs) {
            assertTrue(name + " " + p, s.containsVertex(c.vertexOf(p)));
        }
        assertEquals(name, arrows.length, s.arrowCount());
        for (int[] a : arrows) {
            assertTrue(name + " " + a[0] + "->" + a[1], c.containsArrow(s, a[0], a[1]));
        }
    }

    public void testExample69FirstImplicationHolds() {
        // p. 930: gamma_V => gamma_I = gamma_{I u IV u VI}
        for (boolean merge : new boolean[] { false, true }) {
            ScaleNetworks c = ScaleNetworks.cMajor(merge);
            Subdigraph lhs = c.degree("V").implies(c.degree("I"));
            Subdigraph rhs = c.degree("I").join(c.degree("IV")).join(c.degree("VI"));
            assertEquals(rhs, lhs);
        }
    }

    public void testExample69SecondImplicationHasExtraLoop() {
        // p. 930 states gamma_IV => gamma_V = gamma_{III u V}; the implication also contains VII's loop at 11 (E5)
        for (boolean merge : new boolean[] { false, true }) {
            ScaleNetworks c = ScaleNetworks.cMajor(merge);
            Subdigraph lhs = c.degree("IV").implies(c.degree("V"));
            Subdigraph rhs = c.degree("III").join(c.degree("V"));
            assertFalse(rhs.equals(lhs));
            assertTrue(rhs.leq(lhs));
            assertEquals(rhs.vertexCount(), lhs.vertexCount());
            assertEquals(rhs.arrowCount() + 1, lhs.arrowCount());
            assertTrue(c.containsArrow(lhs, 11, 11));
            assertFalse(c.containsArrow(rhs, 11, 11));
        }
    }

    public void testExample68FundamentalGroup() {
        // pp. 928-929: pi_1(|Omega|) is free on T, Q, R = NP, S = NFP; rank 5 - 2 + 1 = 4
        Digraph o = DigraphTopos.omega();
        assertEquals(1, DigraphHomotopy.components(o));
        assertEquals(4, DigraphHomotopy.fundamentalGroupRank(o));
        int[][] gens = {
            DigraphHomotopy.closedPath(o, DigraphTopos.T),
            DigraphHomotopy.closedPath(o, DigraphTopos.Q),
            DigraphHomotopy.closedPath(o, DigraphTopos.N, DigraphTopos.P),
            DigraphHomotopy.closedPath(o, DigraphTopos.N, DigraphTopos.F, DigraphTopos.P),
        };
        assertTrue(DigraphHomotopy.isCycleBasis(o, gens));
        // the loop F alone also closes, but T, Q, NP, F, NFP are dependent
        int[][] five = { gens[0], gens[1], gens[2], DigraphHomotopy.closedPath(o, DigraphTopos.F), gens[3] };
        assertFalse(DigraphHomotopy.isCycleBasis(o, five));
        int[][] notBasis = { gens[0], gens[1], gens[2], DigraphHomotopy.closedPath(o, DigraphTopos.T, DigraphTopos.T) };
        assertFalse(DigraphHomotopy.isCycleBasis(o, notBasis));
        // T, Q, NP and twice NFP span a sublattice of index 2: independent, but not a basis over Z
        int[][] index2 = { gens[0], gens[1], gens[2],
                           DigraphHomotopy.closedPath(o, DigraphTopos.N, DigraphTopos.F, DigraphTopos.P,
                                                      DigraphTopos.N, DigraphTopos.F, DigraphTopos.P) };
        assertFalse(DigraphHomotopy.isCycleBasis(o, index2));
    }

    public void testClosedPathRejectsGap() {
        try {
            DigraphHomotopy.closedPath(DigraphTopos.omega(), DigraphTopos.N, DigraphTopos.Q);
            fail("N ends at f, Q starts at t");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        try {
            DigraphHomotopy.closedPath(DigraphTopos.omega(), DigraphTopos.P);
            fail("P is not closed");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testRanksOfSamples() {
        // rank = |A| - |V| + components (Topos IV Lemma 113, p. 1459; [590, Thm. 10.7] cited on p. 928)
        int[] expected = { 0, 0, 0, 1, 0, 1, 1, 2 };
        Digraph[] s = DigraphTest.samples();
        for (int i = 0; i < s.length; i++) {
            assertEquals(s[i].toString(), expected[i], DigraphHomotopy.fundamentalGroupRank(s[i]));
        }
        assertEquals(2, DigraphHomotopy.components(Digraph.discrete(2)));
        assertEquals(0, DigraphHomotopy.components(Digraph.empty()));
    }
}
