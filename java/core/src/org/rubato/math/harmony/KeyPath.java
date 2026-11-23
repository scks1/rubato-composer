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

package org.rubato.math.harmony;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rubato.math.global.Interpretations;
import org.rubato.math.harmony.Modulations.Filter;
import org.rubato.math.harmony.Modulations.Rigidity;

/**
 * Follows a chord sequence through major keys by the cadences it displays, in
 * the manner of Agust&iacute;n-Aquino and Curiel-L&oacute;pez's reading of
 * L'Orfeo (arXiv 2512.18862, &sect;5.2, pp. 12&ndash;18), with the quanta and
 * pivots of {@link Modulations} at each change of key.
 * <p>
 * The paper reads its examples by hand and gives no procedure; this one is a
 * mechanical stand-in (a departure):
 * <ol>
 * <li>Only chords equal to a degree triad of some major key take part.</li>
 * <li>A minimal cadential set &mu; of key T is displayed at bar b when every
 * chord of &mu; occurs in bars b &minus; w + 1 &hellip; b, none of them before
 * the bar of the last change of scale, and bar b holds one of them.</li>
 * <li>The first displayed cadence fixes the starting key. A displayed cadence
 * of another key is a modulation. A displayed cadence of the current key is a
 * confirmation, except that k2 = {II,III} in a major key, or k4 = {IV,V} in its
 * relative minor, changes the mode (the duality reading of &sect;4.2, p. 7:
 * R sends k4 to k2).</li>
 * </ol>
 * The cadences are numbered as on p. 5 of the paper: k1 = {II,V},
 * k2 = {II,III}, k3 = {III,IV}, k4 = {IV,V}, k5 = {VII}.
 */
public final class KeyPath {

    /** What happened at a bar. */
    public enum Kind { START, CONFIRMATION, MODULATION, MODE_CHANGE }

    private static final String[] NUMBERED = { "{II,V}", "{II,III}", "{III,IV}", "{IV,V}", "{VII}" };
    private static final int K2 = Cadences.parse("{II,III}");
    private static final int K4 = Cadences.parse("{IV,V}");

    private KeyPath() { /* static utility */ }

    /** One event of the path. */
    public static final class Event {

        private final Kind kind;
        private final int bar;
        private final int firstBar;
        private final Tonality key;
        private final boolean minor;
        private final Tonality from;
        private final boolean fromMinor;
        private final List<Integer> cadences;
        private final List<Quantized> quantized;
        private final Quantized chosen;
        private final List<String> pivotBars;
        private final int previousCadenceBar;
        int tonicBar;

        Event(Kind kind, int bar, int firstBar, Tonality key, boolean minor, Tonality from, boolean fromMinor,
              List<Integer> cadences, List<Quantized> quantized, Quantized chosen, List<String> pivotBars,
              int previousCadenceBar) {
            this.kind = kind;
            this.bar = bar;
            this.firstBar = firstBar;
            this.key = key;
            this.minor = minor;
            this.from = from;
            this.fromMinor = fromMinor;
            this.cadences = cadences;
            this.quantized = quantized;
            this.chosen = chosen;
            this.pivotBars = pivotBars;
            this.previousCadenceBar = previousCadenceBar;
        }

        public Kind kind() { return kind; }

        /** The bar where the cadence is complete, counted from 1. */
        public int bar() { return bar; }

        /** The earliest bar holding a chord of the displayed cadences. */
        public int firstBar() { return firstBar; }

        /** The major key whose scale the event is in. */
        public Tonality key() { return key; }

        /** True if the event is read in the relative minor of {@link #key()}. */
        public boolean isMinor() { return minor; }

        /** True if the event's key is a harmonic minor key (only with {@link Options#withMinorKeys}). */
        public boolean isHarmonicMinor() { return !isMajorScale(key); }

        /** "D" for a major key, "a" for a relative minor. */
        public String keyName() { return name(key, minor); }

        /** The previous key's name, or "" for a start. */
        public String fromName() { return from == null ? "" : name(from, fromMinor); }

        /** The displayed minimal cadential sets as degree masks, in the order k1 to k5. */
        public List<Integer> cadences() { return cadences; }

