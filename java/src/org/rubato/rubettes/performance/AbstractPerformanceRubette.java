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

import static org.rubato.composer.Utilities.makeTitledBorder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.rubato.base.SimpleAbstractRubette;
import org.rubato.composer.RunInfo;
import org.rubato.math.yoneda.Denotator;

/** The shared run and view code of the Tempo Performance and Scalar Performance rubettes. */
abstract class AbstractPerformanceRubette extends SimpleAbstractRubette {

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    AbstractPerformanceRubette() {
        setInCount(1);
        setOutCount(1);
    }

    /** The settings the current properties describe. */
    abstract Performances.Settings settings();

    /** The title of the view's border. */
    abstract String viewTitle();

    public void run(RunInfo runInfo) {
        process(getInput(0));
    }

    /** Performs a score and sets the output; on refusal, reports the reason as an error. */
    public void process(Denotator input) {
        try {
            Performances.Result r = Performances.perform(input, settings());
            reportText = r.getReport();
            info = String.format(Locale.ROOT, "%.1f s", r.getPhysicalDuration());
            setOutput(0, r.getPerformedScore());
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            info = "";
            setOutput(0, null);
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
            text = new JTextArea(16, 72);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder(viewTitle()));
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
        return "Performance";
    }

    public String getInTip(int i) {
        return "Score (zero-addressed)";
    }

    public String getOutTip(int i) {
        return "Performed score, quarters at M.M. 120";
    }
}
