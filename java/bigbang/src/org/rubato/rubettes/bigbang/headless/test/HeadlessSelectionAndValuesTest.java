package org.rubato.rubettes.bigbang.headless.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.NoteValues;
import org.rubato.rubettes.bigbang.headless.Scores;
import org.rubato.rubettes.bigbang.model.BigBangObject;

/**
 * Empty selections and value names in the headless BigBang, from the
 * "Convergences" test piece of 2026-11-22: selecting the notes of a voice
 * that had none and then transforming moved every note, and an object's
 * values could be read only by full names such as "Onset R".
 */
public class HeadlessSelectionAndValuesTest extends TestCase {

	private static final double[][] THREE = {{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}, {2, 64, 80, 0.5, 1}};

	private static List<Denotator> inputs() {
		return Arrays.asList((Denotator)Scores.score(THREE), Scores.soundScore(THREE));
	}

	public void testAnEmptySelectionIsRefusedAndChangesNothing() {
		for (Denotator input : inputs()) {
			BigBang bigBang = new BigBang(input);
			int operations = bigBang.getOperationCount();
			//voice 5 has no notes
			Set<BigBangObject> none = bigBang.objectsWhere(n -> n.getVoice() == 5);
			assertTrue(none.isEmpty());
			try {
				bigBang.select(none);
				fail("an empty selection must be refused");
			} catch (IllegalArgumentException e) {
				assertTrue(e.getMessage(), e.getMessage().contains("selectAll()"));
			}
			try {
				bigBang.select(null);
				fail("a null selection must be refused");
			} catch (IllegalArgumentException e) { }
			assertEquals("a refused selection adds no operation", operations, bigBang.getOperationCount());
			//the refused call leaves the previous selection, here the default of every object
			bigBang.translate(0, 1);
			assertEquals(operations + 1, bigBang.getOperationCount());
			assertEquals(Arrays.asList(61.0, 63.0, 65.0), pitches(bigBang));
		}
	}

	public void testAnEmptySelectionAfterAnotherKeepsTheOther() {
		BigBang bigBang = new BigBang(Scores.score(THREE));
		bigBang.select(bigBang.objectsWhere(n -> n.getVoice() == 1));
		try {
			bigBang.select(Collections.<BigBangObject>emptySet());
			fail();
		} catch (IllegalArgumentException e) { }
		bigBang.translate(0, 12);
		assertEquals(Arrays.asList(60.0, 74.0, 76.0), pitches(bigBang));
	}

	public void testSelectAllTargetsEveryObjectAtTheTimeOfTheOperation() {
		BigBang bigBang = new BigBang(Scores.score(THREE));
		bigBang.select(bigBang.objectsWhere(n -> n.getOnset() < 1)).copying(true).translate(4, 0).copying(false);
		assertEquals(4, bigBang.getObjects().size());
		//selectAll includes the copy made after the selection was set
		bigBang.selectAll().translate(0, 1);
		assertEquals(Arrays.asList(61.0, 63.0, 65.0, 61.0), pitches(bigBang));
	}

	public void testShortAndFullNamesReadTheSameValues() {
		for (Denotator input : inputs()) {
			BigBang bigBang = new BigBang(input);
			BigBangObject second = bigBang.notes().get(1).getObject();
			//the model takes full names only, as before
			assertNull(second.getNthValue("Onset", 0));
			assertEquals(1.0, second.getNthValue("Onset R", 0));
			String[][] names = {{"Onset", "Onset R"}, {"Pitch", "Pitch Q"}, {"Loudness", "Loudness Z"}, {"Duration", "Duration R"}, {"Voice", "Voice Z"}};
			double[] expected = THREE[1];
			for (int i = 0; i < names.length; i++) {
				assertEquals(names[i][0], expected[i], bigBang.value(second, names[i][0]), 0);
				assertEquals(names[i][1], expected[i], bigBang.value(second, names[i][1]), 0);
			}
			try {
				bigBang.value(second, "Tempo");
				fail();
			} catch (IllegalArgumentException e) { }
			NoteValues v = bigBang.values(second);
			assertEquals(1.0, v.getOnset());
			assertEquals(62.0, v.getPitch());
			assertEquals(100.0, v.getLoudness());
			assertEquals(2.0, v.getDuration());
			assertEquals(1.0, v.getVoice());
			assertEquals(62.0, v.get("Pitch Q"));
			assertEquals(Arrays.asList("Onset R", "Pitch Q", "Loudness Z", "Duration R", "Voice Z"),
					Arrays.asList(v.getValues().keySet().toArray()).subList(0, 5));
			try {
				v.getValues().put("Onset R", 3.0);
				fail("the view is immutable");
			} catch (UnsupportedOperationException e) { }
		}
	}

	public void testValuesFollowTheOperations() {
		BigBang bigBang = new BigBang(Scores.score(THREE));
		NoteValues before = bigBang.notes().get(0);
		bigBang.axes("Onset", "Pitch").translate(1, 2);
		assertEquals(0.0, before.getOnset());
		assertEquals(Arrays.asList(1.0, 2.0, 3.0), onsets(bigBang));
		assertEquals(Arrays.asList(62.0, 64.0, 66.0), pitches(bigBang));
	}

	private static List<Double> pitches(BigBang bigBang) {
		List<Double> l = new java.util.ArrayList<Double>();
		for (NoteValues n : bigBang.notes()) l.add(n.getPitch());
		return l;
	}

	private static List<Double> onsets(BigBang bigBang) {
		List<Double> l = new java.util.ArrayList<Double>();
		for (NoteValues n : bigBang.notes()) l.add(n.getOnset());
		return l;
	}
}
