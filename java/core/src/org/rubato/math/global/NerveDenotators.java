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

package org.rubato.math.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.rubato.base.Repository;
import org.rubato.base.RubatoException;
import org.rubato.math.module.ZnElement;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;

/**
 * The nerve of a pitch-class covering as a denotator.
 * <p>
 * Topos I &sect;13.4.1 (p. 259) builds the forms G<sub>0</sub> = F and
 * G<sub>i+1</sub> = Power(G<sub>i</sub>) over the form F of a local
 * composition K. The composition K is in G<sub>1</sub>, a covering I of K is
 * in G<sub>2</sub>, and the nerve n(I) is in G<sub>3</sub>. The repository
 * registers these forms for F = {@value #PITCH_CLASS}, a simple form over
 * Z<sub>12</sub>, under the names {@value #PITCH_CLASS_SET},
 * {@value #PITCH_CLASS_COVERING} and {@value #PITCH_CLASS_NERVE}.
 * <p>
 * {@link #encode} writes the nerve of a zero-addressed global composition in
 * Z<sub>12</sub> as a {@value #PITCH_CLASS_NERVE} denotator: one factor per
 * simplex, each the set of its charts, each chart the set of its pitch
 * classes. All of these denotators are anonymous, because Rubato's XML
 * writer stores named dependencies under their names and would merge equally
 * named charts of different nerves. {@link #violations} checks that a
 * denotator of that form is the nerve of the covering its vertices form
 * (closed under faces, a common point in every simplex, every star
 * present). {@link #decode}, {@link #covering} and {@link #support} read it
 * back; the last two are the union operator of p. 259.
 */
public final class NerveDenotators {

    public static final String PITCH_CLASS = "PitchClass";
    public static final String PITCH_CLASS_SET = "PitchClassSet";
    public static final String PITCH_CLASS_COVERING = "PitchClassCovering";
    public static final String PITCH_CLASS_NERVE = "PitchClassNerve";

    private NerveDenotators() { /* static utility */ }

    /** A decoded nerve: its covering sets and the complex on them. */
    public static final class Decoded {
        /** The covering sets as sorted pitch classes, in vertex order (lexicographic). */
        public final List<int[]> covering;
        /** The nerve; vertex i is covering set i, labelled like {0,4,7}. */
        public final SimplicialComplex complex;

        Decoded(List<int[]> covering, SimplicialComplex complex) {
            this.covering = Collections.unmodifiableList(covering);
            this.complex = complex;
        }
    }

    private static Form form(String name) {
        Form f = Repository.systemRepository().getForm(name);
        if (f == null) {
            throw new IllegalStateException("The repository has no form " + name);
        }
        return f;
    }

    /**
     * The nerve of {@code g} as an anonymous {@value #PITCH_CLASS_NERVE}
     * denotator.
     *
     * @throws IllegalArgumentException if the support is not a set of
     *         pitch classes, that is, not in Z<sub>12</sub><sup>1</sup>
     */
    public static Denotator encode(GlobalComposition g) {
        LocalComposition k = g.getSupport();
        if (k.getModulus() != 12 || k.getDimension() != 1) {
            throw new IllegalArgumentException("A nerve denotator needs pitch classes, a support in Z12; this one is in Z"
                    + k.getModulus() + "^" + k.getDimension());
        }
        SimpleForm pc = (SimpleForm) form(PITCH_CLASS);
        PowerForm set = (PowerForm) form(PITCH_CLASS_SET);
        PowerForm covering = (PowerForm) form(PITCH_CLASS_COVERING);
        PowerForm nerveForm = (PowerForm) form(PITCH_CLASS_NERVE);
        try {
            List<Denotator> charts = new ArrayList<Denotator>();
            for (int i = 0; i < g.coveringSize(); i++) {
                List<Denotator> points = new ArrayList<Denotator>();
                for (int x : g.coveringComposition(i).classes()) {
                    points.add(new SimpleDenotator(null, pc, new ZnElement(x, 12)));
                }
                charts.add(new PowerDenotator(null, set, points));
            }
            List<Denotator> simplexes = new ArrayList<Denotator>();
            for (int[] s : g.nerve().allSimplexes()) {
                List<Denotator> members = new ArrayList<Denotator>();
                for (int v : s) {
                    members.add(charts.get(v));
                }
                simplexes.add(new PowerDenotator(null, covering, members));
            }
            return new PowerDenotator(null, nerveForm, simplexes);
        }
        catch (RubatoException e) {
            throw new IllegalStateException("Could not build the nerve denotator: " + e.getMessage(), e);
        }
    }

