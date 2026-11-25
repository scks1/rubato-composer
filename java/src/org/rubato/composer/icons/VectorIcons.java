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
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.*;

import javax.swing.ImageIcon;

/**
 * The application icons for the FlatLaf themes, drawn as Java2D shapes on
 * a 16 by 16 grid. Outline icons use a 1.4 unit round stroke. The run and
 * stop icons are filled and carry their own colors. The four builder icons
 * and the Scheme icons keep the letters Rubato has always used for them
 * (M for modules, fx for morphisms, D for denotators, F for forms and
 * lambda for Scheme), set in the platform serif italic.
 *
 * Colors come from the theme keys Rubato.icon.foreground, Rubato.icon.run
 * and Rubato.icon.stop.
 */
@SuppressWarnings("nls")
final class VectorIcons {

    private VectorIcons() {}

    private static final String FG   = "Rubato.icon.foreground";
    private static final String RUN  = "Rubato.icon.run";
    private static final String STOP = "Rubato.icon.stop";
    // the group rows of the rubette list draw their chevrons in the row's text color
    private static final String GROUP = "Rubato.group.foreground";

    private static final Color FG_FALLBACK = new Color(0x4a4a4a);


    private static ImageIcon icon(VectorIcon.Painter p) {
        return new VectorIcon(p, FG, FG_FALLBACK);
    }


    static Path2D.Double path() {
        return new Path2D.Double();
    }


    /** A page with a folded corner, its right edge at <code>right</code> and its bottom at <code>bottom</code>. */
    static void page(Graphics2D g, double right, double bottom) {
        Path2D.Double p = path();
        p.moveTo(3, 1.5);
        p.lineTo(right-3.5, 1.5);
        p.lineTo(right, 5);
        p.lineTo(right, bottom);
        p.lineTo(3, bottom);
        p.closePath();
        g.draw(p);
        Path2D.Double f = path();
        f.moveTo(right-3.5, 1.5);
        f.lineTo(right-3.5, 5);
        f.lineTo(right, 5);
        g.draw(f);
    }


    /** Fills the outline of <code>text</code> centered on the grid. */
    static void letters(Graphics2D g, String text, float size, double dy) {
        Font font = new Font(Font.SERIF, Font.ITALIC, 1).deriveFont(size);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        GlyphVector gv = font.createGlyphVector(frc, text);
        Rectangle2D b = gv.getVisualBounds();
        Shape s = gv.getOutline((float)(8-b.getCenterX()), (float)(8-b.getCenterY()+dy));
        g.fill(s);
    }


    static ImageIcon newIcon() {
        return icon((g, i) -> page(g, 13, 14.5));
    }


    static ImageIcon openIcon() {
        return icon((g, i) -> {
            Path2D.Double back = path();
            back.moveTo(1.5, 13);
            back.lineTo(1.5, 3);
            back.lineTo(6, 3);
            back.lineTo(7.5, 4.5);
            back.lineTo(13, 4.5);
            back.lineTo(13, 7);
            g.draw(back);
            Path2D.Double front = path();
            front.moveTo(1.5, 13);
            front.lineTo(3.8, 7.2);
            front.lineTo(15, 7.2);
            front.lineTo(12.7, 13);
            front.closePath();
            g.draw(front);
        });
    }


    static ImageIcon addIcon() {
        return icon((g, i) -> {
            Path2D.Double p = path();
            p.moveTo(8.5, 14.5);
            p.lineTo(3, 14.5);
            p.lineTo(3, 1.5);
            p.lineTo(9.5, 1.5);
            p.lineTo(13, 5);
            p.lineTo(13, 8.5);
            g.draw(p);
            Path2D.Double f = path();
            f.moveTo(9.5, 1.5);
            f.lineTo(9.5, 5);
            f.lineTo(13, 5);
            g.draw(f);
            g.draw(new Line2D.Double(12.5, 10, 12.5, 15));
            g.draw(new Line2D.Double(10, 12.5, 15, 12.5));
        });
    }


