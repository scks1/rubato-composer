package org.rubato.rubettes.bigbang.headless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rubato.math.gesture.Curve;
import org.rubato.math.gesture.Digraph;
import org.rubato.math.gesture.Gesture;
import org.rubato.math.gesture.PolylineCurve;
import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.RElement;
import org.rubato.math.module.RProperFreeElement;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.model.graph.BigBangOperationGraph;
import org.rubato.rubettes.bigbang.model.graph.CompositionState;
import org.rubato.rubettes.bigbang.model.operations.AbstractLocalTransformation;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;
import org.rubato.rubettes.bigbang.model.operations.AbstractTransformation;
import org.rubato.rubettes.bigbang.model.operations.InputCompositionOperation;
import org.rubato.rubettes.bigbang.model.operations.RotationTransformation;

/**
 * Reads a BigBang operation graph as a gesture in the sense of Mazzola, The Topos of Music III,
 * section 61.5, p. 914: a digraph, a point for each vertex and a curve for each arrow whose ends
 * meet the points of the arrow's tail and head. Each transformation is also compared with the
 * curve section 73.2.1, pp. 1152-1154 gives it as a gesture (sources/BIGBANG-GESTURES-SPEC.md).
 *
 * <p>The check works as follows.
 * <ol>
 * <li>{@link #check} refuses a graph that is not a path of operations starting with the input
 *     composition (D4) or whose objects change along the path (D3), and names the reason.</li>
 * <li>The gesture's vertices are the composition states after the input; a vertex point lists
 *     the coordinates of every object, in the order of the objects at the last state, each object
 *     tracked by identity (D1).</li>
 * <li>The curve of an arrow is the polyline through the composition with the arrow's operation
 *     at ratios 0, r/n, ..., r, where r is the ratio the operation holds (1 unless it was
 *     modified), and the other operations at their own ratios (D2, D8).</li>
 * <li>The report gives each arrow's gaps at its tail and head, its largest sample step and, for a
 *     transformation, the largest distance between its morphism and the book's curve.</li>
 * <li>The check restores every ratio and the selected state, and replays the composition.</li>
 * </ol>
 */
public final class OperationGestures {

	public static final double TOLERANCE = Gesture.TOLERANCE;

	private OperationGestures() { }

	/**
	 * Returns the coefficients {a, b, c, d, e, f} of the transformation's morphism at ratio s,
	 * x' = a x + b y + e and y' = c x + d y + f, and restores the transformation's ratio.
	 */
	public static double[] affineAt(AbstractTransformation op, double s) {
		double ratio = op.getModificationRatio();
		try {
			op.modify(s);
			return coefficients(op.getModuleMorphism());
		} finally {
			op.modify(ratio);
		}
	}

	/**
	 * Returns the coefficients of the book's curve at s, built from the transformation's map at
	 * ratio 1 alone. That map is written x -> c + A(x - c) + t with c the transformation's centre
	 * (the origin for a translation). The curve is x -> c + M(s)(x - c) + s t with
	 * M(s) = s A + (1 - s) I, or the rotation by s times the angle for a rotation
	 * (section 73.2.1.1-6, pp. 1152-1154).
	 */
	public static double[] bookAffineAt(AbstractTransformation op, double s) {
		double[] full = affineAt(op, 1);
		double[] c = op instanceof AbstractLocalTransformation
				? ((AbstractLocalTransformation)op).getCenter() : new double[2];
		double a = full[0], b = full[1], cc = full[2], d = full[3];
		//t = (e, f) - (c - A c)
		double tx = full[4] - (c[0] - (a * c[0] + b * c[1]));
		double ty = full[5] - (c[1] - (cc * c[0] + d * c[1]));
		double[][] m;
		if (op instanceof RotationTransformation) {
			double angle = s * ((RotationTransformation)op).getAngle();
			m = new double[][]{{Math.cos(angle), -Math.sin(angle)}, {Math.sin(angle), Math.cos(angle)}};
		} else {
			m = new double[][]{{s * a + (1 - s), s * b}, {s * cc, s * d + (1 - s)}};
		}
		//x' = M x + (c - M c + s t)
		double e = c[0] - (m[0][0] * c[0] + m[0][1] * c[1]) + s * tx;
		double f = c[1] - (m[1][0] * c[0] + m[1][1] * c[1]) + s * ty;
		return new double[]{m[0][0], m[0][1], m[1][0], m[1][1], e, f};
	}

