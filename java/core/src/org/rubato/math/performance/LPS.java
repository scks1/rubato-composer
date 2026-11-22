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
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;

/**
 * A local performance score, reduced to what this package performs (Mazzola, <i>The Topos
 * of Music II</i>, &sect;35.4, eqs. (35.2)-(35.20), pp. 595-597): a kernel of symbolic
 * notes, a tempo curve, a loudness weight Lambda(E) with an affine map a L + b (&sect;39.7.1),
 * the operator that produced it, its mother and its daughters. Operators create daughters:
 * split by voice or onset interval, the tempo operator (39.32) with or without duration
 * preservation, the bar rubato of Example 52 and the loudness basis-pianola operator.
 * A daughter inherits the mother's curves and changes one of them.
 */
public final class LPS {

    private static final DoubleUnaryOperator ZERO = x -> 0;

    private final String name;
    private final List<PerfNote> kernel;
    private final TempoCurve tempo;
    private final DoubleUnaryOperator lambda;
    private final double a;
    private final double b;
    private double articulation = 1;
    private final String operator;
    private LPS mother;
    private final List<LPS> daughters = new ArrayList<LPS>();
    private TempoOperator.Preservation preservation;

    private LPS(String name, List<PerfNote> kernel, TempoCurve tempo, DoubleUnaryOperator lambda, double a, double b,
                String operator) {
        this.name = name;
        this.kernel = Collections.unmodifiableList(new ArrayList<PerfNote>(kernel));
        this.tempo = tempo;
        this.lambda = lambda;
        this.a = a;
        this.b = b;
        this.operator = operator;
    }

    /** A mother LPS with the given kernel and tempo, loudness weight 0 and identity loudness map. */
    public static LPS mother(List<PerfNote> kernel, TempoCurve tempo) {
        return new LPS("Mother", kernel, tempo, ZERO, 1, 0, "mother");
    }

    public String getName() { return name; }

    public List<PerfNote> getKernel() { return kernel; }

    public TempoCurve getTempo() { return tempo; }

    public String getOperator() { return operator; }

    public LPS getMother() { return mother; }

    public List<LPS> getDaughters() { return Collections.unmodifiableList(daughters); }

    /** The duration dilation lambda of Example 51 (p. 591); 1 for the mother. */
    public double getArticulation() { return articulation; }

    /** The duration-preservation record of a {@link #tempoOperatorPreserving} daughter, else null. */
    public TempoOperator.Preservation getPreservation() { return preservation; }

    /** Adds a daughter. Used by the operators; public so that invalid graphs can be tested. */
    public void addDaughter(LPS d) {
        daughters.add(d);
        if (d.mother == null) {
            d.mother = this;
        }
    }

    private LPS daughter(String n, List<PerfNote> k, TempoCurve t, DoubleUnaryOperator l, double na, double nb, String op) {
        LPS d = new LPS(n, k, t, l, na, nb, op);
        d.articulation = articulation;
        addDaughter(d);
        return d;
    }

    /** Omega1 of Fig. 38.5: one daughter per voice, in voice order. */
    public List<LPS> splitByVoice() {
        TreeMap<Integer, List<PerfNote>> by = new TreeMap<Integer, List<PerfNote>>();
        for (PerfNote n : kernel) {
            by.computeIfAbsent(n.voice, v -> new ArrayList<PerfNote>()).add(n);
        }
        List<LPS> r = new ArrayList<LPS>();
        for (Integer v : by.keySet()) {
            r.add(daughter(name + "/v" + v, by.get(v), tempo, lambda, a, b, "split voice " + v));
        }
        return r;
    }

    /** Omega5 of Fig. 38.5: daughters for the onset intervals between the cuts, all nonempty ones kept. */
    public List<LPS> splitByOnset(double[] cuts) {
        double[] c = cuts.clone();
        java.util.Arrays.sort(c);
        List<List<PerfNote>> parts = new ArrayList<List<PerfNote>>();
        for (int i = 0; i <= c.length; i++) {
            parts.add(new ArrayList<PerfNote>());
        }
        for (PerfNote n : kernel) {
            int i = 0;
            while (i < c.length && n.onset >= c[i]) {
                i++;
            }
            parts.get(i).add(n);
        }
        List<LPS> r = new ArrayList<LPS>();
        for (int i = 0; i < parts.size(); i++) {
            if (!parts.get(i).isEmpty()) {
                r.add(daughter(name + "/" + (char) ('A' + i), parts.get(i), tempo, lambda, a, b, "split onset " + i));
            }
        }
        return r;
    }

    /** (39.32): a daughter with tempo w T. */
    public LPS tempoOperator(DoubleUnaryOperator w, String label) {
        return daughter(name + "/" + label, kernel, TempoOperator.apply(tempo, w), lambda, a, b, "tempo " + label);
    }

    /** (39.32) with the duration of [e0, e1] kept by the iteration of p. 653-654. */
    public LPS tempoOperatorPreserving(DoubleUnaryOperator w, double e0, double e1, double beta, double tol,
                                       int maxIter, String label) {
        TempoOperator.Preservation p = TempoOperator.preserveDuration(tempo, w, e0, e1, beta, tol, maxIter);
        LPS d = daughter(name + "/" + label, kernel, p.getCurve(), lambda, a, b, "tempo (preserving) " + label);
        d.preservation = p;
        return d;
    }

    /** Example 52 (p. 624) by Cor. 21: a per-bar rubato keeping every bar's duration (D8). */
    public LPS barRubato(double[] barLines, double[] profile) {
        BarTempo t = new BarTempo(tempo, barLines, profile);
        return daughter(name + "/rubato", kernel, t, lambda, a, b, "bar rubato");
    }

    /**
     * The loudness basis-pianola operator (&sect;39.7.1): Lambda becomes Lambda + mu (the
     * weights add, Lemma 53 (ii)), and the affine map is composed: a' = a an, b' = an b + bn.
     */
    public LPS loudnessOperator(DoubleUnaryOperator mu, double an, double bn, String label) {
        DoubleUnaryOperator l = lambda;
        return daughter(name + "/" + label, kernel, tempo, x -> l.applyAsDouble(x) + mu.applyAsDouble(x),
                a * an, an * b + bn, "loudness " + label);
    }

    /**
     * Example 51 (p. 591): a daughter whose performed durations are lambda times the
     * mother's, F_lambda(E, D) = (f(E), lambda (f(E + D) - f(E))); dilations compose by
     * multiplication.
     */
    public LPS articulationOperator(double lambda, String label) {
        if (!(lambda > 0) || Double.isInfinite(lambda)) {
            throw new IllegalArgumentException("the articulation dilation must be positive: " + lambda);
        }
        LPS d = daughter(name + "/" + label, kernel, tempo, this.lambda, a, b, "articulation " + label);
        d.articulation = articulation * lambda;
        return d;
    }

    /** The performance of one note of this LPS, with onsets measured from the symbolic onset origin. */
    PerformedNote perform(PerfNote n, double origin) {
        double on = 60 * tempo.integral(origin, n.onset);
        double du = articulation * 60 * tempo.integral(n.onset, n.onset + n.duration);
        double lo = BasisPianolaOperator.performedLoudness(lambda.applyAsDouble(n.onset), a, b, n.loudness);
        return new PerformedNote(n, this, on, du, lo);
    }

    public String toString() {
        return name + " [" + operator + ", " + kernel.size() + " notes]";
    }
}
