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

package org.rubato.math.counterpoint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rubato.math.arith.NumberTheory;

/**
 * Three-voice first-species counterpoint after R. Okumura and T. Shibayama,
 * "The art of counterpoint: a Mazzola-type model of three-voice
 * first-species counterpoint", arXiv:2606.01102v3 (29 Sep 2026).
 * <p>
 * A state (a; b, c) has the lower voice a, the middle voice a + b and the
 * upper voice a + c in Z_n, with (b, c) in a harmonic mask H (§2.1, p. 4).
 * The pairs LM, LU and MU project a state to the dual numbers a + &epsilon;.b,
 * a + &epsilon;.c and (a + b) + &epsilon;.(c &minus; b); LM and LU are always
 * active and MU is active when c &minus; b is a consonance (§2.3, p. 5). For a
 * tuple g of maximizing contrapuntal symmetries, one for each active pair,
 * C_H(&xi;; g) is the set of states whose projections lie in the images
 * g_&alpha;X[&epsilon;] (eq. (1)); the admitted successors S_H(&xi;) are the
 * union of C_H(&xi;; g) over the tuples of maximal size (eq. (2), Algorithm
 * 2.2, p. 6).
 * <p>
 * The two-voice symmetries come from {@link ContrapuntalSymmetries#analyse},
 * which returns them for a cantus of 0; this class conjugates them to the
 * pair's cantus x as T^x &#8728; g &#8728; T^&minus;x (spec D4).
 * {@link #check} adds reasons for a refusal (spec D5), and
 * {@link RelaxationMode#HOLD_FIBRE}, {@link RelaxationMode#HOLD} and
 * {@link RelaxationMode#REFLEXIVE} are the spec's relaxations D6, D12 and
 * D13, which are not the paper's. See sources/THREE-VOICE-COUNTERPOINT-SPEC.md.
 */
public final class ThreeVoiceCounterpoint {

    /** The three voice pairs. */
    public enum Pair {
        LM("lower and middle voices"),
        LU("lower and upper voices"),
        MU("middle and upper voices");

        private final String voices;

        Pair(String voices) {
            this.voices = voices;
        }

        /** The pair in words, for example "lower and upper voices". */
        public String voices() {
            return voices;
        }
    }

    /** Which successor relation a call uses. */
    public enum RelaxationMode {
        /** The paper's relation, eq. (2). The default. */
        STRICT,
        /**
         * Ours (spec D6): a pair that stands still is held, and the joint
         * maximization runs over the moving pairs among the states that keep
         * the held pairs fixed. A step where nothing moves is admitted.
         */
        HOLD_FIBRE,
        /**
         * Ours (spec D12): as {@link #HOLD_FIBRE}, but the joint maximization
         * over the moving pairs runs over all states in the mask, not only
         * those that keep the held pairs fixed. From the 26 sources with
         * lower voice 0 it admits 2 steps that HOLD_FIBRE refuses and
         * refuses 21 that HOLD_FIBRE admits.
         */
        HOLD,
        /**
         * Ours (spec D13): the paper's V1 baseline with held pairs skipped.
         * Each moving active pair's target must lie in some image of its
         * maximizing symmetries; there is no joint maximization, so
         * {@link Reason#NOT_MAXIMAL} never occurs. Over the 26 sources with
         * lower voice 0 it admits 1,391 edges more than STRICT: 1,178 of them
         * come from dropping the joint maximization (V1 alone) and 213 from
         * held pairs.
         */
        REFLEXIVE
    }

    /** Why a step is refused (spec D5). */
    public enum Reason {
        /** The source's configuration is not in the mask. */
        SOURCE_OUTSIDE_MASK,
        /** The target's configuration is not in the mask. */
        TARGET_OUTSIDE_MASK,
        /** An active pair keeps its projection; eq. (4) excludes it. Strict mode only. */
        PAIR_STILL,
        /** An active pair moves from a fifth to a fifth (Prop. 3.2). */
        FIFTH_TO_FIFTH,
        /** An active pair's target lies in no image of its maximizing symmetries. */
        PAIR_EXCLUDED,
        /** Every pair passes, but no maximizing tuple contains the target. */
        NOT_MAXIMAL
    }

