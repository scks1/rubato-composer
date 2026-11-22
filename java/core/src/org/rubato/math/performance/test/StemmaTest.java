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
package org.rubato.math.performance.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.performance.LPS;
import org.rubato.math.performance.PerfNote;
import org.rubato.math.performance.PerformanceCell;
import org.rubato.math.performance.PerformedNote;
import org.rubato.math.performance.Stemma;
import org.rubato.math.performance.TempoCurve;
import org.rubato.math.performance.TempoCurves;

/**
 * Stemmata (Mazzola, <i>The Topos of Music II</i>, Defs. 106-107 p. 628, Example 52
 * p. 624, Fig. 38.5 p. 629) and the performance of their leaves.
 */
public class StemmaTest extends TestCase {

    /** Two voices, four bars of four quarters, one note per quarter. */
    private static List<PerfNote> twoHands() {
        List<PerfNote> notes = new ArrayList<PerfNote>();
        for (int q = 0; q < 16; q++) {
            notes.add(new PerfNote(q, 72 + q % 5, 80, 1, 0));
            if (q % 2 == 0) {
                notes.add(new PerfNote(q, 48 + q % 3, 70, 2, 1));
            }
        }
        return notes;
    }

    public void testMotherPerformsByEquation331() {
        TempoCurve t = TempoCurves.linear(120, -4);
        LPS mother = LPS.mother(twoHands(), t);
        Stemma s = new Stemma(mother);
        List<PerformedNote> p = s.perform();
        assertEquals(24, p.size());
        for (PerformedNote n : p) {
            PerfNote k = n.getSymbolic();
            assertEquals("onset", 60 * t.integral(0, k.onset), n.onset, 1e-12);
            assertEquals("duration", 60 * t.integral(k.onset, k.onset + k.duration), n.duration, 1e-12);
            assertEquals("loudness unchanged", k.loudness, n.loudness, 1e-12);
        }
        for (PerformedNote n : new Stemma(LPS.mother(twoHands(), TempoCurves.constant(120))).perform()) {
            assertEquals("constant tempo gives linear onsets", 0.5 * n.getSymbolic().onset, n.onset, 1e-12);
        }
    }

    /** The closed-form articulation map used by the stemma agrees with RK4 on the field (33.9). */
    public void testStemmaAgreesWithFieldPerformance() {
        TempoCurve t = TempoCurves.sine(100, 30);
        LPS mother = LPS.mother(twoHands(), t);
        PerformanceCell cell = PerformanceCell.articulation(t, 0, 0, 0);
        for (PerformedNote n : new Stemma(mother).perform()) {
            PerfNote k = n.getSymbolic();
            double[] v = cell.perform(new double[] { k.onset, k.duration }).value();
            assertEquals(60 * v[0], n.onset, 1e-7);
            assertEquals(60 * v[1], n.duration, 1e-7);
        }
    }

    public void testSplitByVoicePartitions() {
        LPS mother = LPS.mother(twoHands(), TempoCurves.constant(100));
        List<LPS> hands = mother.splitByVoice();
        assertEquals(2, hands.size());
        assertEquals(16, hands.get(0).getKernel().size());
        assertEquals(8, hands.get(1).getKernel().size());
        assertSame(mother, hands.get(0).getMother());
        assertEquals(2, mother.getDaughters().size());
        List<LPS> parts = hands.get(0).splitByOnset(new double[] { 8 });
        assertEquals(8, parts.get(0).getKernel().size());
        assertEquals(8, parts.get(1).getKernel().size());
        new Stemma(mother).validate();
        assertEquals(24, new Stemma(mother).perform().size());
    }

    /**
     * Example 52: the right hand plays a per-bar rubato whose every bar lasts as long as the
     * mother's bar, so both hands meet at each bar line; within the bar the onsets move.
     */
    public void testExample52BarRubato() {
        TempoCurve t = TempoCurves.linear(110, -2);
        LPS mother = LPS.mother(twoHands(), t);
        List<LPS> hands = mother.splitByVoice();
        LPS right = hands.get(0).barRubato(new double[] { 0, 4, 8, 12, 16 }, new double[] { 1.25, 0.8, 1.1 });
        Stemma s = new Stemma(mother);
        s.validate();
        TempoCurve rt = right.getTempo();
        for (int bar = 0; bar < 4; bar++) {
            assertEquals("bar " + bar + " duration", t.integral(4 * bar, 4 * bar + 4), rt.integral(4 * bar, 4 * bar + 4), 1e-10);
            assertEquals("bar line tempo", t.tempo(4 * bar), rt.tempo(4 * bar), 1e-12);
        }
        List<PerformedNote> p = s.perform();
        double maxShift = 0;
        for (PerformedNote n : p) {
            PerfNote k = n.getSymbolic();
            double motherOnset = 60 * t.integral(0, k.onset);
            if (k.onset % 4 == 0) {
                assertEquals("bar line " + k.onset + " voice " + k.voice, motherOnset, n.onset, 1e-8);
            }
            maxShift = Math.max(maxShift, Math.abs(n.onset - motherOnset));
        }
        assertTrue("the rubato moves inner onsets: " + maxShift, maxShift > 0.01);
        try {
            hands.get(1).barRubato(new double[] { 0, 4 }, new double[0]);
            fail("a rubato profile without inner vertices was accepted (Cor. 21 needs f >= 2)");
        }
        catch (IllegalArgumentException expected) { /* E3 */ }
    }

