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

package org.rubato.rubettes.certificates.test;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import org.rubato.rubettes.certificates.AgdaCertificateRubette;
import org.rubato.rubettes.certificates.CertificateReports;

/**
 * The Agda Certificate rubette and its reports (sources/AGDA-CERTIFICATES-SPEC.md
 * section 2 item 5, D7 to D9, D11).
 */
public class CertificateReportsTest extends TestCase {

    private static Denotator melody(int... pitches) {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < pitches.length; i++) {
            notes.add(new ScoreNote(i, pitches[i], 80, 1.0, 0));
        }
        return ScoreNote.toScore(notes);
    }

    private static int count(String s, String sub) {
        int c = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) {
            c++;
        }
        return c;
    }

    /** Without a Score only Ex. B.3 is certified, for C major by default. */
    public void testNoScore() {
        CertificateReports.Report r = CertificateReports.analyze(null, CertificateReports.Settings.defaults());
        String t = r.text();
        assertTrue(t, t.contains("No Score linked"));
        assertTrue(t, t.contains("In C major the dominant triad {G, B, D} contains the leading tone B"));
        String m = r.module();
        assertTrue(m, m.startsWith("{-# OPTIONS --safe --without-K #-}"));
        assertTrue(m, m.contains("\nmodule Certificate where\n"));
        assertTrue(m, m.contains("dominant : DominantContainsLeadingTone 0 major\n"));
        assertFalse(m, m.contains("allInterval :"));
        assertFalse(m, m.contains("inScale :"));
        assertTrue(t, t.endsWith(m));
        assertEquals("1 certificate: dominant holds", r.summary());
    }

    /** D8: an all-interval tetrachord as a melody, in C major (F sharp is outside it). */
    public void testAllIntervalMelody() {
        CertificateReports.Report r = CertificateReports.analyze(melody(60, 61, 64, 66), CertificateReports.Settings.defaults());
        String t = r.text();
        assertTrue(t, t.contains("Score: 4 notes, pitch-class set {0, 1, 4, 6} in Z12"));
        assertTrue(t, t.contains("{0, 1, 4, 6} is all-interval in Z12"));
        assertTrue(t, t.contains("Pitch class 1 (note 2) lies outside C major"));
        String m = r.module();
        assertTrue(m, m.contains("allInterval : AllInterval 12 (0 ∷ 1 ∷ 4 ∷ 6 ∷ [])\n"));
        assertTrue(m, m.contains("inScale : ¬ All (λ p → p ∈ scale 0 major) (0 ∷ 1 ∷ 4 ∷ 6 ∷ [])\n"));
        assertEquals(9, count(m, "\nallInterval "));
        assertEquals("3 certificates: allInterval holds, inScale fails, dominant holds", r.summary());
    }

    /** D7: repeats are kept for membership, removed for the set; mod n for (a), mod 12 for (b). */
    public void testScaleAndModulus() {
        CertificateReports.Settings s = new CertificateReports.Settings(7, 9, "harmonicMinor", "AMinor");
        CertificateReports.Report r = CertificateReports.analyze(melody(57, 59, 57, 68), s);
        String t = r.text();
        assertTrue(t, t.contains("Score: 4 notes, pitch-class set {1, 3, 5} in Z7"));
        assertTrue(t, t.contains("All 4 pitch classes lie in A harmonic minor"));
        assertTrue(t, t.contains("In A harmonic minor the dominant triad {E, Ab, B} contains the leading tone Ab"));
        String m = r.module();
        assertTrue(m, m.contains("\nmodule AMinor where\n"));
        assertTrue(m, m.contains("inScale : All (λ p → p ∈ scale 9 harmonicMinor) (9 ∷ 11 ∷ 9 ∷ 8 ∷ [])\n"));
        assertTrue(m, m.contains("allInterval : ¬ AllInterval 7 (1 ∷ 3 ∷ 5 ∷ [])\n"));
    }

    /** D6: natural minor gives the vacuous certificate; D8: an empty Score is not all-interval. */
    public void testNaturalMinorAndEmptyScore() {
        CertificateReports.Settings s = new CertificateReports.Settings(12, 9, "naturalMinor", "Certificate");
        CertificateReports.Report r = CertificateReports.analyze(ScoreNote.toScore(new ArrayList<ScoreNote>()), s);
        String t = r.text();
        assertTrue(t, t.contains("Score: 0 notes"));
        assertTrue(t, t.contains("{} is not all-interval in Z12: no pair has interval class 0"));
        assertTrue(t, t.contains("holds vacuously"));
        assertTrue(r.module(), r.module().contains("dominant (_ , _ , ())"));
        assertTrue(r.module(), r.module().contains("inScale : All (λ p → p ∈ scale 9 naturalMinor) []\n"));
    }

    /** Refusals: modulus, tonic, mode, module name, a half semitone. */
    public void testRefusals() {
        Object[][] bad = { { 1, 0, "major", "Certificate", "2 to 24" }, { 25, 0, "major", "Certificate", "2 to 24" },
                           { 12, 12, "major", "Certificate", "0 to 11" }, { 12, 0, "dorian", "Certificate", "dorian" },
                           { 12, 0, "major", "certificate", "module name" }, { 12, 0, "major", "Prelude", "module name" } };
        for (Object[] b : bad) {
            try {
                CertificateReports.analyze(null, new CertificateReports.Settings((Integer) b[0], (Integer) b[1], (String) b[2], (String) b[3]));
                fail(Arrays.toString(b));
            }
            catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().contains((String) b[4]));
            }
        }
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        notes.add(new ScoreNote(0, new org.rubato.math.arith.Rational(121, 2), 80, 1.0, 0));
        try {
            CertificateReports.analyze(ScoreNote.toScore(notes), CertificateReports.Settings.defaults());
            fail("half semitone");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("whole semitone"));
        }
    }

    /** The rubette: registered, one input, no output, a named reachable report, refusals as errors. */
    public void testRubette() {
        assertTrue(Arrays.asList(BuiltinRubettes.classes).contains(AgdaCertificateRubette.class.getName()));
        AgdaCertificateRubette rub = new AgdaCertificateRubette();
        assertEquals(1, rub.getInCount());
        assertEquals(0, rub.getOutCount());
        assertEquals("Agda Certificate", rub.getName());
        assertEquals("Score", rub.getGroup());
        rub.process(melody(60, 61, 64, 66));
        assertTrue(rub.getErrors().toString(), rub.getErrors().isEmpty());
        assertEquals("3 certificates: allInterval holds, inScale fails, dominant holds", rub.getInfo());
        rub.getView();
        JTextArea text = rub.getTextArea();
        assertEquals("Agda certificate report", text.getAccessibleContext().getAccessibleName());
        assertTrue(text.isFocusable());
        Set<java.awt.AWTKeyStroke> fwd = text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        assertTrue(fwd.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        assertTrue(text.getText().contains("module Certificate where"));

        AgdaCertificateRubette bad = new AgdaCertificateRubette();
        bad.setSettingsForTest(12, 0, "lydian", "Certificate", null);
        bad.process(null);
        assertFalse(bad.getErrors().isEmpty());
        assertTrue(bad.getReportText(), bad.getReportText().contains("lydian"));
        assertEquals("", bad.getInfo());
    }

    /** D9: with an output file the rubette writes the module as UTF-8, and only the module. */
    public void testWritesModule() throws Exception {
        File dir = Files.createTempDirectory("agda-cert").toFile();
        File out = new File(dir, "Certificate.agda");
        AgdaCertificateRubette rub = new AgdaCertificateRubette();
        rub.setSettingsForTest(12, 0, "major", "Certificate", out);
        rub.process(melody(60, 64, 67));
        assertTrue(rub.getErrors().toString(), rub.getErrors().isEmpty());
        String written = new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8);
        assertEquals(CertificateReports.analyze(melody(60, 64, 67), CertificateReports.Settings.defaults()).module(), written);
        assertTrue(rub.getReportText(), rub.getReportText().contains("Written to " + out.getPath()));

        File wrongName = new File(dir, "Other.agda");
        AgdaCertificateRubette r2 = new AgdaCertificateRubette();
        r2.setSettingsForTest(12, 0, "major", "Certificate", wrongName);
        r2.process(null);
        assertFalse(r2.getErrors().isEmpty());
        assertTrue(r2.getReportText(), r2.getReportText().contains("Certificate.agda"));
        assertFalse(wrongName.exists());
        out.delete();
        dir.delete();
    }

    /** The report shows a 2 px border in the text colour while focused, with the same insets as without. */
    public void testFocusBorder() {
        AgdaCertificateRubette rub = new AgdaCertificateRubette();
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
