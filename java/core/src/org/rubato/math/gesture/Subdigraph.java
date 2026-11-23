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
 * A subdigraph of a finite digraph: a set of vertices and a set of arrows
 * whose tails and heads lie in it. Subdigraphs are the subobjects of the
 * topos of digraphs and form a Heyting algebra (Mazzola, The Topos of
 * Music, vol. IV, Thm. 72, p. 1433; vol. III, section 61.13, p. 930).
 */
public final class Subdigraph {

    private final Digraph ambient;
    private final boolean[] vertices;
    private final boolean[] arrows;

    /**
     * Creates the subdigraph.
     *
     * @throws IllegalArgumentException if an arrow is included without its
     *         tail or head, or the lengths do not fit the ambient digraph
     */
    public Subdigraph(Digraph ambient, boolean[] vertices, boolean[] arrows) {
        if (vertices.length != ambient.vertexCount() || arrows.length != ambient.arrowCount()) {
            throw new IllegalArgumentException("sizes do not fit " + ambient);
        }
        for (int a = 0; a < arrows.length; a++) {
            if (arrows[a] && !(vertices[ambient.tail(a)] && vertices[ambient.head(a)])) {
                throw new IllegalArgumentException("arrow " + a + " is included without its tail or head");
            }
        }
        this.ambient = ambient;
        this.vertices = vertices.clone();
        this.arrows = arrows.clone();
    }

    public static Subdigraph top(Digraph d) {
        boolean[] v = new boolean[d.vertexCount()];
        Arrays.fill(v, true);
        boolean[] a = new boolean[d.arrowCount()];
        Arrays.fill(a, true);
        return new Subdigraph(d, v, a);
    }

    public static Subdigraph bottom(Digraph d) {
        return new Subdigraph(d, new boolean[d.vertexCount()], new boolean[d.arrowCount()]);
    }

    /** Every subdigraph of d. */
    public static List<Subdigraph> all(Digraph d) {
        List<Subdigraph> out = new ArrayList<Subdigraph>();
        int nv = d.vertexCount();
        if (nv > 20) {
            throw new IllegalArgumentException("too many vertices to enumerate: " + nv);
        }
        for (long vm = 0; vm < (1L << nv); vm++) {
            boolean[] v = new boolean[nv];
            for (int i = 0; i < nv; i++) {
                v[i] = (vm >> i & 1) == 1;
            }
            List<Integer> allowed = new ArrayList<Integer>();
            for (int a = 0; a < d.arrowCount(); a++) {
                if (v[d.tail(a)] && v[d.head(a)]) {
                    allowed.add(a);
                }
            }
            if (allowed.size() > 20) {
                throw new IllegalArgumentException("too many arrows to enumerate");
            }
            for (long am = 0; am < (1L << allowed.size()); am++) {
                boolean[] a = new boolean[d.arrowCount()];
                for (int i = 0; i < allowed.size(); i++) {
                    a[allowed.get(i)] = (am >> i & 1) == 1;
                }
                out.add(new Subdigraph(d, v, a));
            }
        }
        return out;
    }

    public Digraph ambient() {
        return ambient;
    }

    public boolean containsVertex(int v) {
        return vertices[v];
    }

    public boolean containsArrow(int a) {
        return arrows[a];
    }

    public int vertexCount() {
        int n = 0;
        for (boolean b : vertices) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    public int arrowCount() {
        int n = 0;
        for (boolean b : arrows) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    private void check(Subdigraph s) {
        if (!ambient.equals(s.ambient)) {
            throw new IllegalArgumentException("subdigraphs of different digraphs");
        }
    }

    public boolean leq(Subdigraph s) {
        check(s);
        for (int v = 0; v < vertices.length; v++) {
            if (vertices[v] && !s.vertices[v]) {
                return false;
            }
        }
        for (int a = 0; a < arrows.length; a++) {
            if (arrows[a] && !s.arrows[a]) {
                return false;
            }
        }
        return true;
    }

    public Subdigraph meet(Subdigraph s) {
        check(s);
        boolean[] v = new boolean[vertices.length];
        boolean[] a = new boolean[arrows.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = vertices[i] && s.vertices[i];
        }
        for (int i = 0; i < a.length; i++) {
            a[i] = arrows[i] && s.arrows[i];
        }
        return new Subdigraph(ambient, v, a);
    }

    public Subdigraph join(Subdigraph s) {
        check(s);
        boolean[] v = new boolean[vertices.length];
        boolean[] a = new boolean[arrows.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = vertices[i] || s.vertices[i];
        }
        for (int i = 0; i < a.length; i++) {
            a[i] = arrows[i] || s.arrows[i];
        }
        return new Subdigraph(ambient, v, a);
    }

    /**
     * The Heyting implication this => s, computed as for presheaves: a
     * vertex is in it if it is not in this or is in s; an arrow is in it
     * if its tail and head are and it is not in this or is in s.
     */
    public Subdigraph implies(Subdigraph s) {
        check(s);
        boolean[] v = new boolean[vertices.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = !vertices[i] || s.vertices[i];
        }
        boolean[] a = new boolean[arrows.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = v[ambient.tail(i)] && v[ambient.head(i)] && (!arrows[i] || s.arrows[i]);
        }
        return new Subdigraph(ambient, v, a);
    }

    /**
     * The implication this => s as described in four parts in vol. III,
     * p. 930: the vertices outside this together with those of s; the
     * arrows common to this and s, the arrows between vertices of both
     * that are not in this, the arrows between vertices outside this, and
     * the arrows joining a vertex of both to a vertex outside this.
     */
    public Subdigraph bookImplies(Subdigraph s) {
        check(s);
        boolean[] v = new boolean[vertices.length];
        boolean[] both = new boolean[vertices.length];
        boolean[] outside = new boolean[vertices.length];
        for (int i = 0; i < v.length; i++) {
            both[i] = vertices[i] && s.vertices[i];
            outside[i] = !vertices[i];
            v[i] = outside[i] || s.vertices[i];
        }
        boolean[] a = new boolean[arrows.length];
        for (int i = 0; i < a.length; i++) {
            int t = ambient.tail(i), h = ambient.head(i);
            boolean common = arrows[i] && s.arrows[i];
            boolean bothNotHere = both[t] && both[h] && !arrows[i];
            boolean outsideArrow = outside[t] && outside[h];
            boolean joining = (both[t] && outside[h]) || (outside[t] && both[h]);
            a[i] = common || bothNotHere || outsideArrow || joining;
        }
        return new Subdigraph(ambient, v, a);
    }

    /** The pseudo-complement, this => bottom. */
    public Subdigraph negation() {
        return implies(bottom(ambient));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Subdigraph)) {
            return false;
        }
        Subdigraph s = (Subdigraph) o;
        return ambient.equals(s.ambient) && Arrays.equals(vertices, s.vertices) && Arrays.equals(arrows, s.arrows);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vertices) * 31 + Arrays.hashCode(arrows);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        for (int v = 0; v < vertices.length; v++) {
            if (vertices[v]) {
                b.append(b.length() > 1 ? " " : "").append(v);
            }
        }
        b.append(";");
        for (int a = 0; a < arrows.length; a++) {
            if (arrows[a]) {
                b.append(" ").append(ambient.tail(a)).append("->").append(ambient.head(a));
            }
        }
        return b.append("}").toString();
    }
}
