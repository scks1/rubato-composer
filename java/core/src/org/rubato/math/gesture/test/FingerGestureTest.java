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

import junit.framework.TestCase;

import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.FingerGesture;
import org.rubato.math.gesture.Gesture;

/**
 * Finger gestures after Example 59 (Mazzola, The Topos of Music, vol. III,
 * p. 915): down, hold and up segments in FingerSpace = R^3 (height, time,
 * pitch), one finger through all notes in onset order.
 */
public class FingerGestureTest extends TestCase {

    static final double[][] THREE = { { 0, 1, 60 }, { 1, 1, 64 }, { 2, 2, 67 } };

    public void testSingleNoteIsThePiDigraph() {
        // Pi = three arrows d, h, u
        FingerGesture f = FingerGesture.of(new double[][] { { 2, 1, 62 } }, FingerGesture.Settings.defaults());
        Gesture g = f.gesture();
        assertEquals(Digraph.arrowN(3), g.skeleton());
        assertPoint(g.vertex(0), 1, 2 - 0.125, 62);
        assertPoint(g.vertex(1), 0, 2, 62);
        assertPoint(g.vertex(2), 0, 3, 62);
        assertPoint(g.vertex(3), 1, 3 + 0.125, 62);
        // the hold segment stays on the key
        assertPoint(g.curve(1).at(0.5), 0, 2.5, 62);
    }

    static void assertPoint(double[] p, double height, double time, double pitch) {
        assertEquals("height", height, p[0], 1e-12);
        assertEquals("time", time, p[1], 1e-12);
        assertEquals("pitch", pitch, p[2], 1e-12);
    }

    public void testMelodyChainsNotes() {
        FingerGesture f = FingerGesture.of(THREE, FingerGesture.Settings.defaults());
        Gesture g = f.gesture();
        assertEquals(Digraph.arrowN(11), g.skeleton());
        assertTrue(g.endpointGap() <= Gesture.TOLERANCE);
        // the move after the first note goes from above 60 to above 64
        assertPoint(g.curve(3).at(0), 1, 1.125, 60);
        assertPoint(g.curve(3).at(1), 1, 0.875, 64);
        assertEquals(2, f.backwardMoves());
        assertEquals(3, f.noteCount());
    }

    public void testBackwardMovesOnlyWhenNotesOverlap() {
        double[][] spaced = { { 0, 0.5, 60 }, { 1, 0.5, 62 } };
        assertEquals(0, FingerGesture.of(spaced, FingerGesture.Settings.defaults()).backwardMoves());
        FingerGesture.Settings tight = new FingerGesture.Settings(1, 0, 0);
        assertEquals(0, FingerGesture.of(THREE, tight).backwardMoves());
    }

    public void testSortsByOnset() {
        double[][] shuffled = { THREE[2], THREE[0], THREE[1] };
        Gesture a = FingerGesture.of(shuffled, FingerGesture.Settings.defaults()).gesture();
        Gesture b = FingerGesture.of(THREE, FingerGesture.Settings.defaults()).gesture();
        assertEquals(0.0, a.distance(b), 0);
    }

    public void testRefusesBadInput() {
        try {
            FingerGesture.of(new double[0][], FingerGesture.Settings.defaults());
            fail("no notes");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        try {
            FingerGesture.of(new double[][] { { 0, -1, 60 } }, FingerGesture.Settings.defaults());
            fail("negative duration");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new FingerGesture.Settings(-1, 0.1, 0.1);
            fail("negative lift");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testPathLength() {
        // one note, no approach or release: down 1, hold d, up 1
        FingerGesture f = FingerGesture.of(new double[][] { { 0, 2, 60 } }, new FingerGesture.Settings(1, 0, 0));
        assertEquals(4.0, f.pathLength(), 1e-12);
    }
}