        /** "k1" ... "k5" for {@link #cadences()}; degree sets such as "{IV,V}" in a harmonic minor key. */
        public List<String> cadenceNames() {
            List<String> out = new ArrayList<String>();
            for (int c : cadences) {
                String k = isMajorScale(key) ? cadenceNumber(c) : "";
                out.add(k.isEmpty() ? Cadences.name(c) : k);
            }
            return out;
        }

        /** For a modulation, every quantized modulation from the old key to the new one. */
        public List<Quantized> quantized() { return quantized; }

        /** The quantized modulation whose cadence is displayed, or null. */
        public Quantized chosen() { return chosen; }

        /** True if a quantized modulation has one of the displayed cadences. */
        public boolean displayedQuantized() { return chosen != null; }

        /** Pivot chords between the previous cadence and this one, as "bar:chord". */
        public List<String> pivotBars() { return pivotBars; }

        /** The first bar after the previous cadence holding the new tonic triad, 0 if none before the next change. */
        public int tonicBar() { return tonicBar; }

        /** A shortest PLR word from the old tonic triad to the new one ("" for a start). */
        public String plrWord() {
            return from == null ? "" : Triads.plrWord(tonicTriad(from, fromMinor), tonicTriad(key, minor));
        }

        /** The TI map from the old tonic triad to the new one, the dual of {@link #plrWord()}. */
        public String tiDual() {
            return from == null ? "" : Triads.tiName(Triads.tiElement(tonicTriad(from, fromMinor), tonicTriad(key, minor)));
        }
    }

    /** How a sonority that is not a triad reaches the key path (departures except NONE). */
    public enum Reduction {
        /** As it is: only exact triads take part (the stage-7 rule). */
        NONE,
        /** The major, minor or diminished triad it contains with the bass as root, else as it is. */
        BASS_TRIAD,
        /**
         * A major, minor or diminished triad it contains: the one with the bass as root, else one
         * holding the bass, major or minor before diminished, then the lowest root; else as it is.
         */
        CONTAINED_TRIAD
    }

    /** The sonority (a pitch-class mask) with its bass pitch class, reduced. */
    public static int reduce(int mask, int bass, Reduction r) {
        if (r == Reduction.NONE || isTriad(mask)) {
            return mask;
        }
        int bassPc = Math.floorMod(bass, 12);
        int best = -1;
        int bestRank = Integer.MAX_VALUE;
        for (int root = 0; root < 12; root++) {
            int[] kinds = { PitchClasses.of(root, root + 4, root + 7), PitchClasses.of(root, root + 3, root + 7),
                            PitchClasses.of(root, root + 3, root + 6) };
            for (int kind = 0; kind < 3; kind++) {
                int t = kinds[kind];
                if ((t & ~mask) != 0) {
                    continue;
                }
                int rank;
                if (root == bassPc) {
                    rank = 0;
                }
                else if (r == Reduction.BASS_TRIAD) {
                    continue;
                }
                else {
                    rank = 100 + ((t >> bassPc & 1) == 1 ? 0 : 50) + (kind == 2 ? 20 : 0);
                }
                rank = rank * 12 + root;
                if (rank < bestRank) {
                    bestRank = rank;
                    best = t;
                }
            }
        }
        return best < 0 ? mask : best;
    }

    private static boolean isTriad(int mask) {
        for (int root = 0; root < 12; root++) {
            if (mask == PitchClasses.of(root, root + 4, root + 7) || mask == PitchClasses.of(root, root + 3, root + 7)
                    || mask == PitchClasses.of(root, root + 3, root + 6)) {
                return true;
            }
        }
        return false;
    }

    /** "k1" to "k5" for the paper's five cadences of the major scale, "" otherwise. */
    public static String cadenceNumber(int degreeSet) {
        for (int i = 0; i < NUMBERED.length; i++) {
            if (Cadences.parse(NUMBERED[i]) == degreeSet) {
                return "k" + (i + 1);
            }
        }
        return "";
    }

    /** Bars of a chord chart ("C | F G | ..."), each a list of pitch-class masks. */
    public static List<List<Integer>> parseChart(String chart) {
        List<List<Integer>> bars = new ArrayList<List<Integer>>();
        for (List<ChordSymbol> bar : ChordSymbol.parseBars(chart)) {
            List<Integer> b = new ArrayList<Integer>();
            for (ChordSymbol c : bar) {
                b.add(c.pitchClasses());
            }
            bars.add(b);
        }
        return bars;
    }

