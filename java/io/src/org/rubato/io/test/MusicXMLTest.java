package org.rubato.io.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.io.musicxml.MusicXMLReader;
import org.rubato.io.musicxml.MusicXMLWriter;
import org.rubato.math.arith.Rational;
import org.rubato.math.yoneda.Denotator;

public class MusicXMLTest extends TestCase {

	/** A score that uses every part of the mapping. */
	static List<ScoreNote> richScore() {
		List<ScoreNote> notes = new ArrayList<ScoreNote>();
		//a C major chord, then a melody
		notes.add(new ScoreNote(0, 60, 80, 2, 0));
		notes.add(new ScoreNote(0, 64, 80, 2, 0));
		notes.add(new ScoreNote(0, 67, 80, 2, 0));
		notes.add(new ScoreNote(2, 72, 100, 0.5, 0));
		notes.add(new ScoreNote(2.5, 71, 101, 0.25, 0));
		//a triplet
		notes.add(new ScoreNote(3, 69, 64, 1.0 / 3, 0));
		notes.add(new ScoreNote(3 + 1.0 / 3, 67, 65, 1.0 / 3, 0));
		notes.add(new ScoreNote(3 + 2.0 / 3, 65, 66, 1.0 / 3, 0));
		//a note across two barlines and one overlapping it (a second MusicXML voice)
		notes.add(new ScoreNote(3.5, 55, 50, 6, 0));
		notes.add(new ScoreNote(5, 59, 51, 1, 0));
		//a grace note and a note of loudness 0, a quarter tone and a pitch in octave 0
		notes.add(new ScoreNote(8, 74, 90, 0, 0));
		notes.add(new ScoreNote(8, 76, 0, 1, 0));
		notes.add(new ScoreNote(9, new Rational(121, 2), 127, 1, 0));
		notes.add(new ScoreNote(10, 12, 1, 1.5, 0));
		//other voices: a bass part, a voice beyond the 16 MIDI channels and a negative one
		notes.add(new ScoreNote(0, 36, 70, 4, 1));
		notes.add(new ScoreNote(4, 43, 70, 4, 1));
		notes.add(new ScoreNote(1, 84, 40, 0.125, 20));
		notes.add(new ScoreNote(12.75, 61, 30, 0.75, -2));
		return notes;
	}

	public void testAScoreRoundTripsThroughMusicXML() throws IOException {
		List<ScoreNote> notes = richScore();
		Denotator score = ScoreNote.toScore(notes);
		MusicXMLWriter writer = new MusicXMLWriter();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer.writeScore(score, out);
		assertEquals(Collections.emptyList(), writer.getWarnings());
		MusicXMLReader reader = new MusicXMLReader();
		Denotator back = reader.readScore(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(Collections.emptyList(), reader.getWarnings());
		assertEquals(ScoreMidiTest.sorted(notes), ScoreNote.fromScore(back));
		assertTrue(score.equals(back));
	}

	public void testWriterLayout() {
		MusicXMLWriter writer = new MusicXMLWriter();
		String xml = writer.toXML(richScore());
		//a triplet needs 3 divisions per quarter and the thirty-second note 8, so 24
		assertTrue(xml.contains("<divisions>24</divisions>"));
		assertTrue(xml.contains("<score-part id=\"V0\">"));
		assertTrue(xml.contains("<midi-channel>2</midi-channel>"));
		assertTrue(xml.contains("<score-part id=\"V20\">"));
		assertTrue(xml.contains("<score-part id=\"V-2\">"));
		//loudness 80 is 88.8889 % of 90
		assertTrue(xml.contains("<note dynamics=\"88.8889\">"));
		//121/2 is 60.5, a quarter tone above middle C
		assertTrue(xml.contains("<step>C</step>\n          <alter>0.5</alter>\n          <octave>4</octave>"));
		assertTrue(xml.contains("<grace/>"));
		assertTrue(xml.contains("<tie type=\"start\"/>"));
		assertTrue(xml.contains("<backup>"));
		assertTrue(xml.contains("<clef>\n          <sign>F</sign>"));
		assertTrue(xml.contains("<type>half</type>"));
		assertTrue(xml.contains("<type>quarter</type>\n        <dot/>"));
	}

	public void testNegativeOnsetsAreRejected() {
		try {
			new MusicXMLWriter().toXML(Arrays.asList(new ScoreNote(-0.5, 60, 80, 1, 0)));
			fail();
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().startsWith("MusicXML has no time before the first measure"));
		}
	}

