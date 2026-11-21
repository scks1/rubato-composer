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

package org.rubato.io;

import static org.rubato.logeo.DenoFactory.makeDenotator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rubato.base.Repository;
import org.rubato.math.arith.Rational;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.FactorDenotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.SimpleDenotator;

/**
 * One note of a denotator of the built-in form Score, which
 * {@link Repository} registers as a power of Note, the limit
 * Onset x Pitch x Loudness x Duration x Voice, with
 * Onset and Duration in R, Pitch in Q, and Loudness and Voice in Z.
 * <p>
 * The file formats in this library share the time unit of the MIDI code in
 * the application (MidiReader, MidiChange): Onset and Duration count quarter
 * notes, Pitch is the MIDI key number (60 is middle C), Loudness is the MIDI
 * velocity and Voice is the MIDI channel.
 */
public final class ScoreNote implements Comparable<ScoreNote> {

    public final double   onset;
    public final Rational pitch;
    public final int      loudness;
    public final double   duration;
    public final int      voice;

    public ScoreNote(double onset, Rational pitch, int loudness, double duration, int voice) {
        if (pitch == null) {
            throw new IllegalArgumentException("pitch is null");
        }
        this.onset = onset;
        this.pitch = pitch;
        this.loudness = loudness;
        this.duration = duration;
        this.voice = voice;
    }

    public ScoreNote(double onset, int pitch, int loudness, double duration, int voice) {
        this(onset, new Rational(pitch), loudness, duration, voice);
    }

    /**
     * Returns the notes of a denotator of form Score, sorted by onset,
     * pitch, voice, duration and loudness.
     *
     * @throws IllegalArgumentException if the denotator is not of form Score
     */
    public static List<ScoreNote> fromScore(Denotator score) {
        if (score == null || !score.hasForm(scoreForm())) {
            throw new IllegalArgumentException("The denotator is not of form Score.");
        }
        List<ScoreNote> notes = new ArrayList<ScoreNote>();
        for (Denotator d : ((FactorDenotator)score).getFactors()) {
            notes.add(fromNote(d));
        }
        Collections.sort(notes);
        return notes;
    }

    /**
     * Reads one denotator of form Note.
     */
    public static ScoreNote fromNote(Denotator note) {
        FactorDenotator f = (FactorDenotator)note;
        double   o = ((SimpleDenotator)f.getFactor(0)).getReal();
        Rational p = ((SimpleDenotator)f.getFactor(1)).getRational();
        int      l = ((SimpleDenotator)f.getFactor(2)).getInteger();
        double   d = ((SimpleDenotator)f.getFactor(3)).getReal();
        int      v = ((SimpleDenotator)f.getFactor(4)).getInteger();
        return new ScoreNote(o, p, l, d, v);
    }

    /**
     * Builds a denotator of form Note, as MidiReader does.
     */
    public Denotator toNote() {
        Repository rep = Repository.systemRepository();
        List<Denotator> factors = new ArrayList<Denotator>(5);
        factors.add(makeDenotator(rep.getForm("Onset"), this.onset));
        factors.add(makeDenotator(rep.getForm("Pitch"), this.pitch));
        factors.add(makeDenotator(rep.getForm("Loudness"), this.loudness));
        factors.add(makeDenotator(rep.getForm("Duration"), this.duration));
        factors.add(makeDenotator(rep.getForm("Voice"), this.voice));
        return makeDenotator(rep.getForm("Note"), factors);
    }

    /**
     * Builds a denotator of form Score from notes. Score is a power form, so
     * equal notes count once.
     */
    public static Denotator toScore(List<ScoreNote> notes) {
        List<Denotator> list = new ArrayList<Denotator>(notes.size());
        for (ScoreNote n : notes) {
            list.add(n.toNote());
        }
        return makeDenotator(scoreForm(), list);
    }

    public static Form scoreForm() {
        return Repository.systemRepository().getForm("Score");
    }

    public int compareTo(ScoreNote o) {
        int c = Double.compare(this.onset, o.onset);
        if (c == 0) c = this.pitch.compareTo(o.pitch);
        if (c == 0) c = Integer.compare(this.voice, o.voice);
        if (c == 0) c = Double.compare(this.duration, o.duration);
        if (c == 0) c = Integer.compare(this.loudness, o.loudness);
        return c;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ScoreNote && this.compareTo((ScoreNote)other) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.onset) * 31 + this.pitch.hashCode() * 7 + this.voice;
    }

    @Override
    public String toString() {
        return "ScoreNote[onset=" + this.onset + ",pitch=" + this.pitch + ",loudness=" + this.loudness
            + ",duration=" + this.duration + ",voice=" + this.voice + "]";
    }
}
