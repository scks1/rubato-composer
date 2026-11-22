package org.rubato.rubettes.bigbang.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The base of BigBang's models. A model fires a {@link ModelChangeEvent} to its
 * listeners when one of its properties changes. Firing follows
 * java.beans.PropertyChangeSupport, which this class used before:
 * <ul>
 * <li>no event is fired when the old and new values are both non-null and equal;</li>
 * <li>listeners are called in the order they were added, from a copy of the list,
 * so a listener may add or remove listeners while it is called;</li>
 * <li>a listener added twice is called twice, and removing it removes one entry;</li>
 * <li>adding or removing null does nothing.</li>
 * </ul>
 */
public abstract class Model {
	
	private final List<ModelChangeListener> listeners = new ArrayList<ModelChangeListener>();
	
	public void addModelChangeListener(ModelChangeListener listener) {
		if (listener != null) {
			synchronized (this.listeners) {
				this.listeners.add(listener);
			}
		}
	}
	
	public void removeModelChangeListener(ModelChangeListener listener) {
		if (listener != null) {
			synchronized (this.listeners) {
				this.listeners.remove(listener);
			}
		}
	}
	
	public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
			return;
		}
		ModelChangeListener[] current;
		synchronized (this.listeners) {
			current = this.listeners.toArray(new ModelChangeListener[0]);
		}
		ModelChangeEvent event = new ModelChangeEvent(this, propertyName, oldValue, newValue);
		for (ModelChangeListener listener : current) {
			listener.modelChanged(event);
		}
	}

}
