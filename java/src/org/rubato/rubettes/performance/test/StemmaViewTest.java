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
package org.rubato.rubettes.performance.test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;

import junit.framework.TestCase;

import org.rubato.base.TextProperty;
import org.rubato.base.DoubleProperty;
import org.rubato.base.IntegerProperty;
import org.rubato.io.ScoreNote;
import org.rubato.math.performance.LPS;
import org.rubato.math.performance.PerfNote;
import org.rubato.rubettes.performance.KernelView;
import org.rubato.rubettes.performance.ScalarPerformanceRubette;
import org.rubato.rubettes.performance.StemmaBrowser;
import org.rubato.rubettes.performance.TempoPerformanceRubette;
import org.rubato.rubettes.performance.WeightWatcherView;

/**
 * The stemma browser, Kernel View and Weight Watcher view of the performance rubettes
 * (Mazzola, <i>The Topos of Music II</i>, &sect;41.4, Figs. 41.11, 41.12, 41.14, pp. 679-681).
 */
public class StemmaViewTest extends TestCase {

    private static final String FIG_41_14 =
          "Metro high=1.2 low=0.8 deformation=0.3 points=0:973,1:5000,2:6000,3:22138,4:7000,5:8000,6:9000,7:9331,8:9000\n"
        + "Motif high=1.1 low=0.9 inverted=yes influence=0.5 tolerance=2 points=2:5,6:1,10:4\n";

    private static TempoPerformanceRubette tempo(String weights) {
        TempoPerformanceRubette t = new TempoPerformanceRubette();
        ((DoubleProperty) t.getProperty("tempoStart")).setDouble(84);
        ((DoubleProperty) t.getProperty("tempoEnd")).setDouble(64);
        ((DoubleProperty) t.getProperty("barLength")).setDouble(4);
        ((DoubleProperty) t.getProperty("rubatoDepth")).setDouble(0.2);
        ((IntegerProperty) t.getProperty("rubatoVoice")).setInt(0);
        ((TextProperty) t.getProperty("weights")).setString(weights);
        t.process(ScoreNote.toScore(WeightedPerformanceTest.notes()));
        return t;
    }

    private static List<String> walk(TreeModel m, Object node, List<String> out) {
        out.add(node.toString());
        for (int i = 0; i < m.getChildCount(node); i++) {
            walk(m, m.getChild(node, i), out);
        }
        return out;
    }

    public void testTheViewHasTabsWithMnemonics() {
        TempoPerformanceRubette t = tempo("");
        t.getView();
        JTabbedPane tabs = t.getTabs();
        assertEquals(3, tabs.getTabCount());
        assertEquals("Report", tabs.getTitleAt(0));
        assertEquals("Stemma", tabs.getTitleAt(1));
        assertEquals("Weight Watcher", tabs.getTitleAt(2));
        assertEquals(KeyEvent.VK_R, tabs.getMnemonicAt(0));
        assertEquals(KeyEvent.VK_S, tabs.getMnemonicAt(1));
        assertEquals(KeyEvent.VK_W, tabs.getMnemonicAt(2));
        ScalarPerformanceRubette s = new ScalarPerformanceRubette();
        s.process(t.getOutput(0));
        s.getView();
        assertEquals("no Weight Watcher for Scalar Performance (D8)", 2, s.getTabs().getTabCount());
        assertNull(s.getWeightWatcherView());
        assertNotNull(s.getStemmaBrowser());
    }

    /** S1: every LPS once, mothers above daughters, with its own name. */
    public void testTheBrowserShowsEveryLpsOnce() {
        TempoPerformanceRubette t = tempo("");
        t.getView();
        JTree tree = t.getStemmaBrowser().getTree();
        List<String> rows = walk(tree.getModel(), tree.getModel().getRoot(), new ArrayList<String>());
        assertEquals(t.getStemma().size(), rows.size());
        assertEquals("Mother: mother, 24 notes", rows.get(0));
        assertEquals("Mother/v0: split voice 0, 16 notes", rows.get(1));
        assertEquals("Mother/v0/rubato: bar rubato, 16 notes", rows.get(2));
        assertEquals("Mother/v1: split voice 1, 8 notes", rows.get(3));
        for (int r = 0; r < tree.getRowCount(); r++) {
            assertTrue("expanded", tree.isExpanded(r) || tree.getModel().isLeaf(tree.getPathForRow(r).getLastPathComponent()));
        }
        assertEquals("all four rows visible", 4, tree.getRowCount());

        TempoPerformanceRubette w = tempo(FIG_41_14);
        w.getView();
        rows = walk(w.getStemmaBrowser().getTree().getModel(), w.getStemmaBrowser().getTree().getModel().getRoot(),
                new ArrayList<String>());
        assertEquals("Mother/weights: tempo weights, 24 notes", rows.get(1));
        assertEquals(5, rows.size());
    }

