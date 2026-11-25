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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.rubato.math.counterpoint.ContrapuntalSymmetries;
import org.rubato.math.counterpoint.DualAffine;
import org.rubato.math.counterpoint.StrongDichotomies;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Pair;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Realization;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Reason;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Refusal;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.RelaxationMode;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.State;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Verdict;

/**
 * Reproduces the counts of Okumura and Shibayama, arXiv 2606.01102v3, for
 * the three-voice model with the Fuxian dichotomy and the mask H_Fux, and
 * the figures of the hold-fibre relaxation computed in
 * sources/papers/2606.01102/ASSESSMENT.md. The expected values are the
 * paper's (Table 1 p. 8, Table 8 p. 24, Table 9 p. 25, Remark 6.4 and
 * Theorem 6.6 p. 21, Proposition 6.8 p. 23, Example 3.3 p. 8, V1 p. 16)
 * and the assessment's (hold-fibre), copied as data.
 */
public class ThreeVoiceCounterpointTest extends TestCase {

    private static final ThreeVoiceCounterpoint M = ThreeVoiceCounterpoint.fux();

    /** Table 8, h1 ... h26, as (b, c). */
    private static final int[][] TABLE8 = {
        { 0, 0 }, { 0, 3 }, { 0, 4 }, { 0, 7 }, { 0, 8 }, { 0, 9 },
        { 3, 0 }, { 3, 3 }, { 3, 7 }, { 3, 8 }, { 3, 9 },
        { 4, 0 }, { 4, 4 }, { 4, 7 }, { 4, 9 },
        { 7, 0 }, { 7, 3 }, { 7, 4 }, { 7, 7 },
        { 8, 0 }, { 8, 3 }, { 8, 8 },
        { 9, 0 }, { 9, 3 }, { 9, 4 }, { 9, 9 } };

    /** Table 9 keyed by (b, c), the paper's c-over-b entries read across. */
    private static final int[][] TABLE9 = {
        { 0, 0, 138 }, { 0, 3, 156 }, { 0, 4, 138 }, { 0, 7, 136 }, { 0, 8, 126 }, { 0, 9, 156 },
        { 3, 0, 184 }, { 3, 3, 132 }, { 3, 7, 136 }, { 3, 8, 204 }, { 3, 9, 256 },
        { 4, 0, 126 }, { 4, 4, 138 }, { 4, 7, 138 }, { 4, 9, 206 },
        { 7, 0, 198 }, { 7, 3, 168 }, { 7, 4, 146 }, { 7, 7, 152 },
        { 8, 0, 138 }, { 8, 3, 152 }, { 8, 8, 120 },
        { 9, 0, 184 }, { 9, 3, 256 }, { 9, 4, 140 }, { 9, 9, 132 } };

    private static List<State> zeroSources() {
        List<State> s = new ArrayList<State>();
        for (int[] h : TABLE8) {
            s.add(M.state(0, h[0], h[1]));
        }
        return s;
    }

    private static List<Realization> inMask() {
        List<Realization> r = new ArrayList<Realization>();
        for (Realization z : M.parsimoniousRealizations()) {
            if (z.isTargetInMask()) {
                r.add(z);
            }
        }
        return r;
    }

    /** Table 1 (p. 8): the forbidden target intervals F_{r,j} of the two-voice model, all 72 cells. */
    public void testTable1() {
        int cells = 0;
        for (int r : new int[] { 0, 3, 4, 7, 8, 9 }) {
            ContrapuntalSymmetries.Result res = ContrapuntalSymmetries.analyse(StrongDichotomies.FUX, 0, r);
            for (int j = 0; j < 12; j++) {
                Set<Integer> want = new HashSet<Integer>();
                switch (r) {
                case 0: if (j % 6 == 0) { want.add(0); } break;
                case 3: case 9: if (j % 3 == 0) { want.add(3); want.add(9); } break;
                case 4: if (j % 2 == 0) { want.add(0); want.add(4); want.add(8); } break;
                case 7: want.add(7); break;
                case 8: if (j % 6 == 0) { want.add(8); } break;
                default: fail();
                }
                Set<Integer> got = new HashSet<Integer>();
                for (int l : res.forbiddenAfterStep(j)) {
                    got.add(l);
                }
                assertEquals("r = " + r + ", j = " + j, want, got);
                cells++;
            }
        }
        assertEquals(72, cells);
    }

