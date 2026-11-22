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

package org.rubato.rubettes.global;

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
import org.rubato.math.yoneda.Denotator;

/**
 * Shows the nerve of a pitch-class covering of a Score: third chains of a
 * seven-tone scale or the chords by onset. See {@link Nerves}.
 */
public class NerveRubette extends SimpleAbstractRubette {

    static final String CHORDS_KEY = "chordsByOnset";
    static final String SIZE_KEY = "chartSize";
    static final String DEGREE_KEY = "degreeOne";

    private static final ImageIcon icon = Icons.loadIcon(NerveRubette.class, "nerveicon.png");

    private JPanel view;
    private JTextArea text;
    private String reportText = "Not run yet.";
    private String info = "";

    public NerveRubette() {
        setInCount(1);
        setOutCount(0);
        putProperty(new BooleanProperty(CHORDS_KEY, "Cover by the chords at each onset (otherwise third chains)", false));
        putProperty(new IntegerProperty(SIZE_KEY, "Third-chain chart size (1 tones, 2 thirds, 3 triads, 4 tetrads)", 3, 1, 4));
        putProperty(new IntegerProperty(DEGREE_KEY, "Pitch class of degree I (-1: the first note's)", -1, -1, 11));
    }

    public void run(RunInfo runInfo) {
        Denotator input = getInput(0);
        if (input == null) {
            addError("Input denotator is null.");
            return;
        }
        try {
            Nerves.Report r = Nerves.analyze(input, settings());
            reportText = r.text();
            info = "f = " + java.util.Arrays.toString(r.getNerve().fVector()) + ", chi = "
                   + r.getNerve().eulerCharacteristic();
        }
        catch (IllegalArgumentException e) {
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
    }

    /** The settings the current properties describe. */
    Nerves.Settings settings() {
        boolean chords = ((BooleanProperty) getProperty(CHORDS_KEY)).getBoolean();
        return new Nerves.Settings(chords ? Nerves.Mode.CHORDS_BY_ONSET : Nerves.Mode.THIRD_CHAINS,
                ((IntegerProperty) getProperty(SIZE_KEY)).getInt(),
                ((IntegerProperty) getProperty(DEGREE_KEY)).getInt());
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
            scroll.setBorder(makeTitledBorder("Nerve"));
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
        return "Nerve";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Computes the nerve of a pitch-class covering of a score";
    }

    public String getLongDescription() {
        return "The Nerve rubette reads the pitch classes of a zero-addressed Score and covers"
             + " them either by the third chains of a seven-tone scale (tones, thirds, triads"
             + " or tetrads on each degree) or by the chords sounding at each onset. Its view"
             + " shows the nerve of the covering: face counts, Euler characteristic, homology,"
             + " boundary and orientability, maximal simplexes and the chord class of every"
             + " simplex.";
    }

    public String getInTip(int i) {
        return "Score";
    }

    public String getOutTip(int i) {
        return "";
    }
}
