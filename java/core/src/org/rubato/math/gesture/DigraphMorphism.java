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
import java.util.Arrays;
import java.util.List;

/**
 * A digraph morphism (u, v) : Gamma -> Delta, with v on vertices and u on
 * arrows, such that the tail and head of u(a) are the images of the tail
 * and head of a (Mazzola, The Topos of Music, vol. III, p. 914).
 */
public final class DigraphMorphism {

    private final Digraph source;
    private final Digraph target;
    private final int[] vertexMap;
    private final int[] arrowMap;

    /**
     * Creates the morphism.
     *
     * @throws IllegalArgumentException if the maps do not commute with
     *         tail and head
     */
    public DigraphMorphism(Digraph source, Digraph target, int[] vertexMap, int[] arrowMap) {
        if (!isMorphism(source, target, vertexMap, arrowMap)) {
            throw new IllegalArgumentException("not a digraph morphism " + source + " -> " + target + ": vertices "
                                               + Arrays.toString(vertexMap) + ", arrows " + Arrays.toString(arrowMap));
        }
        this.source = source;
        this.target = target;
        this.vertexMap = vertexMap.clone();
        this.arrowMap = arrowMap.clone();
    }

    /** True if the maps form a digraph morphism. */
    public static boolean isMorphism(Digraph source, Digraph target, int[] vertexMap, int[] arrowMap) {
        if (vertexMap.length != source.vertexCount() || arrowMap.length != source.arrowCount()) {
            return false;
        }
        for (int v : vertexMap) {
            if (v < 0 || v >= target.vertexCount()) {
                return false;
            }
        }
        for (int a = 0; a < arrowMap.length; a++) {
            int b = arrowMap[a];
            if (b < 0 || b >= target.arrowCount()) {
                return false;
            }
            if (target.tail(b) != vertexMap[source.tail(a)] || target.head(b) != vertexMap[source.head(a)]) {
                return false;
            }
        }
        return true;
    }

    public static DigraphMorphism identity(Digraph d) {
        int[] v = new int[d.vertexCount()];
        for (int i = 0; i < v.length; i++) {
            v[i] = i;
        }
        int[] a = new int[d.arrowCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        return new DigraphMorphism(d, d, v, a);
    }

    /** Every morphism source -> target, vertex maps in lexicographic order. */
    public static List<DigraphMorphism> all(Digraph source, Digraph target) {
        List<DigraphMorphism> out = new ArrayList<DigraphMorphism>();
        int nv = source.vertexCount();
        if (nv > 0 && target.vertexCount() == 0) {
            return out;
        }
        int[] v = new int[nv];
        while (true) {
            extendArrows(source, target, v, new int[source.arrowCount()], 0, out);
            int i = nv - 1;
            while (i >= 0 && v[i] == target.vertexCount() - 1) {
                v[i] = 0;
                i--;
            }
            if (i < 0) {
                return out;
            }
            v[i]++;
        }
    }

    private static void extendArrows(Digraph s, Digraph t, int[] v, int[] a, int k, List<DigraphMorphism> out) {
        if (k == a.length) {
            out.add(new DigraphMorphism(s, t, v, a));
            return;
        }
        for (int b = 0; b < t.arrowCount(); b++) {
            if (t.tail(b) == v[s.tail(k)] && t.head(b) == v[s.head(k)]) {
                a[k] = b;
                extendArrows(s, t, v, a, k + 1, out);
            }
        }
    }

    public Digraph source() {
        return source;
    }

    public Digraph target() {
        return target;
    }

    public int vertex(int v) {
        return vertexMap[v];
    }

    public int arrow(int a) {
        return arrowMap[a];
    }

    /** This morphism followed by next (next o this). */
    public DigraphMorphism then(DigraphMorphism next) {
        if (!target.equals(next.source)) {
            throw new IllegalArgumentException("cannot compose: target " + target + " is not source " + next.source);
        }
        int[] v = new int[vertexMap.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = next.vertexMap[vertexMap[i]];
        }
        int[] a = new int[arrowMap.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = next.arrowMap[arrowMap[i]];
        }
        return new DigraphMorphism(source, next.target, v, a);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DigraphMorphism)) {
            return false;
        }
        DigraphMorphism m = (DigraphMorphism) o;
        return source.equals(m.source) && target.equals(m.target) && Arrays.equals(vertexMap, m.vertexMap)
               && Arrays.equals(arrowMap, m.arrowMap);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vertexMap) * 31 + Arrays.hashCode(arrowMap);
    }

    @Override
    public String toString() {
        return "vertices " + Arrays.toString(vertexMap) + ", arrows " + Arrays.toString(arrowMap);
    }
}
