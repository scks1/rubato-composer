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
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.Collections;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.rubato.base.BooleanProperty;
import org.rubato.base.DoubleProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.base.TextProperty;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.math.gesture.HandGesture;
import org.rubato.math.yoneda.Denotator;

/**
 * Builds the hand gesture of a Score after Example 60 (Mazzola, The Topos of
 * Music, vol. III, p. 915) from a fingering, draws the fingertips and the
 * carpus over time and reports reshapes, shortened notes and problems.
 */
public class HandGestureRubette extends SimpleAbstractRubette {

    public static final String FINGERING_KEY = "fingering";
    public static final String LEFT_KEY = "lefthand";
    static final String ACCEL_KEY = "accel";
    static final String SPU_KEY = "secondsperunit";
    static final String LIFT_KEY = "lift";
    static final String SHORTEN_KEY = "maxshortening";

    /** Dash patterns of fingers 1 to 5 and the carpus; null is a solid line. */
    private static final float[][] DASHES = { null, { 8f, 4f }, { 2f, 3f }, { 10f, 3f, 2f, 3f }, { 4f, 4f },
                                              { 1f, 6f } };
    private static final String[] LABELS = { "1", "2", "3", "4", "5", "carpus" };

    private static final ImageIcon icon = Icons.loadIcon(HandGestureRubette.class, "fingergestureicon.png", "handgesture");

    private JPanel view;
    private JTextArea text;
    private Plot plot;
    private String reportText = "Not run yet.";
    private String info = "";
    private HandGesture gesture;

    public HandGestureRubette() {
        setInCount(1);
        setOutCount(0);
        HandGesture.Settings d = HandGesture.Settings.defaults();
        putProperty(new TextProperty(FINGERING_KEY, "Fingering, one finger (1 thumb to 5) per note in onset order",
                                     "", 60, 5, false, false));
        putProperty(new BooleanProperty(LEFT_KEY, "Left hand", false));
        putProperty(new DoubleProperty(ACCEL_KEY, "Acceleration limit of a fingertip (cm/s^2)", d.accel[0], 1e-6, 1e9));
        putProperty(new DoubleProperty(SPU_KEY, "Seconds per score time unit", d.secondsPerUnit, 1e-6, 1e6));
        putProperty(new DoubleProperty(LIFT_KEY, "Lift height above the key (cm)", d.lift, 1e-6, 1e3));
        putProperty(new DoubleProperty(SHORTEN_KEY, "Largest shortening, as a fraction of the duration",
                                       d.maxShortening, 0, 0.999));
    }

    public void run(RunInfo runInfo) {
        process(getInput(0));
    }

    /** Builds the gesture of a score; on refusal, reports the reason as an error. */
    public void process(Denotator input) {
        try {
            HandGestures.Report r = HandGestures.analyze(input, ((TextProperty) getProperty(FINGERING_KEY)).getString(),
                                                         settings());
            gesture = r.getGesture();
            reportText = r.text();
            info = String.format(Locale.ROOT, "%s, %d reshapes", gesture.performable() ? "performable" : "not performable",
                                 gesture.reshapes().size());
        }
        catch (IllegalArgumentException e) {
            gesture = null;
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
        if (plot != null) {
            plot.describe();
        }
    }

    HandGesture.Settings settings() {
        HandGesture.Settings s = HandGesture.Settings.defaults();
        if (((BooleanProperty) getProperty(LEFT_KEY)).getBoolean()) {
            s = s.withHand(HandGesture.Hand.LEFT);
        }
        return s.withAccel(((DoubleProperty) getProperty(ACCEL_KEY)).getDouble())
                .withSecondsPerUnit(((DoubleProperty) getProperty(SPU_KEY)).getDouble())
                .withLift(((DoubleProperty) getProperty(LIFT_KEY)).getDouble())
                .withMaxShortening(((DoubleProperty) getProperty(SHORTEN_KEY)).getDouble());
    }

    /** The text the view shows. */
    public String getReportText() {
        return reportText;
    }

    /** The plot of the view, after {@link #getView()}. */
    public JComponent getPlot() {
        return plot;
    }

    /** The report text area of the view, after {@link #getView()}. */
    public JComponent getTextArea() {
        return text;
    }

    public boolean hasView() {
        return true;
    }

    public JComponent getView() {
        if (view == null) {
            view = new JPanel(new BorderLayout());
            plot = new Plot();
            plot.setBorder(makeTitledBorder("Pitch over time; thick where the fingertip is on the key"));
            text = new JTextArea(10, 60);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            text.getAccessibleContext().setAccessibleName("Hand gesture report");
            text.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                    Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
            text.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                    Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Hand gesture"));
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, plot, scroll);
            split.setResizeWeight(0.6);
            view.add(split, BorderLayout.CENTER);
        }
        plot.describe();
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
        return "Hand Gesture";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Builds the gesture of a pianist's hand playing a score with a given fingering";
    }

    public String getLongDescription() {
        return "The Hand Gesture rubette reads a zero-addressed Score and a fingering, one finger per"
             + " note in onset order (notes with the same onset by ascending pitch), and builds the"
             + " gesture of the hand after Example 60 of The Topos of Music: five fingertip curves and"
             + " the carpus in the space of pitch, height, front position and time. Free fingers move"
             + " when a span or the finger order requires it (a reshape); moves take the time an"
             + " acceleration limit allows, and a note may end early so the finger reaches its next"
             + " key. The view draws pitch over time and lists reshapes, shortened notes and any"
             + " reason the score cannot be played this way. Spans and the other hand settings are"
             + " fixed defaults, not values from the book.";
    }

