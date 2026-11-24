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

package org.rubato.math.rhythm.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.math.arith.Rational;
import org.rubato.math.module.QElement;
import org.rubato.math.module.QRing;
import org.rubato.math.rhythm.Fraction;
import org.rubato.math.rhythm.RhythmLayout;
import org.rubato.math.rhythm.RhythmNotation;
import org.rubato.math.rhythm.RhythmTree;
import org.rubato.math.rhythm.RhythmTreeDenotators;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.ListDenotator;
import org.rubato.math.yoneda.ListForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;

/**
 * Rhythm trees after Flieder, arXiv 2512.04090, Ex. C.1 and Figure 2
 * (pp. 55-56); values from sources/RHYTHM-TREES-SPEC.md section 4.
 */
public class RhythmTreeTest extends TestCase {

    static final String FIG2 = "9.5 (2 2.5 (1 (2 1) 1 1) 3 (1.5 2))";

    private static Fraction q(String s) {
        return Fraction.parse(s);
    }

    /** The leaves as "onset + length" strings. */
    private static List<String> leaves(RhythmLayout l) {
        List<String> out = new ArrayList<String>();
        for (RhythmLayout.Span s : l.leaves()) {
            out.add(s.onset + " + " + s.length);
        }
        return out;
    }

    public void testFractionParseAndPrint() {
        assertEquals("19/2", q("9.5").toString());
        assertEquals("19/2", q("19/2").toString());
        assertEquals("5", q("5").toString());
        assertEquals("5", q("10/2").toString());
        assertEquals("3/2", q("1.50").toString());
        assertEquals("1/8", q("0.125").toString());
        assertEquals(q("3/2"), q("1.5"));
        assertEquals(1.5, q("3/2").toDouble(), 0.0);
        assertEquals("19/6", q("19/2").multiply(q("5/2")).divide(q("15/2")).toString());
        assertEquals("7/2", q("3/2").add(q("2")).toString());
        assertTrue(q("2").compareTo(q("19/10")) > 0);
        for (String bad : new String[] { "", "x", "1/0", "1.", ".5x", "--1", "1/2/3" }) {
            try {
                Fraction.parse(bad);
                fail("parsed " + bad);
            }
            catch (IllegalArgumentException expected) {
                // fine
            }
        }
    }

