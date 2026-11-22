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
import java.util.Arrays;
import java.util.List;

/**
 * Cadential sets (Mazzola, <i>The Topos of Music I</i>, &sect;26.2.1,
 * Def. 87, p. 455). X is the translation orbit of the interpretation S<sup>(k)</sup>:
 * the twelve transposed interpretations, compared as families of chords. A set
 * C of degrees of S<sup>(k)</sup> is cadential when S<sup>(k)</sup> is the only
 * interpretation in X that has every chord of C among its degrees; it is
 * minimal when no proper subset is cadential. Sets of degrees are 7-bit masks
 * (bit 0 = I).
 */
public final class Cadences {

    static final String[] ROMAN = { "I", "II", "III", "IV", "V", "VI", "VII" };

    private Cadences() { /* static utility */ }

    /** The other interpretations of X (transposes with a different chord family). */
    static List<Tonality> others(Tonality t) {
        List<Tonality> out = new ArrayList<Tonality>();
        List<Integer> own = t.chords();
        for (int p = 1; p < PitchClasses.N; p++) {
            Tonality u = t.transpose(p);
            if (!u.chords().equals(own)) {
                out.add(u);
            }
        }
        return out;
    }

    /** Def. 87: true if no other interpretation of X contains all chords of the set. */
    public static boolean isCadential(Tonality t, int degreeSet) {
        return isCadential(t, degreeSet, others(t));
    }

    static boolean isCadential(Tonality t, int degreeSet, List<Tonality> others) {
        for (Tonality u : others) {
            boolean all = true;
            for (int c : t.chordsOf(degreeSet)) {
                if (!u.hasChord(c)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return false;
            }
        }
        return true;
    }

    /**
     * All minimal cadential sets, ordered by size and then by the degrees'
     * order (I first) read as a word.
     */
    public static List<Integer> minimal(Tonality t) {
        List<Tonality> others = others(t);
        List<Integer> cadential = new ArrayList<Integer>();
        for (int m = 0; m < 128; m++) {
            if (isCadential(t, m, others)) {
                cadential.add(m);
            }
        }
        List<Integer> out = new ArrayList<Integer>();
        for (int m : cadential) {
            boolean min = true;
            for (int c : cadential) {
                if (c != m && (c & ~m) == 0) {
                    min = false;
                    break;
                }
            }
            if (min) {
                out.add(m);
            }
        }
        out.sort((a, b) -> {
            int d = Integer.bitCount(a) - Integer.bitCount(b);
            return d != 0 ? d : word(a).compareTo(word(b));
        });
        return out;
    }

    private static String word(int m) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((m & (1 << i)) != 0) {
                b.append((char) ('a' + i));
            }
        }
        return b.toString();
    }

    /** J1 = {I,II}, J2 = {I,IV}, J3 = {II,III}, J4 = {III,IV}, J5 = {V}, J6 = {VII} (BfA p. 3). */
    public static List<Integer> tetradicMajorJ() {
        return Arrays.asList(0b11, 0b1001, 0b110, 0b1100, 0b10000, 0b1000000);
    }

    /** "{II,III}". */
    public static String name(int degreeSet) {
        StringBuilder b = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < 7; i++) {
            if ((degreeSet & (1 << i)) != 0) {
                if (!first) {
                    b.append(',');
                }
                b.append(ROMAN[i]);
                first = false;
            }
        }
        return b.append('}').toString();
    }

    /** The degree set named by "{II,III}" or "II,III". */
    public static int parse(String s) {
        String t = s.trim();
        if (t.startsWith("{")) {
            t = t.substring(1);
        }
        if (t.endsWith("}")) {
            t = t.substring(0, t.length() - 1);
        }
        int m = 0;
        for (String part : t.split(",")) {
            String r = part.trim();
            if (r.isEmpty()) {
                continue;
            }
            int i = Arrays.asList(ROMAN).indexOf(r.replace("7", ""));
            if (i < 0) {
                throw new IllegalArgumentException("Not a degree: " + r);
            }
            m |= 1 << i;
        }
        return m;
    }
}
