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
import java.awt.event.KeyEvent;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.rubato.base.SimpleAbstractRubette;
import org.rubato.composer.RunInfo;
import org.rubato.math.performance.Stemma;
import org.rubato.math.performance.WeightWatcher;
import org.rubato.math.yoneda.Denotator;

/**
 * The shared run and view code of the Tempo Performance and Scalar Performance rubettes. The
 * view has the tabs Report (the text report), Stemma (the stemma browser and Kernel View of
 * Mazzola, <i>The Topos of Music II</i>, &sect;41.4, Figs. 41.11-41.12) and, for rubettes that
 * take weights, Weight Watcher (Fig. 41.14).
 */
abstract class AbstractPerformanceRubette extends SimpleAbstractRubette {

    private JPanel view;
    private JTabbedPane tabs;
    private JTextArea text;
    private StemmaBrowser browser;
    private WeightWatcherView watcherView;
    private Stemma stemma;
    private WeightWatcher watcher;
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

    /** True for a rubette whose settings carry a Weight Watcher. */
    abstract boolean takesWeights();

    public void run(RunInfo runInfo) {
        process(getInput(0));
    }

    /** Performs a score and sets the output; on refusal, reports the reason as an error. */
    public void process(Denotator input) {
        try {
            Performances.Settings s = settings();
            Performances.Result r = Performances.perform(input, s);
            stemma = r.getStemma();
            watcher = s.getWeights();
            reportText = r.getReport();
            info = String.format(Locale.ROOT, "%.1f s", r.getPhysicalDuration());
            setOutput(0, r.getPerformedScore());
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            stemma = null;
            watcher = null;
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
            KernelView.tabLeaves(text);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            text.getAccessibleContext().setAccessibleName(viewTitle() + " report");
            tabs = new JTabbedPane();
            tabs.setBorder(makeTitledBorder(viewTitle()));
            tabs.addTab("Report", new JScrollPane(text));
            tabs.setMnemonicAt(0, KeyEvent.VK_R);
            browser = new StemmaBrowser();
            tabs.addTab("Stemma", browser);
            tabs.setMnemonicAt(1, KeyEvent.VK_S);
            if (takesWeights()) {
                watcherView = new WeightWatcherView();
                tabs.addTab("Weight Watcher", watcherView);
                tabs.setMnemonicAt(2, KeyEvent.VK_W);
            }
            view.add(tabs, BorderLayout.CENTER);
        }
        text.setText(reportText);
        text.setCaretPosition(0);
        browser.setStemma(stemma, reportText);
        if (watcherView != null) {
            watcherView.setWatcher(watcher);
        }
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

    /** The stemma of the last run, null before a run, after a refusal or for an empty score. */
    public Stemma getStemma() {
        return stemma;
    }

    /** The Weight Watcher of the last run, null without one. */
    public WeightWatcher getWeightWatcher() {
        return watcher;
    }

    /** The view's tabs, null before {@link #getView}. */
    public JTabbedPane getTabs() {
        return tabs;
    }

    /** The report text area, null before {@link #getView}. */
    public JTextArea getReportArea() {
        return text;
    }

    /** The stemma browser, null before {@link #getView}. */
    public StemmaBrowser getStemmaBrowser() {
        return browser;
    }

    /** The Weight Watcher view, null before {@link #getView} and for rubettes without weights. */
    public WeightWatcherView getWeightWatcherView() {
        return watcherView;
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
