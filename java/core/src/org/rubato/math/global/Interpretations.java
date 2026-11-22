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

package org.rubato.math.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The third-chain interpretations X<sup>(1)</sup> &hellip; X<sup>(4)</sup> of a
 * seven-element scale (Mazzola, <i>The Topos of Music</i>, &sect;14.2, Ex. 21,
 * pp. 275&ndash;276): with the scale written x<sub>1</sub>, &hellip;, x<sub>7</sub> in
 * ascending order from its first degree and indexes mod 7, the k-th
 * interpretation has the seven charts {x<sub>i</sub>, x<sub>i+2</sub>, &hellip;,
 * x<sub>i+2(k&minus;1)</sub>}: singletons, thirds, triads and tetrads. The triadic
 * one is the degree interpretation of &sect;13.4.2 (p. 262), whose nerve is the
 * harmonic strip; the tetradic one is the jazz interpretation of Fig. 14.1
 * (p. 276) and of Agust&iacute;n-Aquino and Mazzola (J. Math. Music 14, 2020, &sect;6).
 * <p>
 * The major, melodic minor and harmonic minor scales on C are those of
 * Table 13.4.2.B (p. 262).
 */
public final class Interpretations {

    private Interpretations() { /* static utility */ }

    /** C major {0, 2, 4, 5, 7, 9, 11}. */
    public static final int[] C_MAJOR = { 0, 2, 4, 5, 7, 9, 11 };
    /** C melodic minor {0, 2, 3, 5, 7, 9, 11}. */
    public static final int[] C_MELODIC_MINOR = { 0, 2, 3, 5, 7, 9, 11 };
    /** C harmonic minor {0, 2, 3, 5, 7, 8, 11}. */
    public static final int[] C_HARMONIC_MINOR = { 0, 2, 3, 5, 7, 8, 11 };

    /** The degree names I &hellip; VII. */
    public static final List<String> DEGREES = Arrays.asList("I", "II", "III", "IV", "V", "VI", "VII");

    /**
     * The interpretation X<sup>(k)</sup> of a seven-element subset of Z_n.
     *
     * @param n      the modulus, 12 for PiMod12
     * @param scale  seven distinct classes
     * @param first  the class of degree I; it must be in the scale
     * @param k      the chart size, 1 to 7
     */
    public static GlobalComposition thirdChains(int n, int[] scale, int first, int k) {
        if (k < 1 || k > 7) {
            throw new IllegalArgumentException("Chart size must be 1 to 7, got " + k);
        }
        int[] order = degreeOrder(n, scale, first);
        LocalComposition x = LocalComposition.ofClasses(n, order);
        List<int[]> cover = new ArrayList<int[]>();
        for (int i = 0; i < 7; i++) {
            int[] c = new int[k];
            for (int j = 0; j < k; j++) {
                c[j] = x.indexOf(order[(i + 2 * j) % 7]);
            }
            cover.add(c);
        }
        return GlobalComposition.interpretation(x, DEGREES, cover);
    }

    /** X<sup>(k)</sup> of a scale on C (first degree 0) in PiMod12. */
    public static GlobalComposition thirdChains(int[] scale, int k) {
        return thirdChains(12, scale, scale[0], k);
    }

    /**
     * The seven classes in ascending order from {@code first}, going up
     * mod n.
     */
    public static int[] degreeOrder(int n, int[] scale, int first) {
        if (scale.length != 7) {
            throw new IllegalArgumentException("A third-chain interpretation needs seven classes, got " + scale.length);
        }
        int[] s = new int[7];
        for (int i = 0; i < 7; i++) {
            s[i] = Math.floorMod(scale[i] - first, n);
        }
        Arrays.sort(s);
        for (int i = 1; i < 7; i++) {
            if (s[i] == s[i - 1]) {
                throw new IllegalArgumentException("The scale has a repeated class: " + Arrays.toString(scale));
            }
        }
        if (s[0] != 0) {
            throw new IllegalArgumentException("Degree I " + first + " is not in the scale " + Arrays.toString(scale));
        }
        int[] out = new int[7];
        for (int i = 0; i < 7; i++) {
            out[i] = Math.floorMod(s[i] + first, n);
        }
        return out;
    }

    /**
     * All refinement maps &iota; : I &rarr; J between two interpretations of the
     * same support, i.e. all maps with i &sub; &iota;(i) for every covering set i
     * (Ex. 21, p. 275).
     */
    public static List<int[]> refinements(GlobalComposition fine, GlobalComposition coarse) {
        List<List<Integer>> choices = new ArrayList<List<Integer>>();
        for (int i = 0; i < fine.coveringSize(); i++) {
            List<Integer> c = new ArrayList<Integer>();
            LocalComposition a = fine.coveringComposition(i);
            for (int j = 0; j < coarse.coveringSize(); j++) {
                LocalComposition b = coarse.coveringComposition(j);
                boolean inside = true;
                for (int p = 0; p < a.size() && inside; p++) {
                    inside = b.indexOf(a.point(p)) >= 0;
                }
                if (inside) {
                    c.add(j);
                }
            }
            choices.add(c);
        }
        List<int[]> out = new ArrayList<int[]>();
        extend(choices, new int[choices.size()], 0, out);
        return out;
    }

    private static void extend(List<List<Integer>> choices, int[] cur, int i, List<int[]> out) {
        if (i == cur.length) {
            out.add(cur.clone());
            return;
        }
        for (int j : choices.get(i)) {
            cur[i] = j;
            extend(choices, cur, i + 1, out);
        }
    }
}
