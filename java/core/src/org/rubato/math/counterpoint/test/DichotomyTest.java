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

package org.rubato.math.counterpoint.test;

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.rubato.math.counterpoint.Dichotomy;
import org.rubato.math.counterpoint.DualAffine;
import org.rubato.math.counterpoint.DualNumbers;
import org.rubato.math.counterpoint.FirstSpecies;
import org.rubato.math.counterpoint.FirstSpecies.Kind;
import org.rubato.math.counterpoint.FirstSpecies.Step;
import org.rubato.math.counterpoint.StrongDichotomies;
import org.rubato.math.counterpoint.ZnAffine;
import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.morphism.MappingException;
import org.rubato.math.module.morphism.ModuleMorphism;

/**
 * Dichotomies, polarities and the dual numbers, checked against CCW
 * section 2.1-2.2 (pp. 5-14) and Topos I section 30.1 (pp. 517-519).
 */
public class DichotomyTest extends TestCase {

    /** The six representatives are strong; their polarities are involutions mapping X onto Y. */
    public void testSixStrongClasses() {
        assertEquals(6, StrongDichotomies.all().size());
        for (Map.Entry<Integer, Dichotomy> e : StrongDichotomies.all().entrySet()) {
            Dichotomy d = e.getValue();
            assertTrue("class " + e.getKey(), d.isStrong());
            ZnAffine p = d.polarity();
            ZnAffine pp = p.compose(p);
            assertEquals("class " + e.getKey() + " polarity is an involution", new ZnAffine(0, 1, 12), pp);
            for (int x : d.getConsonances()) {
                assertFalse(d.isConsonant(p.apply(x)));
            }
        }
    }

    /** CCW Example 2.2, p. 8: the polarity of (K/D) is T^2.5. */
    public void testFuxPolarity() {
        assertEquals(new ZnAffine(2, 5, 12), StrongDichotomies.FUX.polarity());
        assertEquals("{0,3,4,7,8,9}/{1,2,5,6,10,11}", StrongDichotomies.FUX.toString());
    }

    /** CCW Example 2.2, p. 8: X2 autocomplementary via T^9.7 but fixed by T^8.5; X1 not autocomplementary. */
    public void testExample22NonStrong() {
        Dichotomy x2 = new Dichotomy(12, 0, 1, 3, 7, 8, 11);
        assertTrue(x2.isAutocomplementary());
        assertTrue(x2.complementingSymmetries().contains(new ZnAffine(9, 7, 12)));
        assertFalse(x2.isRigid());
        assertTrue(x2.stabiliser().contains(new ZnAffine(8, 5, 12)));
        Dichotomy x1 = new Dichotomy(12, 0, 1, 3, 6, 8, 11);
        assertFalse(x1.isAutocomplementary());
        assertFalse(x1.isStrong());
    }

    /** CCW p. 9: Z_4 has no strong dichotomy, Z_2 has two. */
    public void testSmallModuli() {
        assertEquals(0, countStrong(4));
        assertEquals(2, countStrong(2));
    }

    /** DualAffine.apply and its Rubato morphism agree on all of Z_12^2. */
    public void testDualAffineMatchesMorphism() throws MappingException {
        DualAffine g = new DualAffine(3, 8, 5, 4, 12);
        ModuleMorphism m = g.toMorphism();
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                ModuleElement img = m.map(DualNumbers.interval(x, y, 12));
                assertEquals(g.applyCantus(x, y), DualNumbers.cantus(img));
                assertEquals(g.applyInterval(x, y), DualNumbers.interval(img));
            }
        }
        assertEquals("T^{3+\u03b5.8}\u00b7(5+\u03b5.4)", g.toString());
        assertEquals("T^{\u03b5.8}\u00b7(5+\u03b5.4)", DualAffine.ofH(8, 5, 4, 12).toString());
    }

    /** g(z) = (u + e.v) z + (a + e.b) as a product of dual numbers (CCW p. 10). */
    public void testDualAffineIsDualMultiplication() {
        DualAffine g = new DualAffine(7, 2, 11, 3, 12);
        ModuleElement unit = DualNumbers.interval(11, 3, 12);
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                ModuleElement p = DualNumbers.product(unit, DualNumbers.interval(x, y, 12));
                assertEquals((DualNumbers.cantus(p) + 7) % 12, g.applyCantus(x, y));
                assertEquals((DualNumbers.interval(p) + 2) % 12, g.applyInterval(x, y));
            }
        }
        assertTrue(DualNumbers.isUnit(unit));
        assertFalse(DualNumbers.isUnit(DualNumbers.interval(2, 1, 12)));
    }

    public void testComposeMatchesSequentialApplication() {
        DualAffine g = new DualAffine(3, 8, 5, 4, 12);
        DualAffine h = new DualAffine(1, 9, 7, 10, 12);
        DualAffine gh = g.compose(h);
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                int hx = h.applyCantus(x, y), hy = h.applyInterval(x, y);
                assertEquals(g.applyCantus(hx, hy), gh.applyCantus(x, y));
                assertEquals(g.applyInterval(hx, hy), gh.applyInterval(x, y));
            }
        }
    }

    /** A parallel fifth on a step of 2 is forbidden under (K/D) (Table 3.1, class 82, k = 7). */
    public void testFirstSpeciesParallelFifths() {
        List<Step> s = FirstSpecies.check(StrongDichotomies.FUX, FirstSpecies.Orientation.SWEEPING,
                new int[] { 60, 62 }, new int[] { 67, 69 });
        assertEquals(Kind.ADMITTED, s.get(0).kind);
        assertEquals(Kind.FORBIDDEN, s.get(1).kind);
        assertFalse(FirstSpecies.isCorrect(s));
    }

    public void testFirstSpeciesDissonanceAndHanging() {
        List<Step> s = FirstSpecies.check(StrongDichotomies.FUX, FirstSpecies.Orientation.SWEEPING,
                new int[] { 60, 62, 64 }, new int[] { 65, 69, 72 });
        assertEquals(Kind.DISSONANT, s.get(0).kind);   // fourth
        assertEquals(Kind.UNCHECKED, s.get(1).kind);
        assertEquals(Kind.ADMITTED, s.get(2).kind);    // 9 -> 8 on a step of 2
        // the same pitches read hanging: the discantus below gives cantus - discantus
        List<Step> h = FirstSpecies.check(StrongDichotomies.FUX, FirstSpecies.Orientation.HANGING,
                new int[] { 67 }, new int[] { 60 });
        assertEquals(7, h.get(0).y);
    }

    public void testFirstSpeciesLengthMismatch() {
        try {
            FirstSpecies.check(StrongDichotomies.FUX, FirstSpecies.Orientation.SWEEPING, new int[] { 60 }, new int[] { 64, 67 });
            fail();
        }
        catch (IllegalArgumentException expected) { /* expected */ }
    }

    private static int countStrong(int n) {
        int count = 0;
        int k = n / 2;
        for (int mask = 0; mask < (1 << n); mask++) {
            if (Integer.bitCount(mask) != k) {
                continue;
            }
            int[] c = new int[k];
            int j = 0;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    c[j++] = i;
                }
            }
            if (new Dichotomy(n, c).isStrong()) {
                count++;
            }
        }
        return count;
    }
}
