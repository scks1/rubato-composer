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
package org.rubato.math.gesture.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.Gesture;
import org.rubato.math.gesture.HandGesture;
import org.rubato.math.gesture.HandGesture.Hand;
import org.rubato.math.gesture.HandGesture.Reshape;
import org.rubato.math.gesture.HandGesture.Settings;

/**
 * The hand model of Example 60 (Mazzola, The Topos of Music, vol. III,
 * p. 915; section 61.4) with the geometric and physical shaping of
 * Mazzola and Mueller, ICMC 2003 ([772]). Coordinates are (pitch, height,
 * front, time).
 */
public class HandGestureTest extends TestCase {

    /** MM03 Fig. 2: C3 finger 3, C#4 finger 2, C3 finger 3, G3 finger 3, left hand. */
    static final double[][] MM03 = { { 0, 1, 48 }, { 1.5, 1, 61 }, { 2.5, 1, 48 }, { 3.5, 1, 55 } };
    static final int[] MM03_FINGERS = { 3, 2, 3, 3 };

    static Settings onlySpan23(double s) {
        Settings t = Settings.defaults().withHand(Hand.LEFT);
        for (int i = 1; i <= 5; i++) {
            for (int j = i + 1; j <= 5; j++) {
                t = t.withSpan(i, j, 24);
            }
        }
        return t.withSpan(2, 3, s);
    }

    static List<Reshape> of(HandGesture h, int finger) {
        List<Reshape> r = new ArrayList<Reshape>();
        for (Reshape x : h.reshapes()) {
            if (x.finger == finger) {
                r.add(x);
            }
        }
        return r;
    }

    // --- symbolic gesture (Example 59 per finger, MM03 section 3.1) ---

    public void testSymbolicFingerIsPiPerNoteWithWaitAndShift() {
        double[][] notes = { { 0, 1, 60 }, { 2, 1, 64 }, { 3, 0.5, 62 } };
        HandGesture h = HandGesture.of(notes, new int[] { 2, 2, 2 }, Settings.defaults());
        Gesture g = h.symbolic(2);
        assertEquals(4, g.dimension());
        assertEquals(Digraph.arrowN(5 * 3 - 2), g.skeleton());
        double lift = Settings.defaults().lift;
        // first note: hover, down, hold, up
        assertPoint(g.vertex(0), 60, lift, 0, 0);
        assertPoint(g.vertex(1), 60, 0, 0, 0);
        assertPoint(g.vertex(2), 60, 0, 0, 1);
        assertPoint(g.vertex(3), 60, lift, 0, 1);
        // wait keeps the pitch, shift keeps the time (Topos III p. 911)
        assertPoint(g.vertex(4), 60, lift, 0, 2);
        assertPoint(g.vertex(5), 64, lift, 0, 2);
        assertPoint(g.vertex(6), 64, 0, 0, 2);
        assertPoint(g.vertex(13), 62, lift, 0, 3.5);
    }

    public void testBlackKeysAreDeeper() {
        HandGesture h = HandGesture.of(new double[][] { { 0, 1, 61 } }, new int[] { 3 }, Settings.defaults());
        assertEquals(Settings.defaults().blackFront, h.symbolic(3).vertex(1)[2], 1e-12);
        assertEquals(4.0, Settings.defaults().blackFront, 0);
    }

    public void testUnusedFingerIsOneVertex() {
        HandGesture h = HandGesture.of(new double[][] { { 0, 1, 60 } }, new int[] { 1 }, Settings.defaults());
        assertEquals(Digraph.point(), h.symbolic(5).skeleton());
        assertEquals(6, h.curveCount());
    }

    // --- geometric shaping (MM03 section 4.1, Figs. 5 and 6) ---

    public void testMM03ReshapePatternForEverySpanUpTo7() {
        for (int s = 1; s <= 7; s++) {
            HandGesture h = HandGesture.of(MM03, MM03_FINGERS, onlySpan23(s));
            assertTrue("s=" + s + " " + h.problems(), h.performable());
            List<Reshape> f2 = of(h, 2);
            assertEquals("s=" + s + " " + f2, 3, f2.size());
            assertEquals(0, f2.get(0).time, 0);
            assertEquals(2.5, f2.get(1).time, 0);
            assertEquals(3.5, f2.get(2).time, 0);
            assertEquals(48 + s, f2.get(0).to, 0);
            assertTrue(f2.get(2).to > 55);
            List<Reshape> f3 = of(h, 3);
            assertEquals("s=" + s + " " + f3, 1, f3.size());
            assertEquals(1.5, f3.get(0).time, 0);
            assertTrue(f3.get(0).to < 61);
            assertEquals(61 - s, f3.get(0).to, 0);
            assertTrue(of(h, 1).isEmpty() && of(h, 4).isEmpty() && of(h, 5).isEmpty());
        }
    }

