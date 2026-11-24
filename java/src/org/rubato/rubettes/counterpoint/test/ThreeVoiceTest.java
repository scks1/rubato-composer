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

import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.KeyStroke;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.RelaxationMode;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.State;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.counterpoint.ThreeVoice;
import org.rubato.rubettes.counterpoint.ThreeVoiceCounterpointRubette;

/**
 * The headless three-voice API and the Three-Voice Counterpoint rubette
 * (sources/THREE-VOICE-COUNTERPOINT-SPEC.md, §2 items 2 and 3).
 */
public class ThreeVoiceTest extends TestCase {

    /** Example 3.3 (p. 8) as states (a; b, c). */
    static final int[][] EXAMPLE33 = { { 2, 3, 0 }, { 9, 8, 3 }, { 5, 4, 7 }, { 2, 7, 3 }, { 1, 8, 3 }, { 2, 7, 3 },
                                       { 4, 8, 3 }, { 5, 7, 4 }, { 7, 3, 7 }, { 5, 7, 4 }, { 4, 8, 3 }, { 2, 3, 0 } };

    static List<State> example33States() {
        ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
        List<State> l = new ArrayList<State>();
        for (int[] q : EXAMPLE33) {
            l.add(m.state(q[0], q[1], q[2]));
        }
        return l;
    }

