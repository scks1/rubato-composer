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

package org.rubato.rubettes.test;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.FileProperty;
import org.rubato.base.Repository;
import org.rubato.base.Rubette;
import org.rubato.base.RubetteProperty;
import org.rubato.base.SimpleAbstractRubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.rubette.Link;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.math.module.RElement;
import org.rubato.math.module.RRing;
import org.rubato.math.module.morphism.IdentityMorphism;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.rubettes.alteration.AlterationRubette;
import org.rubato.rubettes.builtin.BooleanRubette;
import org.rubato.rubettes.builtin.ConstructorRubette;
import org.rubato.rubettes.builtin.MacroInputRubette;
import org.rubato.rubettes.builtin.MacroOutputRubette;
import org.rubato.rubettes.builtin.RealArithRubette;
import org.rubato.rubettes.builtin.SchemeRubette;
import org.rubato.rubettes.builtin.SplitRubette;
import org.rubato.rubettes.builtin.address.AddressEvalRubette;
import org.rubato.rubettes.image.ImageFileInRubette;
import org.rubato.rubettes.image.ImageFileOutRubette;
import org.rubato.rubettes.score.LilyPondOutRubette;
import org.rubato.rubettes.score.MidiFileInRubette;
import org.rubato.rubettes.score.ScaleRubette;
import org.rubato.rubettes.score.ScoreToCsoundRubette;
import org.rubato.rubettes.util.CoolFormRegistrant;
import org.rubato.rubettes.wallpaper.WallpaperRubette;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Rubettes that build their own properties window keep their settings through opening the
 * window, Apply with no edit, Revert, duplicate (Ctrl+D) and a save followed by a reload.
 * Each test names the bug it covers in the custom property window findings of 2026-11-25.
 * The tests read and set the rubettes' private fields by reflection, as the window code does
 * internally, and call the same methods the window's buttons and the network loader call.
 */
public class CustomPropertyWindowsTest extends TestCase {

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

    private static void set(Object o, String name, Object value) throws Exception {
        field(o, name).set(o, value);
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

    /** The rubette's own XML, as a network save writes it inside the rubette's element. */
    private static String xml(Rubette r) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        XMLWriter writer = new XMLWriter(new PrintStream(bytes, true, "UTF-8"));
        writer.open();
        r.toXML(writer);
        writer.close();
        return bytes.toString("UTF-8");
    }

    /**
     * Saves the rubette and reads it back with fromXML, as a network reload does: NetworkModel
     * calls fromXML on the rubette manager's unconfigured prototype, not on the saved rubette.
     */
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

    private static final RunInfo RUN_INFO = new RunInfo() {
        public boolean stopped() { return false; }
        public void addMessage(RubetteModel rubette, String msg) { }
    };

    /** Runs the rubette in a model of its own, with the given denotators at its inputs. */
    private static void run(Rubette r, Denotator... inputs) {
        RubetteModel model = new RubetteModel(r, r.getName());
        for (int i = 0; i < inputs.length; i++) {
            RealArithRubette source = new RealArithRubette();
            source.setOutput(0, inputs[i]);
            RubetteModel sourceModel = new RubetteModel(source, "source" + i);
            model.setInLink(new Link(sourceModel, 0, model, i));
        }
        r.clearErrors();
        r.run(RUN_INFO);
    }

    private static Denotator real(double d) throws Exception {
        return new SimpleDenotator(NameDenotator.make(""), (SimpleForm) form("Real"), new RElement(d));
    }

    private static String[] texts(Object fields) {
        JTextField[] f = (JTextField[]) fields;
        String[] t = new String[f.length];
        for (int i = 0; i < f.length; i++) {
            t[i] = f[i].getText();
        }
        return t;
    }

    // ---------- bug 1: Macro labels ----------

    private void checkMacroLabelsSurviveTheWindow(Rubette r, String countSetter, String fieldsName) throws Exception {
        call(r, countSetter, 2);
        set(r, "labels", new String[] { "left", "right" });
        set(r, "values", new Denotator[2]);
        r.getProperties();
        assertEquals("opening the window", Arrays.asList("left", "right"), Arrays.asList((String[]) get(r, "labels")));
        assertEquals("the fields show the labels", Arrays.asList("left", "right"), Arrays.asList(texts(get(r, fieldsName))));
        assertTrue(r.applyProperties());
        assertEquals("Apply with no edit", Arrays.asList("left", "right"), Arrays.asList((String[]) get(r, "labels")));
        assertTrue(xml(r).contains("\"left\"") && xml(r).contains("\"right\""));
    }

