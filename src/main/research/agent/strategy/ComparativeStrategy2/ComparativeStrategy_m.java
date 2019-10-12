package main.research.agent.strategy.ComparativeStrategy2;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.communication.TransmissionPath;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static main.research.SetParam.ReplyType.ACCEPT;
import static main.research.SetParam.ReplyType.DECLINE;
import static main.research.SetParam.ResultType.SUCCESS;

public class ComparativeStrategy_m extends MemberStrategyWithRoleChange {
	Map< Agent, Integer > agentStartTimeMap = new HashMap<>();

	@Override
	public void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
		if ( solicitations.isEmpty() ) return;

		if( solicitations.size() > 1 ) {
			solicitations.sort( ( solicitation1, solicitation2 ) -> compareSolicitations( solicitation1, solicitation2, reliableLeadersRanking ) );
		}

		int capacity = SUBTASK_QUEUE_SIZE - mySubtaskQueue.size() - expectedResultMessage;
		while ( solicitations.size() > 0 && capacity-- > 0 ) {
			Solicitation s = Agent.epsilonGreedy( ) ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
			this.agentStartTimeMap.put( s.getFrom(), Manager.getCurrentTime() );
			expectedResultMessage++;
			joinFlag = true;
		}
		while ( !solicitations.isEmpty() ) {
			Solicitation s = solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}

	@Override
	public void reactToResultMessage( ResultOfTeamFormation r ) {
		if ( r.getResult() == SUCCESS ) {
			Subtask st = r.getAllocatedSubtask();
			int roundTripTime = Manager.getCurrentTime() - this.agentStartTimeMap.remove( r.getFrom() );
			Pair< Agent, Subtask > pair = new Pair<>( r.getFrom(), st );
			mySubtaskQueue.add( pair );

			double reward = ( double ) st.reqRes[ st.resType ] / ( double ) roundTripTime;
			this.renewDE( reliableLeadersRanking, r.getFrom(), reward );

		} else {
			this.agentStartTimeMap.remove( r.getFrom() );
			this.renewDE( reliableLeadersRanking, r.getFrom(), 0 );
		}
	}

	@Override
	protected void renewDE( List< AgentDePair > pairList, Agent target, double evaluation ) {
		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );

	}
}
