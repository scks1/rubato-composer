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
import java.util.*;
import java.util.function.Supplier;

import javax.swing.ImageIcon;

/**
 * Vector drawings for the icons of the builtin rubettes, the form types
 * and the score player, used under the FlatLaf themes in place of the
 * bitmaps. {@link Icons#loadIcon(Class, String)} looks each bitmap's
 * resource path up here, so a rubette keeps calling loadIcon with its
 * bitmap name and receives the drawing under FlatLaf and the bitmap under
 * the classic theme. A resource without a drawing here, such as the icon
 * of a plugin rubette, still loads as its bitmap.
 *
 * The drawings share one grammar on the 16 unit grid of {@link VectorIcon}:
 * <ul>
 * <li>Arrows point down, the direction data flows through a network. An
 * arrow below a mark reads from it, an arrow above a mark writes into it.</li>
 * <li>Filled dots stand for points, notes and vertices; note heads are
 * slanted ovals with stems.</li>
 * <li>Rubettes that Rubato has always marked with a letter keep it, set
 * like the builder icons: M, TF, R, the real numbers, sigma, lambda and @.</li>
 * <li>The form types keep the shapes of the 2006 bitmaps: hexagon for
 * limit, diamond for colimit, circle for power, square for list and
 * cylinder for simple.</li>
 * </ul>
 * The rubette and form icons paint in the theme color
 * Rubato.icon.foreground, and in the text color of the label they sit on
 * when that label is selected or is a rubette's name label. The player
 * buttons use the run and stop colors of the toolbar.
 */
@SuppressWarnings("nls")
final class RubetteIcons {

    private RubetteIcons() {}

    private static final String FG   = "Rubato.icon.foreground";
    private static final String RUN  = "Rubato.icon.run";
    private static final String STOP = "Rubato.icon.stop";

    private static final Color FG_FALLBACK   = new Color(0x4a4a4a);
    private static final Color RUN_FALLBACK  = new Color(0x1b7a2e);
    private static final Color STOP_FALLBACK = new Color(0xc62828);

    private static final Map<String,Supplier<ImageIcon>> ICONS = new LinkedHashMap<>();


    /**
     * Returns the vector icon for the bitmap at the class path resource
     * <code>path</code>, such as
     * <code>org/rubato/rubettes/builtin/sourceicon.png</code>, or null if
     * there is none.
     */
    static ImageIcon forResource(String path) {
        Supplier<ImageIcon> s = ICONS.get(path);
        return (s == null) ? null : s.get();
    }


    /**
     * Returns the resource paths that have a vector icon, in the order of
     * the families below.
     */
    static Set<String> resources() {
        return Collections.unmodifiableSet(ICONS.keySet());
    }


    private static void mark(String path, VectorIcon.Painter p) {
        ICONS.put(path, () -> new VectorIcon(p, FG, FG_FALLBACK, 16, true));
    }


    private static void button(String path, String key, Color fallback, VectorIcon.Painter p) {
        ICONS.put(path, () -> new VectorIcon(p, key, fallback, 24, false));
    }


