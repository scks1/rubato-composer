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

package org.rubato.rubettes.harmony.test;

import junit.framework.TestCase;

import org.rubato.base.BooleanProperty;
import org.rubato.base.IntegerProperty;

import org.rubato.math.harmony.Modulations.Filter;
import org.rubato.math.harmony.Modulations.Rigidity;
import org.rubato.rubettes.harmony.CadenceAnalysisRubette;
import org.rubato.rubettes.harmony.Harmonies;
import org.rubato.rubettes.harmony.ModulationRubette;

/** The headless harmony API and the two harmony rubettes. */
public class HarmoniesTest extends TestCase {

    /** Schoenberg's C to F, triads: one quantum, pivots II, IV, VII (Topos I sec. 27.1.5). */
    public void testCToF() {
        String r = Harmonies.modulationReport(new Harmonies.ModulationSettings(0, 0, 3, 5, Filter.COVERED, Rigidity.TI12));
        assertTrue(r, r.contains("minimal cadential sets: {VII} {II,III} {II,V} {III,IV} {IV,V}"));
        assertTrue(r, r.contains("to F (p = 5)"));
        assertTrue(r, r.contains("pivots {II,IV,VII}"));
        assertEquals(1, Harmonies.modulations(new Harmonies.ModulationSettings(0, 0, 3, 5, Filter.COVERED, Rigidity.TI12)).size());
    }

    /** Seventh chords in C, all targets: 24 rows without the covering filter, 20 with it. */
    public void testTetradicCounts() {
        assertEquals(24, Harmonies.modulations(new Harmonies.ModulationSettings(0, 0, 4, 0, Filter.ALGORITHM_1, Rigidity.TI12)).size());
        assertEquals(20, Harmonies.modulations(new Harmonies.ModulationSettings(0, 0, 4, 0, Filter.COVERED, Rigidity.TI12)).size());
        String r = Harmonies.modulationReport(new Harmonies.ModulationSettings(0, 0, 4, 1, Filter.ALGORITHM_1, Rigidity.TI12));
        assertTrue(r, r.contains("{VII7}"));
        assertTrue(r, r.contains("(trace not covered)"));
    }

    public void testRefusals() {
        try {
            new Harmonies.ModulationSettings(0, 0, 5, 0, Filter.COVERED, Rigidity.TI12);
            fail("chord size 5");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("3 or 4"));
        }
        try {
            Harmonies.analysisReport("F6 | Fsus4", 5, true, 2);
            fail("unknown quality");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Fsus4"));
        }
    }

    /** The default chart is Blues for Alice in F; root mode finds J4 in bars 6-7. */
    public void testAnalysisReport() {
        String r = Harmonies.analysisReport("F6 | Em7 A7b9 | Dm7 G7 | Cm7 F7 | Bb7 | Bbm7 Eb7 | Am7 D7", 5, true, 2);
        assertTrue(r, r.contains("bars 6-7: J4 {III,IV} (region A);"));
        assertTrue(r, r.contains("bars 4-5: J5 {V} (region C); J2 {I,IV} (region B);"));
    }

    public void testRubettes() {
        ModulationRubette m = new ModulationRubette();
        assertEquals("Modulation", m.getName());
        assertEquals("Harmony", m.getGroup());
        assertEquals(0, m.getInCount());
        assertNotNull(m.getIcon());
        assertEquals("Not run yet.", m.getReportText());
        m.run(null);
        assertTrue(m.getReportText(), m.getReportText().contains("pivots {II,IV,VII}"));
        assertEquals("26 quanta", m.getInfo());

        CadenceAnalysisRubette c = new CadenceAnalysisRubette();
        assertEquals("Cadential Analysis", c.getName());
        assertNotNull(c.getIcon());
        c.run(null);
        assertTrue(c.getReportText(), c.getReportText().contains("J4 {III,IV} (region A)"));
    }

    /**
     * AUT_X is the default (Simon, 2026-11-23); the literal TI12 reading stays
     * selectable in the settings and through the rubette's "autX" property.
     * Harmonic minor has no inversion symmetry, so there the readings differ.
     */
    public void testRigidityDefault() {
        assertEquals(Rigidity.AUT_X, Harmonies.ModulationSettings.defaults().getRigidity());
        ModulationRubette m = new ModulationRubette();
        assertTrue(((BooleanProperty) m.getProperty("autX")).getBoolean());

        int autX = Harmonies.modulations(new Harmonies.ModulationSettings(1, 0, 3, 0, Filter.COVERED, Rigidity.AUT_X)).size();
        int ti12 = Harmonies.modulations(new Harmonies.ModulationSettings(1, 0, 3, 0, Filter.COVERED, Rigidity.TI12)).size();
        assertTrue(autX + " vs " + ti12, autX != ti12);
        ((IntegerProperty) m.getProperty("scale")).setInt(1);
        m.run(null);
        assertEquals(autX + (autX == 1 ? " quantum" : " quanta"), m.getInfo());
        ((BooleanProperty) m.getProperty("autX")).setBoolean(false);
        m.run(null);
        assertEquals(ti12 + (ti12 == 1 ? " quantum" : " quanta"), m.getInfo());
    }
}
