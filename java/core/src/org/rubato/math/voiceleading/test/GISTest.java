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

import junit.framework.TestCase;

import org.rubato.math.voiceleading.FiniteAction;
import org.rubato.math.voiceleading.GIS;
import org.rubato.math.voiceleading.VoiceLeadingSpace;

/**
 * Lewin's two GIS axioms as Flieder states them (arXiv 2512.04090, Example 3.1,
 * p. 23), on finite tables, and the GIS a voice-leading space yields (spec D8, D9).
 */
public class GISTest extends TestCase {

    static VoiceLeadingSpace pc(String group, int n) {
        return new VoiceLeadingSpace(FiniteAction.pitchClasses(group, n));
    }

    /** Z_n with addition: the interval table int(x, y) = y - x. */
    static int[][] addition(int n) {
        int[][] t = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                t[i][j] = (i + j) % n;
            }
        }
        return t;
    }

    static int[][] differences(int n) {
        int[][] t = new int[n][n];
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                t[x][y] = ((y - x) % n + n) % n;
            }
        }
        return t;
    }

    /** The plain pitch-class GIS passes both axioms. */
    public void testDifferencesFormAGIS() {
        GIS.Result r = new GIS(12, addition(12), differences(12)).check();
        assertTrue(r.toString(), r.isGIS());
        assertEquals("", r.failure());
    }

    /** Axiom 1 fails for int(x, y) = y - x + 1: int(0,0) + int(0,0) = 2 but int(0,0) = 1. */
    public void testCompositionWitness() {
        int[][] t = differences(12);
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                t[x][y] = (t[x][y] + 1) % 12;
            }
        }
        GIS.Result r = new GIS(12, addition(12), t).check();
        assertFalse(r.isGIS());
        assertTrue(r.failure(), r.failure().startsWith("axiom 1 (composition)"));
        assertTrue(r.failure(), r.failure().contains("r = 0, s = 0, t = 0"));
        // axiom 2 alone holds for this table: each s reaches each interval once
        assertTrue(r.failure(), !r.failure().contains("axiom 2"));
    }

    /** Axiom 2 fails when int is constant (axiom 1 holds: 0 + 0 = 0). */
    public void testUniquenessWitness() {
        int[][] t = new int[3][3];
        GIS.Result r = new GIS(3, addition(3), t).check();
        assertFalse(r.isGIS());
        assertTrue(r.failure(), r.failure().startsWith("axiom 2 (unique target)"));
        assertTrue(r.failure(), r.failure().contains("s = 0, i = 0: 3 targets"));
    }

    /** A product that is not associative is refused before the axioms. */
    public void testGroupAxiomsCheckedFirst() {
        int[][] star = { { 0, 1, 2 }, { 1, 0, 0 }, { 2, 0, 1 } }; // 1*(1*2) = 1*0 = 1, (1*1)*2 = 0*2 = 2
        GIS.Result r = new GIS(3, star, differences(3)).check();
        assertFalse(r.isGIS());
        assertTrue(r.failure(), r.failure().startsWith("IVLS is not a group: associativity"));
        int[][] noId = { { 1, 2, 0 }, { 2, 0, 1 }, { 0, 1, 2 } };
        // noId is i + j + 1 mod 3, a group with identity 2: it passes the group check
        assertTrue(new GIS(3, noId, differences(3)).check().failure(), !new GIS(3, noId, differences(3)).check().failure().startsWith("IVLS"));
        int[][] notGroup = { { 0, 0 }, { 0, 1 } }; // multiplication on {0, 1}: 0 has no inverse
        r = new GIS(2, notGroup, new int[][] { { 1, 1 }, { 1, 1 } }).check();
        assertTrue(r.failure(), r.failure().startsWith("IVLS is not a group: inverse"));
        int[][] outOfRange = { { 0, 1 }, { 1, 2 } };
        r = new GIS(2, outOfRange, new int[][] { { 0, 1 }, { 1, 0 } }).check();
        assertTrue(r.failure(), r.failure().startsWith("IVLS is not a group: closure"));
    }

    /** D9: T on Z12 yields a GIS with int(x, y) = T_(y-x). */
    public void testTranspositionsYieldAGIS() {
        VoiceLeadingSpace s = pc("T", 12);
        GIS g = GIS.fromSpace(s);
        assertNotNull(g);
        assertTrue(g.check().isGIS());
        assertEquals("T7", s.action().elementName(g.interval(0, 7)));
        assertEquals("T5", s.action().elementName(g.interval(7, 0)));
        assertEquals("", GIS.whyNot(s));
    }

    /** D9: T/I on Z12 does not; the first pair with two transporters is named. */
    public void testTIOnZ12IsNotAGIS() {
        VoiceLeadingSpace s = pc("TI", 12);
        assertNull(GIS.fromSpace(s));
        assertEquals("(0, 0) has 2 transporters", GIS.whyNot(s));
    }

    /** D9: T on the triads misses major to minor; T/I on the triads is simply transitive. */
    public void testTriads() {
        VoiceLeadingSpace t = new VoiceLeadingSpace(FiniteAction.triads("T"));
        assertNull(GIS.fromSpace(t));
        assertEquals("(C, Cm) has 0 transporters", GIS.whyNot(t));
        VoiceLeadingSpace ti = new VoiceLeadingSpace(FiniteAction.triads("TI"));
        GIS g = GIS.fromSpace(ti);
        assertNotNull(g);
        assertTrue(g.check().toString(), g.check().isGIS());
    }

    /**
     * D8: T/I is not commutative, so the order of the product matters. With
     * i * j = i after j (instead of j after i) the composition axiom fails.
     */
    public void testProductOrderMatters() {
        VoiceLeadingSpace ti = new VoiceLeadingSpace(FiniteAction.triads("TI"));
        GIS g = GIS.fromSpace(ti);
        FiniteAction a = ti.action();
        int m = a.groupSize();
        int[][] wrong = new int[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                wrong[i][j] = a.multiply(i, j);
            }
        }
        GIS.Result r = new GIS(m, wrong, g.intervalTable()).check();
        assertFalse(r.isGIS());
        assertTrue(r.failure(), r.failure().startsWith("axiom 1 (composition)"));
        assertEquals(a.multiply(g.interval(0, 0), g.interval(0, 0)), g.product(g.interval(0, 0), g.interval(0, 0)));
    }

    /** Axiom 2 also fails when an interval has no target: one point, IVLS = Z2 (added for mutation M14). */
    public void testAxiom2NeedsATarget() {
        GIS.Result r = new GIS(2, addition(2), new int[][] { { 0 } }).check();
        assertFalse(r.isGIS());
        assertTrue(r.failure(), r.failure().startsWith("axiom 2 (unique target)"));
        assertTrue(r.failure(), r.failure().contains("s = 0, i = 1: 0 targets"));
    }
}
