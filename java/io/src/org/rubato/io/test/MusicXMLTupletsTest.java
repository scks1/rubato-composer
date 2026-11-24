package org.rubato.io.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.io.musicxml.MusicXMLReader;
import org.rubato.io.musicxml.MusicXMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tuplet brackets in MusicXMLWriter (sources/MUSICXML-TUPLETS-SPEC.md): every
 * measure adds up from note types, dots and time modifications as well as from
 * durations; brackets open and close and hold exactly q normal notes; measures
 * without off-grid boundaries are written as before.
 */
public class MusicXMLTupletsTest extends TestCase {

    // ---- exact fractions --------------------------------------------------------------

    /** A non-negative fraction, kept in lowest terms. */
    static final class Q {
        final BigInteger n, d;

        Q(BigInteger n, BigInteger d) {
            BigInteger g = n.gcd(d);
            if (d.signum() < 0) {
                n = n.negate();
                d = d.negate();
            }
            this.n = g.signum() == 0 ? n : n.divide(g);
            this.d = g.signum() == 0 ? d : d.divide(g);
        }

        static Q of(long n, long d) {
            return new Q(BigInteger.valueOf(n), BigInteger.valueOf(d));
        }

        Q add(Q o) {
            return new Q(this.n.multiply(o.d).add(o.n.multiply(this.d)), this.d.multiply(o.d));
        }

        Q mul(Q o) {
            return new Q(this.n.multiply(o.n), this.d.multiply(o.d));
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Q && ((Q)o).n.equals(this.n) && ((Q)o).d.equals(this.d);
        }

        @Override
        public int hashCode() {
            return this.n.hashCode() * 31 + this.d.hashCode();
        }

        @Override
        public String toString() {
            return this.d.equals(BigInteger.ONE) ? this.n.toString() : this.n + "/" + this.d;
        }
    }

    static final String[] TYPES = { "breve", "whole", "half", "quarter", "eighth", "16th", "32nd", "64th", "128th", "256th", "512th", "1024th" };

    /** A note type in quarter notes. */
    static Q typeValue(String type) {
        int i = Arrays.asList(TYPES).indexOf(type);
        assertTrue("unknown type " + type, i >= 0);
        return i <= 3 ? Q.of(1L << (3 - i), 1) : Q.of(1, 1L << (i - 3));
    }

    /** Written value with dots, in quarter notes. */
    static Q dotted(Q v, int dots) {
        Q out = v, add = v;
        for (int i = 0; i < dots; i++) {
            add = add.mul(Q.of(1, 2));
            out = out.add(add);
        }
        return out;
    }

    // ---- the structural check ---------------------------------------------------------

    static Document dom(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newDefaultInstance();
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    static Element child(Element e, String tag) {
        for (Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element && ((Element)c).getTagName().equals(tag)) {
                return (Element)c;
            }
        }
        return null;
    }

