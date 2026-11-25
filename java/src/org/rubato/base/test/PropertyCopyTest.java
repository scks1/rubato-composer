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
import java.awt.Container;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.FileProperty;
import org.rubato.base.Rubette;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.base.TextProperty;
import org.rubato.composer.components.JSelectFile;
import org.rubato.rubettes.certificates.AgdaCertificateRubette;
import org.rubato.rubettes.gesture.HandGestureRubette;
import org.rubato.rubettes.image.ImageFileInRubette;
import org.rubato.rubettes.performance.TempoPerformanceRubette;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * A copy of a Text property keeps the wrap settings of its original, and a copy of a File property
 * keeps its save-or-open choice. Rubato copies properties when a rubette is duplicated (Ctrl+D,
 * copy and paste) and when a saved network is loaded, so each case is also checked on the
 * properties editor of a real rubette after those two paths. The tests build the editors without
 * showing them, so they run headless.
 */
public class PropertyCopyTest extends TestCase {

    private static JTextArea area(JComponent editor) {
        assertTrue("editor is a scroll pane", editor instanceof JScrollPane);
        return (JTextArea) ((JScrollPane) editor).getViewport().getView();
    }

    private static <T extends Component> T find(Component c, Class<T> type) {
        if (type.isInstance(c)) {
            return type.cast(c);
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                T t = find(child, type);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static boolean saving(JSelectFile selector) throws Exception {
        Field f = JSelectFile.class.getDeclaredField("saving");
        f.setAccessible(true);
        return f.getBoolean(selector);
    }

    private static Rubette reload(SimpleAbstractRubette r) throws Exception {
        File f = File.createTempFile("property-copy", ".xml");
        try {
            XMLWriter writer = new XMLWriter(f);
            writer.open();
            r.toXML(writer);
            writer.close();
            BufferedReader in = new BufferedReader(new FileReader(f));
            try {
                XMLReader reader = new XMLReader(in);
                Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new BufferedReader(new FileReader(f)))).getDocumentElement();
                reader.parse();
                return r.fromXML(reader, element);
            }
            finally {
                in.close();
            }
        }
        finally {
            f.delete();
        }
    }

    private static void assertWrap(String what, boolean line, boolean word, TextProperty p) {
        JTextArea a = area(p.getJComponent());
        assertEquals(what + ": line wrap", line, a.getLineWrap());
        assertEquals(what + ": word wrap", word, a.getWrapStyleWord());
    }

    public void testCloneKeepsEveryWrapCombination() {
        boolean[] values = { false, true };
        for (boolean line : values) {
            for (boolean word : values) {
                TextProperty p = new TextProperty("t", "Text prop", "one", 60, 5, line, word);
                assertWrap("original " + line + "/" + word, line, word, p);
                assertWrap("clone " + line + "/" + word, line, word, p.clone());
                assertWrap("copy constructor " + line + "/" + word, line, word, new TextProperty(p));
            }
        }
    }

    public void testCloneKeepsValueAndSize() {
        TextProperty p = new TextProperty("t", "Text prop", "one\ntwo", 60, 7, false, false);
        TextProperty c = p.clone();
        assertEquals("one\ntwo", c.getString());
        assertEquals(p.getKey(), c.getKey());
        assertEquals(p.getName(), c.getName());
        JTextArea a = area(c.getJComponent());
        assertEquals(60, a.getColumns());
        assertEquals(7, a.getRows());
    }

    public void testDuplicatedTempoPerformanceWeightsDoNotWrap() {
        TempoPerformanceRubette t = new TempoPerformanceRubette();
        JTextArea before = find(t.getProperties(), JTextArea.class);
        assertFalse("original weights field", before.getLineWrap());
        Rubette copy = t.duplicate();
        JTextArea after = find(copy.getProperties(), JTextArea.class);
        assertFalse("duplicated weights field: line wrap", after.getLineWrap());
        assertFalse("duplicated weights field: word wrap", after.getWrapStyleWord());
    }

    public void testReloadedTempoPerformanceWeightsDoNotWrap() throws Exception {
        TempoPerformanceRubette t = new TempoPerformanceRubette();
        ((TextProperty) t.getProperty("weights")).setString("1 0 0 1\n2 0 1 1");
        Rubette back = reload(t);
        assertEquals("1 0 0 1\n2 0 1 1", ((TextProperty) ((SimpleAbstractRubette) back).getProperty("weights")).getString());
        JTextArea after = find(back.getProperties(), JTextArea.class);
        assertFalse("reloaded weights field: line wrap", after.getLineWrap());
        assertFalse("reloaded weights field: word wrap", after.getWrapStyleWord());
    }

    public void testReloadedHandGestureFingeringDoesNotWrap() throws Exception {
        HandGestureRubette h = new HandGestureRubette();
        JTextArea after = find(reload(h).getProperties(), JTextArea.class);
        assertFalse("reloaded fingering field: line wrap", after.getLineWrap());
        assertFalse("reloaded fingering field: word wrap", after.getWrapStyleWord());
    }

    public void testFileCloneKeepsSaving() throws Exception {
        for (boolean s : new boolean[] { false, true }) {
            FileProperty p = new FileProperty("f", "File prop", new String[] { ".txt" }, s);
            assertEquals("original " + s, s, saving((JSelectFile) p.getJComponent()));
            assertEquals("clone " + s, s, saving((JSelectFile) p.clone().getJComponent()));
            assertEquals("copy constructor " + s, s, saving((JSelectFile) new FileProperty(p).getJComponent()));
        }
    }

    public void testDuplicatedAgdaCertificateStillSaves() throws Exception {
        AgdaCertificateRubette a = new AgdaCertificateRubette();
        assertTrue("original", saving(find(a.getProperties(), JSelectFile.class)));
        assertTrue("duplicate", saving(find(a.duplicate().getProperties(), JSelectFile.class)));
    }

    public void testReloadedAgdaCertificateStillSaves() throws Exception {
        AgdaCertificateRubette a = new AgdaCertificateRubette();
        File target = File.createTempFile("Cert", ".agda");
        target.delete();
        ((FileProperty) a.getProperty("file")).setFile(target);
        Rubette back = reload(a);
        // The reader here has no file of its own, so it resolves the saved relative path against the
        // working directory; only the file name is compared.
        assertEquals(target.getName(),
                ((FileProperty) ((SimpleAbstractRubette) back).getProperty("file")).getFile().getName());
        assertTrue("reloaded", saving(find(back.getProperties(), JSelectFile.class)));
    }

    public void testDuplicatedImageFileInStillOpens() throws Exception {
        ImageFileInRubette i = new ImageFileInRubette();
        assertFalse("original", saving(find(i.getProperties(), JSelectFile.class)));
        assertFalse("duplicate", saving(find(i.duplicate().getProperties(), JSelectFile.class)));
    }
}
