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

package org.rubato.base.test;

import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import junit.framework.TestCase;

import org.rubato.base.JRubettePropertiesDialog;
import org.rubato.base.RubetteProperties;
import org.rubato.base.TextProperty;

/**
 * The text area of a Text property hands Tab and Shift+Tab to focus traversal, so a keyboard user
 * can move past it in a properties dialog. Ctrl+Tab and Ctrl+Shift+Tab keep traversing as before.
 * The tests build the editor without showing it, so they run headless.
 */
public class TextPropertyTest extends TestCase {

    private static final AWTKeyStroke TAB = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
    private static final AWTKeyStroke SHIFT_TAB = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
    private static final AWTKeyStroke CTRL_TAB = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK);
    private static final AWTKeyStroke CTRL_SHIFT_TAB =
        KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    private static JTextArea area(JComponent editor) {
        assertTrue("editor is a scroll pane", editor instanceof JScrollPane);
        Component view = ((JScrollPane) editor).getViewport().getView();
        assertTrue("scroll pane holds a text area", view instanceof JTextArea);
        return (JTextArea) view;
    }

    private static Set<AWTKeyStroke> forward(Component c) {
        return c.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
    }

    private static Set<AWTKeyStroke> backward(Component c) {
        return c.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
    }

    public void testTabAndShiftTabTraverse() {
        JTextArea a = area(new TextProperty("t", "Text prop", "one\ntwo").getJComponent());
        assertTrue("Tab moves focus forward", forward(a).contains(TAB));
        assertTrue("Shift+Tab moves focus backward", backward(a).contains(SHIFT_TAB));
        assertTrue("traversal keys are enabled", a.getFocusTraversalKeysEnabled());
    }

    public void testCtrlTabStillTraverses() {
        JTextArea a = area(new TextProperty("t", "Text prop", "").getJComponent());
        assertTrue("Ctrl+Tab moves focus forward", forward(a).contains(CTRL_TAB));
        assertTrue("Ctrl+Shift+Tab moves focus backward", backward(a).contains(CTRL_SHIFT_TAB));
    }

    public void testKeysMatchTheDefaultSets() {
        JTextArea a = area(new TextProperty("t", "Text prop", "").getJComponent());
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        assertEquals(kfm.getDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS), forward(a));
        assertEquals(kfm.getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS), backward(a));
    }

    public void testEveryEditorTraverses() {
        TextProperty p = new TextProperty("t", "Text prop", "");
        JTextArea first = area(p.getJComponent());
        JTextArea second = area(p.getJComponent());
        assertNotSame(first, second);
        assertTrue(forward(second).contains(TAB));
        assertTrue(backward(second).contains(SHIFT_TAB));
    }

    public void testTraversesWithoutWrap() {
        JTextArea a = area(new TextProperty("w", "Weights", "", 60, 5, false, false).getJComponent());
        assertTrue(forward(a).contains(TAB));
        assertTrue(backward(a).contains(SHIFT_TAB));
    }

    public void testEditorOtherwiseUnchanged() {
        JTextArea a = area(new TextProperty("t", "Text prop", "one\ttwo", 60, 5, false, true).getJComponent());
        assertEquals("one\ttwo", a.getText());
        assertTrue(a.isEditable());
        assertFalse(a.getLineWrap());
        assertTrue(a.getWrapStyleWord());
        assertEquals(60, a.getColumns());
        assertEquals(5, a.getRows());
    }

    public void testValueStillFollowsTheArea() {
        TextProperty p = new TextProperty("t", "Text prop", "a");
        JTextArea a = area(p.getJComponent());
        a.setText("3\n2\n3");
        a.setCaretPosition(a.getText().length());
        p.apply();
        assertEquals("3\n2\n3", p.getString());
    }

    public void testTextAreaInTheDialogTraverses() {
        RubetteProperties props = new RubetteProperties();
        props.put(new TextProperty("t", "Text prop", "x"));
        JRubettePropertiesDialog dialog = new JRubettePropertiesDialog(props);
        JTextArea found = null;
        for (Component c : ((JPanel) dialog.getComponent(0)).getComponents()) {
            if (c instanceof JScrollPane && ((JScrollPane) c).getViewport().getView() instanceof JTextArea) {
                found = (JTextArea) ((JScrollPane) c).getViewport().getView();
            }
        }
        assertNotNull("dialog shows the text area", found);
        assertTrue(forward(found).contains(TAB));
        assertTrue(backward(found).contains(SHIFT_TAB));
    }
}
