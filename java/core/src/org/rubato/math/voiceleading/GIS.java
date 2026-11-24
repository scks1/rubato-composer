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

package org.rubato.math.voiceleading;

import java.util.List;

/**
 * A finite generalized interval system (S, IVLS, int) as Flieder states it after
 * Lewin (arXiv 2512.04090, Example 3.1, p. 23), with a check of its two axioms:
 * <ol>
 * <li>int(r, s) &#8902; int(s, t) = int(r, t) for all r, s, t;</li>
 * <li>for every s and every interval i there is exactly one t with int(s, t) = i.</li>
 * </ol>
 * S is {0, ..., |S|-1}, IVLS is {0, ..., m-1} with the product table star, and
 * int is a |S| by |S| table. The group axioms of IVLS are checked first.
 * {@link #fromSpace} builds the GIS of a voice-leading space whose
 * transporter sets all have one element (sources/VOICE-LEADING-SPEC.md, D8, D9).
 */
public final class GIS {

    private final int m;
    private final int[][] star;
    private final int[][] interval;

    /** A GIS candidate; the tables are copied. */
    public GIS(int m, int[][] star, int[][] interval) {
        this.m = m;
        this.star = copy(star);
        this.interval = copy(interval);
    }

    /** The outcome of {@link #check}: either a GIS, or the first failure with its witness. */
    public static final class Result {
        private final String failure;

        Result(String failure) {
            this.failure = failure;
        }

        public boolean isGIS() {
            return failure.isEmpty();
        }

        /** The first failed condition and its witness, or "" for a GIS. */
        public String failure() {
            return failure;
        }

        @Override
        public String toString() {
            return isGIS() ? "a GIS" : "not a GIS: " + failure;
        }
    }

    /** Checks the group axioms of IVLS, then axioms 1 and 2 of p. 23, and reports the first failure. */
    public Result check() {
        String g = groupFailure();
        if (g != null) {
            return new Result("IVLS is not a group: " + g);
        }
        int s = interval.length;
        for (int x = 0; x < s; x++) {
            if (interval[x].length != s) {
                return new Result("int is not a square table: row " + x + " has " + interval[x].length + " entries");
            }
            for (int y = 0; y < s; y++) {
                if (interval[x][y] < 0 || interval[x][y] >= m) {
                    return new Result("int(" + x + ", " + y + ") = " + interval[x][y] + " is not an interval");
                }
            }
        }
        for (int r = 0; r < s; r++) {
            for (int q = 0; q < s; q++) {
                for (int t = 0; t < s; t++) {
                    if (star[interval[r][q]][interval[q][t]] != interval[r][t]) {
                        return new Result("axiom 1 (composition): int(r, s) * int(s, t) = "
                                          + star[interval[r][q]][interval[q][t]] + " but int(r, t) = "
                                          + interval[r][t] + " at r = " + r + ", s = " + q + ", t = " + t);
                    }
                }
            }
        }
        for (int q = 0; q < s; q++) {
            for (int i = 0; i < m; i++) {
                int targets = 0;
                for (int t = 0; t < s; t++) {
                    if (interval[q][t] == i) {
                        targets++;
                    }
                }
                if (targets != 1) {
                    return new Result("axiom 2 (unique target): s = " + q + ", i = " + i + ": " + targets
                                      + (targets == 1 ? " target" : " targets"));
                }
            }
        }
        return new Result("");
    }

    /** The first failed group axiom (closure, associativity, identity, inverse), or null. */
    private String groupFailure() {
        if (m < 1 || star.length != m) {
            return "the product table has " + star.length + " rows for " + m + " elements";
        }
        for (int i = 0; i < m; i++) {
            if (star[i].length != m) {
                return "closure: row " + i + " has " + star[i].length + " entries";
            }
            for (int j = 0; j < m; j++) {
                if (star[i][j] < 0 || star[i][j] >= m) {
                    return "closure: " + i + " * " + j + " = " + star[i][j] + " is outside 0.." + (m - 1);
                }
            }
        }
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < m; k++) {
                    if (star[star[i][j]][k] != star[i][star[j][k]]) {
                        return "associativity: (" + i + " * " + j + ") * " + k + " = " + star[star[i][j]][k]
                               + " but " + i + " * (" + j + " * " + k + ") = " + star[i][star[j][k]];
                    }
                }
            }
        }
        int e = identityElement();
        if (e < 0) {
            return "identity: no element e with e * i = i * e = i for all i";
        }
        for (int i = 0; i < m; i++) {
            boolean has = false;
            for (int j = 0; j < m && !has; j++) {
                has = star[i][j] == e && star[j][i] == e;
            }
            if (!has) {
                return "inverse: " + i + " has none";
            }
        }
        return null;
    }

    private int identityElement() {
        for (int e = 0; e < m; e++) {
            boolean ok = true;
            for (int i = 0; i < m && ok; i++) {
                ok = star[e][i] == i && star[i][e] == i;
            }
            if (ok) {
                return e;
            }
        }
        return -1;
    }

    /**
     * The GIS of a voice-leading space whose transporter sets all have exactly
     * one element: int(x, y) is that transporter and i &#8902; j = j after i (D8).
     * Null when some set has 0 or 2 or more elements; {@link #whyNot} names it.
     */
    public static GIS fromSpace(VoiceLeadingSpace space) {
        if (!whyNot(space).isEmpty()) {
            return null;
        }
        FiniteAction a = space.action();
        int s = a.pointCount(), m = a.groupSize();
        int[][] in = new int[s][s];
        for (int x = 0; x < s; x++) {
            for (int y = 0; y < s; y++) {
                in[x][y] = space.transporters(x, y).get(0);
            }
        }
        int[][] st = new int[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                st[i][j] = a.multiply(j, i);
            }
        }
        return new GIS(m, st, in);
    }

    /** "" when every transporter set has one element, else the first pair (in row order) that does not. */
    public static String whyNot(VoiceLeadingSpace space) {
        FiniteAction a = space.action();
        for (int x = 0; x < a.pointCount(); x++) {
            for (int y = 0; y < a.pointCount(); y++) {
                List<Integer> t = space.transporters(x, y);
                if (t.size() != 1) {
                    return "(" + a.pointName(x) + ", " + a.pointName(y) + ") has " + t.size()
                           + (t.size() == 1 ? " transporter" : " transporters");
                }
            }
        }
        return "";
    }

    /** int(x, y). */
    public int interval(int x, int y) {
        return interval[x][y];
    }

    /** A copy of the int table. */
    public int[][] intervalTable() {
        return copy(interval);
    }

    /** i &#8902; j. */
    public int product(int i, int j) {
        return star[i][j];
    }

    private static int[][] copy(int[][] t) {
        int[][] r = new int[t.length][];
        for (int i = 0; i < t.length; i++) {
            r[i] = t[i].clone();
        }
        return r;
    }
}
