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

import org.rubato.base.BooleanProperty;
import org.rubato.base.DoubleProperty;
import org.rubato.base.IntegerProperty;
import org.rubato.base.TextProperty;
import org.rubato.composer.icons.Icons;
import org.rubato.math.performance.WeightWatcher;

/**
 * Performs a Score with a tempo curve (Mazzola, <i>The Topos of Music II</i>, eq. (33.1),
 * p. 562) and an optional per-bar rubato (Example 52, p. 624, by Cor. 21, p. 576). Weights
 * typed in the Weight Watcher property (&sect;41.4, p. 681) make a daughter by the tempo
 * operator (39.32) before the rubato.
 */
public class TempoPerformanceRubette extends AbstractPerformanceRubette {

    static final String START_KEY = "tempoStart";
    static final String END_KEY = "tempoEnd";
    static final String CURVE_KEY = "tempoCurve";
    static final String BAR_KEY = "barLength";
    static final String DEPTH_KEY = "rubatoDepth";
    static final String VOICE_KEY = "rubatoVoice";
    static final String WEIGHTS_KEY = "weights";
    static final String PRODUCT_KEY = "weightsProduct";

    private static final ImageIcon icon = Icons.loadIcon(TempoPerformanceRubette.class, "tempoperformanceicon.png");

    public TempoPerformanceRubette() {
        putProperty(new DoubleProperty(START_KEY, "Tempo at the first onset (M.M.)", 80, 20, 400));
        putProperty(new DoubleProperty(END_KEY, "Tempo at the last note end (M.M.)", 80, 20, 400));
        putProperty(new IntegerProperty(CURVE_KEY, "Tempo curve (0 linear, 1 exponential)", 0, 0, 1));
        putProperty(new DoubleProperty(BAR_KEY, "Rubato bar length in quarters (0 for none)", 0, 0, 64));
        putProperty(new DoubleProperty(DEPTH_KEY, "Rubato depth (0 to 0.89)", 0, 0, 0.89));
        putProperty(new IntegerProperty(VOICE_KEY, "Rubato voice (-1 for all voices)", -1, -1, 1000));
        putProperty(new TextProperty(WEIGHTS_KEY, "Weight Watcher weights, one per line", "", 60, 5, false, false));
        putProperty(new BooleanProperty(PRODUCT_KEY, "Combine weights as product", false));
    }

    Performances.Settings settings() {
        return Performances.Settings.tempo(((DoubleProperty) getProperty(START_KEY)).getDouble(),
                ((DoubleProperty) getProperty(END_KEY)).getDouble(),
                ((IntegerProperty) getProperty(CURVE_KEY)).getInt() == 1 ? Performances.Curve.EXPONENTIAL
                        : Performances.Curve.LINEAR,
                ((DoubleProperty) getProperty(BAR_KEY)).getDouble(),
                ((DoubleProperty) getProperty(DEPTH_KEY)).getDouble(),
                ((IntegerProperty) getProperty(VOICE_KEY)).getInt())
                .withWeights(WeightWatcher.parse(((TextProperty) getProperty(WEIGHTS_KEY)).getString(),
                        ((BooleanProperty) getProperty(PRODUCT_KEY)).getBoolean()));
    }

    boolean takesWeights() {
        return true;
    }

    String viewTitle() {
        return "Tempo performance";
    }

    public String getName() {
        return "Tempo Performance";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Performs a score with a tempo curve and an optional bar rubato";
    }

    public String getLongDescription() {
        return "The Tempo Performance rubette turns the symbolic onsets and durations of a"
             + " zero-addressed Score into physical time by integrating 1/T for a linear or"
             + " exponential tempo curve T between a start and an end tempo, in closed form."
             + " With a bar length and a depth it adds a rubato in one voice or in all voices"
             + " that keeps every bar as long as the plain tempo's bar. Weights typed one per line,"
             + " as in 'Metro high=1.2 low=0.8 deformation=0.3 points=0:1,4:3,8:2', are normalized,"
             + " extended, deformed and added (or multiplied) by the Weight Watcher, and the combined"
             + " weight multiplies the tempo before the rubato. The output counts"
             + " quarters at M.M. 120, so MIDI File Out and Score Play render it in time;"
             + " the view shows the report, a stemma browser with a Kernel View, and the Weight Watcher.";
    }
}
