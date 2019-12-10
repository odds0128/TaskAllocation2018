package main.research.agent.strategy.reliableAgents;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.OCTuple;
import main.research.agent.strategy.LeaderState;
import main.research.agent.strategy.Strategy;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;
import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;
import org.apache.commons.math3.util.Precision;

import javax.crypto.spec.PSource;
import java.util.*;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.ReplyType.DECLINE;
import static main.research.SetParam.ResultType.FAILURE;
import static main.research.SetParam.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.TaskManager.disposeTask;

public class LeaderStrategy extends LeaderState implements SetParam {
	static final double de_threshold_ = 0.5;
	static final double oc_threshold_ = 3.0;

	private Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	private Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	private Map< Agent, Integer > extraWaitingTimeMap = new HashMap<>();

	public List< OCTuple > getCdTupleList() {
		return ocTupleList;
	}

	private List< OCTuple > ocTupleList = new ArrayList<>();

	@Override
	protected void solicitAsL( Agent leader ) {
		myTask = TaskManager.popTask( );
		if ( myTask == null ) {
			leader.inactivate( 0 );
			return;
		}
		Map<Agent, List<Subtask>> allocationMap = selectMembersLocal( myTask.subtasks );
		if( allocationMap.size() < myTask.subtasks.size() * REDUNDANT_SOLICITATION_TIMES ) {
			leader.principle = Principle.RECIPROCAL;
		} else {
			leader.principle = Principle.RATIONAL;
		}
		repliesToCome = countRepliesToCome( allocationMap );

		if ( allocationMap.isEmpty() ) {
			leader.inactivate( 0 );
			return;
		} else {
			this.sendSolicitationsLocal( leader, allocationMap );
		}
		Strategy.proceedToNextPhase( leader );  // 次のフェイズへ
	}

	private int countRepliesToCome( Map< Agent, List< Subtask>> allocationMap ) {
		int temp = allocationMap.entrySet().stream()
			.mapToInt( entry -> entry.getValue().size() )
			.sum();
		return temp;
	}

