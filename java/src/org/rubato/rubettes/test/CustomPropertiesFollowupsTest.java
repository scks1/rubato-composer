package org.rubato.rubettes.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.base.Rubette;
import org.rubato.math.module.RRing;
import org.rubato.math.module.morphism.IdentityMorphism;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.RAffineMorphism;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.rubettes.score.ScoreToCsoundRubette;
import org.rubato.rubettes.wallpaper.JWallpaperDimensionsTable;
import org.rubato.rubettes.wallpaper.WallpaperRubette;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Covers three bugs found while fixing the custom property windows on 2026-11-25:
 * ScoreToCsound's Revert with no score file, Wallpaper's save while its table holds unapplied
 * rows, and the image rubettes' forms when their classes load before BigBang registers them.
 */
public class CustomPropertiesFollowupsTest extends TestCase {

    // ---------- reflection and XML helpers ----------

    private static Field field(Object o, String name) throws Exception {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            }
            catch (NoSuchFieldException e) {
                // look in the superclass
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object get(Object o, String name) throws Exception {
        return field(o, name).get(o);
    }

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

    private static Form form(String name) {
        return Repository.systemRepository().getForm(name);
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
    private static Rubette reload(Rubette r) throws Exception {
        String s = xml(r);
        Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(s))).getDocumentElement();
        XMLReader reader = new XMLReader(new StringReader("<Rubato></Rubato>"));
        reader.parse();
        Rubette loaded = r.newInstance().fromXML(reader, element);
        assertFalse(reader.getErrors() + " in " + s, reader.hasError());
        assertNotNull("fromXML returned null for " + s, loaded);
        return loaded;
    }

    // ---------- bug 1: ScoreToCsound Revert with no score file ----------

    private static JTextField textField(Object r, String name) throws Exception {
        return (JTextField) get(r, name);
    }

    public void testBug1ScoreToCsoundRevertWithNoFileRestoresTheFields() throws Exception {
        ScoreToCsoundRubette r = new ScoreToCsoundRubette();
        r.getProperties();
        double timeScale = (Double) get(r, "timeScale");
        double a4freq = (Double) get(r, "a4freq");
        String prolog = (String) get(r, "prolog");
        textField(r, "timeScaleField").setText("9.0");
        textField(r, "freqField").setText("7.0");
        ((JTextArea) get(r, "prologTextArea")).setText("edited");
        r.revertProperties();
        assertEquals("no file to show", "", textField(r, "fileNameField").getText());
        assertEquals(Double.toString(timeScale), textField(r, "timeScaleField").getText());
        assertEquals(Double.toString(a4freq), textField(r, "freqField").getText());
        assertEquals(prolog, ((JTextArea) get(r, "prologTextArea")).getText());
        assertTrue(r.applyProperties());
        assertEquals(timeScale, (Double) get(r, "timeScale"), 0.0);
        assertEquals(a4freq, (Double) get(r, "a4freq"), 0.0);
    }

    public void testBug1ScoreToCsoundRevertWithAFileShowsItsName() throws Exception {
        ScoreToCsoundRubette r = new ScoreToCsoundRubette();
        File sco = new File(System.getProperty("java.io.tmpdir"), "custom-properties-followups.sco");
        call(r, "setScoFile", sco.getPath());
        r.getProperties();
        textField(r, "fileNameField").setText("something else");
        r.revertProperties();
        assertEquals(sco.getCanonicalPath(), textField(r, "fileNameField").getText());
    }

    // ---------- bug 2: Wallpaper save with unapplied table rows ----------

    private static List<List<Integer>> paths(int... coordinates) {
        List<List<Integer>> p = new ArrayList<List<Integer>>();
        p.add(new ArrayList<Integer>());
        for (int c : coordinates) {
            p.get(0).add(c);
        }
        return p;
    }

    /** A Wallpaper on Score with the given number of applied morphisms (1 or 2). */
    private static WallpaperRubette wallpaper(int morphisms) throws Exception {
        WallpaperRubette r = (WallpaperRubette) new WallpaperRubette().newInstance();
        call(r, "setInputForm", (PowerForm) form("Score"));
        call(r, "addMorphism", new IdentityMorphism(RRing.ring), 0, 2, paths(0, 0));
        if (morphisms > 1) {
            call(r, "addMorphism", new RAffineMorphism(2, 1), 1, 3, paths(0, 0));
        }
        return r;
    }

