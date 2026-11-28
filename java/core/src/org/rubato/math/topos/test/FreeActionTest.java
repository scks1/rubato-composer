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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.topos.CategoryOfElements;
import org.rubato.math.topos.FiniteCategory;
import org.rubato.math.topos.FiniteFunctor;
import org.rubato.math.topos.FreeAction;
import org.rubato.math.topos.FreeAction.Element;
import org.rubato.math.topos.MasterMonoidExamples;
import org.rubato.math.topos.UniversalMonoid;

/**
 * The category of elements and the free action of a finite functor
 * (arXiv 2611.32655, section 1.1, Prop. 2.5, Example 2.7, Thm. 3.1 and
 * Remark 3.2).
 */
public class FreeActionTest extends TestCase {

    /** Example 2.7: the category of elements of two parallel edges is the crown. */
    public void testCategoryOfElementsOfTheCrown() {
        FiniteFunctor x = MonoidFixtures.crown();
        CategoryOfElements el = new CategoryOfElements(x);
        FiniteCategory c = el.category();
        assertEquals(4, c.objectCount());
        // 4 identities and one arrow from each edge to each of its ends
        assertEquals(8, c.arrowCount());
        assertTrue(c.isAcyclic());
        int e1 = el.objectOf(FiniteCategory.A, 0);
        int a = el.objectOf(FiniteCategory.V, 0);
        int b = el.objectOf(FiniteCategory.V, 1);
        assertEquals(1, c.hom(e1, a).size());
        assertEquals(1, c.hom(e1, b).size());
        assertEquals(0, c.hom(a, e1).size());
        assertEquals(FiniteCategory.TAIL, el.baseArrow(c.hom(e1, a).get(0)));
    }

    /** Example 2.7: W_E = {1}, the edges absorb s and t, a vertex absorbs neither. */
    public void testExample27FreeAction() {
        FreeAction act = new FreeAction(MonoidFixtures.crown());
        UniversalMonoid m = act.monoid();
        int s = FiniteCategory.TAIL, t = FiniteCategory.HEAD;
        assertEquals(1, m.wordsW(FiniteCategory.A, 5).size());
        assertEquals(63, m.wordsW(FiniteCategory.V, 5).size());
        Element e1 = act.root(FiniteCategory.A, 0);
        Element a = act.root(FiniteCategory.V, 0);
        Element b = act.root(FiniteCategory.V, 1);
        assertEquals(a, act.act(new int[] { s }, e1));
        assertEquals(b, act.act(new int[] { t }, e1));
        assertTrue(act.absorbs(e1, s));
        assertTrue(act.absorbs(e1, t));
        assertFalse(act.absorbs(a, s));
        assertFalse(act.absorbs(a, t));
        Element sa = act.act(new int[] { s }, a);
        assertTrue(Arrays.equals(new int[] { s }, sa.word));
        assertFalse(sa.isRoot());
        // u . [w, v] = [uw, v]
        Element tsa = act.act(new int[] { t }, sa);
        assertTrue(Arrays.equals(new int[] { t, s }, tsa.word));
        assertFalse(act.isFinite());
        // 2 edges + 2 vertices x (1 + 2 + 4) words of length <= 2
        assertEquals(16, act.window(2).size());
        String d = act.describeWindow(2);
        assertTrue(d, d.contains("truncated"));
    }

    /** Prop. 2.5: the action is an action, m'.(m.e) = (m'm).e, on a window. */
    public void testProp25IsAnAction() {
        FreeAction act = new FreeAction(MonoidFixtures.crown());
        UniversalMonoid m = act.monoid();
        List<int[]> ws = m.reducedWords(3);
        for (Element e : act.window(2)) {
            assertEquals(e, act.act(new int[0], e));
            for (int[] p : ws) {
                for (int[] q : ws) {
                    assertEquals(act.act(m.multiply(p, q), e), act.act(p, act.act(q, e)));
                }
            }
        }
    }

    /**
     * Thm. 3.1: the arrows from a root [1, x] to [w', x'] in the translation
     * category are the m with m.[1, x] = [w', x'], and they correspond to the
     * arrows g : (c, x) -> (c', x') of the category of elements by m = w' g.
     * Brute force over every m of length <= 4 on Example 2.7.
     */
    public void testThm31HomBijectionOnTheCrown() {
        checkHomBijection(new FreeAction(MonoidFixtures.crown()), 4, 3);
    }

    /** Thm. 3.1 on the Tonnetz as a functor on C_2, words of length <= 2. */
    public void testThm31HomBijectionOnTheTonnetz() {
        FreeAction act = new FreeAction(MasterMonoidExamples.tonnetzFunctor());
        checkHomBijection(act, 2, 1);
    }

    /** Thm. 3.1 on the category P of Example 2.2 with its representable at x as functor. */
    public void testThm31HomBijectionOnACategoryWithRelations() {
        FiniteCategory p = MonoidFixtures.categoryP();
        // X = Hom(x, -): the arrows out of x
        int[] sizes = new int[3];
        List<List<Integer>> vals = new ArrayList<List<Integer>>();
        for (int c = 0; c < 3; c++) {
            vals.add(p.hom(0, c));
            sizes[c] = p.hom(0, c).size();
        }
        int[][] a = new int[p.arrowCount()][];
        for (int f = 0; f < p.arrowCount(); f++) {
            List<Integer> from = vals.get(p.dom(f));
            a[f] = new int[from.size()];
            for (int i = 0; i < from.size(); i++) {
                a[f][i] = vals.get(p.cod(f)).indexOf(p.compose(f, from.get(i)));
            }
        }
        FreeAction act = new FreeAction(new FiniteFunctor(p, sizes, a));
        checkHomBijection(act, 3, 2);
    }

