import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

import org.rubato.base.Repository;
import org.rubato.logeo.DenoFactory;
import org.rubato.math.arith.Complex;
import org.rubato.math.arith.Rational;
import org.rubato.math.module.*;
import org.rubato.math.yoneda.*;
import org.rubato.rubettes.util.CoolFormRegistrant;
import org.rubato.rubettes.util.SoundNoteGenerator;

/**
 * Writes spec/agda/Generated.agda: the forms of the running Java
 * repository and a few denotators built by the Java code, as Agda terms.
 * GeneratedChecks.agda proves what the spec says about them.
 *
 * Usage: java ExportAgda > Generated.agda
 */
public class ExportAgda {

    static Repository rep = Repository.systemRepository();

    /** Root forms; the export follows every coordinate form from them. */
    static final String[] ROOTS = {
        "Score", "MacroScore", "SoundScore", "GeneralScore", "Spectrum",
        "HarmonicSpectrum", "FMSet", "GenericSound", "Envelope",
        "IntegerSet", "RealSet", "BooleanList", "BooleanPair", "IntegerOrReal",
        "PitchClassScore", "RealScore", "Dyads", "Complex"
    };

    public static void main(String[] args) throws Exception {
        new CoolFormRegistrant().registerAllTheCoolStuff();
        PrintStream out = System.out;
        out.println("{-# OPTIONS --safe --without-K #-}");
        out.println();
        out.println("-- Generated.agda: written by spec/agda/gen/ExportAgda.java from the");
        out.println("-- running Java repository. Do not edit; run spec/agda/run-checks.sh.");
        out.println();
        out.println("module Generated where");
        out.println();
        out.println("open import Prelude");
        out.println("open import Forms");
        out.println();
        out.println("nanF infF : Float");
        out.println("nanF = primFloatDiv 0.0 0.0");
        out.println("infF = primFloatDiv 1.0 0.0");
        out.println();

        // forms, in name order
        TreeSet<String> names = new TreeSet<String>();
        List<String> todo = new ArrayList<String>();
        for (String r : ROOTS) todo.add(r);
        while (!todo.isEmpty()) {
            String n = todo.remove(todo.size() - 1);
            if (!names.add(n)) continue;
            Form f = rep.getForm(n);
            if (f == null) throw new IllegalStateException("form not registered: " + n);
            for (String c : coordinateNames(f)) todo.add(c);
        }
        out.println("-- " + names.size() + " forms reachable from " + ROOTS.length + " root forms");
        out.println("javaEnv : Env");
        out.println("javaEnv =");
        for (String n : names) {
            out.println("    " + str(n) + " ≔ " + body(rep.getForm(n)));
            out.println("  ∷");
        }
        out.println("  []");
        out.println();

        // denotators built by the Java code
        SoundNoteGenerator sng = new SoundNoteGenerator();
        emit(out, "javaScore", "a Score of three notes, inserted out of order",
             DenoFactory.makeDenotator(rep.getForm("Score"),
                 note(null, 2.0, 64), note(null, 0.0, 60), note("melody-start", 0.5, 62)));
        emit(out, "javaPaddedNote", "a Note built from three coordinates (D9)",
             DenoFactory.makeDenotator(rep.getForm("Note"),
                 DenoFactory.makeDenotator(rep.getForm("Onset"), 2.5),
                 DenoFactory.makeDenotator(rep.getForm("Pitch"), new Rational(121, 2)),
                 DenoFactory.makeDenotator(rep.getForm("Loudness"), 90)));
        Denotator inner = DenoFactory.makeDenotator(rep.getForm("MacroScore"),
             DenoFactory.makeDenotator(rep.getForm("Knot"), note(null, 1.0, 67),
                 DenoFactory.makeDenotator(rep.getForm("MacroScore"), new ArrayList<Denotator>())));
        emit(out, "javaMacroScore", "a MacroScore with a satellite note",
             DenoFactory.makeDenotator(rep.getForm("MacroScore"),
                 DenoFactory.makeDenotator(rep.getForm("Knot"), note(null, 0.0, 60), inner)));
        emit(out, "javaSoundScore", "SoundNoteGenerator.createMultiLevelSoundScore, three levels",
             sng.createMultiLevelSoundScore(new double[][]{{0, 60, 100, 1, 0}, {1, 64, 99, 1, 1}, {2, 67, 98, 1, 2}}));
        sng.setFMModel(SoundNoteGenerator.FM_MODELS[1]);
        emit(out, "javaFMNode", "SoundNoteGenerator.createFMNodeDenotator, model " + SoundNoteGenerator.FM_MODELS[1],
             sng.createFMNodeDenotator(new double[]{0, 60, 100, 1, 0}));
        Form rest = rep.getForm("Rest");
        Denotator aRest = DenoFactory.makeDenotator(rest,
             DenoFactory.makeDenotator(rep.getForm("Onset"), 1.0),
             DenoFactory.makeDenotator(rep.getForm("Duration"), 0.5),
             DenoFactory.makeDenotator(rep.getForm("Voice"), 0));
        emit(out, "javaGeneralScore", "a GeneralScore holding a note and a rest (colimit)",
             DenoFactory.makeDenotator(rep.getForm("GeneralScore"),
                 DenoFactory.makeDenotator(rep.getForm("NoteOrRest"), 0, note(null, 0.0, 60)),
                 DenoFactory.makeDenotator(rep.getForm("NoteOrRest"), 1, aRest)));
        Form envNode = rep.getForm("EnvelopeNode");
        emit(out, "javaEnvelope", "an Envelope (list) with a repeated node",
             DenoFactory.makeDenotator(rep.getForm("Envelope"),
                 envNodeOf(envNode, 0.0, 0.0), envNodeOf(envNode, 0.1, 1.0), envNodeOf(envNode, 0.0, 0.0)));
        emit(out, "javaComplex", "a Complex simple denotator",
             DenoFactory.makeDenotator("i", rep.getForm("Complex"), new Complex(0.0, 1.0)));
        Form integer = rep.getForm("Integer");
        emit(out, "javaIntegerSetOverflow", "IntegerSet of 1, Integer.MIN_VALUE and 0 (D5)",
             DenoFactory.makeDenotator(rep.getForm("IntegerSet"),
                 DenoFactory.makeDenotator(integer, 1),
                 DenoFactory.makeDenotator(integer, Integer.MIN_VALUE),
                 DenoFactory.makeDenotator(integer, 0)));
        Form real = rep.getForm("Real");
        emit(out, "javaRealSetNaN", "RealSet of NaN, NaN and 1.0 (D6)",
             DenoFactory.makeDenotator(rep.getForm("RealSet"),
                 DenoFactory.makeDenotator(real, Double.NaN),
                 DenoFactory.makeDenotator(real, Double.NaN),
                 DenoFactory.makeDenotator(real, 1.0)));
    }

