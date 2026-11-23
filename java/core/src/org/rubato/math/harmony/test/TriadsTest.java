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

package org.rubato.math.harmony.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.counterpoint.ZnAffine;
import org.rubato.math.global.Interpretations;
import org.rubato.math.harmony.Cadences;
import org.rubato.math.harmony.Tonality;
import org.rubato.math.harmony.Triads;

/**
 * The PLR/TI duality on the 24 major and minor triads as Agust&iacute;n-Aquino
 * and Curiel-L&oacute;pez use it (arXiv 2512.18862, &sect;4.2, pp. 6&ndash;7,
 * and the commutative diagrams on pp. 13 and 16).
 */
public class TriadsTest extends TestCase {

    static int M(String name) { return Triads.parse(name); }

    public void testTriadsAndGenerators() {
        assertEquals(24, Triads.COUNT);
        for (int t = 0; t < Triads.COUNT; t++) {
            assertEquals(t, Triads.fromPitchClasses(Triads.pitchClasses(t)));
            assertEquals(t, Triads.P(Triads.P(t)));
            assertEquals(t, Triads.L(Triads.L(t)));
            assertEquals(t, Triads.R(Triads.R(t)));
            assertTrue(Triads.isMajor(t) != Triads.isMajor(Triads.P(t)));
        }
        // p. 7: F to f, C to e, C to a
        assertEquals(M("f"), Triads.P(M("F")));
        assertEquals(M("e"), Triads.L(M("C")));
        assertEquals(M("a"), Triads.R(M("C")));
        assertEquals(M("C"), Triads.R(M("a")));
        assertEquals("Bb", Triads.name(M("Bb")));
        assertEquals("c#", Triads.name(M("c#")));
        assertEquals(-1, Triads.fromPitchClasses(0x7));
    }

    /** p. 7: both groups have 24 elements and act simply transitively. */
    public void testRegularActions() {
        List<int[]> ti = Triads.tiGroup();
        List<int[]> plr = Triads.plrGroup();
        assertEquals(24, ti.size());
        assertEquals(24, plr.size());
        for (List<int[]> g : java.util.Arrays.asList(ti, plr)) {
            for (int a = 0; a < 24; a++) {
                for (int b = 0; b < 24; b++) {
                    int c = 0;
                    for (int[] p : g) {
                        if (p[a] == b) {
                            c++;
                        }
                    }
                    assertEquals(1, c);
                }
            }
        }
    }

    /** p. 7: every element of PLR commutes with every element of TI. */
    public void testDuality() {
        for (int[] p : Triads.plrGroup()) {
            for (int[] t : Triads.tiGroup()) {
                for (int x = 0; x < 24; x++) {
                    assertEquals(p[t[x]], t[p[x]]);
                }
            }
        }
        // the TI elements that commute with all of TI (T0 and T6) are exactly those also in PLR
        Set<String> plr = new HashSet<String>();
        for (int[] p : Triads.plrGroup()) {
            plr.add(java.util.Arrays.toString(p));
        }
        for (int[] p : Triads.tiGroup()) {
            boolean commutes = true;
            for (int[] t : Triads.tiGroup()) {
                for (int x = 0; x < 24 && commutes; x++) {
                    commutes = p[t[x]] == t[p[x]];
                }
            }
            assertEquals(commutes, plr.contains(java.util.Arrays.toString(p)));
        }
    }

