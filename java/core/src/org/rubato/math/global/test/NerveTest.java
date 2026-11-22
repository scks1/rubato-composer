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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.global.ChordClasses;
import org.rubato.math.global.GlobalComposition;
import org.rubato.math.global.GlobalComposition.Chart;
import org.rubato.math.global.GlobalMorphism;
import org.rubato.math.global.Interpretations;
import org.rubato.math.global.LocalComposition;
import org.rubato.math.global.Resolution;
import org.rubato.math.global.SimplicialComplex;

/**
 * Nerves of global compositions, checked against the published examples:
 * the harmonic strip (Topos I &sect;13.4.2, pp. 261&ndash;263), the third-chain
 * interpretations and their refinements (&sect;14.2, Ex. 21, pp. 275&ndash;276),
 * the class nerve of C<sup>(3)</sup> (&sect;14.5, Ex. 24, Fig. 14.7, p. 284), the
 * tetradic nerve (Fig. 14.1, p. 276; Agust&iacute;n-Aquino and Mazzola,
 * J. Math. Music 14, 2020, &sect;6) and the resolution (&sect;15.2.1, p. 294).
 */
public class NerveTest extends TestCase {

    static final int I = 0, II = 1, III = 2, IV = 3, V = 4, VI = 5, VII = 6;

    static final int[][] SCALES = { Interpretations.C_MAJOR, Interpretations.C_MELODIC_MINOR,
                                    Interpretations.C_HARMONIC_MINOR };

    // ---- the harmonic strip, Topos I p. 262

    /** Seven vertices, fourteen edges, seven triangles for C, cm and ch (p. 262). */
    public void testHarmonicStripFaceCounts() {
        for (int[] scale : SCALES) {
            SimplicialComplex n = Interpretations.thirdChains(scale, 3).nerve();
            assertTrue(Arrays.toString(scale), Arrays.equals(new int[] { 7, 14, 7 }, n.fVector()));
            assertEquals(0, n.eulerCharacteristic());
            assertEquals(2, n.dimension());
        }
    }

    /** The strip is a pseudomanifold with connected boundary along the fifth sequence (p. 262). */
    public void testHarmonicStripBoundaryIsTheFifthSequence() {
        for (int[] scale : SCALES) {
            SimplicialComplex n = Interpretations.thirdChains(scale, 3).nerve();
            assertTrue(n.isPseudomanifold());
            SimplicialComplex b = n.boundary();
            assertEquals(7, b.simplexes(1).size());
            List<Integer> cycle = b.cycle(V);
            assertNotNull("boundary is one cycle", cycle);
            // V -> I -> IV -> VII -> III -> VI -> II (-> V), either direction
            List<Integer> fifths = Arrays.asList(V, I, IV, VII, III, VI, II);
            List<Integer> reversed = Arrays.asList(V, II, VI, III, VII, IV, I);
            assertTrue(cycle.toString(), cycle.equals(fifths) || cycle.equals(reversed));
        }
    }

    /** The harmonic strip is not orientable (p. 262, Fig. 13.8). */
    public void testHarmonicStripIsNotOrientable() {
        for (int[] scale : SCALES) {
            assertFalse(Interpretations.thirdChains(scale, 3).nerve().isOrientable());
        }
    }

    /** Homology of a Moebius strip: Z, Z, 0 (standard topology on the nerve; spec P3). */
    public void testHarmonicStripHomology() {
        for (int[] scale : SCALES) {
            List<SimplicialComplex.HomologyGroup> h = Interpretations.thirdChains(scale, 3).nerve().homology();
            assertEquals("[Z, Z, 0]", h.toString());
        }
    }

    /** Degree chord types of Table 13.4.2.B (p. 262). */
    public void testDegreeChordTypes() {
        String[][] expected = {
            { "maj", "min", "min", "maj", "maj", "min", "dim" },
            { "min", "min", "aug", "maj", "maj", "dim", "dim" },
            { "min", "dim", "aug", "min", "maj", "maj", "dim" },
        };
        for (int s = 0; s < SCALES.length; s++) {
            GlobalComposition g = Interpretations.thirdChains(SCALES[s], 3);
            for (int d = 0; d < 7; d++) {
                assertEquals(Interpretations.DEGREES.get(d) + " of " + Arrays.toString(SCALES[s]),
                             expected[s][d], triadType(g, d));
            }
        }
    }

