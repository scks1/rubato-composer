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

package org.rubato.math.global.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.math.global.GlobalComposition;
import org.rubato.math.global.Interpretations;
import org.rubato.math.global.LocalComposition;
import org.rubato.math.global.NerveDenotators;
import org.rubato.math.global.SimplicialComplex;
import org.rubato.math.module.ZnElement;
import org.rubato.math.module.ZnRing;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

/**
 * The nerve as a denotator of G3 = Power(Power(Power(PitchClass))), after
 * Topos I &sect;13.4.1 (p. 259). Facts E1&ndash;E7 of the stage-9 spec.
 */
public class NerveDenotatorsTest extends TestCase {

    static final int[] C_MAJOR = { 0, 2, 4, 5, 7, 9, 11 };

    static GlobalComposition strip() {
        return Interpretations.thirdChains(12, C_MAJOR, 0, 3);
    }

    /** I IV V I in C as chords: {0,4,7}, {5,9,0}, {7,11,2}; the repeated I is one covering set. */
    static GlobalComposition cadence() {
        LocalComposition k = LocalComposition.ofClasses(12, 0, 2, 4, 5, 7, 9, 11);
        List<String> names = Arrays.asList("t0/3", "t1", "t2");
        List<int[]> cover = new ArrayList<int[]>();
        cover.add(indexes(k, 0, 4, 7));
        cover.add(indexes(k, 5, 9, 0));
        cover.add(indexes(k, 7, 11, 2));
        return GlobalComposition.interpretation(k, names, cover);
    }

    static int[] indexes(LocalComposition k, int... pcs) {
        int[] out = new int[pcs.length];
        for (int i = 0; i < pcs.length; i++) {
            out[i] = k.indexOf(pcs[i]);
        }
        Arrays.sort(out);
        return out;
    }

    /** The pitch classes of a PitchClassSet denotator, sorted. */
    static List<Integer> classes(Denotator chart) {
        List<Integer> out = new ArrayList<Integer>();
        for (Denotator p : ((PowerDenotator) chart).getFactors()) {
            out.add(((ZnElement) ((SimpleDenotator) p).getElement()).getValue());
        }
        java.util.Collections.sort(out);
        return out;
    }

    /** A simplex as the set of its charts, each chart as its sorted pitch classes. */
    static List<List<Integer>> simplex(Denotator s) {
        List<List<Integer>> out = new ArrayList<List<Integer>>();
        for (Denotator c : ((PowerDenotator) s).getFactors()) {
            out.add(classes(c));
        }
        out.sort((a, b) -> a.toString().compareTo(b.toString()));
        return out;
    }

    static List<Integer> chartClasses(GlobalComposition g, int i) {
        List<Integer> out = new ArrayList<Integer>();
        for (int x : g.coveringComposition(i).classes()) {
            out.add(x);
        }
        java.util.Collections.sort(out);
        return out;
    }

    // E1: the forms, G0 = PitchClass over Z12, G(i+1) = Power(G(i))
    public void testFormsAreBuiltinPowersOverZ12() {
        Repository rep = Repository.systemRepository();
        Form g3 = rep.getForm(NerveDenotators.PITCH_CLASS_NERVE);
        assertTrue("PitchClassNerve must be a power form", g3 instanceof PowerForm);
        assertTrue(rep.isBuiltin(g3));
        Form g2 = ((PowerForm) g3).getForm();
        assertEquals(NerveDenotators.PITCH_CLASS_COVERING, g2.getNameString());
        Form g1 = ((PowerForm) g2).getForm();
        assertEquals(NerveDenotators.PITCH_CLASS_SET, g1.getNameString());
        Form g0 = ((PowerForm) g1).getForm();
        assertEquals(NerveDenotators.PITCH_CLASS, g0.getNameString());
        assertTrue(g0 instanceof SimpleForm);
        assertEquals(ZnRing.make(12), ((SimpleForm) g0).getModule());
        assertSame(g1, rep.getForm(NerveDenotators.PITCH_CLASS_SET));
        assertSame(g2, rep.getForm(NerveDenotators.PITCH_CLASS_COVERING));
        assertSame(g0, rep.getForm(NerveDenotators.PITCH_CLASS));
    }

