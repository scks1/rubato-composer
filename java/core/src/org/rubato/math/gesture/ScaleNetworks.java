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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The digraph Gamma_C of Example 69 (Mazzola, The Topos of Music, vol. III,
 * section 61.13, p. 930, figure on p. 931): its vertices are the seven
 * pitch classes of C major and its arrows those of the seven degree
 * networks, where the network of a degree is the orbit of a start tone p
 * under x -> t + g x mod 12, written there as the network of T^t g at p.
 * The networks are the book's data, not derived here.
 */
public final class ScaleNetworks {

    /** Degree name, then t, g and the start tone, as listed on p. 930. */
    private static final Object[][] C_MAJOR = {
        { "I",   7, 3, 0 },
        { "II",  11, 3, 9 },
        { "III", 7, 3, 11 },
        { "IV",  9, 3, 5 },
        { "V",   5, 3, 7 },
        { "VI",  9, 3, 4 },
        { "VII", 1, 2, 2 },
    };

    private static final int[] C_SCALE = { 0, 2, 4, 5, 7, 9, 11 };

    private final int[] scale;
    private final Digraph digraph;
    private final Map<String, Subdigraph> degrees;

    private ScaleNetworks(int[] scale, Digraph digraph, Map<String, Subdigraph> degrees) {
        this.scale = scale;
        this.digraph = digraph;
        this.degrees = degrees;
    }

    /**
     * The arrows {x, t + g x mod 12} of the orbit of p, in the order the
     * orbit visits them; the last arrow closes the orbit.
     */
    public static List<int[]> network(int t, int g, int p) {
        List<int[]> out = new ArrayList<int[]>();
        List<Integer> seen = new ArrayList<Integer>();
        int x = Math.floorMod(p, 12);
        while (!seen.contains(x)) {
            seen.add(x);
            int y = Math.floorMod(t + g * x, 12);
            out.add(new int[] { x, y });
            x = y;
        }
        return out;
    }

    /**
     * Gamma_C. With mergeParallel false there is one arrow for each tail,
     * head and transformation (17 arrows); with true, one arrow for each
     * tail and head (16 arrows: II and VII both have 2 -> 5).
     */
    public static ScaleNetworks cMajor(boolean mergeParallel) {
        Map<String, int[]> keys = new LinkedHashMap<String, int[]>();
        Map<String, List<String>> arrowKeysOfDegree = new LinkedHashMap<String, List<String>>();
        for (Object[] d : C_MAJOR) {
            int t = (Integer) d[1], g = (Integer) d[2], p = (Integer) d[3];
            List<String> ks = new ArrayList<String>();
            for (int[] a : network(t, g, p)) {
                String k = a[0] + ">" + a[1] + (mergeParallel ? "" : ":" + t + "," + g);
                keys.put(k, a);
                ks.add(k);
            }
            arrowKeysOfDegree.put((String) d[0], ks);
        }
        List<String> order = new ArrayList<String>(keys.keySet());
        int[][] arrows = new int[order.size()][];
        for (int i = 0; i < arrows.length; i++) {
            int[] a = keys.get(order.get(i));
            arrows[i] = new int[] { index(C_SCALE, a[0]), index(C_SCALE, a[1]) };
        }
        Digraph dg = Digraph.of(C_SCALE.length, arrows);
        Map<String, Subdigraph> degs = new LinkedHashMap<String, Subdigraph>();
        for (Map.Entry<String, List<String>> e : arrowKeysOfDegree.entrySet()) {
            boolean[] v = new boolean[dg.vertexCount()];
            boolean[] a = new boolean[dg.arrowCount()];
            for (String k : e.getValue()) {
                int i = order.indexOf(k);
                a[i] = true;
                v[dg.tail(i)] = true;
                v[dg.head(i)] = true;
            }
            degs.put(e.getKey(), new Subdigraph(dg, v, a));
        }
        return new ScaleNetworks(C_SCALE, dg, degs);
    }

    private static int index(int[] scale, int pc) {
        for (int i = 0; i < scale.length; i++) {
            if (scale[i] == pc) {
                return i;
            }
        }
        throw new IllegalArgumentException("pitch class " + pc + " is not in the scale");
    }

    public Digraph digraph() {
        return digraph;
    }

    /** The vertex of a pitch class of the scale. */
    public int vertexOf(int pitchClass) {
        return index(scale, pitchClass);
    }

    /** The pitch class of a vertex. */
    public int pitchClass(int vertex) {
        return scale[vertex];
    }

    /** The subdigraph of a degree network, by its Roman numeral. */
    public Subdigraph degree(String name) {
        Subdigraph s = degrees.get(name);
        if (s == null) {
            throw new IllegalArgumentException("no degree " + name);
        }
        return s;
    }

    /** True if s contains an arrow from one pitch class to another. */
    public boolean containsArrow(Subdigraph s, int fromPitchClass, int toPitchClass) {
        int t = vertexOf(fromPitchClass), h = vertexOf(toPitchClass);
        for (int a = 0; a < digraph.arrowCount(); a++) {
            if (s.containsArrow(a) && digraph.tail(a) == t && digraph.head(a) == h) {
                return true;
            }
        }
        return false;
    }

    /** A subdigraph written with pitch classes, for reports. */
    public String describe(Subdigraph s) {
        StringBuilder b = new StringBuilder("{");
        for (int v = 0; v < digraph.vertexCount(); v++) {
            if (s.containsVertex(v)) {
                b.append(b.length() > 1 ? " " : "").append(scale[v]);
            }
        }
        b.append(";");
        for (int a = 0; a < digraph.arrowCount(); a++) {
            if (s.containsArrow(a)) {
                b.append(" ").append(scale[digraph.tail(a)]).append("->").append(scale[digraph.head(a)]);
            }
        }
        return b.append("}").toString();
    }
}
