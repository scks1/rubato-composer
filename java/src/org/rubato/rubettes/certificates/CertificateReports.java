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

package org.rubato.rubettes.certificates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.rubato.io.ScoreNote;
import org.rubato.math.certificates.AgdaCertificates;
import org.rubato.math.certificates.AgdaCertificates.Certificate;
import org.rubato.math.certificates.AgdaCertificates.Mode;
import org.rubato.math.yoneda.Denotator;

/**
 * The report of the Agda Certificate rubette (sources/AGDA-CERTIFICATES-SPEC.md,
 * section 2 item 5): with a Score, whether its pitch-class set mod n is
 * all-interval (Flieder, arXiv 2512.04090, Ex. B.2) and whether its notes lie
 * in the key's scale (Ex. 2.4, lifted by D7); always, Ex. B.3 for the key.
 * Each verdict comes with its certificate, an Agda definition that
 * spec/agda/check-certificate.sh typechecks.
 */
public final class CertificateReports {

    private CertificateReports() { /* static utility */ }

    /** n of Z_n for the all-interval check, the key (tonic and mode), and the module name. */
    public static final class Settings {
        final int n;
        final int tonic;
        final String mode;
        final String moduleName;

        public Settings(int n, int tonic, String mode, String moduleName) {
            this.n = n;
            this.tonic = tonic;
            this.mode = mode;
            this.moduleName = moduleName;
        }

        /** Z12, C major, module Certificate. */
        public static Settings defaults() {
            return new Settings(12, 0, "major", "Certificate");
        }
    }

    /** The report text (verdicts, then the module), its one-line summary, and the module alone. */
    public static final class Report {
        private final String text;
        private final String summary;
        private final String module;

        Report(String text, String summary, String module) {
            this.text = text;
            this.summary = summary;
            this.module = module;
        }

        public String text() {
            return text;
        }

        public String summary() {
            return summary;
        }

        public String module() {
            return module;
        }
    }

    /**
     * Certifies the Score's properties; score may be null.
     *
     * @throws IllegalArgumentException for settings out of range, an unknown
     *         mode, a bad module name, a Score with an address, or a pitch
     *         that is not a whole semitone
     */
    public static Report analyze(Denotator score, Settings s) {
        if (s.n < 2 || s.n > AgdaCertificates.MAX_N) {
            throw new IllegalArgumentException("The modulus must be from 2 to " + AgdaCertificates.MAX_N + ", not " + s.n + ".");
        }
        if (s.tonic < 0 || s.tonic > 11) {
            throw new IllegalArgumentException("The tonic must be a pitch class from 0 to 11, not " + s.tonic + ".");
        }
        Mode m = Mode.parse(s.mode == null ? "" : s.mode);
        List<Certificate> cs = new ArrayList<Certificate>();
        StringBuilder b = new StringBuilder();
        b.append("Agda certificates (Flieder, Ex. 2.4, B.2 and B.3; spec D1 to D11)\n");
        if (score == null) {
            b.append("No Score linked: only the key is certified.\n");
        }
        else {
            if (!score.nullAddressed()) {
                throw new IllegalArgumentException("The score has address " + score.getAddress()
                        + "; Agda Certificate needs a zero-addressed Score.");
            }
            int[] pitches = pitches(ScoreNote.fromScore(score));
            int[] set = AgdaCertificates.pitchClassSet(s.n, pitches);
            int[] pcs = new int[pitches.length];
            for (int k = 0; k < pitches.length; k++) {
                pcs[k] = Math.floorMod(pitches[k], 12);
            }
            b.append("Score: ").append(pitches.length).append(pitches.length == 1 ? " note" : " notes")
             .append(", pitch-class set ").append(braces(set)).append(" in Z").append(s.n)
             .append(", pitch classes mod 12 ").append(Arrays.toString(pcs)).append('\n');
            cs.add(AgdaCertificates.allInterval("allInterval", s.n, set));
            cs.add(AgdaCertificates.scaleMembership("inScale", s.tonic, m, pcs));
        }
        cs.add(AgdaCertificates.dominantContainsLeadingTone("dominant", s.tonic, m));
        String module = AgdaCertificates.module(s.moduleName, cs);

        b.append('\n');
        StringBuilder summary = new StringBuilder();
        summary.append(cs.size()).append(cs.size() == 1 ? " certificate: " : " certificates: ");
        for (int k = 0; k < cs.size(); k++) {
            Certificate c = cs.get(k);
            b.append(c.name).append(": ").append(c.claim).append('\n');
            summary.append(k == 0 ? "" : ", ").append(c.name).append(c.holds ? " holds" : " fails");
        }
        b.append("\nCheck the module with spec/agda/check-certificate.sh ").append(s.moduleName).append(".agda\n\n");
        b.append(module);
        return new Report(b.toString(), summary.toString(), module);
    }

    /** D8: whole-semitone pitches in onset order, repeats kept. */
    static int[] pitches(List<ScoreNote> notes) {
        List<ScoreNote> sorted = new ArrayList<ScoreNote>(notes);
        Collections.sort(sorted);
        int[] p = new int[sorted.size()];
        for (int k = 0; k < p.length; k++) {
            ScoreNote n = sorted.get(k);
            if (!n.pitch.isIntegral()) {
                throw new IllegalArgumentException("The pitch " + n.pitch + " at onset " + n.onset + " is not a whole semitone.");
            }
            p[k] = n.pitch.intValue();
        }
        return p;
    }

    private static String braces(int[] xs) {
        return "{" + Arrays.toString(xs).replace("[", "").replace("]", "") + "}";
    }
}
