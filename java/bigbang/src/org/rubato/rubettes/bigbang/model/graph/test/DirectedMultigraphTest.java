package org.rubato.rubettes.bigbang.model.graph.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.rubettes.bigbang.model.graph.DirectedMultigraph;
import org.rubato.rubettes.bigbang.model.graph.Pair;

/**
 * Checks that DirectedMultigraph keeps the behaviour of the JUNG
 * DirectedSparseMultigraph and DijkstraShortestPath the operation graph used.
 */
public class DirectedMultigraphTest extends TestCase {
	
	private DirectedMultigraph<String,String> graph;
	
	protected void setUp() {
		this.graph = new DirectedMultigraph<String,String>();
		for (String v : new String[]{"0", "1", "2", "3"}) {
			this.graph.addVertex(v);
		}
	}
	
	public void testAddVertexTwiceKeepsOne() {
		assertFalse(this.graph.addVertex("0"));
		assertEquals(4, this.graph.getVertexCount());
	}
	
	public void testAddEdgeAddsMissingEndpointsAndParallelEdges() {
		assertTrue(this.graph.addEdge("a", "0", "1"));
		assertTrue(this.graph.addEdge("b", "0", "1"));
		assertTrue(this.graph.addEdge("c", "1", "9"));
		assertTrue(this.graph.containsVertex("9"));
		assertEquals(5, this.graph.getVertexCount());
		assertEquals(3, this.graph.getEdgeCount());
		assertEquals(Arrays.asList("a", "b"), List.copyOf(this.graph.findEdgeSet("0", "1")));
		assertEquals("a", this.graph.findEdge("0", "1"));
		assertEquals(new Pair<String>("0", "1"), this.graph.getEndpoints("b"));
		assertEquals("1", this.graph.getSource("c"));
		assertEquals("9", this.graph.getDest("c"));
	}
	
	public void testAddingAnEdgeAgain() {
		assertTrue(this.graph.addEdge("a", "0", "1"));
		assertFalse(this.graph.addEdge("a", "0", "1"));
		try {
			this.graph.addEdge("a", "1", "2");
			fail("an edge cannot be added again with other endpoints");
		} catch (IllegalArgumentException e) { }
		try {
			this.graph.addEdge(null, "1", "2");
			fail("a null edge cannot be added");
		} catch (IllegalArgumentException e) { }
		assertEquals(1, this.graph.getEdgeCount());
	}
	
	public void testInAndOutEdges() {
		this.graph.addEdge("a", "0", "1");
		this.graph.addEdge("b", "1", "2");
		this.graph.addEdge("c", "1", "3");
		assertEquals(Arrays.asList("b", "c"), List.copyOf(this.graph.getOutEdges("1")));
		assertEquals(Collections.singletonList("a"), List.copyOf(this.graph.getInEdges("1")));
		assertNull(this.graph.getOutEdges("x"));
		assertNull(this.graph.getInEdges("x"));
		assertNull(this.graph.findEdgeSet("x", "1"));
		assertNull(this.graph.findEdge("0", "2"));
	}
	
	public void testRemoveVertexRemovesIncidentEdges() {
		this.graph.addEdge("a", "0", "1");
		this.graph.addEdge("b", "1", "2");
		this.graph.addEdge("c", "2", "3");
		assertTrue(this.graph.removeVertex("1"));
		assertFalse(this.graph.removeVertex("1"));
		assertEquals(Collections.singletonList("c"), List.copyOf(this.graph.getEdges()));
		assertTrue(this.graph.getOutEdges("0").isEmpty());
		assertTrue(this.graph.getInEdges("2").isEmpty());
	}
	
	public void testRemoveEdge() {
		this.graph.addEdge("a", "0", "1");
		assertTrue(this.graph.removeEdge("a"));
		assertFalse(this.graph.removeEdge("a"));
		assertFalse(this.graph.containsEdge("a"));
		assertNull(this.graph.getEndpoints("a"));
		assertTrue(this.graph.getOutEdges("0").isEmpty());
		assertEquals(4, this.graph.getVertexCount());
	}
	
	public void testShortestPathTakesFewestEdges() {
		this.graph.addEdge("a", "0", "1");
		this.graph.addEdge("b", "1", "2");
		this.graph.addEdge("c", "2", "3");
		this.graph.addEdge("d", "0", "2");
		assertEquals(Arrays.asList("d", "c"), this.graph.getShortestPath("0", "3"));
		assertEquals(Collections.singletonList("b"), this.graph.getShortestPath("1", "2"));
		assertTrue(this.graph.isConnectedByPath("0", "3"));
	}
	
	public void testShortestPathEmptyWhenUnreachableOrSame() {
		this.graph.addEdge("a", "0", "1");
		assertTrue(this.graph.getShortestPath("1", "0").isEmpty());
		assertTrue(this.graph.getShortestPath("0", "0").isEmpty());
		assertFalse(this.graph.isConnectedByPath("1", "0"));
		assertFalse(this.graph.isConnectedByPath("0", "0"));
		try {
			this.graph.getShortestPath("0", "x");
			fail("an unknown vertex has no path");
		} catch (IllegalArgumentException e) { }
	}
	
	public void testTiesGoToTheEdgeAddedFirst() {
		this.graph.addEdge("late", "0", "1");
		this.graph.addEdge("early", "0", "1");
		this.graph.addEdge("x", "1", "2");
		this.graph.addEdge("y", "0", "3");
		this.graph.addEdge("z", "3", "2");
		assertEquals(Arrays.asList("late", "x"), this.graph.getShortestPath("0", "2"));
	}
	
	public void testIterationFollowsInsertionOrder() {
		this.graph.addEdge("c", "2", "3");
		this.graph.addEdge("a", "0", "1");
		this.graph.addEdge("b", "1", "2");
		assertEquals(Arrays.asList("c", "a", "b"), List.copyOf(this.graph.getEdges()));
		assertEquals(Arrays.asList("0", "1", "2", "3"), List.copyOf(this.graph.getVertices()));
	}

}
