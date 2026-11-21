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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rubato.io.ScoreNote;
import org.rubato.math.yoneda.Denotator;

/**
 * Converts between Standard MIDI Files and denotators of form Score without
 * javax.sound.midi.
 * <p>
 * Reading follows the application's MidiReader note for note: each Note On
 * (velocity above 0) opens a note in the slot of its channel and key, a Note
 * Off or a Note On with velocity 0 sets the duration of the note in that
 * slot, and a note that never gets an off event keeps duration 0. Onset and
 * Duration are ticks divided by the file's ticks per quarter note, Pitch is
 * the key, Loudness the velocity and Voice the channel. Tempo, programs,
 * controllers and track numbers are not read.
 * <p>
 * Writing follows the application's MidiPlayer (used by MidiFileOutRubette)
 * except where noted: a format 1 file with 480 ticks per quarter note, a
 * first track holding a Set Tempo of 500000 microseconds per quarter note
 * (120 quarter notes per minute), and a second track holding the notes,
 * channel = Voice mod 16, key = Pitch rounded and clamped to 0..127,
 * velocity = Loudness clamped. The differences:
 * <ul>
 * <li>MidiPlayer moves the first note to tick 1; this writer keeps onsets
 *     where they are, and moves the notes only when an onset is negative,
 *     so that the earliest lands on tick 0 ({@link #getShift()}).</li>
 * <li>MidiPlayer writes a Note On with velocity 0 for a note of loudness
 *     0 or less, which a reader takes for a Note Off; this writer writes
 *     velocity 1.</li>
 * <li>Note Off events carry release velocity 64, the MIDI 1.0 default,
 *     where MidiPlayer repeats the note's loudness.</li>
 * <li>MidiPlayer adds a program change to program 0 on channel 0; this
 *     writer does not. (MidiPlayer's code also adds a System Real-Time Stop
 *     1500 ticks after the last event, but MidiSystem.write leaves it out of
 *     the file; MidiParityTest in the application checks this.)</li>
 * <li>For a negative Voice, MidiPlayer computes Voice % 16, a negative
 *     channel, which ShortMessage.setMessage documents as invalid (not
 *     tested here); this writer uses Math.floorMod, so Voice -1 is
 *     channel 15.</li>
 * <li>At one tick, offs of notes that started earlier come first, then
 *     ons, then offs of notes of duration 0, so a repeated key and a note
 *     of duration 0 both read back as written.</li>
 * </ul>
 */
public final class ScoreMidi {

    public static final int DEFAULT_RESOLUTION = 480;
    public static final int DEFAULT_TEMPO = 500000;

    private int  resolution = DEFAULT_RESOLUTION;
    private int  tempo = DEFAULT_TEMPO;
    private long shift = 0;

    public void setResolution(int ticksPerQuarter) {
        if (ticksPerQuarter <= 0 || ticksPerQuarter >= 0x8000) {
            throw new IllegalArgumentException("ticks per quarter note must be 1..32767: " + ticksPerQuarter);
        }
        this.resolution = ticksPerQuarter;
    }

    public void setTempo(int microsecondsPerQuarter) {
        if (microsecondsPerQuarter <= 0 || microsecondsPerQuarter > 0xFFFFFF) {
            throw new IllegalArgumentException("tempo out of range: " + microsecondsPerQuarter);
        }
        this.tempo = microsecondsPerQuarter;
    }

    /** Ticks added to every event by the last write; nonzero only when a note started before 0. */
    public long getShift() {
        return this.shift;
    }

    //
    // Reading
    //

    public static Denotator readScore(InputStream in) throws IOException {
        return ScoreNote.toScore(readNotes(StandardMidiFile.read(in)));
    }