    /** The type of a stacked-third triad from its root (degree d's first chart point). */
    static String triadType(GlobalComposition g, int d) {
        int[] order = Interpretations.degreeOrder(12, g.getSupport().classes(), g.getSupport().classes()[0]);
        int r = order[d], t = order[(d + 2) % 7], f = order[(d + 4) % 7];
        int a = Math.floorMod(t - r, 12), b = Math.floorMod(f - r, 12);
        if (a == 4 && b == 7) return "maj";
        if (a == 3 && b == 7) return "min";
        if (a == 3 && b == 6) return "dim";
        if (a == 4 && b == 8) return "aug";
        return "?";
    }

    /**
     * Degree orbits under the scale automorphisms (p. 262): major
     * {I,VI},{II,V},{III,IV},{VII}; melodic minor {I,V},{II,IV},{III},{VI,VII};
     * harmonic minor rigid. The automorphisms are the affine bijections of Z12
     * that fix the scale; each permutes the triadic charts.
     */
    public void testDegreeOrbits() {
        assertEquals("[[0, 5], [1, 4], [2, 3], [6]]", degreeOrbits(Interpretations.C_MAJOR).toString());
        assertEquals("[[0, 4], [1, 3], [2], [5, 6]]", degreeOrbits(Interpretations.C_MELODIC_MINOR).toString());
        assertEquals("[[0], [1], [2], [3], [4], [5], [6]]", degreeOrbits(Interpretations.C_HARMONIC_MINOR).toString());
    }

    static List<List<Integer>> degreeOrbits(int[] scale) {
        GlobalComposition g = Interpretations.thirdChains(scale, 3);
        Set<Integer> sc = new HashSet<Integer>();
        for (int x : scale) {
            sc.add(x);
        }
        List<int[]> perms = new ArrayList<int[]>();
        for (int u : new int[] { 1, 5, 7, 11 }) {
            for (int t = 0; t < 12; t++) {
                boolean fixes = true;
                for (int x : scale) {
                    fixes &= sc.contains(Math.floorMod(u * x + t, 12));
                }
                if (!fixes) {
                    continue;
                }
                int[] p = new int[7];
                for (int d = 0; d < 7; d++) {
                    Set<Integer> img = new HashSet<Integer>();
                    for (int x : g.coveringComposition(d).classes()) {
                        img.add(Math.floorMod(u * x + t, 12));
                    }
                    p[d] = -1;
                    for (int e = 0; e < 7; e++) {
                        Set<Integer> c = new HashSet<Integer>();
                        for (int x : g.coveringComposition(e).classes()) {
                            c.add(x);
                        }
                        if (c.equals(img)) {
                            p[d] = e;
                        }
                    }
                    assertTrue("the automorphism permutes the degree charts", p[d] >= 0);
                }
                perms.add(p);
            }
        }
        List<List<Integer>> orbits = new ArrayList<List<Integer>>();
        boolean[] done = new boolean[7];
        for (int d = 0; d < 7; d++) {
            if (done[d]) {
                continue;
            }
            Set<Integer> o = new java.util.TreeSet<Integer>();
            o.add(d);
            for (int[] p : perms) {
                o.add(p[d]);
            }
            for (int x : o) {
                done[x] = true;
            }
            orbits.add(new ArrayList<Integer>(o));
        }
        return orbits;
    }

    // ---- the third-chain interpretations, Ex. 21 p. 275

    /** X(1) is discrete, X(2) a circle of seven thirds. */
    public void testSingletonAndThirdNerves() {
        SimplicialComplex n1 = Interpretations.thirdChains(Interpretations.C_MAJOR, 1).nerve();
        assertTrue(Arrays.equals(new int[] { 7 }, n1.fVector()));
        assertEquals("[Z^7]", n1.homology().toString());
        SimplicialComplex n2 = Interpretations.thirdChains(Interpretations.C_MAJOR, 2).nerve();
        assertTrue(Arrays.equals(new int[] { 7, 7 }, n2.fVector()));
        assertEquals("[Z, Z]", n2.homology().toString());
        assertNotNull(n2.cycle(I));
    }

