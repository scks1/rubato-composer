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
import java.awt.geom.Line2D;
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
import org.rubato.base.IntegerProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.base.StringProperty;
import org.rubato.composer.RunInfo;
import org.rubato.composer.icons.Icons;
import org.rubato.math.performance.FieldExtraction;
import org.rubato.math.performance.FieldExtraction.Case;
import org.rubato.math.yoneda.Denotator;

/**
 * Extracts the performance field of a performed Score relative to its score (Mazzola,
 * <i>The Topos of Music II</i>, &sect;47.3, pp. 759-765, after Stefan M&uuml;ller), draws two of
 * its components as arrows on the onset-pitch plane over the colour-coded interpolated field,
 * and lists every vector. See {@link FieldExtractions} for the departures from the book.
 */
public class FieldExtractionRubette extends SimpleAbstractRubette {

    public static final String COORDINATES_KEY = "coordinates";
    public static final String COMPONENTS_KEY = "components";
    static final String NEIGHBORS_KEY = "maxneighbors";
    static final String DIST_KEY = "maxdist";
    static final String LOC_KEY = "wloc";
    static final String ORTH_KEY = "worth";
    static final String MARGIN_KEY = "margin";
    static final String COLOUR_KEY = "colour";
    static final String CYCLES_KEY = "cycles";

    private static final ImageIcon icon = Icons.loadIcon(FieldExtractionRubette.class, "tempoperformanceicon.png");

    private JPanel view;
    private JTextArea text;
    private Plot plot;
    private String reportText = "Not run yet.";
    private String info = "";
    private FieldExtractions.Report report;

    public FieldExtractionRubette() {
        setInCount(2);
        setOutCount(0);
        FieldExtraction.Settings d = FieldExtraction.Settings.defaults();
        putProperty(new StringProperty(COORDINATES_KEY, "Coordinates of the field (letters from E, H, L, D)", "EHLD", 1, 4));
        putProperty(new StringProperty(COMPONENTS_KEY, "Two components to draw and colour", "ED", 2, 2));
        putProperty(new IntegerProperty(NEIGHBORS_KEY, "Nearest notes tried for each basis", d.getMaxNeighbors(), 1, 16));
        putProperty(new DoubleProperty(DIST_KEY, "Largest neighbour distance, 0 for no limit", 0, 0, 1e6));
        putProperty(new DoubleProperty(LOC_KEY, "Weight of locality (small determinant)", d.getLocalityWeight(), 1e-6, 1e6));
        putProperty(new DoubleProperty(ORTH_KEY, "Weight of orthogonality", d.getOrthogonalityWeight(), 1e-6, 1e6));
        putProperty(new DoubleProperty(MARGIN_KEY, "Frame margin of the interpolation", 1, 1e-6, 1e6));
        putProperty(new BooleanProperty(COLOUR_KEY, "Colour the interpolated field", true));
        putProperty(new IntegerProperty(CYCLES_KEY, "Colour cycles per turn of the vector", 1, 1, 12));
    }

    public void run(RunInfo runInfo) {
        process(getInput(0), getInput(1));
    }

    /** Extracts the field of a performance; on refusal, reports the reason as an error. */
    public void process(Denotator score, Denotator performance) {
        try {
            report = FieldExtractions.analyze(score, performance,
                                              ((StringProperty) getProperty(COORDINATES_KEY)).getString(),
                                              ((StringProperty) getProperty(COMPONENTS_KEY)).getString(), settings(),
                                              ((DoubleProperty) getProperty(MARGIN_KEY)).getDouble());
            reportText = report.text();
            info = FieldExtractions.summary(report.result());
        }
        catch (IllegalArgumentException e) {
            report = null;
            reportText = e.getMessage();
            info = "";
            addError(e.getMessage());
        }
        if (plot != null) {
            plot.describe();
        }
    }

