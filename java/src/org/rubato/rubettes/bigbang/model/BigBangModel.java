package org.rubato.rubettes.bigbang.model;

import static org.rubato.xml.XMLConstants.FORM;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.rubato.rubettes.bigbang.model.undo.AddOrInsertOperationEdit;
import org.rubato.rubettes.bigbang.model.undo.RemoveOperationEdit;
import org.rubato.rubettes.bigbang.model.undo.UndoManager;
import org.rubato.rubettes.bigbang.model.undo.AbstractUndoableEdit;

import org.rubato.math.matrix.RMatrix;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;
import org.rubato.rubettes.bigbang.controller.BigBangController;
import org.rubato.rubettes.bigbang.model.denotators.BigBangDenotatorManager;
import org.rubato.rubettes.bigbang.model.denotators.TransformationProperties;
import org.rubato.rubettes.bigbang.model.graph.BigBangGraphAnimator;
import org.rubato.rubettes.bigbang.model.graph.BigBangOperationGraph;
import org.rubato.rubettes.bigbang.model.graph.CompositionState;
import org.rubato.rubettes.bigbang.model.operations.AbstractOperation;
import org.rubato.rubettes.bigbang.model.operations.AbstractTransformation;
import org.rubato.rubettes.bigbang.model.operations.AddObjectsOperation;
import org.rubato.rubettes.bigbang.model.operations.AddWallpaperDimensionOperation;
import org.rubato.rubettes.bigbang.model.operations.AffineTransformation;
import org.rubato.rubettes.bigbang.model.operations.AlterationOperation;
import org.rubato.rubettes.bigbang.model.operations.BuildSatellitesOperation;
import org.rubato.rubettes.bigbang.model.operations.DeleteObjectsOperation;
import org.rubato.rubettes.bigbang.model.operations.EndWallpaperOperation;
import org.rubato.rubettes.bigbang.model.operations.FlattenOperation;
import org.rubato.rubettes.bigbang.model.operations.ReflectionTransformation;
import org.rubato.rubettes.bigbang.model.operations.RotationTransformation;
import org.rubato.rubettes.bigbang.model.operations.ScalingTransformation;
import org.rubato.rubettes.bigbang.model.operations.InputCompositionOperation;
import org.rubato.rubettes.bigbang.model.operations.ShapingOperation;
import org.rubato.rubettes.bigbang.model.operations.ShearingTransformation;
import org.rubato.rubettes.bigbang.model.operations.TranslationTransformation;
import org.rubato.rubettes.util.DenotatorPath;
import org.rubato.rubettes.util.PerformanceCheck;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;

public class BigBangModel extends Model {
	
	private boolean inputActive;
	private boolean isMultiTouch;
	private BigBangController controller;
	private BigBangDenotatorManager denotators;
	private BigBangObjects objects; //object-oriented representation of the denotator composition
	private UndoManager undoManager;
	private BigBangOperationGraph operationGraph;
	private BigBangGraphAnimator animator;
	
	//Incremental replay: the operations executed by the last update, in order, the path results each
	//of them returned, and the denotator state before each of them (null where it was not captured).
	//An update that knows which operations changed restores the state before the first changed one
	//and executes the graph from there on. Setting the system property rubato.bigbang.fullReplay
	//to true, or calling setIncrementalReplay(false), makes every update execute all operations.
	private boolean incrementalReplay = !Boolean.getBoolean("rubato.bigbang.fullReplay");
	private final List<AbstractOperation> replayedOperations = new ArrayList<AbstractOperation>();
	private final List<OperationPathResults> replayedPathResults = new ArrayList<OperationPathResults>();
	private final List<BigBangDenotatorManager.Checkpoint> checkpoints = new ArrayList<BigBangDenotatorManager.Checkpoint>();
	private int lastReplayStart;
	
	public BigBangModel() {
		this.denotators = new BigBangDenotatorManager();
		this.setInputActive(true);
		this.objects = new BigBangObjects(this.denotators.getForm());
		this.objects.setNewTransformationPreviewCheck(this::canPreviewNewTransformation);
		this.undoManager = new UndoManager();
		this.reset();
	}
	
