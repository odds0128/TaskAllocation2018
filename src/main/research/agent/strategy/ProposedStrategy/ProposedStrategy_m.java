package main.research.agent.strategy.ProposedStrategy;

import main.research.Manager;
import main.research.SetParam;
import main.research.agent.Agent;
import main.research.communication.MessageDeprecated;
import main.research.random.MyRandom;
import main.research.agent.strategy.MemberStrategy;
import main.research.task.AllocatedSubtask;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.DERenewalStrategy.*;

import static main.research.SetParam.MessageType.*;
import static main.research.SetParam.ReplyType.*;
import static main.research.SetParam.Phase.*;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class ProposedStrategy_m extends MemberStrategy implements SetParam {
	private List<MessageDeprecated> solicitations = new ArrayList<>();

	protected void replyAsM(Agent member) {
		boolean flag = replyToSolicitations( member, SUBTASK_QUEUE_SIZE, solicitations );

		if ( flag && getCurrentTime() - member.validatedTicks > THRESHOLD_FOR_ROLE_RENEWAL) member.inactivate(0);
		else proceedToNextPhase(member);
	}

	// TODO: リーダーとサブタスクの対応がバラバラ侍
	protected void receiveAsM(Agent member) {
		if ( member.mySubtaskQueue.isEmpty() ) {
			member.inactivate(0);
		} else {
			// HACK
			Subtask currentSubtask = member.mySubtaskQueue.keySet().iterator().next();
			Agent currentLeader    = member.mySubtaskQueue.get(currentSubtask);

			member.allocated[currentLeader.id][currentSubtask.resType]++;
			member.executionTime = member.calcExecutionTime(member, currentSubtask);
			proceedToNextPhase(member);
		}
	}

	protected void execute(Agent agent_m) {
		agent_m.validatedTicks = getCurrentTime();

		if ( --agent_m.executionTime == 0) {
			// HACK
			Subtask currentSubtask = agent_m.mySubtaskQueue.keySet().iterator().next();
			Agent currentLeader = agent_m.mySubtaskQueue.get(currentSubtask);

			agent_m.required[currentSubtask.resType]++;
			agent_m.sendMessage(agent_m, currentLeader, DONE, 0);
			renewDE( agent_m.reliabilityRankingAsM , currentLeader, 1, withBinary);

			if (agent_m._coalition_check_end_time - getCurrentTime() < COALITION_CHECK_SPAN) {
				agent_m.workWithAsM[currentLeader.id]++;
				agent_m.didTasksAsMember++;
			}
			// 自分のサブタスクが終わったら役割適応度を1で更新して非活性状態へ
			agent_m.mySubtaskQueue.remove(currentSubtask);
			// consider: nextPhaseと一緒にできない？
			agent_m.inactivate(1);
		}
	}

	public void checkMessages(Agent ag_m) {
		while ( ! ag_m.messages.isEmpty() ) {
			MessageDeprecated m = ag_m.messages.remove(0);

			switch ( ( MessageType ) m.getMessageType() ) {
				case DONE:
					reactToDoneMessage( ag_m, m.getFrom() );
					break;
				case SOLICITATION:
					reactToSolicitingMessage(ag_m, m);
					break;
				case RESULT:
					reactToResultMessage(ag_m, m);
					break;
				default:
					System.out.println( ag_m.id + " , type : " + m.getMessageType() );
			}
		}
	}

	// TODO: leaderの側に移動してstatic化する
	void reactToDoneMessage(Agent ag_m, Agent from ) {
		renewDE( ag_m.reliabilityRankingAsL, from, 1, withBinary );
		AllocatedSubtask as = teamHistory[ag_m.id].remove( from );

		// タスク全体が終わったかどうかの判定と，それによる処理
		Task task = ag_m.identifyTask(as.getTaskId());
		task.subtasks.remove(as.getSt());
		if ( task.subtasks.isEmpty() ) {
			ag_m.pastTasks.remove(task);
			Manager.finishTask(ag_m, task);
			ag_m.didTasksAsLeader++;
		}
	}

	private void reactToSolicitingMessage(Agent ag_m, MessageDeprecated m) {
		if( ! ( ag_m.mySubtaskQueue.size() < SUBTASK_QUEUE_SIZE ) ) {
			Agent.sendMessage(ag_m, m.getFrom(), REPLY, DECLINE);
		} else {
			solicitations.add(m);
		}
	}

	private void reactToResultMessage(Agent ag_m, MessageDeprecated m) {
		if( m.getSubtask() == null ) {
			renewDE( ag_m.reliabilityRankingAsM, m.getFrom(), 0, withBinary);
		} else {
			ag_m.mySubtaskQueue.put( m.getSubtask(), m.getFrom() );
		}
	}

	private static boolean replyToSolicitations(Agent member, int room, List<MessageDeprecated> solicitations) {
		if(  solicitations.isEmpty() ) return false;

		boolean joinFlag = false;
		sortSolicitationByDEofLeader(  solicitations, member.reliabilityRankingAsM );

		while (  solicitations.size() > 0 && room > 0 ) {
			// εグリーディーで選択する
			if ( MyRandom.epsilonGreedy( Agent.ε ) ) {
				Agent myRandomLeader = selectLeaderRandomly(  solicitations );
				Agent.sendMessage( member, myRandomLeader, REPLY, ACCEPT);
				joinFlag = true;
				continue;
			}
			Agent.sendMessage( member, solicitations.remove(0).getFrom(), REPLY, ACCEPT);
			member.numberOfExpectedMessages++;
			joinFlag = true;
		}
		while ( ! solicitations.isEmpty() ) {
			member.sendMessage(member,  solicitations.remove(0).getFrom(), REPLY, DECLINE);
		}
		return joinFlag;
	}


	private static void sortSolicitationByDEofLeader(List<MessageDeprecated> solicitations, Map<Agent, Double> DEs ) {
		solicitations.sort( ( solicitation1, solicitation2 ) ->
			(int) ( DEs.get( solicitation2.getFrom() ) - DEs.get( solicitation1.getFrom() ) ));
		// remove
		if( solicitations.get(0).getTo().id == 439 ) {
			List<Double> values = new ArrayList<>( DEs.values() );
			System.out.println( values.subList(0, 5));
		}
	}

	static private Agent selectLeaderRandomly( List<MessageDeprecated> solicitations ) {
		int index = MyRandom.getRandomInt(0, solicitations.size() - 1);
		MessageDeprecated target = solicitations.remove(index);

		return target.getFrom();
	}

	// TODO: Agentクラスのstaticにする
	private void proceedToNextPhase(Agent m ) {
		switch ( m.phase ) {
			case WAITING:
				m.phase = RECEPTION;
				break;
			case RECEPTION:
				m.phase = EXECUTION;
				break;
			case EXECUTION:
				m.phase = SELECT_ROLE;
				break;
		}
		m.validatedTicks = getCurrentTime();
	}

}