    public void testSpan8HasNoCrossingReshape() {
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, onlySpan23(8));
        assertEquals(2, of(h, 2).size());
        assertEquals(0, of(h, 2).get(0).time, 0);
        assertEquals(2.5, of(h, 2).get(1).time, 0);
    }

    public void testMM03PatternWithDefaultSpans() {
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, Settings.defaults().withHand(Hand.LEFT));
        assertTrue(h.problems().toString(), h.performable());
        List<Reshape> f2 = of(h, 2);
        assertEquals(3, f2.size());
        assertEquals(0, f2.get(0).time, 0);
        assertEquals(2.5, f2.get(1).time, 0);
        assertEquals(3.5, f2.get(2).time, 0);
        assertEquals(56, f2.get(2).to, 0);
        assertEquals(1, of(h, 3).size());
        assertEquals(1.5, of(h, 3).get(0).time, 0);
        assertTrue(f2.get(0).reason, f2.get(0).reason.contains("finger 3"));
    }

    public void testCrossingIsReportedAsOrder() {
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, onlySpan23(4));
        Reshape r = of(h, 2).get(2);
        assertTrue(r.reason, r.reason.startsWith("order"));
        assertTrue(of(h, 2).get(0).reason, of(h, 2).get(0).reason.startsWith("span"));
    }

    public void testStartingHandSatisfiesTheConstraints() {
        // MM03: finger 2's first key C#4 is out of reach of finger 3 on C3, so it starts at its e0 reshape
        Settings s = Settings.defaults().withHand(Hand.LEFT);
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, s);
        double t0 = Double.MAX_VALUE;
        for (int f = 1; f <= 5; f++) {
            for (HandGesture.Move m : h.moves(f)) {
                t0 = Math.min(t0, m.start);
            }
        }
        t0 = Math.min(t0, 0) - 1;
        for (int i = 1; i <= 5; i++) {
            for (int j = i + 1; j <= 5; j++) {
                assertTrue(i + "," + j, s.allows(i, h.position(i, t0)[0], j, h.position(j, t0)[0]));
            }
        }
        assertEquals(h.regionPitches().get(0)[1], h.position(2, t0)[0], 0);
    }

    public void testEveryRegionSatisfiesTheConstraints() {
        Random rnd = new Random(7);
        int checked = 0;
        for (int trial = 0; trial < 600; trial++) {
            int n = 2 + rnd.nextInt(6);
            double[][] notes = new double[n][];
            int[] fingers = new int[n];
            double t = 0;
            for (int i = 0; i < n; i++) {
                notes[i] = new double[] { t, 0.5 + rnd.nextInt(3) * 0.5, 55 + rnd.nextInt(14) };
                fingers[i] = 1 + i % 5;
                t += 0.5 * rnd.nextInt(3);
            }
            Settings s = Settings.defaults().withHand(rnd.nextBoolean() ? Hand.LEFT : Hand.RIGHT);
            HandGesture h;
            try {
                h = HandGesture.of(notes, fingers, s);
            }
            catch (IllegalArgumentException e) {
                continue; // one finger on overlapping notes
            }
            if (!h.performable()) {
                continue;
            }
            for (double[] p : h.regionPitches()) {
                for (int i = 1; i <= 5; i++) {
                    for (int j = i + 1; j <= 5; j++) {
                        assertTrue(trial + ": " + i + "," + j, s.allows(i, p[i - 1], j, p[j - 1]));
                    }
                }
            }
            checked++;
        }
        assertTrue("checked " + checked, checked > 100);
    }

    public void testHeldFingersOutOfReachAreNotPerformable() {
        HandGesture h = HandGesture.of(new double[][] { { 0, 1, 48 }, { 0, 1, 72 } }, new int[] { 1, 5 },
                                       Settings.defaults());
        assertFalse(h.performable());
        assertTrue(h.problems().toString(), h.problems().get(0).contains("fingers 1 and 5"));
    }

    public void testHeldFingersCrossedAreNotPerformable() {
        HandGesture h = HandGesture.of(new double[][] { { 0, 1, 64 }, { 0, 1, 62 } }, new int[] { 2, 3 },
                                       Settings.defaults());
        assertFalse(h.performable());
        assertTrue(h.problems().toString(), h.problems().get(0).contains("order"));
        HandGesture left = HandGesture.of(new double[][] { { 0, 1, 64 }, { 0, 1, 62 } }, new int[] { 2, 3 },
                                          Settings.defaults().withHand(Hand.LEFT));
        assertTrue(left.problems().toString(), left.performable());
    }

    public void testFreeFingerWithNoRoomIsNotPerformable() {
        // fingers 2 and 4 hold a whole tone apart; finger 3 must sit between them but also within 1 of the thumb
        Settings s = Settings.defaults().withSpan(1, 3, 1).withSpan(1, 2, 24).withSpan(1, 4, 24);
        HandGesture h = HandGesture.of(new double[][] { { 0, 1, 48 }, { 0, 1, 60 }, { 0, 1, 62 } },
                                       new int[] { 1, 2, 4 }, s);
        assertFalse(h.performable());
        assertTrue(h.problems().toString(), h.problems().get(0).contains("no position for finger 3"));
    }

    // --- refusals ---

    public void testRefusals() {
        double[][] two = { { 0, 1, 60 }, { 1, 1, 62 } };
        refuse(two, new int[] { 1 }, "fingering");
        refuse(two, new int[] { 1, 6 }, "finger 6");
        refuse(two, new int[] { 0, 1 }, "finger 0");
        refuse(new double[][] { { 0, 2, 60 }, { 1, 1, 62 } }, new int[] { 2, 2 }, "overlap");
        refuse(new double[][] { { 0, Double.NaN, 60 } }, new int[] { 2 }, "finite");
        refuse(new double[0][], new int[0], "no notes");
    }

    // --- physical shaping (section 61.4.1 M; section 78.2.5; MM03 section 4.2) ---

    public void testMinimalDurationAndProfile() {
        assertEquals(Math.sqrt(6 * 2.0 / 5000), HandGesture.minimalDuration(2, 5000), 1e-15);
        assertEquals(0, HandGesture.minimalDuration(0, 5000), 0);
        assertEquals(0, HandGesture.fraction(0), 0);
        assertEquals(0.5, HandGesture.fraction(0.5), 1e-15);
        assertEquals(1, HandGesture.fraction(1), 1e-15);
        // velocity on the first ramp is the book's v_y1 (Topos III p. 1194) with t0 = 0, t1 = 1/2, W = 2
        double h = 1e-6;
        for (double t : new double[] { 0.1, 0.2, 0.3, 0.4 }) {
            double v = (HandGesture.fraction(t + h) - HandGesture.fraction(t - h)) / (2 * h);
            double t0 = 0, t1 = 0.5, w = 2;
            double book = (t - t0) * (t - t0) * (2 * t + t0 - 3 * t1) * w / Math.pow(t0 - t1, 3);
            assertEquals(book, v, 1e-6);
        }
    }

    public void testLegatoLeapShortensTheFirstNote() {
        Settings s = Settings.defaults();
        HandGesture h = HandGesture.of(new double[][] { { 0, 1, 60 }, { 1, 1, 65 } }, new int[] { 2, 2 }, s);
        assertTrue(h.problems().toString(), h.performable());
        double a = s.accel[1];
        double need = (HandGesture.minimalDuration(s.lift, a) * 2
                       + HandGesture.minimalDuration(5 * s.semitone, a)) / s.secondsPerUnit;
        assertEquals(1, h.shortenings().size());
        assertEquals(1 - need, h.shortenings().get(0).newEnd, 1e-12);
        assertEquals(1, h.shortenings().get(0).originalEnd, 0);
        // the down move ends exactly at the onset; the up move starts at the shortened end
        List<HandGesture.Move> m = h.moves(2);
        HandGesture.Move last = null;
        for (HandGesture.Move x : m) {
            if (x.to[1] == 0) {
                last = x; // the last down move
            }
        }
        assertEquals(1, last.end, 1e-12);
        assertEquals(0, last.to[1], 0);
        boolean upAtNewEnd = false;
        for (HandGesture.Move x : m) {
            if (x.from[1] == 0 && x.to[1] > 0 && Math.abs(x.start - (1 - need)) < 1e-12) {
                upAtNewEnd = true;
            }
        }
        assertTrue(upAtNewEnd);
        HandGesture strict = HandGesture.of(new double[][] { { 0, 1, 60 }, { 1, 1, 65 } }, new int[] { 2, 2 },
                                            s.withMaxShortening(need - 0.01));
        assertFalse(strict.performable());
        assertTrue(strict.problems().toString(), strict.problems().get(0).contains("shorten"));
    }

    public void testNoShorteningWithEnoughTime() {
        HandGesture h = HandGesture.of(new double[][] { { 0, 1, 60 }, { 2, 1, 65 } }, new int[] { 2, 2 },
                                       Settings.defaults());
        assertTrue(h.shortenings().isEmpty());
        assertTrue(h.performable());
    }

    public void testAccelerationNeverExceedsTheLimit() {
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, Settings.defaults().withHand(Hand.LEFT));
        Settings s = h.settings();
        for (int f = 1; f <= 5; f++) {
            assertTrue(h.peakAcceleration(f) <= s.accel[f - 1] * (1 + 1e-9));
            for (HandGesture.Move m : h.moves(f)) {
                // second differences of the space curve in seconds, in cm/s^2
                double dt = (m.end - m.start) * s.secondsPerUnit / 400;
                for (int k = 1; k < 400; k++) {
                    double t = m.start + k * (m.end - m.start) / 400;
                    double[] a = h.spaceCm(f, t - dt / s.secondsPerUnit), b = h.spaceCm(f, t),
                             c = h.spaceCm(f, t + dt / s.secondsPerUnit);
                    double acc = 0;
                    for (int i = 0; i < 3; i++) {
                        double x = (a[i] - 2 * b[i] + c[i]) / (dt * dt);
                        acc += x * x;
                    }
                    assertTrue(f + " " + Math.sqrt(acc), Math.sqrt(acc) <= s.accel[f - 1] * (1 + 1e-3));
                }
            }
        }
    }

    public void testFasterThanMinimalWouldExceed() {
        // halving a move's duration quadruples its peak acceleration: 6 d / T^2
        double d = 3, a = 5000, t = HandGesture.minimalDuration(d, a);
        assertEquals(a, 6 * d / (t * t), 1e-9);
        assertEquals(4 * a, 6 * d / (t * t / 4), 1e-6);
    }

    public void testCarpusIsTheMeanOfTheFingertips() {
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, Settings.defaults().withHand(Hand.LEFT));
        for (double t : new double[] { 0.5, 1.2, 2, 3, 4 }) {
            double sum = 0;
            for (int f = 1; f <= 5; f++) {
                sum += h.position(f, t)[0];
            }
            double[] c = h.carpus(t);
            assertEquals(sum / 5, c[0], 1e-9);
            assertEquals(Settings.defaults().carpusHeight, c[1], 0);
            assertEquals(Settings.defaults().carpusFront, c[2], 0);
        }
        assertEquals(4, h.physicalCarpus().dimension());
        assertEquals(4, h.physical(2).dimension());
    }

    public void testPhysicalGesturesAreContinuous() {
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, Settings.defaults().withHand(Hand.LEFT));
        for (int f = 1; f <= 5; f++) {
            assertTrue(h.physical(f).endpointGap() < 1e-9);
            assertTrue(h.symbolic(f).endpointGap() < 1e-9);
        }
        assertTrue(h.symbolicCarpus().endpointGap() < 1e-9);
    }

    public void testHeldKeysStayDownForTheirNotes() {
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, Settings.defaults().withHand(Hand.LEFT));
        assertArrayEquals(new double[] { 48, 0, 0 }, h.position(3, 0.5));
        assertArrayEquals(new double[] { 61, 0, 4 }, h.position(2, 2.0));
        assertArrayEquals(new double[] { 55, 0, 0 }, h.position(3, 4.0));
    }

    public void testRecheckCountsViolations() {
        HandGesture h = HandGesture.of(MM03, MM03_FINGERS, Settings.defaults().withHand(Hand.LEFT));
        assertTrue(h.recheckViolations() >= 0);
        HandGesture calm = HandGesture.of(new double[][] { { 0, 1, 60 } }, new int[] { 1 }, Settings.defaults());
        assertEquals(0, calm.recheckViolations());
    }

    static void refuse(double[][] notes, int[] fingers, String part) {
        try {
            HandGesture.of(notes, fingers, Settings.defaults());
            fail("accepted, expected " + part);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(part));
        }
    }

    static void assertArrayEquals(double[] e, double[] a) {
        assertEquals(e.length, a.length);
        for (int i = 0; i < e.length; i++) {
            assertEquals("coordinate " + i, e[i], a[i], 1e-9);
        }
    }

    static void assertPoint(double[] p, double pitch, double height, double front, double time) {
        assertArrayEquals(new double[] { pitch, height, front, time }, p);
    }
}
