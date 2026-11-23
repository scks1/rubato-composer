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

import java.util.Arrays;

/**
 * A hypergesture with skeletons Gamma (outer) and Delta (inner) in R^n: a
 * Gamma-gesture in the space of Delta-gestures in R^n (Mazzola, The Topos
 * of Music, vol. III, section 61.6, pp. 915-918). It is stored as its
 * values on a grid. A cell of a digraph is a vertex v (cell index v) or
 * an arrow a (cell index |V| + a); an outer arrow is sampled at ns + 1
 * equally spaced parameters s, an inner arrow at nt + 1 parameters t, and
 * values between grid nodes are bilinear (departure D1).
 */
public final class Hypergesture {

    /** Values of a hypergesture body at (outer cell, s, inner cell, t). */
    public interface Body {
        double[] at(int outerCell, double s, int innerCell, double t);
    }

    private final Digraph outer;
    private final Digraph inner;
    private final int ns;
    private final int nt;
    private final int dimension;
    /** grid[outer cell][inner cell][i][j][coordinate] */
    private final double[][][][][] grid;

    /** Samples the body on the grid; whether the faces glue is reported by {@link #endpointGap()}. */
    public Hypergesture(Digraph outer, Digraph inner, int ns, int nt, int dimension, Body body) {
        if (ns < 1 || nt < 1) {
            throw new IllegalArgumentException("need at least one grid interval");
        }
        this.outer = outer;
        this.inner = inner;
        this.ns = ns;
        this.nt = nt;
        this.dimension = dimension;
        int oc = outer.vertexCount() + outer.arrowCount();
        int ic = inner.vertexCount() + inner.arrowCount();
        grid = new double[oc][ic][][][];
        for (int x = 0; x < oc; x++) {
            int rows = x < outer.vertexCount() ? 1 : ns + 1;
            for (int y = 0; y < ic; y++) {
                int cols = y < inner.vertexCount() ? 1 : nt + 1;
                grid[x][y] = new double[rows][cols][];
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        double[] p = body.at(x, rows == 1 ? 0 : (double) i / ns, y, cols == 1 ? 0 : (double) j / nt);
                        if (p.length != dimension) {
                            throw new IllegalArgumentException("body returned dimension " + p.length);
                        }
                        grid[x][y][i][j] = p.clone();
                    }
                }
            }
        }
    }

    private Hypergesture(Digraph outer, Digraph inner, int ns, int nt, int dimension, double[][][][][] grid) {
        this.outer = outer;
        this.inner = inner;
        this.ns = ns;
        this.nt = nt;
        this.dimension = dimension;
        this.grid = grid;
    }

    /**
     * The first-species hypergesture of two equally long melodies in the
     * plane (position, pitch): the outer skeleton is the up-arrow through
     * the n notes, the inner one a single arrow from the cantus firmus
     * tone to the discantus tone, and all values are interpolated linearly
     * (vol. III, section 61.14.1, pp. 931-933, read in R^2 with an arrow
     * for each interval; the book's figure works in the pitch-class circle
     * with loops, departure D6).
     */
    public static Hypergesture firstSpecies(int[] cantus, int[] discantus) {
        if (cantus.length != discantus.length || cantus.length == 0) {
            throw new IllegalArgumentException("melodies of lengths " + cantus.length + " and " + discantus.length);
        }
        final int n = cantus.length;
        final double[] c = new double[n], d = new double[n];
        for (int i = 0; i < n; i++) {
            c[i] = cantus[i];
            d[i] = discantus[i];
        }
        Body body = (oc, s, ic, t) -> {
            int k = oc < n ? oc : oc - n;
            double u = oc < n ? 0 : s;
            double x = k + u;
            double cv = oc < n ? c[k] : c[k] + u * (c[k + 1] - c[k]);
            double dv = oc < n ? d[k] : d[k] + u * (d[k + 1] - d[k]);
            double pitch = ic == 0 ? cv : ic == 1 ? dv : cv + t * (dv - cv);
            return new double[] { x, pitch };
        };
        return new Hypergesture(Digraph.arrowN(n - 1), Digraph.arrowN(1), 1, 1, 2, body);
    }

    public Digraph outer() {
        return outer;
    }

    public Digraph inner() {
        return inner;
    }

    public int dimension() {
        return dimension;
    }

    /**
     * The largest mismatch between an arrow's face and the cell at its
     * tail or head, over both skeletons; 0 means the values glue into a
     * hypergesture.
     */
    public double endpointGap() {
        double max = 0;
        int ov = outer.vertexCount(), iv = inner.vertexCount();
        for (int b = 0; b < outer.arrowCount(); b++) {
            for (int y = 0; y < grid[0].length; y++) {
                double[][][] g = grid[ov + b][y];
                for (int j = 0; j < g[0].length; j++) {
                    max = Math.max(max, Curve.distance(g[0][j], grid[outer.tail(b)][y][0][j]));
                    max = Math.max(max, Curve.distance(g[ns][j], grid[outer.head(b)][y][0][j]));
                }
            }
        }
        for (int a = 0; a < inner.arrowCount(); a++) {
            for (int x = 0; x < grid.length; x++) {
                double[][][] g = grid[x][iv + a];
                for (int i = 0; i < g.length; i++) {
                    max = Math.max(max, Curve.distance(g[i][0], grid[x][inner.tail(a)][i][0]));
                    max = Math.max(max, Curve.distance(g[i][nt], grid[x][inner.head(a)][i][0]));
                }
            }
        }
        return max;
    }

    /** The value at (outer cell, s, inner cell, t), bilinear between grid nodes. */
    public double[] at(int outerCell, double s, int innerCell, double t) {
        double[][][] g = grid[outerCell][innerCell];
        double[][] row0, row1;
        double u;
        if (g.length == 1) {
            row0 = row1 = g[0];
            u = 0;
        }
        else {
            double x = Math.max(0, Math.min(1, s)) * ns;
            int i = Math.min((int) Math.floor(x), ns - 1);
            row0 = g[i];
            row1 = g[i + 1];
            u = x - i;
        }
        double[] a = interpolate(row0, t), b = interpolate(row1, t);
        double[] r = new double[dimension];
        for (int k = 0; k < dimension; k++) {
            r[k] = a[k] + u * (b[k] - a[k]);
        }
        return r;
    }

    private double[] interpolate(double[][] row, double t) {
        if (row.length == 1) {
            return row[0];
        }
        double x = Math.max(0, Math.min(1, t)) * nt;
        int j = Math.min((int) Math.floor(x), nt - 1);
        double v = x - j;
        double[] r = new double[dimension];
        for (int k = 0; k < dimension; k++) {
            r[k] = row[j][k] + v * (row[j + 1][k] - row[j][k]);
        }
        return r;
    }

    /** The inner gesture at an outer vertex. */
    public Gesture innerGesture(int outerVertex) {
        return gestureFromRows(grid[outerVertex], 0, 0);
    }

    /** The inner gesture at parameter s of an outer arrow: the outer arrow's curve in the space of inner gestures. */
    public Gesture innerGestureAt(int outerArrow, double s) {
        double x = Math.max(0, Math.min(1, s)) * ns;
        int i = Math.min((int) Math.floor(x), ns - 1);
        return gestureFromRows(grid[outer.vertexCount() + outerArrow], i, x - i);
    }

    private Gesture gestureFromRows(double[][][][] cells, int i, double u) {
        int iv = inner.vertexCount();
        double[][] points = new double[iv][];
        for (int v = 0; v < iv; v++) {
            points[v] = row(cells[v], i, u)[0];
        }
        Curve[] curves = new Curve[inner.arrowCount()];
        for (int a = 0; a < curves.length; a++) {
            curves[a] = PolylineCurve.through(row(cells[iv + a], i, u));
        }
        return new Gesture(inner, points, curves);
    }

    private double[][] row(double[][][] g, int i, double u) {
        if (g.length == 1) {
            return g[0];
        }
        double[][] r = new double[g[i].length][dimension];
        for (int j = 0; j < r.length; j++) {
            for (int k = 0; k < dimension; k++) {
                r[j][k] = g[i][j][k] + u * (g[i + 1][j][k] - g[i][j][k]);
            }
        }
        return r;
    }

    /**
     * The Escher exchange (Prop. 61, p. 916): the same values read as a
     * Delta-gesture of Gamma-gestures.
     */
    public Hypergesture escher() {
        double[][][][][] e = new double[grid[0].length][grid.length][][][];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                double[][][] g = grid[x][y];
                double[][][] t = new double[g[0].length][g.length][];
                for (int i = 0; i < g.length; i++) {
                    for (int j = 0; j < g[i].length; j++) {
                        t[j][i] = g[i][j];
                    }
                }
                e[y][x] = t;
            }
        }
        return new Hypergesture(inner, outer, nt, ns, dimension, e);
    }

    /**
     * The metric of p. 916 applied twice: the sum over outer arrows of the
     * sup over s of the sum over inner arrows of the sup over t. On a
     * bilinear grid both sups are attained at grid nodes (spec section 4).
     */
    public double distance(Hypergesture h) {
        if (!outer.equals(h.outer) || !inner.equals(h.inner) || ns != h.ns || nt != h.nt) {
            throw new IllegalArgumentException("hypergestures of different shapes");
        }
        int ov = outer.vertexCount(), iv = inner.vertexCount();
        double sum = 0;
        for (int b = 0; b < outer.arrowCount(); b++) {
            double supS = 0;
            for (int i = 0; i <= ns; i++) {
                double inner = 0;
                for (int a = 0; a < this.inner.arrowCount(); a++) {
                    double supT = 0;
                    for (int j = 0; j <= nt; j++) {
                        supT = Math.max(supT, Curve.distance(grid[ov + b][iv + a][i][j], h.grid[ov + b][iv + a][i][j]));
                    }
                    inner += supT;
                }
                supS = Math.max(supS, inner);
            }
            sum += supS;
        }
        return sum;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Hypergesture)) {
            return false;
        }
        Hypergesture h = (Hypergesture) o;
        return outer.equals(h.outer) && inner.equals(h.inner) && ns == h.ns && nt == h.nt
               && Arrays.deepEquals(grid, h.grid);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(grid);
    }
}
