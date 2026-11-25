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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.rubato.io.ScoreNote;
import org.rubato.math.arith.Rational;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Realization;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Refusal;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.RelaxationMode;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.State;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.Verdict;
import org.rubato.math.yoneda.Denotator;

/**
 * The headless three-voice counterpoint calls behind the Three-Voice
 * Counterpoint rubette: a note-against-note check of three Score voices, the
 * successor chords of a chord, and the report on the parsimonious P/L/R
 * realizations. The model is {@link ThreeVoiceCounterpoint#fux()}, after
 * Okumura and Shibayama, arXiv 2606.01102v3; departures are listed in
 * sources/THREE-VOICE-COUNTERPOINT-SPEC.md.
 */
public final class ThreeVoice {

    private ThreeVoice() {}

    /** Which Score voices are the lower, middle and upper voice, and the relaxation. */
    public static final class Settings {

        public final int lowerVoice;
        public final int middleVoice;
        public final int upperVoice;
        public final RelaxationMode mode;

        public Settings(int lowerVoice, int middleVoice, int upperVoice, RelaxationMode mode) {
            this.lowerVoice = lowerVoice;
            this.middleVoice = middleVoice;
            this.upperVoice = upperVoice;
            this.mode = mode;
        }

        /** Voices 0, 1, 2 from the lower voice up, strict mode. */
        public static Settings defaults() {
            return new Settings(0, 1, 2, RelaxationMode.STRICT);
        }

        public Settings withMode(RelaxationMode m) {
            return new Settings(lowerVoice, middleVoice, upperVoice, m);
        }

        public Settings withVoices(int lower, int middle, int upper) {
            return new Settings(lower, middle, upper, mode);
        }
    }

    /** The result of a check: one state per chord and one verdict per step. */
    public static final class Report {

        private final Settings settings;
        private final List<ScoreNote[]> chords;
        private final List<State> states;
        private final List<Verdict> verdicts;

        Report(Settings settings, List<ScoreNote[]> chords, List<State> states, List<Verdict> verdicts) {
            this.settings = settings;
            this.chords = chords;
            this.states = Collections.unmodifiableList(states);
            this.verdicts = Collections.unmodifiableList(verdicts);
        }

        public Settings getSettings() { return settings; }

        /** The chords' states, in onset order. */
        public List<State> getStates() { return states; }

        /** The verdict on each step from one chord to the next. */
        public List<Verdict> getVerdicts() { return verdicts; }

        /** The last chord's pitches from the lower voice up. */
        public int[] lastPitches() {
            ScoreNote[] c = chords.get(chords.size() - 1);
            return new int[] { pitch(c[0]), pitch(c[1]), pitch(c[2]) };
        }

        /** True if every chord is in the mask and every step is admitted. */
        public boolean isCorrect() {
            return flaggedChords().isEmpty();
        }

        private TreeSet<Integer> flaggedChords() {
            TreeSet<Integer> f = new TreeSet<Integer>();
            ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
            for (int i = 0; i < states.size(); i++) {
                if (!m.inMask(states.get(i))) {
                    f.add(i);
                }
            }
            for (int i = 0; i < verdicts.size(); i++) {
                if (!verdicts.get(i).isAdmitted()) {
                    f.add(i + 1);
                }
            }
            return f;
        }

        /**
         * The three notes of every chord outside the mask and of the second
         * chord of every refused step (spec D7), lower voice first.
         */
        public List<ScoreNote> flagged() {
            List<ScoreNote> out = new ArrayList<ScoreNote>();
            for (int i : flaggedChords()) {
                Collections.addAll(out, chords.get(i));
            }
            return out;
        }

        /** {@link #flagged()} as a Score denotator; empty if the passage is correct. */
        public Denotator flaggedScore() {
            return ScoreNote.toScore(flagged());
        }

