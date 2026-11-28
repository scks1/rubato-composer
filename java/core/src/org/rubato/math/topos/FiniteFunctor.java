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

/**
 * A covariant functor X : C -&gt; FinSet on a finite category, a C-set in
 * the sense of arXiv 2611.32655 (Carcac&iacute;a-Campos, "One Monoid to
 * Realize Them All", section 1). The value X(c) is the set {0, ...,
 * size(c) - 1}, and an arrow f : c -&gt; d sends x in X(c) to
 * act(f, x) in X(d). A presheaf on C is a functor on the opposite
 * category, which {@link #ofPresheaf(FinitePresheaf)} builds.
 */
public final class FiniteFunctor {

    private final FiniteCategory category;
    private final int[] sizes;
    private final int[][] act;

    /**
     * A functor from its values and its action on arrows.
     *
     * @param category the domain category
     * @param sizes the size of X(c) for each object c
     * @param act act[f][x] = X(f)(x) for x below sizes[dom f]
     * @throws IllegalArgumentException if the data are not a functor
     */
    public FiniteFunctor(FiniteCategory category, int[] sizes, int[][] act) {
        this.category = category;
        this.sizes = sizes.clone();
        if (sizes.length != category.objectCount() || act.length != category.arrowCount()) {
            throw new IllegalArgumentException("sizes do not match the category");
        }
        this.act = new int[act.length][];
        for (int f = 0; f < act.length; f++) {
            this.act[f] = act[f].clone();
        }
        check();
    }

    private void check() {
        FiniteCategory c = category;
        for (int f = 0; f < c.arrowCount(); f++) {
            if (act[f].length != sizes[c.dom(f)]) {
                throw new IllegalArgumentException("action of " + c.arrowName(f) + " has the wrong length");
            }
            for (int y : act[f]) {
                if (y < 0 || y >= sizes[c.cod(f)]) {
                    throw new IllegalArgumentException("action of " + c.arrowName(f) + " leaves its target");
                }
            }
        }
        for (int o = 0; o < c.objectCount(); o++) {
            int id = c.identity(o);
            for (int x = 0; x < sizes[o]; x++) {
                if (act[id][x] != x) {
                    throw new IllegalArgumentException("the identity of " + c.objectName(o) + " moves an element");
                }
            }
        }
        for (int g = 0; g < c.arrowCount(); g++) {
            for (int f : c.arrowsInto(c.dom(g))) {
                int gf = c.compose(g, f);
                for (int x = 0; x < sizes[c.dom(f)]; x++) {
                    if (act[g][act[f][x]] != act[gf][x]) {
                        throw new IllegalArgumentException("not a functor at " + c.arrowName(g) + " after " + c.arrowName(f));
                    }
                }
            }
        }
    }

    /**
     * The presheaf P as a functor on the opposite of its category: an arrow
     * f : x -&gt; y of C is an arrow y -&gt; x of the opposite and sends
     * e in P(y) to e.f in P(x).
     */
    public static FiniteFunctor ofPresheaf(FinitePresheaf p) {
        FiniteCategory c = p.category();
        int[] sizes = new int[c.objectCount()];
        for (int o = 0; o < sizes.length; o++) {
            sizes[o] = p.size(o);
        }
        int[][] a = new int[c.arrowCount()][];
        for (int f = 0; f < a.length; f++) {
            a[f] = new int[p.size(c.cod(f))];
            for (int e = 0; e < a[f].length; e++) {
                a[f][e] = p.restrict(f, e);
            }
        }
        return new FiniteFunctor(c.opposite(), sizes, a);
    }

    public FiniteCategory category() {
        return category;
    }

    public int size(int c) {
        return sizes[c];
    }

    /** X(f)(x) for x in X(dom f). */
    public int act(int f, int x) {
        return act[f][x];
    }

    /** The number of elements of the category of elements. */
    public int totalSize() {
        int t = 0;
        for (int s : sizes) {
            t += s;
        }
        return t;
    }
}
