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

package org.rubato.math.harmony.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.global.Interpretations;
import org.rubato.math.harmony.PitchClasses;
import org.rubato.math.harmony.Plrq;
import org.rubato.math.harmony.PlrqElement;
import org.rubato.math.harmony.SeventhChord;
import org.rubato.math.harmony.Tonality;

/**
 * The PLRQ group on seventh chords (Cannas, Antonini, Pernazza, MCM 2017,
 * LNCS 10527: the 17 generators, eqs. (9)&ndash;(10), Thm. 1) and the
 * morphisms R42, L13, P42 with the facts of Agust&iacute;n-Aquino, Blues for
 * Alice, arXiv 2601.07161 (Defs. 3.1, 3.3, 3.5; Def. 4.1; Prop. 4.2;
 * Thm. 5.1; Def. 5.2; Prop. 7.1).
 */
public class PlrqTest extends TestCase {

    static final int D = SeventhChord.DOMINANT, M = SeventhChord.MAJOR, MIN = SeventhChord.MINOR,
                     HD = SeventhChord.HALF_DIMINISHED, DIM = SeventhChord.DIMINISHED;

    static SeventhChord ch(String s) { return SeventhChord.parse(s); }

    static PlrqElement g(String name) { return Plrq.generator(name); }

    // ---- chords

    public void testChordTypes() {
        assertEquals(PitchClasses.of(0, 4, 7, 10), new SeventhChord(D, 0).pitchClasses());
        assertEquals(PitchClasses.of(0, 3, 7, 10), new SeventhChord(MIN, 0).pitchClasses());
        assertEquals(PitchClasses.of(0, 3, 6, 10), new SeventhChord(HD, 0).pitchClasses());
        assertEquals(PitchClasses.of(0, 4, 7, 11), new SeventhChord(M, 0).pitchClasses());
        assertEquals(PitchClasses.of(0, 3, 6, 9), new SeventhChord(DIM, 0).pitchClasses());
        assertEquals(60, SeventhChord.all().size());
        assertEquals(new SeventhChord(M, 11), ch("Bmaj7"));
        assertEquals(new SeventhChord(MIN, 5), ch("Fm7"));
        assertEquals(new SeventhChord(HD, 2), ch("Dm7b5"));
        assertEquals(new SeventhChord(D, 10), ch("Bb7"));
        assertEquals("Bm7", new SeventhChord(MIN, 11).toString());
    }

    /** The tetradic degrees of C major are Cmaj7, Dm7, Em7, Fmaj7, G7, Am7, Bm7b5. */
    public void testDegreeChords() {
        Tonality c = Tonality.of(Interpretations.C_MAJOR, 0, 4);
        String[] names = { "Cmaj7", "Dm7", "Em7", "Fmaj7", "G7", "Am7", "Bm7b5" };
        for (int d = 0; d < 7; d++) {
            assertEquals(c.degree(d), ch(names[d]).pitchClasses());
            assertEquals(ch(names[d]), SeventhChord.degree(c, d));
        }
    }

    // ---- the 17 generators of Cannas et al.

    /**
     * Each generator with the chord formula printed next to it: left type,
     * right offsets from the left root x. The (sigma, v) values are the
     * paper's; the formulas check them on pitch classes.
     */
    static final Object[][] FORMULAS = {
        { "P12", D, new int[] { 0, 3, 7, 10 } }, { "P14", D, new int[] { 0, 4, 7, 11 } },
        { "P23", MIN, new int[] { 0, 3, 6, 10 } }, { "P35", HD, new int[] { 0, 3, 6, 9 } },
        { "R12", D, new int[] { 0, 4, 7, 9 } }, { "R23", MIN, new int[] { 0, 3, 7, 9 } },
        { "R42", M, new int[] { 0, 4, 7, 9 } }, { "R35", HD, new int[] { 0, 3, 6, 9 } },
        { "R53", DIM, new int[] { 0, 3, 7, 9 } }, { "L13", D, new int[] { 2, 4, 7, 10 } },
        { "L15", D, new int[] { 1, 4, 7, 10 } }, { "L42", M, new int[] { 2, 4, 7, 11 } },
        { "Q43", M, new int[] { 1, 4, 7, 11 } }, { "Q15", D, new int[] { 1, 4, 7, 10 } },
        { "RR35", HD, new int[] { 0, 3, 6, 9 } }, { "QQ51", DIM, new int[] { 0, 2, 6, 9 } },
        { "N51", DIM, new int[] { 0, 3, 5, 9 } },
    };

    public void testSeventeenGeneratorsMatchTheirFormulas() {
        assertEquals(17, Plrq.generators().size());
        for (Object[] f : FORMULAS) {
            PlrqElement e = g((String) f[0]);
            int[] off = (int[]) f[2];
            for (int x = 0; x < 12; x++) {
                SeventhChord left = new SeventhChord((Integer) f[1], x);
                int want = 0;
                for (int o : off) {
                    want |= PitchClasses.of(x + o);
                }
                assertEquals(f[0] + " at " + x, want, e.apply(left).pitchClasses());
            }
        }
    }

