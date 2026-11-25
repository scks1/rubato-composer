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

package org.rubato.composer.icons;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.rubato.composer.Theme;

/**
 * An icon drawn with Java2D shapes on a 16 by 16 grid. The icon paints at
 * the device resolution, so it stays sharp at any scale factor, and takes
 * its colors from the look and feel when it paints, so the light and dark
 * themes share one drawing.
 *
 * The class extends ImageIcon so that it fits the existing fields and
 * method signatures that expect one. {@link #getImage()} returns a
 * multi-resolution rendering, which the look and feel uses to derive the
 * disabled icon and the window manager uses for frame icons.
 */
public class VectorIcon extends ImageIcon {

    /**
     * Draws the shapes of an icon. The graphics context is scaled so that
     * the icon occupies the square from 0,0 to 16,16, has antialiasing and
     * pure stroke control on, a 1.4 unit round stroke and the icon's color
     * set.
     */
    public interface Painter {
        void paint(Graphics2D g, VectorIcon icon);
    }


    /**
     * Creates an icon of 16 by 16 units painted by <code>painter</code> in
     * the UIManager color <code>colorKey</code>, or <code>fallback</code>
     * when the key is not defined.
     */
    public VectorIcon(Painter painter, String colorKey, Color fallback) {
        this(painter, colorKey, fallback, UNITS, false);
    }


    /**
     * Creates an icon of <code>size</code> by <code>size</code> pixels before
     * user interface scaling, painted on the same 16 unit grid. When
     * <code>followsLabel</code> is true and the icon sits on a label that
     * shows selected text, or on a label marked with
     * {@link #FOLLOW_FOREGROUND}, the icon paints in that label's foreground
     * color, so it keeps the contrast of the text beside it.
     */
    public VectorIcon(Painter painter, String colorKey, Color fallback, int size, boolean followsLabel) {
        this.painter = painter;
        this.colorKey = colorKey;
        this.fallback = fallback;
        this.size = size;
        this.followsLabel = followsLabel;
    }


    /**
     * The client property that makes icons created with
     * <code>followsLabel</code> paint in the foreground color of the label
     * they sit on, whatever that color is.
     */
    public static final String FOLLOW_FOREGROUND = "Rubato.icon.followForeground"; //$NON-NLS-1$


    /**
     * Returns true if this icon paints in the color of the label it sits on
     * when that label shows selected text.
     */
    public boolean followsLabel() {
        return followsLabel;
    }


    @Override
    public int getIconWidth() {
        return Theme.scale(size);
    }


    @Override
    public int getIconHeight() {
        return Theme.scale(size);
    }


    /**
     * Returns the color this icon paints in, read from the UIManager.
     */
    public Color getColor() {
        Color c = UIManager.getColor(colorKey);
        return (c != null) ? c : fallback;
    }


    /**
     * Returns the color for secondary strokes in the icon: the primary
     * color at reduced opacity.
     */
    public Color getSecondaryColor() {
        Color c = currentColor != null ? currentColor : getColor();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(c.getAlpha()*0.55f));
    }


    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        Color color = getColor();
        if (c instanceof AbstractButton && !c.isEnabled()) {
            Color d = UIManager.getColor("Label.disabledForeground"); //$NON-NLS-1$
            if (d != null) {
                color = d;
            }
        }
        else if (followsLabel && c instanceof JLabel) {
            Color f = c.getForeground();
            if (f != null && (Boolean.TRUE.equals(((JComponent)c).getClientProperty(FOLLOW_FOREGROUND))
                              || isSelectionForeground(f))) {
                color = f;
            }
        }
        paintAt((Graphics2D)g, x, y, getIconWidth(), color);
    }


    private static final String[] SELECTION_KEYS = {
        "List.selectionForeground", "List.selectionInactiveForeground", //$NON-NLS-1$ //$NON-NLS-2$
        "Tree.selectionForeground", "Tree.selectionInactiveForeground", //$NON-NLS-1$ //$NON-NLS-2$
        "Table.selectionForeground", "Table.selectionInactiveForeground" //$NON-NLS-1$ //$NON-NLS-2$
    };


    private static boolean isSelectionForeground(Color f) {
        for (String key : SELECTION_KEYS) {
            Color s = UIManager.getColor(key);
            if (s != null && s.getRGB() == f.getRGB()) {
                return true;
            }
        }
        return false;
    }


    private void paintAt(Graphics2D g0, int x, int y, int px, Color color) {
        Graphics2D g = (Graphics2D)g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(x, y);
            double s = px/(double)UNITS;
            g.scale(s, s);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            currentColor = color;
            painter.paint(g, this);
        }
        finally {
            currentColor = null;
            g.dispose();
        }
    }


    /**
     * Returns renderings of this icon at 1, 1.5 and 2 times its size.
     */
    @Override
    public Image getImage() {
        int base = getIconWidth();
        Image[] variants = new Image[] { render(base), render(base*3/2), render(base*2) };
        return new BaseMultiResolutionImage(variants);
    }


    private BufferedImage render(int px) {
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setTransform(new AffineTransform());
        paintAt(g, 0, 0, px, getColor());
        g.dispose();
        return img;
    }


    private static final int UNITS = 16;

    private final Painter painter;
    private final int     size;
    private final boolean followsLabel;
    private final String  colorKey;
    private final Color   fallback;
    private Color currentColor;
}
