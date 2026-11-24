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
package org.rubato.math.topos.test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.DigraphMorphism;
import org.rubato.math.gesture.DigraphTopos;
import org.rubato.math.gesture.Subdigraph;
import org.rubato.math.topos.FiniteCategory;
import org.rubato.math.topos.FiniteModAt;
import org.rubato.math.topos.FinitePresheaf;
import org.rubato.math.topos.Subpresheaf;

/**
 * The presheaf topos over a finite address category, checked against the
 * book's statements on Mod@ (sources/FINITE-MODAT-SPEC.md section 2:
 * Topos I pp. 54, 138, 335-336; Topos IV pp. 1428-1434).
 */
public class FiniteModAtTest extends TestCase {

    private static final FiniteCategory AFF2 = FiniteCategory.affineCyclic(2);

    private static BitSet arrows(FiniteCategory c, int x, int[]... tables) {
        BitSet s = new BitSet();
        for (int[] t : tables) {
            s.set(c.find(x, x, t));
        }
        return s;
    }

    private static final int[] ZERO = { 0, 0 };
    private static final int[] ONE = { 1, 1 };
    private static final int[] ID = { 0, 1 };
    private static final int[] SHIFT = { 1, 0 };

    private static int sieveIndex(FiniteModAt t, int x, BitSet s) {
        List<BitSet> all = t.sievesOn(x);
        int i = all.indexOf(s);
        assertTrue("not a sieve: " + s, i >= 0);
        return i;
    }

    public void testSievesOnZ2() {
        FiniteModAt t = new FiniteModAt(AFF2);
        List<BitSet> sieves = t.sievesOn(0);
        assertEquals(5, sieves.size());
        Set<BitSet> expected = new HashSet<BitSet>();
        expected.add(new BitSet());
        expected.add(arrows(AFF2, 0, ZERO));
        expected.add(arrows(AFF2, 0, ONE));
        expected.add(arrows(AFF2, 0, ZERO, ONE));
        expected.add(arrows(AFF2, 0, ZERO, ONE, ID, SHIFT));
        assertEquals(expected, new HashSet<BitSet>(sieves));
        assertEquals(arrows(AFF2, 0, ZERO, ONE, ID, SHIFT), sieves.get(t.trueAt(0)));
        assertEquals(5, t.omega().size(0));
    }

    public void testOmegaRestrictsByPullback() {
        // f*(S) = { g : f∘g in S } (Topos IV p. 1430)
        FiniteModAt t = new FiniteModAt(AFF2);
        FinitePresheaf omega = t.omega();
        int shift = AFF2.find(0, 0, SHIFT);
        int zero = AFF2.find(0, 0, ZERO);
        int s0 = sieveIndex(t, 0, arrows(AFF2, 0, ZERO));
        int s1 = sieveIndex(t, 0, arrows(AFF2, 0, ONE));
        int top = t.trueAt(0);
        int bottom = sieveIndex(t, 0, new BitSet());
        // the translation swaps the two constants
        assertEquals(s1, omega.restrict(shift, s0));
        assertEquals(s0, omega.restrict(shift, s1));
        // along the constant 0, {0} pulls back to everything, {1} to nothing
        assertEquals(top, omega.restrict(zero, s0));
        assertEquals(bottom, omega.restrict(zero, s1));
    }

    public void testNegationOnZ2() {
        FiniteModAt t = new FiniteModAt(AFF2);
        int[][] neg = t.negation();
        int s0 = sieveIndex(t, 0, arrows(AFF2, 0, ZERO));
        int s1 = sieveIndex(t, 0, arrows(AFF2, 0, ONE));
        int both = sieveIndex(t, 0, arrows(AFF2, 0, ZERO, ONE));
        int bottom = sieveIndex(t, 0, new BitSet());
        int top = t.trueAt(0);
        assertEquals(s1, neg[0][s0]);
        assertEquals(s0, neg[0][s1]);
        assertEquals(bottom, neg[0][both]);
        assertEquals(top, neg[0][bottom]);
        assertEquals(bottom, neg[0][top]);
    }

