package org.rubato.io.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.io.midi.ScoreMidi;
import org.rubato.io.midi.StandardMidiFile;
import org.rubato.math.yoneda.Denotator;

public class ScoreMidiTest extends TestCase {

	//the table of variable-length quantities in the Standard MIDI Files 1.0 specification
	private static final long[] VLQ_VALUES = { 0x0, 0x40, 0x7F, 0x80, 0x2000, 0x3FFF, 0x4000, 0x100000, 0x1FFFFF, 0x200000, 0x8000000, 0xFFFFFFF };
	private static final String[] VLQ_BYTES = { "00", "40", "7f", "81 00", "c0 00", "ff 7f", "81 80 00", "c0 80 00", "ff ff 7f", "81 80 80 00", "c0 80 80 00", "ff ff ff 7f" };

	public void testVariableLengthQuantitiesMatchTheSpecificationTable() throws IOException {
		for (int i = 0; i < VLQ_VALUES.length; i++) {
			StandardMidiFile.Track t = new StandardMidiFile.Track();
			t.events.add(StandardMidiFile.Event.channel(VLQ_VALUES[i], 0x90, 0, 60, 100));
			byte[] bytes = this.write(new StandardMidiFile(0, 96, Arrays.asList(t)));
			//header 14 bytes, "MTrk" and length 8 bytes, then the first delta time
			StringBuilder hex = new StringBuilder();
			int n = VLQ_BYTES[i].split(" ").length;
			for (int k = 0; k < n; k++) {
				if (k > 0) hex.append(' ');
				hex.append(String.format("%02x", bytes[22 + k] & 0xFF));
			}
			assertEquals("VLQ of 0x" + Long.toHexString(VLQ_VALUES[i]), VLQ_BYTES[i], hex.toString());
			assertEquals(0x90, bytes[22 + n] & 0xFF);
			StandardMidiFile back = StandardMidiFile.read(new ByteArrayInputStream(bytes));
			assertEquals(VLQ_VALUES[i], back.getTracks().get(0).events.get(0).tick);
		}
	}

	public void testReadsRunningStatusSkipsUnknownChunksAndStopsAtEndOfTrack() throws IOException {
		byte[] file = bytes(
			"4d 54 68 64 00 00 00 06 00 00 00 01 00 60",       //MThd, format 0, 1 track, 96 per quarter
			"58 59 5a 57 00 00 00 02 ab cd",                    //an unknown chunk "XYZW"
			"4d 54 72 6b 00 00 00 13",                          //MTrk, 19 bytes
			"00 90 3c 40",                                      //on C4 velocity 64
			"60 3e 50",                                         //running status: on D4 at 96
			"60 3c 00",                                         //running status: C4 velocity 0 (off) at 192
			"00 80 3e 40",                                      //off D4 at 192
			"00 ff 2f 00",                                      //end of track
			"00");                                              //a byte after the end of track, ignored
		List<ScoreNote> notes = ScoreMidi.readNotes(StandardMidiFile.read(new ByteArrayInputStream(file)));
		assertEquals(Arrays.asList(new ScoreNote(0, 60, 64, 2, 0), new ScoreNote(1, 62, 80, 1, 0)), sorted(notes));
	}

	public void testReadsAFileWithTheSystemRealTimeStopThatMidiPlayerWrites() throws IOException {
		byte[] file = bytes(
			"4d 54 68 64 00 00 00 06 00 01 00 01 01 e0",
			"4d 54 72 6b 00 00 00 0f",
			"00 90 3c 40",
			"83 60 80 3c 40",                                  //off at 480
			"00 fc",                                           //system real-time Stop
			"00 ff 2f 00");
		List<ScoreNote> notes = ScoreMidi.readNotes(StandardMidiFile.read(new ByteArrayInputStream(file)));
		assertEquals(Arrays.asList(new ScoreNote(0, 60, 64, 1, 0)), notes);
	}

	public void testRejectsSmpteDivisionAsMidiReaderDoes() throws IOException {
		byte[] file = bytes("4d 54 68 64 00 00 00 06 00 00 00 01 e7 28", "4d 54 72 6b 00 00 00 04 00 ff 2f 00");
		try {
			ScoreMidi.readNotes(StandardMidiFile.read(new ByteArrayInputStream(file)));
			fail();
		}
		catch (IOException e) {
			assertEquals("Only division type PPQ supported", e.getMessage());
		}
	}

	public void testAScoreRoundTripsThroughAMidiFile() throws IOException {
		List<ScoreNote> notes = new ArrayList<ScoreNote>(Arrays.asList(
			new ScoreNote(0, 60, 100, 1, 0),
			new ScoreNote(0, 64, 90, 1, 0),
			new ScoreNote(1, 60, 80, 0.5, 0),         //the same key again right after the first ends
			new ScoreNote(1.0 / 3, 72, 70, 1.0 / 3, 1), //a triplet: 160 ticks
			new ScoreNote(2.25, 48, 127, 0, 9),       //duration 0
			new ScoreNote(3, 50, 1, 4, 15)));
		Denotator score = ScoreNote.toScore(notes);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ScoreMidi writer = new ScoreMidi();
		writer.writeScore(score, out);
		assertEquals(0, writer.getShift());
		Denotator back = ScoreMidi.readScore(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(sorted(notes), ScoreNote.fromScore(back));
		assertTrue(score.equals(back));
	}

	public void testFileLayout() throws IOException {
		ScoreMidi writer = new ScoreMidi();
		StandardMidiFile f = writer.toFile(Arrays.asList(new ScoreNote(0, 60, 100, 1, 17)));
		assertEquals(1, f.getFormat());
		assertEquals(480, f.getDivision());
		assertEquals(2, f.getTracks().size());
		assertEquals("[0:ff/51 7 161 32]", f.getTracks().get(0).events.toString());
		//voice 17 is channel 1; note off has release velocity 64
		assertEquals("[0:91 60 100, 480:81 60 64]", f.getTracks().get(1).events.toString());
	}

	public void testNegativeOnsetsShiftTheNotesAndValuesAreClamped() throws IOException {
		ScoreMidi writer = new ScoreMidi();
		StandardMidiFile f = writer.toFile(Arrays.asList(new ScoreNote(-1, 200, 0, 1, -1), new ScoreNote(0, -3, 300, 1, 0)));
		assertEquals(480, writer.getShift());
		//voice -1 is channel 15; pitch clamps to 127 and 0, velocity to 1 and 127
		assertEquals("[0:9f 127 1, 480:8f 127 64, 480:90 0 127, 960:80 0 64]", f.getTracks().get(1).events.toString());
	}

	private byte[] write(StandardMidiFile f) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		f.write(out);
		return out.toByteArray();
	}

	static byte[] bytes(String... hex) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (String h : hex) {
			for (String b : h.trim().split("\\s+")) {
				out.write(Integer.parseInt(b, 16));
			}
		}
		return out.toByteArray();
	}

	static List<ScoreNote> sorted(List<ScoreNote> notes) {
		List<ScoreNote> s = new ArrayList<ScoreNote>(notes);
		java.util.Collections.sort(s);
		return s;
	}
}
