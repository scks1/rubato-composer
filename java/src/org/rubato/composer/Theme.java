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

package org.rubato.composer;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.UIManager;

import org.rubato.composer.preferences.UserPreferences;

/**
 * Installs the look and feel at startup and supplies the colors and fonts
 * that Rubato's own painting code uses.
 *
 * <p>The theme comes from the user preference {@code ui.theme} or, when set,
 * the system property {@code rubato.theme}. "light" and "dark" install
 * FlatLaf; "classic" keeps Swing's Metal look with Rubato's original colors.
 * If FlatLaf cannot be installed, Rubato falls back to the platform look and
 * feel and the classic colors.</p>
 *
 * <p>Rubato-specific colors are UIManager keys starting with "Rubato.",
 * defined in org/rubato/composer/themes/FlatLightLaf.properties and
 * FlatDarkLaf.properties. Each lookup passes the classic color as a fallback,
 * so code running without {@link #install()} (tests, headless use) paints as
 * before. Classes read these colors once, when they load, so a theme change
 * takes effect after a restart.</p>
 */
public final class Theme {

    public static final String LIGHT   = "light";   //$NON-NLS-1$
    public static final String DARK    = "dark";    //$NON-NLS-1$
    public static final String CLASSIC = "classic"; //$NON-NLS-1$

    /** Theme names in the order the preferences dialog lists them. */
    public static final String[] NAMES = { LIGHT, DARK, CLASSIC };

    private static String installed = CLASSIC;
    private static boolean flat = false;

    private Theme() {}


    /**
     * Installs the configured look and feel. Call once, before any Swing
     * component is created.
     */
    public static void install() {
        String name = System.getProperty("rubato.theme"); //$NON-NLS-1$
        if (name == null || name.isEmpty()) {
            name = UserPreferences.getUserPreferences().getTheme();
        }
        install(name);
    }


    /**
     * Installs the look and feel for the theme <code>name</code>.
     */
    public static void install(String name) {
        if (LIGHT.equals(name) || DARK.equals(name)) {
            try {
                com.formdev.flatlaf.FlatLaf.registerCustomDefaultsSource("org.rubato.composer.themes", //$NON-NLS-1$
                                                                       Theme.class.getClassLoader());
                if (DARK.equals(name)) {
                    com.formdev.flatlaf.FlatDarkLaf.setup();
                }
                else {
                    com.formdev.flatlaf.FlatLightLaf.setup();
                }
                installed = name;
                flat = true;
                return;
            }
            catch (Throwable e) {
                // FlatLaf missing from the classpath, or it failed to start
                logger.warning("FlatLaf could not be installed, using the platform look and feel: "+e); //$NON-NLS-1$
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Exception ex) {
                    // keep whatever look and feel Swing chose
                }
            }
        }
        installed = CLASSIC;
        flat = false;
    }


    /**
     * Returns the name of the installed theme.
     */
    public static String getInstalled() {
        return installed;
    }


    /**
     * Returns true if FlatLaf is installed.
     */
    public static boolean isFlat() {
        return flat;
    }


    /**
     * Returns true if the installed theme has a dark background.
     */
    public static boolean isDark() {
        return flat && DARK.equals(installed);
    }


    /**
     * Returns the UIManager color for <code>key</code>, or <code>classic</code>
     * when the key is not defined.
     */
    public static Color color(String key, Color classic) {
        if (!flat) {
            return classic;
        }
        Color c = UIManager.getColor(key);
        return (c != null) ? c : classic;
    }


    /**
     * Returns the default label font with the given style, scaled by
     * <code>factor</code>. With FlatLaf the label font follows the user's
     * text scaling; otherwise <code>classic</code> is returned.
     */
    public static Font font(int style, float factor, Font classic) {
        Font base = UIManager.getFont("Label.font"); //$NON-NLS-1$
        if (!flat || base == null) {
            return classic;
        }
        return base.deriveFont(style, base.getSize2D()*factor);
    }


    /**
     * Scales a size in pixels by FlatLaf's user interface scale factor, which
     * follows the font size (flatlaf.uiScale). Returns <code>size</code>
     * unchanged without FlatLaf.
     */
    public static int scale(int size) {
        return flat ? com.formdev.flatlaf.util.UIScale.scale(size) : size;
    }


    private static final Logger logger = Logger.getLogger("org.rubato.composer"); //$NON-NLS-1$
}
