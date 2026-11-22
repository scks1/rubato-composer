package org.rubato.rubettes.bigbang.headless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.rubato.math.matrix.RMatrix;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.model.BigBangObjects;
import org.rubato.rubettes.bigbang.model.denotators.TransformationPaths;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;
import org.rubato.rubettes.bigbang.model.operations.AlterationOperation;
import org.rubato.rubettes.util.CoolFormRegistrant;
import org.rubato.rubettes.util.DenotatorPath;

/**
 * Applies BigBang operations to a composition with no display. A BigBang wraps
 * a {@link BigBangModel}, the model the BigBang rubette uses, and builds the
 * same {@link TransformationProperties} the rubette's view builds from a drag:
 * <ol>
 * <li>the transformed objects are the selection, by default every object of
 * the composition; a selection cannot be empty, since the model reads an
 * operation on no objects as one on every object ({@link #select});</li>
 * <li>the transformation acts on the two coordinates chosen with
 * {@link #axes(String, String)}, by default the first two;</li>
 * <li>centers and vectors are given in denotator values, as the view gives them
 * after converting screen positions.</li>
 * </ol>
 * Every call adds one new operation to the model's operation graph, which
 * replays the graph and updates the composition. {@link #getComposition()}
 * returns the result. Example, for a Score or SoundScore denotator {@code score}:
 * <pre>
 * Denotator result = new BigBang(score)
 *     .axes("Onset", "Pitch")
 *     .translate(1, 12)
 *     .rotate(0, 60, Math.PI / 2)
 *     .getComposition();
 * </pre>
 * The class uses no AWT or Swing and runs in a JVM started with
 * {@code --limit-modules java.base,java.xml,java.logging}. It is not thread-safe.
 */
public class BigBang {
	
	private final BigBangModel model;
	private int[] xy = new int[]{0, 1};
	private Set<BigBangObject> selection;
	private boolean copyAndTransform;
	
	/**
	 * Starts a new BigBang model on a copy of the given composition. The given denotator is not
	 * changed.
	 */
	public BigBang(Denotator composition) {
		this(composition, true);
	}
	
	/**
	 * Starts a new BigBang model on the given zero-addressed composition.
	 * @param incrementalReplay if false, the model executes the whole operation graph on every
	 * change, as it did before incremental replay
	 * @throws IllegalArgumentException if the composition is null or not zero-addressed
	 */
	public BigBang(Denotator composition, boolean incrementalReplay) {
		if (composition == null) {
			throw new IllegalArgumentException("composition is null");
		}
		if (!composition.nullAddressed()) {
			//BigBangDenotatorManager adds objects only at the zero address and drops the rest
			throw new IllegalArgumentException("composition has address " + composition.getAddress()
					+ ", but BigBang needs a zero-addressed one; evaluate it first, for example with atNull()");
		}
		BigBang.registerForms();
		this.model = new BigBangModel();
		this.model.setIncrementalReplay(incrementalReplay);
		this.model.setOrAddComposition(composition.copy());
	}
	
	/**
	 * Registers the forms BigBang works with, such as SoundScore and the FM forms, in the system
	 * repository, as the BigBang rubette does when it starts. Call it before building SoundScore
	 * denotators with the note generators. Registering again does nothing new.
	 */
	public static void registerForms() {
		new CoolFormRegistrant().registerAllTheCoolStuff();
	}
	
	/** Returns the wrapped model, for operations this class does not cover. */
	public BigBangModel getModel() {
		return this.model;
	}
	
	/**
	 * Returns the names of the coordinates the composition's objects have, in the order the
	 * rubette's view lists them, such as "Onset R" and "Pitch Q": the form name, then the
	 * module.
	 */
	public List<String> getCoordinateNames() {
		return Collections.unmodifiableList(this.model.getObjects().getCoordinateSystemValueNames());
	}
	