    /** Def. 3.1 (p. 7) and Table 8 (p. 24): 26 configurations in Table 8's order, 312 states. */
    public void testMaskAndStates() {
        List<int[]> mask = M.mask();
        assertEquals(26, mask.size());
        for (int i = 0; i < 26; i++) {
            assertTrue("h" + (i + 1), Arrays.equals(TABLE8[i], mask.get(i)));
            assertEquals(i + 1, M.label(TABLE8[i][0], TABLE8[i][1]));
        }
        assertEquals(0, M.label(4, 8));
        assertEquals(0, M.label(3, 4));
        assertEquals(0, M.label(5, 0));
        assertEquals(312, M.states().size());
        int distinct = 0;
        for (int[] h : mask) {
            if (h[0] != 0 && h[1] != 0 && h[0] != h[1]) {
                distinct++;
            }
        }
        assertEquals("ten configurations with three distinct pitch classes", 10, distinct);
        assertTrue(M.inMask(M.state(5, 7, 4)));
        assertFalse(M.inMask(M.state(5, 4, 8)));
        assertFalse(M.inMask(M.state(0, 5, 9)));
    }

    /** Table 9 (p. 25): |S_H(ξ)| for each configuration, independent of the lower voice. */
    public void testTable9() {
        int total = 0;
        for (int[] row : TABLE9) {
            int n = M.successors(M.state(0, row[0], row[1]), RelaxationMode.STRICT).size();
            assertEquals("(b, c) = (" + row[0] + ", " + row[1] + ")", row[2], n);
            total += n;
        }
        assertEquals(4156, total);
        int whole = 0;
        for (State s : M.states()) {
            whole += M.successors(s, RelaxationMode.STRICT).size();
        }
        assertEquals("12 × 4,156", 49872, whole);
    }

    /** Remark 6.4 (p. 21): 244 targets in ev(R) against 382 two-voice successors. */
    public void testRemark64() {
        int[] k7 = { 0, 1, 3, 4, 8, 9 };
        Set<State> evr = new HashSet<State>();
        for (int u = 0; u < 12; u++) {
            for (int v : k7) {
                evr.add(M.state(u, 7 * v, 4 * v));
            }
        }
        assertEquals(72, evr.size());
        int three = 0;
        int two = 0;
        for (int v : k7) {
            State s = M.state(0, 7 * v, 4 * v);
            for (State t : M.successors(s, RelaxationMode.STRICT)) {
                if (evr.contains(t)) {
                    three++;
                }
            }
            two += ContrapuntalSymmetries.analyse(StrongDichotomies.FUX, 0, 7 * v % 12).getSuccessorCount();
        }
        assertEquals(244, three);
        assertEquals(382, two);
    }

    private static int[] triad(int r, boolean major) {
        return new int[] { Math.floorMod(r, 12), Math.floorMod(r + (major ? 4 : 3), 12), Math.floorMod(r + 7, 12) };
    }

    private static State voicing(int[] triad, String type) {
        int[] p = new int[3];
        for (int i = 0; i < 3; i++) {
            char ch = type.charAt(i);
            p[i] = ch == '1' ? triad[0] : ch == '3' ? triad[1] : triad[2];
        }
        return M.fromPitchClasses(p[0], p[1], p[2]);
    }

