package org.rubato.io.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

/**
 * Dynamic marks in MusicXML, from the "Convergences" test piece of
 * 2026-11-22: loudness went out only as the note attribute, so engravers
 * showed no dynamics. Marks follow each measure's mean velocity.
 */
public class MusicXMLDynamicsTest extends TestCase {

	public void testBandsMeetHalfwayBetweenTheMarks() {
		int[][] cases = { { 0, 0 }, { 24, 0 }, { 25, 1 }, { 40, 1 }, { 41, 2 }, { 56, 2 }, { 57, 3 }, { 71, 3 },
			{ 72, 4 }, { 87, 4 }, { 88, 5 }, { 103, 5 }, { 104, 6 }, { 118, 6 }, { 119, 7 }, { 127, 7 }, { 200, 7 } };
		String[] marks = { "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff" };
		for (int[] c : cases) {
			assertEquals("velocity " + c[0], marks[c[1]], MusicXMLWriter.dynamicMark(c[0]));
		}
	}

	/**
	 * One voice over four measures with mean velocities 48 (p), 89 (f, though
	 * its notes run from 70 to 100), 89 (f again, no mark) and 30 (pp, after a
	 * note tied in from the measure before).
	 */
	static final List<ScoreNote> LINE = Arrays.asList(
		new ScoreNote(0, 60, 50, 1, 0), new ScoreNote(1, 62, 45, 1, 0), new ScoreNote(2, 64, 50, 1, 0), new ScoreNote(3, 65, 45, 1, 0),
		new ScoreNote(4, 67, 100, 1, 0), new ScoreNote(5, 69, 96, 1, 0), new ScoreNote(6, 71, 70, 2, 0),
		new ScoreNote(8, 72, 98, 1, 0), new ScoreNote(9, 74, 80, 4, 0),
		new ScoreNote(13, 76, 30, 3, 0));

	public void testAMarkIsWrittenWhenAMeasuresBandChangesInAVoice() throws Exception {
		String xml = new MusicXMLWriter().toXML(LINE);
		Document d = MusicXMLPartsTest.dom(xml);
		assertEquals(Arrays.asList("p@60", "f@67", "pp@76"), marks(d));
		NodeList sounds = d.getElementsByTagName("sound");
		List<String> dyn = new ArrayList<String>();
		for (int i = 0; i < sounds.getLength(); i++) {
			Element s = (Element)sounds.item(i);
			if (s.hasAttribute("dynamics")) dyn.add(s.getAttribute("dynamics"));
		}
		// the marks' velocities 49, 96 and 33 as percentages of 90
		assertEquals(Arrays.asList("54.4444", "106.6667", "36.6667"), dyn);
		// every note keeps its exact loudness
		MusicXMLReader r = new MusicXMLReader();
		List<ScoreNote> back = r.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(ScoreMidiTest.sorted(LINE), ScoreMidiTest.sorted(back));
		assertEquals(Collections.emptyList(), r.getWarnings());
	}

	public void testEachVoiceAndStaffKeepsItsOwnMarks() throws Exception {
		List<ScoreNote> notes = Arrays.asList(
			// the first MusicXML voice: a chord of 64 and 110, then 112: mean 95, f; then f again in measure 2, no mark
			new ScoreNote(0, 60, 64, 1, 0), new ScoreNote(0, 67, 110, 1, 0), new ScoreNote(1, 62, 112, 1, 0),
			new ScoreNote(4, 64, 90, 1, 0),
			// an overlapping second MusicXML voice in pp
			new ScoreNote(0.5, 72, 30, 2, 0),
			// a grand-staff part: the lower staff gets its own mark
			new ScoreNote(0, 40, 80, 1, 1), new ScoreNote(0, 80, 80, 1, 1));
		Document d = MusicXMLPartsTest.dom(new MusicXMLWriter().toXML(notes));
		assertEquals(Arrays.asList("f@60", "pp@72", "mf@80", "mf@40"), marks(d));
	}

	public void testMarksCanBeTurnedOffAndTheOutputIsValid() throws Exception {
		MusicXMLWriter off = new MusicXMLWriter();
		off.setDynamicMarks(false);
		assertEquals(0, MusicXMLPartsTest.dom(off.toXML(LINE)).getElementsByTagName("dynamics").getLength());
		Validator v = MusicXMLTest.schemaValidator(MusicXMLTest.schemaPath());
		List<ScoreNote> satellites = MusicXMLNotationTest.satellites();
		MusicXMLWriter w = new MusicXMLWriter();
		for (String xml : Arrays.asList(w.toXML(LINE), w.toXML(satellites), MusicXMLPartsTest.writerWithParts().toXML(satellites))) {
			v.validate(new StreamSource(new StringReader(xml.replaceFirst("<!DOCTYPE[^>]*>\n", ""))));
		}
		// Satellites still round-trips exactly with the marks in
		MusicXMLReader r = new MusicXMLReader();
		String xml = w.toXML(satellites);
		assertTrue(MusicXMLPartsTest.dom(xml).getElementsByTagName("dynamics").getLength() > 0);
		assertEquals(ScoreMidiTest.sorted(satellites), ScoreMidiTest.sorted(r.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))));
		assertEquals(Collections.emptyList(), r.getWarnings());
	}

	/** Each dynamics mark with the MIDI key of the note that follows its direction. */
	private static List<String> marks(Document d) {
		List<String> out = new ArrayList<String>();
		NodeList dirs = d.getElementsByTagName("dynamics");
		for (int i = 0; i < dirs.getLength(); i++) {
			Element dyn = (Element)dirs.item(i);
			String mark = null;
			for (Node c = dyn.getFirstChild(); c != null; c = c.getNextSibling()) {
				if (c instanceof Element) mark = ((Element)c).getTagName();
			}
			Node n = dyn.getParentNode().getParentNode().getNextSibling();
			while (!(n instanceof Element) || !"note".equals(((Element)n).getTagName())) {
				n = n.getNextSibling();
			}
			out.add(mark + "@" + key((Element)n));
		}
		return out;
	}

	private static int key(Element note) {
		String step = MusicXMLPartsTest.text(note, "step");
		String alter = MusicXMLPartsTest.text(note, "alter");
		int octave = Integer.parseInt(MusicXMLPartsTest.text(note, "octave"));
		int pc = "C D EF G A B".indexOf(step);
		return (octave + 1) * 12 + pc + (alter == null ? 0 : Integer.parseInt(alter));
	}
}
