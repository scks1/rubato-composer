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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Enumeration;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.rubato.math.performance.LPS;
import org.rubato.math.performance.PerfNote;
import org.rubato.math.performance.Stemma;

/**
 * The stemma browser of the PerformanceRUBETTE (Mazzola, <i>The Topos of Music II</i>,
 * &sect;41.4, Fig. 41.11, p. 679): every LPS of the stemma under its mother, each with its own
 * name, here as a tree (departure D1 of sources/STEMMA-VIEW-SPEC.md). Selecting an LPS shows
 * its top kernel in the Kernel View (Fig. 41.12) and, as text, its operator, mother,
 * daughters and kernel notes.
 */
public class StemmaBrowser extends JPanel {

    /** A tree node's user object: the LPS, shown as "name: operator, n notes". */
    private static final class Item {
        final LPS lps;
        Item(LPS lps) { this.lps = lps; }
        public String toString() {
            return lps.getName() + ": " + lps.getOperator() + ", " + lps.getKernel().size()
                 + (lps.getKernel().size() == 1 ? " note" : " notes");
        }
    }

    private final JTree tree;
    private final KernelView kernel = new KernelView();
    private final JTextArea details = new JTextArea(8, 40);
    private LPS selected;

    public StemmaBrowser() {
        super(new BorderLayout());
        tree = new JTree(new DefaultTreeModel(null));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getAccessibleContext().setAccessibleName("Stemma");
        tree.getAccessibleContext().setAccessibleDescription(
                "The local performance scores of the stemma, each under its mother. Select one to see its kernel.");
        tree.addTreeSelectionListener(e -> {
            TreePath p = e.getNewLeadSelectionPath();
            show(p == null ? null : ((Item) ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject()).lps);
        });
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, details.getFont().getSize()));
        details.setBorder(new EmptyBorder(4, 6, 4, 6));
        details.getAccessibleContext().setAccessibleName("LPS details and kernel notes");
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(300, 300));
        JLabel kernelLabel = new JLabel("Kernel View");
        kernelLabel.setLabelFor(kernel);
        kernelLabel.setBorder(new EmptyBorder(2, 4, 2, 4));
        JPanel kernelPanel = new JPanel(new BorderLayout());
        kernelPanel.add(kernelLabel, BorderLayout.NORTH);
        kernelPanel.add(kernel, BorderLayout.CENTER);
        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, kernelPanel, new JScrollPane(details));
        right.setResizeWeight(0.6);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, right);
        split.setResizeWeight(0.3);
        add(split, BorderLayout.CENTER);
        setStemma(null, "Not run yet.");
    }

    /** Shows a stemma, every node expanded and the mother selected; for null, shows the message. */
    public void setStemma(Stemma s, String message) {
        if (s == null) {
            tree.setModel(new DefaultTreeModel(null));
            selected = null;
            kernel.setLps(null);
            details.setText(message == null ? "" : message);
            details.setCaretPosition(0);
            return;
        }
        DefaultMutableTreeNode root = node(s.getMother());
        tree.setModel(new DefaultTreeModel(root));
        for (int r = 0; r < tree.getRowCount(); r++) {
            tree.expandRow(r);
        }
        tree.setSelectionRow(0);
    }

    private static DefaultMutableTreeNode node(LPS l) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(new Item(l));
        for (LPS d : l.getDaughters()) {
            n.add(node(d));
        }
        return n;
    }

    /** Selects an LPS of the shown stemma. */
    public void select(LPS l) {
        Object root = tree.getModel().getRoot();
        if (root == null) {
            return;
        }
        Enumeration<?> e = ((DefaultMutableTreeNode) root).preorderEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            if (((Item) n.getUserObject()).lps == l) {
                TreePath p = new TreePath(n.getPath());
                tree.setSelectionPath(p);
                tree.scrollPathToVisible(p);
                return;
            }
        }
        throw new IllegalArgumentException("the LPS " + l.getName() + " is not in the shown stemma");
    }

    private void show(LPS l) {
        selected = l;
        kernel.setLps(l);
        if (l == null) {
            details.setText("No LPS selected.");
            return;
        }
        StringBuilder b = new StringBuilder();
        b.append("LPS: ").append(l.getName()).append('\n');
        b.append("Operator: ").append(l.getOperator()).append('\n');
        b.append("Mother: ").append(l.getMother() == null ? "none (the mother of the stemma)" : l.getMother().getName())
         .append('\n');
        b.append("Daughters: ");
        if (l.getDaughters().isEmpty()) {
            b.append("none");
        }
        for (int i = 0; i < l.getDaughters().size(); i++) {
            b.append(i == 0 ? "" : ", ").append(l.getDaughters().get(i).getName());
        }
        int k = l.getKernel().size();
        b.append("\nKernel: ").append(k).append(k == 1 ? " note" : " notes").append(":\n");
        for (PerfNote n : l.getKernel()) {
            b.append("  ").append(KernelView.describe(n)).append('\n');
        }
        details.setText(b.toString());
        details.setCaretPosition(0);
    }

    /** The selected LPS, null when nothing is shown. */
    public LPS getSelected() {
        return selected;
    }

    public JTree getTree() {
        return tree;
    }

    public KernelView getKernelView() {
        return kernel;
    }

    public JTextArea getDetails() {
        return details;
    }
}
