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

package org.rubato.math.harmony.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.counterpoint.Dichotomy;
import org.rubato.math.counterpoint.ZnAffine;
import org.rubato.math.global.Interpretations;
import org.rubato.math.harmony.Cadences;
import org.rubato.math.harmony.KeyPath;
import org.rubato.math.harmony.KeyPath.Event;
import org.rubato.math.harmony.KeyPath.Kind;
import org.rubato.math.harmony.Modulations;
import org.rubato.math.harmony.Modulations.Filter;
import org.rubato.math.harmony.Modulations.Rigidity;
import org.rubato.math.harmony.PitchClasses;
import org.rubato.math.harmony.Quantized;
import org.rubato.math.harmony.Tonality;

/**
 * Quanta and the key path of Agust&iacute;n-Aquino and Curiel-L&oacute;pez,
 * arXiv 2512.18862: Examples 1&ndash;3 and the summary table (&sect;4.1,
 * pp. 5&ndash;6), and the readings of &sect;5.2 (pp. 13&ndash;16). The charts
 * below are synthetic reconstructions built to meet the bar numbers the paper
 * states; they are not Monteverdi's music.
 */
public class KeyPathTest extends TestCase {

    static final Tonality C = Tonality.of(Interpretations.C_MAJOR, 0, 3);

    /** Synthetic 20-bar chart for In questo lieto e fortunato giorno (C, D, A, C). */
    public static final String ARIA = "C | C G | C Am | F G | D | G A | D Bm | Em G | D | Bm F#m | D F#m"
            + " | G#dim A | E A | C#m F#m | A | Dm | G | C | F G | C";

    /** The chord labels under Fig. 8 (Ecco pur) and Fig. 9 (Mira, deh mira Orfeo), one per bar. */
    public static final String ECCO = "Bb | F | Eb | Cm | Dm | Gm";
    public static final String MIRA = "F | C | Dm | G | Em | Am";

    /** p. 5: k1 to k5 are the five minimal cadential sets of the major scale. */
    public void testCadenceNumbers() {
        String[] k = { "{II,V}", "{II,III}", "{III,IV}", "{IV,V}", "{VII}" };
        HashSet<Integer> expected = new HashSet<Integer>();
        for (int i = 0; i < 5; i++) {
            assertEquals("k" + (i + 1), KeyPath.cadenceNumber(Cadences.parse(k[i])));
            expected.add(Cadences.parse(k[i]));
        }
        assertEquals(expected, new HashSet<Integer>(Cadences.minimal(C)));
        assertEquals("", KeyPath.cadenceNumber(Cadences.parse("{I,V}")));
    }

    static Quantized row(Tonality s, Tonality t, String cadence, Filter f) {
        for (Quantized q : Modulations.quantize(s, t, f, Rigidity.TI12)) {
            if (q.cadence() == Cadences.parse(cadence)) {
                return q;
            }
        }
        return null;
    }

    static void assertRow(Tonality s, int p, String cadence, String modulator, int[] trace, String pivots) {
        for (Filter f : Filter.values()) {
            Quantized q = row(s, s.transpose(p), cadence, f);
            assertNotNull(cadence + " to p = " + p + " under " + f, q);
            assertEquals(modulator, Modulations.name(q.modulator()));
            assertEquals(PitchClasses.of(trace), q.trace());
            assertEquals(pivots, Cadences.name(q.pivots()));
        }
    }

    /** Examples 1-3 and the table on p. 6. */
    public void testExamples() {
        assertRow(C, 2, "{IV,V}", "T6\u2218\u22121", new int[] { 1, 2, 4, 7, 9, 11 }, "{II,IV,V,VII}");
        assertRow(C, 7, "{VII}", "T11\u2218\u22121", new int[] { 0, 2, 6, 9, 11 }, "{III,V,VII}");
        assertRow(C, 3, "{II,V}", "T7\u2218\u22121", new int[] { 0, 2, 5, 7, 8, 10 }, "{II,III,V,VII}");
        // quanta as printed: {1,2,4,5,7,9,11}, {0,2,5,6,9,11}, {0,2,5,7,8,9,10,11}
        assertEquals(PitchClasses.of(1, 2, 4, 5, 7, 9, 11), row(C, C.transpose(2), "{IV,V}", Filter.COVERED).quantum());
        assertEquals(PitchClasses.of(0, 2, 5, 6, 9, 11), row(C, C.transpose(7), "{VII}", Filter.COVERED).quantum());
        assertEquals(PitchClasses.of(0, 2, 5, 7, 8, 9, 10, 11), row(C, C.transpose(3), "{II,V}", Filter.COVERED).quantum());
    }

