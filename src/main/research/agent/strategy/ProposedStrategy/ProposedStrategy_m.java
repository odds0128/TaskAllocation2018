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
import static main.research.agent.strategy.Strategy.renewDE;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class ProposedStrategy_m extends MemberStrategy implements SetParam {
	private boolean joinFlag = false;

	@Override
	public void actAsMember( Agent member ) {
		// doneの処理
		while( ! member.ls.doneList.isEmpty() ) {
			Done d = member.ls.doneList.remove( 0 );
			member.ls.checkDoneMessage( member, d );
		}
		super.actAsMember( member );
	}

	protected void replyAsM( Agent member) {
		if ( joinFlag && getCurrentTime() - member.validatedTicks > THRESHOLD_FOR_ROLE_RENEWAL) member.inactivate(0);
		else Strategy.proceedToNextPhase(member); joinFlag = false;
	}

	protected void receiveAsM(Agent member) {
		if ( member.mySubtaskQueue.isEmpty() ) {
			member.inactivate(0);
		} else {
			// HACK
			Subtask currentSubtask = member.mySubtaskQueue.get( 0 ).getValue();
			Agent currentLeader    = member.mySubtaskQueue.get( 0 ).getKey();

			member.allocated[currentLeader.id][currentSubtask.resType]++;
			member.executionTime = member.calculateExecutionTime(member, currentSubtask);
			Strategy.proceedToNextPhase(member);
		}
	}

	protected void execute(Agent member) {
		member.validatedTicks = getCurrentTime();

		if ( --member.executionTime == 0) {
			// HACK
			Pair<Agent, Subtask> pair = member.mySubtaskQueue.remove( 0 );
			Agent   currentLeader  = pair.getKey();
			Subtask currentSubtask = pair.getValue();

			member.required[currentSubtask.resType]++;
			TransmissionPath.sendMessage( new Done( member, currentLeader ) );
			renewDE( member.reliabilityRankingAsM , currentLeader, 1, withBinary);

			if (member._coalition_check_end_time - getCurrentTime() < COALITION_CHECK_SPAN) {
				member.workWithAsM[currentLeader.id]++;
				member.didTasksAsMember++;
			}
			member.mySubtaskQueue.remove(currentSubtask);
			// consider: nextPhaseと一緒にできない？
			member.inactivate(1);
		}
	}

	protected void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
		if(  solicitations.isEmpty() ) return;

		sortSolicitationByDEofLeader(  solicitations, member.reliabilityRankingAsM );

		int acceptationCount = 0;
		while (  solicitations.size() > 0 && acceptationCount++ <= SUBTASK_QUEUE_SIZE ) {
			Solicitation s = MyRandom.epsilonGreedy( Agent.ε ) ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
			joinFlag = true;
		}
		while ( ! solicitations.isEmpty() ) {
			Solicitation s = solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member,  s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
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

