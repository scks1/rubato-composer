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

package org.rubato.rubettes.counterpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rubato.io.ScoreNote;
import org.rubato.math.arith.Rational;
import org.rubato.math.counterpoint.ContrapuntalSymmetries;
import org.rubato.math.counterpoint.Dichotomy;
import org.rubato.math.counterpoint.FirstSpecies;
import org.rubato.math.counterpoint.FirstSpecies.Kind;
import org.rubato.math.counterpoint.FirstSpecies.Orientation;
import org.rubato.math.counterpoint.FirstSpecies.Step;
import org.rubato.math.counterpoint.StrongDichotomies;
import org.rubato.math.yoneda.Denotator;

/**
 * Headless entry points for first-species counterpoint on Score
 * denotators, the same computations the Counterpoint Check and Successor
 * Generator rubettes run.
 * <pre>
 * Counterpoint.Report r = Counterpoint.check(score);
 * System.out.println(r.text());
 * Denotator marked = r.flaggedScore();
 * </pre>
 * The model reduces pitches mod 12 and has no notion of register, rhythm
 * or the beginning and end of a line; it only judges each interval and
 * each succession of two intervals (CCW chapter 2; section 3.3, p. 35).
 */
public final class Counterpoint {

    private Counterpoint() { /* static utility */ }

    /** Which dichotomy, orientation and voices a check uses. Immutable. */
    public static final class Settings {

        public final Dichotomy dichotomy;
        public final Orientation orientation;
        public final int cantusVoice;
        public final int discantusVoice;

        public Settings(Dichotomy dichotomy, Orientation orientation, int cantusVoice, int discantusVoice) {
            if (cantusVoice == discantusVoice) {
                throw new IllegalArgumentException("The cantus firmus and the discantus need different voices, got "
                        + cantusVoice + " for both");
            }
            dichotomy.polarity();
            this.dichotomy = dichotomy;
            this.orientation = orientation;
            this.cantusVoice = cantusVoice;
            this.discantusVoice = discantusVoice;
        }

        /** (K/D), sweeping, cantus firmus in voice 0, discantus in voice 1. */
        public static Settings defaults() {
            return new Settings(StrongDichotomies.FUX, Orientation.SWEEPING, 0, 1);
        }

        public Settings withDichotomy(Dichotomy d) {
            return new Settings(d, orientation, cantusVoice, discantusVoice);
        }

        public Settings withOrientation(Orientation o) {
            return new Settings(dichotomy, o, cantusVoice, discantusVoice);
        }

        public Settings withVoices(int cantus, int discantus) {
            return new Settings(dichotomy, orientation, cantus, discantus);
        }
    }

    /** The result of a check. */
    public static final class Report {

        private final Settings settings;
        private final List<ScoreNote> cantus;
        private final List<ScoreNote> discantus;
        private final List<Step> steps;

        Report(Settings settings, List<ScoreNote> cantus, List<ScoreNote> discantus, List<Step> steps) {
            this.settings = settings;
            this.cantus = Collections.unmodifiableList(cantus);
            this.discantus = Collections.unmodifiableList(discantus);
            this.steps = steps;
        }

        public Settings getSettings() { return settings; }

        public List<Step> getSteps() { return steps; }

        public List<ScoreNote> getCantus() { return cantus; }

        public List<ScoreNote> getDiscantus() { return discantus; }

        /** True if every interval is consonant and every succession admitted. */
        public boolean isCorrect() {
            return FirstSpecies.isCorrect(steps);
        }

        /** The number of positions of the given kind. */
        public int count(Kind kind) {
            int c = 0;
            for (Step s : steps) {
                if (s.kind == kind) {
                    c++;
                }
            }
            return c;
        }

        /** The discantus notes at dissonant intervals or forbidden successions, in order. */
        public List<ScoreNote> flagged() {
            List<ScoreNote> f = new ArrayList<ScoreNote>();
            for (Step s : steps) {
                if (s.kind == Kind.DISSONANT || s.kind == Kind.FORBIDDEN) {
                    f.add(discantus.get(s.index));
                }
            }
            return f;
        }

        /** {@link #flagged()} as a Score denotator; empty if the line is correct. */
        public Denotator flaggedScore() {
            return ScoreNote.toScore(flagged());
        }

        /** A plain-text report, one line per position. */
        public String text() {
            StringBuilder b = new StringBuilder();
            b.append("First-species check against ").append(settings.dichotomy)
             .append(", ").append(settings.orientation.name().toLowerCase())
             .append(", cantus firmus voice ").append(settings.cantusVoice)
             .append(", discantus voice ").append(settings.discantusVoice)
             .append(". Pitches are reduced mod 12.\n");
            for (Step s : steps) {
                b.append(String.format("%3d  onset %-6s cantus %3d  discantus %3d  interval %2d  %s%n",
                        s.index + 1, trim(cantus.get(s.index).onset), s.cantus, s.discantus, s.y, describe(s)));
            }
            b.append(String.format("%d positions: %d admitted, %d dissonant, %d forbidden, %d unchecked.%n",
                    steps.size(), count(Kind.ADMITTED), count(Kind.DISSONANT), count(Kind.FORBIDDEN), count(Kind.UNCHECKED)));
            return b.toString();
        }

