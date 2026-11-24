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

package org.rubato.math.certificates.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.certificates.AgdaCertificates;
import org.rubato.math.certificates.AgdaCertificates.Certificate;
import org.rubato.math.certificates.AgdaCertificates.Mode;

/**
 * Agda certificates for the properties of Flieder's Examples 2.4, 2.6, B.2 and
 * B.3 (arXiv 2512.04090; sources/AGDA-CERTIFICATES-SPEC.md). The expected values
 * come from the separate Python probe (spec section 4, P01-python-probe.txt).
 */
public class AgdaCertificatesTest extends TestCase {

    private static final int[] AI = { 0, 1, 4, 6 };
    private static final int[] CLUSTER = { 0, 1, 2, 3 };

    /** pcint(x, y) = y - x mod n, and intclass folds it to min(d, n - d) (Ex. B.2, D2). */
    public void testIntervals() {
        assertEquals(1, AgdaCertificates.pcint(12, 0, 1));
        assertEquals(11, AgdaCertificates.pcint(12, 1, 0));
        assertEquals(0, AgdaCertificates.pcint(12, 5, 5));
        assertEquals(1, AgdaCertificates.intervalClass(12, 1, 0));
        assertEquals(6, AgdaCertificates.intervalClass(12, 0, 6));
        assertEquals(5, AgdaCertificates.intervalClass(12, 0, 7));
        assertEquals(3, AgdaCertificates.intervalClass(7, 0, 4));
        assertEquals(6, AgdaCertificates.half(12));
        assertEquals(3, AgdaCertificates.half(7));
        assertEquals(1, AgdaCertificates.half(2));
    }

    /** The pitch-class set of some pitches: distinct, ascending, mod n (D1, D8). */
    public void testPitchClassSet() {
        assertEquals("[0, 4, 7]", Arrays.toString(AgdaCertificates.pitchClassSet(12, 60, 64, 67, 72, -5)));
        assertEquals("[]", Arrays.toString(AgdaCertificates.pitchClassSet(12)));
        assertEquals("[1, 3]", Arrays.toString(AgdaCertificates.pitchClassSet(7, 8, 10, 1)));
    }

