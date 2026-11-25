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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import junit.framework.TestCase;

import org.rubato.composer.BuiltinRubettes;

/**
 * Checks the drawings that {@link RubetteIcons} keeps by name: the icons of
 * rubettes without a bitmap, which show nothing under the classic theme,
 * and the icons of rubettes that share a bitmap with another rubette,
 * which keep the shared bitmap under the classic theme.
 */
@SuppressWarnings("nls")
public class DrawnRubetteIconsTest extends TestCase {

    /** The builtin rubettes that have no bitmap of their own. */
    static final String[] WITHOUT_BITMAP = {
        "org.rubato.rubettes.morphing.MorphingRubette",
        "org.rubato.rubettes.texture.TexturalizeRubette",
        "org.rubato.rubettes.wallpaper.WallpaperRubette",
        "org.rubato.rubettes.alteration.AlterationRubette",
        "org.rubato.rubettes.builtin.ModuleMapRubette",
        "org.rubato.rubettes.score.ScoreToCsoundRubette",
        "org.rubato.rubettes.bigbang.BigBangRubette",
        "org.rubato.rubettes.builtin.SelectFormRubette",
    };


    public void testEveryNamedDrawingPaintsInsideItsGrid() {
        assertFalse(RubetteIcons.names().isEmpty());
        for (String name : RubetteIcons.names()) {
            ImageIcon icon = RubetteIcons.forName(name);
            assertTrue(name, icon instanceof VectorIcon);
            for (int scale = 1; scale <= 2; scale++) {
                int painted = painted(paint(icon, scale));
                int area = 16*16*scale*scale;
                assertTrue(name+" at "+scale+"x paints "+painted, painted > area/40);
                assertTrue(name+" at "+scale+"x paints "+painted, painted < area*3/4);
            }
        }
    }


    public void testNamedDrawingsDoNotReuseBitmapPaths() {
        for (String name : RubetteIcons.names()) {
            assertFalse(name, name.contains("/"));
            assertNull(name, RubetteIcons.forResource(name));
        }
    }


    public void testDrawnIconIsTheDrawingOnlyForFlatLaf() {
        assertTrue(Icons.drawnIcon("morphing", true) instanceof VectorIcon);
        assertSame(Icons.emptyIcon, Icons.drawnIcon("morphing", false));
        // a name without a drawing shows nothing rather than failing
        assertSame(Icons.emptyIcon, Icons.drawnIcon("no such drawing", true));
    }


    public void testUnknownDrawingFallsBackToTheBitmap() {
        ImageIcon icon = Icons.loadIcon(Icons.class, "rubatoicon.png", "no such drawing", true);
        assertFalse(icon instanceof VectorIcon);
        assertTrue(icon.getIconWidth() > 0);
    }


    /**
     * Loads the builtin rubettes in a fresh class loader with Theme set to
     * FlatLaf or classic, since the rubettes keep their icons in static
     * fields, and returns each rubette's icon (absent if the rubette cannot
     * load in the test JVM) together with that loader's emptyIcon.
     */
    private static Map<String,Object> builtinIcons(boolean flat) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            urls.add(new File(entry).toURI().toURL());
        }
        Map<String,Object> result = new LinkedHashMap<>();
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader())) {
            Class<?> theme = loader.loadClass("org.rubato.composer.Theme");
            Field f = theme.getDeclaredField("flat");
            f.setAccessible(true);
            f.setBoolean(null, flat);
            Field n = theme.getDeclaredField("installed");
            n.setAccessible(true);
            n.set(null, flat ? "light" : "classic");
            result.put("emptyIcon", loader.loadClass("org.rubato.composer.icons.Icons").getField("emptyIcon").get(null));
            for (String name : BuiltinRubettes.classes) {
                Object rubette;
                try {
                    rubette = loader.loadClass(name).getDeclaredConstructor().newInstance();
                }
                catch (Throwable e) {
                    continue;
                }
                result.put(name, rubette.getClass().getMethod("getIcon").invoke(rubette));
            }
        }
        return result;
    }


    public void testEveryBuiltinRubetteGetsADrawingUnderFlatLaf() throws Exception {
        Map<String,Object> icons = builtinIcons(true);
        Object empty = icons.remove("emptyIcon");
        assertTrue("only "+icons.size()+" rubettes loaded", icons.size() >= 50);
        for (Map.Entry<String,Object> e : icons.entrySet()) {
            Object icon = e.getValue();
            assertNotNull("no icon: "+e.getKey(), icon);
            assertNotSame("blank icon: "+e.getKey(), empty, icon);
            assertEquals(e.getKey(), "org.rubato.composer.icons.VectorIcon", icon.getClass().getName());
            assertTrue("paints nothing: "+e.getKey(), painted(paint((Icon)icon, 1)) > 0);
        }
        for (String name : WITHOUT_BITMAP) {
            assertTrue("not loaded: "+name, icons.containsKey(name));
        }
    }


    public void testRubettesWithoutBitmapShowNothingUnderClassic() throws Exception {
        Map<String,Object> icons = builtinIcons(false);
        Object empty = icons.get("emptyIcon");
        assertFalse(empty.getClass().getName().endsWith("VectorIcon"));
        for (String name : WITHOUT_BITMAP) {
            assertTrue("not loaded: "+name, icons.containsKey(name));
            assertSame(name, empty, icons.get(name));
        }
    }


    static BufferedImage paint(Icon icon, int scale) {
        int w = icon.getIconWidth();
        BufferedImage img = new BufferedImage(w*scale, w*scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        icon.paintIcon(new JLabel(), g, 0, 0);
        g.dispose();
        return img;
    }


    static int painted(BufferedImage img) {
        int painted = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 128) {
                    painted++;
                }
            }
        }
        return painted;
    }
}
