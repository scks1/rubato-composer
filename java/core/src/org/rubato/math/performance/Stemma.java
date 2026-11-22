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
package org.rubato.math.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A stemma (Mazzola, <i>The Topos of Music II</i>, Defs. 106-107, p. 628): a finite LPS whose
 * daughter graph is a tree, whose mother is not among its own daughters, and which has leaves.
 * The performed piece is the union of the leaves' performances; this package requires the
 * leaves' kernels to partition the mother's kernel (departure D9).
 */
public final class Stemma {

    private final LPS mother;

    public Stemma(LPS mother) {
        this.mother = mother;
    }

    public LPS getMother() { return mother; }

    /** Def. 107. Throws IllegalStateException naming the first violation. */
    public void validate() {
        Map<LPS, Boolean> seen = new IdentityHashMap<LPS, Boolean>();
        walk(mother, seen);
        if (leaves().isEmpty()) {
            throw new IllegalStateException("the stemma has no leaves");
        }
    }

    private static void walk(LPS n, Map<LPS, Boolean> seen) {
        if (seen.put(n, Boolean.TRUE) != null) {
            throw new IllegalStateException("node " + n.getName() + " is reached twice: the daughter graph is not a tree");
        }
        for (LPS d : n.getDaughters()) {
            if (d == n) {
                throw new IllegalStateException("node " + n.getName() + " lists itself among its daughters");
            }
            walk(d, seen);
        }
    }

    /** The number of nodes (after {@link #validate}). */
    public int size() {
        validate();
        return count(mother);
    }

    private static int count(LPS n) {
        int c = 1;
        for (LPS d : n.getDaughters()) {
            c += count(d);
        }
        return c;
    }

    /** The number of edges, size - 1 for a tree. */
    public int edges() {
        validate();
        return edgeCount(mother);
    }

    private static int edgeCount(LPS n) {
        int c = n.getDaughters().size();
        for (LPS d : n.getDaughters()) {
            c += edgeCount(d);
        }
        return c;
    }

    /** The longest chain of operators from the mother to a leaf. */
    public int depth() {
        validate();
        return depth(mother);
    }

    private static int depth(LPS n) {
        int m = 0;
        for (LPS d : n.getDaughters()) {
            m = Math.max(m, 1 + depth(d));
        }
        return m;
    }

    /** The leaves in depth-first order. Does not validate, so it terminates only on finite trees. */
    public List<LPS> leaves() {
        List<LPS> r = new ArrayList<LPS>();
        collect(mother, r, new IdentityHashMap<LPS, Boolean>());
        return r;
    }

    private static void collect(LPS n, List<LPS> r, Map<LPS, Boolean> seen) {
        if (seen.put(n, Boolean.TRUE) != null) {
            return;
        }
        if (n.getDaughters().isEmpty()) {
            r.add(n);
        }
        for (LPS d : n.getDaughters()) {
            collect(d, r, seen);
        }
    }

    /**
     * The performed piece: each leaf performs its kernel with its own curves, physical time 0
     * at the smallest symbolic onset of the mother, sorted by onset.
     */
    public List<PerformedNote> perform() {
        validate();
        double origin = Double.POSITIVE_INFINITY;
        for (PerfNote n : mother.getKernel()) {
            origin = Math.min(origin, n.onset);
        }
        if (mother.getKernel().isEmpty()) {
            origin = 0;
        }
        Map<PerfNote, LPS> owner = new IdentityHashMap<PerfNote, LPS>();
        for (LPS leaf : leaves()) {
            for (PerfNote n : leaf.getKernel()) {
                if (owner.put(n, leaf) != null) {
                    throw new IllegalStateException("leaves " + owner.get(n).getName() + " and " + leaf.getName()
                            + " share a note");
                }
            }
        }
        Map<PerfNote, Boolean> motherNotes = new IdentityHashMap<PerfNote, Boolean>();
        for (PerfNote n : mother.getKernel()) {
            motherNotes.put(n, Boolean.TRUE);
            if (!owner.containsKey(n)) {
                throw new IllegalStateException("note " + n + " of the mother is in no leaf");
            }
        }
        if (owner.size() != motherNotes.size()) {
            throw new IllegalStateException("a leaf performs notes that are not in the mother's kernel");
        }
        List<PerformedNote> r = new ArrayList<PerformedNote>();
        for (PerfNote n : mother.getKernel()) {
            r.add(owner.get(n).perform(n, origin));
        }
        Collections.sort(r, (x, y) -> Double.compare(x.onset, y.onset));
        return r;
    }
}