    /** Theorem 6.6 (p. 21): |Adm(X, Y)| by δ = gcd(d, 6), over End(Y) (Table 7, p. 22) and the four types. */
    public void testTheorem66() {
        int[][] endsMajor = { { 0, 1 }, { 0, 0 }, { 4, 0 }, { 7, 0 }, { 7, 3 }, { 0, 4 }, { 4, 8 }, { 4, 9 } };
        int[][] endsMinor = { { 0, 1 }, { 0, 0 }, { 3, 0 }, { 7, 0 }, { 3, 3 }, { 3, 4 }, { 7, 8 }, { 0, 9 } };
        String[] types = { "135", "153", "351", "315" };
        int[] deltas = { 1, 2, 3, 6 };
        int[][] want = { { 22, 16, 16, 10 }, { 22, 12, 16, 7 } };
        for (int m = 0; m < 2; m++) {
            boolean major = m == 0;
            int[][] ends = major ? endsMajor : endsMinor;
            for (int i = 0; i < 4; i++) {
                int d = deltas[i];
                int[] x = triad(-d, major);
                int count = 0;
                for (int[] e : ends) {
                    int u = Math.floorMod(e[0] + e[1] * d, 12);
                    int v = e[1];
                    for (String k : types) {
                        State s = voicing(x, k);
                        State t = M.state(u + v * s.getLower(), v * s.getB(), v * s.getC());
                        if (M.inMask(t) && M.isAdmitted(s, t, RelaxationMode.STRICT)) {
                            count++;
                        }
                    }
                }
                assertEquals((major ? "major" : "minor") + ", δ = " + d, want[m][i], count);
            }
        }
    }

    /** §6.4 and Prop. 6.8 (p. 23): 288 realizations, 192 in the mask, 12 admitted, all (P, minor, 351). */
    public void testProposition68() {
        List<Realization> all = M.parsimoniousRealizations();
        assertEquals(288, all.size());
        List<Realization> in = inMask();
        assertEquals(192, in.size());
        int admitted = 0;
        int still = 0;
        Set<String> patterns = new HashSet<String>();
        for (Realization z : in) {
            Verdict v = M.check(z.getSource(), z.getTarget(), RelaxationMode.STRICT);
            if (v.isAdmitted()) {
                admitted++;
                patterns.add(z.getTransformation() + (z.isMajor() ? "+" : "-") + z.getType());
            }
            for (Refusal r : v.getRefusals()) {
                if (r.getReason() == Reason.PAIR_STILL) {
                    still++;
                    break;
                }
            }
        }
        assertEquals(12, admitted);
        assertEquals(new HashSet<String>(Arrays.asList("P-351")), patterns);
        assertEquals("of the 192, 168 keep an active pair stationary", 168, still);
        // the paper's example, lower voice up: Eb G C -> E G C
        assertTrue(M.isAdmitted(M.fromPitchClasses(3, 7, 0), M.fromPitchClasses(4, 7, 0), RelaxationMode.STRICT));
        // targets outside the mask hold the fifth of N(X) in the lower voice; never for P
        for (Realization z : all) {
            if (z.getTransformation() == 'P') {
                assertTrue(z.isTargetInMask());
            }
        }
    }

    /** Example 3.3 (p. 8): twelve states in the mask and all 11 steps admitted. */
    public void testExample33() {
        int[][] seq = { { 2, 3, 0 }, { 9, 8, 3 }, { 5, 4, 7 }, { 2, 7, 3 }, { 1, 8, 3 }, { 2, 7, 3 },
                        { 4, 8, 3 }, { 5, 7, 4 }, { 7, 3, 7 }, { 5, 7, 4 }, { 4, 8, 3 }, { 2, 3, 0 } };
        for (int i = 0; i < seq.length; i++) {
            assertTrue("ξ" + (i + 1), M.inMask(M.state(seq[i][0], seq[i][1], seq[i][2])));
        }
        for (int i = 0; i + 1 < seq.length; i++) {
            State s = M.state(seq[i][0], seq[i][1], seq[i][2]);
            State t = M.state(seq[i + 1][0], seq[i + 1][1], seq[i + 1][2]);
            assertTrue("ξ" + (i + 1) + " → ξ" + (i + 2), M.isAdmitted(s, t, RelaxationMode.STRICT));
        }
    }

    /** V1 (p. 16), the baseline without joint maximization: 24 of the 192 and 5,334 edges. */
    public void testV1Baseline() {
        int admitted = 0;
        for (Realization z : inMask()) {
            if (M.isAdmittedV1(z.getSource(), z.getTarget())) {
                admitted++;
            }
        }
        assertEquals(24, admitted);
        int edges = 0;
        for (State s : zeroSources()) {
            for (State t : M.states()) {
                if (M.isAdmittedV1(s, t)) {
                    edges++;
                }
                if (M.isAdmitted(s, t, RelaxationMode.STRICT)) {
                    assertTrue("S_H ⊆ S_H^V1", M.isAdmittedV1(s, t));
                }
            }
        }
        assertEquals(5334, edges);
    }

