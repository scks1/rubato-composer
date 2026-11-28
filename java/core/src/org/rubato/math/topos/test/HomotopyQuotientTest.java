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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.rubato.math.global.SimplicialComplex;
import org.rubato.math.harmony.Triads;
import org.rubato.math.topos.CategoryOfElements;
import org.rubato.math.topos.ChainComplex;
import org.rubato.math.topos.FiniteCategory;
import org.rubato.math.topos.FiniteFunctor;
import org.rubato.math.topos.FiniteMonoidAction;
import org.rubato.math.topos.FreeAction;
import org.rubato.math.topos.FreeAction.Element;
import org.rubato.math.topos.MasterMonoidExamples;
import org.rubato.math.topos.SemisimplicialCategory;
import org.rubato.math.topos.SemisimplicialSet;
import org.rubato.math.topos.UniversalMonoid;

/**
 * Homology of finite semisimplicial sets and of homotopy quotients of
 * finite M_n-sets (arXiv 2611.32655, Example 1.3, Example 1.7, Example 2.7,
 * Example 4.9, Lemma 4.10, Thm. 4.12 and Remark 4.14), and the Tonnetz as
 * a 2-dimensional semisimplicial set.
 */
public class HomotopyQuotientTest extends TestCase {

    private static String h(SemisimplicialSet x) {
        return ChainComplex.format(x.homology());
    }

    public void testChainComplexRefusesABoundaryWhoseSquareIsNotZero() {
        long[][][] b = { null, { { 1 } }, { { 1 } } };
        try {
            new ChainComplex(new int[] { 1, 1, 1 }, b);
            fail("d d != 0 must be refused");
        } catch (IllegalArgumentException expected) {
            // refused
        }
    }

    /** Torsion: a two-vertex semisimplicial set with H_1 = Z/2 (values from the probe's own Smith form). */
    public void testTorsion() {
        SemisimplicialSet x = new SemisimplicialSet(new int[] { 2, 3, 2 }, new int[][][] {
            null,
            { { 0, 0 }, { 0, 0 }, { 0, 1 } },
            { { 0, 0, 0 }, { 1, 0, 1 } } });
        assertEquals("[Z, Z/2, 0]", h(x));
        assertEquals(1, x.eulerCharacteristic());
    }

    public void testSemisimplicialSetRefusesBrokenIdentities() {
        try {
            // a triangle whose edges do not meet: d_0 d_1 != d_0 d_0
            new SemisimplicialSet(new int[] { 3, 3, 1 }, new int[][][] {
                null,
                { { 1, 0 }, { 2, 1 }, { 0, 2 } },
                { { 0, 1, 2 } } });
            fail("broken semisimplicial identities must be refused");
        } catch (IllegalArgumentException expected) {
            // refused
        }
    }

    /** Example 4.9: one vertex, one edge, two triangles with every face e: a sphere. */
    public void testExample49Sphere() {
        SemisimplicialSet s = MasterMonoidExamples.sphere();
        assertEquals("[Z, 0, Z]", h(s));
        assertEquals(2, s.eulerCharacteristic());
    }

    /**
     * Example 4.9 in M_2 ⋉ ι_2!X: the triangles absorb the three d^2_j and
     * branch on d^1_0, d^1_1; the edge absorbs the d^1_i and branches on the
     * d^2_j; the vertex absorbs nothing. Along branches the letters still
     * interact: d^1_0 . [d^2_0, e] = d^1_0 . [d^2_1, e].
     */
    public void testExample49FreeAction() {
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        FreeAction act = new FreeAction(MasterMonoidExamples.sphere().toFunctor(c2));
        Element sigma = act.root(2, 0), e = act.root(1, 0), v = act.root(0, 0);
        for (int j = 0; j <= 2; j++) {
            assertTrue(act.absorbs(sigma, c2.face(2, j)));
            assertEquals(e, act.act(new int[] { c2.face(2, j) }, sigma));
            assertFalse(act.absorbs(e, c2.face(2, j)));
            assertFalse(act.absorbs(v, c2.face(2, j)));
        }
        for (int i = 0; i <= 1; i++) {
            assertFalse(act.absorbs(sigma, c2.face(1, i)));
            assertTrue(act.absorbs(e, c2.face(1, i)));
            assertFalse(act.absorbs(v, c2.face(1, i)));
        }
        Element d20e = act.act(new int[] { c2.face(2, 0) }, e);
        Element d21e = act.act(new int[] { c2.face(2, 1) }, e);
        assertFalse(d20e.equals(d21e));
        int[] d10 = { c2.face(1, 0) };
        assertEquals(act.act(d10, d20e), act.act(d10, d21e));
    }

