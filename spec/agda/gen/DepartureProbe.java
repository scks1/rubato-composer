import java.util.ArrayList;
import java.util.List;

import org.rubato.base.Repository;
import org.rubato.logeo.DenoFactory;
import org.rubato.logeo.FormFactory;
import org.rubato.math.arith.Rational;
import org.rubato.math.module.QElement;
import org.rubato.math.module.RElement;
import org.rubato.math.module.ZElement;
import org.rubato.math.yoneda.*;

/**
 * Runs the Java core on the cases where the Agda spec records a difference
 * between Rubato's implementation and the theory, and prints one line per
 * case. spec/agda/README.md cites the line labels (P1, P2, ...).
 */
public class DepartureProbe {

    static Repository rep = Repository.systemRepository();

    static Denotator note(String name, double onset, int pitch) throws Exception {
        List<Denotator> cs = new ArrayList<Denotator>();
        cs.add(DenoFactory.makeDenotator(rep.getForm("Onset"), onset));
        cs.add(DenoFactory.makeDenotator(rep.getForm("Pitch"), new Rational(pitch)));
        cs.add(DenoFactory.makeDenotator(rep.getForm("Loudness"), 80));
        cs.add(DenoFactory.makeDenotator(rep.getForm("Duration"), 1.0));
        cs.add(DenoFactory.makeDenotator(rep.getForm("Voice"), 0));
        return name == null
            ? DenoFactory.makeDenotator(rep.getForm("Note"), cs)
            : DenoFactory.makeDenotator(name, rep.getForm("Note"), cs);
    }

    static String onsets(PowerDenotator p) throws Exception {
        StringBuilder b = new StringBuilder("[");
        for (Denotator d : p.getFactors()) {
            if (b.length() > 1) b.append(", ");
            b.append(((SimpleDenotator) ((LimitDenotator) d).getFactor(0)).getReal());
        }
        return b.append("]").toString();
    }

    static String elems(PowerDenotator p) {
        StringBuilder b = new StringBuilder("[");
        for (Denotator d : p.getFactors()) {
            if (b.length() > 1) b.append(", ");
            b.append(((SimpleDenotator) d).getElement().stringRep());
        }
        return b.append("]").toString();
    }

    static void p(String label, Object value) {
        System.out.println(label + " " + value);
    }

