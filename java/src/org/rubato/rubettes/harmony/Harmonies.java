/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package org.rubato.rubettes.harmony;

import java.util.List;

import org.rubato.math.global.Interpretations;
import org.rubato.math.harmony.CadentialAnalysis;
import org.rubato.math.harmony.Cadences;
import org.rubato.math.harmony.ChordSymbol;
import org.rubato.math.harmony.Modulations;
import org.rubato.math.harmony.Modulations.Filter;
import org.rubato.math.harmony.Modulations.Rigidity;
import org.rubato.math.harmony.PitchClasses;
import org.rubato.math.harmony.Quantized;
import org.rubato.math.harmony.Tonality;

/**
 * Headless entry points for the harmony rubettes: the modulation quanta and
 * pivots from a tonality to its transposes (Topos I &sect;27.1, Def. 88;
 * Agust&iacute;n-Aquino and Mazzola 2019, Algorithm 1), and a cadential-region
 * report of a lead sheet (Agust&iacute;n-Aquino 2026, &sect;&sect;6&ndash;7).
 */
public final class Harmonies {

    /** The scales offered, by index. */
    public static final String[] SCALES = { "major", "harmonic minor", "melodic minor" };

    static final int[][] SCALE_CLASSES = { Interpretations.C_MAJOR, Interpretations.C_HARMONIC_MINOR,
                                          Interpretations.C_MELODIC_MINOR };

    static final String[] NOTE = { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };

    private Harmonies() { /* static utility */ }

    /** The settings of a modulation listing. */
    public static final class ModulationSettings {
        final int scale, tonic, chordSize, target;
        final Filter filter;
        final Rigidity rigidity;

        /**
         * @param scale     index into {@link #SCALES}
         * @param tonic     the source tonic class
         * @param chordSize 3 (triads) or 4 (seventh chords)
         * @param target    the transposition p = 1 to 11 of the target, or 0 for all
         * @param filter    COVERED keeps quanta whose trace the target's degree chords cover
         * @param rigidity  TI12 (literal reading) or AUT_X (a reconstruction; see HARMONY-SPEC 7.4)
         */
        public ModulationSettings(int scale, int tonic, int chordSize, int target, Filter filter, Rigidity rigidity) {
            if (scale < 0 || scale >= SCALES.length) {
                throw new IllegalArgumentException("Scale index must be 0 to " + (SCALES.length - 1));
            }
            if (chordSize != 3 && chordSize != 4) {
                throw new IllegalArgumentException("Chord size must be 3 or 4, got " + chordSize);
            }
            if (target < 0 || target > 11) {
                throw new IllegalArgumentException("Target must be 0 (all) or 1 to 11, got " + target);
            }
            this.scale = scale;
            this.tonic = Math.floorMod(tonic, PitchClasses.N);
            this.chordSize = chordSize;
            this.target = target;
            this.filter = filter;
            this.rigidity = rigidity;
        }

        /** C major, triads, all targets, covered, TI12. */
        public static ModulationSettings defaults() {
            return new ModulationSettings(0, 0, 3, 0, Filter.COVERED, Rigidity.TI12);
        }

        /** The source tonality. */
        public Tonality source() {
            return Tonality.of(SCALE_CLASSES[scale], 0, chordSize).transpose(tonic);
        }
    }

    /** The quantized modulations the settings select. */
    public static List<Quantized> modulations(ModulationSettings s) {
        Tonality src = s.source();
        if (s.target == 0) {
            return Modulations.all(src, s.filter, s.rigidity);
        }
        return Modulations.quantize(src, src.transpose(s.target), s.filter, s.rigidity);
    }

    /** A plain-text listing: the source's minimal cadential sets, then one line per quantum. */
    public static String modulationReport(ModulationSettings s) {
        Tonality src = s.source();
        String suffix = s.chordSize == 4 ? "7" : "";
        StringBuilder b = new StringBuilder();
        b.append(NOTE[s.tonic]).append(' ').append(SCALES[s.scale]).append(s.chordSize == 4 ? ", seventh chords" : ", triads")
         .append("; filter ").append(s.filter).append(", rigidity ").append(s.rigidity).append('\n');
        b.append("minimal cadential sets:");
        for (int m : Cadences.minimal(src)) {
            b.append(' ').append(degreeName(m, suffix));
        }
        b.append('\n');
        List<Quantized> qs = modulations(s);
        if (qs.isEmpty()) {
            b.append("no quantized modulation\n");
        }
        int lastP = -1;
        for (Quantized q : qs) {
            if (q.p() != lastP) {
                b.append("to ").append(NOTE[Math.floorMod(s.tonic + q.p(), 12)]).append(" (p = ").append(q.p()).append(")\n");
                lastP = q.p();
            }
            b.append("  cadence ").append(degreeName(q.cadence(), suffix))
             .append("  modulator ").append(Modulations.name(q.modulator()))
             .append("  quantum ").append(PitchClasses.toString(q.quantum()))
             .append("  pivots ").append(degreeName(q.pivots(), suffix))
             .append(q.covered() ? "" : "  (trace not covered)").append('\n');
        }
        return b.toString();
    }

    static String degreeName(int set, String suffix) {
        String n = Cadences.name(set);
        return suffix.isEmpty() ? n : n.replaceAll("([IV]+)", "$17");
    }

    /** A cadential-region report of chord bars ("F6 | Em7 A7b9 | ...") in a major key, for seventh chords. */
    public static String analysisReport(String bars, int tonic, boolean rootMode, int window) {
        if (bars == null || bars.trim().isEmpty()) {
            throw new IllegalArgumentException("No chords given");
        }
        if (window < 1) {
            throw new IllegalArgumentException("Window must be at least one bar");
        }
        List<List<ChordSymbol>> parsed = ChordSymbol.parseBars(bars);
        Tonality k = Tonality.of(Interpretations.C_MAJOR, 0, 4).transpose(tonic);
        return CadentialAnalysis.report(k, parsed, rootMode ? CadentialAnalysis.Mode.ROOT : CadentialAnalysis.Mode.LITERAL,
                                        Math.min(window, parsed.size()));
    }
}
