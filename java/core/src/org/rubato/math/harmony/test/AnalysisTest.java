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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.global.Interpretations;
import org.rubato.math.harmony.CadentialAnalysis;
import org.rubato.math.harmony.CadentialAnalysis.Mode;
import org.rubato.math.harmony.Cadences;
import org.rubato.math.harmony.ChordSymbol;
import org.rubato.math.harmony.Modulations;
import org.rubato.math.harmony.Modulations.Filter;
import org.rubato.math.harmony.Modulations.Rigidity;
import org.rubato.math.harmony.PitchClasses;
import org.rubato.math.harmony.Plrq;
import org.rubato.math.harmony.Quantized;
import org.rubato.math.harmony.SeventhChord;
import org.rubato.math.harmony.Tonality;

/**
 * Cadential-region analysis of lead sheets, checked against the analyses of
 * Agust&iacute;n-Aquino, Blues for Alice (arXiv 2601.07161, &sect;6, p. 7, and
 * &sect;7, p. 8). LITERAL mode matches chord pitch classes to degree tetrads;
 * ROOT mode maps chord roots to degrees, a mechanical stand-in for the paper's
 * informal "articulates" (a departure: no alteration or ii&ndash;V syntax is
 * modelled). The tests assert the paper's explicit claims and pin the
 * further sets ROOT mode finds.
 */
public class AnalysisTest extends TestCase {

    static final Tonality C4 = Tonality.of(Interpretations.C_MAJOR, 0, 4);
    static final Tonality F = C4.transpose(5), B_FLAT = C4.transpose(10), E_FLAT = C4.transpose(3),
                          A = C4.transpose(9);

    /** BfA &sect;6, the chord cells as printed. */
    static final String ALICE = "F6 | Em7 A7b9 | Dm7 G7 | Cm7 F7 | Bb7 | Bbm7 Eb7 | Am7 D7 | Abm7 Db7 | Gm7 | C7 | Am7 | Dm7 | Gm7 C7";

    static List<List<ChordSymbol>> bars() {
        return ChordSymbol.parseBars(ALICE);
    }

    static List<ChordSymbol> bars(int from, int to) {
        return CadentialAnalysis.flatten(bars().subList(from - 1, to));
    }

    static int deg(String s) { return Cadences.parse(s); }

    public void testChordSymbols() {
        assertEquals(PitchClasses.of(5, 9, 0, 2), ChordSymbol.parse("F6").pitchClasses());
        assertEquals(PitchClasses.of(9, 1, 4, 7, 10), ChordSymbol.parse("A7b9").pitchClasses());
        assertEquals(PitchClasses.of(5, 9, 1, 3), ChordSymbol.parse("F+7").pitchClasses());
        assertEquals(PitchClasses.of(8, 0, 3, 6, 10), ChordSymbol.parse("Ab9").pitchClasses());
        assertEquals(PitchClasses.of(10, 2, 5, 7), ChordSymbol.parse("Bb6").pitchClasses());
        assertEquals(PitchClasses.of(2, 5, 8, 0), ChordSymbol.parse("Dm7b5").pitchClasses());
        assertEquals(PitchClasses.of(1, 4, 8, 11), ChordSymbol.parse("C#m7").pitchClasses());
        assertEquals(PitchClasses.of(11, 3, 6, 10), ChordSymbol.parse("Bmaj7").pitchClasses());
        assertEquals(10, ChordSymbol.parse("B\u266d7").root());
        assertEquals(13, bars().size());
        try {
            ChordSymbol.parse("Fsus4");
            fail("unknown quality");
        }
        catch (IllegalArgumentException e) { /* expected */ }
    }

    /** Mm. 1-3 articulate J1 through roots; no degree of J1 occurs literally. */
    public void testAliceBars1To3() {
        List<ChordSymbol> c = bars(1, 3);
        assertTrue(CadentialAnalysis.articulated(F, c, Mode.ROOT).contains(deg("I,II")));
        assertEquals(deg("VI"), CadentialAnalysis.degrees(F, c, Mode.LITERAL));
        assertFalse(CadentialAnalysis.articulated(F, c, Mode.LITERAL).contains(deg("I,II")));
        // further sets found by root mode, not claimed by the paper
        assertEquals(Arrays.asList(deg("VII"), deg("I,II"), deg("II,III")),
                     CadentialAnalysis.articulated(F, c, Mode.ROOT));
    }

    /** Mm. 4-5 articulate J2 through the roots of F7 and B&flat;7. */
    public void testAliceBars4To5() {
        List<ChordSymbol> c = bars(4, 5);
        assertTrue(CadentialAnalysis.articulated(F, c, Mode.ROOT).contains(deg("I,IV")));
        assertEquals(Arrays.asList(deg("V"), deg("I,IV")), CadentialAnalysis.articulated(F, c, Mode.ROOT));
    }