    /** Every generator is an involution and keeps at least three tones of each chord. */
    public void testGeneratorsAreParsimoniousInvolutions() {
        for (Map.Entry<String, PlrqElement> e : Plrq.generators().entrySet()) {
            PlrqElement h = e.getValue();
            assertTrue(e.getKey(), h.compose(h).isIdentity());
            for (SeventhChord c : SeventhChord.all()) {
                int common = PitchClasses.size(c.pitchClasses() & h.apply(c).pitchClasses());
                assertTrue(e.getKey() + " " + c, common >= 3);
            }
        }
    }

    /** P35, R35 and RR35 agree on pitch classes and differ in the marked root of d. */
    public void testDiminishedMarkedRoots() {
        SeventhChord hd = new SeventhChord(HD, 0);
        assertEquals(new SeventhChord(DIM, 0), g("P35").apply(hd));
        assertEquals(new SeventhChord(DIM, 9), g("R35").apply(hd));
        assertEquals(new SeventhChord(DIM, 6), g("RR35").apply(hd));
        assertEquals(g("P35").apply(hd).pitchClasses(), g("RR35").apply(hd).pitchClasses());
    }

    /** Eq. (9): (s2, v2)(s1, v1) = (s2 s1, v1 + v2 o s1), the right factor acting first. */
    public void testCompositionRule() {
        for (PlrqElement a : Plrq.generators().values()) {
            for (PlrqElement b : Plrq.generators().values()) {
                for (SeventhChord c : SeventhChord.all()) {
                    assertEquals(a.apply(b.apply(c)), a.compose(b).apply(c));
                }
            }
        }
    }

    /**
     * Eq. (10) and the extra element (Id, (7, 0, 0, 0, -7)). The paper's own
     * sums under eq. (9) give (-7, 7, 0, 0, 0) and (0, -7, 7, 0, 0) for the
     * second and third products; eq. (10) prints the negatives, which generate
     * the same subgroup. The extra generator, printed as "P1235 R23 P12 L15
     * L13", is read as P12 P35 R23 P12 L15 L13.
     */
    public void testKernelProducts() {
        assertEquals(PlrqElement.translationVector(0, -7, 0, 7, 0), g("R42").compose(g("L42")));
        assertEquals(PlrqElement.translationVector(-7, 7, 0, 0, 0), Plrq.word("P14 L42 P14 R12"));
        assertEquals(PlrqElement.translationVector(0, -7, 7, 0, 0), Plrq.word("P12 L13 P12 R23"));
        assertEquals(PlrqElement.translationVector(7, 0, 0, 0, -7), Plrq.word("P12 P35 R23 P12 L15 L13"));
        assertTrue(Plrq.contains(PlrqElement.translationVector(7, -7, 0, 0, 0)));
    }

    /** P12, P14, P23, P35 give the symmetric group on the five types. */
    public void testPermutationPartIsS5() {
        List<PlrqElement> ps = Arrays.asList(g("P12"), g("P14"), g("P23"), g("P35"));
        assertEquals(120, Plrq.order(ps));
    }

    /**
     * Thm. 1: |PLRQ| = 120 &middot; 12^4 = 2,488,320, every element has
     * &Sigma;v = 0, and the elements (Id, v) are exactly the 12^4 vectors
     * with &Sigma;v = 0, so PLRQ = S5 &#8905; Z with Z &#8773; Z12^4.
     */
    public void testTheorem1() {
        Plrq.Structure s = Plrq.structure(new ArrayList<PlrqElement>(Plrq.generators().values()));
        assertEquals(2488320L, s.order);
        assertEquals(120, s.permutations);
        assertEquals(20736L, s.kernel);
        assertTrue(s.allSumsZero);
        assertTrue(s.kernelIsSumZero);
    }

    // ---- Blues for Alice

    /** Defs. 3.1, 3.3: R42 and L13 are Cannas's; Def. 3.5: P42 exchanges M and m on the same root. */
    public void testBluesForAliceMorphisms() {
        assertEquals(ch("Dm7"), Plrq.R42.apply(ch("Fmaj7")));
        assertEquals(ch("Fmaj7"), Plrq.R42.apply(ch("Dm7")));
        assertEquals(ch("Bm7b5"), Plrq.L13.apply(ch("G7")));
        assertEquals(ch("Bm7"), Plrq.P42.apply(ch("Bmaj7")));
        assertEquals(g("R42"), Plrq.R42);
        assertEquals(g("L13"), Plrq.L13);
    }

    /** R42 is an involution and commutes with T^5 (and with every T^s). */
    public void testR42InvolutionCommutingWithT5() {
        assertTrue(Plrq.R42.compose(Plrq.R42).isIdentity());
        assertEquals(Plrq.R42.compose(Plrq.t(5)), Plrq.t(5).compose(Plrq.R42));
        for (int s = 0; s < 12; s++) {
            for (PlrqElement e : Plrq.generators().values()) {
                assertEquals(e.compose(Plrq.t(s)), Plrq.t(s).compose(e));
            }
        }
    }

