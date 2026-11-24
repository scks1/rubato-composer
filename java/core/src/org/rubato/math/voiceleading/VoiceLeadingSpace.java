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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The voice-leading space of a finite group action (Flieder, arXiv 2512.04090,
 * Def. 4.1, pp. 26-27, with the rule of Example 4.1, p. 28): vertices are the
 * points, and the arrows from x to y are the transporters g with gx = y. An
 * arrow is stored as (x, g) with target gx (sources/VOICE-LEADING-SPEC.md, D4).
 * <p>
 * Subjective transformations are the arrows (Def. 4.2 (1)). Objective
 * transformations are taken as automorphisms of the Sigma_VLS structure
 * (section 4.4, Prop. 4.1, p. 31): pairs (h, k) of bijections of the points
 * and of the group with k[vlr(x, y)] = vlr(hx, hy) (D5). Their count is
 * |N(F)| (|K|!)^|F|, with F the permutations the group induces, N(F) its
 * normalizer and K the kernel of the action (our derivation, D6).
 */
public final class VoiceLeadingSpace {

    private final FiniteAction action;
    private final int points;
    private final int elements;
    private final List<int[]> image;      // distinct permutations, in order of first element
    private final int kernel;
    private List<int[]> normalizer;

    public VoiceLeadingSpace(FiniteAction action) {
        this.action = action;
        this.points = action.pointCount();
        this.elements = action.groupSize();
        Map<String, int[]> distinct = new LinkedHashMap<String, int[]>();
        int k = 0;
        for (int g = 0; g < elements; g++) {
            int[] p = action.permutation(g);
            distinct.put(Arrays.toString(p), p);
            if (isIdentity(p)) {
                k++;
            }
        }
        image = new ArrayList<int[]>(distinct.values());
        kernel = k;
    }

    public FiniteAction action() {
        return action;
    }

    /** The transporters from x to y, the subtype of Example 4.1, in element order. */
    public List<Integer> transporters(int x, int y) {
        List<Integer> r = new ArrayList<Integer>();
        for (int g = 0; g < elements; g++) {
            if (action.act(g, x) == y) {
                r.add(g);
            }
        }
        return r;
    }

    /** Every arrow as {source, element, target}. */
    public List<int[]> arrows() {
        List<int[]> r = new ArrayList<int[]>();
        for (int x = 0; x < points; x++) {
            for (int g = 0; g < elements; g++) {
                r.add(new int[] { x, g, action.act(g, x) });
            }
        }
        return r;
    }

    public int arrowCount() {
        return points * elements;
    }

    /** The fewest arrows between an ordered pair of vertices. */
    public int minMultiplicity() {
        int min = Integer.MAX_VALUE;
        for (int x = 0; x < points; x++) {
            for (int y = 0; y < points; y++) {
                min = Math.min(min, transporters(x, y).size());
            }
        }
        return min;
    }

    /** The most arrows between an ordered pair of vertices. */
    public int maxMultiplicity() {
        int max = 0;
        for (int x = 0; x < points; x++) {
            for (int y = 0; y < points; y++) {
                max = Math.max(max, transporters(x, y).size());
            }
        }
        return max;
    }

    /** The number of elements acting as the identity. */
    public int kernelSize() {
        return kernel;
    }

    /** The number of distinct permutations the group induces, |F|. */
    public int imageSize() {
        return image.size();
    }

    /**
     * The bijections h of the points with h F h<sup>-1</sup> = F, found by
     * backtracking over point images (D13). Each is the vertex map h_Pitch of
     * an objective transformation.
     */
    public List<int[]> normalizer() {
        if (normalizer == null) {
            List<int[]> gens = generators();
            List<int[]> found = new ArrayList<int[]>();
            int[] h = new int[points];
            Arrays.fill(h, -1);
            search(0, h, new boolean[points], gens, found);
            normalizer = found;
        }
        return normalizer;
    }

    /** A set of permutations generating F, chosen greedily. */
    private List<int[]> generators() {
        List<int[]> gens = new ArrayList<int[]>();
        Set<String> span = new HashSet<String>();
        span.add(Arrays.toString(identity(points)));
        for (int[] p : image) {
            if (!span.contains(Arrays.toString(p))) {
                gens.add(p);
                span = closure(gens);
            }
        }
        return gens;
    }

    private Set<String> closure(List<int[]> gens) {
        Set<String> seen = new HashSet<String>();
        List<int[]> todo = new ArrayList<int[]>();
        int[] id = identity(points);
        seen.add(Arrays.toString(id));
        todo.add(id);
        while (!todo.isEmpty()) {
            int[] p = todo.remove(todo.size() - 1);
            for (int[] s : gens) {
                int[] q = new int[points];
                for (int x = 0; x < points; x++) {
                    q[x] = s[p[x]];
                }
                if (seen.add(Arrays.toString(q))) {
                    todo.add(q);
                }
            }
        }
        return seen;
    }