    /** Example 1.7: B of the category of elements has the homology of |X| (nerve by Remark 4.14). */
    public void testExample17NerveOfCategoryOfElements() {
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        for (SemisimplicialSet x : Arrays.asList(MasterMonoidExamples.sphere(), MasterMonoidExamples.tonnetz())) {
            CategoryOfElements el = new CategoryOfElements(x.toFunctor(c2));
            SemisimplicialSet nerve = SemisimplicialSet.nerve(el.category());
            assertEquals(h(x), h(nerve));
        }
    }

    public void testNerveRefusesACategoryWithCycles() {
        try {
            SemisimplicialSet.nerve(FiniteCategory.affineAutomorphisms(3));
            fail("a group is not acyclic");
        } catch (IllegalArgumentException expected) {
            // refused
        }
    }

    public void testFunctorRoundTrip() {
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        SemisimplicialSet t = MasterMonoidExamples.tonnetz();
        FiniteFunctor f = t.toFunctor(c2);
        SemisimplicialSet back = SemisimplicialSet.ofFunctor(c2, f);
        for (int k = 1; k <= 2; k++) {
            for (int s = 0; s < t.count(k); s++) {
                for (int i = 0; i <= k; i++) {
                    assertEquals(t.face(k, s, i), back.face(k, s, i));
                }
            }
        }
        assertEquals(72, f.totalSize());
    }

    // ---------------------------------------------------------------- Tonnetz

    public void testTonnetzCountsAndFaceOrder() {
        SemisimplicialSet t = MasterMonoidExamples.tonnetz();
        assertEquals(12, t.count(0));
        assertEquals(36, t.count(1));
        assertEquals(24, t.count(2));
        assertEquals(0, t.eulerCharacteristic());
        // C major (root 0, third 4, fifth 7): d0 the upper interval E-G, d1 the fifth C-G, d2 the lower C-E
        int cMajor = Triads.major(0);
        assertEquals("E-G", t.label(1, t.face(2, cMajor, 0)));
        assertEquals("C-G", t.label(1, t.face(2, cMajor, 1)));
        assertEquals("C-E", t.label(1, t.face(2, cMajor, 2)));
        // A minor (root 9, third 0, fifth 4): C-E, A-E, A-C
        int aMinor = Triads.minor(9);
        assertEquals("C-E", t.label(1, t.face(2, aMinor, 0)));
        assertEquals("A-E", t.label(1, t.face(2, aMinor, 1)));
        assertEquals("A-C", t.label(1, t.face(2, aMinor, 2)));
        assertEquals(Triads.name(cMajor), t.label(2, cMajor));
        // an edge's d0 is its upper pitch class, d1 its lower
        int ce = t.face(2, cMajor, 2);
        assertEquals("E", t.label(0, t.face(1, ce, 0)));
        assertEquals("C", t.label(0, t.face(1, ce, 1)));
    }

    public void testEveryTonnetzEdgeLiesOnTwoTriads() {
        SemisimplicialSet t = MasterMonoidExamples.tonnetz();
        int[] count = new int[36];
        for (int s = 0; s < 24; s++) {
            for (int i = 0; i <= 2; i++) {
                count[t.face(2, s, i)]++;
            }
        }
        for (int e = 0; e < 36; e++) {
            assertEquals(t.label(1, e), 2, count[e]);
        }
    }

    public void testTonnetzIsATorus() {
        assertEquals("[Z, Z^2, Z]", h(MasterMonoidExamples.tonnetz()));
    }

