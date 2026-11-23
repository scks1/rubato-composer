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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rubato.math.counterpoint.ZnAffine;

/**
 * The 24 major and minor triads with the TI group (T<sub>n</sub>(x) = x + n,
 * I<sub>n</sub>(x) = n &minus; x) and the PLR group (parallel, Leittonwechsel,
 * relative) acting on them, as Agust&iacute;n-Aquino and Curiel-L&oacute;pez
 * use them (arXiv 2512.18862, &sect;4.2, pp. 6&ndash;7).
 * <p>
 * Triad r (0 &le; r &lt; 12) is the major triad {r, r+4, r+7}; triad 12 + r is
 * the minor triad {r, r+3, r+7}. P, L and R are the involutions
 * C &#8596; c, C &#8596; e and C &#8596; a, transposed. This is a separate class
 * from the seventh-chord group {@link Plrq}; it shares {@link PitchClasses}
 * and {@link ZnAffine}. Permutations are arrays p with p[x] the image of x;
 * {@link #compose(int[], int[])} returns p &#8728; q (q first), and a word
 * such as "RL" means R &#8728; L, L applied first.
 */
public final class Triads {

    /** The number of triads. */
    public static final int COUNT = 24;

    private static final String[] NAMES = { "C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B" };

    private static List<int[]> tiGroup;
    private static List<int[]> plrGroup;

    private Triads() { /* static utility */ }

    /** The major triad on root r. */
    public static int major(int r) { return Math.floorMod(r, 12); }

    /** The minor triad on root r. */
    public static int minor(int r) { return 12 + Math.floorMod(r, 12); }

    public static boolean isMajor(int t) { return t < 12; }

    public static int root(int t) { return t % 12; }

    /** The pitch classes of triad t as a 12-bit mask. */
    public static int pitchClasses(int t) {
        int r = root(t);
        return PitchClasses.of(r, r + (isMajor(t) ? 4 : 3), r + 7);
    }

    /** The triad with exactly these pitch classes, or &minus;1. */
    public static int fromPitchClasses(int mask) {
        for (int t = 0; t < COUNT; t++) {
            if (pitchClasses(t) == mask) {
                return t;
            }
        }
        return -1;
    }

    /** "C", "Bb" for major triads, "a", "c#" for minor ones. */
    public static String name(int t) {
        String n = NAMES[root(t)];
        return isMajor(t) ? n : n.toLowerCase();
    }

    /** Parses a name as {@link #name(int)} writes it; an upper-case letter means major. */
    public static int parse(String s) {
        String t = s.trim();
        boolean maj = Character.isUpperCase(t.charAt(0));
        String key = Character.toUpperCase(t.charAt(0)) + t.substring(1);
        for (int r = 0; r < 12; r++) {
            if (NAMES[r].equals(key)) {
                return maj ? major(r) : minor(r);
            }
        }
        throw new IllegalArgumentException("Unknown triad " + s);
    }

    /** T<sub>n</sub> applied to triad t. */
    public static int T(int n, int t) {
        return isMajor(t) ? major(root(t) + n) : minor(root(t) + n);
    }

    /** The image of triad t under an affine map x &#8614; ux + t with u = &plusmn;1. */
    public static int apply(ZnAffine g, int t) {
        return fromPitchClasses(PitchClasses.apply(g, pitchClasses(t)));
    }

    /** Parallel: C &#8596; c. */
    public static int P(int t) {
        return isMajor(t) ? minor(root(t)) : major(root(t));
    }

    /** Leittonwechsel: C &#8596; e. */
    public static int L(int t) {
        return isMajor(t) ? minor(root(t) + 4) : major(root(t) - 4);
    }

    /** Relative: C &#8596; a. */
    public static int R(int t) {
        return isMajor(t) ? minor(root(t) + 9) : major(root(t) + 3);
    }

    public static int[] identity() {
        int[] p = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            p[i] = i;
        }
        return p;
    }

    /** p &#8728; q: q first. */
    public static int[] compose(int[] p, int[] q) {
        int[] r = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            r[i] = p[q[i]];
        }
        return r;
    }

    /** The permutation of a word over P, L, R, read as a composition (rightmost letter first). */
    public static int[] perm(String word) {
        int[] p = identity();
        for (int i = word.length() - 1; i >= 0; i--) {
            char c = word.charAt(i);
            int[] g = new int[COUNT];
            for (int x = 0; x < COUNT; x++) {
                switch (c) {
                case 'P': g[x] = P(x); break;
                case 'L': g[x] = L(x); break;
                case 'R': g[x] = R(x); break;
                default: throw new IllegalArgumentException("Letters must be P, L or R, got " + c);
                }
            }
            p = compose(g, p);
        }
        return p;
    }

    /** The permutation of an affine map x &#8614; &plusmn;x + t. */
    public static int[] ti(ZnAffine g) {
        int[] p = new int[COUNT];
        for (int x = 0; x < COUNT; x++) {
            p[x] = apply(g, x);
        }
        return p;
    }

    /** The 24 elements of TI as permutations: T0..T11, then I0..I11. */
    public static synchronized List<int[]> tiGroup() {
        if (tiGroup == null) {
            tiGroup = new ArrayList<int[]>();
            for (int u : new int[] { 1, -1 }) {
                for (int t = 0; t < 12; t++) {
                    tiGroup.add(ti(new ZnAffine(t, u, 12)));
                }
            }
        }
        return new ArrayList<int[]>(tiGroup);
    }

    /** The group generated by P, L and R, as permutations. */
    public static synchronized List<int[]> plrGroup() {
        if (plrGroup == null) {
            List<int[]> gens = Arrays.asList(perm("P"), perm("L"), perm("R"));
            Set<String> seen = new HashSet<String>();
            plrGroup = new ArrayList<int[]>();
            ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
            queue.add(identity());
            seen.add(Arrays.toString(identity()));
            while (!queue.isEmpty()) {
                int[] p = queue.poll();
                plrGroup.add(p);
                for (int[] g : gens) {
                    int[] q = compose(g, p);
                    if (seen.add(Arrays.toString(q))) {
                        queue.add(q);
                    }
                }
            }
        }
        return new ArrayList<int[]>(plrGroup);
    }

    /** A shortest word over P, L, R sending triad a to triad b ("" if a = b). */
    public static String plrWord(int a, int b) {
        Map<Integer, String> word = new HashMap<Integer, String>();
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        word.put(a, "");
        queue.add(a);
        while (!queue.isEmpty()) {
            int x = queue.poll();
            if (x == b) {
                return word.get(x);
            }
            int[] next = { P(x), L(x), R(x) };
            String[] letter = { "P", "L", "R" };
            for (int i = 0; i < 3; i++) {
                if (!word.containsKey(next[i])) {
                    word.put(next[i], letter[i] + word.get(x));
                    queue.add(next[i]);
                }
            }
        }
        throw new IllegalStateException("PLR does not reach " + name(b) + " from " + name(a));
    }

    /** The unique TI map sending triad a to triad b. */
    public static ZnAffine tiElement(int a, int b) {
        for (int u : new int[] { 1, -1 }) {
            for (int t = 0; t < 12; t++) {
                ZnAffine g = new ZnAffine(t, u, 12);
                if (apply(g, a) == b) {
                    return g;
                }
            }
        }
        throw new IllegalStateException("No TI map from " + name(a) + " to " + name(b));
    }

    /** "T7" for x &#8614; x + 7, "I4" for x &#8614; 4 &minus; x. */
    public static String tiName(ZnAffine g) {
        return (g.getFactor() == 1 ? "T" : "I") + g.getTranslation();
    }
}
