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
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.rubato.base.DoubleProperty;
import org.rubato.base.IntegerProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.base.StringProperty;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.math.harmony.KeyPath;
import org.rubato.math.yoneda.Denotator;

/**
 * Follows a Score or a chord chart through major keys by the minimal
 * cadential sets it displays, with the quanta, modulators and pivots at each
 * change of key. The computation is {@link KeyPath}, a mechanical stand-in
 * for the hand readings of Agust&iacute;n-Aquino and Curiel-L&oacute;pez
 * (arXiv 2512.18862, &sect;5.2).
 */
public class KeyPathRubette extends SimpleAbstractRubette {

    static final String CHORDS_KEY = "chords";
    static final String WINDOW_KEY = "window";
    static final String BAR_KEY = "barLength";

    /**
     * A synthetic 20-bar chart for In questo lieto e fortunato giorno (L'Orfeo),
     * built to meet the bar numbers the paper gives on p. 13; not Monteverdi's chords.
     */
    public static final String DEFAULT_CHART = "C | C G | C Am | F G | D | G A | D Bm | Em G | D | Bm F#m | D F#m"
            + " | G#dim A | E A | C#m F#m | A | Dm | G | C | F G | C";

    private static final ImageIcon icon = Icons.loadIcon(KeyPathRubette.class, "keypathicon.png");

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public KeyPathRubette() {
        setInCount(1);
        setOutCount(0);
        putProperty(new StringProperty(CHORDS_KEY, "Chords, bars separated by |, used when no score is connected",
                DEFAULT_CHART));
        putProperty(new IntegerProperty(WINDOW_KEY, "Bars a cadence may span", 2, 1, 8));
        putProperty(new DoubleProperty(BAR_KEY, "Score bar length in onset units", 4.0, 0.0625, 64.0));
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

    /** Analyses a score, or the chord property when the score is null. */
    public void process(Denotator score) {
        try {
            int w = ((IntegerProperty) getProperty(WINDOW_KEY)).getInt();
            List<KeyPath.Event> events;
            String head;
            if (score == null) {
                String chart = ((StringProperty) getProperty(CHORDS_KEY)).getString();
                events = Harmonies.keyPath(chart, w);
                head = DEFAULT_CHART.equals(chart.trim())
                        ? "Chart: synthetic reconstruction of In questo lieto e fortunato giorno (not the real music)\n\n"
                        : "Chart from the chords property\n\n";
            }
            else {
                events = Harmonies.keyPath(score, ((DoubleProperty) getProperty(BAR_KEY)).getDouble(), w);
                head = "Score input\n\n";
            }
            reportText = head + KeyPath.report(events);
            List<String> keys = new ArrayList<String>();
            for (KeyPath.Event e : events) {
                if (e.kind() != KeyPath.Kind.CONFIRMATION) {
                    keys.add(e.keyName());
                }
            }
            info = String.join(", ", keys);
        }
        catch (IllegalArgumentException e) {
            reportText = "Error: " + e.getMessage();
            info = "";
        }
        if (text != null) {
            updateView();
        }
    }

    /** Replaces the chords property. */
    public void setChart(String chart) {
        ((StringProperty) getProperty(CHORDS_KEY)).setString(chart);
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
            text = new JTextArea(20, 80);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Key path"));
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
        return "Key Path";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Follows a score through major keys by the cadences it displays";
    }

    public String getLongDescription() {
        return "The Key Path rubette reads the chords of a zero-addressed Score bar by bar, or a chord"
             + " chart when no score is connected, and marks each bar where a minimal cadential set"
             + " k1 to k5 of a major key is complete within the window. A cadence of a new key is a"
             + " modulation, reported with its quanta, modulators, pivot chords and the bar where the"
             + " new tonic first sounds; a k2 in the current key reads as a turn to the relative minor.";
    }

    public String getInTip(int i) {
        return "Score (optional)";
    }
}