    public static void main(String[] args) throws Exception {
        Form noteForm = rep.getForm("Note");
        Form scoreForm = rep.getForm("Score");

        // P1 a limit denotator built from fewer coordinates than factors is padded with defaults
        List<Denotator> three = new ArrayList<Denotator>();
        three.add(DenoFactory.makeDenotator(rep.getForm("Onset"), 2.5));
        three.add(DenoFactory.makeDenotator(rep.getForm("Pitch"), new Rational(60)));
        three.add(DenoFactory.makeDenotator(rep.getForm("Loudness"), 90));
        LimitDenotator padded = (LimitDenotator) DenoFactory.makeDenotator(noteForm, three);
        p("P1 padded-factor-count", padded.getFactorCount());
        p("P1 padded-duration", ((SimpleDenotator) padded.getFactor(3)).getReal());
        p("P1 padded-voice", ((SimpleDenotator) padded.getFactor(4)).getInteger());

        // P2 extra coordinates beyond the factor count are dropped
        List<Denotator> seven = new ArrayList<Denotator>(((LimitDenotator) note(null, 1.0, 60)).getFactors());
        seven.add(DenoFactory.makeDenotator(rep.getForm("Voice"), 7));
        seven.add(DenoFactory.makeDenotator(rep.getForm("Voice"), 8));
        LimitDenotator truncated = (LimitDenotator) DenoFactory.makeDenotator(noteForm, seven);
        p("P2 given-coordinates", seven.size());
        p("P2 truncated-factor-count", truncated.getFactorCount());

        // P3 power denotators are sorted by compareTo and drop compareTo-equal elements
        List<Denotator> ns = new ArrayList<Denotator>();
        ns.add(note(null, 2.0, 60));
        ns.add(note(null, 1.0, 60));
        ns.add(note(null, 2.0, 60));
        PowerDenotator score = (PowerDenotator) DenoFactory.makeDenotator(scoreForm, ns);
        p("P3 inserted-onsets", "[2.0, 1.0, 2.0]");
        p("P3 stored-onsets", onsets(score));

        // P4 denotator names take part in identity: two equal notes with different names both stay
        List<Denotator> named = new ArrayList<Denotator>();
        named.add(note("a", 1.0, 60));
        named.add(note("b", 1.0, 60));
        named.add(note(null, 1.0, 60));
        PowerDenotator namedScore = (PowerDenotator) DenoFactory.makeDenotator(scoreForm, named);
        p("P4 stored-count", namedScore.getFactorCount());
        p("P4 first-is-anonymous", namedScore.getFactor(0).getName() == null);

        // P5 Z comparison subtracts 32-bit ints, which overflows
        ZElement min = new ZElement(Integer.MIN_VALUE);
        ZElement one = new ZElement(1);
        p("P5 compare(MIN_VALUE,1)>0", min.compareTo(one) > 0);
        List<Denotator> ints = new ArrayList<Denotator>();
        Form integer = rep.getForm("Integer");
        ints.add(DenoFactory.makeDenotator(integer, 1));
        ints.add(DenoFactory.makeDenotator(integer, Integer.MIN_VALUE));
        ints.add(DenoFactory.makeDenotator(integer, 0));
        p("P5 IntegerSet-order", elems((PowerDenotator) DenoFactory.makeDenotator(rep.getForm("IntegerSet"), ints)));

        // P6 R comparison: equality within epsilon; NaN compares greater than everything, itself included
        RElement nan = new RElement(Double.NaN);
        p("P6 epsilon", RElement.getEpsilon());
        p("P6 compare(NaN,NaN)", nan.compareTo(new RElement(Double.NaN)));
        p("P6 compare(NaN,1.0)", nan.compareTo(new RElement(1.0)));
        p("P6 compare(1.0,NaN)", new RElement(1.0).compareTo(nan));
        List<Denotator> reals = new ArrayList<Denotator>();
        Form real = rep.getForm("Real");
        reals.add(DenoFactory.makeDenotator(real, Double.NaN));
        reals.add(DenoFactory.makeDenotator(real, Double.NaN));
        reals.add(DenoFactory.makeDenotator(real, 1.0));
        p("P6 RealSet-with-two-NaN", elems((PowerDenotator) DenoFactory.makeDenotator(rep.getForm("RealSet"), reals)));
        p("P6 compare(0.0,-0.0)", new RElement(0.0).compareTo(new RElement(-0.0)));

        // P7 Q comparison multiplies 32-bit ints, which overflows
        QElement big = new QElement(new Rational(65536, 1));
        QElement small = new QElement(new Rational(1, 65536));
        p("P7 compare(65536,1/65536)<0", big.compareTo(small) < 0);

        // P8 list order: when one list is a prefix of the other, the shorter one is greater
        Form intList = rep.autogenListForm(integer);
        Denotator l1 = DenoFactory.makeDenotator(intList, DenoFactory.makeDenotator(integer, 1));
        Denotator l12 = DenoFactory.makeDenotator(intList, DenoFactory.makeDenotator(integer, 1), DenoFactory.makeDenotator(integer, 2));
        p("P8 compare([1],[1,2])>0", l1.compareTo(l12) > 0);
        p("P8 list-form-name", intList.getNameString());

        // P9 a colimit index outside the cofactor range
        Form intOrReal = rep.getForm("IntegerOrReal");
        try {
            Denotator c = DenoFactory.makeDenotator(intOrReal, 5, DenoFactory.makeDenotator(integer, 1));
            p("P9 index-5", c == null ? "null" : "accepted");
        }
        catch (Throwable t) {
            p("P9 index-5", t.getClass().getName());
        }
        try {
            Denotator c = DenoFactory.makeDenotator(intOrReal, 1, DenoFactory.makeDenotator(integer, 1));
            p("P9 wrong-cofactor", c == null ? "null" : "accepted");
        }
        catch (Throwable t) {
            p("P9 wrong-cofactor", t.getClass().getName());
        }

        // P10 registered forms are equal by name; a second form with a taken name is refused
        LimitForm otherNote = FormFactory.makeLimitForm("Note", rep.getForm("Onset"), rep.getForm("Pitch"));
        p("P10 unregistered-same-name-equals-registered", noteForm.equals(otherNote));
        p("P10 register-conflicting-name", rep.register(otherNote) == null ? "null" : "returned a form");

        // P11 form order: by type first (simple < limit < colimit < power < list), then by name
        p("P11 compare(Score,Note)>0", scoreForm.compareTo(noteForm) > 0);
        p("P11 compare(Onset,Note)<0", rep.getForm("Onset").compareTo(noteForm) < 0);
    }
}
