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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A lead-sheet chord symbol read as a root and a set of pitch classes. The
 * qualities are those that occur in the Blues for Alice and Cherokee charts
 * of Agust&iacute;n-Aquino (arXiv 2601.07161) plus the common triads and sixths;
 * anything else is rejected rather than guessed. No voicing, inversion or
 * slash bass is modelled.
 */
public final class ChordSymbol {

    private static final Map<String, int[]> QUALITIES = new LinkedHashMap<String, int[]>();

    static {
        QUALITIES.put("", new int[] { 0, 4, 7 });
        QUALITIES.put("m", new int[] { 0, 3, 7 });
        QUALITIES.put("dim", new int[] { 0, 3, 6 });
        QUALITIES.put("+", new int[] { 0, 4, 8 });
        QUALITIES.put("6", new int[] { 0, 4, 7, 9 });
        QUALITIES.put("m6", new int[] { 0, 3, 7, 9 });
        QUALITIES.put("7", new int[] { 0, 4, 7, 10 });
        QUALITIES.put("m7", new int[] { 0, 3, 7, 10 });
        QUALITIES.put("maj7", new int[] { 0, 4, 7, 11 });
        QUALITIES.put("m7b5", new int[] { 0, 3, 6, 10 });
        QUALITIES.put("dim7", new int[] { 0, 3, 6, 9 });
        QUALITIES.put("+7", new int[] { 0, 4, 8, 10 });
        QUALITIES.put("9", new int[] { 0, 4, 7, 10, 2 });
        QUALITIES.put("m9", new int[] { 0, 3, 7, 10, 2 });
        QUALITIES.put("maj9", new int[] { 0, 4, 7, 11, 2 });
        QUALITIES.put("7b9", new int[] { 0, 4, 7, 10, 1 });
        QUALITIES.put("7#9", new int[] { 0, 4, 7, 10, 3 });
    }

    private final String symbol;
    private final int root;
    private final int pitchClasses;

    private ChordSymbol(String symbol, int root, int pitchClasses) {
        this.symbol = symbol;
        this.root = root;
        this.pitchClasses = pitchClasses;
    }

    /** Parses one symbol such as "A7b9", "F+7", "B&flat;m7", "C#m7". */
    public static ChordSymbol parse(String symbol) {
        String s = symbol.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty chord symbol");
        }
        int n = SeventhChord.noteLength(s);
        int root = SeventhChord.noteClass(s.substring(0, n));
        String q = s.substring(n).replace('\u266d', 'b').replace('\u266f', '#');
        int[] offsets = QUALITIES.get(q);
        if (offsets == null) {
            throw new IllegalArgumentException("Unknown chord quality in " + symbol);
        }
        int m = 0;
        for (int o : offsets) {
            m |= PitchClasses.of(root + o);
        }
        return new ChordSymbol(s, root, m);
    }

    /** Symbols separated by white space. */
    public static List<ChordSymbol> parseAll(String text) {
        List<ChordSymbol> out = new ArrayList<ChordSymbol>();
        for (String part : text.trim().split("\\s+")) {
            if (!part.isEmpty() && !part.equals("|")) {
                out.add(parse(part));
            }
        }
        return out;
    }

    /** Bars separated by "|", symbols within a bar by white space. */
    public static List<List<ChordSymbol>> parseBars(String text) {
        List<List<ChordSymbol>> out = new ArrayList<List<ChordSymbol>>();
        for (String bar : text.split("\\|")) {
            if (!bar.trim().isEmpty()) {
                out.add(Collections.unmodifiableList(parseAll(bar)));
            }
        }
        return out;
    }

    /** The root class. */
    public int root() {
        return root;
    }

    /** The pitch classes as a mask. */
    public int pitchClasses() {
        return pitchClasses;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChordSymbol && ((ChordSymbol) o).root == root
               && ((ChordSymbol) o).pitchClasses == pitchClasses;
    }

    @Override
    public int hashCode() {
        return root * 4099 + pitchClasses;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
