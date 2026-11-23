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

        /** "D" for a major key, "a" for a relative minor. */
        public String keyName() { return name(key, minor); }

        /** The previous key's name, or "" for a start. */
        public String fromName() { return from == null ? "" : name(from, fromMinor); }

        /** The displayed minimal cadential sets as degree masks, in the order k1 to k5. */
        public List<Integer> cadences() { return cadences; }

        /** "k1" ... "k5" for {@link #cadences()}. */
        public List<String> cadenceNames() {
            List<String> out = new ArrayList<String>();
            for (int c : cadences) {
                out.add(cadenceNumber(c));
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

    /** The path with Def. 88's covering filter and TI12 rigidity. */
    public static List<Event> analyse(List<List<Integer>> bars, int window) {
        return analyse(bars, window, Filter.COVERED, Rigidity.TI12);
    }

    /**
     * The path of a sequence of bars.
     *
     * @throws IllegalArgumentException if the window is less than one bar
     */
    public static List<Event> analyse(List<List<Integer>> bars, int window, Filter filter, Rigidity rigidity) {
        if (window < 1) {
            throw new IllegalArgumentException("The window must be at least one bar, got " + window);
        }
        Tonality c = Tonality.of(Interpretations.C_MAJOR, 0, 3);
        Tonality[] keys = new Tonality[12];
        for (int p = 0; p < 12; p++) {
            keys[p] = c.transpose(p);
        }
        List<Event> events = new ArrayList<Event>();
        int cur = -1;
        boolean minor = false;
        int lastChange = 1;
        int lastCadence = 0;
        for (int b = 1; b <= bars.size(); b++) {
            int lo = Math.max(Math.max(1, b - window + 1), lastChange);
            List<List<Integer>> shown = new ArrayList<List<Integer>>();
            int best = -1;
            for (int k = 0; k < 12; k++) {
                List<Integer> d = displayed(keys[k], bars, lo, b);
                shown.add(d);
                if (k != cur && !d.isEmpty() && (best < 0 || d.size() > shown.get(best).size())) {
                    best = k;
                }
            }
            Event e = null;
            if (cur < 0 && best >= 0) {
                List<Integer> d = shown.get(best);
                minor = d.size() == 1 && d.get(0) == K2;
                e = new Event(Kind.START, b, firstBar(keys[best], d, bars, lo, b), keys[best], minor, null, false,
                        d, Collections.<Quantized>emptyList(), null, Collections.<String>emptyList(), lastCadence);
                cur = best;
            }
            else if (cur >= 0 && best >= 0) {
                List<Integer> d = shown.get(best);
                List<Quantized> q = Modulations.quantize(keys[cur], keys[best], filter, rigidity);
                Quantized chosen = null;
                int pivots = 0;
                for (Quantized x : q) {
                    pivots |= x.pivots();
                    if (chosen == null && d.contains(x.cadence())) {
                        chosen = x;
                    }
                }
                if (chosen != null) {
                    pivots = chosen.pivots();
                }
                boolean newMinor = d.size() == 1 && d.get(0) == K2;
                e = new Event(Kind.MODULATION, b, firstBar(keys[best], d, bars, lo, b), keys[best], newMinor,
                        keys[cur], minor, d, q, chosen, pivotBars(keys[best], pivots, bars, Math.max(1, lastCadence), b),
                        lastCadence);
                cur = best;
                minor = newMinor;
                lastChange = b;
            }
            else if (cur >= 0 && !shown.get(cur).isEmpty()) {
                List<Integer> d = shown.get(cur);
                Kind kind = Kind.CONFIRMATION;
                if (!minor && d.contains(K2)) {
                    kind = Kind.MODE_CHANGE;
                    d = Collections.singletonList(K2);
                }
                else if (minor && d.contains(K4)) {
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

    private static List<Integer> displayed(Tonality t, List<List<Integer>> bars, int lo, int b) {
        List<Integer> out = new ArrayList<Integer>();
        for (String s : NUMBERED) {
            int mu = Cadences.parse(s);
            boolean all = true;
            boolean inLast = false;
            for (int chord : t.chordsOf(mu)) {
                if (latest(chord, bars, lo, b) == 0) {
                    all = false;
                }
                if (bars.get(b - 1).contains(chord)) {
                    inLast = true;
                }
            }
            if (all && inLast) {
                out.add(mu);
            }
        }
        return out;
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
                named.add(cadenceNumber(c) + " " + Cadences.name(c));
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
                b.append(String.format("bar %d: modulation %s -> %s by %s in %s%n", e.bar, e.fromName(), e.keyName(),
                        String.join(", ", named), bars));
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
