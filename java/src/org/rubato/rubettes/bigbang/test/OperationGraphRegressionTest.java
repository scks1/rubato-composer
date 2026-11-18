package org.rubato.rubettes.bigbang.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.model.denotators.TransformationPaths;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.bigbang.model.graph.BigBangOperationGraph;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;

/**
 * Cases that threw in BigBangOperationGraph and BigBangDenotatorManager before they were fixed,
 * each reduced from a sequence found by OperationGraphRandomEditsTest.
 */
public class OperationGraphRegressionTest extends TestCase {

	private TestObjects objects;
	private TransformationPaths nodePaths;
	private BigBangModel model;

	protected void setUp() {
		this.objects = new TestObjects();
		this.nodePaths = this.objects.createStandardTransformationPaths(
				this.objects.SOUND_NODE_FORM, new int[][]{{0,0},{0,1}});
		this.model = new BigBangModel();
		this.model.setOrAddComposition(this.objects.flatSoundScore.copy());
	}

	//removing an operation whose end state has two outgoing operations used to throw a
	//ConcurrentModificationException while combineStates moved them
	public void testRemoveOperationBeforeBranchingState() {
		this.translate(1, 2);
		AbstractOperation first = this.graph().getLastAddedOperation();
		this.translate(0, 1);
		//alternative operation from the state after the first translation
		this.model.selectCompositionStateAt(2);
		this.translate(2, 0);
		this.model.deselectCompositionStates();
		assertEquals(2, this.graph().getOutEdges(this.graph().getCompositionStateAt(2)).size());
		Denotator before = this.model.getComposition().copy();

		this.model.removeOperation(first);
		this.assertConsistent();
		assertEquals(3, this.graph().getEdgeCount());
		assertEquals(2, this.graph().getOutEdges(this.graph().getCompositionStateAt(1)).size());

		this.model.undo();
		this.assertConsistent();
		assertEquals(4, this.graph().getEdgeCount());
		assertEquals(before, this.model.getComposition());
	}

	//undoing the removal of the last operation used to throw an IndexOutOfBoundsException in
	//insertOperation, which computed Integer.MAX_VALUE as the insertion index for a state without
	//outgoing operations
	public void testUndoRemovalOfLastOperation() {
		this.translate(1, 2);
		this.translate(0, 1);
		AbstractOperation last = this.graph().getLastAddedOperation();
		Denotator before = this.model.getComposition().copy();

		this.model.removeOperation(last);
		this.assertConsistent();
		assertEquals(2, this.graph().getEdgeCount());

		this.model.undo();
		this.assertConsistent();
		assertEquals(3, this.graph().getEdgeCount());
		assertEquals(last, this.graph().getLastAddedOperation());
		assertEquals(before, this.model.getComposition());
	}

	//a deletion keeps the paths of the objects it deleted. lowering the modification ratio of the input
	//so it produces fewer objects used to make the next replay throw an IndexOutOfBoundsException in
	//removeSiblingObjects
	public void testDeletionAfterInputProducesFewerObjects() {
		List<BigBangObject> all = new ArrayList<BigBangObject>(this.model.getObjects().getObjectsAt(null));
		assertEquals(3, all.size());
		this.model.deleteObjects(new TreeSet<BigBangObject>(all.subList(0, 2)));
		assertEquals(1, ((PowerDenotator)this.model.getComposition()).getFactorCount());

		this.model.modifyOperation(0, 0.4);
		this.translate(1, 1);
		this.assertConsistent();
		assertTrue(((PowerDenotator)this.model.getComposition()).getFactorCount() <= 1);
	}

	private BigBangOperationGraph graph() {
		return this.model.getTransformationGraph();
	}

	private void translate(double dx, double dy) {
		TreeSet<BigBangObject> all = new TreeSet<BigBangObject>(this.model.getObjects().getAllObjects());
		TransformationProperties p = new TransformationProperties(all, null, Arrays.asList(this.nodePaths), false, true);
		p.setCenter(new double[]{0, 0});
		p.setEndPoint(new double[]{dx, dy});
		this.model.translateObjects(p);
	}

	private void assertConsistent() {
		BigBangOperationGraph graph = this.graph();
		assertEquals(graph.getEdgeCount(), graph.getOperationsInLogicalOrder().size());
		for (int i = 0; i < graph.getVertexCount(); i++) {
			assertEquals(i, graph.getCompositionStateAt(i).getIndex());
			if (i > 0) {
				assertTrue("state " + i + " has an incoming operation",
						graph.getInEdges(graph.getCompositionStateAt(i)).size() > 0);
			}
		}
	}
}
