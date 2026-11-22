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
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The PLRQ group of Cannas, Antonini and Pernazza (MCM 2017, LNCS 10527):
 * its 17 generators as (&sigma;, v) pairs, the group they generate (Thm. 1:
 * S5 &#8905; Z12^4, realised as the pairs with v<sub>1</sub> + &hellip; +
 * v<sub>5</sub> = 0), and the morphisms R42, L13 and P42 of
 * Agust&iacute;n-Aquino, Blues for Alice (arXiv 2601.07161, Defs. 3.1, 3.3,
 * 3.5) with the coslice and prism facts of its &sect;&sect;4&ndash;5.
 * <p>
 * P42, "M &#8596; m on the same root", is the element ((24), 0). It agrees with
 * T8&#8728;L42, which the paper gives as its expression, on major sevenths only.
 */
public final class Plrq {

    private static final Map<String, PlrqElement> GENERATORS = new LinkedHashMap<String, PlrqElement>();

    private static void put(String name, int a, int b, int... v) {
        GENERATORS.put(name, PlrqElement.of(new int[][] { { a, b } }, v));
    }

    static {
        put("P12", 1, 2, 0, 0, 0, 0, 0);
        put("P14", 1, 4, 0, 0, 0, 0, 0);
        put("P23", 2, 3, 0, 0, 0, 0, 0);
        put("P35", 3, 5, 0, 0, 0, 0, 0);
        put("R12", 1, 2, -3, 3, 0, 0, 0);
        put("R23", 2, 3, 0, -3, 3, 0, 0);
        put("R42", 4, 2, 0, 3, 0, -3, 0);
        put("R35", 3, 5, 0, 0, -3, 0, 3);
        put("R53", 5, 3, 0, 0, 3, 0, -3);
        put("L13", 1, 3, 4, 0, -4, 0, 0);
        put("L15", 1, 5, 4, 0, 0, 0, -4);
        put("L42", 4, 2, 0, -4, 0, 4, 0);
        put("Q43", 4, 3, 0, 0, -1, 1, 0);
        put("Q15", 1, 5, 1, 0, 0, 0, -1);
        put("RR35", 3, 5, 0, 0, -6, 0, 6);
        put("QQ51", 5, 1, -2, 0, 0, 0, 2);
        put("N51", 5, 1, -5, 0, 0, 0, 5);
    }

    /** BfA Def. 3.1: M(x) &#8596; m(x &minus; 3). */
    public static final PlrqElement R42 = GENERATORS.get("R42");

    /** BfA Def. 3.3: D(x) &#8596; hd(x + 4). */
    public static final PlrqElement L13 = GENERATORS.get("L13");

    /** BfA Def. 3.5: M(x) &#8596; m(x). */
    public static final PlrqElement P42 = PlrqElement.of(new int[][] { { 2, 4 } }, 0, 0, 0, 0, 0);

    private static BitSet group;

    private Plrq() { /* static utility */ }

    /** The 17 generators by name, in the paper's order. */
    public static Map<String, PlrqElement> generators() {
        return Collections.unmodifiableMap(GENERATORS);
    }

    public static PlrqElement generator(String name) {
        PlrqElement e = GENERATORS.get(name);
        if (e == null) {
            throw new IllegalArgumentException("No generator " + name);
        }
        return e;
    }

    /** The transposition T<sup>s</sup> = (Id, (s, s, s, s, s)). */
    public static PlrqElement t(int s) {
        return PlrqElement.translationVector(s, s, s, s, s);
    }

    /** A product of generator names and Ts such as "P14 L42 P14 R12"; the rightmost acts first. */
    public static PlrqElement word(String w) {
        PlrqElement e = t(0);
        for (String part : w.trim().split("\\s+")) {
            PlrqElement f = part.matches("T\\d+") ? t(Integer.parseInt(part.substring(1))) : generator(part);
            e = e.compose(f);
        }
        return e;
    }

    // ---- enumeration on codes 0 .. 120 * 12^5 - 1

    private static final int[][] PERMS = new int[120][];
    private static final Map<String, Integer> PERM_INDEX = new LinkedHashMap<String, Integer>();
    private static final int V_CODES = 248832; // 12^5

    static {
        List<int[]> ps = new ArrayList<int[]>();
        permute(new int[] { 0, 1, 2, 3, 4 }, 0, ps);
        for (int i = 0; i < ps.size(); i++) {
            PERMS[i] = ps.get(i);
            PERM_INDEX.put(java.util.Arrays.toString(ps.get(i)), i);
        }
    }

    private static void permute(int[] a, int k, List<int[]> out) {
        if (k == a.length) {
            out.add(a.clone());
            return;
        }
        for (int i = k; i < a.length; i++) {
            int x = a[k]; a[k] = a[i]; a[i] = x;
            permute(a, k + 1, out);
            x = a[k]; a[k] = a[i]; a[i] = x;
        }
    }

    static int code(PlrqElement e) {
        int c = 0;
        for (int t = 4; t >= 0; t--) {
            c = c * 12 + e.v[t];
        }
        return PERM_INDEX.get(java.util.Arrays.toString(e.sigma)) * V_CODES + c;
    }

    static PlrqElement decode(int code) {
        int[] s = PERMS[code / V_CODES];
        int c = code % V_CODES;
        int[] v = new int[5];
        for (int t = 0; t < 5; t++) {
            v[t] = c % 12;
            c /= 12;
        }
        return new PlrqElement(s, v);
    }

