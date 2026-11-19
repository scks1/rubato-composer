package org.rubato.rubettes.bigbang.test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.util.SoundNoteGenerator;

/**
 * Deletes selections that contain a parent note together with some or all of its satellites.
 */
public class DeleteParentWithSatellitesTest extends TestCase {

	private TestObjects objects;
	private BigBangModel model;

	protected void setUp() {
		this.objects = new TestObjects();
		this.model = new BigBangModel();
		this.model.setOrAddComposition(this.objects.multiLevelSoundScore.copy());
	}

	public void testDeleteParentAndOneSatellite() {
		BigBangObject parent = this.firstParent();
		TreeSet<BigBangObject> selection = new TreeSet<BigBangObject>();
		selection.add(parent);
		selection.add(parent.getChildren().iterator().next());
		this.deleteAndCheck(selection, parent);
	}

	public void testDeleteParentAndAllDescendants() {
		BigBangObject parent = this.firstParent();
		TreeSet<BigBangObject> selection = new TreeSet<BigBangObject>();
		this.addWithDescendants(parent, selection);
		this.deleteAndCheck(selection, parent);
	}

	public void testDeleteParentAlone() {
		BigBangObject parent = this.firstParent();
		this.model.deleteObjects(new TreeSet<BigBangObject>(List.of(parent)));
		//the score is one chain of three notes, so all of them go
		assertEquals(0, this.model.getObjects().getObjectsAt(null).size());
		this.model.undo();
		assertEquals(this.objects.multiLevelSoundScore, this.model.getComposition());
		assertEquals(3, this.model.getObjects().getObjectsAt(null).size());
	}
	
	//two parents with one satellite each, and a lone note. deleting the first parent, alone or with
	//its satellite, leaves the second parent with its satellite and the lone note
	public void testDeleteFirstOfTwoParents() {
		for (boolean withSatellite : new boolean[]{false, true}) {
			String message = withSatellite ? "parent and satellite" : "parent alone";
			BigBangModel m = new BigBangModel();
			m.setOrAddComposition(new SoundNoteGenerator().createFlatSoundScore(new double[][]{
				{0, 60, 120, 1, 0}, {1, 62, 120, 1, 0}, {2, 64, 120, 1, 0}, {3, 65, 120, 1, 0}, {4, 67, 120, 1, 0}}));
			List<BigBangObject> notes = new ArrayList<BigBangObject>(m.getObjects().getObjectsAt(null));
			m.buildSatellites(new TreeSet<BigBangObject>(List.of(notes.get(1))), notes.get(0), 0);
			List<BigBangObject> top = this.topLevel(m);
			assertEquals(message, 4, top.size());
			m.buildSatellites(new TreeSet<BigBangObject>(List.of(top.get(2))), top.get(1), 0);
			List<BigBangObject> parents = this.parentsWithChildren(m);
			assertEquals(message, 2, parents.size());
			assertEquals(message, 5, m.getObjects().getObjectsAt(null).size());
			assertEquals(message, 3, ((PowerDenotator)m.getComposition()).getFactorCount());
			Denotator before = m.getComposition().copy();
			Denotator secondNode = ((PowerDenotator)m.getComposition()).getFactor(1).copy();
			
			TreeSet<BigBangObject> selection = new TreeSet<BigBangObject>(List.of(parents.get(0)));
			if (withSatellite) {
				selection.addAll(parents.get(0).getChildren());
				assertEquals(message, 2, selection.size());
			}
			m.deleteObjects(selection);
			this.checkSecondParentRemains(m, secondNode, message);
			m.undo();
			assertEquals(message, before, m.getComposition());
			assertEquals(message, 5, m.getObjects().getObjectsAt(null).size());
			assertEquals(message, 2, this.parentsWithChildren(m).size());
			m.redo();
			this.checkSecondParentRemains(m, secondNode, message);
		}
	}
	
	private void checkSecondParentRemains(BigBangModel m, Denotator secondNode, String message) {
		PowerDenotator score = (PowerDenotator)m.getComposition();
		assertEquals(message, 2, score.getFactorCount());
		assertEquals(message, secondNode, score.getFactor(0));
		assertEquals(message, 3, m.getObjects().getObjectsAt(null).size());
		List<BigBangObject> parents = this.parentsWithChildren(m);
		assertEquals(message, 1, parents.size());
		assertEquals(message, 1, parents.get(0).getChildren().size());
		for (BigBangObject object : m.getObjects().getObjectsAt(null)) {
			assertNotNull(message, object.getTopDenotatorPath());
		}
	}
	
	private List<BigBangObject> topLevel(BigBangModel m) {
		List<BigBangObject> top = new ArrayList<BigBangObject>();
		for (BigBangObject object : m.getObjects().getObjectsAt(null)) {
			if (object.getParent() == null) {
				top.add(object);
			}
		}
		return top;
	}
	
	private List<BigBangObject> parentsWithChildren(BigBangModel m) {
		List<BigBangObject> parents = new ArrayList<BigBangObject>();
		for (BigBangObject object : m.getObjects().getObjectsAt(null)) {
			if (object.getParent() == null && object.hasChildren()) {
				parents.add(object);
			}
		}
		return parents;
	}
	
	public void testDeleteEverything() {
		TreeSet<BigBangObject> selection = new TreeSet<BigBangObject>(this.model.getObjects().getObjectsAt(null));
		Denotator before = this.model.getComposition().copy();
		this.model.deleteObjects(selection);
		assertEquals(0, this.model.getObjects().getObjectsAt(null).size());
		this.model.undo();
		assertEquals(before, this.model.getComposition());
		this.model.redo();
		assertEquals(0, this.model.getObjects().getObjectsAt(null).size());
	}

	//deleting the parent removes its satellites with it, so the result equals deleting the parent alone
	private void deleteAndCheck(TreeSet<BigBangObject> selection, BigBangObject parent) {
		Denotator before = this.model.getComposition().copy();
		BigBangModel parentOnly = new BigBangModel();
		parentOnly.setOrAddComposition(this.objects.multiLevelSoundScore.copy());
		parentOnly.deleteObjects(new TreeSet<BigBangObject>(List.of(this.firstParent(parentOnly))));
		Denotator expected = parentOnly.getComposition();

		this.model.deleteObjects(selection);
		assertEquals(expected, this.model.getComposition());
		int count = this.model.getObjects().getObjectsAt(null).size();
		this.model.undo();
		assertEquals(before, this.model.getComposition());
		this.model.redo();
		assertEquals(expected, this.model.getComposition());
		assertEquals(count, this.model.getObjects().getObjectsAt(null).size());
	}

	private BigBangObject firstParent() {
		return this.firstParent(this.model);
	}

	private BigBangObject firstParent(BigBangModel model) {
		for (BigBangObject object : model.getObjects().getObjectsAt(null)) {
			if (object.getParent() == null && object.hasChildren()) {
				return object;
			}
		}
		fail("no parent with satellites");
		return null;
	}

	private void addWithDescendants(BigBangObject object, TreeSet<BigBangObject> selection) {
		selection.add(object);
		for (BigBangObject child : new ArrayList<BigBangObject>(object.getChildren())) {
			this.addWithDescendants(child, selection);
		}
	}

}
