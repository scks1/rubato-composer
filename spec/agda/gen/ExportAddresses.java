import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.rubato.logeo.FormFactory;
import org.rubato.math.arith.Rational;
import org.rubato.math.matrix.QMatrix;
import org.rubato.math.matrix.RMatrix;
import org.rubato.math.matrix.ZMatrix;
import org.rubato.math.module.*;
import org.rubato.math.module.Module;
import org.rubato.math.module.morphism.*;
import org.rubato.math.yoneda.*;

/**
 * Writes spec/agda/GeneratedAddresses.agda: denotators at the addresses
 * Z^m, Q^m and R^m, address changes along affine maps and constants,
 * and the values Java computes for d.changeAddress(f) and d.at(a).
 * AddressChecks.agda checks them against the model in Addresses.agda.
 *
 * Every case is given by small integer data. Q divides each entry by 2
 * and R multiplies it by 0.5, so the R values are exact in binary and
 * Java's double arithmetic and Agda's floats agree.
 *
 * Usage: java ExportAddresses > GeneratedAddresses.agda
 */
public class ExportAddresses {

    /** An affine map C^m -> C^n as an n x m matrix and a shift of length n. */
    static final class Aff {
        final int m, n; final int[][] a; final int[] b; final boolean constant;
        Aff(int m, int[][] a, int[] b, boolean constant) {
            this.m = m; this.n = b.length; this.a = a; this.b = b; this.constant = constant;
        }
    }

    static Aff aff(int m, int[][] a, int... b) { return new Aff(m, a, b, false); }
    /** The constant map C^m -> C^n with value b, built as ConstantMorphism. */
    static Aff cst(int m, int... b) {
        int[][] a = new int[b.length][m];
        return new Aff(m, a, b, true);
    }

    static final Aff[][] CHANGES = {
        // d : C^m -> C^n, f : C^k -> C^m
        { aff(1, new int[][]{{1}}, 65),             aff(1, new int[][]{{2}}, 1) },
        { aff(1, new int[][]{{-1}}, 67),            aff(1, new int[][]{{3}}, -2) },
        { aff(2, new int[][]{{1, 2}}, 60),          aff(1, new int[][]{{1}, {-1}}, 4, 0) },
        { aff(1, new int[][]{{3}, {-2}}, 0, 7),     aff(2, new int[][]{{1, 1}}, -1) },
        { aff(2, new int[][]{{1, 2}, {0, -1}}, 5, 6), aff(2, new int[][]{{2, 0}, {1, 1}}, 1, -3) },
        { cst(1, 66),                               aff(1, new int[][]{{2}}, 1) },
        { cst(1, 66),                               aff(2, new int[][]{{1, 3}}, 0) },
        { aff(1, new int[][]{{1}}, 65),             cst(0, 2) },
        { aff(2, new int[][]{{1, 2}, {0, -1}}, 5, 6), cst(0, 1, -1) },
        { cst(1, 66),                               cst(0, 4) },
    };

    /** d and the element a for d.at(a) */
    static final Object[][] ATS = {
        { aff(1, new int[][]{{1}}, 65), new int[]{0} },
        { aff(1, new int[][]{{1}}, 65), new int[]{1} },
        { aff(1, new int[][]{{-1}}, 67), new int[]{1} },
        { aff(2, new int[][]{{1, 2}, {0, -1}}, 5, 6), new int[]{3, -2} },
        { cst(1, 66), new int[]{9} },
    };

    interface R {
        String tag();
        Module free(int dim);
        ModuleElement elem(int dim, int[] v);
        ModuleMorphism affine(Aff f);
        String agda(ModuleElement e);
        String agdaScalar(int v);
    }

    static ModuleMorphism constant(Module dom, ModuleElement value) {
        return new ConstantMorphism(dom, value);
    }

