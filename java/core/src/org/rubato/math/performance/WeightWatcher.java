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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/**
 * The Weight Watcher of the PerformanceRUBETTE (Mazzola, <i>The Topos of Music II</i>,
 * &sect;41.4, p. 681, Fig. 41.14): a list of weights, each with High Norm, Low Norm,
 * Deformation, an inversion flag, Influence and Tolerance, combined as a sum or, with
 * "Combine as Product", as a product.
 * <p>
 * Each entry turns its discrete weight into a function of the onset in four steps (the
 * order is departure D2 of sources/STEMMA-VIEW-SPEC.md):
 * <ol>
 * <li>normalize the values affinely from [min, max] onto [Low Norm, High Norm], reversed
 * when inverted (D3; this reproduces the "Norm. Mean" of Fig. 41.14);</li>
 * <li>extend them by the C<sup>1</sup> spline of &sect;32.3.2.1 (pp. 553-554) with the
 * frame ]first onset - Tolerance, last onset + Tolerance[, outside of which the weight is 1 (D4);</li>
 * <li>deform the extension by delta(alpha_min, alpha_max, tau) of p. 640 on its image
 * [alpha_min, alpha_max] (eqs. (39.8)-(39.9), D5);</li>
 * <li>multiply by the influence, the scalar multiple of eq. (39.3) (D6).</li>
 * </ol>
 * The combination is the pointwise sum (39.5) or product (39.7) of the entries.
 */
public final class WeightWatcher {

    /** One loaded weight with its Weight Watcher parameters. */
    public static final class Entry {
        private final Weight weight;
        private final double high, low, deformation, influence, tolerance;
        private final boolean inverted;
        private final double[] normalized;
        private final double imageMin, imageMax;

        /**
         * @throws IllegalArgumentException unless 0 &lt; low &le; high, the influence and the
         *         tolerance are positive and every number is finite
         */
        public Entry(Weight weight, double high, double low, double deformation, boolean inverted, double influence,
                     double tolerance) {
            if (!Double.isFinite(high) || !Double.isFinite(low) || !Double.isFinite(deformation)
                    || !Double.isFinite(influence) || !Double.isFinite(tolerance)) {
                throw new IllegalArgumentException("Weight Watcher parameters must be finite numbers");
            }
            if (!(low > 0) || !(high > 0)) {
                throw new IllegalArgumentException("Low Norm and High Norm must be positive: low " + low + ", high " + high);
            }
            if (low > high) {
                throw new IllegalArgumentException("Low Norm must not exceed High Norm: low " + low + ", high " + high);
            }
            if (!(influence > 0)) {
                throw new IllegalArgumentException("the influence must be positive: " + influence);
            }
            if (!(tolerance > 0)) {
                throw new IllegalArgumentException("the tolerance must be positive: " + tolerance);
            }
            this.weight = weight;
            this.high = high;
            this.low = low;
            this.deformation = deformation;
            this.inverted = inverted;
            this.influence = influence;
            this.tolerance = tolerance;
            double[] v = weight.getValues();
            double min = weight.min(), max = weight.max();
            normalized = new double[v.length];
            double lo = 1, hi = 1;
            for (int i = 0; i < v.length; i++) {
                double n;
                if (min == max) {
                    n = inverted ? low : high;
                }
                else if (v[i] == min) {
                    n = inverted ? high : low;
                }
                else if (v[i] == max) {
                    n = inverted ? low : high;
                }
                else {
                    n = normalize(v[i]);
                }
                normalized[i] = n;
                lo = Math.min(lo, n);
                hi = Math.max(hi, n);
            }
            imageMin = lo;
            imageMax = hi;
        }

        /** The affine normalization of a raw value (D3). */
        private double normalize(double raw) {
            double min = weight.min(), max = weight.max();
            if (min == max) {
                return inverted ? low : high;
            }
            double t = (raw - min) / (max - min);
            return inverted ? high - t * (high - low) : low + t * (high - low);
        }

        public Weight getWeight() { return weight; }

        public double getHigh() { return high; }

        public double getLow() { return low; }

        public double getDeformation() { return deformation; }

        public boolean isInverted() { return inverted; }

        public double getInfluence() { return influence; }

        public double getTolerance() { return tolerance; }

        /** The normalized values at the weight's onsets. */
        public double[] normalized() { return normalized.clone(); }

        /** The normalized mean of the raw values, the "Norm. Mean" of Fig. 41.14. */
        public double normMean() { return normalize(weight.mean()); }

        /** The lower end u of the frame: the first onset minus the tolerance. */
        public double frameStart() { return weight.getOnsets()[0] - tolerance; }