	public void setController(BigBangController controller) {
		this.controller = controller;
		controller.addModel(this);
		this.objects.setController(controller);
		this.firePropertyChange(BigBangController.UNDO, null, this.undoManager);
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
		this.fireCompositionChange();
	}
	
	public void reset() {
		this.clearReplayCache();
		this.undoManager.discardAllEdits();
		this.operationGraph = new BigBangOperationGraph();
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
	}
	
	private void setGraph(BigBangOperationGraph graph) {
		this.clearReplayCache();
		this.operationGraph = graph;
	}
	
	public void setInputActive(Boolean inputActive) {
		this.inputActive = inputActive;
		this.firePropertyChange(BigBangController.INPUT_ACTIVE, null, this.inputActive);
	}
	
	public boolean isInputActive() {
		return this.inputActive;
	}
	
	public void setMultiTouch(Boolean multiTouch) {
		this.isMultiTouch = multiTouch;
		this.firePropertyChange(BigBangController.MULTITOUCH, null, this.isMultiTouch);
	}
	
	public void setForm(Form form) {
		//TODO strange, full comparison does not work....
		if (!form.getName().equals(this.denotators.getForm().getName())) {
			this.reset();
			this.denotators.setForm(form);
			this.objects = new BigBangObjects(this.denotators.getForm());
			this.objects.setNewTransformationPreviewCheck(this::canPreviewNewTransformation);
			if (this.controller != null) {
				this.objects.setController(this.controller);
			}
			this.fireCompositionChange();
		}
	}
	
	public void newWindowAdded() {
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
		//TODO ALSO FIRE COMPOSITION CHANGE
	}
	
	/**
	 * if the currently selected operation is a SetOrAddCompositionEdit, replace its composition.
	 * else post a new SetOrAddCompositionEdit
	 */
	public void setOrAddComposition(Denotator composition) {
		if (!this.denotators.isFormCompatibleWithCurrentForm(composition.getForm())) {
			this.setForm(composition.getForm());
		}
		if (this.operationGraph.getSelectedOperation() instanceof InputCompositionOperation) {
			InputCompositionOperation input = (InputCompositionOperation)this.operationGraph.getSelectedOperation();
			input.setOrAddComposition(composition);
			this.operationModified(input);
		} else {
			this.addOperation(new InputCompositionOperation(this, composition));
		}
	}
	
	public Denotator getComposition() {
		return this.denotators.getComposition();
	}
	
	public BigBangDenotatorManager getDenotatorManager() {
		return this.denotators;
	}
	
	public void addObjects(ArrayList<Map<DenotatorPath,Double>> pathsWithValues, ArrayList<DenotatorPath> powersetPaths, Boolean inPreviewMode) {
		if (this.denotators != null) { //needs to be checked for milmeister ghost rubette reacting to leap motion 
			AbstractOperation lastEdit = this.operationGraph.getLastAddedOperation();
			if (lastEdit != null && lastEdit instanceof AddObjectsOperation) {
				AddObjectsOperation addEdit = (AddObjectsOperation) lastEdit;
				if (addEdit.addObjects(pathsWithValues, powersetPaths, inPreviewMode)) {
					this.operationModified(addEdit);
					return;
				}
			}
			this.addOperation(new AddObjectsOperation(this, pathsWithValues, powersetPaths, inPreviewMode));
		}
	}
	
	public void unAddObjects(TreeSet<Map<DenotatorPath,Double>> pathsWithValues) {
		AddObjectsOperation edit = null;
		if (this.operationGraph.getSelectedOperation() instanceof AddObjectsOperation) {
			edit = (AddObjectsOperation)this.operationGraph.getSelectedOperation();
		} else if (this.operationGraph.getLastAddedOperation() != null && this.operationGraph.getLastAddedOperation() instanceof AddObjectsOperation) {
			edit = (AddObjectsOperation)this.operationGraph.getLastAddedOperation();
		}
		if (edit != null) {
			edit.unAddObjects(pathsWithValues);
			this.operationModified(edit);
		}
	}
	
