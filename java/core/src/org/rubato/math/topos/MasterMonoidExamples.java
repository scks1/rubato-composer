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

package org.rubato.math.topos;

import java.util.ArrayList;
import java.util.List;

import org.rubato.math.global.SimplicialComplex;
import org.rubato.math.harmony.Triads;

/**
 * Worked examples for the master-monoid classes (arXiv 2611.32655) and a
 * printed report: the M_2 presentation, the neo-Riemannian Tonnetz as a
 * 2-dimensional semisimplicial set and its free M_2-set, Example 4.9's
 * sphere and Example 2.7's finite F_2-set.
 * <p>
 * The Tonnetz is this fork's example, not the paper's. Its vertices are
 * the 12 pitch classes; its 36 edges are the intervals x -&gt; x + 4
 * (major third), x -&gt; x + 3 (minor third) and x -&gt; x + 7 (fifth), each
 * with d_0 the upper and d_1 the lower pitch class; its 24 triangles are
 * the triads numbered as in {@link Triads}. A triad's vertices are ordered
 * root, third, fifth, so d_0 is the third-fifth edge, d_1 the root-fifth
 * edge and d_2 the root-third edge. That ordering is a choice made here;
 * any ordering of the vertices of each triad that matches the edge
 * directions would serve.
 * <p>
 * Run {@link #main(String[])} to print the report.
 */
public final class MasterMonoidExamples {

    /** Edge offsets of the three interval classes, and their sizes in semitones. */
    private static final int M3 = 0, MINOR3 = 12, P5 = 24;

    private static FiniteFunctor tonnetzFunctor;

    private MasterMonoidExamples() { /* static utility */ }

    private static String pitch(int pc) {
        return Triads.name(Triads.major(pc));
    }

    /** The Tonnetz: 12 vertices, 36 edges, 24 triads, faces ordered root, third, fifth (a choice). */
    public static SemisimplicialSet tonnetz() {
        String[][] labels = new String[3][];
        labels[0] = new String[12];
        for (int pc = 0; pc < 12; pc++) {
            labels[0][pc] = pitch(pc);
        }
        int[][] edges = new int[36][];
        labels[1] = new String[36];
        int[] offsets = { M3, MINOR3, P5 };
        int[] steps = { 4, 3, 7 };
        for (int t = 0; t < 3; t++) {
            for (int pc = 0; pc < 12; pc++) {
                int head = (pc + steps[t]) % 12;
                edges[offsets[t] + pc] = new int[] { head, pc };
                labels[1][offsets[t] + pc] = pitch(pc) + "-" + pitch(head);
            }
        }
        int[][] triads = new int[Triads.COUNT][];
        labels[2] = new String[Triads.COUNT];
        for (int s = 0; s < Triads.COUNT; s++) {
            int r = Triads.root(s);
            if (Triads.isMajor(s)) {
                triads[s] = new int[] { MINOR3 + (r + 4) % 12, P5 + r, M3 + r };
            } else {
                triads[s] = new int[] { M3 + (r + 3) % 12, P5 + r, MINOR3 + r };
            }
            labels[2][s] = Triads.name(s);
        }
        return new SemisimplicialSet(new int[] { 12, 36, Triads.COUNT }, new int[][][] { null, edges, triads }, labels);
    }

    /** The Tonnetz as a functor X : C_2 -&gt; FinSet. */
    public static synchronized FiniteFunctor tonnetzFunctor() {
        if (tonnetzFunctor == null) {
            tonnetzFunctor = tonnetz().toFunctor(SemisimplicialCategory.truncated(2));
        }
        return tonnetzFunctor;
    }

    /** Example 4.9: one vertex v, one edge e with both faces v, two triangles with every face e. */
    public static SemisimplicialSet sphere() {
        return new SemisimplicialSet(new int[] { 1, 1, 2 },
                                     new int[][][] { null, { { 0, 0 } }, { { 0, 0, 0 }, { 0, 0, 0 } } },
                                     new String[][] { { "v" }, { "e" }, { "sigma", "tau" } });
    }

    /**
     * Example 2.7's finite F_2-set as an M_1-set: S = {1, 2, 3} stored as
     * 0, 1, 2, with s the identity and t the 3-cycle 1 -&gt; 2 -&gt; 3 -&gt; 1.
     * The identification of s with d^1_1 (keeping vertex 0, the tail) and
     * of t with d^1_0 is a choice; M_1 = F_2 either way (Example 2.2).
     */
    public static FiniteMonoidAction example27Action() {
        return FiniteMonoidAction.ofGenerators(SemisimplicialCategory.truncated(1), 3,
                                               new int[][][] { null, { { 1, 2, 0 }, { 0, 1, 2 } } });
    }

