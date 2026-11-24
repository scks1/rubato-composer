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
import java.util.Collections;
import java.util.List;

/**
 * A rhythm tree after Flieder, arXiv 2512.04090, Ex. C.1 (pp. 55-56): a term
 * sup((d, [r1, ..., rn]), f) of the W-type with labels A = Q x List(Q) and
 * arities B(d, [r1, ..., rn]) = Fin(n). A node holds its label d, its
 * proportional factors r1..rn and exactly n subtrees, subtree k hanging from
 * the branch labelled rk. Instances are immutable.
 * <p>
 * Departures (sources/RHYTHM-TREES-SPEC.md): labels and factors are greater
 * than 0 (D5); {@link #check} bounds the size (D8). How a tree becomes
 * onsets and durations is {@link RhythmLayout}'s job (D1, D2).
 */
public final class RhythmTree {

    /** Most nodes a tree may have (D8). */
    public static final int MAX_NODES = 4096;
    /** Greatest depth a tree may have, the root being depth 0 (D8). */
    public static final int MAX_DEPTH = 64;

    private final Fraction label;
    private final List<Fraction> factors;
    private final List<RhythmTree> children;
    private final int nodes;
    private final int depth;

    /**
     * The node (label, factors) with the given subtrees.
     *
     * @throws IllegalArgumentException unless there is one subtree per factor
     *         and label and factors are greater than 0
     */
    public RhythmTree(Fraction label, List<Fraction> factors, List<RhythmTree> children) {
        if (label == null || factors == null || children == null) {
            throw new IllegalArgumentException("a rhythm tree needs a label, factors and subtrees");
        }
        if (factors.size() != children.size()) {
            throw new IllegalArgumentException("a node with " + factors.size() + " factor" + (factors.size() == 1 ? "" : "s")
                                               + " but " + children.size() + " subtree" + (children.size() == 1 ? "" : "s")
                                               + "; Ex. C.1 needs one subtree per factor");
        }
        positive("label", label);
        int n = 1;
        int dp = 0;
        for (int k = 0; k < factors.size(); k++) {
            positive("factor", factors.get(k));
            RhythmTree c = children.get(k);
            if (c == null) {
                throw new IllegalArgumentException("subtree " + k + " is missing");
            }
            n += c.nodes;
            dp = Math.max(dp, c.depth + 1);
        }
        this.label = label;
        this.factors = Collections.unmodifiableList(new ArrayList<Fraction>(factors));
        this.children = Collections.unmodifiableList(new ArrayList<RhythmTree>(children));
        this.nodes = n;
        this.depth = dp;
    }

    private static void positive(String what, Fraction f) {
        if (f == null || f.signum() <= 0) {
            throw new IllegalArgumentException("a " + what + " must be greater than 0, not " + f + " (spec D5)");
        }
    }

    /** A node with no branches. */
    public static RhythmTree leaf(Fraction label) {
        return new RhythmTree(label, Collections.<Fraction>emptyList(), Collections.<RhythmTree>emptyList());
    }

    /**
     * Returns the tree if it is within {@link #MAX_NODES} and {@link #MAX_DEPTH}.
     *
     * @throws IllegalArgumentException otherwise
     */
    public RhythmTree check() {
        if (nodes > MAX_NODES) {
            throw new IllegalArgumentException("the tree has " + nodes + " nodes; at most " + MAX_NODES + " are allowed");
        }
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("the tree has depth " + depth + "; at most " + MAX_DEPTH + " is allowed");
        }
        return this;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /** The node's own label d (a duration in the paper's words). */
    public Fraction getLabel() {
        return label;
    }

    /** The proportional factors r1..rn, unmodifiable. */
    public List<Fraction> getFactors() {
        return factors;
    }

    /** The subtrees, one per factor, unmodifiable. */
    public List<RhythmTree> getChildren() {
        return children;
    }

    /** The sum r1 + ... + rn (0 for a leaf). */
    public Fraction factorSum() {
        Fraction s = Fraction.ZERO;
        for (Fraction r : factors) {
            s = s.add(r);
        }
        return s;
    }

    public int nodeCount() {
        return nodes;
    }

    /** The number of edges on the longest path from this node down to a leaf. */
    public int depth() {
        return depth;
    }

    /**
     * The tree in the notation of {@link RhythmNotation} (spec D3): the root as
     * its label, a child as its factor, or factor@label when its own label
     * differs from its factor, and branches in parentheses.
     */
    public String toNotation() {
        StringBuilder b = new StringBuilder(label.toString());
        appendChildren(b);
        return b.toString();
    }

    private void appendChildren(StringBuilder b) {
        if (children.isEmpty()) {
            return;
        }
        b.append(" (");
        for (int k = 0; k < children.size(); k++) {
            if (k > 0) {
                b.append(' ');
            }
            RhythmTree c = children.get(k);
            b.append(factors.get(k));
            if (!c.label.equals(factors.get(k))) {
                b.append('@').append(c.label);
            }
            c.appendChildren(b);
        }
        b.append(')');
    }

    public boolean equals(Object o) {
        if (!(o instanceof RhythmTree)) {
            return false;
        }
        RhythmTree t = (RhythmTree) o;
        return label.equals(t.label) && factors.equals(t.factors) && children.equals(t.children);
    }

    public int hashCode() {
        return (label.hashCode() * 31 + factors.hashCode()) * 31 + children.hashCode();
    }

    public String toString() {
        return toNotation();
    }
}