        /** A plain-text report: each chord, then each step with its reasons. */
        public String text() {
            ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
            StringBuilder b = new StringBuilder();
            b.append("Three-voice first-species check (Okumura and Shibayama, arXiv 2606.01102), Fux's consonances")
             .append(" with the mask H_Fux, ").append(modeText(settings.mode)).append(".\n")
             .append("Lower voice ").append(settings.lowerVoice).append(", middle voice ").append(settings.middleVoice)
             .append(", upper voice ").append(settings.upperVoice)
             .append(". Pitches are reduced mod 12; register and crossing are not judged.\n\n");
            int outside = 0;
            for (int i = 0; i < states.size(); i++) {
                ScoreNote[] c = chords.get(i);
                State s = states.get(i);
                int h = m.label(s.getB(), s.getC());
                if (h == 0) {
                    outside++;
                }
                b.append(String.format(Locale.ROOT, "Chord %2d  onset %-6s pitches %3d %3d %3d  state %-12s %s%n",
                        i + 1, trim(c[0].onset), pitch(c[0]), pitch(c[1]), pitch(c[2]), s,
                        h == 0 ? "not in the mask" : "h" + h));
                if (i + 1 < states.size()) {
                    Verdict v = verdicts.get(i);
                    if (v.isAdmitted()) {
                        b.append(String.format(Locale.ROOT, "  step %d to %d: admitted%n", i + 1, i + 2));
                    }
                    else {
                        b.append(String.format(Locale.ROOT, "  step %d to %d: refused%n", i + 1, i + 2));
                        for (Refusal r : v.getRefusals()) {
                            b.append("    - ").append(r.text()).append('\n');
                        }
                    }
                }
            }
            int admitted = 0;
            for (Verdict v : verdicts) {
                if (v.isAdmitted()) {
                    admitted++;
                }
            }
            b.append(String.format(Locale.ROOT, "%n%d chord%s, %d step%s: %d admitted, %d refused; %d chord%s not in the mask.%n",
                    states.size(), plural(states.size()), verdicts.size(), plural(verdicts.size()), admitted,
                    verdicts.size() - admitted, outside, plural(outside)));
            return b.toString();
        }
    }

    /** The property value of a mode: strict, hold-fibre, hold or reflexive. */
    public static String modeName(RelaxationMode mode) {
        switch (mode) {
            case HOLD_FIBRE: return "hold-fibre";
            case HOLD: return "hold";
            case REFLEXIVE: return "reflexive";
            default: return "strict";
        }
    }

    /** A description of the mode for reports. */
    public static String modeText(RelaxationMode mode) {
        switch (mode) {
            case HOLD_FIBRE:
                return "relaxation hold-fibre (ours, not the paper's: a pair that stands still is held"
                     + " and the other pairs are maximized among the chords that keep it; spec D6)";
            case HOLD:
                return "relaxation hold (ours, not the paper's: a pair that stands still is held"
                     + " and the other pairs are maximized among all chords in the mask; spec D12)";
            case REFLEXIVE:
                return "relaxation reflexive (ours, not the paper's: the paper's V1 baseline with held pairs"
                     + " skipped, so each moving pair is checked alone and there is no joint maximization;"
                     + " from the chords on lower voice 0 it admits 1,391 steps more than strict, 1,178 of them"
                     + " from dropping the joint maximization and 213 from held pairs; spec D13)";
            default:
                return "relaxation strict (the paper's relation, eq. (2))";
        }
    }

    /** The mode for strict, hold-fibre, hold or reflexive, ignoring case and surrounding blanks. */
    public static RelaxationMode parseMode(String s) {
        String t = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        for (RelaxationMode m : RelaxationMode.values()) {
            if (t.equals(modeName(m))) {
                return m;
            }
        }
        throw new IllegalArgumentException(
                "The relaxation must be strict, hold-fibre, hold or reflexive, not \"" + s + "\"");
    }

    /** Three MIDI pitches, lower voice first, separated by blanks. */
    public static int[] parseChord(String s) {
        String[] parts = s == null ? new String[0] : s.trim().split("\\s+");
        String help = "The start chord needs three MIDI pitches from 0 to 127, lower voice first, for example 50 53 62";
        if (parts.length != 3) {
            throw new IllegalArgumentException(help + "; it has \"" + s + "\"");
        }
        int[] p = new int[3];
        for (int i = 0; i < 3; i++) {
            try {
                p[i] = Integer.parseInt(parts[i]);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException(help + "; \"" + parts[i] + "\" is not a whole number");
            }
            if (p[i] < 0 || p[i] > 127) {
                throw new IllegalArgumentException(help + "; " + p[i] + " is out of range");
            }
        }
        return p;
    }