    /**
     * The printed report. Free-action listings are truncated windows of
     * words up to the given length.
     */
    public static String report(int windowLength) {
        StringBuilder b = new StringBuilder();
        SemisimplicialCategory c2 = SemisimplicialCategory.truncated(2);
        b.append("Master monoid (arXiv 2611.32655), Rubato fork demo\n\n");
        b.append(c2.presentation()).append('\n');
        b.append("generators ").append(c2.generators().size()).append(" = n(n+3)/2, relations ")
         .append(c2.relations().size()).append(" (Prop. 4.4)\n\n");

        SemisimplicialSet t = tonnetz();
        b.append("Tonnetz: ").append(t.count(0)).append(" pitch classes, ").append(t.count(1))
         .append(" interval edges, ").append(t.count(2)).append(" triads; Euler characteristic ")
         .append(t.eulerCharacteristic()).append('\n');
        b.append("triad faces ordered root, third, fifth: our choice, the paper has no Tonnetz\n");
        int c = Triads.major(0);
        int a = Triads.minor(9);
        b.append("  C: d0 = ").append(t.label(1, t.face(2, c, 0))).append(", d1 = ").append(t.label(1, t.face(2, c, 1)))
         .append(", d2 = ").append(t.label(1, t.face(2, c, 2))).append('\n');
        b.append("  a: d0 = ").append(t.label(1, t.face(2, a, 0))).append(", d1 = ").append(t.label(1, t.face(2, a, 1)))
         .append(", d2 = ").append(t.label(1, t.face(2, a, 2))).append('\n');
        String h = ChainComplex.format(t.homology());
        b.append("homology of |X| (semisimplicial chain complex): ").append(h).append('\n');
        List<String> names = new ArrayList<String>();
        List<int[]> simplexes = new ArrayList<int[]>();
        for (int pc = 0; pc < 12; pc++) {
            names.add(t.label(0, pc));
        }
        for (int s = 0; s < Triads.COUNT; s++) {
            int r = Triads.root(s);
            simplexes.add(new int[] { r, (r + (Triads.isMajor(s) ? 4 : 3)) % 12, (r + 7) % 12 });
        }
        b.append("same triads in SimplicialComplex.generatedBy: ")
         .append(SimplicialComplex.generatedBy(names, simplexes).homology()).append('\n');
        CategoryOfElements el = new CategoryOfElements(tonnetzFunctor());
        b.append("nerve of the category of elements (Example 1.7, Remark 4.14): ")
         .append(ChainComplex.format(SemisimplicialSet.nerve(el.category()).homology())).append('\n');
        b.append("S = iota_2! X is an M_2-set with S//M_2 ~ |X| (proof of Thm. 4.8), so these are H_*(S//M_2)\n");
        FreeAction act = new FreeAction(tonnetzFunctor());
        b.append("free M_2-set, ").append(act.describeWindow(0).split("\n")[0]).append('\n');
        b.append("free M_2-set, ").append(act.describeWindow(windowLength).split("\n")[0]).append('\n');
        FreeAction.Element root = act.root(2, c);
        b.append("  at the root [1, C]: ");
        UniversalMonoid m = act.monoid();
        boolean first = true;
        for (int letter : m.letters()) {
            if (!first) {
                b.append("; ");
            }
            first = false;
            FreeAction.Element e = act.act(new int[] { letter }, root);
            b.append(m.format(new int[] { letter })).append(e.isRoot() ? " absorbed -> " : " branches -> ")
             .append(e.isRoot() ? t.label(e.type, e.x) : label(m, t, e));
        }
        b.append('\n');
        b.append("  (triads absorb the d^2_j, edges absorb the d^1_i, pitch classes absorb nothing)\n\n");

        SemisimplicialSet sp = sphere();
        b.append("Example 4.9 sphere: counts 1, 1, 2; Euler characteristic ").append(sp.eulerCharacteristic())
         .append("; homology ").append(ChainComplex.format(sp.homology())).append('\n');
        FreeAction sa = new FreeAction(sp.toFunctor(c2));
        FreeAction.Element e = sa.root(1, 0);
        int[] d10 = { c2.face(1, 0) };
        FreeAction.Element x = sa.act(d10, sa.act(new int[] { c2.face(2, 0) }, e));
        FreeAction.Element y = sa.act(d10, sa.act(new int[] { c2.face(2, 1) }, e));
        b.append("  d^1_0 . [d^2_0, e] = ").append(label(sa.monoid(), sp, x)).append(", d^1_0 . [d^2_1, e] = ")
         .append(label(sa.monoid(), sp, y))
         .append(x.equals(y) ? " (equal)" : " (different)").append('\n');
        b.append("  free M_2-set, ").append(sa.describeWindow(windowLength).split("\n")[0]).append("\n\n");

        FiniteMonoidAction ex = example27Action();
        b.append("Example 2.7, S = {1,2,3}, s = identity, t = 3-cycle: S//F_2 via Thm. 4.12 ")
         .append(ChainComplex.format(ex.homotopyQuotientHomology(SemisimplicialCategory.truncated(1))))
         .append(", via Lemma 4.10 and a nerve ")
         .append(ChainComplex.format(ex.homotopyQuotientByNerve().homology())).append('\n');
        return b.toString();
    }

    /** An element [w, x] with the label of x. */
    private static String label(UniversalMonoid m, SemisimplicialSet x, FreeAction.Element e) {
        return "[" + m.format(e.word) + ", " + x.label(e.type, e.x) + "]";
    }

    /** Prints {@link #report(int)} with windows of length 1, or of the length given as the argument. */
    public static void main(String[] args) {
        System.out.print(report(args.length > 0 ? Integer.parseInt(args[0]) : 1));
    }
}
