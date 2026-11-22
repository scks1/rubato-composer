package org.rubato.rubettes.bigbang.model.denotators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.rubato.base.RubatoException;
import org.rubato.math.matrix.RMatrix;
import org.rubato.math.module.morphism.CompositionException;
import org.rubato.math.module.morphism.ModuleMorphism;
import org.rubato.math.module.morphism.RFreeAffineMorphism;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.rubettes.bigbang.model.OperationPathResults;
import org.rubato.rubettes.util.ArbitraryDenotatorMapper;
import org.rubato.rubettes.util.DenotatorPath;
import org.rubato.rubettes.util.FormValueFinder;

public class BigBangMapper extends BigBangManipulator {
	
	private ModuleMorphism morphism;
	private boolean copyAndMap;
	private boolean relative;
	//the object forms in the order of transformationPaths, found when first needed
	private List<Form> objectTypes;
	
	public BigBangMapper(BigBangDenotatorManager denotatorManager, BigBangTransformation transformation) {
		super(denotatorManager, transformation.getTransformationPaths());
		this.morphism = transformation.getModuleMorphism();
		this.copyAndMap = transformation.isCopyAndMap();
		this.relative = transformation.getAnchorNodePath() != null;
	}
	
	/**
	 * Maps the objects at the given paths
	 * @return the resulting newPaths
	 */
	public OperationPathResults mapCategorizedObjects(Set<DenotatorPath> objectPaths) {
		this.mapObjects(new ArrayList<DenotatorPath>(objectPaths));
		return this.denotatorManager.getPathResults();
	}
	
	private void mapObjects(List<DenotatorPath> objectPaths) {
		//TODO check if still necessary
		objectPaths = this.denotatorManager.sortAndReverse(objectPaths);
		
		Iterator<DenotatorPath> objectPathsIterator = objectPaths.iterator();
		if (objectPathsIterator.hasNext()) {
			DenotatorPath firstOfNextSiblings = objectPathsIterator.next();
			while (firstOfNextSiblings != null) {
				firstOfNextSiblings = this.mapAndAddNextSiblings(firstOfNextSiblings, objectPathsIterator);
			}
		}
	}
	
	private DenotatorPath mapAndAddNextSiblings(DenotatorPath firstSiblingPath, Iterator<DenotatorPath> objectPathsIterator) {
		//PerformanceCheck.startTask(".first_sib");
		List<Denotator> siblings = new ArrayList<Denotator>();
		List<DenotatorPath> siblingsPaths = new ArrayList<DenotatorPath>();
		
		Denotator firstSibling = this.denotatorManager.getAbsoluteObject(firstSiblingPath);
		if (firstSibling != null) {
			siblingsPaths.add(firstSiblingPath);
			siblings.add(firstSibling);
		}
		DenotatorPath siblingsAnchorPath = firstSiblingPath.getAnchorPath();
		ModuleMorphism siblingsMorphism = this.morphism;
		if (this.relative) {
			Denotator siblingsAnchor = this.denotatorManager.getAbsoluteObject(siblingsAnchorPath);
			siblingsMorphism = this.generateRelativeMorphism(this.extractValues(siblingsAnchor, this.getPathsFor(siblingsAnchor)));
		}
		
		DenotatorPath currentSiblingPath = firstSiblingPath;
		while (objectPathsIterator.hasNext()) {
			currentSiblingPath = objectPathsIterator.next();
			//PerformanceCheck.startTask(".next_sibs");
			if (currentSiblingPath.isDirectSatelliteOf(siblingsAnchorPath)) {
				Denotator currentSibling = this.denotatorManager.getAbsoluteObject(currentSiblingPath);
				if (currentSibling != null) {
					siblingsPaths.add(currentSiblingPath);
					siblings.add(currentSibling);
				}
			} else {
				this.mapAndReplaceOrAdd(siblings, siblingsAnchorPath, siblingsPaths, siblingsMorphism);
				return currentSiblingPath;
			}
		}
		this.mapAndReplaceOrAdd(siblings, siblingsAnchorPath, siblingsPaths, siblingsMorphism);
		return null;
	}
	
