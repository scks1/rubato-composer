package org.rubato.rubettes.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.base.Rubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.rubette.Link;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.rubettes.alteration.AlterationRubette;
import org.rubato.rubettes.builtin.MacroOutputRubette;
import org.rubato.rubettes.builtin.RealArithRubette;
import org.rubato.rubettes.score.QuantizeRubette;
import org.rubato.rubettes.score.RhythmizeRubette;
import org.rubato.rubettes.texture.TexturalizeRubette;
import org.rubato.rubettes.util.CoolFormRegistrant;
import org.rubato.rubettes.wallpaper.WallpaperRubette;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Checks that rubettes which threw unchecked exceptions on reachable input (an unconnected
 * input, an input of the wrong form, or a fresh MacroOutput) now report a rubette error or
 * run normally, and that a valid input still gives an output.
 */
public class RubetteRunErrorsTest extends TestCase {

    private static final RunInfo RUN_INFO = new RunInfo() {
        public boolean stopped() { return false; }
        public void addMessage(RubetteModel rubette, String msg) { }
    };

    protected void setUp() {
        //Texture and RepeatedNote exist only after BigBang's registrant has run, as at a real launch
        new CoolFormRegistrant().registerAllTheCoolStuff();
    }

    /** A rubette as the program makes it, from its prototype's newInstance. */
    @SuppressWarnings("unchecked")
    private static <T extends Rubette> T fresh(T prototype) {
        return (T) prototype.newInstance();
    }

