package org.rubato.rubettes.bigbang.headless.test;

import java.util.Arrays;

import junit.framework.TestCase;

import org.rubato.math.gesture.Gesture;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.OperationGestures;
import org.rubato.rubettes.bigbang.headless.Scores;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.bigbang.model.graph.BigBangOperationGraph;
import org.rubato.rubettes.bigbang.model.graph.CompositionState;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;
import org.rubato.rubettes.bigbang.model.operations.AbstractTransformation;
import org.rubato.rubettes.bigbang.model.operations.ScalingTransformation;
import org.rubato.math.matrix.RMatrix;

/**
 * BigBang's operation graph as a gesture (Mazzola, The Topos of Music III, section 61.5,
 * p. 914) and its transformations gesturalized as section 73.2.1, pp. 1152-1154 gives them
 * (sources/BIGBANG-GESTURES-SPEC.md).
 */
public class OperationGesturesTest extends TestCase {

	private static final double TOLERANCE = 1e-9;
	private static final int SAMPLES = 16;
	private static final double[][] TWO = {{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}};

	protected void setUp() {
		BigBang.registerForms();
	}

	private static AbstractTransformation last(BigBang bigBang) {
		return (AbstractTransformation)bigBang.getModel().getTransformationGraph().getLastAddedOperation();
	}

	/** One BigBang per transformation type, each with a centre off the origin. */
	private static AbstractTransformation[] sixTransformations() {
		return new AbstractTransformation[]{
			last(new BigBang(Scores.score(TWO)).translate(1, 2)),
			last(new BigBang(Scores.score(TWO)).rotate(1, 61, 4.0)),
			last(new BigBang(Scores.score(TWO)).scale(1, 61, 2, 3)),
			last(new BigBang(Scores.score(TWO)).shear(1, 61, 0.5, 0.25)),
			last(new BigBang(Scores.score(TWO)).reflect(1, 61, 1, 1)),
			last(new BigBang(Scores.score(TWO)).affine(1, 61, new double[]{2, -1}, new double[][]{{1, 2}, {0, 1}}))
		};
	}

	public void testTheBookCurveIsTheCodeCurveForEveryTransformation() {
		for (AbstractTransformation op : sixTransformations()) {
			for (int j = 0; j <= SAMPLES; j++) {
				double s = (double)j / SAMPLES;
				double[] code = OperationGestures.affineAt(op, s);
				double[] book = OperationGestures.bookAffineAt(op, s);
				for (int k = 0; k < 6; k++) {
					assertEquals(op.getClass().getSimpleName() + " s = " + s + " code " + Arrays.toString(code)
							+ " book " + Arrays.toString(book), book[k], code[k], TOLERANCE);
				}
			}
			assertEquals(op.getClass().getSimpleName(), 1.0, op.getModificationRatio());
		}
	}

	public void testTheBookRotationTurnsByTheFractionOfTheAngle() {
		AbstractTransformation rotation = last(new BigBang(Scores.score(TWO)).rotate(1, 61, 4.0));
		double[] half = OperationGestures.bookAffineAt(rotation, 0.5);
		assertEquals(Arrays.toString(half), Math.cos(2.0), half[0], TOLERANCE);
		assertEquals(Arrays.toString(half), -Math.sin(2.0), half[1], TOLERANCE);
		//the centre (1, 61) stays fixed: e = 1 - (a + 61 b), f = 61 - (c + 61 d)
		assertEquals(Arrays.toString(half), 1 - (half[0] + 61 * half[1]), half[4], TOLERANCE);
		assertEquals(Arrays.toString(half), 61 - (half[2] + 61 * half[3]), half[5], TOLERANCE);
	}

	public void testEachTransformationIsAGestureOnTheArrowDigraph() {
		for (AbstractTransformation op : sixTransformations()) {
			Gesture g = OperationGestures.transformationGesture(op, SAMPLES);
			assertEquals(2, g.skeleton().vertexCount());
			assertEquals(1, g.skeleton().arrowCount());
			assertEquals(6, g.dimension());
			assertTrue(Arrays.toString(g.vertex(0)), Arrays.equals(new double[]{1, 0, 0, 1, 0, 0}, g.vertex(0)));
			assertTrue(op.getClass().getSimpleName() + " gap " + g.endpointGap(), g.endpointGap() <= TOLERANCE);
		}
	}

