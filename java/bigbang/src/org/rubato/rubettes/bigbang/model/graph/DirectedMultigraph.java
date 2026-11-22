package org.rubato.rubettes.bigbang.model.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A directed graph that allows several edges between the same two vertices.
 * It replaces JUNG's DirectedSparseMultigraph in the BigBang model and keeps
 * the behaviour the operation graph relies on:
 * <ul>
 * <li>{@link #addEdge} adds missing endpoints, returns false when the edge is
 * already in the graph with the same endpoints, and throws
 * IllegalArgumentException when it is there with other endpoints or is null;</li>
 * <li>{@link #removeVertex} removes the vertex's incident edges first;</li>
 * <li>{@link #getOutEdges}, {@link #getInEdges} and {@link #findEdgeSet}
 * return null for a vertex that is not in the graph;</li>
 * <li>{@link #getShortestPath} returns the edges of a path with the fewest
 * edges, the empty list when source and target are equal or no path exists,
 * and throws IllegalArgumentException for a vertex that is not in the graph,
 * as JUNG's unweighted DijkstraShortestPath.getPath does.</li>
 * </ul>
 * Vertices and edges are kept in the order they were added, so iteration and
 * the choice among parallel edges no longer depend on identity hash codes.
 * Vertices and edges are compared with equals and hashCode.
 */
public class DirectedMultigraph<V,E> {
	
	//for each vertex, its incoming and outgoing edges
	private final Map<V,Set<E>> inEdges = new LinkedHashMap<V,Set<E>>();
	private final Map<V,Set<E>> outEdges = new LinkedHashMap<V,Set<E>>();
	private final Map<E,Pair<V>> edges = new LinkedHashMap<E,Pair<V>>();
	
	public boolean addVertex(V vertex) {
		if (vertex == null) {
			throw new IllegalArgumentException("vertex may not be null");
		}
		if (this.containsVertex(vertex)) {
			return false;
		}
		this.inEdges.put(vertex, new LinkedHashSet<E>());
		this.outEdges.put(vertex, new LinkedHashSet<E>());
		return true;
	}
	
	public boolean removeVertex(V vertex) {
		if (!this.containsVertex(vertex)) {
			return false;
		}
		Set<E> incident = new LinkedHashSet<E>(this.inEdges.get(vertex));
		incident.addAll(this.outEdges.get(vertex));
		for (E edge : incident) {
			this.removeEdge(edge);
		}
		this.inEdges.remove(vertex);
		this.outEdges.remove(vertex);
		return true;
	}
	
	public boolean addEdge(E edge, V source, V dest) {
		if (edge == null) {
			throw new IllegalArgumentException("input edge may not be null");
		}
		Pair<V> endpoints = new Pair<V>(source, dest);
		Pair<V> existing = this.edges.get(edge);
		if (existing != null) {
			if (existing.equals(endpoints)) {
				return false;
			}
			throw new IllegalArgumentException("edge " + edge + " exists in this graph with endpoints " + existing);
		}
		this.addVertex(source);
		this.addVertex(dest);
		this.edges.put(edge, endpoints);
		this.outEdges.get(source).add(edge);
		this.inEdges.get(dest).add(edge);
		return true;
	}
	
	public boolean removeEdge(E edge) {
		if (!this.containsEdge(edge)) {
			return false;
		}
		Pair<V> endpoints = this.edges.remove(edge);
		this.outEdges.get(endpoints.getFirst()).remove(edge);
		this.inEdges.get(endpoints.getSecond()).remove(edge);
		return true;
	}
	
	public boolean containsVertex(V vertex) {
		return vertex != null && this.outEdges.containsKey(vertex);
	}
	
	public boolean containsEdge(E edge) {
		return edge != null && this.edges.containsKey(edge);
	}
	
	public int getVertexCount() {
		return this.outEdges.size();
	}
	
	public int getEdgeCount() {
		return this.edges.size();
	}
	
	public Collection<V> getVertices() {
		return Collections.unmodifiableSet(this.outEdges.keySet());
	}
	
	public Collection<E> getEdges() {
		return Collections.unmodifiableSet(this.edges.keySet());
	}
	
	public Collection<E> getOutEdges(V vertex) {
		if (!this.containsVertex(vertex)) {
			return null;
		}
		return Collections.unmodifiableSet(this.outEdges.get(vertex));
	}
	
	public Collection<E> getInEdges(V vertex) {
		if (!this.containsVertex(vertex)) {
			return null;
		}
		return Collections.unmodifiableSet(this.inEdges.get(vertex));
	}
	
	public Pair<V> getEndpoints(E edge) {
		return this.edges.get(edge);
	}
	
	public V getSource(E edge) {
		Pair<V> endpoints = this.edges.get(edge);
		return endpoints != null ? endpoints.getFirst() : null;
	}
	
	public V getDest(E edge) {
		Pair<V> endpoints = this.edges.get(edge);
		return endpoints != null ? endpoints.getSecond() : null;
	}
	
	/**
	 * Returns the first edge added from source to dest, or null if there is none or either
	 * vertex is not in the graph.
	 */
	public E findEdge(V source, V dest) {
		Collection<E> found = this.findEdgeSet(source, dest);
		if (found == null || found.isEmpty()) {
			return null;
		}
		return found.iterator().next();
	}
	
	/**
	 * Returns the edges from source to dest in the order they were added, or null if either
	 * vertex is not in the graph.
	 */
	public Collection<E> findEdgeSet(V source, V dest) {
		if (!this.containsVertex(source) || !this.containsVertex(dest)) {
			return null;
		}
		List<E> found = new ArrayList<E>();
		for (E edge : this.outEdges.get(source)) {
			if (this.edges.get(edge).getSecond().equals(dest)) {
				found.add(edge);
			}
		}
		return Collections.unmodifiableList(found);
	}
	
	/**
	 * Returns the edges of a path from source to target with the fewest edges, found by a
	 * breadth-first search that follows out edges in the order they were added. Returns the
	 * empty list if source equals target or target cannot be reached.
	 * @throws IllegalArgumentException if source or target is not in the graph
	 */
	public List<E> getShortestPath(V source, V target) {
		if (!this.containsVertex(source)) {
			throw new IllegalArgumentException("Specified graph does not contain the specified source vertex");
		}
		if (!this.containsVertex(target)) {
			throw new IllegalArgumentException("Specified graph does not contain the specified target vertex");
		}
		List<E> path = new ArrayList<E>();
		if (source.equals(target)) {
			return path;
		}
		Map<V,E> reachedBy = new HashMap<V,E>();
		Set<V> visited = new LinkedHashSet<V>();
		Deque<V> queue = new ArrayDeque<V>();
		visited.add(source);
		queue.add(source);
		while (!queue.isEmpty()) {
			V current = queue.poll();
			for (E edge : this.outEdges.get(current)) {
				V next = this.edges.get(edge).getSecond();
				if (visited.add(next)) {
					reachedBy.put(next, edge);
					if (next.equals(target)) {
						for (V v = target; !v.equals(source); v = this.edges.get(reachedBy.get(v)).getFirst()) {
							path.add(0, reachedBy.get(v));
						}
						return path;
					}
					queue.add(next);
				}
			}
		}
		return path;
	}
	
	/**
	 * Returns true if a path of at least one edge leads from source to target.
	 */
	public boolean isConnectedByPath(V source, V target) {
		return !this.getShortestPath(source, target).isEmpty();
	}

}
