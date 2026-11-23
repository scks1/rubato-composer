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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.counterpoint.ZnAffine;
import org.rubato.math.global.Interpretations;
import org.rubato.math.harmony.Cadences;
import org.rubato.math.harmony.Modulations;
import org.rubato.math.harmony.Modulations.Filter;
import org.rubato.math.harmony.Modulations.Rigidity;
import org.rubato.math.harmony.PitchClasses;
import org.rubato.math.harmony.Quantized;
import org.rubato.math.harmony.Tonality;

/**
 * Modulators, quanta and pivots (Mazzola, <i>The Topos of Music I</i>,
 * &sect;27.1.4, Lemma 43, Def. 88 and Thm. 30, pp. 469&ndash;471; Topos IV,
 * App. P.1.1&ndash;P.1.2, pp. 1525&ndash;1527; Agust&iacute;n-Aquino and
 * Mazzola, J. Math. Music 14, 2020, Algorithm 1 and Table 1, PDF pp. 6&ndash;7;
 * Agust&iacute;n-Aquino, Blues for Alice, arXiv 2601.07161, &sect;7).
 */
public class ModulationTest extends TestCase {

    static final Tonality C3 = Tonality.of(Interpretations.C_MAJOR, 0, 3);
    static final Tonality C4 = Tonality.of(Interpretations.C_MAJOR, 0, 4);

    static ZnAffine tr(int t) { return new ZnAffine(t, 1, 12); }

    static ZnAffine inv(int t) { return new ZnAffine(t, -1, 12); }

    static int glyphs(String s) {
        int m = 0;
        for (int i = 0; i < 12; i++) {
            if (s.charAt(i) == 'x') {
                m |= 1 << i;
            }
        }
        return m;
    }

    static int pcs(String csv) {
        int m = 0;
        for (String x : csv.split(",")) {
            m |= 1 << Integer.parseInt(x.trim());
        }
        return m;
    }

    static String key(Quantized q) {
        return q.p() + " " + Cadences.name(q.cadence()) + " " + PitchClasses.toString(q.quantum()) + " "
               + Modulations.name(q.modulator()) + " " + Cadences.name(q.pivots()) + (q.covered() ? "" : "*");
    }

    static List<String> keys(List<Quantized> qs) {
        List<String> out = new ArrayList<String>();
        for (Quantized q : qs) {
            out.add(key(q));
        }
        Collections.sort(out);
        return out;
    }

    // ---- modulators

    /** Lemma 43: modulators are translations or inversions with g(S) = T. */
    public void testModulatorsOfMajor() {
        for (int p = 1; p < 12; p++) {
            List<ZnAffine> g = Modulations.modulators(C3, C3.transpose(p));
            assertEquals(2, g.size());
            assertTrue(g.contains(tr(p)));
            for (ZnAffine h : g) {
                assertEquals(PitchClasses.transpose(C3.scaleMask(), p), PitchClasses.apply(h, C3.scaleMask()));
            }
        }
        assertEquals(Arrays.asList(tr(7), inv(11)), Modulations.modulators(C3, C3.transpose(7)));
        assertEquals("T9∘−1", Modulations.name(inv(9)));
        assertEquals("T3", Modulations.name(tr(3)));
        assertEquals(24, Modulations.ti12().size());
    }

    /** Harmonic minor is rigid: its only modulator to e^p.S is e^p. */
    public void testModulatorsOfARigidScale() {
        Tonality h = Tonality.of(Interpretations.C_HARMONIC_MINOR, 0, 3);
        for (int p = 1; p < 12; p++) {
            assertEquals(Arrays.asList(tr(p)), Modulations.modulators(h, h.transpose(p)));
        }
    }

    // ---- triadic major, Topos IV App. P.1.2 (p. 1527)