    // E1, E6: the harmonic strip as a denotator of G3
    public void testStripEncoding() {
        Denotator d = NerveDenotators.encode(strip());
        assertSame(Repository.systemRepository().getForm(NerveDenotators.PITCH_CLASS_NERVE), d.getForm());
        assertTrue(d.nullAddressed());
        assertNull("the output is anonymous (spec N3)", d.getName());
        PowerDenotator p = (PowerDenotator) d;
        assertEquals(28, p.getFactorCount());
        int[] bySize = new int[4];
        for (Denotator s : p.getFactors()) {
            bySize[((PowerDenotator) s).getFactorCount()]++;
        }
        assertTrue(Arrays.toString(bySize), Arrays.equals(new int[] { 0, 7, 14, 7 }, bySize));
    }

    // the vertices are the seven triads of C major
    public void testVerticesAreTheCharts() {
        GlobalComposition g = strip();
        Denotator d = NerveDenotators.encode(g);
        TreeSet<String> vertices = new TreeSet<String>();
        for (Denotator s : ((PowerDenotator) d).getFactors()) {
            if (((PowerDenotator) s).getFactorCount() == 1) {
                vertices.add(simplex(s).get(0).toString());
            }
        }
        TreeSet<String> expected = new TreeSet<String>();
        for (int i = 0; i < g.coveringSize(); i++) {
            expected.add(chartClasses(g, i).toString());
        }
        assertEquals(expected, vertices);
        assertTrue(vertices.contains("[0, 4, 7]"));
        assertTrue(vertices.contains("[2, 5, 11]"));
    }

    // E2: st(x) is a simplex of n(I) for every point x
    public void testStarsAreSimplexes() {
        GlobalComposition g = strip();
        Denotator d = NerveDenotators.encode(g);
        TreeSet<String> factors = new TreeSet<String>();
        for (Denotator s : ((PowerDenotator) d).getFactors()) {
            factors.add(simplex(s).toString());
        }
        for (int x : C_MAJOR) {
            List<List<Integer>> star = new ArrayList<List<Integer>>();
            for (int i = 0; i < g.coveringSize(); i++) {
                List<Integer> c = chartClasses(g, i);
                if (c.contains(x)) {
                    star.add(c);
                }
            }
            star.sort((a, b) -> a.toString().compareTo(b.toString()));
            assertEquals("each tone lies in three triads", 3, star.size());
            assertTrue("st(" + x + ") = " + star, factors.contains(star.toString()));
        }
    }

    // E3: the union operator gives back I and K
    public void testUnionGivesCoveringAndSupport() {
        GlobalComposition g = strip();
        Denotator d = NerveDenotators.encode(g);
        TreeSet<String> cover = new TreeSet<String>();
        for (int[] c : NerveDenotators.covering(d)) {
            cover.add(Arrays.toString(c));
        }
        TreeSet<String> expected = new TreeSet<String>();
        for (int i = 0; i < g.coveringSize(); i++) {
            expected.add(chartClasses(g, i).toString());
        }
        assertEquals(expected, cover);
        assertTrue(Arrays.equals(C_MAJOR, NerveDenotators.support(d)));
    }

    /** The original nerve with its vertices renamed and reordered to match a decoded complex. */
    static SimplicialComplex relabel(GlobalComposition g, NerveDenotators.Decoded dec) {
        int n = g.coveringSize();
        int[] to = new int[n];
        List<String> labels = new ArrayList<String>();
        for (int j = 0; j < dec.covering.size(); j++) {
            labels.add(dec.complex.label(j));
        }
        for (int i = 0; i < n; i++) {
            to[i] = -1;
            for (int j = 0; j < dec.covering.size(); j++) {
                if (chartClasses(g, i).toString().equals(Arrays.toString(dec.covering.get(j)))) {
                    to[i] = j;
                }
            }
            assertTrue("chart " + i + " found", to[i] >= 0);
        }
        List<int[]> simplexes = new ArrayList<int[]>();
        for (int[] s : g.nerve().allSimplexes()) {
            int[] t = new int[s.length];
            for (int k = 0; k < s.length; k++) {
                t[k] = to[s[k]];
            }
            simplexes.add(t);
        }
        return SimplicialComplex.generatedBy(labels, simplexes);
    }

    // E4, E7: decoding gives back the nerve for every chart size and for chords
    public void testDecodeGivesTheNerve() {
        int[][] f = { { 7 }, { 7, 7 }, { 7, 14, 7 }, { 7, 21, 21, 7 } };
        for (int k = 1; k <= 4; k++) {
            GlobalComposition g = Interpretations.thirdChains(12, C_MAJOR, 0, k);
            NerveDenotators.Decoded dec = NerveDenotators.decode(NerveDenotators.encode(g));
            assertTrue("k = " + k, Arrays.equals(f[k - 1], dec.complex.fVector()));
            assertEquals("k = " + k, relabel(g, dec), dec.complex);
            assertEquals(g.nerve().homology().toString(), dec.complex.homology().toString());
        }
        GlobalComposition c = cadence();
        NerveDenotators.Decoded dec = NerveDenotators.decode(NerveDenotators.encode(c));
        assertTrue(Arrays.equals(new int[] { 3, 2 }, dec.complex.fVector()));
        assertEquals(relabel(c, dec), dec.complex);
        assertEquals("{0,4,7}", dec.complex.label(0));
    }

