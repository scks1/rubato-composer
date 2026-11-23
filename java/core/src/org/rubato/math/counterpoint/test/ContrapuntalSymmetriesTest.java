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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.counterpoint.ContrapuntalSymmetries;
import org.rubato.math.counterpoint.ContrapuntalSymmetries.Result;
import org.rubato.math.counterpoint.Dichotomy;
import org.rubato.math.counterpoint.DualAffine;
import org.rubato.math.counterpoint.StrongDichotomies;

/**
 * Reproduces the published first-species results of Agustin-Aquino, Junod
 * and Mazzola, Computational Counterpoint Worlds (CCW, 2015): every cell of
 * Table 3.1 (p. 36), Table 3.2 (p. 36), Example 2.5 (pp. 15-16) and the Z_6
 * example of Remark 2.3 (p. 23). The numbers in ROWS are the book's table,
 * copied as data: class, consonance k, then the forbidden successor
 * intervals for cantus firmus steps 0..11, "-" for none.
 */
public class ContrapuntalSymmetriesTest extends TestCase {

    private static final String[] ROWS = {
        "64 2 : 2 - 2 - 2 - 2 - 2 - 2 -",
        "64 4 : 2,4,7,9 - 2,4,7,9 - 2,4,7,9 - 2,4,7,9 - 2,4,7,9 - 2,4,7,9 -",
        "64 5 : 5,11 5,11 5,11 5,11 5,11 5,11 5,11 5,11 5,11 5,11 5,11 5,11",
        "64 7 : 2,4,7,9 - 2,4,7,9 - 2,4,7,9 - 2,4,7,9 - 2,4,7,9 - 2,4,7,9 -",
        "64 9 : 9 - 9 - 9 - 9 - 9 - 9 -",
        "64 11 : 11 11 11 11 11 11 11 11 11 11 11 11",
        "68 0 : 0 0 0 0 0 0 0 0 0 0 0 0",
        "68 1 : 1 - 1 - 1 - 1 - 1 - 1 -",
        "68 2 : 2,8 2,8 2,8 2,8 2,8 2,8 2,8 2,8 2,8 2,8 2,8 2,8",
        "68 3 : 1,3,5 - 1,3,5 - 1,3,5 - 1,3,5 - 1,3,5 - 1,3,5 -",
        "68 5 : 5 - 5 - 5 - 5 - 5 - 5 -",
        "68 8 : 8 8 8 8 8 8 8 8 8 8 8 8",
        "71 0 : 0,3 3 0,3 3 0,3 3 0,3 3 0,3 3 0,3 3",
        "71 1 : 1,2,7 - 1,2,7 - 1,2,7 - 1,2,7 - 1,2,7 - 1,2,7 -",
        "71 2 : 2,3 - 2,3 - 2,3 - 2,3 - 2,3 - 2,3 -",
        "71 3 : 2,3 - 2,3 - 2,3 - 2,3 - 2,3 - 2,3 -",
        "71 6 : 3,6 3 3,6 3 3,6 3 3,6 3 3,6 3 3,6 3",
        "71 7 : 1,2,7 - 1,2,7 - 1,2,7 - 1,2,7 - 1,2,7 - 1,2,7 -",
        "75 0 : 0 - - - - - 0 - - - - -",
        "75 1 : 0,1,4,5 - 0,1,4,5 - 0,1,4,5 - 0,1,4,5 - 0,1,4,5 - 0,1,4,5 -",
        "75 2 : 2,5 - - 2,5 - - 2,5 - - 2,5 - -",
        "75 4 : 0,1,4,5 - 0,1,4,5 - 0,1,4,5 - 0,1,4,5 - 0,1,4,5 - 0,1,4,5 -",
        "75 5 : 2,5 - - 2,5 - - 2,5 - - 2,5 - -",
        "75 8 : 2,8 2 2 2,8 2 2 2,8 2 2 2,8 2 2",
        "78 0 : 0,2,6 - 0,2,6 - 0,2,6 - 0,2,6 - 0,2,6 - 0,2,6 -",
        "78 1 : 1,2 - 1,2 - 1,2 - 1,2 - 1,2 - 1,2 -",
        "78 2 : 1,2 - 1,2 - 1,2 - 1,2 - 1,2 - 1,2 -",
        "78 4 : 1,4 - - 1,4 - - 1,4 - - 1,4 - -",
        "78 6 : 0,2,6 - 0,2,6 - 0,2,6 - 0,2,6 - 0,2,6 - 0,2,6 -",
        "78 10 : 1,10 - 1 1,4 1 - 1,10 - 1 1,4 1 -",
        "82 0 : 0 - - - - - 0 - - - - -",
        "82 3 : 3,9 - - 3,9 - - 3,9 - - 3,9 - -",
        "82 4 : 0,4,8 - 0,4,8 - 0,4,8 - 0,4,8 - 0,4,8 - 0,4,8 -",
        "82 7 : 7 7 7 7 7 7 7 7 7 7 7 7",
        "82 8 : 8 - - - - - 8 - - - - -",
        "82 9 : 3,9 - - 3,9 - - 3,9 - - 3,9 - -",
    };