	public BigBangObjects getObjects() {
		return this.objects;
	}
	
	public void deleteObjects(TreeSet<BigBangObject> objects) {
		AbstractOperation lastEdit = this.operationGraph.getLastAddedOperation();
		if (lastEdit != null && lastEdit instanceof DeleteObjectsOperation) {
			((DeleteObjectsOperation)lastEdit).addObjects(objects);
			this.operationModified(lastEdit);
			return;
		}
		this.addOperation(new DeleteObjectsOperation(this, objects));
	}
	
	public void translateObjects(TransformationProperties properties) {
		if (properties.startNewTransformation()) {
			this.addOperation(new TranslationTransformation(this, properties));
		} else if (this.updateTransformation(properties, TranslationTransformation.class)) {
			this.updateAfterModifying(this.operationGraph.getLastAddedOperation());
		}
	}
	
	public void rotateObjects(TransformationProperties properties, double[] startingPoint, Double angle) {
		if (properties.startNewTransformation()) {
			this.addOperation(new RotationTransformation(this, properties, startingPoint, angle));
		} else if (this.updateTransformation(properties, RotationTransformation.class)) {
			RotationTransformation lastRotation = (RotationTransformation)this.operationGraph.getLastAddedOperation();
			lastRotation.setParameters(startingPoint, angle);
			this.updateAfterModifying(lastRotation);
		}
	}
	
	public void scaleObjects(TransformationProperties properties, double[] scaleFactors) {
		if (properties.startNewTransformation()) {
			this.addOperation(new ScalingTransformation(this, properties, scaleFactors));
		} else if (this.updateTransformation(properties, ScalingTransformation.class)) {
			this.modifyLastTransformation(scaleFactors);
		}
	}
	
	public void reflectObjects(TransformationProperties properties, double[] reflectionVector) {
		if (properties.startNewTransformation()) {
			this.addOperation(new ReflectionTransformation(this, properties, reflectionVector));
		} else if (this.updateTransformation(properties, ReflectionTransformation.class)) {
			this.modifyLastTransformation(reflectionVector);
		}
	}
	
	public void shearObjects(TransformationProperties properties, double[] shearingFactors) {
		if (properties.startNewTransformation()) {
			this.addOperation(new ShearingTransformation(this, properties, shearingFactors));
		} else if (this.updateTransformation(properties, ShearingTransformation.class)) {
			this.modifyLastTransformation(shearingFactors);
		}
	}
	
	public void affineTransformObjects(TransformationProperties properties, double[] shift, RMatrix transform) {
		if (properties.startNewTransformation()) {
			this.addOperation(new AffineTransformation(this, properties, shift, transform));
		} else if (this.updateTransformation(properties, AffineTransformation.class)) {
			AffineTransformation lastTransformation = (AffineTransformation)this.operationGraph.getLastAddedOperation();
			lastTransformation.setParameters(shift, transform);
			this.updateAfterModifying(lastTransformation);
		}
	}
	
	private boolean updateTransformation(TransformationProperties properties, Class<?> transformationClass) {
		AbstractOperation lastOperation = this.operationGraph.getLastAddedOperation();
		if (transformationClass.isInstance(lastOperation)) {
			((AbstractTransformation)lastOperation).updateProperties(properties);
			return true;
		}
		return false;
	}
	
	private void modifyLastTransformation(double[] newValues) {
		AbstractTransformation lastTransformation = (AbstractTransformation)this.operationGraph.getLastAddedOperation();
		lastTransformation.modify(newValues);
		this.updateAfterModifying(lastTransformation);
	}
	
	public void shapeObjects(TransformationProperties properties, TreeMap<Double,Double> shapingLocations) {
		ShapingOperation edit = null;
		if (this.operationGraph.getSelectedOperation() instanceof ShapingOperation) {
			edit = (ShapingOperation)this.operationGraph.getSelectedOperation();
		} else if (this.operationGraph.getLastAddedOperation() instanceof ShapingOperation) {
			edit = (ShapingOperation)this.operationGraph.getLastAddedOperation();
		}
		if (edit != null && edit.getShapingPaths().equals(properties.getTransformationPaths())) {
			edit.addShapingLocations(shapingLocations);
			this.updateAfterModifying(edit);
		} else {	
			this.addOperation(new ShapingOperation(this, properties, shapingLocations));
		}
	}
	
