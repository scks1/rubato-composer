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

package org.rubato.math.voiceleading;

import java.util.Arrays;

/**
 * A finite group acting on a finite set of points, the data of a voice-leading
 * rule given by a group action (Flieder, arXiv 2512.04090, Example 4.1, p. 28).
 * <p>
 * Group elements are abstract affine symbols (a, b, tag) standing for the map
 * x &rarr; ax + b on Z<sub>n</sub>; the tag keeps T<sub>k</sub> and
 * I<sub>k</sub> apart when they act alike (n = 2), so T/I always has 2n
 * elements (sources/VOICE-LEADING-SPEC.md, D3). Points are the pitch classes
 * of Z<sub>n</sub> or the 24 consonant triads of Z12 (D2). Each element is
 * stored with its permutation of the points.
 */
public final class FiniteAction {

    /** Largest n for the pitch-class preset. */
    public static final int MAX_N = 24;

    private static final String[] NAMES = { "C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B" };

    private final int n;
    private final boolean triads;
    private final int[][] elements;   // (a, b, tag)
    private final String[] elementNames;
    private final int[][] perms;      // perms[g][x] = g x
    private final int[][] points;     // pitch classes of each point, sorted
    private final String[] pointNames;
    private final int[][] product;
    private final int[] inverse;
    private final int identity;

    private FiniteAction(int n, boolean triads, int[][] elements, String[] elementNames) {
        this.n = n;
        this.triads = triads;
        this.elements = elements;
        this.elementNames = elementNames;
        if (triads) {
            points = new int[24][];
            pointNames = new String[24];
            for (int r = 0; r < 12; r++) {
                points[r] = sorted(r, r + 4, r + 7);
                pointNames[r] = NAMES[r];
                points[12 + r] = sorted(r, r + 3, r + 7);
                pointNames[12 + r] = NAMES[r] + "m";
            }
        }
        else {
            points = new int[n][];
            pointNames = new String[n];
            for (int x = 0; x < n; x++) {
                points[x] = new int[] { x };
                pointNames[x] = Integer.toString(x);
            }
        }
        int m = elements.length;
        perms = new int[m][points.length];
        for (int g = 0; g < m; g++) {
            for (int x = 0; x < points.length; x++) {
                int[] img = new int[points[x].length];
                for (int i = 0; i < img.length; i++) {
                    img[i] = apply(elements[g], points[x][i]);
                }
                Arrays.sort(img);
                int y = find(img);
                if (y < 0) {
                    throw new IllegalArgumentException(elementNames[g] + " does not map the triads to triads"
                                                       + "; the group must act on the triads (T or TI).");
                }
                perms[g][x] = y;
            }
        }
        product = new int[m][m];
        inverse = new int[m];
        int id = -1;
        for (int g = 0; g < m; g++) {
            if (elements[g][0] == 1 % n && elements[g][1] == 0 && elements[g][2] == 0) {
                id = g;
            }
            for (int h = 0; h < m; h++) {
                product[g][h] = indexOfSymbol(compose(elements[g], elements[h]));
            }
        }
        identity = id;
        for (int g = 0; g < m; g++) {
            for (int h = 0; h < m; h++) {
                if (product[g][h] == identity) {
                    inverse[g] = h;
                }
            }
        }
    }

    /**
     * The pitch classes Z<sub>n</sub> under T (transpositions), TI
     * (transpositions and inversions) or Aff (x &rarr; ax + b, a a unit).
     */
    public static FiniteAction pitchClasses(String group, int n) {
        if (n < 2 || n > MAX_N) {
            throw new IllegalArgumentException("n = " + n + "; the pitch-class space needs n from 2 to 24.");
        }
        int[][] s = symbols(group, n);
        return new FiniteAction(n, false, s, names(group, s));
    }

    /** The 24 consonant triads of Z12 (C = {0, 4, 7}, Cm = {0, 3, 7}, ...) under T or TI. */
    public static FiniteAction triads(String group) {
        if ("Aff".equals(group)) {
            throw new IllegalArgumentException("Aff does not act on the triads (5x sends C = {0, 4, 7} to {0, 8, 11})"
                                               + "; use T or TI.");
        }
        int[][] s = symbols(group, 12);
        return new FiniteAction(12, true, s, names(group, s));
    }

