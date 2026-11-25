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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.BooleanProperty;
import org.rubato.base.DenotatorProperty;
import org.rubato.base.FileProperty;
import org.rubato.base.FormProperty;
import org.rubato.base.Repository;
import org.rubato.base.Rubette;
import org.rubato.base.RubetteProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.composer.BuiltinRubettes;
import org.rubato.math.module.RElement;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.rubettes.harmony.ModulationRubette;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Apply in a rubette's properties window, pressed without an edit, leaves every property as it
 * was. The window's Apply and Apply &amp; hide buttons call the rubette's applyProperties(), which
 * copies each property's pending edit over its value; the tests build the editors the window
 * shows with getProperties() and then call applyProperties(), the same calls the buttons make.
 * Every built-in rubette with simple properties is checked new, after a duplicate, after a save
 * and reload, and after loading a network saved before its properties existed. The tests build
 * the editors without showing them, so they run headless.
 */
public class PropertiesApplyKeepsValuesTest extends TestCase {

    private static List<SimpleAbstractRubette> builtins() throws Exception {
        List<SimpleAbstractRubette> list = new ArrayList<SimpleAbstractRubette>();
        List<String> absent = new ArrayList<String>();
        for (String name : BuiltinRubettes.classes) {
            Object o;
            try {
                o = Class.forName(name).newInstance();
            }
            catch (ClassNotFoundException e) {
                // Some built-ins live in modules this test's class path leaves out; the loader skips them too.
                absent.add(name);
                continue;
            }
            if (o instanceof SimpleAbstractRubette && ((SimpleAbstractRubette) o).getRubetteProperties() != null
                    && !((SimpleAbstractRubette) o).getRubetteProperties().getProperties().isEmpty()) {
                list.add((SimpleAbstractRubette) o);
            }
        }
        System.out.println("PropertiesApplyKeepsValuesTest: " + list.size() + " built-in rubettes with properties checked; "
                + absent.size() + " not on the class path: " + absent);
        return list;
    }

