package org.rubato.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.logeo.FormFactory;
import org.rubato.math.module.Module;
import org.rubato.math.module.RElement;
import org.rubato.math.module.RRing;
import org.rubato.math.module.ZElement;
import org.rubato.math.module.ZProperFreeElement;
import org.rubato.math.module.ZProperFreeModule;
import org.rubato.math.module.ZRing;
import org.rubato.math.module.morphism.CanonicalMorphism;
import org.rubato.math.module.morphism.ConstantMorphism;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.RAffineMorphism;
import org.rubato.math.module.morphism.ZAffineMorphism;
import org.rubato.math.yoneda.*;

/**
 * Denotators at non-zero addresses, checked against the definitions in
 * Mazzola, The Topos of Music I (2nd ed.), sections 6.2.1 and 6.3.1: a point
 * at address A is a morphism A to the coordinate module, an address change
 * along f: B to A precomposes with f, and the coordinates of a Power denotator
 * change pointwise. The book's running example is the Z-addressed pitch
 * e_a.b, the affine map x to b*x + a, with tail a and head a + b.
 */
public class AddressTest extends TestCase {

	private static final Module Z = ZRing.ring;
	private static final Module ZERO = ZProperFreeModule.nullModule;
	private static final SimpleForm PITCH = FormFactory.makeZModuleForm("AddressTestPitch");
	private static final SimpleForm ONSET = FormFactory.makeZModuleForm("AddressTestOnset");
	private static final PowerForm CHORD = FormFactory.makePowerForm("AddressTestChord", PITCH);
	private static final ListForm MELODY = FormFactory.makeListForm("AddressTestMelody", PITCH);
	private static final LimitForm NOTE = FormFactory.makeLimitForm("AddressTestNote", ONSET, PITCH);
	private static final ColimitForm EITHER = FormFactory.makeColimitForm("AddressTestEither", PITCH, ONSET);

	/** The Z-addressed point e_a.b: x maps to b*x + a. */
	private static SimpleDenotator arrow(SimpleForm form, int a, int b) throws Exception {
		return new SimpleDenotator(null, form, new ZAffineMorphism(b, a));
	}

	private static int valueAt(Denotator d, int x) throws Exception {
		return ((ZElement)((SimpleDenotator)d).getElement(new ZElement(x))).getValue();
	}

	private static int value(Denotator d) {
		return ((ZElement)((SimpleDenotator)d).getElement()).getValue();
	}

	private static List<Denotator> list(Denotator... ds) {
		return new ArrayList<Denotator>(Arrays.asList(ds));
	}

	private static String pitches(PowerDenotator p) throws Exception {
		StringBuilder b = new StringBuilder();
		for (Denotator d : p.getFactors()) {
			b.append(d.getAddress().isNullModule() ? String.valueOf(value(d)) : valueAt(d, 0) + "=>" + valueAt(d, 1)).append(' ');
		}
		return b.toString().trim();
	}

	// The book's example, p. 53-54. These held before this change and guard it.

	public void testArrowTailAndHeadAreTheValuesAtZeroAndOne() throws Exception {
		SimpleDenotator fSharp = arrow(PITCH, 65, 1);
		SimpleDenotator gFlat = arrow(PITCH, 67, -1);
		assertEquals(65, value(fSharp.at(new ZElement(0))));
		assertEquals(66, value(fSharp.at(new ZElement(1))));
		assertEquals(67, value(gFlat.at(new ZElement(0))));
		assertEquals(66, value(gFlat.at(new ZElement(1))));
		assertFalse(fSharp.equals(gFlat));
	}

	public void testZeroAddressedPointLiftsToTheZeroLengthArrow() throws Exception {
		SimpleDenotator c = new SimpleDenotator(null, PITCH, new ZElement(66));
		Denotator lifted = c.changeAddress(Z);
		assertEquals(Z, lifted.getAddress());
		assertEquals(66, valueAt(lifted, 0));
		assertEquals(66, valueAt(lifted, 1));
	}

	public void testAddressChangeIsFunctorial() throws Exception {
		SimpleDenotator d = arrow(PITCH, 60, 3);
		ModuleMorphism g = new ZAffineMorphism(2, 1);
		ModuleMorphism h = new ZAffineMorphism(-1, 4);
		assertEquals(d.changeAddress(g.compose(h)), d.changeAddress(g).changeAddress(h));
		assertEquals(d, d.changeAddress(new ZAffineMorphism(1, 0)));
	}

	// Address change along a constant, or of a constant. The new address is
	// the domain of the morphism (6.2.2). CompositionMorphism.make gives a
	// composite with a constant the codomain of f as its domain, so the result
	// kept the old address; ModuleMorphismMap.changeDomain now builds it.