    /** Prop. 4.2 in C major. */
    public void testProposition42() {
        assertEquals(ch("Fmaj7"), Plrq.t(5).apply(ch("Cmaj7")));
        assertEquals(ch("Dm7"), Plrq.t(10).apply(ch("Em7")));
        assertEquals(ch("Dm7"), Plrq.R42.apply(ch("Fmaj7")));
        assertEquals(ch("Fmaj7"), Plrq.R42.apply(ch("Dm7")));
    }

    /** Prop. 7.1: P42(I7 of K) = II7 of K - 2 for all twelve keys. */
    public void testProposition71() {
        for (int k = 0; k < 12; k++) {
            Tonality key = Tonality.of(Interpretations.C_MAJOR, 0, 4).transpose(k);
            Tonality below = key.transpose(-2);
            assertEquals("key " + k, SeventhChord.degree(below, 1), Plrq.P42.apply(SeventhChord.degree(key, 0)));
        }
    }

    /** P42 = T8 L42 on major sevenths only; P42 lies in PLRQ and is not a generator. */
    public void testP42AgainstT8L42() {
        PlrqElement t8l42 = Plrq.t(8).compose(g("L42"));
        for (int x = 0; x < 12; x++) {
            assertEquals(Plrq.P42.apply(new SeventhChord(M, x)), t8l42.apply(new SeventhChord(M, x)));
            assertFalse(Plrq.P42.apply(new SeventhChord(MIN, x)).equals(t8l42.apply(new SeventhChord(MIN, x))));
            assertFalse(Plrq.P42.apply(new SeventhChord(D, x)).equals(t8l42.apply(new SeventhChord(D, x))));
        }
        assertTrue(Plrq.contains(Plrq.P42));
        assertFalse(Plrq.contains(t8l42));
        assertFalse(Plrq.generators().containsValue(Plrq.P42));
    }

    /** T^s has &Sigma;v = 5s, so T^s is outside PLRQ for s &ne; 0 and |&lt;PLRQ, T&gt;| = 12 |PLRQ|. */
    public void testTranspositionsAndTheGroupoidGroup() {
        assertTrue(Plrq.contains(Plrq.t(0)));
        for (int s = 1; s < 12; s++) {
            assertFalse(Plrq.contains(Plrq.t(s)));
        }
        List<PlrqElement> gens = new ArrayList<PlrqElement>(Plrq.generators().values());
        gens.add(Plrq.t(1));
        assertEquals(29859840L, Plrq.structure(gens).order);
    }

    /** Def. 4.1 and the coslice triangles under I7 and III7 (BfA p. 5). */
    public void testCosliceTriangles() {
        SeventhChord i7 = ch("Cmaj7"), iii7 = ch("Em7");
        assertTrue(Plrq.isCosliceMorphism(i7, Plrq.t(5), Plrq.R42.compose(Plrq.t(5)), Plrq.R42));
        assertTrue(Plrq.isCosliceMorphism(iii7, Plrq.t(10), Plrq.R42.compose(Plrq.t(10)), Plrq.R42));
        assertEquals(ch("Dm7"), Plrq.R42.compose(Plrq.t(5)).apply(i7));
        assertEquals(ch("Fmaj7"), Plrq.R42.compose(Plrq.t(10)).apply(iii7));
        assertFalse(Plrq.isCosliceMorphism(i7, Plrq.t(5), Plrq.R42.compose(Plrq.t(5)), Plrq.L13));
        assertFalse(ch("Cmaj7").equals(Plrq.R42.apply(ch("Cmaj7"))));
    }

    /** Thm. 5.1: the prism's faces commute. */
    public void testPrism() {
        PlrqElement t7r = Plrq.t(7).compose(Plrq.R42);
        assertEquals(ch("Em7"), t7r.apply(ch("Cmaj7")));
        assertEquals(Plrq.t(5), Plrq.R42.compose(Plrq.t(10)).compose(t7r));
        assertEquals(Plrq.t(5).compose(Plrq.R42), Plrq.t(10).compose(t7r));
        assertTrue(Plrq.prismCommutes());
    }

    /** Def. 5.2: A, B by R42 and C by L13 send the non-common member to the non-common member. */
    public void testConglomerate() {
        Tonality c = Tonality.of(Interpretations.C_MAJOR, 0, 4);
        int[][] pairs = { { 1, 3 }, { 1, 3 }, { 4, 6 } }; // A: II -> IV; B: II -> IV; C: V -> VII
        PlrqElement[] by = { Plrq.R42, Plrq.R42, Plrq.L13 };
        for (int i = 0; i < 3; i++) {
            assertEquals(SeventhChord.degree(c, pairs[i][1]), by[i].apply(SeventhChord.degree(c, pairs[i][0])));
        }
        Set<String> names = new HashSet<String>(Plrq.conglomerate().keySet());
        assertEquals(new HashSet<String>(Arrays.asList("A", "B", "C")), names);
    }

    /** Each element acts on each type's roots as a translation, a stage-2 Z12 affine map. */
    public void testRootMapsAreTranslations() {
        PlrqElement r = Plrq.R42;
        assertEquals(9, r.rootMap(M).apply(0));
        assertEquals(1, r.rootMap(M).getFactor());
        assertEquals(MIN, r.targetType(M));
    }
}