    /** The aria's D to A and A to C are Examples 2 and 3 transposed by 2 and by 9 (finding W4). */
    public void testTransposedExamples() {
        Tonality d = C.transpose(2);
        Tonality a = C.transpose(9);
        assertRow(d, 7, "{VII}", "T3\u2218\u22121", new int[] { 2, 4, 8, 11, 1 }, "{III,V,VII}");
        assertRow(a, 3, "{II,V}", "T1\u2218\u22121", new int[] { 9, 11, 2, 4, 5, 7 }, "{II,III,V,VII}");
        // transposing C to Eb up three semitones gives Eb to Gb, not A to C
        assertEquals(6, C.transpose(3).transpose(3).tonic());
    }

    /** Examples 1 and 3: the trace is a strong dichotomy with a unique polarity; Example 2's has five tones (W3). */
    public void testTracesAsDichotomies() {
        Dichotomy d1 = new Dichotomy(12, 1, 2, 4, 7, 9, 11);
        Dichotomy d3 = new Dichotomy(12, 0, 2, 5, 7, 8, 10);
        assertTrue(d1.isStrong());
        assertTrue(d3.isStrong());
        assertEquals(new ZnAffine(7, -1, 12), d1.polarity());
        assertEquals(new ZnAffine(11, -1, 12), d3.polarity());
        assertEquals(5, PitchClasses.size(row(C, C.transpose(7), "{VII}", Filter.COVERED).trace()));
    }

    static List<Event> path(String chart, int window) {
        return KeyPath.analyse(KeyPath.parseChart(chart), window);
    }

    /** Sec. 5.2, p. 13, on the synthetic chart. */
    public void testAria() {
        List<Event> ev = path(ARIA, 2);
        assertEquals(KeyPath.report(ev), 5, ev.size());
        Event e = ev.get(0);
        assertEquals(Kind.START, e.kind());
        assertEquals("C", e.keyName());
        assertEquals(4, e.bar());
        assertEquals(Arrays.asList("k4"), e.cadenceNames());

        e = ev.get(1);
        assertEquals(Kind.MODULATION, e.kind());
        assertEquals("C", e.fromName());
        assertEquals("D", e.keyName());
        assertEquals(6, e.bar());
        assertEquals(6, e.firstBar());
        assertEquals(Arrays.asList("k4"), e.cadenceNames());
        assertTrue(e.displayedQuantized());
        assertEquals("T6\u2218\u22121", Modulations.name(e.chosen().modulator()));
        assertEquals("{II,IV,V,VII}", Cadences.name(e.chosen().pivots()));
        assertEquals(Arrays.asList("4:G", "6:G", "6:A"), e.pivotBars());
        assertEquals(5, e.tonicBar());

        e = ev.get(2);
        assertEquals("A", e.keyName());
        assertEquals(12, e.bar());
        assertEquals(Arrays.asList("k5"), e.cadenceNames());
        assertEquals("T3\u2218\u22121", Modulations.name(e.chosen().modulator()));
        assertEquals(Arrays.asList("12:g#\u00b0"), e.pivotBars());
        assertEquals(12, e.tonicBar());

        e = ev.get(3);
        assertEquals("C", e.keyName());
        assertEquals(17, e.bar());
        assertEquals(16, e.firstBar());
        assertEquals(Arrays.asList("k1"), e.cadenceNames());
        assertEquals("T1\u2218\u22121", Modulations.name(e.chosen().modulator()));
        assertEquals(Arrays.asList("16:d", "17:G"), e.pivotBars());
        assertEquals(18, e.tonicBar());

        e = ev.get(4);
        assertEquals(Kind.CONFIRMATION, e.kind());
        assertEquals(19, e.bar());
        assertEquals("C", e.keyName());
    }

