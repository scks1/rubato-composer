package org.rubato.rubettes.bigbang.view.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rubato.math.module.ModuleElement;
import org.rubato.math.module.RElement;
import org.rubato.math.module.RProperFreeElement;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.yoneda.Form;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.model.BigBangObjects;

/**
 * Computes the values a view shows while the user drags a new transformation, from the values
 * the objects hold after the last update, without executing any operation or changing any object.
 *
 * <p>The preview reproduces how BigBangMapper changes absolute values when it maps the objects of
 * a transformation that is the last operation of the shown path:
 * <ol>
 * <li>A mapped object moves to the transformation's image of its absolute position. With an
 * anchor, the transformation is applied relative to the object's parent.</li>
 * <li>The mapper stores each object relative to the absolute position its parent had before the
 * mapping, so every object, mapped or not, also moves by its parent's displacement.</li>
 * </ol>
 * As a result, a mapped object computes to f(base) + (parent's preview − parent's base), with f
 * the transformation for mapped objects and the identity otherwise.
 *
 * <p>The preview leaves out what the denotators do afterwards: values in Z, Q or Z_n modules are
 * shown unrounded, and two notes that the mapping makes equal still show as two. The real
 * operation runs when the drag ends and replaces the preview.
 */
public class DragPreview {
	
	private final String[] valueNames;
	private final int[] instanceNumbers;
	private final boolean relativeToParents;
	private final Set<BigBangObject> mappedObjects;
	//the objects whose values the preview changes, every parent before its children
	private final List<BigBangObject> orderedObjects;
	private final Map<BigBangObject,double[]> baseValues;
	private final Map<BigBangObject,double[]> previewValues;
	
	private DragPreview(String[] valueNames, int[] instanceNumbers, boolean relativeToParents, Set<BigBangObject> mappedObjects,
			List<BigBangObject> orderedObjects, Map<BigBangObject,double[]> baseValues) {
		this.valueNames = valueNames;
		this.instanceNumbers = instanceNumbers;
		this.relativeToParents = relativeToParents;
		this.mappedObjects = mappedObjects;
		this.orderedObjects = orderedObjects;
		this.baseValues = baseValues;
		this.previewValues = new IdentityHashMap<BigBangObject,double[]>();
		for (BigBangObject currentObject : orderedObjects) {
			this.previewValues.put(currentObject, baseValues.get(currentObject).clone());
		}
	}
	
	/**
	 * Returns a preview of a transformation of the two given values of the given objects, or null if
	 * the preview could not match the result of the operation.
	 * @param objects the objects of the last update
	 * @param transformedObjects the objects the transformation maps; all objects if empty
	 * @param relativeToParents true if the transformation has an anchor
	 * @param valueNames the coordinate system value names of the x and y coordinates
	 * @param instanceNumbers the instance number of each name within an object
	 */
	public static DragPreview create(BigBangObjects objects, Set<BigBangObject> transformedObjects, boolean relativeToParents,
			String[] valueNames, int[] instanceNumbers) {
		Set<BigBangObject> shownObjects = objects.getObjectsAt(null);
		//BigBangMapper maps with the value paths of the first object type only, so objects of other
		//types, such as modulators, may keep their values: those gestures are not previewed
		Form mappedType = objects.getObjectTypes().get(0);
		if (shownObjects == null || shownObjects.isEmpty() || objects.baseFormContainsColimits()) {
			return null;
		}
		Set<BigBangObject> mapped = Collections.newSetFromMap(new IdentityHashMap<BigBangObject,Boolean>());
		for (BigBangObject currentObject : transformedObjects.isEmpty() ? shownObjects : transformedObjects) {
			if (!shownObjects.contains(currentObject)) {
				return null;
			}
			mapped.add(currentObject);
		}
		
		//objects with a mapped object among their ancestors move too
		Map<BigBangObject,double[]> baseValues = new IdentityHashMap<BigBangObject,double[]>();
		Map<BigBangObject,Integer> depths = new IdentityHashMap<BigBangObject,Integer>();
		for (BigBangObject currentObject : shownObjects) {
			int depth = 0;
			boolean affected = mapped.contains(currentObject);
			for (BigBangObject ancestor = currentObject.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
				depth++;
				affected |= mapped.contains(ancestor);
			}
			if (affected) {
				if (!mappedType.equals(currentObject.getTopDenotatorPath().getEndForm())) {
					return null;
				}
				double[] values = getValues(currentObject, valueNames, instanceNumbers);
				if (values == null) {
					return null;
				}
				baseValues.put(currentObject, values);
				depths.put(currentObject, depth);
			}
		}
		//parents of affected objects are affected themselves or unchanged, and have values
		for (BigBangObject currentObject : new ArrayList<BigBangObject>(baseValues.keySet())) {
			BigBangObject parent = currentObject.getParent();
			if (parent != null && !baseValues.containsKey(parent)) {
				if (getValues(parent, valueNames, instanceNumbers) == null) {
					return null;
				}
			}
			if (relativeToParents && mapped.contains(currentObject) && parent == null) {
				return null;
			}
		}
		List<BigBangObject> ordered = new ArrayList<BigBangObject>(baseValues.keySet());
		ordered.sort((a, b) -> Integer.compare(depths.get(a), depths.get(b)));
		return new DragPreview(valueNames.clone(), instanceNumbers.clone(), relativeToParents, mapped, ordered, baseValues);
	}
	