    /**
     * The 26 rows of P.1.2: p, cadence, quantum as twelve glyphs from class 0,
     * modulator, pivots. In the p = 4 block the book's pivot cells are shifted
     * by one row against the cadences; the three p = 4 rows below carry the
     * pivots of their own quantum (checked by hand, see HARMONY-SPEC.md 7.6).
     */
    static final String[][] P12 = {
        { "1", "II,V", "x.xx.xxxxxxx", "T5∘−1", "II,III,V,VII" },
        { "1", "II,III", "x.xx.xxxxxxx", "T5∘−1", "II,III,V,VII" },
        { "2", "VII", ".xx.xx.x...x", "T6∘−1", "II,IV,VII" },
        { "2", "II,V", ".xx.xx.x.x.x", "T6∘−1", "II,IV,V,VII" },
        { "2", "IV,V", ".xx.xx.x.x.x", "T6∘−1", "II,IV,V,VII" },
        { "3", "II,V", "x.x..x.xxxxx", "T7∘−1", "II,III,V,VII" },
        { "3", "II,III", "x.x..x.xxxxx", "T7∘−1", "II,III,V,VII" },
        { "4", "VII", "..xx.xx..x.x", "T8∘−1", "V,VII" },
        { "4", "IV,V", ".xxxxxxx.x.x", "T8∘−1", "II,IV,V,VII" },
        { "4", "II,III", "xxxx.xxxxx.x", "T8∘−1", "II,III,V,VII" },
        { "5", "VII", "..x.xx.x..xx", "T9∘−1", "II,IV,VII" },
        { "6", "II,III", ".xxxxx.xxxxx", "T6", "II,III,V,VII" },
        { "6", "IV,V", ".xxxxxxxxx.x", "T10∘−1", "II,IV,V,VII" },
        { "6", "IV,V", "xxxx.xxxxx.x", "T6", "II,IV,V,VII" },
        { "6", "II,III", "xxxx.x.xxxxx", "T10∘−1", "II,III,V,VII" },
        { "7", "VII", "x.x..xx..x.x", "T11∘−1", "III,V,VII" },
        { "8", "VII", ".xx..x.x..xx", "T0∘−1", "II,VII" },
        { "8", "IV,V", ".xxxxx.xxxxx", "T0∘−1", "II,IV,V,VII" },
        { "8", "II,III", "xxxx.x.x.xxx", "T0∘−1", "II,III,V,VII" },
        { "9", "II,V", "..x.xxxxxx.x", "T1∘−1", "II,IV,V,VII" },
        { "9", "IV,V", "..x.xxxxxx.x", "T1∘−1", "II,IV,V,VII" },
        { "10", "VII", "x.xx.x...x.x", "T2∘−1", "III,V,VII" },
        { "10", "II,V", "x.xx.x.x.x.x", "T2∘−1", "II,III,V,VII" },
        { "10", "II,III", "x.xx.x.x.x.x", "T2∘−1", "II,III,V,VII" },
        { "11", "II,V", ".xx.xxxxxxxx", "T3∘−1", "II,IV,V,VII" },
        { "11", "IV,V", ".xx.xxxxxxxx", "T3∘−1", "II,IV,V,VII" },
    };

    public void testTriadicMajorModulationsAreP12() {
        List<String> want = new ArrayList<String>();
        for (String[] r : P12) {
            want.add(r[0] + " {" + r[1] + "} " + PitchClasses.toString(glyphs(r[2])) + " " + r[3] + " {" + r[4] + "}");
        }
        Collections.sort(want);
        assertEquals(want, keys(Modulations.all(C3, Filter.COVERED, Rigidity.TI12)));
    }

    /** Rows per p in P.1.2: 2, 3, 2, 3, 1, 4, 1, 3, 2, 3, 2; 20 distinct (p, Q). */
    public void testTriadicMajorCounts() {
        List<Quantized> all = Modulations.all(C3, Filter.COVERED, Rigidity.TI12);
        int[] perP = new int[12];
        Set<String> pq = new TreeSet<String>();
        for (Quantized q : all) {
            perP[q.p()]++;
            pq.add(q.p() + ":" + q.quantum());
        }
        assertTrue(Arrays.equals(new int[] { 0, 2, 3, 2, 3, 1, 4, 1, 3, 2, 3, 2 }, perP));
        assertEquals(20, pq.size());
        // Major has an inversion symmetry, so every TI map sends X to itself
        // and the two rigidity readings coincide.
        assertEquals(keys(all), keys(Modulations.all(C3, Filter.COVERED, Rigidity.AUT_X)));
    }

    /** Schoenberg C to F: pivots {II, IV, VII} with cadence {VII} (Topos I p. 471; P.1.2 p = 5). */
    public void testSchoenbergCToF() {
        List<Quantized> q = Modulations.quantize(C3, C3.transpose(5), Filter.COVERED, Rigidity.TI12);
        assertEquals(1, q.size());
        assertEquals(Cadences.parse("VII"), q.get(0).cadence());
        assertEquals(Cadences.parse("II,IV,VII"), q.get(0).pivots());
        assertEquals(inv(9), q.get(0).modulator());
        assertEquals(PitchClasses.of(2, 4, 5, 7, 10, 11), q.get(0).quantum());
    }