	public void testAConstantChangesToTheDomainOfTheMorphism() throws Exception {
		Denotator c = new SimpleDenotator(null, PITCH, new ZElement(66)).changeAddress(Z);
		Denotator atZero = c.changeAddress(new ConstantMorphism(ZERO, new ZElement(5)));
		assertTrue(atZero.nullAddressed());
		assertEquals(66, value(atZero));
		Module z2 = ZProperFreeModule.make(2);
		Denotator atZ2 = c.changeAddress(ModuleMorphism.getConstantMorphism(z2, new ZElement(1)));
		assertEquals(z2, atZ2.getAddress());
		assertEquals(new ZElement(66), ((SimpleDenotator)atZ2).getElement(ZProperFreeElement.make(new int[]{3, 4})));
	}

	public void testEvaluationIsTheAddressChangeAlongAConstant() throws Exception {
		SimpleDenotator d = arrow(PITCH, 60, 3);
		Denotator viaAt = d.at(new ZElement(2));
		Denotator viaChange = d.changeAddress(new ConstantMorphism(ZERO, new ZElement(2)));
		assertTrue(viaChange.nullAddressed());
		assertEquals(66, value(viaChange));
		assertEquals(viaAt, viaChange);
	}

	// Evaluation creates a new denotator; AutoListMorphismMap.at overwrote
	// the factors of the Power denotator it was called on.

	public void testEvaluatingAPowerLeavesItUnchanged() throws Exception {
		PowerDenotator chord = new PowerDenotator(null, Z, CHORD, list(arrow(PITCH, 60, 1), arrow(PITCH, 64, -1)));
		String before = pitches(chord);
		PowerDenotator head = (PowerDenotator)chord.at(new ZElement(1));
		assertEquals(before, pitches(chord));
		assertEquals(Z, chord.getAddress());
		for (Denotator d : chord.getFactors()) {
			assertEquals(Z, d.getAddress());
		}
		assertEquals("61 63", pitches(head));
	}

	public void testEvaluatingAListLeavesItUnchanged() throws Exception {
		ListDenotator m = new ListDenotator(null, Z, MELODY, list(arrow(PITCH, 64, 1), arrow(PITCH, 60, 1)));
		m.at(new ZElement(1));
		assertEquals(64, valueAt(m.getFactor(0), 0));
		assertEquals(60, valueAt(m.getFactor(1), 0));
	}

	// An evaluated denotator is null-addressed (CompoundMorphism.at says so),
	// also when it has no factors.

	public void testEmptyPowerEvaluatesToTheZeroAddress() throws Exception {
		PowerDenotator empty = new PowerDenotator(null, Z, CHORD, new ArrayList<Denotator>());
		Denotator e = empty.at(new ZElement(0));
		assertTrue(e.nullAddressed());
		assertTrue(empty.atNull().nullAddressed());
		assertEquals(Z, empty.getAddress());
	}

	public void testEmptyListEvaluatesToTheZeroAddress() throws Exception {
		ListDenotator empty = new ListDenotator(null, Z, MELODY, new ArrayList<Denotator>());
		assertTrue(empty.at(new ZElement(3)).nullAddressed());
	}

	public void testEmptyPowerChangesToTheDomainOfTheMorphism() throws Exception {
		PowerDenotator empty = new PowerDenotator(null, Z, CHORD, new ArrayList<Denotator>());
		Denotator c = empty.changeAddress(new ConstantMorphism(ZERO, new ZElement(1)));
		assertTrue(c.nullAddressed());
	}

	// changeAddress documents "null if address change fails". The four
	// factor types threw NullPointerException instead.

	public void testWrongCodomainGivesNullForEveryType() throws Exception {
		ModuleMorphism intoR = new RAffineMorphism(1, 0);
		assertNull(arrow(PITCH, 60, 1).changeAddress(intoR));
		assertNull(new PowerDenotator(null, Z, CHORD, list(arrow(PITCH, 60, 1))).changeAddress(intoR));
		assertNull(new PowerDenotator(null, Z, CHORD, new ArrayList<Denotator>()).changeAddress(intoR));
		assertNull(new ListDenotator(null, Z, MELODY, list(arrow(PITCH, 60, 1))).changeAddress(intoR));
		assertNull(new LimitDenotator(null, Z, NOTE, list(arrow(ONSET, 0, 1), arrow(PITCH, 60, 1))).changeAddress(intoR));
		assertNull(new ColimitDenotator(null, Z, EITHER, 0, arrow(PITCH, 60, 1)).changeAddress(intoR));
	}

