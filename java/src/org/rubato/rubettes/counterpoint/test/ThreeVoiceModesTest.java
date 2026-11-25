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

package org.rubato.rubettes.counterpoint.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.StringProperty;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.RelaxationMode;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.counterpoint.ThreeVoice;
import org.rubato.rubettes.counterpoint.ThreeVoiceCounterpointRubette;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * The relaxations hold and reflexive in the headless API and the rubette
 * (sources/THREE-VOICE-COUNTERPOINT-SPEC.md, D12 and D13).
 */
public class ThreeVoiceModesTest extends TestCase {

    /** C E G, C E♭ G, D F A, C E G♯ in MIDI pitches, lower voice first. */
    static final int[][] PASSAGE = { { 48, 52, 55 }, { 48, 51, 55 }, { 50, 53, 57 }, { 48, 52, 56 } };

    public void testParseAndName() {
        assertEquals(RelaxationMode.HOLD, ThreeVoice.parseMode("hold"));
        assertEquals(RelaxationMode.REFLEXIVE, ThreeVoice.parseMode(" Reflexive "));
        assertEquals(RelaxationMode.STRICT, ThreeVoice.parseMode("strict"));
        assertEquals(RelaxationMode.HOLD_FIBRE, ThreeVoice.parseMode("hold-fibre"));
        for (RelaxationMode m : RelaxationMode.values()) {
            assertEquals(m, ThreeVoice.parseMode(ThreeVoice.modeName(m)));
        }
        assertEquals("hold", ThreeVoice.modeName(RelaxationMode.HOLD));
        assertEquals("reflexive", ThreeVoice.modeName(RelaxationMode.REFLEXIVE));
        try {
            ThreeVoice.parseMode("hold+2v");
            fail();
        }
        catch (IllegalArgumentException e) {
            for (String n : new String[] { "strict", "hold-fibre", "hold", "reflexive" }) {
                assertTrue(e.getMessage(), e.getMessage().contains(n));
            }
        }
    }

    public void testModeText() {
        String h = ThreeVoice.modeText(RelaxationMode.HOLD);
        assertTrue(h, h.startsWith("relaxation hold "));
        assertTrue(h, h.contains("not the paper's"));
        assertTrue(h, h.contains("D12"));
        String r = ThreeVoice.modeText(RelaxationMode.REFLEXIVE);
        assertTrue(r, r.startsWith("relaxation reflexive "));
        assertTrue(r, r.contains("not the paper's"));
        assertTrue(r, r.contains("no joint maximization"));
        assertTrue(r, r.contains("1,178"));
        assertTrue(r, r.contains("213"));
        assertTrue(r, r.contains("1,391"));
    }

    public void testPlrReports() {
        String h = ThreeVoice.plrReport(RelaxationMode.HOLD);
        assertTrue(h, h.contains("192 with the target in the mask, 144 admitted (P 60, L 36, R 48)"));
        String r = ThreeVoice.plrReport(RelaxationMode.REFLEXIVE);
        assertTrue(r, r.contains("192 with the target in the mask, 180 admitted (P 96, L 36, R 48)"));
    }

    /** The passage in all four modes: strict refuses every step, the others admit the first only (P02). */
    public void testPassageInEveryMode() {
        for (RelaxationMode m : RelaxationMode.values()) {
            ThreeVoice.Report rep = ThreeVoice.check(ThreeVoiceTest.chords(PASSAGE),
                                                     ThreeVoice.Settings.defaults().withMode(m));
            assertEquals(3, rep.getVerdicts().size());
            assertEquals(m.name(), m != RelaxationMode.STRICT, rep.getVerdicts().get(0).isAdmitted());
            assertFalse(m.name(), rep.getVerdicts().get(1).isAdmitted());
            assertFalse(m.name(), rep.getVerdicts().get(2).isAdmitted());
            assertTrue(rep.text(), rep.text().contains(ThreeVoice.modeText(m)));
        }
    }

    public void testRubetteInNewModes() {
        ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
        for (RelaxationMode mode : new RelaxationMode[] { RelaxationMode.HOLD, RelaxationMode.REFLEXIVE }) {
            ThreeVoiceCounterpointRubette r = new ThreeVoiceCounterpointRubette();
            r.setSettingsForTest(ThreeVoice.Settings.defaults().withMode(mode), "50 53 62", false);
            assertEquals(ThreeVoice.modeName(mode), ((StringProperty) r.getProperty("relaxation")).getString());
            r.process(null);
            int n = m.successors(m.state(2, 3, 0), mode).size();
            assertEquals(mode.name(), 3 * n, ((PowerDenotator) r.getOutput(1)).getFactorCount());
            assertTrue(r.getReportText(), r.getReportText().contains(n + " successors"));
        }
        assertTrue(m.successors(m.state(2, 3, 0), RelaxationMode.REFLEXIVE).size()
                   > m.successors(m.state(2, 3, 0), RelaxationMode.STRICT).size());
    }

    public void testPropertyLabelNamesEveryMode() {
        String label = new ThreeVoiceCounterpointRubette().getProperty("relaxation").getName();
        for (String n : new String[] { "strict", "hold-fibre", "hold", "reflexive" }) {
            assertTrue(label, label.contains(n));
        }
    }

    /** A saved network keeps its relaxation; values saved before these modes still load. */
    public void testRelaxationSavesAndReloads() throws Exception {
        for (String v : new String[] { "reflexive", "hold", "hold-fibre", "strict" }) {
            ThreeVoiceCounterpointRubette t = new ThreeVoiceCounterpointRubette();
            ((StringProperty) t.getProperty("relaxation")).setString(v);
            File f = File.createTempFile("threevoice", ".xml");
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
                ThreeVoiceCounterpointRubette back = (ThreeVoiceCounterpointRubette) t.fromXML(reader, element);
                in.close();
                assertEquals(v, ((StringProperty) back.getProperty("relaxation")).getString());
                back.process(null);
                assertTrue(back.getErrors().toString(), back.getErrors().isEmpty());
                assertTrue(back.getReportText(), back.getReportText().contains("relaxation " + v + " "));
            }
            finally {
                f.delete();
            }
        }
    }
}