    /** Links each given denotator to the next input; null leaves that input unconnected. */
    private static void connect(Rubette r, Denotator... inputs) {
        RubetteModel model = new RubetteModel(r, r.getName());
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] != null) {
                RealArithRubette source = new RealArithRubette();
                source.setOutput(0, inputs[i]);
                model.setInLink(new Link(new RubetteModel(source, "source" + i), 0, model, i));
            }
        }
    }

    /** Runs the rubette and fails if run() throws. */
    private static List<String> run(Rubette r, Denotator... inputs) {
        connect(r, inputs);
        r.clearErrors();
        try {
            r.run(RUN_INFO);
        } catch (RuntimeException e) {
            StackTraceElement at = null;
            for (StackTraceElement f : e.getStackTrace()) {
                if (f.getClassName().startsWith("org.rubato")) {
                    at = f;
                    break;
                }
            }
            fail(r.getName() + ".run threw " + e + " at " + at);
        }
        return new ArrayList<String>(r.getErrors());
    }

    private static Denotator onset() {
        return Repository.systemRepository().getForm("Onset").createDefaultDenotator();
    }

    private static PowerForm scoreForm() {
        return (PowerForm) Repository.systemRepository().getForm("Score");
    }

    private static PowerDenotator score() throws Exception {
        LimitForm noteForm = (LimitForm) Repository.systemRepository().getForm("Note");
        List<Denotator> notes = new ArrayList<Denotator>();
        notes.add(noteForm.createDefaultDenotator());
        return new PowerDenotator(NameDenotator.make(""), scoreForm(), notes);
    }

    private static void setInputForm(Rubette r, PowerForm form) throws Exception {
        Method m = r.getClass().getDeclaredMethod("setInputForm", PowerForm.class);
        m.setAccessible(true);
        m.invoke(r, form);
    }

    private static List<String> list(String... s) {
        List<String> l = new ArrayList<String>();
        for (String x : s) {
            l.add(x);
        }
        return l;
    }

    // ---------- MacroOutput ----------

    public void testAFreshMacroOutputRunsAndPassesItsInputOn() throws Exception {
        MacroOutputRubette r = fresh(new MacroOutputRubette());
        assertEquals(list(), run(r));
        assertNull(r.getValue(0));
        MacroOutputRubette r2 = fresh(new MacroOutputRubette());
        PowerDenotator s = score();
        assertEquals(list(), run(r2, s));
        assertSame(s, r2.getValue(0));
    }

    public void testAFreshMacroOutputSavesAndReloadsAndRuns() throws Exception {
        MacroOutputRubette r = fresh(new MacroOutputRubette());
        MacroOutputRubette loaded = (MacroOutputRubette) reload(r);
        assertEquals(1, loaded.getInCount());
        PowerDenotator s = score();
        assertEquals(list(), run(loaded, s));
        assertSame(s, loaded.getValue(0));
        MacroOutputRubette dup = (MacroOutputRubette) r.duplicate();
        assertEquals(list(), run(dup, s));
        assertSame(s, dup.getValue(0));
    }

    // ---------- Texturalize ----------

    public void testTexturalizeReportsAnUnconnectedInput() {
        assertEquals(list("Input denotator is null."), run(fresh(new TexturalizeRubette())));
    }

    public void testTexturalizeReportsAnInputThatIsNotAScore() {
        assertEquals(list("Input denotator is not of form \"Score\"."), run(fresh(new TexturalizeRubette()), onset()));
    }

    public void testTexturalizeStillMakesATextureFromAScore() throws Exception {
        TexturalizeRubette r = fresh(new TexturalizeRubette());
        assertEquals(list(), run(r, score()));
        assertNotNull(r.getOutput(0));
        assertEquals("Texture", r.getOutput(0).getForm().getNameString());
        assertEquals(1, ((PowerDenotator) r.getOutput(0)).getFactorCount());
    }

    // ---------- Rhythmize ----------

    public void testRhythmizeReportsAnUnconnectedInput() {
        assertEquals(list("Input denotator is null."), run(fresh(new RhythmizeRubette())));
    }

    public void testRhythmizeReportsAnInputThatIsNotAScore() {
        assertEquals(list("Input denotator is not of form \"Score\"."), run(fresh(new RhythmizeRubette()), onset()));
    }

    public void testRhythmizeStillRhythmizesAScoreWithoutChangingItsInput() throws Exception {
        RhythmizeRubette r = fresh(new RhythmizeRubette());
        PowerDenotator s = score();
        String before = s.toString();
        assertEquals(list(), run(r, s));
        assertNotNull(r.getOutput(0));
        assertNotSame(s, r.getOutput(0));
        assertEquals(1, ((PowerDenotator) r.getOutput(0)).getFactorCount());
        assertEquals("the input score is copied, not changed", before, s.toString());
    }

    // ---------- Quantize ----------

    public void testQuantizeReportsAnUnconnectedInput() {
        assertEquals(list("Input denotator is null."), run(fresh(new QuantizeRubette())));
    }

    public void testQuantizeStillReportsAnInputThatIsNotAScore() {
        assertEquals(list("Input denotator is not of form \"Score\"."), run(fresh(new QuantizeRubette()), onset()));
    }

    public void testQuantizeStillQuantizesAScore() throws Exception {
        QuantizeRubette r = fresh(new QuantizeRubette());
        assertEquals(list(), run(r, score()));
        assertNotNull(r.getOutput(0));
    }

    // ---------- Wallpaper ----------

    public void testAFreshWallpaperReportsItsMissingFormForAnyInput() {
        assertEquals(list("Input form not set."), run(fresh(new WallpaperRubette()), onset()));
    }

    public void testWallpaperReportsAnInputThatIsNotAPower() throws Exception {
        WallpaperRubette r = fresh(new WallpaperRubette());
        setInputForm(r, scoreForm());
        assertEquals(list("Input denotator is not of type Power."), run(r, onset()));
    }

    public void testWallpaperStillReportsAnUnconnectedInputAndRunsOnAScoreWithAMorphism() throws Exception {
        WallpaperRubette r = fresh(new WallpaperRubette());
        setInputForm(r, scoreForm());
        assertEquals(list("Input denotator not set."), run(r));
        Method add = WallpaperRubette.class.getDeclaredMethod("addMorphism",
                org.rubato.math.module.morphism.ModuleMorphism.class, int.class, int.class, List.class);
        add.setAccessible(true);
        List<List<Integer>> paths = new ArrayList<List<Integer>>();
        for (int c : new int[] { 0, 0 }) {
            List<Integer> path = new ArrayList<Integer>();
            path.add(c);
            paths.add(path);
        }
        add.invoke(r, new org.rubato.math.module.morphism.RAffineMorphism(2, 1), 0, 2, paths);
        assertEquals(list(), run(r, score()));
        assertEquals(3, ((PowerDenotator) r.getOutput(0)).getFactorCount());
    }

    // ---------- Alteration ----------

    public void testAFreshAlterationReportsItsMissingFormForAnyInput() {
        assertEquals(list("Input form not set."), run(fresh(new AlterationRubette()), onset(), onset()));
    }

    public void testAlterationReportsAFirstInputThatIsNotAPower() throws Exception {
        AlterationRubette r = fresh(new AlterationRubette());
        setInputForm(r, scoreForm());
        assertEquals(list("Input #0 is of the wrong form."), run(r, onset(), score()));
    }

    public void testAlterationReportsASecondInputThatIsNotAPower() throws Exception {
        AlterationRubette r = fresh(new AlterationRubette());
        setInputForm(r, scoreForm());
        assertEquals(list("Input #1 is of the wrong form."), run(r, score(), onset()));
    }

    public void testAlterationStillReportsNullInputs() throws Exception {
        AlterationRubette r = fresh(new AlterationRubette());
        setInputForm(r, scoreForm());
        assertEquals(list("Input denotator #0 is null."), run(r, null, score()));
        AlterationRubette r2 = fresh(new AlterationRubette());
        setInputForm(r2, scoreForm());
        assertEquals(list("Input denotator #1 is null."), run(r2, score(), null));
    }

    // ---------- helpers ----------

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
}