    private static BitSet closure(List<PlrqElement> gens) {
        int n = gens.size();
        int[][] permMul = new int[n][120];   // index of g.sigma o perm
        int[][][] add = new int[n][120][5];  // g.v o perm
        for (int i = 0; i < n; i++) {
            PlrqElement g = gens.get(i);
            for (int pi = 0; pi < 120; pi++) {
                int[] q = new int[5];
                for (int t = 0; t < 5; t++) {
                    q[t] = g.sigma[PERMS[pi][t]];
                    add[i][pi][t] = g.v[PERMS[pi][t]];
                }
                permMul[i][pi] = PERM_INDEX.get(java.util.Arrays.toString(q));
            }
        }
        BitSet seen = new BitSet(120 * V_CODES);
        BitSet frontier = new BitSet(120 * V_CODES);
        int id = code(t(0));
        seen.set(id);
        frontier.set(id);
        int[] digits = new int[5];
        while (!frontier.isEmpty()) {
            BitSet next = new BitSet(120 * V_CODES);
            for (int c = frontier.nextSetBit(0); c >= 0; c = frontier.nextSetBit(c + 1)) {
                int pi = c / V_CODES;
                int rest = c % V_CODES;
                for (int t = 0; t < 5; t++) {
                    digits[t] = rest % 12;
                    rest /= 12;
                }
                for (int i = 0; i < n; i++) {
                    int[] a = add[i][pi];
                    int vc = 0;
                    for (int t = 4; t >= 0; t--) {
                        vc = vc * 12 + (digits[t] + a[t]) % 12;
                    }
                    int d = permMul[i][pi] * V_CODES + vc;
                    if (!seen.get(d)) {
                        seen.set(d);
                        next.set(d);
                    }
                }
            }
            frontier = next;
        }
        return seen;
    }

    /** The order of the subgroup of S5 &times; Z12^5 generated by the elements. */
    public static long order(List<PlrqElement> gens) {
        return closure(gens).cardinality();
    }

    /** The facts checked for Thm. 1. */
    public static final class Structure {
        /** The order of the group. */
        public long order;
        /** The number of distinct permutation parts. */
        public int permutations;
        /** The number of elements (Id, v). */
        public long kernel;
        /** True if every element has &Sigma;v = 0. */
        public boolean allSumsZero;
        /** True if the elements (Id, v) are exactly the 12^4 vectors with &Sigma;v = 0. */
        public boolean kernelIsSumZero;
    }

    /** Enumerates the generated group and records its structure. */
    public static Structure structure(List<PlrqElement> gens) {
        BitSet g = closure(gens);
        Structure s = new Structure();
        s.order = g.cardinality();
        Set<Integer> perms = new HashSet<Integer>();
        s.allSumsZero = true;
        long kernelSumZero = 0;
        int idPerm = PERM_INDEX.get("[0, 1, 2, 3, 4]");
        for (int c = g.nextSetBit(0); c >= 0; c = g.nextSetBit(c + 1)) {
            int rest = c % V_CODES;
            int sum = 0;
            for (int t = 0; t < 5; t++) {
                sum += rest % 12;
                rest /= 12;
            }
            sum %= 12;
            perms.add(c / V_CODES);
            if (sum != 0) {
                s.allSumsZero = false;
            }
            if (c / V_CODES == idPerm) {
                s.kernel++;
                if (sum == 0) {
                    kernelSumZero++;
                }
            }
        }
        s.permutations = perms.size();
        s.kernelIsSumZero = kernelSumZero == 20736 && s.kernel == 20736;
        return s;
    }

    /** True if the element lies in the group generated by the 17 generators. */
    public static synchronized boolean contains(PlrqElement e) {
        if (group == null) {
            group = closure(new ArrayList<PlrqElement>(GENERATORS.values()));
        }
        return group.get(code(e));
    }

    /**
     * BfA Def. 4.1 in the action groupoid: with f : x &#8594; f(x) and
     * g : x &#8594; g(x), h is a morphism from f to g in x &darr; C when h &#8728; f = g.
     */
    public static boolean isCosliceMorphism(SeventhChord x, PlrqElement f, PlrqElement g, PlrqElement h) {
        return h.compose(f).equals(g) && h.apply(f.apply(x)).equals(g.apply(x));
    }

    /** BfA Thm. 5.1: the faces of the prism (2) commute, checked as group elements. */
    public static boolean prismCommutes() {
        PlrqElement t7r = t(7).compose(R42);
        boolean square1 = R42.compose(t(10)).compose(t7r).equals(t(5));
        boolean square2 = t(10).compose(t7r).equals(t(5).compose(R42));
        boolean involution = R42.compose(R42).isIdentity();
        boolean commute = R42.compose(t(5)).equals(t(5).compose(R42));
        return square1 && square2 && involution && commute;
    }

    /** BfA Def. 5.2: region name to {cadence, cadence, morphism}. */
    public static Map<String, String[]> conglomerate() {
        Map<String, String[]> m = new LinkedHashMap<String, String[]>();
        m.put("A", new String[] { "J3 {II,III}", "J4 {III,IV}", "R42" });
        m.put("B", new String[] { "J1 {I,II}", "J2 {I,IV}", "R42" });
        m.put("C", new String[] { "J5 {V}", "J6 {VII}", "L13" });
        return m;
    }
}
