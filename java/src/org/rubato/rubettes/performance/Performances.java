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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import org.rubato.io.ScoreNote;
import org.rubato.math.performance.LPS;
import org.rubato.math.performance.PerfNote;
import org.rubato.math.performance.PerformedNote;
import org.rubato.math.performance.Stemma;
import org.rubato.math.performance.TempoCurve;
import org.rubato.math.performance.TempoCurves;
import org.rubato.math.yoneda.Denotator;

/**
 * Headless performance of a Score through a small stemma (Mazzola, <i>The Topos of Music
 * II</i>, chapters 33-39). Tempo performance: a mother with a linear or exponential tempo
 * curve from the first onset to the last note end, optionally with the per-bar rubato of
 * Example 52 (p. 624) in one voice or in all. Scalar performance: a mother at constant
 * tempo, a loudness weight Lambda(E) that moves linearly in ln from a start factor to an end
 * factor (&sect;39.7.1, eq. (39.43)), and the articulation dilation of Example 51 (p. 591).
 * <p>
 * Units (departures D1-D3 of sources/PERFORMANCE-SPEC.md): tempi are quarters per minute;
 * the performed Score counts quarters at the reference tempo 120, two per second, which is
 * the default tempo of ScoreMidi; performed loudness is rounded and clamped to 1-127.
 * At M.M. 120 a scalar performance leaves onsets in place, so it can follow a tempo
 * performance.
 */
public final class Performances {

    /** The shape of the tempo curve between the start and end tempi. */
    public enum Curve { LINEAR, EXPONENTIAL }

    /** Performance settings; build them with {@link #tempo} or {@link #scalar}. */
    public static final class Settings {
        final boolean scalar;
        final double tempoStart, tempoEnd;
        final Curve curve;
        final double barLength, rubatoDepth;
        final int rubatoVoice;
        final double loudStart, loudEnd, articulation;

        private Settings(boolean scalar, double tempoStart, double tempoEnd, Curve curve, double barLength,
                         double rubatoDepth, int rubatoVoice, double loudStart, double loudEnd, double articulation) {
            this.scalar = scalar;
            this.tempoStart = tempoStart;
            this.tempoEnd = tempoEnd;
            this.curve = curve;
            this.barLength = barLength;
            this.rubatoDepth = rubatoDepth;
            this.rubatoVoice = rubatoVoice;
            this.loudStart = loudStart;
            this.loudEnd = loudEnd;
            this.articulation = articulation;
        }

        /**
         * Tempo performance.
         *
         * @param start      the tempo at the first onset, M.M.
         * @param end        the tempo at the last note end, M.M.
         * @param curve      linear or exponential between them
         * @param barLength  bar length in quarters for the rubato, 0 for none
         * @param depth      rubato depth in [0, 0.9): inner vertex factors 1 + depth and 1 - depth
         * @param voice      the voice that plays the rubato, -1 for all voices
         */
        public static Settings tempo(double start, double end, Curve curve, double barLength, double depth, int voice) {
            return new Settings(false, start, end, curve, barLength, depth, voice, 1, 1, 1);
        }

        /**
         * Scalar performance.
         *
         * @param tempo        the constant tempo, M.M.; 120 keeps onsets in place
         * @param loudStart    the loudness factor e^Lambda at the first onset
         * @param loudEnd      the loudness factor at the last note end
         * @param articulation the duration dilation lambda of Example 51
         */
        public static Settings scalar(double tempo, double loudStart, double loudEnd, double articulation) {
            return new Settings(true, tempo, tempo, Curve.LINEAR, 0, 0, -1, loudStart, loudEnd, articulation);
        }

        void validate() {
            positive("start tempo", tempoStart);
            positive("end tempo", tempoEnd);
            if (tempoStart > 2000 || tempoEnd > 2000) {
                throw new IllegalArgumentException("tempi above M.M. 2000 are refused");
            }
            if (!(barLength >= 0)) {
                throw new IllegalArgumentException("bar length must not be negative: " + barLength);
            }
            if (!(rubatoDepth >= 0 && rubatoDepth < 0.9)) {
                throw new IllegalArgumentException("rubato depth must lie in [0, 0.9): " + rubatoDepth);
            }
            positive("start loudness factor", loudStart);
            positive("end loudness factor", loudEnd);
            positive("articulation", articulation);
        }

