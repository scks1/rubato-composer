package org.rubato.rubettes.bigbang.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.math.arith.Rational;
import org.rubato.math.module.Module;
import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.QRing;
import org.rubato.math.module.RElement;
import org.rubato.math.module.RRing;
import org.rubato.math.module.ZElement;
import org.rubato.math.module.morphism.CanonicalMorphism;
import org.rubato.math.module.morphism.ConstantMorphism;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.QAffineMorphism;
import org.rubato.math.module.morphism.RAffineMorphism;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;
import org.rubato.math.module.ZRing;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.bigbang.BigBangRubette;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.view.model.BigBangSwingView;
import org.rubato.rubettes.bigbang.view.model.FactsViewState;
import org.rubato.rubettes.util.Point2D;

/**
 * The BigBang rubette's view on a Z-addressed Score (sources/BIGBANG-ADDRESSED-SPEC.md).
 * The rubette hands its input to BigBangModel.setOrAddComposition, which before stage 9
 * failed to add each addressed note and left the view empty. The view shows each arrow's
 * value at zero, and a translation drawn in the view acts on the arrows. Needs a display,
 * as the Maven build's xvfb-run gives it. The input repeats the bigbang module's
 * AddressedBigBangTest notes, since this module's tests cannot see that one's.
 */
public class AddressedViewTest extends TestCase {

	private BigBangModel model;
	private BigBangSwingView view;
	private FactsViewState facts;

	private static Field field(Class<?> c, String name) throws Exception {
		Field field = c.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private void startView(Denotator score) throws Exception {
		BigBang.registerForms();
		BigBangRubette rubette = new BigBangRubette();
		this.model = (BigBangModel)field(BigBangRubette.class, "model").get(rubette);
		this.view = (BigBangSwingView)field(BigBangRubette.class, "view").get(rubette);
		this.facts = (FactsViewState)field(org.rubato.rubettes.bigbang.view.model.BigBangView.class, "factsViewState").get(this.view);
		this.model.setOrAddComposition(score.copy());
	}

	private double invert(double value, int index) {
		double at0 = this.facts.getDenotatorValue(0, index);
		double at1000 = this.facts.getDenotatorValue(1000, index);
		return (value - at0) * 1000 / (at1000 - at0);
	}

	//the four Z-addressed notes of the bigbang module's AddressedBigBangTest: onsets 2x, 1, 3
	//and 5 - x, pitches 60 + x, 64, 67 and 72 - 2x
	private static Denotator zScore() throws Exception {
		Repository rep = Repository.systemRepository();
		double[][] onsets = {{2, 0}, {0, 1}, {0, 3}, {-1, 5}};
		int[][] pitches = {{1, 60}, {0, 64}, {0, 67}, {-2, 72}};
		int[] loudness = {120, 100, 90, 80};
		double[] durations = {1, 2, 1, 3};
		List<Denotator> notes = new ArrayList<Denotator>();
		for (int i = 0; i < 4; i++) {
			ModuleMorphism onset = new RAffineMorphism(onsets[i][0], onsets[i][1]).compose(CanonicalMorphism.make(ZRing.ring, RRing.ring));
			ModuleMorphism pitch = new QAffineMorphism(new Rational(pitches[i][0]), new Rational(pitches[i][1])).compose(CanonicalMorphism.make(ZRing.ring, QRing.ring));
			List<Denotator> c = new ArrayList<Denotator>();
			c.add(new SimpleDenotator(null, (SimpleForm)rep.getForm("Onset"), onset));
			c.add(new SimpleDenotator(null, (SimpleForm)rep.getForm("Pitch"), pitch));
			c.add(new SimpleDenotator(null, (SimpleForm)rep.getForm("Loudness"), constant(new ZElement(loudness[i]))));
			c.add(new SimpleDenotator(null, (SimpleForm)rep.getForm("Duration"), constant(new RElement(durations[i]))));
			c.add(new SimpleDenotator(null, (SimpleForm)rep.getForm("Voice"), constant(new ZElement(i))));
			notes.add(new LimitDenotator(null, (LimitForm)rep.getForm("Note"), c));
		}
		return new PowerDenotator(NameDenotator.make(""), ZRing.ring, (PowerForm)rep.getForm("Score"), notes);
	}

	private static ModuleMorphism constant(ModuleElement value) {
		Module z = ZRing.ring;
		return new ConstantMorphism(z, value);
	}

	private static String values(Denotator score) throws Exception {
		List<String> rows = new ArrayList<String>();
		for (Denotator note : ((PowerDenotator)score).getFactors()) {
			StringBuilder row = new StringBuilder();
			for (int j = 0; j < 5; j++) {
				row.append(String.format("%.6f ", ((RElement)note.getElement(new int[]{j, 0}).cast(RRing.ring)).getValue() + 0.0));
			}
			rows.add(row.toString());
		}
		Collections.sort(rows);
		return rows.toString();
	}

	public void testTheViewShowsEveryAddressedNote() throws Exception {
		this.startView(zScore());
		assertEquals(ZRing.ring, this.model.getComposition().getAddress());
		assertEquals(4, this.model.getObjects().getObjectsAt(null).size());
		//the view's ranges are those of the values at zero: onsets 0 to 5, pitches 60 to 72
		assertEquals(0, this.view.getDisplayObjects().getMinValues().get(0), 1e-9);
		assertEquals(5, this.view.getDisplayObjects().getMaxValues().get(0), 1e-9);
		assertEquals(60, this.view.getDisplayObjects().getMinValues().get(1), 1e-9);
		assertEquals(72, this.view.getDisplayObjects().getMaxValues().get(1), 1e-9);
	}

	public void testATranslationDrawnInTheViewActsOnTheArrows() throws Exception {
		Denotator score = zScore();
		this.startView(score);
		Set<BigBangObject> all = new TreeSet<BigBangObject>(this.model.getObjects().getObjectsAt(null));
		this.view.getDisplayObjects().selectObjects(all);
		Point2D from = new Point2D(this.invert(0, 0), this.invert(0, 1));
		Point2D to = new Point2D(this.invert(1.5, 0), this.invert(7, 1));
		this.view.translateSelectedObjects(from, to, false, true);
		Denotator result = this.model.getComposition();
		assertEquals(ZRing.ring, result.getAddress());
		Denotator expected = new BigBang(score).translate(1.5, 7).getComposition();
		for (int a = -1; a <= 2; a++) {
			assertEquals("at " + a, values(expected.at(new ZElement(a))),
					values(result.at(new ZElement(a))));
		}
	}
}