	/**
	 * Returns the transformation as a gesture on the arrow digraph in R^6: the identity at the
	 * tail, the map at ratio 1 at the head, and the polyline through the maps at ratios
	 * 0, 1/samples, ..., 1 (section 73.2.1, p. 1152; D2).
	 */
	public static Gesture transformationGesture(AbstractTransformation op, int samples) {
		double[] knots = knots(samples);
		double[][] points = new double[knots.length][];
		for (int j = 0; j < knots.length; j++) {
			points[j] = affineAt(op, knots[j]);
		}
		double[][] vertices = {{1, 0, 0, 1, 0, 0}, affineAt(op, 1)};
		return Gesture.unchecked(Digraph.arrowN(1), vertices, new Curve[]{new PolylineCurve(knots, points)});
	}

	/** Checks the model's operation graph as a gesture and restores the model afterwards. */
	public static Report check(BigBangModel model, int samples) {
		BigBangOperationGraph graph = model.getTransformationGraph();
		CompositionState selected = graph.getSelectedCompositionState();
		List<AbstractOperation> ops = new ArrayList<AbstractOperation>(graph.getOperationsInLogicalOrder());
		Map<AbstractOperation, Double> ratios = new IdentityHashMap<AbstractOperation, Double>();
		for (AbstractOperation op : ops) {
			ratios.put(op, op.getModificationRatio());
		}
		try {
			return checkPath(model, graph, ops, samples);
		} finally {
			for (AbstractOperation op : ops) {
				op.modify(ratios.get(op));
			}
			if (selected != null) {
				model.selectCompositionState(selected);
			} else {
				model.deselectCompositionStates();
			}
			model.updateComposition();
		}
	}

	private static Report checkPath(BigBangModel model, BigBangOperationGraph graph, List<AbstractOperation> ops, int samples) {
		String linear = linearityProblem(graph, ops);
		if (linear != null) {
			return Report.refused(linear);
		}
		//objects at the last state, in their order there (D1)
		model.deselectCompositionStates();
		model.updateComposition();
		List<BigBangObject> objects = currentObjects(model);
		//getObjectsAt(op) holds the objects op receives; the objects op i produces are those
		//operation i + 1 receives, or the last state's for the last operation
		for (int i = 1; i < ops.size(); i++) {
			Set<BigBangObject> received = model.getObjects().getObjectsAt(ops.get(i));
			Set<BigBangObject> produced = i + 1 < ops.size()
					? model.getObjects().getObjectsAt(ops.get(i + 1)) : identitySet(objects);
			if (received == null || produced == null
					|| !identitySet(new ArrayList<BigBangObject>(received)).equals(identitySet(new ArrayList<BigBangObject>(produced)))) {
				return Report.refused("the objects change at operation " + i + " ("
						+ ops.get(i).getClass().getSimpleName() + "), so no single space holds the states");
			}
		}
		List<String> names = model.getObjects().getCoordinateSystemValueNames();
		int n = ops.size();
		double[][] vertices = new double[n][];
		for (int v = 1; v <= n; v++) {
			vertices[v - 1] = pointAt(model, graph.getCompositionStateAt(v), null, 1, objects, names);
		}
		double[] knots = knots(samples);
		Curve[] curves = new Curve[n - 1];
		List<ArrowReport> arrows = new ArrayList<ArrowReport>();
		int[][] arrowPairs = new int[n - 1][];
		for (int i = 1; i < n; i++) {
			AbstractOperation op = ops.get(i);
			double ratio = op.getModificationRatio();
			double[][] points = new double[knots.length][];
			for (int j = 0; j < knots.length; j++) {
				points[j] = pointAt(model, graph.getCompositionStateAt(i + 1), op, knots[j] * ratio, objects, names);
			}
			//the last sample, at 1 * ratio, leaves op at its own ratio for the later arrows
			curves[i - 1] = new PolylineCurve(knots, points);
			arrowPairs[i - 1] = new int[]{i - 1, i};
			double maxStep = 0;
			for (int j = 1; j < points.length; j++) {
				maxStep = Math.max(maxStep, Curve.distance(points[j - 1], points[j]));
			}
			double bookGap = Double.NaN;
			if (op instanceof AbstractTransformation) {
				bookGap = 0;
				for (double s : knots) {
					bookGap = Math.max(bookGap, Curve.distance(affineAt((AbstractTransformation)op, s),
							bookAffineAt((AbstractTransformation)op, s)));
				}
			}
			arrows.add(new ArrowReport(i, op.getClass().getSimpleName(),
					Curve.distance(points[0], vertices[i - 1]),
					Curve.distance(points[points.length - 1], vertices[i]), maxStep, bookGap));
		}
		Gesture gesture = Gesture.unchecked(Digraph.of(n, arrowPairs), vertices, curves);
		return new Report(gesture, null, arrows);
	}

