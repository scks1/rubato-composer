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
import org.rubato.rubettes.bigbang.model.operations.RotationTransformation;
import org.rubato.rubettes.bigbang.model.operations.ScalingTransformation;
import org.rubato.rubettes.bigbang.model.operations.ShearingTransformation;
import org.rubato.rubettes.bigbang.model.operations.TranslationTransformation;

/**
 * Drives two BigBang models with the same random edits, one with incremental replay and one that
 * executes the whole operation graph on every update, and compares their compositions and objects
 * after every step.
 */
public class IncrementalReplayTest extends TestCase {

	private TestObjects objects;
	private TransformationPaths nodePaths;
	private BigBangModel incremental, full;
	private String lastStep;

	protected void setUp() {
		this.objects = new TestObjects();
		this.nodePaths = this.objects.createStandardTransformationPaths(
				this.objects.SOUND_NODE_FORM, new int[][]{{0,0},{0,1}});
		this.incremental = new BigBangModel();
		this.full = new BigBangModel();
		this.incremental.setIncrementalReplay(true);
		this.full.setIncrementalReplay(false);
	}

	public void testDragReplaysOnlyTheDraggedOperation() {
		this.both(m -> m.setOrAddComposition(this.objects.flatSoundScore.copy()));
		this.both(m -> m.translateObjects(this.props(m, true, 1, 2)));
		this.both(m -> m.rotateObjects(this.props(m, true, 1, 1), new double[]{0, 0}, 0.3));
		this.both(m -> m.translateObjects(this.props(m, true, 0, 0)));
		for (int i = 1; i <= 5; i++) {
			double d = i;
			this.both(m -> m.translateObjects(this.props(m, false, d, -d)));
			assertEquals("replay start of drag update " + i, 3, this.incremental.getLastReplayStart());
			assertEquals(0, this.full.getLastReplayStart());
		}
	}

	public void testRandomEditsOnFlatScore() {
		this.runSeeds(this.objects.flatSoundScore, 1, 30, 80);
	}

	public void testRandomEditsOnScoreWithSatellites() {
		this.runSeeds(this.objects.multiLevelSoundScore, 101, 15, 80);
	}

	public void testRandomEditsOnLargerScore() {
		double[][] notes = new double[200][];
		for (int i = 0; i < notes.length; i++) {
			notes[i] = new double[]{i * 0.25, 36 + (i * 7) % 48, 60 + i % 60, 0.5, i % 4};
		}
		PowerDenotator score = new org.rubato.rubettes.util.SoundNoteGenerator().createFlatSoundScore(notes);
		this.runSeeds(score, 201, 4, 60);
	}

	//runs one random sequence per seed with fresh models and requires most steps to be compared
	private void runSeeds(Denotator score, long firstSeed, int seeds, int steps) {
		int compared = 0, incrementalUpdates = 0, broken = 0;
		for (long seed = firstSeed; seed < firstSeed + seeds; seed++) {
			this.setUp();
			int[] result = this.runRandomEdits(score, seed, steps);
			compared += result[0];
			incrementalUpdates += result[1];
			broken += result[2];
		}
		System.out.println("IncrementalReplayTest seeds " + firstSeed + ".." + (firstSeed + seeds - 1) + ": "
				+ compared + " steps compared, " + incrementalUpdates + " incremental updates, "
				+ broken + " sequences ended by graph exceptions");
		assertTrue("steps compared: " + compared, compared >= seeds * steps / 2);
		assertTrue("incremental updates: " + incrementalUpdates, incrementalUpdates > compared / 3);
	}

	//returns {steps compared, incremental updates, 1 if a graph exception ended the sequence}
	private int[] runRandomEdits(Denotator score, long seed, int steps) {
		Random random = new Random(seed);
		this.lastStep = null;
		this.both(m -> m.setOrAddComposition(score.copy()));
		int incrementalUpdates = 0, step = 0;
		try {
			for (; step < steps; step++) {
				if (this.randomStep(random)) {
					incrementalUpdates++;
				}
			}
		} catch (junit.framework.AssertionFailedError e) {
			throw new junit.framework.AssertionFailedError("seed " + seed + " step " + step + ": " + e.getMessage());
		} catch (GraphBroken e) {
			System.out.println("IncrementalReplayTest seed " + seed + " ended at step " + step + ", " + e.getMessage());
			return new int[]{step, incrementalUpdates, 1};
		}
		return new int[]{step, incrementalUpdates, 0};
	}
	
