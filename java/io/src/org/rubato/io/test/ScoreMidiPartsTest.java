package org.rubato.io.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.rubato.io.PartInfo;
import org.rubato.io.ScoreNote;
import org.rubato.io.midi.ScoreMidi;
import org.rubato.io.midi.StandardMidiFile;

/** Programs, pan, volume and other controllers in ScoreMidi. */
public class ScoreMidiPartsTest extends TestCase {

	private static final List<ScoreNote> NOTES = Arrays.asList(
		new ScoreNote(0, 72, 90, 1, 0), new ScoreNote(0.5, 64, 70, 1.5, 1),
		new ScoreNote(1, 48, 100, 2, 2), new ScoreNote(2, 36, 60, 1, 17));

	public void testProgramsPanAndVolumeRoundTripAndLeaveTheNotesAlone() throws IOException {
		ScoreMidi plain = new ScoreMidi();
		ScoreMidi midi = new ScoreMidi();
		midi.setPart(0, PartInfo.of("Flute", 73, 84).withVolume(100));
		midi.setPart(1, PartInfo.of("Oboe", 68, 30));
		midi.setPart(2, new PartInfo(null, PartInfo.UNSET, 0, PartInfo.UNSET));
		midi.setPart(17, PartInfo.of("Bass", 45, 127)); // Voice 17 is channel 1, so this overrides the oboe's program and pan
		midi.setController(0, 91, 40);
		StandardMidiFile file = read(write(midi.toFile(NOTES)));
		int[] programs = ScoreMidi.readPrograms(file);
		assertEquals(73, programs[0]);
		assertEquals(45, programs[1]);
		assertEquals(PartInfo.UNSET, programs[2]);
		assertEquals(PartInfo.UNSET, programs[3]);
		assertEquals(new PartInfo(null, 73, 84, 100), ScoreMidi.readPart(file, 0));
		assertEquals(new PartInfo(null, 45, 127, PartInfo.UNSET), ScoreMidi.readPart(file, 1));
		assertEquals(new PartInfo(null, PartInfo.UNSET, 0, PartInfo.UNSET), ScoreMidi.readPart(file, 2));
		assertEquals(PartInfo.NONE, ScoreMidi.readPart(file, 3));
		Map<Integer, Integer> c0 = ScoreMidi.readControllers(file, 0);
		assertEquals(Arrays.asList(7, 10, 91), new ArrayList<Integer>(c0.keySet()));
		assertEquals(Integer.valueOf(40), c0.get(91));
		// the notes read back as they do from a file without settings
		assertEquals(sorted(ScoreMidi.readNotes(read(write(plain.toFile(NOTES))))), sorted(ScoreMidi.readNotes(file)));
	}

	public void testSetupEventsComeFirstAtTickZeroInChannelOrder() throws IOException {
		ScoreMidi midi = new ScoreMidi();
		midi.setPan(2, 20);
		midi.setProgram(2, 11);
		midi.setVolume(2, 90);
		midi.setProgram(0, 73);
		midi.setPan(2, 21); // a second set keeps the first position
		StandardMidiFile file = read(write(midi.toFile(NOTES)));
		List<StandardMidiFile.Event> events = file.getTracks().get(1).events;
		String[] expected = { "c0 73", "c2 11", "b2 10 21", "b2 7 90" };
		for (int i = 0; i < expected.length; i++) {
			StandardMidiFile.Event e = events.get(i);
			assertEquals(0, e.tick);
			StringBuilder s = new StringBuilder(Integer.toHexString(e.status));
			for (byte b : e.data) s.append(' ').append(b & 0xFF);
			assertEquals(expected[i], s.toString());
		}
		assertEquals(0x90, events.get(4).command());
		// a Program Change has one data byte in the file
		assertEquals(1, events.get(0).data.length);
	}

	public void testSettingsCanBeRemovedAndAreChecked() throws IOException {
		ScoreMidi midi = new ScoreMidi();
		midi.setProgram(3, 5);
		midi.setPan(3, 64);
		midi.setProgram(3, PartInfo.UNSET);
		midi.setPan(3, PartInfo.UNSET);
		StandardMidiFile file = midi.toFile(NOTES);
		assertEquals(0x90, file.getTracks().get(1).events.get(0).command());
		int[][] bad = { { -1, 0 }, { 16, 0 }, { 0, 128 }, { 0, -2 } };
		for (int[] b : bad) {
			try {
				midi.setProgram(b[0], b[1]);
				fail("program " + Arrays.toString(b));
			} catch (IllegalArgumentException expected) { }
		}
		try {
			midi.setController(0, 120, 0);
			fail("channel mode message");
		} catch (IllegalArgumentException expected) { }
		try {
			midi.setPan(0, 128);
			fail("pan 128");
		} catch (IllegalArgumentException expected) { }
		try {
			PartInfo.of("x", 128);
			fail("PartInfo program 128");
		} catch (IllegalArgumentException expected) { }
	}

	public void testTheFirstSettingInTimeIsRead() throws IOException {
		StandardMidiFile.Track t1 = new StandardMidiFile.Track();
		t1.events.add(StandardMidiFile.Event.channel(100, 0xC0, 4, 20, 0));
		t1.events.add(StandardMidiFile.Event.channel(100, 0xB0, 4, 10, 1));
		StandardMidiFile.Track t2 = new StandardMidiFile.Track();
		t2.events.add(StandardMidiFile.Event.channel(0, 0xC0, 4, 30, 0));
		t2.events.add(StandardMidiFile.Event.channel(0, 0xB0, 4, 10, 2));
		t2.events.add(StandardMidiFile.Event.channel(200, 0xC0, 4, 40, 0));
		StandardMidiFile file = read(write(new StandardMidiFile(1, 480, Arrays.asList(t1, t2))));
		assertEquals(30, ScoreMidi.readPrograms(file)[4]);
		assertEquals(Integer.valueOf(2), ScoreMidi.readControllers(file, 4).get(10));
	}

	public void testPanAndVolumeMapToMusicXMLUnitsAndBack() {
		assertEquals("-90", PartInfo.panDegrees(0).toPlainString());
		assertEquals("0", PartInfo.panDegrees(64).toPlainString());
		assertEquals("88.59375", PartInfo.panDegrees(127).toPlainString());
		assertEquals("100", PartInfo.volumePercent(127).toPlainString());
		for (int v = 0; v < 128; v++) {
			assertEquals(v, PartInfo.panFromDegrees(PartInfo.panDegrees(v)));
			assertEquals(v, PartInfo.volumeFromPercent(PartInfo.volumePercent(v)));
		}
		assertEquals(0, PartInfo.panFromDegrees(new java.math.BigDecimal("-180")));
		assertEquals(127, PartInfo.panFromDegrees(new java.math.BigDecimal("135")));
	}

	private static byte[] write(StandardMidiFile f) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		f.write(out);
		return out.toByteArray();
	}

	private static StandardMidiFile read(byte[] b) throws IOException {
		return StandardMidiFile.read(new ByteArrayInputStream(b));
	}

	private static List<ScoreNote> sorted(List<ScoreNote> notes) {
		List<ScoreNote> s = new ArrayList<ScoreNote>(notes);
		java.util.Collections.sort(s);
		return s;
	}
}