    static ImageIcon revertIcon() {
        return icon((g, i) -> {
            Path2D.Double p = path();
            p.moveTo(2.5, 6);
            p.lineTo(10, 6);
            p.curveTo(12.8, 6, 14.5, 7.8, 14.5, 10);
            p.curveTo(14.5, 12.2, 12.8, 14, 10, 14);
            p.lineTo(6, 14);
            g.draw(p);
            Path2D.Double h = path();
            h.moveTo(5.5, 3);
            h.lineTo(2.5, 6);
            h.lineTo(5.5, 9);
            g.draw(h);
        });
    }


    static ImageIcon saveIcon() {
        return icon((g, i) -> {
            Path2D.Double tray = path();
            tray.moveTo(2, 10);
            tray.lineTo(2, 14);
            tray.lineTo(14, 14);
            tray.lineTo(14, 10);
            g.draw(tray);
            g.draw(new Line2D.Double(8, 1.8, 8, 10));
            Path2D.Double h = path();
            h.moveTo(4.5, 6.5);
            h.lineTo(8, 10);
            h.lineTo(11.5, 6.5);
            g.draw(h);
        });
    }


    static ImageIcon saveasIcon() {
        return icon((g, i) -> {
            page(g, 13, 14.5);
            g.draw(new Line2D.Double(8, 6, 8, 12));
            Path2D.Double h = path();
            h.moveTo(5.8, 9.8);
            h.lineTo(8, 12);
            h.lineTo(10.2, 9.8);
            g.draw(h);
        });
    }


    static ImageIcon quitIcon() {
        return icon((g, i) -> {
            Path2D.Double door = path();
            door.moveTo(9, 2.5);
            door.lineTo(3, 2.5);
            door.lineTo(3, 13.5);
            door.lineTo(9, 13.5);
            g.draw(door);
            g.draw(new Line2D.Double(6.5, 8, 14.5, 8));
            Path2D.Double h = path();
            h.moveTo(12, 5.5);
            h.lineTo(14.5, 8);
            h.lineTo(12, 10.5);
            g.draw(h);
        });
    }


    static ImageIcon newnetIcon() {
        return icon((g, i) -> {
            g.draw(new Line2D.Double(5.4, 5.2, 10.6, 6.3));
            g.draw(new Line2D.Double(4.6, 6, 5.1, 10.5));
            g.draw(new Ellipse2D.Double(1.8, 2, 3.6, 3.6));
            g.draw(new Ellipse2D.Double(10.6, 4.6, 3.6, 3.6));
            g.draw(new Ellipse2D.Double(3.6, 10.4, 3.6, 3.6));
            g.draw(new Line2D.Double(12.4, 10.5, 12.4, 15));
            g.draw(new Line2D.Double(10.15, 12.75, 14.65, 12.75));
        });
    }


    static ImageIcon closeIcon() {
        return icon((g, i) -> {
            g.draw(new Line2D.Double(4, 4, 12, 12));
            g.draw(new Line2D.Double(12, 4, 4, 12));
        });
    }


    static ImageIcon runIcon() {
        return new VectorIcon((g, i) -> {
            Path2D.Double p = path();
            p.moveTo(4.5, 2.5);
            p.lineTo(13.5, 8);
            p.lineTo(4.5, 13.5);
            p.closePath();
            g.fill(p);
            g.draw(p);
        }, RUN, new Color(0x1b7a2e));
    }


    static ImageIcon runContIcon() {
        return new VectorIcon((g, i) -> {
            for (double x : new double[] { 1.8, 8.3 }) {
                Path2D.Double p = path();
                p.moveTo(x, 3);
                p.lineTo(x+5.9, 8);
                p.lineTo(x, 13);
                p.closePath();
                g.fill(p);
                g.draw(p);
            }
        }, RUN, new Color(0x1b7a2e));
    }


    static ImageIcon stopIcon() {
        return new VectorIcon((g, i) -> {
            RoundRectangle2D.Double r = new RoundRectangle2D.Double(3.5, 3.5, 9, 9, 2, 2);
            g.fill(r);
            g.draw(r);
        }, STOP, new Color(0xc62828));
    }


