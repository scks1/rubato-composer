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
package org.rubato.rubettes.gesture;

import static org.rubato.composer.Utilities.makeTitledBorder;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.rubato.base.DoubleProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.math.gesture.FingerGesture;
import org.rubato.math.gesture.Gesture;
import org.rubato.math.yoneda.Denotator;

/**
 * Draws the finger gesture of a Score after Example 59 (Mazzola, The Topos
 * of Music, vol. III, p. 915) and lists its vertices.
 */
public class FingerGestureRubette extends SimpleAbstractRubette {

    static final String LIFT_KEY = "lift";
    static final String APPROACH_KEY = "approach";
    static final String RELEASE_KEY = "release";

    private static final ImageIcon icon = Icons.loadIcon(FingerGestureRubette.class, "fingergestureicon.png");

    private JPanel view;
    private JTextArea text;
    private Plot plot;
    private String reportText = "Not run yet.";
    private String info = "";
    private FingerGesture gesture;

    public FingerGestureRubette() {
        setInCount(1);
        setOutCount(0);
        putProperty(new DoubleProperty(LIFT_KEY, "Lift height above the key", 1.0, 1e-6, 1e6));
        putProperty(new DoubleProperty(APPROACH_KEY, "Approach time before the onset", 0.125, 0, 1e6));
        putProperty(new DoubleProperty(RELEASE_KEY, "Release time after the note ends", 0.125, 0, 1e6));
    }

    public void run(RunInfo runInfo) {
        process(getInput(0));
    }

    /** Builds the gesture of a score; on refusal, reports the reason as an error. */
    public void process(Denotator input) {
        try {
            FingerGestures.Report r = FingerGestures.analyze(input, settings());
            gesture = r.getGesture();
            reportText = r.text();
            Gesture g = gesture.gesture();
            info = String.format(Locale.ROOT, "%d arrows, %d backward", g.skeleton().arrowCount(),
                                 gesture.backwardMoves());
        }
        catch (IllegalArgumentException e) {
            gesture = null;
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
    }

    FingerGesture.Settings settings() {
        return new FingerGesture.Settings(((DoubleProperty) getProperty(LIFT_KEY)).getDouble(),
                                          ((DoubleProperty) getProperty(APPROACH_KEY)).getDouble(),
                                          ((DoubleProperty) getProperty(RELEASE_KEY)).getDouble());
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
            plot = new Plot();
            plot.setBorder(makeTitledBorder("Time and pitch; thick where the finger is on the key"));
            text = new JTextArea(10, 60);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Finger gesture"));
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, plot, scroll);
            split.setResizeWeight(0.6);
            view.add(split, BorderLayout.CENTER);
        }
        plot.repaint();
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
        return "Finger Gesture";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Draws the finger gesture of a score's notes";
    }

    public String getLongDescription() {
        return "The Finger Gesture rubette reads a zero-addressed Score and builds the gesture of one"
             + " finger playing its notes in onset order, after Example 59 of The Topos of Music:"
             + " for each note the finger goes down onto the key, holds it for the duration and"
             + " goes up, then moves to the next note. The curves lie in the space of height,"
             + " time and pitch. The view draws them in the time-pitch plane and lists the vertices;"
             + " the network box shows the number of arrows and of backward moves, where the next"
             + " note starts before the finger is free.";
    }

    public String getInTip(int i) {
        return "Score (zero-addressed)";
    }

    public String getOutTip(int i) {
        return "";
    }

    /** The time-pitch projection of the gesture. */
    private final class Plot extends JPanel {
        private static final long serialVersionUID = 1L;

        Plot() {
            setPreferredSize(new Dimension(560, 240));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fg = UIManager.getColor("Label.foreground");
            g2.setColor(fg == null ? getForeground() : fg);
            if (gesture == null) {
                g2.drawString("Run the rubette to draw the gesture.", 16, 40);
                g2.dispose();
                return;
            }
            Gesture g = gesture.gesture();
            double tMin = Double.MAX_VALUE, tMax = -Double.MAX_VALUE, pMin = Double.MAX_VALUE, pMax = -Double.MAX_VALUE;
            for (int v = 0; v < g.skeleton().vertexCount(); v++) {
                double[] p = g.vertex(v);
                tMin = Math.min(tMin, p[1]);
                tMax = Math.max(tMax, p[1]);
                pMin = Math.min(pMin, p[2]);
                pMax = Math.max(pMax, p[2]);
            }
            if (tMax - tMin < 1e-9) {
                tMax = tMin + 1;
            }
            pMin -= 1;
            pMax += 1;
            int left = 48, top = 28, w = Math.max(10, getWidth() - left - 20), h = Math.max(10, getHeight() - top - 30);
            g2.drawString(String.format(Locale.ROOT, "%.0f", pMax), 8, top + 10);
            g2.drawString(String.format(Locale.ROOT, "%.0f", pMin), 8, top + h);
            g2.drawString(String.format(Locale.ROOT, "time %.2f to %.2f", tMin, tMax), left, top + h + 18);
            for (int a = 0; a < g.skeleton().arrowCount(); a++) {
                double[] p = g.curve(a).at(0), q = g.curve(a).at(1);
                boolean onKey = p[0] == 0 && q[0] == 0;
                g2.setStroke(new BasicStroke(onKey ? 4f : 1.2f));
                int x0 = left + (int) Math.round((p[1] - tMin) / (tMax - tMin) * w);
                int x1 = left + (int) Math.round((q[1] - tMin) / (tMax - tMin) * w);
                int y0 = top + (int) Math.round((pMax - p[2]) / (pMax - pMin) * h);
                int y1 = top + (int) Math.round((pMax - q[2]) / (pMax - pMin) * h);
                g2.drawLine(x0, y0, x1, y1);
            }
            g2.dispose();
        }
    }
}
