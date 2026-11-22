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

/**
 * A marked seventh chord: a type and a root in Z12 (Cannas, Antonini,
 * Pernazza, MCM 2017, &sect;2). The types are numbered as in that paper:
 * 1 dominant [x, x+4, x+7, x+10], 2 minor [x, x+3, x+7, x+10],
 * 3 half-diminished [x, x+3, x+6, x+10], 4 major [x, x+4, x+7, x+11],
 * 5 diminished [x, x+3, x+6, x+9]. The root is part of the chord, so the
 * three diminished pitch-class sets give twelve distinct chords and there
 * are sixty chords in all.
 */
public final class SeventhChord {

    public static final int DOMINANT = 1, MINOR = 2, HALF_DIMINISHED = 3, MAJOR = 4, DIMINISHED = 5;

    static final int[][] OFFSETS = {
        { 0, 4, 7, 10 }, { 0, 3, 7, 10 }, { 0, 3, 6, 10 }, { 0, 4, 7, 11 }, { 0, 3, 6, 9 }
    };

    static final String[] SUFFIX = { "7", "m7", "m7b5", "maj7", "dim7" };

    static final String[] NAMES = { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };

    private final int type;
    private final int root;

    /**
     * @param type 1 to 5
     * @param root any integer, read mod 12
     */
    public SeventhChord(int type, int root) {
        if (type < 1 || type > 5) {
            throw new IllegalArgumentException("Seventh-chord type must be 1 to 5, got " + type);
        }
        this.type = type;
        this.root = Math.floorMod(root, PitchClasses.N);
    }

    public int type() { return type; }

    public int root() { return root; }

    /** The four pitch classes. */
    public int pitchClasses() {
        int m = 0;
        for (int o : OFFSETS[type - 1]) {
            m |= PitchClasses.of(root + o);
        }
        return m;
    }

    /** The sixty chords, by type and then root. */
    public static List<SeventhChord> all() {
        List<SeventhChord> out = new ArrayList<SeventhChord>();
        for (int t = 1; t <= 5; t++) {
            for (int x = 0; x < PitchClasses.N; x++) {
                out.add(new SeventhChord(t, x));
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * The chord whose pitch classes are degree d of a tetradic tonality, with
     * the degree's first class as root; null if the degree is not one of the
     * five types.
     */
    public static SeventhChord degree(Tonality t, int d) {
        if (t.chordSize() != 4) {
            throw new IllegalArgumentException("Not a tetradic tonality: " + t);
        }
        int root = t.scale()[Math.floorMod(d, 7)];
        int pcs = t.degree(d);
        for (int ty = 1; ty <= 5; ty++) {
            SeventhChord c = new SeventhChord(ty, root);
            if (c.pitchClasses() == pcs) {
                return c;
            }
        }
        return null;
    }

    /** The class of a note name such as "C", "Bb", "B♭", "F#", "F♯". */
    public static int noteClass(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty note name");
        }
        int base = "C D EF G A B".indexOf(Character.toUpperCase(name.charAt(0)));
        if (base < 0) {
            throw new IllegalArgumentException("Not a note name: " + name);
        }
        int x = base;
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == 'b' || c == '\u266d') {
                x--;
            }
            else if (c == '#' || c == '\u266f') {
                x++;
            }
            else {
                throw new IllegalArgumentException("Not a note name: " + name);
            }
        }
        return Math.floorMod(x, PitchClasses.N);
    }

    /** Length of the note name at the start of a symbol (letter plus accidentals). */
    static int noteLength(String s) {
        int i = 1;
        while (i < s.length() && "b#\u266d\u266f".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        return i;
    }

    /**
     * Parses "Cmaj7", "Dm7", "Bm7b5", "G7", "Bbdim7" (also "M7", "&Delta;",
     * "-7", "&oslash;", "o7", and the flat and sharp signs).
     */
    public static SeventhChord parse(String symbol) {
        String s = symbol.trim();
        int n = noteLength(s);
        int root = noteClass(s.substring(0, n));
        String q = s.substring(n).replace('\u266d', 'b');
        int type;
        switch (q) {
            case "7": type = DOMINANT; break;
            case "m7": case "-7": type = MINOR; break;
            case "m7b5": case "\u00f8": case "\u00f87": case "-7b5": type = HALF_DIMINISHED; break;
            case "maj7": case "M7": case "\u0394": case "\u03947": type = MAJOR; break;
            case "dim7": case "o7": case "\u00b07": type = DIMINISHED; break;
            default: throw new IllegalArgumentException("Not a seventh chord: " + symbol);
        }
        return new SeventhChord(type, root);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SeventhChord && ((SeventhChord) o).type == type && ((SeventhChord) o).root == root;
    }

    @Override
    public int hashCode() {
        return type * 31 + root;
    }

    @Override
    public String toString() {
        return NAMES[root] + SUFFIX[type - 1];
    }
}
