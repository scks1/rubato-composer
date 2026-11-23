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

/**
 * The subobject classifier of the topos of digraphs (Mazzola, The Topos of
 * Music, vol. IV, appendix J.1.3, pp. 1459-1460). A digraph is a presheaf
 * on the category with objects V, A and two arrows t, h : V -> A. The
 * vertices of Omega are the sieves on V (empty: FALSE, maximal: TRUE) and
 * its arrows the sieves on A, which are the subsets of {id, t, h} that
 * contain t and h whenever they contain id. The tail of a sieve on A is
 * its pullback along t and its head its pullback along h.
 */
public final class DigraphTopos {

    /** The vertex f (empty sieve). */
    public static final int FALSE = 0;
    /** The vertex t (maximal sieve). */
    public static final int TRUE = 1;

    /** Arrow names as in the figure on vol. IV, p. 1460, in sieve order. */
    public static final int F = 0;   // empty sieve, loop at f
    public static final int N = 1;   // {t}, t -> f
    public static final int P = 2;   // {h}, f -> t
    public static final int Q = 3;   // {t, h}, loop at t
    public static final int T = 4;   // maximal sieve, loop at t: true

    private static final boolean[][] SIEVES = {
        // contains id, contains t, contains h
        { false, false, false },
        { false, true,  false },
        { false, false, true  },
        { false, true,  true  },
        { true,  true,  true  },
    };

    private static final Digraph OMEGA = build();

    private DigraphTopos() { /* static utility */ }

    private static Digraph build() {
        int[][] arrows = new int[SIEVES.length][];
        for (int s = 0; s < SIEVES.length; s++) {
            boolean id = SIEVES[s][0], t = SIEVES[s][1], h = SIEVES[s][2];
            if (id && !(t && h)) {
                throw new IllegalStateException("not a sieve");
            }
            arrows[s] = new int[] { t ? TRUE : FALSE, h ? TRUE : FALSE };
        }
        return Digraph.of(2, arrows);
    }

    /** Omega, two vertices and five arrows. */
    public static Digraph omega() {
        return OMEGA;
    }

    /** The arrow true : 1 -> Omega picks out. */
    public static int trueArrow() {
        return T;
    }

    private static int sieve(boolean id, boolean t, boolean h) {
        for (int s = 0; s < SIEVES.length; s++) {
            if (SIEVES[s][0] == id && SIEVES[s][1] == t && SIEVES[s][2] == h) {
                return s;
            }
        }
        throw new IllegalArgumentException("not a sieve");
    }

    /** The characteristic morphism chi_S : X -> Omega of a subdigraph S of X. */
    public static DigraphMorphism characteristic(Subdigraph s) {
        Digraph x = s.ambient();
        int[] v = new int[x.vertexCount()];
        for (int i = 0; i < v.length; i++) {
            v[i] = s.containsVertex(i) ? TRUE : FALSE;
        }
        int[] a = new int[x.arrowCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = sieve(s.containsArrow(i), s.containsVertex(x.tail(i)), s.containsVertex(x.head(i)));
        }
        return new DigraphMorphism(x, OMEGA, v, a);
    }

    /** The pullback of true along chi : X -> Omega, as a subdigraph of X. */
    public static Subdigraph pullbackOfTrue(DigraphMorphism chi) {
        if (!chi.target().equals(OMEGA)) {
            throw new IllegalArgumentException("target is not Omega");
        }
        Digraph x = chi.source();
        boolean[] v = new boolean[x.vertexCount()];
        for (int i = 0; i < v.length; i++) {
            v[i] = chi.vertex(i) == TRUE;
        }
        boolean[] a = new boolean[x.arrowCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = chi.arrow(i) == T;
        }
        return new Subdigraph(x, v, a);
    }
}
