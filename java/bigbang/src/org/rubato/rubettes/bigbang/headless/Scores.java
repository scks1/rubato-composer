package org.rubato.rubettes.bigbang.headless;

import java.util.ArrayList;
import java.util.List;

import org.rubato.base.Repository;
import org.rubato.base.RubatoException;
import org.rubato.math.arith.Rational;
import org.rubato.math.module.QElement;
import org.rubato.math.module.RElement;
import org.rubato.math.module.RRing;
import org.rubato.math.module.ZElement;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.rubettes.util.SoundNoteGenerator;

/**
 * Builds and reads the Score and SoundScore denotators that {@link BigBang} works on.
 */
public final class Scores {
	
	private Scores() { }
	
	/**
	 * Returns a Score denotator with one Note per row. A row holds onset, pitch, loudness,
	 * duration and voice; pitch is rounded to a rational, loudness and voice to integers.
	 */
	public static PowerDenotator score(double[][] notes) {
		Repository repository = Repository.systemRepository();
		LimitForm noteForm = (LimitForm)repository.getForm("Note");
		try {
			List<Denotator> factors = new ArrayList<Denotator>();
			for (double[] n : notes) {
				List<Denotator> c = new ArrayList<Denotator>(5);
				c.add(new SimpleDenotator(null, (SimpleForm)repository.getForm("Onset"), new RElement(n[0])));
				c.add(new SimpleDenotator(null, (SimpleForm)repository.getForm("Pitch"), new QElement(new Rational(n[1]))));
				c.add(new SimpleDenotator(null, (SimpleForm)repository.getForm("Loudness"), new ZElement((int)Math.round(n[2]))));
				c.add(new SimpleDenotator(null, (SimpleForm)repository.getForm("Duration"), new RElement(n[3])));
				c.add(new SimpleDenotator(null, (SimpleForm)repository.getForm("Voice"), new ZElement((int)Math.round(n[4]))));
				factors.add(new LimitDenotator(null, noteForm, c));
			}
			return new PowerDenotator(NameDenotator.make(""), (PowerForm)repository.getForm("Score"), factors);
		} catch (RubatoException e) {
			throw new IllegalArgumentException("cannot build a Score from the given rows", e);
		}
	}
	
	/**
	 * Returns a flat SoundScore denotator with one SoundNode per row, each with no modulators and
	 * no satellites. A row holds onset, pitch, loudness, duration and voice.
	 */
	public static PowerDenotator soundScore(double[][] notes) {
		BigBang.registerForms();
		return new SoundNoteGenerator().createFlatSoundScore(notes);
	}
	
	/**
	 * Returns the onset, pitch, loudness, duration and voice of every note of a Score, or of
	 * every top-level node of a SoundScore, in the denotator's order.
	 */
	public static double[][] values(Denotator score) {
		List<Denotator> factors = ((PowerDenotator)score).getFactors();
		double[][] values = new double[factors.size()][5];
		try {
			for (int i = 0; i < factors.size(); i++) {
				Denotator note = factors.get(i);
				if (note.getForm().getNameString().endsWith("Node")) {
					note = ((LimitDenotator)note).getFactor(0);
				}
				for (int j = 0; j < 5; j++) {
					values[i][j] = ((RElement)note.getElement(new int[]{j, 0}).cast(RRing.ring)).getValue();
				}
			}
		} catch (RubatoException e) {
			throw new IllegalArgumentException("not a Score or SoundScore", e);
		}
		return values;
	}

}
