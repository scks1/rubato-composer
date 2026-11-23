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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The path algebra QGamma of a finite digraph modulo homogeneous relations
 * (Mazzola, The Topos of Music, vol. IV, Def. 193, p. 1458; vol. III,
 * section 61.10, pp. 925-926). Arrows are named by single characters; a
 * word lists the arrows of a path in the order it runs through them.
 * Dimensions are exact: they come from integer row reduction of the
 * degree-d part of the ideal.
 */
public final class PathAlgebra {

    private final Digraph digraph;
    private final String names;
    private final List<Map<String, Integer>> relations = new ArrayList<Map<String, Integer>>();

    /** names.charAt(a) is the name of arrow a. */
    public PathAlgebra(Digraph digraph, String names) {
        if (names.length() != digraph.arrowCount()) {
            throw new IllegalArgumentException("one name per arrow needed");
        }
        this.digraph = digraph;
        this.names = names;
    }

    /** Adds a homogeneous relation given as coefficient, word, coefficient, word, ... */
    public void addRelation(Object... terms) {
        relations.add(element(terms));
    }

    private Map<String, Integer> element(Object[] terms) {
        if (terms.length == 0 || terms.length % 2 != 0) {
            throw new IllegalArgumentException("terms come in pairs: coefficient, word");
        }
        Map<String, Integer> r = new LinkedHashMap<String, Integer>();
        int length = -1;
        int[] ends = null;
        for (int i = 0; i < terms.length; i += 2) {
            int c = (Integer) terms[i];
            String w = (String) terms[i + 1];
            int[] e = ends(w);
            if (length >= 0 && (w.length() != length || e[0] != ends[0] || e[1] != ends[1])) {
                throw new IllegalArgumentException("not homogeneous: " + w);
            }
            length = w.length();
            ends = e;
            Integer old = r.get(w);
            r.put(w, (old == null ? 0 : old) + c);
        }
        return r;
    }

    /** The start and end vertex of a nonempty word. */
    private int[] ends(String w) {
        if (w.isEmpty()) {
            throw new IllegalArgumentException("empty word");
        }
        int prev = -1, start = -1;
        for (int i = 0; i < w.length(); i++) {
            int a = names.indexOf(w.charAt(i));
            if (a < 0) {
                throw new IllegalArgumentException("no arrow named " + w.charAt(i));
            }
            if (i == 0) {
                start = digraph.tail(a);
            }
            else if (digraph.tail(a) != prev) {
                throw new IllegalArgumentException(w + " is not a path at position " + i);
            }
            prev = digraph.head(a);
        }
        return new int[] { start, prev };
    }

    /** The words of the paths of a given length from one vertex to another. */
    public List<String> paths(int from, int to, int length) {
        List<String> out = new ArrayList<String>();
        if (length == 0) {
            if (from == to) {
                out.add("");
            }
            return out;
        }
        extend(from, to, length, "", out);
        return out;
    }

    private void extend(int at, int to, int left, String w, List<String> out) {
        if (left == 0) {
            if (at == to) {
                out.add(w);
            }
            return;
        }
        for (int a = 0; a < digraph.arrowCount(); a++) {
            if (digraph.tail(a) == at) {
                extend(digraph.head(a), to, left - 1, w + names.charAt(a), out);
            }
        }
    }

    /** The rows spanning the degree-d part of the ideal between two vertices, over the given basis. */
    private List<BigInteger[]> idealRows(int from, int to, int length, List<String> basis) {
        List<BigInteger[]> rows = new ArrayList<BigInteger[]>();
        for (Map<String, Integer> r : relations) {
            String any = r.keySet().iterator().next();
            int rl = any.length();
            if (rl > length) {
                continue;
            }
            int[] e = ends(any);
            for (int u = 0; u <= length - rl; u++) {
                for (String left : paths(from, e[0], u)) {
                    for (String right : paths(e[1], to, length - rl - u)) {
                        BigInteger[] row = new BigInteger[basis.size()];
                        java.util.Arrays.fill(row, BigInteger.ZERO);
                        for (Map.Entry<String, Integer> t : r.entrySet()) {
                            int i = basis.indexOf(left + t.getKey() + right);
                            row[i] = row[i].add(BigInteger.valueOf(t.getValue()));
                        }
                        rows.add(row);
                    }
                }
            }
        }
        return rows;
    }

    /** The dimension of the degree-d part of the quotient between two vertices. */
    public int dimension(int from, int to, int length) {
        List<String> basis = paths(from, to, length);
        return basis.size() - rank(idealRows(from, to, length, basis), basis.size());
    }

    /** The sum of the dimensions of degrees 1 to maxLength: the radical when higher degrees vanish. */
    public int radicalDimension(int from, int to, int maxLength) {
        int sum = 0;
        for (int d = 1; d <= maxLength; d++) {
            sum += dimension(from, to, d);
        }
        return sum;
    }

    /**
     * The dimension of Rad/Rad^2 between two vertices: the degree-1 part,
     * since every relation is homogeneous.
     */
    public int radicalSquareQuotientDimension(int from, int to) {
        return dimension(from, to, 1);
    }

    /** True if the homogeneous element (coefficient, word, ...) lies in the ideal. */
    public boolean inIdeal(Object... terms) {
        Map<String, Integer> x = element(terms);
        String any = x.keySet().iterator().next();
        int[] e = ends(any);
        List<String> basis = paths(e[0], e[1], any.length());
        List<BigInteger[]> rows = idealRows(e[0], e[1], any.length(), basis);
        int r0 = rank(rows, basis.size());
        BigInteger[] row = new BigInteger[basis.size()];
        java.util.Arrays.fill(row, BigInteger.ZERO);
        for (Map.Entry<String, Integer> t : x.entrySet()) {
            int i = basis.indexOf(t.getKey());
            row[i] = row[i].add(BigInteger.valueOf(t.getValue()));
        }
        rows.add(row);
        return rank(rows, basis.size()) == r0;
    }

    /** The rank over Q of integer rows, by fraction-free elimination. */
    static int rank(List<BigInteger[]> input, int cols) {
        List<BigInteger[]> m = new ArrayList<BigInteger[]>();
        for (BigInteger[] r : input) {
            m.add(r.clone());
        }
        int rank = 0;
        for (int c = 0; c < cols && rank < m.size(); c++) {
            int piv = -1;
            for (int i = rank; i < m.size(); i++) {
                if (m.get(i)[c].signum() != 0) {
                    piv = i;
                    break;
                }
            }
            if (piv < 0) {
                continue;
            }
            BigInteger[] p = m.get(piv);
            m.set(piv, m.get(rank));
            m.set(rank, p);
            for (int i = rank + 1; i < m.size(); i++) {
                BigInteger[] r = m.get(i);
                if (r[c].signum() == 0) {
                    continue;
                }
                BigInteger a = p[c], b = r[c];
                BigInteger g = BigInteger.ZERO;
                for (int j = 0; j < cols; j++) {
                    r[j] = r[j].multiply(a).subtract(p[j].multiply(b));
                    g = g.gcd(r[j]);
                }
                if (g.signum() != 0 && !g.equals(BigInteger.ONE)) {
                    for (int j = 0; j < cols; j++) {
                        r[j] = r[j].divide(g);
                    }
                }
            }
            rank++;
        }
        return rank;
    }
}