    /** Def. 88 (1), (2) and (4): Q is the smallest g-invariant set containing the cadence. */
    public void testQuantumIsTheMinimalInvariantSet() {
        for (Quantized q : Modulations.all(C3, Filter.COVERED, Rigidity.TI12)) {
            Tonality t = C3.transpose(q.p());
            int cad = 0;
            for (int c : t.chordsOf(q.cadence())) {
                cad |= c;
            }
            assertEquals(q.quantum(), PitchClasses.apply(q.modulator(), q.quantum()));
            assertTrue(PitchClasses.isSubset(cad, q.quantum()));
            for (int sub = (q.quantum() - 1) & q.quantum(); sub != 0; sub = (sub - 1) & q.quantum()) {
                boolean invariant = PitchClasses.apply(q.modulator(), sub) == sub;
                assertFalse(key(q), invariant && PitchClasses.isSubset(cad, sub));
            }
            assertEquals(q.quantum() & t.scaleMask(), q.trace());
            assertTrue(Modulations.isRigid(q.trace(), Modulations.ti12()));
        }
    }

    // ---- tetradic major, AM 2020 Table 1 (PDF p. 7)

    /** Table 1: Tr, cadence, quantum, modulator, pivots, asterisk. */
    static final String[][] TABLE1 = {
        { "1", "V", "0,2,3,5,6,8,9,11", "T5∘−1", "III,V", "" },
        { "1", "VII", "0,2,3,5,6,7,10,11", "T5∘−1", "VII", "*" },
        { "2", "V", "1,2,4,5,7,9,11", "T6∘−1", "II,V,VII", "" },
        { "2", "VII", "1,2,4,5,7,11", "T6∘−1", "II,VII", "" },
        { "3", "V", "1,2,4,5,7,8,10,11", "T3", "III,V", "" },
        { "3", "V", "2,5,8,9,10,11", "T7∘−1", "V", "" },
        { "3", "VII", "0,2,5,7,8,11", "T7∘−1", "VII", "*" },
        { "4", "V", "2,3,5,6,9,11", "T8∘−1", "V", "" },
        { "5", "VII", "2,4,5,7,10,11", "T9∘−1", "II,VII", "" },
        { "6", "V", "1,2,5,7,8,11", "T6", "V", "" },
        { "6", "V", "1,2,5,8,9,11", "T10∘−1", "V", "" },
        { "6", "VII", "2,3,5,8,9,11", "T6", "VII", "" },
        { "6", "VII", "2,3,5,7,8,11", "T10∘−1", "VII", "" },
        { "7", "V", "0,2,5,6,9,11", "T11∘−1", "III,V", "" },
        { "8", "VII", "1,2,5,7,10,11", "T0∘−1", "VII", "" },
        { "9", "V", "1,2,4,5,7,8,10,11", "T9", "III,V", "" },
        { "9", "V", "2,4,5,8,9,11", "T1∘−1", "V", "*" },
        { "9", "VII", "2,5,6,7,8,11", "T1∘−1", "VII", "" },
        { "10", "V", "0,2,3,5,9,11", "T2∘−1", "III,V", "" },
        { "10", "VII", "0,2,3,5,7,9,11", "T2∘−1", "III,V,VII", "" },
        { "11", "V", "1,2,4,5,6,9,10,11", "T3∘−1", "V", "*" },
        { "11", "VII", "1,2,4,5,7,8,10,11", "T3∘−1", "II,VII", "" },
    };

    static List<String> table1() {
        List<String> out = new ArrayList<String>();
        for (String[] r : TABLE1) {
            out.add(r[0] + " {" + r[1] + "} " + PitchClasses.toString(pcs(r[2])) + " " + r[3] + " {" + r[4] + "}" + r[5]);
        }
        return out;
    }

