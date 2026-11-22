package org.rubato.rubettes.bigbang.view;

import org.rubato.rubettes.bigbang.model.ModelChangeEvent;

public interface View {

    /**
     * Called by the controller when it needs to pass along a property change 
     * from a model.
     *
     * @param evt The property change event from the model
     */
    public void modelPropertyChange(ModelChangeEvent event);
    
}