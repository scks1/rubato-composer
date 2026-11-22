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
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.global.GlobalComposition;
import org.rubato.math.global.Interpretations;
import org.rubato.math.harmony.Cadences;
import org.rubato.math.harmony.PitchClasses;
import org.rubato.math.harmony.Tonality;

/**
 * Degrees and minimal cadential sets (Mazzola, <i>The Topos of Music I</i>,
 * &sect;26.2.1, Def. 87, pp. 455&ndash;456; &sect;27.1.4, p. 470;
 * Agust&iacute;n-Aquino and Mazzola, J. Math. Music 14, 2020, &sect;4 and
 * Remark 5.1; Agust&iacute;n-Aquino, Blues for Alice, arXiv 2601.07161, p. 3;
 * Topos IV, App. P.1.1, pp. 1525&ndash;1526).
 */
public class CadenceTest extends TestCase {

    static final int I = 0, II = 1, III = 2, IV = 3, V = 4, VI = 5, VII = 6;

    static int deg(int... ds) {
        int m = 0;
        for (int d : ds) {
            m |= 1 << d;
        }
        return m;
    }

    // ---- pitch-class sets

    public void testPitchClassMasks() {
        int c = PitchClasses.of(0, 4, 7);
        assertEquals(0x91, c);
        assertEquals("{0,4,7}", PitchClasses.toString(c));
        assertEquals(PitchClasses.of(5, 9, 0), PitchClasses.transpose(c, 5));
        assertEquals(PitchClasses.of(0, 8, 5), PitchClasses.invert(c));
        assertEquals(3, PitchClasses.size(c));
        assertTrue(Arrays.equals(new int[] { 0, 4, 7 }, PitchClasses.toArray(c)));
    }

    // ---- degrees

    /** Degree i of X^(k) is {s_i, s_{i+2}, ...}, indexes mod 7 (p. 469). */
    public void testDegreesOfCMajor() {
        Tonality c3 = Tonality.of(Interpretations.C_MAJOR, 0, 3);
        assertEquals(PitchClasses.of(0, 4, 7), c3.degree(I));
        assertEquals(PitchClasses.of(2, 5, 9), c3.degree(II));
        assertEquals(PitchClasses.of(11, 2, 5), c3.degree(VII));
        Tonality c4 = Tonality.of(Interpretations.C_MAJOR, 0, 4);
        assertEquals(PitchClasses.of(7, 11, 2, 5), c4.degree(V));
        assertEquals(PitchClasses.of(11, 2, 5, 9), c4.degree(VII));
    }

    /** The degrees are the charts of the stage-3 third-chain interpretation. */
    public void testDegreesAreTheThirdChainCharts() {
        for (int k = 1; k <= 7; k++) {
            for (int tonic : Interpretations.C_HARMONIC_MINOR) {
                Tonality t = Tonality.of(Interpretations.C_HARMONIC_MINOR, tonic, k);
                GlobalComposition g = Interpretations.thirdChains(12, Interpretations.C_HARMONIC_MINOR, tonic, k);
                for (int i = 0; i < 7; i++) {
                    assertEquals(PitchClasses.of(g.coveringComposition(i).classes()), t.degree(i));
                }
            }
        }
    }

