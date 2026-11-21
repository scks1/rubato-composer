package org.rubato.rubettes.bigbang.test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.model.BigBangObjects;
import org.rubato.rubettes.bigbang.model.denotators.TransformationPaths;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.util.DenotatorPath;
import org.rubato.rubettes.util.SoundNoteGenerator;

/**
 * Maps SoundScore modulators with the transformation paths BigBangView builds, one set of
 * paths per object type (BigBangView.getXYTransformationPaths).
 */
public class ModulatorMappingTest extends TestCase {

	private BigBangModel model;
	private String[] names;

	protected void setUp() {
		new TestObjects();
		SoundNoteGenerator generator = new SoundNoteGenerator();
		generator.setFMModel(SoundNoteGenerator.FM_MODELS[1]);
		List<Denotator> nodes = new ArrayList<Denotator>();
		for (int i = 0; i < 3; i++) {
			nodes.add(generator.createFMNodeDenotator(new double[]{i, 60 + 2 * i, 100, 1, 0}));
		}
		try {
			PowerDenotator score = new PowerDenotator(NameDenotator.make(""), generator.getSoundScoreForm(), nodes);
			this.model = new BigBangModel();
			this.model.setOrAddComposition(score);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		List<String> valueNames = this.model.getObjects().getCoordinateSystemValueNames();
		this.names = new String[]{valueNames.get(0), valueNames.get(1)};
	}

	public void testObjectTypes() {
		List<Form> types = this.model.getObjects().getObjectTypes();
		assertEquals(2, types.size());
		assertEquals(9, this.model.getObjects().getObjectsAt(null).size());
		assertEquals(6, this.modulators().size());
	}

	public void testTranslatingOneModulatorMovesOnlyThatModulator() {
		BigBangObject modulator = this.modulators().get(3);
		java.util.Map<BigBangObject,double[]> before = this.values();
		this.translate(this.selection(modulator), 0.5, 3);
		java.util.Map<BigBangObject,double[]> after = this.values();
		assertEquals(before.keySet(), after.keySet());
		for (BigBangObject object : before.keySet()) {
			double[] b = before.get(object), a = after.get(object);
			if (object == modulator) {
				assertEquals("moved x", b[0] + 0.5, a[0], 1e-6);
				assertEquals("moved y", b[1] + 3, a[1], 1e-6);
			} else {
				assertEquals(object.getTopDenotatorPath() + " x unchanged", b[0], a[0], 1e-6);
				assertEquals(object.getTopDenotatorPath() + " y unchanged", b[1], a[1], 1e-6);
			}
		}
	}

	/*
	 * When an object and its anchor are both mapped, the model maps the object and it also
	 * follows its anchor, so a satellite moves by twice the translation and a satellite of a
	 * satellite by three times. Modulators follow the same rule as satellites.
	 */
	public void testTranslatingEverythingMovesModulatorsLikeSatellites() {
		java.util.Map<BigBangObject,double[]> before = this.values();
		this.translate(new TreeSet<BigBangObject>(), 1, -2);
		this.assertMovedByDepth(before, this.values(), 1, -2);
	}

	public void testScalingOneModulator() {
		BigBangObject modulator = this.modulators().get(1);
		java.util.Map<BigBangObject,double[]> before = this.values();
		TransformationProperties p = new TransformationProperties(this.selection(modulator), null, this.viewPaths(), false, true);
		p.setCenter(new double[]{0, 0});
		this.model.scaleObjects(p, new double[]{2, 0.5});
		java.util.Map<BigBangObject,double[]> after = this.values();
		for (BigBangObject object : before.keySet()) {
			double[] b = before.get(object), a = after.get(object);
			if (object == modulator) {
				assertEquals(2 * b[0], a[0], 1e-6);
				assertEquals(0.5 * b[1], a[1], 1e-2);
			} else {
				assertEquals(b[0], a[0], 1e-6);
				assertEquals(b[1], a[1], 1e-6);
			}
		}
	}

	public void testCopyingAModulatorAddsAModulatorToTheSameNote() {
		BigBangObject modulator = this.modulators().get(3);
		DenotatorPath path = modulator.getTopDenotatorPath();
		double x = this.value(modulator, 0), y = this.value(modulator, 1);
		int modulatorsBefore = this.modulators().size();
		TransformationProperties p = new TransformationProperties(this.selection(modulator), null, this.viewPaths(), true, true);
		p.setCenter(new double[]{0, 0});
		p.setEndPoint(new double[]{0.5, 3});
		this.model.translateObjects(p);
		assertEquals(modulatorsBefore + 1, this.modulators().size());
		assertEquals("no satellite is added", 9 + 1, this.model.getObjects().getObjectsAt(null).size());
		boolean original = false, copy = false;
		for (BigBangObject object : this.modulators()) {
			DenotatorPath current = object.getTopDenotatorPath();
			if (current.getParentPath().equals(path.getParentPath())) {
				original |= Math.abs(this.value(object, 0) - x) < 1e-6 && Math.abs(this.value(object, 1) - y) < 1e-6;
				copy |= Math.abs(this.value(object, 0) - x - 0.5) < 1e-6 && Math.abs(this.value(object, 1) - y - 3) < 1e-6;
			}
		}
		assertTrue("the original stays", original);
		assertTrue("the copy is a modulator of the same note", copy);
	}

	public void testNoteWithSatellitesAndModulators() throws Exception {
		SoundNoteGenerator generator = new SoundNoteGenerator();
		generator.setFMModel(SoundNoteGenerator.FM_MODELS[1]);
		Denotator carrier = generator.createFMNodeDenotator(new double[]{0, 60, 100, 1, 0}).getFactor(0);
		Denotator satellites = new SoundNoteGenerator().createFlatSoundScore(new double[][]{{1, 3, -4, 0, 0}});
		List<Denotator> nodes = new ArrayList<Denotator>();
		nodes.add(generator.createNodeDenotator(carrier, satellites));
		this.model = new BigBangModel();
		this.model.setOrAddComposition(new PowerDenotator(NameDenotator.make(""), generator.getSoundScoreForm(), nodes));
		java.util.Map<BigBangObject,double[]> before = this.values();
		assertEquals(4, before.size());
		//move the satellite and both modulators, not their note
		TreeSet<BigBangObject> selection = new TreeSet<BigBangObject>();
		for (BigBangObject object : before.keySet()) {
			if (object.getParent() != null) {
				selection.add(object);
			}
		}
		assertEquals(3, selection.size());
		this.translate(selection, 0.25, 2);
		java.util.Map<BigBangObject,double[]> after = this.values();
		assertEquals(before.keySet(), after.keySet());
		for (BigBangObject object : before.keySet()) {
			double d = selection.contains(object) ? 1 : 0;
			assertEquals(object.getTopDenotatorPath() + " x", before.get(object)[0] + d * 0.25, after.get(object)[0], 1e-6);
			assertEquals(object.getTopDenotatorPath() + " y", before.get(object)[1] + d * 2, after.get(object)[1], 1e-6);
		}
	}

	public void testSatellitesForComparison() {
		this.model = new BigBangModel();
		this.model.setOrAddComposition(new SoundNoteGenerator().createMultiLevelSoundScore(
				new double[][]{{0,60,100,1,0},{1,3,-4,0,0},{1,-3,5,0,1}}));
		java.util.Map<BigBangObject,double[]> before = this.values();
		assertEquals(3, before.size());
		this.translate(new TreeSet<BigBangObject>(), 1, -2);
		this.assertMovedByDepth(before, this.values(), 1, -2);
	}

	private void assertMovedByDepth(java.util.Map<BigBangObject,double[]> before, java.util.Map<BigBangObject,double[]> after, double dx, double dy) {
		assertEquals(before.keySet(), after.keySet());
		for (BigBangObject object : before.keySet()) {
			int depth = 0;
			for (BigBangObject current = object; current != null; current = current.getParent()) {
				depth++;
			}
			assertEquals(object.getTopDenotatorPath() + " x", before.get(object)[0] + depth * dx, after.get(object)[0], 1e-6);
			assertEquals(object.getTopDenotatorPath() + " y", before.get(object)[1] + depth * dy, after.get(object)[1], 1e-6);
		}
	}

	private java.util.Map<BigBangObject,double[]> values() {
		java.util.Map<BigBangObject,double[]> result = new java.util.IdentityHashMap<BigBangObject,double[]>();
		for (BigBangObject object : this.model.getObjects().getObjectsAt(null)) {
			result.put(object, new double[]{this.value(object, 0), this.value(object, 1)});
		}
		return result;
	}

	private void translate(TreeSet<BigBangObject> selection, double dx, double dy) {
		TransformationProperties p = new TransformationProperties(selection, null, this.viewPaths(), false, true);
		p.setCenter(new double[]{0, 0});
		p.setEndPoint(new double[]{dx, dy});
		this.model.translateObjects(p);
	}

	//the paths BigBangView.getXYTransformationPaths builds with x and y on the first two values
	private List<TransformationPaths> viewPaths() {
		BigBangObjects objects = this.model.getObjects();
		List<TransformationPaths> paths = new ArrayList<TransformationPaths>();
		for (int i = 0; i < objects.getObjectTypes().size(); i++) {
			TransformationPaths current = new TransformationPaths();
			current.setXYCoordinates(new int[]{0, 1});
			paths.add(current);
		}
		for (int i = 0; i < 2; i++) {
			List<List<DenotatorPath>> current = objects.getAllObjectConfigurationsValuePathsAt(i);
			for (int j = 0; j < current.size(); j++) {
				paths.get(j).setDomainPaths(i, current.get(j));
				paths.get(j).setCodomainPaths(i, current.get(j));
			}
		}
		return paths;
	}

	private List<BigBangObject> modulators() {
		BigBangObjects objects = this.model.getObjects();
		List<BigBangObject> result = new ArrayList<BigBangObject>();
		for (BigBangObject object : objects.getObjectsAt(null)) {
			if (!object.getTopDenotatorPath().getEndForm().equals(objects.getObjectTypes().get(0))) {
				result.add(object);
			}
		}
		return result;
	}

	private TreeSet<BigBangObject> selection(BigBangObject object) {
		TreeSet<BigBangObject> selection = new TreeSet<BigBangObject>();
		selection.add(object);
		return selection;
	}

	private double value(BigBangObject object, int i) {
		Double value = object.getNthValue(this.names[i], 0);
		assertNotNull(object.getTopDenotatorPath() + " has a value for " + this.names[i], value);
		return value;
	}

}
