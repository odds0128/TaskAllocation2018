package main.research.agent.strategy.reliableAgents;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.agent.strategy.MemberTemplateStrategy;
import main.research.communication.TransmissionPath;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.Solicitation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static main.research.Parameter.ReplyType.ACCEPT;
import static main.research.Parameter.ReplyType.DECLINE;


public class MemberStrategy extends MemberTemplateStrategy implements Parameter {
	public static final double threshold_of_reliable_leader = 0.7;

	public static int waiting = 0;
	public static double calculateMeanDE() {
		List<Agent> agentList = AgentManager.getAllAgentList();
		List<Agent> members = new ArrayList<>(  );
		for( Agent ag : agentList ) {
			if( ag.e_member > ag.e_leader ) members.add( ag );
		}

		double sum = 0;
		int nonZeroLeaders = 0;

		for( Agent m : members ) {
			List< Dependability > validPairsList = m.ms.reliableLeadersRanking.stream()
				.filter( pair -> pair.getValue() > 0 )
				.collect( Collectors.toList( ) );
			nonZeroLeaders += validPairsList.stream()
				.count();
			sum += validPairsList.stream()
				.mapToDouble( pair -> pair.getValue() )
				.sum();
		}
		return sum/nonZeroLeaders;
	}

	public static int countReciprocalMembers() {
		return ( int ) AgentManager.getAllAgentList().stream()
			.filter( agent -> agent.e_member > agent.e_leader )
			.filter( member -> member.ms.reliableLeadersRanking.get( 0 ).getValue() > threshold_of_reliable_leader )
			.count();
	}

	@Override
	public void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
		if(  solicitations.isEmpty() ) return;

		solicitations.sort( ( solicitation1, solicitation2 ) ->
			compareSolicitations( solicitation1, solicitation2, reliableLeadersRanking ) );

		if( haveReliableLeader() ) {
			member.principle = Principle.RECIPROCAL;
			replyToReliableLeader( member, solicitations );
		} else {
			member.principle = Principle.RATIONAL;
			replyToOrdinaryLeaders( member, solicitations );
		}
	}

	private void replyToReliableLeader( Agent member, List< Solicitation> solicitations ) {
		int capacity = SUBTASK_QUEUE_SIZE - mySubtaskQueue.size() - expectedResultMessage;
		Agent relLeader = reliableLeadersRanking.get( 0 ).getAgent();
		while( ! solicitations.isEmpty() && capacity > 0) {
			Solicitation s = solicitations.remove( 0 );
			if( s.getFrom() == relLeader ) {
				accept( member, s );
				capacity--;
			}else {
				TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
			}
		}
		declineAll( member, solicitations );
	}

	private boolean haveReliableLeader() {
		return reliableLeadersRanking.get( 0 ).getValue() >= threshold_of_reliable_leader;
	}

	private void accept( Agent member, Solicitation s ) {
		TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
		expectedResultMessage++;
		joinFlag = true;
	}

	private void declineAll( Agent member, List<Solicitation> solicitations ) {
		while ( !solicitations.isEmpty() ) {
			Solicitation s = solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}
	private void replyToOrdinaryLeaders( Agent member, List<Solicitation> solicitations ) {
		int capacity = SUBTASK_QUEUE_SIZE - mySubtaskQueue.size() - expectedResultMessage;
		while ( solicitations.size() > 0 && capacity-- > 0 ) {
			// TODO : 単純にDEの高い順じゃなくて信頼エージェントを判定する.とりあえず信頼エージェントの上限は1で
			Solicitation s = Agent.epsilonGreedy() ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
			accept( member, s );
		}
		declineAll( member, solicitations );
	}


	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}
}