    public void testTonalityOfAnyScaleAndTonic() {
        Tonality f = Tonality.of(new int[] { 10, 5, 7, 9, 0, 2, 4 }, 5, 4);
        assertEquals(5, f.tonic());
        assertEquals(PitchClasses.of(5, 9, 0, 4), f.degree(I));
        assertEquals(PitchClasses.of(10, 2, 5, 9), f.degree(IV));
        assertEquals(PitchClasses.of(0, 2, 4, 5, 7, 9, 10), f.scaleMask());
        assertEquals(Tonality.of(Interpretations.C_MAJOR, 0, 4).transpose(5), f);
        try {
            Tonality.of(Interpretations.C_MAJOR, 1, 3);
            fail("tonic outside the scale");
        }
        catch (IllegalArgumentException e) { /* expected */ }
        try {
            Tonality.of(new int[] { 0, 2, 4 }, 0, 3);
            fail("three-element scale");
        }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    // ---- cadential sets

    /** Dia^(3): {VII}, {II,III}, {II,V}, {III,IV}, {IV,V} (Topos I p. 455). */
    public void testTriadicMajorCadences() {
        List<Integer> got = Cadences.minimal(Tonality.of(Interpretations.C_MAJOR, 0, 3));
        assertEquals(Arrays.asList(deg(VII), deg(II, III), deg(II, V), deg(III, IV), deg(IV, V)), got);
        assertEquals("{II,III}", Cadences.name(deg(II, III)));
    }

    /** J1..J6 of the tetradic major interpretation (BfA p. 3). */
    public void testTetradicMajorCadences() {
        List<Integer> got = Cadences.minimal(Tonality.of(Interpretations.C_MAJOR, 0, 4));
        assertEquals(Arrays.asList(deg(V), deg(VII), deg(I, II), deg(I, IV), deg(II, III), deg(III, IV)), got);
        List<Integer> j = Cadences.tetradicMajorJ();
        assertEquals(Arrays.asList(deg(I, II), deg(I, IV), deg(II, III), deg(III, IV), deg(V), deg(VII)), j);
    }

    /** {II7, V7} is cadential but not minimal (AM 2020, Remark 5.1). */
    public void testCadentialButNotMinimal() {
        Tonality c4 = Tonality.of(Interpretations.C_MAJOR, 0, 4);
        assertTrue(Cadences.isCadential(c4, deg(II, V)));
        assertFalse(Cadences.minimal(c4).contains(deg(II, V)));
        assertFalse(Cadences.isCadential(c4, deg(I)));
        assertFalse(Cadences.isCadential(c4, 0));
        assertTrue(Cadences.isCadential(c4, deg(I, II, III, IV, V, VI, VII)));
    }

    /** Harmonic minor: every pair of degrees is minimal, 21 in all (p. 456). */
    public void testHarmonicMinorHasTwentyOnePairs() {
        List<Integer> got = Cadences.minimal(Tonality.of(Interpretations.C_HARMONIC_MINOR, 0, 3));
        assertEquals(21, got.size());
        for (int m : got) {
            assertEquals(2, Integer.bitCount(m));
        }
    }

    /** Melodic minor has 15 minimal sets (p. 470). */
    public void testMelodicMinorHasFifteen() {
        assertEquals(15, Cadences.minimal(Tonality.of(Interpretations.C_MELODIC_MINOR, 0, 3)).size());
    }

    /** The first column of App. P.1.1 for all 38 TI orbits. */
    public void testMinimalCadenceCountsOfAllOrbits() {
        List<String> wrong = new ArrayList<String>();
        for (String[] row : ToposP11.ROWS) {
            int[] s = ToposP11.scale(row);
            int n = Cadences.minimal(Tonality.of(s, s[0], 3)).size();
            if (n != Integer.parseInt(row[2])) {
                wrong.add(row[0] + ": " + n + " vs " + row[2]);
            }
        }
        assertEquals(new ArrayList<String>(), wrong);
    }

    /** Renaming the tonic renames the degrees and nothing else. */
    public void testTonicChoiceOnlyRenamesDegrees() {
        Tonality c = Tonality.of(Interpretations.C_MAJOR, 0, 3);
        Tonality a = Tonality.of(Interpretations.C_MAJOR, 9, 3);
        List<String> chordsC = new ArrayList<String>();
        for (int m : Cadences.minimal(c)) {
            chordsC.add(c.chordsOf(m).toString());
        }
        List<String> chordsA = new ArrayList<String>();
        for (int m : Cadences.minimal(a)) {
            chordsA.add(a.chordsOf(m).toString());
        }
        java.util.Collections.sort(chordsC);
        java.util.Collections.sort(chordsA);
        assertEquals(chordsC, chordsA);
    }
}