    /** Checks a three-voice Score with {@link Settings#defaults()}. */
    public static Report check(Denotator score) {
        return check(score, Settings.defaults());
    }

    /**
     * Checks a three-voice Score note against note.
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
                    + "; Three-Voice Counterpoint needs a zero-addressed Score. Evaluate it first, for example with atNull().");
        }
        return check(ScoreNote.fromScore(score), settings);
    }

    /** Checks a list of notes, as {@link #check(Denotator, Settings)} does. */
    public static Report check(List<ScoreNote> notes, Settings settings) {
        List<ScoreNote> lo = voice(notes, settings.lowerVoice);
        List<ScoreNote> mi = voice(notes, settings.middleVoice);
        List<ScoreNote> up = voice(notes, settings.upperVoice);
        if (lo.isEmpty() || mi.isEmpty() || up.isEmpty()) {
            throw new IllegalArgumentException("The score needs notes in voice " + settings.lowerVoice + " (lower), voice "
                    + settings.middleVoice + " (middle) and voice " + settings.upperVoice + " (upper); it has "
                    + lo.size() + ", " + mi.size() + " and " + up.size());
        }
        if (lo.size() != mi.size() || lo.size() != up.size()) {
            throw new IllegalArgumentException("First species needs one note per voice at every onset; the voices have "
                    + lo.size() + ", " + mi.size() + " and " + up.size() + " notes");
        }
        ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
        List<ScoreNote[]> chords = new ArrayList<ScoreNote[]>();
        List<State> states = new ArrayList<State>();
        for (int i = 0; i < lo.size(); i++) {
            double t = lo.get(i).onset;
            if (Math.abs(mi.get(i).onset - t) > 1e-9 || Math.abs(up.get(i).onset - t) > 1e-9) {
                throw new IllegalArgumentException("Chord " + (i + 1) + " is not note against note: the voices start at "
                        + lo.get(i).onset + ", " + mi.get(i).onset + " and " + up.get(i).onset);
            }
            ScoreNote[] c = { lo.get(i), mi.get(i), up.get(i) };
            chords.add(c);
            states.add(m.fromPitchClasses(pitch(c[0]), pitch(c[1]), pitch(c[2])));
        }
        List<Verdict> verdicts = new ArrayList<Verdict>();
        for (int i = 0; i + 1 < states.size(); i++) {
            verdicts.add(m.check(states.get(i), states.get(i + 1), settings.mode));
        }
        return new Report(settings, chords, states, verdicts);
    }

    /**
     * The admitted successors of a chord as notes: one chord per successor at
     * onsets 0, 1, 2, …, in the model's state order, voices 0, 1, 2 from the
     * lower voice up. Each voice moves to the nearest pitch with its new pitch
     * class, from a fourth below to a tritone above (spec D8).
     *
     * @throws IllegalArgumentException if the chord is not in the mask
     */
    public static List<ScoreNote> successorNotes(int[] pitches, RelaxationMode mode) {
        ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
        State s = m.fromPitchClasses(pitches[0], pitches[1], pitches[2]);
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        int onset = 0;
        for (State t : m.successors(s, mode)) {
            int[] pcs = { t.getLower(), t.getMiddle(), t.getUpper() };
            for (int v = 0; v < 3; v++) {
                int step = Math.floorMod(pcs[v] - pitches[v] + 5, 12) - 5;
                notes.add(new ScoreNote(onset, pitches[v] + step, 80, 1.0, v));
            }
            onset++;
        }
        return notes;
    }

    /** {@link #successorNotes} as a Score denotator. */
    public static Denotator successorScore(int[] pitches, RelaxationMode mode) {
        return ScoreNote.toScore(successorNotes(pitches, mode));
    }

    /**
     * A realization of a sequence of states, one chord per onset: the lower
     * voice starts at the pitch nearest to {@code lowerStart} and then moves to
     * the nearest pitch, from a fourth below to a tritone above; the middle
     * voice sits b semitones above it (an octave for b = 0) and the upper voice
     * at the lowest pitch above the middle voice with its pitch class. Voices 0,
     * 1, 2 from the lower voice up, notes in onset order.
     */
    public static List<ScoreNote> realize(List<State> states, int lowerStart) {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        int lower = lowerStart;
        for (int i = 0; i < states.size(); i++) {
            State s = states.get(i);
            lower += Math.floorMod(s.getLower() - lower + 5, 12) - 5;
            int middle = lower + (s.getB() == 0 ? 12 : s.getB());
            int upper = middle + 1 + Math.floorMod(s.getUpper() - (middle + 1), 12);
            notes.add(new ScoreNote(i, lower, 80, 1.0, 0));
            notes.add(new ScoreNote(i, middle, 80, 1.0, 1));
            notes.add(new ScoreNote(i, upper, 80, 1.0, 2));
        }
        return notes;
    }

