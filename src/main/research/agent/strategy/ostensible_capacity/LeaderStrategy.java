package main.research.agent.strategy.ostensible_capacity;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.strategy.LeaderTemplateStrategy;
import main.research.agent.strategy.OstensibleCapacity;
import main.research.communication.message.*;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;

import static main.research.task.TaskManager.disposeTask;
import static main.research.Manager.getCurrentTime;
import static main.research.Parameter.ReplyType.*;
import static main.research.Parameter.ResultType.FAILURE;
import static main.research.Parameter.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.*;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class LeaderStrategy extends LeaderTemplateStrategy implements Parameter {
	static double OC_THRESHOLD = 3.0;

	private Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	private Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	private List< OstensibleCapacity > ostensibleCapacityList = new ArrayList<>();

	@Override
	public void sendSolicitations( Agent leader, List< Allocation > allocationList ) {
		for ( Allocation al: allocationList ) {
			timeToStartCommunicatingMap.put( al.getAg(), getCurrentTime() );
			sendMessage( new Solicitation( leader, al.getAg(), al.getSt() ) );
		}
	}


	@Override
	protected List< Allocation > makePreAllocationMap( List< Subtask > subtasks ) {
		List<Allocation> preAllocationList = new ArrayList<>(  );
		Agent candidate;
		OstensibleCapacity.forgetOldOcInformation( ostensibleCapacityList );

		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: subtasks ) {
				if ( Agent.epsilonGreedy() ) candidate = selectMemberForASubtaskRandomly( st );
				else candidate = this.selectMemberFor( st );

				if ( candidate == null ) {
					return null;
				}

				exceptions.add( candidate );
				preAllocationList.add( new Allocation( candidate, st ) );
			}
		}
		return preAllocationList;
	}

	public static int nulls = 0, notNulls = 0;

	@Override
	protected Agent selectMemberFor( Subtask st ) {
		Agent temp = selectMemberAccordingToOC( st );
		if ( temp == null ) nulls++;
		else notNulls++;
		return temp == null ? selectMemberAccordingToDE( st ) : temp;
	}

	// Todo: DE優先の場合も試す
	// TODO: 複数渡せるようにしなきゃ
	private Agent selectMemberAccordingToOC( Subtask st ) {
		Agent candidate = null;
		double maxOC = OC_THRESHOLD, maxDE = 0.5;
		int resType = st.reqResType;

		for ( OstensibleCapacity set: ostensibleCapacityList ) {
			Agent temp = set.getTarget();
			if ( exceptions.contains( temp ) ) continue;
			double ocValue = OstensibleCapacity.getOC( resType, temp, ostensibleCapacityList );
			double deValue = getDeByAgent( temp, dependabilityRanking ).getValue();
			if ( ocValue > maxOC && maxDE > 0.5 || ocValue == maxOC && maxDE < deValue ) {
				candidate = temp;
				maxOC = ocValue;
				maxDE = deValue;
			}
		}
		return candidate;
	}

	private Agent selectMemberAccordingToDE( Subtask st ) {
		for ( Dependability pair: dependabilityRanking ) {
			Agent ag = pair.getAgent();
			if ( ( !exceptions.contains( ag ) ) && ag.canProcess( st ) ) return ag;
		}
		return null;
	}

	@Override
	public void formTeamAsL( Agent leader ) {
		if ( leader.replyList.size() < repliesToCome ) return;
		else repliesToCome = 0;

		Map< Subtask, Agent > mapOfSubtaskAndAgent = new HashMap<>();
		while ( !leader.replyList.isEmpty() ) {
			Reply r = leader.replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();
			updateRoundTripTime( currentFrom );

			if ( r.getReplyType() == DECLINE ) treatBetrayer( currentFrom );
			else if ( mapOfSubtaskAndAgent.containsKey( st ) ) {
				Agent rival = mapOfSubtaskAndAgent.get( st );
				Pair winnerAndLoser = compareDE( currentFrom, rival );

				exceptions.remove( winnerAndLoser.getValue() );
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

				sendMessage( new Result( leader, friend, SUCCESS, st ) );
				appendAllocationHistory( friend, st );
				if ( withinTimeWindow() ) leader.workWithAsL[ friend.id ]++;
			}
			leader.pastTasks.add( myTask );
			leader.phase = nextPhase( leader, true );
		} else {
			apologizeToFriends( leader, new ArrayList<>( mapOfSubtaskAndAgent.values() ) );
			exceptions.removeAll( new ArrayList<>( mapOfSubtaskAndAgent.values() ) );
			disposeTask( leader );
			leader.phase = nextPhase( leader, false );
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
	public void reachReply( Reply r ) {
		super.reachReply( r );
		roundTripTimeMap.put( r.getFrom(), getCurrentTime() - timeToStartCommunicatingMap.get( r.getFrom() ) );
	}

	@Override
	public void checkAllDoneMessages( Agent leader ) {
		while ( !leader.doneList.isEmpty() ) {
			Done d = leader.doneList.remove( 0 );
			Agent from = d.getFrom();
			Subtask st = d.getSt();
			removeAllocationHistory( from, st );

			int bindingTime = getCurrentTime() - timeToStartCommunicatingMap.get( from );
			renewCongestionDegreeMap( from, st, bindingTime );

			renewDE( dependabilityRanking, from, 1 );
			exceptions.remove( from );

			// タスク全体が終わったかどうかの判定と，それによる処理
			// HACK: もうちょいどうにかならんか
			Task task = leader.findTaskContaining( st );

			task.subtasks.remove( st );

			if ( task.subtasks.isEmpty() ) {
				from.pastTasks.remove( task );
				TaskManager.finishTask( leader );
				from.didTasksAsLeader++;
			}
		}
	}

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}


	private void renewCongestionDegreeMap( Agent target, Subtask st, int bindingTime ) {
		double[] tempArray = new double[ Agent.resource_types_ ];
		int resourceType = st.reqResType;

		if ( OstensibleCapacity.alreadyExists( target, ostensibleCapacityList ) ) {
			double newOC = calculateOC( bindingTime, roundTripTimeMap.get( target ), st );
			OstensibleCapacity.updateOC( target, ostensibleCapacityList, resourceType, newOC );
		} else {
			tempArray[ resourceType ] = calculateOC( bindingTime, roundTripTimeMap.get( target ), st );
			ostensibleCapacityList.add( new OstensibleCapacity( target, tempArray, getCurrentTime() ) );
		}
	}

	private double calculateOC( int bindingTime, int roundTripTime, Subtask subtask ) {
		int difficulty = subtask.reqRes[ subtask.reqResType ];
		return difficulty / ( bindingTime - 2.0 * roundTripTime );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( ", exceptions: " + exceptions.size() );
		sb.append( ", ocList: " + ostensibleCapacityList );
		return sb.toString();
	}
}

