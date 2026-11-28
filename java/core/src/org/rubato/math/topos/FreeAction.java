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
import java.util.Arrays;
import java.util.List;

/**
 * The free Mon(C)-set &iota;<sub>!</sub>X on a finite functor
 * X : C -&gt; FinSet (arXiv 2611.32655, Prop. 2.5), with its core and the
 * adjunction of Thm. 3.1.
 * <p>
 * By Prop. 2.5 the elements are the triples [w, x] with x in X(c) and w
 * in W_c; this class calls c the type of the element. A monoid element m
 * acts by factoring m w = w' g with g : c -&gt; c' and w' in W_c' (Lemma
 * 2.4) and returning [w', X(g)(x)]. The roots [1, x] form the core, the
 * image of the fully faithful functor V of Thm. 3.1; the other elements
 * lie on branches.
 * <p>
 * The set is infinite whenever some W_c with X(c) nonempty is, so every
 * listing is a window up to a chosen word length, and
 * {@link #describeWindow(int)} says whether the window is truncated or
 * complete.
 */
public final class FreeAction {

    /** An element [w, x] of &iota;<sub>!</sub>X with x in X(type) and w in W_type. */
    public static final class Element {
        public final int type;
        public final int[] word;
        public final int x;

        Element(int type, int[] word, int x) {
            this.type = type;
            this.word = word;
            this.x = x;
        }

        /** True iff the element lies in the core: its word is the unit. */
        public boolean isRoot() {
            return word.length == 0;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Element)) {
                return false;
            }
            Element e = (Element) o;
            return type == e.type && x == e.x && Arrays.equals(word, e.word);
        }

        @Override
        public int hashCode() {
            return (31 * type + x) * 31 + Arrays.hashCode(word);
        }

        @Override
        public String toString() {
            return "[" + Arrays.toString(word) + ", " + type + ":" + x + "]";
        }
    }

    private final FiniteFunctor functor;
    private final UniversalMonoid monoid;

    public FreeAction(FiniteFunctor x) {
        functor = x;
        monoid = new UniversalMonoid(x.category());
    }

    public FiniteFunctor functor() {
        return functor;
    }

    public UniversalMonoid monoid() {
        return monoid;
    }

    /** The root [1, x] for x in X(c), the image of (c, x) under V (Thm. 3.1). */
    public Element root(int c, int x) {
        if (x < 0 || x >= functor.size(c)) {
            throw new IllegalArgumentException("no element " + x + " at " + c);
        }
        return new Element(c, new int[0], x);
    }

    /** m . [w, x] by Prop. 2.5: factor m w = w' g and return [w', X(g)(x)]. */
    public Element act(int[] m, Element e) {
        UniversalMonoid.Factorization f = monoid.factor(monoid.multiply(m, e.word), e.type);
        return new Element(functor.category().cod(f.g), f.w, functor.act(f.g, e.x));
    }

    /**
     * True iff the letter sends the element into the core. For a root
     * [1, x] of type c this happens exactly when dom u = c, and then
     * u . [1, x] = [1, X(u)(x)]; the name follows the paper's usage in
     * Example 2.7 and Example 4.9.
     */
    public boolean absorbs(Element e, int letter) {
        return act(new int[] { letter }, e).isRoot();
    }

    /** The component w : [1, x] -&gt; [w, x] of the counit at [w, x] (Thm. 3.1). */
    public int[] counit(Element e) {
        return e.word.clone();
    }

    /** VR[w, x] = [1, x], the root under the element (Thm. 3.1). */
    public Element rootOf(Element e) {
        return root(e.type, e.x);
    }

    /**
     * R on arrows (Thm. 3.1): the arrow m : [w, x] -&gt; m.[w, x] of the
     * translation category goes to the unique g with m w = w' g, an arrow
     * (c, x) -&gt; (c', X(g)(x)) of the category of elements.
     */
    public int rootArrow(int[] m, Element e) {
        return monoid.factor(monoid.multiply(m, e.word), e.type).g;
    }

    /**
     * The arrows m : [1, x] -&gt; [w', x'] of the translation category, as
     * the words w' g for the arrows g : (c, x) -&gt; (c', x') of the category
     * of elements (the hom bijection of Thm. 3.1).
     *
     * @param root a root [1, x]
     * @param target any element [w', x']
     */
    public List<int[]> coreArrows(Element root, Element target) {
        if (!root.isRoot()) {
            throw new IllegalArgumentException("the source must be a root");
        }
        FiniteCategory c = functor.category();
        List<int[]> out = new ArrayList<int[]>();
        for (int g : c.hom(root.type, target.type)) {
            if (functor.act(g, root.x) == target.x) {
                out.add(monoid.multiply(target.word, monoid.normalize(g)));
            }
        }
        return out;
    }

    /** Every element [w, x] with w of length at most maxLength, by type, then x, then word. */
    public List<Element> window(int maxLength) {
        List<Element> out = new ArrayList<Element>();
        FiniteCategory c = functor.category();
        for (int o = 0; o < c.objectCount(); o++) {
            if (functor.size(o) == 0) {
                continue;
            }
            List<int[]> ws = monoid.wordsW(o, maxLength);
            for (int x = 0; x < functor.size(o); x++) {
                for (int[] w : ws) {
                    out.add(new Element(o, w, x));
                }
            }
        }
        return out;
    }

    /** True iff &iota;<sub>!</sub>X is finite: W_c is finite for every c with X(c) nonempty. */
    public boolean isFinite() {
        FiniteCategory c = functor.category();
        for (int o = 0; o < c.objectCount(); o++) {
            if (functor.size(o) > 0 && !monoid.isWFinite(o)) {
                return false;
            }
        }
        return true;
    }

    /** The element as "[w, c:x]" with arrow and object names. */
    public String format(Element e) {
        return "[" + monoid.format(e.word) + ", " + functor.category().objectName(e.type) + ":" + e.x + "]";
    }

    /**
     * A listing of the window up to word length maxLength. The first line
     * says whether the window is truncated (the action is infinite) or
     * complete; each element line marks roots as core and gives the counit
     * of a branch element.
     */
    public String describeWindow(int maxLength) {
        List<Element> win = window(maxLength);
        int roots = 0;
        for (Element e : win) {
            if (e.isRoot()) {
                roots++;
            }
        }
        StringBuilder b = new StringBuilder();
        boolean finite = isFinite();
        boolean complete = finite && win.size() == window(maxLength + 1).size();
        if (complete) {
            b.append("complete listing: the free action is finite, ").append(win.size()).append(" elements, ");
        } else {
            b.append("truncated window of the free action (words of length <= ").append(maxLength)
             .append(finite ? "; the action is finite but longer words remain" : "; the action is infinite")
             .append("): ").append(win.size()).append(" elements, ");
        }
        b.append(roots).append(" roots in the core, ").append(win.size() - roots).append(" on branches\n");
        for (Element e : win) {
            b.append("  ").append(format(e));
            if (e.isRoot()) {
                b.append("  root");
            } else {
                b.append("  counit ").append(monoid.format(e.word)).append(" : ").append(format(rootOf(e)))
                 .append(" -> ").append(format(e));
            }
            b.append('\n');
        }
        return b.toString();
    }
}
