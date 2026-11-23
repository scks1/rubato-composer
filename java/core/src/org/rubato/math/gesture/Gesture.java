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

import java.util.function.UnaryOperator;

/**
 * A gesture with skeleton Delta in R^n: a digraph morphism from Delta to
 * the spatial digraph of R^n, given by a point for every vertex and a
 * curve for every arrow that starts at the tail's point and ends at the
 * head's point (Mazzola, The Topos of Music, vol. III, section 61.5,
 * p. 914).
 */
public final class Gesture {

    /**
     * Absolute tolerance for curve ends against vertex points. Coordinates
     * in this package stay below about 1e4 and the checks take a few
     * operations, so rounding stays near 1e-12.
     */
    public static final double TOLERANCE = 1e-9;

    private final Digraph skeleton;
    private final double[][] vertexPoints;
    private final Curve[] curves;
    private final int dimension;

    private Gesture(Digraph skeleton, double[][] vertexPoints, Curve[] curves, boolean check) {
        if (vertexPoints.length != skeleton.vertexCount() || curves.length != skeleton.arrowCount()) {
            throw new IllegalArgumentException("one point per vertex and one curve per arrow needed");
        }
        int dim = -1;
        for (double[] p : vertexPoints) {
            dim = checkDim(dim, p.length);
        }
        for (Curve c : curves) {
            dim = checkDim(dim, c.dimension());
        }
        this.skeleton = skeleton;
        this.vertexPoints = new double[vertexPoints.length][];
        for (int i = 0; i < vertexPoints.length; i++) {
            this.vertexPoints[i] = vertexPoints[i].clone();
        }
        this.curves = curves.clone();
        this.dimension = Math.max(dim, 0);
        if (check && endpointGap() > TOLERANCE) {
            throw new IllegalArgumentException("not a gesture: a curve end misses its vertex point by " + endpointGap());
        }
    }

    private static int checkDim(int dim, int d) {
        if (dim >= 0 && d != dim) {
            throw new IllegalArgumentException("dimensions " + dim + " and " + d);
        }
        return d;
    }

    /**
     * Creates the gesture.
     *
     * @throws IllegalArgumentException if a curve end misses its vertex
     *         point by more than TOLERANCE
     */
    public Gesture(Digraph skeleton, double[][] vertexPoints, Curve[] curves) {
        this(skeleton, vertexPoints, curves, true);
    }

    /** The same data without the endpoint check, for showing where a construction fails. */
    public static Gesture unchecked(Digraph skeleton, double[][] vertexPoints, Curve[] curves) {
        return new Gesture(skeleton, vertexPoints, curves, false);
    }

    public Digraph skeleton() {
        return skeleton;
    }

    public int dimension() {
        return dimension;
    }

    public double[] vertex(int v) {
        return vertexPoints[v].clone();
    }

    public Curve curve(int a) {
        return curves[a];
    }

    /** The largest distance between a curve end and its vertex point. */
    public double endpointGap() {
        double max = 0;
        for (int a = 0; a < curves.length; a++) {
            max = Math.max(max, Curve.distance(curves[a].at(0), vertexPoints[skeleton.tail(a)]));
            max = Math.max(max, Curve.distance(curves[a].at(1), vertexPoints[skeleton.head(a)]));
        }
        return max;
    }

    private void checkSkeleton(Gesture g) {
        if (!skeleton.equals(g.skeleton)) {
            throw new IllegalArgumentException("gestures on different skeletons");
        }
    }

    /**
     * The distance of vol. III, p. 916: the sum over the arrows of the sup
     * distance of their curves. Vertices without arrows do not enter, so
     * it is not a metric when the skeleton has isolated vertices.
     */
    public double bookDistance(Gesture g) {
        checkSkeleton(g);
        double sum = 0;
        for (int a = 0; a < curves.length; a++) {
            sum += Curve.supDistance(curves[a], g.curves[a]);
        }
        return sum;
    }

    /** bookDistance plus the distances at isolated vertices: a metric on all skeletons. */
    public double distance(Gesture g) {
        double sum = bookDistance(g);
        for (int v = 0; v < vertexPoints.length; v++) {
            if (skeleton.isIsolated(v)) {
                sum += Curve.distance(vertexPoints[v], g.vertexPoints[v]);
            }
        }
        return sum;
    }

    /** The gesture h o this, for a continuous h : R^n -> R^m (see {@link Curve#map}). */
    public Gesture map(UnaryOperator<double[]> h) {
        double[][] p = new double[vertexPoints.length][];
        for (int v = 0; v < p.length; v++) {
            p[v] = h.apply(vertexPoints[v].clone());
        }
        Curve[] c = new Curve[curves.length];
        for (int a = 0; a < c.length; a++) {
            c[a] = curves[a].map(h);
        }
        return new Gesture(skeleton, p, c);
    }

    /**
     * True if (f, h) is a gesture morphism from this to target (vol. III,
     * p. 915): h of each vertex point is the point of its image vertex and
     * h of each curve is the curve of its image arrow, within TOLERANCE.
     */
    public boolean isMorphism(DigraphMorphism f, UnaryOperator<double[]> h, Gesture target) {
        if (!f.source().equals(skeleton) || !f.target().equals(target.skeleton)) {
            return false;
        }
        for (int v = 0; v < vertexPoints.length; v++) {
            if (Curve.distance(h.apply(vertexPoints[v].clone()), target.vertexPoints[f.vertex(v)]) > TOLERANCE) {
                return false;
            }
        }
        for (int a = 0; a < curves.length; a++) {
            if (Curve.supDistance(curves[a].map(h), target.curves[f.arrow(a)]) > TOLERANCE) {
                return false;
            }
        }
        return true;
    }
}