    /** Mm. 6-7: B&flat;m7 on root IV, Am7 the literal III7; together J4. */
    public void testAliceBars6To7() {
        List<ChordSymbol> c = bars(6, 7);
        assertTrue(PitchClasses.isSubset(deg("III"), CadentialAnalysis.degrees(F, c, Mode.LITERAL)));
        assertFalse(PitchClasses.isSubset(deg("IV"), CadentialAnalysis.degrees(F, c, Mode.LITERAL)));
        assertEquals(Arrays.asList(deg("III,IV")), CadentialAnalysis.articulated(F, c, Mode.ROOT));
    }

    /** Mm. 9-12 display J3 literally through Gm7 and Am7 (C7 also gives J5 literally). */
    public void testAliceBars9To12() {
        List<ChordSymbol> c = bars(9, 12);
        assertEquals(deg("II,III,V,VI"), CadentialAnalysis.degrees(F, c, Mode.LITERAL));
        assertEquals(Arrays.asList(deg("V"), deg("II,III")), CadentialAnalysis.articulated(F, c, Mode.LITERAL));
    }

    /** The path J1 &#8594; J2 &#8594; J4 &#8594; J3 runs from region B to region A (Def. 5.2, &sect;6). */
    public void testAlicePathThroughRegions() {
        int[] path = { deg("I,II"), deg("I,IV"), deg("III,IV"), deg("II,III") };
        StringBuilder regions = new StringBuilder();
        for (int j : path) {
            regions.append(CadentialAnalysis.region(j));
        }
        assertEquals("BBAA", regions.toString());
        assertEquals("C", CadentialAnalysis.region(deg("V")));
        assertEquals("C", CadentialAnalysis.region(deg("VII")));
        assertNull(CadentialAnalysis.region(deg("II,V")));
    }

    /**
     * &sect;7.1: B&flat; is set up by J5 through F+7's root; in E&flat; Fm7 is the
     * literal II7 and Dm7&flat;5 (VII7) lies in A&flat;9 &cup; B&flat;6 but in
     * neither alone. These are the pivots of the one quantized modulation
     * B&flat; &#8594; E&flat;.
     */
    public void testCherokeeSectionA() {
        List<ChordSymbol> open = ChordSymbol.parseAll("Bbmaj7 F+7 Fm7 Bb7 Ebmaj7 Ab9 Bb6");
        assertTrue(CadentialAnalysis.articulated(B_FLAT, open.subList(0, 2), Mode.ROOT).contains(deg("V")));
        assertEquals(1, CadentialAnalysis.literalDegree(E_FLAT, ChordSymbol.parse("Fm7")));
        int vii = E_FLAT.degree(6);
        assertEquals(SeventhChord.parse("Dm7b5").pitchClasses(), vii);
        assertTrue(CadentialAnalysis.realisedByInclusion(vii, open.subList(5, 7)));
        assertFalse(CadentialAnalysis.realisedByInclusion(vii, open.subList(5, 6)));
        assertFalse(CadentialAnalysis.realisedByInclusion(vii, open.subList(6, 7)));
        List<Quantized> q = Modulations.quantize(B_FLAT, E_FLAT, Filter.ALGORITHM_1, Rigidity.TI12);
        assertEquals(1, q.size());
        assertEquals(deg("II,VII"), q.get(0).pivots());
    }

    /** &sect;7.1, the return: Dm7 is the literal III7 of B&flat;, F+7 stands on root V; Table 1's Tr7 pivots. */
    public void testCherokeeReturn() {
        List<ChordSymbol> back = ChordSymbol.parseAll("Dm7 C7 Cm7 Dm7 G7b9 Cm7 F+7 Bbmaj7");
        assertEquals(2, CadentialAnalysis.literalDegree(B_FLAT, ChordSymbol.parse("Dm7")));
        assertEquals(4, CadentialAnalysis.rootDegree(B_FLAT, ChordSymbol.parse("F+7")));
        assertEquals(-1, CadentialAnalysis.literalDegree(B_FLAT, ChordSymbol.parse("F+7")));
        int found = CadentialAnalysis.degrees(B_FLAT, back.subList(0, 1), Mode.LITERAL)
                    | CadentialAnalysis.degrees(B_FLAT, back.subList(6, 7), Mode.ROOT);
        List<Quantized> q = Modulations.quantize(E_FLAT, B_FLAT, Filter.ALGORITHM_1, Rigidity.TI12);
        assertEquals(1, q.size());
        assertEquals(q.get(0).pivots(), found);
    }

    /** &sect;7.2: P42(Bmaj7) = Bm7, the literal II7 of A; {II, V} is cadential in A but not minimal. */
    public void testCherokeeBridge() {
        SeventhChord bm7 = Plrq.P42.apply(SeventhChord.parse("Bmaj7"));
        assertEquals(1, CadentialAnalysis.literalDegree(A, ChordSymbol.parse(bm7.toString())));
        assertTrue(Cadences.isCadential(A, deg("II,V")));
        assertFalse(Cadences.minimal(A).contains(deg("II,V")));
    }

    /** A report lists bars, literal and root degrees and the articulated sets. */
    public void testReport() {
        String r = CadentialAnalysis.report(F, bars(), Mode.ROOT);
        assertTrue(r, r.contains("J4 {III,IV}"));
        assertTrue(r, r.contains("region A"));
    }
}