	public void buildSatellites(TreeSet<BigBangObject> objects, BigBangObject anchorObject, Integer powersetIndex) {
		this.addOperation(new BuildSatellitesOperation(this, objects, anchorObject, powersetIndex));
	}
	
	public void flattenObjects(TreeSet<BigBangObject> objects) {
		this.addOperation(new FlattenOperation(this, objects));
	}
	
	public void addWallpaperDimension(TreeSet<BigBangObject> objectPaths, Integer rangeFrom, Integer rangeTo) {
		this.addOperation(new AddWallpaperDimensionOperation(this, objectPaths, rangeFrom, rangeTo));
	}
	
	public void endWallpaper() {
		this.addOperation(new EndWallpaperOperation(this));
	}
	
	public void addAlteration(DenotatorPath degreesDimensionPath) {
		this.addOperation(new AlterationOperation(this, degreesDimensionPath));
		this.firePropertyChange(BigBangController.MODIFY_OPERATION, null, this.operationGraph.getLastAddedOperation());
	}
	
	public void fireAlterationComposition(Integer index) {
		if (this.operationGraph.getSelectedOperation() instanceof AlterationOperation) {
			//this.alteration.resetDegrees();
			this.fireObjectSelectionChange(((AlterationOperation)this.operationGraph.getSelectedOperation()).getAlterationComposition(index));
			this.firePropertyChange(BigBangController.FIRE_ALTERATION_COMPOSITION, null, index);
		}
	}
	
	public void setAlterationStartDegree(Double startDegree) {
		if (this.operationGraph.getSelectedOperation() instanceof AlterationOperation) {
			((AlterationOperation)this.operationGraph.getSelectedOperation()).setStartDegree(startDegree);
		}
	}
	
	public void setAlterationEndDegree(Double endDegree) {
		if (this.operationGraph.getSelectedOperation() instanceof AlterationOperation) {
			((AlterationOperation)this.operationGraph.getSelectedOperation()).setEndDegree(endDegree);
		}
	}
	
	public void setAlterationDegreesDimension(DenotatorPath path) {
		if (this.operationGraph.getSelectedOperation() instanceof AlterationOperation) {
			((AlterationOperation)this.operationGraph.getSelectedOperation()).setDegreesDimensionPath(path);
		}
	}
	
	private void addOperation(AbstractOperation operation) {
		AddOrInsertOperationEdit edit = new AddOrInsertOperationEdit(operation, this.operationGraph);
		edit.execute();
		this.postEdit(edit);
	}
	
	public void removeOperation(AbstractOperation operation) {
		RemoveOperationEdit edit = new RemoveOperationEdit(operation, this.operationGraph);
		edit.execute();
		this.postEdit(edit);
		this.objects.removeOperation(operation);
		this.updateAfterStructuralChange();
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
	}
	
	private void postEdit(AbstractUndoableEdit edit) {
		this.undoManager.postEdit(edit);
		this.updateAfterStructuralChange();
		this.firePropertyChange(BigBangController.UNDO, null, this.undoManager);
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
		//this.firePropertyChange(BigBangController.SELECT_COMPOSITION_STATE, null, this.transformationGraph.getSelectedCompositionState());
	}
	
	public void operationModified() {
		this.updateComposition();
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
	}
	
	private void operationModified(AbstractOperation operation) {
		this.updateAfterModifying(operation);
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
	}
	
	public void modifyOperation(Integer operationIndex, Double ratio) {
		if (operationIndex >= 0 && this.operationGraph.getEdgeCount() > operationIndex) {
			DijkstraShortestPath<CompositionState,AbstractOperation> dijkstra = new DijkstraShortestPath<CompositionState,AbstractOperation>(this.operationGraph);
		    List<AbstractOperation> shortestPath = dijkstra.getPath(this.operationGraph.getFirstState(), this.operationGraph.getLastState());
		    AbstractOperation operation = shortestPath.get(operationIndex);
		    operation.modify(ratio);
			this.updateAfterModifying(operation);
			this.firePropertyChange(BigBangController.MODIFY_OPERATION, null, operation);
		}
	}
	
