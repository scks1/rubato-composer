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

package org.rubato.math.rhythm;

import java.util.ArrayList;
import java.util.List;

import org.rubato.base.Repository;
import org.rubato.base.RubatoException;
import org.rubato.math.module.QElement;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.ListDenotator;
import org.rubato.math.yoneda.ListForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;

/**
 * Rhythm trees as denotators of the recursive built-in form RhythmTree
 * (sources/RHYTHM-TREES-SPEC.md D4), which {@link Repository} registers:
 * <pre>
 *   RhythmTree     = Limit(label: RhythmLabel, subtrees: RhythmSubtrees)
 *   RhythmLabel    = Limit(duration: RhythmDuration, factors: RhythmFactors)
 *   RhythmDuration = Simple(Q)      RhythmFactor = Simple(Q)
 *   RhythmFactors  = List(RhythmFactor)
 *   RhythmSubtrees = List(RhythmTree)
 * </pre>
 * RhythmLabel is the paper's A = Q x List(Q) (Flieder, arXiv 2512.04090,
 * Ex. C.1, p. 55). The form cannot say that there is one subtree per factor
 * (B = Fin(n)); {@link #fromDenotator} checks it.
 */
public final class RhythmTreeDenotators {

    public static final String RHYTHM_TREE = "RhythmTree";
    public static final String RHYTHM_LABEL = "RhythmLabel";
    public static final String RHYTHM_DURATION = "RhythmDuration";
    public static final String RHYTHM_FACTORS = "RhythmFactors";
    public static final String RHYTHM_FACTOR = "RhythmFactor";
    public static final String RHYTHM_SUBTREES = "RhythmSubtrees";

    private RhythmTreeDenotators() { /* static only */ }

    private static Repository rep() {
        return Repository.systemRepository();
    }

    /** The RhythmTree form. */
    public static LimitForm treeForm() {
        return (LimitForm) rep().getForm(RHYTHM_TREE);
    }

    /**
     * The tree as an anonymous RhythmTree denotator.
     *
     * @throws IllegalArgumentException if a value does not fit Rubato's rationals (D8)
     */
    public static Denotator toDenotator(RhythmTree t) {
        try {
            return encode(t);
        }
        catch (RubatoException e) {
            throw new IllegalArgumentException("could not build the RhythmTree denotator: " + e.getMessage());
        }
    }

    private static Denotator encode(RhythmTree t) throws RubatoException {
        LimitForm treeForm = treeForm();
        LimitForm labelForm = (LimitForm) rep().getForm(RHYTHM_LABEL);
        SimpleForm durationForm = (SimpleForm) rep().getForm(RHYTHM_DURATION);
        SimpleForm factorForm = (SimpleForm) rep().getForm(RHYTHM_FACTOR);
        ListForm factorsForm = (ListForm) rep().getForm(RHYTHM_FACTORS);
        ListForm subtreesForm = (ListForm) rep().getForm(RHYTHM_SUBTREES);
        List<Denotator> fs = new ArrayList<Denotator>();
        for (Fraction r : t.getFactors()) {
            fs.add(new SimpleDenotator(null, factorForm, new QElement(r.toRational())));
        }
        List<Denotator> label = new ArrayList<Denotator>();
        label.add(new SimpleDenotator(null, durationForm, new QElement(t.getLabel().toRational())));
        label.add(new ListDenotator(null, factorsForm, fs));
        List<Denotator> subs = new ArrayList<Denotator>();
        for (RhythmTree c : t.getChildren()) {
            subs.add(encode(c));
        }
        List<Denotator> node = new ArrayList<Denotator>();
        node.add(new LimitDenotator(null, labelForm, label));
        node.add(new ListDenotator(null, subtreesForm, subs));
        return new LimitDenotator(null, treeForm, node);
    }

    /**
     * Reads a RhythmTree denotator.
     *
     * @throws IllegalArgumentException if d is not a RhythmTree denotator, has
     *         a node whose factors and subtrees differ in number, or breaks
     *         D5 or D8
     */
    public static RhythmTree fromDenotator(Denotator d) {
        int[] count = { 0 };
        return decode(d, 0, count).check();
    }

    private static RhythmTree decode(Denotator d, int depth, int[] count) {
        if (d == null || d.getForm() != treeForm()) {
            throw new IllegalArgumentException("not a RhythmTree denotator"
                                               + (d == null ? "" : ": its form is " + d.getForm().getNameString()));
        }
        if (!d.nullAddressed()) {
            throw new IllegalArgumentException("the RhythmTree denotator must be null-addressed");
        }
        if (depth > RhythmTree.MAX_DEPTH) {
            throw new IllegalArgumentException("the tree has depth above " + RhythmTree.MAX_DEPTH);
        }
        if (++count[0] > RhythmTree.MAX_NODES) {
            throw new IllegalArgumentException("the tree has more than " + RhythmTree.MAX_NODES + " nodes");
        }
        LimitDenotator node = (LimitDenotator) d;
        LimitDenotator label = (LimitDenotator) node.getFactor(0);
        Fraction duration = value(label.getFactor(0));
        List<Fraction> factors = new ArrayList<Fraction>();
        for (Denotator f : ((ListDenotator) label.getFactor(1)).getFactors()) {
            factors.add(value(f));
        }
        List<RhythmTree> children = new ArrayList<RhythmTree>();
        for (Denotator c : ((ListDenotator) node.getFactor(1)).getFactors()) {
            children.add(decode(c, depth + 1, count));
        }
        return new RhythmTree(duration, factors, children);
    }

    private static Fraction value(Denotator d) {
        return Fraction.of(((QElement) ((SimpleDenotator) d).getElement()).getValue());
    }
}
