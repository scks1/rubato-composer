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
package org.rubato.rubettes.performance;

import javax.swing.ImageIcon;

import org.rubato.base.DoubleProperty;
import org.rubato.composer.icons.Icons;

/**
 * Performs dynamics and articulation (Mazzola, <i>The Topos of Music II</i>, &sect;39.6-39.7,
 * pp. 654-657, and Example 51, p. 591) at a constant tempo.
 */
public class ScalarPerformanceRubette extends AbstractPerformanceRubette {

    static final String TEMPO_KEY = "tempo";
    static final String LOUD_START_KEY = "loudnessStart";
    static final String LOUD_END_KEY = "loudnessEnd";
    static final String ARTICULATION_KEY = "articulation";

    private static final ImageIcon icon = Icons.loadIcon(ScalarPerformanceRubette.class, "scalarperformanceicon.png");

    public ScalarPerformanceRubette() {
        putProperty(new DoubleProperty(TEMPO_KEY, "Tempo (M.M.; 120 keeps onsets in place)", 120, 20, 400));
        putProperty(new DoubleProperty(LOUD_START_KEY, "Loudness factor at the first onset", 1, 0.05, 8));
        putProperty(new DoubleProperty(LOUD_END_KEY, "Loudness factor at the last note end", 1, 0.05, 8));
        putProperty(new DoubleProperty(ARTICULATION_KEY, "Articulation: duration factor", 1, 0.05, 8));
    }

    Performances.Settings settings() {
        return Performances.Settings.scalar(((DoubleProperty) getProperty(TEMPO_KEY)).getDouble(),
                ((DoubleProperty) getProperty(LOUD_START_KEY)).getDouble(),
                ((DoubleProperty) getProperty(LOUD_END_KEY)).getDouble(),
                ((DoubleProperty) getProperty(ARTICULATION_KEY)).getDouble());
    }

    String viewTitle() {
        return "Scalar performance";
    }

    public String getName() {
        return "Scalar Performance";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Performs dynamics and articulation at a constant tempo";
    }

    public String getLongDescription() {
        return "The Scalar Performance rubette multiplies the loudness of each note by a factor"
             + " that moves exponentially from a start value to an end value across the piece,"
             + " a crescendo or diminuendo, and multiplies every performed duration by an"
             + " articulation factor, below 1 for staccato and above 1 for legato. At M.M. 120"
             + " it leaves onsets where they are, so it can follow Tempo Performance. Loudness"
             + " is clamped to the MIDI range 1-127.";
    }
}