    /** 2^7 refinement maps for each succession X(i) -> X(i+1) (p. 275). */
    public void testRefinementCount() {
        for (int k = 1; k <= 3; k++) {
            GlobalComposition fine = Interpretations.thirdChains(Interpretations.C_MAJOR, k);
            GlobalComposition coarse = Interpretations.thirdChains(Interpretations.C_MAJOR, k + 1);
            List<int[]> refs = Interpretations.refinements(fine, coarse);
            assertEquals("X(" + k + ") -> X(" + (k + 1) + ")", 128, refs.size());
            for (int[] iota : refs) {
                GlobalMorphism r = GlobalMorphism.refinement(fine, coarse, iota);
                assertTrue(r.violations().toString(), r.isMorphism());
                assertTrue(fine.nerve().isSimplicialMap(r.nerveMap(), coarse.nerve()));
            }
        }
    }

    /**
     * Prop. 14 (p. 282): mathematically equivalent morphisms (here all
     * refinements X(2) -> X(3), which share f = Id) induce contiguous nerve maps.
     */
    public void testRefinementsAreContiguous() {
        GlobalComposition fine = Interpretations.thirdChains(Interpretations.C_MAJOR, 2);
        GlobalComposition coarse = Interpretations.thirdChains(Interpretations.C_MAJOR, 3);
        List<int[]> refs = Interpretations.refinements(fine, coarse);
        SimplicialComplex a = fine.nerve(), b = coarse.nerve();
        for (int[] f : refs) {
            for (int[] g : refs) {
                assertTrue(a.contiguous(f, g, b));
            }
        }
    }

    /** Def. 43 rejects a covering map that breaks f(i) in iota(i). */
    public void testRefinementWithWrongChartIsRejected() {
        GlobalComposition fine = Interpretations.thirdChains(Interpretations.C_MAJOR, 2);
        GlobalComposition coarse = Interpretations.thirdChains(Interpretations.C_MAJOR, 3);
        int[] iota = Interpretations.refinements(fine, coarse).get(0);
        iota[0] = (iota[0] + 3) % 7;
        assertFalse(GlobalMorphism.refinement(fine, coarse, iota).isMorphism());
    }

    // ---- the tetradic nerve, Fig. 14.1 p. 276 and AM 2020 sec. 6

    /** 1-skeleton K7, seven tetrahedra S1..S7, face counts 7, 21, 21, 7. */
    public void testTetradicNerve() {
        SimplicialComplex n = Interpretations.thirdChains(Interpretations.C_MAJOR, 4).nerve();
        assertTrue(Arrays.toString(n.fVector()), Arrays.equals(new int[] { 7, 21, 21, 7 }, n.fVector()));
        assertEquals(0, n.eulerCharacteristic());
        int[][] s = {
            { I, II, IV, VI }, { II, III, V, VII }, { III, IV, VI, I }, { IV, V, VII, II },
            { V, VI, I, III }, { VI, VII, II, IV }, { VII, I, III, V },
        };
        Set<List<Integer>> expected = new HashSet<List<Integer>>();
        for (int[] t : s) {
            int[] c = t.clone();
            Arrays.sort(c);
            expected.add(SimplicialComplex.asList(c));
        }
        Set<List<Integer>> got = new HashSet<List<Integer>>();
        for (int[] t : n.maximalSimplexes()) {
            got.add(SimplicialComplex.asList(t));
        }
        assertEquals(expected, got);
        // the cycle S5, S7, S2, S4, S6, S1, S3: consecutive tetrahedra share a triangle
        int[] cyc = { 4, 6, 1, 3, 5, 0, 2, 4 };
        for (int i = 0; i + 1 < cyc.length; i++) {
            Set<Integer> a = new HashSet<Integer>(SimplicialComplex.asList(s[cyc[i]]));
            a.retainAll(SimplicialComplex.asList(s[cyc[i + 1]]));
            assertEquals(3, a.size());
        }
    }

