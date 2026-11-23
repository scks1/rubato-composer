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

package org.rubato.rubettes.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.rubato.io.ScoreNote;
import org.rubato.math.global.ChordClasses;
import org.rubato.math.global.GlobalComposition;
import org.rubato.math.global.Interpretations;
import org.rubato.math.global.LocalComposition;
import org.rubato.math.global.NerveDenotators;
import org.rubato.math.global.SimplicialComplex;
import org.rubato.math.yoneda.Denotator;

/**
 * Headless access to the nerve of a pitch-class interpretation of a Score
 * (Mazzola, <i>The Topos of Music</i>, &sect;13.2.1, p. 253; &sect;14.5,
 * pp. 282&ndash;284). The pitch classes of the score's notes form the support
 * in PiMod12; the covering is either
 * <ul>
 * <li>the third chains X<sup>(k)</sup> of a seven-tone scale (Ex. 21,
 * p. 275), k = 1 to 4, or</li>
 * <li>the chords by onset: one covering set per distinct chord, holding the
 * pitch classes sounding at an onset and named by its onsets (t0/3).
 * This covering is this implementation's choice, not the book's (GLOBAL-SPEC P6).</li>
 * </ul>
 * The report gives the charts, the nerve's face counts, Euler
 * characteristic and integral homology, the boundary and orientability
 * when the nerve is a pseudomanifold, and the App. N.1 class weight of
 * every simplex.
 */
public final class Nerves {

    private Nerves() { /* static utility */ }

    /** The two coverings. */
    public enum Mode { THIRD_CHAINS, CHORDS_BY_ONSET }

    /** What to compute. */
    public static final class Settings {
        public final Mode mode;
        /** Chart size for third chains, 1 to 4. */
        public final int k;
        /** Pitch class of degree I for third chains, or -1 for the first note's class. */
        public final int degreeOne;

        public Settings(Mode mode, int k, int degreeOne) {
            if (k < 1 || k > 4) {
                throw new IllegalArgumentException("The chart size must be 1 to 4, got " + k);
            }
            if (degreeOne < -1 || degreeOne > 11) {
                throw new IllegalArgumentException("Degree I must be a pitch class 0 to 11 or -1, got " + degreeOne);
            }
            this.mode = mode;
            this.k = k;
            this.degreeOne = degreeOne;
        }

        /** Triads of the scale, degree I at the first note. */
        public static Settings defaults() {
            return new Settings(Mode.THIRD_CHAINS, 3, -1);
        }
    }

    /** The result of an analysis. */
    public static final class Report {
        private final GlobalComposition composition;
        private final SimplicialComplex nerve;
        private final String heading;
        private Denotator denotator;

        Report(GlobalComposition composition, String heading) {
            this.composition = composition;
            this.nerve = composition.nerve();
            this.heading = heading;
        }

        public GlobalComposition getComposition() { return composition; }

        public SimplicialComplex getNerve() { return nerve; }
        /**
         * The nerve as an anonymous PitchClassNerve denotator, a denotator of
         * G<sub>3</sub> in Topos I &sect;13.4.1 (p. 259); see
         * {@link NerveDenotators}.
         */
        public Denotator getDenotator() {
            if (denotator == null) {
                denotator = NerveDenotators.encode(composition);
            }
            return denotator;
        }

        /** A plain-text account of the nerve. */
        public String text() {
            StringBuilder b = new StringBuilder();
            b.append(heading).append('\n');
            b.append("Support: ").append(composition.getSupport()).append('\n');
            b.append("Charts:\n");
            for (int i = 0; i < composition.coveringSize(); i++) {
                b.append("  ").append(composition.names().get(i)).append(" = ")
                 .append(composition.coveringComposition(i)).append('\n');
            }
            b.append("Nerve: ").append(nerve.vertexCount()).append(" vertices, dimension ")
             .append(nerve.dimension()).append('\n');
            b.append("  f-vector ").append(Arrays.toString(nerve.fVector())).append('\n');
            b.append("  Euler characteristic ").append(nerve.eulerCharacteristic()).append('\n');
            b.append("  homology H_0.. ").append(nerve.homology()).append('\n');
            if (nerve.isPseudomanifold() && nerve.dimension() >= 1) {
                SimplicialComplex bd = nerve.boundary();
                b.append("  pseudomanifold, ").append(nerve.isOrientable() ? "orientable" : "not orientable").append('\n');
                if (bd.size() == 0) {
                    b.append("  no boundary\n");
                }
                else {
                    List<Integer> vs = bd.nonIsolatedVertices();
                    List<Integer> cyc = vs.isEmpty() ? null : bd.cycle(vs.get(0));
                    if (cyc != null && nerve.dimension() == 2) {
                        StringBuilder c = new StringBuilder();
                        for (int v : cyc) {
                            c.append(nerve.label(v)).append(" - ");
                        }
                        c.append(nerve.label(cyc.get(0)));
                        b.append("  boundary cycle ").append(c).append('\n');
                    }
                    else {
                        b.append("  boundary ").append(bd.simplexes(nerve.dimension() - 1).size())
                         .append(" faces of dimension ").append(nerve.dimension() - 1).append('\n');
                    }
                }
            }
            else {
                b.append("  not a pseudomanifold\n");
            }
            b.append("Maximal simplexes:\n");
            for (int[] s : nerve.maximalSimplexes()) {
                b.append("  ").append(nerve.format(s)).append('\n');
            }
            b.append("Class weights (Topos IV App. N.1 numbers; - where it has none here):\n");
            Map<Integer, List<String>> byDim = new TreeMap<Integer, List<String>>();
            for (Map.Entry<List<Integer>, Integer> e : ChordClasses.classWeights(composition).entrySet()) {
                int[] s = new int[e.getKey().size()];
                for (int i = 0; i < s.length; i++) {
                    s[i] = e.getKey().get(i);
                }
                List<String> l = byDim.get(s.length - 1);
                if (l == null) {
                    l = new ArrayList<String>();
                    byDim.put(s.length - 1, l);
                }
                l.add(nerve.format(s) + ":" + (e.getValue() == null ? "-" : e.getValue()));
            }
            for (Map.Entry<Integer, List<String>> e : byDim.entrySet()) {
                b.append("  dim ").append(e.getKey()).append(": ").append(join(e.getValue())).append('\n');
            }
            return b.toString();
        }
    }

