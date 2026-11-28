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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.topos.FiniteCategory;
import org.rubato.math.topos.UniversalMonoid;

/**
 * The universal monoid Mon(C) of a finite category and its reduced-word
 * normal forms (arXiv 2611.32655, Def. 2.1, Example 2.2, Lemmas 2.3 and
 * 2.4, Remark 3.2).
 */
public class UniversalMonoidTest extends TestCase {

    public void testExample22NormalFormOfG2F1IsK() {
        UniversalMonoid m = new UniversalMonoid(MonoidFixtures.categoryP());
        assertEquals(6, m.letters().size());
        assertTrue(Arrays.equals(new int[] { MonoidFixtures.K },
                                 m.normalize(MonoidFixtures.G2, MonoidFixtures.F1)));
        assertTrue(Arrays.equals(new int[] { MonoidFixtures.H },
                                 m.normalize(MonoidFixtures.G1, MonoidFixtures.F1)));
        assertFalse(m.isReduced(new int[] { MonoidFixtures.G2, MonoidFixtures.F1 }));
        assertTrue(m.isReduced(new int[] { MonoidFixtures.F1, MonoidFixtures.G2 }));
        assertEquals("k", m.format(m.normalize(MonoidFixtures.G2, MonoidFixtures.F1)));
    }

    public void testExample22KroneckerMonoidIsFree() {
        UniversalMonoid m = new UniversalMonoid(FiniteCategory.digraphSite().opposite());
        assertEquals(2, m.letters().size());
        // no two letters are composable, so every word is reduced: 2^l of length l
        List<int[]> words = m.reducedWords(6);
        int[] byLength = new int[7];
        for (int[] w : words) {
            byLength[w.length]++;
        }
        for (int l = 0; l <= 6; l++) {
            assertEquals("length " + l, 1 << l, byLength[l]);
        }
    }

    /** Lemma 2.3: leftmost and rightmost rewriting meet the normal form on 9330 words. */
    public void testLemma23ConfluenceOnAllWordsUpToLengthFive() {
        FiniteCategory p = MonoidFixtures.categoryP();
        UniversalMonoid m = new UniversalMonoid(p);
        List<int[]> words = MonoidFixtures.allWords(m, 5);
        assertEquals(9330, words.size());
        for (int[] w : words) {
            List<Integer> nf = MonoidFixtures.list(m.normalize(w));
            assertEquals(Arrays.toString(w), MonoidFixtures.rewriteLeftmost(p, MonoidFixtures.list(w)), nf);
            assertEquals(Arrays.toString(w), MonoidFixtures.rewriteRightmost(p, MonoidFixtures.list(w)), nf);
            assertTrue(m.isReduced(m.normalize(w)));
        }
    }

    /** Lemma 2.3: the reduced words are exactly the normal forms, each its own. */
    public void testReducedWordsAreDistinctNormalForms() {
        UniversalMonoid m = new UniversalMonoid(MonoidFixtures.categoryP());
        Set<String> seen = new HashSet<String>();
        for (int[] w : m.reducedWords(5)) {
            assertTrue(m.isReduced(w));
            assertTrue(Arrays.equals(w, m.normalize(w)));
            assertTrue(seen.add(Arrays.toString(w)));
        }
        // every word of length <= 5 normalizes into this list
        for (int[] w : MonoidFixtures.allWords(m, 5)) {
            assertTrue(seen.contains(Arrays.toString(m.normalize(w))));
        }
    }

    public void testMultiplyIsAssociativeAndHasTheEmptyWordAsUnit() {
        UniversalMonoid m = new UniversalMonoid(MonoidFixtures.categoryP());
        List<int[]> ws = m.reducedWords(3);
        for (int[] a : ws) {
            assertTrue(Arrays.equals(a, m.multiply(a, new int[0])));
            assertTrue(Arrays.equals(a, m.multiply(new int[0], a)));
            for (int[] b : ws) {
                for (int[] c : ws.subList(0, Math.min(ws.size(), 40))) {
                    assertTrue(Arrays.equals(m.multiply(m.multiply(a, b), c), m.multiply(a, m.multiply(b, c))));
                }
            }
        }
    }

