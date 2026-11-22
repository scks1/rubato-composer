package org.rubato.rubettes.bigbang.model.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.rubato.rubettes.bigbang.model.Model;
import org.rubato.rubettes.bigbang.model.ModelChangeEvent;
import org.rubato.rubettes.bigbang.model.ModelChangeListener;
import org.rubato.rubettes.bigbang.model.SortedBidiMap;

/**
 * Checks the two small replacements for library classes in the model: SortedBidiMap for
 * collections-generic's TreeBidiMap, and Model's listeners for java.beans.PropertyChangeSupport.
 */
public class ModelReplacementsTest extends TestCase {
	
	public void testBidiMapKeepsBothDirectionsOneToOne() {
		SortedBidiMap<Integer,String> map = new SortedBidiMap<Integer,String>();
		assertNull(map.put(2, "b"));
		assertNull(map.put(1, "a"));
		assertEquals("b", map.put(2, "c"));
		assertEquals(Arrays.asList(1, 2), new ArrayList<Integer>(map.keySet()));
		assertEquals(Integer.valueOf(2), map.getKey("c"));
		assertNull(map.getKey("b"));
		assertFalse(map.containsValue("b"));
		//a value already mapped moves to the new key
		map.put(3, "a");
		assertFalse(map.containsKey(1));
		assertEquals(Integer.valueOf(3), map.getKey("a"));
		assertEquals(2, map.size());
		assertEquals(Integer.valueOf(3), map.removeValue("a"));
		assertEquals("c", map.remove(2));
		assertTrue(map.isEmpty());
	}
	
	public void testBidiMapRefusesNullAndViewChanges() {
		SortedBidiMap<Integer,String> map = new SortedBidiMap<Integer,String>();
		map.put(1, "a");
		try {
			map.put(null, "x");
			fail("null key");
		} catch (NullPointerException e) { }
		try {
			map.put(2, null);
			fail("null value");
		} catch (NullPointerException e) { }
		try {
			map.entrySet().clear();
			fail("the entry view is read-only");
		} catch (UnsupportedOperationException e) { }
		for (Map.Entry<Integer,String> entry : map.entrySet()) {
			try {
				entry.setValue("z");
				fail("entries are read-only");
			} catch (UnsupportedOperationException e) { }
		}
		assertEquals("a", map.get(1));
	}
	
	public void testListenersFollowPropertyChangeSupportRules() {
		Model model = new Model() { };
		final List<String> heard = new ArrayList<String>();
		ModelChangeListener listener = new ModelChangeListener() {
			public void modelChanged(ModelChangeEvent event) {
				heard.add(event.getPropertyName() + "=" + event.getNewValue());
			}
		};
		model.addModelChangeListener(listener);
		model.addModelChangeListener(listener);
		model.addModelChangeListener(null);
		model.firePropertyChange("x", null, 1);
		model.firePropertyChange("x", 1, 1);
		model.firePropertyChange("x", null, null);
		assertEquals(Arrays.asList("x=1", "x=1", "x=null", "x=null"), heard);
		heard.clear();
		model.removeModelChangeListener(listener);
		model.firePropertyChange("y", 1, 2);
		assertEquals(Arrays.asList("y=2"), heard);
	}
	
	public void testListenerAddedDuringFireHearsOnlyLaterEvents() {
		final Model model = new Model() { };
		final List<String> heard = new ArrayList<String>();
		model.addModelChangeListener(new ModelChangeListener() {
			public void modelChanged(ModelChangeEvent event) {
				heard.add("first " + event.getPropertyName());
				if (heard.size() == 1) {
					model.addModelChangeListener(new ModelChangeListener() {
						public void modelChanged(ModelChangeEvent e) {
							heard.add("second " + e.getPropertyName());
						}
					});
				}
			}
		});
		model.firePropertyChange("a", null, 1);
		model.firePropertyChange("b", null, 1);
		assertEquals(Arrays.asList("first a", "first b", "second b"), heard);
		assertSame(model, new ModelChangeEvent(model, "c", null, null).getSource());
	}

}
