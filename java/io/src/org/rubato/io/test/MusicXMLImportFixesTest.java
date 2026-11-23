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

package org.rubato.io.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.io.musicxml.MusicXMLReader;

/**
 * Two faults of exported files met in the Cruda Amarilli test (2026-11-23):
 * tie starts that are never stopped, and measures of rests only that run
 * past the time signature. Synthetic files, one part each.
 */
public class MusicXMLImportFixesTest extends TestCase {

    static final String HEAD = "<?xml version=\"1.0\"?>\n<score-partwise version=\"4.0\"><part-list>"
            + "<score-part id=\"P1\"><part-name>A</part-name></score-part></part-list><part id=\"P1\">";
    static final String ATTR = "<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>";

    static String note(String step, int octave, int duration, String tie) {
        return "<note><pitch><step>" + step + "</step><octave>" + octave + "</octave></pitch><duration>" + duration
                + "</duration>" + (tie == null ? "" : "<tie type=\"" + tie + "\"/>") + "<voice>1</voice></note>";
    }

    static String rest(int duration) {
        return "<note><rest/><duration>" + duration + "</duration><voice>1</voice></note>";
    }

    static List<ScoreNote> read(MusicXMLReader r, String measures) throws IOException {
        String xml = HEAD + measures + "</part></score-partwise>";
        return r.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    static List<String> onsets(List<ScoreNote> notes) {
        List<String> out = new ArrayList<String>();
        for (ScoreNote n : notes) {
            out.add(n.onset + "+" + n.duration);
        }
        return out;
    }

    /** D tied over the barline with a start and no stop: two notes and a warning; with closing, one note. */
    public void testUnstoppedTie() throws IOException {
        String m = "<measure number=\"1\">" + ATTR + rest(2) + note("D", 3, 2, "start") + "</measure>"
                + "<measure number=\"2\">" + note("D", 3, 2, null) + note("E", 3, 2, null) + "</measure>";
        MusicXMLReader r = new MusicXMLReader();
        assertEquals(Arrays.asList("2.0+2.0", "4.0+2.0", "6.0+2.0"), onsets(read(r, m)));
        assertEquals(Arrays.asList("1 tie start in part P1 has no tie stop (first in measure 1); the tied notes stay separate"),
                r.getWarnings());
        r = new MusicXMLReader();
        r.setCloseTies(true);
        assertEquals(Arrays.asList("2.0+4.0", "6.0+2.0"), onsets(read(r, m)));
        assertEquals(Arrays.asList("1 tie start in part P1 has no tie stop (first in measure 1); closed by the next note of the same voice and pitch"),
                r.getWarnings());
    }

    /** A tie start at the end of the part, or followed by another pitch, stays open: warned, never closed. */
    public void testTieWithNoContinuation() throws IOException {
        String m = "<measure number=\"1\">" + ATTR + note("D", 3, 2, "start") + note("E", 3, 2, "start") + "</measure>"
                + "<measure number=\"2\">" + note("F", 3, 4, null) + "</measure>";
        MusicXMLReader r = new MusicXMLReader();
        r.setCloseTies(true);
        assertEquals(Arrays.asList("0.0+2.0", "2.0+2.0", "4.0+4.0"), onsets(read(r, m)));
        assertEquals(1, r.getWarnings().size());
        assertTrue(r.getWarnings().get(0), r.getWarnings().get(0).startsWith("2 tie starts in part P1 have no tie stop"));
    }

    /** Properly stopped ties give no warning, closing on or off. */
    public void testStoppedTieIsQuiet() throws IOException {
        String m = "<measure number=\"1\">" + ATTR + rest(2) + note("D", 3, 2, "start") + "</measure>"
                + "<measure number=\"2\">" + note("D", 3, 2, "stop") + note("E", 3, 2, null) + "</measure>";
        for (boolean close : new boolean[] { false, true }) {
            MusicXMLReader r = new MusicXMLReader();
            r.setCloseTies(close);
            assertEquals(Arrays.asList("2.0+4.0", "6.0+2.0"), onsets(read(r, m)));
            assertEquals(new ArrayList<String>(), r.getWarnings());
        }
    }

    /** Two whole rests in a 4/4 measure: clamped to four quarters with a warning; the next measure starts at 4. */
    public void testOverfullRestMeasure() throws IOException {
        String m = "<measure number=\"1\">" + ATTR + rest(4) + rest(4) + "</measure>"
                + "<measure number=\"2\">" + note("C", 4, 4, null) + "</measure>";
        MusicXMLReader r = new MusicXMLReader();
        assertEquals(Arrays.asList("4.0+4.0"), onsets(read(r, m)));
        assertEquals(Arrays.asList("Measure 1 of part P1 holds only rests and is longer than its time signature; it is cut to the time signature"),
                r.getWarnings());
        r = new MusicXMLReader();
        r.setClampRestMeasures(false);
        assertEquals(Arrays.asList("8.0+4.0"), onsets(read(r, m)));
        assertEquals(Arrays.asList("Measure 1 of part P1 is longer than its time signature; the next measure starts after its last note"),
                r.getWarnings());
    }

    /** An overfull measure with a note in it keeps the earlier behaviour: the next measure starts after it. */
    public void testOverfullMeasureWithNotes() throws IOException {
        String m = "<measure number=\"1\">" + ATTR + rest(4) + note("C", 4, 2, null) + "</measure>"
                + "<measure number=\"2\">" + note("C", 4, 4, null) + "</measure>";
        MusicXMLReader r = new MusicXMLReader();
        assertEquals(Arrays.asList("4.0+2.0", "6.0+4.0"), onsets(read(r, m)));
        assertEquals(Arrays.asList("Measure 1 of part P1 is longer than its time signature; the next measure starts after its last note"),
                r.getWarnings());
    }
}
