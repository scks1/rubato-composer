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
package org.rubato.math.performance;

/** A symbolic note of a performance kernel: onset and duration in quarters, MIDI pitch and velocity, voice. */
public final class PerfNote {

    public final double onset;
    public final double pitch;
    public final double loudness;
    public final double duration;
    public final int voice;

    public PerfNote(double onset, double pitch, double loudness, double duration, int voice) {
        if (!(duration >= 0)) {
            throw new IllegalArgumentException("duration must not be negative: " + duration);
        }
        this.onset = onset;
        this.pitch = pitch;
        this.loudness = loudness;
        this.duration = duration;
        this.voice = voice;
    }

    public String toString() {
        return "(" + onset + ", " + pitch + ", " + loudness + ", " + duration + ", v" + voice + ")";
    }
}
