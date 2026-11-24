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
package org.rubato.rubettes.performance.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.BooleanProperty;
import org.rubato.base.TextProperty;
import org.rubato.io.ScoreNote;
import org.rubato.math.performance.LPS;
import org.rubato.math.performance.TempoCurve;
import org.rubato.math.performance.TempoCurves;
import org.rubato.math.performance.WeightWatcher;
import org.rubato.rubettes.performance.Performances;
import org.rubato.rubettes.performance.TempoPerformanceRubette;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * The Weight Watcher's combined weight feeding the tempo operator (Mazzola, <i>The Topos of
 * Music II</i>, &sect;41.4 p. 682, eq. (39.32)), and the unchanged behaviour without weights.
 */
public class WeightedPerformanceTest extends TestCase {

    static List<ScoreNote> notes() {
        List<ScoreNote> n = new ArrayList<ScoreNote>();
        for (int q = 0; q < 16; q++) {
            n.add(new ScoreNote(q, 72 + q % 5, 80 - q, 1, 0));
            if (q % 2 == 0) {
                n.add(new ScoreNote(q, 48 + q % 3, 70, 2, 1));
            }
        }
        return n;
    }

    private static String text(Performances.Result r) {
        StringBuilder b = new StringBuilder(r.getReport());
        for (ScoreNote s : r.getNotesInInputOrder()) {
            b.append(s.onset).append(' ').append(s.pitch).append(' ').append(s.loudness).append(' ')
             .append(s.duration).append(' ').append(s.voice).append('\n');
        }
        return b.toString();
    }

    private static String sha(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
        StringBuilder b = new StringBuilder();
        for (byte x : d) {
            b.append(String.format("%02x", x));
        }
        return b.toString();
    }

    /**
     * S4: without weights every report and output is byte-identical to the code before this
     * change; the hashes were printed by scratch/stemma-view/Golden.java on 0a1386f
     * (logs/stemma-view-2026-11-23/G00-golden-before.txt).
     */
    public void testNoWeightsKeepsEveryReportAndOutput() throws Exception {
        Performances.Settings[] s = {
            Performances.Settings.tempo(84, 64, Performances.Curve.LINEAR, 0, 0, -1),
            Performances.Settings.tempo(84, 64, Performances.Curve.LINEAR, 4, 0.2, 0),
            Performances.Settings.tempo(60, 90, Performances.Curve.EXPONENTIAL, 4, 0.3, -1),
            Performances.Settings.tempo(80, 80, Performances.Curve.LINEAR, 0, 0, -1),
            Performances.Settings.scalar(120, 0.9, 1.1, 0.95),
        };
        String[] golden = {
            "a0f1acadcc6ad397f74b852599b2211b970c2499e45192300ad9d5f0178bc6f8",
            "c7337c4eca24a80e7c9cb65c54a87df4c01427a6e3916a89a0f63fd6cc7046cd",
            "34ef72be0df0a24c889edcc7d32330f2c054bda65189101aa789323bab260dee",
            "137171e081b2fa93ef4b80112719a25d83100e4414d0c78795083d904a2f58d8",
            "28c8121574ed2d9e01ea92d6502df50a4d6a00ad519fb1eb8182bb3ac9f77033",
        };
        for (int i = 0; i < s.length; i++) {
            assertEquals("case " + i, golden[i], sha(text(Performances.perform(notes(), s[i]))));
        }
        // an empty Weight Watcher changes nothing either
        Performances.Settings e = s[1].withWeights(WeightWatcher.parse("# nothing\n", false));
        assertEquals(golden[1], sha(text(Performances.perform(notes(), e))));
    }

    private static final String TWO =
          "Metro high=1.2 low=0.8 deformation=0.3 points=0:1,4:3,8:2,12:3\n"
        + "Motif high=1.1 low=0.9 inverted=yes influence=0.5 tolerance=2 points=2:5,6:1,10:4\n";

    /** S3: the combined weight makes one daughter of the mother by the tempo operator w T (39.32). */
    public void testWeightsAddATempoDaughter() {
        for (boolean product : new boolean[] { false, true }) {
            WeightWatcher ww = WeightWatcher.parse(TWO, product);
            Performances.Result r = Performances.perform(notes(),
                    Performances.Settings.tempo(84, 64, Performances.Curve.LINEAR, 0, 0, -1).withWeights(ww));
            LPS mother = r.getStemma().getMother();
            assertEquals(1, mother.getDaughters().size());
            LPS d = mother.getDaughters().get(0);
            assertEquals("Mother/weights", d.getName());
            assertEquals("tempo weights", d.getOperator());
            assertTrue(d.getDaughters().isEmpty());
            assertEquals(2, r.getStemma().size());
            DoubleUnaryOperator w = ww.combined();
            TempoCurve t = mother.getTempo();
            for (double e = 0; e <= 16; e += 0.25) {
                assertEquals("w T at " + e, w.applyAsDouble(e) * t.tempo(e), d.getTempo().tempo(e), 1e-9);
            }
            String rep = r.getReport();
            assertTrue(rep, rep.contains("Weight Watcher: 2 weights, combined as " + (product ? "a product (39.7)" : "a sum (39.5)")));
            assertTrue(rep, rep.contains("Mother/weights: tempo weights, 24 notes"));
            // the performed onset of the last quarter follows the weighted tempo, 60 times the integral of 1/(w T)
            double expect = 60 * d.getTempo().integral(0, 15);
            ScoreNote last = r.getNotesInInputOrder().get(r.getNotesInInputOrder().size() - 1);
            assertEquals(15.0, notes().get(notes().size() - 1).onset, 0);
            assertEquals(2 * expect, last.onset, 1e-6);
            Performances.Result plain = Performances.perform(notes(),
                    Performances.Settings.tempo(84, 64, Performances.Curve.LINEAR, 0, 0, -1));
            assertTrue("the weights change the performance",
                    Math.abs(plain.getNotesInInputOrder().get(plain.getNotesInInputOrder().size() - 1).onset - last.onset) > 1e-3);
        }
    }