	public void setOperationDurations(double duration) {
		this.operationGraph.setDurations(duration);
	}
	
	public void setInsertionState(Integer stateIndex) {
		this.operationGraph.setInsertionState(stateIndex);
	}
	
	public void undo() {
		AbstractOperation lastAddedOperation = this.operationGraph.getLastAddedOperation();
		CompositionState state = this.operationGraph.getSource(lastAddedOperation);
		//this.undoneOperations.add(this.transformationGraph.removeLastAddedOperation());
		this.undoManager.undo();
		this.updateAfterStructuralChange();
		this.firePropertyChange(BigBangController.UNDO, null, this.undoManager);
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
	}
	
	public void redo() {
		//this.transformationGraph.addOrInsertOperation(this.undoneOperations.remove(this.undoneOperations.size()-1), false);
		this.undoManager.redo();
		this.updateAfterStructuralChange();
		this.firePropertyChange(BigBangController.REDO, null, this.undoManager);
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
	}
	
	public void selectPreviousCompositionState() {
		if (this.operationGraph.getSelectedCompositionState() != null) {
			if (this.operationGraph.getSelectedCompositionState().getIndex() > 0) {
				this.selectCompositionStateAt(this.operationGraph.getSelectedCompositionState().getIndex()-1);
			}
		} else if (this.operationGraph.getVertexCount() > 1) {
			this.selectCompositionStateAt(this.operationGraph.getVertexCount()-1);
		} else {
			this.selectCompositionStateAt(0);
		}
	}
	
	public void selectNextCompositionState() {
		if (this.operationGraph.getSelectedCompositionState() != null) {
			if (this.operationGraph.getSelectedCompositionState().getIndex() < this.operationGraph.getVertexCount()-1) {
				this.selectCompositionStateAt(this.operationGraph.getSelectedCompositionState().getIndex()+1);
			}
		} else {
			this.selectCompositionStateAt(0);
		}
	}
	
	public void selectCompositionStateAt(Integer stateIndex) {
		this.operationGraph.selectCompositionStateAt(stateIndex);
		this.updateAfterStructuralChange();
		this.firePropertyChange(BigBangController.SELECT_COMPOSITION_STATE, null, this.operationGraph.getSelectedCompositionState());
	}
	
	public void selectCompositionState(CompositionState vertex) {
		this.operationGraph.selectCompositionState(vertex);
		this.updateAfterStructuralChange();
		this.firePropertyChange(BigBangController.SELECT_COMPOSITION_STATE, null, vertex);
	}
	
	public void deselectCompositionStates() {
		this.operationGraph.deselectCompositionStates();
		this.updateAfterStructuralChange();
		this.firePropertyChange(BigBangController.DESELECT_COMPOSITION_STATES, null, null);
	}
	
	public void selectOperation(AbstractOperation edge) {
		this.operationGraph.selectOperation(edge);
		this.firePropertyChange(BigBangController.SELECT_OPERATION, null, edge);
	}
	
	public void deselectOperations() {
		this.operationGraph.selectOperation(null);
		this.firePropertyChange(BigBangController.DESELECT_OPERATIONS, null, null);
	}
	
	public void setOperationDuration(AbstractOperation operation, Double duration) {
		operation.setDuration(duration);
		this.operationGraph.update();
	}
	
	public void toggleGraphAnimation() {
		if (this.animator != null && this.animator.isAlive()) {
			this.animator.end();
		} else {
			if (this.animator == null) {
				this.animator = new BigBangGraphAnimator(this.operationGraph, this);
			} else {
				double previousPosition = this.animator.getPositionInPercent();
				this.animator = new BigBangGraphAnimator(this.operationGraph, this);
				if (previousPosition < 1) {
					this.animator.setPosition(previousPosition);
				}
			}
			this.animator.start();
		}
	}
	