	/**
	 * Returns the index of the first coordinate with the given name, such as "Pitch Q", or failing
	 * that the first one whose form name is the given name, such as "Pitch".
	 * @throws IllegalArgumentException if no coordinate has the name
	 */
	public int getCoordinateIndex(String name) {
		List<String> names = this.model.getObjects().getCoordinateSystemValueNames();
		int index = names.indexOf(name);
		for (int i = 0; index < 0 && i < names.size(); i++) {
			if (names.get(i).startsWith(name + " ")) {
				index = i;
			}
		}
		if (index < 0) {
			throw new IllegalArgumentException("no coordinate named " + name + " in " + this.getCoordinateNames());
		}
		return index;
	}
	
	/** Makes later operations act on the two named coordinates. */
	public BigBang axes(String x, String y) {
		return this.axes(this.getCoordinateIndex(x), this.getCoordinateIndex(y));
	}
	
	/** Makes later operations act on the two coordinates at the given indices. */
	public BigBang axes(int x, int y) {
		int count = this.model.getObjects().getCoordinateSystemValueNames().size();
		if (x < 0 || y < 0 || x >= count || y >= count) {
			throw new IllegalArgumentException("coordinate indices " + x + ", " + y + " outside 0.." + (count-1));
		}
		this.xy = new int[]{x, y};
		return this;
	}
	
