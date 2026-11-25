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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import junit.framework.TestCase;

import org.rubato.composer.BuiltinRubettes;

/**
 * Checks the vector icons of the builtin rubettes and form types: every
 * drawn icon has its bitmap for the classic theme, loadIcon picks the
 * drawing only under FlatLaf, every builtin rubette that has an icon gets
 * a drawing under FlatLaf and its bitmap otherwise, and an icon on a
 * rubette's name label paints in the label's color.
 */
@SuppressWarnings("nls")
public class RubetteIconsTest extends TestCase {

    public void testEveryDrawingHasItsBitmap() {
        assertFalse(RubetteIcons.resources().isEmpty());
        for (String path : RubetteIcons.resources()) {
            assertNotNull("no bitmap for "+path, Icons.class.getResource("/"+path));
        }
    }


    public void testLoadIconPicksTheDrawingOnlyForFlatLaf() {
        for (String path : RubetteIcons.resources()) {
            ImageIcon vector = Icons.loadIcon(Icons.class, "/"+path, true);
            ImageIcon bitmap = Icons.loadIcon(Icons.class, "/"+path, false);
            assertTrue(path, vector instanceof VectorIcon);
            assertFalse(path, bitmap instanceof VectorIcon);
            assertTrue(path, bitmap.getIconWidth() > 0);
        }
    }


    public void testResourcePathResolvesLikeGetResource() {
        assertEquals("org/rubato/rubettes/builtin/sourceicon.png",
                     Icons.resourcePath(org.rubato.rubettes.builtin.SourceRubette.class, "sourceicon.png"));
        assertEquals("org/rubato/composer/icons/midiin.png",
                     Icons.resourcePath(Icons.class, "midiin.png"));
        assertEquals("a/b.png", Icons.resourcePath(Icons.class, "/a/b.png"));
    }


    public void testIconForUnknownResourceIsTheBitmap() {
        // a plugin rubette's own bitmap has no drawing and keeps loading as a bitmap
        ImageIcon icon = Icons.loadIcon(Icons.class, "rubatoicon.png", true);
        assertFalse(icon instanceof VectorIcon);
        assertTrue(icon.getIconWidth() > 0);
    }


    public void testEveryDrawingPaintsInsideItsGrid() {
        for (String path : RubetteIcons.resources()) {
            VectorIcon icon = (VectorIcon)RubetteIcons.forResource(path);
            for (int scale = 1; scale <= 2; scale++) {
                int w = icon.getIconWidth();
                BufferedImage img = new BufferedImage(w*scale, w*scale, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.scale(scale, scale);
                icon.paintIcon(new JLabel(), g, 0, 0);
                g.dispose();
                int painted = 0;
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        if ((img.getRGB(x, y) >>> 24) > 128) {
                            painted++;
                        }
                    }
                }
                int area = img.getWidth()*img.getHeight();
                assertTrue(path+" at "+scale+"x paints "+painted, painted > area/40);
                assertTrue(path+" at "+scale+"x paints "+painted, painted < area*3/4);
            }
        }
    }


    public void testIconOnNameLabelTakesTheLabelColor() {
        Color label = new Color(0x123456);
        VectorIcon icon = (VectorIcon)RubetteIcons.forResource("org/rubato/rubettes/harmony/cadenceicon.png");
        JLabel plain = new JLabel();
        plain.setForeground(label);
        JLabel name = new JLabel();
        name.setForeground(label);
        name.putClientProperty(VectorIcon.FOLLOW_FOREGROUND, Boolean.TRUE);
        assertFalse(paintsColor(icon, plain, label));
        assertTrue(paintsColor(icon, name, label));
    }


    private static boolean paintsColor(VectorIcon icon, JLabel host, Color c) {
        int w = icon.getIconWidth();
        BufferedImage img = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        icon.paintIcon(host, g, 0, 0);
        g.dispose();
        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                int p = img.getRGB(x, y);
                if ((p >>> 24) == 255 && (p & 0xffffff) == (c.getRGB() & 0xffffff)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Loads the builtin rubettes in a fresh class loader with Theme set to
     * FlatLaf or classic, since the rubettes keep their icons in static
     * fields, and returns the class names of their icons ("null" for none).
     */
    private static List<String> builtinIconClasses(boolean flat) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            urls.add(new File(entry).toURI().toURL());
        }
        List<String> result = new ArrayList<>();
        try (URLClassLoader loader = new ChildFirst(urls.toArray(new URL[0]))) {
            Class<?> theme = loader.loadClass("org.rubato.composer.Theme");
            Field f = theme.getDeclaredField("flat");
            f.setAccessible(true);
            f.setBoolean(null, flat);
            Field n = theme.getDeclaredField("installed");
            n.setAccessible(true);
            n.set(null, flat ? "light" : "classic");
            for (String name : BuiltinRubettes.classes) {
                Object rubette;
                try {
                    rubette = loader.loadClass(name).getDeclaredConstructor().newInstance();
                }
                catch (Throwable e) {
                    result.add(name+" not loaded: "+e);
                    continue;
                }
                Object icon = rubette.getClass().getMethod("getIcon").invoke(rubette);
                result.add(name+" "+(icon == null ? "null" : icon.getClass().getName()));
            }
        }
        return result;
    }


    public void testBuiltinRubettesGetDrawingsUnderFlatLafAndBitmapsOtherwise() throws Exception {
        List<String> flat = builtinIconClasses(true);
        List<String> classic = builtinIconClasses(false);
        assertEquals(BuiltinRubettes.classes.length, flat.size());
        int drawn = 0;
        for (int k = 0; k < flat.size(); k++) {
            String f = flat.get(k), c = classic.get(k);
            if (f.contains(" not loaded: ")) {
                // a rubette that cannot load in the test JVM must fail the same way under classic
                assertTrue(c, c.contains(" not loaded: "));
                continue;
            }
            if (f.endsWith(" null")) {
                assertTrue(c, c.endsWith(" null"));
                continue;
            }
            assertTrue("no drawing under FlatLaf: "+f, f.endsWith(" org.rubato.composer.icons.VectorIcon"));
            assertTrue("drawing under classic: "+c, c.endsWith(" javax.swing.ImageIcon"));
            drawn++;
        }
        assertTrue("only "+drawn+" rubettes drawn", drawn >= 40);
    }


    private static final class ChildFirst extends URLClassLoader {

        ChildFirst(URL[] urls) {
            super(urls, ClassLoader.getPlatformClassLoader());
        }
    }
}
