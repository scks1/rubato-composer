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
package org.rubato.rubettes.performance.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.performance.Performances;
import org.rubato.rubettes.performance.ScalarPerformanceRubette;
import org.rubato.rubettes.performance.TempoPerformanceRubette;

/**
 * The headless performance API and its two rubettes. Performed Scores count quarters at
 * the reference tempo 120 (departure D2), so a performed onset in seconds s is 2 s.
 */
public class PerformancesTest extends TestCase {

    private static List<ScoreNote> piece() {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int q = 0; q < 16; q++) {
            notes.add(new ScoreNote(q, 72 + q % 5, 80, 1, 0));
            if (q % 2 == 0) {
                notes.add(new ScoreNote(q, 48 + q % 3, 70, 2, 1));
            }
        }
        return notes;
    }

    private static ScoreNote at(List<ScoreNote> notes, double onset, int voice, List<ScoreNote> performed) {
        int i = 0;
        for (ScoreNote n : notes) {
            if (n.onset == onset && n.voice == voice) {
                return performed.get(i);
            }
            i++;
        }
        throw new AssertionError("no note at " + onset);
    }

    public void testConstantTempoIsLinear() {
        Performances.Settings s = Performances.Settings.tempo(60, 60, Performances.Curve.LINEAR, 0, 0, -1);
        Performances.Result r = Performances.perform(piece(), s);
        List<ScoreNote> in = piece();
        for (int i = 0; i < in.size(); i++) {
            assertEquals("onset", 2 * in.get(i).onset, r.getNotesInInputOrder().get(i).onset, 1e-12);
            assertEquals("duration", 2 * in.get(i).duration, r.getNotesInInputOrder().get(i).duration, 1e-12);
            assertEquals(in.get(i).pitch, r.getNotesInInputOrder().get(i).pitch);
            assertEquals(in.get(i).loudness, r.getNotesInInputOrder().get(i).loudness);
        }
        assertEquals("16 quarters at M.M. 60 + the last note", 16.0, r.getPhysicalDuration(), 1e-12);
    }

    /** A linear ritardando T(E) = 120 - 60 E/16 over [0, 16] gives logarithmic onsets. */
    public void testLinearRitardandoIsLogarithmic() {
        Performances.Settings s = Performances.Settings.tempo(120, 60, Performances.Curve.LINEAR, 0, 0, -1);
        Performances.Result r = Performances.perform(piece(), s);
        double q1 = -60.0 / 16;
        List<ScoreNote> in = piece();
        for (int i = 0; i < in.size(); i++) {
            double e = in.get(i).onset;
            double seconds = 60 / q1 * Math.log((120 + q1 * e) / 120);
            assertEquals("onset " + e, 2 * seconds, r.getNotesInInputOrder().get(i).onset, 1e-10);
        }
        assertEquals(60 / q1 * Math.log(60.0 / 120), r.getPhysicalDuration(), 1e-10);
    }

    public void testExponentialTempo() {
        Performances.Settings s = Performances.Settings.tempo(100, 50, Performances.Curve.EXPONENTIAL, 0, 0, -1);
        Performances.Result r = Performances.perform(piece(), s);
        double k = Math.log(0.5) / 16;
        double e = 9;
        double seconds = 60 * (1 - Math.exp(-k * e)) / (100 * k);
        assertEquals(2 * seconds, at(piece(), 9, 0, r.getNotesInInputOrder()).onset, 1e-10);
    }

    /** Example 52 by Cor. 21: rubato in voice 0, bars of 4; the hands meet at every bar line. */
    public void testBarRubato() {
        Performances.Settings s = Performances.Settings.tempo(90, 90, Performances.Curve.LINEAR, 4, 0.3, 0);
        Performances.Result r = Performances.perform(piece(), s);
        List<ScoreNote> out = r.getNotesInInputOrder();
        for (int bar = 0; bar < 4; bar++) {
            assertEquals("bar " + bar, at(piece(), 4 * bar, 1, out).onset, at(piece(), 4 * bar, 0, out).onset, 1e-9);
            assertEquals("bar " + bar, 2 * 4 * bar * 60 / 90.0, at(piece(), 4 * bar, 0, out).onset, 1e-9);
        }
        assertTrue("inner onsets move",
                Math.abs(at(piece(), 1, 0, out).onset - 2 * 60 / 90.0) > 0.01);
        assertTrue(r.getReport(), r.getReport().contains("bar rubato"));
        assertEquals("mother, two hands, one rubato daughter", 4, r.getStemma().size());
    }

    public void testScalarPerformance() {
        Performances.Settings s = Performances.Settings.scalar(120, 1, 1, 0.5);
        Performances.Result r = Performances.perform(piece(), s);
        List<ScoreNote> in = piece();
        for (int i = 0; i < in.size(); i++) {
            assertEquals("M.M. 120 keeps onsets", in.get(i).onset, r.getNotesInInputOrder().get(i).onset, 1e-12);
            assertEquals("staccato halves durations", 0.5 * in.get(i).duration, r.getNotesInInputOrder().get(i).duration, 1e-12);
            assertEquals(in.get(i).loudness, r.getNotesInInputOrder().get(i).loudness);
        }
        Performances.Result cresc = Performances.perform(piece(), Performances.Settings.scalar(120, 0.5, 2, 1));
        assertEquals("first note at half loudness", 40, at(piece(), 0, 0, cresc.getNotesInInputOrder()).loudness);
        // Lambda(E) = ln 0.5 + ln 4 * E/16; the voice-1 note at E = 14 (loudness 70) gets 0.5 * 4^(14/16)
        assertEquals((int) Math.round(70 * 0.5 * Math.pow(4, 14.0 / 16)), at(piece(), 14, 1, cresc.getNotesInInputOrder()).loudness);
        Performances.Result loud = Performances.perform(piece(), Performances.Settings.scalar(120, 3, 3, 1));
        assertEquals("clamped to 127", 127, loud.getNotesInInputOrder().get(0).loudness);
        assertTrue(loud.getReport(), loud.getReport().contains("clamped"));
    }

    /** Tempo Performance followed by Scalar Performance at M.M. 120 leaves the onsets in place. */
    public void testChaining() {
        Performances.Result t = Performances.perform(piece(), Performances.Settings.tempo(120, 60, Performances.Curve.LINEAR, 0, 0, -1));
        Performances.Result s = Performances.perform(t.getNotesInInputOrder(), Performances.Settings.scalar(120, 1, 1, 0.8));
        for (int i = 0; i < piece().size(); i++) {
            assertEquals(t.getNotesInInputOrder().get(i).onset, s.getNotesInInputOrder().get(i).onset, 1e-12);
            assertEquals(0.8 * t.getNotesInInputOrder().get(i).duration, s.getNotesInInputOrder().get(i).duration, 1e-12);
        }
    }

    public void testRefusals() {
        try {
            Performances.perform(piece(), Performances.Settings.tempo(0, 60, Performances.Curve.LINEAR, 0, 0, -1));
            fail("tempo 0 accepted");
        }
        catch (IllegalArgumentException expected) { /* T > 0 */ }
        try {
            Performances.perform(piece(), Performances.Settings.tempo(60, 60, Performances.Curve.LINEAR, 4, 0.95, 0));
            fail("rubato depth 0.95 accepted");
        }
        catch (IllegalArgumentException expected) { /* depth < 0.9 */ }
        try {
            Performances.perform((Denotator) null, Performances.Settings.scalar(120, 1, 1, 1));
            fail("null score accepted");
        }
        catch (IllegalArgumentException expected) { /* message */ }
        assertEquals(0, Performances.perform(new ArrayList<ScoreNote>(),
                Performances.Settings.scalar(120, 1, 1, 1)).getNotesInInputOrder().size());
    }

    public void testRubettes() {
        TempoPerformanceRubette t = new TempoPerformanceRubette();
        assertEquals("Tempo Performance", t.getName());
        assertEquals("Performance", t.getGroup());
        assertEquals(1, t.getInCount());
        assertEquals(1, t.getOutCount());
        assertNotNull(t.getIcon());
        assertEquals("Not run yet.", t.getReportText());
        t.process(ScoreNote.toScore(piece()));
        Denotator out = t.getOutput(0);
        assertNotNull(t.getReportText(), out);
        assertEquals(piece().size(), ScoreNote.fromScore(out).size());
        assertTrue(t.getInfo(), t.getInfo().endsWith(" s"));

        ScalarPerformanceRubette s = new ScalarPerformanceRubette();
        assertEquals("Scalar Performance", s.getName());
        assertEquals("Performance", s.getGroup());
        assertNotNull(s.getIcon());
        s.process(out);
        assertEquals(piece().size(), ScoreNote.fromScore(s.getOutput(0)).size());
        s.process(null);
        assertTrue(s.getReportText(), s.getReportText().contains("null"));
    }
}
