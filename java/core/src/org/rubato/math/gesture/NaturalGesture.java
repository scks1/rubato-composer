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
 * The natural gesture of a digraph Delta in R^V + C^A, coordinates laid
 * out as the |V| real coordinates followed by a real and an imaginary
 * coordinate for each arrow (Mazzola, The Topos of Music, vol. III,
 * section 61.7, pp. 918-919). Vertex v sits at the unit vector e_v.
 */
public final class NaturalGesture {

    private NaturalGesture() { /* static utility */ }

    /**
     * The curves as printed on p. 919: arrow a from t to h is
     * s -> ((1-s) e_t + s e_h, exp(2 pi i s) e_a). Their ends lie at
     * distance 1 from the vertex points, so the result is not a gesture;
     * it is returned unchecked to show this.
     */
    public static Gesture literal(Digraph d) {
        return build(d, false);
    }

    /**
     * The corrected natural gesture (departure D3): the complex part is
     * (exp(2 pi i s) - 1) e_a, which vanishes at both ends, keeps parallel
     * arrows apart and draws each loop as a circle of radius 1.
     */
    public static Gesture of(Digraph d) {
        return build(d, true);
    }

    private static Gesture build(Digraph d, boolean corrected) {
        int nv = d.vertexCount(), na = d.arrowCount();
        int dim = nv + 2 * na;
        double[][] points = new double[nv][dim];
        for (int v = 0; v < nv; v++) {
            points[v][v] = 1;
        }
        Curve[] curves = new Curve[na];
        for (int a = 0; a < na; a++) {
            final int arrow = a, t = d.tail(a), h = d.head(a);
            curves[a] = new FunctionCurve(dim, s -> {
                double[] p = new double[dim];
                p[t] += 1 - s;
                p[h] += s;
                double angle = 2 * Math.PI * s;
                p[nv + 2 * arrow] = Math.cos(angle) - (corrected ? 1 : 0);
                p[nv + 2 * arrow + 1] = Math.sin(angle);
                return p;
            });
        }
        return corrected ? new Gesture(d, points, curves) : Gesture.unchecked(d, points, curves);
    }
}
