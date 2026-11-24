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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.rubato.math.performance.LPS;
import org.rubato.math.performance.PerfNote;
import org.rubato.math.performance.WeightWatcher;

/**
 * The Kernel View of the PerformanceRUBETTE (Mazzola, <i>The Topos of Music II</i>, &sect;41.4,
 * Fig. 41.12, p. 680): the top kernel of one LPS as a piano roll, onset across, pitch up, with
 * loudness coded by grey level. Louder notes are drawn in greys further from the background,
 * and every rectangle is outlined in the text colour. The view draws the symbolic kernel and
 * no bar lines (departures D10, D11 of sources/STEMMA-VIEW-SPEC.md). Its notes are listed as
 * text in the stemma browser's details, which is the view's text alternative.
 */
public class KernelView extends JComponent {

    private static final int MARGIN = 6;

    private LPS lps;

    public KernelView() {
        setOpaque(true);
        setFocusable(true);
        setPreferredSize(new Dimension(480, 220));
        getAccessibleContext().setAccessibleName("Kernel View");
        getAccessibleContext().setAccessibleDescription("No LPS selected.");
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

    /** Shows the top kernel of an LPS, or nothing for null. */
    public void setLps(LPS l) {
        lps = l;
        getAccessibleContext().setAccessibleDescription(summary(l));
        repaint();
    }

    public LPS getLps() {
        return lps;
    }

    /** A one-sentence summary of what the view draws, its accessible description. */
    static String summary(LPS l) {
        if (l == null) {
            return "No LPS selected.";
        }
        List<PerfNote> k = l.getKernel();
        if (k.isEmpty()) {
            return "Kernel View of " + l.getName() + ": no notes.";
        }
        double[] r = ranges(k);
        return "Kernel View of " + l.getName() + ": " + k.size() + (k.size() == 1 ? " note" : " notes")
             + ", onsets " + WeightWatcher.format(r[0]) + " to " + WeightWatcher.format(r[1])
             + " quarters, pitches " + WeightWatcher.format(r[2]) + " to " + WeightWatcher.format(r[3])
             + ", loudness " + WeightWatcher.format(r[4]) + " to " + WeightWatcher.format(r[5])
             + ". Louder notes are drawn in stronger greys. The notes are listed in the details text.";
    }

    /** Smallest and largest onset, note end, pitch and loudness: {e0, e1, p0, p1, l0, l1, lastOnset}. */
    private static double[] ranges(List<PerfNote> k) {
        double e0 = Double.POSITIVE_INFINITY, e1 = Double.NEGATIVE_INFINITY, p0 = e0, p1 = e1, l0 = e0, l1 = e1,
               on1 = e1;
        for (PerfNote n : k) {
            e0 = Math.min(e0, n.onset);
            on1 = Math.max(on1, n.onset);
            e1 = Math.max(e1, n.onset + n.duration);
            p0 = Math.min(p0, n.pitch);
            p1 = Math.max(p1, n.pitch);
            l0 = Math.min(l0, n.loudness);
            l1 = Math.max(l1, n.loudness);
        }
        return new double[] { e0, on1, p0, p1, l0, l1, e1 };
    }

    /** One line of text for a note, as the details text lists it. */
    public static String describe(PerfNote n) {
        return String.format(Locale.ROOT, "onset %s, pitch %s, loudness %s, duration %s, voice %d",
                WeightWatcher.format(n.onset), WeightWatcher.format(n.pitch), WeightWatcher.format(n.loudness),
                WeightWatcher.format(n.duration), n.voice);
    }

    /**
     * The grey of a note of loudness l when the kernel's loudness runs from min to max: a grey
     * between the background's and the foreground's, a quarter of the way at min and all the
     * way at max (all the way when min = max).
     */
    public static Color fill(double l, double min, double max, Color bg, Color fg) {
        double t = max > min ? 0.25 + 0.75 * (l - min) / (max - min) : 1;
        t = Math.max(0.25, Math.min(1, t));
        double gb = grey(bg), gf = grey(fg);
        int v = (int) Math.round(gb + t * (gf - gb));
        v = Math.max(0, Math.min(255, v));
        return new Color(v, v, v);
    }

    private static double grey(Color c) {
        return 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
    }

    /** The note rectangles for the current size, one per kernel note in kernel order. */
    public List<Rectangle> getRectangles() {
        List<Rectangle> out = new ArrayList<Rectangle>();
        if (lps == null || lps.getKernel().isEmpty()) {
            return out;
        }
        List<PerfNote> k = lps.getKernel();
        double[] r = ranges(k);
        double e0 = r[0], e1 = Math.max(r[6], r[0] + 1e-9), p0 = r[2], p1 = r[3];
        int w = Math.max(1, getWidth() - 2 * MARGIN), h = Math.max(1, getHeight() - 2 * MARGIN);
        double rows = p1 - p0 + 1;
        int rh = Math.max(3, (int) Math.floor(h / rows));
        for (PerfNote n : k) {
            int x = MARGIN + (int) Math.round((n.onset - e0) / (e1 - e0) * w);
            int x2 = MARGIN + (int) Math.round((n.onset + n.duration - e0) / (e1 - e0) * w);
            int rw = Math.max(3, x2 - x - 1);
            if (x + rw > getWidth() - MARGIN) {
                x = Math.max(0, getWidth() - MARGIN - rw);
            }
            int y = MARGIN + (int) Math.round((p1 - n.pitch) / rows * h);
            int hh = Math.min(rh, Math.max(3, h - (y - MARGIN)));
            if (y + hh > getHeight() - MARGIN) {
                y = Math.max(0, getHeight() - MARGIN - hh);
            }
            out.add(new Rectangle(x, y, rw, hh));
        }
        return out;
    }

    /**
     * Makes Tab and Shift+Tab move the focus out of a read-only table or text area, which
     * otherwise keeps Tab for moving between cells or typing, so the controls after it stay
     * reachable from the keyboard.
     */
    static void tabLeaves(JComponent c) {
        c.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
        c.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));
    }

    static Color background() {
        Color c = UIManager.getColor("Panel.background");
        return c == null ? Color.WHITE : c;
    }

    static Color foreground() {
        Color c = UIManager.getColor("Label.foreground");
        return c == null ? Color.BLACK : c;
    }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Color bg = background(), fg = foreground();
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (lps != null && !lps.getKernel().isEmpty()) {
            List<PerfNote> k = lps.getKernel();
            double[] r = ranges(k);
            List<Rectangle> rects = getRectangles();
            for (int i = 0; i < rects.size(); i++) {
                Rectangle q = rects.get(i);
                g.setColor(fill(k.get(i).loudness, r[4], r[5], bg, fg));
                g.fillRect(q.x, q.y, q.width, q.height);
                g.setColor(fg);
                g.drawRect(q.x, q.y, q.width - 1, q.height - 1);
            }
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