    /** Ex. B.2: {0,1,4,6} and {0,1,3,7} are all-interval in Z12; {0,1,2,3} misses 4, 5, 6. */
    public void testAllInterval() {
        assertTrue(AgdaCertificates.isAllInterval(12, AI));
        assertTrue(AgdaCertificates.isAllInterval(12, new int[] { 0, 1, 3, 7 }));
        assertFalse(AgdaCertificates.isAllInterval(12, CLUSTER));
        assertEquals(Arrays.asList(4, 5, 6), AgdaCertificates.missingClasses(12, CLUSTER));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6), AgdaCertificates.missingClasses(12, new int[0]));
        assertTrue(AgdaCertificates.isAllInterval(7, new int[] { 0, 1, 3 }));
    }

    /** NOTES.md and the probe: 48 all-interval tetrachords in Z12, 3002 all-interval subsets in all. */
    public void testCounts() {
        int tetra = 0;
        int all = 0;
        for (int mask = 0; mask < 4096; mask++) {
            int[] c = members(mask);
            if (AgdaCertificates.isAllInterval(12, c)) {
                all++;
                if (c.length == 4) {
                    tetra++;
                }
            }
        }
        assertEquals(48, tetra);
        assertEquals(3002, all);
    }

    private static int[] members(int mask) {
        int[] c = new int[Integer.bitCount(mask)];
        int k = 0;
        for (int i = 0; i < 12; i++) {
            if ((mask & (1 << i)) != 0) {
                c[k++] = i;
            }
        }
        return c;
    }

    /** D5: the three modes listed from the tonic; triad(k, d) stacks every other note (Ex. 2.3). */
    public void testScales() {
        assertEquals("[0, 2, 4, 5, 7, 9, 11]", Arrays.toString(AgdaCertificates.scale(0, Mode.MAJOR)));
        assertEquals("[9, 11, 0, 2, 4, 5, 8]", Arrays.toString(AgdaCertificates.scale(9, Mode.HARMONIC_MINOR)));
        assertEquals("[9, 11, 0, 2, 4, 5, 7]", Arrays.toString(AgdaCertificates.scale(9, Mode.NATURAL_MINOR)));
        assertEquals(0, AgdaCertificates.degree(0, Mode.MAJOR, 1));
        assertEquals(11, AgdaCertificates.degree(0, Mode.MAJOR, 7));
        assertEquals(2, AgdaCertificates.degree(0, Mode.MAJOR, 9));
        assertEquals("[7, 11, 2]", Arrays.toString(AgdaCertificates.triad(0, Mode.MAJOR, 5)));
        assertEquals("[4, 8, 11]", Arrays.toString(AgdaCertificates.triad(9, Mode.HARMONIC_MINOR, 5)));
        assertEquals("[4, 7, 11]", Arrays.toString(AgdaCertificates.triad(9, Mode.NATURAL_MINOR, 5)));
        assertEquals("[2, 6, 9]", Arrays.toString(AgdaCertificates.triad(7, Mode.MAJOR, 5)));
        assertEquals(Mode.HARMONIC_MINOR, Mode.parse("harmonic"));
        assertEquals(Mode.NATURAL_MINOR, Mode.parse("Natural minor"));
        assertEquals(Mode.MAJOR, Mode.parse("MAJOR"));
    }

    /** Ex. 2.6: degree 7 a semitone below degree 1; the leading tone is the tonic minus one. */
    public void testLeadingTone() {
        assertEquals(11, AgdaCertificates.leadingTone(0));
        assertEquals(8, AgdaCertificates.leadingTone(9));
        assertTrue(AgdaCertificates.containsLeadingTone(0, Mode.MAJOR));
        assertTrue(AgdaCertificates.containsLeadingTone(9, Mode.HARMONIC_MINOR));
        assertFalse(AgdaCertificates.containsLeadingTone(9, Mode.NATURAL_MINOR));
        int ok = 0;
        int vacuous = 0;
        for (int t = 0; t < 12; t++) {
            for (Mode m : Mode.values()) {
                if (!AgdaCertificates.containsLeadingTone(t, m)) {
                    vacuous++;
                }
                else if (contains(AgdaCertificates.triad(t, m, 5), AgdaCertificates.leadingTone(t))) {
                    ok++;
                }
            }
        }
        assertEquals(24, ok);
        assertEquals(12, vacuous);
    }

    private static boolean contains(int[] c, int p) {
        for (int x : c) {
            if (x == p) {
                return true;
            }
        }
        return false;
    }

    /** Membership proofs count the position from the head of the list. */
    public void testMembershipTerm() {
        assertEquals("here", AgdaCertificates.membership(0));
        assertEquals("there (there here)", AgdaCertificates.membership(2));
    }

    /** The positive certificate of Ex. B.2: the first pair per class, then the absurd clause. */
    public void testAllIntervalCertificate() {
        Certificate c = AgdaCertificates.allInterval("ai", 12, AI);
        assertTrue(c.holds);
        String a = c.agda;
        assertTrue(a, a.contains("ai : AllInterval 12 (0 ∷ 1 ∷ 4 ∷ 6 ∷ [])\n"));
        assertTrue(a, a.contains("ai 0 _ = 0 , 0 , here , here , refl\n"));
        assertTrue(a, a.contains("ai 1 _ = 0 , 1 , here , there here , refl\n"));
        assertTrue(a, a.contains("ai 2 _ = 4 , 6 , there (there here) , there (there (there here)) , refl\n"));
        assertTrue(a, a.contains("ai 5 _ = 1 , 6 , there here , there (there (there here)) , refl\n"));
        assertTrue(a, a.contains("ai (suc (suc (suc (suc (suc (suc (suc i))))))) (s≤s (s≤s (s≤s (s≤s (s≤s (s≤s ()))))))\n"));
        assertEquals(9, count(a, "\nai "));
        assertEquals("{0, 1, 4, 6} is all-interval in Z12", c.claim);
    }

    /** D4: the smallest missing class and one absurd clause per ordered pair. */
    public void testNotAllIntervalCertificate() {
        Certificate c = AgdaCertificates.allInterval("na", 12, CLUSTER);
        assertFalse(c.holds);
        String a = c.agda;
        assertTrue(a, a.contains("na : ¬ AllInterval 12 (0 ∷ 1 ∷ 2 ∷ 3 ∷ [])\n"));
        assertTrue(a, a.contains("na f = miss (f 4 (s≤s (s≤s (s≤s (s≤s z≤n)))))\n"));
        assertTrue(a, a.contains("  miss : ¬ Hit 12 (0 ∷ 1 ∷ 2 ∷ 3 ∷ []) 4\n"));
        assertEquals(16, count(a, "\n  miss ("));
        assertTrue(a, a.contains("  miss (_ , _ , there (there (there here)) , there here , ())\n"));
        assertEquals("{0, 1, 2, 3} is not all-interval in Z12: no pair has interval class 4", c.claim);
        Certificate e = AgdaCertificates.allInterval("none", 12, new int[0]);
        assertFalse(e.holds);
        assertTrue(e.agda, e.agda.contains("none f = miss (f 0 z≤n)\n"));
        assertTrue(e.agda, e.agda.contains("  miss (_ , _ , () , _)\n"));
    }

    /** Ex. B.3 under D6: a proof for major and harmonic minor, the vacuous proof for natural minor. */
    public void testDominantCertificate() {
        Certificate c = AgdaCertificates.dominantContainsLeadingTone("dC", 0, Mode.MAJOR);
        assertTrue(c.holds);
        assertTrue(c.agda, c.agda.contains("dC : DominantContainsLeadingTone 0 major\ndC (_ , refl , _) = there here\n"));
        assertEquals("In C major the dominant triad {G, B, D} contains the leading tone B", c.claim);
        Certificate h = AgdaCertificates.dominantContainsLeadingTone("dA", 9, Mode.HARMONIC_MINOR);
        assertTrue(h.agda, h.agda.contains("dA : DominantContainsLeadingTone 9 harmonicMinor\ndA (_ , refl , _) = there here\n"));
        Certificate v = AgdaCertificates.dominantContainsLeadingTone("dN", 9, Mode.NATURAL_MINOR);
        assertTrue(v.holds);
        assertTrue(v.agda, v.agda.contains("dN : DominantContainsLeadingTone 9 naturalMinor\ndN (_ , _ , ())\n"));
        assertEquals("In A natural minor degree 7 (G) is not a leading tone, so no chord has dominant function"
                   + " and the claim holds vacuously", v.claim);
    }

    /** Ex. 2.4 lifted to a list of pitch classes (D7). */
    public void testMembershipCertificate() {
        Certificate c = AgdaCertificates.scaleMembership("m", 0, Mode.MAJOR, new int[] { 0, 4, 7, 11 });
        assertTrue(c.holds);
        assertTrue(c.agda, c.agda.contains("m : All (λ p → p ∈ scale 0 major) (0 ∷ 4 ∷ 7 ∷ 11 ∷ [])\n"
                                         + "m = here ∷ there (there here) ∷ there (there (there (there here)))"
                                         + " ∷ there (there (there (there (there (there here))))) ∷ []\n"));
        assertEquals("All 4 pitch classes lie in C major", c.claim);
        Certificate n = AgdaCertificates.scaleMembership("n", 0, Mode.MAJOR, new int[] { 0, 6, 1 });
        assertFalse(n.holds);
        assertTrue(n.agda, n.agda.contains("n : ¬ All (λ p → p ∈ scale 0 major) (0 ∷ 6 ∷ 1 ∷ [])\n"
                                         + "n (_ ∷ p ∷ _) = out p\n  where\n  out : ¬ (6 ∈ scale 0 major)\n"
                                         + "  out (there (there (there (there (there (there (there ())))))))\n"));
        assertEquals("Pitch class 6 (note 2) lies outside C major", n.claim);
        Certificate e = AgdaCertificates.scaleMembership("e", 0, Mode.MAJOR, new int[0]);
        assertTrue(e.holds);
        assertTrue(e.agda, e.agda.contains("e = []\n"));
    }

    /** Bad input is refused with a message. */
    public void testRefusals() {
        assertRefused(() -> AgdaCertificates.allInterval("x", 1, new int[0]));
        assertRefused(() -> AgdaCertificates.allInterval("x", 25, new int[0]));
        assertRefused(() -> AgdaCertificates.allInterval("x", 12, new int[] { 1, 0 }));
        assertRefused(() -> AgdaCertificates.allInterval("x", 12, new int[] { 3, 3 }));
        assertRefused(() -> AgdaCertificates.allInterval("x", 12, new int[] { 12 }));
        assertRefused(() -> AgdaCertificates.dominantContainsLeadingTone("x", 12, Mode.MAJOR));
        assertRefused(() -> AgdaCertificates.scaleMembership("x", 0, Mode.MAJOR, new int[] { -1 }));
        assertRefused(() -> AgdaCertificates.allInterval("Upper", 12, AI));
        assertRefused(() -> AgdaCertificates.allInterval("scale", 12, AI));
        assertRefused(() -> AgdaCertificates.allInterval("a-b", 12, AI));
        assertRefused(() -> AgdaCertificates.module("lower", new ArrayList<Certificate>()));
        assertRefused(() -> AgdaCertificates.module("Prelude", new ArrayList<Certificate>()));
        assertRefused(() -> Mode.parse("dorian"));
        List<Certificate> twice = new ArrayList<Certificate>();
        twice.add(AgdaCertificates.allInterval("a", 12, AI));
        twice.add(AgdaCertificates.allInterval("a", 12, CLUSTER));
        assertRefused(() -> AgdaCertificates.module("Twice", twice));
    }

    private static void assertRefused(Runnable r) {
        try {
            r.run();
            fail("no exception");
        }
        catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /** The module header imports Prelude and Certificates and names the module. */
    public void testModule() {
        List<Certificate> cs = new ArrayList<Certificate>();
        cs.add(AgdaCertificates.allInterval("ai", 12, AI));
        String m = AgdaCertificates.module("MyCert", cs);
        assertTrue(m, m.startsWith("{-# OPTIONS --safe --without-K #-}\n"));
        assertTrue(m, m.contains("\nmodule MyCert where\n\nopen import Prelude\nopen import Certificates\n"));
        assertTrue(m, m.contains("\n-- {0, 1, 4, 6} is all-interval in Z12\nai : AllInterval"));
        assertTrue(m, m.endsWith("\n"));
    }

    /** spec/agda/GeneratedCertificates.agda is the generator's output; CI's Agda job typechecks the file. */
    public void testCommittedModuleIsCurrent() throws IOException {
        File f = new File(specDir(), "GeneratedCertificates.agda");
        String committed = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertEquals(AgdaCertificates.exampleModule(), committed);
    }

    /** The example module holds the 36 keys of Ex. B.3 and both kinds of Ex. B.2 certificate. */
    public void testExampleModule() {
        String m = AgdaCertificates.exampleModule();
        assertTrue(m, m.contains("\nmodule GeneratedCertificates where\n"));
        assertEquals(36, count(m, " : DominantContainsLeadingTone "));
        assertEquals(24, count(m, " (_ , refl , _) = "));
        assertEquals(12, count(m, " (_ , _ , ())\n"));
        assertTrue(count(m, " : AllInterval ") >= 3);
        assertTrue(count(m, " : ¬ AllInterval ") >= 3);
        assertTrue(count(m, " : All (λ p ") >= 2);
        assertTrue(count(m, " : ¬ All (λ p ") >= 1);
    }

    /**
     * When agda 2.6.3 is on the PATH (not in the CI Maven job), it accepts the
     * example module and rejects a forged certificate. Without agda this test only
     * prints that it did not run.
     */
    public void testAgdaChecksCertificatesWhenAvailable() throws Exception {
        String agda = findAgda();
        if (agda == null) {
            System.out.println("AgdaCertificatesTest: agda not on PATH, Agda run not done");
            return;
        }
        File dir = Files.createTempDirectory("certs").toFile();
        copy("Prelude.agda", dir);
        copy("Certificates.agda", dir);
        write(new File(dir, "GeneratedCertificates.agda"), AgdaCertificates.exampleModule());
        assertEquals(0, runAgda(agda, dir, "GeneratedCertificates.agda"));
        String forged = AgdaCertificates.module("Forged", Arrays.asList(AgdaCertificates.allInterval("forged", 12, AI)))
            .replace("forged 2 _ = 4 , 6 ,", "forged 2 _ = 4 , 7 ,");
        assertTrue(forged.contains("forged 2 _ = 4 , 7 ,"));
        write(new File(dir, "Forged.agda"), forged);
        assertTrue(runAgda(agda, dir, "Forged.agda") != 0);
    }

    private static String findAgda() {
        String env = System.getenv("AGDA");
        if (env != null && new File(env).canExecute()) {
            return env;
        }
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        for (String p : path.split(File.pathSeparator)) {
            File f = new File(p, "agda");
            if (f.canExecute()) {
                return f.getPath();
            }
        }
        return null;
    }

    private static int runAgda(String agda, File dir, String file) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(agda, "--no-libraries", "-i", dir.getPath(), new File(dir, file).getPath());
        pb.environment().put("LC_ALL", "C.UTF-8");
        String home = System.getenv("HOME") != null ? System.getenv("HOME") : System.getProperty("user.home");
        File prim = new File(home, ".local/share/agda-prim/datadir");
        if (System.getenv("Agda_datadir") == null && prim.isDirectory()) {
            pb.environment().put("Agda_datadir", prim.getPath());
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(new File(dir, file + ".log"));
        return pb.start().waitFor();
    }

    private static void copy(String name, File dir) throws IOException {
        Files.copy(new File(specDir(), name).toPath(), new File(dir, name).toPath());
    }

    private static void write(File f, String s) throws IOException {
        Files.write(f.toPath(), s.getBytes(StandardCharsets.UTF_8));
    }

    /** spec/agda, found upward from the working directory (java/core under Maven). */
    static File specDir() {
        File d = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (d != null) {
            File s = new File(d, "spec/agda");
            if (s.isDirectory()) {
                return s;
            }
            d = d.getParentFile();
        }
        throw new IllegalStateException("spec/agda not found above " + System.getProperty("user.dir"));
    }

    private static int count(String s, String sub) {
        int n = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) {
            n++;
        }
        return n;
    }
}