    /** A state (a; b, c) over Z_n, reduced mod n. */
    public static final class State {
        private final int a;
        private final int b;
        private final int c;
        private final int n;

        State(int a, int b, int c, int n) {
            this.a = NumberTheory.mod(a, n);
            this.b = NumberTheory.mod(b, n);
            this.c = NumberTheory.mod(c, n);
            this.n = n;
        }

        /** The lower voice's pitch class a. */
        public int getLower() { return a; }

        /** The interval b from the lower to the middle voice. */
        public int getB() { return b; }

        /** The interval c from the lower to the upper voice. */
        public int getC() { return c; }

        /** The middle voice's pitch class a + b. */
        public int getMiddle() { return (a + b) % n; }

        /** The upper voice's pitch class a + c. */
        public int getUpper() { return (a + c) % n; }

        /** The modulus n. */
        public int getModulus() { return n; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof State)) {
                return false;
            }
            State s = (State) o;
            return s.a == a && s.b == b && s.c == c && s.n == n;
        }

        @Override
        public int hashCode() {
            return ((a * 31 + b) * 31 + c) * 31 + n;
        }

        /** The paper's notation, for example "(2; 3, 0)". */
        @Override
        public String toString() {
            return "(" + a + "; " + b + ", " + c + ")";
        }
    }

    /** One reason for refusing a step, with the pair it concerns (null for the mask reasons). */
    public static final class Refusal {
        private final Reason reason;
        private final Pair pair;
        private final int[] from;
        private final int[] to;

        Refusal(Reason reason, Pair pair, int[] from, int[] to) {
            this.reason = reason;
            this.pair = pair;
            this.from = from;
            this.to = to;
        }

        /** The reason. */
        public Reason getReason() { return reason; }

        /** The pair, or null for a mask reason or NOT_MAXIMAL. */
        public Pair getPair() { return pair; }

        /** One sentence for a report. */
        public String text() {
            switch (reason) {
            case SOURCE_OUTSIDE_MASK:
                return "the first chord is not in the mask (Def. 3.1)";
            case TARGET_OUTSIDE_MASK:
                return "the second chord is not in the mask (Def. 3.1)";
            case PAIR_STILL:
                return "the " + pair.voices() + " stand still at " + dual(from)
                     + "; eq. (4) excludes a stationary active pair";
            case FIFTH_TO_FIFTH:
                return "the " + pair.voices() + " move from a fifth to a fifth, "
                     + dual(from) + " to " + dual(to) + " (Prop. 3.2)";
            case PAIR_EXCLUDED:
                return "the " + pair.voices() + " move " + dual(from) + " to " + dual(to)
                     + ", which no maximizing symmetry of that pair allows";
            case NOT_MAXIMAL:
            default:
                return "each pair allows its motion, but the joint maximization of eq. (2) drops this chord";
            }
        }

        private static String dual(int[] z) {
            return z[0] + "+\u03b5." + z[1];
        }

        @Override
        public String toString() {
            return reason + (pair == null ? "" : ":" + pair);
        }
    }

    /** The decision on one step with the reasons for a refusal; admitted exactly when there are none. */
    public static final class Verdict {
        private final State source;
        private final State target;
        private final RelaxationMode mode;
        private final List<Refusal> refusals;

        Verdict(State source, State target, RelaxationMode mode, List<Refusal> refusals) {
            this.source = source;
            this.target = target;
            this.mode = mode;
            this.refusals = Collections.unmodifiableList(refusals);
        }

        /** The first state. */
        public State getSource() { return source; }

        /** The second state. */
        public State getTarget() { return target; }

        /** The mode the decision used. */
        public RelaxationMode getMode() { return mode; }

        /** True if no reason applies. */
        public boolean isAdmitted() { return refusals.isEmpty(); }

        /** The reasons, in the order of {@link Reason}; empty when admitted. */
        public List<Refusal> getRefusals() { return refusals; }
    }

    /** One parsimonious realization of a P, L or R transformation (§6.4, p. 23; spec D11). */
    public static final class Realization {
        private final char transformation;
        private final int root;
        private final boolean major;
        private final String type;
        private final State source;
        private final State target;
        private final boolean targetInMask;

        Realization(char transformation, int root, boolean major, String type,
                    State source, State target, boolean targetInMask) {
            this.transformation = transformation;
            this.root = root;
            this.major = major;
            this.type = type;
            this.source = source;
            this.target = target;
            this.targetInMask = targetInMask;
        }

        /** 'P', 'L' or 'R'. */
        public char getTransformation() { return transformation; }

        /** The source triad's root. */
        public int getRoot() { return root; }

        /** True if the source triad is major. */
        public boolean isMajor() { return major; }

        /** The voicing type, the source triad's members from the lower voice up: "135", "153", "351" or "315". */
        public String getType() { return type; }

        /** The source state. */
        public State getSource() { return source; }

        /** The target state: the source with the one moving voice moved. */
        public State getTarget() { return target; }

        /** True if the target's configuration lies in the mask. */
        public boolean isTargetInMask() { return targetInMask; }
    }

    private final Dichotomy dichotomy;
    private final int n;
    private final List<int[]> mask;
    private final Map<Integer, Integer> labels = new HashMap<Integer, Integer>();
    private final List<State> states;
    private final Map<State, Integer> stateIndex = new HashMap<State, Integer>();
    private final BitSet xe;
    private final Map<String, BitSet> preimages = new HashMap<String, BitSet>();
    private final Map<State, BitSet> strictCache = new HashMap<State, BitSet>();
    private final Map<String, BitSet> holdCache = new HashMap<String, BitSet>();

    /**
     * A model for a strong dichotomy and a mask of configurations (b, c),
     * each a pair of consonances. The mask's order gives the labels h1, h2, ….
     *
     * @throws IllegalArgumentException if an entry is not a pair of consonances or repeats
     */
    public ThreeVoiceCounterpoint(Dichotomy d, int[][] mask) {
        if (!d.isStrong()) {
            throw new IllegalArgumentException("The dichotomy " + d + " is not strong");
        }
        this.dichotomy = d;
        this.n = d.getModulus();
        List<int[]> m = new ArrayList<int[]>();
        for (int[] h : mask) {
            if (h.length != 2) {
                throw new IllegalArgumentException("A mask entry needs two intervals (b, c)");
            }
            int b = NumberTheory.mod(h[0], n);
            int c = NumberTheory.mod(h[1], n);
            if (!d.isConsonant(b) || !d.isConsonant(c)) {
                throw new IllegalArgumentException("The mask entry (" + h[0] + ", " + h[1]
                        + ") contains a dissonance");
            }
            if (labels.put(b * n + c, m.size() + 1) != null) {
                throw new IllegalArgumentException("The mask entry (" + b + ", " + c + ") repeats");
            }
            m.add(new int[] { b, c });
        }
        this.mask = Collections.unmodifiableList(m);
        List<State> s = new ArrayList<State>();
        for (int a = 0; a < n; a++) {
            for (int[] h : m) {
                State st = new State(a, h[0], h[1], n);
                stateIndex.put(st, s.size());
                s.add(st);
            }
        }
        this.states = Collections.unmodifiableList(s);
        this.xe = new BitSet(n * n);
        for (int x = 0; x < n; x++) {
            for (int y : d.getConsonances()) {
                xe.set(x * n + y);
            }
        }
    }

    /**
     * H_Fux of Def. 3.1 (p. 7): pairs of Fuxian consonances (b, c) with
     * c &minus; b not &plusmn;1 or &plusmn;2 and {b, c} &ne; {4, 8}, in Table
     * 8's order (p. 24), which sorts by b and then c.
     */
    public static int[][] fuxMask() {
        int[] k = StrongDichotomies.FUX.getConsonances();
        List<int[]> m = new ArrayList<int[]>();
        for (int b : k) {
            for (int c : k) {
                int diff = NumberTheory.mod(c - b, 12);
                if (diff == 1 || diff == 2 || diff == 10 || diff == 11) {
                    continue;
                }
                if ((b == 4 && c == 8) || (b == 8 && c == 4)) {
                    continue;
                }
                m.add(new int[] { b, c });
            }
        }
        return m.toArray(new int[m.size()][]);
    }

    private static ThreeVoiceCounterpoint fux;

    /** The model for the Fuxian dichotomy and H_Fux (spec D1). */
    public static synchronized ThreeVoiceCounterpoint fux() {
        if (fux == null) {
            fux = new ThreeVoiceCounterpoint(StrongDichotomies.FUX, fuxMask());
        }
        return fux;
    }

    /** The dichotomy. */
    public Dichotomy getDichotomy() { return dichotomy; }

    /** The mask's configurations (b, c) in label order. */
    public List<int[]> mask() {
        List<int[]> copy = new ArrayList<int[]>();
        for (int[] h : mask) {
            copy.add(h.clone());
        }
        return copy;
    }

    /** The label i of h_i for (b, c), or 0 if (b, c) is not in the mask. */
    public int label(int b, int c) {
        Integer l = labels.get(NumberTheory.mod(b, n) * n + NumberTheory.mod(c, n));
        return l == null ? 0 : l;
    }

    /** All states of the mask, by lower voice and then label. */
    public List<State> states() { return states; }

    /** True if the state's configuration lies in the mask. */
    public boolean inMask(State s) {
        return stateIndex.containsKey(s);
    }

    /** The state (a; b, c), reduced mod n; it need not lie in the mask. */
    public State state(int a, int b, int c) {
        return new State(a, b, c, n);
    }

    /** The state with these three pitch classes, from the lower voice up. */
    public State fromPitchClasses(int lower, int middle, int upper) {
        return new State(lower, middle - lower, upper - lower, n);
    }

    /** The pair's projection as {cantus, interval}. */
    public static int[] project(Pair p, State s) {
        switch (p) {
        case LM:
            return new int[] { s.a, s.b };
        case LU:
            return new int[] { s.a, s.c };
        case MU:
        default:
            return new int[] { (s.a + s.b) % s.n, NumberTheory.mod(s.c - s.b, s.n) };
        }
    }

    /** LM and LU, and MU when c &minus; b is a consonance (§2.3). */
    public List<Pair> activePairs(State s) {
        List<Pair> l = new ArrayList<Pair>();
        l.add(Pair.LM);
        l.add(Pair.LU);
        if (dichotomy.isConsonant(project(Pair.MU, s)[1])) {
            l.add(Pair.MU);
        }
        return l;
    }

    /**
     * The maximizing contrapuntal symmetries &Sigma;_X of the pair's
     * projection, as symmetries at its own cantus x: T^x &#8728; g &#8728;
     * T^&minus;x for the symmetries g at cantus 0 in H (spec D4).
     *
     * @throws IllegalArgumentException if the pair's interval is a dissonance
     */
    public List<DualAffine> symmetries(Pair p, State s) {
        int[] z = project(p, s);
        DualAffine to = new DualAffine(z[0], 0, 1, 0, n);
        DualAffine back = new DualAffine(NumberTheory.mod(-z[0], n), 0, 1, 0, n);
        List<DualAffine> out = new ArrayList<DualAffine>();
        for (DualAffine g : ContrapuntalSymmetries.analyse(dichotomy, 0, z[1]).getSymmetries()) {
            out.add(to.compose(g).compose(back));
        }
        return out;
    }

    /** The distinct preimages over the state list of g X[&epsilon;] for g in &Sigma; of the pair. */
    private List<BitSet> pairPreimages(Pair p, State s) {
        Map<String, BitSet> distinct = new LinkedHashMap<String, BitSet>();
        for (DualAffine g : symmetries(p, s)) {
            BitSet img = image(g);
            String key = p + "|" + img;
            BitSet pre = preimages.get(key);
            if (pre == null) {
                pre = new BitSet(states.size());
                for (int i = 0; i < states.size(); i++) {
                    int[] z = project(p, states.get(i));
                    if (img.get(z[0] * n + z[1])) {
                        pre.set(i);
                    }
                }
                preimages.put(key, pre);
            }
            distinct.put(key, pre);
        }
        return new ArrayList<BitSet>(distinct.values());
    }

    /** The union over the pair's maximizing symmetries of g X[&epsilon;], over Z_n &times; Z_n. */
    private BitSet unionImage(Pair p, State s) {
        BitSet u = new BitSet(n * n);
        for (DualAffine g : symmetries(p, s)) {
            u.or(image(g));
        }
        return u;
    }

    private BitSet image(DualAffine g) {
        BitSet img = new BitSet(n * n);
        for (int i = xe.nextSetBit(0); i >= 0; i = xe.nextSetBit(i + 1)) {
            int x = i / n;
            int y = i % n;
            img.set(g.applyCantus(x, y) * n + g.applyInterval(x, y));
        }
        return img;
    }

    /** The union of the maximal intersections over all tuples, starting from base. */
    private static BitSet jointUnion(List<List<BitSet>> lists, BitSet base) {
        int[] pos = new int[lists.size()];
        int best = -1;
        BitSet union = new BitSet();
        while (true) {
            BitSet m = (BitSet) base.clone();
            for (int i = 0; i < pos.length; i++) {
                m.and(lists.get(i).get(pos[i]));
            }
            int c = m.cardinality();
            if (c > best) {
                best = c;
                union = m;
            }
            else if (c == best) {
                union.or(m);
            }
            int i = 0;
            while (i < pos.length && ++pos[i] == lists.get(i).size()) {
                pos[i] = 0;
                i++;
            }
            if (i == pos.length) {
                return union;
            }
        }
    }

    private void requireInMask(State s) {
        if (!inMask(s)) {
            throw new IllegalArgumentException("The state " + s + " is not in the mask");
        }
    }

    private BitSet allStates() {
        BitSet b = new BitSet(states.size());
        b.set(0, states.size());
        return b;
    }

    /** S_H(&xi;) of eq. (2) as a set of state indices. */
    private synchronized BitSet strictSuccessors(State s) {
        BitSet r = strictCache.get(s);
        if (r == null) {
            List<List<BitSet>> lists = new ArrayList<List<BitSet>>();
            for (Pair p : activePairs(s)) {
                lists.add(pairPreimages(p, s));
            }
            r = jointUnion(lists, allStates());
            strictCache.put(s, r);
        }
        return r;
    }

    /**
     * The hold union for a source and its set of still pairs (spec D6 step 3,
     * D12): with fibre, the maximization runs over the states that keep the
     * held pairs fixed; without, over all states in the mask.
     */
    private synchronized BitSet holdUnion(State s, List<Pair> still, boolean fibre) {
        String key = s + "|" + still + "|" + fibre;
        BitSet r = holdCache.get(key);
        if (r == null) {
            BitSet base = new BitSet(states.size());
            for (int i = 0; i < states.size(); i++) {
                boolean keeps = true;
                for (Pair p : still) {
                    if (fibre && !java.util.Arrays.equals(project(p, s), project(p, states.get(i)))) {
                        keeps = false;
                        break;
                    }
                }
                if (keeps) {
                    base.set(i);
                }
            }
            List<List<BitSet>> lists = new ArrayList<List<BitSet>>();
            for (Pair p : activePairs(s)) {
                if (!still.contains(p)) {
                    lists.add(pairPreimages(p, s));
                }
            }
            r = jointUnion(lists, base);
            holdCache.put(key, r);
        }
        return r;
    }

    private List<Pair> stillPairs(State s, State t) {
        List<Pair> still = new ArrayList<Pair>();
        for (Pair p : activePairs(s)) {
            if (java.util.Arrays.equals(project(p, s), project(p, t))) {
                still.add(p);
            }
        }
        return still;
    }

    /** True if t is an admitted successor of s in the mode; false if either lies outside the mask. */
    public boolean isAdmitted(State s, State t, RelaxationMode mode) {
        if (!inMask(s) || !inMask(t)) {
            return false;
        }
        int ti = stateIndex.get(t);
        if (mode == RelaxationMode.STRICT) {
            return strictSuccessors(s).get(ti);
        }
        List<Pair> still = stillPairs(s, t);
        if (mode == RelaxationMode.REFLEXIVE) {
            return pairsAdmit(s, ti, still);
        }
        if (still.size() == activePairs(s).size()) {
            return true;
        }
        return holdUnion(s, still, mode == RelaxationMode.HOLD_FIBRE).get(ti);
    }

    /**
     * True if every active pair of s outside skip has its target projection,
     * for the state at index ti, in some image of its maximizing symmetries.
     */
    private boolean pairsAdmit(State s, int ti, List<Pair> skip) {
        for (Pair p : activePairs(s)) {
            if (skip.contains(p)) {
                continue;
            }
            boolean some = false;
            for (BitSet pre : pairPreimages(p, s)) {
                if (pre.get(ti)) {
                    some = true;
                    break;
                }
            }
            if (!some) {
                return false;
            }
        }
        return true;
    }

    /**
     * The admitted successors of s in the state list's order.
     *
     * @throws IllegalArgumentException if s is not in the mask
     */
    public List<State> successors(State s, RelaxationMode mode) {
        requireInMask(s);
        List<State> out = new ArrayList<State>();
        if (mode == RelaxationMode.STRICT) {
            BitSet b = strictSuccessors(s);
            for (int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
                out.add(states.get(i));
            }
            return out;
        }
        for (State t : states) {
            if (isAdmitted(s, t, mode)) {
                out.add(t);
            }
        }
        return out;
    }

    /** The decision on s &#8594; t with every reason for a refusal (spec D5). */
    public Verdict check(State s, State t, RelaxationMode mode) {
        List<Refusal> out = new ArrayList<Refusal>();
        if (!inMask(s)) {
            out.add(new Refusal(Reason.SOURCE_OUTSIDE_MASK, null, null, null));
        }
        if (!inMask(t)) {
            out.add(new Refusal(Reason.TARGET_OUTSIDE_MASK, null, null, null));
        }
        if (!out.isEmpty()) {
            return new Verdict(s, t, mode, out);
        }
        List<Pair> still = stillPairs(s, t);
        List<Pair> moving = new ArrayList<Pair>();
        for (Pair p : activePairs(s)) {
            if (!still.contains(p)) {
                moving.add(p);
            }
        }
        if (mode == RelaxationMode.STRICT) {
            for (Pair p : still) {
                int[] z = project(p, s);
                out.add(new Refusal(Reason.PAIR_STILL, p, z, z));
            }
        }
        else if (moving.isEmpty()) {
            return new Verdict(s, t, mode, out);
        }
        List<Refusal> fifths = new ArrayList<Refusal>();
        List<Refusal> excluded = new ArrayList<Refusal>();
        for (Pair p : moving) {
            int[] z0 = project(p, s);
            int[] z1 = project(p, t);
            if (z0[1] == fifth() && z1[1] == fifth()) {
                fifths.add(new Refusal(Reason.FIFTH_TO_FIFTH, p, z0, z1));
            }
            else if (!unionImage(p, s).get(z1[0] * n + z1[1])) {
                excluded.add(new Refusal(Reason.PAIR_EXCLUDED, p, z0, z1));
            }
        }
        out.addAll(fifths);
        out.addAll(excluded);
        if (out.isEmpty() && !isAdmitted(s, t, mode)) {
            out.add(new Refusal(Reason.NOT_MAXIMAL, null, null, null));
        }
        return new Verdict(s, t, mode, out);
    }

    /** The perfect fifth, n &times; 7 / 12, or -1 when n is not a multiple of 12. */
    private int fifth() {
        return n % 12 == 0 ? 7 * n / 12 : -1;
    }

    /**
     * The paper's V1 baseline (§5, p. 16; spec D10): each active pair's
     * projection must lie in some image of its maximizing symmetries, with no
     * joint maximization.
     */
    public boolean isAdmittedV1(State s, State t) {
        if (!inMask(s) || !inMask(t)) {
            return false;
        }
        int ti = stateIndex.get(t);
        for (Pair p : activePairs(s)) {
            boolean some = false;
            for (BitSet pre : pairPreimages(p, s)) {
                if (pre.get(ti)) {
                    some = true;
                    break;
                }
            }
            if (!some) {
                return false;
            }
        }
        return true;
    }

    /**
     * The 288 parsimonious realizations of §6.4 (p. 23) for n = 12: for each
     * of the 24 triads, each of P, L and R, and each voicing type 135, 153,
     * 351, 315, the source voicing and the target in which the one voice that
     * changes moves to the new pitch class (spec D11).
     *
     * @throws IllegalStateException if n is not 12
     */
    public List<Realization> parsimoniousRealizations() {
        if (n != 12) {
            throw new IllegalStateException("P, L and R need Z_12");
        }
        String[] types = { "135", "153", "351", "315" };
        List<Realization> out = new ArrayList<Realization>();
        for (int r = 0; r < 12; r++) {
            for (int m = 0; m < 2; m++) {
                boolean major = m == 0;
                int[] x = triad(r, major);
                for (char name : new char[] { 'P', 'L', 'R' }) {
                    int[] y;
                    if (name == 'P') {
                        y = triad(r, !major);
                    }
                    else if (name == 'L') {
                        y = major ? triad(r + 4, false) : triad(r + 8, true);
                    }
                    else {
                        y = major ? triad(r + 9, false) : triad(r + 3, true);
                    }
                    int gone = -1;
                    int added = -1;
                    for (int q : x) {
                        if (!contains(y, q)) {
                            gone = q;
                        }
                    }
                    for (int q : y) {
                        if (!contains(x, q)) {
                            added = q;
                        }
                    }
                    for (String type : types) {
                        int[] pcs = new int[3];
                        for (int i = 0; i < 3; i++) {
                            char ch = type.charAt(i);
                            pcs[i] = ch == '1' ? x[0] : ch == '3' ? x[1] : x[2];
                        }
                        State src = fromPitchClasses(pcs[0], pcs[1], pcs[2]);
                        for (int i = 0; i < 3; i++) {
                            if (pcs[i] == gone) {
                                pcs[i] = added;
                            }
                        }
                        State tgt = fromPitchClasses(pcs[0], pcs[1], pcs[2]);
                        out.add(new Realization(name, r, major, type, src, tgt, inMask(tgt)));
                    }
                }
            }
        }
        return out;
    }

    private static int[] triad(int r, boolean major) {
        return new int[] { NumberTheory.mod(r, 12), NumberTheory.mod(r + (major ? 4 : 3), 12),
                           NumberTheory.mod(r + 7, 12) };
    }

    private static boolean contains(int[] a, int v) {
        for (int x : a) {
            if (x == v) {
                return true;
            }
        }
        return false;
    }
}
