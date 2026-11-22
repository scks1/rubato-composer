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

/** A performed note: physical onset and duration in seconds and performed loudness, with its symbolic note. */
public final class PerformedNote {

    public final double onset;
    public final double duration;
    public final double loudness;
    private final PerfNote symbolic;
    private final LPS leaf;

    PerformedNote(PerfNote symbolic, LPS leaf, double onset, double duration, double loudness) {
        this.symbolic = symbolic;
        this.leaf = leaf;
        this.onset = onset;
        this.duration = duration;
        this.loudness = loudness;
    }

    public PerfNote getSymbolic() { return symbolic; }

    /** The stemma leaf that performed this note. */
    public LPS getLeaf() { return leaf; }
}