    public void testTable31AllCells() {
        int cells = 0;
        Set<String> rowsSeen = new HashSet<String>();
        for (String row : ROWS) {
            String[] halves = row.split(":");
            String[] head = halves[0].trim().split(" ");
            String[] cellText = halves[1].trim().split(" ");
            int cls = Integer.parseInt(head[0]);
            int k = Integer.parseInt(head[1]);
            assertEquals(row, 12, cellText.length);
            rowsSeen.add(cls + " " + k);
            Result r = ContrapuntalSymmetries.analyse(StrongDichotomies.ofClass(cls), k);
            for (int step = 0; step < 12; step++) {
                int[] expected = cellText[step].equals("-") ? new int[0] : parse(cellText[step]);
                assertEquals("class " + cls + ", k = " + k + ", step " + step,
                        Arrays.toString(expected), Arrays.toString(r.forbiddenAfterStep(step)));
                cells++;
            }
        }
        assertEquals(432, cells);
        // every consonance of every class has a row
        for (Map.Entry<Integer, Dichotomy> e : StrongDichotomies.all().entrySet()) {
            for (int k : e.getValue().getConsonances()) {
                assertTrue(e.getKey() + " " + k, rowsSeen.contains(e.getKey() + " " + k));
            }
        }
    }

    /** Table 3.2: parallels forbidden after every cantus firmus step. */
    public void testTable32GeneralForbiddenParallels() {
        assertEquals("[5, 11]", alwaysForbiddenParallels(64));
        assertEquals("[0, 2, 8]", alwaysForbiddenParallels(68));
        assertEquals("[]", alwaysForbiddenParallels(71));
        assertEquals("[]", alwaysForbiddenParallels(75));
        assertEquals("[]", alwaysForbiddenParallels(78));
        assertEquals("[7]", alwaysForbiddenParallels(82));
    }

    /** Example 2.5: (K/D), xi = e.9, maximal intersection 56, g = T^{e.8}(5+e.4) contrapuntal. */
    public void testExample25() {
        Result r = ContrapuntalSymmetries.analyse(StrongDichotomies.FUX, 9);
        assertEquals(56, r.getMaxIntersection());
        assertTrue(r.getSymmetries().contains(DualAffine.ofH(8, 5, 4, 12)));
    }

    /** Remark 2.3: W = {0,1,3} in Z_6, xi = e.1: 15 successors, unique symmetry T^{e.3}(1+e.3). */
    public void testZ6Remark23() {
        Dichotomy w = new Dichotomy(6, 0, 1, 3);
        assertTrue(w.isStrong());
        Result r = ContrapuntalSymmetries.analyse(w, 1);
        assertEquals(15, r.getSuccessorCount());
        assertEquals(15, r.getMaxIntersection());
        assertEquals(1, r.getSymmetries().size());
        assertEquals(DualAffine.ofH(3, 1, 3, 6), r.getSymmetries().get(0));
    }

    /**
     * Remark 2.3, Z_16. The book gives 64 successors, 105 symmetries and
     * "H ... 128 symmetries". This code reads 64 as the single-symmetry
     * intersection and 128 as the candidates of Def. 2.5 (the symmetries of
     * H that meet its first two conditions; H itself has 2048 elements).
     * It finds 121 contrapuntal symmetries, not 105, and a union of 127
     * successors; neither book figure is asserted for those two.
     * An independent recount (brute force over H, Algorithm 2.1 as printed,
     * and brute force over the whole affine group) gives 121 too, and no
     * strong dichotomy of Z_16 at any consonance gives 105, so the book's
     * 105 is taken as an error (COUNTERPOINT-SPEC A2).
     */
    public void testZ16Remark23Readings() {
        Dichotomy x = new Dichotomy(16, 1, 4, 5, 6, 7, 8, 14, 15);
        assertTrue(x.isStrong());
        Result r = ContrapuntalSymmetries.analyse(x, 6);
        assertEquals(64, r.getMaxIntersection());
        assertEquals(128, r.getCandidateCount());
        assertEquals(121, r.getSymmetries().size());
        assertEquals(127, r.getSuccessorCount());
    }

