package org.rubato.rubettes.bigbang.model.operations.test;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.RElement;
import org.rubato.math.module.RProperFreeElement;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.Scores;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;
import org.rubato.rubettes.bigbang.model.operations.AffineTransformation;

/**
 * An affine operation gesturalized as Mazzola, The Topos of Music III, section 73.2.1.6,
 * p. 1154 gives it, with the centre c held constant (section 73.2.1, p. 1152): at ratio r
 * the operation maps x to c + (r A + (1 - r) I)(x - c) + r b
 * (sources/BIGBANG-GESTURES-SPEC.md, section 3.2).
 */
public class AffineGesturalizationTest extends TestCase {

	private static final double TOLERANCE = 1e-9;
	private static final double[][] TWO = {{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}};
	private static final double[] CENTER = {0, 60};
	private static final double[] SHIFT = {1, 0};
	private static final double[][] QUARTER_TURN = {{0, -1}, {1, 0}};

	private BigBang bigBang;
	private AffineTransformation affine;

	protected void setUp() {
		BigBang.registerForms();
		this.bigBang = new BigBang(Scores.score(TWO));
		this.bigBang.affine(CENTER[0], CENTER[1], SHIFT, QUARTER_TURN);
		this.affine = (AffineTransformation)this.bigBang.getModel().getTransformationGraph().getLastAddedOperation();
	}

	private double[][] notesAt(double ratio) {
		this.affine.modify(ratio);
		return Scores.values(this.bigBang.replay());
	}

	private static void assertOnsetsAndPitches(String label, double[][] expected, double[][] actual) {
		assertEquals(label + " " + Arrays.deepToString(actual), expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(label + " " + Arrays.deepToString(actual), expected[i][0], actual[i][0], TOLERANCE);
			assertEquals(label + " " + Arrays.deepToString(actual), expected[i][1], actual[i][1], TOLERANCE);
		}
	}

	public void testAtRatioZeroTheCompositionIsTheInput() {
		assertOnsetsAndPitches("ratio 0", new double[][]{{0, 60}, {1, 62}}, this.notesAt(0));
	}

	public void testHalfwayTheMatrixAndShiftAreInterpolated() {
		//(0,60) is the centre: c + r b = (0.5, 60); (1,62): M(1,2) = (-0.5, 1.5), plus c and r b
		assertOnsetsAndPitches("ratio 0.5", new double[][]{{0, 61.5}, {0.5, 60}}, this.notesAt(0.5));
	}

	public void testAtRatioOneTheMapIsUnchanged() {
		assertOnsetsAndPitches("ratio 1", new double[][]{{-1, 61}, {1, 60}}, this.notesAt(1));
	}

	public void testTheMorphismFollowsTheFormulaAtEverySample() {
		for (int j = 0; j <= 8; j++) {
			double r = j / 8.0;
			this.affine.modify(r);
			double[] actual = coefficients(this.affine.getModuleMorphism());
			double[] expected = formula(r);
			for (int k = 0; k < 6; k++) {
				assertEquals("r = " + r + ": " + Arrays.toString(actual), expected[k], actual[k], TOLERANCE);
			}
		}
	}

	public void testTheFirstPartOfASplitIsTheMapAtTheSplitRatio() {
		List<AbstractOperation> parts = this.affine.getSplitOperations(0.25);
		AffineTransformation first = (AffineTransformation)parts.get(0);
		first.modify(1);
		double[] actual = coefficients(first.getModuleMorphism());
		double[] expected = formula(0.25);
		for (int k = 0; k < 6; k++) {
			assertEquals(Arrays.toString(actual), expected[k], actual[k], TOLERANCE);
		}
		first.modify(0);
		double[] identity = coefficients(first.getModuleMorphism());
		assertTrue(Arrays.toString(identity), Arrays.equals(new double[]{1, 0, 0, 1, 0, 0}, identity));
	}

	/** {a, b, c, d, e, f} of x' = a x + b y + e, y' = c x + d y + f at ratio r. */
	private static double[] formula(double r) {
		double[][] m = new double[2][2];
		for (int i = 0; i < 2; i++) {
			for (int k = 0; k < 2; k++) {
				m[i][k] = r * QUARTER_TURN[i][k] + (i == k ? 1 - r : 0);
			}
		}
		//x' = c + M(x - c) + r b = M x + (c - M c + r b)
		double e = CENTER[0] - (m[0][0] * CENTER[0] + m[0][1] * CENTER[1]) + r * SHIFT[0];
		double f = CENTER[1] - (m[1][0] * CENTER[0] + m[1][1] * CENTER[1]) + r * SHIFT[1];
		return new double[]{m[0][0], m[0][1], m[1][0], m[1][1], e, f};
	}

	private static double[] coefficients(ModuleMorphism morphism) {
		double[] o = map(morphism, 0, 0);
		double[] ex = map(morphism, 1, 0);
		double[] ey = map(morphism, 0, 1);
		return new double[]{ex[0] - o[0], ey[0] - o[0], ex[1] - o[1], ey[1] - o[1], o[0], o[1]};
	}

	private static double[] map(ModuleMorphism morphism, double x, double y) {
		try {
			ModuleElement image = morphism.map(RProperFreeElement.make(new double[]{x, y}));
			return new double[]{((RElement)image.getComponent(0)).getValue(), ((RElement)image.getComponent(1)).getValue()};
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

}