    /**
     * Rules of the path. {@link #literal(int)} is the stage-7 reading of the
     * paper; the other switches are departures for real music, each off in the
     * literal reading.
     */
    public static final class Options {

        final int window;
        final boolean preferCurrent;
        final int minStay;
        final boolean needQuantum;
        final boolean dimResolves;
        final boolean ordered;
        final boolean modeFirst;
        final boolean minorKeys;

        private Options(int window, boolean preferCurrent, int minStay, boolean needQuantum, boolean dimResolves,
                        boolean ordered, boolean modeFirst, boolean minorKeys) {
            if (window < 1) {
                throw new IllegalArgumentException("The window must be at least one bar, got " + window);
            }
            if (minStay < 0) {
                throw new IllegalArgumentException("The minimum stay must not be negative, got " + minStay);
            }
            this.window = window;
            this.preferCurrent = preferCurrent;
            this.minStay = minStay;
            this.needQuantum = needQuantum;
            this.dimResolves = dimResolves;
            this.ordered = ordered;
            this.modeFirst = modeFirst;
            this.minorKeys = minorKeys;
        }

        /** The stage-7 rules: every switch off. */
        public static Options literal(int window) {
            return new Options(window, false, 0, false, false, false, false, false);
        }

        /**
         * The rules that did best on the Monteverdi madrigals of the 2026-11-23
         * test (tuned on Cruda Amarilli, checked on five others): preferCurrent,
         * a two-bar stay, needQuantum, dimResolves and the minor keys. A
         * departure: it keeps Ecco pur and Mira but changes the synthetic aria,
         * so it is not the default.
         */
        public static Options realMusic(int window) {
            return literal(window).withPreferCurrent(true).withMinStay(2).withNeedQuantum(true).withDimResolves(true)
                    .withMinorKeys(true);
        }

        public int window() { return window; }
        public boolean preferCurrent() { return preferCurrent; }
        public int minStay() { return minStay; }
        public boolean needQuantum() { return needQuantum; }
        public boolean dimResolves() { return dimResolves; }
        public boolean ordered() { return ordered; }
        public boolean modeFirst() { return modeFirst; }
        public boolean minorKeys() { return minorKeys; }

        /** Keep the current key when it displays a cadence (no modulation that bar). */
        public Options withPreferCurrent(boolean on) {
            return new Options(window, on, minStay, needQuantum, dimResolves, ordered, modeFirst, minorKeys);
        }

        /** No modulation fewer than this many bars after the last change of scale. */
        public Options withMinStay(int bars) {
            return new Options(window, preferCurrent, bars, needQuantum, dimResolves, ordered, modeFirst, minorKeys);
        }

        /** Between keys of one scale type, modulate only by the cadence of a quantized modulation. */
        public Options withNeedQuantum(boolean on) {
            return new Options(window, preferCurrent, minStay, on, dimResolves, ordered, modeFirst, minorKeys);
        }

        /** A cadential set that is one diminished triad counts only when the key's tonic triad follows it. */
        public Options withDimResolves(boolean on) {
            return new Options(window, preferCurrent, minStay, needQuantum, on, ordered, modeFirst, minorKeys);
        }

        /** Sets holding V or VII count only when that chord comes after the set's other chords and resolves to I. */
        public Options withOrdered(boolean on) {
            return new Options(window, preferCurrent, minStay, needQuantum, dimResolves, on, modeFirst, minorKeys);
        }

        /** Decide a change of mode inside the current scale before looking at other keys. */
        public Options withModeFirst(boolean on) {
            return new Options(window, preferCurrent, minStay, needQuantum, dimResolves, ordered, on, minorKeys);
        }

        /** Add the twelve harmonic minor keys, with their own minimal cadential sets. */
        public Options withMinorKeys(boolean on) {
            return new Options(window, preferCurrent, minStay, needQuantum, dimResolves, ordered, modeFirst, on);
        }

        public Options withWindow(int bars) {
            return new Options(bars, preferCurrent, minStay, needQuantum, dimResolves, ordered, modeFirst, minorKeys);
        }

