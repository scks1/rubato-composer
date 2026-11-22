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

package org.rubato.math.counterpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rubato.math.arith.NumberTheory;

/**
 * Checks a first-species line, one discantus tone against each cantus
 * firmus tone, against the admitted successors of a strong dichotomy.
 * <p>
 * Pitches are integers (MIDI numbers or any other semitone count) and are
 * reduced mod n, as the model works in Z_n (CCW section 3.3, p. 35). The
 * interval of a pair is the discantus minus the cantus firmus for the
 * sweeping orientation and the cantus firmus minus the discantus for the
 * hanging one (CCW section 2.2.1, p. 13).
 */
public final class FirstSpecies {

    /** How the interval of a pair is read from its two pitches. */
    public enum Orientation {
        /** Discantus = x + y, so y = discantus &minus; cantus. */
        SWEEPING,
        /** Discantus = x &minus; y, so y = cantus &minus; discantus. */
        HANGING
    }

    /** What the check found at one position. */
    public enum Kind {
        /** The interval is consonant and, after the first position, an admitted successor. */
        ADMITTED,
        /** The interval is a dissonance of the dichotomy. */
        DISSONANT,
        /** Both intervals are consonant but this one is not an admitted successor of the previous one. */
        FORBIDDEN,
        /** The interval is consonant but the previous one is dissonant, so no successor rule applies. */
        UNCHECKED
    }

    /** One position of the line. */
    public static final class Step {

        public final int index;
        public final int cantus;
        public final int discantus;
        /** The counterpoint interval x + &epsilon;.y with x = cantus mod n. */
        public final int x;
        public final int y;
        public final Kind kind;

        Step(int index, int cantus, int discantus, int x, int y, Kind kind) {
            this.index = index;
            this.cantus = cantus;
            this.discantus = discantus;
            this.x = x;
            this.y = y;
            this.kind = kind;
        }

        @Override
        public String toString() {
            return (index + 1) + ": " + cantus + "/" + discantus + " = " + x + "+\u03b5." + y + " " + kind;
        }
    }

    private FirstSpecies() { /* static utility */ }

    /**
     * Checks the line.
     *
     * @param d a strong dichotomy, for example {@link StrongDichotomies#FUX}
     * @param cantus cantus firmus pitches
     * @param discantus discantus pitches, one per cantus firmus pitch
     * @throws IllegalArgumentException if the arrays differ in length or the dichotomy is not strong
     */
    public static List<Step> check(Dichotomy d, Orientation o, int[] cantus, int[] discantus) {
        if (cantus.length != discantus.length) {
            throw new IllegalArgumentException("First species needs one discantus tone per cantus firmus tone: "
                    + cantus.length + " against " + discantus.length);
        }
        d.polarity();
        int n = d.getModulus();
        List<Step> steps = new ArrayList<Step>();
        for (int i = 0; i < cantus.length; i++) {
            int x = NumberTheory.mod(cantus[i], n);
            int y = NumberTheory.mod(o == Orientation.SWEEPING ? discantus[i] - cantus[i] : cantus[i] - discantus[i], n);
            Kind kind;
            if (!d.isConsonant(y)) {
                kind = Kind.DISSONANT;
            }
            else if (i == 0) {
                kind = Kind.ADMITTED;
            }
            else {
                Step prev = steps.get(i - 1);
                if (prev.kind == Kind.DISSONANT) {
                    kind = Kind.UNCHECKED;
                }
                else {
                    kind = ContrapuntalSymmetries.isAdmitted(d, prev.x, prev.y, x, y) ? Kind.ADMITTED : Kind.FORBIDDEN;
                }
            }
            steps.add(new Step(i, cantus[i], discantus[i], x, y, kind));
        }
        return Collections.unmodifiableList(steps);
    }

    /** True if every position is {@link Kind#ADMITTED}. */
    public static boolean isCorrect(List<Step> steps) {
        for (Step s : steps) {
            if (s.kind != Kind.ADMITTED) {
                return false;
            }
        }
        return true;
    }
}