    /** The same 24 triads as vertex sets in the fork's SimplicialComplex give the same homology. */
    public void testTonnetzAgreesWithSimplicialComplex() {
        List<String> labels = new ArrayList<String>();
        SemisimplicialSet t = MasterMonoidExamples.tonnetz();
        for (int v = 0; v < 12; v++) {
            labels.add(t.label(0, v));
        }
        List<int[]> tris = new ArrayList<int[]>();
        for (int s = 0; s < Triads.COUNT; s++) {
            int r = Triads.root(s);
            tris.add(new int[] { r, (r + (Triads.isMajor(s) ? 4 : 3)) % 12, (r + 7) % 12 });
        }
        SimplicialComplex k = SimplicialComplex.generatedBy(labels, tris);
        assertEquals(k.homology().toString(), h(t));
        assertEquals(k.eulerCharacteristic(), t.eulerCharacteristic());
    }

    /**
     * Thm. 4.8 on the Tonnetz: in M_2 ⋉ ι_2!X the triads absorb the d^2_j and
     * branch on the d^1_i, the interval edges absorb the d^1_i, and the pitch
     * classes absorb nothing.
     */
    public void testTonnetzFreeAction() {
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        FreeAction act = new FreeAction(MasterMonoidExamples.tonnetzFunctor());
        assertFalse(act.isFinite());
        UniversalMonoid m = act.monoid();
        for (int letter : m.letters()) {
            int d = c2.category().dom(letter);
            for (int c = 0; c <= 2; c++) {
                for (int x = 0; x < act.functor().size(c); x++) {
                    assertEquals(d == c, act.absorbs(act.root(c, x), letter));
                }
            }
        }
        // window of length 1: 12 x 9 + 36 x 7 + 24 x 3 elements
        assertEquals(12 * 9 + 36 * 7 + 24 * 3, act.window(1).size());
    }

    // ------------------------------------------------ Lemma 4.10 and Thm 4.12

    /** Example 2.7 restricted: S = {1, 2, 3}, s the identity, t a 3-cycle: a bouquet of four circles. */
    public void testExample27RestrictedActionIsFourCircles() {
        SemisimplicialCategory c1 = SemisimplicialCategory.truncated(1);
        FiniteMonoidAction s = MasterMonoidExamples.example27Action();
        assertEquals("[Z, Z^4]", ChainComplex.format(s.homotopyQuotientHomology(c1)));
        // the restriction is the oriented triangle; its category of elements has the homology of a circle
        SemisimplicialSet tri = SemisimplicialSet.ofFunctor(c1, s.restriction());
        assertEquals("[Z, Z]", h(tri));
        // the letters swapped give the same
        FiniteMonoidAction swapped = FiniteMonoidAction.ofGenerators(c1, 3,
            new int[][][] { null, { { 0, 1, 2 }, { 1, 2, 0 } } });
        assertEquals("[Z, Z^4]", ChainComplex.format(swapped.homotopyQuotientHomology(c1)));
    }

    /**
     * Example 1.3 as an independent oracle for n = 1: M_1 = F_2 ⋉ S is free
     * on the graph with an edge x -> s x and an edge x -> t x for each x, so
     * its homology must match the Thm. 4.12 complex on 300 random F_2-sets.
     */
    public void testThm412AgreesWithExample13OnRandomF2Sets() {
        SemisimplicialCategory c1 = SemisimplicialCategory.truncated(1);
        Random rnd = new Random(32655);
        for (int trial = 0; trial < 300; trial++) {
            int n = 1 + rnd.nextInt(9);
            int[] a = new int[n], b = new int[n];
            for (int x = 0; x < n; x++) {
                a[x] = rnd.nextInt(n);
                b[x] = rnd.nextInt(n);
            }
            int[][] edges = new int[2 * n][];
            for (int x = 0; x < n; x++) {
                edges[x] = new int[] { a[x], x };
                edges[n + x] = new int[] { b[x], x };
            }
            SemisimplicialSet graph = new SemisimplicialSet(new int[] { n, 2 * n }, new int[][][] { null, edges });
            FiniteMonoidAction s = FiniteMonoidAction.ofGenerators(c1, n, new int[][][] { null, { a, b } });
            assertEquals("trial " + trial, h(graph), ChainComplex.format(s.homotopyQuotientHomology(c1)));
        }
    }