    /**
     * Algorithm 1 run as printed gives Table 1's 22 rows and two more,
     * (Tr3, {VII7}, T3) and (Tr9, {VII7}, T9), quantum {0,2,3,5,6,8,9,11},
     * pivots {II7, VII7}, covered. HARMONY-SPEC.md 7.1 records the finding.
     */
    public void testTetradicMajorIsTable1PlusTwoRows() {
        List<String> got = keys(Modulations.all(C4, Filter.ALGORITHM_1, Rigidity.TI12));
        List<String> want = table1();
        want.add("3 {VII} {0,2,3,5,6,8,9,11} T3 {II,VII}");
        want.add("9 {VII} {0,2,3,5,6,8,9,11} T9 {II,VII}");
        Collections.sort(want);
        assertEquals(want, got);
        assertTrue(got.containsAll(table1()));
    }

    /** The two extra rows: the trace is rigid under all 23 non-identity TI maps and covered. */
    public void testTheTwoOmittedRowsSatisfyDef88() {
        for (int p : new int[] { 3, 9 }) {
            List<Quantized> qs = Modulations.quantize(C4, C4.transpose(p), Filter.COVERED, Rigidity.TI12);
            Quantized extra = null;
            for (Quantized q : qs) {
                if (q.cadence() == Cadences.parse("VII") && q.modulator().equals(tr(p))) {
                    extra = q;
                }
            }
            assertNotNull("Tr" + p, extra);
            assertEquals(PitchClasses.of(0, 2, 3, 5, 6, 8, 9, 11), extra.quantum());
            assertTrue(extra.covered());
            for (ZnAffine h : Modulations.ti12()) {
                if (!h.equals(tr(0))) {
                    assertTrue(PitchClasses.apply(h, extra.trace()) != extra.trace());
                }
            }
        }
        Tonality eFlat = C4.transpose(3);
        assertEquals(PitchClasses.of(0, 2, 3, 5, 8), PitchClasses.of(0, 2, 3, 5, 6, 8, 9, 11) & eFlat.scaleMask());
        // The other modulator to E-flat, x -> 7 - x, does not fix this quantum,
        // so the quantum fixes its modulator (Topos I p. 470).
        assertTrue(Modulations.modulators(C4, eFlat).contains(inv(7)));
        int q = PitchClasses.of(0, 2, 3, 5, 6, 8, 9, 11);
        assertTrue(PitchClasses.apply(inv(7), q) != q);
    }

    /** Insight (1): all quantized tetradic modulations come from {V7} and {VII7}. */
    public void testOnlyV7AndVII7Quantize() {
        for (Quantized q : Modulations.all(C4, Filter.ALGORITHM_1, Rigidity.TI12)) {
            assertTrue(q.cadence() == Cadences.parse("V") || q.cadence() == Cadences.parse("VII"));
        }
    }

    /** Insight (2): the asterisk rows are those whose pivots do not cover T &cap; Q. */
    public void testCoveringDropsExactlyTheAsteriskRows() {
        List<String> covered = keys(Modulations.all(C4, Filter.COVERED, Rigidity.TI12));
        assertEquals(20, covered.size());
        for (String s : covered) {
            assertFalse(s, s.endsWith("*"));
        }
    }

    /**
     * Insight (3): the octatonic quanta. (T3&#8728;&minus;1, {V7}) at Tr11 is not
     * one; (T3&#8728;&minus;1, {VII7}) is (HARMONY-SPEC.md 7.2).
     */
    public void testOctatonicQuanta() {
        int oct1 = PitchClasses.of(0, 2, 3, 5, 6, 8, 9, 11);
        int oct2 = PitchClasses.transpose(oct1, 1);
        int oct3 = PitchClasses.transpose(oct1, 2);
        List<String> octatonic = new ArrayList<String>();
        for (Quantized q : Modulations.all(C4, Filter.ALGORITHM_1, Rigidity.TI12)) {
            if (q.quantum() == oct1 || q.quantum() == oct2 || q.quantum() == oct3) {
                octatonic.add(q.p() + " " + Modulations.name(q.modulator()) + " " + Cadences.name(q.cadence()));
            }
        }
        Collections.sort(octatonic);
        List<String> want = new ArrayList<String>(Arrays.asList("1 T5∘−1 {V}", "11 T3∘−1 {VII}", "3 T3 {V}",
                                                                "3 T3 {VII}", "9 T9 {V}", "9 T9 {VII}"));
        Collections.sort(want);
        assertEquals(want, octatonic);
    }