    /** Ex. C.1: a node is (d, [r1..rn]) with exactly n subtrees (B = Fin(n)). */
    public void testShapeIsChecked() {
        RhythmTree leaf = RhythmTree.leaf(q("1"));
        assertTrue(leaf.isLeaf());
        try {
            new RhythmTree(q("2"), Arrays.asList(q("1"), q("1")), Collections.singletonList(leaf));
            fail("two factors, one subtree");
        }
        catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("2 factors but 1 subtree"));
        }
        // D5: durations and factors greater than 0
        for (String bad : new String[] { "0", "-1" }) {
            try {
                RhythmTree.leaf(q(bad));
                fail("label " + bad);
            }
            catch (IllegalArgumentException expected) {
                // fine
            }
            try {
                new RhythmTree(q("2"), Collections.singletonList(q(bad)), Collections.singletonList(leaf));
                fail("factor " + bad);
            }
            catch (IllegalArgumentException expected) {
                // fine
            }
        }
    }

    /** D3: Figure 2 in the notation, and printing round-trips. */
    public void testNotationFigure2() {
        RhythmTree t = RhythmNotation.parse(FIG2);
        assertEquals(q("19/2"), t.getLabel());
        assertEquals(Arrays.asList(q("2"), q("5/2"), q("3")), t.getFactors());
        assertEquals(3, t.getChildren().size());
        RhythmTree two5 = t.getChildren().get(1);
        assertEquals(q("5/2"), two5.getLabel());
        assertEquals(Arrays.asList(q("1"), q("1"), q("1")), two5.getFactors());
        assertEquals(Arrays.asList(q("2"), q("1")), two5.getChildren().get(0).getFactors());
        assertEquals(Arrays.asList(q("3/2"), q("2")), t.getChildren().get(2).getFactors());
        assertEquals(11, t.nodeCount());
        assertEquals("depth counts edges: root, 2.5, 1, then the leaves 2 and 1", 3, t.depth());
        assertEquals("19/2 (2 5/2 (1 (2 1) 1 1) 3 (3/2 2))", t.toNotation());
        assertEquals(t, RhythmNotation.parse(t.toNotation()));
    }

    /** D3: r@d gives a child whose own label differs from its factor. */
    public void testNotationOwnLabel() {
        RhythmTree t = RhythmNotation.parse("4 (1 3@2 (1 1))");
        assertEquals(q("1"), t.getChildren().get(0).getLabel());
        assertEquals(q("3"), t.getFactors().get(1));
        assertEquals(q("2"), t.getChildren().get(1).getLabel());
        assertEquals("4 (1 3@2 (1 1))", t.toNotation());
        assertEquals(t, RhythmNotation.parse(t.toNotation()));
    }

    public void testNotationErrors() {
        String[][] cases = {
            { "", "empty" },
            { "2@3", "root" },
            { "4 (1 1", "missing )" },
            { "4 (1 1))", "after the tree" },
            { "4 ()", "no branches" },
            { "4 (1 x)", "position 6" },
            { "4 (0 1)", "greater than 0" },
        };
        for (String[] c : cases) {
            try {
                RhythmNotation.parse(c[0]);
                fail("parsed " + c[0]);
            }
            catch (IllegalArgumentException e) {
                assertTrue(c[0] + ": " + e.getMessage(), e.getMessage().contains(c[1]));
            }
        }
    }

    /** D5: a value that is not positive is reported at its own position, for factors and own labels. */
    public void testNonPositivePositions() {
        String[][] cases = {
            { "4 (0 1)", "position 4: 0 must be greater than 0" },
            { "4 (1 1@-2)", "position 8: -2 must be greater than 0" },
            { "0 (1)", "position 1: 0 must be greater than 0" },
        };
        for (String[] c : cases) {
            try {
                RhythmNotation.parse(c[0]);
                fail("parsed " + c[0]);
            }
            catch (IllegalArgumentException e) {
                assertTrue(c[0] + ": " + e.getMessage(), e.getMessage().startsWith(c[1]));
            }
        }
    }

    /** D1, spec section 4: Figure 2 in proportional timing. */
    public void testProportionalFigure2() {
        RhythmLayout l = RhythmLayout.layout(RhythmNotation.parse(FIG2), RhythmLayout.Timing.PROPORTIONAL, Fraction.ZERO);
        assertEquals(Arrays.asList("0 + 38/15", "38/15 + 19/27", "437/135 + 19/54", "323/90 + 19/18",
                                   "209/45 + 19/18", "57/10 + 57/35", "513/70 + 76/35"), leaves(l));
        Fraction sum = Fraction.ZERO;
        for (RhythmLayout.Span s : l.leaves()) {
            sum = sum.add(s.length);
        }
        assertEquals(q("19/2"), sum);
        assertEquals(q("19/2"), l.end());
        assertTrue(l.mismatches().isEmpty());
        assertEquals(11, l.spans().size());
        RhythmLayout.Span two5 = l.spans().get(2);
        assertEquals(1, two5.depth);
        assertEquals(q("38/15"), two5.onset);
        assertEquals(q("19/6"), two5.length);
        assertEquals("1", two5.path);
        assertEquals("", l.spans().get(0).path);
        assertEquals("1.0.1", l.spans().get(5).path);
    }

    /** Ex. C.1 (p. 55): 5 divided proportionally into 1, 1, 1 is three equal parts. */
    public void testThreeEqualParts() {
        RhythmTree t = RhythmNotation.parse("5 (1 1 1)");
        assertEquals(Arrays.asList("0 + 5/3", "5/3 + 5/3", "10/3 + 5/3"),
                     leaves(RhythmLayout.layout(t, RhythmLayout.Timing.PROPORTIONAL, Fraction.ZERO)));
        RhythmLayout label = RhythmLayout.layout(t, RhythmLayout.Timing.LABEL, Fraction.ZERO);
        assertEquals(Arrays.asList("0 + 1", "1 + 1", "2 + 1"), leaves(label));
        assertEquals(1, label.mismatches().size());
        assertEquals(q("3"), label.mismatches().get(0).childrenTotal);
    }

    /** D2, spec section 4: Figure 2 in label timing ends at 8 and overlaps. */
    public void testLabelFigure2() {
        RhythmLayout l = RhythmLayout.layout(RhythmNotation.parse(FIG2), RhythmLayout.Timing.LABEL, Fraction.ZERO);
        assertEquals(Arrays.asList("0 + 2", "2 + 2", "4 + 1", "3 + 1", "4 + 1", "9/2 + 3/2", "6 + 2"), leaves(l));
        Fraction last = Fraction.ZERO;
        for (RhythmLayout.Span s : l.leaves()) {
            last = s.end().compareTo(last) > 0 ? s.end() : last;
        }
        assertEquals("the last note ends at 8", q("8"), last);
        assertEquals("the root itself still spans its label", q("19/2"), l.end());
        List<String> mm = new ArrayList<String>();
        for (RhythmLayout.Span s : l.mismatches()) {
            mm.add(s.path + ":" + s.length + " vs " + s.childrenTotal);
        }
        assertEquals(Arrays.asList(":19/2 vs 15/2", "1:5/2 vs 3", "1.0:1 vs 3", "2:3 vs 7/2"), mm);
    }

    /** A start onset shifts every span. */
    public void testStartOnset() {
        RhythmLayout l = RhythmLayout.layout(RhythmNotation.parse("4 (1 1 (1 1 1) 1)"), RhythmLayout.Timing.PROPORTIONAL, q("8"));
        assertEquals(Arrays.asList("8 + 4/3", "28/3 + 4/9", "88/9 + 4/9", "92/9 + 4/9", "32/3 + 4/3"), leaves(l));
        assertEquals(q("12"), l.end());
    }

    /** D1: in proportional timing a child's own label does not change its length. */
    public void testOwnLabelIgnoredInProportional() {
        RhythmTree a = RhythmNotation.parse("4 (1 3)");
        RhythmTree b = RhythmNotation.parse("4 (1@7 3@1/5)");
        assertEquals(leaves(RhythmLayout.layout(a, RhythmLayout.Timing.PROPORTIONAL, Fraction.ZERO)),
                     leaves(RhythmLayout.layout(b, RhythmLayout.Timing.PROPORTIONAL, Fraction.ZERO)));
        assertEquals(Arrays.asList("0 + 7", "7 + 1/5"),
                     leaves(RhythmLayout.layout(b, RhythmLayout.Timing.LABEL, Fraction.ZERO)));
    }

    /** D8: size limits. */
    public void testLimits() {
        StringBuilder deep = new StringBuilder("1");
        for (int i = 0; i < 70; i++) {
            deep.append(" (1");
        }
        for (int i = 0; i < 70; i++) {
            deep.append(")");
        }
        try {
            RhythmNotation.parse(deep.toString());
            fail("depth 71 accepted");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("depth"));
        }
        StringBuilder wide = new StringBuilder("1 (");
        for (int i = 0; i < 4100; i++) {
            wide.append("1 ");
        }
        wide.append(")");
        try {
            RhythmNotation.parse(wide.toString());
            fail("4101 nodes accepted");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("nodes"));
        }
    }

    /** D4: the recursive form, registered with the built-ins. */
    public void testForms() {
        Repository rep = Repository.systemRepository();
        LimitForm tree = (LimitForm) rep.getForm(RhythmTreeDenotators.RHYTHM_TREE);
        assertNotNull(tree);
        assertTrue(rep.isBuiltin(tree));
        assertEquals(2, tree.getFormCount());
        LimitForm label = (LimitForm) tree.getForm(0);
        assertEquals(RhythmTreeDenotators.RHYTHM_LABEL, label.getNameString());
        SimpleForm duration = (SimpleForm) label.getForm(0);
        assertEquals(RhythmTreeDenotators.RHYTHM_DURATION, duration.getNameString());
        assertEquals(QRing.ring, duration.getModule());
        ListForm factors = (ListForm) label.getForm(1);
        assertEquals(RhythmTreeDenotators.RHYTHM_FACTORS, factors.getNameString());
        assertEquals(RhythmTreeDenotators.RHYTHM_FACTOR, factors.getForm().getNameString());
        assertEquals(QRing.ring, ((SimpleForm) factors.getForm()).getModule());
        ListForm subtrees = (ListForm) tree.getForm(1);
        assertEquals(RhythmTreeDenotators.RHYTHM_SUBTREES, subtrees.getNameString());
        assertSame("the subtrees are rhythm trees again", tree, subtrees.getForm());
    }

    /** D4: Figure 2 as a denotator and back. */
    public void testDenotatorRoundTrip() {
        RhythmTree t = RhythmNotation.parse(FIG2);
        Denotator d = RhythmTreeDenotators.toDenotator(t);
        assertSame(Repository.systemRepository().getForm(RhythmTreeDenotators.RHYTHM_TREE), d.getForm());
        LimitDenotator root = (LimitDenotator) d;
        LimitDenotator label = (LimitDenotator) root.getFactor(0);
        Rational dur = ((QElement) ((SimpleDenotator) label.getFactor(0)).getElement()).getValue();
        assertEquals(new Rational(19, 2), dur);
        assertEquals(3, ((ListDenotator) label.getFactor(1)).getFactorCount());
        assertEquals(3, ((ListDenotator) root.getFactor(1)).getFactorCount());
        assertEquals(t, RhythmTreeDenotators.fromDenotator(d));
        RhythmTree own = RhythmNotation.parse("4 (1 3@2 (1 1))");
        assertEquals(own, RhythmTreeDenotators.fromDenotator(RhythmTreeDenotators.toDenotator(own)));
    }

    /** D4: a denotator whose lists disagree in length is refused when read. */
    public void testMalformedDenotator() throws Exception {
        Denotator good = RhythmTreeDenotators.toDenotator(RhythmNotation.parse("4 (1 1)"));
        LimitDenotator root = (LimitDenotator) good;
        ListDenotator subtrees = (ListDenotator) root.getFactor(1);
        List<Denotator> one = new ArrayList<Denotator>(subtrees.getFactors().subList(0, 1));
        ListDenotator shorter = new ListDenotator(null, (ListForm) subtrees.getForm(), one);
        List<Denotator> coords = new ArrayList<Denotator>();
        coords.add(root.getFactor(0));
        coords.add(shorter);
        Denotator bad = new LimitDenotator(null, (LimitForm) root.getForm(), coords);
        try {
            RhythmTreeDenotators.fromDenotator(bad);
            fail("2 factors and 1 subtree accepted");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("2 factors but 1 subtree"));
        }
        try {
            RhythmTreeDenotators.fromDenotator(((LimitDenotator) RhythmTreeDenotators.toDenotator(RhythmNotation.parse("1"))).getFactor(0));
            fail("a label accepted as a tree");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("RhythmTree"));
        }
    }

    /** D8: values beyond Rubato's int Rational are refused, not wrapped. */
    public void testRationalOverflowRefused() {
        RhythmTree big = RhythmTree.leaf(q("4294967296"));
        try {
            RhythmTreeDenotators.toDenotator(big);
            fail("2^32 accepted");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("too large"));
        }
    }
}
