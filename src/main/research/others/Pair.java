package main.research.others;

import java.util.AbstractMap;

public class Pair<K, V> extends AbstractMap.SimpleEntry<K, V> {
	/** serialVersionUID. */
	private static final long serialVersionUID = 6411527075103472113L;

	public Pair(final K key, final V value) {
		super(key, value);
	}
}
