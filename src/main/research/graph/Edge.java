package main.research.graph;

import java.util.Objects;

public class Edge {
	public int from_id;      // エッジの根元
	public int to_id;        // エッジの先端

	Edge( int from_id, int to_id ) {
		this.from_id = from_id;
		this.to_id = to_id;
	}

	@Override
	public boolean equals( Object o ) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;
		Edge targetEdge = (Edge ) o;

		return from_id ==  targetEdge.from_id && to_id == targetEdge.to_id;
	}

	@Override
	public int hashCode() {
		return Objects.hash( from_id, to_id );
	}
}
