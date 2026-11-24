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

package org.rubato.math.certificates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Proof certificates in Agda for properties of pitch-class sets and keys, after
 * Flieder's propositions-as-types examples (arXiv 2512.04090: Ex. 2.3, 2.4, 2.6
 * on pp. 11-13, Ex. B.2 and B.3 on pp. 51-53).
 * <p>
 * Each method decides a claim in Java and writes an Agda definition whose type
 * is the claim (or its negation) as spec/agda/Certificates.agda states it, and
 * whose body is the proof: witnesses for a positive claim, an exhaustive case
 * split for a negative one. Agda checks the term against its own definitions, so
 * a wrong verdict or a wrong witness gives a term that does not typecheck. The
 * departures from the paper are listed in sources/AGDA-CERTIFICATES-SPEC.md.
 */
public final class AgdaCertificates {

    private AgdaCertificates() {}

    /** Largest modulus accepted for pitch-class sets. */
    public static final int MAX_N = 24;

    private static final String[] NAMES = { "C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B" };

    private static final Pattern DEFINITION = Pattern.compile("[a-z][A-Za-z0-9]*");
    private static final Pattern MODULE = Pattern.compile("[A-Z][A-Za-z0-9]*");

    /** Names Prelude.agda and Certificates.agda define or Agda reserves; a certificate may not reuse them. */
    private static final Set<String> RESERVED = new HashSet<String>(Arrays.asList(
        "all", "any", "cong", "cmpChars", "cmpInt", "cmpNat", "cmpString", "eqChars", "flipO", "length",
        "map", "not", "nth", "sym", "thenO", "here", "there", "half", "pcint", "intclass", "steps", "scale",
        "degree", "triad", "leadingTone", "major", "harmonicMinor", "naturalMinor", "refl", "true", "false",
        "suc", "zero", "just", "nothing", "tt", "fst", "snd", "data", "where", "module", "open", "import",
        "let", "in", "with", "record", "field", "constructor", "infix", "infixl", "infixr", "postulate",
        "primitive", "abstract", "private", "instance", "mutual", "rewrite", "using", "hiding", "renaming",
        "public", "forall", "quote", "unquote", "tactic", "pattern", "syntax", "variable", "macro",
        "interleaved", "opaque", "unfolding", "eta", "inductive", "coinductive", "overlap", "do", "miss",
        "out", "f", "i", "p"));

    private static final Set<String> RESERVED_MODULES = new HashSet<String>(Arrays.asList(
        "Prelude", "Certificates", "Agda", "Set", "Prop"));

    /** The three keys' modes of spec D5, with their steps from the tonic. */
    public enum Mode {
        MAJOR("major", "major", 0, 2, 4, 5, 7, 9, 11),
        HARMONIC_MINOR("harmonicMinor", "harmonic minor", 0, 2, 3, 5, 7, 8, 11),
        NATURAL_MINOR("naturalMinor", "natural minor", 0, 2, 3, 5, 7, 8, 10);

        private final String agda;
        private final String label;
        private final int[] steps;

        Mode(String agda, String label, int... steps) {
            this.agda = agda;
            this.label = label;
            this.steps = steps;
        }

        /** The constructor name in Certificates.agda. */
        public String agda() {
            return agda;
        }

        /** The mode as words, e.g. "harmonic minor". */
        public String label() {
            return label;
        }

        /**
         * Reads a mode in any letter case: major, harmonic (minor), natural (minor),
         * or the constant's name.
         *
         * @throws IllegalArgumentException for anything else
         */
        public static Mode parse(String s) {
            String t = s.trim().toLowerCase(Locale.ROOT).replace('_', ' ').replaceAll("\\s+", " ");
            if (t.equals("major")) {
                return MAJOR;
            }
            if (t.equals("harmonic") || t.equals("harmonic minor") || t.equals("harmonicminor")) {
                return HARMONIC_MINOR;
            }
            if (t.equals("natural") || t.equals("natural minor") || t.equals("naturalminor") || t.equals("minor")) {
                return NATURAL_MINOR;
            }
            throw new IllegalArgumentException("Unknown mode \"" + s + "\"; use major, harmonic minor or natural minor.");
        }
    }