    /** Solid torus: homology Z, Z, 0, 0, orientable (spec A4, P3, P4). */
    public void testTetradicNerveHomologyAndOrientation() {
        SimplicialComplex n = Interpretations.thirdChains(Interpretations.C_MAJOR, 4).nerve();
        assertEquals("[Z, Z, 0, 0]", n.homology().toString());
        assertTrue(n.isPseudomanifold());
        assertTrue(n.isOrientable());
    }

    /**
     * Fig. 14.1 (p. 276): each tetrahedron is the configuration around a
     * fixed tone, i.e. the maximal simplexes are the stars of the seven tones;
     * the same holds for the triangles of the harmonic strip.
     */
    public void testMaximalSimplexesAreToneStars() {
        for (int k = 3; k <= 4; k++) {
            GlobalComposition g = Interpretations.thirdChains(Interpretations.C_MAJOR, k);
            Set<List<Integer>> stars = new HashSet<List<Integer>>();
            for (int p = 0; p < 7; p++) {
                List<Integer> star = new ArrayList<Integer>();
                for (int i = 0; i < 7; i++) {
                    if (Arrays.binarySearch(g.coveringSet(i), p) >= 0) {
                        star.add(i);
                    }
                }
                stars.add(star);
            }
            Set<List<Integer>> max = new HashSet<List<Integer>>();
            for (int[] t : g.nerve().maximalSimplexes()) {
                max.add(SimplicialComplex.asList(t));
            }
            assertEquals(stars, max);
        }
    }

    // ---- the class nerve, Ex. 24 p. 284 and App. N.1

    /** App. N.1 p. 1502: 5 classes of two tones (3-7) and 9 of three tones (8-16). */
    public void testChordClassCounts() {
        assertEquals(1, ChordClasses.classes(12, 1).size());
        assertEquals(5, ChordClasses.classes(12, 2).size());
        assertEquals(9, ChordClasses.classes(12, 3).size());
        // the representatives of 2..16 are pairwise non-isomorphic
        List<LocalComposition> reps = new ArrayList<LocalComposition>();
        for (Map.Entry<Integer, int[]> e : ChordClasses.REPRESENTATIVES.entrySet()) {
            if (e.getKey() == 1) continue;
            LocalComposition r = LocalComposition.ofClasses(12, e.getValue());
            for (LocalComposition q : reps) {
                assertFalse(r + " ~ " + q, r.size() == q.size() && r.isIsomorphicTo(q));
            }
            reps.add(r);
            assertEquals(e.getKey(), ChordClasses.classNumber(r));
        }
    }

    /**
     * Spec A2: Def. 26 isomorphism and the affine-symmetry orbits agree on
     * chords of one to three tones in Z12 (the classes App. N.1 numbers 2-16).
     */
    public void testIsomorphismMatchesAffineOrbits() {
        for (int k = 1; k <= 3; k++) {
            assertEquals("k = " + k, ChordClasses.affineOrbits(12, k).size(), ChordClasses.classes(12, k).size());
        }
    }

    /**
     * Fig. 14.7: degrees I..VI weigh 10 (major/minor triads), VII weighs 15
     * (diminished); third edges weigh 5 (minor third) or 6 (major third),
     * single-tone edges and all seven triangles weigh 2.
     */
    public void testClassNerveOfCMajor() {
        GlobalComposition g = Interpretations.thirdChains(Interpretations.C_MAJOR, 3);
        Map<List<Integer>, Integer> w = ChordClasses.classWeights(g);
        for (int d = I; d <= VI; d++) {
            assertEquals(Integer.valueOf(10), w.get(Arrays.asList(d)));
        }
        assertEquals(Integer.valueOf(15), w.get(Arrays.asList(VII)));
        int[][] minorThirds = { { I, III }, { II, VII }, { IV, VI }, { V, VII } };
        int[][] majorThirds = { { I, VI }, { II, IV }, { III, V } };
        for (int[] e : minorThirds) {
            assertEquals(Integer.valueOf(5), w.get(SimplicialComplex.asList(e)));
        }
        for (int[] e : majorThirds) {
            assertEquals(Integer.valueOf(6), w.get(SimplicialComplex.asList(e)));
        }
        int singles = 0, triangles = 0;
        for (Map.Entry<List<Integer>, Integer> e : w.entrySet()) {
            if (e.getKey().size() == 2 && e.getValue() == 2) singles++;
            if (e.getKey().size() == 3) {
                assertEquals(Integer.valueOf(2), e.getValue());
                triangles++;
            }
        }
        assertEquals(7, singles);
        assertEquals("seven 2-simplexes of weight 2", 7, triangles);
    }

