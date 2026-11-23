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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Faces and boundaries of hypergestures (Mazzola, The Topos of Music,
 * vol. III, section 63.3, pp. 968-970).
 *
 * Def. 112 defines the face of a gesture g on Gamma recursively: for a
 * discrete Gamma it is the sum of g's vertex values; otherwise it is the
 * sum over the arrows a of the face of g restricted to Gamma with the tail
 * of a (and the arrows at it) removed, minus the face of g restricted to
 * Gamma with the head of a removed. The face is therefore a sum
 * phi(v) g(v) over the vertices, with integer coefficients phi that
 * depend only on Gamma; {@link #coefficients} computes them.
 *
 * A cube over a sequence of digraphs Gamma_1 ... Gamma_n is written as a
 * list with -1 at each free position and a vertex of Gamma_i at each
 * fixed one; it stands for the restriction of a hypergesture to those
 * vertices. The boundary of a cube is the sum over its free positions,
 * k-th free position with sign (-1)^k, of the face over that digraph
 * (departure D5: chains of formal cubes, not of hypergestures in a space).
 */
public final class Faces {

    private Faces() { /* static utility */ }

    /** The coefficients phi(v) of the face over d, indexed by vertex. */
    public static long[] coefficients(Digraph d) {
        if (d.vertexCount() > 62) {
            throw new IllegalArgumentException("too many vertices");
        }
        Map<Long, long[]> memo = new HashMap<Long, long[]>();
        return face(d, 0L, memo).clone();
    }

    /** Coefficients over d with the vertices in removed (bit mask) and their arrows deleted. */
    private static long[] face(Digraph d, long removed, Map<Long, long[]> memo) {
        long[] cached = memo.get(removed);
        if (cached != null) {
            return cached;
        }
        long[] out = new long[d.vertexCount()];
        boolean anyArrow = false;
        for (int a = 0; a < d.arrowCount(); a++) {
            int t = d.tail(a), h = d.head(a);
            if ((removed >> t & 1) == 1 || (removed >> h & 1) == 1) {
                continue;
            }
            anyArrow = true;
            long[] minus = face(d, removed | 1L << t, memo);
            long[] plus = face(d, removed | 1L << h, memo);
            for (int v = 0; v < out.length; v++) {
                out[v] += minus[v] - plus[v];
            }
        }
        if (!anyArrow) {
            for (int v = 0; v < out.length; v++) {
                out[v] = (removed >> v & 1) == 1 ? 0 : 1;
            }
        }
        memo.put(removed, out);
        return out;
    }

    /** The chain made of the single cube with every position free. */
    public static Map<List<Integer>, Long> topCube(Digraph[] seq) {
        Map<List<Integer>, Long> c = new LinkedHashMap<List<Integer>, Long>();
        c.put(Collections.nCopies(seq.length, -1), 1L);
        return c;
    }

    /** The boundary of a chain of cubes over seq; zero coefficients are dropped. */
    public static Map<List<Integer>, Long> boundary(Digraph[] seq, Map<List<Integer>, Long> chain) {
        long[][] phi = new long[seq.length][];
        for (int i = 0; i < seq.length; i++) {
            phi[i] = coefficients(seq[i]);
        }
        Map<List<Integer>, Long> out = new LinkedHashMap<List<Integer>, Long>();
        for (Map.Entry<List<Integer>, Long> e : chain.entrySet()) {
            List<Integer> cube = e.getKey();
            if (cube.size() != seq.length) {
                throw new IllegalArgumentException("cube " + cube + " does not fit " + seq.length + " digraphs");
            }
            int k = 0;
            for (int i = 0; i < cube.size(); i++) {
                if (cube.get(i) != -1) {
                    continue;
                }
                long sign = k % 2 == 0 ? 1 : -1;
                for (int v = 0; v < phi[i].length; v++) {
                    if (phi[i][v] == 0) {
                        continue;
                    }
                    List<Integer> face = new ArrayList<Integer>(cube);
                    face.set(i, v);
                    long add = sign * phi[i][v] * e.getValue();
                    Long old = out.get(face);
                    long sum = (old == null ? 0 : old) + add;
                    if (sum == 0) {
                        out.remove(face);
                    }
                    else {
                        out.put(face, sum);
                    }
                }
                k++;
            }
        }
        return out;
    }
}
