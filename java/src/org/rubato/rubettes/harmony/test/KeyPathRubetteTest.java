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

package org.rubato.rubettes.harmony.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.math.harmony.ChordSymbol;
import org.rubato.math.harmony.KeyPath;
import org.rubato.math.harmony.KeyPath.Reduction;
import org.rubato.math.harmony.PitchClasses;
import org.rubato.math.harmony.KeyPath.Event;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.harmony.Harmonies;
import org.rubato.rubettes.harmony.KeyPathRubette;

/** The Key Path rubette and its headless calls. */
public class KeyPathRubetteTest extends TestCase {

    /** A score with each chord of a chart in close position from middle C, four beats a bar. */
    static Denotator chartScore(String chart) {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        int bar = 0;
        for (List<ChordSymbol> b : ChordSymbol.parseBars(chart)) {
            double len = 4.0 / b.size();
            for (int i = 0; i < b.size(); i++) {
                int mask = b.get(i).pitchClasses();
                for (int pc = 0; pc < 12; pc++) {
                    if ((mask >> pc & 1) == 1) {
                        notes.add(new ScoreNote(bar * 4 + i * len, 60 + pc, 80, len, 0));
                    }
                }
                notes.add(new ScoreNote(bar * 4 + i * len, 36 + b.get(i).root(), 80, len, 1));
            }
            bar++;
        }
        return ScoreNote.toScore(notes);
    }

    public void testScoreBarsMatchTheChart() {
        String chart = KeyPathRubette.DEFAULT_CHART;
        List<List<Integer>> fromScore = Harmonies.keyPathBars(ScoreNote.fromScore(chartScore(chart)), 4.0);
        assertEquals(KeyPath.parseChart(chart), fromScore);
    }

    public void testScoreAndChartGiveTheSamePath() {
        List<Event> a = KeyPath.analyse(KeyPath.parseChart(KeyPathRubette.DEFAULT_CHART), 2);
        List<Event> b = Harmonies.keyPath(chartScore(KeyPathRubette.DEFAULT_CHART), 4.0, 2);
        assertEquals(KeyPath.report(a), KeyPath.report(b));
        assertEquals(5, b.size());
        assertEquals("A", b.get(2).keyName());
        assertEquals(12, b.get(2).bar());
    }

    public void testRubette() {
        KeyPathRubette r = new KeyPathRubette();
        r.process(null);
        String text = r.getReportText();
        assertTrue(text, text.contains("bar 4: starts in C"));
        assertTrue(text, text.contains("modulation C -> D"));
        assertTrue(text, text.contains("modulation D -> A"));
        assertTrue(text, text.contains("modulation A -> C"));
        assertTrue(text, text.contains("synthetic"));
        assertEquals("C, D, A, C", r.getInfo());
        r.process(chartScore(KeyPathRubette.DEFAULT_CHART));
        assertTrue(r.getReportText(), r.getReportText().contains("modulation A -> C"));
        assertEquals("Harmony", r.getGroup());
        assertEquals("Key Path", r.getName());
        assertNotNull(r.getIcon());
    }

    /** Held notes count at later onsets: C and E held over a bar, G then A on top give C then a. */
    public void testHeldNotes() {
        List<ScoreNote> n = new ArrayList<ScoreNote>();
        n.add(new ScoreNote(0, 60, 80, 2.0, 0));
        n.add(new ScoreNote(0, 64, 80, 2.0, 0));
        n.add(new ScoreNote(0, 67, 80, 1.0, 1));
        n.add(new ScoreNote(1, 69, 80, 1.0, 1));
        n.add(new ScoreNote(2, 62, 80, 1.0, 0));
        List<List<Integer>> bars = Harmonies.keyPathBars(n, 2.0);
        assertEquals(2, bars.size());
        assertEquals(KeyPath.parseChart("C Am | D").get(0), bars.get(0));
        assertEquals(1, bars.get(1).size());
    }

