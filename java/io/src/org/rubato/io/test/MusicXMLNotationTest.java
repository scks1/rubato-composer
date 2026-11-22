package org.rubato.io.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.io.musicxml.MusicXMLReader;
import org.rubato.io.musicxml.MusicXMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Measure filling, note values, clefs and tempo in MusicXML, from the
 * "Satellites" test piece of 2026-11-21: its MusicXML read back with notes
 * moved, and its notation had untyped notes and one clef for a part from
 * C2 to B-flat 6.
 */
public class MusicXMLNotationTest extends TestCase {

	private static List<ScoreNote> roundTrip(MusicXMLWriter writer, MusicXMLReader reader, List<ScoreNote> notes) throws IOException {
		String xml = writer.toXML(notes);
		return reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	/** A second MusicXML voice that ends before the barline used to move every later note of the part back by the gap. */
	public void testAVoiceThatStopsBeforeTheBarlineMovesNothingAfterIt() throws IOException {
		List<ScoreNote> notes = Arrays.asList(
			new ScoreNote(0, 60, 90, 4, 0),
			new ScoreNote(1, 64, 90, 1, 0),
			new ScoreNote(4, 67, 90, 1, 0));
		MusicXMLReader reader = new MusicXMLReader();
		assertEquals(ScoreMidiTest.sorted(notes), ScoreMidiTest.sorted(roundTrip(new MusicXMLWriter(), reader, notes)));
		assertEquals(Collections.emptyList(), reader.getWarnings());
	}

	/** Files from other programs may leave a voice short of the barline with no forward element. */
	public void testTheReaderStartsEachMeasureAtTheTimeSignature() throws IOException {
		String xml = "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>A</part-name></score-part></part-list>"
			+ "<part id=\"P1\">"
			//a pickup of one quarter in 3/4
			+ "<measure number=\"0\" implicit=\"yes\"><attributes><divisions>2</divisions><time><beats>3</beats><beat-type>4</beat-type></time></attributes>"
			+ "<note><pitch><step>G</step><octave>4</octave></pitch><duration>2</duration><voice>1</voice><type>quarter</type></note></measure>"
			//voice 2 stops after one quarter and nothing fills the rest
			+ "<measure number=\"1\"><note><pitch><step>C</step><octave>5</octave></pitch><duration>6</duration><voice>1</voice><type>half</type><dot/></note>"
			+ "<backup><duration>6</duration></backup>"
			+ "<note><pitch><step>E</step><octave>4</octave></pitch><duration>2</duration><voice>2</voice><type>quarter</type></note></measure>"
			//a composite 2+3/8 time signature, then a note on its first beat
			+ "<measure number=\"2\"><attributes><time><beats>2+3</beats><beat-type>8</beat-type></time></attributes>"
			+ "<note><pitch><step>D</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>eighth</type></note></measure>"
			+ "<measure number=\"3\"><note><pitch><step>F</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice><type>eighth</type></note></measure>"
			+ "</part></score-partwise>";
		MusicXMLReader reader = new MusicXMLReader();
		List<ScoreNote> notes = reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		List<ScoreNote> expected = Arrays.asList(
			new ScoreNote(0, 67, 90, 1, 0),
			new ScoreNote(1, 72, 90, 3, 0),
			new ScoreNote(1, 64, 90, 1, 0),
			new ScoreNote(4, 74, 90, 0.5, 0),
			//5/8 is two and a half quarters
			new ScoreNote(6.5, 77, 90, 0.5, 0));
		assertEquals(ScoreMidiTest.sorted(expected), ScoreMidiTest.sorted(notes));
		assertEquals(Collections.emptyList(), reader.getWarnings());
	}

	/** The 238 notes of the Satellites test piece (onset, pitch, loudness, duration, voice). */
	static List<ScoreNote> satellites() throws IOException {
		File csv = null;
		for (String candidate : new String[] { "src/org/rubato/io/test/satellites-notes.csv", "java/io/src/org/rubato/io/test/satellites-notes.csv" }) {
			if (new File(candidate).isFile()) {
				csv = new File(candidate);
				break;
			}
		}
		assertNotNull("satellites-notes.csv not found beside this test", csv);
		List<ScoreNote> notes = new ArrayList<ScoreNote>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(csv), StandardCharsets.UTF_8))) {
			String line = r.readLine();
			assertEquals("onset,pitch,loudness,duration,voice", line);
			while ((line = r.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				String[] f = line.split(",");
				notes.add(new ScoreNote(Double.parseDouble(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]), Double.parseDouble(f[3]), Integer.parseInt(f[4])));
			}
		}
		return notes;
	}

	public void testSatellitesRoundTripsExactly() throws IOException {
		List<ScoreNote> notes = satellites();
		assertEquals(238, notes.size());
		MusicXMLWriter writer = new MusicXMLWriter();
		MusicXMLReader reader = new MusicXMLReader();
		List<ScoreNote> back = roundTrip(writer, reader, notes);
		assertEquals(Collections.emptyList(), writer.getWarnings());
		assertEquals(Collections.emptyList(), reader.getWarnings());
		assertEquals(ScoreMidiTest.sorted(notes), ScoreMidiTest.sorted(back));
	}

	private static Document dom(String xml) throws Exception {
		DocumentBuilderFactory f = DocumentBuilderFactory.newDefaultInstance();
		f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
	}

	private static String text(Element e, String tag) {
		NodeList l = e.getElementsByTagName(tag);
		return l.getLength() == 0 ? null : l.item(0).getTextContent().trim();
	}

	/** Every note and rest carries a type, except measure rests, which MusicXML lets go without one. */
	public void testEveryNoteHasAType() throws Exception {
		for (List<ScoreNote> notes : Arrays.asList(satellites(), MusicXMLTest.richScore())) {
			Document d = dom(new MusicXMLWriter().toXML(notes));
			NodeList all = d.getElementsByTagName("note");
			int measureRests = 0;
			for (int i = 0; i < all.getLength(); i++) {
				Element n = (Element)all.item(i);
				NodeList rest = n.getElementsByTagName("rest");
				if (rest.getLength() > 0 && "yes".equals(((Element)rest.item(0)).getAttribute("measure"))) {
					measureRests++;
					continue;
				}
				assertNotNull("note " + i + " has no type", text(n, "type"));
			}
			assertTrue(all.getLength() > measureRests);
		}
	}

	/** A note of 1.25 quarters at beat 0.25 is a tied quarter and sixteenth, and rests follow the beats. */
	public void testSplitPiecesAreTiedAndAddUp() throws Exception {
		List<ScoreNote> notes = Arrays.asList(
			new ScoreNote(0.25, 72, 90, 1.25, 0),
			new ScoreNote(8, 72, 90, 0.5, 0));
		String xml = new MusicXMLWriter().toXML(notes);
		Document d = dom(xml);
		assertEquals("4", text(d.getDocumentElement(), "divisions"));
		NodeList all = d.getElementsByTagName("note");
		List<String> pitched = new ArrayList<String>();
		List<String> rests = new ArrayList<String>();
		for (int i = 0; i < all.getLength(); i++) {
			Element n = (Element)all.item(i);
			String type = text(n, "type") + (n.getElementsByTagName("dot").getLength() > 0 ? "." : "");
			String piece = text(n, "duration") + ":" + type;
			if (n.getElementsByTagName("rest").getLength() > 0) {
				rests.add(piece);
			}
			else {
				NodeList ties = n.getElementsByTagName("tie");
				StringBuilder t = new StringBuilder();
				for (int k = 0; k < ties.getLength(); k++) {
					t.append(k > 0 ? "+" : "").append(((Element)ties.item(k)).getAttribute("type"));
				}
				pitched.add(piece + ":" + t);
			}
		}
		assertEquals(Arrays.asList("4:quarter:start", "1:16th:stop", "2:eighth:"), pitched);
		//measure 1: a sixteenth rest before the note, which ends at 1.5, then an eighth to the beat and a half;
		//measure 2 is a measure rest; measure 3 has an eighth, a quarter and a half after the eighth note
		assertEquals(Arrays.asList("1:16th", "2:eighth", "8:half", "16:null", "2:eighth", "4:quarter", "8:half"), rests);
		MusicXMLReader reader = new MusicXMLReader();
		assertEquals(ScoreMidiTest.sorted(notes), ScoreMidiTest.sorted(reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))));
	}

	/** Triplets get triplet note values with a 3:2 time modification. */
	public void testTripletsGetTimeModification() throws Exception {
		List<ScoreNote> notes = Arrays.asList(
			new ScoreNote(0, 60, 90, 2.0 / 3, 0),
			new ScoreNote(2.0 / 3, 62, 90, 2.0 / 3, 0),
			new ScoreNote(4.0 / 3, 64, 90, 2.0 / 3, 0));
		MusicXMLWriter writer = new MusicXMLWriter();
		Document d = dom(writer.toXML(notes));
		assertEquals(Collections.emptyList(), writer.getWarnings());
		NodeList all = d.getElementsByTagName("note");
		for (int i = 0; i < 3; i++) {
			Element n = (Element)all.item(i);
			assertEquals("quarter", text(n, "type"));
			assertEquals("3", text(n, "actual-notes"));
			assertEquals("2", text(n, "normal-notes"));
		}
	}

	/** A part from C2 to B-flat 6 goes on a grand staff and reads back note for note. */
	public void testAWideRangePartGoesOnAGrandStaff() throws Exception {
		List<ScoreNote> notes = Arrays.asList(
			new ScoreNote(0, 36, 90, 1, 2),
			new ScoreNote(0, 59, 90, 1, 2),
			new ScoreNote(0, 60, 90, 1, 2),
			new ScoreNote(0, 94, 90, 1, 2),
			new ScoreNote(1, 48, 90, 3.5, 2),
			new ScoreNote(2, 72, 90, 2.5, 2),
			new ScoreNote(2.5, 76, 90, 0.5, 2));
		MusicXMLWriter writer = new MusicXMLWriter();
		String xml = writer.toXML(notes);
		Document d = dom(xml);
		assertEquals("2", text(d.getDocumentElement(), "staves"));
		NodeList clefs = d.getElementsByTagName("clef");
		assertEquals(2, clefs.getLength());
		assertEquals("G", text((Element)clefs.item(0), "sign"));
		assertEquals("F", text((Element)clefs.item(1), "sign"));
		NodeList all = d.getElementsByTagName("note");
		for (int i = 0; i < all.getLength(); i++) {
			Element n = (Element)all.item(i);
			String staff = text(n, "staff");
			assertNotNull("note " + i + " has no staff", staff);
			String octave = text(n, "octave");
			if (octave != null) {
				//octave 4 starts at middle C
				assertEquals("note " + i, Integer.parseInt(octave) >= 4 ? "1" : "2", staff);
			}
		}
		MusicXMLReader reader = new MusicXMLReader();
		assertEquals(ScoreMidiTest.sorted(notes), ScoreMidiTest.sorted(reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))));
		assertEquals(Collections.emptyList(), reader.getWarnings());
		//narrow parts keep one staff
		assertFalse(new MusicXMLWriter().toXML(Arrays.asList(new ScoreNote(0, 53, 90, 1, 0), new ScoreNote(1, 84, 90, 1, 0))).contains("<staves>"));
		assertTrue(new MusicXMLWriter().toXML(Arrays.asList(new ScoreNote(0, 30, 90, 1, 0), new ScoreNote(1, 67, 90, 1, 0))).contains("<sign>F</sign>"));
	}

	public void testTempoRoundTrips() throws IOException {
		List<ScoreNote> notes = Arrays.asList(new ScoreNote(0, 60, 90, 1, 0));
		for (int us : new int[] { 500000, 400000, 437500, 1, 0xFFFFFF }) {
			MusicXMLWriter writer = new MusicXMLWriter();
			writer.setTempo(us);
			MusicXMLReader reader = new MusicXMLReader();
			roundTrip(writer, reader, notes);
			assertTrue(reader.hasTempo());
			assertEquals(us, reader.getTempo());
		}
		MusicXMLWriter writer = new MusicXMLWriter();
		writer.setTempo(400000);
		String xml = writer.toXML(notes);
		assertTrue(xml, xml.contains("<per-minute>150</per-minute>"));
		assertTrue(xml, xml.contains("<sound tempo=\"150\"/>"));
		try {
			writer.setTempo(0);
			fail();
		}
		catch (IllegalArgumentException e) {
			//expected, as in ScoreMidi
		}
		//a file without a tempo
		MusicXMLReader reader = new MusicXMLReader();
		reader.read(new ByteArrayInputStream(("<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>A</part-name></score-part></part-list>"
			+ "<part id=\"P1\"><measure number=\"1\"/></part></score-partwise>").getBytes(StandardCharsets.UTF_8)));
		assertFalse(reader.hasTempo());
		assertEquals(MusicXMLWriter.DEFAULT_TEMPO, reader.getTempo());
	}

	public void testOutputsAreValidAgainstTheMusicXML40Schema() throws Exception {
		Validator v = MusicXMLTest.schemaValidator(MusicXMLTest.schemaPath());
		MusicXMLWriter tempo = new MusicXMLWriter();
		tempo.setTempo(437500);
		MusicXMLWriter waltz = new MusicXMLWriter();
		waltz.setTime(3, 4);
		List<String> docs = Arrays.asList(
			new MusicXMLWriter().toXML(satellites()),
			tempo.toXML(satellites()),
			waltz.toXML(satellites()),
			new MusicXMLWriter().toXML(Arrays.asList(new ScoreNote(0, 36, 90, 1, 2), new ScoreNote(0.25, 94, 90, 5.5, 2))),
			new MusicXMLWriter().toXML(Arrays.asList(new ScoreNote(0, 60, 90, 2.0 / 3, 0), new ScoreNote(0.25, 72, 90, 1.25, 1))));
		for (String xml : docs) {
			v.validate(new StreamSource(new StringReader(xml.replaceFirst("<!DOCTYPE[^>]*>\n", ""))));
		}
		System.out.println("MusicXMLNotationTest: " + docs.size() + " documents valid against " + MusicXMLTest.schemaPath());
	}
}
