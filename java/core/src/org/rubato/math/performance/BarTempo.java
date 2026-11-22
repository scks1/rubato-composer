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
package org.rubato.math.performance;

/**
 * A tempo curve that follows its mother outside the bars and a Cor. 21 polygon inside each
 * bar, so that each bar lasts as long as the mother's bar (Example 52, p. 624; departure D8).
 */
final class BarTempo implements TempoCurve {

    private final TempoCurve mother;
    private final double[] bars;
    private final Polygon[] polygons;

    BarTempo(TempoCurve mother, double[] bars, double[] profile) {
        if (bars.length < 2) {
            throw new IllegalArgumentException("a bar rubato needs at least two bar lines");
        }
        if (profile.length == 0) {
            throw new IllegalArgumentException("Cor. 21 needs at least one inner vertex per bar (f >= 2)");
        }
        for (double p : profile) {
            if (!(p > 0)) {
                throw new IllegalArgumentException("rubato profile factors must be positive: " + p);
            }
        }
        this.mother = mother;
        this.bars = bars.clone();
        polygons = new Polygon[bars.length - 1];
        int m = profile.length;
        for (int i = 0; i + 1 < bars.length; i++) {
            double a = bars[i], b = bars[i + 1];
            if (!(b > a)) {
                throw new IllegalArgumentException("bar lines must increase");
            }
            double[] e = new double[m + 2], t = new double[m + 2];
            e[0] = a;
            t[0] = mother.tempo(a);
            for (int k = 1; k <= m; k++) {
                e[k] = a + (b - a) * k / (m + 1);
                t[k] = mother.tempo(e[k]) * profile[k - 1];
            }
            e[m + 1] = b;
            t[m + 1] = mother.tempo(b);
            Polygon p = new Polygon(e, t);
            polygons[i] = Tempo.stretch(p, mother.integral(a, b) / p.integral(a, b));
        }
    }

    private int bar(double x) {
        for (int i = 0; i < polygons.length; i++) {
            if (x >= bars[i] && x < bars[i + 1]) {
                return i;
            }
        }
        return -1;
    }

    public double tempo(double x) {
        int i = bar(x);
        return i < 0 ? mother.tempo(x) : polygons[i].tempo(x);
    }

    private double piece(double a, double b) {
        int i = bar(0.5 * (a + b));
        return i < 0 ? mother.integral(a, b) : polygons[i].integral(a, b);
    }

    public double integral(double a, double b) {
        if (b < a) {
            return -integral(b, a);
        }
        double sum = 0, lo = a;
        for (double cut : bars) {
            if (cut > lo && cut < b) {
                sum += piece(lo, cut);
                lo = cut;
            }
        }
        return sum + piece(lo, b);
    }
}