	private Map< Agent, List<Subtask> > selectMembersLocal( List< Subtask > subtasks ) {
		OCTuple.forgetOldOcInformation( ocTupleList );
		forgetOldRoundTripTimeInformation();

		Map<Agent, List<Subtask> > allocationMap = new HashMap<>(  );
		List<Subtask> unassignedSubtasks;
		Agent candidate;

		unassignedSubtasks = allocateToRelAg( subtasks, allocationMap );
		exceptions.addAll( allocationMap.keySet() );
		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: unassignedSubtasks ) {
				if ( Agent.epsilonGreedy( ) ) candidate = selectMemberForASubtaskRandomly( st );
				else candidate = this.selectMemberArbitrary( st );
				if ( candidate == null ) {
					return new HashMap<>();
				}
				exceptions.add( candidate );
				List<Subtask> temp = new ArrayList<>(  );
				temp.add( st );
				allocationMap.put( candidate, temp );
			}
		}
		return allocationMap;
	}

	private List<Subtask> allocateToRelAg( List< Subtask > subtasks, Map<Agent, List<Subtask>> alMap ) {
		Map<Agent, List<Subtask>> map = new HashMap<>(  );
		List<Subtask> unassigned = new ArrayList<>(  );

		for( Subtask st : subtasks ) {
			boolean toBeAssigned = false;
			for( AgentDePair ag_de : reliableMembersRanking ) {
				if( ag_de.getDe() <= de_threshold_ ) break;

				Agent reliableAgent = ag_de.getAgent();
				if( !reliableAgent.canProcessTheSubtask( st ) || exceptions.contains( reliableAgent ) ) continue;

				double oc = OCTuple.getOC( st.resType, reliableAgent, getOcTupleList( this ) );
				if( oc > oc_threshold_ ) {
					if ( !map.containsKey( reliableAgent ) ) map.put( reliableAgent, new ArrayList<>() );
					map.get( reliableAgent ).add( st );
					toBeAssigned = true;
					break;
				} else continue;
			}
			if( toBeAssigned == false ) unassigned.add( st );
		}
		alMap.putAll( map );
		return unassigned;
	}

	private Agent selectMemberArbitrary( Subtask st ) {
		return  selectMemberAccordingToDE( st );
	}

	private Agent selectMemberAccordingToDE( Subtask st ) {
		for ( AgentDePair pair: reliableMembersRanking ) {
			Agent ag = pair.getAgent();
			if ( ( !exceptions.contains( ag ) ) && ag.canProcessTheSubtask( st ) ) return ag;
		}
		return null;
	}

	private void sendSolicitationsLocal( Agent leader, Map< Agent, List<Subtask> > agentSubtaskMap ) {
		for ( Map.Entry<Agent, List<Subtask> > ag_stList : agentSubtaskMap.entrySet() ) {
			Agent ag = ag_stList.getKey();
			timeToStartCommunicatingMap.put( ag, getCurrentTime() );
			for( Subtask st : ag_stList.getValue() ) {
				sendMessage( new Solicitation( leader, ag, st ) );
			}
		}
	}

	@Override
	public void formTeamAsL( Agent leader ) {
		if ( leader.replyList.size() < repliesToCome ) return;
		else repliesToCome = 0;

		Map< Subtask, Agent > allocationMap = new HashMap<>();
		while ( !leader.replyList.isEmpty() ) {
			ReplyToSolicitation r = leader.replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();

			int extraWaitingTime = calculateExtraWaitingTime( currentFrom );
			extraWaitingTimeMap.put( currentFrom, extraWaitingTime );

			if ( r.getReplyType() == DECLINE ) treatBetrayer( currentFrom );
			else if ( allocationMap.containsKey( st ) ) {
				Agent rival = allocationMap.get( st );
				Pair winnerAndLoser = compareDE( currentFrom, rival );

				exceptions.remove( winnerAndLoser.getValue() );
				sendMessage( new ResultOfTeamFormation( leader, ( Agent ) winnerAndLoser.getValue(), FAILURE, null ) );
				allocationMap.put( st, ( Agent ) winnerAndLoser.getKey() );
			} else {
				allocationMap.put( st, currentFrom );
			}
		}
		if ( canExecuteTheTask( myTask, allocationMap.keySet() ) ) {
			for ( Map.Entry entry: allocationMap.entrySet() ) {
				Agent friend = ( Agent ) entry.getValue();
				Subtask st = ( Subtask ) entry.getKey();

				sendMessage( new ResultOfTeamFormation( leader, friend, SUCCESS, st ) );
				appendAllocationHistory( friend, st );
				if ( withinTimeWindow() ) leader.workWithAsL[ friend.id ]++;
				leader.pastTasks.add( myTask );
			}
			leader.inactivate( 1 );
		} else {
			apologizeToFriends( leader, new ArrayList<>( allocationMap.values() ) );
			exceptions.removeAll( new ArrayList<>( allocationMap.values() ) );
			disposeTask();
			leader.inactivate( 0 );
		}
		myTask = null;
	}

	private int calculateExtraWaitingTime( Agent ag ) {
		return ( getCurrentTime() - timeToStartCommunicatingMap.get( ag ) ) - roundTripTimeMap.get( ag );
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
		updateOstensibleCapacityMap( from, st, bindingTime );

		renewDE( reliableMembersRanking, from, 1 );
		exceptions.remove( from );

		// タスク全体が終わったかどうかの判定と，それによる処理
		// HACK: もうちょいどうにかならんか
		Task task = leader.findTaskContainingThisSubtask( st );

		task.subtasks.remove( st );

		if ( task.subtasks.isEmpty() ) {
			TaskManager.finishTask();
			from.didTasksAsLeader++;
		}
	}

	@Override
	protected void renewDE( List< AgentDePair > pairList, Agent target, double evaluation ) {
		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}

	private void updateOstensibleCapacityMap( Agent target, Subtask st, int bindingTime ) {
		double[] tempArray = new double[ Agent.resource_types_ ];
		int resourceType = st.resType;

		if ( OCTuple.alreadyExists( target, ocTupleList ) ) {
			double newOC = calculateOC( bindingTime, target, st );
			double oldOC = OCTuple.getOC( resourceType, target, ocTupleList );
			// 最良の場合のみ記憶しておく
			if( newOC >= oldOC ) OCTuple.updateOC( target, ocTupleList, resourceType, newOC );
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

	public static List< OCTuple > getOcTupleList( LeaderStrategy psl ) {
		return psl.ocTupleList;
	}

	void clear() {
	}
}
