package main.research.agent.strategy.puttingDeOcAndDelayIntoOneDimensionalValue;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.strategy.LeaderTemplateStrategy;
import main.research.agent.strategy.OCTuple;
import main.research.communication.message.Done;
import main.research.communication.message.Reply;
import main.research.communication.message.Result;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;
import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;
import org.apache.commons.math3.util.Precision;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static main.research.Manager.getCurrentTime;
import static main.research.Parameter.ReplyType.DECLINE;
import static main.research.Parameter.ResultType.FAILURE;
import static main.research.Parameter.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.TaskManager.disposeTask;

// TODO: 中身を表したクラス名にする
public class LeaderStrategy extends LeaderTemplateStrategy implements Parameter {
	// 評価指標 = αDE + βOC + γDelay
	static final double α = 1;
	static final double β = 0.3;
	static final double γ = 0.3;
	private static final double EVALUATION_THRESHOLD = 0.5;

	private Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	private Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	private Map< Agent, Integer > extraWaitingTimeMap = new HashMap<>();
	private List< OCTuple > ocTupleList = new ArrayList<>();

	@Override
	protected Agent selectMemberFor( Subtask st ) {
		for ( Dependability pair: dependabilityRanking ) {
			Agent ag = pair.getAgent();
			if ( ( !exceptions.contains( ag ) ) && ag.canProcess( st ) ) return ag;
		}
		return null;
	}


	@Override
	public void sendSolicitations( Agent leader, List< Allocation > allocationList ) {
		for ( Allocation al : allocationList ) {
			timeToStartCommunicatingMap.put( al.getAg(), getCurrentTime() );
			sendMessage( new Solicitation( leader, al.getAg(), al.getSt() ) );
		}
	}

	// todo: 混雑度を元にしたメンバー選定のロジックの実装
	@Override
	protected List< LeaderTemplateStrategy.Allocation > makePreAllocationMap( List< Subtask > subtasks ) {
		List<Allocation> preAllocationList = new ArrayList<>(  );

		Agent candidate;
		OCTuple.forgetOldOcInformation( ocTupleList );
		forgetOldRoundTripTimeInformation();

		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: subtasks ) {
				if ( Agent.epsilonGreedy() ) candidate = selectMemberForASubtaskRandomly( st );
				else candidate = this.selectMemberArbitrary( st );

				if ( candidate == null ) {
					return null;
				}

				exceptions.add( candidate );
				preAllocationList.add( new Allocation( candidate, st ) );
			}
		}
		return preAllocationList;
	}

	public static int nulls = 0;
	public static int notNulls = 0;

	private Agent selectMemberArbitrary( Subtask st ) {
		Agent candidate = selectMemberAccordingToEvaluation( st );
		if ( candidate == null ) {
			nulls++;
			candidate = selectMemberAccordingToDE( st );
		} else {
			notNulls++;
		}
		return candidate;
	}

	private Agent selectMemberAccordingToEvaluation( Subtask st ) {
		double maxEvaluation = 0;
		Agent returnAgent = null;

		for ( Dependability pair: dependabilityRanking ) {
			Agent tempAgent = pair.getAgent();
			if ( !tempAgent.canProcess( st ) ) break;
			if ( exceptions.contains( tempAgent ) ) continue;
			double tempEvaluation = calculateMemberEvaluation( tempAgent, st );
			if ( tempEvaluation > maxEvaluation ) {
				maxEvaluation = tempEvaluation;
				returnAgent = tempAgent;
			}
		}
		if ( maxEvaluation < EVALUATION_THRESHOLD ) return null;
		return returnAgent;
	}

	private double calculateMemberEvaluation( Agent target, Subtask st ) {
		if ( !OCTuple.alreadyExists( target, ocTupleList ) ) {
			return α * getDeByAgent( target, dependabilityRanking ).getValue();
		}

		double[] ocs = ocTupleList.stream()
			.mapToDouble( tuple -> tuple.getOCArray()[ st.resType ] )
			.toArray();
		double[] rtts = roundTripTimeMap.values().stream()
			.mapToDouble( value -> value )
			.toArray();

		double oc_standard_deviation = calculateStandardDeviation( ocs );
		double rtt_standard_deviation = calculateStandardDeviation( rtts );

		return α * getDeByAgent( target, dependabilityRanking ).getValue()
			+ β * ( OCTuple.calculateAverageOC( st.resType, ocTupleList ) - OCTuple.getOC( st.resType, target, ocTupleList ) ) / oc_standard_deviation
			+ γ * ( calculateAverageRoundTripTime() - roundTripTimeMap.get( target ) ) / rtt_standard_deviation;
	}

	public static Double calculateStandardDeviation( double[] values ) {
		SynchronizedSummaryStatistics stats = new SynchronizedSummaryStatistics();
		for ( double value: values ) stats.addValue( value );
		return Precision.round( stats.getStandardDeviation(), 2 );
	}


	private double calculateAverageRoundTripTime() {
		return roundTripTimeMap.values().stream()
			.mapToDouble( v -> v )
			.average()
			.getAsDouble();
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
			// TODO: 待たせた時間は別で保持する
			int extraWaitingTime = calculateExtraWaitingTime( currentFrom );
			extraWaitingTimeMap.put( currentFrom, extraWaitingTime );

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
				leader.pastTasks.add( myTask );
			}
			leader.phase = nextPhase( leader, true );
		} else {
			apologizeToFriends( leader, new ArrayList<>( mapOfSubtaskAndAgent.values() ) );
			exceptions.removeAll( new ArrayList<>( mapOfSubtaskAndAgent.values() ) );
			disposeTask(leader);
			leader.phase = nextPhase( leader, false );
		}
		myTask = null;
	}

	private int calculateExtraWaitingTime( Agent ag ) {
		return ( getCurrentTime() - timeToStartCommunicatingMap.get( ag ) ) - roundTripTimeMap.get( ag );
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
			Task task = leader.findTaskContainingThisSubtask( st );

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

	private void renewCongestionDegreeMap( Agent target, Subtask st, int bindingTime ) {
		double[] tempArray = new double[ Agent.resource_types_ ];
		int resourceType = st.resType;

		if ( OCTuple.alreadyExists( target, ocTupleList ) ) {
			double newOC = calculateOC( bindingTime, target, st );
			assert newOC > 0 : "illegal congestion degree";
			OCTuple.updateOC( target, ocTupleList, resourceType, newOC );
		} else {
			tempArray[ resourceType ] = calculateOC( bindingTime, target, st );
			ocTupleList.add( new OCTuple( target, tempArray, getCurrentTime() ) );
		}
	}

	private double calculateOC( int bindingTime, Agent ag, Subtask subtask ) {
		int difficulty = subtask.reqRes[ subtask.resType ];
		return difficulty / ( bindingTime - ( 2.0 * roundTripTimeMap.get( ag ) + extraWaitingTimeMap.get( ag ) ) );
	}

	public void forgetOldRoundTripTimeInformation() {
		int size = roundTripTimeMap.size();
		for ( int i = 0; i < size; i++ ) {
			// OCの蒸発と同じタイミングで蒸発させる
			Map.Entry< Agent, Integer > entry = roundTripTimeMap.entrySet().iterator().next();
			if ( !OCTuple.alreadyExists( entry.getKey(), ocTupleList ) ) {
				roundTripTimeMap.remove( entry );
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( ", exceptions: " + exceptions.size() );
		sb.append( ", ocList: " + ocTupleList );
		return sb.toString();
	}

	public static List< OCTuple > getCdSetList( LeaderStrategy psl ) {
		return psl.ocTupleList;
	}

}
