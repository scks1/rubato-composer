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

package org.rubato.math.global;

/**
 * Integer Smith normal form and linear congruences, for the small
 * matrices that nerves and chart maps produce.
 * <p>
 * {@link #smith(long[][])} computes unimodular U, V with U&middot;A&middot;V = D,
 * D diagonal with d<sub>1</sub> | d<sub>2</sub> | &hellip;. Homology uses the
 * diagonal; {@link #solveMod(long[][], long[], int)} uses U and V to decide
 * whether A&middot;z &equiv; y has a solution modulo n and to return one.
 * The entries stay small for the complexes in this package; the code
 * checks for overflow and throws {@link ArithmeticException} instead of
 * returning a wrong answer.
 */
public final class IntMatrices {

    private IntMatrices() { /* static utility */ }

    /** The result of a Smith normal form computation: U A V = D. */
    public static final class Smith {
        public final long[][] u;
        public final long[][] d;
        public final long[][] v;
        /** The non-zero diagonal entries d_1 | d_2 | ..., all positive. */
        public final long[] diagonal;

        Smith(long[][] u, long[][] d, long[][] v, long[] diagonal) {
            this.u = u;
            this.d = d;
            this.v = v;
            this.diagonal = diagonal;
        }

        /** The rank of A, the number of non-zero diagonal entries. */
        public int rank() {
            return diagonal.length;
        }
    }

    /**
     * Computes the Smith normal form of an integer matrix with r rows and c
     * columns. A matrix with no rows or no columns has rank 0.
     */
    public static Smith smith(long[][] a) {
        int r = a.length;
        int c = r == 0 ? 0 : a[0].length;
        long[][] d = new long[r][];
        for (int i = 0; i < r; i++) {
            if (a[i].length != c) {
                throw new IllegalArgumentException("Row " + i + " has " + a[i].length + " entries, expected " + c);
            }
            d[i] = a[i].clone();
        }
        long[][] u = identity(r);
        long[][] v = identity(c);
        int t = 0;
        while (t < r && t < c) {
            // choose the entry of least absolute value in the remaining block
            int pi = -1, pj = -1;
            long best = 0;
            for (int i = t; i < r; i++) {
                for (int j = t; j < c; j++) {
                    long x = Math.abs(d[i][j]);
                    if (x != 0 && (pi < 0 || x < best)) {
                        best = x; pi = i; pj = j;
                    }
                }
            }
            if (pi < 0) {
                break;
            }
            swapRows(d, t, pi); swapRows(u, t, pi);
            swapCols(d, t, pj); swapCols(v, t, pj);
            boolean clean = true;
            for (int i = t + 1; i < r; i++) {
                long q = Math.floorDiv(d[i][t], d[t][t]);
                if (q != 0) {
                    addRow(d, i, t, -q); addRow(u, i, t, -q);
                }
                if (d[i][t] != 0) {
                    clean = false;
                }
            }
            for (int j = t + 1; j < c; j++) {
                long q = Math.floorDiv(d[t][j], d[t][t]);
                if (q != 0) {
                    addCol(d, j, t, -q); addCol(v, j, t, -q);
                }
                if (d[t][j] != 0) {
                    clean = false;
                }
            }
            if (!clean) {
                continue;
            }
            // divisibility: d[t][t] must divide every remaining entry
            int bi = -1;
            for (int i = t + 1; i < r && bi < 0; i++) {
                for (int j = t + 1; j < c; j++) {
                    if (d[i][j] % d[t][t] != 0) {
                        bi = i;
                        break;
                    }
                }
            }
            if (bi >= 0) {
                addRow(d, t, bi, 1); addRow(u, t, bi, 1);
                continue;
            }
            if (d[t][t] < 0) {
                negateRow(d, t); negateRow(u, t);
            }
            t++;
        }
        long[] diag = new long[t];
        for (int i = 0; i < t; i++) {
            diag[i] = d[i][i];
        }
        return new Smith(u, d, v, diag);
    }

    /**
     * Solves A&middot;z &equiv; y (mod n) for an integer vector z, or over the
     * integers if n is 0. Returns a solution with entries in [0, n) (any
     * integer solution if n is 0), or null if there is none.
     */
    public static long[] solveMod(long[][] a, long[] y, int n) {
        int r = a.length;
        if (y.length != r) {
            throw new IllegalArgumentException("Right-hand side has " + y.length + " entries for " + r + " rows");
        }
        if (n < 0) {
            throw new IllegalArgumentException("Modulus must not be negative, got " + n);
        }
        int c = r == 0 ? 0 : a[0].length;
        if (r == 0) {
            return new long[0];
        }
        // [A | n I] w = y over Z
        int cc = n == 0 ? c : c + r;
        long[][] ext = new long[r][cc];
        for (int i = 0; i < r; i++) {
            System.arraycopy(a[i], 0, ext[i], 0, c);
            if (n != 0) {
                ext[i][c + i] = n;
            }
        }
        Smith s = smith(ext);
        long[] uy = multiply(s.u, y);
        long[] t = new long[cc];
        for (int i = 0; i < r; i++) {
            if (i < s.rank()) {
                if (uy[i] % s.diagonal[i] != 0) {
                    return null;
                }
                t[i] = uy[i] / s.diagonal[i];
            }
            else if (uy[i] != 0) {
                return null;
            }
        }
        long[] w = multiply(s.v, t);
        long[] z = new long[c];
        for (int j = 0; j < c; j++) {
            z[j] = n == 0 ? w[j] : Math.floorMod(w[j], (long) n);
        }
        return z;
    }

    /** M&middot;x for a matrix M and a vector x. */
    public static long[] multiply(long[][] m, long[] x) {
        long[] out = new long[m.length];
        for (int i = 0; i < m.length; i++) {
            long s = 0;
            for (int j = 0; j < x.length; j++) {
                s = Math.addExact(s, Math.multiplyExact(m[i][j], x[j]));
            }
            out[i] = s;
        }
        return out;
    }

    /** The product M&middot;N of two matrices. */
    public static long[][] multiply(long[][] m, long[][] nn) {
        int r = m.length;
        int k = nn.length;
        int c = k == 0 ? 0 : nn[0].length;
        long[][] out = new long[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                long s = 0;
                for (int l = 0; l < k; l++) {
                    s = Math.addExact(s, Math.multiplyExact(m[i][l], nn[l][j]));
                }
                out[i][j] = s;
            }
        }
        return out;
    }

    static long[][] identity(int n) {
        long[][] id = new long[n][n];
        for (int i = 0; i < n; i++) {
            id[i][i] = 1;
        }
        return id;
    }

    private static void swapRows(long[][] m, int i, int j) {
        if (i != j) {
            long[] tmp = m[i]; m[i] = m[j]; m[j] = tmp;
        }
    }

    private static void swapCols(long[][] m, int i, int j) {
        if (i != j) {
            for (long[] row : m) {
                long tmp = row[i]; row[i] = row[j]; row[j] = tmp;
            }
        }
    }

    /** row i += q * row j */
    private static void addRow(long[][] m, int i, int j, long q) {
        for (int k = 0; k < m[i].length; k++) {
            m[i][k] = Math.addExact(m[i][k], Math.multiplyExact(q, m[j][k]));
        }
    }

    /** column i += q * column j */
    private static void addCol(long[][] m, int i, int j, long q) {
        for (long[] row : m) {
            row[i] = Math.addExact(row[i], Math.multiplyExact(q, row[j]));
        }
    }

    private static void negateRow(long[][] m, int i) {
        for (int k = 0; k < m[i].length; k++) {
            m[i][k] = -m[i][k];
        }
    }
}
