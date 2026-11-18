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

import java.awt.Color;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.border.Border;

public final class Utilities {

    public static void installEscapeKey(final JDialog comp) {
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                comp.setVisible(false);
            }
        };         
        comp.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE"); //$NON-NLS-1$
        comp.getRootPane().getActionMap().put("ESCAPE", escapeAction); //$NON-NLS-1$
    }

    
    public static void installEnterKey(final JDialog comp, final Action action) {
        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
        comp.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterKeyStroke, "ENTER"); //$NON-NLS-1$
        comp.getRootPane().getActionMap().put("ENTER", action); //$NON-NLS-1$
    }
    
    
    public static void installKey(final JComponent comp, final String keyString, final int key, final Action action) {
        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(key, 0, false);
        comp.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterKeyStroke, keyString);
        comp.getRootPane().getActionMap().put(keyString, action);
    }
    
    
    public static JDialog getJDialog(JComponent c) {
        Container p = c.getParent();
        while (p != null && !(p instanceof JDialog)) {
            p = p.getParent();
        }
        return (JDialog)p;
    }


    public static Window getWindow(JComponent c) {
        Container p = c.getParent();
        while (p != null && !(p instanceof Window)) {
            p = p.getParent();
        }
        return (Window)p;
    }


    public static Border makeTitledBorder(String title) {
        if (Theme.isFlat()) {
            // the look and feel's own titled border
            return BorderFactory.createTitledBorder(title);
        }
        return BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title);
    }


    /**
     * Returns a one-pixel line border in the theme's component border color,
     * or in <code>classic</code> without FlatLaf.
     */
    public static Border makeLineBorder(Color classic) {
        return BorderFactory.createLineBorder(Theme.color("Component.borderColor", classic)); //$NON-NLS-1$
    }


    /**
     * Returns the background for text fields and read-only value labels.
     */
    public static Color textBackground() {
        return Theme.color("TextField.background", Color.WHITE); //$NON-NLS-1$
    }


    /**
     * Returns the foreground for error messages.
     */
    public static Color errorForeground() {
        return Theme.color("Rubato.status.error", Color.RED); //$NON-NLS-1$
    }
    
    
    public final static String NULL_STRING = ""; //$NON-NLS-1$
    
    public final static Color ERROR_BG_COLOR = Theme.color("Rubato.entry.errorBackground", new Color(1.0f, 0.6f, 0.0f)); //$NON-NLS-1$
   
    private Utilities() { /* pure static class */ }
}
