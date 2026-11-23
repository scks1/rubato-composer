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

package org.rubato.rubettes.counterpoint.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.math.counterpoint.StrongDichotomies;
import org.rubato.math.counterpoint.SymmetryTrace;
import org.rubato.math.counterpoint.SymmetryTrace.Step;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.counterpoint.Counterpoint;
import org.rubato.rubettes.counterpoint.SymmetryTraceRubette;

/** The Symmetry Trace rubette and its headless calls. */
public class SymmetryTraceRubetteTest extends TestCase {

    /** Confitebor primo mm. 64-67 as a two-voice score: cantus in voice 0 from c', discantus above. */
    static Denotator confiteborScore(boolean hanging) {
        int[][] xi = SymmetryTrace.parse(SymmetryTraceRubette.DEFAULT_INTERVALS);
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < xi.length; i++) {
            int cf = 60 + xi[i][0];
            notes.add(new ScoreNote(i, cf, 80, 1.0, 0));
            notes.add(new ScoreNote(i, hanging ? cf - xi[i][1] - 12 : cf + xi[i][1] + 12, 80, 1.0, 1));
        }
        return ScoreNote.toScore(notes);
    }

    public void testScoreGivesTheConfiteborTable() {
        for (boolean hanging : new boolean[] { false, true }) {
            Counterpoint.Settings s = new Counterpoint.Settings(StrongDichotomies.FUX,
                    hanging ? org.rubato.math.counterpoint.FirstSpecies.Orientation.HANGING
                            : org.rubato.math.counterpoint.FirstSpecies.Orientation.SWEEPING, 0, 1);
            List<Step> steps = Counterpoint.trace(confiteborScore(hanging), s);
            int[] card = { 1, 1, 1, 1, 1, 1, 1, 1 };
            assertEquals(card.length, steps.size());
            for (int i = 0; i < card.length; i++) {
                assertEquals("row " + (i + 1), card[i], steps.get(i).cardinality());
            }
            assertEquals(10, steps.get(2).getFrom()[0]);
            assertEquals(7, steps.get(2).getFrom()[1]);
        }
    }

    public void testRubetteOnTextAndScore() {
        SymmetryTraceRubette r = new SymmetryTraceRubette();
        r.process(null);
        String text = r.getReportText();
        assertTrue(text, text.contains("e^{\u03b5.6}\u2218(1+\u03b5.6)"));
        assertTrue(text, text.contains("[fixes cantus]}  (1)"));
        assertEquals("8 steps, 8 symmetries", r.getInfo());
        r.process(confiteborScore(false));
        assertEquals(text, r.getReportText());
        assertEquals("Score", r.getGroup());
        assertEquals("Symmetry Trace", r.getName());
        assertNotNull(r.getIcon());
    }

    public void testRefusals() {
        Counterpoint.Settings s = Counterpoint.Settings.defaults();
        List<ScoreNote> one = new ArrayList<ScoreNote>();
        one.add(new ScoreNote(0, 60, 80, 1.0, 0));
        one.add(new ScoreNote(0, 67, 80, 1.0, 1));
        try {
            Counterpoint.trace(ScoreNote.toScore(one), s);
            fail();
        }
        catch (IllegalArgumentException expected) { /* expected */ }
        SymmetryTraceRubette r = new SymmetryTraceRubette();
        r.setIntervals("7, 4");
        r.process(null);
        assertTrue(r.getReportText(), r.getReportText().contains("x+\u03b5.y"));
    }
}