    /**
     * For n = 2 there is no oracle independent of the paper. This checks
     * the Thm. 4.12 complex (vertex v(s) of each typed copy) against Lemma
     * 4.10 read directly, with B of the category of elements of ι*S taken as
     * its nerve (Remark 4.14) and a cone vertex joined to every typed copy,
     * on 200 random finite M_2-sets.
     */
    public void testThm412AgreesWithLemma410ByNerveOnRandomM2Sets() {
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        Random rnd = new Random(2611);
        for (int trial = 0; trial < 200; trial++) {
            FiniteMonoidAction s = MonoidFixtures.randomM2Set(c2, rnd);
            assertEquals("trial " + trial,
                         ChainComplex.format(s.homotopyQuotientByNerve().homology()),
                         ChainComplex.format(s.homotopyQuotientHomology(c2)));
            assertTrue(s.homotopyQuotientComplex(c2).dimension() <= 2);
        }
        // the same comparison for n = 1 on Example 2.7
        FiniteMonoidAction ex = MasterMonoidExamples.example27Action();
        assertEquals(ChainComplex.format(ex.homotopyQuotientByNerve().homology()),
                     ChainComplex.format(ex.homotopyQuotientHomology(SemisimplicialCategory.truncated(1))));
    }

    /**
     * Thm. 4.12's extra edges join s&#772; to v(s) = Z(&epsilon;)(s), the 0-th
     * vertex, with &epsilon; computed here as the word d^1_1 d^2_2 (k = 2),
     * d^1_1 (k = 1) and the unit (k = 0). Homology cannot see this choice:
     * any vertex in the same path component gives the same homotopy type.
     */
    public void testThm412ExtraEdgesJoinTheZerothVertex() {
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        Random rnd = new Random(412);
        int[][] eps = { {}, c2.element(new int[][] { { 1, 1 } }), c2.element(new int[][] { { 1, 1 }, { 2, 2 } }) };
        for (int trial = 0; trial < 20; trial++) {
            FiniteMonoidAction s = MonoidFixtures.randomM2Set(c2, rnd);
            SemisimplicialSet q = s.homotopyQuotientComplex(c2);
            int n = s.size();
            for (int k = 0; k <= 2; k++) {
                for (int x = 0; x < n; x++) {
                    int edge = n + k * n + x;
                    assertEquals(n + x, q.face(1, edge, 0));
                    assertEquals(s.act(eps[k], x), q.face(1, edge, 1));
                }
            }
        }
    }

    /** Thm. 4.12: the complex for S has n+1 typed copies per element, one cone vertex and one edge per copy. */
    public void testThm412ComplexShape() {
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        FiniteMonoidAction s = MonoidFixtures.randomM2Set(c2, new Random(7));
        SemisimplicialSet q = s.homotopyQuotientComplex(c2);
        int n = s.size();
        assertEquals(2 * n, q.count(0));
        assertEquals(n + 3 * n, q.count(1));
        assertEquals(n, q.count(2));
    }

    public void testActionRefusesBrokenRelations() {
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        int[][][] g = { null, { { 0, 0 }, { 1, 1 } }, { { 0, 1 }, { 0, 1 }, { 1, 0 } } };
        try {
            // d^1_0 d^2_2 = d^1_1 d^2_0 fails: on 0, d^2_2 gives 1 then d^1_0 gives 0; d^2_0 gives 0 then d^1_1 gives 1
            FiniteMonoidAction.ofGenerators(c2, 2, g);
            fail("an M_2-set must satisfy the relations of Prop. 4.4");
        } catch (IllegalArgumentException expected) {
            // refused
        }
    }

    /** An action of Mon(C) restricts to a functor that has S at every object. */
    public void testRestrictionHasTheSameSetAtEveryObject() {
        FiniteMonoidAction s = MasterMonoidExamples.example27Action();
        FiniteFunctor r = s.restriction();
        for (int c = 0; c < r.category().objectCount(); c++) {
            assertEquals(3, r.size(c));
        }
        for (int letter : s.monoid().letters()) {
            for (int x = 0; x < 3; x++) {
                assertEquals(s.act(new int[] { letter }, x), r.act(letter, x));
            }
        }
    }

    /** The demo's report names the window as truncated and gives both homologies. */
    public void testReport() {
        String r = MasterMonoidExamples.report(1);
        assertTrue(r, r.contains("[Z, Z^2, Z]"));
        assertTrue(r, r.contains("[Z, 0, Z]"));
        assertTrue(r, r.contains("truncated"));
        assertTrue(r, r.contains("our choice"));
    }
}
