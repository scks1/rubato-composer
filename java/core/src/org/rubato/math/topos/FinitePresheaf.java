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
package org.rubato.math.topos;

import java.util.List;

/**
 * A finite presheaf on a {@link FiniteCategory}: a contravariant functor
 * to finite sets, the finite counterpart of a functor in Mod@ (Mazzola,
 * The Topos of Music, vol. I, section 6.2.2, p. 54). Its value at an
 * object x is the set 0..size(x)-1, and an arrow f : x -> y acts on the
 * right, sending an element e of the value at y to e.f in the value at x.
 * The constructor checks that identities act trivially and that
 * (e.g).f = e.(g after f), and refuses data that are not a functor.
 */
public final class FinitePresheaf {

    private final FiniteCategory category;
    private final int[] sizes;
    private final int[][] restrict;
    /** For a representable: the arrow behind each element, per object. */
    private final int[][] arrowOf;
    private final int represented;

    /**
     * A presheaf from its values and restriction maps.
     *
     * @param category the address category
     * @param sizes the size of the value at each object
     * @param restrict restrict[f][e] = e.f, for f : x -> y and e below sizes[y]
     * @throws IllegalArgumentException if the data are not a functor
     */
    public FinitePresheaf(FiniteCategory category, int[] sizes, int[][] restrict) {
        this(category, sizes, restrict, null, -1);
    }

    private FinitePresheaf(FiniteCategory category, int[] sizes, int[][] restrict,
                           int[][] arrowOf, int represented) {
        this.category = category;
        this.sizes = sizes.clone();
        this.restrict = new int[restrict.length][];
        for (int f = 0; f < restrict.length; f++) {
            this.restrict[f] = restrict[f].clone();
        }
        this.arrowOf = arrowOf;
        this.represented = represented;
        check();
    }

    private void check() {
        FiniteCategory c = category;
        if (sizes.length != c.objectCount() || restrict.length != c.arrowCount()) {
            throw new IllegalArgumentException("sizes do not match the category");
        }
        for (int f = 0; f < c.arrowCount(); f++) {
            if (restrict[f].length != sizes[c.cod(f)]) {
                throw new IllegalArgumentException("restriction along " + c.arrowName(f) + " has the wrong length");
            }
            for (int e : restrict[f]) {
                if (e < 0 || e >= sizes[c.dom(f)]) {
                    throw new IllegalArgumentException("restriction along " + c.arrowName(f) + " leaves its target");
                }
            }
        }
        for (int x = 0; x < c.objectCount(); x++) {
            int id = c.identity(x);
            for (int e = 0; e < sizes[x]; e++) {
                if (restrict[id][e] != e) {
                    throw new IllegalArgumentException("the identity of " + c.objectName(x) + " moves an element");
                }
            }
        }
        for (int g = 0; g < c.arrowCount(); g++) {
            for (int f : c.arrowsInto(c.dom(g))) {
                int gf = c.compose(g, f);
                for (int e = 0; e < sizes[c.cod(g)]; e++) {
                    if (restrict[f][restrict[g][e]] != restrict[gf][e]) {
                        throw new IllegalArgumentException("not a functor at " + c.arrowName(g) + " after " + c.arrowName(f));
                    }
                }
            }
        }
    }

    /**
     * The representable presheaf @y = Hom(-, y): its value at x holds the
     * arrows x -> y in index order, and f acts by e.f = e after f
     * (Topos IV p. 1428, Example 127).
     */
    public static FinitePresheaf representable(FiniteCategory c, int y) {
        int k = c.objectCount();
        int[] sizes = new int[k];
        int[][] arrowOf = new int[k][];
        int[] position = new int[c.arrowCount()];
        for (int x = 0; x < k; x++) {
            List<Integer> h = c.hom(x, y);
            sizes[x] = h.size();
            arrowOf[x] = new int[h.size()];
            for (int i = 0; i < h.size(); i++) {
                arrowOf[x][i] = h.get(i);
                position[h.get(i)] = i;
            }
        }
        int[][] restrict = new int[c.arrowCount()][];
        for (int f = 0; f < c.arrowCount(); f++) {
            int target = c.cod(f);
            restrict[f] = new int[sizes[target]];
            for (int e = 0; e < sizes[target]; e++) {
                restrict[f][e] = position[c.compose(arrowOf[target][e], f)];
            }
        }
        return new FinitePresheaf(c, sizes, restrict, arrowOf, y);
    }

    /** The terminal presheaf, one element everywhere. */
    public static FinitePresheaf terminal(FiniteCategory c) {
        int[] sizes = new int[c.objectCount()];
        java.util.Arrays.fill(sizes, 1);
        int[][] restrict = new int[c.arrowCount()][];
        for (int f = 0; f < c.arrowCount(); f++) {
            restrict[f] = new int[] { 0 };
        }
        return new FinitePresheaf(c, sizes, restrict);
    }

    public FiniteCategory category() {
        return category;
    }

    public int size(int x) {
        return sizes[x];
    }

    /** The total number of elements over all objects. */
    public int totalSize() {
        int s = 0;
        for (int v : sizes) {
            s += v;
        }
        return s;
    }

    /** e.f for f : x -> y and e in the value at y. */
    public int restrict(int f, int e) {
        return restrict[f][e];
    }

    /**
     * For a representable @y, the element of its value at dom(arrow) that
     * is the given arrow into y.
     *
     * @throws IllegalArgumentException if this is not a representable or
     *         the arrow does not end at the represented object
     */
    public int elementOf(int arrow) {
        if (arrowOf == null || category.cod(arrow) != represented) {
            throw new IllegalArgumentException("not an element of this presheaf");
        }
        int[] row = arrowOf[category.dom(arrow)];
        for (int i = 0; i < row.length; i++) {
            if (row[i] == arrow) {
                return i;
            }
        }
        throw new IllegalArgumentException("not an element of this presheaf");
    }
}