	public void testValuesThatNeedRoundingAreReported() {
		MusicXMLWriter writer = new MusicXMLWriter();
		writer.toXML(Arrays.asList(new ScoreNote(0.1234567, new Rational(181, 3), -5, 1, 0)));
		//the onset, the pitch, the loudness, and the rests around the rounded onset, which no note value expresses
		assertEquals(writer.getWarnings().toString(), 4, writer.getWarnings().size());
		assertTrue(writer.getWarnings().get(3), writer.getWarnings().get(3).startsWith("2 notes or rests have lengths that no plain, dotted or triplet note value expresses"));
	}

	/** A hand-written file that uses what MusicXMLWriter never writes. */
	private static final String FOREIGN =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		"<!DOCTYPE score-partwise PUBLIC \"-//Recordare//DTD MusicXML 4.0 Partwise//EN\" \"http://www.musicxml.org/dtds/partwise.dtd\">\n" +
		"<score-partwise version=\"4.0\">\n" +
		"  <part-list>\n" +
		"    <score-part id=\"P1\"><part-name>Flute</part-name>\n" +
		"      <score-instrument id=\"P1-I1\"><instrument-name>Flute</instrument-name></score-instrument>\n" +
		"      <midi-instrument id=\"P1-I1\"><midi-channel>4</midi-channel></midi-instrument></score-part>\n" +
		"    <score-part id=\"P2\"><part-name>Drums</part-name></score-part>\n" +
		"  </part-list>\n" +
		"  <part id=\"P1\">\n" +
		"    <measure number=\"1\">\n" +
		"      <attributes><divisions>2</divisions><time><beats>3</beats><beat-type>4</beat-type></time></attributes>\n" +
		"      <direction placement=\"below\"><direction-type><dynamics><p/></dynamics></direction-type><sound dynamics=\"54.44\"/></direction>\n" +
		"      <note><pitch><step>E</step><alter>-1</alter><octave>5</octave></pitch><duration>2</duration><voice>1</voice><type>quarter</type></note>\n" +
		"      <note><chord/><pitch><step>G</step><octave>5</octave></pitch><duration>2</duration><voice>1</voice></note>\n" +
		"      <note><cue/><pitch><step>A</step><octave>5</octave></pitch><duration>1</duration><voice>1</voice></note>\n" +
		"      <note><rest/><duration>1</duration><voice>1</voice></note>\n" +
		"      <note><pitch><step>B</step><octave>4</octave></pitch><duration>2</duration><tie type=\"start\"/><voice>1</voice></note>\n" +
		"      <backup><duration>6</duration></backup>\n" +
		"      <note dynamics=\"100\"><pitch><step>C</step><octave>3</octave></pitch><duration>6</duration><voice>2</voice></note>\n" +
		"    </measure>\n" +
		"    <measure number=\"2\">\n" +
		"      <attributes><divisions>6</divisions></attributes>\n" +
		"      <note><pitch><step>B</step><octave>4</octave></pitch><duration>3</duration><tie type=\"stop\"/><voice>1</voice></note>\n" +
		"      <note><grace slash=\"yes\"/><pitch><step>D</step><octave>5</octave></pitch><voice>1</voice><type>eighth</type></note>\n" +
		"      <note><pitch><step>C</step><alter>1</alter><octave>5</octave></pitch><duration>2</duration><voice>1</voice></note>\n" +
		"      <forward><duration>1</duration></forward>\n" +
		"      <note><pitch><step>C</step><octave>5</octave></pitch><duration>12</duration><voice>1</voice></note>\n" +
		"    </measure>\n" +
		"  </part>\n" +
		"  <part id=\"P2\">\n" +
		"    <measure number=\"1\">\n" +
		"      <attributes><divisions>1</divisions></attributes>\n" +
		"      <note><unpitched><display-step>C</display-step><display-octave>5</display-octave></unpitched><duration>1</duration><voice>1</voice></note>\n" +
		"      <note><pitch><step>F</step><octave>2</octave></pitch><duration>2</duration><voice>1</voice></note>\n" +
		"    </measure>\n" +
		"  </part>\n" +
		"</score-partwise>\n";

