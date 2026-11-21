package org.rubato.rubettes.bigbang.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.model.BigBangObjects;
import org.rubato.rubettes.bigbang.model.denotators.TransformationPaths;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;
import org.rubato.rubettes.bigbang.model.operations.AbstractTransformation;
import org.rubato.rubettes.bigbang.model.operations.ReflectionTransformation;
import org.rubato.rubettes.bigbang.model.operations.RotationTransformation;
import org.rubato.rubettes.bigbang.model.operations.ScalingTransformation;
import org.rubato.rubettes.bigbang.model.operations.ShearingTransformation;
import org.rubato.rubettes.bigbang.model.operations.TranslationTransformation;
import org.rubato.rubettes.bigbang.view.model.DragPreview;

/**
 * Checks the drag preview of BigBangView against the model.
 *
 * <ul>
 * <li>A drag gesture with the preview leaves the model alone until release and then adds the
 * transformation once with the final parameters. Three models take the same random sequences of
 * gestures and other edits: one handles gestures that way with incremental replay, one the same
 * way with full replay, and one receives every drag update as BigBangView sends it without the
 * preview. After every step the compositions must be equal.</li>
 * <li>Before each release, the preview computed from the shown values must match the values the
 * model holds after the release, and computing it must not change the model.</li>
 * </ul>
 */
public class DragPreviewTest extends TestCase {

	//values in Q modules are rounded by the denotators, the preview shows them unrounded
	private static final double TOLERANCE = 1e-2;
	
	private TestObjects objects;
	private TransformationPaths nodePaths;
	private BigBangModel preview, previewFull, perUpdate;
	private String lastStep;
	private int previewsChecked, valuesChecked, fallbacks;
	private double maxDeviation;

	protected void setUp() {
		this.objects = new TestObjects();
		this.nodePaths = this.objects.createStandardTransformationPaths(
				this.objects.SOUND_NODE_FORM, new int[][]{{0,0},{0,1}});
		this.preview = new BigBangModel();
		this.previewFull = new BigBangModel();
		this.perUpdate = new BigBangModel();
		this.preview.setIncrementalReplay(true);
		this.previewFull.setIncrementalReplay(false);
		this.perUpdate.setIncrementalReplay(true);
	}

	public void testGesturesOnFlatScore() {
		this.runSeeds(this.objects.flatSoundScore, 1, 20, 40);
	}

	public void testGesturesOnScoreWithSatellites() {
		this.runSeeds(this.objects.multiLevelSoundScore, 101, 20, 40);
	}

	public void testGesturesOnLargerScore() {
		double[][] notes = new double[200][];
		for (int i = 0; i < notes.length; i++) {
			notes[i] = new double[]{i * 0.25, 36 + (i * 7) % 48, 60 + i % 60, 0.5, i % 4};
		}
		PowerDenotator score = new org.rubato.rubettes.util.SoundNoteGenerator().createFlatSoundScore(notes);
		this.runSeeds(score, 201, 4, 30);
	}

	public void testGesturesOnModulatorsAreNotPreviewed() throws Exception {
		//four notes with two modulators each
		org.rubato.rubettes.util.SoundNoteGenerator generator = new org.rubato.rubettes.util.SoundNoteGenerator();
		generator.setFMModel(org.rubato.rubettes.util.SoundNoteGenerator.FM_MODELS[1]);
		org.rubato.math.yoneda.NameDenotator name = org.rubato.math.yoneda.NameDenotator.make("");
		List<Denotator> nodes = new ArrayList<Denotator>();
		for (int i = 0; i < 4; i++) {
			nodes.add(generator.createFMNodeDenotator(new double[]{i, 60 + 2 * i, 100, 1, 0}));
		}
		Denotator score = new PowerDenotator(name, generator.getSoundScoreForm(), nodes);
		//the model itself cannot map modulators with these paths (a NullPointerException in
		//ArbitraryDenotatorMapper), so this checks only that such gestures are not previewed
		BigBangModel model = new BigBangModel();
		model.setOrAddComposition(score);
		BigBangObjects objects = model.getObjects();
		String[] names = this.names(model);
		int modulators = 0;
		for (BigBangObject object : objects.getObjectsAt(null)) {
			if (!object.getTopDenotatorPath().getEndForm().equals(objects.getObjectTypes().get(0))) {
				modulators++;
				TreeSet<BigBangObject> selection = new TreeSet<BigBangObject>();
				selection.add(object);
				assertNull(DragPreview.create(objects, selection, false, names, new int[]{0, 0}));
			}
		}
		assertEquals(8, modulators);
		assertNull("all objects, modulators included", DragPreview.create(objects, new TreeSet<BigBangObject>(), false, names, new int[]{0, 0}));
	}