	//returns true if the incremental model's last update started after the first operation
	private boolean randomStep(Random random) {
		{
			int choice = random.nextInt(12);
			List<AbstractOperation> path = this.full.getTransformationGraph().getCurrentlyExecutedOperationsInOrder();
			AbstractOperation last = this.full.getTransformationGraph().getLastAddedOperation();
			double a = random.nextDouble() * 4 - 2, b = random.nextDouble() * 4 - 2;
			if (choice <= 2 || path.size() < 2) {
				int kind = random.nextInt(4);
				this.lastStep = "add operation kind " + kind;
				this.both(m -> this.addOperation(m, kind, a, b));
			} else if (choice <= 5) {
				this.lastStep = "drag " + last;
				if (last instanceof TranslationTransformation) {
					this.both(m -> m.translateObjects(this.props(m, false, a, b)));
				} else if (last instanceof RotationTransformation) {
					this.both(m -> m.rotateObjects(this.props(m, false, 1, 1), new double[]{0, 0}, a));
				} else if (last instanceof ScalingTransformation) {
					this.both(m -> m.scaleObjects(this.props(m, false, 1, 1), new double[]{1 + a / 10, 1 + b / 10}));
				} else if (last instanceof ShearingTransformation) {
					this.both(m -> m.shearObjects(this.props(m, false, 1, 1), new double[]{a / 10, b / 10}));
				} else {
					return false;
				}
			} else if (choice == 6) {
				int index = random.nextInt(this.full.getTransformationGraph().getEdgeCount());
				double ratio = random.nextDouble();
				this.lastStep = "modify operation " + index + " to " + ratio;
				this.both(m -> {
					try {
						m.modifyOperation(index, ratio);
					} catch (IndexOutOfBoundsException e) {
						//the shortest path from the first to the last state can be shorter than the edge count
					}
				});
			} else if (choice == 7) {
				this.lastStep = "undo";
				this.both(m -> m.undo());
			} else if (choice == 8) {
				this.lastStep = "redo";
				this.both(m -> m.redo());
			} else if (choice == 9) {
				int state = random.nextInt(this.full.getTransformationGraph().getVertexCount());
				this.lastStep = "select state " + state;
				this.both(m -> m.selectCompositionStateAt(state));
				if (random.nextBoolean()) {
					int kind = random.nextInt(4);
					this.lastStep = "alternative operation from state " + state;
					this.both(m -> this.addOperation(m, kind, a, b));
				}
				this.lastStep = "deselect states";
				this.both(m -> m.deselectCompositionStates());
			} else if (choice == 10 && path.size() > 1) {
				int index = 1 + random.nextInt(path.size() - 1);
				this.lastStep = "remove operation " + index;
				this.both(m -> m.removeOperation(
						m.getTransformationGraph().getCurrentlyExecutedOperationsInOrder().get(index)));
			} else if (choice == 11) {
				int count = 1 + random.nextInt(2);
				this.lastStep = "delete " + count + " objects";
				this.both(m -> {
					//top-level objects only: deleting a parent together with its satellites throws in both models
					List<BigBangObject> all = new ArrayList<BigBangObject>();
					for (BigBangObject object : m.getObjects().getObjectsAt(null)) {
						if (object.getParent() == null) {
							all.add(object);
						}
					}
					if (all.size() > count) {
						m.deleteObjects(new TreeSet<BigBangObject>(all.subList(0, count)));
					}
				});
			}
			return this.incremental.getLastReplayStart() > 0;
		}
	}

	private interface Edit {
		void apply(BigBangModel model);
	}

	//BigBangOperationGraph throws on some removals and on undoing them (a ConcurrentModificationException
	//in combineStates, an IndexOutOfBoundsException in insertOperation), and leaves itself in a state that
	//depends on hash order. A sequence that hits one is checked for the same exception in both models
	//and then ends.
	private void both(Edit edit) {
		RuntimeException incrementalException = null, fullException = null;
		try {
			edit.apply(this.incremental);
		} catch (RuntimeException e) {
			incrementalException = e;
		}
		try {
			edit.apply(this.full);
		} catch (RuntimeException e) {
			fullException = e;
		}
		if (incrementalException != null || fullException != null) {
			assertNotNull(this.lastStep + ": only the full model threw " + fullException, incrementalException);
			assertNotNull(this.lastStep + ": only the incremental model threw " + incrementalException, fullException);
			assertEquals(fullException.getClass(), incrementalException.getClass());
			throw new GraphBroken(this.lastStep + ": " + fullException);
		}
		this.assertSameState();
	}
	
	private static class GraphBroken extends RuntimeException {
		GraphBroken(String message) {
			super(message);
		}
	}

	private void assertSameState() {
		String context = this.lastStep != null ? this.lastStep : "setup";
		assertEquals(context + ": path length",
				this.full.getTransformationGraph().getCurrentlyExecutedOperationsInOrder().size(),
				this.incremental.getTransformationGraph().getCurrentlyExecutedOperationsInOrder().size());
		if (!this.full.getComposition().equals(this.incremental.getComposition())) {
			List<String> f = this.fingerprint(this.full.getObjects()), i = this.fingerprint(this.incremental.getObjects());
			List<String> onlyFull = new ArrayList<String>(f), onlyIncremental = new ArrayList<String>(i);
			onlyFull.removeAll(i);
			onlyIncremental.removeAll(f);
			fail(context + ": compositions differ; objects only in full replay " + onlyFull
					+ ", only in incremental " + onlyIncremental + "; path " + this.full.getTransformationGraph().getCurrentlyExecutedOperationsInOrder()
					+ "; replay start " + this.incremental.getLastReplayStart());
		}
		assertEquals(context + ": objects", this.fingerprint(this.full.getObjects()), this.fingerprint(this.incremental.getObjects()));
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
			BigBangObject parent = object.getParent();
			entry.append(" parent=").append(parent != null ? parent.getTopDenotatorPath() : null);
			result.add(entry.toString());
		}
		return result;
	}

	private void addOperation(BigBangModel model, int kind, double a, double b) {
		switch (kind) {
		case 0:
			model.translateObjects(this.props(model, true, a, b));
			break;
		case 1:
			model.rotateObjects(this.props(model, true, 1, 1), new double[]{0, 0}, a);
			break;
		case 2:
			model.scaleObjects(this.props(model, true, 1, 1), new double[]{1 + a / 10, 1 + b / 10});
			break;
		default:
			model.shearObjects(this.props(model, true, 1, 1), new double[]{a / 10, b / 10});
			break;
		}
	}

	private TransformationProperties props(BigBangModel model, boolean startNew, double dx, double dy) {
		TreeSet<BigBangObject> all = new TreeSet<BigBangObject>(model.getObjects().getAllObjects());
		TransformationProperties p = new TransformationProperties(all, null, Arrays.asList(this.nodePaths), false, startNew);
		p.setCenter(new double[]{0, 0});
		p.setEndPoint(new double[]{dx, dy});
		return p;
	}
}