    /** p. 7: TI and PLR are isomorphic (both dihedral of order 24): T_a I_0^e maps to (LR)^a L^e. */
    public void testIsomorphism() {
        Map<String, int[]> phi = new HashMap<String, int[]>();
        int[] lr = Triads.compose(Triads.perm("L"), Triads.perm("R"));
        int[] l = Triads.perm("L");
        for (int a = 0; a < 12; a++) {
            for (int e = 0; e < 2; e++) {
                int[] src = Triads.ti(new ZnAffine(e == 0 ? a : a, e == 0 ? 1 : -1, 12));
                int[] img = Triads.identity();
                for (int i = 0; i < a; i++) {
                    img = Triads.compose(img, lr);
                }
                if (e == 1) {
                    img = Triads.compose(img, l);
                }
                phi.put(java.util.Arrays.toString(src), img);
            }
        }
        assertEquals(24, phi.size());
        Set<String> images = new HashSet<String>();
        for (int[] v : phi.values()) {
            images.add(java.util.Arrays.toString(v));
        }
        assertEquals(24, images.size());
        for (int[] x : Triads.tiGroup()) {
            for (int[] y : Triads.tiGroup()) {
                int[] xy = Triads.compose(x, y);
                assertTrue(java.util.Arrays.equals(phi.get(java.util.Arrays.toString(xy)),
                        Triads.compose(phi.get(java.util.Arrays.toString(x)), phi.get(java.util.Arrays.toString(y)))));
            }
        }
    }

    /** Eqs. (1) and (2), p. 7, for every major key; R sends k4 = {IV,V} to k2 = {II,III}. */
    public void testEquationsOneAndTwo() {
        for (int k = 0; k < 12; k++) {
            int one = Triads.major(k);
            int vi = Triads.minor(k + 9);
            int iii = Triads.minor(k + 4);
            int ii = Triads.minor(k + 2);
            assertEquals(vi, Triads.R(one));
            assertEquals(iii, Triads.R(Triads.T(7, one)));
            assertEquals(iii, Triads.T(7, Triads.R(one)));
            assertEquals(ii, Triads.R(Triads.T(5, one)));
            assertEquals(ii, Triads.T(5, Triads.R(one)));
            Tonality t = Tonality.of(Interpretations.C_MAJOR, 0, 3).transpose(k);
            assertEquals(Triads.pitchClasses(ii), t.degree(1));
            assertEquals(Triads.pitchClasses(iii), t.degree(2));
            assertTrue(Cadences.minimal(t).contains(Cadences.parse("{II,III}")));
            assertTrue(Cadences.minimal(t).contains(Cadences.parse("{IV,V}")));
        }
    }

    /** The diagrams of Ecco pur (p. 13) and Mira, deh mira Orfeo (p. 16) commute. */
    public void testDiagrams() {
        String[][] rows = { { "Bb", "Eb", "F", "g", "c", "d" }, { "C", "F", "G", "a", "d", "e" } };
        for (String[] r : rows) {
            int mid = M(r[0]);
            assertEquals(M(r[1]), Triads.T(5, mid));
            assertEquals(M(r[2]), Triads.T(7, mid));
            assertEquals(M(r[3]), Triads.R(mid));
            assertEquals(M(r[4]), Triads.R(M(r[1])));
            assertEquals(M(r[5]), Triads.R(M(r[2])));
            assertEquals(M(r[4]), Triads.T(5, M(r[3])));
            assertEquals(M(r[5]), Triads.T(7, M(r[3])));
        }
    }

    /** Unique TI element and shortest PLR word between two triads. */
    public void testDualPair() {
        assertEquals("R", Triads.plrWord(M("C"), M("a")));
        assertEquals("I4", Triads.tiName(Triads.tiElement(M("C"), M("a"))));
        assertEquals("I0", Triads.tiName(Triads.tiElement(M("Bb"), M("g"))));
        assertEquals("T7", Triads.tiName(Triads.tiElement(M("C"), M("G"))));
        assertEquals("", Triads.plrWord(M("D"), M("D")));
        // the word, applied right to left, does reach the target
        for (int a = 0; a < 24; a++) {
            for (int b = 0; b < 24; b++) {
                assertEquals(b, Triads.perm(Triads.plrWord(a, b))[a]);
                assertEquals(b, Triads.T(0, Triads.fromPitchClasses(
                        org.rubato.math.harmony.PitchClasses.apply(Triads.tiElement(a, b), Triads.pitchClasses(a)))));
            }
        }
    }
}
