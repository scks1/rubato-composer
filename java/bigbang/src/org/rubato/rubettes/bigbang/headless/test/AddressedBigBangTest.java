package org.rubato.rubettes.bigbang.headless.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.math.arith.Rational;
import org.rubato.math.matrix.ZMatrix;
import org.rubato.math.module.Module;
import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.QElement;
import org.rubato.math.module.QRing;
import org.rubato.math.module.RElement;
import org.rubato.math.module.RRing;
import org.rubato.math.module.ZElement;
import org.rubato.math.module.ZProperFreeElement;
import org.rubato.math.module.ZProperFreeModule;
import org.rubato.math.module.ZRing;
import org.rubato.math.module.morphism.CanonicalMorphism;
import org.rubato.math.module.morphism.ConstantMorphism;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.QAffineMorphism;
import org.rubato.math.module.morphism.RAffineMorphism;
import org.rubato.math.module.morphism.ZFreeAffineMorphism;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.util.DenotatorPath;
import org.rubato.rubettes.util.SoundNoteGenerator;

/**
 * BigBang on addressed compositions (sources/BIGBANG-ADDRESSED-SPEC.md). A note's
 * onset and pitch at address A are arrows from A (Mazzola, The Topos of Music I,
 * section 6.2.1, pp. 53-54). A transformation f acts on an arrow x as f.x, so
 * evaluating the result at an address point a gives f applied to x(a): for each
 * operation op the tests compare op(D).at(a) with op(D.at(a)) at several points a.
 */
public class AddressedBigBangTest extends TestCase {

	private static final int[] POINTS = {-1, 0, 1, 2};

	protected void setUp() {
		BigBang.registerForms();
	}

	// ---------------------------------------------------------------- inputs

	private static Repository rep() {
		return Repository.systemRepository();
	}

	/** The Z-addressed real arrow x maps to slope*x + base. */
	private static ModuleMorphism zr(double slope, double base) throws Exception {
		return new RAffineMorphism(slope, base).compose(CanonicalMorphism.make(ZRing.ring, RRing.ring));
	}

	/** The Z-addressed rational arrow x maps to slope*x + base. */
	private static ModuleMorphism zq(int slope, int base) throws Exception {
		return new QAffineMorphism(new Rational(slope), new Rational(base)).compose(CanonicalMorphism.make(ZRing.ring, QRing.ring));
	}

	private static ModuleMorphism constant(Module address, ModuleElement value) {
		return new ConstantMorphism(address, value);
	}

	private static Denotator note(Module address, ModuleMorphism onset, ModuleMorphism pitch, int loudness, double duration, int voice) throws Exception {
		List<Denotator> c = new ArrayList<Denotator>();
		c.add(new SimpleDenotator(null, (SimpleForm)rep().getForm("Onset"), onset));
		c.add(new SimpleDenotator(null, (SimpleForm)rep().getForm("Pitch"), pitch));
		c.add(new SimpleDenotator(null, (SimpleForm)rep().getForm("Loudness"), constant(address, new ZElement(loudness))));
		c.add(new SimpleDenotator(null, (SimpleForm)rep().getForm("Duration"), constant(address, new RElement(duration))));
		c.add(new SimpleDenotator(null, (SimpleForm)rep().getForm("Voice"), constant(address, new ZElement(voice))));
		return new LimitDenotator(null, (LimitForm)rep().getForm("Note"), c);
	}

	private static Denotator score(Module address, List<Denotator> notes) throws Exception {
		return new PowerDenotator(NameDenotator.make(""), address, (PowerForm)rep().getForm("Score"), notes);
	}

	/** Four Z-addressed notes; three of them have onset or pitch arrows that are not constant. */
	public static Denotator zScore() throws Exception {
		List<Denotator> notes = new ArrayList<Denotator>();
		notes.add(note(ZRing.ring, zr(2, 0), zq(1, 60), 120, 1, 0));
		notes.add(note(ZRing.ring, zr(0, 1), zq(0, 64), 100, 2, 1));
		notes.add(note(ZRing.ring, constant(ZRing.ring, new RElement(3)), constant(ZRing.ring, new QElement(new Rational(67))), 90, 1, 2));
		notes.add(note(ZRing.ring, zr(-1, 5), zq(-2, 72), 80, 3, 3));
		return score(ZRing.ring, notes);
	}

