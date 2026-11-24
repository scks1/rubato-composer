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

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import junit.framework.TestCase;

import org.rubato.base.StringProperty;
import org.rubato.composer.BuiltinRubettes;
import org.rubato.io.ScoreNote;
import org.rubato.math.performance.FieldExtraction;
import org.rubato.math.performance.FieldExtraction.Case;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.performance.FieldExtractionRubette;
import org.rubato.rubettes.performance.FieldExtractions;
import org.rubato.rubettes.performance.Performances;

/**
 * Field extraction from a Score and its performance (Mazzola, <i>The Topos of Music II</i>,
 * &sect;47.3, pp. 759-765) and the Field Extraction rubette. Performed Scores count quarters
 * at M.M. 120 (sources/FIELD-EXTRACTION-SPEC.md D1).
 */
public class FieldExtractionsTest extends TestCase {

    /** Twelve notes, pitch, loudness and duration varied so that EHLD bases are regular. */
    static List<ScoreNote> notes() {
        int[] pitch = { 60, 62, 64, 65, 67, 69, 71, 72, 64, 67, 60, 72 };
        int[] loud = { 80, 70, 90, 60, 85, 75, 65, 95, 72, 88, 66, 78 };
        double[] dur = { 1, 0.5, 0.5, 1, 0.75, 0.25, 1, 0.5, 1.5, 0.5, 1, 2 };
        List<ScoreNote> n = new ArrayList<ScoreNote>();
        double e = 0;
        for (int i = 0; i < pitch.length; i++) {
            n.add(new ScoreNote(e, pitch[i], loud[i], dur[i], 0));
            e += i % 3 == 2 ? 1 : 0.5;
        }
        return n;
    }

    static Denotator score() {
        return ScoreNote.toScore(notes());
    }

    static Denotator perform(Performances.Settings s) {
        return Performances.perform(score(), s).getPerformedScore();
    }

    /** At M.M. 120 the performed Score equals the score: the diagonal everywhere (F2, D1). */
    public void testMechanicalPerformanceIsDiagonal() {
        FieldExtractions.Report r = FieldExtractions.analyze(score(), perform(Performances.Settings.scalar(120, 1, 1, 1)),
                                                             "EHLD", "ED", FieldExtraction.Settings.defaults(), 1);
        assertEquals(12, r.result().vectors().size());
        for (FieldExtraction.Vector v : r.result().vectors()) {
            assertEquals(Case.REGULAR, v.getCase());
            for (double z : v.value()) {
                assertEquals("note " + v.index, 1.0, z, 1e-9);
            }
        }
    }

    /** At M.M. 60 onsets and durations double, so the E and D components are 1/2 (F1). */
    public void testConstantTempoSixty() {
        FieldExtractions.Report r = FieldExtractions.analyze(score(), perform(Performances.Settings.scalar(60, 1, 1, 1)),
                                                             "EHLD", "ED", FieldExtraction.Settings.defaults(), 1);
        for (FieldExtraction.Vector v : r.result().vectors()) {
            double[] z = v.value();
            assertEquals("E of note " + v.index, 0.5, z[0], 1e-9);
            assertEquals("H of note " + v.index, 1.0, z[1], 1e-9);
            assertEquals("L of note " + v.index, 1.0, z[2], 1e-9);
            assertEquals("D of note " + v.index, 0.5, z[3], 1e-9);
        }
    }

    /**
     * A linear ritardando from M.M. 120 to 60 over the piece: on ED the extracted E component
     * follows T(E)/120 within 0.1, an O(h) error at these note distances (F3). An independent
     * numpy computation of the same bases (scratch/field-extraction/ritardando_probe.py) gives
     * the largest error 0.0943 at note 1, Z_E = 0.9057.
     */
    public void testRitardandoFollowsTheTempoCurve() {
        List<ScoreNote> n = notes();
        double e0 = n.get(0).onset, e1 = 0;
        for (ScoreNote x : n) {
            e1 = Math.max(e1, x.onset + x.duration);
        }
        Denotator perf = perform(Performances.Settings.tempo(120, 60, Performances.Curve.LINEAR, 0, 0, -1));
        FieldExtractions.Report r = FieldExtractions.analyze(score(), perf, "ED", "ED",
                                                             FieldExtraction.Settings.defaults(), 1);
        double worst = 0;
        for (FieldExtraction.Vector v : r.result().vectors()) {
            assertEquals(Case.REGULAR, v.getCase());
            double e = n.get(v.index).onset;
            double t = 120 + (60 - 120) * (e - e0) / (e1 - e0);
            worst = Math.max(worst, Math.abs(v.value()[0] - t / 120));
        }
        assertTrue("largest E error " + worst, worst < 0.1);
        assertEquals(0.9057, r.result().vectors().get(0).value()[0], 1e-4);
        assertTrue("the field is not the diagonal", r.result().vectors().get(11).value()[0] < 0.8);
    }