	private static String linearityProblem(BigBangOperationGraph graph, List<AbstractOperation> ops) {
		if (ops.isEmpty() || !(ops.get(0) instanceof InputCompositionOperation)) {
			return "the graph does not start with an input composition";
		}
		if (graph.getEdgeCount() != ops.size() || graph.getVertexCount() != ops.size() + 1) {
			return "the graph is not linear: " + graph.getEdgeCount() + " operations between "
					+ graph.getVertexCount() + " states";
		}
		for (int i = 0; i < ops.size(); i++) {
			AbstractOperation op = ops.get(i);
			if (graph.getSource(op) != graph.getCompositionStateAt(i)
					|| graph.getDest(op) != graph.getCompositionStateAt(i + 1)) {
				return "the graph is not linear: operation " + i + " does not lead from state " + i
						+ " to state " + (i + 1);
			}
		}
		return null;
	}

	/**
	 * Returns the coordinates of the objects with the given state shown and, if op is given,
	 * op at ratio s.
	 */
	private static double[] pointAt(BigBangModel model, CompositionState state, AbstractOperation op, double s,
			List<BigBangObject> objects, List<String> names) {
		model.selectCompositionState(state);
		if (op != null) {
			op.modify(s);
		}
		model.updateComposition();
		double[] point = new double[objects.size() * names.size()];
		int k = 0;
		for (BigBangObject object : objects) {
			Map<String, Integer> seen = new HashMap<String, Integer>();
			for (String name : names) {
				int nth = seen.containsKey(name) ? seen.get(name) + 1 : 0;
				seen.put(name, nth);
				Double value = object.getNthValue(name, nth);
				point[k++] = value == null ? Double.NaN : value;
			}
		}
		return point;
	}

	private static List<BigBangObject> currentObjects(BigBangModel model) {
		Set<BigBangObject> objects = model.getObjects().getObjectsAt(null);
		return objects == null ? Collections.<BigBangObject>emptyList() : new ArrayList<BigBangObject>(objects);
	}

	private static Set<BigBangObject> identitySet(List<BigBangObject> objects) {
		Set<BigBangObject> set = Collections.newSetFromMap(new IdentityHashMap<BigBangObject, Boolean>());
		set.addAll(objects);
		return set;
	}

	private static double[] knots(int samples) {
		if (samples < 1) {
			throw new IllegalArgumentException("at least one sample interval needed: " + samples);
		}
		double[] knots = new double[samples + 1];
		for (int j = 0; j <= samples; j++) {
			knots[j] = (double)j / samples;
		}
		knots[samples] = 1;
		return knots;
	}

