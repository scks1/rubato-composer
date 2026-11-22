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

package org.rubato.math.counterpoint;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The six classes of strong dichotomies of Z_12 (Topos I p. 518), with the
 * representatives CCW Table 3.2 (p. 36) gives and the class numbers of
 * Mazzola's classification (Topos IV App. N.1) that CCW Table 3.1 uses.
 * The numbers are names copied from the book; they are not recomputed.
 */
public final class StrongDichotomies {

    private StrongDichotomies() { /* static utility */ }

    /** Class 64, the major-scale dichotomy (I/J). */
    public static final Dichotomy CLASS_64 = new Dichotomy(12, 2, 4, 5, 7, 9, 11);
    /** Class 68. */
    public static final Dichotomy CLASS_68 = new Dichotomy(12, 0, 1, 2, 3, 5, 8);
    /** Class 71. */
    public static final Dichotomy CLASS_71 = new Dichotomy(12, 0, 1, 2, 3, 6, 7);
    /** Class 75. */
    public static final Dichotomy CLASS_75 = new Dichotomy(12, 0, 1, 2, 4, 5, 8);
    /** Class 78. */
    public static final Dichotomy CLASS_78 = new Dichotomy(12, 0, 1, 2, 4, 6, 10);
    /** Class 82, the Fux consonances (K/D): unison, thirds, fifth, sixths. */
    public static final Dichotomy CLASS_82 = new Dichotomy(12, 0, 3, 4, 7, 8, 9);

    /** The Fux dichotomy (K/D), the default for first-species checks. */
    public static final Dichotomy FUX = CLASS_82;

    private static final Map<Integer, Dichotomy> BY_CLASS;

    static {
        Map<Integer, Dichotomy> m = new LinkedHashMap<Integer, Dichotomy>();
        m.put(64, CLASS_64);
        m.put(68, CLASS_68);
        m.put(71, CLASS_71);
        m.put(75, CLASS_75);
        m.put(78, CLASS_78);
        m.put(82, CLASS_82);
        BY_CLASS = Collections.unmodifiableMap(m);
    }

    /** Class number to representative, in increasing class order. */
    public static Map<Integer, Dichotomy> all() {
        return BY_CLASS;
    }

    /**
     * The representative of a class.
     *
     * @throws IllegalArgumentException if the number is not one of 64, 68, 71, 75, 78, 82
     */
    public static Dichotomy ofClass(int number) {
        Dichotomy d = BY_CLASS.get(number);
        if (d == null) {
            throw new IllegalArgumentException("No strong dichotomy class " + number
                    + "; the classes are " + BY_CLASS.keySet());
        }
        return d;
    }
}
