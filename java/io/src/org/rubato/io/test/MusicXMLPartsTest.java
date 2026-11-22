package org.rubato.io.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import junit.framework.TestCase;

import org.rubato.io.PartInfo;
import org.rubato.io.ScoreNote;
import org.rubato.io.musicxml.MusicXMLReader;
import org.rubato.io.musicxml.MusicXMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Part names and MIDI instruments in MusicXML, from the "Convergences" test
 * piece of 2026-11-22, whose parts could only be called "Voice n".
 */
public class MusicXMLPartsTest extends TestCase {

	static final List<ScoreNote> NOTES = Arrays.asList(
		new ScoreNote(0, 72, 90, 1, 0), new ScoreNote(1, 64, 70, 1, 1),
		new ScoreNote(0, 48, 100, 2, 2), new ScoreNote(2, 40, 60, 1, 20));

	static MusicXMLWriter writerWithParts() {
		MusicXMLWriter w = new MusicXMLWriter();
		w.setPart(0, PartInfo.of("Flute", 73, 84).withVolume(100));
		w.setPart(1, PartInfo.of("Oboe & \"Cor\" <anglais>", 68));
		w.setPart(2, new PartInfo(null, PartInfo.UNSET, 0, PartInfo.UNSET));
		w.setPart(20, PartInfo.of("Contrabass", 43, 127));
		w.setPart(9, PartInfo.of("Unused", 1)); // no notes in voice 9, so no part
		return w;
	}

	public void testNamesAndInstrumentsRoundTrip() throws IOException {
		MusicXMLWriter w = writerWithParts();
		String xml = w.toXML(NOTES);
		MusicXMLReader r = new MusicXMLReader();
		List<ScoreNote> back = r.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(ScoreMidiTest.sorted(NOTES), ScoreMidiTest.sorted(back));
		assertEquals(Collections.emptyList(), r.getWarnings());
		Map<Integer, PartInfo> parts = r.getParts();
		assertEquals(Arrays.asList(0, 1, 2, 20), new java.util.ArrayList<Integer>(parts.keySet()));
		assertEquals(new PartInfo("Flute", 73, 84, 100), parts.get(0));
		assertEquals(PartInfo.of("Oboe & \"Cor\" <anglais>", 68), parts.get(1));
		// a part with no name keeps the writer's default name
		assertEquals(new PartInfo("Voice 2", PartInfo.UNSET, 0, PartInfo.UNSET), parts.get(2));
		assertEquals(PartInfo.of("Contrabass", 43, 127), parts.get(20));
		assertEquals("Flute", r.getPartNames().get(0));
	}

	public void testTheMidiInstrumentUsesMusicXMLUnits() throws Exception {
		Document d = dom(writerWithParts().toXML(NOTES));
		Element flute = scorePart(d, "V0");
		assertEquals("Flute", text(flute, "part-name"));
		assertEquals("Flute", text(flute, "instrument-name"));
		assertEquals("1", text(flute, "midi-channel"));
		assertEquals("74", text(flute, "midi-program")); // MusicXML counts programs from 1
		assertEquals("78.7402", text(flute, "volume"));
		assertEquals("28.125", text(flute, "pan"));
		Element bass = scorePart(d, "V20");
		assertNull("voices above 15 have no channel", text(bass, "midi-channel"));
		assertEquals("44", text(bass, "midi-program"));
		assertEquals("88.59375", text(bass, "pan"));
		assertNull(scorePart(d, "V9"));
		// a voice with no settings is written as before
		Document plain = dom(new MusicXMLWriter().toXML(NOTES));
		Element v1 = scorePart(plain, "V1");
		assertEquals("Voice 1", text(v1, "part-name"));
		assertEquals("2", text(v1, "midi-channel"));
		assertNull(text(v1, "midi-program"));
		assertNull(text(v1, "pan"));
		assertNull(scorePart(plain, "V20").getElementsByTagName("midi-instrument").item(0));
	}

	public void testProgramsFromOtherFilesAreRead() throws IOException {
		String xml = "<score-partwise version=\"4.0\"><part-list>"
			+ "<score-part id=\"P1\"><part-name>Violin</part-name><score-instrument id=\"P1-I1\"><instrument-name>Violin</instrument-name></score-instrument>"
			+ "<midi-instrument id=\"P1-I1\"><midi-channel>3</midi-channel><midi-program>41</midi-program><volume>80</volume><pan>-45</pan></midi-instrument></score-part>"
			+ "<score-part id=\"P2\"><midi-instrument id=\"P2-I1\"><midi-program>200</midi-program></midi-instrument></score-part>"
			+ "</part-list><part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions></attributes>"
			+ "<note><pitch><step>A</step><octave>4</octave></pitch><duration>1</duration></note></measure></part>"
			+ "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>1</divisions></attributes>"
			+ "<note><pitch><step>A</step><octave>3</octave></pitch><duration>1</duration></note></measure></part></score-partwise>";
		MusicXMLReader r = new MusicXMLReader();
		r.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		// P1 reads as voice 2 from its channel; P2 as its index, 1
		assertEquals(new PartInfo("Violin", 40, 32, 102), r.getParts().get(2));
		assertEquals(new PartInfo(null, PartInfo.UNSET, PartInfo.UNSET, PartInfo.UNSET), r.getParts().get(1));
		assertEquals(1, r.getWarnings().size());
		assertTrue(r.getWarnings().get(0), r.getWarnings().get(0).contains("midi-program 200"));
	}

	public void testOutputIsValidAgainstTheMusicXML40Schema() throws Exception {
		Validator v = MusicXMLTest.schemaValidator(MusicXMLTest.schemaPath());
		MusicXMLWriter w = writerWithParts();
		for (String xml : Arrays.asList(w.toXML(NOTES), w.toXML(MusicXMLNotationTest.satellites()))) {
			v.validate(new StreamSource(new StringReader(xml.replaceFirst("<!DOCTYPE[^>]*>\n", ""))));
		}
	}

	static Document dom(String xml) throws Exception {
		DocumentBuilderFactory f = DocumentBuilderFactory.newDefaultInstance();
		f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}

	private static Element scorePart(Document d, String id) {
		NodeList l = d.getElementsByTagName("score-part");
		for (int i = 0; i < l.getLength(); i++) {
			if (id.equals(((Element)l.item(i)).getAttribute("id"))) {
				return (Element)l.item(i);
			}
		}
		return null;
	}

	static String text(Element e, String tag) {
		NodeList l = e.getElementsByTagName(tag);
		return l.getLength() == 0 ? null : l.item(0).getTextContent().trim();
	}
}