    static ImageIcon prefIcon() {
        return icon((g, i) -> {
            double[][] rows = { {4, 10}, {8, 5}, {12, 11} };
            for (double[] r : rows) {
                g.draw(new Line2D.Double(1.8, r[0], r[1]-2, r[0]));
                g.draw(new Line2D.Double(r[1]+2, r[0], 14.2, r[0]));
                g.draw(new Ellipse2D.Double(r[1]-1.6, r[0]-1.6, 3.2, 3.2));
            }
        });
    }


    static ImageIcon cutIcon() {
        return icon((g, i) -> {
            g.draw(new Ellipse2D.Double(2.2, 10, 4.2, 4.2));
            g.draw(new Ellipse2D.Double(9.6, 10, 4.2, 4.2));
            g.draw(new Line2D.Double(5.6, 10.6, 11.5, 1.8));
            g.draw(new Line2D.Double(10.4, 10.6, 4.5, 1.8));
        });
    }


    static ImageIcon copyIcon() {
        return icon((g, i) -> {
            Path2D.Double back = path();
            back.moveTo(2.5, 10.5);
            back.lineTo(2.5, 2.5);
            back.lineTo(10.5, 2.5);
            g.draw(back);
            g.draw(new RoundRectangle2D.Double(5.5, 5.5, 8, 8, 1.5, 1.5));
        });
    }


    static ImageIcon pasteIcon() {
        return icon((g, i) -> {
            Path2D.Double board = path();
            board.moveTo(5.5, 3);
            board.lineTo(3, 3);
            board.lineTo(3, 14.5);
            board.lineTo(13, 14.5);
            board.lineTo(13, 3);
            board.lineTo(10.5, 3);
            g.draw(board);
            g.draw(new RoundRectangle2D.Double(5.5, 1.5, 5, 3, 1, 1));
        });
    }


    static ImageIcon browseIcon() {
        return icon((g, i) -> {
            g.draw(new Ellipse2D.Double(2, 2, 8.5, 8.5));
            g.draw(new Line2D.Double(9.4, 9.4, 14, 14));
        });
    }


    static ImageIcon moduleIcon() {
        return icon((g, i) -> letters(g, "M", 15f, 0));
    }


    static ImageIcon morphIcon() {
        return icon((g, i) -> letters(g, "fx", 14f, 0.5));
    }


    static ImageIcon denoIcon() {
        return icon((g, i) -> letters(g, "D", 15f, 0));
    }


    static ImageIcon formIcon() {
        return icon((g, i) -> letters(g, "F", 15f, 0));
    }


    static ImageIcon schemeIcon() {
        return icon((g, i) -> letters(g, "\u03bb", 15f, 0));
    }


    static ImageIcon schemeEditIcon() {
        return icon((g, i) -> {
            page(g, 13, 14.5);
            Font font = new Font(Font.SERIF, Font.ITALIC, 1).deriveFont(9f);
            GlyphVector gv = font.createGlyphVector(new FontRenderContext(null, true, true), "\u03bb");
            Rectangle2D b = gv.getVisualBounds();
            g.fill(gv.getOutline((float)(8-b.getCenterX()), (float)(10-b.getCenterY())));
        });
    }


    static ImageIcon tdownIcon() {
        return new VectorIcon((g, i) -> {
            Path2D.Double p = path();
            p.moveTo(4.5, 6.5);
            p.lineTo(8, 10);
            p.lineTo(11.5, 6.5);
            g.draw(p);
        }, GROUP, Color.WHITE);
    }


    static ImageIcon trightIcon() {
        return new VectorIcon((g, i) -> {
            Path2D.Double p = path();
            p.moveTo(6.5, 4.5);
            p.lineTo(10, 8);
            p.lineTo(6.5, 11.5);
            g.draw(p);
        }, GROUP, Color.WHITE);
    }


    static ImageIcon emptyIcon() {
        return icon((g, i) -> {});
    }
}
