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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.counterpoint.Dichotomy;
import org.rubato.math.counterpoint.DualAffine;
import org.rubato.math.counterpoint.StrongDichotomies;
import org.rubato.math.counterpoint.SymmetryTrace;
import org.rubato.math.counterpoint.SymmetryTrace.Step;

/**
 * The per-step contrapuntal symmetries of Agust&iacute;n-Aquino and
 * Curiel-L&oacute;pez, "An Algebraic-Symmetric Analysis of Counterpoint and
 * Modulation in the Music of Claudio Monteverdi", arXiv 2512.18862, &sect;5.1,
 * pp. 8&ndash;12. Each table is checked row by row: the set of symmetries and
 * its cardinality. The interval sequences are the ones printed in the paper.
 */
public class SymmetryTraceTest extends TestCase {

    // the symmetries named in the tables, e^{eps.t} o (u + eps.v)
    static final String G7 = "e^{ε.0}∘7";
    static final String G76 = "e^{ε.6}∘(7+ε.6)";
    static final String G16 = "e^{ε.6}∘(1+ε.6)";
    static final String G54 = "e^{ε.8}∘(5+ε.4)";
    static final String G58 = "e^{ε.8}∘(5+ε.8)";
    static final String G118 = "e^{ε.11}∘(11+ε.8)";
    static final String G114 = "e^{ε.11}∘(11+ε.4)";
    static final String G11 = "e^{ε.11}∘11";
    static final String G378 = "e^{ε.3}∘(7+ε.8)";
    static final String G37 = "e^{ε.3}∘7";

    static final String CONFITEBOR = "0+ε.7, 0+ε.4, 10+ε.7, 10+ε.4, 9+ε.7, 9+ε.3, 7+ε.7, 7+ε.4, 5+ε.7";
    static final String MA_TU = "5+ε.7, 5+ε.4, 3+ε.7, 3+ε.4, 2+ε.7";
    static final String IO_MI_SON = "7+ε.7, 7+ε.3, 5+ε.7, 5+ε.4, 3+ε.7, 3+ε.4, 2+ε.7, 2+ε.3, 0+ε.7";
    static final String LAUDATE = "2+ε.0, 2+ε.9, 0+ε.0, 0+ε.9, 11+ε.0, 11+ε.8, 9+ε.0, 9+ε.8, 7+ε.0, 7+ε.9";
    static final String GLORIA_86 = "2+ε.0, 2+ε.9, 0+ε.0, 0+ε.9, 11+ε.0, 11+ε.8, 9+ε.0, 9+ε.8, 7+ε.0";
    static final String GLORIA_108 = "11+ε.3, 11+ε.0, 9+ε.3, 9+ε.0, 7+ε.4";

    static String[] set(String... s) { return s; }

    static void assertTable(String intervals, String[][] expected) {
        List<Step> steps = SymmetryTrace.trace(StrongDichotomies.FUX, SymmetryTrace.parse(intervals));
        assertEquals(intervals, expected.length, steps.size());
        for (int i = 0; i < expected.length; i++) {
            List<String> got = new ArrayList<String>();
            for (DualAffine g : steps.get(i).getSymmetries()) {
                got.add(SymmetryTrace.name(g));
            }
            assertEquals("row " + (i + 1) + " of " + intervals + ": " + got, expected[i].length, steps.get(i).cardinality());
            for (String g : expected[i]) {
                assertTrue("row " + (i + 1) + " lacks " + g + ", has " + got, got.contains(g));
            }
        }
    }

    /** Confitebor primo mm. 64-67, p. 8. */
    public void testConfitebor() {
        assertTable(CONFITEBOR, new String[][] {
            set(G7), set(G76), set(G7), set(G16), set(G7), set(G54), set(G7), set(G76) });
    }

    /** Ma tu, piu che mai dura mm. 6-8, p. 9. */
    public void testMaTu() {
        assertTable(MA_TU, new String[][] { set(G7), set(G76), set(G7), set(G16) });
    }

    /** Io mi son giovinetta mm. 52-53, p. 9. */
    public void testIoMiSon() {
        assertTable(IO_MI_SON, new String[][] {
            set(G7), set(G54), set(G7), set(G76), set(G7), set(G16), set(G7), set(G54) });
    }

    /** Laudate Dominum mm. 98-103, p. 10. */
    public void testLaudate() {
        assertTable(LAUDATE, new String[][] {
            set(G16, G76), set(G58, G54), set(G16, G76), set(G58, G54), set(G118, G114, G11),
            set(G378, G37), set(G118, G114, G11), set(G378, G37), set(G16, G76) });
    }

    /** Gloria a 7 mm. 1-5, the 8-6 succession, p. 11. */
    public void testGloria86() {
        assertTable(GLORIA_86, new String[][] {
            set(G16, G76), set(G58, G54), set(G16, G76), set(G58, G54), set(G118, G114, G11),
            set(G37, G378), set(G118, G114, G11), set(G37, G378) });
    }

