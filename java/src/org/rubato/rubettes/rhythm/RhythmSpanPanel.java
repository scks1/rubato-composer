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

package org.rubato.rubettes.rhythm;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

import org.rubato.math.rhythm.Fraction;
import org.rubato.math.rhythm.RhythmLayout;

/**
 * Draws every node of a laid-out rhythm tree as a box spanning its onset to
 * its end, one row per depth with the root on top. Leaves (the notes, spec
 * D6) are filled in the text colour with their length in the background
 * colour; inner nodes are outlined in the text colour. Overlaps in label
 * timing (D2) are drawn as they fall. The drawing has no information that
 * the rubette's text report lacks, which serves as its text alternative.
 */
public class RhythmSpanPanel extends JComponent {

    private static final long serialVersionUID = 1L;

    static final int INSET = 8;
    static final int ROW = 28;
    static final int GAP = 6;

    private RhythmLayout shown;

    public RhythmSpanPanel() {
        setOpaque(true);
        getAccessibleContext().setAccessibleName("Rhythm tree spans");
        getAccessibleContext().setAccessibleDescription(
            "A drawing of every node's span in time, one row per depth; the rhythm tree report lists the same spans as text.");
    }

    /** Shows the layout, or nothing for null. */
    public void setLayoutShown(RhythmLayout l) {
        shown = l;
        revalidate();
        repaint();
    }

    public RhythmLayout getLayoutShown() {
        return shown;
    }

    private int rows() {
        int d = 0;
        if (shown != null) {
            for (RhythmLayout.Span s : shown.spans()) {
                d = Math.max(d, s.depth);
            }
        }
        return d + 1;
    }

    public Dimension getPreferredSize() {
        return new Dimension(640, 2 * INSET + rows() * ROW + (rows() - 1) * GAP);
    }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Color bg = getBackground();
        Color fg = getForeground();
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (shown == null || shown.spans().isEmpty()) {
            g.dispose();
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Fraction start = shown.spans().get(0).onset;
        double total = shown.end().subtract(start).toDouble();
        double width = Math.max(1, getWidth() - 2 * INSET);
        FontMetrics fm = g.getFontMetrics(getFont());
        g.setFont(getFont());
        for (RhythmLayout.Span s : shown.spans()) {
            int x0 = INSET + (int) Math.round(s.onset.subtract(start).toDouble() / total * width);
            int x1 = INSET + (int) Math.round(s.end().subtract(start).toDouble() / total * width);
            int w = Math.max(2, x1 - x0);
            int y = INSET + s.depth * (ROW + GAP);
            String label = s.length.toString();
            boolean fits = fm.stringWidth(label) + 6 <= w;
            int tx = x0 + (w - fm.stringWidth(label)) / 2;
            int ty = y + (ROW + fm.getAscent() - fm.getDescent()) / 2;
            if (s.isLeaf()) {
                g.setColor(fg);
                g.fillRect(x0, y, w - 1, ROW);
                if (fits) {
                    g.setColor(bg);
                    g.drawString(label, tx, ty);
                }
            }
            else {
                g.setColor(fg);
                g.drawRect(x0, y, w - 1, ROW - 1);
                if (fits) {
                    g.drawString(label, tx, ty);
                }
            }
        }
        g.dispose();
    }

    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                private static final long serialVersionUID = 1L;

                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.CANVAS;
                }
            };
        }
        return accessibleContext;
    }
}
