package main.research.agent.strategy.reliable_agents;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.strategy.OCTuple;
import main.research.agent.strategy.LeaderTemplateStrategy;
import main.research.communication.message.Done;
import main.research.communication.message.Reply;
import main.research.communication.message.Result;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;

import java.util.*;

import static main.research.Manager.getCurrentTime;
import static main.research.Parameter.ReplyType.DECLINE;
import static main.research.Parameter.ResultType.FAILURE;
import static main.research.Parameter.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.TaskManager.disposeTask;

public class LeaderStrategy extends LeaderTemplateStrategy implements Parameter {
	public static final double de_threshold_ = 0.7;
	static final double oc_threshold_ = 3.0;

	public Principle principle = Principle.RATIONAL;
	private Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	private Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	private Map< Agent, Integer > extraWaitingTimeMap = new HashMap<>();

	public List< OCTuple > getCdTupleList() {
		return ocTupleList;
	}

	private List< OCTuple > ocTupleList = new ArrayList<>();

	@Override
	protected void solicitAsL( Agent leader ) {
		myTask = TaskManager.popTask();
		boolean canGoNext = false;

		if ( myTask != null ) {
			List< Allocation > allocationMap = makePreAllocation( myTask.subtasks );
			if ( allocationMap.size() < myTask.subtasks.size() * REDUNDANT_SOLICITATION_TIMES ) {
				principle = Principle.RECIPROCAL;
			} else {
				principle = Principle.RATIONAL;
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

		List< Agent > reliableMembers = selectReliableMembersFrom( dependabilityRanking );
		List< Allocation > preAllocationList = allocatePreferentially( subtasks, reliableMembers );
		List< Subtask > unallocatedSubtasks = getUnallocatedSubtasks( subtasks, preAllocationList );

		preAllocationList.addAll( allocateRedundantlyFor( unallocatedSubtasks ) );
		return preAllocationList;
	}

	private List< Allocation > allocateRedundantlyFor( List< Subtask > unallocatedSubtasks ) {
		List< Allocation > retAllocationList = new ArrayList<>();

		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: unallocatedSubtasks ) {
				Agent candidate;
				if ( Agent.epsilonGreedy() ) candidate = selectMemberForASubtaskRandomly( st );
				else candidate = this.selectMemberFor( st );
				if ( candidate == null ) {
					return null;
				}
				exceptions.add( candidate );
				retAllocationList.add( new Allocation( candidate, st ) );
			}
		}
		return retAllocationList;
	}

	private List< Agent > selectReliableMembersFrom( List< Dependability > dependabilityRanking ) {
		List<Agent> ret = new ArrayList<>(  );
		for( Dependability d : dependabilityRanking ) {
			if( d.getValue() > de_threshold_ ) ret.add( d.getAgent() );
			else break;
		}
		return ret;
	}

	private List< Subtask > getUnallocatedSubtasks( List< Subtask > subtasks, List< Allocation > preAllocationList ) {
		List<Subtask> temp = new ArrayList<>( subtasks );
		for( Allocation toBeAllocated : preAllocationList ) {
			temp.remove( toBeAllocated.getSt() );
		}
		return temp;
	}

	private List< Allocation > allocatePreferentially( List< Subtask > subtasks, List<Agent> reliableAgents ) {
		List<Allocation> retAllocationList = new ArrayList<>(  );

		for ( Subtask st: subtasks ) {
			for( Agent relAg : reliableAgents ) {
				if( relAg.canProcess( st ) ) {
					retAllocationList.add( new Allocation( relAg, st ) );
					break;
				}
			}
		}
		return retAllocationList;
	}

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
			Reply r = leader.replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();

			int extraWaitingTime = calculateExtraWaitingTime( currentFrom );
			extraWaitingTimeMap.put( currentFrom, extraWaitingTime );

			if ( r.getReplyType() == DECLINE ) treatBetrayer( currentFrom );
			else if ( allocationMap.containsKey( st ) ) {
				Agent rival = allocationMap.get( st );
				Pair winnerAndLoser = compareDE( currentFrom, rival );

				exceptions.remove( winnerAndLoser.getValue() );
				sendMessage( new Result( leader, ( Agent ) winnerAndLoser.getValue(), FAILURE, null ) );
				allocationMap.put( st, ( Agent ) winnerAndLoser.getKey() );
			} else {
				allocationMap.put( st, currentFrom );
			}
		}
		if ( canExecuteTheTask( myTask, allocationMap.keySet() ) ) {
			for ( Map.Entry entry: allocationMap.entrySet() ) {
				Agent friend = ( Agent ) entry.getValue();
				Subtask st = ( Subtask ) entry.getKey();

				sendMessage( new Result( leader, friend, SUCCESS, st ) );
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
			updateOstensibleCapacityMap( from, st, bindingTime );

			renewDE( dependabilityRanking, from, 1 );
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
