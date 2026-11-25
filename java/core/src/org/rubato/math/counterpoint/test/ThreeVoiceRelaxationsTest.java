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

import org.rubato.math.counterpoint.ThreeVoiceCounterpoint;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Pair;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Realization;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Reason;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Refusal;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.RelaxationMode;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.State;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Verdict;

/**
 * The HOLD and REFLEXIVE relaxations (spec D12, D13). The expected values
 * come from the oracle in sources/papers/2606.01102/three_voice_check.py
 * (ASSESSMENT.md, candidate B) and from
 * logs/three-voice-relaxations-2026-11-24/P02-modes-probe.txt, copied as data.
 */
public class ThreeVoiceRelaxationsTest extends TestCase {

    private static final ThreeVoiceCounterpoint M = ThreeVoiceCounterpoint.fux();

    private static final RelaxationMode[] NEW_MODES = { RelaxationMode.HOLD, RelaxationMode.REFLEXIVE };

    private static List<State> zeroSources() {
        List<State> out = new ArrayList<State>();
        for (State s : M.states()) {
            if (s.getLower() == 0) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean anyStill(State s, State t) {
        for (Pair p : M.activePairs(s)) {
            if (Arrays.equals(ThreeVoiceCounterpoint.project(p, s), ThreeVoiceCounterpoint.project(p, t))) {
                return true;
            }
        }
        return false;
    }

    /** STRICT stays the first constant and the four modes keep their order. */
    public void testModes() {
        assertEquals(Arrays.asList(RelaxationMode.STRICT, RelaxationMode.HOLD_FIBRE, RelaxationMode.HOLD,
                                   RelaxationMode.REFLEXIVE),
                     Arrays.asList(RelaxationMode.values()));
        assertEquals(26, zeroSources().size());
    }

    /** Prop. 6.8's 192 under each new mode: admitted by transformation, and refusals by reason set (P02). */
    public void testProposition68Splits() {
        Map<String, Map<Character, Integer>> by = new HashMap<String, Map<Character, Integer>>();
        Map<String, Map<String, Integer>> sets = new HashMap<String, Map<String, Integer>>();
        for (RelaxationMode mode : NEW_MODES) {
            Map<Character, Integer> b = new TreeMap<Character, Integer>();
            Map<String, Integer> r = new TreeMap<String, Integer>();
            int inMask = 0;
            for (Realization z : M.parsimoniousRealizations()) {
                if (!z.isTargetInMask()) {
                    continue;
                }
                inMask++;
                Verdict v = M.check(z.getSource(), z.getTarget(), mode);
                assertEquals(v.isAdmitted(), M.isAdmitted(z.getSource(), z.getTarget(), mode));
                String key;
                if (v.isAdmitted()) {
                    Integer c = b.get(z.getTransformation());
                    b.put(z.getTransformation(), c == null ? 1 : c + 1);
                    key = "admitted";
                }
                else {
                    Set<String> rs = new java.util.TreeSet<String>();
                    for (Refusal f : v.getRefusals()) {
                        rs.add(f.getReason().name());
                    }
                    key = String.join("+", rs);
                }
                Integer c = r.get(key);
                r.put(key, c == null ? 1 : c + 1);
            }
            assertEquals(192, inMask);
            by.put(mode.name(), b);
            sets.put(mode.name(), r);
        }
        Map<Character, Integer> hold = new TreeMap<Character, Integer>();
        hold.put('P', 60);
        hold.put('L', 36);
        hold.put('R', 48);
        assertEquals(hold, by.get("HOLD"));
        Map<Character, Integer> refl = new TreeMap<Character, Integer>();
        refl.put('P', 96);
        refl.put('L', 36);
        refl.put('R', 48);
        assertEquals(refl, by.get("REFLEXIVE"));
        Map<String, Integer> holdSets = new TreeMap<String, Integer>();
        holdSets.put("admitted", 144);
        holdSets.put("NOT_MAXIMAL", 36);
        holdSets.put("PAIR_EXCLUDED", 12);
        assertEquals(holdSets, sets.get("HOLD"));
        Map<String, Integer> reflSets = new TreeMap<String, Integer>();
        reflSets.put("admitted", 180);
        reflSets.put("PAIR_EXCLUDED", 12);
        assertEquals(reflSets, sets.get("REFLEXIVE"));
    }

    /** REFLEXIVE refuses only the 12 realizations L from a minor triad of type 135 (extra_check.out). */
    public void testReflexiveRefusesOnlyLMinor135() {
        Map<String, Integer> refused = new TreeMap<String, Integer>();
        for (Realization z : M.parsimoniousRealizations()) {
            if (z.isTargetInMask() && !M.isAdmitted(z.getSource(), z.getTarget(), RelaxationMode.REFLEXIVE)) {
                String k = z.getTransformation() + (z.isMajor() ? "+" : "-") + z.getType();
                Integer c = refused.get(k);
                refused.put(k, c == null ? 1 : c + 1);
            }
        }
        Map<String, Integer> want = new TreeMap<String, Integer>();
        want.put("L-135", 12);
        assertEquals(want, refused);
    }

    /**
     * Edges from the 26 sources with lower voice 0: 4,346 (HOLD) and 5,547
     * (REFLEXIVE); neither loses a strict edge or admits a fifth to a fifth.
     */
    public void testEdgeFigures() {
        int[] edges = new int[NEW_MODES.length];
        int[] lost = new int[NEW_MODES.length];
        int[] fifths = new int[NEW_MODES.length];
        for (State s : zeroSources()) {
            for (State t : M.states()) {
                boolean strict = M.isAdmitted(s, t, RelaxationMode.STRICT);
                for (int i = 0; i < NEW_MODES.length; i++) {
                    boolean in = M.isAdmitted(s, t, NEW_MODES[i]);
                    if (in) {
                        edges[i]++;
                        for (Pair p : Pair.values()) {
                            int[] z0 = ThreeVoiceCounterpoint.project(p, s);
                            int[] z1 = ThreeVoiceCounterpoint.project(p, t);
                            if (z0[1] == 7 && z1[1] == 7 && !Arrays.equals(z0, z1)) {
                                fifths[i]++;
                                break;
                            }
                        }
                    }
                    if (strict && !in) {
                        lost[i]++;
                    }
                }
            }
        }
        assertEquals(4346, edges[0]);
        assertEquals(5547, edges[1]);
        assertEquals(0, lost[0]);
        assertEquals(0, lost[1]);
        assertEquals(0, fifths[0]);
        assertEquals(0, fifths[1]);
    }

    /**
     * REFLEXIVE is V1 with held pairs skipped: it contains V1's 5,334 edges
     * and adds 213, each with a held pair. Of its 1,391 edges over strict,
     * 1,178 come from dropping the joint maximization and 213 from held notes.
     */
    public void testReflexiveIsV1PlusHeldPairs() {
        int v1 = 0;
        int extra = 0;
        int strict = 0;
        for (State s : zeroSources()) {
            for (State t : M.states()) {
                boolean a = M.isAdmittedV1(s, t);
                boolean r = M.isAdmitted(s, t, RelaxationMode.REFLEXIVE);
                if (M.isAdmitted(s, t, RelaxationMode.STRICT)) {
                    strict++;
                }
                if (a) {
                    v1++;
                    assertTrue("V1 ⊆ reflexive: " + s + " → " + t, r);
                }
                if (r && !a) {
                    extra++;
                    assertTrue("a reflexive edge outside V1 holds a pair: " + s + " → " + t, anyStill(s, t));
                }
                if (!anyStill(s, t)) {
                    assertEquals(s + " → " + t, a, r);
                }
            }
        }
        assertEquals(4156, strict);
        assertEquals(5334, v1);
        assertEquals(213, extra);
        assertEquals(1391, v1 + extra - strict);
        assertEquals(1178, v1 - strict);
    }

    /** HOLD and the strict relation agree on every step where no active pair stands still. */
    public void testHoldLeavesMovingStepsAlone() {
        for (State s : zeroSources()) {
            for (State t : M.states()) {
                if (!anyStill(s, t)) {
                    assertEquals(s + " → " + t, M.isAdmitted(s, t, RelaxationMode.STRICT),
                                 M.isAdmitted(s, t, RelaxationMode.HOLD));
                }
            }
        }
    }

    /**
     * HOLD and HOLD_FIBRE differ on 23 steps from the lower-voice-0 sources:
     * HOLD admits 2 that HOLD_FIBRE refuses and refuses 21 that it admits
     * (oracle, scratch/three-voice-relaxations/hold_vs_fibre.py).
     */
    public void testHoldAgainstHoldFibre() {
        List<String> holdOnly = new ArrayList<String>();
        int fibreOnly = 0;
        for (State s : zeroSources()) {
            for (State t : M.states()) {
                boolean h = M.isAdmitted(s, t, RelaxationMode.HOLD);
                boolean f = M.isAdmitted(s, t, RelaxationMode.HOLD_FIBRE);
                if (h && !f) {
                    holdOnly.add(s + " → " + t);
                }
                if (f && !h) {
                    fibreOnly++;
                }
            }
        }
        assertEquals(Arrays.asList(M.state(0, 0, 0) + " → " + M.state(3, 9, 9),
                                   M.state(0, 8, 8) + " → " + M.state(11, 9, 9)), holdOnly);
        assertEquals(21, fibreOnly);
    }

    /**
     * check() agrees with successors() and isAdmitted() in both new modes for
     * all 312 × 312 steps, and the reason counts over the lower-voice-0
     * sources match P02.
     */
    public void testCheckAgreesWithSuccessors() {
        Map<RelaxationMode, Map<Reason, Integer>> kinds = new HashMap<RelaxationMode, Map<Reason, Integer>>();
        Map<RelaxationMode, Integer> admitted = new HashMap<RelaxationMode, Integer>();
        for (RelaxationMode mode : NEW_MODES) {
            Map<Reason, Integer> k = new HashMap<Reason, Integer>();
            int adm = 0;
            for (State s : M.states()) {
                Set<State> succ = new HashSet<State>(M.successors(s, mode));
                for (State t : M.states()) {
                    Verdict v = M.check(s, t, mode);
                    boolean in = succ.contains(t);
                    assertEquals(mode + " " + s + " → " + t, in, v.isAdmitted());
                    assertEquals(in, M.isAdmitted(s, t, mode));
                    assertEquals(in, v.getRefusals().isEmpty());
                    if (s.getLower() == 0) {
                        if (in) {
                            adm++;
                        }
                        Set<Reason> seen = new HashSet<Reason>();
                        for (Refusal r : v.getRefusals()) {
                            seen.add(r.getReason());
                        }
                        for (Reason r : seen) {
                            Integer c = k.get(r);
                            k.put(r, c == null ? 1 : c + 1);
                        }
                    }
                }
            }
            kinds.put(mode, k);
            admitted.put(mode, adm);
        }
        Map<Reason, Integer> hold = new HashMap<Reason, Integer>();
        hold.put(Reason.FIFTH_TO_FIFTH, 429);
        hold.put(Reason.PAIR_EXCLUDED, 2214);
        hold.put(Reason.NOT_MAXIMAL, 1201);
        assertEquals(hold, kinds.get(RelaxationMode.HOLD));
        Map<Reason, Integer> refl = new HashMap<Reason, Integer>();
        refl.put(Reason.FIFTH_TO_FIFTH, 429);
        refl.put(Reason.PAIR_EXCLUDED, 2214);
        assertEquals(refl, kinds.get(RelaxationMode.REFLEXIVE));
        assertEquals(Integer.valueOf(4346), admitted.get(RelaxationMode.HOLD));
        assertEquals(Integer.valueOf(5547), admitted.get(RelaxationMode.REFLEXIVE));
    }

    private static List<String> reasons(Verdict v) {
        List<String> l = new ArrayList<String>();
        for (Refusal r : v.getRefusals()) {
            l.add(r.getReason() + (r.getPair() == null ? "" : ":" + r.getPair()));
        }
        return l;
    }

    /** The named steps of P02 in the new modes. */
    public void testReasons() {
        State ceg = M.fromPitchClasses(0, 4, 7);
        State cebg = M.fromPitchClasses(0, 3, 7);
        for (RelaxationMode mode : NEW_MODES) {
            assertTrue(mode.name(), M.check(ceg, cebg, mode).isAdmitted());
            assertTrue(mode.name(), M.check(ceg, ceg, mode).isAdmitted());
            assertEquals(mode.name(), Arrays.asList("PAIR_EXCLUDED:MU"),
                         reasons(M.check(cebg, M.fromPitchClasses(0, 3, 8), mode)));
            assertEquals(mode.name(), Arrays.asList("FIFTH_TO_FIFTH:LU"),
                         reasons(M.check(ceg, M.fromPitchClasses(2, 5, 9), mode)));
            assertEquals(mode.name(), Arrays.asList("FIFTH_TO_FIFTH:LU", "PAIR_EXCLUDED:MU"),
                         reasons(M.check(cebg, M.fromPitchClasses(2, 5, 9), mode)));
            assertEquals(mode.name(), Arrays.asList("TARGET_OUTSIDE_MASK"),
                         reasons(M.check(M.fromPitchClasses(2, 5, 9), M.fromPitchClasses(0, 4, 8), mode)));
        }
        State egc = M.fromPitchClasses(4, 7, 0);
        State ebgc = M.fromPitchClasses(3, 7, 0);
        assertEquals(Arrays.asList("NOT_MAXIMAL"), reasons(M.check(egc, ebgc, RelaxationMode.HOLD)));
        assertTrue(M.check(egc, ebgc, RelaxationMode.REFLEXIVE).isAdmitted());
    }
}