	private static final List<ScoreNote> FOREIGN_NOTES = Arrays.asList(
		//sound dynamics 54.44 % is velocity 49; channel 4 is voice 3
		new ScoreNote(0, 75, 49, 1, 3),
		new ScoreNote(0, 79, 49, 1, 3),
		//the cue note and the rest take a quarter; the tied B lasts 1 + 0.5
		new ScoreNote(2, 71, 49, 1.5, 3),
		new ScoreNote(3.5, 74, 49, 0, 3),
		new ScoreNote(3.5, 73, 49, 1.0 / 3, 3),
		//after a forward of 1/6
		new ScoreNote(4, 72, 49, 2, 3),
		new ScoreNote(0, 48, 90, 3, 3),
		//the drum part has no channel, so it is voice 1, its index; the unpitched note is skipped
		new ScoreNote(1, 41, 90, 2, 1));

	public void testReadsAFileFromAnotherProgram() throws IOException {
		MusicXMLReader reader = new MusicXMLReader();
		List<ScoreNote> notes = reader.read(new ByteArrayInputStream(FOREIGN.getBytes(StandardCharsets.UTF_8)));
		assertEquals(ScoreMidiTest.sorted(FOREIGN_NOTES), ScoreMidiTest.sorted(notes));
		assertEquals(1, reader.getWarnings().size());
		assertTrue(reader.getWarnings().get(0).startsWith("An unpitched note"));
	}