    /** A claim, Java's verdict on it and the Agda definition that proves the verdict. */
    public static final class Certificate {
        /** The Agda name of the definition. */
        public final String name;
        /** The claim in words, including the verdict. */
        public final String claim;
        /** Whether the claim of the paper's type holds (true for a vacuous proof). */
        public final boolean holds;
        /** The Agda definition: type signature and clauses, one comment line first. */
        public final String agda;

        Certificate(String name, String claim, boolean holds, String agda) {
            this.name = name;
            this.claim = claim;
            this.holds = holds;
            this.agda = agda;
        }
    }

    // ------------------------------------------------------------------
    // Values

    /** pcint(x, y) = y - x in Z_n (Ex. B.2; D2). */
    public static int pcint(int n, int x, int y) {
        return Math.floorMod(y - x, n);
    }

    /** intclass(pcint(x, y)): the representative min(d, n - d) of the interval class (D2). */
    public static int intervalClass(int n, int x, int y) {
        int d = pcint(n, x, y);
        return Math.min(d, n - d);
    }

    /** floor(n / 2), the largest interval class; IC has half(n) + 1 elements. */
    public static int half(int n) {
        return n / 2;
    }

    /** The distinct pitch classes mod n of some pitches, ascending (D1, D8). */
    public static int[] pitchClassSet(int n, int... pitches) {
        checkModulus(n);
        TreeSet<Integer> s = new TreeSet<Integer>();
        for (int p : pitches) {
            s.add(Math.floorMod(p, n));
        }
        int[] c = new int[s.size()];
        int k = 0;
        for (int x : s) {
            c[k++] = x;
        }
        return c;
    }

    /** The interval classes no ordered pair of the set reaches, ascending. */
    public static List<Integer> missingClasses(int n, int[] set) {
        checkSet(n, set);
        boolean[] hit = new boolean[half(n) + 1];
        for (int x : set) {
            for (int y : set) {
                hit[intervalClass(n, x, y)] = true;
            }
        }
        List<Integer> m = new ArrayList<Integer>();
        for (int i = 0; i < hit.length; i++) {
            if (!hit[i]) {
                m.add(i);
            }
        }
        return m;
    }

    /** Whether every interval class of Z_n occurs between two elements of the set (Ex. B.2). */
    public static boolean isAllInterval(int n, int[] set) {
        return missingClasses(n, set).isEmpty();
    }

    /** The key's seven pitch classes, listed from the tonic (D5). */
    public static int[] scale(int tonic, Mode m) {
        checkTonic(tonic);
        int[] s = new int[7];
        for (int i = 0; i < 7; i++) {
            s[i] = (tonic + m.steps[i]) % 12;
        }
        return s;
    }

    /** Degree d (1 = tonic; above 7 wraps), as Ex. 2.3's stacking needs (D5). */
    public static int degree(int tonic, Mode m, int d) {
        if (d < 1) {
            throw new IllegalArgumentException("Scale degrees start at 1, not " + d + ".");
        }
        return scale(tonic, m)[(d - 1) % 7];
    }

    /** triad(k, d) = {s_d, s_(d+2), s_(d+4)} (Ex. 2.3), in that order. */
    public static int[] triad(int tonic, Mode m, int d) {
        return new int[] { degree(tonic, m, d), degree(tonic, m, d + 2), degree(tonic, m, d + 4) };
    }

    /** lt(k): the pitch class a semitone below the tonic (D6). */
    public static int leadingTone(int tonic) {
        checkTonic(tonic);
        return (tonic + 11) % 12;
    }

    /** Ex. 2.6: degree 7 lies a semitone below degree 1. */
    public static boolean containsLeadingTone(int tonic, Mode m) {
        return degree(tonic, m, 7) == (degree(tonic, m, 1) + 11) % 12;
    }

    // ------------------------------------------------------------------
    // Certificates

    /** The proof that the element at this position of a list is in the list. */
    public static String membership(int position) {
        if (position == 0) {
            return "here";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 1; i < position; i++) {
            b.append("there (");
        }
        b.append("there here");
        for (int i = 1; i < position; i++) {
            b.append(')');
        }
        return b.toString();
    }