	/** Returns the objects of the composition as it stands after the last operation. */
	public Set<BigBangObject> getObjects() {
		Set<BigBangObject> objects = this.model.getObjects().getObjectsAt(null);
		if (objects == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(new TreeSet<BigBangObject>(objects));
	}
	
	/**
	 * Returns an object's value of a coordinate given by its full name
	 * ("Onset R") or its form name ("Onset"), resolved as
	 * {@link #getCoordinateIndex} resolves it, so the names that
	 * {@link #axes(String, String)} takes work here too. The model's
	 * {@link BigBangObject#getNthValue} takes full names only. Returns null
	 * when the object has no such value.
	 * @throws IllegalArgumentException if no coordinate has the name
	 */
	public Double value(BigBangObject object, String name) {
		List<String> names = this.model.getObjects().getCoordinateSystemValueNames();
		int index = this.getCoordinateIndex(name);
		String full = names.get(index);
		//the n-th coordinate of that name, for names that occur more than once
		int n = 0;
		for (int i = 0; i < index; i++) {
			if (names.get(i).equals(full)) {
				n++;
			}
		}
		return object.getNthValue(full, n);
	}
	
	/** Returns an immutable view of an object's values as they stand after the last operation. */
	public NoteValues values(BigBangObject object) {
		List<String> names = this.model.getObjects().getCoordinateSystemValueNames();
		Map<String, Double> values = new LinkedHashMap<String, Double>();
		Map<String, Integer> seen = new HashMap<String, Integer>();
		for (String name : names) {
			int n = seen.containsKey(name) ? seen.get(name) + 1 : 0;
			seen.put(name, n);
			if (n == 0) {
				values.put(name, object.getNthValue(name, 0));
			}
		}
		return new NoteValues(object, values);
	}
	
	/** Returns the values of every object, in the order of {@link #getObjects()}. */
	public List<NoteValues> notes() {
		List<NoteValues> notes = new ArrayList<NoteValues>();
		for (BigBangObject object : this.getObjects()) {
			notes.add(this.values(object));
		}
		return Collections.unmodifiableList(notes);
	}
	
	/** Returns the objects whose values pass the test, for {@link #select}; the set may be empty. */
	public Set<BigBangObject> objectsWhere(Predicate<NoteValues> test) {
		Set<BigBangObject> objects = new TreeSet<BigBangObject>();
		for (NoteValues note : this.notes()) {
			if (test.test(note)) {
				objects.add(note.getObject());
			}
		}
		return Collections.unmodifiableSet(objects);
	}
	
	/**
	 * Makes later operations act on the given objects only.
	 * <p>
	 * The set must not be empty. The model treats an operation with no
	 * objects as one on every object, which is what a drag with nothing
	 * selected does in the rubette's view, so an empty selection here would
	 * transform the whole composition. Use {@link #selectAll()} to act on
	 * every object, and check a computed selection for emptiness before
	 * passing it.
	 * @throws IllegalArgumentException if objects is null or empty
	 */
	public BigBang select(Set<BigBangObject> objects) {
		if (objects == null || objects.isEmpty()) {
			throw new IllegalArgumentException("the selection is empty; an operation on no objects would act on every object. "
				+ "Use selectAll() to act on every object, or skip the operation");
		}
		this.selection = new TreeSet<BigBangObject>(objects);
		return this;
	}
	
	/** Makes later operations act on every object of the composition, the default. */
	public BigBang selectAll() {
		this.selection = null;
		return this;
	}
	
	/**
	 * Sets whether later transformations keep the original objects and add transformed copies,
	 * as a drag with the copy modifier does. The default is false.
	 */
	public BigBang copying(boolean copyAndTransform) {
		this.copyAndTransform = copyAndTransform;
		return this;
	}
	
	/** Moves the selected objects by dx and dy along the two coordinates. */
	public BigBang translate(double dx, double dy) {
		this.model.translateObjects(this.properties(new double[]{0, 0}, new double[]{dx, dy}));
		return this;
	}
	
	/** Rotates the selected objects by angle radians, counterclockwise, around (cx, cy). */
	public BigBang rotate(double cx, double cy, double angle) {
		double[] center = new double[]{cx, cy};
		this.model.rotateObjects(this.properties(center, center), center.clone(), angle);
		return this;
	}
	
	/** Scales the selected objects by sx and sy around (cx, cy). */
	public BigBang scale(double cx, double cy, double sx, double sy) {
		double[] center = new double[]{cx, cy};
		this.model.scaleObjects(this.properties(center, center), new double[]{sx, sy});
		return this;
	}
	
	/** Reflects the selected objects in the line through (cx, cy) along the vector (vx, vy). */
	public BigBang reflect(double cx, double cy, double vx, double vy) {
		if (vx == 0 && vy == 0) {
			throw new IllegalArgumentException("the reflection vector is zero");
		}
		double[] center = new double[]{cx, cy};
		this.model.reflectObjects(this.properties(center, center), new double[]{vx, vy});
		return this;
	}
	
	/** Shears the selected objects by sx and sy around (cx, cy). */
	public BigBang shear(double cx, double cy, double sx, double sy) {
		double[] center = new double[]{cx, cy};
		this.model.shearObjects(this.properties(center, center), new double[]{sx, sy});
		return this;
	}
	
	/**
	 * Maps the selected objects with x' = matrix * (x - c) + c + shift, where c is (cx, cy) and
	 * matrix has two rows of two entries.
	 */
	public BigBang affine(double cx, double cy, double[] shift, double[][] matrix) {
		double[] center = new double[]{cx, cy};
		this.model.affineTransformObjects(this.properties(center, center), shift.clone(), new RMatrix(matrix));
		return this;
	}
	
	/**
	 * Starts a wallpaper dimension on the selected objects. The transformations added until
	 * {@link #endWallpaper()} become its motif's transformations, repeated from rangeFrom to
	 * rangeTo times. Several dimensions can be added before the wallpaper ends.
	 */
	public BigBang addWallpaperDimension(int rangeFrom, int rangeTo) {
		this.model.addWallpaperDimension(this.selected(), rangeFrom, rangeTo);
		return this;
	}
	
	/** Ends the wallpaper started with {@link #addWallpaperDimension(int, int)}. */
	public BigBang endWallpaper() {
		this.model.endWallpaper();
		return this;
	}
	
	/**
	 * Alters the foreground objects towards their nearest neighbours among the background
	 * objects in the named coordinates. The degree of alteration runs from startDegree to
	 * endDegree along the coordinate named degreesCoordinate.
	 */
	public BigBang alter(Set<BigBangObject> foreground, Set<BigBangObject> background, List<String> coordinates,
			String degreesCoordinate, double startDegree, double endDegree) {
		List<DenotatorPath> alterationCoordinates = new ArrayList<DenotatorPath>();
		for (String name : coordinates) {
			alterationCoordinates.add(this.valuePath(name));
		}
		this.model.addAlteration(this.valuePath(degreesCoordinate));
		AbstractOperation operation = this.model.getTransformationGraph().getLastAddedOperation();
		AlterationOperation alteration = (AlterationOperation)operation;
		alteration.setForegroundComposition(new TreeSet<BigBangObject>(foreground));
		alteration.setBackgroundComposition(new TreeSet<BigBangObject>(background));
		alteration.setAlterationCoordinates(alterationCoordinates);
		alteration.setStartDegree(startDegree);
		alteration.setEndDegree(endDegree);
		this.model.operationModified();
		return this;
	}
	
	/** Undoes the last operation. */
	public BigBang undo() {
		this.model.undo();
		return this;
	}
	
	/** Redoes the last undone operation. */
	public BigBang redo() {
		this.model.redo();
		return this;
	}
	
	/** Executes the whole operation graph again and returns the resulting composition. */
	public Denotator replay() {
		this.model.updateComposition();
		return this.model.getComposition();
	}
	
	/** Returns the composition after the operations added so far. */
	public Denotator getComposition() {
		return this.model.getComposition();
	}
	
	/**
	 * Returns the number of operations in the model's operation graph, counting the operation
	 * that holds the input composition.
	 */
	public int getOperationCount() {
		return this.model.getTransformationGraph().getEdgeCount();
	}
	
	/**
	 * Returns the path of the named value in the first object type's standard configuration,
	 * the path the view uses for alteration coordinates.
	 */
	public DenotatorPath valuePath(String coordinateName) {
		int index = this.getCoordinateIndex(coordinateName);
		List<List<DenotatorPath>> paths = this.model.getObjects().getAllObjectConfigurationsValuePathsAt(index);
		if (paths.isEmpty() || paths.get(0) == null || paths.get(0).isEmpty()) {
			throw new IllegalArgumentException("coordinate " + coordinateName + " has no value path");
		}
		return paths.get(0).get(0);
	}
	
	/**
	 * Returns the transformation paths of the two chosen coordinates for every object type, built
	 * as the rubette's view builds them.
	 */
	public static List<TransformationPaths> transformationPaths(BigBangObjects objects, int[] xy) {
		List<TransformationPaths> paths = new ArrayList<TransformationPaths>();
		for (int i = 0; i < objects.getObjectTypes().size(); i++) {
			TransformationPaths current = new TransformationPaths();
			current.setXYCoordinates(xy.clone());
			paths.add(current);
		}
		for (int i = 0; i < xy.length; i++) {
			List<List<DenotatorPath>> current = objects.getAllObjectConfigurationsValuePathsAt(xy[i]);
			for (int j = 0; j < current.size(); j++) {
				paths.get(j).setDomainPaths(i, current.get(j));
				paths.get(j).setCodomainPaths(i, current.get(j));
			}
		}
		return paths;
	}
	
	private TreeSet<BigBangObject> selected() {
		if (this.selection != null) {
			return new TreeSet<BigBangObject>(this.selection);
		}
		Set<BigBangObject> all = this.model.getObjects().getObjectsAt(null);
		return all != null ? new TreeSet<BigBangObject>(all) : new TreeSet<BigBangObject>();
	}
	
	private TransformationProperties properties(double[] center, double[] endPoint) {
		List<TransformationPaths> paths = BigBang.transformationPaths(this.model.getObjects(), this.xy);
		TransformationProperties properties = new TransformationProperties(this.selected(), null, paths, this.copyAndTransform, true);
		properties.setCenter(center.clone());
		properties.setEndPoint(endPoint.clone());
		return properties;
	}

}
