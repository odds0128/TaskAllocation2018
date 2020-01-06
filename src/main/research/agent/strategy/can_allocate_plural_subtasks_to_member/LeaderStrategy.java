package main.research.agent.strategy.can_allocate_plural_subtasks_to_member;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.strategy.OstensibleCapacity;
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
	private Map< Agent, Integer > timeToStartCommunicatingMap = new LinkedHashMap<>();
	private Map< Agent, Integer > roundTripTimeMap = new LinkedHashMap<>();
	private Map< Agent, Integer > extraWaitingTimeMap = new LinkedHashMap<>();
	private List< OstensibleCapacity > ostensibleCapacityList = new ArrayList<>();

	@Override
	protected void solicitAsL( Agent leader ) {
		myTask = TaskManager.popTask();
		boolean canGoNext = false;

		if ( myTask != null ) {
			List< Allocation > allocationMap = makePreAllocation( leader, myTask.subtasks );

			if ( allocationMap.size() < myTask.subtasks.size() * REDUNDANT_SOLICITATION_TIMES ) {
				principle = Principle.RECIPROCAL;
			} else {
				principle = Principle.RATIONAL;
			}
			repliesToCome = allocationMap.size();

			if ( !allocationMap.isEmpty() ) {
				canGoNext = true;
				sendSolicitations( leader, allocationMap );
			}else{
				disposeTask( leader );
			}
		}
		nextPhase( leader, canGoNext );  // 次のフェイズへ
	}

	private List< Allocation > makePreAllocation( Agent self, List< Subtask > subtasks ) {
		OstensibleCapacity.forgetOldOcInformation( ostensibleCapacityList );
		forgetOldRoundTripTimeInformation();

		List< Agent > reliableMembers = selectReliableMembersFrom( dependabilityRanking );
		List< Allocation > preAllocationList = allocatePreferentially( subtasks, reliableMembers );
		List< Subtask > unallocatedSubtasks = getUnallocatedSubtasks( subtasks, preAllocationList );

		preAllocationList.addAll( allocateRedundantlyFor( self, unallocatedSubtasks ) );
		return preAllocationList;
	}

	private List< Allocation > allocateRedundantlyFor( Agent self, List< Subtask > unallocatedSubtasks ) {
		List< Allocation > retAllocationList = new ArrayList<>();

		List<Agent> exceptions = new ArrayList<>( Arrays.asList( self ) );
		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: unallocatedSubtasks ) {
				Agent candidate;
				if ( Agent.epsilonGreedy() ) {
					candidate = selectMemberForASubtaskRandomly( exceptions, st );
				} else candidate = this.selectMemberFor( exceptions, st );
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
		List< Agent > ret = new ArrayList<>();
		for ( Dependability d: dependabilityRanking ) {
			if ( d.getValue() > de_threshold_ ) ret.add( d.getAgent() );
			else break;
		}
		return ret;
	}

	private List< Subtask > getUnallocatedSubtasks( List< Subtask > subtasks, List< Allocation > preAllocationList ) {
		List< Subtask > temp = new ArrayList<>( subtasks );
		for ( Allocation toBeAllocated: preAllocationList ) {
			temp.remove( toBeAllocated.getSt() );
		}
		return temp;
	}

	private List< Allocation > allocatePreferentially( List< Subtask > subtasks, List< Agent > reliableAgents ) {
		List< Allocation > retAllocationList = new ArrayList<>();

		for ( Subtask st: subtasks ) {
			for ( Agent relAg: reliableAgents ) {
				if ( isGoodAt( st, relAg ) ) {
					retAllocationList.add( new Allocation( relAg, st ) );
					break;
				}
			}
		}
		return retAllocationList;
	}

	private boolean isGoodAt( Subtask st, Agent reliableAgent ) {
		double temp = OstensibleCapacity.getOC( st.reqResType, reliableAgent, ostensibleCapacityList );
		return temp >= oc_threshold_;
	}

	@Override
	protected Agent selectMemberFor( List<Agent>exceptions, Subtask st ) {
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
		if ( leader.replyList.size() != repliesToCome ) {
			return;
		}

		assert repliesToCome == leader.replyList.size() : "Expected: " + repliesToCome + ", Actual: " + leader.resultList.size();

		Map< Subtask, Agent > mapOfSubtaskAndAgent = new LinkedHashMap<>();
		while ( !leader.replyList.isEmpty() ) {
			Reply r = leader.replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();

			int extraWaitingTime = calculateExtraWaitingTime( currentFrom );
			extraWaitingTimeMap.put( currentFrom, extraWaitingTime );

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

		if ( canExecuteTheTask( myTask, mapOfSubtaskAndAgent.keySet()) ) {
			for ( Map.Entry entry: mapOfSubtaskAndAgent.entrySet() ) {
				Agent friend = ( Agent ) entry.getValue();
				Subtask st = ( Subtask ) entry.getKey();

				sendMessage( new Result( leader, friend, SUCCESS, st ) );
				appendAllocationHistory( friend, st );
				if ( withinTimeWindow() ) leader.workWithAsL[ friend.id ]++;
			}
			leader.pastTasks.add( myTask );
			nextPhase( leader, true );
		} else {
			apologizeToFriends( leader, new ArrayList<>( mapOfSubtaskAndAgent.values() ) );

			disposeTask( leader );
			nextPhase( leader, false );
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

			// タスク全体が終わったかどうかの判定と，それによる処理
			Task task = leader.findTaskContaining( st );

			// remove
			try {
				task.subtasks.remove( st );
			} catch ( NullPointerException e ) {
				System.out.println( "Task: " + task );
				System.out.println( "Subtask: " + st );
				System.out.println( leader );
				System.out.println( from );
				System.out.println();
				System.exit( 0 );
			}

			if ( task.subtasks.isEmpty() ) {
				finishTask( leader, task );
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
		int resourceType = st.reqResType;

		if ( OstensibleCapacity.alreadyExists( target, ostensibleCapacityList ) ) {
			double newOC = calculateOC( bindingTime, target, st );
			double oldOC = OstensibleCapacity.getOC( resourceType, target, ostensibleCapacityList );
			// 最良の場合のみ記憶しておく
			if ( newOC >= oldOC ) OstensibleCapacity.updateOC( target, ostensibleCapacityList, resourceType, newOC );
		} else {
			tempArray[ resourceType ] = calculateOC( bindingTime, target, st );
			ostensibleCapacityList.add( new OstensibleCapacity( target, tempArray, getCurrentTime() ) );
		}
	}

	private double calculateOC( int bindingTime, Agent ag, Subtask subtask ) {
		int difficulty = subtask.reqRes[ subtask.reqResType ];
		return difficulty / ( bindingTime - ( 2.0 * roundTripTimeMap.get( ag ) + extraWaitingTimeMap.get( ag ) ) );
	}

	public void forgetOldRoundTripTimeInformation() {
		int size = roundTripTimeMap.size();
		for ( int i = 0; i < size; i++ ) {
			// OCの蒸発と同じタイミングで蒸発させる
			Map.Entry< Agent, Integer > entry = roundTripTimeMap.entrySet().iterator().next();
			if ( !OstensibleCapacity.alreadyExists( entry.getKey(), ostensibleCapacityList ) ) {
				roundTripTimeMap.remove( entry );
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( ", ocList: " + ostensibleCapacityList );
		return sb.toString();
	}


	public List< OstensibleCapacity > getOCList() {
		return ostensibleCapacityList;
	}

	public static List< OstensibleCapacity > getOcTupleList( LeaderStrategy psl ) {
		return psl.ostensibleCapacityList;
	}
}
