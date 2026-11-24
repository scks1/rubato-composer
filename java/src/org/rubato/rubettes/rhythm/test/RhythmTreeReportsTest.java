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

package org.rubato.rubettes.rhythm.test;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import junit.framework.TestCase;

import org.rubato.composer.BuiltinRubettes;
import org.rubato.io.ScoreNote;
import org.rubato.math.rhythm.RhythmNotation;
import org.rubato.math.rhythm.RhythmTreeDenotators;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.rhythm.RhythmSpanPanel;
import org.rubato.rubettes.rhythm.RhythmTreeReports;
import org.rubato.rubettes.rhythm.RhythmTreeRubette;

/**
 * The Rhythm Tree rubette and its reports (sources/RHYTHM-TREES-SPEC.md
 * section 2 item 3, D1, D2, D6, D7, D9).
 */
public class RhythmTreeReportsTest extends TestCase {

    private static final double EPS = 1e-12;

    private static double[][] onsetsAndDurations(Denotator score) {
        List<ScoreNote> notes = ScoreNote.fromScore(score);
        double[][] out = new double[notes.size()][];
        for (int i = 0; i < out.length; i++) {
            out[i] = new double[] { notes.get(i).onset, notes.get(i).duration };
        }
        return out;
    }

    private static void assertNotes(double[][] expected, Denotator score) {
        double[][] got = onsetsAndDurations(score);
        assertEquals(Arrays.deepToString(got), expected.length, got.length);
        for (int i = 0; i < got.length; i++) {
            assertEquals("onset " + i, expected[i][0], got[i][0], EPS);
            assertEquals("duration " + i, expected[i][1], got[i][1], EPS);
        }
    }

    /** D1, D6, D7: Figure 2 by default, seven notes at the proportional values. */
    public void testDefaultFigure2() {
        RhythmTreeReports.Report r = RhythmTreeReports.render(null, RhythmTreeReports.Settings.defaults());
        assertNotes(new double[][] {
            { 0, 38.0 / 15 }, { 38.0 / 15, 19.0 / 27 }, { 437.0 / 135, 19.0 / 54 }, { 323.0 / 90, 19.0 / 18 },
            { 209.0 / 45, 19.0 / 18 }, { 57.0 / 10, 57.0 / 35 }, { 513.0 / 70, 76.0 / 35 } }, r.score());
        for (ScoreNote n : ScoreNote.fromScore(r.score())) {
            assertEquals(60, n.pitch.intValue());
            assertEquals(80, n.loudness);
            assertEquals(0, n.voice);
        }
        assertEquals(RhythmNotation.parse(RhythmTreeReports.DEFAULT_TREE), RhythmTreeDenotators.fromDenotator(r.treeDenotator()));
        String t = r.text();
        assertTrue(t, t.contains("from the tree property"));
        assertTrue(t, t.contains("19/2 (2 5/2 (1 (2 1) 1 1) 3 (3/2 2))"));
        assertTrue(t, t.contains("proportional"));
        assertTrue(t, t.contains("11 nodes, depth 3, 7 leaves"));
        assertTrue(t, t.contains("513/70"));
        assertTrue(t, t.contains("76/35"));
        assertFalse(t, t.contains("do not fill"));
        assertEquals("7 notes, proportional timing, last note ends at 19/2", r.summary());
    }

    /** D2, D9: label timing; two identical notes at 4 merge, since a Score is a set. */
    public void testLabelTiming() {
        RhythmTreeReports.Settings s = RhythmTreeReports.Settings.defaults().withTiming("label");
        RhythmTreeReports.Report r = RhythmTreeReports.render(null, s);
        assertNotes(new double[][] { { 0, 2 }, { 2, 2 }, { 3, 1 }, { 4, 1 }, { 4.5, 1.5 }, { 6, 2 } }, r.score());
        String t = r.text();
        assertTrue(t, t.contains("do not fill"));
        assertTrue(t, t.contains("root: the children last 15/2, the node 19/2 (2 short)"));
        assertTrue(t, t.contains("1: the children last 3, the node 5/2 (1/2 over)"));
        assertTrue(t, t.contains("1.0: the children last 3, the node 1 (2 over)"));
        assertTrue(t, t.contains("2: the children last 7/2, the node 3 (1/2 over)"));
        assertTrue(t, t.contains("1 identical note merged"));
        assertEquals("6 notes, label timing, last note ends at 8", r.summary());
    }

    /** The tree input replaces the text property; start, pitch, loudness and voice apply. */
    public void testInputAndSettings() {
        Denotator in = RhythmTreeDenotators.toDenotator(RhythmNotation.parse("4 (1 1 (1 1 1) 1)"));
        RhythmTreeReports.Settings s = new RhythmTreeReports.Settings("1 (1)", "proportional", "8", 67, 100, 2);
        RhythmTreeReports.Report r = RhythmTreeReports.render(in, s);
        assertNotes(new double[][] { { 8, 4.0 / 3 }, { 28.0 / 3, 4.0 / 9 }, { 88.0 / 9, 4.0 / 9 }, { 92.0 / 9, 4.0 / 9 },
                                     { 32.0 / 3, 4.0 / 3 } }, r.score());
        for (ScoreNote n : ScoreNote.fromScore(r.score())) {
            assertEquals(67, n.pitch.intValue());
            assertEquals(100, n.loudness);
            assertEquals(2, n.voice);
        }
        assertTrue(r.text(), r.text().contains("from the input"));
        assertEquals("5 notes, proportional timing, last note ends at 12", r.summary());
    }

