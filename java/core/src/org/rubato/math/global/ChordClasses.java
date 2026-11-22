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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Isomorphism classes of zero-addressed chords in PiMod12 and the class
 * weights of a nerve (Mazzola, <i>The Topos of Music</i>, &sect;14.5,
 * Lemma 25, p. 283; Ex. 24 and Fig. 14.7, p. 284).
 * <p>
 * Two chords are isomorphic if some bijection between them is an
 * isomorphism of local compositions ({@link LocalComposition#isIsomorphicTo}).
 * Class numbers follow the list in App. N.1 (Topos IV, p. 1502): the
 * representatives of classes 1 to 16, the full chromatic set and every
 * chord of one to three tones, are copied from that page as cited
 * numeric data. Larger chords get no number from this class.
 */
public final class ChordClasses {

    private ChordClasses() { /* static utility */ }

    /** Representatives of App. N.1 classes 1 to 16 (Topos IV p. 1502), keyed by class number. */
    public static final Map<Integer, int[]> REPRESENTATIVES;

    static {
        Map<Integer, int[]> m = new LinkedHashMap<Integer, int[]>();
        m.put(1, new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 });
        m.put(2, new int[] { 0 });
        m.put(3, new int[] { 0, 1 });
        m.put(4, new int[] { 0, 2 });
        m.put(5, new int[] { 0, 3 });
        m.put(6, new int[] { 0, 4 });
        m.put(7, new int[] { 0, 6 });
        m.put(8, new int[] { 0, 1, 2 });
        m.put(9, new int[] { 0, 1, 3 });
        m.put(10, new int[] { 0, 1, 4 });
        m.put(11, new int[] { 0, 1, 5 });
        m.put(12, new int[] { 0, 1, 6 });
        m.put(13, new int[] { 0, 2, 4 });
        m.put(14, new int[] { 0, 2, 6 });
        m.put(15, new int[] { 0, 3, 6 });
        m.put(16, new int[] { 0, 4, 8 });
        REPRESENTATIVES = Collections.unmodifiableMap(m);
    }

    /**
     * The App. N.1 class number of a chord in Z_12, or null if the chord has
     * more than three and fewer than twelve tones (no representative here).
     */
    public static Integer classNumber(LocalComposition chord) {
        if (chord.getModulus() != 12 || chord.getDimension() != 1) {
            throw new IllegalArgumentException("Class numbers are defined for chords in Z_12, got " + chord);
        }
        if (chord.size() == 12) {
            return 1;
        }
        if (chord.size() > 3) {
            return null;
        }
        for (Map.Entry<Integer, int[]> e : REPRESENTATIVES.entrySet()) {
            LocalComposition r = LocalComposition.ofClasses(12, e.getValue());
            if (r.size() == chord.size() && chord.isIsomorphicTo(r)) {
                return e.getKey();
            }
        }
        throw new IllegalStateException("No App. N.1 class found for " + chord);
    }

    /**
     * The isomorphism classes of the k-element subsets of Z_n, as one
     * representative each (the lexicographically first subset of the class).
     * Brute force; k at most 4.
     */
    public static List<LocalComposition> classes(int n, int k) {
        if (k < 1 || k > 4) {
            throw new IllegalArgumentException("k must be 1 to 4 for the brute-force search, got " + k);
        }
        List<LocalComposition> reps = new ArrayList<LocalComposition>();
        int[] c = new int[k];
        for (int i = 0; i < k; i++) {
            c[i] = i;
        }
        while (true) {
            LocalComposition s = LocalComposition.ofClasses(n, c);
            boolean known = false;
            for (LocalComposition r : reps) {
                if (s.isIsomorphicTo(r)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                reps.add(s);
            }
            int i = k - 1;
            while (i >= 0 && c[i] == n - k + i) {
                i--;
            }
            if (i < 0) {
                break;
            }
            c[i]++;
            for (int j = i + 1; j < k; j++) {
                c[j] = c[j - 1] + 1;
            }
        }
        return reps;
    }

    /**
     * The orbits of the k-element subsets of Z_n under the affine bijections
     * x &#8614; u&middot;x + t (u a unit), one representative each. Used to compare
     * Def. 26 isomorphism with the symmetry-group reading (spec A2).
     */
    public static List<LocalComposition> affineOrbits(int n, int k) {
        List<LocalComposition> reps = new ArrayList<LocalComposition>();
        Map<LocalComposition, Boolean> seen = new HashMap<LocalComposition, Boolean>();
        int[] c = new int[k];
        for (int i = 0; i < k; i++) {
            c[i] = i;
        }
        while (true) {
            LocalComposition s = LocalComposition.ofClasses(n, c);
            if (!seen.containsKey(s)) {
                reps.add(s);
                for (int u = 1; u < n; u++) {
                    if (gcd(u, n) != 1) {
                        continue;
                    }
                    for (int t = 0; t < n; t++) {
                        int[] img = new int[k];
                        for (int i = 0; i < k; i++) {
                            img[i] = u * c[i] + t;
                        }
                        seen.put(LocalComposition.ofClasses(n, img), Boolean.TRUE);
                    }
                }
            }
            int i = k - 1;
            while (i >= 0 && c[i] == n - k + i) {
                i--;
            }
            if (i < 0) {
                break;
            }
            c[i]++;
            for (int j = i + 1; j < k; j++) {
                c[j] = c[j - 1] + 1;
            }
        }
        return reps;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /**
     * The class weight of a global composition in Z_12: for each simplex of
     * its nerve (a sorted vertex list), the App. N.1 class number of the
     * intersection X_&sigma;, or null where this class has no number.
     */
    public static Map<List<Integer>, Integer> classWeights(GlobalComposition g) {
        Map<List<Integer>, Integer> w = new LinkedHashMap<List<Integer>, Integer>();
        SimplicialComplex n = g.nerve();
        for (int[] s : n.allSimplexes()) {
            w.put(SimplicialComplex.asList(s), classNumber(g.intersectionComposition(s)));
        }
        return w;
    }
}
