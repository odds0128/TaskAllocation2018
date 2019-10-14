package main.research.agent.strategy.reliableAgents;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.communication.TransmissionPath;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.Solicitation;

import java.util.List;

import static main.research.SetParam.ReplyType.ACCEPT;
import static main.research.SetParam.ReplyType.DECLINE;


public class MemberStrategy extends MemberStrategyWithRoleChange implements SetParam {
	private final double threshold_of_reliable_leader = 0.5;

	// remove
	public static int waiting = 0;
	@Override
	public void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
		if(  solicitations.isEmpty() ) return;

		solicitations.sort( ( solicitation1, solicitation2 ) ->
			compareSolicitations( solicitation1, solicitation2, reliableLeadersRanking ) );

		if( haveReliableLeader() ) {
			Agent reliableLeader = reliableLeadersRanking.get( 0 ).getAgent();
			Solicitation s = solicitations.remove( 0 );
			if( reliableLeader.equals( s.getFrom() ) ) {
				accept( member, s );
			}
			else {
				// remove
				waiting++;
				TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
			}
			decline( member, solicitations );
		} else {
			replyToOrdinaryLeaders( member, solicitations );
		}
	}

	private boolean haveReliableLeader() {
		return reliableLeadersRanking.get( 0 ).getDe() >= threshold_of_reliable_leader;
	}

	private void accept( Agent member, Solicitation s ) {
		TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
		expectedResultMessage++;
		joinFlag = true;
	}

	private void decline( Agent member, List<Solicitation> solicitations ) {
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
		decline( member, solicitations );
	}


	@Override
	protected void renewDE( List<AgentDePair> pairList, Agent target, double evaluation ) {
		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}
}

