package org.rubato.rubettes.bigbang.model;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A one-to-one map whose keys and values are both comparable. It replaces the
 * TreeBidiMap of collections-generic in {@link OperationPathResults} and keeps
 * its behaviour:
 * <ul>
 * <li>{@link #put} removes the key's previous mapping and any other key's
 * mapping to the same value before it adds the new one, so no two keys ever
 * share a value;</li>
 * <li>{@link #getKey} and {@link #containsValue} look the value up in a second,
 * inverse tree;</li>
 * <li>iteration runs in ascending key order;</li>
 * <li>null keys and values throw NullPointerException.</li>
 * </ul>
 * The entry, key and value views are read-only.
 */
public class SortedBidiMap<K extends Comparable<? super K>,V extends Comparable<? super V>> extends AbstractMap<K,V> {
	
	private final TreeMap<K,V> forward = new TreeMap<K,V>();
	private final TreeMap<V,K> inverse = new TreeMap<V,K>();
	
	public SortedBidiMap() {
	}
	
	public SortedBidiMap(Map<? extends K,? extends V> map) {
		this.putAll(map);
	}
	
	@Override
	public V put(K key, V value) {
		if (key == null || value == null) {
			throw new NullPointerException("SortedBidiMap does not accept null keys or values");
		}
		V previous = this.forward.get(key);
		this.remove(key);
		this.removeValue(value);
		this.forward.put(key, value);
		this.inverse.put(value, key);
		return previous;
	}
	
	@Override
	public V get(Object key) {
		this.checkNotNull(key);
		return this.forward.get(key);
	}
	
	@Override
	public boolean containsKey(Object key) {
		this.checkNotNull(key);
		return this.forward.containsKey(key);
	}
	
	@Override
	public boolean containsValue(Object value) {
		this.checkNotNull(value);
		return this.inverse.containsKey(value);
	}
	
	/** Returns the key mapped to the given value, or null if no key is. */
	public K getKey(Object value) {
		this.checkNotNull(value);
		return this.inverse.get(value);
	}
	
	@Override
	public V remove(Object key) {
		this.checkNotNull(key);
		V value = this.forward.remove(key);
		if (value != null) {
			this.inverse.remove(value);
		}
		return value;
	}
	
	/** Removes the mapping to the given value and returns its key, or null if there was none. */
	public K removeValue(Object value) {
		this.checkNotNull(value);
		K key = this.inverse.remove(value);
		if (key != null) {
			this.forward.remove(key);
		}
		return key;
	}
	
	@Override
	public void clear() {
		this.forward.clear();
		this.inverse.clear();
	}
	
	@Override
	public int size() {
		return this.forward.size();
	}
	
	@Override
	public Set<Map.Entry<K,V>> entrySet() {
		return Collections.unmodifiableMap(this.forward).entrySet();
	}
	
	private void checkNotNull(Object object) {
		if (object == null) {
			throw new NullPointerException("SortedBidiMap does not accept null keys or values");
		}
	}

}