    /** D8: the rubato acts on the weights daughter. */
    public void testRubatoActsOnTheWeightsDaughter() {
        Performances.Result r = Performances.perform(notes(),
                Performances.Settings.tempo(84, 64, Performances.Curve.LINEAR, 4, 0.2, 0)
                        .withWeights(WeightWatcher.parse(TWO, false)));
        LPS d = r.getStemma().getMother().getDaughters().get(0);
        assertEquals("Mother/weights", d.getName());
        assertEquals(2, d.getDaughters().size());
        assertEquals("Mother/weights/v0", d.getDaughters().get(0).getName());
        assertEquals("Mother/weights/v0/rubato", d.getDaughters().get(0).getDaughters().get(0).getName());
        assertEquals(5, r.getStemma().size());
        assertEquals(2, r.getStemma().leaves().size());
    }

    /** D8 with the rubato in all voices: it hangs under the weights daughter, not beside it. */
    public void testAllVoiceRubatoActsOnTheWeightsDaughter() {
        Performances.Result r = Performances.perform(notes(),
                Performances.Settings.tempo(84, 64, Performances.Curve.LINEAR, 4, 0.2, -1)
                        .withWeights(WeightWatcher.parse(TWO, false)));
        LPS mother = r.getStemma().getMother();
        assertEquals(1, mother.getDaughters().size());
        LPS d = mother.getDaughters().get(0);
        assertEquals("Mother/weights", d.getName());
        assertEquals(1, d.getDaughters().size());
        assertEquals("Mother/weights/rubato", d.getDaughters().get(0).getName());
        assertEquals(3, r.getStemma().size());
        assertEquals(1, r.getStemma().leaves().size());
    }

    public void testScalarSettingsRefuseWeights() {
        try {
            Performances.Settings.scalar(120, 1, 1, 1).withWeights(WeightWatcher.parse(TWO, false));
            fail("scalar settings took weights");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Tempo Performance"));
        }
    }

    public void testTheRubetteReadsItsWeightProperties() {
        TempoPerformanceRubette t = new TempoPerformanceRubette();
        TextProperty text = (TextProperty) t.getProperty("weights");
        BooleanProperty product = (BooleanProperty) t.getProperty("weightsProduct");
        assertEquals("", text.getString());
        assertFalse(product.getBoolean());
        text.setString(TWO);
        product.setBoolean(true);
        t.process(ScoreNote.toScore(notes()));
        assertNotNull(t.getReportText(), t.getOutput(0));
        assertTrue(t.getReportText(), t.getReportText().contains("combined as a product (39.7)"));
        assertEquals("Mother/weights", t.getStemma().getMother().getDaughters().get(0).getName());
        assertEquals(2, t.getWeightWatcher().getEntries().size());

        text.setString("Metro high=1.2 low=0.8\n");
        t.process(ScoreNote.toScore(notes()));
        assertNull(t.getOutput(0));
        assertTrue(t.getReportText(), t.getReportText().contains("line 1"));
        assertTrue(t.getErrors().toString(), t.getErrors().toString().contains("points"));
        assertNull(t.getStemma());
    }

    /** The two new properties save and reload with the rubette. */
    public void testWeightPropertiesSaveAndReload() throws Exception {
        TempoPerformanceRubette t = new TempoPerformanceRubette();
        ((TextProperty) t.getProperty("weights")).setString(TWO.trim());
        ((BooleanProperty) t.getProperty("weightsProduct")).setBoolean(true);
        File f = File.createTempFile("weights", ".xml");
        try {
            XMLWriter writer = new XMLWriter(f);
            writer.open();
            t.toXML(writer);
            writer.close();
            BufferedReader in = new BufferedReader(new FileReader(f));
            XMLReader reader = new XMLReader(in);
            Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new BufferedReader(new FileReader(f)))).getDocumentElement();
            reader.parse();
            TempoPerformanceRubette back = (TempoPerformanceRubette) t.fromXML(reader, element);
            in.close();
            assertEquals(TWO.trim(), ((TextProperty) back.getProperty("weights")).getString());
            assertTrue(((BooleanProperty) back.getProperty("weightsProduct")).getBoolean());
            back.process(ScoreNote.toScore(notes()));
            assertEquals("Mother/weights", back.getStemma().getMother().getDaughters().get(0).getName());
        }
        finally {
            f.delete();
        }
    }

    /** The tempo curve of the stage-5 mother is what the weighted daughter multiplies. */
    public void testTheMotherKeepsItsTempo() {
        Performances.Result r = Performances.perform(notes(),
                Performances.Settings.tempo(84, 64, Performances.Curve.LINEAR, 0, 0, -1)
                        .withWeights(WeightWatcher.parse(TWO, false)));
        TempoCurve linear = TempoCurves.linear(84, -20.0 / 16);
        for (double e = 0; e <= 16; e += 1) {
            assertEquals(linear.tempo(e), r.getStemma().getMother().getTempo().tempo(e), 1e-9);
        }
    }
}