    /** A G7 sonority reaches the key path as G under CONTAINED_TRIAD, and is dropped by NONE as before. */
    public void testReducedBars() {
        List<ScoreNote> n = new ArrayList<ScoreNote>();
        n.add(new ScoreNote(0, 43, 80, 2.0, 0));
        n.add(new ScoreNote(0, 59, 80, 2.0, 1));
        n.add(new ScoreNote(0, 62, 80, 2.0, 2));
        n.add(new ScoreNote(0, 65, 80, 2.0, 3));
        n.add(new ScoreNote(2, 48, 80, 2.0, 0));
        n.add(new ScoreNote(2, 64, 80, 2.0, 1));
        n.add(new ScoreNote(2, 67, 80, 2.0, 2));
        assertEquals(Arrays.asList(Arrays.asList(PitchClasses.of(7, 11, 2, 5), PitchClasses.of(0, 4, 7))),
                Harmonies.keyPathBars(n, 4.0));
        assertEquals(Arrays.asList(Arrays.asList(PitchClasses.of(7, 11, 2), PitchClasses.of(0, 4, 7))),
                Harmonies.keyPathBars(n, 4.0, Reduction.CONTAINED_TRIAD));
        assertEquals(Harmonies.keyPathBars(n, 4.0), Harmonies.keyPathBars(n, 4.0, Reduction.NONE));
        // a reduced chord equal to the one before it in the bar is dropped as a repeat
        n.add(new ScoreNote(3, 69, 80, 1.0, 3));
        List<Integer> bar = Harmonies.keyPathBars(n, 4.0, Reduction.BASS_TRIAD).get(0);
        assertEquals(Arrays.asList(PitchClasses.of(7, 11, 2), PitchClasses.of(0, 4, 7)), bar);
    }

    /** The real-music rules and the triad reduction are off by default and selectable. */
    public void testRubetteRules() {
        KeyPathRubette r = new KeyPathRubette();
        assertFalse(r.isRealMusic());
        assertFalse(r.isReduceTriads());
        r.setRealMusic(true);
        r.process(null);
        assertEquals("C, D, d, C", r.getInfo());
        assertTrue(r.getReportText(), r.getReportText().contains("Rules: real-music preset (a departure"));
        r.setRealMusic(false);
        r.process(null);
        assertEquals("C, D, A, C", r.getInfo());
        assertFalse(r.getReportText().contains("Rules:"));
        // the reduction acts on scores only: F, G7, C (k4 of C) shows C only when the G7 is read as G
        List<ScoreNote> n = new ArrayList<ScoreNote>();
        int[][] chords = { { 41, 57, 60, 65 }, { 43, 59, 62, 65 }, { 36, 52, 55, 60 } };
        for (int i = 0; i < chords.length; i++) {
            for (int p : chords[i]) {
                n.add(new ScoreNote(i, p, 80, 1, 0));
            }
        }
        Denotator score = ScoreNote.toScore(n);
        r.process(score);
        assertEquals("", r.getInfo());
        r.setReduceTriads(true);
        r.process(score);
        assertEquals("C", r.getInfo());
        assertTrue(r.getReportText(), r.getReportText().contains("Sonorities reduced to a contained triad (a departure"));
        assertEquals(KeyPath.report(Harmonies.keyPath(score, 4.0, KeyPath.defaults(2), Reduction.CONTAINED_TRIAD)),
                KeyPath.report(KeyPath.analyse(Harmonies.keyPathBars(n, 4.0, Reduction.CONTAINED_TRIAD), 2)));
    }

    public void testRefusals() {
        try {
            Harmonies.keyPathBars(new ArrayList<ScoreNote>(), 0);
            fail();
        }
        catch (IllegalArgumentException expected) { /* expected */ }
        KeyPathRubette r = new KeyPathRubette();
        r.setChart("C | Xq");
        r.process(null);
        assertFalse(r.getReportText().contains("starts in"));
    }
}