    FieldExtraction.Settings settings() {
        FieldExtraction.Settings s = FieldExtraction.Settings.defaults()
            .withMaxNeighbors(((IntegerProperty) getProperty(NEIGHBORS_KEY)).getInt())
            .withWeights(((DoubleProperty) getProperty(LOC_KEY)).getDouble(),
                         ((DoubleProperty) getProperty(ORTH_KEY)).getDouble());
        double dist = ((DoubleProperty) getProperty(DIST_KEY)).getDouble();
        return dist > 0 ? s.withMaxDist(dist) : s;
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
            plot.setBorder(makeTitledBorder("Pitch over onset; arrows show two field components"));
            text = new JTextArea(10, 60);
            text.setEditable(false);
            text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, text.getFont().getSize()));
            text.setBorder(new EmptyBorder(4, 6, 4, 6));
            text.getAccessibleContext().setAccessibleName("Performance field report");
            text.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                    Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
            text.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                    Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(makeTitledBorder("Performance field"));
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
        return "Performance";
    }

    public String getName() {
        return "Field Extraction";
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getShortDescription() {
        return "Extracts the performance field of a performed score";
    }

    public String getLongDescription() {
        return "The Field Extraction rubette reads a zero-addressed Score (input 1) and a performance of it"
             + " (input 2, for example the output of the Tempo or Scalar Performance rubette) and extracts the"
             + " discrete performance field after section 47.3 of The Topos of Music: for each note a basis of"
             + " differences to nearby notes, chosen for small determinant and near orthogonality, its image in"
             + " the performance, and the field vector, which is the inverse of that local linear map applied"
             + " to the diagonal. A singular image gives only a direction or no information. Notes of each"
             + " pitch are matched in onset order, so every note must be played exactly once. The view draws"
             + " two chosen components as arrows on the onset-pitch plane, colours the interpolated field,"
             + " and the report lists every vector. Performed Scores count quarters at M.M. 120.";
    }

    public String getInTip(int i) {
        return i == 0 ? "Score (zero-addressed)" : "Performed Score (zero-addressed)";
    }

    public String getOutTip(int i) {
        return "";
    }

    /** The onset-pitch plane with the notes, their field arrows and the coloured field. */
    private final class Plot extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int CELL = 4;
        private static final double ARROW = 26;

        Plot() {
            setPreferredSize(new Dimension(560, 300));
            setFocusable(true);
            getAccessibleContext().setAccessibleName("Performance field plot");
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
            if (report == null) {
                getAccessibleContext().setAccessibleDescription("No performance field yet; run the rubette.");
                return;
            }
            FieldExtraction.Result r = report.result();
            String comps = componentLetters();
            StringBuilder b = new StringBuilder("Pitch over onset, ").append(r.vectors().size()).append(" notes, ")
                .append(r.count(Case.REGULAR)).append(" regular, ").append(r.count(Case.DIRECTION))
                .append(" direction only, ").append(r.count(Case.NONE)).append(" no information. Arrows show the ")
                .append(comps).append(" components");
            if (colour() && report.interpolation() != null) {
                b.append("; colour shows the interpolated field, green along the diagonal");
            }
            b.append(". The report beside the plot lists every value.");
            getAccessibleContext().setAccessibleDescription(b.toString());
        }

        private String componentLetters() {
            String s = ((StringProperty) getProperty(COMPONENTS_KEY)).getString().replaceAll("\\s", "")
                .toUpperCase(Locale.ROOT);
            return s.length() == 2 ? s.charAt(0) + " and " + s.charAt(1) : s;
        }

        private boolean colour() {
            return ((BooleanProperty) getProperty(COLOUR_KEY)).getBoolean();
        }

        private Color fg() {
            Color c = UIManager.getColor("Label.foreground");
            return c == null ? getForeground() : c;
        }

        private Color bg() {
            Color c = UIManager.getColor("Panel.background");
            return c == null ? getBackground() : c;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fg = fg();
            g2.setColor(fg);
            if (report == null) {
                g2.drawString("Run the rubette to draw the field.", 16, 40);
            }
            else {
                paintField(g2);
            }
            if (isFocusOwner()) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
            g2.dispose();
        }

        private void paintField(Graphics2D g2) {
            double[][] notes = report.scoreNotes();
            double eMin = Double.MAX_VALUE, eMax = -Double.MAX_VALUE, hMin = Double.MAX_VALUE, hMax = -Double.MAX_VALUE;
            for (double[] n : notes) {
                eMin = Math.min(eMin, n[0]);
                eMax = Math.max(eMax, n[0] + n[3]);
                hMin = Math.min(hMin, n[1]);
                hMax = Math.max(hMax, n[1] + 1);
            }
            eMin -= 0.5;
            eMax += 0.5;
            hMin -= 2;
            hMax += 2;
            int left = 48, right = 20, top = 28, bottom = 30;
            int w = Math.max(10, getWidth() - left - right), h = Math.max(10, getHeight() - top - bottom);
            Color fg = fg(), bg = bg();
            FieldExtraction.Interpolation f = report.interpolation();
            int[] comp = report.components();
            double maxLen = 0;
            for (FieldExtraction.Vector v : report.result().vectors()) {
                if (v.getCase() != Case.NONE) {
                    double[] z = v.value();
                    maxLen = Math.max(maxLen, Math.hypot(z[comp[0]], z[comp[1]]));
                }
            }
            if (colour() && f != null) {
                int cycles = ((IntegerProperty) getProperty(CYCLES_KEY)).getInt();
                for (int px = 0; px < w; px += CELL) {
                    for (int py = 0; py < h; py += CELL) {
                        double e = eMin + (px + CELL / 2.0) / w * (eMax - eMin);
                        double p = hMax - (py + CELL / 2.0) / h * (hMax - hMin);
                        double[] z = f.at(new double[] { e, p });
                        float hue = (float) FieldExtractions.hue(z[0], z[1], cycles);
                        float bri = (float) FieldExtractions.brightness(Math.hypot(z[0], z[1]), maxLen);
                        g2.setColor(Color.getHSBColor(hue, 0.55f, bri));
                        g2.fillRect(left + px, top + py, Math.min(CELL, w - px), Math.min(CELL, h - py));
                    }
                }
            }
            g2.setColor(fg);
            g2.drawRect(left, top, w, h);
            g2.drawString(String.format(Locale.ROOT, "%.0f", hMax), 8, top + 10);
            g2.drawString(String.format(Locale.ROOT, "%.0f", hMin), 8, top + h);
            g2.drawString(String.format(Locale.ROOT, "onset %.2f to %.2f", eMin, eMax), left, top + h + 18);
            BasicStroke halo = new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            BasicStroke thin = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            BasicStroke dashed = new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f,
                                                 new float[] { 4f, 3f }, 0f);
            for (FieldExtraction.Vector v : report.result().vectors()) {
                double[] n = notes[v.index];
                double x0 = left + (n[0] - eMin) / (eMax - eMin) * w;
                double x1 = left + (n[0] + n[3] - eMin) / (eMax - eMin) * w;
                double y0 = top + (hMax - n[1] - 1) / (hMax - hMin) * h;
                double y1 = top + (hMax - n[1]) / (hMax - hMin) * h;
                java.awt.geom.Rectangle2D.Double rect = new java.awt.geom.Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0);
                g2.setStroke(halo);
                g2.setColor(bg);
                g2.draw(rect);
                g2.setStroke(thin);
                g2.setColor(fg);
                g2.draw(rect);
                double cx = x0, cy = (y0 + y1) / 2;
                if (v.getCase() == Case.NONE) {
                    Path2D.Double cross = new Path2D.Double();
                    cross.moveTo(cx - 4, cy - 4);
                    cross.lineTo(cx + 4, cy + 4);
                    cross.moveTo(cx - 4, cy + 4);
                    cross.lineTo(cx + 4, cy - 4);
                    drawHaloed(g2, cross, halo, thin, bg, fg);
                    continue;
                }
                double[] z = v.value();
                double dx = z[comp[0]], dy = z[comp[1]];
                double len = Math.hypot(dx, dy);
                if (len == 0 || maxLen == 0) {
                    continue;
                }
                double sc = ARROW / maxLen;
                double ex = cx + dx * sc, ey = cy - dy * sc;
                Path2D.Double arrow = new Path2D.Double();
                arrow.append(new Line2D.Double(cx, cy, ex, ey), false);
                double ux = (ex - cx) / Math.hypot(ex - cx, ey - cy), uy = (ey - cy) / Math.hypot(ex - cx, ey - cy);
                arrow.moveTo(ex, ey);
                arrow.lineTo(ex - 7 * ux + 4 * uy, ey - 7 * uy - 4 * ux);
                arrow.moveTo(ex, ey);
                arrow.lineTo(ex - 7 * ux - 4 * uy, ey - 7 * uy + 4 * ux);
                drawHaloed(g2, arrow, halo, v.getCase() == Case.DIRECTION ? dashed : thin, bg, fg);
            }
        }

        private void drawHaloed(Graphics2D g2, java.awt.Shape s, BasicStroke halo, BasicStroke line, Color bg, Color fg) {
            g2.setStroke(halo);
            g2.setColor(bg);
            g2.draw(s);
            g2.setStroke(line);
            g2.setColor(fg);
            g2.draw(s);
        }
    }
}