        @Override
        public String toString() {
            return "window " + window + (preferCurrent ? ", prefer current" : "") + (minStay > 0 ? ", stay " + minStay : "")
                    + (needQuantum ? ", need quantum" : "") + (dimResolves ? ", diminished resolves" : "")
                    + (ordered ? ", ordered" : "") + (modeFirst ? ", mode first" : "") + (minorKeys ? ", minor keys" : "");
        }
    }

    /**
     * The default rules at a window: the paper's literal reading. The
     * real-music preset does not reproduce the synthetic aria, so it is an
     * option ({@link Options#realMusic}).
     */
    public static Options defaults(int window) {
        return Options.literal(window);
    }

    /** The path under the given rules, with Def. 88's covering filter and the default rigidity reading. */
    public static List<Event> analyse(List<List<Integer>> bars, Options options) {
        return analyse(bars, options, Filter.COVERED, Modulations.DEFAULT_RIGIDITY);
    }

    /** The path with Def. 88's covering filter, the default rigidity reading and the default rules. */
    public static List<Event> analyse(List<List<Integer>> bars, int window) {
        return analyse(bars, defaults(window));
    }

    /**
     * The path of a sequence of bars under the default rules at a window.
     *
     * @throws IllegalArgumentException if the window is less than one bar
     */
    public static List<Event> analyse(List<List<Integer>> bars, int window, Filter filter, Rigidity rigidity) {
        return analyse(bars, defaults(window), filter, rigidity);
    }