	// Definition 4: the coordinates of an M-addressed denotator lie in
	// M@Fun(F), so denotators at different addresses are different. Java
	// compared only the coordinate maps, and a constant map ignored its domain.

	public void testDenotatorsAtDifferentAddressesDiffer() throws Exception {
		SimpleDenotator zero = new SimpleDenotator(null, PITCH, new ZElement(66));
		Denotator lifted = zero.changeAddress(Z);
		assertFalse(zero.equals(lifted));
		assertTrue(zero.compareTo(lifted) != 0);
		assertEquals(Integer.signum(zero.compareTo(lifted)), -Integer.signum(lifted.compareTo(zero)));

		PowerDenotator e0 = new PowerDenotator(null, ZERO, CHORD, new ArrayList<Denotator>());
		PowerDenotator eZ = new PowerDenotator(null, Z, CHORD, new ArrayList<Denotator>());
		assertFalse(e0.equals(eZ));
		assertTrue(e0.compareTo(eZ) != 0);

		LimitDenotator n0 = new LimitDenotator(null, ZERO, NOTE,
				list(new SimpleDenotator(null, ONSET, new ZElement(0)), zero));
		Denotator nZ = n0.changeAddress(Z);
		assertFalse(n0.equals(nZ));
		assertTrue(n0.compareTo(nZ) != 0);
	}

	public void testSameAddressComparisonIsUnchanged() throws Exception {
		SimpleDenotator a = new SimpleDenotator(null, PITCH, new ZElement(60));
		SimpleDenotator b = new SimpleDenotator(null, PITCH, new ZElement(64));
		assertTrue(a.compareTo(b) < 0);
		assertEquals(a, new SimpleDenotator(null, PITCH, new ZElement(60)));
	}

	private static final SimpleForm REAL = FormFactory.makeRModuleForm("AddressTestReal");

	/**
	 * A copy of a Z-addressed constant keeps its address in its morphism too. The copy
	 * dropped the domain before stage 9, so adding an arrow to the copied constant failed.
	 */
	public void testACopiedAddressedConstantKeepsItsDomain() throws Exception {
		SimpleDenotator constant = (SimpleDenotator)new SimpleDenotator(null, REAL, new RElement(3)).changeAddress(Z);
		SimpleDenotator copy = (SimpleDenotator)constant.copy();
		assertEquals(Z, copy.getAddress());
		assertEquals(Z, copy.getModuleMorphism().getDomain());
		ModuleMorphism arrow = new RAffineMorphism(2, 0).compose(CanonicalMorphism.make(Z, RRing.ring));
		ModuleMorphism sum = copy.getModuleMorphism().sum(arrow);
		assertEquals(new RElement(5), sum.map(new ZElement(1)));
	}

	/**
	 * A constant coordinate and an arrow coordinate are ordered and compared the same way
	 * from both sides. PowerDenotator keeps its factors in a TreeMap, which loses objects
	 * when the two orders disagree; before stage 9 an arrow map ranked itself after every
	 * constant map while a constant map compared the two morphisms' strings.
	 */
	public void testConstantAndArrowCoordinatesOrderConsistently() throws Exception {
		SimpleDenotator constant = (SimpleDenotator)new SimpleDenotator(null, REAL, new RElement(3)).changeAddress(Z);
		SimpleDenotator arrow = new SimpleDenotator(null, REAL, new RAffineMorphism(2, 0).compose(CanonicalMorphism.make(Z, RRing.ring)));
		assertTrue(constant.compareTo(arrow) != 0);
		assertEquals(Integer.signum(constant.compareTo(arrow)), -Integer.signum(arrow.compareTo(constant)));
		assertFalse(constant.equals(arrow));
		assertFalse(arrow.equals(constant));
		SimpleDenotator constantArrow = new SimpleDenotator(null, REAL, new ConstantMorphism(Z, new RElement(3)));
		assertEquals(0, constant.compareTo(constantArrow));
		assertEquals(0, constantArrow.compareTo(constant));
		assertTrue(constant.equals(constantArrow));
		assertTrue(constantArrow.equals(constant));
		//_make_unsafe keeps a constant morphism in an arrow map, which then meets the constant map
		SimpleDenotator wrapped = SimpleDenotator._make_unsafe(null, REAL, new ConstantMorphism(Z, new RElement(3)));
		assertEquals(0, wrapped.compareTo(constant));
		assertEquals(0, constant.compareTo(wrapped));
		assertTrue(wrapped.equals(constant));
		assertTrue(constant.equals(wrapped));
	}
}
