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
 * A discrete weight on onsets (Mazzola, <i>The Topos of Music II</i>, &sect;39.1.1, p. 638):
 * a name and real values at strictly increasing onsets. The Weight Watcher normalizes,
 * extends, deforms and combines weights; see {@link WeightWatcher}.
 */
public final class Weight {

    private final String name;
    private final double[] onsets;
    private final double[] values;

    /**
     * @throws IllegalArgumentException unless there is at least one point, onsets and values
     *         have the same number of entries, the onsets increase strictly and every number is finite
     */
    public Weight(String name, double[] onsets, double[] values) {
        if (onsets.length != values.length) {
            throw new IllegalArgumentException("a weight needs the same number of onsets and values: "
                    + onsets.length + " onsets, " + values.length + " values");
        }
        if (onsets.length == 0) {
            throw new IllegalArgumentException("a weight needs at least one point");
        }
        for (int i = 0; i < onsets.length; i++) {
            if (!Double.isFinite(onsets[i]) || !Double.isFinite(values[i])) {
                throw new IllegalArgumentException("weight onsets and values must be finite: " + onsets[i] + ":" + values[i]);
            }
            if (i > 0 && !(onsets[i] > onsets[i - 1])) {
                throw new IllegalArgumentException("weight onsets must be strictly increasing: " + onsets[i - 1]
                        + " then " + onsets[i]);
            }
        }
        this.name = name;
        this.onsets = onsets.clone();
        this.values = values.clone();
    }

    public String getName() { return name; }

    public double[] getOnsets() { return onsets.clone(); }

    public double[] getValues() { return values.clone(); }

    public int size() { return onsets.length; }

    public double min() {
        double m = Double.POSITIVE_INFINITY;
        for (double v : values) {
            m = Math.min(m, v);
        }
        return m;
    }

    public double max() {
        double m = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            m = Math.max(m, v);
        }
        return m;
    }

    public double mean() {
        double s = 0;
        for (double v : values) {
            s += v;
        }
        return s / values.length;
    }
}
