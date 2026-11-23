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

import java.util.function.DoubleFunction;
import java.util.function.UnaryOperator;

/**
 * A curve given by a function of s in [0, 1]. Continuity is the caller's
 * responsibility; sup distances involving it are sampled.
 */
public final class FunctionCurve extends Curve {

    private final int dimension;
    private final DoubleFunction<double[]> f;

    public FunctionCurve(int dimension, DoubleFunction<double[]> f) {
        this.dimension = dimension;
        this.f = f;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public double[] at(double s) {
        double[] p = f.apply(Math.max(0, Math.min(1, s)));
        if (p.length != dimension) {
            throw new IllegalStateException("curve returned dimension " + p.length + ", not " + dimension);
        }
        return p;
    }

    @Override
    public Curve map(UnaryOperator<double[]> h) {
        return new FunctionCurve(dimension, s -> h.apply(at(s)));
    }
}