    /**
     * Ex. B.2: a proof of {@code AllInterval n C}, or of its negation from the
     * smallest missing interval class (D4).
     */
    public static Certificate allInterval(String name, int n, int[] set) {
        checkName(name);
        List<Integer> missing = missingClasses(n, set);
        String c = list(set);
        String setText = braces(set);
        StringBuilder b = new StringBuilder();
        if (missing.isEmpty()) {
            String claim = setText + " is all-interval in Z" + n;
            b.append("-- ").append(claim).append('\n');
            b.append(name).append(" : AllInterval ").append(n).append(' ').append(c).append('\n');
            for (int i = 0; i <= half(n); i++) {
                int[] w = witness(n, set, i);
                b.append(name).append(' ').append(i).append(" _ = ").append(set[w[0]]).append(" , ").append(set[w[1]])
                 .append(" , ").append(membership(w[0])).append(" , ").append(membership(w[1])).append(" , refl\n");
            }
            b.append(name).append(' ').append(sucs(half(n) + 1, "i")).append(' ').append(steps(half(n), "()")).append('\n');
            return new Certificate(name, claim, true, b.toString());
        }
        int i = missing.get(0);
        String claim = setText + " is not all-interval in Z" + n + ": no pair has interval class " + i;
        b.append("-- ").append(claim).append('\n');
        b.append(name).append(" : ¬ AllInterval ").append(n).append(' ').append(c).append('\n');
        b.append(name).append(" f = miss (f ").append(i).append(' ').append(steps(i, "z≤n")).append(")\n");
        b.append("  where\n");
        b.append("  miss : ¬ Hit ").append(n).append(' ').append(c).append(' ').append(i).append('\n');
        if (set.length == 0) {
            b.append("  miss (_ , _ , () , _)\n");
        }
        for (int x = 0; x < set.length; x++) {
            for (int y = 0; y < set.length; y++) {
                b.append("  miss (_ , _ , ").append(membership(x)).append(" , ").append(membership(y)).append(" , ())\n");
            }
        }
        return new Certificate(name, claim, false, b.toString());
    }

    /** The first pair of positions (x before y in the set's order) whose interval class is i. */
    private static int[] witness(int n, int[] set, int i) {
        for (int x = 0; x < set.length; x++) {
            for (int y = 0; y < set.length; y++) {
                if (intervalClass(n, set[x], set[y]) == i) {
                    return new int[] { x, y };
                }
            }
        }
        throw new IllegalStateException("no pair for interval class " + i);
    }

    /**
     * Ex. B.3 under D6: in the key, every chord with dominant function contains
     * the leading tone; vacuous when degree 7 is not a leading tone.
     */
    public static Certificate dominantContainsLeadingTone(String name, int tonic, Mode m) {
        checkName(name);
        checkTonic(tonic);
        String key = NAMES[tonic] + " " + m.label();
        StringBuilder b = new StringBuilder();
        String claim;
        String type = name + " : DominantContainsLeadingTone " + tonic + " " + m.agda() + "\n";
        if (containsLeadingTone(tonic, m)) {
            int[] v = triad(tonic, m, 5);
            int lt = leadingTone(tonic);
            int pos = -1;
            for (int k = 0; k < v.length; k++) {
                if (v[k] == lt) {
                    pos = k;
                    break;
                }
            }
            if (pos < 0) {
                throw new IllegalStateException("the leading tone is not in V of " + key);
            }
            claim = "In " + key + " the dominant triad {" + NAMES[v[0]] + ", " + NAMES[v[1]] + ", " + NAMES[v[2]]
                  + "} contains the leading tone " + NAMES[lt];
            b.append("-- ").append(claim).append('\n').append(type);
            b.append(name).append(" (_ , refl , _) = ").append(membership(pos)).append('\n');
        }
        else {
            claim = "In " + key + " degree 7 (" + NAMES[degree(tonic, m, 7)] + ") is not a leading tone, so no chord"
                  + " has dominant function and the claim holds vacuously";
            b.append("-- ").append(claim).append('\n').append(type);
            b.append(name).append(" (_ , _ , ())\n");
        }
        return new Certificate(name, claim, true, b.toString());
    }

