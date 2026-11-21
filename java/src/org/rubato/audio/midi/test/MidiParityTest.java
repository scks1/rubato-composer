package org.rubato.audio.midi.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;

import junit.framework.TestCase;

import org.rubato.audio.midi.MidiPlayer;
import org.rubato.audio.midi.MidiReader;
import org.rubato.io.ScoreNote;
import org.rubato.io.midi.ScoreMidi;
import org.rubato.io.midi.StandardMidiFile;
import org.rubato.math.arith.Rational;
import org.rubato.math.yoneda.Denotator;

/**
 * Compares rubato-io's headless MIDI file code with the javax.sound.midi code
 * that MidiFileInRubette (MidiReader) and MidiFileOutRubette (MidiPlayer)
 * use. The readers must agree exactly; the writers must agree on every note
 * event, apart from the differences listed in ScoreMidi's documentation.
 */
public class MidiParityTest extends TestCase {

	/** Notes within MIDI's ranges, starting after time 0, in four voices. */
	static List<ScoreNote> notes() {
		List<ScoreNote> notes = new ArrayList<ScoreNote>();
		notes.add(new ScoreNote(0.5, 60, 100, 1, 0));
		notes.add(new ScoreNote(0.5, 64, 90, 1, 0));
		notes.add(new ScoreNote(1.5, 60, 80, 0.5, 0));        //the same key right after the first one ends
		notes.add(new ScoreNote(1.0, 72, 70, 1.0 / 3, 1));
		notes.add(new ScoreNote(4.0 / 3, 74, 71, 1.0 / 3, 1));
		notes.add(new ScoreNote(5.0 / 3, 76, 72, 1.0 / 3, 1));
		notes.add(new ScoreNote(2, 36, 127, 4, 2));
		notes.add(new ScoreNote(2.75, 50, 1, 0.25, 3));
		notes.add(new ScoreNote(3, new Rational(121, 2), 64, 1, 3)); //a quarter tone: both writers round the key
		return notes;
	}

	private static byte[] midiPlayerFile(Denotator score) throws IOException {
		MidiPlayer player = new MidiPlayer();
		player.setScore(score);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		player.writeSequence(out);
		return out.toByteArray();
	}

	private static byte[] scoreMidiFile(Denotator score) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new ScoreMidi().writeScore(score, out);
		return out.toByteArray();
	}

	public void testMidiReaderAndScoreMidiReadTheSameScoreFromBothWriters() throws IOException, InvalidMidiDataException {
		Denotator score = ScoreNote.toScore(notes());
		for (byte[] file : Arrays.asList(midiPlayerFile(score), scoreMidiFile(score))) {
			Denotator old = new MidiReader(new ByteArrayInputStream(file)).getDenotator();
			Denotator headless = ScoreMidi.readScore(new ByteArrayInputStream(file));
			assertEquals(ScoreNote.fromScore(old), ScoreNote.fromScore(headless));
			assertTrue(old.equals(headless));
		}
	}

	public void testScoreMidiWritesTheNoteEventsMidiPlayerWrites() throws IOException {
		List<ScoreNote> notes = notes();
		Denotator score = ScoreNote.toScore(notes);
		StandardMidiFile old = StandardMidiFile.read(new ByteArrayInputStream(midiPlayerFile(score)));
		StandardMidiFile headless = StandardMidiFile.read(new ByteArrayInputStream(scoreMidiFile(score)));
		assertEquals(1, old.getFormat());
		assertEquals(1, headless.getFormat());
		assertEquals(480, old.getDivision());
		assertEquals(480, headless.getDivision());

		//MidiPlayer moves the first note to tick 1; ScoreMidi keeps the onsets
		long begin = Math.round(0.5 * 480);
		long shift = 1 - begin;
		List<String> oldOns = new ArrayList<String>(), newOns = new ArrayList<String>();
		List<String> oldOffs = new ArrayList<String>(), newOffs = new ArrayList<String>();
		List<String> oldOther = new ArrayList<String>();
		long lastNoteTick = 0;
		for (StandardMidiFile.Track t : old.getTracks()) {
			for (StandardMidiFile.Event e : t.events) {
				if (e.isChannelMessage() && (e.command() == 0x90 || e.command() == 0x80)) {
					String key = (e.tick - shift) + " ch" + e.channel() + " key" + e.data1();
					if (e.command() == 0x90 && e.data2() > 0) oldOns.add(key + " vel" + e.data2());
					else oldOffs.add(key + " release" + e.data2());
					lastNoteTick = Math.max(lastNoteTick, e.tick);
				}
				else if (!(e.status == 0xFF && e.metaType == StandardMidiFile.META_END_OF_TRACK)) {
					oldOther.add(e.toString());
				}
			}
		}
		for (StandardMidiFile.Track t : headless.getTracks()) {
			for (StandardMidiFile.Event e : t.events) {
				if (e.isChannelMessage()) {
					String key = e.tick + " ch" + e.channel() + " key" + e.data1();
					if (e.command() == 0x90 && e.data2() > 0) newOns.add(key + " vel" + e.data2());
					else newOffs.add(key + " release" + e.data2());
				}
			}
		}
		Collections.sort(oldOns);
		Collections.sort(newOns);
		assertEquals(notes.size(), newOns.size());
		assertEquals(oldOns, newOns);

		//note-offs at the same ticks; MidiPlayer's release velocity is the note's loudness, ScoreMidi's is 64
		List<String> expectedOldOffs = new ArrayList<String>();
		for (ScoreNote n : notes) {
			long tick = Math.round(n.onset * 480) + Math.round(n.duration * 480);
			expectedOldOffs.add(tick + " ch" + n.voice + " key" + n.pitch.intValue() + " release" + n.loudness);
		}
		Collections.sort(expectedOldOffs);
		Collections.sort(oldOffs);
		assertEquals(expectedOldOffs, oldOffs);
		List<String> releasedAt64 = new ArrayList<String>();
		for (String s : expectedOldOffs) {
			releasedAt64.add(s.substring(0, s.lastIndexOf(" release")) + " release64");
		}
		Collections.sort(releasedAt64);
		Collections.sort(newOffs);
		assertEquals(releasedAt64, newOffs);

		//what else MidiPlayer writes: tempo and program change at 0. Its code also
		//adds a Stop (0xFC) 1500 ticks after the last note, but the file written
		//by MidiSystem.write holds no Stop, and the note track ends at its last
		//note event (observed with Temurin 21, logs/io-2026-11-21/I04).
		assertEquals(Arrays.asList("0:ff/51 7 161 32", "0:c0 0"), oldOther);
		assertEquals(2, old.getTracks().size());
		List<StandardMidiFile.Event> noteTrack = old.getTracks().get(1).events;
		StandardMidiFile.Event end = noteTrack.get(noteTrack.size() - 1);
		assertEquals(StandardMidiFile.META_END_OF_TRACK, end.metaType);
		assertEquals(lastNoteTick, end.tick);
		System.out.println("MidiParityTest: " + notes.size() + " notes; note-ons equal after MidiPlayer's shift of " + shift
			+ " ticks; note-offs equal apart from release velocity; MidiPlayer adds " + oldOther
			+ " and ends its note track at tick " + end.tick + " (last note event " + lastNoteTick + ")");
	}
}
