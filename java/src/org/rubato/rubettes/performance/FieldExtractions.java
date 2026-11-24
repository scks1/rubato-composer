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
package org.rubato.rubettes.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.rubato.io.ScoreNote;
import org.rubato.math.performance.FieldExtraction;
import org.rubato.math.performance.FieldExtraction.Case;
import org.rubato.math.yoneda.Denotator;

/**
 * Field extraction from a Score and a performance of it (Mazzola, <i>The Topos of Music II</i>,
 * &sect;47.3, pp. 759-765): matches the notes, extracts the discrete performance field on chosen
 * coordinates with {@link FieldExtraction}, interpolates two of its components on the
 * onset-pitch plane and reports every value.
 * <p>
 * Departures (sources/FIELD-EXTRACTION-SPEC.md): D1 units (performed Scores count quarters at
 * M.M. 120), D2 matching by pitch in onset order, D9 the interpolation on the E-H plane of the
 * regular notes only, D10 colours, D11 the view plane.
 */
public final class FieldExtractions {

    /** The letters of the coordinates of a note, in the order of the Note form. */
    public static final String LETTERS = "EHLD";

    /** The outcome of an extraction. */
    public static final class Report {
        private final FieldExtraction.Result result;
        private final FieldExtraction.Interpolation interpolation;
        private final double[][] scorePoints;
        private final int[] components;
        private final String text;

        Report(FieldExtraction.Result result, FieldExtraction.Interpolation interpolation, double[][] scorePoints,
               int[] components, String text) {
            this.result = result;
            this.interpolation = interpolation;
            this.scorePoints = scorePoints;
            this.components = components;
            this.text = text;
        }

        /** The discrete field, one vector per score note in onset order. */
        public FieldExtraction.Result result() { return result; }

        /** The interpolated field of the two shown components on (E, H); null without regular notes. */
        public FieldExtraction.Interpolation interpolation() { return interpolation; }

        /** Each score note as (E, H, L, D), in the order of {@link #result()}. */
        public double[][] scoreNotes() {
            double[][] c = new double[scorePoints.length][];
            for (int i = 0; i < c.length; i++) {
                c[i] = scorePoints[i].clone();
            }
            return c;
        }

        /** The positions, within the vectors, of the two shown components. */
        public int[] components() { return components.clone(); }

        public String text() { return text; }
    }

    private FieldExtractions() { /* static utility */ }

