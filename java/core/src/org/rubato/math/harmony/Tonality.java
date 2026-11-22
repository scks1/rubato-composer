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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rubato.math.global.GlobalComposition;
import org.rubato.math.global.Interpretations;

/**
 * A seven-element scale S in Z12 with a tonic and its k-chord
 * interpretation S<sup>(k)</sup>: with s<sub>1</sub>, &hellip;, s<sub>7</sub>
 * the scale in circular order from the tonic and indexes mod 7, degree i is
 * {s<sub>i</sub>, s<sub>i+2</sub>, &hellip;, s<sub>i+2(k&minus;1)</sub>}
 * (Mazzola, <i>The Topos of Music I</i>, &sect;27.1.4, p. 469, for k = 3; the
 * third chains of &sect;14.2, Ex. 21, pp. 275&ndash;276, in general). The
 * degree charts come from {@link Interpretations#thirdChains(int, int[], int, int)}.
 * Degrees are numbered 0 (I) to 6 (VII); a set of degrees is a 7-bit mask.
 */
public final class Tonality {

    private final int[] order;
    private final int k;
    private final int[] degrees;
    private final GlobalComposition interpretation;

    private Tonality(int[] scale, int tonic, int k) {
        this.interpretation = Interpretations.thirdChains(PitchClasses.N, scale, tonic, k);
        this.order = Interpretations.degreeOrder(PitchClasses.N, scale, tonic);
        this.k = k;
        this.degrees = new int[7];
        for (int i = 0; i < 7; i++) {
            degrees[i] = PitchClasses.of(interpretation.coveringComposition(i).classes());
        }
    }

    /**
     * The k-chord interpretation of a seven-element scale.
     *
     * @param scale seven distinct classes, in any order
     * @param tonic the class of degree I; it must be in the scale
     * @param k     the chord size, 1 to 7 (3 for triads, 4 for seventh chords)
     */
    public static Tonality of(int[] scale, int tonic, int k) {
        int[] s = new int[scale.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = Math.floorMod(scale[i], PitchClasses.N);
        }
        return new Tonality(s, Math.floorMod(tonic, PitchClasses.N), k);
    }

    /** The scale with tonic s<sub>1</sub>, as a mask. */
    public static Tonality of(int scaleMask, int tonic, int k) {
        return of(PitchClasses.toArray(scaleMask), tonic, k);
    }

    /** The chord of degree i (0 = I). */
    public int degree(int i) {
        return degrees[Math.floorMod(i, 7)];
    }

    /** The seven degree chords, I to VII. */
    public int[] degrees() {
        return degrees.clone();
    }

    /** The tonic class. */
    public int tonic() {
        return order[0];
    }

    /** The scale classes in circular order from the tonic. */
    public int[] scale() {
        return order.clone();
    }

    /** The scale as a mask. */
    public int scaleMask() {
        return PitchClasses.of(order);
    }

    /** The chord size k. */
    public int chordSize() {
        return k;
    }

    /** The interpretation as a stage-3 global composition. */
    public GlobalComposition interpretation() {
        return interpretation;
    }

    /** e<sup>p</sup>.S<sup>(k)</sup>: scale and tonic moved up by p. */
    public Tonality transpose(int p) {
        int[] s = new int[7];
        for (int i = 0; i < 7; i++) {
            s[i] = order[i] + p;
        }
        return of(s, order[0] + p, k);
    }

    /** The degree chords as an unordered family (sorted masks). */
    public List<Integer> chords() {
        return chordsOf(0x7f);
    }

    /** The chords of a set of degrees, sorted by mask. */
    public List<Integer> chordsOf(int degreeSet) {
        List<Integer> out = new ArrayList<Integer>();
        for (int i = 0; i < 7; i++) {
            if ((degreeSet & (1 << i)) != 0) {
                out.add(degrees[i]);
            }
        }
        Collections.sort(out);
        return out;
    }

    /** True if the chord is one of the degrees. */
    public boolean hasChord(int chord) {
        for (int d : degrees) {
            if (d == chord) {
                return true;
            }
        }
        return false;
    }

    /** The degree whose chord this is, or -1. */
    public int degreeOf(int chord) {
        for (int i = 0; i < 7; i++) {
            if (degrees[i] == chord) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tonality)) {
            return false;
        }
        Tonality t = (Tonality) o;
        return k == t.k && order[0] == t.order[0] && scaleMask() == t.scaleMask();
    }

    @Override
    public int hashCode() {
        return (scaleMask() * 31 + order[0]) * 31 + k;
    }

    @Override
    public String toString() {
        return "S" + PitchClasses.toString(scaleMask()) + " tonic " + order[0] + " (k=" + k + ")";
    }
}
