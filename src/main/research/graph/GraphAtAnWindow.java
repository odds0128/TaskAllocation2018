package main.research.graph;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.grid.Grid;

import static main.research.SetParam.Principle.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Edgeクラス
 * 辺の情報を格納するクラス
 */
public class GraphAtAnWindow implements SetParam {
	private Map<Edge, Integer> graph;

    public GraphAtAnWindow( ) {
    	graph = new HashMap<>(  );
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