	// ---------------------------------------------------------------- oracle

	/** Every note's values, nested satellites prefixed by their anchor, sorted. */
	public static String values(Denotator composition) {
		List<String> rows = new ArrayList<String>();
		try {
			collect(composition, "", rows);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		Collections.sort(rows);
		return rows.toString();
	}

	private static void collect(Denotator power, String prefix, List<String> rows) throws Exception {
		for (Denotator object : ((PowerDenotator)power).getFactors()) {
			Denotator note = object;
			if (object.getForm().getNameString().endsWith("Node")) {
				note = ((LimitDenotator)object).getFactor(0);
			}
			StringBuilder row = new StringBuilder(prefix);
			for (int j = 0; j < 5; j++) {
				double v = ((RElement)note.getElement(new int[]{j, 0}).cast(RRing.ring)).getValue();
				row.append(String.format("%.6f ", v + 0.0));
			}
			rows.add(row.toString());
			if (object != note) {
				for (int i = 1; i < ((LimitDenotator)object).getFactorCount(); i++) {
					Denotator child = ((LimitDenotator)object).getFactor(i);
					if (child instanceof PowerDenotator && child.getForm().getNameString().contains("Score")) {
						collect(child, row.toString().trim() + " > ", rows);
					}
				}
			}
		}
	}

	private static ModuleElement z(int a) {
		return new ZElement(a);
	}

	/** Returns the points among POINTS where op(D).at(a) and op(D.at(a)) differ. */
	private static List<Integer> disagreements(Denotator input, UnaryOperator<BigBang> op) throws Exception {
		Denotator result = op.apply(new BigBang(input)).getComposition();
		assertEquals("the result keeps the input's address", input.getAddress(), result.getAddress());
		List<Integer> differ = new ArrayList<Integer>();
		for (int a : POINTS) {
			String addressed = values(result.at(z(a)));
			String reference = values(op.apply(new BigBang(input.at(z(a)))).getComposition());
			if (!addressed.equals(reference)) {
				differ.add(a);
			}
		}
		return differ;
	}

	private static void assertCommutes(String name, Denotator input, UnaryOperator<BigBang> op) throws Exception {
		assertEquals(name + ": address points where op(D).at(a) differs from op(D.at(a))",
				Collections.<Integer>emptyList(), disagreements(input, op));
	}

	private static Set<BigBangObject> voices(BigBang b, int... voices) {
		return b.objectsWhere(n -> {
			Double v = n.get("Voice Z");
			if (v == null) {
				return false;
			}
			for (int x : voices) {
				if (v == x) {
					return true;
				}
			}
			return false;
		});
	}

	// ---------------------------------------------------------------- tests

	public void testTheModelKeepsTheArrows() throws Exception {
		BigBang bigBang = new BigBang(zScore());
		assertEquals(4, bigBang.getObjects().size());
		Denotator result = bigBang.axes("Onset", "Pitch").translate(1, 12).getComposition();
		assertEquals(ZRing.ring, result.getAddress());
		//the first note's onset 2x is translated to 2x + 1, so its tail is 1 and its head 3
		assertEquals("[1.000000 72.000000 120.000000 1.000000 0.000000 , 2.000000 76.000000 100.000000 2.000000 1.000000 , "
				+ "4.000000 79.000000 90.000000 1.000000 2.000000 , 6.000000 84.000000 80.000000 3.000000 3.000000 ]",
				values(result.atNull()));
		assertEquals("[2.000000 76.000000 100.000000 2.000000 1.000000 , 3.000000 73.000000 120.000000 1.000000 0.000000 , "
				+ "4.000000 79.000000 90.000000 1.000000 2.000000 , 5.000000 82.000000 80.000000 3.000000 3.000000 ]",
				values(result.at(z(1))));
	}

	public void testAffineTransformationsCommuteWithEvaluation() throws Exception {
		Denotator d = zScore();
		assertCommutes("translate", d, b -> b.axes("Onset", "Pitch").translate(1, 12));
		assertCommutes("rotate", d, b -> b.axes("Onset", "Pitch").rotate(1, 60, Math.PI / 2));
		assertCommutes("scale", d, b -> b.axes("Onset", "Pitch").scale(1, 60, 2, 0.5));
		assertCommutes("reflect", d, b -> b.axes("Onset", "Pitch").reflect(0, 60, 1, 1));
		assertCommutes("shear", d, b -> b.axes("Onset", "Pitch").shear(0, 60, 0.5, 0.25));
		assertCommutes("affine", d, b -> b.axes("Onset", "Pitch").affine(0, 0, new double[]{1, 2}, new double[][]{{1, 2}, {3, 4}}));
	}

	public void testCopiesSequencesAndWallpapersCommute() throws Exception {
		Denotator d = zScore();
		assertCommutes("copy and translate", d, b -> b.axes("Onset", "Pitch").copying(true).translate(10, 0));
		assertCommutes("two operations", d, b -> b.axes("Onset", "Pitch").translate(1, 12).axes("Pitch", "Loudness").rotate(60, 100, Math.PI / 3));
		assertCommutes("wallpaper", d, b -> b.axes("Onset", "Pitch").addWallpaperDimension(0, 2).translate(8, 1).endWallpaper());
		assertCommutes("ratio 0.5", d, b -> {
			b.axes("Onset", "Pitch").translate(2, 12);
			b.getModel().modifyOperation(1, 0.5);
			return b;
		});
	}

	public void testUndoRedoAndFullReplayCommute() throws Exception {
		Denotator d = zScore();
		assertCommutes("undo", d, b -> b.axes("Onset", "Pitch").translate(1, 12).rotate(0, 60, 1).undo());
		assertCommutes("undo, redo", d, b -> b.axes("Onset", "Pitch").translate(1, 12).rotate(0, 60, 1).undo().redo());
		BigBang full = new BigBang(d, false).axes("Onset", "Pitch").translate(1, 12).rotate(0, 60, 1);
		BigBang incremental = new BigBang(d, true).axes("Onset", "Pitch").translate(1, 12).rotate(0, 60, 1);
		for (int a : POINTS) {
			assertEquals(values(incremental.getComposition().at(z(a))), values(full.getComposition().at(z(a))));
			assertEquals(values(incremental.getComposition().at(z(a))), values(incremental.replay().at(z(a))));
		}
	}

	public void testSelectionAndDeletionCommute() throws Exception {
		Denotator d = zScore();
		assertCommutes("select voices 1 and 3", d, b -> b.select(voices(b, 1, 3)).axes("Onset", "Pitch").translate(4, -4));
		assertCommutes("delete voice 1", d, b -> {
			b.getModel().deleteObjects(new TreeSet<BigBangObject>(voices(b, 1)));
			return b;
		});
	}

	public void testAnAddedObjectIsAConstantArrow() throws Exception {
		UnaryOperator<BigBang> add = b -> {
			java.util.Map<DenotatorPath,Double> values = new TreeMap<DenotatorPath,Double>();
			double[] note = {10, 50, 70, 1, 4};
			for (int i = 0; i < note.length; i++) {
				values.put(new DenotatorPath(rep().getForm("Note"), new int[]{i}), note[i]);
			}
			ArrayList<java.util.Map<DenotatorPath,Double>> objects = new ArrayList<java.util.Map<DenotatorPath,Double>>();
			objects.add(values);
			ArrayList<DenotatorPath> parents = new ArrayList<DenotatorPath>();
			parents.add(new DenotatorPath(rep().getForm("Score")));
			b.getModel().addObjects(objects, parents, false);
			return b;
		};
		Denotator result = add.apply(new BigBang(zScore())).getComposition();
		assertEquals(5, ((PowerDenotator)result).getFactorCount());
		assertCommutes("add object", zScore(), add);
	}

	public void testSatellitesAndFlatteningKeepArrows() throws Exception {
		Denotator d = zSoundScore();
		UnaryOperator<BigBang> build = b -> {
			b.getModel().buildSatellites(new TreeSet<BigBangObject>(voices(b, 1, 2)), voices(b, 0).iterator().next(), 0);
			return b;
		};
		assertCommutes("build satellites", d, build);
		assertCommutes("build, then flatten", d, b -> {
			build.apply(b).getModel().flattenObjects(new TreeSet<BigBangObject>(voices(b, 1, 2)));
			return b;
		});
		assertCommutes("build, then translate the anchor", d, b -> build.apply(b).select(voices(b, 0)).axes("Onset", "Pitch").translate(1, 12));
		Denotator built = build.apply(new BigBang(d)).getComposition();
		assertTrue("satellites were built", values(built.atNull()).contains(" > "));
	}

	/** Foreground pitches 60 + 2x and 62 + 2x; background pitches 65 + 10x and 73. */
	private static Denotator alterationScore() throws Exception {
		List<Denotator> notes = new ArrayList<Denotator>();
		notes.add(note(ZRing.ring, constant(ZRing.ring, new RElement(0)), zq(2, 60), 120, 1, 0));
		notes.add(note(ZRing.ring, constant(ZRing.ring, new RElement(1)), zq(2, 62), 120, 1, 0));
		notes.add(note(ZRing.ring, constant(ZRing.ring, new RElement(10)), zq(10, 65), 120, 1, 1));
		notes.add(note(ZRing.ring, constant(ZRing.ring, new RElement(11)), zq(0, 73), 120, 1, 1));
		return score(ZRing.ring, notes);
	}

	private static UnaryOperator<BigBang> alteration(double degree) {
		return b -> b.alter(voices(b, 0), voices(b, 1), Arrays.asList("Pitch"), "Onset", degree, degree);
	}

	public void testAlterationCommutesWhenTheNeighbourIsTheSameEverywhere() throws Exception {
		Denotator d = zScore();
		assertCommutes("alter voice 0 towards voices 2 and 3", d,
				b -> b.alter(voices(b, 0), voices(b, 2, 3), Arrays.asList("Onset", "Pitch"), "Onset", 0.5, 0.5));
	}

	public void testAlterationChoosesTheNeighbourAtTheTail() throws Exception {
		//at x = 0 the nearest background pitch is 65 (the arrow 65 + 10x); at x = 1 and 2 a
		//pointwise alteration would choose 73 instead, but the build keeps the tail's choice
		assertEquals(Arrays.asList(1, 2), disagreements(alterationScore(), alteration(1)));
		Denotator result = alteration(1).apply(new BigBang(alterationScore())).getComposition();
		assertEquals("[0.000000 75.000000 120.000000 1.000000 0.000000 , 1.000000 75.000000 120.000000 1.000000 0.000000 , "
				+ "10.000000 75.000000 120.000000 1.000000 1.000000 , 11.000000 73.000000 120.000000 1.000000 1.000000 ]",
				values(result.at(z(1))));
		//halfway: 60 + 2x moves to 62.5 + 6x, which at x = 1 is 68.5
		Denotator half = alteration(0.5).apply(new BigBang(alterationScore())).getComposition();
		assertTrue(values(half.at(z(1))), values(half.at(z(1))).startsWith("[0.000000 68.500000 "));
	}

	public void testShapingWritesAConstant() throws Exception {
		UnaryOperator<BigBang> shape = b -> {
			TreeMap<Double,Double> locations = new TreeMap<Double,Double>();
			locations.put(0.0, 40.0);
			int[] xy = {b.getCoordinateIndex("Onset"), b.getCoordinateIndex("Pitch")};
			TransformationProperties properties = new TransformationProperties(new TreeSet<BigBangObject>(b.getObjects()), null,
					BigBang.transformationPaths(b.getModel().getObjects(), xy), false, true);
			properties.setCenter(new double[]{0, 0});
			properties.setEndPoint(new double[]{0, 0});
			b.getModel().shapeObjects(properties, locations);
			return b;
		};
		//the note with onset 2x has tail 0, so it is shaped; its pitch is then 40 at every x
		assertEquals(Arrays.asList(-1, 1, 2), disagreements(zScore(), shape));
		Denotator result = shape.apply(new BigBang(zScore())).getComposition();
		for (int a : POINTS) {
			assertTrue(values(result.at(z(a))), values(result.at(z(a))).contains(String.format("%.6f 40.000000 120.000000", 2.0 * a + 0.0)));
		}
	}

	public void testAZSquaredAddress() throws Exception {
		Module z2 = ZProperFreeModule.make(2);
		ModuleMorphism toZ = ZFreeAffineMorphism.make(new ZMatrix(new int[][]{{1, 2}}), new int[]{3});
		ModuleMorphism onset = CanonicalMorphism.make(ZRing.ring, RRing.ring).compose(toZ);
		ModuleMorphism pitch = new QAffineMorphism(new Rational(1), new Rational(60)).compose(CanonicalMorphism.make(ZRing.ring, QRing.ring).compose(toZ));
		List<Denotator> notes = new ArrayList<Denotator>();
		notes.add(note(z2, onset, pitch, 120, 1, 0));
		notes.add(note(z2, constant(z2, new RElement(5)), constant(z2, new QElement(new Rational(70))), 100, 1, 1));
		Denotator d = score(z2, notes);
		BigBang bigBang = new BigBang(d).axes("Onset", "Pitch").rotate(0, 60, Math.PI / 2);
		Denotator result = bigBang.getComposition();
		assertEquals(z2, result.getAddress());
		int[][] points = {{0, 0}, {1, 0}, {0, 1}, {-2, 3}};
		for (int[] p : points) {
			ModuleElement at = ZProperFreeElement.make(p);
			String reference = values(new BigBang(d.at(at)).axes("Onset", "Pitch").rotate(0, 60, Math.PI / 2).getComposition());
			assertEquals(Arrays.toString(p), reference, values(result.at(at)));
		}
	}

	public void testARealAddress() throws Exception {
		ModuleMorphism onset = new RAffineMorphism(0.5, 1);
		List<Denotator> notes = new ArrayList<Denotator>();
		notes.add(note(RRing.ring, onset, constant(RRing.ring, new QElement(new Rational(60))), 120, 1, 0));
		notes.add(note(RRing.ring, constant(RRing.ring, new RElement(4)), constant(RRing.ring, new QElement(new Rational(64))), 100, 1, 1));
		Denotator d = score(RRing.ring, notes);
		Denotator result = new BigBang(d).axes("Onset", "Pitch").scale(0, 60, 2, 1).getComposition();
		assertEquals(RRing.ring, result.getAddress());
		for (double a : new double[]{-1.5, 0, 0.25, 3}) {
			String reference = values(new BigBang(d.at(new RElement(a))).axes("Onset", "Pitch").scale(0, 60, 2, 1).getComposition());
			assertEquals(String.valueOf(a), reference, values(result.at(new RElement(a))));
		}
	}

	/** A Z-addressed flat SoundScore whose onsets and pitches are arrows. */
	static Denotator zSoundScore() throws Exception {
		SoundNoteGenerator generator = new SoundNoteGenerator();
		double[][] v = {{0, 60, 120, 1, 0}, {1, 64, 100, 2, 1}, {3, 67, 90, 1, 2}, {5, 72, 80, 3, 3}};
		PowerDenotator flat = (PowerDenotator)generator.createFlatSoundScore(v).changeAddress(ZRing.ring);
		int[][] slopes = {{2, 1}, {0, 0}, {1, -1}, {-1, -2}};
		List<Denotator> nodes = new ArrayList<Denotator>();
		for (Denotator n : flat.getFactors()) {
			LimitDenotator node = (LimitDenotator)n.copy();
			LimitDenotator note = (LimitDenotator)node.getFactor(0).copy();
			int voice = (int)Math.round(((RElement)note.getElement(new int[]{4, 0}).cast(RRing.ring)).getValue());
			for (int k = 0; k < 2; k++) {
				SimpleDenotator s = (SimpleDenotator)note.getFactor(k);
				double base = ((RElement)s.getElement().cast(RRing.ring)).getValue();
				ModuleMorphism arrow = s.getSimpleForm().getModule().equals(RRing.ring)
						? zr(slopes[voice][k], base) : zq(slopes[voice][k], (int)base);
				note.setFactor(k, new SimpleDenotator(NameDenotator.make(""), s.getSimpleForm(), arrow));
			}
			node.setFactor(0, note);
			nodes.add(node);
		}
		return new PowerDenotator(NameDenotator.make(""), ZRing.ring, flat.getPowerForm(), nodes);
	}

}