    private static List<ModuleMorphism> applied(Rubette r) throws Exception {
        return ((WallpaperRubette) r).getMorphismsTable().getMorphisms();
    }

    public void testBug2WallpaperSaveAfterAnAddedEmptyRowWritesTheAppliedMorphisms() throws Exception {
        WallpaperRubette r = wallpaper(1);
        String before = xml(r);
        r.getMorphismsTable().addMorphism();
        assertEquals(before, xml(r));
        Rubette loaded = reload(r);
        assertEquals(1, applied(loaded).size());
        JWallpaperDimensionsTable table = ((WallpaperRubette) loaded).getMorphismsTable();
        assertEquals(0, table.getRangeFrom(applied(loaded).get(0)));
        assertEquals(2, table.getRangeTo(applied(loaded).get(0)));
    }

    public void testBug2WallpaperSaveAfterAnAddedFilledRowWritesTheAppliedMorphisms() throws Exception {
        WallpaperRubette r = wallpaper(1);
        String before = xml(r);
        r.getMorphismsTable().addMorphism(new RAffineMorphism(3, 4), 1, 5, paths(0, 0));
        assertEquals(before, xml(r));
        assertEquals(1, applied(reload(r)).size());
    }

    public void testBug2WallpaperSaveAfterARemovedRowWritesTheAppliedMorphisms() throws Exception {
        WallpaperRubette r = wallpaper(2);
        String before = xml(r);
        ((DefaultTableModel) r.getMorphismsTable().getModel()).removeRow(1);
        assertEquals(before, xml(r));
        Rubette loaded = reload(r);
        assertEquals(2, applied(loaded).size());
        JWallpaperDimensionsTable table = ((WallpaperRubette) loaded).getMorphismsTable();
        assertEquals(1, table.getRangeFrom(applied(loaded).get(1)));
        assertEquals(3, table.getRangeTo(applied(loaded).get(1)));
    }

    // ---------- bug 3: image rubettes loaded before BigBang's forms exist ----------

    /** The test class path, read again by a class loader that shares no classes with this one. */
    private static URL[] testClassPath() throws Exception {
        String cp = System.getProperty("surefire.test.class.path");
        if (cp == null || cp.isEmpty()) {
            cp = System.getProperty("java.class.path");
        }
        List<URL> urls = new ArrayList<URL>();
        for (String entry : cp.split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                urls.add(new File(entry).toURI().toURL());
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }

    public void testBug3ImageRubettesLoadedBeforeTheirFormsRun() throws Exception {
        File png = File.createTempFile("custom-properties-followups-in", ".png");
        File out = File.createTempFile("custom-properties-followups-out", ".png");
        out.delete();
        URLClassLoader fresh = new URLClassLoader(testClassPath(), ClassLoader.getPlatformClassLoader());
        try {
            BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0xffff0000);
            image.setRGB(2, 1, 0xff0000ff);
            ImageIO.write(image, "png", png);

            Class<?> repository = Class.forName("org.rubato.base.Repository", true, fresh);
            assertNotSame("the class loader shares no classes", Repository.class, repository);
            Object system = repository.getMethod("systemRepository").invoke(null);
            Method getForm = repository.getMethod("getForm", String.class);
            for (String name : Arrays.asList("Pixel", "Image", "VariableSizePixel", "VSPixelImage")) {
                assertNull(name + " is not registered yet", getForm.invoke(system, name));
            }
            // the classes load, and run their static initializers, before the forms exist
            Class.forName("org.rubato.rubettes.image.ImageFileInRubette", true, fresh);
            Class.forName("org.rubato.rubettes.image.ImageFileOutRubette", true, fresh);

            Class<?> check = Class.forName(ImageFormsLoadOrderCheck.class.getName(), true, fresh);
            Object result = check.getMethod("registerThenRun", String.class, String.class)
                    .invoke(null, png.getPath(), out.getPath());
            assertEquals("OK", result);
            BufferedImage written = ImageIO.read(out);
            assertEquals(3, written.getWidth());
            assertEquals(2, written.getHeight());
        }
        finally {
            fresh.close();
            png.delete();
            out.delete();
        }
    }
}