	public void testPreviewIsUnavailableWhileAStateIsSelected() {
		this.preview.setOrAddComposition(this.objects.flatSoundScore.copy());
		this.preview.translateObjects(this.props(this.preview, this.all(this.preview), null, true, 0, 0, 1, 1));
		assertTrue(this.preview.getObjects().canPreviewNewTransformation());
		this.preview.selectCompositionStateAt(1);
		assertFalse(this.preview.getObjects().canPreviewNewTransformation());
		this.preview.deselectCompositionStates();
		assertTrue(this.preview.getObjects().canPreviewNewTransformation());
		this.preview.setInsertionState(1);
		assertFalse(this.preview.getObjects().canPreviewNewTransformation());
		this.preview.setInsertionState(null);
		this.preview.addWallpaperDimension(this.all(this.preview), 0, 2);
		assertFalse(this.preview.getObjects().canPreviewNewTransformation());
	}

	private void runSeeds(Denotator score, long firstSeed, int seeds, int steps) {
		int compared = 0;
		this.previewsChecked = this.valuesChecked = this.fallbacks = 0;
		this.maxDeviation = 0;
		for (long seed = firstSeed; seed < firstSeed + seeds; seed++) {
			this.setUp();
			compared += this.runRandomSequence(score, seed, steps);
		}
		System.out.println("DragPreviewTest seeds " + firstSeed + ".." + (firstSeed + seeds - 1) + ": " + compared
				+ " steps compared, " + this.previewsChecked + " previews checked against the released result ("
				+ this.valuesChecked + " objects, max deviation " + this.maxDeviation + "), "
				+ this.fallbacks + " gestures without a preview");
		assertEquals("steps compared", seeds * steps, compared);
		assertTrue("previews checked: " + this.previewsChecked, this.previewsChecked > compared / 4);
	}

	private int runRandomSequence(Denotator score, long seed, int steps) {
		Random random = new Random(seed);
		this.lastStep = "load";
		this.all(m -> m.setOrAddComposition(score.copy()));
		int step = 0;
		try {
			for (; step < steps; step++) {
				this.randomStep(random);
			}
		} catch (junit.framework.AssertionFailedError e) {
			throw new junit.framework.AssertionFailedError("seed " + seed + " step " + step + " (" + this.lastStep + "): " + e.getMessage());
		}
		return step;
	}

	private void randomStep(Random random) {
		int choice = random.nextInt(10);
		List<AbstractOperation> path = this.previewFull.getTransformationGraph().getCurrentlyExecutedOperationsInOrder();
		if (choice <= 5 || path.size() < 2) {
			this.gesture(random);
		} else if (choice == 6) {
			this.lastStep = "undo";
			this.all(m -> m.undo());
		} else if (choice == 7) {
			this.lastStep = "redo";
			this.all(m -> m.redo());
		} else if (choice == 8) {
			int state = random.nextInt(this.previewFull.getTransformationGraph().getVertexCount());
			this.lastStep = "select state " + state + ", then a gesture";
			this.all(m -> m.selectCompositionStateAt(state));
			this.gesture(random);
			this.all(m -> m.deselectCompositionStates());
		} else {
			int index = 1 + random.nextInt(path.size() - 1);
			this.lastStep = "remove operation " + index;
			this.all(m -> m.removeOperation(m.getTransformationGraph().getCurrentlyExecutedOperationsInOrder().get(index)));
		}
	}

