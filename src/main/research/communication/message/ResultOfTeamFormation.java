package main.research.communication.message;

import main.research.SetParam.ResultType;
import main.research.agent.Agent;
import main.research.task.Subtask;

public class ResultOfTeamFormation extends Message{

	static private int results = 0;

	ResultType result;
	Subtask allocatedSubtask;

	public ResultOfTeamFormation( Agent from, Agent to, ResultType result, Subtask allocatedSubtask ) {
		super( from, to );
		results++;
		this.result = result;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( );
		sb.append( "From: " + from.id ).append( "(" + from.role + ") , " );
		sb.append( "To: " + to.id ).append( "(" + to.role + ") , " );
		sb.append( "Result: " + result + ", " );
		sb.append( "AllocatedSubtask: " + allocatedSubtask );
		return sb.toString();
	}

	@Override
	public void clear() {
		results = 0;
	}
}
