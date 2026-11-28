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
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.topos.FiniteCategory;
import org.rubato.math.topos.SemisimplicialCategory;
import org.rubato.math.topos.UniversalMonoid;

/**
 * The truncated semisimplicial categories C_n and the presentation of the
 * truncated master monoids M_n = Mon(C_n) (arXiv 2611.32655, section 4.1,
 * Def. 4.3, Prop. 4.4 and the M_2 presentation on p. 19).
 */
public class SemisimplicialMonoidTest extends TestCase {

    private static int binomial(int n, int k) {
        long r = 1;
        for (int i = 0; i < k; i++) {
            r = r * (n - i) / (i + 1);
        }
        return (int) r;
    }

    /** C_n has the injective monotone maps [l] -> [k] as arrows [k] -> [l]. */
    public void testTruncatedCategoryHomSizes() {
        for (int n = 0; n <= 4; n++) {
            SemisimplicialCategory s = SemisimplicialCategory.truncated(n);
            FiniteCategory c = s.category();
            assertEquals(n + 1, c.objectCount());
            assertTrue(c.isAcyclic());
            for (int k = 0; k <= n; k++) {
                for (int l = 0; l <= n; l++) {
                    int expected = l <= k ? binomial(k + 1, l + 1) : 0;
                    assertEquals("C_" + n + "([" + k + "], [" + l + "])", expected, c.hom(k, l).size());
                }
            }
        }
    }

    /** The semisimplicial identities d^{k-1}_i d^k_j = d^{k-1}_{j-1} d^k_i hold in C_n. */
    public void testSemisimplicialIdentitiesInTheCategory() {
        SemisimplicialCategory s = SemisimplicialCategory.truncated(4);
        FiniteCategory c = s.category();
        for (int k = 2; k <= 4; k++) {
            for (int j = 0; j <= k; j++) {
                for (int i = 0; i < j; i++) {
                    assertEquals(c.compose(s.face(k - 1, j - 1), s.face(k, i)),
                                 c.compose(s.face(k - 1, i), s.face(k, j)));
                }
            }
        }
        // d^k_i keeps every vertex but i
        assertTrue(Arrays.equals(new int[] { 0, 2 }, s.kept(s.face(2, 1))));
        assertTrue(Arrays.equals(new int[] { 0 }, s.kept(c.compose(s.face(1, 1), s.face(2, 2)))));
        assertEquals("d^1_1 d^2_2", c.arrowName(c.compose(s.face(1, 1), s.face(2, 2))));
        assertEquals("d^2_0", c.arrowName(s.face(2, 0)));
    }

    /** Prop. 4.4: n(n+3)/2 generators and the sum over k of C(k+1, 2) relations. */
    public void testProp44Counts() {
        for (int n = 1; n <= 7; n++) {
            SemisimplicialCategory s = SemisimplicialCategory.truncated(n);
            int rels = 0;
            for (int k = 2; k <= n; k++) {
                rels += binomial(k + 1, 2);
            }
            assertEquals(n * (n + 3) / 2, s.generators().size());
            assertEquals(rels, s.relations().size());
            assertEquals(n * (n + 3) / 2, SemisimplicialCategory.generatorCount(n));
            assertEquals(rels, SemisimplicialCategory.relationCount(n));
        }
        int[] gens = { 2, 5, 9, 14, 20, 27, 35 };
        int[] rels = { 0, 3, 9, 19, 34, 55, 83 };
        for (int n = 1; n <= 7; n++) {
            assertEquals(gens[n - 1], SemisimplicialCategory.generatorCount(n));
            assertEquals(rels[n - 1], SemisimplicialCategory.relationCount(n));
        }
    }

    /** The M_2 presentation exactly as printed on p. 19. */
    public void testM2RelationsAsPrinted() {
        SemisimplicialCategory s = SemisimplicialCategory.truncated(2);
        assertEquals(Arrays.asList("d^1_0", "d^1_1", "d^2_0", "d^2_1", "d^2_2"), s.generators());
        List<String> rels = s.relationStrings();
        assertEquals(Arrays.asList("d^1_0 d^2_1 = d^1_0 d^2_0",
                                   "d^1_0 d^2_2 = d^1_1 d^2_0",
                                   "d^1_1 d^2_2 = d^1_1 d^2_1"), rels);
        assertEquals("M_2 = < d^1_0, d^1_1, d^2_0, d^2_1, d^2_2 | d^1_0 d^2_1 = d^1_0 d^2_0, "
                     + "d^1_0 d^2_2 = d^1_1 d^2_0, d^1_1 d^2_2 = d^1_1 d^2_1 >", s.presentation());
        assertEquals("M_1 = < d^1_0, d^1_1 >", SemisimplicialCategory.truncated(1).presentation());
    }

    /** Each relation of Prop. 4.4 holds in Mon(C_n): both sides have one normal form. */
    public void testRelationsHoldInTheUniversalMonoid() {
        for (int n = 2; n <= 5; n++) {
            SemisimplicialCategory s = SemisimplicialCategory.truncated(n);
            UniversalMonoid m = s.monoid();
            for (SemisimplicialCategory.Relation r : s.relations()) {
                int[] left = s.element(r.left);
                int[] right = s.element(r.right);
                assertTrue(r.toString(), Arrays.equals(left, right));
                assertEquals(1, left.length);
            }
        }
    }

    /**
     * Normal forms in M_2 (Lemma 2.3 with p. 19): the letters are the eight
     * non-identity arrows of C_2, and the word d^1_0 d^2_1 reduces to the one
     * letter keeping vertex 2, named by its canonical factorization. The counts of reduced words by length are checked
     * again in spec/agda/MasterMonoid.agda.
     */
    public void testM2NormalForms() {
        SemisimplicialCategory s = SemisimplicialCategory.truncated(2);
        UniversalMonoid m = s.monoid();
        assertEquals(8, m.letters().size());
        int[] w = s.element(new int[][] { { 1, 0 }, { 2, 1 } });
        assertEquals(1, w.length);
        assertTrue(Arrays.equals(new int[] { 2 }, s.kept(w[0])));
        assertEquals("(d^1_0 d^2_1)", m.format(w));
        // d^2_0 d^1_0 is reduced: d^2_0 leaves [2] and d^1_0 lands in [0]
        int[] r = s.element(new int[][] { { 2, 0 }, { 1, 0 } });
        assertEquals(2, r.length);
        assertEquals("d^2_0 d^1_0", m.format(r));
        int[] byLength = new int[4];
        for (int[] u : m.reducedWords(3)) {
            byLength[u.length]++;
        }
        assertEquals(1, byLength[0]);
        assertEquals(8, byLength[1]);
        assertEquals(58, byLength[2]);
        assertEquals(416, byLength[3]);
    }

    public void testElementRejectsUnknownGenerators() {
        SemisimplicialCategory s = SemisimplicialCategory.truncated(2);
        try {
            s.element(new int[][] { { 3, 0 } });
            fail("d^3_0 is not a generator of M_2");
        } catch (IllegalArgumentException expected) {
            // refused
        }
    }
}
