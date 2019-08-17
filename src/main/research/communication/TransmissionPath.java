package main.research.communication;

import main.research.communication.message.*;
import main.research.others.Pair;
import main.research.SetParam;
import main.research.agent.Agent;
import main.research.grid.Grid;
import main.research.task.Subtask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static main.research.SetParam.Role.LEADER;
import static main.research.SetParam.Role.MEMBER;

public class TransmissionPath implements SetParam {
	private static int messageNum = 0;
	private static int communicationTime = 0;

	private static List< Pair<Message, Integer> > messageQueue = new ArrayList<>(  );

	public static void sendMessage( Message m ) {
		assert ! ( m.getTo().role == LEADER && m instanceof ResultOfTeamFormation ) : "Wrong result message";
		assert ! ( m.getTo().role == MEMBER && m instanceof ReplyToSolicitation )   : "Wrong reply message.";

		int untilArrival = Grid.getDelay( m.getFrom(), m.getTo() );
		messageQueue.add( new Pair<>(m, untilArrival) );
		messageNum++;
		communicationTime += untilArrival;
	}
	
	public static void transmit() {
		Collections.sort( messageQueue, Comparator.comparing( Pair::getValue ) );
		approaching( messageQueue );

		while( ! messageQueue.isEmpty() ) {
			Pair<Message, Integer> pair = messageQueue.remove( 0 );
			if( pair.getValue() == 0 ) {
				reachPost( pair.getKey() );
			} else {
				messageQueue.add(0, pair);
				break;
			}
		}
	}

	static void approaching( List< Pair< Message, Integer > > sortedMessageQueue ) {
		for ( Pair<Message, Integer> pair: sortedMessageQueue ) {
			int former = pair.getValue();
			pair.setValue( former - 1 );
		}
	}

	static void reachPost( Message m ) {
		switch ( m.getClass().getSimpleName() ) {
			case "Solicitation":
				m.getTo().ms.reachSolicitation( (Solicitation ) m );
				break;
			case "ReplyToSolicitation":
				m.getTo().ls.reachReply( ( ReplyToSolicitation ) m );
				break;
			case "ResultOfTeamFormation":
				m.getTo().ms.reachResult( ( ResultOfTeamFormation ) m );
				break;
			case "Done":
				m.getTo().ls.reachDone( ( Done ) m );
				break;
		}
	}

	public static int getMessageNum() {
		return messageNum;
	}

	static public double getAverageCommunicationTime() {
		return ( double ) communicationTime / messageNum;
	}

	public static void clear() {
		messageQueue.clear();
		messageNum = 0;
		communicationTime = 0;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		return str.toString();
	}

}
