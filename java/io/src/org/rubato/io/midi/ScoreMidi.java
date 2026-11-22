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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.rubato.io.PartInfo;
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
 * the key, Loudness the velocity and Voice the channel. Tempo and track
 * numbers are not read; programs and controllers are not part of the
 * notes, and {@link #readPrograms}, {@link #readControllers} and
 * {@link #readPart} read them separately.
 * <p>
 * Writing follows the application's MidiPlayer (used by MidiFileOutRubette)
 * except where noted: a format 1 file with 480 ticks per quarter note, a
 * first track holding a Set Tempo of 500000 microseconds per quarter note
 * (120 quarter notes per minute), and a second track holding the notes,
 * channel = Voice mod 16, key = Pitch rounded to the nearest key (halves
 * up, by Rational.intValue, which calls Math.round) and clamped to 0..127,
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
 *     writer adds only the programs and controllers set with
 *     {@link #setProgram}, {@link #setController}, {@link #setPan},
 *     {@link #setVolume} or {@link #setPart}, at tick 0 at the start of
 *     the note track: channel by channel, the program first, then the
 *     controllers in the order they were first set. (MidiPlayer's code
 *     also adds a System Real-Time Stop 1500 ticks after the last event,
 *     but MidiSystem.write leaves it out of the file; MidiParityTest in
 *     the application checks this.)</li>
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

    /** Controller numbers of pan and channel volume (MIDI 1.0). */
    public static final int PAN = 10, VOLUME = 7;

    private int  resolution = DEFAULT_RESOLUTION;
    private int  tempo = DEFAULT_TEMPO;
    private long shift = 0;
    /** Program per channel; channels without one are absent. */
    private final TreeMap<Integer, Integer> programs = new TreeMap<Integer, Integer>();
    /** Controller values per channel, in the order they were first set. */
    private final TreeMap<Integer, LinkedHashMap<Integer, Integer>> controllers = new TreeMap<Integer, LinkedHashMap<Integer, Integer>>();

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

    /**
     * Sets the program of a channel (0 to 15) to a General MIDI program
     * number minus 1 (0 to 127), written as a Program Change at tick 0.
     * {@link PartInfo#UNSET} removes it.
     */
    public void setProgram(int channel, int program) {
        checkChannel(channel);
        if (program == PartInfo.UNSET) {
            this.programs.remove(channel);
            return;
        }
        if (program < 0 || program > 127) {
            throw new IllegalArgumentException("program must be 0..127: " + program);
        }
        this.programs.put(channel, program);
    }

    /** Sets the pan of a channel, controller 10, 0 (left) to 127 (right) with 64 the center. */
    public void setPan(int channel, int pan) {
        this.setController(channel, PAN, pan);
    }

    /** Sets the channel volume, controller 7, 0 to 127. */
    public void setVolume(int channel, int volume) {
        this.setController(channel, VOLUME, volume);
    }

    /**
     * Sets a controller (0 to 119; 120 to 127 are channel mode messages)
     * of a channel to a value (0 to 127), written as a Control Change at
     * tick 0. {@link PartInfo#UNSET} removes it. Controllers are written in
     * the order they were first set.
     */
    public void setController(int channel, int controller, int value) {
        checkChannel(channel);
        if (controller < 0 || controller > 119) {
            throw new IllegalArgumentException("controller must be 0..119: " + controller);
        }
        LinkedHashMap<Integer, Integer> map = this.controllers.get(channel);
        if (value == PartInfo.UNSET) {
            if (map != null) {
                map.remove(controller);
            }
            return;
        }
        if (value < 0 || value > 127) {
            throw new IllegalArgumentException("controller value must be 0..127: " + value);
        }
        if (map == null) {
            map = new LinkedHashMap<Integer, Integer>();
            this.controllers.put(channel, map);
        }
        map.put(controller, value);
    }

    /**
     * Applies the program, pan and volume of a part (those it gives) to the
     * channel of a Voice, Voice mod 16 as the notes use. The name is not
     * written: all notes share one track.
     */
    public void setPart(int voice, PartInfo part) {
        int channel = Math.floorMod(voice, 16);
        if (part.hasProgram()) {
            this.setProgram(channel, part.getProgram());
        }
        if (part.hasPan()) {
            this.setPan(channel, part.getPan());
        }
        if (part.hasVolume()) {
            this.setVolume(channel, part.getVolume());
        }
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel > 15) {
            throw new IllegalArgumentException("channel must be 0..15: " + channel);
        }
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

    /**
     * The first program of each channel in a file: index = channel,
     * {@link PartInfo#UNSET} where the file has no Program Change on it.
     * "First" means the earliest tick over all tracks, the earlier track
     * at equal ticks.
     */
    public static int[] readPrograms(StandardMidiFile file) {
        int[] programs = new int[16];
        long[] ticks = new long[16];
        java.util.Arrays.fill(programs, PartInfo.UNSET);
        for (StandardMidiFile.Track track : file.getTracks()) {
            for (StandardMidiFile.Event e : track.events) {
                if (e.isChannelMessage() && e.command() == 0xC0) {
                    int c = e.channel();
                    if (programs[c] == PartInfo.UNSET || e.tick < ticks[c]) {
                        programs[c] = e.data1();
                        ticks[c] = e.tick;
                    }
                }
            }
        }
        return programs;
    }

    /**
     * The first value of each controller on a channel in a file, by
     * controller number, "first" as in {@link #readPrograms}.
     */
    public static Map<Integer, Integer> readControllers(StandardMidiFile file, int channel) {
        checkChannel(channel);
        TreeMap<Integer, Integer> values = new TreeMap<Integer, Integer>();
        Map<Integer, Long> ticks = new HashMap<Integer, Long>();
        for (StandardMidiFile.Track track : file.getTracks()) {
            for (StandardMidiFile.Event e : track.events) {
                if (e.isChannelMessage() && e.command() == 0xB0 && e.channel() == channel) {
                    Long t = ticks.get(e.data1());
                    if (t == null || e.tick < t) {
                        values.put(e.data1(), e.data2());
                        ticks.put(e.data1(), e.tick);
                    }
                }
            }
        }
        return values;
    }

    /** The first program, pan and volume of a channel in a file, with no name. */
    public static PartInfo readPart(StandardMidiFile file, int channel) {
        Map<Integer, Integer> c = readControllers(file, channel);
        return new PartInfo(null, readPrograms(file)[channel],
                c.containsKey(PAN) ? c.get(PAN) : PartInfo.UNSET,
                c.containsKey(VOLUME) ? c.get(VOLUME) : PartInfo.UNSET);
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
        // channel setup at tick 0, before the notes: per channel, the program, then the controllers
        TreeMap<Integer, Boolean> channels = new TreeMap<Integer, Boolean>();
        for (int c : this.programs.keySet()) channels.put(c, Boolean.TRUE);
        for (int c : this.controllers.keySet()) channels.put(c, Boolean.TRUE);
        for (int c : channels.keySet()) {
            Integer program = this.programs.get(c);
            if (program != null) {
                track.events.add(StandardMidiFile.Event.channel(0, 0xC0, c, program, 0));
            }
            Map<Integer, Integer> values = this.controllers.get(c);
            if (values != null) {
                for (Map.Entry<Integer, Integer> v : values.entrySet()) {
                    track.events.add(StandardMidiFile.Event.channel(0, 0xB0, c, v.getKey(), v.getValue()));
                }
            }
        }
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