    static Denotator envNodeOf(Form envNode, double onset, double amplitude) throws Exception {
        return DenoFactory.makeDenotator(envNode,
            DenoFactory.makeDenotator(rep.getForm("Onset"), onset),
            DenoFactory.makeDenotator(rep.getForm("Amplitude"), amplitude));
    }

    static Denotator note(String name, double onset, int pitch) throws Exception {
        Denotator[] cs = {
            DenoFactory.makeDenotator(rep.getForm("Onset"), onset),
            DenoFactory.makeDenotator(rep.getForm("Pitch"), new Rational(pitch)),
            DenoFactory.makeDenotator(rep.getForm("Loudness"), 80),
            DenoFactory.makeDenotator(rep.getForm("Duration"), 1.0),
            DenoFactory.makeDenotator(rep.getForm("Voice"), 0)
        };
        return name == null
            ? DenoFactory.makeDenotator(rep.getForm("Note"), cs)
            : DenoFactory.makeDenotator(name, rep.getForm("Note"), cs);
    }

    static void emit(PrintStream out, String id, String comment, Denotator d) {
        if (d == null) throw new IllegalStateException("Java returned null for " + id);
        out.println("-- " + comment);
        out.println(id + " : Den");
        out.println(id + " =");
        out.println(den(d, 1));
        out.println();
    }

    static List<String> coordinateNames(Form f) {
        List<String> res = new ArrayList<String>();
        switch (f.getType()) {
        case Form.LIMIT:
        case Form.COLIMIT:
            for (int i = 0; i < f.getFormCount(); i++) res.add(f.getForm(i).getNameString());
            break;
        case Form.POWER:
        case Form.LIST:
            res.add(f.getForm(0).getNameString());
            break;
        default:
        }
        return res;
    }

    static String body(Form f) {
        switch (f.getType()) {
        case Form.SIMPLE:
            return "simple " + paren(module(((SimpleForm) f).getModule()));
        case Form.LIMIT: {
            LimitForm l = (LimitForm) f;
            List<String> labels = new ArrayList<String>();
            if (l.hasLabels()) for (int i = 0; i < l.getFormCount(); i++) labels.add(l.indexToLabel(i));
            return "limit " + strList(coordinateNames(f)) + " " + strList(labels);
        }
        case Form.COLIMIT: {
            ColimitForm c = (ColimitForm) f;
            List<String> labels = new ArrayList<String>();
            if (c.hasLabels()) for (int i = 0; i < c.getFormCount(); i++) labels.add(c.indexToLabel(i));
            return "colimit " + strList(coordinateNames(f)) + " " + strList(labels);
        }
        case Form.POWER:
            return "power " + str(f.getForm(0).getNameString());
        case Form.LIST:
            return "list " + str(f.getForm(0).getNameString());
        default:
            throw new IllegalStateException("unknown form type " + f.getType());
        }
    }