        private static void positive(String what, double v) {
            if (!(v > 0) || Double.isInfinite(v)) {
                throw new IllegalArgumentException(what + " must be positive: " + v);
            }
        }
    }

    /** The outcome of a performance. */
    public static final class Result {
        private final List<ScoreNote> notes;
        private final Stemma stemma;
        private final double physicalDuration;
        private final String report;

        Result(List<ScoreNote> notes, Stemma stemma, double physicalDuration, String report) {
            this.notes = notes;
            this.stemma = stemma;
            this.physicalDuration = physicalDuration;
            this.report = report;
        }

        /** The performed notes, one for each input note, in input order. */
        public List<ScoreNote> getNotesInInputOrder() { return notes; }

        /** The performed Score. */
        public Denotator getPerformedScore() { return ScoreNote.toScore(notes); }

        /** The stemma that performed the piece, null for an empty piece. */
        public Stemma getStemma() { return stemma; }

        /** Seconds from the first performed onset to the last performed note end. */
        public double getPhysicalDuration() { return physicalDuration; }

        public String getReport() { return report; }
    }

    private Performances() { /* static utility */ }

    /** Performs a zero-addressed Score. */
    public static Result perform(Denotator score, Settings settings) {
        if (score == null) {
            throw new IllegalArgumentException("The score is null.");
        }
        if (!score.nullAddressed()) {
            throw new IllegalArgumentException("The score has address " + score.getAddress()
                    + "; performance needs a zero-addressed Score. Evaluate it first, for example with atNull().");
        }
        return perform(ScoreNote.fromScore(score), settings);
    }

    private static String f(double x) {
        return String.format(Locale.ROOT, "%.4f", x);
    }

