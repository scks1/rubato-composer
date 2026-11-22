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
package org.rubato.math.performance;

/**
 * An initial set I of a performance cell (Mazzola, <i>The Topos of Music II</i>, &sect;34,
 * Def. 101 condition 4, p. 586). {@link #reach} is positive off the set on the side the
 * backward curves come from and at most 0 once a curve has met I. Implemented sets are
 * unions of coordinate lines {x_k = c} (departure D4).
 */
public interface InitialSet {

    /** Positive while x has not met the set, at most 0 once it has. */
    double reach(double[] x);

    /** Moves a point that met the set exactly onto it (for lines: sets the coordinate). */
    void project(double[] x);

    /**
     * The union of the lines {x_k = c_k}: reach(x) = min_k (x_k - c_k), so the set is met
     * when one coordinate falls to its bound (the left and bottom lines of &sect;34.5).
     */
    static InitialSet lines(final int[] coords, final double[] values) {
        if (coords.length == 0 || coords.length != values.length) {
            throw new IllegalArgumentException("one value per coordinate line");
        }
        final int[] c = coords.clone();
        final double[] v = values.clone();
        return new InitialSet() {
            public double reach(double[] x) {
                double m = Double.POSITIVE_INFINITY;
                for (int k = 0; k < c.length; k++) {
                    m = Math.min(m, x[c[k]] - v[k]);
                }
                return m;
            }
            public void project(double[] x) {
                int best = 0;
                for (int k = 1; k < c.length; k++) {
                    if (x[c[k]] - v[k] < x[c[best]] - v[best]) {
                        best = k;
                    }
                }
                x[c[best]] = v[best];
            }
        };
    }
}