    /** S2: selecting an LPS shows its top kernel and its notes as text. */
    public void testSelectingShowsTheKernel() {
        TempoPerformanceRubette t = tempo("");
        t.getView();
        StemmaBrowser b = t.getStemmaBrowser();
        assertSame("the mother is selected first", t.getStemma().getMother(), b.getSelected());
        LPS v1 = t.getStemma().getMother().getDaughters().get(1);
        b.select(v1);
        assertSame(v1, b.getSelected());
        assertSame(v1, b.getKernelView().getLps());
        String details = b.getDetails().getText();
        assertTrue(details, details.contains("Mother/v1"));
        assertTrue(details, details.contains("Operator: split voice 1"));
        assertTrue(details, details.contains("Mother: Mother"));
        assertTrue(details, details.contains("8 notes"));
        for (PerfNote n : v1.getKernel()) {
            assertTrue(details, details.contains(KernelView.describe(n)));
        }
        String desc = b.getKernelView().getAccessibleContext().getAccessibleDescription();
        assertTrue(desc, desc.contains("8 notes"));
        assertTrue(desc, desc.contains("Mother/v1"));
        // selecting through the tree's own selection model does the same
        JTree tree = b.getTree();
        tree.setSelectionRow(1);
        assertEquals("Mother/v0", b.getSelected().getName());
    }

    /** S2: the grey level codes loudness: louder notes stand further from the background. */
    public void testGreyLevelCodesLoudness() {
        for (Color bg : new Color[] { new Color(0x3c3f41), Color.WHITE }) {
            Color fg = bg.equals(Color.WHITE) ? Color.BLACK : new Color(0xbbbbbb);
            double prev = -1;
            for (int l = 1; l <= 127; l += 7) {
                Color c = KernelView.fill(l, 1, 127, bg, fg);
                double d = Math.abs(lum(c) - lum(bg));
                assertTrue("loudness " + l + " on " + bg, d > prev);
                prev = d;
                assertEquals("grey", c.getRed(), c.getGreen(), 1);
                assertEquals("grey", c.getRed(), c.getBlue(), 1);
            }
        }
        Color one = KernelView.fill(60, 60, 60, Color.WHITE, Color.BLACK);
        assertNotNull("one loudness value draws at the strongest grey", one);
        assertEquals(KernelView.fill(127, 1, 127, Color.WHITE, Color.BLACK), one);
    }