        /** The upper end v of the frame: the last onset plus the tolerance. */
        public double frameEnd() { return weight.getOnsets()[weight.size() - 1] + tolerance; }

        /** alpha_min, the smallest value of the extension. */
        public double imageMin() { return imageMin; }

        /** alpha_max, the largest value of the extension. */
        public double imageMax() { return imageMax; }

        /** The extension of the normalized weight at onset e (&sect;32.3.2.1). */
        public double extended(double e) {
            return extend(weight.getOnsets(), normalized, frameStart(), frameEnd(), e);
        }

        /** The entry's function of the onset: influence times the deformed extension. */
        public DoubleUnaryOperator function() {
            final double[] xs = weight.getOnsets();
            final double[] fs = normalized.clone();
            final double u = frameStart(), v = frameEnd();
            return e -> influence * deform(imageMin, imageMax, deformation, extend(xs, fs, u, v, e));
        }
    }

    private final List<Entry> entries;
    private final boolean product;

    public WeightWatcher(List<Entry> entries, boolean product) {
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
        this.product = product;
    }

    public List<Entry> getEntries() { return entries; }

    /** True when the weights combine as a product ("Combine as Product"), false for the sum. */
    public boolean isProduct() { return product; }

    public boolean isEmpty() { return entries.isEmpty(); }

    /** The combined weight: the sum (39.5) or product (39.7) of the entries' functions. */
    public DoubleUnaryOperator combined() {
        if (entries.isEmpty()) {
            throw new IllegalStateException("the Weight Watcher has no weights");
        }
        final List<DoubleUnaryOperator> fs = new ArrayList<DoubleUnaryOperator>();
        for (Entry e : entries) {
            fs.add(e.function());
        }
        final boolean p = product;
        return e -> {
            double r = p ? 1 : 0;
            for (DoubleUnaryOperator f : fs) {
                r = p ? r * f.applyAsDouble(e) : r + f.applyAsDouble(e);
            }
            return r;
        };
    }

    /** The smallest frame start of the entries. */
    public double frameStart() {
        double m = Double.POSITIVE_INFINITY;
        for (Entry e : nonEmpty()) {
            m = Math.min(m, e.frameStart());
        }
        return m;
    }

    /** The largest frame end of the entries. */
    public double frameEnd() {
        double m = Double.NEGATIVE_INFINITY;
        for (Entry e : nonEmpty()) {
            m = Math.max(m, e.frameEnd());
        }
        return m;
    }

    private List<Entry> nonEmpty() {
        if (entries.isEmpty()) {
            throw new IllegalStateException("the Weight Watcher has no weights");
        }
        return entries;
    }

    /**
     * The C<sup>1</sup> extension of &sect;32.3.2.1 (p. 554) in one dimension: the value fs[i]
     * at xs[i], 1 at the frame points u and v, zero slope at all of them, the cubic
     * f_a + (f_b - f_a) s<sup>2</sup> (3 - 2 s) with s = (t - a)/(b - a) between successive
     * points (Exercise 71), and 1 outside ]u, v[.
     *
     * @throws IllegalArgumentException unless u &lt; xs[0] and xs[last] &lt; v
     */
    public static double extend(double[] xs, double[] fs, double u, double v, double t) {
        if (!(u < xs[0]) || !(xs[xs.length - 1] < v)) {
            throw new IllegalArgumentException("the frame ]" + u + ", " + v + "[ must contain the weight's onsets");
        }
        if (t <= u || t >= v) {
            return 1;
        }
        double a, b, fa, fb;
        if (t < xs[0]) {
            a = u; b = xs[0]; fa = 1; fb = fs[0];
        }
        else if (t >= xs[xs.length - 1]) {
            a = xs[xs.length - 1]; b = v; fa = fs[fs.length - 1]; fb = 1;
        }
        else {
            int i = Arrays.binarySearch(xs, t);
            if (i < 0) {
                i = -i - 2;
            }
            a = xs[i]; b = xs[i + 1]; fa = fs[i]; fb = fs[i + 1];
        }
        double s = (t - a) / (b - a);
        return fa + (fb - fa) * s * s * (3 - 2 * s);
    }

    /**
     * The deformation delta(alpha_min, alpha_max, tau) of p. 640: the identity outside
     * [alpha_min, alpha_max], and inside it the affine image of
     * delta(0, 1, tau)(s) = s / (e<sup>2 tau</sup> - s (e<sup>2 tau</sup> - 1)). The same
     * formula with negative tau is the diagonal reflection of delta(0, 1, |tau|) (D5).
     */
    public static double deform(double amin, double amax, double tau, double t) {
        if (!(amax > amin) || t <= amin || t >= amax) {
            return t;
        }
        double s = (t - amin) / (amax - amin);
        double c = Math.exp(2 * tau);
        return amin + (amax - amin) * (s / (c - s * (c - 1)));
    }

