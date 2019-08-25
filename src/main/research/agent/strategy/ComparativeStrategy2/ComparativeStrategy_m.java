package main.research.agent.strategy.ComparativeStrategy2;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.communication.TransmissionPath;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.grid.Grid;
import main.research.others.Pair;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;

import java.util.HashMap;
import java.util.Map;

import static main.research.SetParam.ReplyType.ACCEPT;
import static main.research.SetParam.ReplyType.DECLINE;
import static main.research.SetParam.ResultType.SUCCESS;

import java.util.*;

public class ComparativeStrategy_m extends MemberStrategyWithRoleChange {
	Map< Agent, Integer > agentStartTimeMap = new HashMap<>();

	@Override
	public void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
		if ( solicitations.isEmpty() ) return;

		if( solicitations.size() > 1 ) {
			solicitations.sort( ( solicitation1, solicitation2 ) ->
				(int) ( reliableLeadersRanking.get( solicitation1.getFrom() ) - reliableLeadersRanking.get( solicitation2.getFrom() ) ));
		}

		int capacity = SUBTASK_QUEUE_SIZE - mySubtaskQueue.size() - expectedResultMessage;
		while ( solicitations.size() > 0 && capacity-- > 0 ) {
			Solicitation s = MyRandom.epsilonGreedy( Agent.ε ) ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
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
	protected void renewDE( Map< Agent, Double > deMap, Agent target, double evaluation ) {
		double formerDE = deMap.get( target );
		double newDE;

		newDE = renewDEbyArbitraryReward( formerDE, evaluation );
		// 0or1でやってみて結果が変わるか確認する
//		boolean flag = evaluation > 0 ? true : false;
//		newDE = renewDEby0or1( formerDE, flag );
		deMap.put( target, newDE );
	}
}
