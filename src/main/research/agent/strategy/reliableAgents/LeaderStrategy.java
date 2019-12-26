package main.research.agent.strategy.reliableAgents;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.strategy.OCTuple;
import main.research.agent.strategy.LeaderTemplateStrategy;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;
import org.apache.xmlbeans.impl.xb.xsdschema.All;

import java.util.*;

import static main.research.Manager.getCurrentTime;
import static main.research.Parameter.ReplyType.DECLINE;
import static main.research.Parameter.ResultType.FAILURE;
import static main.research.Parameter.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.TaskManager.disposeTask;

public class LeaderStrategy extends LeaderTemplateStrategy implements Parameter {
	public static final double de_threshold_ = 0.5;
	static final double oc_threshold_ = 3.0;

	private Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	private Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	private Map< Agent, Integer > extraWaitingTimeMap = new HashMap<>();

	public List< OCTuple > getCdTupleList() {
		return ocTupleList;
	}

	private List< OCTuple > ocTupleList = new ArrayList<>();

	// todo: 削除
	@Override
	protected Agent selectAMemberForASubtask( Subtask st ) {
		for ( Dependability pair: reliableMembersRanking ) {
			Agent ag = pair.getAgent();
			if ( ( !exceptions.contains( ag ) ) && ag.canProcessTheSubtask( st ) ) return ag;
		}
		return null;
	}

	@Override
	protected void solicitAsL( Agent leader ) {
		myTask = TaskManager.popTask();
		boolean canGoNext = false;

		if ( myTask != null ) {
			List< Allocation > allocationMap = makePreAllocation( myTask.subtasks );
			if ( allocationMap.size() < myTask.subtasks.size() * REDUNDANT_SOLICITATION_TIMES ) {
				leader.principle = Principle.RECIPROCAL;
			} else {
				leader.principle = Principle.RATIONAL;
			}
			repliesToCome = allocationMap.size();
			if ( !allocationMap.isEmpty() ) {
				canGoNext = true;
				sendSolicitations( leader, allocationMap );
			}
		}
		leader.phase = nextPhase( leader, canGoNext );  // 次のフェイズへ
	}

	private List< Allocation > makePreAllocation( List< Subtask > subtasks ) {
		OCTuple.forgetOldOcInformation( ocTupleList );
		forgetOldRoundTripTimeInformation();

		Agent candidate;

		List< Allocation > preAllocationList = allocateToRelAg( subtasks );
		List< Subtask > unallocatedSubtasks = getUnallocatedSubtasks(subtasks, preAllocationList);
		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: unallocatedSubtasks ) {
				if ( Agent.epsilonGreedy() ) candidate = selectMemberForASubtaskRandomly( st );
				else candidate = this.selectMemberAccordingToDE( st );
				if ( candidate == null ) {
					return null;
				}
				exceptions.add( candidate );
				preAllocationList.add( new Allocation( candidate, st ) );
			}
		}
		return preAllocationList;
	}

	private List< Subtask > getUnallocatedSubtasks( List< Subtask > subtasks, List< Allocation > preAllocationList ) {
		List<Subtask> temp = new ArrayList<>( subtasks );
		for( Allocation toBeAllocated : preAllocationList ) {
			temp.remove( toBeAllocated.getSt() );
		}
		return temp;
	}

	private List< Allocation > allocateToRelAg( List< Subtask > subtasks ) {
		List<Allocation> retAllocationList = new ArrayList<>(  );

		for ( Subtask st: subtasks ) {
			for ( Dependability ag_de: reliableMembersRanking ) {
				if ( ag_de.getValue() <= de_threshold_ ) break;

				Agent reliableAgent = ag_de.getAgent();
				// todo: exceptionsに含めないようにする
				if ( !reliableAgent.canProcessTheSubtask( st ) || exceptions.contains( reliableAgent ) ) continue;

				double oc = OCTuple.getOC( st.resType, reliableAgent, getOcTupleList( this ) );
				if ( oc > oc_threshold_ ) {
					retAllocationList.add( new Allocation( reliableAgent, st ) );
					break;
				}
			}
		}
		return retAllocationList;
	}

	private Agent selectMemberAccordingToDE( Subtask st ) {
		for ( Dependability pair: reliableMembersRanking ) {
			Agent ag = pair.getAgent();
			if ( ( !exceptions.contains( ag ) ) && ag.canProcessTheSubtask( st ) ) return ag;
		}
		return null;
	}

	@Override
	public void sendSolicitations( Agent leader, List< Allocation > allocationList ) {
		for ( Allocation al: allocationList ) {
			timeToStartCommunicatingMap.put( al.getAg(), getCurrentTime() );
			sendMessage( new Solicitation( leader, al.getAg(), al.getSt() ) );
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
			leader.phase = nextPhase( leader, true );
		} else {
			apologizeToFriends( leader, new ArrayList<>( allocationMap.values() ) );
			exceptions.removeAll( new ArrayList<>( allocationMap.values() ) );
			disposeTask( leader );
			leader.phase = nextPhase( leader, false );
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
	public void checkAllDoneMessages( Agent leader ) {
		while ( !leader.doneList.isEmpty() ) {
			Done d = leader.doneList.remove( 0 );

			Agent from = d.getFrom();
			Subtask st = d.getSt();
			removeAllocationHistory( from, st );

			int bindingTime = getCurrentTime() - timeToStartCommunicatingMap.get( from );
			updateOstensibleCapacityMap( from, st, bindingTime );

			renewDE( reliableMembersRanking, from, 1 );
			exceptions.remove( from );

			// タスク全体が終わったかどうかの判定と，それによる処理
			// HACK: もうちょいどうにかならんか
			Task task = leader.findTaskContainingThisSubtask( st );

			task.subtasks.remove( st );

			if ( task.subtasks.isEmpty() ) {
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

	private void updateOstensibleCapacityMap( Agent target, Subtask st, int bindingTime ) {
		double[] tempArray = new double[ Agent.resource_types_ ];
		int resourceType = st.resType;

		if ( OCTuple.alreadyExists( target, ocTupleList ) ) {
			double newOC = calculateOC( bindingTime, target, st );
			double oldOC = OCTuple.getOC( resourceType, target, ocTupleList );
			// 最良の場合のみ記憶しておく
			if ( newOC >= oldOC ) OCTuple.updateOC( target, ocTupleList, resourceType, newOC );
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
}