	private static double[] getValues(BigBangObject object, String[] valueNames, int[] instanceNumbers) {
		double[] values = new double[valueNames.length];
		for (int i = 0; i < valueNames.length; i++) {
			Double value = object.getNthValue(valueNames[i], instanceNumbers[i]);
			if (value == null) {
				return null;
			}
			values[i] = value;
		}
		return values;
	}
	
	/**
	 * Recomputes the preview for the given morphism, an affine map of the plane.
	 */
	public void update(ModuleMorphism morphism) {
		double[] affine = DragPreview.getAffineCoefficients(morphism);
		this.update(affine);
	}
	
	/**
	 * Recomputes the preview for the affine map (x,y) -> (a*x + b*y + e, c*x + d*y + f), given as
	 * {a, b, c, d, e, f}.
	 */
	public void update(double[] affine) {
		for (BigBangObject currentObject : this.orderedObjects) {
			double[] base = this.baseValues.get(currentObject);
			double[] preview = this.previewValues.get(currentObject);
			double[] parentBase = null, parentPreview = null;
			BigBangObject parent = currentObject.getParent();
			if (parent != null) {
				parentBase = this.baseValues.get(parent);
				parentPreview = this.previewValues.get(parent);
				if (parentBase == null) {
					//an unchanged parent: no displacement, but its position anchors a relative map
					parentBase = getValues(parent, this.valueNames, this.instanceNumbers);
					parentPreview = parentBase;
				}
			}
			double x = base[0], y = base[1];
			if (this.mappedObjects.contains(currentObject)) {
				double cx = 0, cy = 0;
				if (this.relativeToParents) {
					cx = parentBase[0];
					cy = parentBase[1];
				}
				double rx = x - cx, ry = y - cy;
				x = affine[0]*rx + affine[1]*ry + affine[4] + cx;
				y = affine[2]*rx + affine[3]*ry + affine[5] + cy;
			}
			if (parentBase != null) {
				x += parentPreview[0] - parentBase[0];
				y += parentPreview[1] - parentBase[1];
			}
			preview[0] = x;
			preview[1] = y;
		}
	}
	
	/**
	 * Returns {a, b, c, d, e, f} with morphism(x,y) = (a*x + b*y + e, c*x + d*y + f), read off from
	 * the images of (0,0), (1,0) and (0,1).
	 */
	public static double[] getAffineCoefficients(ModuleMorphism morphism) {
		double[] o = DragPreview.map(morphism, 0, 0);
		double[] ex = DragPreview.map(morphism, 1, 0);
		double[] ey = DragPreview.map(morphism, 0, 1);
		return new double[]{ex[0]-o[0], ey[0]-o[0], ex[1]-o[1], ey[1]-o[1], o[0], o[1]};
	}
	
	private static double[] map(ModuleMorphism morphism, double x, double y) {
		try {
			ModuleElement image = morphism.map(RProperFreeElement.make(new double[]{x, y}));
			return new double[]{((RElement)image.getComponent(0)).getValue(), ((RElement)image.getComponent(1)).getValue()};
		} catch (Exception e) {
			throw new IllegalArgumentException("not an affine morphism of R^2: " + morphism, e);
		}
	}
	
	/**
	 * Returns the previewed value of the given name and instance number, or null if the preview
	 * leaves the value as it is.
	 */
	public Double getValue(BigBangObject object, String valueName, int instanceNumber) {
		double[] values = this.previewValues.get(object);
		if (values != null) {
			for (int i = 0; i < this.valueNames.length; i++) {
				if (instanceNumber == this.instanceNumbers[i] && valueName.equals(this.valueNames[i])) {
					return values[i];
				}
			}
		}
		return null;
	}
	
	/**
	 * Returns the objects whose values the preview changes, every parent before its children.
	 */
	public List<BigBangObject> getChangedObjects() {
		return Collections.unmodifiableList(this.orderedObjects);
	}
	
	public double[] getPreviewValues(BigBangObject object) {
		double[] values = this.previewValues.get(object);
		return values != null ? values.clone() : null;
	}

}
