package main.research.graph;

import main.research.Parameter;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Edgeクラス
 * 辺の情報を格納するクラス
 */
public class GraphAtAnWindow implements Parameter {
	private Map<Edge, Integer> graph;

    public GraphAtAnWindow( ) {
    	graph = new LinkedHashMap<>(  );
    }

    public void aggregate( int leader_id , int member_id ) {
    	Edge edge = new Edge(leader_id, member_id );
    	if( graph.containsKey( edge ) ) {
    		int currentValue = graph.get( edge ).intValue();
    		graph.replace( edge, currentValue + 1 );
		} else {
    		graph.put( edge, 1 );
		}
	}

	public Map< Edge, Integer > getGraph() {
		return graph;
	}

}