    /** The hold-fibre figures of ASSESSMENT.md (candidate B) and extra_check.out. */
    public void testHoldFibreFigures() {
        Map<Character, Integer> by = new TreeMap<Character, Integer>();
        Map<String, Integer> refused = new TreeMap<String, Integer>();
        for (Realization z : inMask()) {
            if (M.isAdmitted(z.getSource(), z.getTarget(), RelaxationMode.HOLD_FIBRE)) {
                Integer c = by.get(z.getTransformation());
                by.put(z.getTransformation(), c == null ? 1 : c + 1);
            }
            else {
                String k = z.getTransformation() + (z.isMajor() ? "+" : "-") + z.getType();
                Integer c = refused.get(k);
                refused.put(k, c == null ? 1 : c + 1);
            }
        }
        assertEquals(Integer.valueOf(84), by.get('P'));
        assertEquals(Integer.valueOf(36), by.get('L'));
        assertEquals(Integer.valueOf(48), by.get('R'));
        Map<String, Integer> wantRefused = new TreeMap<String, Integer>();
        wantRefused.put("P+351", 12);
        wantRefused.put("L-135", 12);
        assertEquals(wantRefused, refused);
        int edges = 0;
        int lost = 0;
        int fifths = 0;
        for (State s : zeroSources()) {
            for (State t : M.states()) {
                boolean strict = M.isAdmitted(s, t, RelaxationMode.STRICT);
                boolean hold = M.isAdmitted(s, t, RelaxationMode.HOLD_FIBRE);
                if (hold) {
                    edges++;
                    for (Pair p : Pair.values()) {
                        int[] z0 = ThreeVoiceCounterpoint.project(p, s);
                        int[] z1 = ThreeVoiceCounterpoint.project(p, t);
                        if (z0[1] == 7 && z1[1] == 7 && !Arrays.equals(z0, z1)) {
                            fifths++;
                            break;
                        }
                    }
                }
                if (strict && !hold) {
                    lost++;
                }
            }
        }
        assertEquals(4365, edges);
        assertEquals(0, lost);
        assertEquals(0, fifths);
    }

    /** A target with no stationary active pair gets the strict decision under hold-fibre. */
    public void testHoldFibreLeavesMovingStepsAlone() {
        for (State s : zeroSources()) {
            for (State t : M.states()) {
                boolean anyStill = false;
                for (Pair p : M.activePairs(s)) {
                    if (Arrays.equals(ThreeVoiceCounterpoint.project(p, s), ThreeVoiceCounterpoint.project(p, t))) {
                        anyStill = true;
                    }
                }
                if (!anyStill) {
                    assertEquals(s + " → " + t, M.isAdmitted(s, t, RelaxationMode.STRICT),
                                 M.isAdmitted(s, t, RelaxationMode.HOLD_FIBRE));
                }
            }
        }
    }

