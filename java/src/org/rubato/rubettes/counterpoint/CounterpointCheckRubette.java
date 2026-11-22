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
import org.rubato.math.counterpoint.Dichotomy;
import org.rubato.math.counterpoint.FirstSpecies.Orientation;
import org.rubato.math.counterpoint.StrongDichotomies;
import org.rubato.math.yoneda.Denotator;

/**
 * Checks a two-voice first-species Score against a strong dichotomy and
 * outputs the discantus notes at dissonant intervals or forbidden
 * successions. The view lists every position. The computation is
 * {@link Counterpoint#check(Denotator, Counterpoint.Settings)}.
 */
public class CounterpointCheckRubette extends SimpleAbstractRubette {

    static final String CLASS_KEY = "dichotomyClass";
    static final String CANTUS_KEY = "cantusVoice";
    static final String DISCANTUS_KEY = "discantusVoice";
    static final String HANGING_KEY = "hanging";

    private static final ImageIcon icon = Icons.loadIcon(CounterpointCheckRubette.class, "counterpointcheckicon.png");

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public CounterpointCheckRubette() {
        setInCount(1);
        setOutCount(1);
        putProperty(new IntegerProperty(CLASS_KEY, "Dichotomy class (64, 68, 71, 75, 78 or 82)", 82, 64, 82));
        putProperty(new IntegerProperty(CANTUS_KEY, "Cantus firmus voice", 0, 0, 1000));
        putProperty(new IntegerProperty(DISCANTUS_KEY, "Discantus voice", 1, 0, 1000));
        putProperty(new BooleanProperty(HANGING_KEY, "Discantus below the cantus firmus (hanging)", false));
    }

    public void run(RunInfo runInfo) {
        Denotator input = getInput(0);
        if (input == null) {
            addError("Input denotator is null.");
            return;
        }
        try {
            Counterpoint.Report r = Counterpoint.check(input, settings());
            reportText = r.text();
            info = r.isCorrect() ? "correct" : r.flagged().size() + " flagged";
            setOutput(0, r.flaggedScore());
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
    }

    /** The settings the current properties describe. */
    Counterpoint.Settings settings() {
        int c = ((IntegerProperty) getProperty(CLASS_KEY)).getInt();
        Dichotomy d = StrongDichotomies.ofClass(c);
        boolean hanging = ((BooleanProperty) getProperty(HANGING_KEY)).getBoolean();
        return new Counterpoint.Settings(d, hanging ? Orientation.HANGING : Orientation.SWEEPING,
                ((IntegerProperty) getProperty(CANTUS_KEY)).getInt(),
                ((IntegerProperty) getProperty(DISCANTUS_KEY)).getInt());
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
            text = new JTextArea(16, 72);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Counterpoint check"));
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
        return "Counterpoint Check";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Checks first-species counterpoint in a two-voice score";
    }

    public String getLongDescription() {
        return "The Counterpoint Check rubette reads the cantus firmus and discantus voices of a"
             + " zero-addressed Score, note against note, and checks each interval and each"
             + " succession of intervals against a strong dichotomy of Z12, by default Fux's"
             + " consonances (class 82). Its output holds the discantus notes at dissonant"
             + " intervals or forbidden successions; its view lists every position.";
    }

    public String getInTip(int i) {
        return "Two-voice score";
    }

    public String getOutTip(int i) {
        return "Flagged discantus notes";
    }
}