    /** Insight (5): chaining at Tr2 {V7}; the pivots are {II7, V7, VII7}. */
    public void testChainingPivots() {
        for (Quantized q : Modulations.quantize(C4, C4.transpose(2), Filter.ALGORITHM_1, Rigidity.TI12)) {
            if (q.cadence() == Cadences.parse("V")) {
                assertEquals(Cadences.parse("II,V,VII"), q.pivots());
            }
        }
    }

    /** Insight (4): at a tritone each pivot is the tritone substitute of the same source degree. */
    public void testTritonePivotsAreSubstitutes() {
        Tonality t = C4.transpose(6);
        for (Quantized q : Modulations.quantize(C4, t, Filter.ALGORITHM_1, Rigidity.TI12)) {
            for (int d = 0; d < 7; d++) {
                if ((q.pivots() & (1 << d)) != 0) {
                    assertEquals(PitchClasses.transpose(C4.degree(d), 6), t.degree(d));
                }
            }
        }
    }

    /**
     * BfA &sect;7: Cherokee B&flat; to E&flat; (Tr5) has pivots II7 and VII7; E&flat; to
     * B&flat; (Tr7) has III7 and V7; keys a fifth or a major third apart have
     * exactly one quantized modulation.
     */
    public void testCherokeeDistances() {
        Tonality bFlat = C4.transpose(10);
        List<Quantized> there = Modulations.quantize(bFlat, bFlat.transpose(5), Filter.ALGORITHM_1, Rigidity.TI12);
        assertEquals(1, there.size());
        assertEquals(Cadences.parse("II,VII"), there.get(0).pivots());
        // The paper's T9∘−1 is Table 1's name, relative to C; conjugated by
        // e^10 it is x -> 5 - x on absolute pitch classes.
        assertEquals(inv(5), there.get(0).modulator());
        Tonality eFlat = C4.transpose(3);
        List<Quantized> back = Modulations.quantize(eFlat, eFlat.transpose(7), Filter.ALGORITHM_1, Rigidity.TI12);
        assertEquals(1, back.size());
        assertEquals(Cadences.parse("III,V"), back.get(0).pivots());
        Map<Integer, Integer> perP = new HashMap<Integer, Integer>();
        for (Quantized q : Modulations.all(C4, Filter.ALGORITHM_1, Rigidity.TI12)) {
            perP.merge(q.p(), 1, Integer::sum);
        }
        for (int p = 1; p < 12; p++) {
            boolean single = perP.get(p) == 1;
            assertEquals("Tr" + p, p == 4 || p == 5 || p == 7 || p == 8, single);
        }
    }

    /** BfA &sect;7.2: the Cherokee bridge B to A (Tr10) does quantize, with {V7} and {VII7}. */
    public void testBridgeDistanceHasQuantizedModulations() {
        Tonality b = C4.transpose(11);
        List<Quantized> qs = Modulations.quantize(b, b.transpose(10), Filter.ALGORITHM_1, Rigidity.TI12);
        List<Integer> cads = new ArrayList<Integer>();
        for (Quantized q : qs) {
            cads.add(q.cadence());
        }
        Collections.sort(cads);
        assertEquals(Arrays.asList(Cadences.parse("V"), Cadences.parse("VII")), cads);
        assertFalse(cads.contains(Cadences.parse("I,II")));
    }

    // ---- all 38 orbits, Topos IV App. P.1.1 and Topos I Thm. 30

    static int[] counts(Tonality s, Rigidity r) {
        List<Quantized> all = Modulations.all(s, Filter.COVERED, r);
        Set<String> pq = new TreeSet<String>();
        boolean[] seen = new boolean[12];
        for (Quantized q : all) {
            pq.add(q.p() + ":" + q.quantum());
            seen[q.p()] = true;
        }
        int missing = 0;
        for (int p = 1; p < 12; p++) {
            if (!seen[p]) {
                missing |= 1 << p;
            }
        }
        return new int[] { pq.size(), all.size(), missing };
    }

    /** 28 rigid orbits and 10 with an inversion symmetry (Thm. 30). */
    public void testRigidAndNonRigidOrbits() {
        int rigid = 0;
        for (String[] row : ToposP11.ROWS) {
            int s = PitchClasses.of(ToposP11.scale(row));
            if (Modulations.isRigid(s, Modulations.ti12())) {
                rigid++;
            }
        }
        assertEquals(28, rigid);
    }

