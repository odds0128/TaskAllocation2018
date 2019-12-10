package main.research.communication.message;

import main.research.SetParam.ResultType;
import main.research.agent.Agent;
import main.research.graph.GraphAtAnWindow;
import main.research.task.Subtask;

public class ResultOfTeamFormation extends Message{

	static private int results = 0;
	static GraphAtAnWindow graph;

	ResultType result;
	Subtask allocatedSubtask;

	public ResultOfTeamFormation( Agent from, Agent to, ResultType result, Subtask allocatedSubtask ) {
		super( from, to );
		results++;
		this.result = result;
		if( graph != null && result == ResultType.SUCCESS ) graph.aggregate(from.id, to.id );
		this.allocatedSubtask = allocatedSubtask;
	}

	public ResultType getResult() {
		return result;
	}

	public Subtask getAllocatedSubtask() {
		return allocatedSubtask;
	}

	public static int getResults() {
		return results;
	}

	public static void setGraph( GraphAtAnWindow currentGraph ) {
		graph = currentGraph;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( );
		sb.append( "Result       - " );
		sb.append( "From: " + from );
		sb.append( "To: " + to );
		sb.append( "Result: " + result + ", " );
		sb.append( "AllocatedSubtask: " + allocatedSubtask );
		return sb.toString();
	}

	@Override
	public void clear() {
		results = 0;
	}
}
