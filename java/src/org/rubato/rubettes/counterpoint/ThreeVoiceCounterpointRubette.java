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
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

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

import org.rubato.base.BooleanProperty;
import org.rubato.base.IntegerProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.base.StringProperty;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.io.ScoreNote;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint;
import org.rubato.math.counterpoint.ThreeVoiceCounterpoint.State;
import org.rubato.math.yoneda.Denotator;

/**
 * Checks three-voice first-species counterpoint after Okumura and
 * Shibayama (arXiv 2606.01102v3) and generates successor chords. The optional
 * input is a Score whose lower, middle and upper voices move note against
 * note. Output 0 holds the notes of chords outside the mask and of the
 * second chord of each refused step; output 1 holds the admitted successors
 * of the input's last chord or, without an input, of the start chord. The
 * relaxation property chooses the paper's relation (strict, the default) or
 * one of our relaxations hold-fibre, hold and reflexive. The view lists each chord and
 * step with the reasons for a refusal and can append the P/L/R report.
 * Departures: sources/THREE-VOICE-COUNTERPOINT-SPEC.md.
 */
public class ThreeVoiceCounterpointRubette extends SimpleAbstractRubette {

    static final String LOWER_KEY = "lowerVoice";
    static final String MIDDLE_KEY = "middleVoice";
    static final String UPPER_KEY = "upperVoice";
    static final String RELAXATION_KEY = "relaxation";
    static final String START_KEY = "startChord";
    static final String PLR_KEY = "plrReport";

    /** Example 3.3's first chord (2; 3, 0), D F D. */
    public static final String DEFAULT_START = "50 53 62";

    private static final ImageIcon icon = Icons.loadIcon(CounterpointCheckRubette.class, "counterpointcheckicon.png", "threevoicecounterpoint");

    private static final Border UNFOCUSED = new EmptyBorder(4, 6, 4, 6);

    /** The report's border while it has the keyboard focus. */
    static Border focusedBorder(JTextArea t) {
        return new CompoundBorder(new LineBorder(t.getForeground(), 2), new EmptyBorder(2, 4, 2, 4));
    }

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public ThreeVoiceCounterpointRubette() {
        setInCount(1);
        setOutCount(2);
        putProperty(new IntegerProperty(LOWER_KEY, "Lower voice", 0, 0, 1000));
        putProperty(new IntegerProperty(MIDDLE_KEY, "Middle voice", 1, 0, 1000));
        putProperty(new IntegerProperty(UPPER_KEY, "Upper voice", 2, 0, 1000));
        putProperty(new StringProperty(RELAXATION_KEY,
                "Relaxation: strict (the paper's relation), or ours: hold-fibre, hold or reflexive (a voice pair may stand still)",
                "strict", 1, 20));
        putProperty(new StringProperty(START_KEY,
                "Start chord when the input is not linked: three MIDI pitches, lower voice first",
                DEFAULT_START, 1, 40));
        putProperty(new BooleanProperty(PLR_KEY, "Append the P/L/R report (Prop. 6.8) to the view", false));
    }

    public void run(RunInfo runInfo) {
        process(getInput(0));
    }