    public void testBug01MacroInputOpeningTheWindowKeepsTheLabels() throws Exception {
        checkMacroLabelsSurviveTheWindow(new MacroInputRubette(), "setOutCount", "outTextFields");
    }

    public void testBug01MacroOutputOpeningTheWindowKeepsTheLabels() throws Exception {
        checkMacroLabelsSurviveTheWindow(new MacroOutputRubette(), "setInCount", "inTextFields");
    }

    public void testBug01MacroDuplicateAndReloadThenApplyKeepTheLabels() throws Exception {
        MacroInputRubette in = new MacroInputRubette();
        in.setOutCount(2);
        set(in, "labels", new String[] { "left", "right" });
        set(in, "values", new Denotator[2]);
        for (Rubette r : new Rubette[] { in.duplicate(), reload(in) }) {
            r.getProperties();
            assertTrue(r.applyProperties());
            assertEquals(Arrays.asList("left", "right"), Arrays.asList((String[]) get(r, "labels")));
        }
    }

    /** Moving the slider keeps the text of the fields that remain; new fields start empty. */
    public void testBug01MacroSliderKeepsTheRemainingLabels() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("CustomPropertyWindowsTest: slider test skipped, no display");
            return;
        }
        MacroInputRubette r = new MacroInputRubette();
        r.setOutCount(2);
        set(r, "labels", new String[] { "left", "right" });
        set(r, "values", new Denotator[2]);
        JComponent panel = r.getProperties();
        JDialog dialog = new JDialog();
        try {
            dialog.add(panel);
            ((JTextField[]) get(r, "outTextFields"))[0].setText("typed");
            JSlider slider = (JSlider) get(get(r, "outSlider"), "outSlider");
            slider.setValue(3);
            assertEquals(Arrays.asList("typed", "right", ""), Arrays.asList(texts(get(r, "outTextFields"))));
            assertEquals("moving the slider changes no label before Apply",
                    Arrays.asList("left", "right"), Arrays.asList((String[]) get(r, "labels")));
            slider.setValue(1);
            assertEquals(Arrays.asList("typed"), Arrays.asList(texts(get(r, "outTextFields"))));
            slider.setValue(2);
            assertEquals(Arrays.asList("typed", ""), Arrays.asList(texts(get(r, "outTextFields"))));
            assertTrue(r.applyProperties());
            assertEquals(Arrays.asList("typed", ""), Arrays.asList((String[]) get(r, "labels")));
        }
        finally {
            dialog.dispose();
        }
    }

    // ---------- bug 2: Boolean save ----------

    public void testBug02BooleanReloadKeepsTheExpression() throws Exception {
        BooleanRubette r = new BooleanRubette();
        r.setInCount(3);
        assertTrue((Boolean) call(get(r, "expression"), "parse", "#0 & #2", 3));
        set(r, "expressionString", "#0 & #2");
        set(r, "values", new boolean[3]);
        Rubette loaded = reload(r);
        assertEquals("#0 & #2", get(loaded, "expressionString"));
        assertEquals(3, loaded.getInCount());
        assertTrue("the expression compiles with the loaded input count",
                (Boolean) call(get(loaded, "expression"), "hasCode"));
        run(loaded);
        assertFalse(loaded.getErrors().toString(), loaded.hasErrors());
        assertNotNull(loaded.getOutput(0));
    }

    // ---------- bug 3: Scheme input count ----------

    public void testBug03SchemeReloadKeepsTheInputCount() throws Exception {
        SchemeRubette r = new SchemeRubette();
        r.setInCount(2);
        r.setOutCount(3);
        set(r, "schemeCode", "(define x 1)");
        Rubette loaded = reload(r);
        assertEquals(2, loaded.getInCount());
        assertEquals(3, loaded.getOutCount());
    }

    // ---------- bug 4: ScoreToCsound Revert ----------

    public void testBug04ScoreToCsoundRevertKeepsTheFrequency() throws Exception {
        ScoreToCsoundRubette r = new ScoreToCsoundRubette();
        File sco = File.createTempFile("custom-properties", ".sco");
        sco.delete();
        call(r, "setScoFile", sco.getPath());
        set(r, "timeScale", 1.5);
        set(r, "a4freq", 442.0);
        r.getProperties();
        r.revertProperties();
        assertTrue(r.applyProperties());
        assertEquals(442.0, (Double) get(r, "a4freq"), 0.0);
        assertEquals(1.5, (Double) get(r, "timeScale"), 0.0);
    }

    // ---------- bug 5: Scale reload ----------

    private static double[] preset(ScaleRubette r, int notes, int index) throws Exception {
        Object[] names = r.getScaleMap().getScaleNames(notes);
        return r.getScaleMap().get(names[index].toString());
    }

    private static ScaleRubette dorianOnD() throws Exception {
        ScaleRubette r = new ScaleRubette();
        set(r, "numberOfNotes", 7);
        set(r, "preset", 2);
        set(r, "root", 62.0);
        set(r, "intervals", preset(r, 7, 2));
        return r;
    }

    public void testBug05ScaleReloadUsesThePresetsIntervals() throws Exception {
        ScaleRubette r = dorianOnD();
        double[] dorian = preset(r, 7, 2);
        assertFalse("preset 2 differs from preset 1", Arrays.equals(dorian, preset(r, 7, 1)));
        Rubette loaded = reload(r);
        assertTrue(Arrays.toString((double[]) get(loaded, "intervals")),
                Arrays.equals(dorian, (double[]) get(loaded, "intervals")));
        assertEquals(2, get(loaded, "preset"));
        assertEquals(62.0, (Double) get(loaded, "root"), 0.0);
    }

    // ---------- bug 6: RealArith reload ----------

    public void testBug06RealArithRunsAfterAReloadWithTwoInputs() throws Exception {
        RealArithRubette r = new RealArithRubette();
        r.setInCount(2);
        assertTrue((Boolean) call(r, "compile", "#0+#1", 2));
        set(r, "expressionString", "#0+#1");
        set(r, "isResultReal", true);
        set(r, "values", new double[2]);
        Rubette loaded = reload(r);
        assertEquals(2, ((double[]) get(loaded, "values")).length);
        run(loaded, real(1.5), real(2.0));
        assertFalse(loaded.getErrors().toString(), loaded.hasErrors());
        assertEquals(3.5, ((SimpleDenotator) loaded.getOutput(0)).getReal(), 1e-12);
    }

    // ---------- bug 7: a rubette with no form reloads ----------

    public void testBug07UnsetConstructorSplitAndAlterationReload() throws Exception {
        for (Rubette r : new Rubette[] { new ConstructorRubette(), new SplitRubette(), new AlterationRubette() }) {
            Rubette loaded = reload(r);
            assertEquals(r.getClass(), loaded.getClass());
        }
        assertNull(((ConstructorRubette) reload(new ConstructorRubette())).getForm());
        assertNull(get(reload(new SplitRubette()), "form"));
        assertNull(get(reload(new AlterationRubette()), "inputForm"));
    }

    public void testBug07ConfiguredConstructorSplitAndAlterationStillReload() throws Exception {
        ConstructorRubette c = new ConstructorRubette();
        call(c, "setForm", form("Note"));
        assertEquals(form("Note"), ((ConstructorRubette) reload(c)).getForm());
        SplitRubette s = new SplitRubette();
        call(s, "set", (LimitForm) form("Note"), new int[] { 0, 2 });
        assertEquals(xml(s), xml(reload(s)));
        AlterationRubette a = new AlterationRubette();
        call(a, "setInputForm", (PowerForm) form("Score"));
        assertEquals(form("Score"), get(reload(a), "inputForm"));
    }

    // ---------- bug 8: AddressEval duplicate ----------

    public void testBug08AddressEvalDuplicateKeepsTheMorphismAndSaves() throws Exception {
        AddressEvalRubette r = new AddressEvalRubette();
        set(r, "evalType", 3);
        IdentityMorphism m = new IdentityMorphism(RRing.ring);
        set(r, "morphism", m);
        Rubette copy = r.duplicate();
        assertSame(m, get(copy, "morphism"));
        assertEquals("the copy saves like the original", xml(r), xml(copy));
    }

    public void testBug08AddressEvalDuplicateKeepsTheInputCount() throws Exception {
        AddressEvalRubette r = new AddressEvalRubette();
        set(r, "evalType", 4);
        r.setInCount(2);
        assertEquals(2, r.duplicate().getInCount());
    }

    // ---------- bug 9: Scale, Alteration and Wallpaper duplicates ----------

    public void testBug09ScaleDuplicateKeepsItsSettings() throws Exception {
        ScaleRubette r = dorianOnD();
        Rubette copy = r.duplicate();
        assertEquals(7, get(copy, "numberOfNotes"));
        assertEquals(2, get(copy, "preset"));
        assertEquals(62.0, (Double) get(copy, "root"), 0.0);
        assertTrue(Arrays.equals(preset(r, 7, 2), (double[]) get(copy, "intervals")));
        assertNotSame("the copy has its own intervals", get(r, "intervals"), get(copy, "intervals"));
        copy.getProperties();
        assertTrue(copy.applyProperties());
        assertEquals("Apply with no edit on the copy", xml(r), xml(copy));
    }

    public void testBug09AlterationDuplicateKeepsItsSettings() throws Exception {
        AlterationRubette r = new AlterationRubette();
        call(r, "setInputForm", (PowerForm) form("Score"));
        call(r, "setGlobal", true);
        set(r, "globalStartDegree", 0.25);
        set(r, "globalEndDegree", 0.75);
        Rubette copy = r.duplicate();
        assertEquals(form("Score"), get(copy, "inputForm"));
        assertEquals(true, get(copy, "global"));
        assertEquals(0.25, (Double) get(copy, "globalStartDegree"), 0.0);
        assertEquals(0.75, (Double) get(copy, "globalEndDegree"), 0.0);
        assertEquals(xml(r), xml(copy));
    }

    public void testBug09AlterationDuplicateKeepsItsDimensions() throws Exception {
        AlterationRubette r = new AlterationRubette();
        call(r, "setInputForm", (PowerForm) form("Score"));
        Object table = get(r, "dimensionsTable");
        call(table, "addDimension", (SimpleForm) form("Onset"), 0.2, 0.8, (SimpleForm) form("Pitch"));
        assertTrue((Boolean) call(table, "applyChanges"));
        assertEquals(1, call(table, "dimensionCount"));
        Rubette copy = r.duplicate();
        assertEquals(1, call(get(copy, "dimensionsTable"), "dimensionCount"));
        assertEquals(xml(r), xml(copy));
        assertTrue(xml(copy).contains("Dimension"));
    }

    public void testBug09WallpaperDuplicateKeepsItsMorphisms() throws Exception {
        WallpaperRubette r = (WallpaperRubette) new WallpaperRubette().newInstance();
        call(r, "setInputForm", (PowerForm) form("Score"));
        List<List<Integer>> paths = new ArrayList<List<Integer>>();
        paths.add(new ArrayList<Integer>(Arrays.asList(0, 0)));
        call(r, "addMorphism", new IdentityMorphism(RRing.ring), 0, 2, paths);
        Rubette copy = r.duplicate();
        assertEquals(form("Score"), get(copy, "inputForm"));
        assertEquals(xml(r), xml(copy));
        assertTrue(xml(copy).contains("Morphism"));
    }

    public void testBug09UnsetDuplicatesStayUnset() throws Exception {
        assertEquals(xml(new AlterationRubette()), xml(new AlterationRubette().duplicate()));
        Rubette w = new WallpaperRubette().newInstance();
        assertEquals(xml(w), xml(w.duplicate()));
        ScaleRubette s = new ScaleRubette();
        assertEquals(xml(s), xml(s.duplicate()));
    }

    // ---------- bug 10: MidiFileIn duplicate ----------

    public void testBug10MidiFileInDuplicateKeepsTempoStoreAndScore() throws Exception {
        MidiFileInRubette r = new MidiFileInRubette();
        r.setMidiFile("probe-in.mid");
        r.setTempoFactor(2.0);
        r.setStoreDenotator(false);
        Denotator score = new PowerDenotator(NameDenotator.make("s"), (PowerForm) form("Score"), new ArrayList<Denotator>());
        r.setScore(score);
        MidiFileInRubette copy = (MidiFileInRubette) r.duplicate();
        assertEquals(2.0, copy.getTempoFactor(), 0.0);
        assertEquals(false, get(copy, "storeDenotator"));
        assertSame(score, copy.getScore());
        assertEquals(get(r, "midiFile"), get(copy, "midiFile"));
    }

    // ---------- bug 11: file rubettes after a duplicate ----------

    private static void setFile(SimpleAbstractRubette r, File f) {
        for (RubetteProperty p : r.getRubetteProperties().getProperties()) {
            if (p instanceof FileProperty) {
                ((FileProperty) p).setFile(f);
            }
        }
        // the window's Apply, which the original had before it was duplicated
        r.applyProperties();
    }

    public void testBug11DuplicatedLilyPondOutWritesItsFile() throws Exception {
        File ly = File.createTempFile("custom-properties", ".ly");
        ly.delete();
        try {
            LilyPondOutRubette r = new LilyPondOutRubette();
            setFile(r, ly);
            Rubette copy = r.duplicate();
            Denotator score = new PowerDenotator(NameDenotator.make(""), (PowerForm) form("Score"), new ArrayList<Denotator>());
            run(copy, score);
            assertFalse(copy.getErrors().toString(), copy.hasErrors());
            assertTrue("the copy wrote " + ly, ly.exists());
            assertEquals("a new copy's info names the file", ly.getName(), r.duplicate().getInfo());
        }
        finally {
            ly.delete();
        }
    }

    /**
     * The image rubettes read their forms into static fields when their classes load, and the
     * forms exist only after BigBang's CoolFormRegistrant has run. The app loads BigBang first;
     * here the forms are registered and any null static field is filled again.
     */
    private static void registerImageForms() throws Exception {
        new CoolFormRegistrant().registerAllTheCoolStuff();
        String[][] statics = {
            { "org.rubato.rubettes.image.ImageFileInRubette", "PIXEL_FORM", "Pixel" },
            { "org.rubato.rubettes.image.ImageFileInRubette", "IMAGE_FORM", "Image" },
            { "org.rubato.rubettes.image.ImageFileInRubette", "VS_PIXEL_FORM", "VariableSizePixel" },
            { "org.rubato.rubettes.image.ImageFileInRubette", "VS_IMAGE_FORM", "VSPixelImage" },
            { "org.rubato.rubettes.image.ImageFileOutRubette", "IMAGE_FORM", "Image" },
            { "org.rubato.rubettes.image.ImageFileOutRubette", "VS_IMAGE_FORM", "VSPixelImage" },
        };
        for (String[] s : statics) {
            assertNotNull(s[2], form(s[2]));
            Field f = Class.forName(s[0]).getDeclaredField(s[1]);
            f.setAccessible(true);
            if (f.get(null) == null) {
                f.set(null, form(s[2]));
            }
        }
    }

    public void testBug11DuplicatedImageFileInAndOutReadAndWriteTheirFiles() throws Exception {
        registerImageForms();
        File png = File.createTempFile("custom-properties-in", ".png");
        File out = File.createTempFile("custom-properties-out", ".png");
        out.delete();
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0xffff0000);
            image.setRGB(1, 1, 0xff0000ff);
            ImageIO.write(image, "png", png);
            ImageFileInRubette in = new ImageFileInRubette();
            setFile(in, png);
            Rubette inCopy = in.duplicate();
            run(inCopy);
            assertFalse(inCopy.getErrors().toString(), inCopy.hasErrors());
            assertNotNull(inCopy.getOutput(0));
            assertEquals("a new copy's info names the file", png.getName(), in.duplicate().getInfo());
            ImageFileOutRubette o = new ImageFileOutRubette();
            setFile(o, out);
            Rubette outCopy = o.duplicate();
            run(outCopy, inCopy.getOutput(0));
            assertFalse(outCopy.getErrors().toString(), outCopy.hasErrors());
            assertTrue("the copy wrote " + out, out.exists());
            assertEquals("a new copy's info names the file", out.getName(), o.duplicate().getInfo());
        }
        finally {
            png.delete();
            out.delete();
        }
    }
}
