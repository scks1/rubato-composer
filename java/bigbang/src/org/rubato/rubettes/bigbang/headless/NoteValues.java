package org.rubato.rubettes.bigbang.headless;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rubato.rubettes.bigbang.model.BigBangObject;

/**
 * The values of one object of a {@link BigBang} composition, read when the
 * view was made: the five note values by name, and every coordinate by its
 * full name, such as "Onset R", in the order of
 * {@link BigBang#getCoordinateNames()}. A value the object does not have is
 * null. Instances are immutable; {@link #getObject()} gives the object for
 * {@link BigBang#select}.
 */
public final class NoteValues {

	private final BigBangObject object;
	private final Map<String, Double> values;

	NoteValues(BigBangObject object, Map<String, Double> values) {
		this.object = object;
		this.values = Collections.unmodifiableMap(new LinkedHashMap<String, Double>(values));
	}

	/** The object these values belong to. */
	public BigBangObject getObject() {
		return this.object;
	}

	/** Every coordinate's value by full name, in coordinate order; null where the object has none. */
	public Map<String, Double> getValues() {
		return this.values;
	}

	/**
	 * The value of a coordinate given by its full name ("Pitch Q") or its
	 * form name ("Pitch"), resolved as {@link BigBang#getCoordinateIndex}
	 * resolves it; null when the object has no such value.
	 * @throws IllegalArgumentException if no coordinate has the name
	 */
	public Double get(String name) {
		if (this.values.containsKey(name)) {
			return this.values.get(name);
		}
		String full = null;
		for (String n : this.values.keySet()) {
			if (n.startsWith(name + " ")) {
				if (full != null && !full.equals(n)) {
					throw new IllegalArgumentException("the name " + name + " matches both " + full + " and " + n);
				}
				full = n;
			}
		}
		if (full == null) {
			throw new IllegalArgumentException("no coordinate named " + name + " in " + this.values.keySet());
		}
		return this.values.get(full);
	}

	public Double getOnset() { return this.get("Onset"); }
	public Double getPitch() { return this.get("Pitch"); }
	public Double getLoudness() { return this.get("Loudness"); }
	public Double getDuration() { return this.get("Duration"); }
	public Double getVoice() { return this.get("Voice"); }

	@Override
	public String toString() {
		return "NoteValues" + this.values;
	}
}