    /** Lemma 2.4: (w, g) -> wg is a bijection from the disjoint union of W_d x C(c, d). */
    public void testLemma24UniqueFactorization() {
        FiniteCategory p = MonoidFixtures.categoryP();
        UniversalMonoid m = new UniversalMonoid(p);
        int maxLength = 4;
        for (int c = 0; c < p.objectCount(); c++) {
            Set<String> images = new HashSet<String>();
            int pairs = 0;
            for (int d = 0; d < p.objectCount(); d++) {
                for (int[] w : m.wordsW(d, maxLength)) {
                    for (int g : p.hom(c, d)) {
                        int[] wg = m.multiply(w, m.normalize(g));
                        if (wg.length > maxLength) {
                            continue;
                        }
                        pairs++;
                        assertTrue("injective at c=" + c, images.add(Arrays.toString(wg)));
                        UniversalMonoid.Factorization f = m.factor(wg, c);
                        assertTrue(Arrays.equals(w, f.w));
                        assertEquals(g, f.g);
                    }
                }
            }
            // surjective onto all elements of length <= maxLength
            assertEquals(m.reducedWords(maxLength).size(), images.size());
            assertEquals(pairs, images.size());
        }
    }

    /** W_c holds the empty word and the reduced words whose first-acting letter leaves another object. */
    public void testWordsWAtAnObject() {
        UniversalMonoid m = new UniversalMonoid(MonoidFixtures.categoryP());
        for (int[] w : m.wordsW(0, 3)) {
            assertTrue(w.length == 0 || m.category().dom(w[w.length - 1]) != 0);
            assertTrue(m.inW(w, 0));
        }
        assertFalse(m.inW(new int[] { MonoidFixtures.F1 }, 0));
        assertTrue(m.inW(new int[] { MonoidFixtures.G1 }, 0));
    }

    /** Remark 3.2: with a retraction r s = 1, the word r s is the unit. */
    public void testRetractionCancels() {
        FiniteCategory c = MonoidFixtures.retraction();
        assertFalse(c.isAcyclic());
        UniversalMonoid m = new UniversalMonoid(c);
        assertEquals(0, m.normalize(MonoidFixtures.R, MonoidFixtures.S).length);
        assertTrue(Arrays.equals(new int[] { MonoidFixtures.E }, m.normalize(MonoidFixtures.S, MonoidFixtures.R)));
        assertTrue(Arrays.equals(new int[] { MonoidFixtures.R },
                                 m.normalize(MonoidFixtures.R, MonoidFixtures.S, MonoidFixtures.R)));
    }

    /** For one object, Mon(C) = C: every two letters compose, so reduced words have length at most 1. */
    public void testOneObjectCategoryIsItsOwnMonoid() {
        FiniteCategory c = FiniteCategory.affineAutomorphisms(3);
        UniversalMonoid m = new UniversalMonoid(c);
        assertEquals(c.arrowCount() - 1, m.letters().size());
        assertEquals(c.arrowCount(), m.reducedWords(4).size());
    }

    public void testOppositeAndAcyclic() {
        FiniteCategory p = MonoidFixtures.categoryP();
        FiniteCategory op = p.opposite();
        assertEquals(p.arrowCount(), op.arrowCount());
        assertEquals(p.dom(MonoidFixtures.F1), op.cod(MonoidFixtures.F1));
        assertEquals(MonoidFixtures.K, op.compose(MonoidFixtures.F1, MonoidFixtures.G2));
        assertTrue(p.isAcyclic());
        assertTrue(op.isAcyclic());
        assertFalse(FiniteCategory.affineAutomorphisms(3).isAcyclic());
    }
}
