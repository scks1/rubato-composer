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

import org.rubato.math.module.morphism.ZnAffineMorphism;
import org.rubato.math.topos.FiniteCategory;

/**
 * Finite address categories: the full subcategory of Mod on cyclic
 * Z-modules with affine maps, the groupoid of affine bijections and the
 * digraph site (sources/FINITE-MODAT-SPEC.md, sections 3 and 6).
 */
public class FiniteCategoryTest extends TestCase {

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public void testAffineEndomorphismsOfZ2() {
        FiniteCategory c = FiniteCategory.affineCyclic(2);
        assertEquals(1, c.objectCount());
        assertEquals("Z2", c.objectName(0));
        assertEquals(4, c.arrowCount());
        // the constants 0 and 1, the identity and the translation x+1
        assertNotNull(c.find(0, 0, new int[] { 0, 0 }));
        assertNotNull(c.find(0, 0, new int[] { 1, 1 }));
        assertNotNull(c.find(0, 0, new int[] { 0, 1 }));
        assertNotNull(c.find(0, 0, new int[] { 1, 0 }));
        assertEquals(c.identity(0), c.find(0, 0, new int[] { 0, 1 }).intValue());
        assertFalse(c.isGroupoid());
        assertEquals(-1, c.terminalObject());
    }

    public void testHomSizesAreNTimesGcd() {
        int[] moduli = { 1, 2, 3, 4, 6 };
        FiniteCategory c = FiniteCategory.affineCyclic(moduli);
        for (int i = 0; i < moduli.length; i++) {
            for (int j = 0; j < moduli.length; j++) {
                int expected = moduli[j] * gcd(moduli[i], moduli[j]);
                assertEquals("Aff(Z" + moduli[i] + ", Z" + moduli[j] + ")",
                             expected, c.hom(i, j).size());
            }
        }
    }

    public void testNamesShowTheAffineForm() {
        FiniteCategory c = FiniteCategory.affineCyclic(2);
        int t = c.find(0, 0, new int[] { 1, 0 });
        assertEquals("x+1 : Z2 -> Z2", c.arrowName(t));
        int k = c.find(0, 0, new int[] { 1, 1 });
        assertEquals("1 : Z2 -> Z2", c.arrowName(k));
        FiniteCategory d = FiniteCategory.affineCyclic(1, 3);
        assertEquals("Z1", d.objectName(0));
        int two = d.find(0, 1, new int[] { 2 });
        assertEquals("2 : Z1 -> Z3", d.arrowName(two));
        int dbl = FiniteCategory.affineCyclic(3).find(0, 0, new int[] { 1, 0, 2 });
        assertEquals("2x+1 : Z3 -> Z3", FiniteCategory.affineCyclic(3).arrowName(dbl));
    }

    public void testCompositionAgreesWithZnAffineMorphism() throws Exception {
        for (int n = 2; n <= 6; n++) {
            FiniteCategory c = FiniteCategory.affineCyclic(n);
            assertEquals(n * n, c.arrowCount());
            for (int a1 = 0; a1 < n; a1++) {
                for (int b1 = 0; b1 < n; b1++) {
                    for (int a2 = 0; a2 < n; a2++) {
                        for (int b2 = 0; b2 < n; b2++) {
                            ZnAffineMorphism f = new ZnAffineMorphism(a1, b1, n);
                            ZnAffineMorphism g = new ZnAffineMorphism(a2, b2, n);
                            ZnAffineMorphism gf = (ZnAffineMorphism) g.compose(f);
                            int fi = c.find(0, 0, table(f, n));
                            int gi = c.find(0, 0, table(g, n));
                            int expected = c.find(0, 0, table(gf, n));
                            assertEquals(expected, c.compose(gi, fi));
                        }
                    }
                }
            }
        }
    }

    private static int[] table(ZnAffineMorphism f, int n) {
        int[] t = new int[n];
        for (int x = 0; x < n; x++) {
            t[x] = f.mapValue(x);
        }
        return t;
    }

    public void testZeroModuleIsTerminal() {
        FiniteCategory c = FiniteCategory.affineCyclic(1, 2, 3);
        assertEquals(0, c.terminalObject());
        for (int x = 0; x < 3; x++) {
            assertEquals(1, c.hom(x, 0).size());
        }
        // maps out of the zero module are the constants
        assertEquals(2, c.hom(0, 1).size());
        assertEquals(3, c.hom(0, 2).size());
        // Z3 -> Z2 has only the constants
        assertEquals(2, c.hom(2, 1).size());
    }