    /** Ecco pur, Fig. 8 labels: B-flat by k4, then g by k2 = R(k4), then g. */
    public void testEccoPur() {
        List<Event> ev = path(ECCO, 2);
        assertEquals(KeyPath.report(ev), 2, ev.size());
        assertEquals(Kind.START, ev.get(0).kind());
        assertEquals("Bb", ev.get(0).keyName());
        assertEquals(3, ev.get(0).bar());
        assertEquals(Kind.MODE_CHANGE, ev.get(1).kind());
        assertEquals("g", ev.get(1).keyName());
        assertEquals(5, ev.get(1).bar());
        assertEquals(Arrays.asList("k2"), ev.get(1).cadenceNames());
        assertEquals("R", ev.get(1).plrWord());
        assertEquals("I0", ev.get(1).tiDual());
        assertEquals(6, ev.get(1).tonicBar());
    }

    /** Mira, deh mira Orfeo, Fig. 9 labels: C by k1 and k4, then a by k2, with a four-bar window. */
    public void testMira() {
        List<Event> ev = path(MIRA, 4);
        assertEquals(KeyPath.report(ev), 2, ev.size());
        assertEquals("C", ev.get(0).keyName());
        assertEquals(4, ev.get(0).bar());
        assertEquals(Arrays.asList("k1", "k4"), ev.get(0).cadenceNames());
        assertEquals(Kind.MODE_CHANGE, ev.get(1).kind());
        assertEquals("a", ev.get(1).keyName());
        assertEquals(5, ev.get(1).bar());
        assertEquals("R", ev.get(1).plrWord());
        assertEquals("I4", ev.get(1).tiDual());
        assertEquals(6, ev.get(1).tonicBar());
        // with two bars the labels display only k1 = {d,G}: no mode change
        List<Event> two = path(MIRA, 2);
        assertEquals(KeyPath.report(two), 1, two.size());
        assertEquals(Arrays.asList("k1"), two.get(0).cadenceNames());
    }

    public void testReportAndRefusals() {
        String r = KeyPath.report(path(ARIA, 2));
        assertTrue(r, r.contains("bar 6"));
        assertTrue(r, r.contains("C -> D"));
        assertTrue(r, r.contains("T6\u2218\u22121"));
        assertTrue(path("C | Am | F", 2).isEmpty());
        try {
            KeyPath.analyse(KeyPath.parseChart("C"), 0);
            fail();
        }
        catch (IllegalArgumentException expected) { /* expected */ }
    }

    /**
     * After a change of key the window starts at the change: with a three-bar window, the G of
     * bar 3 does not join the A of bar 5 to confirm D, since D's k4 was completed at bar 4.
     */
    public void testWindowStartsAtTheChange() {
        List<Event> ev = path("C | F G | G | A | A", 3);
        // C starts at bar 2 and is confirmed at bar 3 (F G, then G); D comes at bar 4; nothing at bar 5
        assertEquals(KeyPath.report(ev), 3, ev.size());
        assertEquals(Kind.MODULATION, ev.get(2).kind());
        assertEquals("D", ev.get(2).keyName());
        assertEquals(4, ev.get(2).bar());
    }

    /** The two-argument call uses the default reading; on major keys both readings give the same path. */
    public void testDefaultReading() {
        List<List<Integer>> bars = KeyPath.parseChart(ARIA);
        String byDefault = KeyPath.report(KeyPath.analyse(bars, 2));
        assertEquals(KeyPath.report(KeyPath.analyse(bars, 2, Filter.COVERED, Rigidity.AUT_X)), byDefault);
        assertEquals(KeyPath.report(KeyPath.analyse(bars, 2, Filter.COVERED, Rigidity.TI12)), byDefault);
    }
}
