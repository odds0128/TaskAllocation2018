package main.research.communication;

import main.research.Manager;
import main.research.agent.AgentManager;
import main.research.communication.message.*;
import main.research.others.Pair;
import main.research.Parameter;
import main.research.grid.Grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TransmissionPath implements Parameter {
	private static int totalMessageNum = 0;
	private static int communicationTime = 0;

	private static List< Pair< Message, Integer > > messageQueue = new ArrayList<>();

	static int solicit_num_ = 0, reply_accept_num_ = 0, reply_decline_num_ = 0;
	static int result_success_num_ = 0, result_failure_num_ = 0, done_num_ = 0;
	public static int[] solicitToAgents = new int[ AgentManager.agent_num_ ];

	private static final int monitoredAgent1 = 26;
	private static final int monitoredAgent2 = 491;
	private static final boolean doMonitor = false;
	private static final boolean doMonitorAll = false;

	public static void sendMessage( Message m ) {
		assert m.getFrom() != m.getTo() : "he asks himself";

		if ( doMonitor ) {
			if ( doMonitorAll ) {
				System.out.print( m );
			} else if ( m.getFrom().id == monitoredAgent1 && m.getTo().id == monitoredAgent2 ||
				m.getTo().id == monitoredAgent1 && m.getFrom().id == monitoredAgent2 ) {
				System.out.print( String.format( "%8d: ", Manager.getCurrentTime() ) + m );
			}
		}

		countMessage( m );
		int untilArrival = Grid.getDelay( m.getFrom(), m.getTo() );
		messageQueue.add( new Pair<>( m, untilArrival ) );
		totalMessageNum++;
		communicationTime += untilArrival;
	}

	private static void countMessage( Message m ) {
		switch ( m.getClass().getSimpleName() ) {
			case "Solicitation":
				solicitToAgents[ m.getTo().id ]++;
				solicit_num_++;
				break;
			case "ReplyToSolicitation":
				Reply rs = ( Reply ) m;
				if ( rs.getReplyType() == ReplyType.ACCEPT ) reply_accept_num_++;
				else if ( rs.getReplyType() == ReplyType.DECLINE ) reply_decline_num_++;
				break;
			case "ResultOfTeamFormation":
				Result rt = ( Result ) m;
				if ( rt.getResult() == ResultType.SUCCESS ) result_success_num_++;
				else if ( rt.getResult() == ResultType.FAILURE ) result_failure_num_++;
				break;
			case "Done":
				done_num_++;
				break;
		}
	}

	public static void transmit() {
		Collections.sort( messageQueue, Comparator.comparing( Pair::getValue ) );
		approaching( messageQueue );

		while ( !messageQueue.isEmpty() ) {
			Pair< Message, Integer > pair = messageQueue.remove( 0 );
			if ( pair.getValue() == 0 ) {
				pair.getKey().getTo().reachPost( pair.getKey() );
			} else {
				messageQueue.add( 0, pair );
				break;
			}
		}
	}

	static void approaching( List< Pair< Message, Integer > > sortedMessageQueue ) {
		for ( Pair< Message, Integer > pair: sortedMessageQueue ) {
			int former = pair.getValue();
			pair.setValue( former - 1 );
		}
	}

	public static int getMessageNum() {
		int temp = totalMessageNum;
		return temp;
	}

	static public double getAverageCommunicationTime() {
		return ( double ) communicationTime / totalMessageNum;
	}

	public static void clear() {
		messageQueue.clear();
		totalMessageNum = 0;
		communicationTime = 0;
	}

	public static void showMessages() {
		System.out.println( "solicitations: " + solicit_num_ );
		System.out.println( "replies: " + ( reply_accept_num_ + reply_decline_num_ ) );
		System.out.println( " - accept: " + reply_accept_num_ );
		System.out.println( " - decline: " + reply_decline_num_ );
		System.out.println( "results: " + ( result_success_num_ + result_failure_num_ ) );
		System.out.println( " - success: " + result_success_num_ );
		System.out.println( " - failure: " + result_failure_num_ );
		System.out.println( "done: " + ( done_num_ ) );
	}
}
