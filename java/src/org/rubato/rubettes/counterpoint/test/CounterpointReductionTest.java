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
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.math.counterpoint.FirstSpecies.Orientation;
import org.rubato.rubettes.counterpoint.Counterpoint;
import org.rubato.rubettes.counterpoint.Counterpoint.Reduced;
import org.rubato.rubettes.counterpoint.Counterpoint.Settings;

/** The two-voice reduction of polyphony for the symmetry trace (a departure; 2026-11-23). */
public class CounterpointReductionTest extends TestCase {

    static List<String> dyads(List<ScoreNote> notes) {
        List<String> out = new ArrayList<String>();
        for (ScoreNote n : notes) {
            out.add(n.voice + "@" + n.onset + ":" + n.pitch + "/" + n.duration);
        }
        return out;
    }

    /**
     * Cantus whole notes C4 and D4 against a discantus in quarters: every onset of either voice where
     * both sound is a dyad; a held repeat (G4 tied as two notes) is dropped; a crossing (the discantus
     * on B3 below the cantus C4) is reported; an onset where one voice rests is counted.
     */
    public void testReduce() {
        List<ScoreNote> n = new ArrayList<ScoreNote>();
        n.add(new ScoreNote(0, 60, 80, 4, 0));
        n.add(new ScoreNote(4, 62, 80, 4, 0));
        n.add(new ScoreNote(0, 67, 80, 1, 1));
        n.add(new ScoreNote(1, 67, 80, 1, 1));
        n.add(new ScoreNote(2, 59, 80, 1, 1));
        n.add(new ScoreNote(4, 69, 80, 2, 1));
        n.add(new ScoreNote(8, 71, 80, 1, 1));
        Reduced r = Counterpoint.reduce(n, Settings.defaults());
        assertEquals(Arrays.asList("0@0.0:60/2.0", "1@0.0:67/2.0", "0@2.0:60/2.0", "1@2.0:59/2.0",
                "0@4.0:62/4.0", "1@4.0:69/4.0"), dyads(r.notes()));
        assertEquals(Arrays.asList(2.0), r.crossings());
        assertEquals(1, r.repeats());
        assertEquals(1, r.oneVoiceOnly());
        // the reduced passage is note against note, so the trace runs on it
        assertEquals(2, Counterpoint.trace(r.notes(), Settings.defaults()).size());
        // hanging: the discantus below the cantus is no crossing, the ones above are
        Reduced h = Counterpoint.reduce(n, Settings.defaults().withOrientation(Orientation.HANGING));
        assertEquals(Arrays.asList(0.0, 4.0), h.crossings());
        assertTrue(r.summary(), r.summary().contains("1 voice crossing (at 2)"));
    }

    /** A passage that is already note against note comes back unchanged. */
    public void testAlignedIsUnchanged() {
        List<ScoreNote> n = new ArrayList<ScoreNote>();
        int[] cf = { 62, 65, 64, 62 };
        int[] dc = { 69, 69, 67, 74 };
        for (int i = 0; i < 4; i++) {
            n.add(new ScoreNote(i, cf[i], 80, 1, 0));
            n.add(new ScoreNote(i, dc[i], 80, 1, 1));
        }
        Reduced r = Counterpoint.reduce(n, Settings.defaults());
        assertEquals(dyads(n), dyads(r.notes()));
        assertEquals(0, r.repeats());
        assertTrue(r.crossings().isEmpty());
        assertTrue(Arrays.deepEquals(Counterpoint.intervals(n, Settings.defaults()),
                Counterpoint.intervals(r.notes(), Settings.defaults())));
    }

    /** In a chord of one voice the highest note counts for the discantus and the lowest for the cantus. */
    public void testChordsInAVoice() {
        List<ScoreNote> n = new ArrayList<ScoreNote>();
        n.add(new ScoreNote(0, 48, 80, 1, 0));
        n.add(new ScoreNote(0, 55, 80, 1, 0));
        n.add(new ScoreNote(0, 64, 80, 1, 1));
        n.add(new ScoreNote(0, 72, 80, 1, 1));
        Reduced r = Counterpoint.reduce(n, Settings.defaults());
        assertEquals(Arrays.asList("0@0.0:48/1.0", "1@0.0:72/1.0"), dyads(r.notes()));
    }
}