    private static Map<String, Object> values(Rubette r) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (RubetteProperty p : ((SimpleAbstractRubette) r).getRubetteProperties().getProperties()) {
            Object v = p.getValue();
            m.put(p.getKey(), v instanceof File ? ((File) v).getName() : v);
        }
        return m;
    }

    /** Opens the editors as the window does, applies with no edit, and lists every changed value. */
    private static void applyWithoutEdit(String what, Rubette r, Map<String, Object> expected, List<String> changes) {
        r.getProperties();
        r.applyProperties();
        Map<String, Object> after = values(r);
        for (Map.Entry<String, Object> e : expected.entrySet()) {
            Object a = after.get(e.getKey());
            if (e.getValue() == null ? a != null : !e.getValue().equals(a)) {
                changes.add(what + " " + r.getClass().getSimpleName() + "." + e.getKey() + ": "
                        + e.getValue() + " -> " + a);
            }
        }
    }

    /** Sets every Boolean to the opposite of its default and every File to a file. */
    private static void setNonDefaults(SimpleAbstractRubette r) throws Exception {
        for (RubetteProperty p : r.getRubetteProperties().getProperties()) {
            if (p instanceof BooleanProperty) {
                ((BooleanProperty) p).setBoolean(!((BooleanProperty) p).getBoolean());
            }
            else if (p instanceof FileProperty) {
                File f = File.createTempFile("apply-" + p.getKey(), ".out");
                f.delete();
                ((FileProperty) p).setFile(f);
            }
        }
    }

    private static Rubette load(SimpleAbstractRubette r, boolean withProperties) throws Exception {
        File f = File.createTempFile("properties-apply", ".xml");
        try {
            XMLWriter writer = new XMLWriter(f);
            writer.open();
            if (withProperties) {
                r.toXML(writer);
            }
            writer.close();
            BufferedReader in = new BufferedReader(new FileReader(f));
            try {
                XMLReader reader = new XMLReader(in);
                Element element;
                if (withProperties) {
                    element = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .parse(new InputSource(new BufferedReader(new FileReader(f)))).getDocumentElement();
                }
                else {
                    // A network saved before these properties existed: the rubette's element is empty.
                    element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
                            .createElement("Rubette");
                }
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

    private static void assertNoChanges(String what, int checked, List<String> changes) {
        assertTrue(what + ": no built-in rubettes with properties found", checked > 10);
        assertTrue(what + ": Apply with no edit changed " + changes.size() + " values:\n  "
                + String.join("\n  ", changes), changes.isEmpty());
    }

    public void testEveryNewBuiltinRubetteKeepsItsValues() throws Exception {
        List<String> changes = new ArrayList<String>();
        List<SimpleAbstractRubette> all = builtins();
        for (SimpleAbstractRubette r : all) {
            applyWithoutEdit("new", r, values(r), changes);
        }
        assertNoChanges("new", all.size(), changes);
    }

    public void testEveryDuplicatedBuiltinRubetteKeepsItsValues() throws Exception {
        List<String> changes = new ArrayList<String>();
        List<SimpleAbstractRubette> all = builtins();
        for (SimpleAbstractRubette r : all) {
            setNonDefaults(r);
            Rubette copy = r.duplicate();
            applyWithoutEdit("duplicate of", copy, values(r), changes);
        }
        assertNoChanges("duplicate", all.size(), changes);
    }

    public void testEveryDuplicatedNewBuiltinRubetteKeepsItsDefaults() throws Exception {
        List<String> changes = new ArrayList<String>();
        List<SimpleAbstractRubette> all = builtins();
        for (SimpleAbstractRubette r : all) {
            applyWithoutEdit("duplicate of new", r.duplicate(), values(r), changes);
        }
        assertNoChanges("duplicate of new", all.size(), changes);
    }

    public void testEveryReloadedBuiltinRubetteKeepsItsValues() throws Exception {
        List<String> changes = new ArrayList<String>();
        List<SimpleAbstractRubette> all = builtins();
        for (SimpleAbstractRubette r : all) {
            setNonDefaults(r);
            Rubette back = load(r, true);
            Map<String, Object> expected = values(r);
            Map<String, Object> loaded = values(back);
            if (!expected.equals(loaded)) {
                changes.add("load " + r.getClass().getSimpleName() + ": " + expected + " -> " + loaded);
            }
            applyWithoutEdit("reloaded", back, expected, changes);
        }
        assertNoChanges("reload", all.size(), changes);
    }

    public void testOldNetworkWithoutPropertiesLoadsDefaults() throws Exception {
        List<String> changes = new ArrayList<String>();
        List<SimpleAbstractRubette> all = builtins();
        for (SimpleAbstractRubette r : all) {
            Map<String, Object> defaults = values(r);
            Rubette back = load(r, false);
            Map<String, Object> loaded = values(back);
            for (Map.Entry<String, Object> e : defaults.entrySet()) {
                Object a = loaded.get(e.getKey());
                if (e.getValue() == null ? a != null : !e.getValue().equals(a)) {
                    changes.add("load " + r.getClass().getSimpleName() + "." + e.getKey() + ": "
                            + e.getValue() + " -> " + a);
                }
            }
            applyWithoutEdit("old network", back, defaults, changes);
        }
        assertNoChanges("old network", all.size(), changes);
    }

    public void testNewModulationKeepsAutXRigidity() {
        ModulationRubette m = new ModulationRubette();
        BooleanProperty autX = (BooleanProperty) m.getProperty("autX");
        assertTrue("default rigidity is AUT_X", autX.getBoolean());
        m.getProperties();
        m.applyProperties();
        assertTrue("Apply with no edit keeps AUT_X rigidity (off would be literal TI12)", autX.getBoolean());
        assertTrue("covered filter stays on", ((BooleanProperty) m.getProperty("covered")).getBoolean());
    }

    public void testBooleanPropertyAppliesItsOwnValue() {
        for (boolean v : new boolean[] { false, true }) {
            BooleanProperty p = new BooleanProperty("b", "Bool", v);
            p.apply();
            assertEquals("new " + v, v, p.getBoolean());
            BooleanProperty q = new BooleanProperty("b", "Bool", false);
            q.setBoolean(v);
            BooleanProperty c = q.clone();
            c.apply();
            assertEquals("clone " + v, v, c.getBoolean());
        }
    }

    public void testFilePropertyCloneAppliesItsOwnValue() {
        FileProperty p = new FileProperty("f", "File", new String[] { ".txt" }, true);
        p.apply();
        assertNull("new file property stays empty", p.getFile());
        File f = new File("apply-keeps.txt");
        p.setFile(f);
        FileProperty c = p.clone();
        c.apply();
        assertEquals(f, c.getFile());
    }

    public void testFormPropertyAppliesItsOwnValue() {
        Form score = Repository.systemRepository().getForm("Score");
        assertNotNull(score);
        FormProperty p = new FormProperty("form", "Form", score);
        FormProperty c = p.clone();
        p.getJComponent();
        p.apply();
        assertSame("new", score, p.getForm());
        c.apply();
        assertSame("clone", score, c.getForm());
        FormProperty empty = new FormProperty("form", "Form");
        empty.apply();
        assertNull("empty stays empty", empty.getForm());
    }

    public void testDenotatorPropertyAppliesItsOwnValue() throws Exception {
        SimpleForm real = (SimpleForm) Repository.systemRepository().getForm("Real");
        Denotator d = new SimpleDenotator(NameDenotator.make("d"), real, new RElement(1.5));
        DenotatorProperty p = new DenotatorProperty("den", "Denotator", d);
        DenotatorProperty c = p.clone();
        p.getJComponent();
        p.apply();
        assertSame("new", d, p.getDenotator());
        c.apply();
        assertSame("clone", d, c.getDenotator());
    }

    public void testPendingEditStillApplies() {
        BooleanProperty p = new BooleanProperty("b", "Bool", true);
        p.getJComponent();
        javax.swing.JCheckBox box = (javax.swing.JCheckBox) ((java.awt.Container) p.getJComponent()).getComponent(0);
        box.doClick();
        assertTrue("click alone does not apply", p.getBoolean());
        p.apply();
        assertFalse("Apply takes the edit", p.getBoolean());
        p.revert();
        assertFalse("revert after Apply keeps the applied value", p.getBoolean());
    }

    public void testRevertDropsPendingEdit() {
        BooleanProperty p = new BooleanProperty("b", "Bool", true);
        javax.swing.JCheckBox box = (javax.swing.JCheckBox) ((java.awt.Container) p.getJComponent()).getComponent(0);
        box.doClick();
        p.revert();
        assertTrue("box shows the value again", box.isSelected());
        p.apply();
        assertTrue("Apply after Revert keeps the value", p.getBoolean());
    }
}
