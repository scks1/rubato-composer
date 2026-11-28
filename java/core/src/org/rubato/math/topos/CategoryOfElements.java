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

import java.util.ArrayList;
import java.util.List;

/**
 * The category of elements &int;X of a finite functor X : C -&gt; FinSet
 * (arXiv 2611.32655, section 1.1). Its objects are the pairs (c, x) with
 * x in X(c), numbered object by object; its arrows (c, x) -&gt; (d, y) are
 * the arrows f : c -&gt; d of C with X(f)(x) = y, so each arrow is a pair
 * (f, x).
 */
public final class CategoryOfElements {

    private final FiniteFunctor functor;
    private final FiniteCategory category;
    private final int[] offset;
    private final int[] objectBase;
    private final int[] objectElement;
    private final int[] arrowBase;
    private final int[] arrowElement;

    public CategoryOfElements(FiniteFunctor x) {
        functor = x;
        FiniteCategory c = x.category();
        int k = c.objectCount();
        offset = new int[k];
        int n = 0;
        for (int o = 0; o < k; o++) {
            offset[o] = n;
            n += x.size(o);
        }
        objectBase = new int[n];
        objectElement = new int[n];
        String[] objectNames = new String[n];
        for (int o = 0; o < k; o++) {
            for (int e = 0; e < x.size(o); e++) {
                objectBase[offset[o] + e] = o;
                objectElement[offset[o] + e] = e;
                objectNames[offset[o] + e] = "(" + c.objectName(o) + "," + e + ")";
            }
        }
        List<Integer> bases = new ArrayList<Integer>();
        List<Integer> elements = new ArrayList<Integer>();
        int[][] index = new int[c.arrowCount()][];
        for (int f = 0; f < c.arrowCount(); f++) {
            index[f] = new int[x.size(c.dom(f))];
            for (int e = 0; e < index[f].length; e++) {
                index[f][e] = bases.size();
                bases.add(f);
                elements.add(e);
            }
        }
        int m = bases.size();
        arrowBase = new int[m];
        arrowElement = new int[m];
        int[] dom = new int[m];
        int[] cod = new int[m];
        String[] arrowNames = new String[m];
        for (int a = 0; a < m; a++) {
            int f = bases.get(a);
            int e = elements.get(a);
            arrowBase[a] = f;
            arrowElement[a] = e;
            dom[a] = offset[c.dom(f)] + e;
            cod[a] = offset[c.cod(f)] + x.act(f, e);
            arrowNames[a] = "(" + c.arrowName(f) + "," + e + ")";
        }
        int[] identity = new int[n];
        for (int o = 0; o < n; o++) {
            identity[o] = index[c.identity(objectBase[o])][objectElement[o]];
        }
        int[][] compose = new int[m][m];
        for (int g = 0; g < m; g++) {
            for (int f = 0; f < m; f++) {
                compose[g][f] = -1;
                if (cod[f] == dom[g]) {
                    compose[g][f] = index[c.compose(arrowBase[g], arrowBase[f])][arrowElement[f]];
                }
            }
        }
        category = FiniteCategory.of(objectNames, dom, cod, arrowNames, identity, compose);
    }

    public FiniteFunctor functor() {
        return functor;
    }

    /** &int;X as a finite category. */
    public FiniteCategory category() {
        return category;
    }

    /** The object (c, x). */
    public int objectOf(int c, int x) {
        if (x < 0 || x >= functor.size(c)) {
            throw new IllegalArgumentException("no element " + x + " at " + c);
        }
        return offset[c] + x;
    }

    /** The object c of C under an object (c, x). */
    public int baseObject(int o) {
        return objectBase[o];
    }

    /** The element x under an object (c, x). */
    public int element(int o) {
        return objectElement[o];
    }

    /** The arrow f of C under an arrow (f, x). */
    public int baseArrow(int a) {
        return arrowBase[a];
    }
}
