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
 * Subsets of Z12 as 12-bit masks: bit x is set when pitch class x belongs to
 * the set. The harmony package uses masks for chords, scales, quanta and
 * their traces because every definition it implements quantifies over small
 * finite families of such sets.
 */
public final class PitchClasses {

    /** The modulus of PiMod12. */
    public static final int N = 12;

    /** The mask of all twelve classes. */
    public static final int ALL = (1 << N) - 1;

    private PitchClasses() { /* static utility */ }

    /** The mask of the given classes, each read mod 12. */
    public static int of(int... pcs) {
        int m = 0;
        for (int x : pcs) {
            m |= 1 << Math.floorMod(x, N);
        }
        return m;
    }

    /** The classes of a mask in ascending order. */
    public static int[] toArray(int m) {
        int[] out = new int[Integer.bitCount(m & ALL)];
        int k = 0;
        for (int x = 0; x < N; x++) {
            if ((m & (1 << x)) != 0) {
                out[k++] = x;
            }
        }
        return out;
    }

    /** The number of classes. */
    public static int size(int m) {
        return Integer.bitCount(m & ALL);
    }

    /** True if x is in the set. */
    public static boolean contains(int m, int x) {
        return (m & (1 << Math.floorMod(x, N))) != 0;
    }

    /** True if every class of a is in b. */
    public static boolean isSubset(int a, int b) {
        return (a & ~b) == 0;
    }

    /** The set moved by x &#8614; x + t. */
    public static int transpose(int m, int t) {
        t = Math.floorMod(t, N);
        return ((m << t) | (m >>> (N - t))) & ALL;
    }

    /** The set moved by x &#8614; &minus;x. */
    public static int invert(int m) {
        int out = 0;
        for (int x = 0; x < N; x++) {
            if ((m & (1 << x)) != 0) {
                out |= 1 << Math.floorMod(-x, N);
            }
        }
        return out;
    }

    /** The image of the set under an affine map of Z12. */
    public static int apply(ZnAffine g, int m) {
        if (g.getModulus() != N) {
            throw new IllegalArgumentException("Not a map of Z12: " + g);
        }
        int out = 0;
        for (int x = 0; x < N; x++) {
            if ((m & (1 << x)) != 0) {
                out |= 1 << g.apply(x);
            }
        }
        return out;
    }

    /** "{0,4,7}". */
    public static String toString(int m) {
        StringBuilder b = new StringBuilder("{");
        int[] xs = toArray(m);
        for (int i = 0; i < xs.length; i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(xs[i]);
        }
        return b.append('}').toString();
    }
}