    static List<Element> children(Element e, String tag) {
        List<Element> out = new ArrayList<Element>();
        for (Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element && (tag == null || ((Element)c).getTagName().equals(tag))) {
                out.add((Element)c);
            }
        }
        return out;
    }

    static int intText(Element e, String tag) {
        return Integer.parseInt(child(e, tag).getTextContent().trim());
    }

    /** Counts of what the check saw, for assertions about brackets. */
    static final class Seen {
        final List<String> brackets = new ArrayList<String>();
        int typed, notes, hiddenRests;
    }

    /**
     * Checks a file the writer wrote: in every measure, every voice adds up to
     * the measure both in durations and in written values times time
     * modifications; each typed note's duration equals its written value
     * times normal/actual; every bracket opens and closes in one voice of one
     * measure, is not nested, and holds q normal notes of its tuplet-normal type.
     */
    static Seen check(String xml) throws Exception {
        Seen seen = new Seen();
        Document d = dom(xml);
        for (Element part : children(d.getDocumentElement(), "part")) {
            Q divisions = null;
            Q measure = null;
            for (Element m : children(part, "measure")) {
                Element attrs = child(m, "attributes");
                if (attrs != null && child(attrs, "divisions") != null) {
                    divisions = Q.of(intText(attrs, "divisions"), 1);
                    Element time = child(attrs, "time");
                    measure = Q.of(4L * intText(time, "beats"), intText(time, "beat-type"));
                }
                Map<String, Q> byDuration = new HashMap<String, Q>();
                Map<String, Q> byType = new HashMap<String, Q>();
                Map<String, Object[]> open = new HashMap<String, Object[]>();
                for (Element e : children(m, null)) {
                    String tag = e.getTagName();
                    if (tag.equals("forward")) {
                        String v = child(e, "voice").getTextContent().trim();
                        Q len = Q.of(intText(e, "duration"), 1).mul(new Q(BigInteger.ONE, divisions.n));
                        byDuration.merge(v, len, Q::add);
                        byType.merge(v, len, Q::add);
                        assertFalse("forward inside an open bracket in voice " + v, open.containsKey(v));
                        continue;
                    }
                    if (!tag.equals("note")) {
                        continue;
                    }
                    seen.notes++;
                    if (child(e, "chord") != null || child(e, "grace") != null) {
                        assertNull("a chord note or grace note carries a tuplet", child(e, "notations") == null ? null : child(child(e, "notations"), "tuplet"));
                        continue;
                    }
                    String v = child(e, "voice").getTextContent().trim();
                    Q len = Q.of(intText(e, "duration"), 1).mul(new Q(BigInteger.ONE, divisions.n));
                    byDuration.merge(v, len, Q::add);
                    Element rest = child(e, "rest");
                    if (rest != null && "no".equals(e.getAttribute("print-object"))) {
                        seen.hiddenRests++;
                    }
                    Element type = child(e, "type");
                    if (type == null) {
                        assertTrue("a note without a type that is not a measure rest", rest != null && "yes".equals(rest.getAttribute("measure")));
                        byType.merge(v, len, Q::add);
                        continue;
                    }
                    seen.typed++;
                    Q written = dotted(typeValue(type.getTextContent().trim()), children(e, "dot").size());
                    Element tm = child(e, "time-modification");
                    Q sounding = written;
                    if (tm != null) {
                        sounding = written.mul(Q.of(intText(tm, "normal-notes"), intText(tm, "actual-notes")));
                    }
                    assertEquals("duration against type in measure " + m.getAttribute("number"), sounding, len);
                    byType.merge(v, sounding, Q::add);
                    Element notations = child(e, "notations");
                    List<Element> tuplets = notations == null ? new ArrayList<Element>() : children(notations, "tuplet");
                    for (Element t : tuplets) {
                        assertEquals("brackets are not nested", "1", t.getAttribute("number"));
                        if ("start".equals(t.getAttribute("type"))) {
                            assertFalse("a bracket opens inside another", open.containsKey(v));
                            assertNotNull("a bracket without time-modification", tm);
                            Element actual = child(t, "tuplet-actual"), normal = child(t, "tuplet-normal");
                            int q = intText(actual, "tuplet-number"), n = intText(normal, "tuplet-number");
                            assertEquals(q, intText(tm, "actual-notes"));
                            assertEquals(n, intText(tm, "normal-notes"));
                            Q unit = dotted(typeValue(child(normal, "tuplet-type").getTextContent().trim()), children(normal, "tuplet-dot").size());
                            open.put(v, new Object[] { q, n, unit, Q.of(0, 1) });
                            seen.brackets.add(q + ":" + n + " " + child(normal, "tuplet-type").getTextContent().trim() + (children(normal, "tuplet-dot").isEmpty() ? "" : ".") + " m" + m.getAttribute("number"));
                        }
                    }
                    Object[] b = open.get(v);
                    if (tm != null) {
                        assertNotNull("a time modification outside a bracket in measure " + m.getAttribute("number"), b);
                        assertEquals((int)(Integer)b[0], intText(tm, "actual-notes"));
                        b[3] = ((Q)b[3]).add(written);
                    }
                    else {
                        assertNull("a note without time modification inside a bracket", b);
                    }
                    for (Element t : tuplets) {
                        if ("stop".equals(t.getAttribute("type"))) {
                            assertNotNull("a bracket stops that never started", b);
                            Q full = ((Q)b[2]).mul(Q.of((Integer)b[0], 1));
                            assertEquals("a bracket's notes fill q normal notes", full, b[3]);
                            open.remove(v);
                        }
                    }
                }
                assertTrue("a bracket left open at the end of measure " + m.getAttribute("number"), open.isEmpty());
                for (Map.Entry<String, Q> e : byDuration.entrySet()) {
                    assertEquals("voice " + e.getKey() + " of measure " + m.getAttribute("number") + " by duration", measure, e.getValue());
                    assertEquals("voice " + e.getKey() + " of measure " + m.getAttribute("number") + " by type", measure, byType.get(e.getKey()));
                }
            }
        }
        return seen;
    }

    static List<ScoreNote> readBack(String xml) throws IOException {
        MusicXMLReader r = new MusicXMLReader();
        List<ScoreNote> back = r.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(Collections.emptyList(), r.getWarnings());
        return back;
    }

    static List<ScoreNote> fr(long[][] v) {
        List<ScoreNote> out = new ArrayList<ScoreNote>();
        for (long[] p : v) {
            out.add(new ScoreNote((double)p[0] / p[1], 60, 80, (double)p[2] / p[3], 0));
        }
        return out;
    }

    static void validate(String xml) throws Exception {
        Validator v = MusicXMLTest.schemaValidator(MusicXMLTest.schemaPath());
        v.validate(new StreamSource(new StringReader(xml.replaceFirst("<!DOCTYPE[^>]*>\n", ""))));
    }

    /** Writes, validates, checks and reads back; the notes must read back exactly. */
    static Seen roundTrip(MusicXMLWriter w, List<ScoreNote> notes) throws Exception {
        String xml = w.toXML(notes);
        validate(xml);
        Seen s = check(xml);
        assertEquals(ScoreMidiTest.sorted(notes), ScoreMidiTest.sorted(readBack(xml)));
        return s;
    }

    // ---- the tests --------------------------------------------------------------------

    /** 5 (1 1 1): three notes of 5/3 quarter in 4/4, one 3:2 bracket of halves over measure 1. */
    public void testFiveInThreeGetsOneBracketOverTheMeasure() throws Exception {
        MusicXMLWriter w = new MusicXMLWriter();
        Seen s = roundTrip(w, fr(new long[][] { { 0, 1, 5, 3 }, { 5, 3, 5, 3 }, { 10, 3, 5, 3 } }));
        assertEquals(Collections.emptyList(), w.getWarnings());
        assertEquals(Arrays.asList("3:2 half m1"), s.brackets);
    }

    /** 4 (1 1 (1 1 1) 1): nested thirds become one 9:8 bracket of eighths, not nested brackets. */
    public void testNestedThirdsBecomeOneNineToEightBracket() throws Exception {
        MusicXMLWriter w = new MusicXMLWriter();
        Seen s = roundTrip(w, fr(new long[][] { { 0, 1, 4, 3 }, { 4, 3, 4, 9 }, { 16, 9, 4, 9 }, { 20, 9, 4, 9 }, { 8, 3, 4, 3 } }));
        assertEquals(Collections.emptyList(), w.getWarnings());
        assertEquals(Arrays.asList("9:8 eighth m1"), s.brackets);
        assertEquals(5, s.typed);
    }

    /** Five sixteenths in beat 1 and seven in beat 2 get a 5:4 and a 7:4 bracket of sixteenths. */
    public void testQuintupletAndSeptupletGetOneBeatBrackets() throws Exception {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        // onsets as num / den in doubles, so they compare equal with what the reader computes
        for (int i = 0; i < 5; i++) notes.add(new ScoreNote(i / 5.0, 62 + i, 80, 0.2, 0));
        for (int i = 0; i < 7; i++) notes.add(new ScoreNote((7 + i) / 7.0, 60 + i, 80, 1 / 7.0, 0));
        notes.add(new ScoreNote(2, 72, 80, 2, 0));
        MusicXMLWriter w = new MusicXMLWriter();
        Seen s = roundTrip(w, notes);
        assertEquals(Collections.emptyList(), w.getWarnings());
        assertEquals(Arrays.asList("5:4 16th m1", "7:4 16th m1"), s.brackets);
    }

    /** A triplet on beat 3 gets a bracket over that beat only; beats 1, 2 and 4 stay plain quarters. */
    public void testATripletOnBeatThreeKeepsItsBracketToTheBeat() throws Exception {
        List<ScoreNote> notes = Arrays.asList(
            new ScoreNote(0, 60, 80, 1, 0), new ScoreNote(1, 62, 80, 1, 0),
            new ScoreNote(2, 64, 80, 1.0 / 3, 0), new ScoreNote(2 + 1.0 / 3, 65, 80, 1.0 / 3, 0), new ScoreNote(2 + 2.0 / 3, 67, 80, 1.0 / 3, 0),
            new ScoreNote(3, 69, 80, 1, 0));
        MusicXMLWriter w = new MusicXMLWriter();
        String xml = w.toXML(notes);
        Seen s = check(xml);
        assertEquals(Arrays.asList("3:2 eighth m1"), s.brackets);
        assertEquals(3, xml.split("<type>quarter</type>", -1).length - 1);
        assertEquals(3, xml.split("<time-modification>", -1).length - 1);
    }

    /** Denominators 15, 27 and 35 (rhythm-tree values) get composite brackets and read back exactly. */
    public void testDenominatorsFifteenTwentySevenAndThirtyFive() throws Exception {
        MusicXMLWriter w = new MusicXMLWriter();
        Seen s = roundTrip(w, fr(new long[][] { { 0, 1, 4, 15 }, { 4, 15, 26, 15 }, { 2, 1, 4, 27 }, { 58, 27, 50, 27 }, { 4, 1, 4, 35 }, { 144, 35, 136, 35 } }));
        assertEquals(Collections.emptyList(), w.getWarnings());
        List<String> actual = new ArrayList<String>();
        for (String b : s.brackets) {
            actual.add(b.substring(0, b.indexOf(':')));
        }
        assertTrue(s.brackets.toString(), actual.contains("15"));
        assertTrue(s.brackets.toString(), actual.contains("27"));
        assertTrue(s.brackets.toString(), actual.contains("35"));
    }

    /** Figure 2 of arXiv 2512.04090 in proportional timing: every note typed, every measure adds up, exact read-back. */
    public void testFigureTwoProportionalIsExact() throws Exception {
        MusicXMLWriter w = new MusicXMLWriter();
        Seen s = roundTrip(w, fr(new long[][] { { 0, 1, 38, 15 }, { 38, 15, 19, 27 }, { 437, 135, 19, 54 }, { 323, 90, 19, 18 }, { 209, 45, 19, 18 }, { 57, 10, 57, 35 }, { 513, 70, 76, 35 } }));
        assertEquals(Collections.emptyList(), w.getWarnings());
        assertFalse(s.brackets.isEmpty());
    }

    /** Quintuplet quarters across a 3/4 measure. */
    public void testAQuintupletInThreeFour() throws Exception {
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (int i = 0; i < 5; i++) notes.add(new ScoreNote(3 * i / 5.0, 60 + i, 80, 0.6, 0));
        MusicXMLWriter w = new MusicXMLWriter();
        w.setTime(3, 4);
        Seen s = roundTrip(w, notes);
        assertEquals(Collections.emptyList(), w.getWarnings());
        assertEquals(1, s.brackets.size());
        assertTrue(s.brackets.toString(), s.brackets.get(0).startsWith("5:4 "));
    }

    /** In a chord only the first note carries the tuplet element; all carry the time modification. */
    public void testAChordCarriesTheBracketOnItsFirstNote() throws Exception {
        List<ScoreNote> notes = Arrays.asList(
            new ScoreNote(0, 60, 80, 2.0 / 3, 0), new ScoreNote(0, 64, 80, 2.0 / 3, 0),
            new ScoreNote(2.0 / 3, 62, 80, 2.0 / 3, 0),
            new ScoreNote(4.0 / 3, 64, 80, 2.0 / 3, 0), new ScoreNote(4.0 / 3, 67, 80, 2.0 / 3, 0));
        MusicXMLWriter w = new MusicXMLWriter();
        String xml = w.toXML(notes);
        check(xml);
        assertEquals(5, xml.split("<time-modification>", -1).length - 1);
        assertEquals(1, xml.split("<tuplet type=\"start\"", -1).length - 1);
        assertEquals(1, xml.split("<tuplet type=\"stop\"", -1).length - 1);
        assertEquals(ScoreMidiTest.sorted(notes), ScoreMidiTest.sorted(readBack(xml)));
    }

    /**
     * Gaps inside a bracket in a second MusicXML voice become hidden rests, so
     * the bracket adds up; the rest of that voice's measure is a forward.
     */
    public void testAGapInsideABracketInASecondVoiceIsAHiddenRest() throws Exception {
        List<ScoreNote> notes = Arrays.asList(
            new ScoreNote(0, 72, 80, 1, 0), new ScoreNote(1, 74, 80, 1, 0), new ScoreNote(2, 76, 80, 1, 0), new ScoreNote(3, 77, 80, 1, 0),
            new ScoreNote(1.0 / 3, 60, 80, 1.0 / 3, 0));
        MusicXMLWriter w = new MusicXMLWriter();
        String xml = w.toXML(notes);
        assertEquals(1, xml.split("<forward>", -1).length - 1);
        Seen s = roundTrip(w, notes);
        assertEquals(2, s.hiddenRests);
        assertEquals(Arrays.asList("3:2 eighth m1"), s.brackets);
    }

    /** Triplets that cross the barline are tied, with a bracket on each side. */
    public void testTripletsAcrossTheBarlineGetABracketInEachMeasure() throws Exception {
        MusicXMLWriter w = new MusicXMLWriter();
        Seen s = roundTrip(w, Arrays.asList(new ScoreNote(3, 60, 80, 2.0 / 3, 0), new ScoreNote(3 + 2.0 / 3, 62, 80, 2.0 / 3, 0), new ScoreNote(4 + 1.0 / 3, 64, 80, 2.0 / 3, 0)));
        assertEquals(2, s.brackets.size());
        assertTrue(s.brackets.get(0).endsWith("m1"));
        assertTrue(s.brackets.get(1).endsWith("m2"));
    }

    /** A grace note inside a triplet stays a grace note with no time modification. */
    public void testAGraceNoteInsideATriplet() throws Exception {
        List<ScoreNote> notes = Arrays.asList(new ScoreNote(0, 60, 80, 1.0 / 3, 0), new ScoreNote(1.0 / 3, 74, 80, 0, 0), new ScoreNote(1.0 / 3, 62, 80, 1.0 / 3, 0), new ScoreNote(2.0 / 3, 64, 80, 1.0 / 3, 0));
        MusicXMLWriter w = new MusicXMLWriter();
        Seen s = roundTrip(w, notes);
        assertEquals(Arrays.asList("3:2 eighth m1"), s.brackets);
    }

    /**
     * An onset of 1/521 quarter needs a 521:512 bracket, more than the writer
     * allows: the measure is rounded to 1024ths with a warning that keeps the
     * old warning's opening words, and every note still has a type.
     */
    public void testBeyondTheLimitTheMeasureIsRoundedAndReported() throws Exception {
        MusicXMLWriter w = new MusicXMLWriter();
        String xml = w.toXML(Arrays.asList(new ScoreNote(1.0 / 521, 60, 80, 1, 0)));
        validate(xml);
        Seen s = check(xml);
        assertTrue(s.brackets.isEmpty());
        assertEquals(w.getWarnings().toString(), 1, w.getWarnings().size());
        String warning = w.getWarnings().get(0);
        assertTrue(warning, warning.startsWith("2 notes or rests have lengths that no plain, dotted or triplet note value expresses"));
        assertTrue(warning, warning.contains("rounded to the nearest 1024th note"));
        assertTrue(warning, warning.contains("measure 1"));
        assertEquals(0, intText((Element)dom(xml).getElementsByTagName("attributes").item(0), "divisions") % 256);
        List<ScoreNote> back = readBack(xml);
        assertEquals(1, back.size());
        // the rounded onset is within half a 1024th note of 1/521
        assertTrue(Math.abs(back.get(0).onset - 1.0 / 521) <= 1.0 / 512 + 1e-12);
        assertEquals(1.0, back.get(0).duration, 1e-12);
    }

    /**
     * An onset of 1/257 quarter would fit a 257:256 bracket of 1024th notes
     * over beat 1, but 257 is above the limit of 255: the measure is rounded
     * and reported instead.
     */
    public void testABracketAboveTheLimitIsNotWritten() throws Exception {
        MusicXMLWriter w = new MusicXMLWriter();
        String xml = w.toXML(Arrays.asList(new ScoreNote(1.0 / 257, 60, 80, 1, 0)));
        Seen s = check(xml);
        assertTrue(s.brackets.toString(), s.brackets.isEmpty());
        assertEquals(w.getWarnings().toString(), 1, w.getWarnings().size());
        assertTrue(w.getWarnings().get(0), w.getWarnings().get(0).contains("rounded to the nearest 1024th note"));
    }

    /** Measures with no off-grid boundary are written byte for byte as before (sha256 of the a9eeeab writer's output). */
    public void testFilesWithoutTupletsAreUnchanged() throws Exception {
        List<ScoreNote> satellites = MusicXMLNotationTest.satellites();
        MusicXMLWriter waltz = new MusicXMLWriter();
        waltz.setTime(3, 4);
        assertEquals("ad3f7e8bafc4497b65c1582ae42f0ec86f5dfb75f8635457c25b897608c10015", sha256(new MusicXMLWriter().toXML(satellites)));
        assertEquals("72a5a4a05143f63757c67e8d8a28e4c30b7b8111bc6ecc47ac712d0ef6eb0d82", sha256(waltz.toXML(satellites)));
        assertEquals("0c1642f7f40721c9590476c84568d2c7bfe4d8a3120d9ff074017116b0dcc568", sha256(MusicXMLPartsTest.writerWithParts().toXML(satellites)));
        assertEquals("16f5a32b73d6e13c008c2f808d9897b69cd337a5cc11586736651148afc1b255", sha256(new MusicXMLWriter().toXML(MusicXMLDynamicsTest.LINE)));
        assertEquals("8a7809635a97c1d42eb0909ab7797838bd494ca8aa0eb0e047ece26ebcb730d5", sha256(MusicXMLPartsTest.writerWithParts().toXML(MusicXMLPartsTest.NOTES)));
        assertEquals("c7afedb4bacc61895d5a4a3df7ee753711bfb48970811a81989e7e637764df1d", sha256(new MusicXMLWriter().toXML(Arrays.asList(new ScoreNote(0.25, 72, 90, 1.25, 0), new ScoreNote(8, 72, 90, 0.5, 0)))));
    }

    /** The writer's other outputs pass the structural check too (satellites, the rich score). */
    public void testExistingScoresPassTheCheck() throws Exception {
        check(new MusicXMLWriter().toXML(MusicXMLNotationTest.satellites()));
        Seen s = check(new MusicXMLWriter().toXML(MusicXMLTest.richScore()));
        assertEquals(Arrays.asList("3:2 eighth m1"), s.brackets);
    }

    static String sha256(String s) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        for (byte x : h) b.append(String.format("%02x", x));
        return b.toString();
    }
}