    public void testZ2IsNotBoolean() {
        FiniteModAt t = new FiniteModAt(AFF2);
        FinitePresheaf y = FinitePresheaf.representable(AFF2, 0);
        assertFalse(t.isBoolean(y));
        assertFalse(t.isBooleanTopos());
        // the witness: {0} v not{0} = {0, 1}, not the top (Prop. 124)
        Subpresheaf s = Subpresheaf.generatedBy(y, new int[][] { { 0, y.elementOf(AFF2.find(0, 0, ZERO)) } });
        Subpresheaf j = s.join(s.negation());
        assertFalse(j.equals(Subpresheaf.top(y)));
        assertEquals(2, j.size(0));
        assertEquals(s, s.negation().negation());
    }

    public void testGroupoidsAreBoolean() {
        for (int n = 2; n <= 4; n++) {
            FiniteCategory g = FiniteCategory.affineAutomorphisms(n);
            FiniteModAt t = new FiniteModAt(g);
            assertEquals(2, t.sievesOn(0).size());
            assertTrue(t.isBooleanTopos());
            assertTrue(t.isBoolean(FinitePresheaf.representable(g, 0)));
            assertTrue(t.isBoolean(t.omega()));
        }
    }

    private static List<FiniteCategory> categories() {
        List<FiniteCategory> list = new ArrayList<FiniteCategory>();
        list.add(AFF2);
        list.add(FiniteCategory.affineCyclic(1, 2));
        list.add(FiniteCategory.affineCyclic(2, 3));
        list.add(FiniteCategory.affineCyclic(1, 2, 3));
        list.add(FiniteCategory.affineCyclic(4));
        list.add(FiniteCategory.affineAutomorphisms(2));
        list.add(FiniteCategory.affineAutomorphisms(3));
        list.add(FiniteCategory.affineAutomorphisms(4));
        list.add(FiniteCategory.digraphSite());
        return list;
    }

    public void testBooleanExactlyForGroupoids() {
        // Topos I p. 335: a presheaf topos is Boolean iff C is a groupoid
        for (FiniteCategory c : categories()) {
            FiniteModAt t = new FiniteModAt(c);
            assertEquals(c.objectName(0) + "/" + c.arrowCount(), c.isGroupoid(), t.isBooleanTopos());
        }
    }

    private static List<FinitePresheaf> presheaves(FiniteModAt t) {
        FiniteCategory c = t.category();
        List<FinitePresheaf> list = new ArrayList<FinitePresheaf>();
        for (int y = 0; y < c.objectCount(); y++) {
            list.add(FinitePresheaf.representable(c, y));
        }
        list.add(FinitePresheaf.terminal(c));
        list.add(t.omega());
        return list;
    }

    public void testYonedaSubOfRepresentableIsOmega() {
        // Example 127: Sub(@Y) = Omega(Y)
        for (FiniteCategory c : categories()) {
            FiniteModAt t = new FiniteModAt(c);
            for (int y = 0; y < c.objectCount(); y++) {
                FinitePresheaf r = FinitePresheaf.representable(c, y);
                assertEquals(t.sievesOn(y).size(), t.subobjects(r).size());
            }
        }
    }

    public void testCharacteristicMapsClassifySubobjects() {
        // Def. 169: each subobject is the pullback of true along exactly one map
        for (FiniteCategory c : categories()) {
            FiniteModAt t = new FiniteModAt(c);
            for (FinitePresheaf p : presheaves(t)) {
                List<Subpresheaf> subs = t.subobjects(p);
                List<int[][]> nats = t.naturalTransformations(p, t.omega());
                assertEquals(subs.size(), nats.size());
                for (Subpresheaf s : subs) {
                    int[][] chi = t.characteristic(s);
                    assertTrue(t.isNatural(p, t.omega(), chi));
                    assertEquals(s, t.pullbackOfTrue(p, chi));
                    int hits = 0;
                    for (int[][] n : nats) {
                        if (t.pullbackOfTrue(p, n).equals(s)) {
                            hits++;
                        }
                    }
                    assertEquals(1, hits);
                }
            }
        }
    }

