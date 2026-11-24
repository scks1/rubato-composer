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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.Locale;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.rubato.math.performance.WeightWatcher;

/**
 * The Weight Watcher of the PerformanceRUBETTE (Mazzola, <i>The Topos of Music II</i>,
 * &sect;41.4, Fig. 41.14, p. 681), read only (departure D9 of sources/STEMMA-VIEW-SPEC.md):
 * one table row per weight with its limits and parameters, the combination flag, a plot of
 * the combined weight across the frame and, as the plot's text alternative, the combined
 * weight at every weight onset and at the frame ends.
 */
public class WeightWatcherView extends JPanel {

    static final String[] COLUMNS = { "Weight", "Min", "Max", "Mean", "Norm. Mean", "High Norm", "Low Norm",
                                      "Influence", "Deformation", "Inverted", "Tolerance" };

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel combine = new JLabel();
    private final Plot plot = new Plot();
    private final JTextArea values = new JTextArea(6, 40);

    /** The plot of the combined weight. */
    static final class Plot extends JComponent {
        private WeightWatcher watcher;

        Plot() {
            setOpaque(true);
            setFocusable(true);
            setPreferredSize(new Dimension(480, 160));
            getAccessibleContext().setAccessibleName("Combined weight plot");
            getAccessibleContext().setAccessibleDescription("No weights.");
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { repaint(); }
                public void focusLost(FocusEvent e) { repaint(); }
            });
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
            });
        }

        /** A drawing for assistive technology: role canvas, with the name and description set here. */
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJComponent() {
                    public AccessibleRole getAccessibleRole() {
                        return AccessibleRole.CANVAS;
                    }
                };
            }
            return accessibleContext;
        }

        void setWatcher(WeightWatcher w) {
            watcher = w;
            if (w == null || w.isEmpty()) {
                getAccessibleContext().setAccessibleDescription("No weights.");
            }
            else {
                double[] mm = minMax(w);
                int k = w.getEntries().size();
                getAccessibleContext().setAccessibleDescription("Plot of the combined weight of " + k
                        + (k == 1 ? " weight" : " weights") + " from E = " + WeightWatcher.format(w.frameStart())
                        + " to E = " + WeightWatcher.format(w.frameEnd()) + ", between "
                        + String.format(Locale.ROOT, "%.3f", mm[0]) + " and " + String.format(Locale.ROOT, "%.3f", mm[1])
                        + ". The values are listed in the text below the plot.");
            }
            repaint();
        }

        static double[] minMax(WeightWatcher w) {
            DoubleUnaryOperator f = w.combined();
            double a = w.frameStart(), b = w.frameEnd(), lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
            for (int i = 0; i <= 400; i++) {
                double v = f.applyAsDouble(a + (b - a) * i / 400);
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            }
            return new double[] { lo, hi };
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Color bg = KernelView.background(), fg = KernelView.foreground();
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int m = 10, w = getWidth() - 2 * m, h = getHeight() - 2 * m;
            if (watcher != null && !watcher.isEmpty() && w > 0 && h > 0) {
                DoubleUnaryOperator f = watcher.combined();
                double a = watcher.frameStart(), b = watcher.frameEnd();
                double[] mm = minMax(watcher);
                double lo = Math.min(mm[0], 1), hi = Math.max(mm[1], 1);
                if (hi - lo < 1e-9) {
                    hi = lo + 1;
                }
                double pad = (hi - lo) * 0.08;
                lo -= pad;
                hi += pad;
                // the dashed reference line at weight 1
                g.setColor(fg);
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                        new float[] { 4f, 4f }, 0f));
                int y1 = m + (int) Math.round((hi - 1) / (hi - lo) * h);
                g.drawLine(m, y1, m + w, y1);
                g.setStroke(new BasicStroke(2f));
                Path2D.Double p = new Path2D.Double();
                for (int i = 0; i <= w; i++) {
                    double e = a + (b - a) * i / w;
                    double y = m + (hi - f.applyAsDouble(e)) / (hi - lo) * h;
                    if (i == 0) {
                        p.moveTo(m + i, y);
                    }
                    else {
                        p.lineTo(m + i, y);
                    }
                }
                g.draw(p);
            }
            if (isFocusOwner()) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setColor(fg);
                g.setStroke(new BasicStroke(2f));
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
            g.dispose();
        }
    }

    public WeightWatcherView() {
        super(new BorderLayout());
        table.setFillsViewportHeight(true);
        table.getAccessibleContext().setAccessibleName("Weight Watcher weights");
        table.getAccessibleContext().setAccessibleDescription(
                "One row per weight: limits of its values, normalized mean and the Weight Watcher parameters.");
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(640, 110));
        values.setEditable(false);
        values.setFont(new Font(Font.MONOSPACED, Font.PLAIN, values.getFont().getSize()));
        values.setBorder(new EmptyBorder(4, 6, 4, 6));
        values.getAccessibleContext().setAccessibleName("Combined weight values");
        combine.setBorder(new EmptyBorder(4, 6, 4, 6));
        JPanel top = new JPanel(new BorderLayout());
        top.add(tableScroll, BorderLayout.CENTER);
        top.add(combine, BorderLayout.SOUTH);
        JLabel plotLabel = new JLabel("Combined weight (dashed line: weight 1)");
        plotLabel.setLabelFor(plot);
        plotLabel.setBorder(new EmptyBorder(2, 6, 2, 6));
        JPanel plotPanel = new JPanel(new BorderLayout());
        plotPanel.add(plotLabel, BorderLayout.NORTH);
        plotPanel.add(plot, BorderLayout.CENTER);
        JSplitPane lower = new JSplitPane(JSplitPane.VERTICAL_SPLIT, plotPanel, new JScrollPane(values));
        lower.setResizeWeight(0.6);
        JSplitPane all = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, lower);
        all.setResizeWeight(0.3);
        add(all, BorderLayout.CENTER);
        setWatcher(null);
    }

    /** Shows a Weight Watcher; null or an empty one shows that there are no weights. */
    public void setWatcher(WeightWatcher w) {
        model.setRowCount(0);
        plot.setWatcher(w);
        combine.setText("Combine as product: " + (w != null && w.isProduct() ? "yes" : "no"));
        if (w == null || w.isEmpty()) {
            values.setText("No weights. Type them in the rubette's properties, one per line:\n"
                    + "name high=1.2 low=0.8 [deformation=0] [inverted=no] [influence=1] [tolerance=1]"
                    + " points=E:w,E:w,...");
            values.setCaretPosition(0);
            return;
        }
        TreeMap<Double, String> at = new TreeMap<Double, String>();
        for (WeightWatcher.Entry e : w.getEntries()) {
            model.addRow(new Object[] { e.getWeight().getName(), WeightWatcher.format(e.getWeight().min()),
                    WeightWatcher.format(e.getWeight().max()), WeightWatcher.format(e.getWeight().mean()),
                    WeightWatcher.format(e.normMean()), WeightWatcher.format(e.getHigh()),
                    WeightWatcher.format(e.getLow()), WeightWatcher.format(e.getInfluence()),
                    WeightWatcher.format(e.getDeformation()), e.isInverted() ? "yes" : "no",
                    WeightWatcher.format(e.getTolerance()) });
            for (double x : e.getWeight().getOnsets()) {
                at.put(x, "");
            }
        }
        at.put(w.frameStart(), " (frame start)");
        at.put(w.frameEnd(), " (frame end)");
        DoubleUnaryOperator f = w.combined();
        StringBuilder b = new StringBuilder("Combined weight (" + (w.isProduct() ? "product, 39.7" : "sum, 39.5")
                + ") at the weight onsets and the frame ends; each weight is 1 outside its frame before its influence:\n");
        for (java.util.Map.Entry<Double, String> x : at.entrySet()) {
            b.append(String.format(Locale.ROOT, "E = %s: %.6f%s\n", WeightWatcher.format(x.getKey()),
                    f.applyAsDouble(x.getKey()), x.getValue()));
        }
        values.setText(b.toString());
        values.setCaretPosition(0);
    }

    public JTable getTable() {
        return table;
    }

    public JLabel getCombineLabel() {
        return combine;
    }

    public JComponent getPlot() {
        return plot;
    }

    public JTextArea getValues() {
        return values;
    }
}
