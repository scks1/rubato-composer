package org.rubato.rubettes.bigbang.headless.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.Scores;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;

/**
 * Checks the headless BigBang operations on Score and SoundScore denotators against values
 * worked out by hand, and checks undo, redo, replay and cloning.
 */
public class HeadlessBigBangTest extends TestCase {
	
	private static final double TOLERANCE = 1e-9;
	private static final double[][] TWO = {{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}};
	
	private static void assertNotes(String label, double[][] expected, Denotator actual) {
		double[][] values = Scores.values(actual);
		assertEquals(label + ": " + Arrays.deepToString(values), expected.length, values.length);
		for (int i = 0; i < expected.length; i++) {
			for (int j = 0; j < expected[i].length; j++) {
				assertEquals(label + ": note " + i + ", value " + j + " in " + Arrays.deepToString(values),
						expected[i][j], values[i][j], TOLERANCE);
			}
		}
	}
	
	private static List<Denotator> inputs() {
		return Arrays.asList((Denotator)Scores.score(TWO), Scores.soundScore(TWO));
	}
	
	public void testScoresRoundTripTheirValues() {
		assertNotes("Score", TWO, Scores.score(TWO));
		assertNotes("SoundScore", TWO, Scores.soundScore(TWO));
		assertEquals("Score", Scores.score(TWO).getForm().getNameString());
		assertEquals("SoundScore", Scores.soundScore(TWO).getForm().getNameString());
	}
	
	public void testCoordinatesAndTheInputStayUnchanged() {
		for (Denotator input : inputs()) {
			Denotator copy = input.copy();
			BigBang bigBang = new BigBang(input);
			assertEquals(Arrays.asList("Onset R", "Pitch Q", "Loudness Z", "Duration R", "Voice Z"),
					bigBang.getCoordinateNames().subList(0, 5));
			assertEquals(1, bigBang.getCoordinateIndex("Pitch"));
			assertEquals(1, bigBang.getCoordinateIndex("Pitch Q"));
			try {
				bigBang.getCoordinateIndex("Tempo");
				fail("no such coordinate");
			} catch (IllegalArgumentException e) { }
			bigBang.translate(5, 5);
			assertEquals(copy, input);
			assertEquals(input.getForm(), bigBang.getComposition().getForm());
		}
	}
	
	public void testTranslation() {
		for (Denotator input : inputs()) {
			assertNotes(input.getForm().getNameString(), new double[][]{{1, 63, 120, 1, 0}, {2, 65, 100, 2, 1}},
					new BigBang(input).translate(1, 3).getComposition());
		}
	}
	
	public void testTranslationOnOtherAxes() {
		assertNotes("loudness and duration", new double[][]{{0, 60, 110, 1.5, 0}, {1, 62, 90, 2.5, 1}},
				new BigBang(Scores.score(TWO)).axes("Duration", "Loudness").translate(0.5, -10).getComposition());
	}
	
	public void testRotationByAHalfTurn() {
		for (Denotator input : inputs()) {
			assertNotes(input.getForm().getNameString(), new double[][]{{-1, 58, 100, 2, 1}, {0, 60, 120, 1, 0}},
					new BigBang(input).rotate(0, 60, Math.PI).getComposition());
		}
	}
	
	public void testScaling() {
		for (Denotator input : inputs()) {
			assertNotes(input.getForm().getNameString(), new double[][]{{0, 60, 120, 1, 0}, {2, 61, 100, 2, 1}},
					new BigBang(input).scale(0, 60, 2, 0.5).getComposition());
		}
	}
	
	public void testReflectionKeepsTheOriginalsWhenCopying() {
		for (Denotator input : inputs()) {
			assertNotes(input.getForm().getNameString(),
					new double[][]{{0, 60, 120, 1, 0}, {0, 62, 120, 1, 0}, {1, 60, 100, 2, 1}, {1, 62, 100, 2, 1}},
					new BigBang(input).copying(true).reflect(0, 61, 1, 0).getComposition());
		}
	}
	
	public void testShearing() {
		for (Denotator input : inputs()) {
			assertNotes(input.getForm().getNameString(), new double[][]{{0, 60, 120, 1, 0}, {3, 62, 100, 2, 1}},
					new BigBang(input).shear(0, 60, 1, 0).getComposition());
		}
	}
	
	public void testAffineMap() {
		//x' = (x - 0) + 0.5 (y - 60) + 1, y' = (y - 60) + 60 + 2
		assertNotes("affine", new double[][]{{1, 62, 120, 1, 0}, {3, 64, 100, 2, 1}},
				new BigBang(Scores.score(TWO)).affine(0, 60, new double[]{1, 2}, new double[][]{{1, 0.5}, {0, 1}}).getComposition());
	}
	
