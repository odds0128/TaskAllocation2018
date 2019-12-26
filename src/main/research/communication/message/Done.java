package main.research.communication.message;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.task.Subtask;

public class Done extends Message{
	private static int done = 0;
	private Subtask st;

	public Done( Agent from, Agent to, Subtask st ) {
		super( from, to );
		this.st = st;
		done++;
	}

	public Subtask getSt() {
		return st;
	}

	public static int getDone() {
		return done;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( );
		sb.append( "Done         - " );
		sb.append( "From: " + from );
		sb.append( "To: " + to );
		return sb.toString();
	}

	@Override
	public void clear() {
		done++;
	}
}
