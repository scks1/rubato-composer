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

import java.util.Arrays;

/**
 * A finite directed multigraph Gamma : A -> V x V (Mazzola, The Topos of
 * Music, vol. III, section 61.5, p. 914; vol. IV, appendix J.1.1,
 * p. 1457). Vertices are 0..vertexCount-1 and arrows 0..arrowCount-1.
 * The tail of an arrow is the first projection and its head the second;
 * loops and parallel arrows are allowed. Instances are immutable.
 */
public final class Digraph {

    private final int vertexCount;
    private final int[] tails;
    private final int[] heads;

    private Digraph(int vertexCount, int[] tails, int[] heads) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("negative vertex count " + vertexCount);
        }
        for (int a = 0; a < tails.length; a++) {
            if (tails[a] < 0 || tails[a] >= vertexCount || heads[a] < 0 || heads[a] >= vertexCount) {
                throw new IllegalArgumentException("arrow " + a + " (" + tails[a] + " -> " + heads[a]
                                                   + ") leaves the vertices 0.." + (vertexCount - 1));
            }
        }
        this.vertexCount = vertexCount;
        this.tails = tails;
        this.heads = heads;
    }

    /** The digraph with the given vertex count and arrows {tail, head}. */
    public static Digraph of(int vertexCount, int[]... arrows) {
        int[] t = new int[arrows.length];
        int[] h = new int[arrows.length];
        for (int a = 0; a < arrows.length; a++) {
            if (arrows[a].length != 2) {
                throw new IllegalArgumentException("arrow " + a + " needs {tail, head}");
            }
            t[a] = arrows[a][0];
            h[a] = arrows[a][1];
        }
        return new Digraph(vertexCount, t, h);
    }

    /** The empty digraph. */
    public static Digraph empty() {
        return new Digraph(0, new int[0], new int[0]);
    }

    /** n vertices and no arrows. */
    public static Digraph discrete(int n) {
        return new Digraph(n, new int[0], new int[0]);
    }

    /** One vertex, no arrow: the up-arrow of length 0. */
    public static Digraph point() {
        return discrete(1);
    }

    /** One vertex with one loop: the final digraph 1. */
    public static Digraph loop() {
        return new Digraph(1, new int[] { 0 }, new int[] { 0 });
    }

    /** The up-arrow of length n: vertices 0..n and arrows i -> i+1 (vol. IV, Example 135). */
    public static Digraph arrowN(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("negative length " + n);
        }
        int[] t = new int[n];
        int[] h = new int[n];
        for (int i = 0; i < n; i++) {
            t[i] = i;
            h[i] = i + 1;
        }
        return new Digraph(n + 1, t, h);
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int arrowCount() {
        return tails.length;
    }

    public int tail(int arrow) {
        return tails[arrow];
    }

    public int head(int arrow) {
        return heads[arrow];
    }

    /** True if no arrow starts or ends at the vertex. */
    public boolean isIsolated(int vertex) {
        for (int a = 0; a < tails.length; a++) {
            if (tails[a] == vertex || heads[a] == vertex) {
                return false;
            }
        }
        return true;
    }

    /** The dual digraph, tail and head exchanged (vol. IV, Example 136). */
    public Digraph dual() {
        return new Digraph(vertexCount, heads.clone(), tails.clone());
    }

    /**
     * The digraph with the given vertices removed together with every
     * arrow that touches one of them. Remaining vertices and arrows keep
     * their order and are renumbered from 0.
     */
    public Digraph without(boolean[] removedVertices) {
        int[] index = new int[vertexCount];
        int n = 0;
        for (int v = 0; v < vertexCount; v++) {
            index[v] = removedVertices[v] ? -1 : n++;
        }
        int m = 0;
        for (int a = 0; a < tails.length; a++) {
            if (index[tails[a]] >= 0 && index[heads[a]] >= 0) {
                m++;
            }
        }
        int[] t = new int[m];
        int[] h = new int[m];
        m = 0;
        for (int a = 0; a < tails.length; a++) {
            if (index[tails[a]] >= 0 && index[heads[a]] >= 0) {
                t[m] = index[tails[a]];
                h[m] = index[heads[a]];
                m++;
            }
        }
        return new Digraph(n, t, h);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Digraph)) {
            return false;
        }
        Digraph d = (Digraph) o;
        return vertexCount == d.vertexCount && Arrays.equals(tails, d.tails) && Arrays.equals(heads, d.heads);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * vertexCount + Arrays.hashCode(tails)) + Arrays.hashCode(heads);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Digraph(" + vertexCount + ";");
        for (int a = 0; a < tails.length; a++) {
            b.append(a == 0 ? " " : ", ").append(tails[a]).append("->").append(heads[a]);
        }
        return b.append(")").toString();
    }
}
