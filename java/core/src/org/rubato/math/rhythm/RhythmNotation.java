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

/**
 * Reads rhythm trees from text (sources/RHYTHM-TREES-SPEC.md D3):
 * <pre>
 *   tree  := label [ "(" child+ ")" ]
 *   child := factor [ "@" label ] [ "(" child+ ")" ]
 * </pre>
 * The root gives its label d. A child gives its factor r, and its own label
 * d' is r unless written as r@d'. Numbers are integers, decimals or
 * fractions such as 19/2, read exactly. Figure 2 of Flieder, arXiv
 * 2512.04090 (p. 56) reads {@code 9.5 (2 2.5 (1 (2 1) 1 1) 3 (1.5 2))}.
 * Error messages give 1-based character positions.
 */
public final class RhythmNotation {

    private final String text;
    private int pos;
    private int nodes;

    private RhythmNotation(String text) {
        this.text = text;
    }

    /**
     * Parses a whole tree.
     *
     * @throws IllegalArgumentException with the position of the first error
     */
    public static RhythmTree parse(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("the rhythm tree is empty");
        }
        RhythmNotation p = new RhythmNotation(s);
        p.skip();
        int at = p.pos;
        String[] label = p.number();
        if (label[1] != null) {
            throw new IllegalArgumentException("position " + (at + 1) + ": the root has a label only, no factor@label");
        }
        RhythmTree t = p.node(p.value(label[0], at), 0);
        p.skip();
        if (p.pos < s.length()) {
            throw new IllegalArgumentException("position " + (p.pos + 1) + ": \"" + s.charAt(p.pos) + "\" after the tree");
        }
        return t.check();
    }

    /** The rest of a node whose label has been read: its branches, if any. */
    private RhythmTree node(Fraction label, int depth) {
        if (depth > RhythmTree.MAX_DEPTH) {
            throw new IllegalArgumentException("the tree has depth above " + RhythmTree.MAX_DEPTH + "; at most "
                                               + RhythmTree.MAX_DEPTH + " is allowed");
        }
        if (++nodes > RhythmTree.MAX_NODES) {
            throw new IllegalArgumentException("the tree has more than " + RhythmTree.MAX_NODES + " nodes");
        }
        List<Fraction> factors = new ArrayList<Fraction>();
        List<RhythmTree> children = new ArrayList<RhythmTree>();
        skip();
        if (pos < text.length() && text.charAt(pos) == '(') {
            int open = pos;
            pos++;
            while (true) {
                skip();
                if (pos >= text.length()) {
                    throw new IllegalArgumentException("position " + (open + 1) + ": this \"(\" has no matching \")\" (missing ))");
                }
                char c = text.charAt(pos);
                if (c == ')') {
                    if (factors.isEmpty()) {
                        throw new IllegalArgumentException("position " + (open + 1) + ": \"()\" has no branches; leave the parentheses out for a leaf");
                    }
                    pos++;
                    break;
                }
                int at = pos;
                String[] fl = number();
                Fraction r = value(fl[0], at);
                Fraction own = fl[1] == null ? r : value(fl[1], at + fl[0].length() + 1);
                factors.add(r);
                children.add(node(own, depth + 1));
            }
        }
        try {
            return new RhythmTree(label, factors, children);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("position " + (pos + 1) + ": " + e.getMessage());
        }
    }

    private void skip() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    /** The next token split at '@': { factor or label, own label or null }. */
    private String[] number() {
        int start = pos;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (Character.isWhitespace(c) || c == '(' || c == ')') {
                break;
            }
            pos++;
        }
        if (pos == start) {
            throw new IllegalArgumentException("position " + (start + 1) + ": a number is expected"
                                               + (start < text.length() ? " before \"" + text.charAt(start) + "\"" : " at the end"));
        }
        String tok = text.substring(start, pos);
        int at = tok.indexOf('@');
        if (at < 0) {
            return new String[] { tok, null };
        }
        return new String[] { tok.substring(0, at), tok.substring(at + 1) };
    }

    private Fraction value(String s, int at) {
        Fraction f;
        try {
            f = Fraction.parse(s);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("position " + (at + 1) + ": " + e.getMessage());
        }
        if (f.signum() <= 0) {
            throw new IllegalArgumentException("position " + (at + 1) + ": " + s + " must be greater than 0 (spec D5)");
        }
        return f;
    }
}
