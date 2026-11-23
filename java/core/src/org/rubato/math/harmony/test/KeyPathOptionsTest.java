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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.harmony.KeyPath;
import org.rubato.math.harmony.KeyPath.Event;
import org.rubato.math.harmony.KeyPath.Kind;
import org.rubato.math.harmony.KeyPath.Options;
import org.rubato.math.harmony.KeyPath.Reduction;
import org.rubato.math.harmony.PitchClasses;

/**
 * The key-path rules added after the Cruda Amarilli test (2026-11-23). Each
 * chart is synthetic and isolates one rule; the literal rules of stage 7 are
 * run beside the switched rule to show the difference.
 */
public class KeyPathOptionsTest extends TestCase {

    static List<Event> path(String chart, Options o) {
        return KeyPath.analyse(KeyPath.parseChart(chart), o);
    }

    static List<String> summary(List<Event> ev) {
        List<String> out = new ArrayList<String>();
        for (Event e : ev) {
            out.add(e.bar() + ":" + e.kind() + ":" + e.keyName());
        }
        return out;
    }

    /** C's k4 and G's k1 both shown in bar 4: the literal rule modulates twice, preferCurrent keeps C. */
    public void testPreferCurrent() {
        String chart = "C | F G | C | F G Am D | F";
        assertEquals(Arrays.asList("2:START:C", "4:MODULATION:G", "5:MODULATION:C"),
                summary(path(chart, Options.literal(2))));
        assertEquals(Arrays.asList("2:START:C", "4:CONFIRMATION:C", "5:CONFIRMATION:C"),
                summary(path(chart, Options.literal(2).withPreferCurrent(true))));
    }

    /** A modulation one bar after the last one is held back by a three-bar stay. */
    public void testMinStay() {
        String chart = "F G | Am D | F G";
        assertEquals(Arrays.asList("1:START:C", "2:MODULATION:G", "3:MODULATION:C"),
                summary(path(chart, Options.literal(2))));
        assertEquals(Arrays.asList("1:START:C", "2:MODULATION:G"),
                summary(path(chart, Options.literal(2).withMinStay(3))));
        // a stay of one bar changes nothing
        assertEquals(summary(path(chart, Options.literal(2))), summary(path(chart, Options.literal(2).withMinStay(1))));
    }

    /** G to C by k4 has no quantum (the quantized G to C modulations have other cadences). */
    public void testNeedQuantum() {
        String chart = "Am D | F G";
        List<Event> lit = path(chart, Options.literal(2));
        assertEquals(Arrays.asList("1:START:G", "2:MODULATION:C"), summary(lit));
        assertNull(lit.get(1).chosen());
        assertFalse(lit.get(1).quantized().isEmpty());
        assertEquals(Arrays.asList("1:START:G"), summary(path(chart, Options.literal(2).withNeedQuantum(true))));
        // the aria's three modulations all have their quantum, so the switch keeps them
        assertEquals(summary(path(KeyPathTest.ARIA, Options.literal(2))),
                summary(path(KeyPathTest.ARIA, Options.literal(2).withNeedQuantum(true))));
    }

    /** A lone f#&deg; is G's k5 in the literal rule; with dimResolves it needs a following G. */
    public void testDimResolves() {
        assertEquals(Arrays.asList("1:START:C", "2:MODULATION:G"),
                summary(path("F G | F#dim | Em", Options.literal(2))));
        assertEquals(Arrays.asList("1:START:C"),
                summary(path("F G | F#dim | Em", Options.literal(2).withDimResolves(true))));
        assertEquals(Arrays.asList("1:START:C", "2:MODULATION:G"),
                summary(path("F G | F#dim G", Options.literal(2).withDimResolves(true))));
        // the resolution may fall in the next bar; the event is then at the bar of the tonic
        assertEquals(Arrays.asList("1:START:C", "3:MODULATION:G"),
                summary(path("F G | F#dim | G", Options.literal(2).withDimResolves(true))));
    }

