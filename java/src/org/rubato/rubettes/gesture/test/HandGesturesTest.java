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

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import junit.framework.TestCase;

import org.rubato.base.BooleanProperty;
import org.rubato.base.TextProperty;
import org.rubato.composer.BuiltinRubettes;
import org.rubato.io.ScoreNote;
import org.rubato.math.gesture.HandGesture;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.gesture.HandGestureRubette;
import org.rubato.rubettes.gesture.HandGestures;

/** The headless hand-gesture API and the Hand Gesture rubette (Topos III, Example 60). */
public class HandGesturesTest extends TestCase {

    /** MM03 Fig. 2 as a score: C3, C#4, C3, G3. */
    static Denotator mm03() {
        double[][] n = { { 0, 1, 48 }, { 1.5, 1, 61 }, { 2.5, 1, 48 }, { 3.5, 1, 55 } };
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (double[] x : n) {
            notes.add(new ScoreNote(x[0], (int) x[2], 80, x[1], 0));
        }
        return ScoreNote.toScore(notes);
    }

    public void testParseFingering() {
        int[] f = HandGestures.parseFingering("# MM03 Fig. 2\n3\n\n 2 \n3   # back to C3\n3\n");
        assertTrue(Arrays.toString(f), Arrays.equals(new int[] { 3, 2, 3, 3 }, f));
    }

    public void testParseFingeringRefusals() {
        refuseParse("3\nx\n", "line 2");
        refuseParse("3\n6\n", "line 2");
        refuseParse("3 4\n", "line 1");
        refuseParse("# nothing\n\n", "no fingers");
    }

    private static void refuseParse(String text, String word) {
        try {
            HandGestures.parseFingering(text);
            fail(text);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(word));
        }
    }

    public void testFromScore() {
        HandGestures.Report r = HandGestures.analyze(mm03(), "3\n2\n3\n3",
                                                     HandGesture.Settings.defaults().withHand(HandGesture.Hand.LEFT));
        HandGesture h = r.getGesture();
        assertEquals(4, h.noteCount());
        assertTrue(Arrays.equals(new int[] { 3, 2, 3, 3 }, h.fingering()));
        String t = r.text();
        assertTrue(t, t.contains("Hand gesture of 4 notes"));
        assertTrue(t, t.contains("Example 60"));
        assertTrue(t, t.contains("left hand"));
        assertTrue(t, t.contains("performable: yes"));
        assertTrue(t, t.contains("finger 2 moved"));
        assertTrue(t, t.contains("reshapes: " + h.reshapes().size()));
    }

    public void testFingeringMustMatchTheScore() {
        try {
            HandGestures.analyze(mm03(), "3\n2\n3", HandGesture.Settings.defaults());
            fail("three fingers for four notes");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("fingering"));
        }
    }

    public void testRefusesEmptyScore() {
        try {
            HandGestures.analyze(ScoreNote.toScore(new ArrayList<ScoreNote>()), "1", HandGesture.Settings.defaults());
            fail("no notes");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no notes"));
        }
    }

    public void testRubette() {
        HandGestureRubette r = new HandGestureRubette();
        assertEquals("Hand Gesture", r.getName());
        assertEquals("Score", r.getGroup());
        assertEquals(1, r.getInCount());
        assertEquals(0, r.getOutCount());
        assertEquals("Not run yet.", r.getReportText());
        assertNotNull(r.getIcon());
        ((TextProperty) r.getProperty(HandGestureRubette.FINGERING_KEY)).setString("3\n2\n3\n3");
        ((BooleanProperty) r.getProperty(HandGestureRubette.LEFT_KEY)).setValue(Boolean.TRUE);
        r.process(mm03());
        assertFalse(r.getReportText(), r.hasErrors());
        assertTrue(r.getReportText(), r.getReportText().contains("4 notes"));
        assertTrue(r.getInfo(), r.getInfo().startsWith("performable"));
        assertTrue(r.getReportText(), r.getReportText().contains("left hand"));
        HandGestureRubette right = new HandGestureRubette();
        ((TextProperty) right.getProperty(HandGestureRubette.FINGERING_KEY)).setString("3\n2\n3\n3");
        right.process(mm03());
        assertTrue(right.getReportText(), right.getReportText().contains("right hand"));
    }

    public void testRubetteReportsBadFingering() {
        HandGestureRubette r = new HandGestureRubette();
        r.process(mm03());
        assertTrue(r.hasErrors());
        assertTrue(r.getReportText(), r.getReportText().contains("no fingers"));
    }

    public void testRubetteReportsNullInput() {
        HandGestureRubette r = new HandGestureRubette();
        r.process(null);
        assertTrue(r.hasErrors());
        assertTrue(r.getReportText(), r.getReportText().contains("null"));
    }

    public void testViewIsReachableAndDescribed() {
        HandGestureRubette r = new HandGestureRubette();
        ((TextProperty) r.getProperty(HandGestureRubette.FINGERING_KEY)).setString("3\n2\n3\n3");
        ((BooleanProperty) r.getProperty(HandGestureRubette.LEFT_KEY)).setValue(Boolean.TRUE);
        r.process(mm03());
        assertNotNull(r.getView());
        JComponent plot = r.getPlot();
        assertTrue(plot.isFocusable());
        assertNotNull(plot.getAccessibleContext().getAccessibleName());
        String d = plot.getAccessibleContext().getAccessibleDescription();
        assertTrue(d, d.contains("finger 2") && d.contains("carpus"));
        HandGesture g = HandGestures.analyze(mm03(), "3\n2\n3\n3",
                                             HandGesture.Settings.defaults().withHand(HandGesture.Hand.LEFT)).getGesture();
        int k = g.moves(2).size();
        assertTrue(d, d.contains("finger 2: " + k + (k == 1 ? " move;" : " moves;")));
        JComponent text = r.getTextArea();
        assertNotNull(text.getAccessibleContext().getAccessibleName());
        Set<?> fwd = text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        assertTrue(fwd.toString(), fwd.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
    }

    public void testRegistered() {
        assertTrue(Arrays.asList(BuiltinRubettes.classes).contains("org.rubato.rubettes.gesture.HandGestureRubette"));
    }
}