    /**
     * Reads coordinate letters (E onset, H pitch, L loudness, D duration), ignoring spaces and
     * case, and returns their positions in EHLD.
     */
    public static int[] parseCoordinates(String s) {
        String t = s == null ? "" : s.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        if (t.isEmpty()) {
            throw new IllegalArgumentException("There are no coordinates; give letters from E, H, L, D.");
        }
        int[] r = new int[t.length()];
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            int k = LETTERS.indexOf(c);
            if (k < 0) {
                throw new IllegalArgumentException("Unknown coordinate " + c + "; give letters from E, H, L, D.");
            }
            if (t.indexOf(c) != i) {
                throw new IllegalArgumentException("The coordinate " + c + " appears twice.");
            }
            r[i] = k;
        }
        return r;
    }

    /**
     * Extracts the field.
     *
     * @param score       the zero-addressed Score
     * @param performance the zero-addressed performed Score, one note for each score note
     * @param coordinates letters of the space the field lives in, for example "EHLD"
     * @param components  two of those letters, the components drawn and interpolated
     * @param settings    the basis search settings
     * @param margin      the frame margin of the interpolation (D9), in E and H units
     */
    public static Report analyze(Denotator score, Denotator performance, String coordinates, String components,
                                 FieldExtraction.Settings settings, double margin) {
        int[] coords = parseCoordinates(coordinates);
        int[] comps = parseCoordinates(components);
        if (comps.length != 2) {
            throw new IllegalArgumentException("Give two components to show, not " + comps.length + ".");
        }
        int[] shown = new int[2];
        for (int k = 0; k < 2; k++) {
            shown[k] = -1;
            for (int i = 0; i < coords.length; i++) {
                if (coords[i] == comps[k]) {
                    shown[k] = i;
                }
            }
            if (shown[k] < 0) {
                throw new IllegalArgumentException("The component " + LETTERS.charAt(comps[k])
                        + " is not among the coordinates " + letters(coords) + ".");
            }
        }
        double[][] s = points(score, "score"), p = points(performance, "performance");
        int[] m = FieldExtraction.matchByPitch(s, p, 1, 0, 3);
        double[][] sx = new double[s.length][coords.length], px = new double[s.length][coords.length];
        for (int i = 0; i < s.length; i++) {
            for (int k = 0; k < coords.length; k++) {
                sx[i][k] = s[i][coords[k]];
                px[i][k] = p[m[i]][coords[k]];
            }
        }
        FieldExtraction.Result r = FieldExtraction.extract(sx, px, settings);
        List<double[]> ip = new ArrayList<double[]>(), iv = new ArrayList<double[]>();
        for (FieldExtraction.Vector v : r.vectors()) {
            if (v.getCase() == Case.REGULAR) {
                double[] z = v.value();
                ip.add(new double[] { s[v.index][0], s[v.index][1] });
                iv.add(new double[] { z[shown[0]], z[shown[1]] });
            }
        }
        FieldExtraction.Interpolation f = ip.isEmpty() ? null
            : new FieldExtraction.Interpolation(ip.toArray(new double[0][]), iv.toArray(new double[0][]), margin);
        return new Report(r, f, s, shown, text(r, f, s, p, m, coords, comps, settings, margin));
    }

    private static double[][] points(Denotator d, String what) {
        if (d == null) {
            throw new IllegalArgumentException("The " + what + " is null.");
        }
        if (!d.nullAddressed()) {
            throw new IllegalArgumentException("The " + what + " has address " + d.getAddress()
                    + "; field extraction needs a zero-addressed Score.");
        }
        List<ScoreNote> n = ScoreNote.fromScore(d);
        if (n.isEmpty()) {
            throw new IllegalArgumentException("The " + what + " has no notes.");
        }
        double[][] r = new double[n.size()][];
        for (int i = 0; i < r.length; i++) {
            ScoreNote x = n.get(i);
            r[i] = new double[] { x.onset, x.pitch.doubleValue(), x.loudness, x.duration };
        }
        return r;
    }

    private static String letters(int[] c) {
        StringBuilder b = new StringBuilder();
        for (int k : c) {
            b.append(LETTERS.charAt(k));
        }
        return b.toString();
    }

    /** "12 notes: 12 regular", with the other cases only when they occur. */
    public static String summary(FieldExtraction.Result r) {
        StringBuilder b = new StringBuilder();
        b.append(r.vectors().size()).append(" notes: ").append(r.count(Case.REGULAR)).append(" regular");
        if (r.count(Case.DIRECTION) > 0) {
            b.append(", ").append(r.count(Case.DIRECTION)).append(" direction only");
        }
        if (r.count(Case.NONE) > 0) {
            b.append(", ").append(r.count(Case.NONE)).append(" no information");
        }
        return b.toString();
    }

    private static String text(FieldExtraction.Result r, FieldExtraction.Interpolation f, double[][] s, double[][] p,
                               int[] m, int[] coords, int[] comps, FieldExtraction.Settings st, double margin) {
        StringBuilder b = new StringBuilder();
        String c = letters(coords);
        b.append("Field extraction (Topos of Music II, \u00a747.3): ").append(r.vectors().size())
         .append(" notes on the coordinates ").append(c).append(".\n");
        b.append("Performed values count quarters at M.M. 120, two per second; the diagonal (1, ..., 1) means"
                 + " played at M.M. 120 with nothing shaped.\n");
        b.append(r.count(Case.REGULAR)).append(" regular, ").append(r.count(Case.DIRECTION)).append(" direction only, ")
         .append(r.count(Case.NONE)).append(" no information; ").append(r.constructedNotes())
         .append(" notes with a constructed basis vector.\n");
        b.append("Basis search: at most ").append(st.getMaxNeighbors()).append(" nearest notes");
        if (!Double.isInfinite(st.getMaxDist())) {
            b.append(" within distance ").append(f(st.getMaxDist()));
        }
        b.append(", weights ").append(f(st.getLocalityWeight())).append(" (locality) and ")
         .append(f(st.getOrthogonalityWeight())).append(" (orthogonality).\n");
        if (f == null) {
            b.append("No regular notes, so there is no interpolated field.\n");
        }
        else {
            double[] lo = f.frameLow(), hi = f.frameHigh();
            b.append("Interpolated field of ").append(LETTERS.charAt(comps[0])).append(" and ")
             .append(LETTERS.charAt(comps[1])).append(" on onset and pitch, frame E ").append(f(lo[0])).append(" to ")
             .append(f(hi[0])).append(", H ").append(f(lo[1])).append(" to ").append(f(hi[1])).append(" (margin ")
             .append(f(margin)).append(").\n");
        }
        b.append("Each note: score and performed values (").append(c).append("), case, vector, basis notes"
                 + " (c for a constructed vector), cost.\n");
        for (FieldExtraction.Vector v : r.vectors()) {
            int i = v.index;
            b.append(String.format(Locale.ROOT, "note %d: ", i + 1));
            b.append(tuple(pick(s[i], coords))).append(" -> ").append(tuple(pick(p[m[i]], coords))).append("; ");
            switch (v.getCase()) {
                case REGULAR:
                    b.append("regular, Z = ").append(tuple(v.value()));
                    break;
                case DIRECTION:
                    b.append("direction only, ").append(tuple(v.value()));
                    break;
                default:
                    b.append("no information");
            }
            b.append("; basis ");
            int[] nb = v.basis().neighbors();
            for (int k = 0; k < nb.length; k++) {
                b.append(k == 0 ? "" : ", ").append(nb[k] < 0 ? "c" : String.valueOf(nb[k] + 1));
            }
            b.append("; cost ").append(f(v.basis().cost())).append('\n');
        }
        return b.toString();
    }

    private static double[] pick(double[] x, int[] coords) {
        double[] r = new double[coords.length];
        for (int k = 0; k < coords.length; k++) {
            r[k] = x[coords[k]];
        }
        return r;
    }

    private static String tuple(double[] x) {
        StringBuilder b = new StringBuilder("(");
        for (int k = 0; k < x.length; k++) {
            b.append(k == 0 ? "" : ", ").append(f(x[k]));
        }
        return b.append(')').toString();
    }

    private static String f(double x) {
        return String.format(Locale.ROOT, "%.4f", x == 0 ? 0 : x);
    }

    /**
     * D10: the hue of a vector (x, y) of the two shown components, green for the diagonal,
     * turning once around the colour circle per cycle as the angle turns once.
     */
    public static double hue(double x, double y, double cycles) {
        double h = 1 / 3.0 + cycles * (Math.atan2(y, x) - Math.PI / 4) / (2 * Math.PI);
        h = h % 1;
        return h < 0 ? h + 1 : h;
    }

    /** D10: brightness 0.2 + 0.8 min(1, length / largest length). */
    public static double brightness(double length, double largest) {
        return largest > 0 ? 0.2 + 0.8 * Math.min(1, length / largest) : 0.2;
    }
}