	/**
	 * @param position between 0 and 1
	 */
	public void setGraphAnimationPosition(Double position) {
		if (this.animator == null || !this.animator.isAlive()) {
			this.animator = new BigBangGraphAnimator(this.operationGraph, this);
		}
		this.animator.setPosition(position);
	}
	
	/**
	 * Splits the currently selected operation at the current position of the animator if it is splittable
	 */
	public void splitOperation() {
		this.operationGraph.splitSelectedAndParallelOperations(this.animator.getPositionInSeconds());
		this.animator.setGraph(this.operationGraph);
		this.updateComposition();
		this.firePropertyChange(BigBangController.GRAPH, null, this.operationGraph);
	}
	
	public BigBangOperationGraph getTransformationGraph() {
		return this.operationGraph;
	}
	
	/**
	 * Executes every operation on the current path of the graph. Callers that changed operations
	 * without telling the model which ones use this method.
	 */
	public synchronized void updateComposition() {
		this.replayFrom(0);
	}
	
	/**
	 * Updates after operations were added, removed or reordered on the current path, or a different
	 * state was selected, with no operation changed in place.
	 */
	private synchronized void updateAfterStructuralChange() {
		this.replayFrom(this.getReplayStart(null));
	}
	
	/**
	 * Updates after the given operation changed in place, as a drag gesture changes the last
	 * transformation.
	 */
	private synchronized void updateAfterModifying(AbstractOperation modifiedOperation) {
		this.replayFrom(this.getReplayStart(modifiedOperation));
	}
	
	/**
	 * Returns true if a transformation started now would be appended after the last operation of
	 * the path the objects show and mapped directly: no operation or state is selected, no
	 * insertion state is set, no wallpaper or alteration is open and the graph is not being
	 * animated. A view can then preview a drag from the values the objects hold and add the
	 * transformation once, when the drag ends.
	 */
	public boolean canPreviewNewTransformation() {
		return this.operationGraph.getSelectedOperation() == null
				&& this.operationGraph.getSelectedCompositionState() == null
				&& this.operationGraph.getInsertionState() == null
				&& !this.denotators.isBuildingWallpaperOrAlteration()
				&& (this.animator == null || !this.animator.isAlive())
				&& this.objects.getObjectsAt(null) != null;
	}
	
	public void setIncrementalReplay(boolean incrementalReplay) {
		this.incrementalReplay = incrementalReplay;
		this.clearReplayCache();
	}
	
	/**
	 * Returns the index of the first operation that the last update executed, 0 if it executed all
	 * of them.
	 */
	public int getLastReplayStart() {
		return this.lastReplayStart;
	}
	
	private void clearReplayCache() {
		this.replayedOperations.clear();
		this.replayedPathResults.clear();
		this.checkpoints.clear();
	}
	
	/**
	 * Returns the index of the first operation on the current path that has to be executed again:
	 * the first one that differs from the last update's operations, or the modified one if it comes
	 * earlier. The index then moves back to the nearest operation with a captured checkpoint.
	 */
	private int getReplayStart(AbstractOperation modifiedOperation) {
		if (!this.incrementalReplay) {
			return 0;
		}
		List<AbstractOperation> current = this.operationGraph.getCurrentlyExecutedOperationsInOrder();
		int start = 0;
		int commonLength = Math.min(current.size(), this.replayedOperations.size());
		while (start < commonLength && current.get(start) == this.replayedOperations.get(start)) {
			start++;
		}
		if (modifiedOperation != null) {
			for (int i = 0; i < start; i++) {
				if (current.get(i) == modifiedOperation) {
					start = i;
					break;
				}
			}
		}
		//the last operation on the path always runs again, since its results set up the final objects
		start = Math.min(start, current.size()-1);
		while (start > 0 && (start >= this.checkpoints.size() || this.checkpoints.get(start) == null)) {
			start--;
		}
		return Math.max(start, 0);
	}
	