    /** A chart read from a {@value #PITCH_CLASS_SET} denotator, or null with a message added. */
    private static int[] chart(Denotator c, List<String> problems) {
        if (!(c instanceof PowerDenotator) || !c.getForm().getNameString().equals(PITCH_CLASS_SET)) {
            problems.add("a chart has form " + c.getForm().getNameString() + ", not " + PITCH_CLASS_SET);
            return null;
        }
        TreeSet<Integer> xs = new TreeSet<Integer>();
        for (Denotator p : ((PowerDenotator) c).getFactors()) {
            if (!(p instanceof SimpleDenotator) || !(((SimpleDenotator) p).getElement() instanceof ZnElement)
                || ((ZnElement) ((SimpleDenotator) p).getElement()).getModulus() != 12) {
                problems.add("a chart holds " + p + ", not a pitch class");
                return null;
            }
            xs.add(((ZnElement) ((SimpleDenotator) p).getElement()).getValue());
        }
        if (xs.isEmpty()) {
            problems.add("a covering set is empty");
            return null;
        }
        int[] out = new int[xs.size()];
        int i = 0;
        for (int x : xs) {
            out[i++] = x;
        }
        return out;
    }

    /** The parsed simplexes as sets of chart keys, with the charts by key; null after a form problem. */
    private static List<Set<String>> parse(Denotator nerve, Map<String, int[]> charts, List<String> problems) {
        if (nerve == null) {
            problems.add("the denotator is null");
            return null;
        }
        if (!(nerve instanceof PowerDenotator) || !nerve.getForm().getNameString().equals(PITCH_CLASS_NERVE)) {
            problems.add("the denotator has form " + nerve.getForm().getNameString() + ", not " + PITCH_CLASS_NERVE);
            return null;
        }
        if (!nerve.nullAddressed()) {
            problems.add("the denotator has address " + nerve.getAddress() + "; only zero-addressed nerves are read");
            return null;
        }
        List<Set<String>> simplexes = new ArrayList<Set<String>>();
        for (Denotator s : ((PowerDenotator) nerve).getFactors()) {
            if (!(s instanceof PowerDenotator) || !s.getForm().getNameString().equals(PITCH_CLASS_COVERING)) {
                problems.add("a simplex has form " + s.getForm().getNameString() + ", not " + PITCH_CLASS_COVERING);
                return null;
            }
            Set<String> keys = new TreeSet<String>();
            for (Denotator c : ((PowerDenotator) s).getFactors()) {
                int[] ch = chart(c, problems);
                if (ch == null) {
                    return null;
                }
                String key = key(ch);
                charts.put(key, ch);
                keys.add(key);
            }
            if (keys.isEmpty()) {
                problems.add("a simplex is empty");
                return null;
            }
            simplexes.add(keys);
        }
        return simplexes;
    }

