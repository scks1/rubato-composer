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
 * A vector field on R^n, a performance field in the sense of Mazzola, <i>The Topos of
 * Music II</i>, &sect;33.2.2, eq. (33.11), p. 568: Z = (J p)^-1 Delta. Fields used for
 * performance must be locally Lipschitz.
 */
public interface Field {

    /** The dimension n of the parameter space. */
    int dimension();

    /** The field vector at x, a new array of length n. */
    double[] at(double[] x);
}