    /**
     * Literal Def. 88 (rigidity under all of TI12) against P.1.1: the counts
     * agree for 38.1, 52, 55 and 59 only. Testing rigidity under the maps
     * that preserve X agrees for 28 orbits, including Thm. 30's 226 at 54.1
     * and 53 at 41.1 (HARMONY-SPEC.md 7.4). The test pins which orbits agree;
     * it does not tune either rule.
     */
    public void testP11AgreementUnderBothRigidityReadings() {
        List<String> agreeTi = new ArrayList<String>();
        List<String> disagreeAut = new ArrayList<String>();
        for (String[] row : ToposP11.ROWS) {
            int[] sc = ToposP11.scale(row);
            Tonality s = Tonality.of(sc, sc[0], 3);
            int[] ti = counts(s, Rigidity.TI12);
            int[] aut = counts(s, Rigidity.AUT_X);
            boolean bang = !row[5].isEmpty();
            if (ti[0] == Integer.parseInt(row[3]) && ti[1] == Integer.parseInt(row[4]) && (ti[2] != 0) == bang) {
                agreeTi.add(row[0]);
            }
            if (!(aut[0] == Integer.parseInt(row[3]) && aut[1] == Integer.parseInt(row[4]) && (aut[2] != 0) == bang)) {
                disagreeAut.add(row[0]);
            }
        }
        assertEquals(Arrays.asList("38.1", "52", "55", "59"), agreeTi);
        assertEquals(Arrays.asList("38", "40.1", "43.1", "47", "47.1", "49.1", "50", "50.1", "61", "62"), disagreeAut);
    }

    /** Thm. 30 numbers under each reading: 54.1, 41.1, 47.1, 38.1. */
    public void testTheorem30Numbers() {
        assertEquals(226, counts(orbit("54.1"), Rigidity.AUT_X)[1]);
        assertEquals(222, counts(orbit("54.1"), Rigidity.TI12)[1]);
        assertEquals(53, counts(orbit("41.1"), Rigidity.AUT_X)[1]);
        assertEquals(49, counts(orbit("41.1"), Rigidity.TI12)[1]);
        assertEquals(118, counts(orbit("47.1"), Rigidity.TI12)[1]);
        assertEquals(26, counts(orbit("38.1"), Rigidity.TI12)[1]);
    }

    /**
     * Thm. 30's non-rigid exceptions: computed, 38 lacks p = 1, 11 and 52, 55
     * lack p = 5, 7, the reverse of the text; 62 lacks none (HARMONY-SPEC.md 7.5).
     */
    public void testNonRigidExceptions() {
        int p1p11 = (1 << 1) | (1 << 11), p5p7 = (1 << 5) | (1 << 7);
        assertEquals(p1p11, counts(orbit("38"), Rigidity.TI12)[2]);
        assertEquals(p5p7, counts(orbit("52"), Rigidity.TI12)[2]);
        assertEquals(p5p7, counts(orbit("55"), Rigidity.TI12)[2]);
        assertEquals(0, counts(orbit("62"), Rigidity.TI12)[2]);
    }

    static Tonality orbit(String name) {
        for (String[] row : ToposP11.ROWS) {
            if (row[0].equals(name)) {
                int[] sc = ToposP11.scale(row);
                return Tonality.of(sc, sc[0], 3);
            }
        }
        throw new IllegalArgumentException(name);
    }

    /**
     * Simon's decision of 2026-11-23: AUT_X is the default reading. Against
     * Topos IV App. P.1.1 it matches 28 of 38 orbits, TI12 4 of 38
     * (HARMONY-SPEC 7.4, testP11Counts).
     */
    public void testDefaultRigidityIsAutX() {
        assertEquals(Rigidity.AUT_X, Modulations.DEFAULT_RIGIDITY);
    }

    /**
     * The major scale has an inversion symmetry, so AUT_X is all of TI12 for
     * every major tonality, and the default does not change major-key results.
     */
    public void testAutXIsTi12ForMajorKeys() {
        for (int tonic = 0; tonic < 12; tonic++) {
            assertEquals(24, Modulations.autX(C3.transpose(tonic)).size());
            assertEquals(24, Modulations.autX(C4.transpose(tonic)).size());
        }
        assertEquals(keys(Modulations.all(C4, Filter.ALGORITHM_1, Rigidity.TI12)),
                keys(Modulations.all(C4, Filter.ALGORITHM_1, Rigidity.AUT_X)));
    }
}
