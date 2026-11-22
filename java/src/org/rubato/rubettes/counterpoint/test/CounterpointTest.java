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
import org.rubato.math.arith.Rational;
import org.rubato.math.counterpoint.ContrapuntalSymmetries;
import org.rubato.math.counterpoint.FirstSpecies.Kind;
import org.rubato.math.counterpoint.FirstSpecies.Orientation;
import org.rubato.math.counterpoint.StrongDichotomies;
import org.rubato.math.module.ZRing;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.counterpoint.Counterpoint;
import org.rubato.rubettes.counterpoint.SuccessorGeneratorRubette;

/**
 * The headless counterpoint API and the Successor Generator rubette.
 */
public class CounterpointTest extends TestCase {

    /**
     * The Phrygian counterpoint of CCW Fig. 5.6, p. 82 (after Tittel, Der
     * neue Gradus, example 67a): its intervals over the cantus firmus E C D C
     * A A G E F E are 7 0 3 7 0 3 4 0 9 0. The figure's octaves did not survive
     * text extraction; the model reduces pitches mod 12, so the octaves here
     * are a choice. CCW uses the piece as a path of admitted steps in the
     * Fuxian world (Fig. 5.7, p. 83).
     */
    static final int[] PHRYGIAN_CANTUS = { 64, 60, 62, 60, 57, 69, 67, 64, 65, 64 };
    static final int[] PHRYGIAN_DISCANTUS = { 71, 72, 77, 79, 81, 84, 83, 88, 86, 88 };

    static Denotator score(int[] cantus, int[] discantus) {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < cantus.length; i++) {
            notes.add(new ScoreNote(i, cantus[i], 80, 1.0, 0));
        }
        for (int i = 0; i < discantus.length; i++) {
            notes.add(new ScoreNote(i, discantus[i], 80, 1.0, 1));
        }
        return ScoreNote.toScore(notes);
    }

    public void testPhrygianExampleIsCorrect() {
        Counterpoint.Report r = Counterpoint.check(score(PHRYGIAN_CANTUS, PHRYGIAN_DISCANTUS));
        int[] intervals = { 7, 0, 3, 7, 0, 3, 4, 0, 9, 0 };
        for (int i = 0; i < intervals.length; i++) {
            assertEquals(intervals[i], r.getSteps().get(i).y);
        }
        assertTrue(r.text(), r.isCorrect());
        assertEquals(10, r.count(Kind.ADMITTED));
        assertEquals(0, ((PowerDenotator) r.flaggedScore()).getFactorCount());
    }

    public void testParallelFifthsAndDissonanceAreFlagged() {
        // 7 -> 7 on a step of 2 is forbidden in (K/D); 5 is dissonant
        Counterpoint.Report r = Counterpoint.check(score(new int[] { 60, 62, 64, 65 }, new int[] { 67, 69, 69, 72 }));
        assertEquals(Kind.ADMITTED, r.getSteps().get(0).kind);
        assertEquals(Kind.FORBIDDEN, r.getSteps().get(1).kind);
        assertEquals(Kind.DISSONANT, r.getSteps().get(2).kind);
        assertEquals(Kind.UNCHECKED, r.getSteps().get(3).kind);
        List<ScoreNote> f = r.flagged();
        assertEquals(2, f.size());
        assertEquals(new Rational(69), f.get(0).pitch);
        assertEquals(1.0, f.get(0).onset, 0.0);
        assertEquals(2.0, f.get(1).onset, 0.0);
        assertEquals(2, ((PowerDenotator) r.flaggedScore()).getFactorCount());
        assertTrue(r.text().contains("forbidden after interval 7"));
    }

    public void testVoicesAndOrientation() {
        // the discantus below the cantus firmus, read hanging: intervals are cantus - discantus
        Counterpoint.Settings s = Counterpoint.Settings.defaults().withOrientation(Orientation.HANGING).withVoices(1, 0);
        int[] lower = new int[PHRYGIAN_CANTUS.length];
        for (int i = 0; i < lower.length; i++) {
            lower[i] = 2 * PHRYGIAN_CANTUS[i] - PHRYGIAN_DISCANTUS[i];
        }
        Counterpoint.Report r = Counterpoint.check(score(lower, PHRYGIAN_CANTUS), s);
        assertEquals(7, r.getSteps().get(0).y);
    }

    public void testRefusals() {
        assertRefused(score(PHRYGIAN_CANTUS, PHRYGIAN_DISCANTUS).changeAddress(ZRing.ring), "zero-addressed");
        assertRefused(score(new int[] { 60, 62 }, new int[] { 67 }), "one discantus note per cantus firmus note");
        assertRefused(score(new int[] { 60 }, new int[0]), "voice 1");
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        notes.add(new ScoreNote(0, 60, 80, 1.0, 0));
        notes.add(new ScoreNote(0, new Rational(129, 2), 80, 1.0, 1));
        assertRefused(ScoreNote.toScore(notes), "not a whole semitone");
        notes.set(1, new ScoreNote(0.5, 67, 80, 1.0, 1));
        assertRefused(ScoreNote.toScore(notes), "not note against note");
        try {
            Counterpoint.Settings.defaults().withVoices(2, 2);
            fail();
        }
        catch (IllegalArgumentException expected) { /* expected */ }
    }

    public void testSuccessorScore() {
        Denotator d = Counterpoint.successorScore(StrongDichotomies.FUX, Orientation.SWEEPING, 60, 7);
        ContrapuntalSymmetries.Result r = Counterpoint.successors(StrongDichotomies.FUX, 0, 7);
        List<ScoreNote> notes = ScoreNote.fromScore(d);
        assertEquals(2 * r.getSuccessorCount(), notes.size());
        Counterpoint.Report check = Counterpoint.check(d);
        for (int i = 0; i < check.getSteps().size(); i++) {
            int[] s = r.getSuccessors().get(i);
            assertEquals(s[0], check.getSteps().get(i).x);
            assertEquals(s[1], check.getSteps().get(i).y);
            int cf = check.getCantus().get(i).pitch.getNumerator();
            assertTrue(cf >= 55 && cf <= 66);
        }
    }

    public void testSuccessorGeneratorRubette() {
        SuccessorGeneratorRubette rubette = new SuccessorGeneratorRubette();
        rubette.run(null);
        Denotator out = rubette.getOutput(0);
        ContrapuntalSymmetries.Result r = Counterpoint.successors(StrongDichotomies.FUX, 0, 7);
        assertEquals(2 * r.getSuccessorCount(), ((PowerDenotator) out).getFactorCount());
        assertEquals(r.getSuccessorCount() + " successors", rubette.getInfo());
        assertTrue(rubette.getReportText(), rubette.getReportText().contains("Largest intersection a single symmetry gives: "
                + r.getMaxIntersection()));
    }

    private static void assertRefused(Denotator score, String messagePart) {
        try {
            Counterpoint.check(score);
            fail("expected a refusal containing \"" + messagePart + "\"");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(messagePart));
        }
    }
}