    /**
     * The report on the 288 parsimonious realizations of P, L and R (§6.4,
     * Prop. 6.8, p. 23) in a mode: totals, then one line per transformation,
     * source mode and voicing type with the commonest reason for a refusal.
     */
    public static String plrReport(RelaxationMode mode) {
        ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
        List<Realization> all = m.parsimoniousRealizations();
        int inMask = 0;
        int admitted = 0;
        Map<Character, Integer> by = new LinkedHashMap<Character, Integer>();
        by.put('P', 0);
        by.put('L', 0);
        by.put('R', 0);
        Map<String, int[]> groups = new LinkedHashMap<String, int[]>();
        Map<String, Map<String, Integer>> reasons = new LinkedHashMap<String, Map<String, Integer>>();
        for (Realization z : all) {
            String key = z.getTransformation() + " " + (z.isMajor() ? "major" : "minor") + " " + z.getType();
            int[] g = groups.get(key);
            if (g == null) {
                g = new int[3];
                groups.put(key, g);
                reasons.put(key, new LinkedHashMap<String, Integer>());
            }
            g[0]++;
            if (!z.isTargetInMask()) {
                continue;
            }
            inMask++;
            g[1]++;
            Verdict v = m.check(z.getSource(), z.getTarget(), mode);
            if (v.isAdmitted()) {
                admitted++;
                g[2]++;
                by.put(z.getTransformation(), by.get(z.getTransformation()) + 1);
            }
            else {
                TreeSet<String> rs = new TreeSet<String>();
                for (Refusal r : v.getRefusals()) {
                    rs.add(r.getReason().name().toLowerCase(Locale.ROOT).replace('_', ' '));
                }
                String k = String.join(" and ", rs);
                Map<String, Integer> c = reasons.get(key);
                c.put(k, c.containsKey(k) ? c.get(k) + 1 : 1);
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("Parsimonious P/L/R realizations (\u00a76.4 and Prop. 6.8 of arXiv 2606.01102), ")
         .append(modeText(mode)).append(".\n")
         .append(String.format(Locale.ROOT, "%d realizations, %d with the target in the mask, %d admitted (P %d, L %d, R %d).%n",
                 all.size(), inMask, admitted, by.get('P'), by.get('L'), by.get('R')))
         .append("Type lists the source triad's members from the lower voice up (1 root, 3 third, 5 fifth).\n");
        for (Map.Entry<String, int[]> e : groups.entrySet()) {
            int[] g = e.getValue();
            String common = "";
            int best = 0;
            for (Map.Entry<String, Integer> r : reasons.get(e.getKey()).entrySet()) {
                if (r.getValue() > best) {
                    best = r.getValue();
                    common = r.getKey();
                }
            }
            b.append(String.format(Locale.ROOT, "  %s  %2d in the mask, %2d admitted%s%n", e.getKey(), g[1], g[2],
                    best == 0 ? "" : "; refused for " + common));
        }
        return b.toString();
    }

    private static List<ScoreNote> voice(List<ScoreNote> notes, int voice) {
        List<ScoreNote> v = new ArrayList<ScoreNote>();
        for (ScoreNote n : notes) {
            if (n.voice == voice) {
                v.add(n);
            }
        }
        Collections.sort(v, new Comparator<ScoreNote>() {
            public int compare(ScoreNote a, ScoreNote b) {
                return Double.compare(a.onset, b.onset);
            }
        });
        return v;
    }

    private static int pitch(ScoreNote n) {
        Rational p = n.pitch;
        if (!p.isIntegral()) {
            throw new IllegalArgumentException("The pitch " + p + " at onset " + n.onset
                    + " is not a whole semitone; the counterpoint model works in Z_12.");
        }
        return p.getNumerator();
    }

    private static String plural(int k) {
        return k == 1 ? "" : "s";
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }
}