    public String getInTip(int i) {
        return "Score (zero-addressed)";
    }

    public String getOutTip(int i) {
        return "";
    }

    /** The time-pitch projection of the physical fingertips and the carpus. */
    private final class Plot extends JPanel {
        private static final long serialVersionUID = 1L;

        Plot() {
            setPreferredSize(new Dimension(560, 260));
            setFocusable(true);
            getAccessibleContext().setAccessibleName("Hand gesture plot");
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                }
            });
            describe();
        }

        void describe() {
            if (gesture == null) {
                getAccessibleContext().setAccessibleDescription("No hand gesture yet; run the rubette.");
                return;
            }
            StringBuilder b = new StringBuilder("Pitch over time of the five fingertips and the carpus, "
                                                + gesture.noteCount() + " notes, "
                                                + (gesture.performable() ? "performable" : "not performable") + ".");
            for (int f = 1; f <= 5; f++) {
                int k = gesture.moves(f).size();
                b.append(String.format(Locale.ROOT, " finger %d: %d %s;", f, k, k == 1 ? "move" : "moves"));
            }
            b.append(" carpus follows the mean fingertip pitch. The report beside the plot lists every value.");
            getAccessibleContext().setAccessibleDescription(b.toString());
        }

        private Color fg() {
            Color c = UIManager.getColor("Label.foreground");
            return c == null ? getForeground() : c;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fg = fg();
            g2.setColor(fg);
            if (gesture == null) {
                g2.drawString("Run the rubette to draw the gesture.", 16, 40);
            }
            else {
                paintGesture(g2);
            }
            if (isFocusOwner()) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
            g2.dispose();
        }

        private void paintGesture(Graphics2D g2) {
            double[][] notes = gesture.notes();
            double tMin = Double.MAX_VALUE, tMax = -Double.MAX_VALUE, pMin = Double.MAX_VALUE, pMax = -Double.MAX_VALUE;
            for (double[] n : notes) {
                tMin = Math.min(tMin, n[0]);
                tMax = Math.max(tMax, n[0] + n[1]);
            }
            for (int f = 1; f <= 5; f++) {
                for (HandGesture.Move m : gesture.moves(f)) {
                    tMin = Math.min(tMin, m.start);
                    tMax = Math.max(tMax, m.end);
                }
            }
            if (tMax - tMin < 1e-9) {
                tMax = tMin + 1;
            }
            int samples = 400;
            double[][][] pts = new double[6][samples + 1][];
            for (int k = 0; k <= samples; k++) {
                double t = tMin + k * (tMax - tMin) / samples;
                for (int f = 1; f <= 5; f++) {
                    pts[f - 1][k] = gesture.position(f, t);
                }
                pts[5][k] = gesture.carpus(t);
                for (int c = 0; c < 6; c++) {
                    pMin = Math.min(pMin, pts[c][k][0]);
                    pMax = Math.max(pMax, pts[c][k][0]);
                }
            }
            pMin -= 1;
            pMax += 1;
            int left = 48, right = 64, top = 28;
            int w = Math.max(10, getWidth() - left - right), h = Math.max(10, getHeight() - top - 30);
            g2.drawString(String.format(Locale.ROOT, "%.0f", pMax), 8, top + 10);
            g2.drawString(String.format(Locale.ROOT, "%.0f", pMin), 8, top + h);
            g2.drawString(String.format(Locale.ROOT, "time %.2f to %.2f", tMin, tMax), left, top + h + 18);
            int[] labelY = new int[6];
            for (int c = 0; c < 6; c++) {
                // one dashed path for the whole curve keeps the dash phase continuous; held keys go on top, thick
                Path2D.Double line = new Path2D.Double(), held = new Path2D.Double();
                int lastX = 0, lastY = 0;
                for (int k = 0; k <= samples; k++) {
                    double[] p = pts[c][k];
                    int x = left + (int) Math.round((double) k / samples * w);
                    int y = top + (int) Math.round((pMax - p[0]) / (pMax - pMin) * h);
                    if (k == 0) {
                        line.moveTo(x, y);
                    }
                    else {
                        line.lineTo(x, y);
                        if (c < 5 && p[1] < 1e-9 && pts[c][k - 1][1] < 1e-9) {
                            held.moveTo(lastX, lastY);
                            held.lineTo(x, y);
                        }
                    }
                    lastX = x;
                    lastY = y;
                }
                g2.setStroke(DASHES[c] == null ? new BasicStroke(1.4f)
                    : new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DASHES[c], 0f));
                g2.draw(line);
                g2.setStroke(new BasicStroke(4f));
                g2.draw(held);
                labelY[c] = lastY;
            }
            // labels at the right end of each curve, pushed apart so that none overlaps another
            Integer[] order = { 0, 1, 2, 3, 4, 5 };
            java.util.Arrays.sort(order, (a, b) -> Integer.compare(labelY[a], labelY[b]));
            int gap = g2.getFontMetrics().getHeight(), prev = Integer.MIN_VALUE / 2;
            for (int c : order) {
                int y = Math.max(labelY[c] + 4, prev + gap);
                g2.drawString(LABELS[c], left + w + 6, y);
                prev = y;
            }
        }
    }
}
