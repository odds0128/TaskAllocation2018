package main.research.agent.strategy.ProposedStrategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.strategy.Strategy;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.others.random.*;
import main.research.agent.strategy.MemberStrategy;
import main.research.task.Subtask;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.DERenewalStrategy.*;

import static main.research.SetParam.ReplyType.*;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class ProposedStrategy_m extends MemberStrategy implements SetParam {
	private boolean joinFlag = false;

	@Override
	public void actAsMember( Agent member ) {
		while( ! member.ls.doneList.isEmpty() ) {
			Done d = member.ls.doneList.remove( 0 );
			member.ls.checkDoneMessage( member, d );
		}
		super.actAsMember( member );
	}

	protected void replyAsM( Agent member) {
		if( joinFlag ) {
			Strategy.proceedToNextPhase( member );
			joinFlag = false;
		} else if( getCurrentTime() - member.validatedTicks > THRESHOLD_FOR_ROLE_RENEWAL) {
			member.inactivate(0);
		}
	}

	protected void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
		if(  solicitations.isEmpty() ) return;

		sortSolicitationByDEofLeader(  solicitations, reliableLeadersRanking );

		int capacity = SUBTASK_QUEUE_SIZE - mySubtaskQueue.size() - expectedResultMessage;
		while (  solicitations.size() > 0 && capacity-- > 0 ) {
			Solicitation s = MyRandom.epsilonGreedy( Agent.ε ) ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
			expectedResultMessage++;
			joinFlag = true;
		}
		while ( ! solicitations.isEmpty() ) {
			Solicitation s = solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member,  s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}


	protected void receiveAsM(Agent member) {
		if( expectedResultMessage > 0 ) return;

		if ( mySubtaskQueue.isEmpty() ) {
			member.inactivate(0);
		} else {
			Subtask currentSubtask = mySubtaskQueue.get( 0 ).getValue();
			Agent currentLeader    = mySubtaskQueue.get( 0 ).getKey();

			member.allocated[currentLeader.id][currentSubtask.resType]++;
			member.executionTime = member.calculateExecutionTime(member, currentSubtask);
			Strategy.proceedToNextPhase(member);
		}
	}

	protected void execute(Agent member) {
		member.validatedTicks = getCurrentTime();

		if ( --member.executionTime == 0) {
			// HACK
			Pair<Agent, Subtask> pair = mySubtaskQueue.remove( 0 );
			Agent   currentLeader  = pair.getKey();
			Subtask currentSubtask = pair.getValue();

			member.required[currentSubtask.resType]++;
			TransmissionPath.sendMessage( new Done( member, currentLeader ) );
			renewDE( reliableLeadersRanking , currentLeader, 1, withBinary);

			if (member._coalition_check_end_time - getCurrentTime() < COALITION_CHECK_SPAN) {
				member.workWithAsM[currentLeader.id]++;
				member.didTasksAsMember++;
			}
			mySubtaskQueue.remove(currentSubtask);
			// consider: nextPhaseと一緒にできない？
			member.inactivate(1);
		}
	}


	private static void sortSolicitationByDEofLeader( List< Solicitation > solicitations, Map<Agent, Double> DEs ) {
		solicitations.sort( ( solicitation1, solicitation2 ) ->
			(int) ( DEs.get( solicitation2.getFrom() ) - DEs.get( solicitation1.getFrom() ) ));
	}

	static private Solicitation selectSolicitationRandomly( List< Solicitation > solicitations ) {
		int index = MyRandom.getRandomInt(0, solicitations.size() - 1);
		return solicitations.remove(index);
	}
}

