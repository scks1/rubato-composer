package org.rubato.rubettes.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.composer.RunInfo;
import org.rubato.composer.rubette.Link;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.RAffineMorphism;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.rubettes.builtin.RealArithRubette;
import org.rubato.rubettes.wallpaper.JWallpaperDimensionsTable;
import org.rubato.rubettes.wallpaper.WallpaperRubette;

/**
 * Checks that WallpaperRubette's protected getUnitedMappedDenotators(input) leaves the applied
 * morphisms, ranges and coordinates in place and gives the same output as the list-consuming
 * private overload that run() calls on a copy.
 */
public class WallpaperUnitedMappedTest extends TestCase {

    private static Method method(String name, Class<?>... types) throws Exception {
        Method m = WallpaperRubette.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m;
    }

    private static Object call(Object o, Method m, Object... args) throws Exception {
        try {
            return m.invoke(o, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    /** The protected one-argument method under test. */
    private static PowerDenotator united(WallpaperRubette r, PowerDenotator input) throws Exception {
        return (PowerDenotator) call(r, method("getUnitedMappedDenotators", PowerDenotator.class), input);
    }

    /** The private overload on a copy of the applied list, as run() calls it. */
    private static PowerDenotator unitedOnCopy(WallpaperRubette r, PowerDenotator input) throws Exception {
        List<ModuleMorphism> copy = new ArrayList<ModuleMorphism>(r.getMorphismsTable().getMorphisms());
        return (PowerDenotator) call(r, method("getUnitedMappedDenotators", PowerDenotator.class, List.class),
                input, copy);
    }

    private static List<List<Integer>> paths(int... simpleForms) {
        List<List<Integer>> p = new ArrayList<List<Integer>>();
        for (int c : simpleForms) {
            List<Integer> path = new ArrayList<Integer>();
            path.add(c);
            p.add(path);
        }
        return p;
    }

    private static WallpaperRubette empty() throws Exception {
        WallpaperRubette r = (WallpaperRubette) new WallpaperRubette().newInstance();
        call(r, method("setInputForm", PowerForm.class), (PowerForm) Repository.systemRepository().getForm("Score"));
        return r;
    }

    /**
     * A Wallpaper on Score with two applied morphisms: onset to onset (x2+1) over 0..2 and
     * duration to duration (x1+0.5) over 1..3.
     */
    private static WallpaperRubette wallpaper() throws Exception {
        WallpaperRubette r = empty();
        Method add = method("addMorphism", ModuleMorphism.class, int.class, int.class, List.class);
        call(r, add, new RAffineMorphism(2, 1), 0, 2, paths(0, 0));
        call(r, add, new RAffineMorphism(1, 0.5), 1, 3, paths(3, 3));
        return r;
    }

    /** A score with one default note. */
    private static PowerDenotator score() throws Exception {
        LimitForm noteForm = (LimitForm) Repository.systemRepository().getForm("Note");
        PowerForm scoreForm = (PowerForm) Repository.systemRepository().getForm("Score");
        List<Denotator> notes = new ArrayList<Denotator>();
        notes.add(noteForm.createDefaultDenotator());
        return new PowerDenotator(NameDenotator.make(""), scoreForm, notes);
    }

    /** What the table holds: morphisms, ranges and coordinates, as text. */
    private static String applied(WallpaperRubette r) {
        JWallpaperDimensionsTable t = r.getMorphismsTable();
        StringBuilder s = new StringBuilder();
        for (ModuleMorphism m : t.getMorphisms()) {
            s.append(m).append(' ').append(t.getRangeFrom(m)).append("..").append(t.getRangeTo(m))
                    .append(' ').append(t.getCoordinates(m)).append(';');
        }
        return s.toString();
    }

    private static final RunInfo RUN_INFO = new RunInfo() {
        public boolean stopped() { return false; }
        public void addMessage(RubetteModel rubette, String msg) { }
    };

    private static PowerDenotator run(WallpaperRubette r, Denotator input) {
        RubetteModel model = new RubetteModel(r, r.getName());
        RealArithRubette source = new RealArithRubette();
        source.setOutput(0, input);
        model.setInLink(new Link(new RubetteModel(source, "source"), 0, model, 0));
        r.clearErrors();
        r.run(RUN_INFO);
        assertFalse(String.valueOf(r.getErrors()), r.hasErrors());
        return (PowerDenotator) r.getOutput(0);
    }

    public void testOneCallKeepsTheAppliedMorphisms() throws Exception {
        WallpaperRubette r = wallpaper();
        String before = applied(r);
        assertEquals(2, r.getMorphismsTable().getMorphisms().size());
        united(r, score());
        assertEquals(2, r.getMorphismsTable().getMorphisms().size());
        assertEquals(before, applied(r));
    }

    public void testTwoCallsGiveEqualOutput() throws Exception {
        WallpaperRubette r = wallpaper();
        PowerDenotator first = united(r, score());
        PowerDenotator second = united(r, score());
        assertEquals(first.getFactorCount(), second.getFactorCount());
        assertTrue(first.equals(second));
    }

    public void testOutputMatchesTheOldPathOnAFreshRubette() throws Exception {
        PowerDenotator old = unitedOnCopy(wallpaper(), score());
        PowerDenotator now = united(wallpaper(), score());
        // onset 0..2 gives 3 copies, duration 1..3 gives 3: 9 distinct notes
        assertEquals(9, old.getFactorCount());
        assertEquals(old.getFactorCount(), now.getFactorCount());
        assertTrue(old.equals(now));
        // the comparison can tell outputs apart
        assertFalse(old.equals(score()));
    }

    public void testEmptyListFailsAsBeforeAndStaysEmpty() throws Exception {
        WallpaperRubette r = empty();
        Class<?> oldFailure = null;
        try {
            unitedOnCopy(r, score());
        } catch (Exception e) {
            oldFailure = e.getClass();
        }
        assertEquals(IndexOutOfBoundsException.class, oldFailure);
        Class<?> newFailure = null;
        try {
            united(r, score());
        } catch (Exception e) {
            newFailure = e.getClass();
        }
        assertEquals(oldFailure, newFailure);
        assertEquals(0, r.getMorphismsTable().getMorphisms().size());
    }

    public void testRunAfterADirectCallMatches() throws Exception {
        PowerDenotator runOnly = run(wallpaper(), score());
        WallpaperRubette r = wallpaper();
        PowerDenotator direct = united(r, score());
        PowerDenotator afterCall = run(r, score());
        assertEquals(9, runOnly.getFactorCount());
        assertTrue(runOnly.equals(afterCall));
        assertTrue(direct.equals(afterCall));
    }
}