	public void testATransformationThatIgnoresItsRatioIsNotAGesture() {
		BigBang bigBang = new BigBang(Scores.score(TWO));
		BigBangModel model = bigBang.getModel();
		TransformationProperties properties = new TransformationProperties(bigBang.getObjects(), null,
				BigBang.transformationPaths(model.getObjects(), new int[]{0, 1}), false, true);
		properties.setCenter(new double[]{1, 61});
		properties.setEndPoint(new double[]{1, 61});
		ScalingTransformation fixed = new ScalingTransformation(model, properties, new double[]{2, 2}) {
			protected RMatrix getMatrix() {
				return new RMatrix(new double[][]{{2, 0}, {0, 2}});
			}
		};
		Gesture g = OperationGestures.transformationGesture(fixed, SAMPLES);
		//the curve starts at the full map, x -> 2x - (1, 61), instead of the identity
		assertEquals(Math.sqrt(1 + 1 + 1 + 61 * 61), g.endpointGap(), TOLERANCE);
		double[] code = OperationGestures.affineAt(fixed, 0);
		double[] book = OperationGestures.bookAffineAt(fixed, 0);
		assertTrue(Arrays.toString(code), Arrays.equals(new double[]{2, 0, 0, 2, -1, -61}, code));
		assertTrue(Arrays.toString(book), Arrays.equals(new double[]{1, 0, 0, 1, 0, 0}, book));
	}

	private static BigBang threeTransformations() {
		return new BigBang(Scores.score(TWO)).translate(1, 2).rotate(1.5, 63, Math.PI)
				.affine(1, 61, new double[]{2, -1}, new double[][]{{1, 2}, {0, 1}});
	}

	public void testALinearGraphOfTransformationsIsAGesture() {
		BigBang bigBang = threeTransformations();
		OperationGestures.Report report = OperationGestures.check(bigBang.getModel(), SAMPLES);
		assertNull(report.text(), report.reason());
		assertTrue(report.text(), report.isGesture());
		Gesture g = report.gesture();
		//states 1 to 4 (state 0 and the input dropped), three arrows in a row
		assertEquals(4, g.skeleton().vertexCount());
		assertEquals(3, g.skeleton().arrowCount());
		for (int a = 0; a < 3; a++) {
			assertEquals(a, g.skeleton().tail(a));
			assertEquals(a + 1, g.skeleton().head(a));
		}
		//two notes, five coordinates each
		assertEquals(10, g.dimension());
		assertTrue(report.text(), report.endpointGap() <= TOLERANCE);
		assertEquals(3, report.arrows().size());
		for (OperationGestures.ArrowReport arrow : report.arrows()) {
			assertTrue(report.text(), arrow.tailGap() <= TOLERANCE);
			assertTrue(report.text(), arrow.headGap() <= TOLERANCE);
			assertTrue(report.text(), arrow.bookGap() <= TOLERANCE);
			assertTrue(report.text(), arrow.maxStep() < 1);
		}
	}

	public void testTheVertexPointsAreTheStates() {
		BigBang bigBang = threeTransformations();
		Gesture g = OperationGestures.check(bigBang.getModel(), SAMPLES).gesture();
		//state 1 is the input, objects in final order: the half-turn swaps the two notes
		double[] input = g.vertex(0);
		double[] last = g.vertex(3);
		double[][] finalNotes = Scores.values(bigBang.getComposition());
		double[] sortedLast = {last[0], last[5]};
		Arrays.sort(sortedLast);
		assertEquals(Arrays.toString(last), finalNotes[0][0], sortedLast[0], TOLERANCE);
		assertEquals(Arrays.toString(last), finalNotes[1][0], sortedLast[1], TOLERANCE);
		double[] inputOnsets = {input[0], input[5]};
		Arrays.sort(inputOnsets);
		assertEquals(0, inputOnsets[0], TOLERANCE);
		assertEquals(1, inputOnsets[1], TOLERANCE);
		//each object keeps its identity: the note with pitch 60 at the input is the one with
		//loudness 120 at the end
		int first = input[1] == 60 ? 0 : 5;
		assertEquals(120, last[first + 2], TOLERANCE);
	}