    private static String key(int[] chart) {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < chart.length; i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(chart[i]);
        }
        return b.append('}').toString();
    }

    /**
     * Lists why {@code nerve} is not the nerve of the covering its vertices
     * form; an empty list means it is. Checked: the forms, a common point in
     * every simplex (&sect;13.2.1, p. 253), closure under faces (App. H.2.1,
     * p. 1445), and the star st(x) of every point x as a simplex (p. 259),
     * which with closure gives every set of charts with a common point.
     */
    public static List<String> violations(Denotator nerve) {
        List<String> problems = new ArrayList<String>();
        Map<String, int[]> charts = new HashMap<String, int[]>();
        List<Set<String>> simplexes = parse(nerve, charts, problems);
        if (simplexes == null) {
            return problems;
        }
        if (charts.size() > SimplicialComplex.MAX_VERTICES) {
            problems.add(charts.size() + " covering sets; at most " + SimplicialComplex.MAX_VERTICES + " are supported");
            return problems;
        }
        Set<Set<String>> present = new HashSet<Set<String>>(simplexes);
        for (Set<String> s : simplexes) {
            // a common point
            Set<Integer> common = null;
            for (String c : s) {
                Set<Integer> xs = new HashSet<Integer>();
                for (int x : charts.get(c)) {
                    xs.add(x);
                }
                if (common == null) {
                    common = xs;
                }
                else {
                    common.retainAll(xs);
                }
            }
            if (common.isEmpty()) {
                problems.add("the simplex " + s + " has no common point");
            }
            // the faces of codimension one; the rest follow by induction
            if (s.size() > 1) {
                for (String c : s) {
                    Set<String> face = new TreeSet<String>(s);
                    face.remove(c);
                    if (!present.contains(face)) {
                        problems.add("the simplex " + s + " is missing its face " + face);
                    }
                }
            }
        }
        // the star of every point
        TreeSet<Integer> points = new TreeSet<Integer>();
        for (int[] c : charts.values()) {
            for (int x : c) {
                points.add(x);
            }
        }
        for (int x : points) {
            Set<String> star = new TreeSet<String>();
            for (Map.Entry<String, int[]> e : charts.entrySet()) {
                if (Arrays.binarySearch(e.getValue(), x) >= 0) {
                    star.add(e.getKey());
                }
            }
            if (!present.contains(star)) {
                problems.add("the star of " + x + ", " + star + ", is not a simplex");
            }
        }
        return problems;
    }

    /**
     * Reads a nerve back.
     *
     * @throws IllegalArgumentException with the {@link #violations} if
     *         {@code nerve} is not the nerve of its covering
     */
    public static Decoded decode(Denotator nerve) {
        List<String> problems = violations(nerve);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Not a nerve: " + String.join("; ", problems));
        }
        Map<String, int[]> charts = new HashMap<String, int[]>();
        List<Set<String>> simplexes = parse(nerve, charts, new ArrayList<String>());
        // vertices sorted by their pitch classes, lexicographically
        List<String> keys = new ArrayList<String>(charts.keySet());
        Collections.sort(keys, (a, b) -> Arrays.compare(charts.get(a), charts.get(b)));
        Map<String, Integer> index = new LinkedHashMap<String, Integer>();
        for (String c : keys) {
            index.put(c, index.size());
        }
        List<int[]> covering = new ArrayList<int[]>();
        List<String> labels = new ArrayList<String>();
        for (String c : index.keySet()) {
            covering.add(charts.get(c).clone());
            labels.add(c);
        }
        Set<Long> masks = new HashSet<Long>();
        for (Set<String> s : simplexes) {
            long m = 0;
            for (String c : s) {
                m |= 1L << index.get(c);
            }
            masks.add(m);
        }
        return new Decoded(covering, SimplicialComplex.ofMasks(labels, masks));
    }

    /** The covering I: the union of the simplexes, in vertex order. */
    public static List<int[]> covering(Denotator nerve) {
        return decode(nerve).covering;
    }

    /** The support K: the union of the covering sets, sorted. */
    public static int[] support(Denotator nerve) {
        TreeSet<Integer> xs = new TreeSet<Integer>();
        for (int[] c : covering(nerve)) {
            for (int x : c) {
                xs.add(x);
            }
        }
        int[] out = new int[xs.size()];
        int i = 0;
        for (int x : xs) {
            out[i++] = x;
        }
        return out;
    }
}