    static final R ZR = new R() {
        public String tag() { return "Z"; }
        public Module free(int dim) { return dim == 1 ? ZRing.ring : ZProperFreeModule.make(dim); }
        public ModuleElement elem(int dim, int[] v) {
            return dim == 1 ? new ZElement(v[0]) : ZProperFreeElement.make(v);
        }
        public ModuleMorphism affine(Aff f) {
            if (f.constant) return constant(free(f.m), elem(f.n, f.b));
            return ZFreeAffineMorphism.make(new ZMatrix(f.a), f.b);
        }
        public String agda(ModuleElement e) { return agdaScalar(((ZElement) e).getValue()); }
        public String agdaScalar(int v) { return v >= 0 ? "pos " + v : "negsuc " + (-v - 1); }
    };

    static Rational half(int v) { return new Rational(v, 2); }

    static final R QR = new R() {
        public String tag() { return "Q"; }
        public Module free(int dim) { return dim == 1 ? QRing.ring : QProperFreeModule.make(dim); }
        public ModuleElement elem(int dim, int[] v) {
            Rational[] r = new Rational[v.length];
            for (int i = 0; i < v.length; i++) r[i] = half(v[i]);
            return dim == 1 ? new QElement(r[0]) : QProperFreeElement.make(r);
        }
        public ModuleMorphism affine(Aff f) {
            if (f.constant) return constant(free(f.m), elem(f.n, f.b));
            Rational[][] a = new Rational[f.n][f.m];
            for (int i = 0; i < f.n; i++) for (int j = 0; j < f.m; j++) a[i][j] = half(f.a[i][j]);
            Rational[] b = new Rational[f.n];
            for (int i = 0; i < f.n; i++) b[i] = half(f.b[i]);
            return QFreeAffineMorphism.make(new QMatrix(a), b);
        }
        public String agda(ModuleElement e) {
            Rational r = ((QElement) e).getValue();
            return "(" + ZR.agdaScalar(r.getNumerator()) + ") / " + r.getDenominator();
        }
        public String agdaScalar(int v) { return "(" + ZR.agdaScalar(v) + ") / 2"; }
    };

    static String fl(double v) {
        if (v < 0 || (v == 0 && 1 / v < 0)) return "primFloatNegate " + Double.toString(-v);
        return Double.toString(v);
    }

    static final R RR = new R() {
        public String tag() { return "R"; }
        public Module free(int dim) { return dim == 1 ? RRing.ring : RProperFreeModule.make(dim); }
        public ModuleElement elem(int dim, int[] v) {
            double[] r = new double[v.length];
            for (int i = 0; i < v.length; i++) r[i] = v[i] * 0.5;
            return dim == 1 ? new RElement(r[0]) : RProperFreeElement.make(r);
        }
        public ModuleMorphism affine(Aff f) {
            if (f.constant) return constant(free(f.m), elem(f.n, f.b));
            double[][] a = new double[f.n][f.m];
            for (int i = 0; i < f.n; i++) for (int j = 0; j < f.m; j++) a[i][j] = f.a[i][j] * 0.5;
            double[] b = new double[f.n];
            for (int i = 0; i < f.n; i++) b[i] = f.b[i] * 0.5;
            return RFreeAffineMorphism.make(new RMatrix(a), b);
        }
        public String agda(ModuleElement e) { return paren(fl(((RElement) e).getValue())); }
        public String agdaScalar(int v) { return paren(fl(v * 0.5)); }
    };

    static String paren(String s) { return s.indexOf(' ') < 0 ? s : "(" + s + ")"; }

    static String vec(List<String> xs) {
        StringBuilder b = new StringBuilder("(");
        for (String x : xs) b.append(paren(x)).append(" ∷ᵛ ");
        return b.append("⟨⟩)").toString();
    }

    static String vecInts(R r, int[] v) {
        List<String> xs = new ArrayList<String>();
        for (int x : v) xs.add(r.agdaScalar(x));
        return vec(xs);
    }

    /** The components of a Java element of C^dim, written as an Agda vector. */
    static String vecElem(R r, ModuleElement e, int dim) {
        List<String> xs = new ArrayList<String>();
        for (int i = 0; i < dim; i++) xs.add(r.agda(dim == 1 ? e : e.getComponent(i)));
        return vec(xs);
    }