    private static final List<String> KEYS = Arrays.asList("high", "low", "deformation", "inverted", "influence",
                                                           "tolerance");

    /**
     * Reads weights from text, one per line (D7):
     * <code>name high=&hellip; low=&hellip; [deformation=0] [inverted=no] [influence=1]
     * [tolerance=1] points=E:w,E:w,&hellip;</code>. Empty lines and lines starting with
     * <code>#</code> are skipped; <code>points=</code> comes last and may contain spaces.
     *
     * @throws IllegalArgumentException naming the line of the first error
     */
    public static WeightWatcher parse(String text, boolean product) {
        List<Entry> list = new ArrayList<Entry>();
        String[] lines = text == null ? new String[0] : text.split("\\r?\\n", -1);
        for (int ln = 0; ln < lines.length; ln++) {
            String line = lines[ln].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String where = "Weight Watcher line " + (ln + 1) + ": ";
            try {
                list.add(parseLine(line));
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(where + e.getMessage(), e);
            }
        }
        return new WeightWatcher(list, product);
    }

    private static Entry parseLine(String line) {
        int p = line.indexOf("points=");
        if (p < 0) {
            throw new IllegalArgumentException("points=E:w,E:w,... is missing");
        }
        String[] head = line.substring(0, p).trim().split("\\s+");
        String pts = line.substring(p + "points=".length()).replaceAll("\\s+", "");
        if (head.length == 0 || head[0].isEmpty() || head[0].contains("=")) {
            throw new IllegalArgumentException("the line must start with the weight's name");
        }
        String name = head[0];
        Map<String, String> kv = new LinkedHashMap<String, String>();
        for (int i = 1; i < head.length; i++) {
            int eq = head[i].indexOf('=');
            String k = eq < 0 ? head[i] : head[i].substring(0, eq);
            if (!KEYS.contains(k)) {
                throw new IllegalArgumentException("unknown key " + k + " (keys: " + KEYS + ", points)");
            }
            if (eq < 0) {
                throw new IllegalArgumentException(k + " needs a value, as in " + k + "=1");
            }
            if (kv.put(k, head[i].substring(eq + 1)) != null) {
                throw new IllegalArgumentException(k + " is given twice");
            }
        }
        if (!kv.containsKey("high")) {
            throw new IllegalArgumentException("high= (High Norm) is missing");
        }
        if (!kv.containsKey("low")) {
            throw new IllegalArgumentException("low= (Low Norm) is missing");
        }
        double high = number(kv, "high", 0), low = number(kv, "low", 0);
        double deformation = number(kv, "deformation", 0), influence = number(kv, "influence", 1);
        double tolerance = number(kv, "tolerance", 1);
        boolean inverted = false;
        if (kv.containsKey("inverted")) {
            String s = kv.get("inverted").toLowerCase(Locale.ROOT);
            if (s.equals("yes") || s.equals("true")) {
                inverted = true;
            }
            else if (!s.equals("no") && !s.equals("false")) {
                throw new IllegalArgumentException("inverted must be yes or no: " + kv.get("inverted"));
            }
        }
        if (pts.isEmpty()) {
            throw new IllegalArgumentException("points= lists no points");
        }
        String[] items = pts.split(",");
        double[] e = new double[items.length], w = new double[items.length];
        for (int i = 0; i < items.length; i++) {
            String[] ew = items[i].split(":", -1);
            if (ew.length != 2) {
                throw new IllegalArgumentException("points must be E:w pairs separated by commas: " + items[i]);
            }
            try {
                e[i] = Double.parseDouble(ew[0]);
                w[i] = Double.parseDouble(ew[1]);
            }
            catch (NumberFormatException x) {
                throw new IllegalArgumentException("points must be E:w pairs of numbers: " + items[i]);
            }
        }
        return new Entry(new Weight(name, e, w), high, low, deformation, inverted, influence, tolerance);
    }

    private static double number(Map<String, String> kv, String key, double dflt) {
        String s = kv.get(key);
        if (s == null) {
            return dflt;
        }
        try {
            double d = Double.parseDouble(s);
            if (Double.isFinite(d)) {
                return d;
            }
        }
        catch (NumberFormatException x) {
            // reported below
        }
        throw new IllegalArgumentException(key + " must be a finite number: " + s);
    }

    /** A number in the views and reports: integers without decimals, else up to ten decimals. */
    public static String format(double x) {
        if (x == Math.rint(x) && Math.abs(x) < 1e15) {
            return Long.toString((long) x);
        }
        String s = String.format(Locale.ROOT, "%.10f", x);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
