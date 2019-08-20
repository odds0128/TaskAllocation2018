package main.research.communication.message;

import main.research.Manager;
import main.research.agent.Agent;

public class Done extends Message{
	private static int done = 0;

	public Done( Agent from, Agent to ) {
		super( from, to );
		done++;
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
