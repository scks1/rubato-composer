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

package org.rubato.rubettes.counterpoint;

import static org.rubato.composer.Utilities.makeTitledBorder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.rubato.base.BooleanProperty;
import org.rubato.base.IntegerProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.base.StringProperty;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.math.counterpoint.FirstSpecies.Orientation;
import org.rubato.math.counterpoint.StrongDichotomies;
import org.rubato.math.counterpoint.SymmetryTrace;
import org.rubato.math.yoneda.Denotator;

/**
 * Lists the contrapuntal symmetries that admit each step of a two-voice
 * passage, as in the tables of Agust&iacute;n-Aquino and Curiel-L&oacute;pez
 * (arXiv 2512.18862, &sect;5.1). With a Score on its input it reads the
 * cantus firmus and discantus voices; with no input it reads the intervals
 * property. The computation is {@link SymmetryTrace}.
 */
public class SymmetryTraceRubette extends SimpleAbstractRubette {

    static final String INTERVALS_KEY = "intervals";
    static final String CLASS_KEY = "dichotomyClass";
    static final String CANTUS_KEY = "cantusVoice";
    static final String DISCANTUS_KEY = "discantusVoice";
    static final String HANGING_KEY = "hanging";

    /** The intervals the paper gives for Confitebor primo, mm. 64-67 (p. 8). */
    public static final String DEFAULT_INTERVALS =
        "0+\u03b5.7, 0+\u03b5.4, 10+\u03b5.7, 10+\u03b5.4, 9+\u03b5.7, 9+\u03b5.3, 7+\u03b5.7, 7+\u03b5.4, 5+\u03b5.7";

    private static final ImageIcon icon = Icons.loadIcon(SymmetryTraceRubette.class, "symmetrytraceicon.png");

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public SymmetryTraceRubette() {
        setInCount(1);
        setOutCount(0);
        putProperty(new StringProperty(INTERVALS_KEY, "Intervals x+\u03b5.y, used when no score is connected", DEFAULT_INTERVALS));
        putProperty(new IntegerProperty(CLASS_KEY, "Dichotomy class (64, 68, 71, 75, 78 or 82)", 82, 64, 82));
        putProperty(new IntegerProperty(CANTUS_KEY, "Cantus firmus voice", 0, 0, 1000));
        putProperty(new IntegerProperty(DISCANTUS_KEY, "Discantus voice", 1, 0, 1000));
        putProperty(new BooleanProperty(HANGING_KEY, "Discantus below the cantus firmus (hanging)", false));
    }

    public void run(RunInfo runInfo) {
        Denotator input = null;
        try {
            input = getInput(0);
        }
        catch (RuntimeException e) {
            input = null;
        }
        process(input);
        if (reportText.startsWith("Error: ")) {
            addError(reportText.substring(7));
        }
    }

    /** Computes the trace of a score, or of the intervals property when the score is null. */
    public void process(Denotator score) {
        try {
            Counterpoint.Settings s = settings();
            List<SymmetryTrace.Step> steps = score == null
                    ? SymmetryTrace.trace(s.dichotomy, SymmetryTrace.parse(intervals()))
                    : Counterpoint.trace(score, s);
            int total = 0;
            for (SymmetryTrace.Step st : steps) {
                total += st.cardinality();
            }
            reportText = SymmetryTrace.report(steps);
            info = steps.size() + " steps, " + total + " symmetries";
        }
        catch (IllegalArgumentException e) {
            reportText = "Error: " + e.getMessage();
            info = "";
        }
        if (text != null) {
            updateView();
        }
    }

    /** The settings the current properties describe. */
    Counterpoint.Settings settings() {
        boolean hanging = ((BooleanProperty) getProperty(HANGING_KEY)).getBoolean();
        return new Counterpoint.Settings(StrongDichotomies.ofClass(((IntegerProperty) getProperty(CLASS_KEY)).getInt()),
                hanging ? Orientation.HANGING : Orientation.SWEEPING,
                ((IntegerProperty) getProperty(CANTUS_KEY)).getInt(),
                ((IntegerProperty) getProperty(DISCANTUS_KEY)).getInt());
    }

    String intervals() {
        return ((StringProperty) getProperty(INTERVALS_KEY)).getString();
    }

    /** Replaces the intervals property. */
    public void setIntervals(String s) {
        ((StringProperty) getProperty(INTERVALS_KEY)).setString(s);
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
            text = new JTextArea(14, 72);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Symmetries of each step"));
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
        return "Score";
    }

    public String getName() {
        return "Symmetry Trace";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Lists the contrapuntal symmetries admitting each step of a two-voice passage";
    }

    public String getLongDescription() {
        return "The Symmetry Trace rubette takes a two-voice passage, either the cantus firmus and"
             + " discantus voices of a zero-addressed Score or a list of intervals x+\u03b5.y, and"
             + " lists for each step the contrapuntal symmetries of the first interval that admit the"
             + " second, with their number, as in Agust\u00edn-Aquino and Curiel-L\u00f3pez's tables"
             + " of Monteverdi passages. The dichotomy is Fux's consonances (class 82) by default.";
    }

    public String getInTip(int i) {
        return "Two-voice score (optional)";
    }
}
