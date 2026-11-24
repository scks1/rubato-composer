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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * A subpresheaf S of a {@link FinitePresheaf} P: a subset of each value,
 * closed under restriction. With the operations of Prop. 125 (Mazzola,
 * The Topos of Music, vol. IV, p. 1433) the subpresheaves of P form a
 * Heyting algebra:
 * <ul>
 * <li>join and meet are union and intersection at each object;</li>
 * <li>x lies in (S implies T) at X iff for every f : Y -> X, x.f in S
 *     gives x.f in T;</li>
 * <li>x lies in (not S) at X iff x.f lies outside S for every f : Y -> X.</li>
 * </ul>
 * Instances are immutable; the constructor refuses a family that is not
 * closed under restriction.
 */
public final class Subpresheaf {

    private final FinitePresheaf ambient;
    private final boolean[][] in;

    /**
     * @param ambient the presheaf P
     * @param in in[x][e] says whether element e of P at x belongs
     * @throws IllegalArgumentException if the family is not closed under restriction
     */
    public Subpresheaf(FinitePresheaf ambient, boolean[][] in) {
        this.ambient = ambient;
        FiniteCategory c = ambient.category();
        if (in.length != c.objectCount()) {
            throw new IllegalArgumentException("one row per object needed");
        }
        this.in = new boolean[in.length][];
        for (int x = 0; x < in.length; x++) {
            if (in[x].length != ambient.size(x)) {
                throw new IllegalArgumentException("row " + x + " has the wrong length");
            }
            this.in[x] = in[x].clone();
        }
        for (int f = 0; f < c.arrowCount(); f++) {
            int y = c.cod(f);
            for (int e = 0; e < ambient.size(y); e++) {
                if (this.in[y][e] && !this.in[c.dom(f)][ambient.restrict(f, e)]) {
                    throw new IllegalArgumentException("not closed under restriction along " + c.arrowName(f));
                }
            }
        }
    }

    /** The same as the constructor, with one row per object as varargs. */
    public static Subpresheaf of(FinitePresheaf ambient, boolean[]... in) {
        return new Subpresheaf(ambient, in);
    }

    /**
     * The smallest subpresheaf containing the given elements, each given as
     * {object, element}.
     */
    public static Subpresheaf generatedBy(FinitePresheaf ambient, int[][] generators) {
        boolean[][] in = empty(ambient);
        Deque<int[]> todo = new ArrayDeque<int[]>();
        for (int[] g : generators) {
            if (!in[g[0]][g[1]]) {
                in[g[0]][g[1]] = true;
                todo.add(g);
            }
        }
        close(ambient, in, todo);
        return new Subpresheaf(ambient, in);
    }

    /** Adds the restrictions of every queued element until none is missing. */
    static void close(FinitePresheaf ambient, boolean[][] in, Deque<int[]> todo) {
        FiniteCategory c = ambient.category();
        while (!todo.isEmpty()) {
            int[] g = todo.poll();
            for (int f : c.arrowsInto(g[0])) {
                int x = c.dom(f);
                int e = ambient.restrict(f, g[1]);
                if (!in[x][e]) {
                    in[x][e] = true;
                    todo.add(new int[] { x, e });
                }
            }
        }
    }

    static boolean[][] empty(FinitePresheaf ambient) {
        int k = ambient.category().objectCount();
        boolean[][] in = new boolean[k][];
        for (int x = 0; x < k; x++) {
            in[x] = new boolean[ambient.size(x)];
        }
        return in;
    }

    /** P itself. */
    public static Subpresheaf top(FinitePresheaf ambient) {
        boolean[][] in = empty(ambient);
        for (boolean[] row : in) {
            Arrays.fill(row, true);
        }
        return new Subpresheaf(ambient, in);
    }

    /** The empty subpresheaf. */
    public static Subpresheaf bottom(FinitePresheaf ambient) {
        return new Subpresheaf(ambient, empty(ambient));
    }

    public FinitePresheaf ambient() {
        return ambient;
    }

    public boolean contains(int x, int e) {
        return in[x][e];
    }

    /** The number of elements at x. */
    public int size(int x) {
        int s = 0;
        for (boolean b : in[x]) {
            if (b) {
                s++;
            }
        }
        return s;
    }

    private void same(Subpresheaf t) {
        if (t.ambient != ambient) {
            throw new IllegalArgumentException("different ambient presheaves");
        }
    }

    public boolean leq(Subpresheaf t) {
        same(t);
        for (int x = 0; x < in.length; x++) {
            for (int e = 0; e < in[x].length; e++) {
                if (in[x][e] && !t.in[x][e]) {
                    return false;
                }
            }
        }
        return true;
    }

    public Subpresheaf join(Subpresheaf t) {
        same(t);
        boolean[][] out = empty(ambient);
        for (int x = 0; x < in.length; x++) {
            for (int e = 0; e < in[x].length; e++) {
                out[x][e] = in[x][e] || t.in[x][e];
            }
        }
        return new Subpresheaf(ambient, out);
    }

    public Subpresheaf meet(Subpresheaf t) {
        same(t);
        boolean[][] out = empty(ambient);
        for (int x = 0; x < in.length; x++) {
            for (int e = 0; e < in[x].length; e++) {
                out[x][e] = in[x][e] && t.in[x][e];
            }
        }
        return new Subpresheaf(ambient, out);
    }

    /** Prop. 125 (iii). */
    public Subpresheaf implies(Subpresheaf t) {
        same(t);
        FiniteCategory c = ambient.category();
        boolean[][] out = empty(ambient);
        for (int x = 0; x < in.length; x++) {
            for (int e = 0; e < in[x].length; e++) {
                boolean ok = true;
                for (int f : c.arrowsInto(x)) {
                    int y = c.dom(f);
                    int r = ambient.restrict(f, e);
                    if (in[y][r] && !t.in[y][r]) {
                        ok = false;
                        break;
                    }
                }
                out[x][e] = ok;
            }
        }
        return new Subpresheaf(ambient, out);
    }

    /** Prop. 125 (iv). */
    public Subpresheaf negation() {
        FiniteCategory c = ambient.category();
        boolean[][] out = empty(ambient);
        for (int x = 0; x < in.length; x++) {
            for (int e = 0; e < in[x].length; e++) {
                boolean ok = true;
                for (int f : c.arrowsInto(x)) {
                    if (in[c.dom(f)][ambient.restrict(f, e)]) {
                        ok = false;
                        break;
                    }
                }
                out[x][e] = ok;
            }
        }
        return new Subpresheaf(ambient, out);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Subpresheaf)) {
            return false;
        }
        Subpresheaf t = (Subpresheaf) o;
        return t.ambient == ambient && Arrays.deepEquals(in, t.in);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(in);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        FiniteCategory c = ambient.category();
        for (int x = 0; x < in.length; x++) {
            if (x > 0) {
                b.append("; ");
            }
            b.append(c.objectName(x)).append(':');
            for (int e = 0; e < in[x].length; e++) {
                if (in[x][e]) {
                    b.append(' ').append(e);
                }
            }
        }
        return b.append('}').toString();
    }
}