    public static List<ScoreNote> readNotes(StandardMidiFile file) throws IOException {
        if (!file.isPPQ()) {
            throw new IOException("Only division type PPQ supported");
        }
        int tpq = file.getDivision();
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (StandardMidiFile.Track track : file.getTracks()) {
            Key[][] keys = new Key[16][128];
            List<Key> opened = new ArrayList<Key>();
            for (StandardMidiFile.Event e : track.events) {
                if (!e.isChannelMessage()) {
                    continue;
                }
                int command = e.command();
                if (command == 0x90 && e.data2() > 0) {
                    Key k = new Key(e.tick, e.data1(), e.data2(), e.channel());
                    keys[e.channel()][e.data1()] = k;
                    opened.add(k);
                }
                else if (command == 0x80 || command == 0x90) {
                    Key k = keys[e.channel()][e.data1()];
                    if (k != null) {
                        k.duration = e.tick - k.tick;
                    }
                }
            }
            for (Key k : opened) {
                notes.add(new ScoreNote((double)k.tick / tpq, k.key, k.velocity, (double)k.duration / tpq, k.channel));
            }
        }
        return notes;
    }

    private static final class Key {
        final long tick;
        final int key;
        final int velocity;
        final int channel;
        long duration;

        Key(long tick, int key, int velocity, int channel) {
            this.tick = tick;
            this.key = key;
            this.velocity = velocity;
            this.channel = channel;
        }
    }

    //
    // Writing
    //

    public void writeScore(Denotator score, OutputStream out) throws IOException {
        this.toFile(ScoreNote.fromScore(score)).write(out);
    }

    public StandardMidiFile toFile(List<ScoreNote> notes) {
        List<Pending> events = new ArrayList<Pending>(notes.size() * 2);
        long min = 0;
        for (ScoreNote n : notes) {
            long on = Math.round(n.onset * this.resolution);
            long off = on + Math.round(n.duration * this.resolution);
            if (off < on) {
                throw new IllegalArgumentException("negative duration: " + n);
            }
            int channel = Math.floorMod(n.voice, 16);
            int key = Math.max(0, Math.min(127, n.pitch.intValue()));
            int velocity = Math.max(1, Math.min(127, n.loudness));
            boolean zero = off == on;
            events.add(new Pending(on, 1, StandardMidiFile.Event.channel(0, 0x90, channel, key, velocity)));
            events.add(new Pending(off, zero ? 2 : 0, StandardMidiFile.Event.channel(0, 0x80, channel, key, 64)));
            min = Math.min(min, on);
        }
        this.shift = -min;
        Collections.sort(events, PENDING_ORDER);
        StandardMidiFile.Track conductor = new StandardMidiFile.Track();
        conductor.events.add(StandardMidiFile.Event.tempo(0, this.tempo));
        StandardMidiFile.Track track = new StandardMidiFile.Track();
        for (Pending p : events) {
            StandardMidiFile.Event e = p.event;
            track.events.add(new StandardMidiFile.Event(p.tick + this.shift, e.status, -1, e.data));
        }
        List<StandardMidiFile.Track> tracks = new ArrayList<StandardMidiFile.Track>(2);
        tracks.add(conductor);
        tracks.add(track);
        return new StandardMidiFile(1, this.resolution, tracks);
    }

    private static final class Pending {
        final long tick;
        final int  rank;
        final StandardMidiFile.Event event;

        Pending(long tick, int rank, StandardMidiFile.Event event) {
            this.tick = tick;
            this.rank = rank;
            this.event = event;
        }
    }

    private static final Comparator<Pending> PENDING_ORDER = new Comparator<Pending>() {
        public int compare(Pending a, Pending b) {
            int c = Long.compare(a.tick, b.tick);
            if (c == 0) c = Integer.compare(a.rank, b.rank);
            if (c == 0) c = Integer.compare(a.event.channel(), b.event.channel());
            if (c == 0) c = Integer.compare(a.event.data1(), b.event.data1());
            if (c == 0) c = Integer.compare(a.event.data2(), b.event.data2());
            return c;
        }
    };
}