	private static double[] coefficients(ModuleMorphism morphism) {
		double[] o = map(morphism, 0, 0);
		double[] ex = map(morphism, 1, 0);
		double[] ey = map(morphism, 0, 1);
		return new double[]{ex[0] - o[0], ey[0] - o[0], ex[1] - o[1], ey[1] - o[1], o[0], o[1]};
	}

	private static double[] map(ModuleMorphism morphism, double x, double y) {
		try {
			ModuleElement image = morphism.map(RProperFreeElement.make(new double[]{x, y}));
			return new double[]{((RElement)image.getComponent(0)).getValue(), ((RElement)image.getComponent(1)).getValue()};
		} catch (Exception e) {
			throw new IllegalStateException("cannot evaluate the morphism at (" + x + ", " + y + ")", e);
		}
	}

	/** The result of {@link #check}: a gesture with a report per arrow, or the reason it was refused. */
	public static final class Report {

		private final Gesture gesture;
		private final String reason;
		private final List<ArrowReport> arrows;

		private Report(Gesture gesture, String reason, List<ArrowReport> arrows) {
			this.gesture = gesture;
			this.reason = reason;
			this.arrows = Collections.unmodifiableList(arrows);
		}

		private static Report refused(String reason) {
			return new Report(null, reason, Collections.<ArrowReport>emptyList());
		}

		/** The gesture, built without the endpoint check; null if the graph was refused. */
		public Gesture gesture() {
			return this.gesture;
		}

		/** Why the graph was refused; null if it was read. */
		public String reason() {
			return this.reason;
		}

		/** True if the graph was read and every curve end meets its vertex point within TOLERANCE. */
		public boolean isGesture() {
			return this.gesture != null && this.gesture.endpointGap() <= TOLERANCE;
		}

		/** The gesture's largest endpoint gap; NaN if the graph was refused. */
		public double endpointGap() {
			return this.gesture == null ? Double.NaN : this.gesture.endpointGap();
		}

		public List<ArrowReport> arrows() {
			return this.arrows;
		}

		public String text() {
			if (this.gesture == null) {
				return "not read: " + this.reason;
			}
			StringBuilder text = new StringBuilder();
			text.append(this.isGesture() ? "gesture" : "not a gesture").append(", ")
					.append(this.gesture.skeleton().vertexCount()).append(" states, endpoint gap ")
					.append(this.endpointGap());
			for (ArrowReport arrow : this.arrows) {
				text.append("\n  ").append(arrow);
			}
			return text.toString();
		}

		public String toString() {
			return this.text();
		}
	}

	/** One arrow's gaps, largest sample step and distance from the book's curve. */
	public static final class ArrowReport {

		private final int index;
		private final String operation;
		private final double tailGap, headGap, maxStep, bookGap;

		private ArrowReport(int index, String operation, double tailGap, double headGap, double maxStep, double bookGap) {
			this.index = index;
			this.operation = operation;
			this.tailGap = tailGap;
			this.headGap = headGap;
			this.maxStep = maxStep;
			this.bookGap = bookGap;
		}

		/** The operation's index in the graph's logical order. */
		public int index() {
			return this.index;
		}

		public String operation() {
			return this.operation;
		}

		/** Distance between the curve at 0 and the tail state's point. */
		public double tailGap() {
			return this.tailGap;
		}

		/** Distance between the curve at 1 and the head state's point. */
		public double headGap() {
			return this.headGap;
		}

		/** Largest distance between two neighbouring samples. */
		public double maxStep() {
			return this.maxStep;
		}

		/** Largest coefficient distance from the book's curve; NaN for an operation that is no transformation. */
		public double bookGap() {
			return this.bookGap;
		}

		public String toString() {
			Map<String, Double> values = new LinkedHashMap<String, Double>();
			values.put("tail gap", this.tailGap);
			values.put("head gap", this.headGap);
			values.put("largest step", this.maxStep);
			values.put("book gap", this.bookGap);
			return this.index + " " + this.operation + " " + values;
		}
	}

}