    public void testRefusals() {
        String[][] cases = {
            { "4 (1 x)", "proportional", "0", "position 6" },
            { "4 (1 1)", "metric", "0", "metric" },
            { "4 (1 1)", "label", "-1", "start" },
            { "4 (1 1)", "label", "one", "start" },
        };
        for (String[] c : cases) {
            try {
                RhythmTreeReports.render(null, new RhythmTreeReports.Settings(c[0], c[1], c[2], 60, 80, 0));
                fail(Arrays.toString(c));
            }
            catch (IllegalArgumentException e) {
                assertTrue(Arrays.toString(c) + ": " + e.getMessage(), e.getMessage().contains(c[3]));
            }
        }
        try {
            RhythmTreeReports.render(ScoreNote.toScore(java.util.Collections.<ScoreNote>emptyList()), RhythmTreeReports.Settings.defaults());
            fail("a Score accepted as a tree");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("RhythmTree"));
        }
    }

    /** The rubette: registered, one input, two outputs, a named report reachable by Tab, refusals as errors. */
    public void testRubette() {
        assertTrue(Arrays.asList(BuiltinRubettes.classes).contains(RhythmTreeRubette.class.getName()));
        RhythmTreeRubette rub = new RhythmTreeRubette();
        assertEquals(1, rub.getInCount());
        assertEquals(2, rub.getOutCount());
        assertEquals("Rhythm Tree", rub.getName());
        assertEquals("Score", rub.getGroup());
        assertEquals("Score", rub.getOutTip(0));
        assertEquals("RhythmTree", rub.getOutTip(1));
        rub.process(null);
        assertTrue(rub.getErrors().toString(), rub.getErrors().isEmpty());
        assertEquals(7, ScoreNote.fromScore(rub.getOutput(0)).size());
        assertNotNull(rub.getOutput(1));
        assertEquals("7 notes, proportional timing, last note ends at 19/2", rub.getInfo());
        rub.getView();
        JTextArea text = rub.getTextArea();
        assertEquals("Rhythm tree report", text.getAccessibleContext().getAccessibleName());
        assertTrue(text.isFocusable());
        Set<java.awt.AWTKeyStroke> fwd = text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        assertTrue(fwd.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        Set<java.awt.AWTKeyStroke> back = text.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
        assertTrue(back.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK)));
        RhythmSpanPanel spans = rub.getSpanPanel();
        assertEquals("Rhythm tree spans", spans.getAccessibleContext().getAccessibleName());
        assertTrue(spans.getAccessibleContext().getAccessibleDescription().contains("report"));
        assertEquals(11, spans.getLayoutShown().spans().size());

        RhythmTreeRubette bad = new RhythmTreeRubette();
        bad.setSettingsForTest(new RhythmTreeReports.Settings("4 (1 1", "proportional", "0", 60, 80, 0));
        bad.process(null);
        assertFalse(bad.getErrors().isEmpty());
        assertNull(bad.getOutput(0));
        assertNull(bad.getOutput(1));
        assertTrue(bad.getReportText(), bad.getReportText().contains("missing )"));
        assertEquals("", bad.getInfo());
    }

    /** The report shows a 2 px border in the text colour while focused, keeping its insets. */
    public void testFocusBorder() {
        RhythmTreeRubette rub = new RhythmTreeRubette();
        rub.getView();
        JTextArea text = rub.getTextArea();
        java.awt.Insets before = text.getBorder().getBorderInsets(text);
        for (java.awt.event.FocusListener l : text.getFocusListeners()) {
            l.focusGained(new java.awt.event.FocusEvent(text, java.awt.event.FocusEvent.FOCUS_GAINED));
        }
        assertTrue(text.getBorder() instanceof javax.swing.border.CompoundBorder);
        javax.swing.border.Border outer = ((javax.swing.border.CompoundBorder) text.getBorder()).getOutsideBorder();
        assertEquals(2, ((javax.swing.border.LineBorder) outer).getThickness());
        assertEquals(text.getForeground(), ((javax.swing.border.LineBorder) outer).getLineColor());
        assertEquals(before, text.getBorder().getBorderInsets(text));
        for (java.awt.event.FocusListener l : text.getFocusListeners()) {
            l.focusLost(new java.awt.event.FocusEvent(text, java.awt.event.FocusEvent.FOCUS_LOST));
        }
        assertFalse(text.getBorder() instanceof javax.swing.border.CompoundBorder);
    }
}
