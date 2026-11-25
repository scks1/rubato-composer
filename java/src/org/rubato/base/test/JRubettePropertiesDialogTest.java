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

import java.awt.Component;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import junit.framework.TestCase;

import org.rubato.base.*;
import org.rubato.composer.components.JLabelField;
import org.rubato.math.arith.Complex;
import org.rubato.math.arith.Rational;

/**
 * Labels in the generic rubette properties dialog name the component that takes focus for each
 * property, so assistive technology announces the property's name. The tests build the dialog
 * without showing it, so they run headless.
 */
public class JRubettePropertiesDialogTest extends TestCase {

    private static RubetteProperties allTypes() {
        RubetteProperties p = new RubetteProperties();
        p.put(new BooleanProperty("b", "Boolean prop", false));
        p.put(new ComplexProperty("c", "Complex prop", new Complex(1, 2)));
        p.put(new DoubleProperty("d", "Double prop", 1.5));
        p.put(new IntegerProperty("i", "Integer prop", 3));
        p.put(new RationalProperty("r", "Rational prop", new Rational(1, 3)));
        p.put(new StringProperty("s", "String prop", "strict"));
        p.put(new TextProperty("t", "Text prop", "text"));
        p.put(new FileProperty("f", "File prop", new String[] { ".txt" }, false));
        p.put(new FormProperty("fo", "Form prop"));
        p.put(new DenotatorProperty("de", "Denotator prop"));
        return p;
    }

    private static Component[] rows(RubetteProperties p) {
        JRubettePropertiesDialog dialog = new JRubettePropertiesDialog(p);
        assertEquals(1, dialog.getComponentCount());
        return ((JPanel) dialog.getComponent(0)).getComponents();
    }

    private static JLabel label(Component[] cs, String text) {
        for (Component c : cs) {
            if (c instanceof JLabel && ((JLabel) c).getText().equals(text)) {
                return (JLabel) c;
            }
        }
        fail("no label " + text);
        return null;
    }

    private static Component editorAfter(Component[] cs, JLabel label) {
        for (int k = 0; k + 1 < cs.length; k++) {
            if (cs[k] == label) {
                return cs[k + 1];
            }
        }
        fail("no editor after " + label.getText());
        return null;
    }

    private static void assertNamedBy(JLabel label, Component target) {
        assertSame(label.getText(), target, label.getLabelFor());
        assertSame(label.getText(), label, ((JComponent) target).getClientProperty("labeledBy"));
        assertEquals(label.getText(), target.getAccessibleContext().getAccessibleName());
    }

    public void testLayoutUnchanged() {
        Component[] cs = rows(allTypes());
        assertEquals(20, cs.length);
        String[] names = { "Boolean prop:", "Complex prop:", "Double prop:", "Integer prop:", "Rational prop:",
                           "String prop:", "Text prop:", "File prop:", "Form prop:", "Denotator prop:" };
        for (int k = 0; k < names.length; k++) {
            assertTrue(cs[2 * k] instanceof JLabel);
            assertEquals(names[k], ((JLabel) cs[2 * k]).getText());
            assertFalse(cs[2 * k + 1] instanceof JLabel);
        }
    }

    public void testTextFieldEditorsAreNamed() {
        Component[] cs = rows(allTypes());
        for (String n : new String[] { "Complex prop:", "Double prop:", "Integer prop:", "Rational prop:",
                                       "String prop:" }) {
            JLabel l = label(cs, n);
            Component editor = editorAfter(cs, l);
            assertTrue(n, editor instanceof JTextField);
            assertNamedBy(l, editor);
        }
    }

    public void testTextAreaInsideScrollPaneIsNamed() {
        Component[] cs = rows(allTypes());
        JLabel l = label(cs, "Text prop:");
        JScrollPane editor = (JScrollPane) editorAfter(cs, l);
        Component area = editor.getViewport().getView();
        assertTrue(area instanceof JTextArea);
        assertNamedBy(l, area);
    }

    public void testCheckboxInsidePanelIsNamed() {
        Component[] cs = rows(allTypes());
        JLabel l = label(cs, "Boolean prop:");
        JPanel editor = (JPanel) editorAfter(cs, l);
        JCheckBox box = (JCheckBox) editor.getComponent(0);
        assertEquals("", box.getText());
        assertNamedBy(l, box);
    }

    public void testSelectorsNameTheirValueFieldAndKeepButtonNames() {
        Component[] cs = rows(allTypes());
        String[][] cases = { { "File prop:", "Browse..." }, { "Form prop:", "Select" },
                             { "Denotator prop:", "Select" } };
        for (String[] c : cases) {
            JLabel l = label(cs, c[0]);
            JComponent editor = (JComponent) editorAfter(cs, l);
            JLabelField field = find(editor, JLabelField.class);
            assertNotNull(c[0], field);
            assertNamedBy(l, field);
            AbstractButton button = find(editor, AbstractButton.class);
            assertNotNull(c[0], button);
            assertEquals(c[0], c[1], button.getAccessibleContext().getAccessibleName());
            assertNull(c[0], button.getClientProperty("labeledBy"));
        }
    }

    public void testThreeVoiceCounterpointFieldsAreNamed() {
        JComponent dialog = new org.rubato.rubettes.counterpoint.ThreeVoiceCounterpointRubette().getProperties();
        assertTrue(dialog instanceof JRubettePropertiesDialog);
        Component[] cs = ((JPanel) dialog.getComponent(0)).getComponents();
        int named = 0;
        for (int k = 0; k + 1 < cs.length; k += 2) {
            JLabel l = (JLabel) cs[k];
            Component target = l.getLabelFor();
            assertNotNull(l.getText(), target);
            assertEquals(l.getText(), target.getAccessibleContext().getAccessibleName());
            named++;
        }
        assertEquals(6, named);
    }

    private static <T> T find(Component c, Class<T> type) {
        if (type.isInstance(c)) {
            return type.cast(c);
        }
        if (c instanceof java.awt.Container) {
            for (Component d : ((java.awt.Container) c).getComponents()) {
                T t = find(d, type);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }
}