        private String describe(Step s) {
            switch (s.kind) {
            case ADMITTED:   return s.index == 0 ? "consonant" : "admitted";
            case DISSONANT:  return "dissonant interval";
            case FORBIDDEN:  return "forbidden after interval " + steps.get(s.index - 1).y;
            default:         return "not checked, previous interval dissonant";
            }
        }
    }

    /** Checks a Score with {@link Settings#defaults()}. */
    public static Report check(Denotator score) {
        return check(score, Settings.defaults());
    }

    /**
     * Checks a two-voice Score.
     *
     * @throws IllegalArgumentException if the denotator is not a zero-addressed Score, a voice is
     *         missing, the voices are not note against note, or a pitch is not an integer
     */
    public static Report check(Denotator score, Settings settings) {
        if (score == null) {
            throw new IllegalArgumentException("The score is null.");
        }
        if (!score.nullAddressed()) {
            throw new IllegalArgumentException("The score has address " + score.getAddress()
                    + "; Counterpoint Check needs a zero-addressed Score. Evaluate it first, for example with atNull().");
        }
        return check(ScoreNote.fromScore(score), settings);
    }

    /** Checks a list of notes, as {@link #check(Denotator, Settings)} does. */
    public static Report check(List<ScoreNote> notes, Settings settings) {
        List<ScoreNote> cf = voice(notes, settings.cantusVoice);
        List<ScoreNote> dc = voice(notes, settings.discantusVoice);
        if (cf.isEmpty() || dc.isEmpty()) {
            throw new IllegalArgumentException("The score needs notes in voice " + settings.cantusVoice
                    + " (cantus firmus) and voice " + settings.discantusVoice + " (discantus); it has "
                    + cf.size() + " and " + dc.size());
        }
        if (cf.size() != dc.size()) {
            throw new IllegalArgumentException("First species needs one discantus note per cantus firmus note; the score has "
                    + cf.size() + " against " + dc.size());
        }
        int[] c = new int[cf.size()];
        int[] d = new int[dc.size()];
        for (int i = 0; i < c.length; i++) {
            if (Math.abs(cf.get(i).onset - dc.get(i).onset) > 1e-9) {
                throw new IllegalArgumentException("Note " + (i + 1) + " is not note against note: the cantus firmus starts at "
                        + cf.get(i).onset + " and the discantus at " + dc.get(i).onset);
            }
            c[i] = integerPitch(cf.get(i));
            d[i] = integerPitch(dc.get(i));
        }
        return new Report(settings, cf, dc, FirstSpecies.check(settings.dichotomy, settings.orientation, c, d));
    }

    /** The admitted successors of x + &epsilon;.k; see {@link ContrapuntalSymmetries}. */
    public static ContrapuntalSymmetries.Result successors(Dichotomy d, int x, int k) {
        return ContrapuntalSymmetries.analyse(d, x, k);
    }

    /**
     * The admitted successors of the interval k over the cantus firmus pitch
     * as a two-voice Score: one dyad per successor, at onsets 0, 1, 2, ...,
     * cantus firmus in voice 0 and discantus in voice 1. The next cantus
     * firmus pitch is the nearest one with the successor's pitch class,
     * from a fourth below to a tritone above.
     */
    public static Denotator successorScore(Dichotomy d, Orientation o, int cantusPitch, int k) {
        ContrapuntalSymmetries.Result r = successors(d, Math.floorMod(cantusPitch, 12), k);
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        int onset = 0;
        for (int[] s : r.getSuccessors()) {
            int step = Math.floorMod(s[0] - cantusPitch + 5, 12) - 5;
            int cf = cantusPitch + step;
            int dc = o == Orientation.SWEEPING ? cf + s[1] : cf - s[1];
            notes.add(new ScoreNote(onset, cf, 80, 1.0, 0));
            notes.add(new ScoreNote(onset, dc, 80, 1.0, 1));
            onset++;
        }
        return ScoreNote.toScore(notes);
    }

    private static List<ScoreNote> voice(List<ScoreNote> notes, int voice) {
        List<ScoreNote> v = new ArrayList<ScoreNote>();
        for (ScoreNote n : notes) {
            if (n.voice == voice) {
                v.add(n);
            }
        }
        return v;
    }

    private static int integerPitch(ScoreNote n) {
        Rational p = n.pitch;
        if (!p.isIntegral()) {
            throw new IllegalArgumentException("The pitch " + p + " at onset " + n.onset
                    + " is not a whole semitone; the counterpoint model works in Z_12.");
        }
        return p.getNumerator();
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }
}