	/*
	 * Maps the given objects and adds them to the given anchorPath (they should thus originally be siblings).
	 * Returns a list with all the 
	 */
	private void mapAndReplaceOrAdd(List<Denotator> objects, DenotatorPath anchorPath, List<DenotatorPath> siblingsPaths, ModuleMorphism morphism) {
		if (objects.size() > 0) {
			List<Denotator> mappedObjects = new ArrayList<Denotator>();
			//siblings can be of different types, e.g. satellites and modulators of one note
			java.util.Map<TransformationPaths,ArbitraryDenotatorMapper> mappers = new java.util.IdentityHashMap<TransformationPaths,ArbitraryDenotatorMapper>();
			List<DenotatorPath> mappedPaths = new ArrayList<DenotatorPath>();
			for (int i = 0; i < objects.size(); i++) {
				Denotator currentObject = objects.get(i);
				TransformationPaths paths = this.getPathsFor(currentObject);
				ArbitraryDenotatorMapper mapper = mappers.get(paths);
				if (mapper == null) {
					mapper = new ArbitraryDenotatorMapper(morphism, paths);
					mappers.put(paths, mapper);
				}
				try {
					mappedObjects.add(mapper.getMappedDenotator(currentObject));
					mappedPaths.add(siblingsPaths.get(i));
				} catch (RubatoException e) {
					e.printStackTrace();
				}
			}
			if (mappedObjects.isEmpty()) {
				return;
			}
			//objects of one anchor can sit in different powersets, e.g. its satellites and its
			//modulators, so each run of objects goes back to the powerset it came from
			int start = 0;
			for (int i = 1; i <= mappedObjects.size(); i++) {
				if (i == mappedObjects.size() || !mappedPaths.get(i).getParentPath().equals(mappedPaths.get(start).getParentPath())) {
					List<Denotator> run = mappedObjects.subList(start, i);
					if (!this.copyAndMap) {
						this.denotatorManager.replaceSiblingObjects(new ArrayList<Denotator>(run), new ArrayList<DenotatorPath>(mappedPaths.subList(start, i)));
					} else {
						this.denotatorManager.addObjectsToParent(new ArrayList<Denotator>(run), mappedPaths.get(start).getParentPath());
					}
					start = i;
				}
			}
		}
	}
	
	/*
	 * Returns the transformation paths for the type of the given object. BigBangView passes one
	 * TransformationPaths per object type, in the order FormValueFinder finds the types. A list
	 * with a single entry, as in transformations saved before, applies to all objects.
	 */
	private TransformationPaths getPathsFor(Denotator object) {
		if (this.transformationPaths.size() > 1 && object != null) {
			if (this.objectTypes == null) {
				this.objectTypes = new FormValueFinder(this.denotatorManager.getForm(), true).getObjectForms();
			}
			int typeIndex = this.objectTypes.indexOf(object.getForm());
			if (typeIndex >= 0 && typeIndex < this.transformationPaths.size()) {
				return this.transformationPaths.get(typeIndex);
			}
		}
		return this.transformationPaths.get(0);
	}
	
	private ModuleMorphism generateRelativeMorphism(double[] anchorLocation) {
		RMatrix identity = new RMatrix(new double[][]{{1,0},{0,1}});
		double[] shift1 = new double[]{-1*anchorLocation[0],-1*anchorLocation[1]};
		double[] shift2 = new double[]{anchorLocation[0],anchorLocation[1]};
		ModuleMorphism relativeMorphism = this.morphism;
		try {
			relativeMorphism = relativeMorphism.compose(RFreeAffineMorphism.make(identity, shift1));
			relativeMorphism = RFreeAffineMorphism.make(identity, shift2).compose(relativeMorphism);
		} catch (CompositionException e) { e.printStackTrace(); }
		return relativeMorphism;
	}
	
	//TODO: WOOOO REMOVE THIS AND MAKE OBJECT GENERATOR WORK!!!
	private double[] extractValues(Denotator object, TransformationPaths transformationPaths) {
		double v1 = 0, v2 = 0;
		v1 = this.denotatorManager.getObjectGenerator().getDoubleValue(object, transformationPaths.getDomainPath(0, object));
		v2 = this.denotatorManager.getObjectGenerator().getDoubleValue(object, transformationPaths.getDomainPath(1, object));
		return new double[] {v1, v2};
	}

}
