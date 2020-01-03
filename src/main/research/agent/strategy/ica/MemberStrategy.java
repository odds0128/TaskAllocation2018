package main.research.agent.strategy.ica;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.agent.strategy.MemberTemplateStrategy;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Reply;
import main.research.communication.message.Result;
import main.research.communication.message.Solicitation;
import main.research.task.Subtask;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static main.research.Parameter.ReplyType.ACCEPT;
import static main.research.Parameter.ReplyType.DECLINE;
import static main.research.Parameter.ResultType.SUCCESS;

public class MemberStrategy extends MemberTemplateStrategy {
	Map< Agent, Integer > agentStartTimeMap = new HashMap<>();

	@Override
	public void replyTo( List< Solicitation > solicitations, Agent member ) {
		if ( solicitations.isEmpty() ) return;

		if( solicitations.size() > 1 ) {
			solicitations.sort( ( solicitation1, solicitation2 ) -> compareSolicitations( solicitation1, solicitation2, dependabilityRanking ) );
		}

		int capacity = Agent.subtask_queue_size_ - mySubtaskQueue.size() - expectedResultMessage;
		while ( solicitations.size() > 0 && capacity-- > 0 ) {
			Solicitation s = Agent.epsilonGreedy( ) ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
			TransmissionPath.sendMessage( new Reply( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
			this.agentStartTimeMap.put( s.getFrom(), Manager.getCurrentTime() );
			expectedResultMessage++;
		}
		while ( !solicitations.isEmpty() ) {
			Solicitation s = solicitations.remove( 0 );
			TransmissionPath.sendMessage( new Reply( member, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}

	@Override
	public void reactTo( Result r ) {
		if ( r.getResult() == SUCCESS ) {
			Subtask st = r.getAllocatedSubtask();
			int roundTripTime = Manager.getCurrentTime() - this.agentStartTimeMap.remove( r.getFrom() );
			SubtaskFrom sf = new SubtaskFrom(st, r.getFrom());
			mySubtaskQueue.add( sf );
			currentSubtaskProcessTime = calculateProcessTime( r.getTo(), st );

			double reward = ( double ) st.reqRes[ st.reqResType ] / ( double ) roundTripTime;
			this.renewDE( dependabilityRanking, r.getFrom(), reward );

		} else {
			this.agentStartTimeMap.remove( r.getFrom() );
			this.renewDE( dependabilityRanking, r.getFrom(), 0 );
		}
	}

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );

	}
}