    /** The path under the given rules. */
    public static List<Event> analyse(List<List<Integer>> bars, Options o, Filter filter, Rigidity rigidity) {
        int window = o.window;
        Tonality c = Tonality.of(Interpretations.C_MAJOR, 0, 3);
        Tonality h = Tonality.of(Interpretations.C_HARMONIC_MINOR, 0, 3);
        int n = o.minorKeys ? 24 : 12;
        Tonality[] keys = new Tonality[n];
        List<List<Integer>> sets = new ArrayList<List<Integer>>();
        List<Integer> numbered = new ArrayList<Integer>();
        for (String x : NUMBERED) {
            numbered.add(Cadences.parse(x));
        }
        for (int p = 0; p < 12; p++) {
            keys[p] = c.transpose(p);
            sets.add(numbered);
        }
        if (o.minorKeys) {
            for (int p = 0; p < 12; p++) {
                keys[12 + p] = h.transpose(p);
            }
            sets.clear();
            for (int k = 0; k < n; k++) {
                sets.add(minimalAmong(keys[k], keys));
            }
        }
        List<Event> events = new ArrayList<Event>();
        int cur = -1;
        boolean minor = false;
        int lastChange = 1;
        int lastModulation = 0;
        int lastCadence = 0;
        for (int b = 1; b <= bars.size(); b++) {
            int lo = Math.max(Math.max(1, b - window + 1), lastChange);
            List<List<Integer>> shown = new ArrayList<List<Integer>>();
            for (int k = 0; k < n; k++) {
                shown.add(displayed(keys[k], sets.get(k), bars, lo, b, o));
            }
            boolean curShown = cur >= 0 && !shown.get(cur).isEmpty();
            boolean stay = curShown && (o.preferCurrent || (o.modeFirst && changesMode(keys[cur], minor, shown.get(cur))));
            boolean held = cur >= 0 && o.minStay > 0 && lastModulation > 0 && b - lastModulation < o.minStay;
            int best = -1;
            if (!stay && !held) {
                for (int k = 0; k < n; k++) {
                    List<Integer> d = shown.get(k);
                    if (k == cur || d.isEmpty()) {
                        continue;
                    }
                    if (o.needQuantum && cur >= 0 && sameType(keys[cur], keys[k])
                            && chosen(Modulations.quantize(keys[cur], keys[k], filter, rigidity), d) == null) {
                        continue;
                    }
                    if (best < 0 || d.size() > shown.get(best).size()) {
                        best = k;
                    }
                }
            }
            Event e = null;
            if (cur < 0 && best >= 0) {
                List<Integer> d = shown.get(best);
                minor = readsMinor(keys[best], d);
                e = new Event(Kind.START, b, firstBar(keys[best], d, bars, lo, b), keys[best], minor, null, false,
                        d, Collections.<Quantized>emptyList(), null, Collections.<String>emptyList(), lastCadence);
                cur = best;
            }
            else if (cur >= 0 && best >= 0) {
                List<Integer> d = shown.get(best);
                List<Quantized> q = Modulations.quantize(keys[cur], keys[best], filter, rigidity);
                Quantized chosen = chosen(q, d);
                int pivots = 0;
                for (Quantized x : q) {
                    pivots |= x.pivots();
                }
                if (chosen != null) {
                    pivots = chosen.pivots();
                }
                boolean newMinor = readsMinor(keys[best], d);
                e = new Event(Kind.MODULATION, b, firstBar(keys[best], d, bars, lo, b), keys[best], newMinor,
                        keys[cur], minor, d, q, chosen, pivotBars(keys[best], pivots, bars, Math.max(1, lastCadence), b),
                        lastCadence);
                cur = best;
                minor = newMinor;
                lastChange = b;
                lastModulation = b;
            }
            else if (curShown) {
                List<Integer> d = shown.get(cur);
                Kind kind = Kind.CONFIRMATION;
                if (isMajorScale(keys[cur]) && !minor && d.contains(K2)) {
                    kind = Kind.MODE_CHANGE;
                    d = Collections.singletonList(K2);
                }
                else if (isMajorScale(keys[cur]) && minor && d.contains(K4)) {
                    kind = Kind.MODE_CHANGE;
                    d = Collections.singletonList(K4);
                }
                boolean newMinor = kind == Kind.MODE_CHANGE ? !minor : minor;
                e = new Event(kind, b, firstBar(keys[cur], d, bars, lo, b), keys[cur], newMinor, keys[cur], minor, d,
                        Collections.<Quantized>emptyList(), null, Collections.<String>emptyList(), lastCadence);
                minor = newMinor;
            }
            if (e != null) {
                events.add(e);
                lastCadence = b;
            }
        }
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            if (e.kind != Kind.MODULATION && e.kind != Kind.MODE_CHANGE) {
                continue;
            }
            int end = bars.size();
            for (int j = i + 1; j < events.size(); j++) {
                if (events.get(j).kind == Kind.MODULATION || events.get(j).kind == Kind.MODE_CHANGE) {
                    end = events.get(j).bar;
                    break;
                }
            }
            int tonic = Triads.pitchClasses(tonicTriad(e.key, e.minor));
            for (int b = e.previousCadenceBar + 1; b <= end && e.tonicBar == 0; b++) {
                if (bars.get(b - 1).contains(tonic)) {
                    e.tonicBar = b;
                }
            }
        }
        return events;
    }

    /**
     * Def. 87 with X widened to the given keys of both scale types (a
     * departure): the minimal sets of degrees of t whose chords no other key
     * of the list holds together, ordered by size and then by degrees.
     */
    static List<Integer> minimalAmong(Tonality t, Tonality[] keys) {
        List<Integer> cadential = new ArrayList<Integer>();
        for (int m = 1; m < 128; m++) {
            boolean unique = true;
            for (Tonality u : keys) {
                if (u.equals(t)) {
                    continue;
                }
                boolean all = true;
                for (int chord : t.chordsOf(m)) {
                    if (!u.hasChord(chord)) {
                        all = false;
                        break;
                    }
                }
                if (all) {
                    unique = false;
                    break;
                }
            }
            if (unique) {
                cadential.add(m);
            }
        }
        List<Integer> out = new ArrayList<Integer>();
        for (int m : cadential) {
            boolean min = true;
            for (int x : cadential) {
                if (x != m && (x & ~m) == 0) {
                    min = false;
                    break;
                }
            }
            if (min) {
                out.add(m);
            }
        }
        out.sort((a, b) -> {
            int d = Integer.bitCount(a) - Integer.bitCount(b);
            return d != 0 ? d : Integer.compare(Integer.reverse(b), Integer.reverse(a));
        });
        return out;
    }

    private static Quantized chosen(List<Quantized> q, List<Integer> displayed) {
        for (Quantized x : q) {
            if (displayed.contains(x.cadence())) {
                return x;
            }
        }
        return null;
    }

    /** True for the transposes of the major scale, false for the harmonic minor keys. */
    static boolean isMajorScale(Tonality t) {
        return t.scaleMask() == PitchClasses.transpose(MAJOR_MASK, t.tonic());
    }

    private static final int MAJOR_MASK = PitchClasses.of(Interpretations.C_MAJOR);

    private static boolean sameType(Tonality s, Tonality t) {
        return isMajorScale(s) == isMajorScale(t);
    }

    /** A harmonic minor key always reads minor; a major key reads its relative minor when k2 alone is shown. */
    private static boolean readsMinor(Tonality t, List<Integer> d) {
        return !isMajorScale(t) || d.size() == 1 && d.get(0) == K2;
    }

    private static boolean changesMode(Tonality t, boolean minor, List<Integer> d) {
        return isMajorScale(t) && (!minor && d.contains(K2) || minor && d.contains(K4));
    }

    private static boolean isDiminished(int mask) {
        for (int r = 0; r < 12; r++) {
            if (PitchClasses.of(r, r + 3, r + 6) == mask) {
                return true;
            }
        }
        return false;
    }

    private static List<Integer> displayed(Tonality t, List<Integer> sets, List<List<Integer>> bars, int lo, int b,
                                           Options o) {
        List<Integer> out = new ArrayList<Integer>();
        for (int mu : sets) {
            List<Integer> chords = t.chordsOf(mu);
            boolean all = true;
            boolean inLast = false;
            for (int chord : chords) {
                if (latest(chord, bars, lo, b) == 0) {
                    all = false;
                }
                if (bars.get(b - 1).contains(chord)) {
                    inLast = true;
                }
            }
            if (!all) {
                continue;
            }
            boolean loneDiminished = chords.size() == 1 && isDiminished(chords.get(0));
            boolean dominant = (mu >> 4 & 1) == 1 || (mu >> 6 & 1) == 1;
            if (o.dimResolves && loneDiminished || o.ordered && dominant) {
                if (resolves(t, mu, bars, lo, b, o.ordered)) {
                    out.add(mu);
                }
            }
            else if (inLast) {
                out.add(mu);
            }
        }
        return out;
    }

    /**
     * True if the set's dominant chord (V, else VII) is followed by the tonic
     * triad within bars lo..b, with the dominant or that tonic in bar b; when
     * ordered, every other chord of the set also occurs before that dominant.
     */
    private static boolean resolves(Tonality t, int mu, List<List<Integer>> bars, int lo, int b, boolean ordered) {
        int dom = t.degree((mu >> 4 & 1) == 1 ? 4 : 6);
        int tonic = t.degree(0);
        List<Integer> chords = new ArrayList<Integer>();
        List<Integer> at = new ArrayList<Integer>();
        for (int i = lo; i <= b; i++) {
            for (int chord : bars.get(i - 1)) {
                chords.add(chord);
                at.add(i);
            }
        }
        for (int i = 0; i < chords.size(); i++) {
            if (chords.get(i) != dom) {
                continue;
            }
            int j = chords.subList(i + 1, chords.size()).indexOf(tonic);
            if (j < 0) {
                continue;
            }
            j += i + 1;
            if (at.get(i) != b && at.get(j) != b) {
                continue;
            }
            boolean before = true;
            if (ordered) {
                for (int other : t.chordsOf(mu)) {
                    if (other != dom && chords.subList(0, i).indexOf(other) < 0) {
                        before = false;
                    }
                }
            }
            if (before) {
                return true;
            }
        }
        return false;
    }

    private static int latest(int chord, List<List<Integer>> bars, int lo, int b) {
        for (int i = b; i >= lo; i--) {
            if (bars.get(i - 1).contains(chord)) {
                return i;
            }
        }
        return 0;
    }

    private static int firstBar(Tonality t, List<Integer> cadences, List<List<Integer>> bars, int lo, int b) {
        int first = b;
        for (int mu : cadences) {
            for (int chord : t.chordsOf(mu)) {
                first = Math.min(first, latest(chord, bars, lo, b));
            }
        }
        return first;
    }

    private static List<String> pivotBars(Tonality t, int pivots, List<List<Integer>> bars, int from, int to) {
        List<String> out = new ArrayList<String>();
        for (int b = from; b <= to; b++) {
            List<Integer> done = new ArrayList<Integer>();
            for (int chord : bars.get(b - 1)) {
                if (done.contains(chord)) {
                    continue;
                }
                done.add(chord);
                int d = t.degreeOf(chord);
                if (d >= 0 && (pivots >> d & 1) == 1) {
                    out.add(b + ":" + chordName(chord));
                }
            }
        }
        return out;
    }

    /** The tonic triad of a major key or of its relative minor. */
    static int tonicTriad(Tonality key, boolean minor) {
        if (!isMajorScale(key)) {
            return Triads.minor(key.tonic());
        }
        return minor ? Triads.minor(key.tonic() + 9) : Triads.major(key.tonic());
    }

    static String name(Tonality key, boolean minor) {
        return Triads.name(tonicTriad(key, minor));
    }

    /** "G", "e", "g#&deg;", or the pitch classes for other chords. */
    public static String chordName(int mask) {
        int t = Triads.fromPitchClasses(mask);
        if (t >= 0) {
            return Triads.name(t);
        }
        String[] sharps = { "c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b" };
        for (int r = 0; r < 12; r++) {
            if (PitchClasses.of(r, r + 3, r + 6) == mask) {
                return sharps[r] + "\u00b0";
            }
        }
        return PitchClasses.toString(mask);
    }

    /** A plain-text account, one paragraph per event. */
    public static String report(List<Event> events) {
        StringBuilder b = new StringBuilder();
        if (events.isEmpty()) {
            b.append("No minimal cadential set of a major key is displayed.\n");
        }
        for (Event e : events) {
            List<String> named = new ArrayList<String>();
            for (int c : e.cadences) {
                String k = isMajorScale(e.key) ? cadenceNumber(c) : "";
                named.add(k.isEmpty() ? Cadences.name(c) : k + " " + Cadences.name(c));
            }
            String bars = e.firstBar == e.bar ? "bar " + e.bar : "bars " + e.firstBar + "-" + e.bar;
            switch (e.kind) {
            case START:
                b.append(String.format("bar %d: starts in %s, cadence %s in %s%n", e.bar, e.keyName(),
                        String.join(", ", named), bars));
                break;
            case CONFIRMATION:
                b.append(String.format("bar %d: %s confirmed by %s in %s%n", e.bar, e.keyName(),
                        String.join(", ", named), bars));
                break;
            case MODE_CHANGE:
                b.append(String.format("bar %d: %s -> %s (same scale) by %s in %s; tonic triads by PLR %s, TI dual %s",
                        e.bar, e.fromName(), e.keyName(), String.join(", ", named), bars, e.plrWord(), e.tiDual()));
                b.append(e.tonicBar > 0 ? "; " + e.keyName() + " first at bar " + e.tonicBar + "\n" : "\n");
                break;
            default:
                if (e.chosen != null) {
                    b.append(String.format("bar %d: modulation %s -> %s by %s in %s%n", e.bar, e.fromName(), e.keyName(),
                            String.join(", ", named), bars));
                }
                else {
                    b.append(String.format("bar %d: modulation %s -> %s, displayed %s in %s, no quantum%n", e.bar,
                            e.fromName(), e.keyName(), String.join(", ", named), bars));
                }
                if (e.quantized.isEmpty()) {
                    b.append("    no quantized modulation between these keys\n");
                }
                for (Quantized q : e.quantized) {
                    b.append(String.format("    %s %s: quantum %s, trace %s, pivots %s%s%n", Modulations.name(q.modulator()),
                            cadenceNumber(q.cadence()) + " " + Cadences.name(q.cadence()),
                            PitchClasses.toString(q.quantum()), PitchClasses.toString(q.trace()),
                            Cadences.name(q.pivots()), q == e.chosen ? "  <- displayed cadence" : ""));
                }
                b.append("    pivot chords: ").append(e.pivotBars.isEmpty() ? "none" : String.join(" ", e.pivotBars)).append('\n');
                b.append(e.tonicBar > 0 ? "    " + e.keyName() + "'s I first at bar " + e.tonicBar + "\n"
                                        : "    " + e.keyName() + "'s I does not occur before the next change\n");
                b.append("    tonic triads by PLR ").append(e.plrWord().isEmpty() ? "(none)" : e.plrWord())
                 .append(", TI dual ").append(e.tiDual()).append('\n');
            }
        }
        return b.toString();
    }
}
