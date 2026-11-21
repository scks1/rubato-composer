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

package org.rubato.io.midi;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads and writes Standard MIDI Files (SMF, the MIDI Manufacturers
 * Association's "Standard MIDI Files 1.0" specification) with only
 * java.base, so that the headless libraries do not need javax.sound.midi,
 * which lives in the java.desktop module.
 * <p>
 * A file holds a header (format, track count, division) and tracks of
 * events with absolute ticks. Reading accepts running status, skips chunk
 * types other than MThd and MTrk, and reads system common and real-time
 * status bytes that some writers put into files. Writing uses no running
 * status and ends each track with an End of Track meta event.
 */
public final class StandardMidiFile {

    /** Meta event type of End of Track. */
    public static final int META_END_OF_TRACK = 0x2F;
    /** Meta event type of Set Tempo (microseconds per quarter note, 3 bytes). */
    public static final int META_TEMPO = 0x51;

    /** One event. {@code status} is the status byte: a channel status, 0xF0 or 0xF7 for SysEx, 0xFF for a meta event, or another system status. */
    public static final class Event {
        public final long   tick;
        public final int    status;
        /** The meta event type, or -1 for events that are not meta events. */
        public final int    metaType;
        /** Data bytes: the one or two data bytes of a channel message, or the payload of a SysEx or meta event. */
        public final byte[] data;

        public Event(long tick, int status, int metaType, byte[] data) {
            if (tick < 0) {
                throw new IllegalArgumentException("negative tick " + tick);
            }
            this.tick = tick;
            this.status = status;
            this.metaType = metaType;
            this.data = data;
        }

        public static Event channel(long tick, int command, int channel, int data1, int data2) {
            int status = (command & 0xF0) | (channel & 0x0F);
            int n = dataLength(status);
            byte[] data = n == 1 ? new byte[] { (byte)(data1 & 0x7F) }
                                 : new byte[] { (byte)(data1 & 0x7F), (byte)(data2 & 0x7F) };
            return new Event(tick, status, -1, data);
        }

        public static Event meta(long tick, int type, byte[] data) {
            return new Event(tick, 0xFF, type, data);
        }

        public static Event tempo(long tick, int microsecondsPerQuarter) {
            int m = microsecondsPerQuarter;
            return meta(tick, META_TEMPO, new byte[] { (byte)(m >> 16), (byte)(m >> 8), (byte)m });
        }

        public boolean isChannelMessage() {
            return this.status >= 0x80 && this.status < 0xF0;
        }

        public int command() {
            return this.status & 0xF0;
        }

        public int channel() {
            return this.status & 0x0F;
        }

        public int data1() {
            return this.data.length > 0 ? this.data[0] & 0xFF : 0;
        }

        public int data2() {
            return this.data.length > 1 ? this.data[1] & 0xFF : 0;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(this.tick).append(':').append(Integer.toHexString(this.status));
            if (this.metaType >= 0) {
                b.append('/').append(Integer.toHexString(this.metaType));
            }
            for (byte x : this.data) {
                b.append(' ').append(x & 0xFF);
            }
            return b.toString();
        }
    }

    /** A track: events in file order, with absolute ticks. */
    public static final class Track {
        public final List<Event> events = new ArrayList<Event>();
    }

    private final int format;
    private final int division;
    private final List<Track> tracks;

    /**
     * @param format   0, 1 or 2
     * @param division ticks per quarter note when positive and below 0x8000;
     *                 a raw SMPTE division word otherwise
     */
    public StandardMidiFile(int format, int division, List<Track> tracks) {
        if (format < 0 || format > 2) {
            throw new IllegalArgumentException("SMF format must be 0, 1 or 2: " + format);
        }
        if (division <= 0 || division > 0xFFFF) {
            throw new IllegalArgumentException("bad division " + division);
        }
        this.format = format;
        this.division = division;
        this.tracks = Collections.unmodifiableList(new ArrayList<Track>(tracks));
    }

    public int getFormat() {
        return this.format;
    }

    public int getDivision() {
        return this.division;
    }

    /** True when the division counts ticks per quarter note (bit 15 clear). */
    public boolean isPPQ() {
        return (this.division & 0x8000) == 0;
    }

    public List<Track> getTracks() {
        return this.tracks;
    }

    /**
     * Number of data bytes after a status byte, or -1 for SysEx and meta
     * events, which carry a length.
     */
    static int dataLength(int status) {
        if (status < 0x80) {
            throw new IllegalArgumentException("not a status byte: " + status);
        }
        if (status < 0xF0) {
            int c = status & 0xF0;
            return (c == 0xC0 || c == 0xD0) ? 1 : 2;
        }
        switch (status) {
            case 0xF0: case 0xF7: case 0xFF: return -1;
            case 0xF1: case 0xF3: return 1;
            case 0xF2: return 2;
            default: return 0;
        }
    }

    //
    // Reading
    //

    public static StandardMidiFile read(InputStream input) throws IOException {
        DataInputStream in = new DataInputStream(input);
        String id = readChunkId(in);
        if (!"MThd".equals(id)) {
            throw new IOException("Not a Standard MIDI File: the first chunk is \"" + id + "\", not \"MThd\"");
        }
        long headerLength = in.readInt() & 0xFFFFFFFFL;
        if (headerLength < 6) {
            throw new IOException("MThd chunk too short: " + headerLength);
        }
        int format = in.readUnsignedShort();
        int ntrks = in.readUnsignedShort();
        int division = in.readUnsignedShort();
        skipFully(in, headerLength - 6);
        List<Track> tracks = new ArrayList<Track>(ntrks);
        while (tracks.size() < ntrks) {
            String chunk;
            try {
                chunk = readChunkId(in);
            }
            catch (EOFException e) {
                throw new IOException("The header announces " + ntrks + " tracks, the file holds " + tracks.size());
            }
            long length = in.readInt() & 0xFFFFFFFFL;
            if (length > Integer.MAX_VALUE) {
                throw new IOException("chunk too long: " + length);
            }
            byte[] body = new byte[(int)length];
            in.readFully(body);
            if ("MTrk".equals(chunk)) {
                tracks.add(readTrack(body));
            }
            // other chunk types are skipped, as the SMF specification asks
        }
        return new StandardMidiFile(format > 2 ? 1 : format, division == 0 ? 1 : division, tracks);
    }

    private static Track readTrack(byte[] body) throws IOException {
        Track track = new Track();
        int[] pos = { 0 };
        long tick = 0;
        int running = -1;
        while (pos[0] < body.length) {
            tick += readVarLen(body, pos);
            int b = byteAt(body, pos);
            int status;
            if (b >= 0x80) {
                status = b;
                pos[0]++;
            }
            else {
                if (running < 0) {
                    throw new IOException("data byte " + b + " without a status at offset " + pos[0]);
                }
                status = running;
            }
            if (status == 0xFF) {
                int type = byteAt(body, pos);
                pos[0]++;
                int len = (int)readVarLen(body, pos);
                byte[] data = slice(body, pos, len);
                track.events.add(new Event(tick, status, type, data));
                running = -1;
                if (type == META_END_OF_TRACK) {
                    break;
                }
            }
            else if (status == 0xF0 || status == 0xF7) {
                int len = (int)readVarLen(body, pos);
                track.events.add(new Event(tick, status, -1, slice(body, pos, len)));
                running = -1;
            }
            else {
                int n = dataLength(status);
                track.events.add(new Event(tick, status, -1, slice(body, pos, n)));
                if (status < 0xF0) {
                    running = status;
                }
                else if (status < 0xF8) {
                    // system common messages cancel running status;
                    // real-time messages (0xF8-0xFE) leave it in place
                    running = -1;
                }
            }
        }
        return track;
    }

    private static int byteAt(byte[] body, int[] pos) throws IOException {
        if (pos[0] >= body.length) {
            throw new IOException("track ends inside an event");
        }
        return body[pos[0]] & 0xFF;
    }

    private static byte[] slice(byte[] body, int[] pos, int len) throws IOException {
        if (len < 0 || pos[0] + len > body.length) {
            throw new IOException("track ends inside an event");
        }
        byte[] data = new byte[len];
        System.arraycopy(body, pos[0], data, 0, len);
        pos[0] += len;
        return data;
    }

    static long readVarLen(byte[] body, int[] pos) throws IOException {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            int b = byteAt(body, pos);
            pos[0]++;
            value = (value << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                return value;
            }
        }
        throw new IOException("variable-length quantity longer than 4 bytes");
    }

    private static String readChunkId(DataInputStream in) throws IOException {
        byte[] id = new byte[4];
        in.readFully(id);
        return new String(id, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    private static void skipFully(DataInputStream in, long n) throws IOException {
        while (n > 0) {
            int skipped = in.skipBytes((int)Math.min(n, Integer.MAX_VALUE));
            if (skipped <= 0) {
                throw new EOFException();
            }
            n -= skipped;
        }
    }

    //
    // Writing
    //

    public void write(OutputStream out) throws IOException {
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(new byte[] { 'M', 'T', 'h', 'd', 0, 0, 0, 6 });
        writeShort(file, this.format);
        writeShort(file, this.tracks.size());
        writeShort(file, this.division);
        for (Track t : this.tracks) {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            long last = 0;
            boolean ended = false;
            for (Event e : t.events) {
                if (e.tick < last) {
                    throw new IllegalStateException("track events are not in tick order at " + e);
                }
                writeVarLen(body, e.tick - last);
                last = e.tick;
                body.write(e.status);
                if (e.status == 0xFF) {
                    body.write(e.metaType);
                    writeVarLen(body, e.data.length);
                }
                else if (e.status == 0xF0 || e.status == 0xF7) {
                    writeVarLen(body, e.data.length);
                }
                body.write(e.data);
                if (e.status == 0xFF && e.metaType == META_END_OF_TRACK) {
                    ended = true;
                    break;
                }
            }
            if (!ended) {
                writeVarLen(body, 0);
                body.write(new byte[] { (byte)0xFF, META_END_OF_TRACK, 0 });
            }
            file.write(new byte[] { 'M', 'T', 'r', 'k' });
            int n = body.size();
            file.write(new byte[] { (byte)(n >> 24), (byte)(n >> 16), (byte)(n >> 8), (byte)n });
            body.writeTo(file);
        }
        file.writeTo(out);
    }

    private static void writeShort(ByteArrayOutputStream out, int v) {
        out.write((v >> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    static void writeVarLen(ByteArrayOutputStream out, long value) {
        if (value < 0 || value > 0x0FFFFFFFL) {
            throw new IllegalArgumentException("delta time out of range for a variable-length quantity: " + value);
        }
        int shift = 21;
        boolean started = false;
        for (; shift > 0; shift -= 7) {
            int group = (int)((value >> shift) & 0x7F);
            if (group != 0 || started) {
                out.write(group | 0x80);
                started = true;
            }
        }
        out.write((int)(value & 0x7F));
    }
}
