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
import org.rubato.base.StringProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;

/**
 * Reports which minimal cadential sets of a major key a chord chart's bars
 * realise. See {@link Harmonies#analysisReport}.
 */
public class CadenceAnalysisRubette extends SimpleAbstractRubette {

    static final String CHORDS_KEY = "chords";
    static final String TONIC_KEY = "tonic";
    static final String ROOT_KEY = "rootMode";
    static final String WINDOW_KEY = "window";

    /** Blues for Alice as given in Agust&iacute;n-Aquino 2026, &sect;6. */
    static final String DEFAULT_CHART =
        "F6 | Em7 A7b9 | Dm7 G7 | Cm7 F7 | Bb7 | Bbm7 Eb7 | Am7 D7 | Abm7 Db7 | Gm7 | C7 | Am7 | Dm7 | Gm7 C7";

    private static final ImageIcon icon = Icons.loadIcon(CadenceAnalysisRubette.class, "cadenceicon.png");

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";

    public CadenceAnalysisRubette() {
        setInCount(0);
        setOutCount(0);
        putProperty(new StringProperty(CHORDS_KEY, "Chords, bars separated by |", DEFAULT_CHART));
        putProperty(new IntegerProperty(TONIC_KEY, "Pitch class of the major key's tonic", 5, 0, 11));
        putProperty(new BooleanProperty(ROOT_KEY, "Read degrees from chord roots (otherwise literal chord tones)", true));
        putProperty(new IntegerProperty(WINDOW_KEY, "Bars per window", 2, 1, 8));
    }

    public void run(RunInfo runInfo) {
        try {
            reportText = Harmonies.analysisReport(((StringProperty) getProperty(CHORDS_KEY)).getString(),
                    ((IntegerProperty) getProperty(TONIC_KEY)).getInt(),
                    ((BooleanProperty) getProperty(ROOT_KEY)).getBoolean(),
                    ((IntegerProperty) getProperty(WINDOW_KEY)).getInt());
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            addError(e.getMessage());
        }
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
            scroll.setBorder(makeTitledBorder("Cadential regions"));
            view.add(scroll, BorderLayout.CENTER);
        }
        text.setText(reportText);
        text.setCaretPosition(0);
        return view;
    }

    public void updateView() {
        getView();
    }

    public String getGroup() {
        return "Harmony";
    }

    public String getName() {
        return "Cadential Analysis";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Finds the minimal cadential sets a chord chart realises";
    }

    public String getLongDescription() {
        return "The Cadential Analysis rubette reads a lead-sheet chord chart in a major key and"
             + " lists, for each window of bars, the minimal cadential sets of seventh chords"
             + " (J1 to J6) whose degrees occur, with the regions A, B and C of the Blues for"
             + " Alice conglomerate. Degrees come from chord roots or from literal chord tones.";
    }

    public String getInTip(int i) {
        return "";
    }

    public String getOutTip(int i) {
        return "";
    }
}