    public void testAutomorphismsFormAGroupoid() {
        FiniteCategory g2 = FiniteCategory.affineAutomorphisms(2);
        assertEquals(2, g2.arrowCount());
        assertTrue(g2.isGroupoid());
        FiniteCategory g3 = FiniteCategory.affineAutomorphisms(3);
        assertEquals(6, g3.arrowCount());
        assertTrue(g3.isGroupoid());
        FiniteCategory g4 = FiniteCategory.affineAutomorphisms(4);
        assertEquals(8, g4.arrowCount());
        assertTrue(g4.isGroupoid());
        assertFalse(FiniteCategory.affineCyclic(1, 2).isGroupoid());
    }

    public void testDigraphSite() {
        FiniteCategory s = FiniteCategory.digraphSite();
        assertEquals(2, s.objectCount());
        assertEquals("V", s.objectName(FiniteCategory.V));
        assertEquals("A", s.objectName(FiniteCategory.A));
        assertEquals(4, s.arrowCount());
        assertEquals(2, s.hom(FiniteCategory.V, FiniteCategory.A).size());
        assertEquals(0, s.hom(FiniteCategory.A, FiniteCategory.V).size());
        assertEquals(-1, s.terminalObject());
        assertFalse(s.isGroupoid());
    }

    public void testArrowsIntoAnObject() {
        FiniteCategory c = FiniteCategory.affineCyclic(1, 2, 3);
        List<Integer> into = c.arrowsInto(1);
        assertEquals(2 + 4 + 2, into.size());
        for (int f : into) {
            assertEquals(1, c.cod(f));
        }
    }

    public void testRefusesTablesThatBreakTheLaws() {
        String[] obj = { "X" };
        int[] dom = { 0, 0 };
        int[] cod = { 0, 0 };
        String[] names = { "id", "e" };
        // e∘e = id is a group of order 2: accepted
        FiniteCategory ok = FiniteCategory.of(obj, dom, cod, names, new int[] { 0 },
                                              new int[][] { { 0, 1 }, { 1, 0 } });
        assertTrue(ok.isGroupoid());
        // the identity is not neutral
        try {
            FiniteCategory.of(obj, dom, cod, names, new int[] { 0 },
                              new int[][] { { 0, 0 }, { 1, 0 } });
            fail("accepted a non-neutral identity");
        } catch (IllegalArgumentException expected) { /* refused */ }
        // the constant table is associative, so only the neutrality check refuses it
        try {
            FiniteCategory.of(obj, dom, cod, names, new int[] { 0 },
                              new int[][] { { 0, 0 }, { 0, 0 } });
            fail("accepted an associative table whose identity is not neutral");
        } catch (IllegalArgumentException expected) { /* refused */ }
        // not associative: three arrows id, a, b with a∘b = a, b∘a = b, a∘a = b
        String[] n3 = { "id", "a", "b" };
        int[] d3 = { 0, 0, 0 };
        try {
            FiniteCategory.of(obj, d3, d3, n3, new int[] { 0 },
                              new int[][] { { 0, 1, 2 }, { 1, 2, 1 }, { 2, 2, 2 } });
            fail("accepted a non-associative table");
        } catch (IllegalArgumentException expected) { /* refused */ }
        // a composite with the wrong codomain
        try {
            FiniteCategory.of(new String[] { "X", "Y" }, new int[] { 0, 1, 0 },
                              new int[] { 0, 1, 1 }, new String[] { "1X", "1Y", "f" },
                              new int[] { 0, 1 },
                              new int[][] { { 0, -1, -1 }, { -1, 1, 2 }, { 0, -1, -1 } });
            fail("accepted a composite with the wrong ends");
        } catch (IllegalArgumentException expected) { /* refused */ }
    }

    public void testChainCategory() {
        FiniteCategory chain = FiniteCategory.of(new String[] { "0", "1" },
                                                 new int[] { 0, 1, 0 },
                                                 new int[] { 0, 1, 1 },
                                                 new String[] { "1_0", "1_1", "u" },
                                                 new int[] { 0, 1 },
                                                 new int[][] { { 0, -1, -1 }, { -1, 1, 2 }, { 2, -1, -1 } });
        assertEquals(1, chain.terminalObject());
        assertEquals(Arrays.asList(0, 2), chain.arrowsFrom(0));
    }
}
