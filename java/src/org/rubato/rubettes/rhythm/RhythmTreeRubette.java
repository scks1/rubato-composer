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

package org.rubato.rubettes.rhythm;

import static org.rubato.composer.Utilities.makeTitledBorder;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.rubato.base.IntegerProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.base.StringProperty;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.score.RhythmizeRubette;

/**
 * Renders a rhythm tree (Flieder, arXiv 2512.04090, Ex. C.1 and Figure 2)
 * as a Score. The tree comes from the optional RhythmTree input or, when it
 * is not linked, from the tree property in the notation of
 * {@link org.rubato.math.rhythm.RhythmNotation}. Output 0 is the Score,
 * output 1 the tree as a RhythmTree denotator. The view draws the node
 * spans by depth ({@link RhythmSpanPanel}) above the text report
 * ({@link RhythmTreeReports}). Departures: sources/RHYTHM-TREES-SPEC.md.
 */
public class RhythmTreeRubette extends SimpleAbstractRubette {

    static final String TREE_KEY = "tree";
    static final String TIMING_KEY = "timing";
    static final String START_KEY = "start";
    static final String PITCH_KEY = "pitch";
    static final String LOUDNESS_KEY = "loudness";
    static final String VOICE_KEY = "voice";

    private static final ImageIcon icon = Icons.loadIcon(RhythmizeRubette.class, "rhythmizeicon.png", "rhythmtree");

    private static final Border UNFOCUSED = new EmptyBorder(4, 6, 4, 6);

    /** The report's border while it has the keyboard focus. */
    static Border focusedBorder(JTextArea t) {
        return new CompoundBorder(new LineBorder(t.getForeground(), 2), new EmptyBorder(2, 4, 2, 4));
    }

    private JPanel view;
    private JTextArea text;
    private RhythmSpanPanel spans;
    private String reportText = "Not run yet.";
    private String info = "";
    private RhythmTreeReports.Report last;

    public RhythmTreeRubette() {
        setInCount(1);
        setOutCount(2);
        putProperty(new StringProperty(TREE_KEY, "Rhythm tree, used when the input is not linked; for example "
                                       + RhythmTreeReports.DEFAULT_TREE, RhythmTreeReports.DEFAULT_TREE, 1, 20000));
        putProperty(new StringProperty(TIMING_KEY, "Timing: proportional (the factors share the parent) or label (each node lasts its label)",
                                       "proportional", 1, 20));
        putProperty(new StringProperty(START_KEY, "Start onset (0 or more; 2.5 or 19/2 allowed)", "0", 1, 40));
        putProperty(new IntegerProperty(PITCH_KEY, "Pitch of every note (MIDI key)", 60, 0, 127));
        putProperty(new IntegerProperty(LOUDNESS_KEY, "Loudness of every note", 80, 0, 127));
        putProperty(new IntegerProperty(VOICE_KEY, "Voice of every note", 0, 0, 127));
    }

    public void run(RunInfo runInfo) {
        process(getInput(0));
    }

    /** Renders the input tree, or the tree property when input is null, and sets both outputs. */
    public void process(Denotator input) {
        try {
            last = RhythmTreeReports.render(input, settings());
            setOutput(0, last.score());
            setOutput(1, last.treeDenotator());
            reportText = last.text();
            info = last.summary();
        }
        catch (IllegalArgumentException e) {
            last = null;
            setOutput(0, null);
            setOutput(1, null);
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
        refresh();
    }

    private void refresh() {
        if (text != null) {
            text.setText(reportText);
            text.setCaretPosition(0);
        }
        if (spans != null) {
            spans.setLayoutShown(last == null ? null : last.layout());
        }
    }

    RhythmTreeReports.Settings settings() {
        return new RhythmTreeReports.Settings(((StringProperty) getProperty(TREE_KEY)).getString(),
                                              ((StringProperty) getProperty(TIMING_KEY)).getString(),
                                              ((StringProperty) getProperty(START_KEY)).getString(),
                                              ((IntegerProperty) getProperty(PITCH_KEY)).getInt(),
                                              ((IntegerProperty) getProperty(LOUDNESS_KEY)).getInt(),
                                              ((IntegerProperty) getProperty(VOICE_KEY)).getInt());
    }

    /** Sets the properties directly, for tests and shot drivers. */
    public void setSettingsForTest(RhythmTreeReports.Settings s) {
        ((StringProperty) getProperty(TREE_KEY)).setString(s.tree);
        ((StringProperty) getProperty(TIMING_KEY)).setString(s.timing);
        ((StringProperty) getProperty(START_KEY)).setString(s.start);
        ((IntegerProperty) getProperty(PITCH_KEY)).setInt(s.pitch);
        ((IntegerProperty) getProperty(LOUDNESS_KEY)).setInt(s.loudness);
        ((IntegerProperty) getProperty(VOICE_KEY)).setInt(s.voice);
    }

    /** The text the view shows. */
    public String getReportText() {
        return reportText;
    }

    /** The report area, after {@link #getView}. */
    public JTextArea getTextArea() {
        return text;
    }

    /** The drawing, after {@link #getView}. */
    public RhythmSpanPanel getSpanPanel() {
        return spans;
    }

    public boolean hasView() {
        return true;
    }

    public JComponent getView() {
        if (view == null) {
            view = new JPanel(new BorderLayout());
            spans = new RhythmSpanPanel();
            JScrollPane top = new JScrollPane(spans);
            top.setBorder(makeTitledBorder("Spans by depth"));
            text = new JTextArea(18, 80);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(UNFOCUSED);
            // the caret alone is too faint a focus indicator; a 2 px line in the text colour
            // replaces 2 px of the padding, so the text does not move
            text.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    text.setBorder(focusedBorder(text));
                }

                public void focusLost(FocusEvent e) {
                    text.setBorder(UNFOCUSED);
                }
            });
            text.getAccessibleContext().setAccessibleName("Rhythm tree report");
            text.getAccessibleContext().setAccessibleDescription(
                "The tree, the timing, and every node's exact onset, length and end, as text; leaves marked note became the Score's notes.");
            text.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                       Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
            text.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                       Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Rhythm tree"));
            view.add(top, BorderLayout.NORTH);
            view.add(scroll, BorderLayout.CENTER);
        }
        if (last == null && reportText.equals("Not run yet.")) {
            // show the tree property before the first run, without outputs
            try {
                last = RhythmTreeReports.render(null, settings());
                reportText = "Not run yet. The tree property gives:\n\n" + last.text();
            }
            catch (IllegalArgumentException e) {
                last = null;
            }
        }
        refresh();
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
        return "Rhythm Tree";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Renders a rhythm tree (a W-type after Flieder) as Score onsets and durations";
    }

    public String getLongDescription() {
        return "The Rhythm Tree rubette turns a rhythm tree, in the sense of Flieder's type-theoretic"
             + " account (arXiv 2512.04090, Example C.1), into a Score. A node has a rational label and"
             + " a list of proportional factors, one per subtree; the tree comes from the input or from"
             + " the tree property, written like 9.5 (2 2.5 (1 (2 1) 1 1) 3 (1.5 2)), where r@d gives a"
             + " child the factor r and its own label d. In proportional timing a child lasts its"
             + " parent's length times its factor over the sum of the factors; in label timing every"
             + " node lasts its own label. Each leaf becomes a note with the pitch, loudness and voice"
             + " set in the properties. The view draws every node's span and lists the exact values.";
    }

    public String getInTip(int i) {
        return "RhythmTree (optional)";
    }

    public String getOutTip(int i) {
        return i == 0 ? "Score" : "RhythmTree";
    }
}