    /** Gloria a 7, the 10-8 succession, p. 11: one five-element set. */
    public void testGloria108() {
        assertTable(GLORIA_108, new String[][] {
            set(G58, G54), set(G16, G76, G118, G114, G11), set(G58, G54), set(G114, G11) });
    }

    /** p. 12: one symmetry per 5-3 step, at least two per 8-6 and 10-8 step. */
    public void testParsimonyClaims() {
        for (String s : new String[] { CONFITEBOR, MA_TU, IO_MI_SON }) {
            for (Step st : SymmetryTrace.trace(StrongDichotomies.FUX, SymmetryTrace.parse(s))) {
                assertEquals(1, st.cardinality());
            }
        }
        for (String s : new String[] { LAUDATE, GLORIA_86, GLORIA_108 }) {
            for (Step st : SymmetryTrace.trace(StrongDichotomies.FUX, SymmetryTrace.parse(s))) {
                assertTrue(st.cardinality() >= 2);
            }
        }
    }

    /** p. 12: in the 5-3 tables only e^{eps.6} o (1+eps.6) has linear part 1 (fixes the cantus firmus). */
    public void testCantusFixingSymmetry() {
        int found = 0;
        for (String s : new String[] { CONFITEBOR, MA_TU, IO_MI_SON }) {
            for (Step st : SymmetryTrace.trace(StrongDichotomies.FUX, SymmetryTrace.parse(s))) {
                for (DualAffine g : st.getSymmetries()) {
                    if (SymmetryTrace.fixesCantus(g)) {
                        assertEquals(G16, SymmetryTrace.name(g));
                        found++;
                    }
                }
            }
        }
        assertEquals(3, found);
    }

    /**
     * Finding W1: "perfect to imperfect" steps in Laudate have three
     * symmetries only for octave to minor sixth; octave to major sixth has two.
     */
    public void testLaudatePerfectToImperfect() {
        List<Step> steps = SymmetryTrace.trace(StrongDichotomies.FUX, SymmetryTrace.parse(LAUDATE));
        int[] rows = { 0, 2, 4, 6, 8 };
        int[] cards = { 2, 2, 3, 3, 2 };
        for (int i = 0; i < rows.length; i++) {
            Step st = steps.get(rows[i]);
            assertEquals(0, st.getFrom()[1]);
            assertEquals(cards[i], st.cardinality());
        }
    }

    /** Each listed symmetry admits the step: the translated successor lies in gK[eps] and K[eps]. */
    public void testSymmetriesAdmitTheStep() {
        for (Step st : SymmetryTrace.trace(StrongDichotomies.FUX, SymmetryTrace.parse(GLORIA_108))) {
            int dx = Math.floorMod(st.getTo()[0] - st.getFrom()[0], 12);
            for (DualAffine g : st.getSymmetries()) {
                assertTrue(SymmetryTrace.inImageOfConsonances(g, StrongDichotomies.FUX, dx, st.getTo()[1]));
                assertFalse(SymmetryTrace.inImageOfConsonances(g, StrongDichotomies.FUX, 0, st.getFrom()[1]));
            }
        }
    }

    public void testDissonantAndParse() {
        List<Step> steps = SymmetryTrace.trace(StrongDichotomies.FUX, SymmetryTrace.parse("0+e.7; 2 + e.2 ; 4+e.4"));
        assertEquals(2, steps.size());
        assertEquals(0, steps.get(0).cardinality());
        assertFalse(steps.get(0).isConsonantPair());
        assertEquals(0, steps.get(1).cardinality());
        int[][] p = SymmetryTrace.parse("e.7, 3+ε.4");
        assertEquals(0, p[0][0]);
        assertEquals(7, p[0][1]);
        assertEquals(3, p[1][0]);
        try {
            SymmetryTrace.parse("7, x");
            fail();
        }
        catch (IllegalArgumentException expected) { /* expected */ }
        String r = SymmetryTrace.report(steps);
        assertTrue(r, r.contains("dissonant"));
    }

    /** The preimage test agrees with the forward map for maps outside H (a != 0, u not self-inverse in general). */
    public void testImageOfConsonancesOutsideH() {
        Dichotomy d = StrongDichotomies.FUX;
        int[][] maps = { { 3, 0, 5, 1 }, { 7, 2, 7, 5 }, { 11, 9, 11, 3 }, { 5, 4, 1, 8 } };
        for (int[] m : maps) {
            DualAffine g = new DualAffine(m[0], m[1], m[2], m[3], 12);
            boolean[][] image = new boolean[12][12];
            for (int x = 0; x < 12; x++) {
                for (int y = 0; y < 12; y++) {
                    if (d.isConsonant(y)) {
                        image[(m[2] * x + m[0]) % 12][(m[3] * x + m[2] * y + m[1]) % 12] = true;
                    }
                }
            }
            for (int x = 0; x < 12; x++) {
                for (int y = 0; y < 12; y++) {
                    assertEquals(java.util.Arrays.toString(m) + " at " + x + "," + y, image[x][y],
                            SymmetryTrace.inImageOfConsonances(g, d, x, y));
                }
            }
        }
    }
}
