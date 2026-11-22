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

package org.rubato.math.harmony;

import org.rubato.math.counterpoint.ZnAffine;

/**
 * One quantized modulation (g, &mu;) from S<sup>(k)</sup> to
 * T<sup>(k)</sup> = e<sup>p</sup>.S<sup>(k)</sup> with its quantum Q, the
 * trace T &cap; Q, the pivots (the degrees of T<sup>(k)</sup> inside the trace)
 * and whether the pivots cover the trace (Mazzola, <i>The Topos of Music I</i>,
 * &sect;27.1.4, Def. 88, p. 470; the asterisk of Agust&iacute;n-Aquino and
 * Mazzola, J. Math. Music 14, 2020, Table 1, marks covered = false).
 */
public final class Quantized {

    private final int p;
    private final int cadence;
    private final int quantum;
    private final ZnAffine modulator;
    private final int trace;
    private final int pivots;
    private final boolean covered;

    Quantized(int p, int cadence, int quantum, ZnAffine modulator, int trace, int pivots, boolean covered) {
        this.p = p;
        this.cadence = cadence;
        this.quantum = quantum;
        this.modulator = modulator;
        this.trace = trace;
        this.pivots = pivots;
        this.covered = covered;
    }

    /** The transposition from source to target, 1 to 11. */
    public int p() { return p; }

    /** The minimal cadential set &mu; of the target, as a degree mask. */
    public int cadence() { return cadence; }

    /** The quantum Q, a pitch-class mask. */
    public int quantum() { return quantum; }

    /** The modulator g, a translation or an inversion of Z12. */
    public ZnAffine modulator() { return modulator; }

    /** T &cap; Q. */
    public int trace() { return trace; }

    /** The pivots, as a degree mask of the target. */
    public int pivots() { return pivots; }

    /** True if the pivot chords cover T &cap; Q. */
    public boolean covered() { return covered; }

    @Override
    public String toString() {
        return "Tr" + p + " " + Cadences.name(cadence) + " Q=" + PitchClasses.toString(quantum) + " "
               + Modulations.name(modulator) + " pivots " + Cadences.name(pivots) + (covered ? "" : "*");
    }
}
