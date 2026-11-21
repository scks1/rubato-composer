package org.rubato.rubettes.util;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.math.module.QElement;
import org.rubato.math.module.RElement;
import org.rubato.math.module.ZElement;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.LimitDenotator;
import org.rubato.math.yoneda.LimitForm;
import org.rubato.math.yoneda.NameDenotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.math.yoneda.PowerForm;
import org.rubato.math.yoneda.SimpleDenotator;
import org.rubato.math.yoneda.SimpleForm;

/**
 * Checks that SoundNoteGenerator builds denotators of the registered SoundNote form,
 * which is Onset, Pitch, Loudness, Duration, Voice and Modulators (CoolFormRegistrant).
 */
public class SoundNoteGeneratorTest extends TestCase {

	static {
		new CoolFormRegistrant().registerAllTheCoolStuff();
	}

	private final Repository repository = Repository.systemRepository();
	private final LimitForm soundNoteForm = (LimitForm)this.repository.getForm("SoundNote");
	private final LimitForm soundNodeForm = (LimitForm)this.repository.getForm("SoundNode");
	private final PowerForm soundScoreForm = (PowerForm)this.repository.getForm("SoundScore");
	private final PowerForm modulatorsForm = (PowerForm)this.repository.getForm("Modulators");

	public void testSoundNoteFormHasNoLayer() {
		//the form definition the generator has to follow
		assertEquals(6, this.soundNoteForm.getFormCount());
		assertEquals(this.modulatorsForm, this.soundNoteForm.getForm(5));
	}

	public void testCarrierOnlyNodeHasNoModulators() {
		LimitDenotator note = this.fmNote(SoundNoteGenerator.FM_MODELS[0]);
		assertEquals(0, this.modulators(note).getFactorCount());
	}

	public void testDoubleModulatorNodeHasTwoModulators() throws Exception {
		LimitDenotator note = this.fmNote(SoundNoteGenerator.FM_MODELS[1]);
		PowerDenotator modulators = this.modulators(note);
		assertEquals(2, modulators.getFactorCount());
		List<Double> pitches = new ArrayList<Double>();
		for (Denotator modulator : modulators.getFactors()) {
			this.assertSoundNote((LimitDenotator)modulator);
			pitches.add(((QElement)((LimitDenotator)modulator).getElement(new int[]{1,0})).getValue().doubleValue());
			assertEquals(0, this.modulators((LimitDenotator)modulator).getFactorCount());
		}
		assertTrue(pitches.contains(20.0));
		assertTrue(pitches.contains(50.0));
	}

	public void testNestedModulatorNodeHasAModulatorOfAModulator() {
		LimitDenotator note = this.fmNote(SoundNoteGenerator.FM_MODELS[2]);
		PowerDenotator modulators = this.modulators(note);
		assertEquals(1, modulators.getFactorCount());
		LimitDenotator modulator = (LimitDenotator)modulators.getFactor(0);
		this.assertSoundNote(modulator);
		PowerDenotator second = this.modulators(modulator);
		assertEquals(1, second.getFactorCount());
		this.assertSoundNote((LimitDenotator)second.getFactor(0));
	}

	public void testScoreConvertsToSoundScoreKeepingTheNoteValues() throws Exception {
		SimpleForm onset = (SimpleForm)this.repository.getForm("Onset");
		SimpleForm pitch = (SimpleForm)this.repository.getForm("Pitch");
		SimpleForm loudness = (SimpleForm)this.repository.getForm("Loudness");
		SimpleForm duration = (SimpleForm)this.repository.getForm("Duration");
		SimpleForm voice = (SimpleForm)this.repository.getForm("Voice");
		LimitForm noteForm = (LimitForm)this.repository.getForm("Note");
		PowerForm scoreForm = (PowerForm)this.repository.getForm("Score");
		NameDenotator name = NameDenotator.make("");
		List<Denotator> notes = new ArrayList<Denotator>();
		for (int i = 0; i < 3; i++) {
			List<Denotator> coordinates = new ArrayList<Denotator>();
			coordinates.add(new SimpleDenotator(name, onset, new RElement(i)));
			coordinates.add(new SimpleDenotator(name, pitch, new QElement(60 + i)));
			coordinates.add(new SimpleDenotator(name, loudness, new ZElement(100 - i)));
			coordinates.add(new SimpleDenotator(name, duration, new RElement(1)));
			coordinates.add(new SimpleDenotator(name, voice, new ZElement(i)));
			notes.add(new LimitDenotator(name, noteForm, coordinates));
		}
		PowerDenotator score = new PowerDenotator(name, scoreForm, notes);

		Denotator converted = new SoundNoteGenerator().convertScore(score);
		assertNotNull(converted);
		assertEquals(this.soundScoreForm, converted.getForm());
		PowerDenotator soundScore = (PowerDenotator)converted;
		assertEquals(3, soundScore.getFactorCount());
		List<Double> onsets = new ArrayList<Double>();
		for (Denotator node : soundScore.getFactors()) {
			assertEquals(this.soundNodeForm, node.getForm());
			LimitDenotator note = (LimitDenotator)((LimitDenotator)node).getFactor(0);
			this.assertSoundNote(note);
			double o = ((RElement)note.getElement(new int[]{0,0})).getValue();
			onsets.add(o);
			int i = (int)Math.round(o);
			//the voice stays the note's own voice
			assertEquals(i, ((ZElement)note.getElement(new int[]{4,0})).getValue());
			assertEquals(100 - i, ((ZElement)note.getElement(new int[]{2,0})).getValue());
		}
		assertTrue(onsets.contains(0.0) && onsets.contains(1.0) && onsets.contains(2.0));
	}

	private LimitDenotator fmNote(String model) {
		SoundNoteGenerator generator = new SoundNoteGenerator();
		generator.setFMModel(model);
		LimitDenotator node = generator.createFMNodeDenotator(new double[]{0, 60, 100, 1, 0});
		assertNotNull("createFMNodeDenotator returns a node for " + model, node);
		assertEquals(this.soundNodeForm, node.getForm());
		LimitDenotator note = (LimitDenotator)node.getFactor(0);
		this.assertSoundNote(note);
		return note;
	}

	private void assertSoundNote(LimitDenotator note) {
		assertEquals(this.soundNoteForm, note.getForm());
		assertEquals(6, note.getFactorCount());
		for (int i = 0; i < 6; i++) {
			assertEquals(this.soundNoteForm.getForm(i), note.getFactor(i).getForm());
		}
	}

	private PowerDenotator modulators(LimitDenotator note) {
		Denotator modulators = note.getFactor(5);
		assertNotNull(modulators);
		assertEquals(this.modulatorsForm, modulators.getForm());
		return (PowerDenotator)modulators;
	}

}
