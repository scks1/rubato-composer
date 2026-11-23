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
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.math.harmony.ChordSymbol;
import org.rubato.math.harmony.KeyPath;
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
