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
import org.rubato.math.counterpoint.ContrapuntalSymmetries;
import org.rubato.math.counterpoint.Dichotomy;
import org.rubato.math.counterpoint.DualAffine;
import org.rubato.math.counterpoint.FirstSpecies.Orientation;
import org.rubato.math.counterpoint.StrongDichotomies;

/**
 * Lists the admitted successors of a consonant interval over a cantus
 * firmus pitch, with no input and one Score output of successor dyads.
 * The view gives both successor counts the model distinguishes: the
 * largest intersection a single contrapuntal symmetry gives, and the union
 * over all of them (see {@link ContrapuntalSymmetries}).
 */
public class SuccessorGeneratorRubette extends SimpleAbstractRubette {

    static final String CLASS_KEY = "dichotomyClass";
    static final String PITCH_KEY = "cantusPitch";
    static final String INTERVAL_KEY = "interval";
    static final String HANGING_KEY = "hanging";

    private static final ImageIcon icon = Icons.loadIcon(SuccessorGeneratorRubette.class, "successorgeneratoricon.png");

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public SuccessorGeneratorRubette() {
        setInCount(0);
        setOutCount(1);
        putProperty(new IntegerProperty(CLASS_KEY, "Dichotomy class (64, 68, 71, 75, 78 or 82)", 82, 64, 82));
        putProperty(new IntegerProperty(PITCH_KEY, "Cantus firmus pitch", 60, 0, 127));
        putProperty(new IntegerProperty(INTERVAL_KEY, "Interval (semitones mod 12)", 7, 0, 11));
        putProperty(new BooleanProperty(HANGING_KEY, "Discantus below the cantus firmus (hanging)", false));
    }

    public void run(RunInfo runInfo) {
        try {
            Dichotomy d = StrongDichotomies.ofClass(((IntegerProperty) getProperty(CLASS_KEY)).getInt());
            int pitch = ((IntegerProperty) getProperty(PITCH_KEY)).getInt();
            int k = ((IntegerProperty) getProperty(INTERVAL_KEY)).getInt();
            Orientation o = ((BooleanProperty) getProperty(HANGING_KEY)).getBoolean() ? Orientation.HANGING : Orientation.SWEEPING;
            ContrapuntalSymmetries.Result r = Counterpoint.successors(d, Math.floorMod(pitch, 12), k);
            reportText = describe(r, pitch);
            info = r.getSuccessorCount() + " successors";
            setOutput(0, Counterpoint.successorScore(d, o, pitch, k));
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
    }

    static String describe(ContrapuntalSymmetries.Result r, int pitch) {
        StringBuilder b = new StringBuilder();
        b.append("Successors of interval ").append(r.getInterval()).append(" over cantus firmus ").append(pitch)
         .append(" (pitch class ").append(r.getCantus()).append(") for ").append(r.getDichotomy()).append(".\n");
        b.append("Admitted successors, union over all contrapuntal symmetries: ").append(r.getSuccessorCount()).append('\n');
        b.append("Largest intersection a single symmetry gives: ").append(r.getMaxIntersection())
         .append(" (CCW Thm. 3.1, p. 35, bounds this from below by 42; Topos I Thm. 33, p. 538, by 36)\n");
        b.append("Contrapuntal symmetries: ").append(r.getSymmetries().size()).append('\n');
        for (DualAffine g : r.getSymmetries()) {
            b.append("  ").append(g).append('\n');
        }
        b.append("Successors as (cantus firmus pitch class, interval), by step of the cantus firmus:\n");
        for (int step = 0; step < 12; step++) {
            StringBuilder row = new StringBuilder();
            for (int k2 = 0; k2 < 12; k2++) {
                if (r.isSuccessor(Math.floorMod(r.getCantus() + step, 12), k2)) {
                    row.append(row.length() == 0 ? "" : " ").append(k2);
                }
            }
            b.append(String.format("  step %2d: %s%n", step, row.length() == 0 ? "none" : row));
        }
        return b.toString();
    }

    public String getReportText() {
        return reportText;
    }

    public boolean hasView() {
        return true;
    }

    public JComponent getView() {
        if (view == null) {
            view = new JPanel(new BorderLayout());
            text = new JTextArea(24, 72);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Admitted successors"));
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
        return "Successor Generator";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Lists the admitted successors of a counterpoint interval";
    }

    public String getLongDescription() {
        return "The Successor Generator rubette computes the contrapuntal symmetries of a consonant"
             + " interval over a cantus firmus pitch for a strong dichotomy of Z12 and outputs every"
             + " admitted successor as a two-voice dyad, one per onset. Its view lists the symmetries"
             + " and the successors by step of the cantus firmus.";
    }

    public String getInTip(int i) {
        return "";
    }

    public String getOutTip(int i) {
        return "Successor dyads";
    }
}
