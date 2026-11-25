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

package org.rubato.composer.icons;

import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.net.URL;

import javax.swing.ImageIcon;

import org.rubato.composer.Theme;

@SuppressWarnings("nls")
public class Icons {
    
    public final static ImageIcon emptyIcon; 
    public final static ImageIcon newIcon;
    public final static ImageIcon openIcon;
    public final static ImageIcon revertIcon;
    public final static ImageIcon addIcon;
    public final static ImageIcon saveIcon;
    public final static ImageIcon saveasIcon;
    public final static ImageIcon quitIcon;
    public final static ImageIcon newnetIcon;
    public final static ImageIcon closeIcon;
    public final static ImageIcon runIcon;
    public final static ImageIcon runContIcon;
    public final static ImageIcon stopIcon;
    public final static ImageIcon prefIcon;
    public final static ImageIcon rubatoIcon; 
    public final static ImageIcon splashIcon; 
    public final static ImageIcon denoIcon; 
    public final static ImageIcon formIcon; 
    public final static ImageIcon moduleIcon; 
    public final static ImageIcon morphIcon; 
    public final static ImageIcon limitIcon; 
    public final static ImageIcon colimitIcon; 
    public final static ImageIcon powerIcon; 
    public final static ImageIcon listIcon; 
    public final static ImageIcon simpleIcon; 
    public final static ImageIcon midiinIcon; 
    public final static ImageIcon midioutIcon; 
    public final static ImageIcon tdownIcon; 
    public final static ImageIcon trightIcon; 
    public final static ImageIcon schemeIcon; 
    public final static ImageIcon schemeEditIcon; 
    public final static ImageIcon browseIcon;
    public final static ImageIcon cutIcon;
    public final static ImageIcon copyIcon;
    public final static ImageIcon pasteIcon;

    
    public static ImageIcon loadIcon(Object obj, String name) {
        return loadIcon(obj.getClass(), name);
    }
    
    
    public static ImageIcon loadIcon(Class<?> cls, String name) {
        return loadIcon(cls, name, Theme.isFlat());
    }


    /**
     * Loads the icon <code>name</code> next to <code>cls</code>. With
     * <code>vector</code> true, an icon that {@link RubetteIcons} draws is
     * returned as its drawing; every other icon is loaded from its bitmap.
     */
    static ImageIcon loadIcon(Class<?> cls, String name, boolean vector) {
        if (vector) {
            ImageIcon drawn = RubetteIcons.forResource(resourcePath(cls, name));
            if (drawn != null) {
                return drawn;
            }
        }
        URL imageURL;
        imageURL = cls.getResource(name);
        ImageIcon icon = new ImageIcon(imageURL);
        if (name.startsWith("splash")) {
            // the splash window lays its parts out in fixed pixels
            return icon;
        }
        if (Theme.isDark()) {
            icon = forDarkBackground(icon);
        }
        return scaled(icon);
    }


    /**
     * Loads the icon of a rubette that shares the bitmap <code>name</code>
     * next to <code>cls</code> with another rubette but has a drawing of
     * its own: under FlatLaf the drawing <code>drawing</code> from
     * {@link RubetteIcons}, under the classic theme the shared bitmap.
     */
    public static ImageIcon loadIcon(Class<?> cls, String name, String drawing) {
        return loadIcon(cls, name, drawing, Theme.isFlat());
    }


    static ImageIcon loadIcon(Class<?> cls, String name, String drawing, boolean vector) {
        if (vector) {
            ImageIcon drawn = RubetteIcons.forName(drawing);
            if (drawn != null) {
                return drawn;
            }
        }
        return loadIcon(cls, name, false);
    }


    /**
     * Returns the icon of a rubette that has no bitmap: under FlatLaf the
     * drawing <code>drawing</code> from {@link RubetteIcons}, under the
     * classic theme {@link #emptyIcon}, which a rubette box shows as no
     * icon at all.
     */
    public static ImageIcon drawnIcon(String drawing) {
        return drawnIcon(drawing, Theme.isFlat());
    }


    static ImageIcon drawnIcon(String drawing, boolean vector) {
        ImageIcon drawn = vector ? RubetteIcons.forName(drawing) : null;
        return (drawn != null) ? drawn : emptyIcon;
    }


    /**
     * Returns the class path resource path of <code>name</code> resolved
     * against <code>cls</code>, the way Class.getResource resolves it.
     */
    static String resourcePath(Class<?> cls, String name) {
        if (name.startsWith("/")) { //$NON-NLS-1$
            return name.substring(1);
        }
        String pkg = cls.getPackageName();
        return pkg.isEmpty() ? name : pkg.replace('.', '/')+"/"+name; //$NON-NLS-1$
    }