    private static double lum(Color c) {
        double[] v = { c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0 };
        for (int i = 0; i < 3; i++) {
            v[i] = v[i] <= 0.04045 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }

    /** The Kernel View draws one outlined rectangle per note. */
    public void testTheKernelViewPaintsTheNotes() {
        TempoPerformanceRubette t = tempo("");
        t.getView();
        KernelView k = t.getStemmaBrowser().getKernelView();
        k.setSize(480, 240);
        BufferedImage img = new BufferedImage(480, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        k.paint(g);
        g.dispose();
        int bg = img.getRGB(1, 1), other = 0;
        for (int x = 0; x < 480; x++) {
            for (int y = 0; y < 240; y++) {
                if (img.getRGB(x, y) != bg) {
                    other++;
                }
            }
        }
        assertTrue("notes drawn: " + other, other > 2000);
        assertEquals(24, k.getRectangles().size());
        for (java.awt.Rectangle r : k.getRectangles()) {
            assertTrue("inside the view: " + r, r.x >= 0 && r.y >= 0 && r.x + r.width <= 480 && r.y + r.height <= 240);
            assertTrue("at least 3 px wide: " + r, r.width >= 3 && r.height >= 3);
        }
    }

    /** The Weight Watcher table and its text alternative (Fig. 41.14). */
    public void testTheWeightWatcherShowsEveryWeight() {
        TempoPerformanceRubette t = tempo(FIG_41_14);
        t.getView();
        WeightWatcherView v = t.getWeightWatcherView();
        JTable table = v.getTable();
        assertEquals(2, table.getRowCount());
        String[] cols = { "Weight", "Min", "Max", "Mean", "Norm. Mean", "High Norm", "Low Norm", "Influence",
                          "Deformation", "Inverted", "Tolerance" };
        assertEquals(cols.length, table.getColumnCount());
        for (int c = 0; c < cols.length; c++) {
            assertEquals(cols[c], table.getColumnName(c));
        }
        assertEquals("Metro", table.getValueAt(0, 0));
        assertEquals("973", table.getValueAt(0, 1));
        assertEquals("22138", table.getValueAt(0, 2));
        assertEquals("8493.5555555556", table.getValueAt(0, 3));
        assertEquals("0.9421319264", table.getValueAt(0, 4));
        assertEquals("no", table.getValueAt(0, 9));
        assertEquals("yes", table.getValueAt(1, 9));
        assertFalse(table.isCellEditable(0, 5));
        assertEquals("Combine as product: no", v.getCombineLabel().getText());
        String values = v.getValues().getText();
        assertTrue(values, values.contains("Combined weight (sum, 39.5)"));
        assertTrue(values, values.contains("E = -1"));
        assertTrue(values, values.contains("E = 12"));
        double w2 = t.getWeightWatcher().combined().applyAsDouble(2);
        assertTrue(values, values.contains(String.format(java.util.Locale.ROOT, "E = 2: %.6f", w2)));

        TempoPerformanceRubette none = tempo("");
        none.getView();
        assertEquals(0, none.getWeightWatcherView().getTable().getRowCount());
        assertTrue(none.getWeightWatcherView().getValues().getText().contains("No weights"));
    }

    public void testAccessibleNamesAndKeyboardReach() {
        TempoPerformanceRubette t = tempo(FIG_41_14);
        t.getView();
        StemmaBrowser b = t.getStemmaBrowser();
        WeightWatcherView v = t.getWeightWatcherView();
        javax.swing.JComponent[] named = { b.getTree(), b.getKernelView(), b.getDetails(), v.getTable(), v.getPlot(),
                                           v.getValues(), t.getReportArea() };
        for (javax.swing.JComponent c : named) {
            String n = c.getAccessibleContext().getAccessibleName();
            assertNotNull(c.getClass().getSimpleName(), n);
            assertTrue(c.getClass().getSimpleName() + " name", n.length() > 3);
            assertTrue(c.getClass().getSimpleName() + " focusable", c.isFocusable());
        }
        String pd = v.getPlot().getAccessibleContext().getAccessibleDescription();
        assertTrue(pd, pd.contains("2 weights"));
        assertTrue(pd, pd.contains("from E = -1 to E = 12"));
        // Tab and Shift+Tab leave the read-only table and text areas instead of moving between cells
        javax.swing.KeyStroke tab = javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
        javax.swing.KeyStroke back = javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK);
        for (javax.swing.JComponent c : new javax.swing.JComponent[] { v.getTable(), v.getValues(), b.getDetails(),
                                                                       t.getReportArea() }) {
            assertTrue(c.getClass().getSimpleName() + " Tab",
                    c.getFocusTraversalKeys(java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS).contains(tab));
            assertTrue(c.getClass().getSimpleName() + " Shift+Tab",
                    c.getFocusTraversalKeys(java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS).contains(back));
        }
        assertFalse("the values text claims the weights stay 1 outside their frames",
                v.getValues().getText().contains("each weight is 1 outside"));
    }

    public void testBeforeARunAndAfterARefusal() {
        TempoPerformanceRubette t = new TempoPerformanceRubette();
        t.getView();
        assertNull(t.getStemmaBrowser().getTree().getModel().getRoot());
        assertTrue(t.getStemmaBrowser().getDetails().getText(), t.getStemmaBrowser().getDetails().getText().contains("Not run yet"));
        t = tempo(FIG_41_14);
        t.getView();
        ((TextProperty) t.getProperty("weights")).setString("broken");
        t.process(ScoreNote.toScore(WeightedPerformanceTest.notes()));
        t.updateView();
        assertNull(t.getStemmaBrowser().getTree().getModel().getRoot());
        assertEquals(0, t.getWeightWatcherView().getTable().getRowCount());
        assertTrue(t.getStemmaBrowser().getDetails().getText(), t.getStemmaBrowser().getDetails().getText().contains("line 1"));
    }
}
