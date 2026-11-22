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

package org.rubato.math.counterpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.rubato.math.arith.NumberTheory;

/**
 * A marked interval dichotomy (X/Y) of Z_n with n = 2k: X, the consonances,
 * has k elements and Y is its complement (CCW Defs. 2.1&ndash;2.2,
 * pp. 5&ndash;6; Topos I section 30.1, pp. 517&ndash;518, zero address only).
 * <p>
 * The dichotomy is <i>autocomplementary</i> if some affine symmetry p
 * maps X onto Y, <i>rigid</i> if only the identity maps X onto X, and
 * <i>strong</i> if both hold; the p of a strong dichotomy is unique and is
 * its <i>polarity</i> (CCW Def. 2.3, p. 8; Topos I Def. 92, p. 518).
 */
public final class Dichotomy {

    private final int n;
    private final BitSet consonances;

    /**
     * Creates the dichotomy whose consonances are the given residues mod n.
     *
     * @throws IllegalArgumentException if n is odd or the set does not have n/2 elements
     */
    public Dichotomy(int n, int... consonances) {
        if (n < 2 || n % 2 != 0) {
            throw new IllegalArgumentException("A dichotomy needs an even modulus, got " + n);
        }
        this.n = n;
        this.consonances = new BitSet(n);
        for (int c : consonances) {
            this.consonances.set(NumberTheory.mod(c, n));
        }
        if (this.consonances.cardinality() != n / 2) {
            throw new IllegalArgumentException("A dichotomy of Z_" + n + " needs " + n / 2
                    + " distinct consonances, got " + Arrays.toString(consonances));
        }
    }

    public int getModulus() { return n; }

    /** k = n/2. */
    public int getHalf() { return n / 2; }

    public boolean isConsonant(int y) {
        return consonances.get(NumberTheory.mod(y, n));
    }

    /** The consonances X in increasing order. */
    public int[] getConsonances() {
        return consonances.stream().toArray();
    }

    /** The dissonances Y in increasing order. */
    public int[] getDissonances() {
        BitSet y = (BitSet) consonances.clone();
        y.flip(0, n);
        return y.stream().toArray();
    }

    /** All affine symmetries T^u &middot; v of Z_n. */
    public static List<ZnAffine> affineSymmetries(int n) {
        List<ZnAffine> all = new ArrayList<ZnAffine>();
        for (int v = 1; v < n; v++) {
            if (ZnAffine.isUnit(v, n)) {
                for (int u = 0; u < n; u++) {
                    all.add(new ZnAffine(u, v, n));
                }
            }
        }
        return all;
    }

    /** The symmetries f with f(X) = Y. */
    public List<ZnAffine> complementingSymmetries() {
        BitSet y = (BitSet) consonances.clone();
        y.flip(0, n);
        return symmetriesMapping(y);
    }

    /** The symmetries f with f(X) = X, the stabiliser of X. */
    public List<ZnAffine> stabiliser() {
        return symmetriesMapping(consonances);
    }

    public boolean isAutocomplementary() {
        return !complementingSymmetries().isEmpty();
    }

    public boolean isRigid() {
        return stabiliser().size() == 1;
    }

    public boolean isStrong() {
        return isAutocomplementary() && isRigid();
    }

    /**
     * The polarity of a strong dichotomy.
     *
     * @throws IllegalStateException if the dichotomy is not strong
     */
    public ZnAffine polarity() {
        List<ZnAffine> p = complementingSymmetries();
        if (p.size() != 1 || !isRigid()) {
            throw new IllegalStateException(this + " is not strong; it has " + p.size()
                    + " complementing symmetries and a stabiliser of order " + stabiliser().size());
        }
        return p.get(0);
    }

    /** The image f(X) as a new dichotomy. */
    public Dichotomy image(ZnAffine f) {
        int[] x = getConsonances();
        int[] fx = new int[x.length];
        for (int i = 0; i < x.length; i++) {
            fx[i] = f.apply(x[i]);
        }
        return new Dichotomy(n, fx);
    }

    BitSet consonanceBits() {
        return (BitSet) consonances.clone();
    }

    private List<ZnAffine> symmetriesMapping(BitSet target) {
        int[] x = getConsonances();
        List<ZnAffine> found = new ArrayList<ZnAffine>();
        for (ZnAffine f : affineSymmetries(n)) {
            BitSet img = new BitSet(n);
            for (int c : x) {
                img.set(f.apply(c));
            }
            if (img.equals(target)) {
                found.add(f);
            }
        }
        return Collections.unmodifiableList(found);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Dichotomy && ((Dichotomy) o).n == n && ((Dichotomy) o).consonances.equals(consonances);
    }

    @Override
    public int hashCode() {
        return consonances.hashCode() * 31 + n;
    }

    /** For example "{0,3,4,7,8,9}/{1,2,5,6,10,11}". */
    @Override
    public String toString() {
        return set(getConsonances()) + "/" + set(getDissonances());
    }

    private static String set(int[] s) {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < s.length; i++) {
            b.append(i == 0 ? "" : ",").append(s[i]);
        }
        return b.append('}').toString();
    }
}