	/**
	 * Executes the operations of the current path from the given index on. For an index above 0,
	 * the denotators are restored to the checkpoint before that operation and the objects at that
	 * operation are rebuilt from the previous operation's stored path results.
	 */
	private void replayFrom(int start) {
		if (this.operationGraph.getEdgeCount() > 0) {
			List<AbstractOperation> operationsToBeExecuted = new ArrayList<AbstractOperation>(this.operationGraph.getCurrentlyExecutedOperationsInOrder());
			if (start > 0) {
				this.denotators.restore(this.checkpoints.get(start));
				this.objects.updatePaths(operationsToBeExecuted.get(start-1), operationsToBeExecuted.get(start), this.replayedPathResults.get(start-1));
			} else {
				this.denotators.reset();
			}
			this.lastReplayStart = start;
			this.truncateReplayCache(start);
			
			OperationPathResults currentPathResults = null;
			for (int i = start; i < operationsToBeExecuted.size(); i++) {
				AbstractOperation currentOperation = operationsToBeExecuted.get(i);
				AbstractOperation nextOperation = null;
				if (i < operationsToBeExecuted.size()-1) {
					nextOperation = operationsToBeExecuted.get(i+1);
				}
				if (i >= this.checkpoints.size()) {
					this.checkpoints.add(this.incrementalReplay && i > 0 ? this.denotators.checkpoint() : null);
				}
				//System.out.println(currentOperation);
				PerformanceCheck.startTask("execute");
				currentPathResults = currentOperation.execute();
				this.replayedOperations.add(currentOperation);
				this.replayedPathResults.add(currentPathResults);
				PerformanceCheck.startTask("update paths");
				this.objects.updatePaths(currentOperation, nextOperation, currentPathResults);
			}
			//System.out.println("BO "+this.objects.getObjectsAt(null).size());
			//System.out.println(operationsToBeExecuted + " " + this.objects.getObjects());
			
			PerformanceCheck.startTask("extract");
			new DenotatorValueExtractor(this.objects, this.denotators.getComposition());
			
			//TODO reconsider how to select paths. will be taken from bbobjects or saved in visualobjects..
			PerformanceCheck.startTask("fire");
			this.fireCompositionChange();
		} else {
			this.clearReplayCache();
			this.lastReplayStart = 0;
			this.objects.clearObjects();
			this.denotators.reset();
			this.fireCompositionChange();
		}
	}
	
	//keeps the operations and results before start, and the checkpoints up to and including start
	private void truncateReplayCache(int start) {
		while (this.replayedOperations.size() > start) {
			this.replayedOperations.remove(this.replayedOperations.size()-1);
			this.replayedPathResults.remove(this.replayedPathResults.size()-1);
		}
		int keptCheckpoints = start > 0 ? start+1 : 0;
		while (this.checkpoints.size() > keptCheckpoints) {
			this.checkpoints.remove(this.checkpoints.size()-1);
		}
	}
	
	private void fireObjectSelectionChange(Set<BigBangObject> newSelection) {
		this.firePropertyChange(BigBangController.OBJECT_SELECTION, null, newSelection);
	}
	
	private void fireCompositionChange() {
		//TODO REPOLACE BY JUST SENDING ADDED/CHANGED OR REMOVED OBJECTS!!!!
		this.firePropertyChange(BigBangController.COMPOSITION, null, this.objects);
	}
	
	public BigBangModel clone() {
		BigBangModel clonedModel = new BigBangModel();
		clonedModel.setForm(this.denotators.getForm());
		clonedModel.operationGraph = this.operationGraph.clone(clonedModel);
		clonedModel.updateComposition();
		return clonedModel;
	}

	public void toXML(XMLWriter writer) {
		writer.writeFormRef(this.denotators.getForm());
		//System.out.println("TOXML " +this.denotators.getForm());
		this.operationGraph.toXML(writer);
	}
	
	public static BigBangModel fromXML(XMLReader reader, Element element) {
		BigBangModel loadedModel = new BigBangModel();
		try {
			Form form = reader.parseAndResolveForm(XMLReader.getChild(element, FORM));
			loadedModel.setForm(form);
			loadedModel.setGraph(BigBangOperationGraph.fromXML(loadedModel, reader, element));
			//initiates bbobjects and all
			loadedModel.updateComposition();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return loadedModel;
	}

}