    /** Ordered sets: II or IV before V, and V resolving to I within the window. */
    public void testOrdered() {
        assertEquals(Arrays.asList("1:START:C"), summary(path("Dm G | Am", Options.literal(2))));
        assertTrue(path("Dm G | Am", Options.literal(2).withOrdered(true)).isEmpty());
        assertEquals(Arrays.asList("1:START:C"), summary(path("Dm G C", Options.literal(2).withOrdered(true))));
        assertEquals(Arrays.asList("2:START:C"), summary(path("Dm G | C", Options.literal(2).withOrdered(true))));
        // V before II does not count, even when I follows
        assertTrue(path("G Dm C", Options.literal(2).withOrdered(true)).isEmpty());
        // ordered also makes a lone diminished triad wait for its tonic
        assertTrue(path("F#dim | Em", Options.literal(2).withOrdered(true)).isEmpty());
    }

    /** B-flat shows k2 (towards g) while G shows k1: the literal rule modulates to G, modeFirst reads g. */
    public void testModeFirst() {
        String chart = "Bb | F | Eb | Cm | Dm Am D";
        assertEquals(Arrays.asList("3:START:Bb", "5:MODULATION:G"), summary(path(chart, Options.literal(2))));
        List<Event> ev = path(chart, Options.literal(2).withModeFirst(true));
        assertEquals(Arrays.asList("3:START:Bb", "5:MODE_CHANGE:g"), summary(ev));
        assertEquals("R", ev.get(1).plrWord());
        // preferCurrent reaches the same mode change
        assertEquals(summary(ev), summary(path(chart, Options.literal(2).withPreferCurrent(true))));
    }

    /**
     * d harmonic minor: iv and V (g and A) identify it among all 24 keys. Every
     * pair of harmonic minor degrees is minimal cadential among the harmonic
     * minor keys, so i and V (d and A) confirm d in bar 3.
     */
    public void testMinorKeys() {
        String chart = "Dm | Gm A | Dm";
        assertTrue(summary(path(chart, Options.literal(2))).toString(), path(chart, Options.literal(2)).isEmpty());
        List<Event> ev = path(chart, Options.literal(2).withMinorKeys(true));
        assertEquals(Arrays.asList("2:START:d", "3:CONFIRMATION:d"), summary(ev));
        assertEquals(Arrays.asList("{IV,V}", "{I,V}"), ev.get(0).cadenceNames());
        assertTrue(ev.get(0).isMinor());
        assertTrue(ev.get(0).isHarmonicMinor());
        // C major's k4 then a's iv-V: a modulation C -> a between scale types, with no quantum
        List<Event> ca = path("F G | C | Dm E | Am", Options.literal(2).withMinorKeys(true));
        assertEquals(Arrays.asList("1:START:C", "3:MODULATION:a", "4:CONFIRMATION:a"), summary(ca));
        assertTrue(ca.get(1).quantized().isEmpty());
        assertEquals("I4", ca.get(1).tiDual());
    }

    /**
     * Among all 24 keys a lone diminished triad identifies no key (b&deg; is VII of C,
     * II of a and VII of c harmonic minor), so C loses k5; b&deg; with C (I and VII)
     * is a new minimal set.
     */
    public void testWidenedCadentialSets() {
        List<Event> ev = path("F G | Bdim | C", Options.literal(2).withMinorKeys(true));
        assertEquals(Arrays.asList("1:START:C", "3:CONFIRMATION:C"), summary(ev));
        assertEquals(Arrays.asList("{I,VII}"), ev.get(1).cadenceNames());
        assertEquals(Arrays.asList("1:START:C", "2:CONFIRMATION:C"), summary(path("F G | Bdim | C", Options.literal(2))));
    }

    /** The report tells a modulation backed by a quantum from one with no quantum. */
    public void testNoQuantumLabel() {
        String r = KeyPath.report(path("Am D | F G", Options.literal(2)));
        assertTrue(r, r.contains("G -> C, displayed k4 {IV,V} in bar 2, no quantum"));
        String aria = KeyPath.report(path(KeyPathTest.ARIA, Options.literal(2)));
        assertFalse(aria, aria.contains("no quantum"));
        assertTrue(aria, aria.contains("modulation C -> D by k4 {IV,V}"));
    }

    /** The paper's charts give the same path under the literal and the default rules. */
    public void testPaperChartsUnderBothRules() {
        Object[][] cases = { { KeyPathTest.ARIA, 2 }, { KeyPathTest.ECCO, 2 }, { KeyPathTest.MIRA, 4 },
                             { KeyPathTest.MIRA, 2 } };
        for (Object[] c : cases) {
            String chart = (String) c[0];
            int w = (Integer) c[1];
            assertEquals(chart, KeyPath.report(path(chart, Options.literal(w))),
                    KeyPath.report(path(chart, KeyPath.defaults(w))));
        }
    }

