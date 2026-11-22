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
 * A tempo curve T(E) > 0 in score units per minute, with the integral of 1/T
 * that turns symbolic onsets into physical time (Mazzola, <i>The Topos of
 * Music II</i>, &sect;33.1.1, eq. (33.1), p. 562: e(E) = e0 + &int; 1/T).
 */
public interface TempoCurve {

    /** The tempo at the symbolic onset e, in score units per minute. */
    double tempo(double e);

    /**
     * The integral of 1/T from a to b, in minutes; negative when b &lt; a.
     *
     * @throws IllegalArgumentException if T is not positive and finite on [a, b]
     */
    double integral(double a, double b);
}
