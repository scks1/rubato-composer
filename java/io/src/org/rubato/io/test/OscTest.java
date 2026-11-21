package org.rubato.io.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.io.ScoreNote;
import org.rubato.io.osc.Osc;
import org.rubato.io.osc.OscNotes;
import org.rubato.math.arith.Rational;

public class OscTest extends TestCase {

	//"Examples Supporting the OpenSoundControl 1.0 Spec" (Matt Wright, 2002), OSC Message Examples
	public void testEncodesTheSpecificationsExampleMessages() throws IOException {
		byte[] frequency = ScoreMidiTest.bytes(
			"2f 6f 73 63 69 6c 6c 61 74 6f 72 2f 34 2f 66 72 65 71 75 65 6e 63 79 00",
			"2c 66 00 00 43 dc 00 00");
		Osc.Message m = new Osc.Message("/oscillator/4/frequency", 440.0f);
		assertTrue(Arrays.equals(frequency, Osc.encode(m)));
		assertEquals(m, Osc.decode(frequency));

		byte[] foo = ScoreMidiTest.bytes(
			"2f 66 6f 6f 00 00 00 00 2c 69 69 73 66 66 00 00",
			"00 00 03 e8 ff ff ff ff 68 65 6c 6c 6f 00 00 00",
			"3f 9d f3 b6 40 b5 b2 2d");
		Osc.Message m2 = new Osc.Message("/foo", 1000, -1, "hello", 1.234f, 5.678f);
		assertTrue(Arrays.equals(foo, Osc.encode(m2)));
		assertEquals(m2, Osc.decode(foo));
	}

	public void testOtherTypesAndNestedBundlesRoundTrip() throws IOException {
		Osc.Message a = new Osc.Message("/a", new byte[] { 1, 2, 3, 4, 5 }, 1L << 40, Math.PI, Boolean.TRUE, Boolean.FALSE, null, new Osc.TimeTag(0x83AA7E8000000000L), "");
		Osc.Message b = new Osc.Message("/b");
		Osc.Bundle inner = new Osc.Bundle(Osc.TimeTag.IMMEDIATELY, Arrays.asList(b));
		Osc.Bundle outer = new Osc.Bundle(new Osc.TimeTag(42), Arrays.asList(a, inner));
		byte[] bytes = Osc.encode(outer);
		assertEquals(0, bytes.length % 4);
		assertEquals(outer, Osc.decode(bytes));
		assertEquals(",bhdTFNts", a.typeTags());
		//a blob of 5 bytes takes 4 + 8
		assertEquals(4 + 12 + 12 + 8 + 8 + 8 + 4, Osc.encode(a).length);
	}

	public void testMalformedPacketsAreErrors() {
		String[] bad = {
			"2f 66 6f 6f 00 00 00 00 2c 69 00 00",           //an int32 is missing
			"2f 66 6f 6f 00 00 00 00 2c 7a 00 00 00 00 00 00", //unknown type z
			"2f 66 6f 6f",                                     //unterminated address
			"23 62 75 6e 64 6c 65 00 00 00 00 00 00 00 00 01 00 00 00 08", //bundle element longer than the packet
			"41 42 43 00"                                      //neither '/' nor '#'
		};
		for (String hex : bad) {
			try {
				Osc.decode(ScoreMidiTest.bytes(hex));
				fail(hex);
			}
			catch (IOException e) {
				//expected
			}
		}
	}

	public void testNoteMessages() throws IOException {
		ScoreNote n = new ScoreNote(1.5, new Rational(121, 2), 100, 0.25, 3);
		Osc.Message m = OscNotes.toMessage(n);
		assertEquals("/rubato/note", m.address);
		assertEquals(",ffifi", m.typeTags());
		assertEquals(n, OscNotes.fromMessage((Osc.Message)Osc.decode(Osc.encode(m))));
	}

	public void testAScoreGoesOverUdpOnLoopback() throws IOException {
		List<ScoreNote> notes = new ArrayList<ScoreNote>();
		for (int i = 0; i < 1203; i++) {
			notes.add(new ScoreNote(i * 0.25, 36 + i % 60, 1 + i % 127, 0.5, i % 4));
		}
		InetAddress loopback = InetAddress.getLoopbackAddress();
		OscNotes.Receiver receiver = new OscNotes.Receiver(new InetSocketAddress(loopback, 0));
		OscNotes.Sender sender = new OscNotes.Sender(new InetSocketAddress(loopback, receiver.getPort()));
		try {
			sender.sendScore(notes);
			List<ScoreNote> got = receiver.receiveScore(5000);
			assertEquals(notes, got);
		}
		finally {
			sender.close();
			receiver.close();
		}
	}

	public void testAMissingEndIsATimeout() throws IOException {
		InetAddress loopback = InetAddress.getLoopbackAddress();
		OscNotes.Receiver receiver = new OscNotes.Receiver(new InetSocketAddress(loopback, 0));
		OscNotes.Sender sender = new OscNotes.Sender(new InetSocketAddress(loopback, receiver.getPort()));
		try {
			sender.send(new Osc.Bundle(Osc.TimeTag.IMMEDIATELY, Arrays.asList(OscNotes.toMessage(new ScoreNote(0, 60, 80, 1, 0)))));
			receiver.receiveScore(300);
			fail();
		}
		catch (IOException e) {
			assertEquals("No OSC packet within 300 ms after 1 notes", e.getMessage());
		}
		finally {
			sender.close();
			receiver.close();
		}
	}
}