    /** Checks the input, or uses the start chord when input is null, and sets both outputs. */
    public void process(Denotator input) {
        try {
            ThreeVoice.Settings s = settings();
            StringBuilder b = new StringBuilder();
            int[] from;
            if (input != null) {
                ThreeVoice.Report r = ThreeVoice.check(input, s);
                setOutput(0, r.flaggedScore());
                b.append(r.text());
                from = r.lastPitches();
                info = r.isCorrect() ? "correct" : r.flagged().size() + " flagged";
                b.append("\nSuccessors of the last chord");
            }
            else {
                from = ThreeVoice.parseChord(((StringProperty) getProperty(START_KEY)).getString());
                setOutput(0, ScoreNote.toScore(Collections.<ScoreNote>emptyList()));
                b.append("No score linked; output 1 holds the successors of the start chord.\n")
                 .append(ThreeVoice.modeText(s.mode)).append(".\n\nSuccessors of the start chord");
                info = "";
            }
            ThreeVoiceCounterpoint m = ThreeVoiceCounterpoint.fux();
            State last = m.fromPitchClasses(from[0], from[1], from[2]);
            b.append(String.format(" %d %d %d, state %s", from[0], from[1], from[2], last));
            if (m.inMask(last)) {
                List<ScoreNote> succ = ThreeVoice.successorNotes(from, s.mode);
                setOutput(1, ScoreNote.toScore(succ));
                int k = succ.size() / 3;
                b.append(String.format(" h%d: %d successor%s, one chord per onset in output 1.%n",
                        m.label(last.getB(), last.getC()), k, k == 1 ? "" : "s"));
                if (input == null) {
                    info = k + " successors";
                }
            }
            else {
                setOutput(1, ScoreNote.toScore(Collections.<ScoreNote>emptyList()));
                b.append(": not in the mask, so it has no successors.\n");
            }
            if (((BooleanProperty) getProperty(PLR_KEY)).getBoolean()) {
                b.append('\n').append(ThreeVoice.plrReport(s.mode));
            }
            reportText = b.toString();
        }
        catch (IllegalArgumentException e) {
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
    }

    /** The settings the current properties describe. */
    ThreeVoice.Settings settings() {
        return new ThreeVoice.Settings(((IntegerProperty) getProperty(LOWER_KEY)).getInt(),
                                       ((IntegerProperty) getProperty(MIDDLE_KEY)).getInt(),
                                       ((IntegerProperty) getProperty(UPPER_KEY)).getInt(),
                                       ThreeVoice.parseMode(((StringProperty) getProperty(RELAXATION_KEY)).getString()));
    }

    /** Sets the properties directly, for tests and shot drivers. */
    public void setSettingsForTest(ThreeVoice.Settings s, String startChord, boolean plr) {
        ((IntegerProperty) getProperty(LOWER_KEY)).setInt(s.lowerVoice);
        ((IntegerProperty) getProperty(MIDDLE_KEY)).setInt(s.middleVoice);
        ((IntegerProperty) getProperty(UPPER_KEY)).setInt(s.upperVoice);
        ((StringProperty) getProperty(RELAXATION_KEY)).setString(ThreeVoice.modeName(s.mode));
        ((StringProperty) getProperty(START_KEY)).setString(startChord);
        ((BooleanProperty) getProperty(PLR_KEY)).setBoolean(plr);
    }

    /** The text the view shows. */
    public String getReportText() {
        return reportText;
    }

    /** The report area, after {@link #getView}. */
    public JTextArea getTextArea() {
        return text;
    }

    public boolean hasView() {
        return true;
    }

    public JComponent getView() {
        if (view == null) {
            view = new JPanel(new BorderLayout());
            text = new JTextArea(20, 88);
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
            text.getAccessibleContext().setAccessibleName("Three-voice counterpoint report");
            text.getAccessibleContext().setAccessibleDescription(
                "Each chord with its pitches and state, each step admitted or refused with the reasons,"
                + " and the successors of the last chord, as text.");
            text.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                       Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
            text.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                       Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Three-voice counterpoint"));
            view.add(scroll, BorderLayout.CENTER);
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
        return "Three-Voice Counterpoint";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Checks three-voice first-species counterpoint and lists successor chords";
    }

    public String getLongDescription() {
        return "The Three-Voice Counterpoint rubette follows Okumura and Shibayama's model (arXiv 2606.01102):"
             + " a chord is three pitch classes whose intervals above the lower voice lie in the mask H_Fux,"
             + " and a step to the next chord is admitted when the contrapuntal symmetries of the voice pairs,"
             + " maximized together, allow it. It reads the lower, middle and upper voices of a zero-addressed"
             + " Score note against note. Output 0 holds the chords outside the mask and the second chord of"
             + " each refused step; output 1 holds the admitted successors of the last chord, or of the start"
             + " chord when nothing is linked. The relaxation strict is the paper's relation; hold-fibre,"
             + " hold and reflexive are ours and let a voice pair stand still; reflexive also drops the"
             + " joint maximization. The view lists every step with its reasons.";
    }

    public String getInTip(int i) {
        return "Three-voice score (optional)";
    }

    public String getOutTip(int i) {
        return i == 0 ? "Flagged chords" : "Successor chords";
    }
}
