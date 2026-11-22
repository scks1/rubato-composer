package org.rubato.rubettes.bigbang.view.subview.graph;

import org.rubato.rubettes.bigbang.model.graph.BigBangOperationGraph;
import org.rubato.rubettes.bigbang.model.graph.CompositionState;
import org.rubato.rubettes.bigbang.model.graph.Pair;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;

/**
 * Copies BigBang's operation graph into a JUNG graph for the graph panel's
 * layout and viewer. The copy holds the same CompositionState and
 * AbstractOperation objects, so picking a vertex or an edge in the panel
 * selects the model's own state or operation. The model fires a GRAPH event
 * after every change to its graph, and the panel makes a new copy each time.
 */
public class OperationGraphSnapshot {
	
	private OperationGraphSnapshot() {
	}
	
	public static Graph<CompositionState,AbstractOperation> of(BigBangOperationGraph graph) {
		if (graph == null) {
			return null;
		}
		Graph<CompositionState,AbstractOperation> copy = new DirectedSparseMultigraph<CompositionState,AbstractOperation>();
		for (CompositionState state : graph.getVertices()) {
			copy.addVertex(state);
		}
		for (AbstractOperation operation : graph.getEdges()) {
			Pair<CompositionState> endpoints = graph.getEndpoints(operation);
			copy.addEdge(operation, endpoints.getFirst(), endpoints.getSecond());
		}
		return copy;
	}

}