    /**
     * Ex. 2.4 for a list of pitch classes (D7): every one lies in the key's scale,
     * or the first one outside it does not.
     */
    public static Certificate scaleMembership(String name, int tonic, Mode m, int[] pcs) {
        checkName(name);
        int[] s = scale(tonic, m);
        for (int p : pcs) {
            if (p < 0 || p > 11) {
                throw new IllegalArgumentException("Pitch class " + p + " is outside 0 to 11.");
            }
        }
        String key = NAMES[tonic] + " " + m.label();
        String pred = "(λ p → p ∈ scale " + tonic + " " + m.agda() + ")";
        String ps = list(pcs);
        StringBuilder b = new StringBuilder();
        int outside = -1;
        for (int k = 0; k < pcs.length && outside < 0; k++) {
            if (position(s, pcs[k]) < 0) {
                outside = k;
            }
        }
        if (outside < 0) {
            String claim = "All " + pcs.length + " pitch classes lie in " + key;
            b.append("-- ").append(claim).append('\n');
            b.append(name).append(" : All ").append(pred).append(' ').append(ps).append('\n');
            b.append(name).append(" = ");
            for (int p : pcs) {
                b.append(membership(position(s, p))).append(" ∷ ");
            }
            b.append("[]\n");
            return new Certificate(name, claim, true, b.toString());
        }
        int p = pcs[outside];
        String claim = "Pitch class " + p + " (note " + (outside + 1) + ") lies outside " + key;
        b.append("-- ").append(claim).append('\n');
        b.append(name).append(" : ¬ All ").append(pred).append(' ').append(ps).append('\n');
        b.append(name).append(" (");
        for (int k = 0; k < outside; k++) {
            b.append("_ ∷ ");
        }
        b.append("p ∷ _) = out p\n");
        b.append("  where\n");
        b.append("  out : ¬ (").append(p).append(" ∈ scale ").append(tonic).append(' ').append(m.agda()).append(")\n");
        StringBuilder pat = new StringBuilder();
        for (int k = 0; k < s.length; k++) {
            pat.append("(there ");
        }
        pat.append("()");
        for (int k = 0; k < s.length; k++) {
            pat.append(')');
        }
        b.append("  out ").append(pat).append('\n');
        return new Certificate(name, claim, false, b.toString());
    }

    private static int position(int[] s, int p) {
        for (int k = 0; k < s.length; k++) {
            if (s[k] == p) {
                return k;
            }
        }
        return -1;
    }

