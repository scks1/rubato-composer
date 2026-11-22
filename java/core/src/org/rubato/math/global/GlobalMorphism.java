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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rubato.math.global.GlobalComposition.Chart;

/**
 * A morphism (f, &iota;, Id) of zero-addressed objective global compositions
 * (Mazzola, <i>The Topos of Music</i>, &sect;14.2, Def. 43, p. 274): a map f
 * of supports and a map &iota; of covering sets with f(i) &sub; &iota;(i), such
 * that for every chart t over i and every chart s over &iota;(i) the induced
 * map K<sub>t</sub> &rarr; L<sub>s</sub> is a morphism of local compositions.
 * All address changes are identities. {@link #violations()} lists what fails;
 * {@link #nerveMap()} is the induced simplicial map n(f/&alpha;) of &sect;14.4
 * (p. 282). Instances are immutable.
 */
public final class GlobalMorphism {

    public final GlobalComposition domain;
    public final GlobalComposition codomain;
    private final int[] f;
    private final int[] iota;

    /**
     * @param f    f[x] is the codomain support index of the image of support point x
     * @param iota iota[i] is the codomain covering set that receives covering set i
     */
    public GlobalMorphism(GlobalComposition domain, GlobalComposition codomain, int[] f, int[] iota) {
        if (f.length != domain.getSupport().size()) {
            throw new IllegalArgumentException("f has " + f.length + " values for " + domain.getSupport().size() + " points");
        }
        if (iota.length != domain.coveringSize()) {
            throw new IllegalArgumentException("iota has " + iota.length + " values for " + domain.coveringSize() + " covering sets");
        }
        for (int x : f) {
            if (x < 0 || x >= codomain.getSupport().size()) {
                throw new IllegalArgumentException("f has value " + x + " outside the codomain support");
            }
        }
        for (int j : iota) {
            if (j < 0 || j >= codomain.coveringSize()) {
                throw new IllegalArgumentException("iota has value " + j + " outside the codomain covering");
            }
        }
        this.domain = domain;
        this.codomain = codomain;
        this.f = f.clone();
        this.iota = iota.clone();
    }

    /** The refinement morphism Id/Id : K<sup>I</sup> &rarr; K<sup>J</sup> of Ex. 21 (p. 275). */
    public static GlobalMorphism refinement(GlobalComposition fine, GlobalComposition coarse, int[] iota) {
        int[] id = new int[fine.getSupport().size()];
        for (int i = 0; i < id.length; i++) {
            id[i] = coarse.getSupport().indexOf(fine.getSupport().point(i));
            if (id[i] < 0) {
                throw new IllegalArgumentException("The supports differ");
            }
        }
        return new GlobalMorphism(fine, coarse, id, iota);
    }

    public int f(int x) { return f[x]; }

    public int iota(int i) { return iota[i]; }

    /** The covering map &iota;, a fresh copy. */
    public int[] iota() { return iota.clone(); }

    /** The support map f, a fresh copy. */
    public int[] supportMap() { return f.clone(); }

    /** Every way the data fail Def. 43; empty if this is a morphism. */
    public List<String> violations() {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < domain.coveringSize(); i++) {
            int[] target = codomain.coveringSet(iota[i]);
            for (int x : domain.coveringSet(i)) {
                if (Arrays.binarySearch(target, f[x]) < 0) {
                    out.add("f(" + domain.names().get(i) + ") is not inside " + codomain.names().get(iota[i]));
                    break;
                }
            }
        }
        if (!out.isEmpty()) {
            return out;
        }
        for (Chart t : domain.atlas()) {
            for (Chart s : codomain.atlas()) {
                if (s.coveringSet != iota[t.coveringSet]) {
                    continue;
                }
                int[] ft = new int[t.domain.size()];
                for (int p = 0; p < ft.length; p++) {
                    int y = f[t.phi(p)];
                    ft[p] = -1;
                    for (int q = 0; q < s.domain.size(); q++) {
                        if (s.phi(q) == y) {
                            ft[p] = q;
                        }
                    }
                }
                if (!t.domain.isMorphism(ft, s.domain)) {
                    out.add("the chart map " + t.domain + " -> " + s.domain + " over "
                            + domain.names().get(t.coveringSet) + " is not a morphism of local compositions");
                }
            }
        }
        return out;
    }

    /** Returns true if the data satisfy Def. 43. */
    public boolean isMorphism() {
        return violations().isEmpty();
    }

    /** The induced simplicial map of nerves, &iota; on vertices (&sect;14.4, p. 282). */
    public int[] nerveMap() {
        return iota.clone();
    }

    /** The composition g &middot; this (Def. 45, p. 275). */
    public GlobalMorphism then(GlobalMorphism g) {
        if (g.domain != codomain) {
            throw new IllegalArgumentException("The morphisms do not compose");
        }
        int[] h = new int[f.length];
        for (int x = 0; x < h.length; x++) {
            h[x] = g.f[f[x]];
        }
        int[] z = new int[iota.length];
        for (int i = 0; i < z.length; i++) {
            z[i] = g.iota[iota[i]];
        }
        return new GlobalMorphism(domain, g.codomain, h, z);
    }

    /** The inverse data (f&minus;1, &iota;&minus;1), or null if f or &iota; is not bijective. */
    public GlobalMorphism inverseData() {
        int[] fi = invert(f, codomain.getSupport().size());
        int[] ii = invert(iota, codomain.coveringSize());
        return fi == null || ii == null ? null : new GlobalMorphism(codomain, domain, fi, ii);
    }

    /**
     * Returns true if this is an isomorphism: a morphism whose inverse data
     * are also a morphism.
     */
    public boolean isIsomorphism() {
        GlobalMorphism inv = inverseData();
        return isMorphism() && inv != null && inv.isMorphism();
    }

    /** Returns true if f is a bijection of supports. */
    public boolean isBijective() {
        return invert(f, codomain.getSupport().size()) != null;
    }

    private static int[] invert(int[] m, int size) {
        if (m.length != size) {
            return null;
        }
        int[] inv = new int[size];
        Arrays.fill(inv, -1);
        for (int i = 0; i < m.length; i++) {
            if (inv[m[i]] >= 0) {
                return null;
            }
            inv[m[i]] = i;
        }
        return inv;
    }
}
