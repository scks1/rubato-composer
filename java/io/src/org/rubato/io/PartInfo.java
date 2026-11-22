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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * The name and MIDI settings of one voice, which ScoreMidi and
 * MusicXMLWriter both take, so one value configures both writers.
 * <p>
 * Program, pan and volume use MIDI's units: program 0 to 127 (General MIDI
 * program n is written as program n-1 here, as in the Program Change
 * message), pan 0 to 127 with 64 as the center (controller 10), volume 0 to
 * 127 (controller 7). {@link #UNSET} leaves a setting out. The name is the
 * part and instrument name in MusicXML; null means the writer's default,
 * "Voice n". Instances are immutable; the {@code with} methods return
 * changed copies.
 */
public final class PartInfo {

    /** The value of a setting that is not given. */
    public static final int UNSET = -1;

    /** A part with no name and no MIDI settings. */
    public static final PartInfo NONE = new PartInfo(null, UNSET, UNSET, UNSET);

    private final String name;
    private final int program;
    private final int pan;
    private final int volume;

    /**
     * @param name the part name, or null for the writer's default
     * @param program 0 to 127, or {@link #UNSET}
     * @param pan 0 to 127 (64 is the center), or {@link #UNSET}
     * @param volume 0 to 127, or {@link #UNSET}
     */
    public PartInfo(String name, int program, int pan, int volume) {
        this.name = name;
        this.program = check("program", program);
        this.pan = check("pan", pan);
        this.volume = check("volume", volume);
    }

    /** A part with the given name and General MIDI program (0 to 127). */
    public static PartInfo of(String name, int program) {
        return new PartInfo(name, program, UNSET, UNSET);
    }

    /** A part with the given name, program and pan. */
    public static PartInfo of(String name, int program, int pan) {
        return new PartInfo(name, program, pan, UNSET);
    }

    private static int check(String what, int value) {
        if (value != UNSET && (value < 0 || value > 127)) {
            throw new IllegalArgumentException(what + " must be 0..127 or UNSET: " + value);
        }
        return value;
    }

    public String getName() { return this.name; }
    public int getProgram() { return this.program; }
    public int getPan() { return this.pan; }
    public int getVolume() { return this.volume; }

    public boolean hasProgram() { return this.program != UNSET; }
    public boolean hasPan() { return this.pan != UNSET; }
    public boolean hasVolume() { return this.volume != UNSET; }

    /** Whether any MIDI setting is given. */
    public boolean hasMidiSettings() {
        return this.hasProgram() || this.hasPan() || this.hasVolume();
    }

    public PartInfo withName(String name) { return new PartInfo(name, this.program, this.pan, this.volume); }
    public PartInfo withProgram(int program) { return new PartInfo(this.name, program, this.pan, this.volume); }
    public PartInfo withPan(int pan) { return new PartInfo(this.name, this.program, pan, this.volume); }
    public PartInfo withVolume(int volume) { return new PartInfo(this.name, this.program, this.pan, volume); }

    /**
     * MusicXML's pan in degrees for a MIDI pan: (pan - 64) * 90 / 64, so 0
     * is -90 (hard left), 64 is 0 and 127 is 88.59375. The value is exact,
     * and {@link #panFromDegrees} maps it back to the same pan.
     */
    public static BigDecimal panDegrees(int pan) {
        return BigDecimal.valueOf(pan - 64L).multiply(BigDecimal.valueOf(90)).divide(BigDecimal.valueOf(64)).stripTrailingZeros();
    }

    /**
     * The MIDI pan for MusicXML's pan in degrees, the inverse of
     * {@link #panDegrees}: degrees * 64 / 90 + 64, rounded and clamped to
     * 0..127. MusicXML allows -180 to 180; values past -90 and 90 (behind
     * the listener) clamp to the hard left and right.
     */
    public static int panFromDegrees(BigDecimal degrees) {
        BigDecimal v = degrees.multiply(BigDecimal.valueOf(64)).divide(BigDecimal.valueOf(90), 0, RoundingMode.HALF_EVEN).add(BigDecimal.valueOf(64));
        return Math.max(0, Math.min(127, v.intValue()));
    }

    /** MusicXML's volume, a percentage of the maximum, for a MIDI volume: volume * 100 / 127 to 4 places. */
    public static BigDecimal volumePercent(int volume) {
        return BigDecimal.valueOf(volume * 100L).divide(BigDecimal.valueOf(127), 4, RoundingMode.HALF_EVEN).stripTrailingZeros();
    }

    /** The MIDI volume for MusicXML's volume percentage, rounded and clamped to 0..127. */
    public static int volumeFromPercent(BigDecimal percent) {
        BigDecimal v = percent.multiply(BigDecimal.valueOf(127)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_EVEN);
        return Math.max(0, Math.min(127, v.intValue()));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PartInfo)) {
            return false;
        }
        PartInfo p = (PartInfo)o;
        return Objects.equals(this.name, p.name) && this.program == p.program && this.pan == p.pan && this.volume == p.volume;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.program, this.pan, this.volume);
    }

    @Override
    public String toString() {
        return "PartInfo[name=" + this.name + ",program=" + this.program + ",pan=" + this.pan + ",volume=" + this.volume + "]";
    }
}