	//one drag gesture: a kind of transformation, a selection, a center and a number of updates
	private void gesture(Random random) {
		int kind = random.nextInt(5);
		int selection = random.nextInt(4);
		int updates = 1 + random.nextInt(12);
		long selectionSeed = random.nextLong();
		double cx = random.nextDouble() * 4 - 2, cy = random.nextDouble() * 4 - 2;
		double[][] parameters = new double[updates][];
		for (int i = 0; i < updates; i++) {
			parameters[i] = new double[]{random.nextDouble() * 4 - 2, random.nextDouble() * 4 - 2};
		}
		this.lastStep = "gesture kind " + kind + ", selection " + selection + ", " + updates + " updates";
		
		//the preview, from the values the objects show before the gesture
		DragPreview dragPreview = null;
		List<BigBangObject> changed = null;
		if (this.preview.getObjects().canPreviewNewTransformation()) {
			Selection s = this.select(this.preview, selection, selectionSeed);
			String[] names = this.names(this.preview);
			dragPreview = DragPreview.create(this.preview.getObjects(), s.objects, s.anchor != null, names, new int[]{0, 0});
			if (dragPreview == null) {
				this.fallbacks++;
			} else {
				Denotator compositionBefore = this.preview.getComposition().copy();
				List<String> valuesBefore = this.fingerprint(this.preview.getObjects());
				int edgesBefore = this.preview.getTransformationGraph().getEdgeCount();
				for (double[] p : parameters) {
					TransformationProperties props = this.props(this.preview, s.objects, s.anchor, true, cx, cy, p[0], p[1]);
					dragPreview.update(this.previewOperation(kind, props, p).getModuleMorphism());
				}
				assertEquals("the preview changed the composition", compositionBefore, this.preview.getComposition());
				assertEquals("the preview changed the objects", valuesBefore, this.fingerprint(this.preview.getObjects()));
				assertEquals("the preview changed the graph", edgesBefore, this.preview.getTransformationGraph().getEdgeCount());
				changed = new ArrayList<BigBangObject>(dragPreview.getChangedObjects());
			}
		}
		
		//release: the preview protocol sends the final parameters once
		double[] last = parameters[updates - 1];
		for (BigBangModel m : new BigBangModel[]{this.preview, this.previewFull}) {
			Selection s = this.select(m, selection, selectionSeed);
			this.send(m, kind, this.props(m, s.objects, s.anchor, true, cx, cy, last[0], last[1]), last);
		}
		//without the preview, every update reaches the model
		Selection s = this.select(this.perUpdate, selection, selectionSeed);
		for (int i = 0; i < updates; i++) {
			this.send(this.perUpdate, kind, this.props(this.perUpdate, s.objects, s.anchor, i == 0, cx, cy, parameters[i][0], parameters[i][1]), parameters[i]);
		}
		//the release event repeats the last update
		this.send(this.perUpdate, kind, this.props(this.perUpdate, s.objects, s.anchor, false, cx, cy, last[0], last[1]), last);
		this.assertSameState();
		
		if (dragPreview != null) {
			String[] names = this.names(this.preview);
			this.previewsChecked++;
			for (BigBangObject object : changed) {
				if (!this.preview.getObjects().getObjectsAt(null).contains(object)) {
					continue; //merged with an equal note
				}
				double[] expected = dragPreview.getPreviewValues(object);
				for (int i = 0; i < 2; i++) {
					double actual = object.getNthValue(names[i], 0);
					double deviation = Math.abs(actual - expected[i]);
					this.maxDeviation = Math.max(this.maxDeviation, deviation);
					assertTrue("preview of " + object.getTopDenotatorPath() + " " + names[i] + ": " + expected[i] + ", released " + actual,
							deviation <= TOLERANCE * Math.max(1, Math.abs(actual)));
				}
				this.valuesChecked++;
			}
		}
	}

	private AbstractTransformation previewOperation(int kind, TransformationProperties props, double[] p) {
		switch (kind) {
		case 0: return new TranslationTransformation(null, props);
		case 1: return new RotationTransformation(null, props, new double[]{0, 0}, p[0]);
		case 2: return new ScalingTransformation(null, props, new double[]{1 + p[0] / 4, 1 + p[1] / 4});
		case 3: return new ShearingTransformation(null, props, new double[]{p[0] / 4, p[1] / 4});
		default: return new ReflectionTransformation(null, props, new double[]{p[0] + 0.1, p[1]});
		}
	}