	public void testTheCheckLeavesTheModelAsItWas() {
		BigBang bigBang = threeTransformations();
		Denotator before = bigBang.getComposition().copy();
		BigBangOperationGraph graph = bigBang.getModel().getTransformationGraph();
		OperationGestures.check(bigBang.getModel(), SAMPLES);
		for (AbstractOperation op : graph.getOperationsInLogicalOrder()) {
			assertEquals(op.toString(), 1.0, op.getModificationRatio());
		}
		assertNull(graph.getSelectedCompositionState());
		assertTrue(Arrays.deepToString(Scores.values(bigBang.getComposition())),
				Arrays.deepEquals(Scores.values(before), Scores.values(bigBang.getComposition())));
	}

	public void testAnOperationHeldPartwayKeepsItsRatioForTheLaterArrows() {
		BigBang bigBang = new BigBang(Scores.score(TWO)).translate(4, 0).translate(0, 12);
		BigBangOperationGraph graph = bigBang.getModel().getTransformationGraph();
		AbstractOperation first = graph.getOperationsInLogicalOrder().get(1);
		first.modify(0.5);
		bigBang.replay();
		OperationGestures.Report report = OperationGestures.check(bigBang.getModel(), SAMPLES);
		assertTrue(report.text(), report.isGesture());
		//the second state has the onsets moved by 2, half of 4
		double[] second = report.gesture().vertex(1);
		double[] onsets = {second[0], second[5]};
		Arrays.sort(onsets);
		assertEquals(Arrays.toString(second), 2, onsets[0], TOLERANCE);
		assertEquals(Arrays.toString(second), 3, onsets[1], TOLERANCE);
		assertEquals(0.5, first.getModificationRatio());
	}

	public void testTheCheckKeepsASelectedState() {
		BigBang bigBang = threeTransformations();
		BigBangModel model = bigBang.getModel();
		model.selectCompositionStateAt(2);
		CompositionState selected = model.getTransformationGraph().getSelectedCompositionState();
		OperationGestures.check(model, SAMPLES);
		assertSame(selected, model.getTransformationGraph().getSelectedCompositionState());
	}

	public void testCopyingRefusesBecauseTheObjectsChange() {
		BigBang bigBang = new BigBang(Scores.score(TWO)).translate(1, 2).copying(true).translate(0, 12);
		OperationGestures.Report report = OperationGestures.check(bigBang.getModel(), SAMPLES);
		assertNull(report.gesture());
		assertFalse(report.isGesture());
		assertTrue(report.reason(), report.reason().contains("objects"));
		//the second translation copies; the first leaves the objects as they are
		assertTrue(report.reason(), report.reason().contains("operation 2 (TranslationTransformation)"));
		BigBang copyFirst = new BigBang(Scores.score(TWO)).copying(true).translate(0, 12).copying(false).scale(0, 60, 2, 2);
		String reason = OperationGestures.check(copyFirst.getModel(), SAMPLES).reason();
		assertTrue(reason, reason.contains("operation 1 (TranslationTransformation)"));
	}

	public void testAParallelOperationRefuses() {
		BigBang bigBang = new BigBang(Scores.score(TWO)).translate(1, 2).scale(0, 60, 2, 2);
		BigBangOperationGraph graph = bigBang.getModel().getTransformationGraph();
		AbstractOperation translation = graph.getOperationsInLogicalOrder().get(1);
		graph.addEdge(translation.clone(), graph.getSource(translation), graph.getDest(translation));
		OperationGestures.Report report = OperationGestures.check(bigBang.getModel(), SAMPLES);
		assertNull(report.gesture());
		assertTrue(report.reason(), report.reason().contains("linear"));
	}

	public void testOnlyAnInputIsAGestureWithOneVertex() {
		OperationGestures.Report report = OperationGestures.check(new BigBang(Scores.score(TWO)).getModel(), SAMPLES);
		assertNull(report.reason(), report.reason());
		assertEquals(1, report.gesture().skeleton().vertexCount());
		assertEquals(0, report.gesture().skeleton().arrowCount());
		assertTrue(report.isGesture());
	}

}