	public void testSelectionLimitsTheOperation() {
		BigBang bigBang = new BigBang(Scores.score(TWO));
		List<BigBangObject> objects = new ArrayList<BigBangObject>(bigBang.getObjects());
		assertEquals(2, objects.size());
		bigBang.select(new TreeSet<BigBangObject>(objects.subList(1, 2))).translate(0, 12);
		assertNotes("second note only", new double[][]{{0, 60, 120, 1, 0}, {1, 74, 100, 2, 1}}, bigBang.getComposition());
		bigBang.selectAll().translate(0, -12);
		assertNotes("all again", new double[][]{{0, 48, 120, 1, 0}, {1, 62, 100, 2, 1}}, bigBang.getComposition());
	}
	
	public void testWallpaper() {
		for (Denotator input : inputs()) {
			BigBang bigBang = new BigBang(input).addWallpaperDimension(0, 2).translate(2, 0).endWallpaper();
			assertNotes(input.getForm().getNameString(), new double[][]{
					{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}, {2, 60, 120, 1, 0},
					{3, 62, 100, 2, 1}, {4, 60, 120, 1, 0}, {5, 62, 100, 2, 1}}, bigBang.getComposition());
		}
	}
	
	public void testTwoDimensionalWallpaper() {
		BigBang bigBang = new BigBang(Scores.score(new double[][]{{0, 60, 120, 1, 0}}))
				.addWallpaperDimension(0, 1).translate(1, 0)
				.addWallpaperDimension(0, 1).translate(0, 7)
				.endWallpaper();
		assertNotes("2 by 2", new double[][]{{0, 60, 120, 1, 0}, {0, 67, 120, 1, 0}, {1, 60, 120, 1, 0}, {1, 67, 120, 1, 0}},
				bigBang.getComposition());
	}
	
	public void testAlterationMovesForegroundToNearestBackgroundPitch() {
		double[][] notes = {{0, 60, 120, 1, 0}, {1, 62, 120, 1, 0}, {10, 70, 120, 1, 0}, {11, 73, 120, 1, 0}};
		BigBang bigBang = new BigBang(Scores.score(notes));
		List<BigBangObject> objects = new ArrayList<BigBangObject>(bigBang.getObjects());
		bigBang.alter(new TreeSet<BigBangObject>(objects.subList(0, 2)), new TreeSet<BigBangObject>(objects.subList(2, 4)),
				Arrays.asList("Pitch"), "Onset", 1, 1);
		assertNotes("full alteration", new double[][]{{0, 70, 120, 1, 0}, {1, 70, 120, 1, 0}, {10, 70, 120, 1, 0}, {11, 73, 120, 1, 0}},
				bigBang.getComposition());
	}
	
	public void testUndoRedoAndReplay() {
		for (Denotator input : inputs()) {
			BigBang bigBang = new BigBang(input).translate(1, 0).scale(0, 60, 2, 1);
			double[][] both = {{2, 60, 120, 1, 0}, {4, 62, 100, 2, 1}};
			assertNotes("both", both, bigBang.getComposition());
			assertEquals(3, bigBang.getOperationCount());
			bigBang.undo();
			assertNotes("undo", new double[][]{{1, 60, 120, 1, 0}, {2, 62, 100, 2, 1}}, bigBang.getComposition());
			assertEquals(2, bigBang.getOperationCount());
			bigBang.redo();
			assertNotes("redo", both, bigBang.getComposition());
			assertNotes("replay", both, bigBang.replay());
		}
	}
	
	public void testIncrementalAndFullReplayAgree() {
		for (Denotator input : inputs()) {
			BigBang incremental = new BigBang(input, true);
			BigBang full = new BigBang(input, false);
			for (BigBang b : new BigBang[]{incremental, full}) {
				b.translate(1, 3).copying(true).rotate(1, 62, 0.75).copying(false).shear(0, 60, 0.2, 0)
						.undo().reflect(2, 62, 0, 1).addWallpaperDimension(0, 2).translate(0.5, 1).endWallpaper();
			}
			assertEquals(full.getComposition(), incremental.getComposition());
			assertEquals(full.getComposition(), incremental.replay());
		}
	}
	
	public void testClonedModelReplaysToTheSameComposition() {
		BigBang bigBang = new BigBang(Scores.soundScore(TWO)).translate(1, 2).copying(true).reflect(0, 61, 1, 0);
		BigBangModel copy = bigBang.getModel().clone();
		copy.updateComposition();
		assertEquals(bigBang.getComposition(), copy.getComposition());
	}
	
	public void testLargerScore() {
		double[][] notes = new double[300][];
		for (int i = 0; i < notes.length; i++) {
			notes[i] = new double[]{i * 0.25, 36 + (i * 7) % 48, 60 + i % 60, 0.5, i % 4};
		}
		PowerDenotator score = Scores.soundScore(notes);
		double[][] result = Scores.values(new BigBang(score).translate(10, 0).getComposition());
		assertEquals(300, result.length);
		assertEquals(10, result[0][0], TOLERANCE);
		assertEquals(10 + 299 * 0.25, result[299][0], TOLERANCE);
	}

}
