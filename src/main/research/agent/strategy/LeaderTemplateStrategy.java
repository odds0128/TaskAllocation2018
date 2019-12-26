package main.research.agent.strategy;

import main.research.Manager;
import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;

import static main.research.Parameter.Phase.*;
import static main.research.Parameter.ReplyType.DECLINE;
import static main.research.Parameter.ResultType.FAILURE;
import static main.research.Parameter.ResultType.SUCCESS;
import static main.research.agent.Agent.α_;
import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.TaskManager.disposeTask;

import java.util.*;

public abstract class LeaderTemplateStrategy extends TemplateStrategy implements Parameter {

	protected Set< Agent > exceptions = new HashSet<>();
	protected int repliesToCome = 0;            // 送ったsolicitationの数を覚えておく
	protected Task myTask;
	private List< Allocation > allocationHistory = new ArrayList<>();
	public List< Dependability > reliableMembersRanking = new ArrayList<>();

	// question: Leader has a LeaderStrategy のはずなので本来引数に「自分」を渡さなくてもいいはずでは？
	// TODO: Leaderクラスのインスタンスメソッドにする
	public void actAsLeader( Agent leader ) {
		preprocess( leader );

		Collections.sort( reliableMembersRanking, Comparator.comparingDouble( Dependability::getValue ).reversed() );

		if ( leader.phase == SOLICIT ) leader.ls.solicitAsL( leader );
		else if ( leader.phase == FORM_TEAM ) leader.ls.formTeamAsL( leader );

		evaporateAllDependability( reliableMembersRanking );
	}

	private void preprocess( Agent leader ) {
		declineAllSolicitations( leader );
		checkAllDoneMessages( leader );
	}

