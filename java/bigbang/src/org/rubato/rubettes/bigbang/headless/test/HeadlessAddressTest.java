package org.rubato.rubettes.bigbang.headless.test;

import junit.framework.TestCase;

import org.rubato.math.module.ZRing;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.Scores;

/**
 * The headless BigBang works on zero-addressed compositions. Given a
 * Z-addressed Score, BigBangDenotatorManager failed to add each note
 * (RubatoAddressException, printed to stderr) and the model went on with
 * no objects, so every later operation acted on nothing.
 */
public class HeadlessAddressTest extends TestCase {

	private static final double[][] TWO = {{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}};

	public void testAnAddressedCompositionIsRefusedWithTheWayOut() {
		Denotator zScore = Scores.score(TWO).changeAddress(ZRing.ring);
		assertEquals(ZRing.ring, zScore.getAddress());
		try {
			new BigBang(zScore);
			fail("a Z-addressed composition must be refused");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("atNull()"));
			assertTrue(e.getMessage(), e.getMessage().contains("ZRing"));
		}
	}

	public void testItsZeroAddressedEvaluationStillWorks() {
		Denotator zScore = Scores.score(TWO).changeAddress(ZRing.ring);
		BigBang bigBang = new BigBang(zScore.atNull());
		assertEquals(2, bigBang.getObjects().size());
	}
}