    public void testHeytingLaws() {
        // G.27, Prop. 123 and 124 on Sub(P), with the operations of Prop. 125
        for (FiniteCategory c : categories()) {
            FiniteModAt t = new FiniteModAt(c);
            for (FinitePresheaf p : presheaves(t)) {
                List<Subpresheaf> subs = t.subobjects(p);
                if (subs.size() > 40) {
                    continue;
                }
                Subpresheaf top = Subpresheaf.top(p);
                Subpresheaf bottom = Subpresheaf.bottom(p);
                boolean nn = true, lem = true;
                for (Subpresheaf x : subs) {
                    Subpresheaf nx = x.negation();
                    assertEquals(x.implies(bottom), nx);
                    assertTrue(x.leq(nx.negation()));
                    assertEquals(nx, nx.negation().negation());
                    assertEquals(top, x.implies(x));
                    nn &= nx.negation().equals(x);
                    lem &= x.join(nx).equals(top);
                    for (Subpresheaf y : subs) {
                        if (x.leq(y)) {
                            assertTrue(y.negation().leq(nx));
                        }
                        assertEquals(x.meet(y).negation().negation(),
                                     nx.negation().meet(y.negation().negation()));
                        assertEquals(x.meet(y), x.meet(x.implies(y)));
                        assertEquals(y, y.meet(x.implies(y)));
                        for (Subpresheaf z : subs) {
                            assertEquals(z.leq(y.implies(x)), z.meet(y).leq(x));
                            assertEquals(x.implies(y.meet(z)), x.implies(y).meet(x.implies(z)));
                            assertEquals(x.meet(y.join(z)), x.meet(y).join(x.meet(z)));
                        }
                    }
                }
                assertEquals(nn, lem);
                assertEquals(nn, t.isBoolean(p));
            }
        }
    }

    public void testJoinAndMeetArePointwise() {
        FiniteCategory c = FiniteCategory.affineCyclic(1, 2);
        FiniteModAt t = new FiniteModAt(c);
        FinitePresheaf p = t.omega();
        for (Subpresheaf x : t.subobjects(p)) {
            for (Subpresheaf y : t.subobjects(p)) {
                for (int o = 0; o < c.objectCount(); o++) {
                    for (int e = 0; e < p.size(o); e++) {
                        assertEquals(x.contains(o, e) || y.contains(o, e), x.join(y).contains(o, e));
                        assertEquals(x.contains(o, e) && y.contains(o, e), x.meet(y).contains(o, e));
                    }
                }
            }
        }
    }

    public void testNegationIsTheCharacterOfFalse() {
        // Topos IV p. 1434, item 1
        for (FiniteCategory c : categories()) {
            FiniteModAt t = new FiniteModAt(c);
            FinitePresheaf omega = t.omega();
            int[][] neg = t.negation();
            assertTrue(t.isNatural(omega, omega, neg));
            int[] f = t.falseArrow();
            int[][] gens = new int[c.objectCount()][];
            for (int x = 0; x < c.objectCount(); x++) {
                gens[x] = new int[] { x, f[x] };
                assertTrue(t.sievesOn(x).get(f[x]).isEmpty());
            }
            Subpresheaf falseSub = Subpresheaf.generatedBy(omega, gens);
            for (int x = 0; x < c.objectCount(); x++) {
                assertEquals(1, falseSub.size(x));
            }
            int[][] chi = t.characteristic(falseSub);
            for (int x = 0; x < c.objectCount(); x++) {
                assertTrue(java.util.Arrays.equals(neg[x], chi[x]));
            }
        }
    }

    public void testTwoGlobalTruthValuesWithZeroModule() {
        // Topos I p. 335: 1 = @0 and 1@Omega = Omega(0) = Sub(@0)
        FiniteCategory c = FiniteCategory.affineCyclic(1, 2, 3);
        FiniteModAt t = new FiniteModAt(c);
        int zero = c.terminalObject();
        assertEquals(0, zero);
        assertEquals(2, t.sievesOn(zero).size());
        assertEquals(2, t.globalTruthValues().size());
        assertFalse(t.isBooleanTopos());
    }

