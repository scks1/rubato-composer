package org.rubato.rubettes.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.base.Rubette;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.RAffineMorphism;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.rubettes.wallpaper.JWallpaperDimensionsTable;
import org.rubato.rubettes.wallpaper.WallpaperRubette;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Checks that the Wallpaper rubette's applied ranges and coordinates change only through a
 * successful Apply: opening the view or a failed Apply validates the table into the working
 * copy and leaves the applied settings, the save and run() unchanged.
 */
public class WallpaperWorkingCopyTest extends TestCase {

    private static Object call(Object o, String name, Object... args) throws Exception {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == args.length) {
                    m.setAccessible(true);
                    return m.invoke(o, args);
                }
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static String xml(Rubette r) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        XMLWriter writer = new XMLWriter(new PrintStream(bytes, true, "UTF-8"));
        writer.open();
        r.toXML(writer);
        writer.close();
        return bytes.toString("UTF-8");
    }

    /** Saves the rubette and reads it back with fromXML on a new prototype, as a network reload does. */
    private static WallpaperRubette reload(Rubette r) throws Exception {
        String s = xml(r);
        Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(s))).getDocumentElement();
        XMLReader reader = new XMLReader(new StringReader("<Rubato></Rubato>"));
        reader.parse();
        Rubette loaded = r.newInstance().fromXML(reader, element);
        assertFalse(reader.getErrors() + " in " + s, reader.hasError());
        assertNotNull("fromXML returned null for " + s, loaded);
        return (WallpaperRubette) loaded;
    }

    /** Coordinates as the table stores them: one single-index path per simple form chosen. */
    private static List<List<Integer>> paths(int... simpleForms) {
        List<List<Integer>> p = new ArrayList<List<Integer>>();
        for (int c : simpleForms) {
            List<Integer> path = new ArrayList<Integer>();
            path.add(c);
            p.add(path);
        }
        return p;
    }

    /** A Wallpaper on Score with one applied morphism over the range 0..2 from simple form 0 to simple form 0. */
    private static WallpaperRubette wallpaper() throws Exception {
        WallpaperRubette r = (WallpaperRubette) new WallpaperRubette().newInstance();
        call(r, "setInputForm", (PowerForm) Repository.systemRepository().getForm("Score"));
        call(r, "addMorphism", new RAffineMorphism(2, 1), 0, 2, paths(0, 0));
        return r;
    }

    private static DefaultTableModel model(WallpaperRubette r) {
        return (DefaultTableModel) r.getMorphismsTable().getModel();
    }

    private static ModuleMorphism first(WallpaperRubette r) {
        return r.getMorphismsTable().getMorphisms().get(0);
    }

    private static int appliedTo(WallpaperRubette r) {
        return r.getMorphismsTable().getRangeTo(first(r));
    }

    /**
     * A score with one default note, mapped by the rubette's applied morphisms. The morphism list
     * is copied first, as run() does, because getUnitedMappedDenotators consumes the list.
     */
    private static PowerDenotator output(WallpaperRubette r) throws Exception {
        LimitForm noteForm = (LimitForm) Repository.systemRepository().getForm("Note");
        PowerForm scoreForm = (PowerForm) Repository.systemRepository().getForm("Score");
        List<Denotator> notes = new ArrayList<Denotator>();
        notes.add(noteForm.createDefaultDenotator());
        PowerDenotator input = new PowerDenotator(NameDenotator.make(""), scoreForm, notes);
        List<ModuleMorphism> morphisms = new ArrayList<ModuleMorphism>(r.getMorphismsTable().getMorphisms());
        return (PowerDenotator) call(r, "getUnitedMappedDenotators", input, morphisms);
    }

    public void testOpeningTheViewKeepsTheAppliedRange() throws Exception {
        WallpaperRubette r = wallpaper();
        String before = xml(r);
        model(r).setValueAt("1", 0, 1);
        model(r).setValueAt("4", 0, 2);
        r.getView();
        assertEquals(0, r.getMorphismsTable().getRangeFrom(first(r)));
        assertEquals(2, appliedTo(r));
        assertEquals(before, xml(r));
    }

    public void testOpeningTheViewKeepsTheAppliedCoordinates() throws Exception {
        WallpaperRubette r = wallpaper();
        model(r).setValueAt(paths(1, 1), 0, 3);
        r.getView();
        assertEquals(paths(0, 0), r.getMorphismsTable().getCoordinates(first(r)));
    }

    public void testOpeningTheViewKeepsTheRunOutput() throws Exception {
        WallpaperRubette r = wallpaper();
        PowerDenotator before = output(r);
        model(r).setValueAt("4", 0, 2);
        r.getView();
        PowerDenotator after = output(r);
        assertEquals(before.getFactorCount(), after.getFactorCount());
        assertEquals(before, after);
    }

    public void testAFailedApplyKeepsTheAppliedSettings() throws Exception {
        WallpaperRubette r = wallpaper();
        r.getProperties();
        String before = xml(r);
        model(r).setValueAt("4", 0, 2);
        r.getMorphismsTable().addMorphism();
        assertFalse(r.applyProperties());
        assertEquals(1, r.getMorphismsTable().getMorphisms().size());
        assertEquals(2, appliedTo(r));
        assertEquals(before, xml(r));
    }

    public void testASuccessfulApplyTakesEffect() throws Exception {
        WallpaperRubette r = wallpaper();
        r.getProperties();
        int countBefore = output(r).getFactorCount();
        model(r).setValueAt("4", 0, 2);
        assertTrue(r.applyProperties());
        assertEquals(4, appliedTo(r));
        assertTrue(xml(r), xml(r).contains("rangeTo=\"4\""));
        assertTrue(output(r).getFactorCount() > countBefore);
        WallpaperRubette loaded = reload(r);
        assertEquals(4, loaded.getMorphismsTable().getRangeTo(first(loaded)));
    }

    public void testAnApplyThenAViewWithAnUnappliedEditKeepsTheApplied() throws Exception {
        WallpaperRubette r = wallpaper();
        r.getProperties();
        model(r).setValueAt("4", 0, 2);
        assertTrue(r.applyProperties());
        model(r).setValueAt("7", 0, 2);
        r.getView();
        assertEquals(4, appliedTo(r));
    }

    public void testRevertAfterOpeningTheViewRestoresTheAppliedRange() throws Exception {
        WallpaperRubette r = wallpaper();
        r.getProperties();
        model(r).setValueAt("4", 0, 2);
        r.getView();
        r.revertProperties();
        assertEquals("2", model(r).getValueAt(0, 2));
        assertEquals(2, appliedTo(r));
    }

    public void testDuplicateAfterOpeningTheViewCopiesTheAppliedSettings() throws Exception {
        WallpaperRubette r = wallpaper();
        model(r).setValueAt("4", 0, 2);
        model(r).setValueAt(paths(1, 1), 0, 3);
        r.getView();
        WallpaperRubette copy = (WallpaperRubette) r.duplicate();
        JWallpaperDimensionsTable table = copy.getMorphismsTable();
        assertEquals(1, table.getMorphisms().size());
        assertEquals(2, table.getRangeTo(table.getMorphisms().get(0)));
        assertEquals(paths(0, 0), table.getCoordinates(table.getMorphisms().get(0)));
    }

    public void testReloadAfterOpeningTheViewKeepsTheAppliedSettings() throws Exception {
        WallpaperRubette r = wallpaper();
        model(r).setValueAt("4", 0, 2);
        r.getView();
        WallpaperRubette loaded = reload(r);
        JWallpaperDimensionsTable table = loaded.getMorphismsTable();
        assertEquals(0, table.getRangeFrom(first(loaded)));
        assertEquals(2, table.getRangeTo(first(loaded)));
    }
}