    /** Performs a list of notes. */
    public static Result perform(List<ScoreNote> input, Settings s) {
        s.validate();
        if (input.isEmpty()) {
            return new Result(new ArrayList<ScoreNote>(), null, 0, "The score is empty; nothing to perform.");
        }
        List<PerfNote> kernel = new ArrayList<PerfNote>();
        double e0 = Double.POSITIVE_INFINITY, e1 = Double.NEGATIVE_INFINITY;
        for (ScoreNote n : input) {
            kernel.add(new PerfNote(n.onset, n.pitch.doubleValue(), n.loudness, n.duration, n.voice));
            e0 = Math.min(e0, n.onset);
            e1 = Math.max(e1, n.onset + n.duration);
        }
        StringBuilder r = new StringBuilder();
        TempoCurve t = curve(s, e0, e1, r);
        LPS mother = LPS.mother(kernel, t);
        if (s.scalar) {
            LPS leaf = mother;
            if (s.loudStart != 1 || s.loudEnd != 1) {
                final double ls = Math.log(s.loudStart), le = Math.log(s.loudEnd);
                final double a = e0, span = e1 - e0;
                DoubleUnaryOperator lambda = span > 0 ? x -> ls + (le - ls) * (x - a) / span : x -> ls;
                leaf = leaf.loudnessOperator(lambda, 1, 0, "dynamics");
                r.append("Dynamics: Lambda(E) from ln ").append(f(s.loudStart)).append(" to ln ")
                 .append(f(s.loudEnd)).append(", linear in E (39.43).\n");
            }
            if (s.articulation != 1) {
                leaf = leaf.articulationOperator(s.articulation, "dilation");
                r.append("Articulation: durations dilated by ").append(f(s.articulation)).append(" (Example 51).\n");
            }
        }
        else if (s.barLength > 0 && s.rubatoDepth > 0) {
            List<Double> lines = new ArrayList<Double>();
            for (double x = e0; ; x += s.barLength) {
                lines.add(x);
                if (x >= e1) {
                    break;
                }
            }
            double[] bars = new double[lines.size()];
            for (int i = 0; i < bars.length; i++) {
                bars[i] = lines.get(i);
            }
            double[] profile = { 1 + s.rubatoDepth, 1 - s.rubatoDepth };
            LPS target = mother;
            if (s.rubatoVoice >= 0) {
                target = null;
                for (LPS d : mother.splitByVoice()) {
                    if (d.getKernel().get(0).voice == s.rubatoVoice) {
                        target = d;
                    }
                }
                if (target == null) {
                    throw new IllegalArgumentException("the score has no voice " + s.rubatoVoice + " for the rubato");
                }
            }
            target.barRubato(bars, profile);
            r.append("Rubato: ").append(bars.length - 1).append(" bars of ").append(f(s.barLength))
             .append(" quarters in ").append(s.rubatoVoice < 0 ? "all voices" : "voice " + s.rubatoVoice)
             .append(", inner tempo factors ").append(f(profile[0])).append(", ").append(f(profile[1]))
             .append(", each bar kept as long as the mother's by Cor. 21 (Example 52).\n");
        }
        Stemma stemma = new Stemma(mother);
        List<PerformedNote> performed = stemma.perform();
        Map<PerfNote, PerformedNote> byNote = new IdentityHashMap<PerfNote, PerformedNote>();
        double first = Double.POSITIVE_INFINITY, last = Double.NEGATIVE_INFINITY;
        for (PerformedNote p : performed) {
            byNote.put(p.getSymbolic(), p);
            first = Math.min(first, p.onset);
            last = Math.max(last, p.onset + p.duration);
        }
        List<ScoreNote> out = new ArrayList<ScoreNote>();
        int clamped = 0;
        for (int i = 0; i < input.size(); i++) {
            ScoreNote n = input.get(i);
            PerformedNote p = byNote.get(kernel.get(i));
            long v = Math.round(p.loudness);
            int loud = (int) Math.max(1, Math.min(127, v));
            if (loud != v) {
                clamped++;
            }
            out.add(new ScoreNote(2 * p.onset, n.pitch, loud, 2 * p.duration, n.voice));
        }
        r.append("Stemma (Def. 107): ").append(stemma.size()).append(" nodes, ").append(stemma.leaves().size())
         .append(stemma.leaves().size() == 1 ? " leaf.\n" : " leaves.\n");
        tree(mother, "  ", r);
        r.append("Performed ").append(input.size()).append(" notes in ").append(f(last - first)).append(" s");
        r.append(" (score span ").append(f(e1 - e0)).append(" quarters).\n");
        if (clamped > 0) {
            r.append(clamped).append(" loudness values clamped to 1-127.\n");
        }
        r.append("Output: onsets and durations in quarters at M.M. 120 (2 per second).\n");
        return new Result(out, stemma, last - first, r.toString());
    }

    private static void tree(LPS n, String indent, StringBuilder r) {
        r.append(indent).append(n.getName()).append(": ").append(n.getOperator()).append(", ")
         .append(n.getKernel().size()).append(" notes\n");
        for (LPS d : n.getDaughters()) {
            tree(d, indent + "  ", r);
        }
    }

    private static TempoCurve curve(Settings s, double e0, double e1, StringBuilder r) {
        double span = e1 - e0;
        if (span <= 0 || s.tempoStart == s.tempoEnd) {
            r.append("Tempo: constant M.M. ").append(f(s.tempoStart)).append(".\n");
            return TempoCurves.constant(s.tempoStart);
        }
        if (s.curve == Curve.EXPONENTIAL) {
            double k = Math.log(s.tempoEnd / s.tempoStart) / span;
            r.append("Tempo: exponential from M.M. ").append(f(s.tempoStart)).append(" to ").append(f(s.tempoEnd))
             .append(", closed-form integral.\n");
            return TempoCurves.exponential(s.tempoStart * Math.exp(-k * e0), k);
        }
        double q1 = (s.tempoEnd - s.tempoStart) / span;
        r.append("Tempo: linear from M.M. ").append(f(s.tempoStart)).append(" to ").append(f(s.tempoEnd))
         .append(", logarithmic onsets in closed form.\n");
        return TempoCurves.linear(s.tempoStart - q1 * e0, q1);
    }
}