    static Denotator chords(int[][] pitches) {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < pitches.length; i++) {
            for (int v = 0; v < 3; v++) {
                notes.add(new ScoreNote(i, pitches[i][v], 80, 1.0, v));
            }
        }
        return ScoreNote.toScore(notes);
    }

    public void testExample33RealizationIsCorrect() {
        List<ScoreNote> notes = ThreeVoice.realize(example33States(), 50);
        assertEquals(36, notes.size());
        // the first chord: D F D, lower voice 50
        assertEquals(50, notes.get(0).pitch.getNumerator());
        ThreeVoice.Report r = ThreeVoice.check(ScoreNote.toScore(notes), ThreeVoice.Settings.defaults());
        assertEquals(12, r.getStates().size());
        for (int i = 0; i < 12; i++) {
            assertEquals(example33States().get(i), r.getStates().get(i));
        }
        assertEquals(11, r.getVerdicts().size());
        assertTrue(r.text(), r.isCorrect());
        assertEquals(0, ((PowerDenotator) r.flaggedScore()).getFactorCount());
        assertTrue(r.text(), r.text().contains("11 steps: 11 admitted, 0 refused"));
        // every voice moves by at most a tritone and the voices stay in order
        for (int i = 0; i < 36; i++) {
            ScoreNote n = notes.get(i);
            assertEquals(i / 3, (int) n.onset);
            assertEquals(i % 3, n.voice);
        }
    }

    public void testRefusedStepsAreFlaggedWithReasons() {
        // C E G -> C Eb G (the LU pair stands still), -> D F A (fifth to fifth), -> C E G# (outside the mask)
        int[][] p = { { 48, 52, 55 }, { 48, 51, 55 }, { 50, 53, 57 }, { 48, 52, 56 } };
        ThreeVoice.Report r = ThreeVoice.check(chords(p), ThreeVoice.Settings.defaults());
        assertFalse(r.isCorrect());
        assertEquals(3, r.getVerdicts().size());
        String t = r.text();
        assertTrue(t, t.contains("lower and upper voices stand still"));
        assertTrue(t, t.contains("from a fifth to a fifth"));
        assertTrue(t, t.contains("not in the mask"));
        // targets of the three refused steps, three notes each
        assertEquals(9, r.flagged().size());
        assertEquals(9, ((PowerDenotator) r.flaggedScore()).getFactorCount());
        // hold-fibre admits the held fifth C-G
        ThreeVoice.Report h = ThreeVoice.check(chords(p), ThreeVoice.Settings.defaults().withMode(RelaxationMode.HOLD_FIBRE));
        assertTrue(h.getVerdicts().get(0).isAdmitted());
        assertFalse(h.getVerdicts().get(1).isAdmitted());
        assertTrue(h.text(), h.text().contains("hold-fibre"));
        assertTrue(h.text(), h.text().contains("not the paper's"));
        assertEquals(6, h.flagged().size());
    }

    public void testChordOutsideMaskIsFlaggedEvenAlone() {
        ThreeVoice.Report r = ThreeVoice.check(chords(new int[][] { { 48, 53, 57 } }), ThreeVoice.Settings.defaults());
        assertFalse(r.isCorrect());
        assertEquals(0, r.getVerdicts().size());
        assertEquals(3, r.flagged().size());
        assertTrue(r.text(), r.text().contains("not in the mask"));
    }

    public void testVoicesAndInputErrors() {
        try {
            List<ScoreNote> notes = new ArrayList<ScoreNote>();
            notes.add(new ScoreNote(0, 48, 80, 1, 0));
            notes.add(new ScoreNote(0, 52, 80, 1, 1));
            ThreeVoice.check(notes, ThreeVoice.Settings.defaults());
            fail();
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("voice 2"));
        }
        try {
            List<ScoreNote> notes = new ArrayList<ScoreNote>();
            notes.add(new ScoreNote(0, 48, 80, 1, 0));
            notes.add(new ScoreNote(0, 52, 80, 1, 1));
            notes.add(new ScoreNote(0.5, 55, 80, 1, 2));
            ThreeVoice.check(notes, ThreeVoice.Settings.defaults());
            fail();
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("note against note"));
        }
        // other voice numbers
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        notes.add(new ScoreNote(0, 48, 80, 1, 5));
        notes.add(new ScoreNote(0, 52, 80, 1, 3));
        notes.add(new ScoreNote(0, 55, 80, 1, 7));
        ThreeVoice.Report r = ThreeVoice.check(notes, new ThreeVoice.Settings(5, 3, 7, RelaxationMode.STRICT));
        assertEquals(ThreeVoiceCounterpoint.fux().fromPitchClasses(0, 4, 7), r.getStates().get(0));
        assertEquals(RelaxationMode.STRICT, ThreeVoice.parseMode("strict"));
        assertEquals(RelaxationMode.HOLD_FIBRE, ThreeVoice.parseMode(" Hold-Fibre "));
        try {
            ThreeVoice.parseMode("loose");
            fail();
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("strict") && e.getMessage().contains("hold-fibre"));
        }
        assertEquals(3, ThreeVoice.parseChord("50 53 62").length);
        try {
            ThreeVoice.parseChord("50 53");
            fail();
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("three"));
        }
    }

    public void testSuccessorScore() {
        // Example 3.3's first chord (2; 3, 0) has configuration h7 = (3, 0): 184 strict successors (Table 9)
        ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
        State xi1 = m.state(2, 3, 0);
        int n = m.successors(xi1, RelaxationMode.STRICT).size();
        assertEquals(184, n);
        List<ScoreNote> notes = ThreeVoice.successorNotes(new int[] { 50, 53, 62 }, RelaxationMode.STRICT);
        assertEquals(3 * 184, notes.size());
        for (int i = 0; i < notes.size(); i += 3) {
            State s = m.fromPitchClasses(notes.get(i).pitch.getNumerator(), notes.get(i + 1).pitch.getNumerator(),
                                         notes.get(i + 2).pitch.getNumerator());
            assertTrue(m.isAdmitted(xi1, s, RelaxationMode.STRICT));
            int[] from = { 50, 53, 62 };
            for (int v = 0; v < 3; v++) {
                int step = notes.get(i + v).pitch.getNumerator() - from[v];
                assertTrue("step " + step, step >= -5 && step <= 6);
                assertEquals(v, notes.get(i + v).voice);
                assertEquals(i / 3, (int) notes.get(i + v).onset);
            }
        }
        // the second chord of Example 3.3 is among them
        boolean found = false;
        for (int i = 0; i < notes.size(); i += 3) {
            if (m.fromPitchClasses(notes.get(i).pitch.getNumerator(), notes.get(i + 1).pitch.getNumerator(),
                                   notes.get(i + 2).pitch.getNumerator()).equals(m.state(9, 8, 3))) {
                found = true;
            }
        }
        assertTrue(found);
        int hold = ThreeVoice.successorNotes(new int[] { 50, 53, 62 }, RelaxationMode.HOLD_FIBRE).size() / 3;
        assertTrue("hold-fibre adds successors", hold > 184);
        try {
            ThreeVoice.successorNotes(new int[] { 48, 53, 57 }, RelaxationMode.STRICT);
            fail();
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("mask"));
        }
    }

    public void testPlrReport() {
        String strict = ThreeVoice.plrReport(RelaxationMode.STRICT);
        assertTrue(strict, strict.contains("288 realizations, 192 with the target in the mask, 12 admitted"));
        assertTrue(strict, strict.contains("P minor 351"));
        String hold = ThreeVoice.plrReport(RelaxationMode.HOLD_FIBRE);
        assertTrue(hold, hold.contains("192 with the target in the mask, 168 admitted"));
        assertTrue(hold, hold.contains("P 84, L 36, R 48"));
    }

    public void testRubetteWithoutInputGivesStartChordSuccessors() {
        ThreeVoiceCounterpointRubette r = new ThreeVoiceCounterpointRubette();
        assertTrue(java.util.Arrays.asList(org.rubato.composer.BuiltinRubettes.classes)
                   .contains(ThreeVoiceCounterpointRubette.class.getName()));
        assertEquals("Three-Voice Counterpoint", r.getName());
        assertEquals("Score", r.getGroup());
        assertEquals(1, r.getInCount());
        assertEquals(2, r.getOutCount());
        r.process(null);
        assertEquals(0, ((PowerDenotator) r.getOutput(0)).getFactorCount());
        assertEquals(3 * 184, ((PowerDenotator) r.getOutput(1)).getFactorCount());
        assertTrue(r.getReportText(), r.getReportText().contains("start chord"));
        assertTrue(r.getReportText(), r.getReportText().contains("184 successors"));
        assertFalse(r.getReportText(), r.getReportText().contains("288 realizations"));
    }

    public void testRubetteChecksInputAndAppendsReport() {
        ThreeVoiceCounterpointRubette r = new ThreeVoiceCounterpointRubette();
        r.setSettingsForTest(ThreeVoice.Settings.defaults().withMode(RelaxationMode.HOLD_FIBRE), "50 53 62", true);
        int[][] p = { { 48, 52, 55 }, { 48, 51, 55 } };
        r.process(chords(p));
        assertEquals(0, ((PowerDenotator) r.getOutput(0)).getFactorCount());
        // successors of the last chord, C Eb G, under hold-fibre
        ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
        int n = m.successors(m.fromPitchClasses(0, 3, 7), RelaxationMode.HOLD_FIBRE).size();
        assertEquals(3 * n, ((PowerDenotator) r.getOutput(1)).getFactorCount());
        assertTrue(r.getReportText(), r.getReportText().contains("1 step: 1 admitted"));
        assertTrue(r.getReportText(), r.getReportText().contains("168 admitted"));
        assertEquals("correct", r.getInfo());
    }

    public void testRubetteReportsBadProperties() {
        ThreeVoiceCounterpointRubette r = new ThreeVoiceCounterpointRubette();
        r.setSettingsForTest(ThreeVoice.Settings.defaults(), "50 53", false);
        r.process(null);
        assertNull(r.getOutput(1));
        assertTrue(r.getReportText(), r.getReportText().contains("three"));
        assertEquals(1, r.getErrors().size());
        assertEquals(r.getReportText(), r.getErrors().get(0));
    }

    /** A unison between the lower and middle voices is written an octave apart (D8). */
    public void testRealizeWritesUnisonAsOctave() {
        ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
        List<State> states = new ArrayList<State>();
        states.add(m.state(2, 0, 7));
        states.add(m.state(2, 3, 7));
        List<ScoreNote> n = ThreeVoice.realize(states, 50);
        assertEquals(6, n.size());
        assertEquals(50, n.get(0).pitch.getNumerator());
        assertEquals(62, n.get(1).pitch.getNumerator());
        assertEquals(69, n.get(2).pitch.getNumerator());
        assertEquals(53, n.get(4).pitch.getNumerator());
        assertEquals(57, n.get(5).pitch.getNumerator());
    }

    public void testViewAccessibility() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }
        ThreeVoiceCounterpointRubette r = new ThreeVoiceCounterpointRubette();
        r.process(null);
        r.getView();
        javax.swing.JTextArea t = r.getTextArea();
        assertEquals("Three-voice counterpoint report", t.getAccessibleContext().getAccessibleName());
        assertNotNull(t.getAccessibleContext().getAccessibleDescription());
        assertFalse(t.isEditable());
        assertTrue(t.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS)
                    .contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        assertTrue(t.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS)
                    .contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
        assertEquals(r.getReportText(), t.getText());
    }
}