    private void checkHomBijection(FreeAction act, int maxLength, int targetLength) {
        UniversalMonoid m = act.monoid();
        FiniteFunctor x = act.functor();
        CategoryOfElements el = new CategoryOfElements(x);
        List<int[]> words = m.reducedWords(maxLength);
        int checked = 0;
        for (int c = 0; c < x.category().objectCount(); c++) {
            for (int xi = 0; xi < x.size(c); xi++) {
                Element root = act.root(c, xi);
                Map<Element, Set<String>> brute = new HashMap<Element, Set<String>>();
                for (int[] w : words) {
                    Element target = act.act(w, root);
                    if (target.word.length <= targetLength) {
                        Set<String> s = brute.get(target);
                        if (s == null) {
                            s = new HashSet<String>();
                            brute.put(target, s);
                        }
                        s.add(Arrays.toString(w));
                    }
                }
                for (Element target : act.window(targetLength)) {
                    Set<String> formula = new HashSet<String>();
                    for (int[] w : act.coreArrows(root, target)) {
                        if (w.length <= maxLength) {
                            formula.add(Arrays.toString(w));
                        }
                    }
                    Set<String> b = brute.get(target);
                    if (b == null) {
                        b = new HashSet<String>();
                    }
                    assertEquals(root + " -> " + target, b, formula);
                    // and the count is the hom-set of the category of elements
                    int nel = el.category().hom(el.objectOf(c, xi), el.objectOf(target.type, target.x)).size();
                    assertEquals(nel, act.coreArrows(root, target).size());
                    checked++;
                }
            }
        }
        assertTrue(checked > 0);
    }

    /** Thm. 3.1: R sends m : [w, x] -> [w', x'] to the g with m w = w' g; the counit is w. */
    public void testThm31RootArrowAndCounit() {
        FreeAction act = new FreeAction(MonoidFixtures.crown());
        UniversalMonoid m = act.monoid();
        for (Element e : act.window(2)) {
            assertTrue(Arrays.equals(e.word, act.counit(e)));
            assertEquals(act.root(e.type, e.x), act.rootOf(e));
            assertEquals(e, act.act(act.counit(e), act.rootOf(e)));
            for (int[] w : m.reducedWords(2)) {
                Element t = act.act(w, e);
                int g = act.rootArrow(w, e);
                FiniteCategory c = act.functor().category();
                assertEquals(e.type, c.dom(g));
                assertEquals(t.type, c.cod(g));
                assertEquals(t.x, act.functor().act(g, e.x));
                assertTrue(Arrays.equals(m.multiply(w, e.word), m.multiply(t.word, m.normalize(g))));
            }
        }
    }

    /** Remark 3.2: with r s = 1 a branch returns to the core, r.[s, x] = [1, x]. */
    public void testRemark32BranchReturnsWithARetraction() {
        FiniteCategory c = MonoidFixtures.retraction();
        // X(a) = {p}, X(b) = {q}, every arrow constant
        int[][] a = new int[c.arrowCount()][];
        for (int f = 0; f < c.arrowCount(); f++) {
            a[f] = new int[] { 0 };
        }
        FreeAction act = new FreeAction(new FiniteFunctor(c, new int[] { 1, 1 }, a));
        Element q = act.root(1, 0);
        // [s, q]: s : a -> b cannot be absorbed by q, which has type b
        Element sq = act.act(new int[] { MonoidFixtures.S }, q);
        assertFalse(sq.isRoot());
        assertEquals(q, act.act(new int[] { MonoidFixtures.R }, sq));
    }

    /** One-object categories: Mon(C) = C and the free action is X itself, a complete window. */
    public void testOneObjectFreeActionIsFinite() {
        FiniteCategory c = FiniteCategory.affineAutomorphisms(3);
        int n = c.arrowCount();
        int[][] a = new int[n][];
        for (int f = 0; f < n; f++) {
            a[f] = c.table(f);
        }
        FreeAction act = new FreeAction(new FiniteFunctor(c, new int[] { 3 }, a));
        assertTrue(act.isFinite());
        assertEquals(3, act.window(5).size());
        String d = act.describeWindow(5);
        assertTrue(d, d.contains("complete"));
    }

    public void testFunctorChecksItsLaws() {
        FiniteCategory k = FiniteCategory.digraphSite().opposite();
        int[][] a = { { 0 }, { 0 }, { 0 }, { 0 } };
        new FiniteFunctor(k, new int[] { 1, 1 }, a);
        try {
            new FiniteFunctor(k, new int[] { 1, 1 }, new int[][] { { 0 }, { 0 }, { 1 }, { 0 } });
            fail("an image outside the value must be refused");
        } catch (IllegalArgumentException expected) {
            // refused
        }
        try {
            new FiniteFunctor(k, new int[] { 2, 1 }, new int[][] { { 1, 0 }, { 0 }, { 0 }, { 0 } });
            fail("an identity that moves an element must be refused");
        } catch (IllegalArgumentException expected) {
            // refused
        }
    }
}
