package org.rubato.rubettes.bigbang.headless;

import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;

/**
 * Runs a few BigBang operations on a four-note melody and prints the notes
 * before and after. Run it with rubato-core and rubato-bigbang on the class
 * path, for example:
 * <pre>
 * java --limit-modules java.base,java.xml,java.logging \
 *     -cp rubato-core.jar:rubato-bigbang.jar \
 *     org.rubato.rubettes.bigbang.headless.BigBangExample
 * </pre>
 */
public class BigBangExample {
	
	public static void main(String[] args) {
		PowerDenotator melody = Scores.score(new double[][]{
				{0, 60, 120, 1, 0}, {1, 64, 120, 1, 0}, {2, 67, 120, 1, 0}, {3, 72, 120, 1, 0}});
		System.out.println("input, form " + melody.getForm().getNameString() + ":");
		print(melody);
		
		BigBang bigBang = new BigBang(melody).axes("Onset", "Pitch");
		bigBang.translate(4, 0);
		bigBang.copying(true).reflect(0, 66, 1, 0);
		bigBang.copying(false).scale(0, 66, 0.5, 1);
		Denotator result = bigBang.getComposition();
		
		System.out.println("coordinates: " + bigBang.getCoordinateNames());
		System.out.println("after translating by 4 beats, adding the inversion around pitch 66 and halving the onsets ("
				+ bigBang.getOperationCount() + " operations):");
		print(result);
		bigBang.undo();
		System.out.println("after undoing the scaling:");
		print(bigBang.getComposition());
	}
	
	private static void print(Denotator score) {
		for (double[] note : Scores.values(score)) {
			System.out.println("  onset " + note[0] + ", pitch " + note[1] + ", loudness " + note[2]
					+ ", duration " + note[3] + ", voice " + note[4]);
		}
	}

}