    static String module(org.rubato.math.module.Module m) {
        if (m instanceof ZRing) return "ZM";
        if (m instanceof QRing) return "QM";
        if (m instanceof RRing) return "RM";
        if (m instanceof CRing) return "CM";
        if (m instanceof ZnRing) return "ZnM " + ((ZnRing) m).getModulus();
        if (m instanceof RProperFreeModule) return "RFreeM " + m.getDimension();
        return "otherM " + str(m.toString());
    }

    static String den(Denotator d, int depth) {
        // The spec models zero-addressed denotators only (README, D4).
        if (!d.nullAddressed()) throw new IllegalStateException("not zero-addressed: " + d);
        String ind = "  ".repeat(depth);
        String head = name(d) + " " + str(d.getForm().getNameString());
        switch (d.getType()) {
        case Denotator.SIMPLE:
            return ind + "simpleD " + head + " " + paren(element(((SimpleDenotator) d).getElement()));
        case Denotator.LIMIT:
            return ind + "limitD " + head + "\n" + dens(((LimitDenotator) d).getFactors(), depth + 1);
        case Denotator.COLIMIT: {
            ColimitDenotator c = (ColimitDenotator) d;
            return ind + "colimitD " + head + " " + c.getIndex() + "\n" + ind + "  (\n" + den(c.getFactor(), depth + 2) + "\n" + ind + "  )";
        }
        case Denotator.POWER:
            return ind + "powerD " + head + "\n" + dens(((PowerDenotator) d).getFactors(), depth + 1);
        case Denotator.LIST:
            return ind + "listD " + head + "\n" + dens(((ListDenotator) d).getFactors(), depth + 1);
        default:
            throw new IllegalStateException("unknown denotator type " + d.getType());
        }
    }

    static String dens(List<Denotator> ds, int depth) {
        String ind = "  ".repeat(depth);
        if (ds.isEmpty()) return ind + "[]";
        StringBuilder b = new StringBuilder();
        b.append(ind).append("(\n");
        for (Denotator d : ds) {
            b.append(den(d, depth + 1)).append("\n").append(ind).append("∷\n");
        }
        b.append(ind).append("  [])");
        return b.toString();
    }

    static String name(Denotator d) {
        NameDenotator n = d.getName();
        return n == null ? "nothing" : "(just " + str(n.getNameString()) + ")";
    }

    static String element(ModuleElement e) {
        if (e instanceof ZElement) return "zE " + paren(integer(((ZElement) e).getValue()));
        if (e instanceof ZnElement) {
            ZnElement z = (ZnElement) e;
            return "znE " + z.getModulus() + " " + z.getValue();
        }
        if (e instanceof QElement) {
            Rational r = ((QElement) e).getValue();
            return "qE " + paren(integer(r.getNumerator())) + " " + r.getDenominator();
        }
        if (e instanceof RElement) return "rE " + paren(fl(((RElement) e).getValue()));
        if (e instanceof CElement) {
            Complex c = ((CElement) e).getValue();
            return "cE " + paren(fl(c.getReal())) + " " + paren(fl(c.getImag()));
        }
        if (e instanceof RProperFreeElement) {
            StringBuilder b = new StringBuilder("rvE (");
            for (double v : ((RProperFreeElement) e).getValue()) b.append(paren(fl(v))).append(" ∷ ");
            return b.append("[])").toString();
        }
        throw new IllegalStateException("element not modelled: " + e.getClass().getName());
    }

    static String integer(long v) {
        return v >= 0 ? "pos " + v : "negsuc " + (-v - 1);
    }

    static String fl(double v) {
        if (Double.isNaN(v)) return "nanF";
        if (Double.isInfinite(v)) return v > 0 ? "infF" : "primFloatNegate infF";
        if (v < 0 || (v == 0 && 1 / v < 0)) return "primFloatNegate " + paren(fl(-v));
        return Double.toString(v).replace('E', 'e');
    }

    static String paren(String s) {
        return s.indexOf(' ') < 0 ? s : "(" + s + ")";
    }

    static String str(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.append('"').toString();
    }

    static String strList(List<String> xs) {
        if (xs.isEmpty()) return "[]";
        StringBuilder b = new StringBuilder("(");
        for (String x : xs) b.append(str(x)).append(" ∷ ");
        return b.append("[])").toString();
    }
}
