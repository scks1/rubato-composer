package org.rubato.rubettes.bigbang.model;

/**
 * Receives the {@link ModelChangeEvent}s a {@link Model} fires.
 */
public interface ModelChangeListener {
	
	public void modelChanged(ModelChangeEvent event);

}