    // shared strokes and shapes

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }


    private static void dot(Graphics2D g, double x, double y, double r) {
        g.fill(new Ellipse2D.Double(x-r, y-r, 2*r, 2*r));
    }


    private static void ring(Graphics2D g, double x, double y, double r) {
        g.draw(new Ellipse2D.Double(x-r, y-r, 2*r, 2*r));
    }


    private static void polyline(Graphics2D g, double... xy) {
        Path2D.Double p = VectorIcons.path();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i+1]);
        }
        g.draw(p);
    }


    private static Path2D.Double polygon(double... xy) {
        Path2D.Double p = VectorIcons.path();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i+1]);
        }
        p.closePath();
        return p;
    }


    /** A line ending in an open arrowhead at x2,y2. */
    private static void arrow(Graphics2D g, double x1, double y1, double x2, double y2) {
        line(g, x1, y1, x2, y2);
        double a = Math.atan2(y2-y1, x2-x1);
        double len = 2.8, spread = Math.toRadians(42);
        polyline(g, x2-len*Math.cos(a-spread), y2-len*Math.sin(a-spread),
                    x2, y2,
                    x2-len*Math.cos(a+spread), y2-len*Math.sin(a+spread));
    }


    /** A slanted note head centered on x,y. */
    private static void head(Graphics2D g, double x, double y, double w, double h) {
        Shape e = new Ellipse2D.Double(x-w/2, y-h/2, w, h);
        g.fill(AffineTransform.getRotateInstance(Math.toRadians(-20), x, y).createTransformedShape(e));
    }


    /** Fills <code>text</code> in the upright platform serif, centered on the grid. */
    private static void upright(Graphics2D g, String text, float size, double dy) {
        upright(g, text, Font.PLAIN, size, dy);
    }


    private static void upright(Graphics2D g, String text, int style, float size, double dy) {
        Font font = new Font(Font.SERIF, style, 1).deriveFont(size);
        GlyphVector gv = font.createGlyphVector(new FontRenderContext(null, true, true), text);
        Rectangle2D b = gv.getVisualBounds();
        g.fill(gv.getOutline((float)(8-b.getCenterX()), (float)(8-b.getCenterY()+dy)));
    }


    private static void withColor(Graphics2D g, Color c, Runnable r) {
        Color old = g.getColor();
        g.setColor(c);
        r.run();
        g.setColor(old);
    }


    private static void dashed(Graphics2D g, Runnable r) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[] { 1.6f, 1.4f }, 0f));
        r.run();
        g.setStroke(old);
    }


    /** A three key keyboard in the box x, y, 12 wide and h high. */
    private static void keyboard(Graphics2D g, double y, double h) {
        g.draw(new RoundRectangle2D.Double(2, y, 12, h, 1.5, 1.5));
        g.fill(new Rectangle2D.Double(5.1, y, 1.8, h*0.6));
        g.fill(new Rectangle2D.Double(9.1, y, 1.8, h*0.6));
        line(g, 6, y+h*0.6, 6, y+h);
        line(g, 10, y+h*0.6, 10, y+h);
    }


    /** A picture frame with a hill and a sun in the box 2, y, 12 wide and h high. */
    private static void picture(Graphics2D g, double y, double h) {
        g.draw(new RoundRectangle2D.Double(2, y, 12, h, 1.5, 1.5));
        polyline(g, 3.2, y+h-1.2, 6.5, y+h-4.2, 9, y+h-1.8, 10.6, y+h-3, 12.8, y+h-1.2);
        dot(g, 11, y+2.1, 1.0);
    }


    /** Three staff lines with a note head in the box 2, y, 12 wide and h high. */
    private static void staff(Graphics2D g, double y, double h) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            double ly = y+h*(0.2+0.3*i);
            line(g, 1.5, ly, 14.5, ly);
        }
        g.setStroke(old);
        head(g, 9, y+h*0.65, 3.6, 2.7);
        line(g, 10.6, y+h*0.6, 10.6, y-0.2);
    }


    /** A small rounded box standing for a rubette in a network. */
    private static void node(Graphics2D g, double x, double y) {
        g.draw(new RoundRectangle2D.Double(x, y, 5.5, 3.6, 1.6, 1.6));
    }


    private static void link(Graphics2D g, double x1, double y1, double x2, double y2) {
        double mx = (x1+x2)/2;
        g.draw(new CubicCurve2D.Double(x1, y1, mx, y1, mx, y2, x2, y2));
    }


    private static final String B = "org/rubato/rubettes/builtin/";
    private static final String S = "org/rubato/rubettes/score/";
    private static final String C = "org/rubato/composer/icons/";
    private static final String R = "org/rubato/rubettes/";


    static {
        // Core rubettes

        mark(B+"sourceicon.png", (g, i) -> {
            polyline(g, 2, 9, 2, 14, 14, 14, 14, 9);
            arrow(g, 8, 1.5, 8, 10.5);
        });
        mark(B+"displayicon.png", (g, i) -> {
            g.draw(new RoundRectangle2D.Double(1.5, 2, 13, 9.5, 2.5, 2.5));
            line(g, 8, 11.5, 8, 14);
            line(g, 4.5, 14.2, 11.5, 14.2);
        });
        mark(B+"latchicon.png", (g, i) -> {
            line(g, 8, 1.5, 8, 6.5);
            polyline(g, 3, 11, 3, 6.5, 13, 6.5, 13, 11);
            line(g, 8, 6.5, 8, 11);
            dot(g, 3, 13.2, 1.5);
            dot(g, 8, 13.2, 1.5);
            dot(g, 13, 13.2, 1.5);
        });
        mark(B+"simpleicon.png", (g, i) -> VectorIcons.letters(g, "M", 15f, 0));
        // the dashed line is the edge of the macro network
        mark(B+"inputicon.png", (g, i) -> {
            withColor(g, i.getSecondaryColor(), () -> dashed(g, () -> line(g, 1.5, 2.5, 14.5, 2.5)));
            arrow(g, 5, 2.5, 5, 13.5);
            arrow(g, 11, 2.5, 11, 13.5);
        });
        mark(B+"outputicon.png", (g, i) -> {
            arrow(g, 5, 2, 5, 12.5);
            arrow(g, 11, 2, 11, 12.5);
            withColor(g, i.getSecondaryColor(), () -> dashed(g, () -> line(g, 1.5, 13.8, 14.5, 13.8)));
        });
        mark(B+"constricon.png", (g, i) -> {
            polyline(g, 5.5, 2, 2, 8, 5.5, 14);
            polyline(g, 10.5, 2, 14, 8, 10.5, 14);
            dot(g, 6.3, 8, 1.3);
            dot(g, 9.7, 8, 1.3);
        });
        mark(B+"logicicon.png", (g, i) -> VectorIcons.letters(g, "TF", 12f, 0));
        mark(B+"registericon.png", (g, i) -> {
            ring(g, 8, 8, 6.6);
            upright(g, "R", 9.5f, 0);
        });
        mark(B+"arithicon.png", (g, i) -> {
            Font serif = new Font(Font.SERIF, Font.PLAIN, 12);
            upright(g, serif.canDisplay('\u211d') ? "\u211d" : "R", Font.BOLD, 14.5f, 0);
        });
        mark(B+"spliticon.png", (g, i) -> {
            g.draw(new RoundRectangle2D.Double(5, 1.5, 6, 3.8, 1.4, 1.4));
            line(g, 8, 5.3, 8, 7.8);
            polyline(g, 3.5, 10, 3.5, 7.8, 12.5, 7.8, 12.5, 10);
            g.draw(new RoundRectangle2D.Double(1.5, 10, 4, 4, 1.4, 1.4));
            g.draw(new RoundRectangle2D.Double(10.5, 10, 4, 4, 1.4, 1.4));
        });
        mark(B+"seticon.png", (g, i) -> {
            Ellipse2D a = new Ellipse2D.Double(1.5, 3.5, 9, 9);
            Ellipse2D b = new Ellipse2D.Double(5.5, 3.5, 9, 9);
            Area lens = new Area(a);
            lens.intersect(new Area(b));
            withColor(g, i.getSecondaryColor(), () -> g.fill(lens));
            g.draw(a);
            g.draw(b);
        });
        mark(B+"listicon.png", (g, i) -> {
            for (double y : new double[] { 3.5, 8, 12.5 }) {
                dot(g, 2.8, y, 1.3);
                line(g, 6, y, 14.5, y);
            }
        });
        mark(B+"staticon.png", (g, i) -> upright(g, "\u03a3", 14.5f, 0));
        mark(B+"muxicon.png", (g, i) -> {
            withColor(g, i.getSecondaryColor(), () -> {
                Path2D.Double l = VectorIcons.path();
                l.moveTo(2.5, 1.5);
                l.lineTo(2.5, 4.5);
                l.quadTo(2.5, 8.5, 8, 8.5);
                g.draw(l);
                Path2D.Double r = VectorIcons.path();
                r.moveTo(13.5, 1.5);
                r.lineTo(13.5, 4.5);
                r.quadTo(13.5, 8.5, 8, 8.5);
                g.draw(r);
            });
            line(g, 8, 1.5, 8, 14.5);
        });
        mark(B+"reformicon.png", (g, i) -> {
            g.draw(new RoundRectangle2D.Double(1.5, 1.5, 5, 5, 1.2, 1.2));
            ring(g, 11.5, 11.5, 3);
            Path2D.Double p = VectorIcons.path();
            p.moveTo(6.5, 4);
            p.quadTo(11.5, 4, 11.5, 7.6);
            g.draw(p);
            polyline(g, 9.5, 5.9, 11.5, 7.9, 13.5, 5.9);
        });
        mark(B+"schemeicon.png", (g, i) -> VectorIcons.letters(g, "\u03bb", 15f, 0));
        mark(B+"neticon.png", (g, i) -> {
            node(g, 1.5, 1.5);
            node(g, 9, 6.2);
            node(g, 1.5, 10.9);
            link(g, 7, 3.3, 9, 8);
            link(g, 9, 8, 7, 12.7);
        });
        mark(B+"address/addressicon.png", (g, i) -> upright(g, "@", 14f, 0.3));

        // Score and file rubettes

        mark(C+"midiin.png", (g, i) -> {
            keyboard(g, 1.5, 6.5);
            arrow(g, 8, 9, 8, 14.5);
        });
        mark(C+"midiout.png", (g, i) -> {
            arrow(g, 8, 1.5, 8, 7);
            keyboard(g, 8, 6.5);
        });
        mark(R+"image/imagefileinicon.png", (g, i) -> {
            picture(g, 1.5, 7);
            arrow(g, 8, 9.5, 8, 14.5);
        });
        mark(R+"image/imagefileouticon.png", (g, i) -> {
            arrow(g, 8, 1.5, 8, 6.5);
            picture(g, 7.5, 7);
        });
        mark(S+"lilypondouticon.png", (g, i) -> {
            arrow(g, 5, 1.5, 5, 6.5);
            staff(g, 8, 6.5);
        });
        mark(S+"melodyicon.png", (g, i) -> {
            head(g, 3.5, 12.5, 3.8, 2.8);
            head(g, 8, 10.5, 3.8, 2.8);
            head(g, 12.5, 8.5, 3.8, 2.8);
            line(g, 5.1, 12, 5.1, 5.5);
            line(g, 9.6, 10, 9.6, 3.5);
            line(g, 14.1, 8, 14.1, 1.5);
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            line(g, 5.1, 5.5, 14.1, 1.5);
            g.setStroke(old);
        });
        mark(S+"scoreplayicon.png", (g, i) -> {
            head(g, 4, 12.5, 3.8, 2.8);
            line(g, 5.6, 12, 5.6, 2.5);
            Path2D.Double f = VectorIcons.path();
            f.moveTo(5.6, 2.5);
            f.quadTo(8.6, 4.2, 7.6, 7.6);
            g.draw(f);
            g.draw(new Arc2D.Double(8.5-2.6, 10-2.6, 5.2, 5.2, -50, 100, Arc2D.OPEN));
            g.draw(new Arc2D.Double(8.5-5.4, 10-5.4, 10.8, 10.8, -45, 90, Arc2D.OPEN));
        });
        mark(S+"scaleicon.png", (g, i) ->
            polyline(g, 1.5, 14.5, 1.5, 11.5, 5, 11.5, 5, 8, 8.5, 8, 8.5, 4.5, 12, 4.5, 12, 1.5, 14.5, 1.5));
        mark(S+"quantizeicon.png", (g, i) -> {
            line(g, 1.5, 14.2, 14.5, 14.2);
            double[] xs = { 2.5, 6, 9.5, 13 };
            double[] ys = { 8, 4, 9.5, 5.5 };
            for (int k = 0; k < 4; k++) {
                final double x = xs[k], y = ys[k];
                line(g, x, 14.2, x, 12.2);
                withColor(g, i.getSecondaryColor(), () -> dashed(g, () -> line(g, x, y+2, x, 11.6)));
                dot(g, x, y, 1.5);
            }
        });
        mark(S+"rhythmizeicon.png", (g, i) -> {
            double[] xs = { 2.5, 6, 9.6, 12.8 };
            for (double x : xs) {
                head(g, x, 13, 3.3, 2.4);
                line(g, x+1.4, 12.6, x+1.4, 3.5);
            }
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            line(g, 3.9, 3.5, 7.4, 3.5);
            line(g, 11.0, 3.5, 14.2, 3.5);
            line(g, 11.0, 6.2, 14.2, 6.2);
            g.setStroke(old);
        });
        button(S+"play.png", RUN, RUN_FALLBACK, (g, i) -> g.fill(polygon(5, 2.8, 13.2, 8, 5, 13.2)));
        button(S+"pause.png", FG, FG_FALLBACK, (g, i) -> {
            g.fill(new RoundRectangle2D.Double(4.2, 3, 2.8, 10, 1, 1));
            g.fill(new RoundRectangle2D.Double(9, 3, 2.8, 10, 1, 1));
        });
        button(S+"stop.png", STOP, STOP_FALLBACK, (g, i) ->
            g.fill(new RoundRectangle2D.Double(3.5, 3.5, 9, 9, 1.6, 1.6)));
        mark(R+"select2d/select2dicon.png", (g, i) -> {
            withColor(g, i.getSecondaryColor(), () -> dot(g, 13.5, 13.5, 1.3));
            dashed(g, () -> g.draw(polygon(2, 4, 11, 1.8, 14, 9.5, 5, 13.5)));
            dot(g, 6.5, 7, 1.4);
            dot(g, 10, 8.5, 1.4);
        });

        // Theory rubettes

        mark(R+"counterpoint/counterpointcheckicon.png", (g, i) -> {
            withColor(g, i.getSecondaryColor(), () -> {
                line(g, 3, 4.5, 7.5, 3);
                line(g, 3, 11.5, 7.5, 10);
            });
            dot(g, 3, 4.5, 1.5);
            dot(g, 3, 11.5, 1.5);
            dot(g, 7.5, 3, 1.5);
            dot(g, 7.5, 10, 1.5);
            line(g, 10.6, 5.4, 14.4, 9.2);
            line(g, 14.4, 5.4, 10.6, 9.2);
        });
        mark(R+"counterpoint/successorgeneratoricon.png", (g, i) -> {
            dot(g, 2.8, 8, 1.7);
            for (double y : new double[] { 2.8, 8, 13.2 }) {
                line(g, 4.6, 8, 11.4, y);
                dot(g, 13.2, y, 1.5);
            }
        });
        mark(R+"counterpoint/symmetrytraceicon.png", (g, i) -> {
            withColor(g, i.getSecondaryColor(), () -> dashed(g, () -> line(g, 8, 1, 8, 15)));
            polyline(g, 6, 1.8, 2.5, 5, 6, 8, 2.5, 11, 6, 14.2);
            polyline(g, 10, 1.8, 13.5, 5, 10, 8, 13.5, 11, 10, 14.2);
        });
        mark(R+"gesture/fingergestureicon.png", (g, i) -> {
            g.draw(new CubicCurve2D.Double(2, 10.5, 4.5, 1.5, 6.5, 1.5, 8.2, 8));
            g.draw(new CubicCurve2D.Double(8.2, 8, 10, 14.5, 12.5, 14.5, 14.5, 5.5));
            dot(g, 2, 10.5, 1.6);
        });
        mark(R+"global/nerveicon.png", (g, i) -> {
            Path2D.Double t = polygon(2.5, 13, 7.5, 3, 12, 13);
            withColor(g, i.getSecondaryColor(), () -> g.fill(t));
            g.draw(t);
            polyline(g, 7.5, 3, 14, 6, 12, 13);
            for (double[] v : new double[][] { {2.5, 13}, {7.5, 3}, {12, 13}, {14, 6} }) {
                dot(g, v[0], v[1], 1.5);
            }
        });
        mark(R+"harmony/cadenceicon.png", (g, i) -> {
            g.draw(new Rectangle2D.Double(2, 9, 3, 5));
            g.draw(new Rectangle2D.Double(6.5, 6, 3, 8));
            g.fill(new Rectangle2D.Double(10.3, 2.3, 4.4, 12.4));
        });
        mark(R+"harmony/keypathicon.png", (g, i) -> {
            polyline(g, 2.5, 12.5, 6, 7, 10, 9.5, 13.5, 3.5);
            dot(g, 2.5, 12.5, 1.6);
            dot(g, 6, 7, 1.6);
            dot(g, 10, 9.5, 1.6);
            dot(g, 13.5, 3.5, 1.6);
        });
        mark(R+"harmony/modulationicon.png", (g, i) -> {
            ring(g, 4, 7, 2.6);
            arrow(g, 7.2, 7, 9.6, 7);
            dot(g, 12.3, 7, 2.6);
            line(g, 1.5, 13, 14.5, 13);
        });
        mark(R+"performance/scalarperformanceicon.png", (g, i) ->
            polyline(g, 14.5, 3, 1.5, 8, 14.5, 13));
        mark(R+"performance/tempoperformanceicon.png", (g, i) -> {
            line(g, 1.5, 14.2, 14.5, 14.2);
            for (double x : new double[] { 2, 6.8, 10.4, 12.8, 14.3 }) {
                line(g, x, 14.2, x, 12.2);
            }
            Path2D.Double c = VectorIcons.path();
            c.moveTo(1.5, 9.5);
            c.quadTo(9.5, 9.5, 14.5, 2);
            g.draw(c);
        });
    }
}