    /** Reducing a sonority to a triad for the key path (a departure, off by default). */
    public void testReduce() {
        int g7 = PitchClasses.of(7, 11, 2, 5);
        int g = PitchClasses.of(7, 11, 2);
        int bdim = PitchClasses.of(11, 2, 5);
        assertEquals(g7, KeyPath.reduce(g7, 7, Reduction.NONE));
        assertEquals(g, KeyPath.reduce(g7, 7, Reduction.BASS_TRIAD));
        assertEquals(g, KeyPath.reduce(g7, 7, Reduction.CONTAINED_TRIAD));
        // first inversion: the triad on the bass is b-diminished
        assertEquals(bdim, KeyPath.reduce(g7, 11, Reduction.BASS_TRIAD));
        assertEquals(bdim, KeyPath.reduce(g7, 11, Reduction.CONTAINED_TRIAD));
        // fifth in the bass: no triad on D; the contained triad prefers major or minor to diminished
        assertEquals(g7, KeyPath.reduce(g7, 2, Reduction.BASS_TRIAD));
        assertEquals(g, KeyPath.reduce(g7, 2, Reduction.CONTAINED_TRIAD));
        int am7 = PitchClasses.of(9, 0, 4, 7);
        assertEquals(PitchClasses.of(0, 4, 7), KeyPath.reduce(am7, 0, Reduction.BASS_TRIAD));
        assertEquals(PitchClasses.of(9, 0, 4), KeyPath.reduce(am7, 9, Reduction.CONTAINED_TRIAD));
        // open fifths and unisons hold no triad and stay as they are
        assertEquals(PitchClasses.of(7, 2), KeyPath.reduce(PitchClasses.of(7, 2), 7, Reduction.CONTAINED_TRIAD));
        assertEquals(PitchClasses.of(7), KeyPath.reduce(PitchClasses.of(7), 7, Reduction.CONTAINED_TRIAD));
        // a triad is its own reduction
        assertEquals(bdim, KeyPath.reduce(bdim, 2, Reduction.BASS_TRIAD));
        // C6 over E: C and a both hold the bass and neither has it as root; the lower root wins
        assertEquals(PitchClasses.of(0, 4, 7), KeyPath.reduce(am7, 4, Reduction.CONTAINED_TRIAD));
        assertEquals(am7, KeyPath.reduce(am7, 4, Reduction.BASS_TRIAD));
        // A7 over E: A major and c#-diminished both hold the bass; major or minor wins over the lower root
        assertEquals(PitchClasses.of(9, 1, 4), KeyPath.reduce(PitchClasses.of(9, 1, 4, 7), 4, Reduction.CONTAINED_TRIAD));
    }

    /**
     * The real-music preset keeps Ecco pur and Mira but not the aria: among all 24 keys the lone
     * g#&deg; of bar 12 no longer identifies A, and bar 16's d minor (A then Dm) comes in.
     */
    public void testRealMusicPreset() {
        Options o = Options.realMusic(2);
        assertTrue(o.preferCurrent() && o.minStay() == 2 && o.needQuantum() && o.dimResolves() && o.minorKeys());
        assertFalse(o.ordered() || o.modeFirst());
        assertEquals("window 2, prefer current, stay 2, need quantum, diminished resolves, minor keys", o.toString());
        assertEquals(KeyPath.report(path(KeyPathTest.ECCO, Options.literal(2))), KeyPath.report(path(KeyPathTest.ECCO, o)));
        assertEquals(KeyPath.report(path(KeyPathTest.MIRA, Options.literal(4))),
                KeyPath.report(path(KeyPathTest.MIRA, o.withWindow(4))));
        assertEquals(Arrays.asList("4:START:C", "6:MODULATION:D", "16:MODULATION:d", "19:MODULATION:C"),
                summary(path(KeyPathTest.ARIA, o)));
        // the default stays the paper's literal rule
        assertEquals(Options.literal(3).toString(), KeyPath.defaults(3).toString());
    }

    public void testOptionRefusals() {
        try {
            Options.literal(0);
            fail();
        }
        catch (IllegalArgumentException expected) { /* expected */ }
        try {
            Options.literal(2).withMinStay(-1);
            fail();
        }
        catch (IllegalArgumentException expected) { /* expected */ }
        assertEquals("window 2", Options.literal(2).toString());
    }
}