    /**
     * Ex. 24: the class nerve's only nontrivial automorphism fixes VII and
     * exchanges I-VI, II-V, III-IV, the automorphism the scale induces.
     */
    public void testClassNerveAutomorphisms() {
        GlobalComposition g = Interpretations.thirdChains(Interpretations.C_MAJOR, 3);
        SimplicialComplex n = g.nerve();
        List<int[]> auts = n.automorphisms(ChordClasses.classWeights(g));
        assertEquals(2, auts.size());
        int[] swap = null;
        for (int[] p : auts) {
            if (p[0] != 0) swap = p;
        }
        assertTrue(Arrays.equals(new int[] { VI, V, IV, III, II, I, VII }, swap));
        // without weights the strip has more symmetry (its dihedral group of order 14)
        assertEquals(14, n.automorphisms(null).size());
    }

    // ---- gluing, Def. 36 (vi)

    /** The obvious atlas of an interpretation glues. */
    public void testInterpretationGlues() {
        for (int k = 1; k <= 4; k++) {
            assertEquals(0, Interpretations.thirdChains(Interpretations.C_MAJOR, k).gluingViolations().size());
        }
    }

    /**
     * An atlas whose chart for I is the minor triad {0, 3, 7}, mapped onto
     * {0, 4, 7} by 0->0, 3->4, 7->7, fails (vi). On the overlap with III =
     * {4, 7, 11} the transition {3, 7} -> {4, 7} needs 4u = 3 mod 12; on the
     * overlap with VI = {0, 4, 9} the transition {0, 3} -> {0, 4} needs
     * 3u = 4 mod 12. Neither has a solution. The overlaps with IV and V are
     * single points and glue.
     */
    public void testBadChartBreaksGluing() {
        GlobalComposition g = Interpretations.thirdChains(Interpretations.C_MAJOR, 3);
        List<Chart> atlas = new ArrayList<Chart>(g.atlas());
        LocalComposition minor = LocalComposition.ofClasses(12, 0, 3, 7);
        int[] phi = { g.getSupport().indexOf(0), g.getSupport().indexOf(4), g.getSupport().indexOf(7) };
        atlas.set(I, new Chart(minor, I, phi));
        List<int[]> cover = new ArrayList<int[]>();
        for (int i = 0; i < 7; i++) {
            cover.add(g.coveringSet(i));
        }
        GlobalComposition bad = new GlobalComposition(g.getSupport(), g.names(), cover, atlas);
        List<String> v = bad.gluingViolations();
        assertEquals(v.toString(), 4, v.size());
        for (String line : v) {
            assertTrue(line, line.contains("(I)"));
            assertTrue(line, line.contains("(III)") || line.contains("(VI)"));
        }
    }

