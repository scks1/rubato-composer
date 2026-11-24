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

package org.rubato.rubettes.voiceleading;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.rubato.io.ScoreNote;
import org.rubato.math.voiceleading.FiniteAction;
import org.rubato.math.voiceleading.GIS;
import org.rubato.math.voiceleading.VoiceLeadingSpace;
import org.rubato.math.yoneda.Denotator;

/**
 * Headless report on a finite voice-leading space (Flieder, arXiv 2512.04090,
 * section 4): its subjective and objective transformations, the GIS check of
 * Example 3.1, and the transporters between successive pitch classes or triads
 * of a Score. The departures are listed in sources/VOICE-LEADING-SPEC.md.
 */
public final class VoiceLeadings {

    /** Largest number of vertex maps the report lists one by one. */
    public static final int LIST_LIMIT = 48;

    private VoiceLeadings() { /* static utility */ }

    /** The space (PC or TRIADS), the group (T, TI or Aff), n, and the pair whose transporters are listed. */
    public static final class Settings {
        final String space;
        final String group;
        final int n;
        final int from;
        final int to;

        public Settings(String space, String group, int n, int from, int to) {
            this.space = space;
            this.group = group;
            this.n = n;
            this.from = from;
            this.to = to;
        }

        /** T/I on Z12, transporters from 0 to 4. */
        public static Settings defaults() {
            return new Settings("PC", "TI", 12, 0, 4);
        }
    }

    /** The report text, its one-line summary, and the space it describes. */
    public static final class Report {
        private final String text;
        private final String summary;
        private final VoiceLeadingSpace space;

        Report(String text, String summary, VoiceLeadingSpace space) {
            this.text = text;
            this.summary = summary;
            this.space = space;
        }

        public String text() {
            return text;
        }

        public String summary() {
            return summary;
        }

        public VoiceLeadingSpace space() {
            return space;
        }
    }

    /** The action the settings describe. */
    static FiniteAction action(Settings s) {
        if ("PC".equals(s.space)) {
            return FiniteAction.pitchClasses(s.group, s.n);
        }
        if ("TRIADS".equals(s.space)) {
            return FiniteAction.triads(s.group);
        }
        throw new IllegalArgumentException("Unknown space \"" + s.space + "\"; use PC (pitch classes) or TRIADS.");
    }

    /**
     * Reports on the space; score may be null.
     *
     * @throws IllegalArgumentException for unknown settings, a pair outside the
     *         space, a Score with an address, or a pitch that is not a whole semitone
     */
    public static Report analyze(Denotator score, Settings s) {
        FiniteAction a = action(s);
        int points = a.pointCount();
        for (int p : new int[] { s.from, s.to }) {
            if (p < 0 || p >= points) {
                throw new IllegalArgumentException("Point " + p + " is outside the space; use 0 to " + (points - 1) + ".");
            }
        }
        VoiceLeadingSpace v = new VoiceLeadingSpace(a);
        StringBuilder b = new StringBuilder();
        String groupLabel = "TI".equals(s.group) ? "T/I" : s.group;
        String where = a.isTriads() ? "the consonant triads of Z12" : "Z" + a.modulus();
        b.append("Voice-leading space: ").append(groupLabel).append(" on ").append(where)
         .append(" (Flieder, Def. 4.1 and Ex. 4.1)\n");
        b.append(points).append(" vertices, ").append(a.groupSize()).append(" group elements, ")
         .append(v.arrowCount()).append(" arrows; ").append(v.minMultiplicity()).append(" to ")
         .append(v.maxMultiplicity()).append(" arrows over each ordered pair of vertices.\n\n");

        b.append("Subjective transformations (arrows, Def. 4.2) from ").append(a.pointName(s.from)).append(" to ")
         .append(a.pointName(s.to)).append(": ").append(names(a, v.transporters(s.from, s.to))).append("\n\n");

        BigInteger sigma = v.sigmaAutomorphismCount();
        List<int[]> norm = v.normalizer();
        b.append("Objective transformations (Prop. 4.1; spec D5, D6):\n");
        b.append(sigma).append(" Sigma_VLS automorphisms = |N(F)| ").append(norm.size()).append(" x (|K| = ")
         .append(v.kernelSize()).append(")!^").append(v.imageSize()).append('\n');
        b.append("induced by conjugation (Ex. 4.3): ").append(v.conjugationInducedCount()).append(" of ").append(sigma)
         .append('\n');
        if (norm.size() <= LIST_LIMIT) {
            b.append("vertex maps (* induced by conjugation): ");
            for (int i = 0; i < norm.size(); i++) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(v.describeVertexMap(norm.get(i)));
                if (v.isConjugationVertexMap(norm.get(i))) {
                    b.append(" *");
                }
            }
            b.append('\n');
        }
        else {
            b.append("vertex maps: ").append(norm.size()).append(", not listed\n");
        }
        BigInteger bare = v.bareQuiverAutomorphismCount();
        if (bare == null) {
            b.append("bare quiver automorphisms: not computed (multiplicities ").append(v.minMultiplicity()).append(" to ")
             .append(v.maxMultiplicity()).append(" differ, D7)\n\n");
        }
        else {
            b.append("bare quiver automorphisms (Def. 4.2 read literally, D7): ").append(points).append("! x (")
             .append(v.minMultiplicity()).append("!)^").append(points * points).append(", a ")
             .append(bare.toString().length()).append("-digit number\n\n");
        }