    public void testTerminalSievesAreSupportSets() {
        // Topos I p. 336: a sieve in the terminal object is its set of
        // addresses, closed under maps into its members
        List<FiniteCategory> list = categories();
        list.add(FiniteCategory.of(new String[] { "0", "1" }, new int[] { 0, 1, 0 },
                                   new int[] { 0, 1, 1 }, new String[] { "1_0", "1_1", "u" },
                                   new int[] { 0, 1 },
                                   new int[][] { { 0, -1, -1 }, { -1, 1, 2 }, { 2, -1, -1 } }));
        int checked = 0;
        for (FiniteCategory c : list) {
            int one = c.terminalObject();
            if (one < 0) {
                continue;
            }
            checked++;
            FiniteModAt t = new FiniteModAt(c);
            Set<Set<Integer>> fromSieves = new HashSet<Set<Integer>>();
            for (BitSet s : t.sievesOn(one)) {
                Set<Integer> support = new HashSet<Integer>();
                for (int f = s.nextSetBit(0); f >= 0; f = s.nextSetBit(f + 1)) {
                    support.add(c.dom(f));
                }
                fromSieves.add(support);
            }
            Set<Set<Integer>> closed = new HashSet<Set<Integer>>();
            int k = c.objectCount();
            for (int mask = 0; mask < (1 << k); mask++) {
                Set<Integer> s = new HashSet<Integer>();
                for (int o = 0; o < k; o++) {
                    if ((mask & (1 << o)) != 0) {
                        s.add(o);
                    }
                }
                boolean ok = true;
                for (int f = 0; f < c.arrowCount(); f++) {
                    if (s.contains(c.cod(f)) && !s.contains(c.dom(f))) {
                        ok = false;
                    }
                }
                if (ok) {
                    closed.add(s);
                }
            }
            assertEquals(closed, fromSieves);
            assertEquals(t.sievesOn(one).size(), t.globalTruthValues().size());
        }
        assertEquals(3, checked); // affineCyclic(1, 2), affineCyclic(1, 2, 3) and the chain
        FiniteCategory chain = list.get(list.size() - 1);
        assertEquals(3, new FiniteModAt(chain).globalTruthValues().size());
    }

    private static FinitePresheaf ofDigraph(Digraph d) {
        FiniteCategory s = FiniteCategory.digraphSite();
        int[] sizes = new int[2];
        sizes[FiniteCategory.V] = d.vertexCount();
        sizes[FiniteCategory.A] = d.arrowCount();
        int[][] restrict = new int[s.arrowCount()][];
        for (int f = 0; f < s.arrowCount(); f++) {
            int from = s.cod(f);
            restrict[f] = new int[sizes[from]];
            for (int e = 0; e < sizes[from]; e++) {
                if (f == FiniteCategory.TAIL) {
                    restrict[f][e] = d.tail(e);
                } else if (f == FiniteCategory.HEAD) {
                    restrict[f][e] = d.head(e);
                } else {
                    restrict[f][e] = e;
                }
            }
        }
        return new FinitePresheaf(s, sizes, restrict);
    }

    public void testDigraphSiteAgreesWithStageSix() {
        FiniteCategory s = FiniteCategory.digraphSite();
        FiniteModAt t = new FiniteModAt(s);
        FinitePresheaf omega = t.omega();
        assertEquals(2, omega.size(FiniteCategory.V));
        assertEquals(5, omega.size(FiniteCategory.A));
        // the same digraph as DigraphTopos.omega(), vertex FALSE = empty sieve
        int emptyV = t.sievesOn(FiniteCategory.V).indexOf(new BitSet());
        Digraph mine = ofOmega(omega, emptyV);
        Digraph theirs = DigraphTopos.omega();
        assertEquals(sortedArrows(theirs), sortedArrows(mine));
        Digraph[] samples = { Digraph.point(), Digraph.loop(), Digraph.discrete(2),
                              Digraph.arrowN(1), Digraph.arrowN(2),
                              Digraph.of(1, new int[] { 0, 0 }, new int[] { 0, 0 }),
                              Digraph.of(2, new int[] { 0, 1 }, new int[] { 1, 0 }) };
        for (Digraph d : samples) {
            FinitePresheaf p = ofDigraph(d);
            List<Subpresheaf> subs = t.subobjects(p);
            assertEquals(Subdigraph.all(d).size(), subs.size());
            assertEquals(subs.size(), t.naturalTransformations(p, omega).size());
            for (Subdigraph sd : Subdigraph.all(d)) {
                Subpresheaf sp = Subpresheaf.of(p, vertexSet(sd), arrowSet(sd));
                int[][] chi = t.characteristic(sp);
                DigraphMorphism dchi = DigraphTopos.characteristic(sd);
                for (int v = 0; v < d.vertexCount(); v++) {
                    boolean isTrue = chi[FiniteCategory.V][v] == t.trueAt(FiniteCategory.V);
                    assertEquals(dchi.vertex(v) == DigraphTopos.TRUE, isTrue);
                }
                for (int a = 0; a < d.arrowCount(); a++) {
                    BitSet sieve = t.sievesOn(FiniteCategory.A).get(chi[FiniteCategory.A][a]);
                    int expected = DigraphTopos.F;
                    boolean id = sieve.get(s.identity(FiniteCategory.A));
                    boolean tl = sieve.get(FiniteCategory.TAIL);
                    boolean hd = sieve.get(FiniteCategory.HEAD);
                    if (id) {
                        expected = DigraphTopos.T;
                    } else if (tl && hd) {
                        expected = DigraphTopos.Q;
                    } else if (tl) {
                        expected = DigraphTopos.N;
                    } else if (hd) {
                        expected = DigraphTopos.P;
                    }
                    assertEquals(dchi.arrow(a), expected);
                }
            }
        }
    }