	public void testReadsACompressedFile() throws IOException {
		ByteArrayOutputStream mxl = new ByteArrayOutputStream();
		ZipOutputStream zip = new ZipOutputStream(mxl);
		zip.putNextEntry(new ZipEntry("mimetype"));
		zip.write("application/vnd.recordare.musicxml".getBytes(StandardCharsets.US_ASCII));
		zip.putNextEntry(new ZipEntry("META-INF/container.xml"));
		zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<container><rootfiles>"
			+ "<rootfile full-path=\"score/piece.musicxml\" media-type=\"application/vnd.recordare.musicxml+xml\"/>"
			+ "</rootfiles></container>").getBytes(StandardCharsets.UTF_8));
		zip.putNextEntry(new ZipEntry("score/piece.musicxml"));
		zip.write(FOREIGN.getBytes(StandardCharsets.UTF_8));
		zip.close();
		List<ScoreNote> notes = new MusicXMLReader().read(new ByteArrayInputStream(mxl.toByteArray()));
		assertEquals(ScoreMidiTest.sorted(FOREIGN_NOTES), ScoreMidiTest.sorted(notes));
	}

	public void testTimewiseFilesAreRefusedWithAMessage() {
		try {
			new MusicXMLReader().read(new ByteArrayInputStream("<score-timewise version=\"4.0\"/>".getBytes(StandardCharsets.UTF_8)));
			fail();
		}
		catch (IOException e) {
			assertTrue(e.getMessage(), e.getMessage().startsWith("Timewise MusicXML is not supported"));
		}
	}

	public void testExternalEntitiesAreNotLoaded() throws IOException {
		String xml = "<?xml version=\"1.0\"?>\n<!DOCTYPE score-partwise [<!ENTITY x SYSTEM \"file:///etc/hostname\">]>\n"
			+ "<score-partwise version=\"4.0\"><part-list><score-part id=\"P1\"><part-name>&x;</part-name></score-part></part-list>"
			+ "<part id=\"P1\"><measure number=\"1\"/></part></score-partwise>";
		try {
			new MusicXMLReader().read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		}
		catch (IOException e) {
			//refusing the entity is also fine; reading it is not
			assertFalse(e.getMessage(), e.getMessage().contains("FileNotFound"));
		}
	}

	/**
	 * Validates the writer's output against the MusicXML 4.0 XSD in
	 * schema/musicxml-4.0 (with xml.xsd and xlink.xsd beside it). The system
	 * property musicxml.xsd can name another copy. A missing schema fails the
	 * test instead of skipping it.
	 */
	public void testOutputIsValidAgainstTheMusicXML40Schema() throws Exception {
		String xsd = schemaPath();
		Validator v = schemaValidator(xsd);
		List<List<ScoreNote>> scores = Arrays.asList(richScore(), new ArrayList<ScoreNote>(), Arrays.asList(new ScoreNote(0, 60, 80, 1, 0)));
		for (List<ScoreNote> notes : scores) {
			String xml = new MusicXMLWriter().toXML(notes);
			//the validator would otherwise try to read the DTD
			xml = xml.replaceFirst("<!DOCTYPE[^>]*>\n", "");
			v.validate(new StreamSource(new java.io.StringReader(xml)));
		}
		//the schema must also reject a document, or the check above proves nothing
		try {
			v.validate(new StreamSource(new java.io.StringReader("<score-partwise version=\"4.0\"><part-list/><bogus/></score-partwise>")));
			fail("an invalid document passed the schema");
		}
		catch (org.xml.sax.SAXException e) {
			//expected
		}
		System.out.println("MusicXMLTest: " + scores.size() + " documents valid against " + xsd + "; one invalid document rejected");
	}

	/** The MusicXML 4.0 XSD: the system property musicxml.xsd, else the copy in the rubato-io module. */
	static String schemaPath() {
		String xsd = System.getProperty("musicxml.xsd");
		if (xsd == null) {
			for (String candidate : new String[] { "schema/musicxml-4.0/musicxml.xsd", "java/io/schema/musicxml-4.0/musicxml.xsd" }) {
				if (new File(candidate).isFile()) {
					xsd = candidate;
					break;
				}
			}
		}
		assertNotNull("MusicXML 4.0 schema not found; expected schema/musicxml-4.0/musicxml.xsd in the rubato-io module", xsd);
		assertTrue("no schema at " + xsd, new File(xsd).isFile());
		return xsd;
	}

	/** A validator for the XSD at xsd that reads its imports from beside it and loads no DTD. */
	static Validator schemaValidator(String xsd) throws org.xml.sax.SAXException {
		SchemaFactory sf = SchemaFactory.newDefaultInstance();
		sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
		sf.setResourceResolver(new LocalSchemas(new File(xsd).getParentFile()));
		Schema schema = sf.newSchema(new File(xsd));
		Validator v = schema.newValidator();
		v.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		return v;
	}

	/** Resolves the schema's imports of xml.xsd and xlink.xsd from the schema's directory. */
	private static final class LocalSchemas implements org.w3c.dom.ls.LSResourceResolver {
		private final File dir;
		LocalSchemas(File dir) { this.dir = dir; }
		public org.w3c.dom.ls.LSInput resolveResource(String type, String ns, String publicId, final String systemId, final String baseURI) {
			if (systemId == null) return null;
			final File f = new File(this.dir, systemId.substring(systemId.lastIndexOf('/') + 1));
			if (!f.isFile()) return null;
			return new org.w3c.dom.ls.LSInput() {
				public java.io.Reader getCharacterStream() { return null; }
				public void setCharacterStream(java.io.Reader r) { }
				public java.io.InputStream getByteStream() {
					try { return new java.io.FileInputStream(f); } catch (IOException e) { return null; }
				}
				public void setByteStream(java.io.InputStream s) { }
				public String getStringData() { return null; }
				public void setStringData(String s) { }
				public String getSystemId() { return f.toURI().toString(); }
				public void setSystemId(String s) { }
				public String getPublicId() { return null; }
				public void setPublicId(String s) { }
				public String getBaseURI() { return baseURI; }
				public void setBaseURI(String s) { }
				public String getEncoding() { return null; }
				public void setEncoding(String s) { }
				public boolean getCertifiedText() { return false; }
				public void setCertifiedText(boolean b) { }
			};
		}
	}
}