    private static int[][] symbols(String group, int n) {
        int[][] s;
        if ("T".equals(group)) {
            s = new int[n][];
            for (int b = 0; b < n; b++) {
                s[b] = new int[] { 1 % n, b, 0 };
            }
        }
        else if ("TI".equals(group)) {
            s = new int[2 * n][];
            for (int b = 0; b < n; b++) {
                s[b] = new int[] { 1 % n, b, 0 };
                s[n + b] = new int[] { n - 1, b, 1 };
            }
        }
        else if ("Aff".equals(group)) {
            int units = 0;
            for (int a = 1; a < n; a++) {
                if (gcd(a, n) == 1) {
                    units++;
                }
            }
            s = new int[units * n][];
            int i = 0;
            for (int a = 1; a < n; a++) {
                if (gcd(a, n) == 1) {
                    for (int b = 0; b < n; b++) {
                        s[i++] = new int[] { a, b, 0 };
                    }
                }
            }
        }
        else {
            throw new IllegalArgumentException("Unknown group \"" + group + "\"; use T, TI or Aff.");
        }
        return s;
    }

    private static String[] names(String group, int[][] s) {
        String[] r = new String[s.length];
        for (int g = 0; g < s.length; g++) {
            if ("Aff".equals(group)) {
                r[g] = affineName(s[g][0], s[g][1]);
            }
            else {
                r[g] = (s[g][2] == 1 ? "I" : "T") + s[g][1];
            }
        }
        return r;
    }

    /** The name of x &rarr; ax + b: "x", "5x", "x+3", "5x+3". */
    public static String affineName(int a, int b) {
        String head = a == 1 ? "x" : a + "x";
        return b == 0 ? head : head + "+" + b;
    }

    private int apply(int[] g, int x) {
        return ((g[0] * x + g[1]) % n + n) % n;
    }

    /** g after h, as symbols: x &rarr; a(cx + d) + b, tags added mod 2. */
    private int[] compose(int[] g, int[] h) {
        return new int[] { (g[0] * h[0]) % n, ((g[0] * h[1] + g[1]) % n + n) % n, g[2] ^ h[2] };
    }

    private int indexOfSymbol(int[] s) {
        for (int g = 0; g < elements.length; g++) {
            if (elements[g][0] == s[0] && elements[g][1] == s[1] && elements[g][2] == s[2]) {
                return g;
            }
        }
        throw new IllegalStateException("group not closed at " + Arrays.toString(s));
    }

    private int find(int[] sortedPcs) {
        for (int y = 0; y < points.length; y++) {
            if (Arrays.equals(points[y], sortedPcs)) {
                return y;
            }
        }
        return -1;
    }

    private static int[] sorted(int... pcs) {
        int[] r = new int[pcs.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = pcs[i] % 12;
        }
        Arrays.sort(r);
        return r;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /** The modulus n of Z<sub>n</sub>. */
    public int modulus() {
        return n;
    }

    /** Whether the points are the consonant triads. */
    public boolean isTriads() {
        return triads;
    }

    public int pointCount() {
        return points.length;
    }

    public int groupSize() {
        return elements.length;
    }

    public String pointName(int x) {
        return pointNames[x];
    }

    public String elementName(int g) {
        return elementNames[g];
    }

    /** The element with this name, or -1. */
    public int indexOf(String name) {
        for (int g = 0; g < elementNames.length; g++) {
            if (elementNames[g].equals(name)) {
                return g;
            }
        }
        return -1;
    }

    /** The point g x. */
    public int act(int g, int x) {
        return perms[g][x];
    }

    /** The permutation of the points by g (a copy). */
    public int[] permutation(int g) {
        return perms[g].clone();
    }

    /** g after h. */
    public int multiply(int g, int h) {
        return product[g][h];
    }

    public int inverse(int g) {
        return inverse[g];
    }

    public int identity() {
        return identity;
    }

    /** The sorted pitch classes of point x. */
    public int[] pitchClassesOf(int x) {
        return points[x].clone();
    }

    /** The triad with these pitch classes in any order, or -1 (always -1 for the pitch-class preset). */
    public int triadIndex(int[] pcs) {
        if (!triads) {
            return -1;
        }
        int[] s = pcs.clone();
        for (int i = 0; i < s.length; i++) {
            s[i] = ((s[i] % 12) + 12) % 12;
        }
        Arrays.sort(s);
        return find(s);
    }
}