    /** check() agrees with the successor sets in both modes for all 312 × 312 steps, and reasons appear exactly on refusals. */
    public void testCheckAgreesWithSuccessors() {
        Map<Reason, Integer> strictKinds = new HashMap<Reason, Integer>();
        Map<Reason, Integer> holdKinds = new HashMap<Reason, Integer>();
        for (RelaxationMode mode : new RelaxationMode[] { RelaxationMode.STRICT, RelaxationMode.HOLD_FIBRE }) {
            for (State s : M.states()) {
                Set<State> succ = new HashSet<State>(M.successors(s, mode));
                for (State t : M.states()) {
                    Verdict v = M.check(s, t, mode);
                    boolean in = succ.contains(t);
                    assertEquals(mode + " " + s + " → " + t, in, v.isAdmitted());
                    assertEquals(in, M.isAdmitted(s, t, mode));
                    assertEquals(in, v.getRefusals().isEmpty());
                    if (s.getLower() == 0) {
                        Set<Reason> seen = new HashSet<Reason>();
                        for (Refusal r : v.getRefusals()) {
                            seen.add(r.getReason());
                        }
                        Map<Reason, Integer> kinds = mode == RelaxationMode.STRICT ? strictKinds : holdKinds;
                        for (Reason r : seen) {
                            Integer c = kinds.get(r);
                            kinds.put(r, c == null ? 1 : c + 1);
                        }
                    }
                }
            }
        }
        // P01-reasons-probe.txt, from the assessment's oracle
        assertEquals(Integer.valueOf(270), strictKinds.get(Reason.PAIR_STILL));
        assertEquals(Integer.valueOf(429), strictKinds.get(Reason.FIFTH_TO_FIFTH));
        assertEquals(Integer.valueOf(2214), strictKinds.get(Reason.PAIR_EXCLUDED));
        assertEquals(Integer.valueOf(1178), strictKinds.get(Reason.NOT_MAXIMAL));
        assertNull(holdKinds.get(Reason.PAIR_STILL));
        assertEquals(Integer.valueOf(429), holdKinds.get(Reason.FIFTH_TO_FIFTH));
        assertEquals(Integer.valueOf(2214), holdKinds.get(Reason.PAIR_EXCLUDED));
        assertEquals(Integer.valueOf(1182), holdKinds.get(Reason.NOT_MAXIMAL));
    }

    private static List<String> reasons(Verdict v) {
        List<String> l = new ArrayList<String>();
        for (Refusal r : v.getRefusals()) {
            l.add(r.getReason() + (r.getPair() == null ? "" : ":" + r.getPair()));
        }
        return l;
    }

    /** Named steps with the reasons P01-reasons-probe.txt computed. */
    public void testReasons() {
        State cegRoot = M.fromPitchClasses(0, 4, 7);
        assertEquals(Arrays.asList("PAIR_STILL:LU"),
                     reasons(M.check(cegRoot, M.fromPitchClasses(0, 3, 7), RelaxationMode.STRICT)));
        assertTrue(M.check(cegRoot, M.fromPitchClasses(0, 3, 7), RelaxationMode.HOLD_FIBRE).isAdmitted());
        assertEquals(Arrays.asList("PAIR_STILL:LM", "PAIR_EXCLUDED:MU"),
                     reasons(M.check(M.fromPitchClasses(0, 3, 7), M.fromPitchClasses(0, 3, 8), RelaxationMode.STRICT)));
        assertEquals(Arrays.asList("PAIR_EXCLUDED:MU"),
                     reasons(M.check(M.fromPitchClasses(0, 3, 7), M.fromPitchClasses(0, 3, 8), RelaxationMode.HOLD_FIBRE)));
        assertEquals(Arrays.asList("NOT_MAXIMAL"),
                     reasons(M.check(M.fromPitchClasses(4, 7, 0), M.fromPitchClasses(3, 7, 0), RelaxationMode.STRICT)));
        assertEquals(Arrays.asList("FIFTH_TO_FIFTH:LM"),
                     reasons(M.check(M.state(0, 7, 0), M.state(2, 7, 0), RelaxationMode.STRICT)));
        assertEquals(Arrays.asList("FIFTH_TO_FIFTH:LU"),
                     reasons(M.check(cegRoot, M.fromPitchClasses(2, 5, 9), RelaxationMode.HOLD_FIBRE)));
        assertEquals(Arrays.asList("PAIR_STILL:LM", "PAIR_STILL:LU", "PAIR_STILL:MU"),
                     reasons(M.check(cegRoot, cegRoot, RelaxationMode.STRICT)));
        assertTrue(M.check(cegRoot, cegRoot, RelaxationMode.HOLD_FIBRE).isAdmitted());
        assertEquals(Arrays.asList("NOT_MAXIMAL"),
                     reasons(M.check(M.state(0, 0, 0), M.state(0, 4, 4), RelaxationMode.STRICT)));
        // outside the mask: an augmented triad and a fourth above the bass
        assertEquals(Arrays.asList("TARGET_OUTSIDE_MASK"),
                     reasons(M.check(cegRoot, M.state(0, 4, 8), RelaxationMode.STRICT)));
        assertEquals(Arrays.asList("SOURCE_OUTSIDE_MASK"),
                     reasons(M.check(M.state(0, 5, 9), cegRoot, RelaxationMode.STRICT)));
        assertEquals(Arrays.asList("SOURCE_OUTSIDE_MASK", "TARGET_OUTSIDE_MASK"),
                     reasons(M.check(M.state(0, 5, 9), M.state(0, 4, 8), RelaxationMode.HOLD_FIBRE)));
        String text = M.check(cegRoot, M.fromPitchClasses(0, 3, 7), RelaxationMode.STRICT).getRefusals().get(0).text();
        assertTrue(text, text.contains("lower and upper voices") && text.contains("eq. (4)"));
    }