        GIS gis = GIS.fromSpace(v);
        boolean isGIS;
        if (gis == null) {
            isGIS = false;
            b.append("GIS (Ex. 3.1): no; ").append(GIS.whyNot(v)).append('\n');
        }
        else {
            GIS.Result r = gis.check();
            isGIS = r.isGIS();
            if (isGIS) {
                b.append("GIS (Ex. 3.1): yes; int(x, y) is the one transporter from x to y, i * j = j after i (D8);"
                         + " both axioms hold\n");
            }
            else {
                b.append("GIS (Ex. 3.1): no; ").append(r.failure()).append('\n');
            }
        }
        b.append('\n');

        if (score == null) {
            b.append("No Score linked.\n");
        }
        else {
            if (!score.nullAddressed()) {
                throw new IllegalArgumentException("The score has address " + score.getAddress()
                        + "; Voice-Leading Space needs a zero-addressed Score.");
            }
            scoreSteps(ScoreNote.fromScore(score), v, b);
        }
        String summary = v.arrowCount() + " arrows, " + sigma + " objective, " + (isGIS ? "a GIS" : "not a GIS");
        return new Report(b.toString(), summary, v);
    }

    /** D10: successive pitch classes, or successive consonant triads by onset, with their transporters. */
    static void scoreSteps(List<ScoreNote> notes, VoiceLeadingSpace v, StringBuilder b) {
        FiniteAction a = v.action();
        List<ScoreNote> sorted = new ArrayList<ScoreNote>(notes);
        Collections.sort(sorted);
        for (ScoreNote n : sorted) {
            if (!n.pitch.isIntegral()) {
                throw new IllegalArgumentException("The pitch " + n.pitch + " at onset " + n.onset + " is not a whole semitone.");
            }
        }
        List<Integer> pts = new ArrayList<Integer>();
        List<Double> onsets = new ArrayList<Double>();
        List<String> skipped = new ArrayList<String>();
        if (a.isTriads()) {
            Map<Double, TreeSet<Integer>> byOnset = new TreeMap<Double, TreeSet<Integer>>();
            for (ScoreNote n : sorted) {
                TreeSet<Integer> c = byOnset.get(n.onset);
                if (c == null) {
                    c = new TreeSet<Integer>();
                    byOnset.put(n.onset, c);
                }
                c.add(Math.floorMod(n.pitch.getNumerator(), 12));
            }
            for (Map.Entry<Double, TreeSet<Integer>> e : byOnset.entrySet()) {
                int[] pcs = new int[e.getValue().size()];
                int i = 0;
                for (int pc : e.getValue()) {
                    pcs[i++] = pc;
                }
                int t = a.triadIndex(pcs);
                if (t < 0) {
                    skipped.add("onset " + onset(e.getKey()) + ": " + e.getValue().toString().replace('[', '{').replace(']', '}')
                                + " is not a consonant triad, skipped");
                }
                else {
                    pts.add(t);
                    onsets.add(e.getKey());
                }
            }
            b.append("Score: ").append(byOnset.size()).append(" onsets, ").append(pts.size()).append(" triads, ")
             .append(skipped.size()).append(" skipped\n");
        }
        else {
            for (ScoreNote n : sorted) {
                pts.add(Math.floorMod(n.pitch.getNumerator(), a.modulus()));
                onsets.add(n.onset);
            }
            b.append("Score: ").append(pts.size()).append(" notes, ").append(Math.max(0, pts.size() - 1)).append(" steps\n");
        }
        for (String sk : skipped) {
            b.append(sk).append('\n');
        }
        for (int i = 1; i < pts.size(); i++) {
            int x = pts.get(i - 1), y = pts.get(i);
            b.append("step ").append(i).append(": ").append(a.pointName(x)).append(" -> ").append(a.pointName(y))
             .append(" (onsets ").append(onset(onsets.get(i - 1))).append(" to ").append(onset(onsets.get(i)))
             .append("): ").append(names(a, v.transporters(x, y))).append('\n');
        }
    }

    private static String names(FiniteAction a, List<Integer> gs) {
        if (gs.isEmpty()) {
            return "none";
        }
        StringBuilder b = new StringBuilder();
        for (int g : gs) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(a.elementName(g));
        }
        return b.toString();
    }

    private static String onset(double t) {
        return t == Math.rint(t) ? Long.toString((long) t) : Double.toString(t);
    }
}
