package org.rubato.rubettes.bigbang.headless.test;

import junit.framework.TestCase;

import org.rubato.math.module.ZRing;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.Scores;

/**
 * The headless BigBang on addressed compositions. Stage 1 refused them,
 * because BigBangDenotatorManager failed to add each note of a Z-addressed
 * Score (RubatoAddressException, printed to stderr) and went on with no
 * objects. Stage 9 keeps the address (sources/BIGBANG-ADDRESSED-SPEC.md);
 * AddressedBigBangTest checks the operations.
 */
public class HeadlessAddressTest extends TestCase {

	private static final double[][] TWO = {{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}};

	public void testAnAddressedCompositionKeepsItsAddressAndObjects() {
		Denotator zScore = Scores.score(TWO).changeAddress(ZRing.ring);
		assertEquals(ZRing.ring, zScore.getAddress());
		BigBang bigBang = new BigBang(zScore);
		assertEquals(2, bigBang.getObjects().size());
		assertEquals(ZRing.ring, bigBang.getComposition().getAddress());
		assertNotes(TWO, bigBang.getComposition().atNull());
	}

	public void testItsZeroAddressedEvaluationStillWorks() {
		Denotator zScore = Scores.score(TWO).changeAddress(ZRing.ring);
		BigBang bigBang = new BigBang(zScore.atNull());
		assertEquals(2, bigBang.getObjects().size());
	}

	private static void assertNotes(double[][] expected, Denotator score) {
		double[][] actual = Scores.values(score);
		assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			for (int j = 0; j < expected[i].length; j++) {
				assertEquals(expected[i][j], actual[i][j], 1e-9);
			}
		}
	}
}
