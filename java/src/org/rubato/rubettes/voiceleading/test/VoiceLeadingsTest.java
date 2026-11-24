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

package org.rubato.rubettes.voiceleading.test;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import junit.framework.TestCase;

import org.rubato.composer.BuiltinRubettes;
import org.rubato.io.ScoreNote;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.voiceleading.VoiceLeadingSpaceRubette;
import org.rubato.rubettes.voiceleading.VoiceLeadings;

/** The headless voice-leading report and the Voice-Leading Space rubette (sources/VOICE-LEADING-SPEC.md). */
public class VoiceLeadingsTest extends TestCase {

    static Denotator melody(int... pitches) {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < pitches.length; i++) {
            notes.add(new ScoreNote(i, pitches[i], 80, 1.0, 0));
        }
        return ScoreNote.toScore(notes);
    }

    /** Chords, one per onset; each row lists the chord's MIDI pitches. */
    static Denotator chords(int[]... rows) {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < rows.length; i++) {
            for (int p : rows[i]) {
                notes.add(new ScoreNote(i, p, 80, 1.0, 0));
            }
        }
        return ScoreNote.toScore(notes);
    }

    /** The default report: T/I on Z12, transporters from 0 to 4, 48 objective maps of which 24 are conjugations. */
    public void testDefaultReport() {
        VoiceLeadings.Report r = VoiceLeadings.analyze(null, VoiceLeadings.Settings.defaults());
        String t = r.text();
        assertTrue(t, t.contains("T/I on Z12"));
        assertTrue(t, t.contains("12 vertices, 24 group elements, 288 arrows"));
        assertTrue(t, t.contains("from 0 to 4: T4, I4"));
        assertTrue(t, t.contains("48 Sigma_VLS automorphisms = |N(F)| 48 x (|K| = 1)!^24"));
        assertTrue(t, t.contains("induced by conjugation (Ex. 4.3): 24 of 48"));
        assertTrue(t, t.contains("5x+3"));
        assertTrue(t, t.contains("x+3 *"));
        assertFalse(t, t.contains("5x+3 *"));
        assertTrue(t, t.contains("12! x (2!)^144, a 53-digit number"));
        assertTrue(t, t.contains("GIS (Ex. 3.1): no; (0, 0) has 2 transporters"));
        assertTrue(t, t.contains("No Score linked."));
        assertEquals("288 arrows, 48 objective, not a GIS", r.summary());
    }

    /** T/I on the triads is a GIS; C to Am is I4 alone; 1152 objective maps are not listed. */
    public void testTriadsAreAGIS() {
        VoiceLeadings.Settings s = new VoiceLeadings.Settings("TRIADS", "TI", 12, 0, 21);
        VoiceLeadings.Report r = VoiceLeadings.analyze(null, s);
        String t = r.text();
        assertTrue(t, t.contains("T/I on the consonant triads"));
        assertTrue(t, t.contains("from C to Am: I4"));
        assertTrue(t, t.contains("1152 Sigma_VLS automorphisms"));
        assertTrue(t, t.contains("vertex maps: 1152, not listed"));
        assertTrue(t, t.contains("GIS (Ex. 3.1): yes"));
        assertTrue(t, t.contains("24! x (1!)^576, a 24-digit number"));
        assertEquals("576 arrows, 1152 objective, a GIS", r.summary());
    }

    /** D10: a melody is read in onset order, pitch classes mod n. */
    public void testMelodySteps() {
        VoiceLeadings.Report r = VoiceLeadings.analyze(melody(60, 64, 67, 60), VoiceLeadings.Settings.defaults());
        String t = r.text();
        assertTrue(t, t.contains("Score: 4 notes, 3 steps"));
        assertTrue(t, t.contains("step 1: 0 -> 4 (onsets 0 to 1): T4, I4"));
        assertTrue(t, t.contains("step 2: 4 -> 7 (onsets 1 to 2): T3, I11"));
        assertTrue(t, t.contains("step 3: 7 -> 0 (onsets 2 to 3): T5, I7"));
        VoiceLeadings.Report r7 = VoiceLeadings.analyze(melody(60, 64), new VoiceLeadings.Settings("PC", "T", 7, 0, 1));
        assertTrue(r7.text(), r7.text().contains("step 1: 4 -> 1 (onsets 0 to 1): T4"));
    }

    /** D10: chords that are not consonant triads are skipped and named. */
    public void testTriadSteps() {
        VoiceLeadings.Settings s = new VoiceLeadings.Settings("TRIADS", "TI", 12, 0, 0);
        Denotator d = chords(new int[] { 60, 64, 67 }, new int[] { 57, 60, 64 }, new int[] { 60, 64, 68 },
                             new int[] { 55, 59, 62 });
        String t = VoiceLeadings.analyze(d, s).text();
        assertTrue(t, t.contains("Score: 4 onsets, 3 triads, 1 skipped"));
        assertTrue(t, t.contains("onset 2: {0, 4, 8} is not a consonant triad, skipped"));
        assertTrue(t, t.contains("step 1: C -> Am (onsets 0 to 1): I4"));
        assertTrue(t, t.contains("step 2: Am -> G (onsets 1 to 3): I11"));
        String tt = VoiceLeadings.analyze(d, new VoiceLeadings.Settings("TRIADS", "T", 12, 0, 0)).text();
        assertTrue(tt, tt.contains("step 1: C -> Am (onsets 0 to 1): none"));
    }

    /** Refusals: unknown space, Aff on triads, a pair outside the space, a non-semitone pitch. */
    public void testRefusals() {
        String[][] bad = { { "CHORDS", "TI" }, { "TRIADS", "Aff" } };
        for (String[] b : bad) {
            try {
                VoiceLeadings.analyze(null, new VoiceLeadings.Settings(b[0], b[1], 12, 0, 0));
                fail(Arrays.toString(b));
            }
            catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().length() > 10);
            }
        }
        try {
            VoiceLeadings.analyze(null, new VoiceLeadings.Settings("PC", "TI", 7, 0, 7));
            fail("7 is not in Z7");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("0 to 6"));
        }
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        notes.add(new ScoreNote(0, new org.rubato.math.arith.Rational(121, 2), 80, 1.0, 0));
        try {
            VoiceLeadings.analyze(ScoreNote.toScore(notes), VoiceLeadings.Settings.defaults());
            fail("half semitone");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("whole semitone"));
        }
    }

    /** The rubette: registered, reports refusals as errors, and shows a reachable, named report. */
    public void testRubette() {
        assertTrue(Arrays.asList(BuiltinRubettes.classes).contains(VoiceLeadingSpaceRubette.class.getName()));
        VoiceLeadingSpaceRubette rub = new VoiceLeadingSpaceRubette();
        assertEquals(1, rub.getInCount());
        assertEquals(0, rub.getOutCount());
        assertEquals("Voice-Leading Space", rub.getName());
        rub.process(melody(60, 64));
        assertTrue(rub.getErrors().toString(), rub.getErrors().isEmpty());
        assertEquals("288 arrows, 48 objective, not a GIS", rub.getInfo());
        assertTrue(rub.getReportText(), rub.getReportText().contains("step 1: 0 -> 4"));
        rub.getView();
        JTextArea text = rub.getTextArea();
        assertEquals("Voice-leading space report", text.getAccessibleContext().getAccessibleName());
        assertTrue(text.isFocusable());
        Set<java.awt.AWTKeyStroke> fwd = text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        assertTrue(fwd.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        assertTrue(text.getText().contains("288 arrows"));

        VoiceLeadingSpaceRubette bad = new VoiceLeadingSpaceRubette();
        bad.setSettingsForTest("TRIADS", "Aff", 12, 0, 0);
        bad.process(null);
        assertFalse(bad.getErrors().isEmpty());
        assertTrue(bad.getReportText(), bad.getReportText().contains("Aff does not act on the triads"));
        assertEquals("", bad.getInfo());
    }

    /** The report shows a 2 px border in the text colour while focused, with the same insets as without (F-gui-contrast.txt). */
    public void testFocusBorder() {
        VoiceLeadingSpaceRubette rub = new VoiceLeadingSpaceRubette();
        rub.getView();
        JTextArea text = rub.getTextArea();
        java.awt.Insets before = text.getBorder().getBorderInsets(text);
        for (java.awt.event.FocusListener l : text.getFocusListeners()) {
            l.focusGained(new java.awt.event.FocusEvent(text, java.awt.event.FocusEvent.FOCUS_GAINED));
        }
        assertTrue(text.getBorder() instanceof javax.swing.border.CompoundBorder);
        javax.swing.border.Border outer = ((javax.swing.border.CompoundBorder) text.getBorder()).getOutsideBorder();
        assertTrue(outer instanceof javax.swing.border.LineBorder);
        assertEquals(2, ((javax.swing.border.LineBorder) outer).getThickness());
        assertEquals(text.getForeground(), ((javax.swing.border.LineBorder) outer).getLineColor());
        assertEquals(before, text.getBorder().getBorderInsets(text));
        for (java.awt.event.FocusListener l : text.getFocusListeners()) {
            l.focusLost(new java.awt.event.FocusEvent(text, java.awt.event.FocusEvent.FOCUS_LOST));
        }
        assertFalse(text.getBorder() instanceof javax.swing.border.CompoundBorder);
        assertEquals(before, text.getBorder().getBorderInsets(text));
    }
}