    public void testTempoAndLoudnessOperators() {
        TempoCurve t = TempoCurves.constant(90);
        LPS mother = LPS.mother(twoHands(), t);
        LPS slower = mother.tempoOperator(x -> 0.5, "half tempo");
        PerfNote k = slower.getKernel().get(3);
        List<PerformedNote> p = new Stemma(mother).perform();
        PerformedNote n = find(p, k);
        assertEquals(60 * k.onset / 45, n.onset, 1e-10);
        LPS louder = slower.loudnessOperator(x -> 0.1 * x, 1.0, 0.0, "crescendo");
        p = new Stemma(mother).perform();
        n = find(p, k);
        assertEquals(Math.exp(0.1 * k.onset) * k.loudness, n.loudness, 1e-10);
        LPS again = louder.loudnessOperator(x -> 0.05, 1.0, 0.0, "plus");
        n = find(new Stemma(mother).perform(), k);
        assertEquals("Lemma 53 (ii): weights add", Math.exp(0.1 * k.onset + 0.05) * k.loudness, n.loudness, 1e-10);
        assertEquals(3, new Stemma(mother).depth());
        assertSame(again, new Stemma(mother).leaves().get(0));
    }

    public void testDurationPreservingTempoOperator() {
        TempoCurve t = TempoCurves.constant(100);
        LPS mother = LPS.mother(twoHands(), t);
        LPS d = mother.tempoOperatorPreserving(x -> 1 + 0.4 * Math.sin(x), 4, 12, 1, 1e-12, 50, "wave");
        assertTrue(d.getPreservation().isConverged());
        assertEquals(t.integral(4, 12), d.getTempo().integral(4, 12), 1e-10);
        for (PerformedNote n : new Stemma(mother).perform()) {
            if (n.getSymbolic().onset >= 12 || n.getSymbolic().onset <= 4) {
                assertEquals("outside the interval nothing moves", 60 * t.integral(0, n.getSymbolic().onset), n.onset, 1e-8);
            }
        }
    }

    private static PerformedNote find(List<PerformedNote> p, PerfNote k) {
        for (PerformedNote n : p) {
            if (n.getSymbolic() == k) {
                return n;
            }
        }
        throw new AssertionError("note not performed");
    }

    /** Def. 107: tree, no self-daughter, no node reached twice, leaves. */
    public void testDefinition107Validation() {
        LPS m = LPS.mother(twoHands(), TempoCurves.constant(100));
        List<LPS> h = m.splitByVoice();
        new Stemma(m).validate();
        h.get(0).addDaughter(m);
        assertInvalid(m, "cycle");
        m = LPS.mother(twoHands(), TempoCurves.constant(100));
        m.addDaughter(m);
        assertInvalid(m, "self");
        m = LPS.mother(twoHands(), TempoCurves.constant(100));
        h = m.splitByVoice();
        h.get(0).addDaughter(h.get(1));
        assertInvalid(m, "shared daughter");
        m = LPS.mother(twoHands(), TempoCurves.constant(100));
        m.splitByVoice();
        new Stemma(m).validate();
    }

    private static void assertInvalid(LPS m, String what) {
        try {
            new Stemma(m).validate();
            fail(what + " accepted");
        }
        catch (IllegalStateException expected) { /* Def. 107 */ }
    }

    /**
     * Fig. 38.5 (p. 629): the stemma of the 1996 performance. Mother; Omega1 separates the
     * hands; Omega2-Omega4 refine each hand; Omega5 splits each hand in two; Omega6, Omega7
     * refine the four parts. 21 nodes, 20 edges, 4 leaves, 7 operator layers.
     */
    public void testFig385Counts() {
        LPS m = LPS.mother(twoHands(), TempoCurves.constant(100));
        List<LPS> hands = m.splitByVoice();
        List<LPS> leaves = new ArrayList<LPS>();
        for (LPS hand : hands) {
            LPS x = hand;
            for (int layer = 2; layer <= 4; layer++) {
                x = x.tempoOperator(e -> 1.0, "Omega" + layer);
            }
            for (LPS part : x.splitByOnset(new double[] { 8 })) {
                LPS y = part;
                for (int layer = 6; layer <= 7; layer++) {
                    y = y.loudnessOperator(e -> 0.0, 1, 0, "Omega" + layer);
                }
                leaves.add(y);
            }
        }
        Stemma s = new Stemma(m);
        s.validate();
        assertEquals("nodes", 21, s.size());
        assertEquals("edges", 20, s.edges());
        assertEquals("leaves", 4, s.leaves().size());
        assertEquals("operator layers", 7, s.depth());
        assertEquals(leaves, s.leaves());
        assertEquals(24, s.perform().size());
    }

    /** The performed piece needs leaves whose kernels partition the mother's kernel. */
    public void testLeavesMustPartitionTheMother() {
        LPS m = LPS.mother(twoHands(), TempoCurves.constant(100));
        List<LPS> h = m.splitByVoice();
        h.get(1).addDaughter(LPS.mother(h.get(0).getKernel(), TempoCurves.constant(50)));
        try {
            new Stemma(m).perform();
            fail("overlapping leaves performed");
        }
        catch (IllegalStateException expected) { /* departure D9 */ }
    }
}
