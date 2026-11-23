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
package org.rubato.rubettes.gesture.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.FingerGesture;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.gesture.FingerGestureRubette;
import org.rubato.rubettes.gesture.FingerGestures;

/** The headless finger-gesture API and the Finger Gesture rubette. */
public class FingerGesturesTest extends TestCase {

    static Denotator melody() {
        int[] p = { 60, 64, 67, 72 };
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < p.length; i++) {
            notes.add(new ScoreNote(i, p[i], 80, 0.75, 0));
        }
        return ScoreNote.toScore(notes);
    }

    public void testFromScore() {
        FingerGestures.Report r = FingerGestures.analyze(melody(), FingerGesture.Settings.defaults());
        assertEquals(Digraph.arrowN(15), r.getGesture().gesture().skeleton());
        assertEquals(0, r.getGesture().backwardMoves());
        String t = r.text();
        assertTrue(t, t.contains("4 notes"));
        assertTrue(t, t.contains("16 vertices, 15 arrows"));
        assertTrue(t, t.contains("0 backward moves"));
    }

    public void testRefusesEmptyScore() {
        try {
            FingerGestures.analyze(ScoreNote.toScore(new ArrayList<ScoreNote>()), FingerGesture.Settings.defaults());
            fail("no notes");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no notes"));
        }
    }

    public void testRubette() {
        FingerGestureRubette r = new FingerGestureRubette();
        assertEquals("Finger Gesture", r.getName());
        assertEquals("Score", r.getGroup());
        assertEquals(1, r.getInCount());
        assertEquals(0, r.getOutCount());
        assertEquals("Not run yet.", r.getReportText());
        r.process(melody());
        assertTrue(r.getReportText(), r.getReportText().contains("4 notes"));
        assertTrue(r.getInfo(), r.getInfo().startsWith("15 arrows"));
        assertNotNull(r.getIcon());
    }

    public void testRubetteReportsNullInput() {
        FingerGestureRubette r = new FingerGestureRubette();
        r.process(null);
        assertTrue(r.hasErrors());
        assertTrue(r.getReportText(), r.getReportText().contains("null"));
    }
}
