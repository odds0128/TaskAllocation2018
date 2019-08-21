package main.research.agent.strategy.ProposedStrategy;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.strategy.LeaderStrategyWithRoleChange;
import main.research.agent.strategy.Strategy;
import main.research.communication.message.*;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.task.Task.disposeTask;
import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.DERenewalStrategy.*;
import static main.research.SetParam.ReplyType.*;
import static main.research.SetParam.ResultType.FAILURE;
import static main.research.SetParam.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.*;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class ProposedStrategy_l extends LeaderStrategyWithRoleChange implements SetParam {
	Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	Map< Agent, double[] > congestionDegreeMap = new HashMap<>();

	@Override
	public void sendSolicitations( Agent leader, Map< Agent, Subtask > agentSubtaskMap ) {
		for ( Map.Entry< Agent, Subtask > ag_st: agentSubtaskMap.entrySet() ) {
			timeToStartCommunicatingMap.put( ag_st.getKey(), getCurrentTime() );
			sendMessage( new Solicitation( leader, ag_st.getKey(), ag_st.getValue() ) );
		}
	}

	@Override
	public void formTeamAsL( Agent leader ) {
		if ( replyList.size() < repliesToCome ) return;
		else repliesToCome = 0;

		Map< Subtask, Agent > mapOfSubtaskAndAgent = new HashMap<>();
		while ( !replyList.isEmpty() ) {
			ReplyToSolicitation r = replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();
			updateRoundTripTime( currentFrom );

			if ( r.getReplyType() == DECLINE ) treatBetrayer( currentFrom );
			else if ( mapOfSubtaskAndAgent.containsKey( st ) ) {
				Agent rival = mapOfSubtaskAndAgent.get( st );
				Pair winnerAndLoser = compareDE( currentFrom, rival );

				exceptions.remove( winnerAndLoser.getValue() );
				sendMessage( new ResultOfTeamFormation( leader, ( Agent ) winnerAndLoser.getValue(), FAILURE, null ) );
				mapOfSubtaskAndAgent.put( st, ( Agent ) winnerAndLoser.getKey() );
			} else {
				mapOfSubtaskAndAgent.put( st, currentFrom );
			}
		}
		if ( canExecuteTheTask( myTask, mapOfSubtaskAndAgent.keySet() ) ) {
			for ( Map.Entry entry: mapOfSubtaskAndAgent.entrySet() ) {
				Agent friend = ( Agent ) entry.getValue();
				Subtask st = ( Subtask ) entry.getKey();

				sendMessage( new ResultOfTeamFormation( leader, friend, SUCCESS, st ) );
				appendAllocationHistory( friend, st );
				if ( withinTimeWindow() ) leader.workWithAsL[ friend.id ]++;
				leader.pastTasks.add( myTask );
			}
			leader.inactivate( 1 );
		} else {
			apologizeToFriends( leader, new ArrayList<>( mapOfSubtaskAndAgent.values() ) );
			exceptions.removeAll( new ArrayList<>( mapOfSubtaskAndAgent.values() ) );
			disposeTask();
			leader.inactivate( 0 );
		}
		myTask = null;
	}

	// hack: 現状リーダーは全員からの返信をもらってから割り当てを開始するため，早くに返信が到着したエージェントとの総通信時間が見かけ上長くなってしまう．
	// だからここではそれを訂正するために，その差分をroundTripTimeの一部として足し合わせることで混雑度の計算が狂わないようにしている
	void updateRoundTripTime( Agent target ) {
		int gap = getCurrentTime() - timeToStartCommunicatingMap.get( target ) - roundTripTimeMap.get( target );
		assert gap % 2 == 0 : "gap is odd.";
		int modifiedRoundTripTime = roundTripTimeMap.get( target ) + gap / 2;
		roundTripTimeMap.put( target, modifiedRoundTripTime );
	}

	@Override
	public void reachReply( ReplyToSolicitation r ) {
		super.reachReply( r );
		roundTripTimeMap.put( r.getFrom(), getCurrentTime() - timeToStartCommunicatingMap.get( r.getFrom() ) );
	}

	@Override
	public void checkDoneMessage( Agent leader, Done d ) {
		Agent from = d.getFrom();
		Subtask st = getAllocatedSubtask( d.getFrom() );

		int bindingTime = getCurrentTime() - timeToStartCommunicatingMap.get( from );
		renewCongestionDegreeMap( congestionDegreeMap, roundTripTimeMap, from, st, bindingTime );

		renewDE( reliableMembersRanking, from, 1 );
		exceptions.remove( from );

		// タスク全体が終わったかどうかの判定と，それによる処理
		// HACK: もうちょいどうにかならんか
		Task task = leader.findTaskContainingThisSubtask( st );

		task.subtasks.remove( st );

		if ( task.subtasks.isEmpty() ) {
			from.pastTasks.remove( task );
			Task.finishTask();
			from.didTasksAsLeader++;
		}
	}

	@Override
	protected void renewDE( Map< Agent, Double > deMap, Agent target, double evaluation ) {
		double formerDE = deMap.get( target );
		boolean b = evaluation > 0;
		double newDE;

		newDE = renewDEby0or1( formerDE, b );
		deMap.put( target, newDE );
		Strategy.sortReliabilityRanking( deMap );
	}

	// CONSIDER: 以下２つのメソッドが果たしてstaticがいいのか?
	private static void renewCongestionDegreeMap( Map< Agent, double[] > cdm, Map< Agent, Integer > rtm, Agent target, Subtask st, int bindingTime ) {
		double[] tempArray;
		if ( cdm.containsKey( target ) ) {
			tempArray = cdm.get( target );
		} else {
			tempArray = new double[ RESOURCE_TYPES ];
		}
		int requiredResourceType = st.resType;
		tempArray[ requiredResourceType ] = calculateCongestionDegree( bindingTime, rtm.get( target ), st );
		cdm.put( target, tempArray );
	}

	private static double calculateCongestionDegree( int bindingTime, int roundTripTime, Subtask subtask ) {
		int difficulty = subtask.reqRes[ subtask.resType ];
		return difficulty / ( bindingTime - 2.0 * roundTripTime );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( ", exceptions: " + exceptions.size() );
		return sb.toString();
	}

	void clear() {
	}
}
