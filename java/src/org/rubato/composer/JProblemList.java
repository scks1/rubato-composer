/*
 * Copyright (C) 2005 Gérard Milmeister
 *
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

package org.rubato.composer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.rubato.composer.icons.VectorIcon;
import org.rubato.composer.network.JNetwork;
import org.rubato.composer.rubette.JRubette;

public class JProblemList extends JPanel {

    public JProblemList() {
        setLayout(new BorderLayout());
        problemListModel = new ProblemListModel();
        problemList = new JList(problemListModel) {
            // lines fit the visible width and end in an ellipsis; the tooltip has the full text
            public boolean getScrollableTracksViewportWidth() {
                return Theme.isFlat() || super.getScrollableTracksViewportWidth();
            }
            public String getToolTipText(MouseEvent e) {
                int i = locationToIndex(e.getPoint());
                if (i < 0 || !getCellBounds(i, i).contains(e.getPoint())) {
                    return null;
                }
                return toolTip((Problem)getModel().getElementAt(i));
            }
        };
        if (Theme.isFlat()) {
            problemList.setCellRenderer(new ProblemRenderer());
        }
        ToolTipManager.sharedInstance().registerComponent(problemList);
        problemList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    showSelectedRubette();
                }
            }
        });
        problemList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "showRubette"); //$NON-NLS-1$
        problemList.getActionMap().put("showRubette", new AbstractAction() { //$NON-NLS-1$
            public void actionPerformed(ActionEvent e) {
                showSelectedRubette();
            }
        });
        problemList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Problem problem = (Problem)problemList.getSelectedValue();
                    if (problem != null) {
                        problem.getJNetwork().highlight(problem.getJRubette(), true);
                    }
                }
            }
        });
        add(new JScrollPane(problemList), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        clearButton = new JButton(Messages.getString("JProblemList.clear")); //$NON-NLS-1$
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearProblems();
            }
        });
        buttonPanel.add(clearButton);
        
        add(buttonPanel, BorderLayout.EAST);
    }
 
    
    public void addProblems(List<Problem> problems) {
        for (Problem problem : problems) {
            problemListModel.addProblem(problem);
        }
    }
    
    
    public void addProblem(Problem problem) {
        problemListModel.addProblem(problem);
    }
    
    
    public void addProblem(String msg, JNetwork jnetwork, JRubette jrubette) {
        problemListModel.addProblem(new Problem(msg, jnetwork, jrubette));
    }
    
    
    public void clearProblems() {
        problemListModel.clearProblems();        
    }
    
    
    public void removeProblemsFor(JRubette jrubette) {
        problemListModel.removeProblemsFor(jrubette);
    }
    
    
    /**
     * Brings the selected problem's rubette into view: selects the tab of
     * its network, scrolls the network to the rubette and gives the
     * network the keyboard focus. The rubette keeps its problem border.
     */
    protected void showSelectedRubette() {
        Problem problem = (Problem)problemList.getSelectedValue();
        if (problem == null || problem.getJNetwork() == null || problem.getJRubette() == null) {
            return;
        }
        JNetwork jnetwork = problem.getJNetwork();
        JRubette jrubette = problem.getJRubette();
        Component child = jnetwork;
        for (Container parent = jnetwork.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof JTabbedPane) {
                ((JTabbedPane)parent).setSelectedComponent(child);
            }
            child = parent;
        }
        jnetwork.highlight(jrubette, true);
        Rectangle r = jrubette.getBounds();
        r.grow(Theme.scale(24), Theme.scale(24));
        jnetwork.scrollRectToVisible(r);
        jnetwork.requestFocusInWindow();
    }


    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    }


    private static String toolTip(Problem p) {
        return "<html><body style='width: 420px'><b>"+escape(p.getJNetwork()+" : "+p.getJRubette()) //$NON-NLS-1$ //$NON-NLS-2$
               +"</b><br>"+escape(String.valueOf(p.getMessage()))+"</body></html>"; //$NON-NLS-1$ //$NON-NLS-2$
    }


    /**
     * Draws a problem line as an error mark and the rubette name in bold,
     * then the message, which ends in an ellipsis when it does not fit,
     * and the network name at the right.
     */
    private static class ProblemRenderer extends JPanel implements ListCellRenderer {

        ProblemRenderer() {
            super(new BorderLayout(Theme.scale(8), 0));
            setBorder(BorderFactory.createEmptyBorder(Theme.scale(3), Theme.scale(6), Theme.scale(3), Theme.scale(6)));
            name.setIcon(errorIcon);
            name.setIconTextGap(Theme.scale(6));
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            add(name, BorderLayout.WEST);
            add(message, BorderLayout.CENTER);
            add(network, BorderLayout.EAST);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Problem p = (Problem)value;
            Color fg = isSelected ? list.getSelectionForeground() : list.getForeground();
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setOpaque(true);
            name.setText(String.valueOf(p.getJRubette()));
            message.setText(String.valueOf(p.getMessage()));
            network.setText(String.valueOf(p.getJNetwork()));
            name.setForeground(fg);
            message.setForeground(fg);
            network.setForeground(isSelected ? fg : secondaryColor);
            Border focus = cellHasFocus ? UIManager.getBorder("List.focusCellHighlightBorder") : null; //$NON-NLS-1$
            Border pad = BorderFactory.createEmptyBorder(Theme.scale(3), Theme.scale(6), Theme.scale(3), Theme.scale(6));
            setBorder(focus != null ? BorderFactory.createCompoundBorder(focus, pad) : pad);
            return this;
        }

        private final JLabel name    = new JLabel();
        private final JLabel message = new JLabel();
        private final JLabel network = new JLabel();
    }


    private static final Color secondaryColor = Theme.color("Rubato.problem.secondary", Color.GRAY); //$NON-NLS-1$


    /** A filled circle with an exclamation mark in the problem color. */
    private static final Icon errorIcon = new VectorIcon((g, icon) -> {
        g.fill(new Ellipse2D.Double(1, 1, 14, 14));
        Color mark = UIManager.getColor("List.background"); //$NON-NLS-1$
        g.setColor(mark != null ? mark : Color.WHITE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(8, 4.5, 8, 9));
        g.fill(new Ellipse2D.Double(6.9, 10.6, 2.2, 2.2));
    }, "Rubato.canvas.problem", Color.RED); //$NON-NLS-1$


    protected  JList            problemList;
    private    ProblemListModel problemListModel;
    private    JButton          clearButton;
    
    
    public class ProblemListModel extends DefaultListModel {
        
        public void addProblem(Problem problem) {
            addElement(problem);
        }
        
        public void clearProblems() {
            for (int i = 0; i < getSize(); i++) {
                Problem problem = (Problem)get(i);
                problem.getJNetwork().highlight(problem.getJRubette(), false);
            }
            removeAllElements();
        }
        
        public void removeProblemsFor(JRubette jrubette) {
            ArrayList<Problem> removes = new ArrayList<Problem>();
            for (int i = 0; i < getSize(); i++) {
                Problem problem = (Problem)get(i);
                if (problem.getJRubette() == jrubette) {
                    removes.add(problem);
                }
            }
            for (Problem problem : removes) {
                removeElement(problem);
            }
        }
    }
}
