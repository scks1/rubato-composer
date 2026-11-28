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

package org.rubato.math.topos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rubato.math.global.IntMatrices;

/**
 * A bounded chain complex of finitely generated free abelian groups
 * C_0 &lt;- C_1 &lt;- ... &lt;- C_top, given by integer boundary matrices, and
 * its integral homology from Smith normal forms
 * ({@link IntMatrices#smith(long[][])}). Unlike
 * {@link org.rubato.math.global.SimplicialComplex} it takes any matrices,
 * so a cell may meet the same face more than once, as the cells of a
 * semisimplicial set do (arXiv 2611.32655, section 4.1).
 */
public final class ChainComplex {

    /** H_k as Z^rank plus the cyclic torsion summands Z/t. */
    public static final class Homology {
        public final int rank;
        public final List<Long> torsion;

        Homology(int rank, List<Long> torsion) {
            this.rank = rank;
            this.torsion = Collections.unmodifiableList(torsion);
        }

        /** "0", "Z", "Z^2", "Z/2", "Z + Z/2": the format of SimplicialComplex.HomologyGroup. */
        @Override
        public String toString() {
            if (rank == 0 && torsion.isEmpty()) {
                return "0";
            }
            StringBuilder b = new StringBuilder();
            if (rank > 0) {
                b.append(rank == 1 ? "Z" : "Z^" + rank);
            }
            for (long t : torsion) {
                if (b.length() > 0) {
                    b.append(" + ");
                }
                b.append("Z/").append(t);
            }
            return b.toString();
        }
    }

    private final int[] ranks;
    private final long[][][] boundaries;

    /**
     * A chain complex from its ranks and boundary matrices.
     *
     * @param ranks the rank of C_k for k = 0 .. top
     * @param boundaries boundaries[k] is the matrix of C_k -&gt; C_{k-1} with
     *        ranks[k-1] rows and ranks[k] columns, for k = 1 .. top;
     *        boundaries[0] is ignored
     * @throws IllegalArgumentException if a matrix has the wrong shape or a
     *         composite of two boundaries is not zero
     */
    public ChainComplex(int[] ranks, long[][][] boundaries) {
        this.ranks = ranks.clone();
        if (boundaries.length != ranks.length) {
            throw new IllegalArgumentException("one boundary per degree is needed");
        }
        this.boundaries = new long[ranks.length][][];
        for (int k = 1; k < ranks.length; k++) {
            long[][] m = boundaries[k];
            if (m.length != ranks[k - 1]) {
                throw new IllegalArgumentException("boundary " + k + " has " + m.length + " rows");
            }
            this.boundaries[k] = new long[m.length][];
            for (int r = 0; r < m.length; r++) {
                if (m[r].length != ranks[k]) {
                    throw new IllegalArgumentException("boundary " + k + " has a row of the wrong length");
                }
                this.boundaries[k][r] = m[r].clone();
            }
        }
        for (int k = 2; k < ranks.length; k++) {
            for (int r = 0; r < ranks[k - 2]; r++) {
                for (int c = 0; c < ranks[k]; c++) {
                    long s = 0;
                    for (int j = 0; j < ranks[k - 1]; j++) {
                        s += this.boundaries[k - 1][r][j] * this.boundaries[k][j][c];
                    }
                    if (s != 0) {
                        throw new IllegalArgumentException("the boundary does not square to zero in degree " + k);
                    }
                }
            }
        }
    }

    /** The top degree. */
    public int top() {
        return ranks.length - 1;
    }

    public int rank(int k) {
        return ranks[k];
    }

    /** H_0 ... H_top: rank C_k - rank d_k - rank d_{k+1}, with the invariant factors above 1 of d_{k+1} as torsion. */
    public List<Homology> homology() {
        int n = ranks.length;
        long[][] diag = new long[n + 1][];
        diag[0] = new long[0];
        diag[n] = new long[0];
        for (int k = 1; k < n; k++) {
            diag[k] = IntMatrices.smith(boundaries[k]).diagonal;
        }
        List<Homology> out = new ArrayList<Homology>();
        for (int k = 0; k < n; k++) {
            List<Long> torsion = new ArrayList<Long>();
            for (long d : diag[k + 1]) {
                if (d > 1) {
                    torsion.add(d);
                }
            }
            out.add(new Homology(ranks[k] - diag[k].length - diag[k + 1].length, torsion));
        }
        return out;
    }

    /** "[Z, Z^2, Z]": the list format of SimplicialComplex.homology(). */
    public static String format(List<Homology> h) {
        return h.toString();
    }
}
