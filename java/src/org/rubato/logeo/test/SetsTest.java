package org.rubato.logeo.test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.logeo.Sets;
import org.rubato.math.arith.Rational;
import org.rubato.math.module.QElement;
import org.rubato.math.module.RElement;
import org.rubato.math.module.ZElement;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;

/**
 * Checks the merge-based set operations in {@link Sets} on power denotators.
 * PowerDenotator.getFactors() returns a LinkedList, so a merge that indexes
 * into its inputs without copying them runs in quadratic time. The large-score
 * test times a union of two 50,000-note scores, which takes well under a
 * second with linear merging and many seconds with quadratic merging.
 */
public class SetsTest extends TestCase {

    private static final Repository REP = Repository.systemRepository();
    private static final PowerForm SCORE = (PowerForm) REP.getForm("Score");
    private static final LimitForm NOTE = (LimitForm) REP.getForm("Note");
    private static final SimpleForm ONSET = (SimpleForm) REP.getForm("Onset");
    private static final SimpleForm PITCH = (SimpleForm) REP.getForm("Pitch");
    private static final SimpleForm LOUDNESS = (SimpleForm) REP.getForm("Loudness");
    private static final SimpleForm DURATION = (SimpleForm) REP.getForm("Duration");
    private static final SimpleForm VOICE = (SimpleForm) REP.getForm("Voice");

    /** A score holding one note for every onset index from first to last, stepping by step. */
    private static PowerDenotator score(int first, int last, int step) throws Exception {
        List<Denotator> notes = new ArrayList<Denotator>();
        for (int i = first; i <= last; i += step) {
            List<Denotator> c = new ArrayList<Denotator>(5);
            c.add(new SimpleDenotator(null, ONSET, new RElement(i * 0.25)));
            c.add(new SimpleDenotator(null, PITCH, new QElement(new Rational(36 + (i * 7) % 48))));
            c.add(new SimpleDenotator(null, LOUDNESS, new ZElement(60 + i % 60)));
            c.add(new SimpleDenotator(null, DURATION, new RElement(0.5)));
            c.add(new SimpleDenotator(null, VOICE, new ZElement(i % 4)));
            notes.add(new LimitDenotator(null, NOTE, c));
        }
        return new PowerDenotator(NameDenotator.make("s"), SCORE, notes);
    }

    private static List<Denotator> sorted(TreeSet<Denotator> set) {
        return new ArrayList<Denotator>(set);
    }

    private static void assertFactors(List<Denotator> expected, PowerDenotator actual) {
        List<Denotator> factors = actual.getFactors();
        assertEquals(expected.size(), factors.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("factor " + i, expected.get(i), factors.get(i));
        }
    }

    public void testUnionIntersectionDifferenceMatchSortedSets() throws Exception {
        // multiples of 2 in [0, 60] and multiples of 3 in [0, 90] overlap on multiples of 6
        PowerDenotator a = score(0, 60, 2);
        PowerDenotator b = score(0, 90, 3);
        TreeSet<Denotator> sa = new TreeSet<Denotator>(a.getFactors());
        TreeSet<Denotator> sb = new TreeSet<Denotator>(b.getFactors());

        TreeSet<Denotator> union = new TreeSet<Denotator>(sa);
        union.addAll(sb);
        assertFactors(sorted(union), Sets.union(a, b));

        TreeSet<Denotator> inter = new TreeSet<Denotator>(sa);
        inter.retainAll(sb);
        assertFactors(sorted(inter), Sets.intersection(a, b));

        TreeSet<Denotator> diff = new TreeSet<Denotator>(sa);
        diff.removeAll(sb);
        assertFactors(sorted(diff), Sets.difference(a, b));
    }

    public void testUnionOfLargeScoresIsLinear() throws Exception {
        int n = 50000;
        PowerDenotator a = score(0, 2 * n - 2, 2);
        PowerDenotator b = score(1, 2 * n - 1, 2);
        long start = System.nanoTime();
        PowerDenotator u = Sets.union(a, b);
        long ms = (System.nanoTime() - start) / 1000000;
        System.out.println("SetsTest: union of two " + n + "-note scores took " + ms + " ms");
        assertEquals(2 * n, u.getFactorCount());
        // linear merging takes tens of milliseconds here; indexing the LinkedList inputs takes seconds
        assertTrue("union of two " + n + "-note scores took " + ms + " ms", ms < 2000);
    }
}
