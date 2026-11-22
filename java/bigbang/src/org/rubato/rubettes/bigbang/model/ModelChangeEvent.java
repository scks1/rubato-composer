package org.rubato.rubettes.bigbang.model;

/**
 * Tells a {@link ModelChangeListener} that a property of a {@link Model}
 * changed. It carries the same four values as java.beans.PropertyChangeEvent,
 * which it replaces so that the model layer does not need the java.desktop
 * module.
 */
public class ModelChangeEvent {
	
	private final Object source;
	private final String propertyName;
	private final Object oldValue;
	private final Object newValue;
	
	public ModelChangeEvent(Object source, String propertyName, Object oldValue, Object newValue) {
		if (source == null) {
			throw new IllegalArgumentException("null source");
		}
		this.source = source;
		this.propertyName = propertyName;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	/** Returns the model that fired this event. */
	public Object getSource() {
		return this.source;
	}
	
	public String getPropertyName() {
		return this.propertyName;
	}
	
	public Object getOldValue() {
		return this.oldValue;
	}
	
	public Object getNewValue() {
		return this.newValue;
	}
	
	@Override
	public String toString() {
		return this.getClass().getName() + "[propertyName=" + this.propertyName + "; oldValue=" + this.oldValue
				+ "; newValue=" + this.newValue + "; source=" + this.source + "]";
	}

}