    private static boolean[] vertexSet(Subdigraph sd) {
        boolean[] v = new boolean[sd.ambient().vertexCount()];
        for (int i = 0; i < v.length; i++) {
            v[i] = sd.containsVertex(i);
        }
        return v;
    }

    private static boolean[] arrowSet(Subdigraph sd) {
        boolean[] a = new boolean[sd.ambient().arrowCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = sd.containsArrow(i);
        }
        return a;
    }

    private static Digraph ofOmega(FinitePresheaf omega, int emptyV) {
        int n = omega.size(FiniteCategory.A);
        int[][] arr = new int[n][];
        for (int a = 0; a < n; a++) {
            int tl = omega.restrict(FiniteCategory.TAIL, a) == emptyV ? DigraphTopos.FALSE : DigraphTopos.TRUE;
            int hd = omega.restrict(FiniteCategory.HEAD, a) == emptyV ? DigraphTopos.FALSE : DigraphTopos.TRUE;
            arr[a] = new int[] { tl, hd };
        }
        return Digraph.of(2, arr);
    }

    private static List<String> sortedArrows(Digraph d) {
        List<String> l = new ArrayList<String>();
        for (int a = 0; a < d.arrowCount(); a++) {
            l.add(d.tail(a) + ">" + d.head(a));
        }
        java.util.Collections.sort(l);
        return l;
    }

    public void testSizeGuard() {
        // eight points on which every arrow acts as the identity: 256 subobjects
        int[][] restrict = new int[AFF2.arrowCount()][];
        for (int f = 0; f < AFF2.arrowCount(); f++) {
            restrict[f] = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        }
        FinitePresheaf p = new FinitePresheaf(AFF2, new int[] { 8 }, restrict);
        assertEquals(256, new FiniteModAt(AFF2).subobjects(p).size());
        FiniteModAt t = new FiniteModAt(AFF2, 50);
        try {
            t.subobjects(p);
            fail("no limit on subobjects");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("50"));
        }
        try {
            t.naturalTransformations(p, p);
            fail("no limit on natural transformations");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("50"));
        }
    }

    public void testRefusesAPresheafThatIsNotAFunctor() {
        FiniteCategory c = AFF2;
        int[][] restrict = new int[c.arrowCount()][];
        for (int f = 0; f < c.arrowCount(); f++) {
            restrict[f] = new int[] { 0, 1 };
        }
        // a two-element set where every arrow acts as the identity is a functor
        new FinitePresheaf(c, new int[] { 2 }, restrict);
        // the constant 0 acting as the swap breaks 0∘0 = 0
        restrict[c.find(0, 0, ZERO)] = new int[] { 1, 0 };
        try {
            new FinitePresheaf(c, new int[] { 2 }, restrict);
            fail("accepted a non-functor");
        } catch (IllegalArgumentException expected) { /* refused */ }
    }

    public void testSubpresheafMustBeClosed() {
        FinitePresheaf y = FinitePresheaf.representable(AFF2, 0);
        boolean[][] in = new boolean[1][4];
        in[0][y.elementOf(AFF2.find(0, 0, ID))] = true;
        try {
            new Subpresheaf(y, in);
            fail("accepted a set that is not closed");
        } catch (IllegalArgumentException expected) { /* refused */ }
    }
}