    /** Analyses a Score with the default settings. */
    public static Report analyze(Denotator score) {
        return analyze(score, Settings.defaults());
    }

    /**
     * Analyses a zero-addressed Score.
     *
     * @throws IllegalArgumentException if the denotator is not a zero-addressed
     *         Score, has no notes, has a pitch that is not a whole semitone, or
     *         does not fit the chosen covering
     */
    public static Report analyze(Denotator score, Settings settings) {
        if (score == null) {
            throw new IllegalArgumentException("The score is null.");
        }
        if (!score.nullAddressed()) {
            throw new IllegalArgumentException("The score has address " + score.getAddress()
                    + "; Nerve needs a zero-addressed Score. Evaluate it first, for example with atNull().");
        }
        return analyze(ScoreNote.fromScore(score), settings);
    }

    /** Analyses a list of notes, as {@link #analyze(Denotator, Settings)} does. */
    public static Report analyze(List<ScoreNote> notes, Settings settings) {
        if (notes.isEmpty()) {
            throw new IllegalArgumentException("The score has no notes.");
        }
        List<ScoreNote> sorted = new ArrayList<ScoreNote>(notes);
        java.util.Collections.sort(sorted);
        TreeSet<Integer> classes = new TreeSet<Integer>();
        Map<Double, TreeSet<Integer>> byOnset = new TreeMap<Double, TreeSet<Integer>>();
        for (ScoreNote n : sorted) {
            if (!n.pitch.isIntegral()) {
                throw new IllegalArgumentException("The pitch " + n.pitch + " at onset " + n.onset + " is not a whole semitone.");
            }
            int pc = Math.floorMod(n.pitch.getNumerator(), 12);
            classes.add(pc);
            TreeSet<Integer> c = byOnset.get(n.onset);
            if (c == null) {
                c = new TreeSet<Integer>();
                byOnset.put(n.onset, c);
            }
            c.add(pc);
        }
        int[] scale = toArray(classes);
        if (settings.mode == Mode.THIRD_CHAINS) {
            if (scale.length != 7) {
                throw new IllegalArgumentException("Third chains need exactly seven pitch classes; the score has "
                        + scale.length + ": " + Arrays.toString(scale));
            }
            int first = settings.degreeOne >= 0 ? settings.degreeOne
                    : Math.floorMod(sorted.get(0).pitch.getNumerator(), 12);
            if (!classes.contains(first)) {
                throw new IllegalArgumentException("Degree I " + first + " is not among the score's pitch classes "
                        + Arrays.toString(scale));
            }
            return thirdChains(scale, first, settings.k);
        }
        // chords by onset: identical chords form one covering set
        Map<List<Integer>, List<String>> chords = new LinkedHashMap<List<Integer>, List<String>>();
        for (Map.Entry<Double, TreeSet<Integer>> e : byOnset.entrySet()) {
            List<Integer> c = new ArrayList<Integer>(e.getValue());
            List<String> names = chords.get(c);
            if (names == null) {
                names = new ArrayList<String>();
                chords.put(c, names);
            }
            names.add(formatOnset(e.getKey()));
        }
        if (chords.size() > SimplicialComplex.MAX_VERTICES) {
            throw new IllegalArgumentException("The score has " + chords.size() + " distinct chords; Nerve handles at most "
                    + SimplicialComplex.MAX_VERTICES);
        }
        LocalComposition x = LocalComposition.ofClasses(12, scale);
        List<String> names = new ArrayList<String>();
        List<int[]> cover = new ArrayList<int[]>();
        for (Map.Entry<List<Integer>, List<String>> e : chords.entrySet()) {
            names.add("t" + joinWith(e.getValue(), "/"));
            int[] c = new int[e.getKey().size()];
            for (int i = 0; i < c.length; i++) {
                c[i] = x.indexOf(e.getKey().get(i));
            }
            cover.add(c);
        }
        return new Report(GlobalComposition.interpretation(x, names, cover),
                "Nerve of the chords by onset (" + cover.size() + " covering sets)");
    }

    /** The third-chain interpretation X(k) of a seven-tone scale with degree I at {@code first}. */
    public static Report thirdChains(int[] scale, int first, int k) {
        GlobalComposition g = Interpretations.thirdChains(12, scale, first, k);
        return new Report(g, "Nerve of the third chains X(" + k + "), degree I = " + first);
    }

    private static String formatOnset(double t) {
        return t == Math.rint(t) ? Long.toString((long) t) : Double.toString(t);
    }

    private static String join(List<String> l) {
        return joinWith(l, ", ");
    }

    private static String joinWith(List<String> l, String sep) {
        StringBuilder b = new StringBuilder();
        for (String s : l) {
            if (b.length() > 0) {
                b.append(sep);
            }
            b.append(s);
        }
        return b.toString();
    }

    private static int[] toArray(TreeSet<Integer> s) {
        int[] a = new int[s.size()];
        int i = 0;
        for (int x : s) {
            a[i++] = x;
        }
        return a;
    }
}
