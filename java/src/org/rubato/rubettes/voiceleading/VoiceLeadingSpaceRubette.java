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

package org.rubato.rubettes.voiceleading;

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
import org.rubato.rubettes.global.NerveRubette;

/**
 * Shows a finite voice-leading space (Flieder, arXiv 2512.04090, section 4): its
 * subjective transformations between two chosen points, its objective
 * transformations as Sigma_VLS automorphisms, a check of Lewin's GIS axioms,
 * and, when a Score is linked, the transporters between its successive pitch
 * classes or triads. See {@link VoiceLeadings}; the departures are in
 * sources/VOICE-LEADING-SPEC.md.
 */
public class VoiceLeadingSpaceRubette extends SimpleAbstractRubette {

    static final String SPACE_KEY = "space";
    static final String GROUP_KEY = "group";
    static final String N_KEY = "modulus";
    static final String FROM_KEY = "from";
    static final String TO_KEY = "to";

    private static final ImageIcon icon = Icons.loadIcon(NerveRubette.class, "nerveicon.png");

    private static final Border UNFOCUSED = new EmptyBorder(4, 6, 4, 6);

    /** The report's border while it has the keyboard focus. */
    static Border focusedBorder(JTextArea t) {
        return new CompoundBorder(new LineBorder(t.getForeground(), 2), new EmptyBorder(2, 4, 2, 4));
    }

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public VoiceLeadingSpaceRubette() {
        setInCount(1);
        setOutCount(0);
        putProperty(new StringProperty(SPACE_KEY, "Space: PC (pitch classes of Z_n) or TRIADS (the 24 consonant triads)", "PC", 2, 6));
        putProperty(new StringProperty(GROUP_KEY, "Group: T, TI (transpositions and inversions) or Aff", "TI", 1, 3));
        putProperty(new IntegerProperty(N_KEY, "n of Z_n for PC (2 to 24)", 12, 2, 24));
        putProperty(new IntegerProperty(FROM_KEY, "From point (a pitch class, or a triad index: 0-11 major, 12-23 minor)", 0, 0, 23));
        putProperty(new IntegerProperty(TO_KEY, "To point", 4, 0, 23));
    }

    public void run(RunInfo runInfo) {
        process(getInput(0));
    }

    /** Reports on the space; the Score may be null (input not linked). */
    public void process(Denotator score) {
        try {
            VoiceLeadings.Report r = VoiceLeadings.analyze(score, settings());
            reportText = r.text();
            info = r.summary();
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
        if (text != null) {
            text.setText(reportText);
            text.setCaretPosition(0);
        }
    }

    VoiceLeadings.Settings settings() {
        return new VoiceLeadings.Settings(((StringProperty) getProperty(SPACE_KEY)).getString().trim().toUpperCase(),
                                          group(((StringProperty) getProperty(GROUP_KEY)).getString().trim()),
                                          ((IntegerProperty) getProperty(N_KEY)).getInt(),
                                          ((IntegerProperty) getProperty(FROM_KEY)).getInt(),
                                          ((IntegerProperty) getProperty(TO_KEY)).getInt());
    }

    /** Accepts any letter case: t, ti, aff. */
    private static String group(String g) {
        if (g.equalsIgnoreCase("aff")) {
            return "Aff";
        }
        return g.toUpperCase();
    }

    /** Sets the properties directly, for tests and shot drivers. */
    public void setSettingsForTest(String space, String group, int n, int from, int to) {
        ((StringProperty) getProperty(SPACE_KEY)).setString(space);
        ((StringProperty) getProperty(GROUP_KEY)).setString(group);
        ((IntegerProperty) getProperty(N_KEY)).setInt(n);
        ((IntegerProperty) getProperty(FROM_KEY)).setInt(from);
        ((IntegerProperty) getProperty(TO_KEY)).setInt(to);
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
            text = new JTextArea(22, 80);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
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
            text.getAccessibleContext().setAccessibleName("Voice-leading space report");
            text.getAccessibleContext().setAccessibleDescription(
                "Transporters, objective transformations, GIS check and the Score's steps, as text.");
            text.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                       Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
            text.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                       Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Voice-leading space"));
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
        return "Voice-Leading Space";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Shows a finite voice-leading space, its objective transformations and a GIS check";
    }

    public String getLongDescription() {
        return "The Voice-Leading Space rubette builds the voice-leading space of a group acting on"
             + " pitch classes or on the 24 consonant triads, after Flieder's type-theoretic account"
             + " (arXiv 2512.04090, section 4): the vertices are the points, the arrows from x to y"
             + " are the group elements that move x to y. Its view lists the arrows between the two"
             + " chosen points, counts the objective transformations (the automorphisms of the space"
             + " that respect which arrows go where, against those induced by conjugation and the"
             + " bare quiver automorphisms), and checks Lewin's two GIS axioms. With a Score linked"
             + " to its input it also lists the arrows between successive pitch classes, or between"
             + " the successive consonant triads by onset.";
    }

    public String getInTip(int i) {
        return "Score (optional)";
    }

    public String getOutTip(int i) {
        return "";
    }
}
