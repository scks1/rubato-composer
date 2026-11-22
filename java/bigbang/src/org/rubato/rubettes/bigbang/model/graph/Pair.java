package org.rubato.rubettes.bigbang.model.graph;

/**
 * An ordered pair of non-null elements: the source and the destination of an
 * edge in a {@link DirectedMultigraph}. Two pairs are equal when their first
 * elements are equal and their second elements are equal.
 */
public final class Pair<T> {
	
	private final T first;
	private final T second;
	
	public Pair(T first, T second) {
		if (first == null || second == null) {
			throw new IllegalArgumentException("Pair cannot contain null values");
		}
		this.first = first;
		this.second = second;
	}
	
	public T getFirst() {
		return this.first;
	}
	
	public T getSecond() {
		return this.second;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Pair)) {
			return false;
		}
		Pair<?> pair = (Pair<?>)other;
		return this.first.equals(pair.first) && this.second.equals(pair.second);
	}
	
	@Override
	public int hashCode() {
		return 31 * this.first.hashCode() + this.second.hashCode();
	}
	
	@Override
	public String toString() {
		return "<" + this.first + ", " + this.second + ">";
	}

}
