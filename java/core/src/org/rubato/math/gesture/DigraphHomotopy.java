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
package org.rubato.math.gesture;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Homotopy facts about the geometric realization |Gamma| of a finite
 * digraph, a graph in the topological sense. Its fundamental group at
 * any vertex is free, of rank the number of arrows outside a spanning
 * tree of the component, |A| - |V| + (number of components) in all
 * (Mazzola, The Topos of Music, vol. III, p. 928, citing [590, Thm. 10.7];
 * vol. IV, Lemma 113, p. 1459). Cycles are handled as integer vectors
 * over the arrows; a basis of the cycle space is a basis of H_1(|Gamma|; Z),
 * the abelianization of the fundamental group.
 */
public final class DigraphHomotopy {

    private DigraphHomotopy() { /* static utility */ }

    /** The number of connected components of |Gamma|. */
    public static int components(Digraph d) {
        int[] parent = new int[d.vertexCount()];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
        int c = d.vertexCount();
        for (int a = 0; a < d.arrowCount(); a++) {
            int x = find(parent, d.tail(a)), y = find(parent, d.head(a));
            if (x != y) {
                parent[x] = y;
                c--;
            }
        }
        return c;
    }

    private static int find(int[] p, int x) {
        while (p[x] != x) {
            p[x] = p[p[x]];
            x = p[x];
        }
        return x;
    }

    /** The rank of the free fundamental group of each component, summed. */
    public static int fundamentalGroupRank(Digraph d) {
        return d.arrowCount() - d.vertexCount() + components(d);
    }

    /**
     * The arrows of a spanning forest found by breadth-first search from
     * vertex 0, 1, ..., ignoring directions; loops are never tree arrows.
     */
    public static boolean[] spanningForest(Digraph d) {
        boolean[] tree = new boolean[d.arrowCount()];
        boolean[] seen = new boolean[d.vertexCount()];
        for (int r = 0; r < d.vertexCount(); r++) {
            if (seen[r]) {
                continue;
            }
            seen[r] = true;
            Deque<Integer> queue = new ArrayDeque<Integer>();
            queue.add(r);
            while (!queue.isEmpty()) {
                int v = queue.poll();
                for (int a = 0; a < d.arrowCount(); a++) {
                    int w = d.tail(a) == v ? d.head(a) : d.head(a) == v ? d.tail(a) : -1;
                    if (w >= 0 && !seen[w]) {
                        seen[w] = true;
                        tree[a] = true;
                        queue.add(w);
                    }
                }
            }
        }
        return tree;
    }

    /**
     * The arrow-count vector of a closed path that runs through the given
     * arrows in order, each from its tail to its head.
     *
     * @throws IllegalArgumentException if consecutive arrows do not meet or
     *         the path does not return to its start
     */
    public static int[] closedPath(Digraph d, int... arrows) {
        if (arrows.length == 0) {
            throw new IllegalArgumentException("empty path");
        }
        int[] v = new int[d.arrowCount()];
        for (int i = 0; i < arrows.length; i++) {
            int next = arrows[(i + 1) % arrows.length];
            if (d.head(arrows[i]) != d.tail(next)) {
                throw new IllegalArgumentException("arrow " + arrows[i] + " ends at " + d.head(arrows[i])
                                                   + " but arrow " + next + " starts at " + d.tail(next));
            }
            v[arrows[i]]++;
        }
        return v;
    }

    /** True if the vector is a cycle: every vertex is entered as often as it is left. */
    public static boolean isCycle(Digraph d, int[] v) {
        long[] balance = new long[d.vertexCount()];
        for (int a = 0; a < d.arrowCount(); a++) {
            balance[d.head(a)] += v[a];
            balance[d.tail(a)] -= v[a];
        }
        for (long b : balance) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * True if the vectors form a basis of the integer cycle space. A cycle
     * is determined by its entries on the arrows outside a spanning
     * forest, and every choice of those entries gives a cycle, so the
     * vectors form a basis if and only if they are cycles, as many as the
     * rank, and their matrix on the non-tree arrows has determinant +1 or -1.
     */
    public static boolean isCycleBasis(Digraph d, int[][] cycles) {
        int rank = fundamentalGroupRank(d);
        if (cycles.length != rank) {
            return false;
        }
        for (int[] c : cycles) {
            if (c.length != d.arrowCount() || !isCycle(d, c)) {
                return false;
            }
        }
        boolean[] tree = spanningForest(d);
        int[] outside = new int[rank];
        int k = 0;
        for (int a = 0; a < tree.length; a++) {
            if (!tree[a]) {
                outside[k++] = a;
            }
        }
        long[][] m = new long[rank][rank];
        for (int i = 0; i < rank; i++) {
            for (int j = 0; j < rank; j++) {
                m[i][j] = cycles[i][outside[j]];
            }
        }
        long det = determinant(m);
        return det == 1 || det == -1;
    }

    /** Integer determinant by fraction-free (Bareiss) elimination. */
    static long determinant(long[][] matrix) {
        int n = matrix.length;
        if (n == 0) {
            return 1;
        }
        long[][] m = new long[n][];
        for (int i = 0; i < n; i++) {
            m[i] = Arrays.copyOf(matrix[i], n);
        }
        long sign = 1, prev = 1;
        for (int k = 0; k < n - 1; k++) {
            if (m[k][k] == 0) {
                int swap = -1;
                for (int i = k + 1; i < n; i++) {
                    if (m[i][k] != 0) {
                        swap = i;
                        break;
                    }
                }
                if (swap < 0) {
                    return 0;
                }
                long[] t = m[k];
                m[k] = m[swap];
                m[swap] = t;
                sign = -sign;
            }
            for (int i = k + 1; i < n; i++) {
                for (int j = k + 1; j < n; j++) {
                    m[i][j] = (m[i][j] * m[k][k] - m[i][k] * m[k][j]) / prev;
                }
            }
            prev = m[k][k];
        }
        return sign * m[n - 1][n - 1];
    }
}
