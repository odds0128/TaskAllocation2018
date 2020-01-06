package main.research.agent.strategy.reciprocal_agents;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.agent.strategy.LeaderTemplateStrategy;
import main.research.communication.message.Done;
import main.research.communication.message.Solicitation;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.TaskManager.disposeTask;


public class LeaderStrategy extends LeaderTemplateStrategy {
	public static final double de_threshold_ = 0.7;
	public Principle principle = Principle.RATIONAL;

	@Override
	protected void solicitAsL( Agent leader ) {
		myTask = TaskManager.popTask();
		boolean canGoNext = false;

		if ( myTask != null ) {
			List< Allocation > allocationMap = makePreAllocationMap( leader, myTask.subtasks );
			repliesToCome = allocationMap.size();

			if ( !allocationMap.isEmpty() ) {
				if ( allocationMap.size() < myTask.subtasks.size() * REDUNDANT_SOLICITATION_TIMES ) {
					principle = Principle.RECIPROCAL;
				} else {
					principle = Principle.RATIONAL;
				}

				sendSolicitations( leader, allocationMap );
				canGoNext = true;
			} else {
				disposeTask(leader);
			}
		}
		nextPhase( leader, canGoNext );  // 次のフェイズへ
	}

	@Override
	protected List< Allocation > makePreAllocationMap( Agent self, List< Subtask > subtasks ) {
		List< Agent > reliableMembers = selectReliableMembersFrom( dependabilityRanking );
		List< Allocation > preAllocationList = allocatePreferentially( self, reliableMembers, subtasks );
		List< Subtask > unallocatedSubtasks = getUnallocatedSubtasks( subtasks, preAllocationList );

		List< Allocation > redundantlyAllocations = allocateRedundantlyFor( self, reliableMembers, unallocatedSubtasks );
		if ( redundantlyAllocations.isEmpty() ) return Collections.emptyList();
		assert ! redundantlyAllocations.isEmpty() : "割り当ての可能性がないサブタスクがあるよ";
		preAllocationList.addAll( redundantlyAllocations );
		return preAllocationList;
	}

	private List< Agent > selectReliableMembersFrom( List< Dependability > dependabilityRanking ) {
		List< Agent > ret = new ArrayList<>();
		double prevDE = 10000;

		for ( Dependability d: dependabilityRanking ) {
			assert prevDE >= d.getValue() : "Didn't be sorted rightly";
			if ( d.getValue() > de_threshold_ ) {
				ret.add( d.getAgent() );
				prevDE = d.getValue();
			} else break;
		}
		return ret;
	}

	// Allocation: agentとsubtaskのタプル的なデータクラス
	private List< Allocation > allocatePreferentially( Agent self, List< Agent > reliableAgents, List< Subtask > subtasks ) {
		if ( reliableAgents.isEmpty() ) {
			return new ArrayList<>(  );
		}

		List< Allocation > retAllocationList = new ArrayList<>();
		List< Agent > exceptions = new ArrayList<>( AgentManager.getAllAgentList() );
		exceptions.removeAll( reliableAgents );

		for ( Subtask st: subtasks ) {
			Agent candidate = this.selectMemberFor( exceptions, st );
			exceptions.add( candidate );
			retAllocationList.add( new Allocation( candidate, st ) );
		}
		return retAllocationList;
	}

	private List< Subtask > getUnallocatedSubtasks( List< Subtask > subtasks, List< Allocation > preAllocationList ) {
		if( preAllocationList.isEmpty() ) return subtasks;

		List< Subtask > temp = new ArrayList<>( subtasks );
		for ( Allocation toBeAllocated: preAllocationList ) {
			temp.remove( toBeAllocated.getSt() );
		}
		return temp;
	}


	private List< Allocation > allocateRedundantlyFor( Agent self, List< Agent > reliableAgents, List< Subtask > unallocatedSubtasks ) {
		List< Allocation > retAllocationList = new ArrayList<>();
		List< Agent > exceptions = reliableAgents;
		exceptions.add( self );

		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: unallocatedSubtasks ) {
				Agent candidate;
				if ( Agent.epsilonGreedy() ) {
					candidate = selectMemberForASubtaskRandomly( exceptions, st );
				} else candidate = this.selectMemberFor( exceptions, st );
				if ( candidate == null ) {
					return Collections.emptyList();
				}
				exceptions.add( candidate );
				retAllocationList.add( new Allocation( candidate, st ) );
			}
		}
		return retAllocationList;
	}

	@Override
	protected Agent selectMemberFor( List< Agent > exceptions, Subtask st ) {
		for ( Dependability pair: dependabilityRanking ) {
			Agent ag = pair.getAgent();
			if ( ( !exceptions.contains( ag ) ) && ag.canProcess( st ) ) return ag;
		}
		return null;
	}


	@Override
	public void sendSolicitations( Agent leader, List< Allocation > allocationList ) {
		for ( Allocation al: allocationList ) {
			sendMessage( new Solicitation( leader, al.getAg(), al.getSt() ) );
		}
	}

	@Override
	public void checkAllDoneMessages( Agent leader ) {
		while ( !leader.doneList.isEmpty() ) {
			Done d = leader.doneList.remove( 0 );
			Agent from = d.getFrom();
			Subtask st = d.getSt();
			removeAllocationHistory( from, st );

			renewDE( dependabilityRanking, from, 1 );

			// タスク全体が終わったかどうかの判定と，それによる処理
			// HACK: もうちょいどうにかならんか
			// 今終わったサブタスクasが含まれるtaskを見つける
			// それによってタスク全体が終われば終了報告等をする

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
		boolean b = evaluation > 0;

		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEby0or1( b );
	}
}
