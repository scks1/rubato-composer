package org.rubato.rubettes.bigbang.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.model.denotators.TransformationPaths;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.bigbang.model.graph.BigBangOperationGraph;
import org.rubato.rubettes.bigbang.model.graph.CompositionState;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;
import org.rubato.rubettes.bigbang.model.operations.RotationTransformation;
import org.rubato.rubettes.bigbang.model.operations.ScalingTransformation;
import org.rubato.rubettes.bigbang.model.operations.ShearingTransformation;
import org.rubato.rubettes.bigbang.model.operations.TranslationTransformation;

/**
 * Drives a BigBang model with random sequences of adds, drags, modifications, undo, redo, state
 * selection, alternative operations, removals and deletions, and checks that no step throws and that
 * the operation graph stays consistent after every step.
 */
public class OperationGraphRandomEditsTest extends TestCase {

	private TestObjects objects;
	private TransformationPaths nodePaths;
	private BigBangModel full;
	private String lastStep;
	private final Map<String,Integer> failures = new LinkedHashMap<String,Integer>();

	protected void setUp() {
		this.objects = new TestObjects();
		this.nodePaths = this.objects.createStandardTransformationPaths(
				this.objects.SOUND_NODE_FORM, new int[][]{{0,0},{0,1}});
		this.full = new BigBangModel();
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

	private void runSeeds(Denotator score, long firstSeed, int seeds, int steps) {
		this.failures.clear();
		int completed = 0;
		for (long seed = firstSeed; seed < firstSeed + seeds; seed++) {
			this.setUp();
			Random random = new Random(seed);
			this.lastStep = null;
			this.full.setOrAddComposition(score.copy());
			int step = 0;
			try {
				for (; step < steps; step++) {
					this.randomStep(random);
					this.assertGraphConsistent();
				}
				completed++;
			} catch (RuntimeException e) {
				String where = e.getClass().getSimpleName() + " at " + topRubatoFrame(e) + " after " + this.lastStep;
				System.out.println("OperationGraphRandomEditsTest seed " + seed + " step " + step + ": " + where);
				this.failures.merge(where, 1, Integer::sum);
			} catch (junit.framework.AssertionFailedError e) {
				String where = "inconsistent graph after " + this.lastStep + ": " + e.getMessage();
				System.out.println("OperationGraphRandomEditsTest seed " + seed + " step " + step + ": " + where);
				this.failures.merge(where, 1, Integer::sum);
			}
		}
		System.out.println("OperationGraphRandomEditsTest seeds " + firstSeed + ".." + (firstSeed + seeds - 1)
				+ ": " + completed + " of " + seeds + " sequences completed");
		assertEquals("sequences that failed: " + this.failures, seeds, completed);
	}

	private static String topRubatoFrame(Throwable e) {
		for (StackTraceElement frame : e.getStackTrace()) {
			if (frame.getClassName().startsWith("org.rubato") && !frame.getClassName().endsWith("Test")) {
				return frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1)
						+ "." + frame.getMethodName() + ":" + frame.getLineNumber();
			}
		}
		return "?";
	}

	//every edge is listed once in logical order, states are numbered by position, and every state
	//other than the first has an incoming operation
	private void assertGraphConsistent() {
		BigBangOperationGraph graph = this.full.getTransformationGraph();
		assertEquals("operations in logical order", graph.getEdgeCount(), graph.getOperationsInLogicalOrder().size());
		for (AbstractOperation operation : graph.getOperationsInLogicalOrder()) {
			assertTrue("listed operation in graph: " + operation, graph.containsEdge(operation));
		}
		for (int i = 0; i < graph.getVertexCount(); i++) {
			CompositionState state = graph.getCompositionStateAt(i);
			assertEquals("index of state " + i, i, state.getIndex());
			if (i > 0) {
				assertTrue("state " + i + " has an incoming operation", graph.getInEdges(state).size() > 0);
			}
		}
	}

	private interface Edit {
		void apply(BigBangModel model);
	}

	private void both(Edit edit) {
		edit.apply(this.full);
	}

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
				//0: the first top-level objects. 1: the first objects at any level, which on a score with
				//satellites is a parent with its first satellite. 2: the last objects at any level, often a
				//satellite with its own satellite
				int which = random.nextInt(3);
				this.lastStep = "delete " + count + " objects (selection " + which + ")";
				this.both(m -> {
					List<BigBangObject> all = new ArrayList<BigBangObject>();
					for (BigBangObject object : m.getObjects().getObjectsAt(null)) {
						if (which > 0 || object.getParent() == null) {
							all.add(object);
						}
					}
					if (all.size() > count) {
						List<BigBangObject> selection = which == 2 ? all.subList(all.size() - count, all.size()) : all.subList(0, count);
						m.deleteObjects(new TreeSet<BigBangObject>(selection));
					}
				});
			}
			return true;
		}
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
