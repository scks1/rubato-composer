/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.rubato.rubettes.morphing;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.base.Rubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.rubette.Link;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.math.module.RElement;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.builtin.RealArithRubette;
import org.rubato.rubettes.util.MacroNoteGenerator;

/**
 * Checks that the Morphing rubette reports a rubette error, instead of throwing, when an input
 * Score has all its notes at one onset (one note, a chord, or no notes), and that Scores with
 * notes at two or more onsets still morph.
 */
public class MorphingZeroSpanTest extends TestCase {

    private static final RunInfo RUN_INFO = new RunInfo() {
        public boolean stopped() { return false; }
        public void addMessage(RubetteModel rubette, String msg) { }
    };

    private static final String NEEDS_0 = "Input #0 needs notes at two or more different onsets.";
    private static final String NEEDS_1 = "Input #1 needs notes at two or more different onsets.";

    private MacroNoteGenerator generator;

    protected void setUp() {
        this.generator = new MacroNoteGenerator();
    }

    /** A Morphing rubette as the program makes it, from its prototype's newInstance. */
    private static MorphingRubette fresh() {
        return (MorphingRubette) new MorphingRubette().newInstance();
    }

    /** Links each given denotator to the next input of the rubette. */
    private static void connect(Rubette r, Denotator... inputs) {
        RubetteModel model = new RubetteModel(r, r.getName());
        for (int i = 0; i < inputs.length; i++) {
            RealArithRubette source = new RealArithRubette();
            source.setOutput(0, inputs[i]);
            model.setInLink(new Link(new RubetteModel(source, "source" + i), 0, model, i));
        }
    }

    /** Runs the rubette on the inputs, fails if run() throws, and returns its errors. */
    private static List<String> run(Rubette r, Denotator... inputs) {
        connect(r, inputs);
        r.clearErrors();
        try {
            r.run(RUN_INFO);
        }
        catch (RuntimeException e) {
            fail("Morphing.run threw " + e);
        }
        return new ArrayList<String>(r.getErrors());
    }

    private static List<String> list(String... s) {
        List<String> l = new ArrayList<String>();
        for (String x : s) {
            l.add(x);
        }
        return l;
    }

    private PowerDenotator melody(double... pitches) {
        return this.generator.createSimpleMelody(1, pitches);
    }

    /** A Score with one note per pitch, all at onset 0. */
    private PowerDenotator chord(double... pitches) throws Exception {
        List<Denotator> notes = new ArrayList<Denotator>();
        for (double p : pitches) {
            notes.add(this.generator.createNoteDenotator(0, p, 120, 1, 0));
        }
        return new PowerDenotator(NameDenotator.make(""), this.generator.getScoreForm(), notes);
    }

    private PowerDenotator emptyScore() throws Exception {
        return new PowerDenotator(NameDenotator.make(""), this.generator.getScoreForm(),
                                  new ArrayList<Denotator>());
    }

    private static void assertFiniteTimes(PowerDenotator score) throws Exception {
        for (Denotator note : score.getFactors()) {
            double onset = ((RElement) note.getElement(new int[] {0, 0})).getValue();
            double duration = ((RElement) note.getElement(new int[] {3, 0})).getValue();
            assertFalse("onset " + onset, Double.isNaN(onset) || Double.isInfinite(onset));
            assertFalse("duration " + duration, Double.isNaN(duration) || Double.isInfinite(duration));
        }
    }

    public void testTwoOneNoteScoresReportBothInputs() throws Exception {
        MorphingRubette r = fresh();
        assertEquals(list(NEEDS_0, NEEDS_1), run(r, melody(60), melody(64)));
        assertNull(r.getOutput(0));
    }

    public void testAOneNoteFirstInputIsReported() throws Exception {
        MorphingRubette r = fresh();
        assertEquals(list(NEEDS_0), run(r, melody(60), melody(60, 62, 64)));
        assertNull(r.getOutput(0));
    }

    public void testAChordSecondInputIsReported() throws Exception {
        MorphingRubette r = fresh();
        assertEquals(list(NEEDS_1), run(r, melody(60, 62, 64), chord(60, 64, 67)));
        assertNull(r.getOutput(0));
    }

    public void testAnEmptyScoreIsReported() throws Exception {
        MorphingRubette r = fresh();
        assertEquals(list(NEEDS_0), run(r, emptyScore(), melody(60, 62, 64)));
        assertNull(r.getOutput(0));
    }

    public void testTheInputsAreNotChangedByAReportedRun() throws Exception {
        PowerDenotator one = melody(60);
        PowerDenotator three = melody(60, 62, 64);
        PowerDenotator oneCopy = (PowerDenotator) one.copy();
        PowerDenotator threeCopy = (PowerDenotator) three.copy();
        run(fresh(), one, three);
        assertTrue(one.equals(oneCopy));
        assertTrue(three.equals(threeCopy));
    }

    public void testAReportedRunLeavesTheEarlierOutputInPlaceLikeTheOtherInputErrors() throws Exception {
        MorphingRubette r = fresh();
        assertEquals(list(), run(r, melody(60, 62), melody(60, 62, 64)));
        Denotator earlier = r.getOutput(0);
        assertNotNull(earlier);
        assertEquals(list(NEEDS_0), run(r, melody(60), melody(60, 62, 64)));
        assertSame(earlier, r.getOutput(0));
    }

    public void testScoresWithTwoOrMoreOnsetsStillMorph() throws Exception {
        MorphingRubette r = fresh();
        assertEquals(list(), run(r, melody(60, 62), melody(60, 62, 64)));
        PowerDenotator out = (PowerDenotator) r.getOutput(0);
        assertNotNull(out);
        assertFiniteTimes(out);
    }

    public void testAChordPlusALaterNoteStillMorphs() throws Exception {
        PowerDenotator chordThenNote = chord(60, 64, 67);
        chordThenNote.appendFactor(this.generator.createNoteDenotator(2, 72, 120, 1, 0));
        MorphingRubette r = fresh();
        assertEquals(list(), run(r, chordThenNote, melody(60, 62, 64, 65)));
        PowerDenotator out = (PowerDenotator) r.getOutput(0);
        assertNotNull(out);
        assertFiniteTimes(out);
    }
}
