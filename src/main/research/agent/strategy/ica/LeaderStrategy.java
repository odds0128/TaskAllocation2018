package main.research.agent.strategy.ica;

import main.research.Manager;
import main.research.agent.Agent;
import main.research.agent.strategy.LeaderTemplateStrategy;
import main.research.communication.message.Done;
import main.research.communication.message.Reply;
import main.research.communication.message.Result;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static main.research.Parameter.ReplyType.DECLINE;
import static main.research.Parameter.ResultType.FAILURE;
import static main.research.Parameter.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.TaskManager.disposeTask;

public class LeaderStrategy extends LeaderTemplateStrategy {
	Map< Agent, Integer > agentStartTimeMap = new LinkedHashMap<>();

	@Override
	protected Agent selectMemberFor( List<Agent> exceptions, Subtask st ) {
		for ( Dependability pair: dependabilityRanking ) {
			Agent ag = pair.getAgent();
			if ( ( !exceptions.contains( ag ) ) && ag.canProcess( st ) ) return ag;
		}
		return null;
	}


	@Override
	public void sendSolicitations( Agent leader, List< Allocation > allocationList ) {
		for ( Allocation al : allocationList ) {
			sendMessage( new Solicitation( leader, al.getAg(), al.getSt() ) );
		}
	}

	@Override
	public void formTeamAsL( Agent leader ) {
		if ( leader.replyList.size() < repliesToCome ) return;

		assert repliesToCome == leader.replyList.size() : "Expected: " + repliesToCome + ", Actual: " + leader.resultList.size();

		Map< Subtask, Agent > mapOfSubtaskAndAgent = new LinkedHashMap<>();
		while ( !leader.replyList.isEmpty() ) {
			Reply r = leader.replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();

			if ( r.getReplyType() == DECLINE ) treatBetrayer( currentFrom );
			else if ( mapOfSubtaskAndAgent.containsKey( st ) ) {
				Agent rival = mapOfSubtaskAndAgent.get( st );
				Pair winnerAndLoser = compareDE( currentFrom, rival );

				sendMessage( new Result( leader, ( Agent ) winnerAndLoser.getValue(), FAILURE, null ) );
				mapOfSubtaskAndAgent.put( st, ( Agent ) winnerAndLoser.getKey() );
			} else {
				mapOfSubtaskAndAgent.put( st, currentFrom );
			}
		}
		if ( canExecuteTheTask( myTask, mapOfSubtaskAndAgent.keySet() ) ) {
			for ( Map.Entry entry: mapOfSubtaskAndAgent.entrySet() ) {
				Agent friend = ( Agent ) entry.getValue();
				Subtask st = ( Subtask ) entry.getKey();

				agentStartTimeMap.put( friend, Manager.getCurrentTime() );

				sendMessage( new Result( leader, friend, SUCCESS, st ) );
				appendAllocationHistory( friend, st );
				if ( withinTimeWindow() ) leader.workWithAsL[ friend.id ]++;
			}
			leader.pastTasks.add( myTask );
			nextPhase( leader, true );
		} else {
			apologizeToFriends( leader, new ArrayList<>( mapOfSubtaskAndAgent.values() ) );
			disposeTask(leader);
			nextPhase( leader, false );
		}
		myTask = null;
	}

	@Override
	public void checkAllDoneMessages( Agent leader ) {
		while ( !leader.doneList.isEmpty() ) {
			Done d = leader.doneList.remove( 0 );
			Agent from = d.getFrom();
			Subtask st = d.getSt();
			removeAllocationHistory( from, st );

			int roundTripTime = Manager.getCurrentTime() - this.agentStartTimeMap.get( from );
			// consider: 謎の5
			double reward = 5 * ( double ) st.reqRes[ st.reqResType ] / ( double ) roundTripTime;

			this.renewDE( dependabilityRanking, from, reward );

			// タスク全体が終わったかどうかの判定と，それによる処理
			// HACK: もうちょいどうにかならんか
			// 今終わったサブタスクasが含まれるtaskを見つける
			// それによってタスク全体が終われば終了報告等をする

			Task task = leader.findTaskContaining( st );
			task.subtasks.remove( st );
			if ( task.subtasks.isEmpty() ) {
				from.pastTasks.remove( task );
				TaskManager.finishTask(leader);
				from.didTasksAsLeader++;
			}
		}
	}

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}
}