    /** The conjugated symmetries from H give the same images gX[ε] as the whole group (D4). */
    public void testConjugatedSymmetriesMatchFullGroup() {
        int[][] sample = { { 0, 0, 0 }, { 5, 7, 4 }, { 7, 3, 7 }, { 11, 9, 3 }, { 2, 8, 3 }, { 4, 4, 9 } };
        for (int[] q : sample) {
            State s = M.state(q[0], q[1], q[2]);
            for (Pair p : M.activePairs(s)) {
                int[] z = ThreeVoiceCounterpoint.project(p, s);
                Set<Set<Integer>> ours = new HashSet<Set<Integer>>();
                for (DualAffine g : M.symmetries(p, s)) {
                    ours.add(image(g));
                }
                Set<Set<Integer>> full = new HashSet<Set<Integer>>();
                for (DualAffine g : ContrapuntalSymmetries.bruteForce(StrongDichotomies.FUX, z[0], z[1]).getSymmetries()) {
                    full.add(image(g));
                }
                assertEquals(s + " " + p, full, ours);
            }
        }
    }

    private static Set<Integer> image(DualAffine g) {
        Set<Integer> s = new HashSet<Integer>();
        for (int x = 0; x < 12; x++) {
            for (int y : StrongDichotomies.FUX.getConsonances()) {
                s.add(g.applyCantus(x, y) * 12 + g.applyInterval(x, y));
            }
        }
        return s;
    }

    /** Active pairs, projections and the state's pitch classes. */
    public void testProjectionsAndActivePairs() {
        State s = M.state(2, 3, 0);
        assertEquals(2, s.getLower());
        assertEquals(5, s.getMiddle());
        assertEquals(2, s.getUpper());
        assertTrue(Arrays.equals(new int[] { 2, 3 }, ThreeVoiceCounterpoint.project(Pair.LM, s)));
        assertTrue(Arrays.equals(new int[] { 2, 0 }, ThreeVoiceCounterpoint.project(Pair.LU, s)));
        assertTrue(Arrays.equals(new int[] { 5, 9 }, ThreeVoiceCounterpoint.project(Pair.MU, s)));
        assertEquals(Arrays.asList(Pair.LM, Pair.LU, Pair.MU), M.activePairs(s));
        // C E A has a fourth between the upper voices: MU inactive
        assertEquals(Arrays.asList(Pair.LM, Pair.LU), M.activePairs(M.fromPitchClasses(0, 4, 9)));
        assertEquals(M.state(14, -9, 12), M.state(2, 3, 0));
        assertEquals(M.state(0, 4, 9), M.fromPitchClasses(12, 16, 21));
    }

    /** Invalid masks and sources are refused with a message. */
    public void testInvalidInput() {
        try {
            new ThreeVoiceCounterpoint(StrongDichotomies.FUX, new int[][] { { 0, 5 } });
            fail("a mask entry with a dissonance must be refused");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("5"));
        }
        try {
            M.successors(M.state(0, 5, 9), RelaxationMode.STRICT);
            fail("a source outside the mask has no successor set");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("mask"));
        }
        ThreeVoiceCounterpoint same = new ThreeVoiceCounterpoint(StrongDichotomies.FUX, TABLE8);
        assertEquals(138, same.successors(same.state(0, 0, 0), RelaxationMode.STRICT).size());
    }
}
