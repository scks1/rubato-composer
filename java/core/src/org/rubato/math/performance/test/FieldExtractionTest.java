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
package org.rubato.math.performance.test;

import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

import org.rubato.math.performance.FieldExtraction;
import org.rubato.math.performance.FieldExtraction.Case;
import org.rubato.math.performance.TempoCurve;
import org.rubato.math.performance.TempoCurves;

/**
 * Field extraction (Mazzola, <i>The Topos of Music II</i>, &sect;47.3.4 pp. 762-763): the
 * field Z(X) = U V<sup>-1</sup> Delta = det(V)<sup>-1</sup> U Adj(V) Delta in its three cases,
 * the basis chosen by w_loc |det U| + w_orth / |det U<sup>Norm</sup>|, and the constructed
 * basis vectors of sources/FIELD-EXTRACTION-SPEC.md D5 and D6.
 */
public class FieldExtractionTest extends TestCase {

    private static final double TOL = 1e-12;

    private static void assertVector(String what, double[] expected, double[] actual, double tol) {
        assertNotNull(what + ": no vector", actual);
        assertEquals(what + ": length", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(what + "[" + i + "] of " + Arrays.toString(actual), expected[i], actual[i], tol);
        }
    }

    public void testDeterminantAndAdjugate() {
        double[][] v = { { 2, 2 }, { 1, 3 } };
        assertEquals(4.0, FieldExtraction.det(v), TOL);
        double[][] adj = FieldExtraction.adjugate(v);
        assertVector("adj row 0", new double[] { 3, -2 }, adj[0], TOL);
        assertVector("adj row 1", new double[] { -1, 2 }, adj[1], TOL);
        double[][] m3 = { { 1, 2, 0 }, { 0, 1, 4 }, { 5, 6, 0 } };
        assertEquals(16.0, FieldExtraction.det(m3), 1e-12);
        Random r = new Random(4703);
        for (int trial = 0; trial < 20; trial++) {
            int n = 1 + r.nextInt(5);
            double[][] m = new double[n][n];
            for (double[] row : m) {
                for (int j = 0; j < n; j++) {
                    row[j] = r.nextGaussian();
                }
            }
            double d = FieldExtraction.det(m);
            double[][] a = FieldExtraction.adjugate(m);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double s = 0;
                    for (int k = 0; k < n; k++) {
                        s += m[i][k] * a[k][j];
                    }
                    assertEquals("M Adj(M) = det I at " + i + "," + j + " (n = " + n + ")", i == j ? d : 0, s, 1e-9);
                }
            }
        }
    }

    /** Spec section 6: A = [[2, 0], [1, 2]] gives A^-1 Delta = (0.5, 0.25) for both bases (F1). */
    public void testAffineFieldIsTheSameForEveryBasis() {
        double[][] a = { { 2, 0 }, { 1, 2 } };
        double[][][] bases = { { { 1, 0 }, { 0, 1 } }, { { 1, 1 }, { 0, 1 } } };
        for (double[][] u : bases) {
            double[][] v = multiply(a, u);
            FieldExtraction.Local z = FieldExtraction.field(u, v, 1e-9);
            assertEquals(Case.REGULAR, z.getCase());
            assertVector("A^-1 Delta", new double[] { 0.5, 0.25 }, z.value(), TOL);
        }
    }

    /** F1 on whole compositions: an affine performance of random notes gives A^-1 Delta at every note. */
    public void testAffinePerformanceGivesTheInverseAtEveryNote() {
        Random r = new Random(762);
        int n = 4;
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = (i == j ? 1.5 : 0) + 0.3 * r.nextGaussian();
            }
        }
        double[] b = { 3, -1, 0.5, 2 };
        double[][] score = new double[30][n];
        double[][] performed = new double[30][];
        for (int k = 0; k < score.length; k++) {
            for (int j = 0; j < n; j++) {
                score[k][j] = 10 * r.nextDouble();
            }
            performed[k] = add(apply(a, score[k]), b);
        }
        double[] expected = solve(a, ones(n));
        FieldExtraction.Result res = FieldExtraction.extract(score, performed, FieldExtraction.Settings.defaults());
        assertEquals(30, res.vectors().size());
        for (FieldExtraction.Vector v : res.vectors()) {
            assertEquals("note " + v.index, Case.REGULAR, v.getCase());
            assertEquals("note " + v.index + " constructed", 0, v.basis().constructed());
            assertVector("note " + v.index, expected, v.value(), 1e-9);
        }
        assertEquals(30, res.count(Case.REGULAR));
    }

    /** F2: a performance that changes nothing gives the diagonal Delta (Example 1, p. 764). */
    public void testIdentityGivesTheDiagonal() {
        double[][] score = { { 0, 60, 80, 1 }, { 1, 62, 70, 1 }, { 2, 64, 90, 0.5 }, { 2.5, 65, 60, 0.5 },
                             { 3, 67, 85, 2 }, { 5, 60, 75, 1 } };
        FieldExtraction.Result res = FieldExtraction.extract(score, score, FieldExtraction.Settings.defaults());
        for (FieldExtraction.Vector v : res.vectors()) {
            assertVector("note " + v.index, ones(4), v.value(), 1e-12);
        }
    }

    /** Case 2 of p. 762: det V = 0, Adj V != 0, only the direction (2, -1)/sqrt 5 (spec section 6, D8). */
    public void testDirectionOnly() {
        double[][] u = { { 1, 0 }, { 0, 1 } };
        double[][] v = { { 1, 2 }, { 2, 4 } };
        FieldExtraction.Local z = FieldExtraction.field(u, v, 1e-9);
        assertEquals(Case.DIRECTION, z.getCase());
        double s = Math.sqrt(5);
        assertVector("direction", new double[] { 2 / s, -1 / s }, z.value(), TOL);
    }

    /** Case 3 of p. 762: det V = 0 and Adj V = 0, no information. */
    public void testNoInformation() {
        double[][] u2 = { { 1, 0 }, { 0, 1 } };
        assertEquals(Case.NONE, FieldExtraction.field(u2, new double[2][2], 1e-9).getCase());
        assertNull(FieldExtraction.field(u2, new double[2][2], 1e-9).value());
        double[][] u3 = { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
        double[][] rank1 = { { 1, 2, 3 }, { 2, 4, 6 }, { -1, -2, -3 } };
        assertEquals(Case.NONE, FieldExtraction.field(u3, rank1, 1e-9).getCase());
    }

    /** D8: Adj V != 0 but Adj V Delta = 0 leaves no direction. */
    public void testZeroDirectionCountsAsNone() {
        double[][] u = { { 1, 0 }, { 0, 1 } };
        double[][] v = { { 1, 1 }, { 1, 1 } };
        assertEquals(Case.NONE, FieldExtraction.field(u, v, 1e-9).getCase());
    }

    /** The regular case equals det(V)^-1 U Adj(V) Delta (p. 762). */
    public void testRegularCaseEqualsTheAdjugateForm() {
        Random r = new Random(4734);
        for (int trial = 0; trial < 30; trial++) {
            int n = 1 + r.nextInt(4);
            double[][] u = new double[n][n], v = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    u[i][j] = r.nextGaussian() + (i == j ? 2 : 0);
                    v[i][j] = r.nextGaussian() + (i == j ? 2 : 0);
                }
            }
            FieldExtraction.Local z = FieldExtraction.field(u, v, 1e-9);
            assertEquals(Case.REGULAR, z.getCase());
            double[] expected = apply(u, apply(FieldExtraction.adjugate(v), ones(n)));
            double d = FieldExtraction.det(v);
            for (int i = 0; i < n; i++) {
                expected[i] /= d;
            }
            assertVector("trial " + trial, expected, z.value(), 1e-8);
        }
    }

    /** Spec section 6: with weights 1 and 1 the basis (a, b) costs 2 and wins; locality alone picks (a, d). */
    public void testBasisFollowsTheCost() {
        double[][] pts = { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 }, { 2, 0.1 } };
        FieldExtraction.Basis b = FieldExtraction.basis(pts, 0, FieldExtraction.Settings.defaults());
        assertEquals(2.0, b.cost(), 1e-12);
        assertEquals(0, b.constructed());
        int[] nb = b.neighbors().clone();
        Arrays.sort(nb);
        assertTrue(Arrays.toString(nb), Arrays.equals(new int[] { 1, 2 }, nb));
        FieldExtraction.Basis loc = FieldExtraction.basis(pts, 0,
                FieldExtraction.Settings.defaults().withWeights(1, 1e-9));
        int[] nl = loc.neighbors().clone();
        Arrays.sort(nl);
        assertTrue(Arrays.toString(nl), Arrays.equals(new int[] { 1, 4 }, nl));
        assertEquals(0.1, loc.cost(), 1e-6);
        FieldExtraction.Basis orth = FieldExtraction.basis(pts, 0,
                FieldExtraction.Settings.defaults().withWeights(1e-9, 1));
        int[] no = orth.neighbors().clone();
        Arrays.sort(no);
        assertTrue(Arrays.toString(no), Arrays.equals(new int[] { 1, 2 }, no));
    }

    /** The basis columns are the difference vectors N - X (p. 763). */
    public void testBasisColumnsAreDifferenceVectors() {
        double[][] pts = { { 1, 1 }, { 2, 1 }, { 1, 2 } };
        FieldExtraction.Basis b = FieldExtraction.basis(pts, 0, FieldExtraction.Settings.defaults());
        double[][] u = b.matrix();
        for (int k = 0; k < 2; k++) {
            int j = b.neighbors()[k];
            assertEquals(pts[j][0] - 1, u[0][k], TOL);
            assertEquals(pts[j][1] - 1, u[1][k], TOL);
        }
    }

    /** Locality (p. 762): maxNeighbors and maxDist keep far notes out of the basis. */
    public void testNeighbourhoodLimits() {
        double[][] pts = { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 0.6, 0.6 }, { 50, 50 }, { 49, 51 } };
        FieldExtraction.Basis b = FieldExtraction.basis(pts, 0, FieldExtraction.Settings.defaults());
        for (int j : b.neighbors()) {
            assertTrue("far note " + j, j >= 1 && j <= 3);
        }
        FieldExtraction.Basis two = FieldExtraction.basis(pts, 0,
                FieldExtraction.Settings.defaults().withMaxNeighbors(2));
        int[] n2 = two.neighbors().clone();
        Arrays.sort(n2);
        assertTrue("the two nearest: " + Arrays.toString(n2), Arrays.equals(new int[] { 1, 3 }, n2));
        FieldExtraction.Basis near = FieldExtraction.basis(pts, 0,
                FieldExtraction.Settings.defaults().withMaxDist(0.9));
        assertEquals("only one note within 0.9, so one constructed column", 1, near.constructed());
    }

    /**
     * An isolated chord (p. 763): EH points (0, 60), (0, 64), (0, 67) give no regular basis.
     * D5 keeps (0, 4), drops the parallel (0, 7) and constructs (1, 0); D6 maps it to itself.
     * Performed onsets 0, 0.1, 0.2: V = [[0.1, 1], [4, 0]], so Z = (0.975, 1).
     */
    public void testIsolatedChordConstructsAnOnsetVector() {
        double[][] score = { { 0, 60 }, { 0, 64 }, { 0, 67 } };
        double[][] performed = { { 0, 60 }, { 0.1, 64 }, { 0.2, 67 } };
        FieldExtraction.Result res = FieldExtraction.extract(score, performed, FieldExtraction.Settings.defaults());
        FieldExtraction.Vector v = res.vectors().get(0);
        assertEquals(1, v.basis().constructed());
        assertEquals(1, v.basis().neighbors()[0]);
        assertEquals(-1, v.basis().neighbors()[1]);
        double[][] u = v.basis().matrix();
        assertEquals(1.0, u[0][1], TOL);
        assertEquals(0.0, u[1][1], TOL);
        assertEquals(Case.REGULAR, v.getCase());
        assertVector("chord note 0", new double[] { 0.975, 1 }, v.value(), 1e-12);
        assertEquals(3, res.constructedNotes());
    }

    /** A single note: no neighbours, a whole constructed basis, so the identity's diagonal (D6). */
    public void testSingleNote() {
        FieldExtraction.Result res = FieldExtraction.extract(new double[][] { { 1, 2 } }, new double[][] { { 7, 9 } },
                                                             FieldExtraction.Settings.defaults());
        assertEquals(2, res.vectors().get(0).basis().constructed());
        assertVector("single", ones(2), res.vectors().get(0).value(), TOL);
    }

    /**
     * F3 with eq. (33.9): the linear tempo T(E) = 2 - 0.1 E performed on ED by
     * (f(E), f(E + D) - f(E)) has the field (T(E), 2 T(E + D) - T(E)). Extraction on a grid
     * of spacing h errs by O(h): halving h at least nearly halves the largest error.
     */
    public void testTempoArticulationFieldConverges() {
        TempoCurve t = TempoCurves.linear(2, -0.1);
        double e1 = errorOnGrid(t, 0.5), e2 = errorOnGrid(t, 0.25), e3 = errorOnGrid(t, 0.125);
        assertTrue("error at h = 0.5: " + e1, e1 < 0.2);
        assertTrue("O(h): " + e1 + " -> " + e2, e2 < 0.6 * e1);
        assertTrue("O(h): " + e2 + " -> " + e3, e3 < 0.6 * e2);
    }

    private static double errorOnGrid(TempoCurve t, double h) {
        int ne = (int) Math.round(4 / h) + 1, nd = (int) Math.round(2 / h) + 1;
        double[][] score = new double[ne * nd][], performed = new double[ne * nd][];
        int k = 0;
        for (int i = 0; i < ne; i++) {
            for (int j = 0; j < nd; j++) {
                double e = 2 + i * h, d = 0.5 + j * h;
                score[k] = new double[] { e, d };
                double fe = t.integral(0, e), fs = t.integral(0, e + d);
                performed[k] = new double[] { fe, fs - fe };
                k++;
            }
        }
        FieldExtraction.Result res = FieldExtraction.extract(score, performed, FieldExtraction.Settings.defaults());
        double worst = 0;
        for (FieldExtraction.Vector v : res.vectors()) {
            double[] x = score[v.index];
            assertEquals(Case.REGULAR, v.getCase());
            double ze = t.tempo(x[0]), zd = 2 * t.tempo(x[0] + x[1]) - t.tempo(x[0]);
            worst = Math.max(worst, Math.max(Math.abs(v.value()[0] - ze), Math.abs(v.value()[1] - zd)));
        }
        return worst;
    }

    public void testRefusals() {
        refuse(new double[][] { { 0, 1 } }, new double[][] { { 0, 1 }, { 1, 1 } }, "same number");
        refuse(new double[][] { { 0, 1 } }, new double[][] { { 0 } }, "dimension");
        refuse(new double[0][], new double[0][], "no notes");
        refuse(new double[][] { { 0, Double.NaN } }, new double[][] { { 0, 1 } }, "finite");
        try {
            FieldExtraction.Settings.defaults().withWeights(0, 1);
            fail("zero weight");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("positive"));
        }
        try {
            FieldExtraction.Settings.defaults().withMaxNeighbors(0);
            fail("no neighbours");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("at least 1"));
        }
    }

    private static void refuse(double[][] s, double[][] p, String word) {
        try {
            FieldExtraction.extract(s, p, FieldExtraction.Settings.defaults());
            fail(word);
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(word));
        }
    }

    /** D2: the trivial matching pairs notes per pitch in onset order, so an arpeggio still matches. */
    public void testMatchByPitch() {
        // E, H, L, D
        double[][] score = { { 0, 60, 80, 1 }, { 0, 64, 80, 1 }, { 0, 67, 80, 1 }, { 1, 60, 80, 1 } };
        double[][] performed = { { 0.3, 67, 70, 1 }, { 0.0, 60, 70, 1 }, { 1.1, 60, 70, 1 }, { 0.15, 64, 70, 1 } };
        int[] m = FieldExtraction.matchByPitch(score, performed, 1, 0, 3);
        assertTrue(Arrays.toString(m), Arrays.equals(new int[] { 1, 3, 0, 2 }, m));
        double[][] missing = { { 0.3, 67, 70, 1 }, { 0.0, 60, 70, 1 }, { 0.15, 64, 70, 1 } };
        try {
            FieldExtraction.matchByPitch(score, missing, 1, 0, 3);
            fail("missing note");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("pitch 60"));
        }
    }

    /** Added after mutation M13 survived: performed notes of one pitch listed against onset order. */
    public void testMatchFollowsOnsetOrderWithinAPitch() {
        double[][] score = { { 0, 60, 80, 1 }, { 1, 60, 80, 1 }, { 2, 60, 80, 1 } };
        double[][] performed = { { 2.2, 60, 70, 1 }, { 0.1, 60, 70, 1 }, { 1.05, 60, 70, 1 } };
        int[] m = FieldExtraction.matchByPitch(score, performed, 1, 0, 3);
        assertTrue(Arrays.toString(m), Arrays.equals(new int[] { 1, 2, 0 }, m));
    }

    /**
     * Added after mutation M4 survived: D7's tolerance is relative, so a regular image at a
     * small scale (V = 1e-6 A, det 4e-12) is still regular, with Z = 1e6 A^-1 Delta.
     */
    public void testRegularAtSmallScale() {
        double[][] u = { { 1, 0 }, { 0, 1 } };
        double[][] v = { { 2e-6, 0 }, { 1e-6, 2e-6 } };
        FieldExtraction.Local l = FieldExtraction.field(u, v, 1e-9);
        assertEquals(FieldExtraction.Case.REGULAR, l.getCase());
        assertEquals(0.5e6, l.value()[0], 1e-3);
        assertEquals(0.25e6, l.value()[1], 1e-3);
    }

    /**
     * Added after mutation M9 survived: at X = (0, 0) with neighbours (1, 0), (0, 1), (-1, 0),
     * all at distance 1, the candidates {1, 2} and {2, 3} both cost 2; the first in candidate
     * order wins (D4).
     */
    public void testFirstCandidateWinsTies() {
        double[][] pts = { { 0, 0 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
        FieldExtraction.Basis b = FieldExtraction.basis(pts, 0, FieldExtraction.Settings.defaults());
        assertTrue(Arrays.toString(b.neighbors()), Arrays.equals(new int[] { 1, 2 }, b.neighbors()));
        assertEquals(2.0, b.cost(), 1e-12);
    }

    private static double[][] multiply(double[][] a, double[][] b) {
        int n = a.length;
        double[][] c = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    c[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return c;
    }

    private static double[] apply(double[][] a, double[] x) {
        double[] y = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < x.length; j++) {
                y[i] += a[i][j] * x[j];
            }
        }
        return y;
    }

    private static double[] add(double[] a, double[] b) {
        double[] c = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = a[i] + b[i];
        }
        return c;
    }

    private static double[] ones(int n) {
        double[] d = new double[n];
        Arrays.fill(d, 1);
        return d;
    }

    /** Gaussian elimination, independent of FieldExtraction. */
    private static double[] solve(double[][] a0, double[] b0) {
        int n = a0.length;
        double[][] a = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a0[i], 0, a[i], 0, n);
            a[i][n] = b0[i];
        }
        for (int c = 0; c < n; c++) {
            int p = c;
            for (int r = c + 1; r < n; r++) {
                if (Math.abs(a[r][c]) > Math.abs(a[p][c])) {
                    p = r;
                }
            }
            double[] tmp = a[c];
            a[c] = a[p];
            a[p] = tmp;
            for (int r = 0; r < n; r++) {
                if (r != c) {
                    double f = a[r][c] / a[c][c];
                    for (int k = c; k <= n; k++) {
                        a[r][k] -= f * a[c][k];
                    }
                }
            }
        }
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = a[i][n] / a[i][i];
        }
        return x;
    }
}
