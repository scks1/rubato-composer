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
 * The exact time span of every node of a rhythm tree. Flieder (arXiv
 * 2512.04090, Ex. C.1) does not say whether a child's length comes from its
 * parent's factor r_k or from its own label d', so both readings are here
 * (sources/RHYTHM-TREES-SPEC.md):
 * <ul>
 * <li>{@link Timing#PROPORTIONAL} (D1, the default): the root lasts its label;
 *     child k of a node lasting s lasts s * r_k / (r_1 + ... + r_n).</li>
 * <li>{@link Timing#LABEL} (D2): every node lasts its own label.</li>
 * </ul>
 * In both, children follow one another from their parent's onset.
 */
public final class RhythmLayout {

    /** Which value sets a child's length. */
    public enum Timing {
        /** The parent's length shared in proportion to the factors (D1). */
        PROPORTIONAL("proportional"),
        /** The child's own label (D2). */
        LABEL("label");

        private final String word;

        Timing(String word) {
            this.word = word;
        }

        public String word() {
            return word;
        }

        /** The timing named by its word, or null. */
        public static Timing of(String word) {
            for (Timing t : values()) {
                if (t.word.equalsIgnoreCase(word.trim())) {
                    return t;
                }
            }
            return null;
        }
    }

    /** One node's place in time. */
    public static final class Span {
        /** The node itself. */
        public final RhythmTree node;
        /** Child indexes from the root, joined by dots; "" for the root. */
        public final String path;
        /** 0 for the root. */
        public final int depth;
        public final Fraction onset;
        public final Fraction length;
        /** The sum of the children's lengths (0 for a leaf). */
        public final Fraction childrenTotal;

        Span(RhythmTree node, String path, int depth, Fraction onset, Fraction length, Fraction childrenTotal) {
            this.node = node;
            this.path = path;
            this.depth = depth;
            this.onset = onset;
            this.length = length;
            this.childrenTotal = childrenTotal;
        }

        public Fraction end() {
            return onset.add(length);
        }

        public boolean isLeaf() {
            return node.isLeaf();
        }
    }

    private final Timing timing;
    private final List<Span> spans = new ArrayList<Span>();
    private final List<Span> leaves = new ArrayList<Span>();
    private final List<Span> mismatches = new ArrayList<Span>();
    private Fraction end;

    private RhythmLayout(Timing timing, Fraction start) {
        this.timing = timing;
        this.end = start;
    }

    /** Lays the tree out from the start onset. */
    public static RhythmLayout layout(RhythmTree t, Timing timing, Fraction start) {
        RhythmLayout l = new RhythmLayout(timing, start);
        l.place(t, "", 0, start, t.getLabel());
        for (Span s : l.spans) {
            if (!s.isLeaf() && !s.childrenTotal.equals(s.length)) {
                l.mismatches.add(s);
            }
        }
        return l;
    }

    private void place(RhythmTree t, String path, int depth, Fraction onset, Fraction length) {
        int at = spans.size();
        spans.add(null);
        Fraction total = Fraction.ZERO;
        Fraction cursor = onset;
        Fraction sum = t.factorSum();
        List<RhythmTree> kids = t.getChildren();
        for (int k = 0; k < kids.size(); k++) {
            RhythmTree c = kids.get(k);
            Fraction l = timing == Timing.PROPORTIONAL
                ? length.multiply(t.getFactors().get(k)).divide(sum)
                : c.getLabel();
            place(c, path.isEmpty() ? Integer.toString(k) : path + "." + k, depth + 1, cursor, l);
            cursor = cursor.add(l);
            total = total.add(l);
        }
        Span s = new Span(t, path, depth, onset, length, total);
        spans.set(at, s);
        if (t.isLeaf()) {
            leaves.add(s);
        }
        if (s.end().compareTo(end) > 0) {
            end = s.end();
        }
        if (cursor.compareTo(end) > 0) {
            end = cursor;
        }
    }

    public Timing timing() {
        return timing;
    }

    /** Every node, root first, in preorder. */
    public List<Span> spans() {
        return Collections.unmodifiableList(spans);
    }

    /** The leaves in tree order (D6: these become the notes). */
    public List<Span> leaves() {
        return Collections.unmodifiableList(leaves);
    }

    /**
     * Inner nodes whose children's lengths do not add up to the node's own
     * length, in preorder; always empty in proportional timing.
     */
    public List<Span> mismatches() {
        return Collections.unmodifiableList(mismatches);
    }

    /** The latest end of any node. */
    public Fraction end() {
        return end;
    }
}