	private void declineAllSolicitations( Agent leader ) {
		while ( !leader.solicitationList.isEmpty() ) {
			Solicitation s = leader.solicitationList.remove( 0 );
			sendMessage( new ReplyToSolicitation( leader, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}

	protected abstract void checkAllDoneMessages( Agent to );

	protected void solicitAsL( Agent leader ) {
		myTask = TaskManager.popTask();
		boolean canGoNext = false;

		if ( myTask != null ) {
			List<Allocation> allocationMap = makePreAllocationMap( myTask.subtasks );
			repliesToCome = allocationMap.size();

			if ( !allocationMap.isEmpty() ) {
				sendSolicitations( leader, allocationMap );
				canGoNext = true;
			}
		}
		leader.phase = nextPhase( leader, canGoNext );  // 次のフェイズへ
	}

	protected List< Allocation > makePreAllocationMap( List< Subtask > subtasks ) {
		List<Allocation> preAllocationList = new ArrayList<>(  );
		Agent candidate;

		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: subtasks ) {
				if ( Agent.epsilonGreedy() ) candidate = selectMemberForASubtaskRandomly( st );
				else candidate = selectAMemberForASubtask( st );

				if ( candidate == null ) return null;

				exceptions.add( candidate );
				preAllocationList.add( new Allocation( candidate, st ) );
			}
		}
		return preAllocationList;
	}

	protected Agent selectMemberForASubtaskRandomly( Subtask st ) {
		Agent candidate;
		do {
			candidate = AgentManager.getAgentRandomly( exceptions, AgentManager.getAllAgentList() );
		} while ( !candidate.canProcessTheSubtask( st ) );
		return candidate;
	}


	protected abstract Agent selectAMemberForASubtask( Subtask st);

	protected abstract void sendSolicitations( Agent from, List<Allocation> allocationList );

//	public void sendSolicitations( Agent leader, Map< Agent, Subtask > agentSubtaskMap ) {
//		for ( Map.Entry< Agent, Subtask > ag_st: agentSubtaskMap.entrySet() ) {
//			sendMessage( new Solicitation( leader, ag_st.getKey(), ag_st.getValue() ) );
//		}
//	}
//
	public void formTeamAsL( Agent leader ) {
		if ( leader.replyList.size() < repliesToCome ) return;
		else repliesToCome = 0;

		Map< Subtask, Agent > SubtaskAgentMap = new HashMap<>();
		while ( !leader.replyList.isEmpty() ) {
			ReplyToSolicitation r = leader.replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();

			if ( r.getReplyType() == DECLINE ) treatBetrayer( currentFrom );
			else if ( SubtaskAgentMap.containsKey( st ) ) {
				Agent rival = SubtaskAgentMap.get( st );
				Pair winnerAndLoser = compareDE( currentFrom, rival );

				exceptions.remove( winnerAndLoser.getValue() );
				sendMessage( new ResultOfTeamFormation( leader, ( Agent ) winnerAndLoser.getValue(), FAILURE, null ) );
				SubtaskAgentMap.put( st, ( Agent ) winnerAndLoser.getKey() );
			} else {
				SubtaskAgentMap.put( st, currentFrom );
			}
		}
		if ( canExecuteTheTask( myTask, SubtaskAgentMap.keySet() ) ) {
			for ( Map.Entry entry: SubtaskAgentMap.entrySet() ) {
				Agent friend = ( Agent ) entry.getValue();
				Subtask st = ( Subtask ) entry.getKey();

				sendMessage( new ResultOfTeamFormation( leader, friend, SUCCESS, st ) );
				appendAllocationHistory( friend, st );
				if ( withinTimeWindow() ) leader.workWithAsL[ friend.id ]++;
				leader.pastTasks.add( myTask );
			}
			nextPhase( leader, true );
		} else {
			apologizeToFriends( leader, new ArrayList<>( SubtaskAgentMap.values() ) );
			exceptions.removeAll( new ArrayList<>( SubtaskAgentMap.values() ) );
			disposeTask( leader );
			nextPhase( leader, false );
		}
		myTask = null;
	}

	protected Pair compareDE( Agent a, Agent b ) {
		if ( getDeByAgent( a, reliableMembersRanking ).getValue() < getDeByAgent( b, reliableMembersRanking ).getValue() )
			return new Pair( b, a );
		return new Pair( a, b );
	}

	protected void treatBetrayer( Agent betrayer ) {
		exceptions.remove( betrayer );
		renewDE( reliableMembersRanking, betrayer, 0 );
	}

	protected boolean canExecuteTheTask( Task task, Set< Subtask > subtaskSet ) {
		// TODO: あとでサイズだけ比較するようにする
		int actual = 0;
		for ( Subtask st: subtaskSet ) if ( task.contains( st ) ) actual++;
		return task.subtasks.size() == actual;
	}

	protected void apologizeToFriends( Agent failingLeader, List< Agent > friends ) {
		for ( Agent friend: friends ) sendMessage( new ResultOfTeamFormation( failingLeader, friend, FAILURE, null ) );
	}


	protected void appendAllocationHistory( Agent member, Subtask s ) {
		allocationHistory.add( new Allocation( member, s ) );
	}

	protected int countReciprocalLeader() {
		return 0;
	}

	protected boolean removeAllocationHistory( Agent member, Subtask st ) {
		return allocationHistory.remove( new Allocation( member, st ) );
	}

	public void setMemberRankingRandomly( Agent self, List< Agent > agentList ) {
		List< Agent > tempList = AgentManager.generateRandomAgentList( agentList );
		for ( Agent temp: tempList ) {
			reliableMembersRanking.add( new Dependability( temp, Agent.initial_de_ ) );
		}
		reliableMembersRanking.remove( self );
	}

	public void addMyselfToExceptions( Agent self ) {
		exceptions.add( self );
	}

	protected abstract void renewDE( List< Dependability > pairList, Agent target, double evaluation );

	// todo: 削除
	public void reachReply( ReplyToSolicitation r ) {
		r.getTo().replyList.add( r );
	}

	@Override
	protected Phase nextPhase( Agent leader, boolean wasSuccess ) {
		leader.validatedTicks = Manager.getCurrentTime();

		if ( !wasSuccess ) {
			leader.role = inactivate( leader, 0 );
			return SELECT_ROLE;
		}
		switch ( leader.phase ) {
			case SOLICIT:
				return FORM_TEAM;
			case FORM_TEAM:
				leader.role = inactivate( leader, 1 );
			default:
				return SELECT_ROLE;
		}
	}

	@Override
	public Role inactivate( Agent leader, double value ) {
		leader.e_leader = leader.e_leader * ( 1.0 - α_ ) + α_ * value;
		return Role.JONE_DOE;
	}

	protected class Allocation {
		final Agent ag;
		final Subtask st;

		public Allocation( Agent ag, Subtask st ) {
			this.ag = ag;
			this.st = st;
		}

		public Agent getAg() {
			return ag;
		}

		public Subtask getSt() {
			return st;
		}

		@Override
		public boolean equals( Object o ) {
			if ( o == this ) {
				return true;
			}
			if ( !( o instanceof Allocation ) ) {
				return false;
			}
			Allocation d = ( Allocation ) o;
			return d.ag == ag && d.st == st;
		}

		@Override
		public int hashCode() {
			int result = 17;
			result = 31 * result + ag.id;
			result = 31 * result + st.getId();
			return result;
		}
	}
}