    private void search(int i, int[] h, boolean[] used, List<int[]> gens, List<int[]> found) {
        if (i == points) {
            found.add(h.clone());
            return;
        }
        for (int v = 0; v < points; v++) {
            if (!used[v]) {
                h[i] = v;
                used[v] = true;
                if (consistent(h, gens)) {
                    search(i + 1, h, used, gens, found);
                }
                h[i] = -1;
                used[v] = false;
            }
        }
    }

    /**
     * Whether, for every generator s, some f in F agrees with the partial
     * conjugate h s h<sup>-1</sup>, which sends h(x) to h(s(x)) wherever both
     * are assigned.
     */
    private boolean consistent(int[] h, List<int[]> gens) {
        for (int[] s : gens) {
            boolean some = false;
            for (int[] f : image) {
                boolean agree = true;
                for (int x = 0; x < points && agree; x++) {
                    if (h[x] >= 0 && h[s[x]] >= 0 && f[h[x]] != h[s[x]]) {
                        agree = false;
                    }
                }
                if (agree) {
                    some = true;
                    break;
                }
            }
            if (!some) {
                return false;
            }
        }
        return true;
    }

    /** |N(F)| (|K|!)^|F|, the number of objective transformations (D6). */
    public BigInteger sigmaAutomorphismCount() {
        return BigInteger.valueOf(normalizer().size()).multiply(factorial(kernel).pow(image.size()));
    }

    /**
     * Whether (h, k) is a Sigma_VLS automorphism: both bijections and
     * k[vlr(x, y)] = vlr(hx, hy) for all x, y. Since each g lies in
     * vlr(x, gx), this holds exactly when k(g) h(x) = h(gx) for all g and x.
     */
    public boolean isSigmaAutomorphism(int[] h, int[] k) {
        if (!isBijection(h, points) || !isBijection(k, elements)) {
            return false;
        }
        for (int g = 0; g < elements; g++) {
            for (int x = 0; x < points; x++) {
                if (action.act(k[g], h[x]) != h[action.act(g, x)]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The number of distinct quiver maps that the elements phi induce by
     * x &rarr; phi x on vertices and g &rarr; phi g phi<sup>-1</sup> on arrows
     * (Example 4.3, p. 30).
     */
    public int conjugationInducedCount() {
        Set<String> maps = new HashSet<String>();
        for (int phi = 0; phi < elements; phi++) {
            maps.add(Arrays.toString(action.permutation(phi)) + Arrays.toString(conjugation(phi)));
        }
        return maps.size();
    }

    /** The arrow map g &rarr; phi g phi<sup>-1</sup>. */
    public int[] conjugation(int phi) {
        int[] k = new int[elements];
        for (int g = 0; g < elements; g++) {
            k[g] = action.multiply(action.multiply(phi, g), action.inverse(phi));
        }
        return k;
    }

    /** Whether the vertex map h is the vertex part of some conjugation (Example 4.3). */
    public boolean isConjugationVertexMap(int[] h) {
        for (int phi = 0; phi < elements; phi++) {
            if (Arrays.equals(h, action.permutation(phi))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The number of bare quiver automorphisms of Def. 4.2 when every ordered
     * vertex pair carries the same number m of arrows: n! (m!)^(n^2). Null
     * otherwise (D7).
     */
    public BigInteger bareQuiverAutomorphismCount() {
        int min = minMultiplicity(), max = maxMultiplicity();
        if (min != max) {
            return null;
        }
        return factorial(points).multiply(factorial(min).pow(points * points));
    }

    /** "5x+3" for an affine map of Z_n, otherwise the map in cycle notation with point names. */
    public String describeVertexMap(int[] h) {
        if (!action.isTriads()) {
            int n = action.modulus();
            int b = h[0], a = ((h[1 % n] - h[0]) % n + n) % n;
            boolean affine = true;
            for (int x = 0; x < n; x++) {
                if (h[x] != (a * x + b) % n) {
                    affine = false;
                }
            }
            if (affine) {
                return FiniteAction.affineName(a, b);
            }
        }
        StringBuilder sb = new StringBuilder();
        boolean[] done = new boolean[points];
        for (int x = 0; x < points; x++) {
            if (!done[x] && h[x] != x) {
                sb.append('(');
                int y = x;
                while (!done[y]) {
                    done[y] = true;
                    sb.append(action.pointName(y));
                    y = h[y];
                    if (!done[y]) {
                        sb.append(' ');
                    }
                }
                sb.append(')');
            }
        }
        return sb.length() == 0 ? "identity" : sb.toString();
    }

    static BigInteger factorial(int n) {
        BigInteger r = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            r = r.multiply(BigInteger.valueOf(i));
        }
        return r;
    }

    private static boolean isBijection(int[] p, int size) {
        if (p == null || p.length != size) {
            return false;
        }
        boolean[] seen = new boolean[size];
        for (int v : p) {
            if (v < 0 || v >= size || seen[v]) {
                return false;
            }
            seen[v] = true;
        }
        return true;
    }

    private static boolean isIdentity(int[] p) {
        for (int i = 0; i < p.length; i++) {
            if (p[i] != i) {
                return false;
            }
        }
        return true;
    }

    private static int[] identity(int n) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = i;
        }
        return r;
    }
}