	private void send(BigBangModel m, int kind, TransformationProperties props, double[] p) {
		switch (kind) {
		case 0: m.translateObjects(props); break;
		case 1: m.rotateObjects(props, new double[]{0, 0}, p[0]); break;
		case 2: m.scaleObjects(props, new double[]{1 + p[0] / 4, 1 + p[1] / 4}); break;
		case 3: m.shearObjects(props, new double[]{p[0] / 4, p[1] / 4}); break;
		default: m.reflectObjects(props, new double[]{p[0] + 0.1, p[1]}); break;
		}
	}

	private static class Selection {
		TreeSet<BigBangObject> objects = new TreeSet<BigBangObject>();
		BigBangObject anchor;
	}

	//0: nothing selected (all objects), 1: top-level objects, 2: a random subset, 3: an anchor and its satellites
	private Selection select(BigBangModel m, int kind, long seed) {
		Selection s = new Selection();
		List<BigBangObject> shown = new ArrayList<BigBangObject>(m.getObjects().getObjectsAt(null));
		Random random = new Random(seed);
		if (kind == 1) {
			for (BigBangObject o : shown) {
				if (o.getParent() == null) {
					s.objects.add(o);
				}
			}
		} else if (kind == 2) {
			for (BigBangObject o : shown) {
				if (random.nextBoolean()) {
					s.objects.add(o);
				}
			}
		} else if (kind == 3) {
			for (BigBangObject o : shown) {
				if (!o.getChildren().isEmpty() && shown.containsAll(o.getChildren())) {
					s.anchor = o;
					s.objects.addAll(o.getChildren());
					break;
				}
			}
		}
		return s;
	}

	private String[] names(BigBangModel m) {
		List<String> names = m.getObjects().getCoordinateSystemValueNames();
		return new String[]{names.get(0), names.get(1)};
	}

	private interface Edit {
		void apply(BigBangModel model);
	}

	private void all(Edit edit) {
		edit.apply(this.preview);
		edit.apply(this.previewFull);
		edit.apply(this.perUpdate);
		this.assertSameState();
	}

	private void assertSameState() {
		assertEquals(this.lastStep + ": incremental and full replay", this.previewFull.getComposition(), this.preview.getComposition());
		assertEquals(this.lastStep + ": objects", this.fingerprint(this.previewFull.getObjects()), this.fingerprint(this.preview.getObjects()));
		assertEquals(this.lastStep + ": preview and per-update protocols", this.perUpdate.getComposition(), this.preview.getComposition());
		assertEquals(this.lastStep + ": operations", this.names(this.perUpdate.getTransformationGraph().getCurrentlyExecutedOperationsInOrder()),
				this.names(this.preview.getTransformationGraph().getCurrentlyExecutedOperationsInOrder()));
	}

	private List<String> names(List<AbstractOperation> operations) {
		List<String> names = new ArrayList<String>();
		for (AbstractOperation o : operations) {
			names.add(o.getPresentationName());
		}
		return names;
	}

	private List<String> fingerprint(BigBangObjects objects) {
		List<String> result = new ArrayList<String>();
		if (objects.getObjectsAt(null) == null) {
			return result;
		}
		for (BigBangObject object : objects.getObjectsAt(null)) {
			StringBuilder entry = new StringBuilder(String.valueOf(object.getTopDenotatorPath()));
			for (String name : objects.getCoordinateSystemValueNames()) {
				entry.append(' ').append(name).append('=').append(object.getValues(name));
			}
			result.add(entry.toString());
		}
		return result;
	}

	private TreeSet<BigBangObject> all(BigBangModel model) {
		return new TreeSet<BigBangObject>(model.getObjects().getObjectsAt(null));
	}

	private TransformationProperties props(BigBangModel model, TreeSet<BigBangObject> selected, BigBangObject anchor, boolean startNew,
			double cx, double cy, double dx, double dy) {
		TransformationProperties p = new TransformationProperties(selected, anchor, Arrays.asList(this.nodePaths), false, startNew);
		p.setCenter(new double[]{cx, cy});
		p.setEndPoint(new double[]{cx + dx, cy + dy});
		return p;
	}
}
