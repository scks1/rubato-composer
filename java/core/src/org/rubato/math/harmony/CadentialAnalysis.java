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
import java.util.List;

/**
 * Finds the minimal cadential sets of a tonality that a stretch of chords
 * realises, after Agust&iacute;n-Aquino, Blues for Alice (arXiv 2601.07161,
 * &sect;&sect;2, 6, 7). Two readings of a chord's degrees are offered:
 * <ul>
 * <li>LITERAL: a degree counts when its degree chord's pitch classes all lie
 * in the chord symbol's pitch classes;</li>
 * <li>ROOT: a degree counts when the chord's root is that scale degree. This
 * is a departure, a mechanical stand-in for the paper's informal
 * "articulates" (p. 7); it ignores chord quality, alterations and
 * ii&ndash;V syntax.</li>
 * </ul>
 * A cadential set is articulated by a stretch when it is a subset of the
 * stretch's degrees.
 */
public final class CadentialAnalysis {

    /** How a chord is read as degrees. */
    public enum Mode { LITERAL, ROOT }

    private CadentialAnalysis() { /* static utility */ }

    /** The chords of consecutive bars as one list. */
    public static List<ChordSymbol> flatten(List<List<ChordSymbol>> bars) {
        List<ChordSymbol> out = new ArrayList<ChordSymbol>();
        for (List<ChordSymbol> b : bars) {
            out.addAll(b);
        }
        return out;
    }

    /** The lowest degree (0 = I) whose chord lies in the symbol's pitch classes, or -1. */
    public static int literalDegree(Tonality k, ChordSymbol c) {
        for (int i = 0; i < 7; i++) {
            if (PitchClasses.isSubset(k.degree(i), c.pitchClasses())) {
                return i;
            }
        }
        return -1;
    }

    /** The degree of the chord's root in the scale, or -1 if the root is off the scale. */
    public static int rootDegree(Tonality k, ChordSymbol c) {
        int[] s = k.scale();
        for (int i = 0; i < 7; i++) {
            if (s[i] == c.root()) {
                return i;
            }
        }
        return -1;
    }

    /** The degrees of one chord, as a 7-bit mask. */
    public static int degrees(Tonality k, ChordSymbol c, Mode mode) {
        if (mode == Mode.ROOT) {
            int d = rootDegree(k, c);
            return d < 0 ? 0 : 1 << d;
        }
        int m = 0;
        for (int i = 0; i < 7; i++) {
            if (PitchClasses.isSubset(k.degree(i), c.pitchClasses())) {
                m |= 1 << i;
            }
        }
        return m;
    }

    /** The union of the chords' degrees. */
    public static int degrees(Tonality k, List<ChordSymbol> chords, Mode mode) {
        int m = 0;
        for (ChordSymbol c : chords) {
            m |= degrees(k, c, mode);
        }
        return m;
    }

    /** The minimal cadential sets of k contained in the chords' degrees, in Cadences.minimal order. */
    public static List<Integer> articulated(Tonality k, List<ChordSymbol> chords, Mode mode) {
        int d = degrees(k, chords, mode);
        List<Integer> out = new ArrayList<Integer>();
        for (int j : Cadences.minimal(k)) {
            if ((j & ~d) == 0) {
                out.add(j);
            }
        }
        return out;
    }

    /** True if the chord's pitch classes lie in the union of the unit's (BfA &sect;7.1, VII7 in A&flat;9 &cup; B&flat;6). */
    public static boolean realisedByInclusion(int chord, List<ChordSymbol> unit) {
        int u = 0;
        for (ChordSymbol c : unit) {
            u |= c.pitchClasses();
        }
        return PitchClasses.isSubset(chord, u);
    }

    /**
     * BfA Def. 5.2 for the major tetradic sets: A for J3 and J4, B for J1 and
     * J2, C for J5 and J6, null otherwise.
     */
    public static String region(int cadence) {
        List<Integer> j = Cadences.tetradicMajorJ();
        int i = j.indexOf(cadence);
        if (i < 0) {
            return null;
        }
        return i < 2 ? "B" : i < 4 ? "A" : "C";
    }

    /** "J4 {III,IV}" for a major tetradic set of a major tonality, else "{III,IV}". */
    public static String name(Tonality k, int cadence) {
        int i = Cadences.tetradicMajorJ().indexOf(cadence);
        if (i >= 0 && isMajorTetradic(k)) {
            return "J" + (i + 1) + " " + Cadences.name(cadence);
        }
        return Cadences.name(cadence);
    }

    static boolean isMajorTetradic(Tonality k) {
        return k.chordSize() == 4
               && k.scaleMask() == PitchClasses.transpose(PitchClasses.of(0, 2, 4, 5, 7, 9, 11), k.tonic());
    }

    /** A report with one line per bar and one per window of two consecutive bars. */
    public static String report(Tonality k, List<List<ChordSymbol>> bars, Mode mode) {
        return report(k, bars, mode, 2);
    }

    /** A report with one line per bar and one per window of the given number of bars. */
    public static String report(Tonality k, List<List<ChordSymbol>> bars, Mode mode, int window) {
        StringBuilder b = new StringBuilder();
        b.append("Key ").append(SeventhChord.NAMES[k.tonic()]).append(", mode ").append(mode).append('\n');
        for (int i = 0; i < bars.size(); i++) {
            b.append("bar ").append(i + 1).append(": ").append(bars.get(i))
             .append("  degrees ").append(Cadences.name(degrees(k, bars.get(i), mode))).append('\n');
        }
        for (int i = 0; i + window <= bars.size(); i++) {
            List<Integer> a = articulated(k, flatten(bars.subList(i, i + window)), mode);
            if (a.isEmpty()) {
                continue;
            }
            b.append("bars ").append(i + 1).append('-').append(i + window).append(':');
            for (int j : a) {
                b.append(' ').append(name(k, j));
                String r = isMajorTetradic(k) ? region(j) : null;
                if (r != null) {
                    b.append(" (region ").append(r).append(')');
                }
                b.append(';');
            }
            b.append('\n');
        }
        return b.toString();
    }
}