    /**
     * Returns <code>icon</code> scaled by the user interface scale factor
     * (flatlaf.uiScale). Whole factors replicate pixels so the bitmap icons
     * stay sharp; other factors use smooth scaling.
     */
    public static ImageIcon scaled(ImageIcon icon) {
        int w = icon.getIconWidth(), h = icon.getIconHeight();
        int sw = Theme.scale(w), sh = Theme.scale(h);
        if (w <= 0 || h <= 0 || (sw == w && sh == h)) {
            return icon;
        }
        boolean whole = (sw % w == 0) && (sh % h == 0);
        return new ImageIcon(icon.getImage().getScaledInstance(sw, sh,
            whole ? java.awt.Image.SCALE_REPLICATE : java.awt.Image.SCALE_SMOOTH));
    }


    /**
     * Returns a copy of <code>icon</code> for a dark background. The icons
     * are drawn in black and gray for a light background, so the filter
     * inverts the lightness of dark, unsaturated pixels and leaves colored
     * pixels and transparency alone.
     */
    public static ImageIcon forDarkBackground(ImageIcon icon) {
        RGBImageFilter filter = new RGBImageFilter() {
            { canFilterIndexColorModel = true; }
            public int filterRGB(int x, int y, int argb) {
                int a = argb >>> 24;
                int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                if (a == 0 || max - min > 48 || max > 140) {
                    return argb;
                }
                // dark gray or black: mirror the lightness around the middle
                int r2 = 255 - r*3/4 - 32, g2 = 255 - g*3/4 - 32, b2 = 255 - b*3/4 - 32;
                return (a << 24) | (r2 << 16) | (g2 << 8) | b2;
            }
        };
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(
            new FilteredImageSource(icon.getImage().getSource(), filter)));
    }

    
    public static ImageIcon loadIcon(String name) {
        return loadIcon(Icons.class, name);
    }
    

    static {
        boolean vector = Theme.isFlat();
        emptyIcon   = vector ? VectorIcons.emptyIcon()   : loadIcon("emptyicon.png");
        newIcon     = vector ? VectorIcons.newIcon()     : loadIcon("newicon.png");
        openIcon    = vector ? VectorIcons.openIcon()    : loadIcon("openicon.png");
        revertIcon  = vector ? VectorIcons.revertIcon()  : loadIcon("reverticon.png");
        addIcon     = vector ? VectorIcons.addIcon()     : loadIcon("addicon.png");
        saveIcon    = vector ? VectorIcons.saveIcon()    : loadIcon("saveicon.png");
        saveasIcon  = vector ? VectorIcons.saveasIcon()  : loadIcon("saveasicon.png");
        quitIcon    = vector ? VectorIcons.quitIcon()    : loadIcon("quiticon.png");
        runIcon     = vector ? VectorIcons.runIcon()     : loadIcon("runicon.png");
        runContIcon = vector ? VectorIcons.runContIcon() : loadIcon("runconticon.png");
        stopIcon    = vector ? VectorIcons.stopIcon()    : loadIcon("stopicon.png");
        newnetIcon  = vector ? VectorIcons.newnetIcon()  : loadIcon("newneticon.png");
        closeIcon   = vector ? VectorIcons.closeIcon()   : loadIcon("closeicon.png");
        prefIcon    = vector ? VectorIcons.prefIcon()    : loadIcon("preficon.png");
        rubatoIcon  = loadIcon("rubatoicon.png");
        splashIcon  = loadIcon("splash.png");
        denoIcon    = vector ? VectorIcons.denoIcon()    : loadIcon("denotator.png");
        formIcon    = vector ? VectorIcons.formIcon()    : loadIcon("form.png");
        moduleIcon  = vector ? VectorIcons.moduleIcon()  : loadIcon("module.png");
        morphIcon   = vector ? VectorIcons.morphIcon()   : loadIcon("morph.png");
        limitIcon   = loadIcon("limit.png");
        colimitIcon = loadIcon("colimit.png");
        powerIcon   = loadIcon("power.png");
        listIcon    = loadIcon("list.png");
        simpleIcon  = loadIcon("simple.png");
        midiinIcon  = loadIcon("midiin.png");
        midioutIcon = loadIcon("midiout.png");
        tdownIcon   = vector ? VectorIcons.tdownIcon()   : loadIcon("triandown.png");
        trightIcon  = vector ? VectorIcons.trightIcon()  : loadIcon("trianright.png");
        schemeIcon  = vector ? VectorIcons.schemeIcon()  : loadIcon("schemeicon.png");
        schemeEditIcon = vector ? VectorIcons.schemeEditIcon() : loadIcon("schemeediticon.png");
        browseIcon  = vector ? VectorIcons.browseIcon()  : loadIcon("browseicon.png");
        cutIcon     = vector ? VectorIcons.cutIcon()     : loadIcon("cuticon.png");
        copyIcon    = vector ? VectorIcons.copyIcon()    : loadIcon("copyicon.png");
        pasteIcon   = vector ? VectorIcons.pasteIcon()   : loadIcon("pasteicon.png");
    }
}