    static String affAgda(R r, Aff f) {
        String t = r.tag() + "m.";
        List<String> rows = new ArrayList<String>();
        for (int i = 0; i < f.n; i++) rows.add(vecInts(r, f.a[i]));
        return "(" + t + "affine " + vec(rows) + " " + vecInts(r, f.b) + ")";
    }

    static int formCount = 0;

    static SimpleDenotator den(R r, Aff d) throws Exception {
        SimpleForm form = FormFactory.makeModuleForm("ExportAddresses" + r.tag() + (formCount++), r.free(d.n));
        return new SimpleDenotator(null, form, r.affine(d));
    }

    static ModuleElement value(Denotator d, ModuleElement at) throws Exception {
        return ((SimpleDenotator) d).getElement(at);
    }

    static void export(PrintStream out, R r) throws Exception {
        String t = r.tag();
        out.println("-- " + CHANGES.length + " address changes over " + t);
        out.println(t.toLowerCase() + "Changes : List " + t + "m.ChangeCase");
        out.println(t.toLowerCase() + "Changes =");
        String sep = "    ";
        for (Aff[] c : CHANGES) {
            Aff d = c[0], f = c[1];
            SimpleDenotator dd = den(r, d);
            ModuleMorphism fm = r.affine(f);
            Denotator changed = dd.changeAddress(fm);
            if (changed == null) throw new IllegalStateException("changeAddress returned null");
            Module dom = f.m == 0 ? r.free(1).getNullModule() : r.free(f.m);
            if (!changed.getAddress().equals(dom)) {
                throw new IllegalStateException("address " + changed.getAddress() + ", expected " + dom);
            }
            List<String> samples = new ArrayList<String>();
            List<int[]> points = new ArrayList<int[]>();
            points.add(new int[f.m]);
            for (int i = 0; i < f.m; i++) { int[] e = new int[f.m]; e[i] = 2; points.add(e); }
            for (int[] p : points) {
                ModuleElement pe = f.m == 0 ? dom.getZero() : r.elem(f.m, p);
                samples.add("(" + vecInts(r, p) + " " + t + "m.↦ " + vecElem(r, value(changed, pe), d.n) + ")");
            }
            StringBuilder sl = new StringBuilder("(");
            for (String s : samples) sl.append(s).append(" ∷ ");
            sl.append("[])");
            out.println(sep + t + "m.change " + f.m + " " + d.m + " " + d.n);
            out.println("      " + affAgda(r, d));
            out.println("      " + affAgda(r, f));
            out.println("      " + sl);
            sep = "  ∷ ";
        }
        out.println("  ∷ []");
        out.println();
        out.println("-- " + ATS.length + " evaluations over " + t);
        out.println(t.toLowerCase() + "Ats : List " + t + "m.AtCase");
        out.println(t.toLowerCase() + "Ats =");
        sep = "    ";
        for (Object[] c : ATS) {
            Aff d = (Aff) c[0]; int[] a = (int[]) c[1];
            Denotator e = den(r, d).at(r.elem(d.m, a));
            if (!e.nullAddressed()) throw new IllegalStateException("at() is not null-addressed");
            ModuleElement v = ((SimpleDenotator) e).getElement();
            out.println(sep + t + "m.atCase " + d.m + " " + d.n + " " + affAgda(r, d) + " " + vecInts(r, a) + " " + vecElem(r, v, d.n));
            sep = "  ∷ ";
        }
        out.println("  ∷ []");
        out.println();
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = System.out;
        out.println("{-# OPTIONS --safe --without-K #-}");
        out.println();
        out.println("-- GeneratedAddresses.agda: written by spec/agda/gen/ExportAddresses.java");
        out.println("-- from the running Java code. Do not edit; run spec/agda/run-checks.sh.");
        out.println();
        out.println("module GeneratedAddresses where");
        out.println();
        out.println("open import Prelude hiding (_+_; _*_)");
        out.println("open import Addresses");
        out.println();
        out.println("module Zm = Free ℤ-ring");
        out.println("module Qm = Free ℚ-ring");
        out.println("module Rm = Free ℝ-ring");
        out.println();
        export(out, ZR);
        export(out, QR);
        export(out, RR);
    }
}