    /** A whole module: options, header comment, imports and the certificates in order. */
    public static String module(String moduleName, List<Certificate> cs) {
        if (moduleName == null || !MODULE.matcher(moduleName).matches() || RESERVED_MODULES.contains(moduleName)) {
            throw new IllegalArgumentException("The module name \"" + moduleName + "\" must start with a capital letter,"
                                             + " use only letters and digits, and not be Prelude or Certificates.");
        }
        Set<String> seen = new HashSet<String>();
        for (Certificate c : cs) {
            if (!seen.add(c.name)) {
                throw new IllegalArgumentException("Two certificates are named " + c.name + ".");
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("{-# OPTIONS --safe --without-K #-}\n\n");
        b.append("-- ").append(moduleName).append(".agda: proof certificates written by Rubato's Agda certificates\n");
        b.append("-- (org.rubato.math.certificates.AgdaCertificates). Each definition's type is a claim of\n");
        b.append("-- spec/agda/Certificates.agda and its body is the proof; check the file with\n");
        b.append("-- spec/agda/check-certificate.sh ").append(moduleName).append(".agda\n\n");
        b.append("module ").append(moduleName).append(" where\n\n");
        b.append("open import Prelude\n");
        b.append("open import Certificates\n");
        for (Certificate c : cs) {
            b.append('\n').append(c.agda);
        }
        return b.toString();
    }

    /** The module committed as spec/agda/GeneratedCertificates.agda (spec section 2, item 3). */
    public static String exampleModule() {
        List<Certificate> cs = new ArrayList<Certificate>();
        cs.add(allInterval("ai0146", 12, new int[] { 0, 1, 4, 6 }));
        cs.add(allInterval("ai0137", 12, new int[] { 0, 1, 3, 7 }));
        cs.add(allInterval("ai013z7", 7, new int[] { 0, 1, 3 }));
        cs.add(allInterval("chromatic", 12, new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }));
        cs.add(allInterval("cluster0123", 12, new int[] { 0, 1, 2, 3 }));
        cs.add(allInterval("majorTriad", 12, new int[] { 0, 4, 7 }));
        cs.add(allInterval("emptySet", 12, new int[0]));
        cs.add(allInterval("trichordZ24", 24, new int[] { 0, 1, 3 }));
        for (Mode m : Mode.values()) {
            for (int t = 0; t < 12; t++) {
                cs.add(dominantContainsLeadingTone("dom" + suffix(m) + t, t, m));
            }
        }
        cs.add(scaleMembership("cMajorChord", 0, Mode.MAJOR, new int[] { 0, 4, 7, 11 }));
        cs.add(scaleMembership("aHarmonicLine", 9, Mode.HARMONIC_MINOR, new int[] { 9, 11, 0, 8, 9 }));
        cs.add(scaleMembership("noNotes", 0, Mode.MAJOR, new int[0]));
        cs.add(scaleMembership("tritoneOutside", 0, Mode.MAJOR, new int[] { 0, 6, 1 }));
        cs.add(scaleMembership("gSharpOutside", 9, Mode.NATURAL_MINOR, new int[] { 9, 8 }));
        String m = module("GeneratedCertificates", cs);
        return m.replace("-- GeneratedCertificates.agda: proof certificates written by",
                         "-- GeneratedCertificates.agda: do not edit; spec/agda/gen/ExportCertificates.java writes it\n"
                       + "-- from AgdaCertificates.exampleModule(). Proof certificates written by");
    }

    private static String suffix(Mode m) {
        switch (m) {
            case MAJOR: return "Major";
            case HARMONIC_MINOR: return "Harmonic";
            default: return "Natural";
        }
    }

    // ------------------------------------------------------------------
    // Text helpers and checks

    /** An Agda list literal of naturals. */
    static String list(int[] xs) {
        if (xs.length == 0) {
            return "[]";
        }
        StringBuilder b = new StringBuilder("(");
        for (int x : xs) {
            b.append(x).append(" ∷ ");
        }
        return b.append("[])").toString();
    }

    private static String braces(int[] xs) {
        return "{" + Arrays.toString(xs).replace("[", "").replace("]", "") + "}";
    }

    /** suc applied k times around a variable. */
    private static String sucs(int k, String var) {
        StringBuilder b = new StringBuilder(var);
        for (int i = 0; i < k; i++) {
            b.insert(0, "(suc ").append(')');
        }
        return b.toString();
    }

    /** s≤s applied k times around a term. */
    private static String steps(int k, String inner) {
        if (k == 0) {
            return inner;
        }
        StringBuilder b = new StringBuilder(inner);
        for (int i = 0; i < k; i++) {
            b.insert(0, "(s≤s ").append(')');
        }
        return b.toString();
    }

    private static void checkName(String name) {
        if (name == null || !DEFINITION.matcher(name).matches() || RESERVED.contains(name)) {
            throw new IllegalArgumentException("The certificate name \"" + name + "\" must start with a small letter,"
                                             + " use only letters and digits, and not be a name the library defines.");
        }
    }

    private static void checkModulus(int n) {
        if (n < 2 || n > MAX_N) {
            throw new IllegalArgumentException("The modulus must be from 2 to " + MAX_N + ", not " + n + ".");
        }
    }

    private static void checkTonic(int tonic) {
        if (tonic < 0 || tonic > 11) {
            throw new IllegalArgumentException("The tonic must be a pitch class from 0 to 11, not " + tonic + ".");
        }
    }

    private static void checkSet(int n, int[] set) {
        checkModulus(n);
        for (int k = 0; k < set.length; k++) {
            if (set[k] < 0 || set[k] >= n) {
                throw new IllegalArgumentException("Pitch class " + set[k] + " is outside 0 to " + (n - 1) + ".");
            }
            if (k > 0 && set[k] <= set[k - 1]) {
                throw new IllegalArgumentException("A pitch-class set must be listed ascending without repeats.");
            }
        }
    }
}
