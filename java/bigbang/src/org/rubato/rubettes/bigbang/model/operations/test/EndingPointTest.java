package org.rubato.rubettes.bigbang.model.operations.test;

import java.util.Arrays;

import junit.framework.TestCase;

import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.Scores;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.bigbang.model.operations.AbstractLocalTransformation;

/**
 * The ending point of a local transformation at a modification ratio r lies on the
 * segment from the centre c to the ending point e: c + (e - c) * r in each coordinate
 * (sources/BIGBANG-GESTURES-SPEC.md, section 2).
 */
public class EndingPointTest extends TestCase {

	private static final double TOLERANCE = 1e-12;

	protected void setUp() {
		BigBang.registerForms();
	}

	private static AbstractLocalTransformation scalingWith(double[] center, double[] end) {
		BigBang bigBang = new BigBang(Scores.score(new double[][]{{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}}));
		bigBang.scale(center[0], center[1], 2, 2);
		BigBangModel model = bigBang.getModel();
		AbstractLocalTransformation scaling =
				(AbstractLocalTransformation)model.getTransformationGraph().getLastAddedOperation();
		TransformationProperties properties = new TransformationProperties(bigBang.getObjects(), null,
				BigBang.transformationPaths(model.getObjects(), new int[]{0, 1}), false, true);
		properties.setCenter(center);
		properties.setEndPoint(end);
		scaling.updateProperties(properties);
		return scaling;
	}

	public void testTheEndingPointUsesEachCoordinateOfTheCentre() {
		AbstractLocalTransformation scaling = scalingWith(new double[]{1, 5}, new double[]{3, 9});
		scaling.modify(0.5);
		double[] point = scaling.getEndingPoint();
		assertEquals(Arrays.toString(point), 2, point[0], TOLERANCE);
		assertEquals(Arrays.toString(point), 7, point[1], TOLERANCE);
	}

	public void testTheEndingPointRunsFromTheCentreToTheEnd() {
		AbstractLocalTransformation scaling = scalingWith(new double[]{-2, 4}, new double[]{6, -4});
		scaling.modify(0);
		assertTrue(Arrays.toString(scaling.getEndingPoint()),
				Arrays.equals(new double[]{-2, 4}, scaling.getEndingPoint()));
		scaling.modify(1);
		assertTrue(Arrays.toString(scaling.getEndingPoint()),
				Arrays.equals(new double[]{6, -4}, scaling.getEndingPoint()));
	}

}
