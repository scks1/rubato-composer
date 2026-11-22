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

package org.rubato.math.global.test;

import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

import org.rubato.math.global.IntMatrices;
import org.rubato.math.global.IntMatrices.Smith;

/** Smith normal form and linear congruences, the arithmetic behind Def. 23 morphisms. */
public class IntMatricesTest extends TestCase {

    public void testSmithFormOfKnownMatrix() {
        long[][] a = { { 2, 4, 4 }, { -6, 6, 12 }, { 10, -4, -16 } };
        Smith s = IntMatrices.smith(a);
        assertTrue(Arrays.toString(s.diagonal), Arrays.equals(new long[] { 2, 6, 12 }, s.diagonal));
        checkFactorization(a, s);
    }

    public void testSmithFormOfRandomMatrices() {
        Random r = new Random(20261122L);
        for (int trial = 0; trial < 200; trial++) {
            int rows = 1 + r.nextInt(5), cols = 1 + r.nextInt(5);
            long[][] a = new long[rows][cols];
            for (long[] row : a) {
                for (int j = 0; j < cols; j++) {
                    row[j] = r.nextInt(25) - 12;
                }
            }
            Smith s = IntMatrices.smith(a);
            checkFactorization(a, s);
            long[] d = s.diagonal;
            for (int i = 0; i + 1 < d.length; i++) {
                if (d[i + 1] != 0) {
                    assertEquals("divisibility", 0, d[i + 1] % d[i]);
                }
                else {
                    for (int j = i + 1; j < d.length; j++) assertEquals(0, d[j]);
                }
            }
        }
    }

    static void checkFactorization(long[][] a, Smith s) {
        long[][] uav = IntMatrices.multiply(IntMatrices.multiply(s.u, a), s.v);
        for (int i = 0; i < uav.length; i++) {
            for (int j = 0; j < uav[i].length; j++) {
                assertEquals(i == j ? s.d[i][j] : 0, uav[i][j]);
                assertEquals(s.d[i][j], uav[i][j]);
            }
        }
    }

    public void testSolveMod() {
        // 4x = 3 mod 12 has no solution; 4x = 8 mod 12 has x = 2
        assertNull(IntMatrices.solveMod(new long[][] { { 4 } }, new long[] { 3 }, 12));
        long[] x = IntMatrices.solveMod(new long[][] { { 4 } }, new long[] { 8 }, 12);
        assertNotNull(x);
        assertEquals(8, Math.floorMod(4 * x[0], 12));
        // a 2x2 system mod 12 with a singular matrix (det 14 = 2) and solution (1, 2)
        long[][] a = { { 3, 1 }, { 1, 5 } };
        long[] y = { 5, 11 };
        // (7, 2) has no solution
        assertNull(IntMatrices.solveMod(a, new long[] { 7, 2 }, 12));
        long[] z = IntMatrices.solveMod(a, y, 12);
        assertNotNull(z);
        for (int i = 0; i < 2; i++) {
            assertEquals(y[i], Math.floorMod(a[i][0] * z[0] + a[i][1] * z[1], 12));
        }
    }

    public void testSolveModAgreesWithBruteForce() {
        Random r = new Random(7L);
        for (int trial = 0; trial < 300; trial++) {
            int n = 2 + r.nextInt(11);
            long[][] a = new long[2][2];
            long[] y = new long[2];
            for (int i = 0; i < 2; i++) {
                y[i] = r.nextInt(n);
                for (int j = 0; j < 2; j++) a[i][j] = r.nextInt(n);
            }
            boolean exists = false;
            for (int u = 0; u < n && !exists; u++) {
                for (int v = 0; v < n && !exists; v++) {
                    exists = Math.floorMod(a[0][0] * u + a[0][1] * v - y[0], n) == 0
                          && Math.floorMod(a[1][0] * u + a[1][1] * v - y[1], n) == 0;
                }
            }
            long[] z = IntMatrices.solveMod(a, y, n);
            assertEquals("n = " + n + " a = " + Arrays.deepToString(a) + " y = " + Arrays.toString(y), exists, z != null);
            if (z != null) {
                for (int i = 0; i < 2; i++) {
                    assertEquals(0, Math.floorMod(a[i][0] * z[0] + a[i][1] * z[1] - y[i], n));
                }
            }
        }
    }
}
