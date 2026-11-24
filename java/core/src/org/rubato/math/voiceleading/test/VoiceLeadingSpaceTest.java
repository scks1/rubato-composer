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

package org.rubato.math.voiceleading.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.voiceleading.FiniteAction;
import org.rubato.math.voiceleading.VoiceLeadingSpace;

/**
 * The voice-leading space of a finite group action (Flieder, arXiv 2512.04090,
 * Def. 4.1, Ex. 4.1 and 4.3, Def. 4.2, Prop. 4.1; sources/VOICE-LEADING-SPEC.md).
 * The expected counts come from the separate Python probe (spec section 4).
 */
public class VoiceLeadingSpaceTest extends TestCase {

    static VoiceLeadingSpace pc(String group, int n) {
        return new VoiceLeadingSpace(FiniteAction.pitchClasses(group, n));
    }

    static VoiceLeadingSpace triads(String group) {
        return new VoiceLeadingSpace(FiniteAction.triads(group));
    }

    /** Ex. 4.3: T/I on Z12 has 24 elements and two transporters over every pair. */
    public void testTIOnZ12HasTwoTransportersEverywhere() {
        VoiceLeadingSpace s = pc("TI", 12);
        assertEquals(12, s.action().pointCount());
        assertEquals(24, s.action().groupSize());
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                assertEquals(x + "->" + y, 2, s.transporters(x, y).size());
            }
        }
        assertEquals(288, s.arrowCount());
        assertEquals(2, s.minMultiplicity());
        assertEquals(2, s.maxMultiplicity());
    }

    /** The transporters from 0 to 4 are T4 and I4 (x -> -x + 4). */
    public void testTransportersByName() {
        VoiceLeadingSpace s = pc("TI", 12);
        Set<String> names = new HashSet<String>();
        for (int g : s.transporters(0, 4)) {
            names.add(s.action().elementName(g));
            assertEquals(4, s.action().act(g, 0));
        }
        assertEquals(new HashSet<String>(Arrays.asList("T4", "I4")), names);
        // from 3 to 4: T1 and I7
        names.clear();
        for (int g : s.transporters(3, 4)) {
            names.add(s.action().elementName(g));
        }
        assertEquals(new HashSet<String>(Arrays.asList("T1", "I7")), names);
    }

    /** Def. 4.1: every arrow (x, g) has source x and target gx, and each appears once. */
    public void testArrowsHaveSourceAndTarget() {
        VoiceLeadingSpace s = pc("T", 7);
        List<int[]> arrows = s.arrows();
        assertEquals(49, arrows.size());
        Set<String> seen = new HashSet<String>();
        for (int[] a : arrows) {
            assertEquals(a[2], s.action().act(a[1], a[0]));
            assertTrue(seen.add(a[0] + ":" + a[1]));
        }
    }

    /** The abstract group: I_j after T_k is I_(j-k); inverses and identity. */
    public void testGroupProduct() {
        FiniteAction a = FiniteAction.pitchClasses("TI", 12);
        int t3 = a.indexOf("T3"), i5 = a.indexOf("I5"), i2 = a.indexOf("I2");
        assertEquals(i2, a.multiply(i5, t3));
        assertEquals("T0", a.elementName(a.identity()));
        for (int g = 0; g < a.groupSize(); g++) {
            assertEquals(a.identity(), a.multiply(g, a.inverse(g)));
            assertEquals(a.identity(), a.multiply(a.inverse(g), g));
            for (int x = 0; x < 12; x++) {
                for (int h = 0; h < a.groupSize(); h++) {
                    assertEquals(a.act(g, a.act(h, x)), a.act(a.multiply(g, h), x));
                }
            }
        }
        FiniteAction aff = FiniteAction.pitchClasses("Aff", 12);
        assertEquals(48, aff.groupSize());
        assertEquals(11, aff.act(aff.indexOf("5x+3"), 4));
    }

    /** D6: the normalizer sizes of the probe. */
    public void testNormalizerSizes() {
        assertEquals(48, pc("T", 12).normalizer().size());
        assertEquals(48, pc("TI", 12).normalizer().size());
        assertEquals(48, pc("Aff", 12).normalizer().size());
        assertEquals(1152, triads("TI").normalizer().size());
        assertEquals(1152, triads("T").normalizer().size());
    }

    /** Every normalizer element of T/I on Z12 is x -> ax + b with a a unit, and all 48 occur. */
    public void testNormalizerOfTIIsAffine() {
        VoiceLeadingSpace s = pc("TI", 12);
        Set<String> maps = new HashSet<String>();
        for (int[] h : s.normalizer()) {
            int b = h[0], a = (h[1] - h[0] + 12) % 12;
            for (int x = 0; x < 12; x++) {
                assertEquals((a * x + b) % 12, h[x]);
            }
            assertTrue(a == 1 || a == 5 || a == 7 || a == 11);
            maps.add(a + "," + b);
        }
        assertEquals(48, maps.size());
        assertEquals("5x+3", s.describeVertexMap(affine(5, 3)));
    }

    static int[] affine(int a, int b) {
        int[] h = new int[12];
        for (int x = 0; x < 12; x++) {
            h[x] = (a * x + b) % 12;
        }
        return h;
    }

    /** D6 against a brute force over all pairs (h_Pitch, h_Arrow), on small spaces. */
    public void testSigmaCountMatchesBruteForce() {
        String[] groups = { "T", "TI", "Aff" };
        for (String g : groups) {
            for (int n = 2; n <= 4; n++) {
                VoiceLeadingSpace s = pc(g, n);
                assertEquals(g + " Z" + n, BigInteger.valueOf(bruteForce(s)), s.sigmaAutomorphismCount());
            }
        }
        assertEquals(BigInteger.valueOf(8), pc("TI", 2).sigmaAutomorphismCount());
        assertEquals(BigInteger.valueOf(48), pc("TI", 12).sigmaAutomorphismCount());
        assertEquals(BigInteger.valueOf(1152), triads("TI").sigmaAutomorphismCount());
    }

    /** D3: T/I on Z2 is not faithful; T_k and I_k act alike. */
    public void testKernelOnZ2() {
        VoiceLeadingSpace s = pc("TI", 2);
        assertEquals(4, s.action().groupSize());
        assertEquals(2, s.kernelSize());
        assertEquals(2, s.imageSize());
        assertEquals(1, pc("TI", 12).kernelSize());
    }

    /** Ex. 4.3: the maps induced by conjugation, counted as distinct quiver maps. */
    public void testConjugationInducedCounts() {
        assertEquals(24, pc("TI", 12).conjugationInducedCount());
        assertEquals(12, pc("T", 12).conjugationInducedCount());
        assertEquals(48, pc("Aff", 12).conjugationInducedCount());
        assertEquals(2, pc("TI", 2).conjugationInducedCount());
        assertEquals(24, triads("TI").conjugationInducedCount());
    }

    /** Ex. 4.3: every conjugation-induced map preserves vlr, so it is a Sigma_VLS automorphism. */
    public void testConjugationsPreserveTheRule() {
        VoiceLeadingSpace s = pc("TI", 12);
        FiniteAction a = s.action();
        for (int phi = 0; phi < a.groupSize(); phi++) {
            int[] h = a.permutation(phi);
            int[] arrowMap = new int[a.groupSize()];
            for (int g = 0; g < a.groupSize(); g++) {
                arrowMap[g] = a.multiply(a.multiply(phi, g), a.inverse(phi));
            }
            assertTrue(s.isSigmaAutomorphism(h, arrowMap));
        }
        // a vertex map outside the normalizer is not one, with any arrow map
        int[] swap01 = affine(1, 0);
        swap01[0] = 1;
        swap01[1] = 0;
        assertFalse(s.isSigmaAutomorphism(swap01, identity(24)));
    }

    /** D7: bare quiver automorphisms, n!(m!)^(n^2) for uniform multiplicity, else null. */
    public void testBareQuiverCount() {
        BigInteger want = factorial(12).multiply(BigInteger.valueOf(2).pow(144));
        assertEquals(want, pc("TI", 12).bareQuiverAutomorphismCount());
        assertEquals(53, want.toString().length());
        assertEquals(factorial(12), pc("T", 12).bareQuiverAutomorphismCount());
        assertNull(triads("T").bareQuiverAutomorphismCount());
    }

    /** D2: refused presets. */
    public void testRefusals() {
        try {
            FiniteAction.triads("Aff");
            fail("Aff does not act on the triads");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("triads"));
        }
        for (int n : new int[] { 1, 25 }) {
            try {
                FiniteAction.pitchClasses("T", n);
                fail("n = " + n);
            }
            catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("2 to 24"));
            }
        }
        try {
            FiniteAction.pitchClasses("Q", 12);
            fail("unknown group");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("T, TI or Aff"));
        }
    }

    /** The triad points: C major is {0, 4, 7}, A minor {9, 0, 4}; names C and Am. */
    public void testTriadPoints() {
        FiniteAction t = FiniteAction.triads("TI");
        assertEquals(24, t.pointCount());
        assertEquals("C", t.pointName(0));
        assertEquals("Am", t.pointName(21));
        assertTrue(Arrays.equals(new int[] { 0, 4, 9 }, t.pitchClassesOf(21)));
        assertEquals(21, t.triadIndex(new int[] { 9, 0, 4 }));
        assertEquals(-1, t.triadIndex(new int[] { 0, 4, 8 }));
        VoiceLeadingSpace s = new VoiceLeadingSpace(t);
        List<Integer> cToAm = s.transporters(0, 21);
        assertEquals(1, cToAm.size());
        assertEquals("I4", t.elementName(cToAm.get(0)));
        assertEquals(0, triads("T").transporters(0, 12).size());
    }

    static int[] identity(int n) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = i;
        }
        return r;
    }

    static BigInteger factorial(int n) {
        BigInteger r = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            r = r.multiply(BigInteger.valueOf(i));
        }
        return r;
    }

    /** Counts pairs (h_Pitch, h_Arrow) with h_Arrow[vlr(x, y)] = vlr(h x, h y), by enumeration. */
    static long bruteForce(VoiceLeadingSpace s) {
        int n = s.action().pointCount(), m = s.action().groupSize();
        List<int[]> vp = permutations(n), ap = permutations(m);
        long count = 0;
        for (int[] h : vp) {
            for (int[] k : ap) {
                if (s.isSigmaAutomorphism(h, k)) {
                    count++;
                }
            }
        }
        return count;
    }

    static List<int[]> permutations(int n) {
        List<int[]> out = new ArrayList<int[]>();
        permute(identity(n), 0, out);
        return out;
    }

    private static void permute(int[] a, int i, List<int[]> out) {
        if (i == a.length) {
            out.add(a.clone());
            return;
        }
        for (int j = i; j < a.length; j++) {
            int t = a[i]; a[i] = a[j]; a[j] = t;
            permute(a, i + 1, out);
            t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }

    /** conjugation(phi) is g -> phi g phi^-1: by T1, I0 goes to I2, and the pair is a Sigma_VLS automorphism (added for mutation M10). */
    public void testConjugationMethod() {
        VoiceLeadingSpace s = pc("TI", 12);
        FiniteAction a = s.action();
        int[] k = s.conjugation(a.indexOf("T1"));
        assertEquals(a.indexOf("I2"), k[a.indexOf("I0")]);
        assertEquals(a.indexOf("T5"), k[a.indexOf("T5")]);
        for (int phi = 0; phi < a.groupSize(); phi++) {
            assertTrue(a.elementName(phi), s.isSigmaAutomorphism(a.permutation(phi), s.conjugation(phi)));
        }
    }
}
