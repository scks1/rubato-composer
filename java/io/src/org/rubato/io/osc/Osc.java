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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * OSC 1.0 packets (The Open Sound Control 1.0 Specification, Matt Wright,
 * CNMAT, 2002; opensoundcontrol.stanford.edu/spec-1_0.html): messages and
 * bundles, encoded big-endian with every part padded to a multiple of 4
 * bytes.
 * <p>
 * Arguments map to type tags as follows: Integer i (int32), Float f
 * (float32), String s (OSC-string), byte[] b (blob) are the four types the
 * specification requires. The decoder and encoder also handle these
 * optional ones from the specification's list of nonstandard types: Long h
 * (int64), Double d (float64), {@link TimeTag} t, Boolean T and F, and null
 * N. Any other type tag is an error, since the decoder cannot know its size.
 */
public final class Osc {

    private Osc() {}

    /** An NTP time tag: 32 bits of seconds since 1900 and 32 bits of fraction; 1 means "immediately". */
    public static final class TimeTag {
        public static final TimeTag IMMEDIATELY = new TimeTag(1L);
        public final long bits;

        public TimeTag(long bits) {
            this.bits = bits;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TimeTag && ((TimeTag)o).bits == this.bits;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.bits);
        }

        @Override
        public String toString() {
            return "TimeTag[" + Long.toHexString(this.bits) + "]";
        }
    }

    /** A message: an address pattern and arguments. */
    public static final class Message {
        public final String address;
        public final List<Object> arguments;

        public Message(String address, Object... arguments) {
            this(address, Arrays.asList(arguments));
        }

        public Message(String address, List<Object> arguments) {
            if (address == null || !address.startsWith("/")) {
                throw new IllegalArgumentException("An OSC address pattern starts with '/': " + address);
            }
            this.address = address;
            this.arguments = Collections.unmodifiableList(new ArrayList<Object>(arguments));
            Osc.typeTags(this.arguments);
        }

        public String typeTags() {
            return Osc.typeTags(this.arguments);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Message)) {
                return false;
            }
            Message m = (Message)o;
            if (!this.address.equals(m.address) || this.arguments.size() != m.arguments.size()) {
                return false;
            }
            for (int i = 0; i < this.arguments.size(); i++) {
                Object a = this.arguments.get(i), b = m.arguments.get(i);
                if (a instanceof byte[] && b instanceof byte[]) {
                    if (!Arrays.equals((byte[])a, (byte[])b)) return false;
                }
                else if (a == null ? b != null : !a.equals(b)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return this.address.hashCode() * 31 + this.arguments.size();
        }

        @Override
        public String toString() {
            return this.address + " " + this.typeTags() + " " + this.arguments;
        }
    }

    /** A bundle: a time tag and elements, each a Message or a Bundle. */
    public static final class Bundle {
        public final TimeTag timeTag;
        public final List<Object> elements;

        public Bundle(TimeTag timeTag, List<?> elements) {
            this.timeTag = timeTag;
            for (Object e : elements) {
                if (!(e instanceof Message || e instanceof Bundle)) {
                    throw new IllegalArgumentException("A bundle element is a message or a bundle: " + e);
                }
            }
            this.elements = Collections.unmodifiableList(new ArrayList<Object>(elements));
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Bundle && ((Bundle)o).timeTag.equals(this.timeTag) && ((Bundle)o).elements.equals(this.elements);
        }

        @Override
        public int hashCode() {
            return this.timeTag.hashCode() * 31 + this.elements.hashCode();
        }
    }

    static String typeTags(List<Object> arguments) {
        StringBuilder b = new StringBuilder(",");
        for (Object a : arguments) {
            if (a instanceof Integer) b.append('i');
            else if (a instanceof Float) b.append('f');
            else if (a instanceof String) b.append('s');
            else if (a instanceof byte[]) b.append('b');
            else if (a instanceof Long) b.append('h');
            else if (a instanceof Double) b.append('d');
            else if (a instanceof TimeTag) b.append('t');
            else if (a instanceof Boolean) b.append(((Boolean)a) ? 'T' : 'F');
            else if (a == null) b.append('N');
            else throw new IllegalArgumentException("no OSC type for " + a.getClass().getName());
        }
        return b.toString();
    }

    //
    // Encoding
    //

    /** Encodes a Message or a Bundle. */
    public static byte[] encode(Object packet) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (packet instanceof Message) {
            encodeMessage((Message)packet, out);
        }
        else if (packet instanceof Bundle) {
            Bundle bundle = (Bundle)packet;
            writeString(out, "#bundle");
            writeLong(out, bundle.timeTag.bits);
            for (Object e : bundle.elements) {
                byte[] element = encode(e);
                writeInt(out, element.length);
                out.write(element, 0, element.length);
            }
        }
        else {
            throw new IllegalArgumentException("not an OSC packet: " + packet);
        }
        return out.toByteArray();
    }

    private static void encodeMessage(Message m, ByteArrayOutputStream out) {
        writeString(out, m.address);
        writeString(out, m.typeTags());
        for (Object a : m.arguments) {
            if (a instanceof Integer) writeInt(out, (Integer)a);
            else if (a instanceof Float) writeInt(out, Float.floatToIntBits((Float)a));
            else if (a instanceof String) writeString(out, (String)a);
            else if (a instanceof byte[]) {
                byte[] blob = (byte[])a;
                writeInt(out, blob.length);
                out.write(blob, 0, blob.length);
                pad(out, blob.length);
            }
            else if (a instanceof Long) writeLong(out, (Long)a);
            else if (a instanceof Double) writeLong(out, Double.doubleToLongBits((Double)a));
            else if (a instanceof TimeTag) writeLong(out, ((TimeTag)a).bits);
            // T, F and N carry no data
        }
    }

    private static void writeString(ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
        for (byte x : bytes) {
            if (x == 0) {
                throw new IllegalArgumentException("An OSC-string cannot contain a null character");
            }
        }
        out.write(bytes, 0, bytes.length);
        out.write(0);
        pad(out, bytes.length + 1);
    }

    private static void pad(ByteArrayOutputStream out, int length) {
        for (int i = length; i % 4 != 0; i++) {
            out.write(0);
        }
    }

    private static void writeInt(ByteArrayOutputStream out, int v) {
        out.write(v >>> 24);
        out.write(v >>> 16);
        out.write(v >>> 8);
        out.write(v);
    }

    private static void writeLong(ByteArrayOutputStream out, long v) {
        writeInt(out, (int)(v >>> 32));
        writeInt(out, (int)v);
    }

    //
    // Decoding
    //

    /** Decodes a packet into a Message or a Bundle. */
    public static Object decode(byte[] data, int offset, int length) throws IOException {
        if (length % 4 != 0) {
            throw new IOException("An OSC packet's size is a multiple of 4, not " + length);
        }
        ByteBuffer buf = ByteBuffer.wrap(data, offset, length);
        try {
            return decodePacket(buf, offset + length);
        }
        catch (BufferUnderflowException e) {
            throw new IOException("The OSC packet ends early");
        }
    }

    public static Object decode(byte[] data) throws IOException {
        return decode(data, 0, data.length);
    }

    private static Object decodePacket(ByteBuffer buf, int end) throws IOException {
        if (buf.remaining() == 0) {
            throw new IOException("empty OSC packet");
        }
        byte first = buf.get(buf.position());
        if (first == '#') {
            String tag = readString(buf);
            if (!"#bundle".equals(tag)) {
                throw new IOException("bad bundle tag " + tag);
            }
            TimeTag t = new TimeTag(buf.getLong());
            List<Object> elements = new ArrayList<Object>();
            while (buf.position() < end) {
                int size = buf.getInt();
                if (size < 0 || size % 4 != 0 || buf.position() + size > end) {
                    throw new IOException("bad bundle element size " + size);
                }
                ByteBuffer slice = buf.slice(buf.position(), size);
                elements.add(decodePacket(slice, size));
                buf.position(buf.position() + size);
            }
            return new Bundle(t, elements);
        }
        if (first != '/') {
            throw new IOException("An OSC packet starts with '/' or '#', not " + (char)first);
        }
        String address = readString(buf);
        List<Object> args = new ArrayList<Object>();
        if (buf.position() >= end) {
            // OSC 1.0 lets older senders omit the type tag string
            return new Message(address, args);
        }
        String tags = readString(buf);
        if (!tags.startsWith(",")) {
            throw new IOException("The type tag string starts with ',': " + tags);
        }
        for (int i = 1; i < tags.length(); i++) {
            char t = tags.charAt(i);
            switch (t) {
                case 'i': args.add(buf.getInt()); break;
                case 'f': args.add(Float.intBitsToFloat(buf.getInt())); break;
                case 's': args.add(readString(buf)); break;
                case 'b': {
                    int n = buf.getInt();
                    if (n < 0 || n > buf.remaining()) {
                        throw new IOException("bad blob size " + n);
                    }
                    byte[] blob = new byte[n];
                    buf.get(blob);
                    buf.position(buf.position() + (4 - n % 4) % 4);
                    args.add(blob);
                    break;
                }
                case 'h': args.add(buf.getLong()); break;
                case 'd': args.add(Double.longBitsToDouble(buf.getLong())); break;
                case 't': args.add(new TimeTag(buf.getLong())); break;
                case 'T': args.add(Boolean.TRUE); break;
                case 'F': args.add(Boolean.FALSE); break;
                case 'N': args.add(null); break;
                default: throw new IOException("unsupported OSC type tag '" + t + "'");
            }
        }
        return new Message(address, args);
    }

    private static String readString(ByteBuffer buf) throws IOException {
        int start = buf.position();
        int i = start;
        while (true) {
            if (i >= buf.limit()) {
                throw new IOException("unterminated OSC-string");
            }
            if (buf.get(i) == 0) {
                break;
            }
            i++;
        }
        byte[] bytes = new byte[i - start];
        buf.get(bytes);
        int padded = (i - start + 1 + 3) / 4 * 4;
        buf.position(start + padded);
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}