    /**
     * The two counts over all 36 consonances of the six classes. The books
     * disagree on the lower bound (CCW Thm. 3.1 p. 35: 42; Topos I Thm. 33
     * p. 538: 36); this test asserts only what the code computes.
     */
    public void testCountsOverZ12() {
        int minMeet = Integer.MAX_VALUE, maxMeet = 0, minUnion = Integer.MAX_VALUE, maxUnion = 0, pairs = 0;
        for (Dichotomy d : StrongDichotomies.all().values()) {
            for (int k : d.getConsonances()) {
                Result r = ContrapuntalSymmetries.analyse(d, k);
                minMeet = Math.min(minMeet, r.getMaxIntersection());
                maxMeet = Math.max(maxMeet, r.getMaxIntersection());
                minUnion = Math.min(minUnion, r.getSuccessorCount());
                maxUnion = Math.max(maxUnion, r.getSuccessorCount());
                assertTrue(r.getSuccessorCount() >= r.getMaxIntersection());
                pairs++;
            }
        }
        assertEquals(36, pairs);
        assertEquals("smallest single-symmetry count", 42, minMeet);
        assertEquals("largest single-symmetry count", 60, maxMeet);
        assertEquals("smallest union count", 48, minUnion);
        assertEquals("largest union count", 70, maxUnion);
    }

    /** The reduction to H and cantus 0 (CCW Thm. 2.2) agrees with Def. 2.5 over the whole group. */
    public void testReductionAgreesWithWholeGroup() {
        int[][] cases = { { 82, 0, 9 }, { 82, 5, 7 }, { 82, 11, 0 }, { 64, 3, 4 }, { 71, 7, 6 }, { 78, 2, 1 } };
        for (int[] c : cases) {
            Dichotomy d = StrongDichotomies.ofClass(c[0]);
            Result reduced = ContrapuntalSymmetries.analyse(d, c[1], c[2]);
            Result whole = ContrapuntalSymmetries.bruteForce(d, c[1], c[2]);
            String what = "class " + c[0] + ", xi = " + c[1] + "+e." + c[2];
            assertEquals(what, reduced.getMaxIntersection(), whole.getMaxIntersection());
            assertEquals(what, toSet(reduced.getSuccessors()), toSet(whole.getSuccessors()));
        }
    }

    /** Successors of x + e.k are those of e.k moved by T^x (CCW Thm. 2.2). */
    public void testTranslation() {
        Result zero = ContrapuntalSymmetries.analyse(StrongDichotomies.FUX, 3);
        Result five = ContrapuntalSymmetries.analyse(StrongDichotomies.FUX, 5, 3);
        assertEquals(zero.getSuccessorCount(), five.getSuccessorCount());
        for (int[] s : zero.getSuccessors()) {
            assertTrue(five.isSuccessor(s[0] + 5, s[1]));
        }
    }

    /** CCW Prop. 2.2: p_x[e] keeps the cantus firmus x and swaps consonant and dissonant intervals. */
    public void testInducedPolarity() {
        for (Dichotomy d : StrongDichotomies.all().values()) {
            for (int x = 0; x < 12; x++) {
                DualAffine p = ContrapuntalSymmetries.inducedPolarity(d, x);
                for (int y = 0; y < 12; y++) {
                    assertEquals(x, p.applyCantus(x, y));
                    assertEquals(!d.isConsonant(y), d.isConsonant(p.applyInterval(x, y)));
                }
            }
        }
        assertEquals("T^{\u03b5.2}\u00b75", ContrapuntalSymmetries.inducedPolarity(StrongDichotomies.FUX, 0).toString());
    }

    public void testDissonanceRefused() {
        try {
            ContrapuntalSymmetries.analyse(StrongDichotomies.FUX, 5);
            fail("5 is a dissonance of (K/D)");
        }
        catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not a consonance"));
        }
    }

    public void testNonStrongRefused() {
        try {
            ContrapuntalSymmetries.analyse(new Dichotomy(6, 0, 2, 4), 0);
            fail("{0,2,4}/{1,3,5} is not strong (CCW Example 2.6, p. 23)");
        }
        catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("not strong"));
        }
    }

    private static String alwaysForbiddenParallels(int cls) {
        Dichotomy d = StrongDichotomies.ofClass(cls);
        StringBuilder b = new StringBuilder("[");
        for (int k : d.getConsonances()) {
            Result r = ContrapuntalSymmetries.analyse(d, k);
            boolean always = true;
            for (int step = 0; step < 12; step++) {
                if (r.isSuccessor(step, k)) {
                    always = false;
                }
            }
            if (always) {
                b.append(b.length() > 1 ? ", " : "").append(k);
            }
        }
        return b.append("]").toString();
    }

    private static int[] parse(String cell) {
        String[] p = cell.split(",");
        int[] r = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            r[i] = Integer.parseInt(p[i]);
        }
        return r;
    }

    private static Set<String> toSet(List<int[]> pairs) {
        Set<String> s = new HashSet<String>();
        for (int[] p : pairs) {
            s.add(p[0] + "," + p[1]);
        }
        return s;
    }
}