    /** Covering sets must cover the support and have charts (Def. 36 (i), (iv)). */
    public void testCoveringValidation() {
        LocalComposition x = LocalComposition.ofClasses(12, 0, 4, 7);
        try {
            GlobalComposition.interpretation(x, Arrays.asList("a"), Arrays.asList(new int[] { 0, 1 }));
            fail("a covering that misses a point must be rejected");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("misses"));
        }
        try {
            GlobalComposition.interpretation(x, Arrays.asList("a"), Arrays.asList(new int[0]));
            fail("an empty covering set must be rejected");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("empty"));
        }
    }

    // ---- the resolution, sec. 15.2.1 p. 294

    /**
     * res : Delta G -> G is a bijective morphism with an isomorphism of
     * nerves; for the harmonic strip it is not an isomorphism (footnote 5):
     * the inverse's chart map from the triad {0, 4, 7} of Z12 to the
     * standard chart {0, e1, e2} of Z12^2 would need 4m = e1 and 7m = e2,
     * hence 12m = 4(e2 - e1), which is not 0.
     */
    public void testResolutionOfHarmonicStrip() {
        GlobalComposition g = Interpretations.thirdChains(Interpretations.C_MAJOR, 3);
        GlobalComposition d = Resolution.standard(g);
        assertEquals(6, d.getSupport().getDimension());
        assertEquals(7, d.getSupport().size());
        assertEquals("the standard atlas glues", 0, d.gluingViolations().size());
        GlobalMorphism res = Resolution.resolution(g);
        assertTrue(res.violations().toString(), res.isMorphism());
        assertTrue(res.isBijective());
        int[] id = { 0, 1, 2, 3, 4, 5, 6 };
        assertTrue(Arrays.equals(id, res.nerveMap()));
        assertTrue(d.nerve().isIsomorphism(res.nerveMap(), g.nerve()));
        assertFalse(res.isIsomorphism());
    }

    /** For a composition over the standard simplex itself the resolution is an isomorphism. */
    public void testResolutionOfStandardIsIsomorphism() {
        GlobalComposition g = Interpretations.thirdChains(Interpretations.C_MAJOR, 3);
        GlobalComposition d = Resolution.standard(g);
        GlobalMorphism res = Resolution.resolution(d);
        assertTrue(res.isMorphism());
        assertTrue(res.isIsomorphism());
    }

    /** Local composition morphisms solve the affine extension problem (Def. 23). */
    public void testLocalMorphisms() {
        LocalComposition major = LocalComposition.ofClasses(12, 0, 4, 7);
        LocalComposition minor = LocalComposition.ofClasses(12, 0, 3, 7);
        // inversion x -> 7 - x sends 0->7, 4->3, 7->0
        int[] f = { minor.indexOf(7), minor.indexOf(3), minor.indexOf(0) };
        assertTrue(major.isIsomorphism(f, minor));
        LocalComposition.AffineMap h = major.morphism(f, minor);
        assertNotNull(h);
        assertTrue(Arrays.equals(new int[] { 3 }, h.apply(new int[] { 4 })));
        assertNotNull(h.toMorphism());
        // 0->0, 4->3, 7->7: u*4 = 3 mod 12 has no solution
        int[] g = { minor.indexOf(0), minor.indexOf(3), minor.indexOf(7) };
        assertFalse(major.isMorphism(g, minor));
        // a constant map is always a morphism but never an isomorphism
        int[] c = { 0, 0, 0 };
        assertTrue(major.isMorphism(c, minor));
        assertFalse(major.isIsomorphism(c, minor));
        // {0, 6} and {0, 3} are not isomorphic: 6u = 3 has no solution
        assertFalse(LocalComposition.ofClasses(12, 0, 6).isIsomorphicTo(LocalComposition.ofClasses(12, 0, 3)));
        // x -> 3x sends {0, 1} onto {0, 3}, but nothing sends 3 back to 1
        int[] t = { 0, 1 };
        assertTrue(LocalComposition.ofClasses(12, 0, 1).isMorphism(t, LocalComposition.ofClasses(12, 0, 3)));
        assertFalse(LocalComposition.ofClasses(12, 0, 1).isIsomorphism(t, LocalComposition.ofClasses(12, 0, 3)));
    }

    /** Module elements round-trip through LocalComposition. */
    public void testModuleElementRoundTrip() {
        LocalComposition x = LocalComposition.ofClasses(12, 11, 0, 4, 7);
        assertEquals(x, LocalComposition.fromModuleElements(x.toModuleElements()));
        LocalComposition d = Resolution.localStandard(12, 3);
        assertEquals(d, LocalComposition.fromModuleElements(d.toModuleElements()));
    }
}