    public void testParseCoordinates() {
        assertTrue(Arrays.equals(new int[] { 0, 1, 2, 3 }, FieldExtractions.parseCoordinates("EHLD")));
        assertTrue(Arrays.equals(new int[] { 0, 3 }, FieldExtractions.parseCoordinates(" e d ")));
        assertTrue(Arrays.equals(new int[] { 3, 0 }, FieldExtractions.parseCoordinates("DE")));
        refuseParse("EX", "X");
        refuseParse("EE", "twice");
        refuseParse("  ", "no coordinates");
    }

    private static void refuseParse(String s, String word) {
        try {
            FieldExtractions.parseCoordinates(s);
            fail(s);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(word));
        }
    }

    public void testRefusals() {
        Denotator perf = perform(Performances.Settings.scalar(120, 1, 1, 1));
        refuse(score(), perf, "EHLD", "EX", "X");
        refuse(score(), perf, "EH", "ED", "D is not among the coordinates");
        refuse(score(), perf, "EHLD", "E", "two components");
        List<ScoreNote> less = new ArrayList<ScoreNote>(ScoreNote.fromScore(perf));
        less.remove(0);
        refuse(score(), ScoreNote.toScore(less), "EHLD", "ED", "pitch 60");
        refuse(null, perf, "EHLD", "ED", "score");
    }

    private static void refuse(Denotator s, Denotator p, String coords, String comps, String word) {
        try {
            FieldExtractions.analyze(s, p, coords, comps, FieldExtraction.Settings.defaults(), 1);
            fail(word);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(word));
        }
    }

    /** The report has a header, the case counts and one line per note. */
    public void testReportListsEveryNote() {
        FieldExtractions.Report r = FieldExtractions.analyze(score(), perform(Performances.Settings.scalar(60, 1, 1, 1)),
                                                             "EHLD", "ED", FieldExtraction.Settings.defaults(), 1);
        String t = r.text();
        assertTrue(t, t.contains("12 notes"));
        assertTrue(t, t.contains("12 regular, 0 direction only, 0 no information"));
        assertTrue(t, t.contains("M.M. 120"));
        for (int i = 1; i <= 12; i++) {
            assertTrue("note " + i, t.contains(String.format("note %d:", i)));
        }
        assertTrue(t, t.contains("(0.5000, 1.0000, 1.0000, 0.5000)"));
    }

    /** The interpolation over E and H takes each note's shown components at its own point. */
    public void testInterpolationAtTheNotes() {
        Denotator perf = perform(Performances.Settings.tempo(120, 60, Performances.Curve.LINEAR, 0, 0, -1));
        FieldExtractions.Report r = FieldExtractions.analyze(score(), perf, "EHLD", "ED",
                                                             FieldExtraction.Settings.defaults(), 1);
        FieldExtraction.Interpolation f = r.interpolation();
        assertNotNull(f);
        List<ScoreNote> n = notes();
        for (FieldExtraction.Vector v : r.result().vectors()) {
            ScoreNote x = n.get(v.index);
            double[] at = f.at(new double[] { x.onset, x.pitch.doubleValue() });
            assertEquals(v.value()[0], at[0], 1e-12);
            assertEquals(v.value()[3], at[1], 1e-12);
        }
        double[] far = f.at(new double[] { -50, 60 });
        assertEquals(1.0, far[0], 1e-12);
        assertEquals(1.0, far[1], 1e-12);
    }

    /** D10: the diagonal is green, a quarter turn moves the hue by a quarter; brightness grows with length. */
    public void testColours() {
        assertEquals(1 / 3.0, FieldExtractions.hue(1, 1, 1), 1e-12);
        assertEquals(1 / 3.0 + 0.25, FieldExtractions.hue(-1, 1, 1), 1e-12);
        assertEquals((1 / 3.0 + 0.125 * 2) % 1, FieldExtractions.hue(0, 1, 2), 1e-12);
        assertEquals(0.2, FieldExtractions.brightness(0, 2), 1e-12);
        assertEquals(0.6, FieldExtractions.brightness(1, 2), 1e-12);
        assertEquals(1.0, FieldExtractions.brightness(5, 2), 1e-12);
    }

    public void testRubette() {
        FieldExtractionRubette rub = new FieldExtractionRubette();
        assertEquals("Field Extraction", rub.getName());
        assertEquals("Performance", rub.getGroup());
        assertEquals(2, rub.getInCount());
        assertEquals(0, rub.getOutCount());
        assertEquals("EHLD", ((StringProperty) rub.getProperty(FieldExtractionRubette.COORDINATES_KEY)).getString());
        assertEquals("ED", ((StringProperty) rub.getProperty(FieldExtractionRubette.COMPONENTS_KEY)).getString());
        assertTrue(rub.getReportText().contains("Not run"));
        rub.process(score(), perform(Performances.Settings.scalar(60, 1, 1, 1)));
        assertEquals("12 notes: 12 regular", rub.getInfo());
        assertTrue(rub.getReportText().contains("note 12:"));
        ((StringProperty) rub.getProperty(FieldExtractionRubette.COMPONENTS_KEY)).setString("EQ");
        rub.process(score(), perform(Performances.Settings.scalar(60, 1, 1, 1)));
        assertTrue(rub.getReportText(), rub.getReportText().contains("Q"));
        assertEquals("", rub.getInfo());
        assertTrue(Arrays.asList(BuiltinRubettes.classes)
                         .contains("org.rubato.rubettes.performance.FieldExtractionRubette"));
    }

    /** The view: named, focusable plot and report; Tab leaves the report; the plot describes the counts. */
    public void testViewIsReachableAndDescribed() {
        FieldExtractionRubette rub = new FieldExtractionRubette();
        rub.process(score(), perform(Performances.Settings.tempo(120, 60, Performances.Curve.LINEAR, 0, 0, -1)));
        rub.getView();
        JComponent plot = rub.getPlot(), text = rub.getTextArea();
        assertEquals("Performance field plot", plot.getAccessibleContext().getAccessibleName());
        assertEquals("Performance field report", text.getAccessibleContext().getAccessibleName());
        assertTrue(plot.isFocusable());
        String d = plot.getAccessibleContext().getAccessibleDescription();
        assertTrue(d, d.contains("12 notes"));
        assertTrue(d, d.contains("12 regular"));
        assertTrue(d, d.contains("E and D"));
        assertTrue(d, d.contains("report"));
        Set<java.awt.AWTKeyStroke> fwd = text.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        assertTrue(fwd.contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        assertTrue(rub.getReportText().contains("note 1:"));
    }
    /**
     * Added after mutation M24 survived: the performance swaps the order of two notes of
     * different pitches, so its notes sorted by onset do not line up with the score's.
     */
    public void testPerformanceIsReadThroughTheMatching() {
        List<ScoreNote> s = new ArrayList<ScoreNote>(), p = new ArrayList<ScoreNote>();
        s.add(new ScoreNote(0, 64, 80, 1, 0));
        s.add(new ScoreNote(0.5, 60, 80, 1, 0));
        s.add(new ScoreNote(1.5, 67, 80, 1, 0));
        p.add(new ScoreNote(0.8, 64, 80, 1, 0));
        p.add(new ScoreNote(0.5, 60, 80, 1, 0));
        p.add(new ScoreNote(1.5, 67, 80, 1, 0));
        FieldExtractions.Report r = FieldExtractions.analyze(ScoreNote.toScore(s), ScoreNote.toScore(p), "EH", "EH",
                                                             FieldExtraction.Settings.defaults(), 1);
        assertTrue(r.text(), r.text().contains("note 1: (0.0000, 64.0000) -> (0.8000, 64.0000)"));
        assertTrue(r.text(), r.text().contains("note 2: (0.5000, 60.0000) -> (0.5000, 60.0000)"));
        // the field itself must come from the matched pairs, not from the notes in sorted order
        double[][] sp = { { 0, 64 }, { 0.5, 60 }, { 1.5, 67 } }, pp = { { 0.8, 64 }, { 0.5, 60 }, { 1.5, 67 } };
        FieldExtraction.Result want = FieldExtraction.extract(sp, pp, FieldExtraction.Settings.defaults());
        for (int i = 0; i < 3; i++) {
            assertEquals(want.vectors().get(i).getCase(), r.result().vectors().get(i).getCase());
            assertTrue("note " + (i + 1), Arrays.equals(want.vectors().get(i).value(), r.result().vectors().get(i).value()));
        }
    }
}
