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

import java.util.Arrays;

import org.rubato.math.counterpoint.ZnAffine;

/**
 * An element (&sigma;, v) of S5 &times; Z12^5 acting on marked seventh chords by
 * (t, x) &#8614; (&sigma;(t), x + v<sub>t</sub>) (Cannas, Antonini, Pernazza,
 * MCM 2017). Composition with the right factor acting first is
 * (&sigma;<sub>2</sub>, v<sub>2</sub>)(&sigma;<sub>1</sub>, v<sub>1</sub>) =
 * (&sigma;<sub>2</sub>&sigma;<sub>1</sub>, v<sub>1</sub> + v<sub>2</sub>&#8728;&sigma;<sub>1</sub>),
 * which is the paper's eq. (9). Types are 1 to 5 in the public interface.
 */
public final class PlrqElement {

    final int[] sigma; // 0-based: type t+1 goes to sigma[t]+1
    final int[] v;

    PlrqElement(int[] sigma, int[] v) {
        this.sigma = sigma.clone();
        this.v = new int[5];
        for (int i = 0; i < 5; i++) {
            this.v[i] = Math.floorMod(v[i], PitchClasses.N);
        }
    }

    /**
     * The element whose permutation is the product of the given transpositions
     * of types (each a pair, 1-based) and whose vector is v.
     */
    public static PlrqElement of(int[][] transpositions, int... v) {
        int[] s = { 0, 1, 2, 3, 4 };
        for (int[] tr : transpositions) {
            int a = tr[0] - 1, b = tr[1] - 1;
            for (int i = 0; i < 5; i++) {
                if (s[i] == a) {
                    s[i] = b;
                }
                else if (s[i] == b) {
                    s[i] = a;
                }
            }
        }
        return new PlrqElement(s, v);
    }

    /** (Id, v). */
    public static PlrqElement translationVector(int... v) {
        return new PlrqElement(new int[] { 0, 1, 2, 3, 4 }, v);
    }

    public SeventhChord apply(SeventhChord c) {
        int t = c.type() - 1;
        return new SeventhChord(sigma[t] + 1, c.root() + v[t]);
    }

    /** this &#8728; other: other acts first. */
    public PlrqElement compose(PlrqElement other) {
        int[] s = new int[5];
        int[] w = new int[5];
        for (int t = 0; t < 5; t++) {
            s[t] = sigma[other.sigma[t]];
            w[t] = other.v[t] + v[other.sigma[t]];
        }
        return new PlrqElement(s, w);
    }

    public PlrqElement inverse() {
        int[] s = new int[5];
        int[] w = new int[5];
        for (int t = 0; t < 5; t++) {
            s[sigma[t]] = t;
            w[sigma[t]] = -v[t];
        }
        return new PlrqElement(s, w);
    }

    public boolean isIdentity() {
        for (int t = 0; t < 5; t++) {
            if (sigma[t] != t || v[t] != 0) {
                return false;
            }
        }
        return true;
    }

    /** The type that chords of the given type (1-5) are sent to. */
    public int targetType(int type) {
        return sigma[type - 1] + 1;
    }

    /** On the roots of one type the element is the translation x &#8614; x + v<sub>t</sub>. */
    public ZnAffine rootMap(int type) {
        return new ZnAffine(v[type - 1], 1, PitchClasses.N);
    }

    /** v<sub>1</sub> + &hellip; + v<sub>5</sub> mod 12. */
    public int vectorSum() {
        int s = 0;
        for (int x : v) {
            s += x;
        }
        return Math.floorMod(s, PitchClasses.N);
    }

    /** The vector v, entries in 0..11. */
    public int[] vector() {
        return v.clone();
    }

    /** The permutation as 1-based images of types 1..5. */
    public int[] permutation() {
        int[] p = new int[5];
        for (int t = 0; t < 5; t++) {
            p[t] = sigma[t] + 1;
        }
        return p;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlrqElement && Arrays.equals(sigma, ((PlrqElement) o).sigma)
               && Arrays.equals(v, ((PlrqElement) o).v);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sigma) * 31 + Arrays.hashCode(v);
    }

    @Override
    public String toString() {
        return "(" + Arrays.toString(permutation()) + ", " + Arrays.toString(v) + ")";
    }
}
