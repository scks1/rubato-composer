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

import static org.rubato.composer.Utilities.makeTitledBorder;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.rubato.base.BooleanProperty;
import org.rubato.base.IntegerProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.math.harmony.Modulations.Filter;
import org.rubato.math.harmony.Modulations.Rigidity;
import org.rubato.math.harmony.Quantized;

/**
 * Lists the modulation quanta, modulators and pivots from a tonality to its
 * transposes. See {@link Harmonies#modulationReport}.
 */
public class ModulationRubette extends SimpleAbstractRubette {

    static final String SCALE_KEY = "scale";
    static final String TONIC_KEY = "tonic";
    static final String SIZE_KEY = "chordSize";
    static final String TARGET_KEY = "target";
    static final String COVERED_KEY = "covered";
    static final String AUTX_KEY = "autX";

    private static final ImageIcon icon = Icons.loadIcon(ModulationRubette.class, "modulationicon.png");

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public ModulationRubette() {
        setInCount(0);
        setOutCount(0);
        putProperty(new IntegerProperty(SCALE_KEY, "Scale (0 major, 1 harmonic minor, 2 melodic minor)", 0, 0, 2));
        putProperty(new IntegerProperty(TONIC_KEY, "Pitch class of the source tonic", 0, 0, 11));
        putProperty(new IntegerProperty(SIZE_KEY, "Chord size (3 triads, 4 seventh chords)", 3, 3, 4));
        putProperty(new IntegerProperty(TARGET_KEY, "Target transposition p (0: all eleven)", 0, 0, 11));
        putProperty(new BooleanProperty(COVERED_KEY, "Keep only quanta whose trace the target's chords cover", true));
        putProperty(new BooleanProperty(AUTX_KEY, "Rigidity under the scale's TI automorphisms only (reconstruction)", false));
    }

    public void run(RunInfo runInfo) {
        try {
            Harmonies.ModulationSettings s = settings();
            reportText = Harmonies.modulationReport(s);
            java.util.List<Quantized> q = Harmonies.modulations(s);
            info = q.size() + (q.size() == 1 ? " quantum" : " quanta");
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
    }

    /** The settings the current properties describe. */
    Harmonies.ModulationSettings settings() {
        return new Harmonies.ModulationSettings(
                ((IntegerProperty) getProperty(SCALE_KEY)).getInt(),
                ((IntegerProperty) getProperty(TONIC_KEY)).getInt(),
                ((IntegerProperty) getProperty(SIZE_KEY)).getInt(),
                ((IntegerProperty) getProperty(TARGET_KEY)).getInt(),
                ((BooleanProperty) getProperty(COVERED_KEY)).getBoolean() ? Filter.COVERED : Filter.ALGORITHM_1,
                ((BooleanProperty) getProperty(AUTX_KEY)).getBoolean() ? Rigidity.AUT_X : Rigidity.TI12);
    }

    /** The text the view shows. */
    public String getReportText() {
        return reportText;
    }

    public boolean hasView() {
        return true;
    }

    public JComponent getView() {
        if (view == null) {
            view = new JPanel(new BorderLayout());
            text = new JTextArea(18, 72);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Modulation quanta"));
            view.add(scroll, BorderLayout.CENTER);
        }
        text.setText(reportText);
        text.setCaretPosition(0);
        return view;
    }

    public void updateView() {
        getView();
    }

    public boolean hasInfo() {
        return true;
    }

    public String getInfo() {
        return info;
    }

    public String getGroup() {
        return "Harmony";
    }

    public String getName() {
        return "Modulation";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Lists modulation quanta and pivots between tonalities";
    }

    public String getLongDescription() {
        return "The Modulation rubette takes a seven-tone scale (major, harmonic or melodic minor),"
             + " a tonic and a chord size, and for each transposed target tonality lists the"
             + " quantized modulations of Mazzola's model: the minimal cadential set of the"
             + " target, the modulator, the modulation quantum and the pivot degrees.";
    }

    public String getInTip(int i) {
        return "";
    }

    public String getOutTip(int i) {
        return "";
    }
}
