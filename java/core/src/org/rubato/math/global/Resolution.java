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
import java.util.List;

import org.rubato.math.global.GlobalComposition.Chart;

/**
 * The global standard composition and the resolution of a zero-addressed
 * global composition (Mazzola, <i>The Topos of Music</i>, &sect;15.2.1,
 * pp. 293&ndash;294).
 * <p>
 * At the zero address the local standard composition of dimension m is
 * {&sigma;<sub>0</sub>, &hellip;, &sigma;<sub>m</sub>} &sub; R<sup>m</sup> with
 * &sigma;<sub>0</sub> = 0 and &sigma;<sub>i</sub> = e<sub>i</sub> ((15.12)&ndash;(15.13); for
 * A = 0 the summands a<sub>i</sub> vanish). Numbering the support of G<sup>I</sup>
 * as 0 &hellip; m, the covering of G becomes a covering of {&sigma;<sub>i</sub>}, and
 * the standard atlas has, for each covering set q = {t<sub>0</sub> &lt; &hellip; &lt;
 * t<sub>c</sub>}, the chart &Delta;<sup>c</sup> &rarr; &Delta;<sup>m</sup>,
 * &sigma;<sub>j</sub> &#8614; &sigma;<sub>t<sub>j</sub></sub>. The resolution
 * res : &Delta;G<sup>I</sup> &rarr; G<sup>I</sup> sends &sigma;<sub>i</sub> to the i-th
 * support point and each covering set to itself (15.16). The book notes it
 * is bijective with an isomorphism of nerves, but not an isomorphism in
 * general (footnote 5, p. 294).
 * <p>
 * Here R = Z_n, the modulus of the support; the dimension is m, or 1 when
 * the support has a single point.
 */
public final class Resolution {

    private Resolution() { /* static utility */ }

    /** The zero-addressed local standard composition {0, e_1, ..., e_c} in Z_n^max(c,1). */
    public static LocalComposition localStandard(int n, int c) {
        int d = Math.max(c, 1);
        List<int[]> ps = new ArrayList<int[]>();
        ps.add(new int[d]);
        for (int i = 1; i <= c; i++) {
            int[] e = new int[d];
            e[i - 1] = 1;
            ps.add(e);
        }
        return new LocalComposition(n, d, ps);
    }

    /** The point &sigma;_i of the standard composition of dimension m. */
    static int[] sigma(int i, int m) {
        int[] p = new int[Math.max(m, 1)];
        if (i > 0) {
            p[i - 1] = 1;
        }
        return p;
    }

    /** The global standard composition &Delta;G<sup>I</sup> with its standard atlas. */
    public static GlobalComposition standard(GlobalComposition g) {
        int n = g.getSupport().getModulus();
        int m = g.getSupport().size() - 1;
        LocalComposition delta = localStandard(n, m);
        // support index of sigma_i in delta
        int[] at = new int[m + 1];
        for (int i = 0; i <= m; i++) {
            at[i] = delta.indexOf(sigma(i, m));
        }
        List<int[]> cover = new ArrayList<int[]>();
        List<Chart> atlas = new ArrayList<Chart>();
        for (int q = 0; q < g.coveringSize(); q++) {
            int[] ts = g.coveringSet(q);
            int[] c = new int[ts.length];
            for (int j = 0; j < ts.length; j++) {
                c[j] = at[ts[j]];
            }
            cover.add(c);
            int cdim = ts.length - 1;
            LocalComposition k = localStandard(n, cdim);
            int[] phi = new int[ts.length];
            for (int j = 0; j < ts.length; j++) {
                phi[k.indexOf(sigma(j, cdim))] = at[ts[j]];
            }
            atlas.add(new Chart(k, q, phi));
        }
        return new GlobalComposition(delta, g.names(), cover, atlas);
    }

    /** The resolution res : &Delta;G<sup>I</sup> &rarr; G<sup>I</sup> (15.16). */
    public static GlobalMorphism resolution(GlobalComposition g) {
        GlobalComposition d = standard(g);
        int m = g.getSupport().size() - 1;
        int[] f = new int[m + 1];
        for (int i = 0; i <= m; i++) {
            f[d.getSupport().indexOf(sigma(i, m))] = i;
        }
        int[] iota = new int[g.coveringSize()];
        for (int q = 0; q < iota.length; q++) {
            iota[q] = q;
        }
        return new GlobalMorphism(d, g, f, iota);
    }
}
