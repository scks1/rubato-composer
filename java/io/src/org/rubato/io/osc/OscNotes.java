/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.rubato.io.osc;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.rubato.io.ScoreNote;
import org.rubato.math.arith.Rational;

/**
 * Sends and receives Score notes as OSC 1.0 messages over UDP.
 * <p>
 * OSC defines no address space for notes, so this class sets one:
 * <ul>
 * <li>{@code /rubato/note ,ffifi} onset, pitch, loudness, duration, voice:
 *     Onset and Duration in quarter notes and Pitch as a MIDI key number,
 *     all three as float32; Loudness and Voice as int32. These are only
 *     the types OSC 1.0 requires, so most receivers accept them; float32
 *     keeps about 7 significant digits, and a Pitch that is not a whole
 *     number comes back as the nearest Rational with denominator 1920.</li>
 * <li>{@code /rubato/score/end ,i} the number of notes sent, after the
 *     last bundle of a score.</li>
 * </ul>
 * A score goes out as bundles of at most {@link #NOTES_PER_BUNDLE} note
 * messages with time tag "immediately", each in one datagram. UDP may drop
 * or reorder datagrams; {@link Receiver#receiveScore} reports a count that
 * does not match.
 */
public final class OscNotes {

    public static final String NOTE_ADDRESS = "/rubato/note";
    public static final String END_ADDRESS = "/rubato/score/end";
    public static final int NOTES_PER_BUNDLE = 500;

    private OscNotes() {}

    public static Osc.Message toMessage(ScoreNote n) {
        return new Osc.Message(NOTE_ADDRESS, (float)n.onset, (float)n.pitch.doubleValue(), n.loudness, (float)n.duration, n.voice);
    }

    /** The note in a {@code /rubato/note} message, or null for another address. */
    public static ScoreNote fromMessage(Osc.Message m) throws IOException {
        if (!NOTE_ADDRESS.equals(m.address)) {
            return null;
        }
        if (!",ffifi".equals(m.typeTags())) {
            throw new IOException(NOTE_ADDRESS + " takes ,ffifi, not " + m.typeTags());
        }
        float pitch = (Float)m.arguments.get(1);
        Rational p = pitch == Math.rint(pitch) ? new Rational((int)pitch) : new Rational(pitch, 1920);
        return new ScoreNote((Float)m.arguments.get(0), p, (Integer)m.arguments.get(2), (Float)m.arguments.get(3), (Integer)m.arguments.get(4));
    }

    /**
     * Sends notes to one host and port. The channel is opened for the
     * target's address family (IPv4 or IPv6) and connected to the target,
     * so an IPv4 target is not reached through an IPv4-mapped IPv6 socket.
     */
    public static final class Sender implements Closeable {
        private final DatagramChannel channel;

        public Sender(InetSocketAddress target) throws IOException {
            if (target.isUnresolved()) {
                throw new IOException("Unresolved OSC target " + target);
            }
            ProtocolFamily family = target.getAddress() instanceof Inet6Address ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;
            this.channel = DatagramChannel.open(family);
            this.channel.connect(target);
        }

        public void send(Object packet) throws IOException {
            this.channel.write(ByteBuffer.wrap(Osc.encode(packet)));
        }

        /** Sends the notes in bundles, then the end message with their count. */
        public void sendScore(List<ScoreNote> notes) throws IOException {
            for (int i = 0; i < notes.size(); i += NOTES_PER_BUNDLE) {
                List<Osc.Message> messages = new ArrayList<Osc.Message>();
                for (ScoreNote n : notes.subList(i, Math.min(notes.size(), i + NOTES_PER_BUNDLE))) {
                    messages.add(toMessage(n));
                }
                this.send(new Osc.Bundle(Osc.TimeTag.IMMEDIATELY, messages));
            }
            this.send(new Osc.Message(END_ADDRESS, notes.size()));
        }

        public void close() throws IOException {
            this.channel.close();
        }
    }

    /** Receives notes on a UDP port. */
    public static final class Receiver implements Closeable {
        private final DatagramSocket socket;
        private final byte[] buffer = new byte[65536];

        /** Binds to the address; port 0 picks a free port. */
        public Receiver(InetSocketAddress bind) throws IOException {
            this.socket = new DatagramSocket(bind);
        }

        public int getPort() {
            return this.socket.getLocalPort();
        }

        /** Waits up to the timeout for one packet and decodes it, or returns null on timeout. */
        public Object receive(int timeoutMillis) throws IOException {
            this.socket.setSoTimeout(timeoutMillis);
            DatagramPacket p = new DatagramPacket(this.buffer, this.buffer.length);
            try {
                this.socket.receive(p);
            }
            catch (SocketTimeoutException e) {
                return null;
            }
            return Osc.decode(p.getData(), p.getOffset(), p.getLength());
        }

        /**
         * Collects notes until an end message arrives, waiting at most the
         * timeout for each packet.
         *
         * @throws IOException on a timeout, or when the end message's count
         *                     differs from the notes received
         */
        public List<ScoreNote> receiveScore(int timeoutMillis) throws IOException {
            List<ScoreNote> notes = new ArrayList<ScoreNote>();
            while (true) {
                Object packet = this.receive(timeoutMillis);
                if (packet == null) {
                    throw new IOException("No OSC packet within " + timeoutMillis + " ms after " + notes.size() + " notes");
                }
                Integer end = collect(packet, notes);
                if (end != null) {
                    if (end != notes.size()) {
                        throw new IOException("The sender sent " + end + " notes; " + notes.size() + " arrived");
                    }
                    return notes;
                }
            }
        }

        private static Integer collect(Object packet, List<ScoreNote> notes) throws IOException {
            if (packet instanceof Osc.Bundle) {
                Integer end = null;
                for (Object e : ((Osc.Bundle)packet).elements) {
                    Integer x = collect(e, notes);
                    if (x != null) end = x;
                }
                return end;
            }
            Osc.Message m = (Osc.Message)packet;
            if (END_ADDRESS.equals(m.address) && ",i".equals(m.typeTags())) {
                return (Integer)m.arguments.get(0);
            }
            ScoreNote n = fromMessage(m);
            if (n != null) {
                notes.add(n);
            }
            return null;
        }

        public void close() {
            this.socket.close();
        }
    }
}