    // E4, E5: the validator
    public void testValidator() {
        Denotator d = NerveDenotators.encode(strip());
        assertEquals(new ArrayList<String>(), NerveDenotators.violations(d));
        List<Denotator> factors = new ArrayList<Denotator>(((PowerDenotator) d).getFactors());
        Form g3 = d.getForm();

        // drop one edge: a triangle loses a face and a star goes missing
        List<Denotator> lessEdge = new ArrayList<Denotator>(factors);
        for (Denotator s : factors) {
            if (((PowerDenotator) s).getFactorCount() == 2) {
                lessEdge.remove(s);
                break;
            }
        }
        List<String> v = NerveDenotators.violations(power(g3, lessEdge));
        assertTrue(v.toString(), contains(v, "missing its face"));

        // add {I, II}: C E G and D F A share no tone
        Denotator chartI = null, chartII = null;
        for (Denotator s : factors) {
            if (((PowerDenotator) s).getFactorCount() == 1) {
                Denotator c = ((PowerDenotator) s).getFactor(0);
                if (classes(c).equals(Arrays.asList(0, 4, 7))) {
                    chartI = c;
                }
                if (classes(c).equals(Arrays.asList(2, 5, 9))) {
                    chartII = c;
                }
            }
        }
        assertNotNull(chartI);
        assertNotNull(chartII);
        List<Denotator> withBad = new ArrayList<Denotator>(factors);
        withBad.add(power(((PowerForm) g3).getForm(), Arrays.asList(chartI, chartII)));
        v = NerveDenotators.violations(power(g3, withBad));
        assertTrue(v.toString(), contains(v, "no common point"));

        // drop a whole triangle: faces stay closed, but the star of a tone is missing
        List<Denotator> lessTriangle = new ArrayList<Denotator>(factors);
        for (Denotator s : factors) {
            if (((PowerDenotator) s).getFactorCount() == 3) {
                lessTriangle.remove(s);
                break;
            }
        }
        v = NerveDenotators.violations(power(g3, lessTriangle));
        assertFalse(v.toString(), contains(v, "missing its face"));
        assertTrue(v.toString(), contains(v, "star"));

        // a Score is not a nerve
        Denotator score = power(Repository.systemRepository().getForm("Score"), new ArrayList<Denotator>());
        v = NerveDenotators.violations(score);
        assertTrue(v.toString(), contains(v, "PitchClassNerve"));
        try {
            NerveDenotators.decode(power(g3, withBad));
            fail("decode must refuse a denotator that is not a nerve");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no common point"));
        }
    }

    // only pitch-class supports
    public void testRefusesOtherSupports() {
        GlobalComposition z7 = Interpretations.thirdChains(7, new int[] { 0, 1, 2, 3, 4, 5, 6 }, 0, 3);
        try {
            NerveDenotators.encode(z7);
            fail("a Z7 support must be refused");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Z12"));
        }
    }

    // the output survives Rubato's XML writer and reader
    public void testXmlRoundTrip() throws Exception {
        Denotator d = NerveDenotators.encode(strip());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, "UTF-8");
        XMLWriter w = new XMLWriter(out);
        w.writeDenotatorRef(d);
        out.flush();
        String xml = bytes.toString("UTF-8");
        assertFalse(xml, xml.contains("ref="));
        Element e = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes("UTF-8"))).getDocumentElement();
        XMLReader r = new XMLReader(new StringReader("<Rubato/>"));
        r.parse();
        Denotator back = r.parseDenotator(e);
        assertFalse(r.getErrors().toString(), r.hasError());
        assertEquals(d, back);
        assertEquals(new ArrayList<String>(), NerveDenotators.violations(back));
    }

    static Denotator power(Form f, List<Denotator> factors) {
        try {
            return new PowerDenotator(null, (PowerForm) f, factors);
        }
        catch (org.rubato.base.RubatoException e) {
            throw new AssertionError(e);
        }
    }

    static boolean contains(List<String> v, String fragment) {
        for (String s : v) {
            if (s.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
