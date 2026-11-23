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

package org.rubato.math.module.morphism.test;

import junit.framework.TestCase;

import org.rubato.math.matrix.RMatrix;
import org.rubato.math.module.Module;
import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.RProperFreeElement;
import org.rubato.math.module.RProperFreeModule;
import org.rubato.math.module.ZProperFreeElement;
import org.rubato.math.module.ZProperFreeModule;
import org.rubato.math.module.morphism.ConstantMorphism;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.RFreeAffineMorphism;

/**
 * f.compose(g) is h(x) = f(g(x)), so h has g's domain and f's codomain.
 * CompositionMorphism.make built its constants on f's codomain instead
 * (stage-1 open choice 5, fixed in stage 8).
 */
public class CompositionMorphismTest extends TestCase {

    private static final Module R2 = RProperFreeModule.make(2);
    private static final Module R3 = RProperFreeModule.make(3);
    private static final Module Z3 = ZProperFreeModule.make(3);

    /** (x, y, z) -> (x + 2y, z - 1), a non-constant map R^3 -> R^2. */
    private static ModuleMorphism affineR3toR2() {
        return RFreeAffineMorphism.make(new RMatrix(new double[][] { { 1, 2, 0 }, { 0, 0, 1 } }), new double[] { 0, -1 });
    }

    /** (x, y) -> (y, x + 1), a non-constant map R^2 -> R^2. */
    private static ModuleMorphism affineR2toR2() {
        return RFreeAffineMorphism.make(new RMatrix(new double[][] { { 0, 1 }, { 1, 0 } }), new double[] { 0, 1 });
    }

    /** Constant f after a non-constant g. */
    public void testConstantAfterMap() throws Exception {
        ModuleElement five = RProperFreeElement.make(new double[] { 5, 6 });
        ModuleMorphism f = new ConstantMorphism(R2, five);
        ModuleMorphism g = affineR3toR2();
        ModuleMorphism h = f.compose(g);
        assertEquals(R3, h.getDomain());
        assertEquals(R2, h.getCodomain());
        assertTrue(h.isConstant());
        assertEquals(five, h.map(RProperFreeElement.make(new double[] { 1, 2, 3 })));
    }

    /** Non-constant f after a constant g whose domain differs from both of f's modules. */
    public void testMapAfterConstant() throws Exception {
        ModuleMorphism g = new ConstantMorphism(Z3, RProperFreeElement.make(new double[] { 3, 4 }));
        ModuleMorphism f = affineR2toR2();
        ModuleMorphism h = f.compose(g);
        assertEquals(Z3, h.getDomain());
        assertEquals(R2, h.getCodomain());
        assertTrue(h.isConstant());
        assertEquals(RProperFreeElement.make(new double[] { 4, 4 }), h.map(ZProperFreeElement.make(new int[] { 1, 1, 1 })));
    }

    /** The composite can be summed with another map on g's domain. */
    public void testCompositeSumsOnTheDomainOfG() throws Exception {
        ModuleMorphism g = new ConstantMorphism(Z3, RProperFreeElement.make(new double[] { 3, 4 }));
        ModuleMorphism h = affineR2toR2().compose(g);
        ModuleMorphism other = new ConstantMorphism(Z3, RProperFreeElement.make(new double[] { 1, 1 }));
        ModuleMorphism sum = h.sum(other);
        assertEquals(RProperFreeElement.make(new double[] { 5, 5 }), sum.map(Z3.getZero()));
    }

    /** Constant after constant: g's domain, f's value. */
    public void testConstantAfterConstant() throws Exception {
        ModuleMorphism g = new ConstantMorphism(Z3, RProperFreeElement.make(new double[] { 3, 4, 5 }));
        ModuleElement seven = RProperFreeElement.make(new double[] { 7, 7 });
        ModuleMorphism f = new ConstantMorphism(R3, seven);
        ModuleMorphism h = f.compose(g);
        assertEquals(Z3, h.getDomain());
        assertEquals(seven, h.map(Z3.getZero()));
    }
}
