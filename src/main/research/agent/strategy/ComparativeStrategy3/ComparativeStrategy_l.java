package main.research.agent.strategy.ComparativeStrategy3;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.CDSet;
import main.research.agent.strategy.LeaderStrategyWithRoleChange;
import main.research.communication.message.*;
import main.research.others.Pair;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.task.Task.disposeTask;
import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.ReplyType.*;
import static main.research.SetParam.ResultType.FAILURE;
import static main.research.SetParam.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.*;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class ComparativeStrategy_l extends LeaderStrategyWithRoleChange implements SetParam {
	static double CD_THRESHOLD = 5.0;
	static double DE_THRESHOLD = INITIAL_VALUE_OF_DE;

	private Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	private Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	private List< CDSet > cdSetList = new ArrayList<>();

	@Override
	public void sendSolicitations( Agent leader, Map< Agent, Subtask > agentSubtaskMap ) {
		for ( Map.Entry< Agent, Subtask > ag_st: agentSubtaskMap.entrySet() ) {
			timeToStartCommunicatingMap.put( ag_st.getKey(), getCurrentTime() );
			sendMessage( new Solicitation( leader, ag_st.getKey(), ag_st.getValue() ) );
		}
	}

	// todo: 混雑度を元にしたメンバー選定のロジックの実装
	//
	@Override
	protected Map< Agent, Subtask > selectMembers( List< Subtask > subtasks ) {
		Map< Agent, Subtask > memberCandidates = new HashMap<>();
		Agent candidate;
		CDSet.forgetOldCdInformation( cdSetList );

		for ( int i = 0; i < REBUNDUNT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: subtasks ) {
				if ( MyRandom.epsilonGreedy( Agent.ε ) ) candidate = selectMemberForASubtaskRandomly( st );
				else candidate = this.selectMemberArbitrary( st );

				if( candidate == null ) {
					return new HashMap< >() ;
				}

				exceptions.add( candidate );
				memberCandidates.put( candidate, st );
			}
		}
		return memberCandidates;
	}

	public static int nulls = 0, notNulls = 0;
	private Agent selectMemberArbitrary( Subtask st ) {
		Agent temp = selectMemberAccordingToCD( st );
		if( temp == null ) nulls++; else notNulls++;
		return temp == null ? selectMemberAccordingToDE( st ) : temp;
	}

	// Todo: DE優先の場合も試す
	// TODO: 複数渡せるようにしなきゃ
	private Agent selectMemberAccordingToCD( Subtask st ) {
		Agent candidate = null;
		double maxCD = CD_THRESHOLD, maxDE = 0;
		int resType = st.resType;

		for( CDSet set : cdSetList ) {
			Agent temp = set.getTarget();
			double cdValue = CDSet.getCD( temp, cdSetList )[resType]; // consider: 回りくどくない?
			double deValue = getPairByAgent( temp, reliableMembersRanking ).getDe();
			if( cdValue > maxCD || cdValue == maxCD && maxDE < deValue ) {
				candidate = temp; maxCD = cdValue; maxDE = deValue;
			}
		}
		return candidate;
	}

	private Agent selectMemberAccordingToDE( Subtask st ) {
		for ( AgentDePair pair : reliableMembersRanking ) {
			Agent ag = pair.getTarget();
			if ( ( ! exceptions.contains( ag ) ) && ag.canProcessTheSubtask( st ) ) return ag;
		}
		return null;
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
	private void updateRoundTripTime( Agent target ) {
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
		renewCongestionDegreeMap( from, st, bindingTime );

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
	protected void renewDE( List< AgentDePair > pairList, Agent target, double evaluation ) {
		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}



	// HACK
	private void renewCongestionDegreeMap( Agent target, Subtask st, int bindingTime ) {
		double[] tempArray;
		if ( CDSet.alreadyExists( target, cdSetList ) ) {
			tempArray = CDSet.getCD( target, cdSetList );
			int resType = st.resType;
			tempArray[ resType ] = calculateCD( bindingTime, roundTripTimeMap.get( target ), st );
			CDSet.replace( target, tempArray, cdSetList );
		} else {
			tempArray = new double[ RESOURCE_TYPES ];
			int resType = st.resType;
			tempArray[ resType ] = calculateCD( bindingTime, roundTripTimeMap.get( target ), st );
			cdSetList.add( new CDSet( target, tempArray, getCurrentTime() ) );
		}
	}

	private double calculateCD( int bindingTime, int roundTripTime, Subtask subtask ) {
		int difficulty = subtask.reqRes[ subtask.resType ];
		return difficulty / ( bindingTime - 2.0 * roundTripTime );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( ", exceptions: " + exceptions.size() );
		sb.append( ", cdList: " + cdSetList );
		return sb.toString();
	}

	public static List<CDSet> getCdSetList( ComparativeStrategy_l psl ) {
		return psl.cdSetList;
	}

	void clear() {
	}
}
