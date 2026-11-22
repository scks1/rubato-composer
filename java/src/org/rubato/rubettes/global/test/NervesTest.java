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

package org.rubato.rubettes.global.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.math.arith.Rational;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.global.NerveRubette;
import org.rubato.rubettes.global.Nerves;

/** The headless nerve API and the Nerve rubette. */
public class NervesTest extends TestCase {

    /** A C-major scale from C4, one note per onset. */
    static Denotator cMajorScale() {
        int[] p = { 60, 62, 64, 65, 67, 69, 71 };
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < p.length; i++) {
            notes.add(new ScoreNote(i, p[i], 80, 1.0, 0));
        }
        return ScoreNote.toScore(notes);
    }

    /** The default settings give the harmonic strip (Topos I p. 262). */
    public void testScaleGivesHarmonicStrip() {
        Nerves.Report r = Nerves.analyze(cMajorScale());
        assertTrue(Arrays.equals(new int[] { 7, 14, 7 }, r.getNerve().fVector()));
        String t = r.text();
        assertTrue(t, t.contains("Euler characteristic 0"));
        assertTrue(t, t.contains("not orientable"));
        assertTrue(t, t.contains("homology H_0.. [Z, Z, 0]"));
        assertTrue(t, t.contains("boundary cycle I - IV - VII - III - VI - II - V - I")
                   || t.contains("boundary cycle I - V - II - VI - III - VII - IV - I"));
        assertTrue(t, t.contains("{I}:10"));
        assertTrue(t, t.contains("{VII}:15"));
    }

    /** Tetrads give the solid torus of AM 2020 sec. 6. */
    public void testTetrads() {
        Nerves.Report r = Nerves.analyze(cMajorScale(), new Nerves.Settings(Nerves.Mode.THIRD_CHAINS, 4, -1));
        assertTrue(Arrays.equals(new int[] { 7, 21, 21, 7 }, r.getNerve().fVector()));
        assertTrue(r.text(), r.text().contains("pseudomanifold, orientable"));
    }

    /** Degree I can be set: A minor natural on A gives the same strip with relabelled degrees. */
    public void testDegreeOne() {
        Nerves.Report r = Nerves.analyze(cMajorScale(), new Nerves.Settings(Nerves.Mode.THIRD_CHAINS, 3, 9));
        assertTrue(r.text(), r.text().contains("degree I = 9"));
        // degree I = A: its triad {9, 0, 4} is minor, weight class 10; VII = G B D weighs 10, II = B D F weighs 15
        assertTrue(r.text(), r.text().contains("{II}:15"));
    }

    /** Chords by onset: I IV V I in C gives three covering sets. */
    public void testChordsByOnset() {
        int[][] chords = { { 60, 64, 67 }, { 65, 69, 72 }, { 67, 71, 74 }, { 60, 64, 67 } };
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int t = 0; t < chords.length; t++) {
            for (int p : chords[t]) {
                notes.add(new ScoreNote(t, p, 80, 1.0, 0));
            }
        }
        Nerves.Report r = Nerves.analyze(ScoreNote.toScore(notes),
                new Nerves.Settings(Nerves.Mode.CHORDS_BY_ONSET, 3, -1));
        assertEquals(3, r.getComposition().coveringSize());
        assertEquals("t0/3", r.getComposition().names().get(0));
        // C-F share C, C-G share G, F-G share nothing: a path, contractible
        assertTrue(Arrays.equals(new int[] { 3, 2 }, r.getNerve().fVector()));
        assertEquals(1, r.getNerve().eulerCharacteristic());
    }

    public void testRefusals() {
        assertRefused(null, Nerves.Settings.defaults(), "null");
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        notes.add(new ScoreNote(0, 60, 80, 1.0, 0));
        notes.add(new ScoreNote(1, 64, 80, 1.0, 0));
        assertRefused(ScoreNote.toScore(notes), Nerves.Settings.defaults(), "exactly seven");
        notes.add(new ScoreNote(2, new Rational(129, 2), 80, 1.0, 0));
        assertRefused(ScoreNote.toScore(notes), Nerves.Settings.defaults(), "not a whole semitone");
        assertRefused(cMajorScale(), new Nerves.Settings(Nerves.Mode.THIRD_CHAINS, 3, 1), "not among");
        try {
            new Nerves.Settings(Nerves.Mode.THIRD_CHAINS, 5, -1);
            fail("chart size 5 must be refused");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("1 to 4"));
        }
    }

    static void assertRefused(Denotator d, Nerves.Settings s, String fragment) {
        try {
            Nerves.analyze(d, s);
            fail("expected a refusal containing " + fragment);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(fragment));
        }
    }

    public void testRubetteMetadata() {
        NerveRubette r = new NerveRubette();
        assertEquals("Nerve", r.getName());
        assertEquals("Score", r.getGroup());
        assertEquals(1, r.getInCount());
        assertEquals(0, r.getOutCount());
        assertNotNull(r.getIcon());
        assertEquals("Not run yet.", r.getReportText());
    }
}
