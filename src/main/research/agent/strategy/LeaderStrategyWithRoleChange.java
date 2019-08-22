package main.research.agent.strategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.ReplyType.DECLINE;
import static main.research.SetParam.ResultType.FAILURE;
import static main.research.SetParam.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.Task.disposeTask;

import java.util.*;

public abstract class LeaderStrategyWithRoleChange implements Strategy, SetParam {
	// TODO: Hash~ のサイズ勘案
	protected Set< Agent > exceptions = new HashSet<>();
	protected int repliesToCome = 0;            // 送ったsolicitationの数を覚えておく
	protected Task myTask;
	private Map< Agent, List< Subtask > > allocationHistory = new HashMap<>();
	public Map< Agent, Double > reliableMembersRanking = new LinkedHashMap<>( HASH_MAP_SIZE );

	protected List< ReplyToSolicitation > replyList = new ArrayList<>();
	List< Done > doneList = new ArrayList<>(); // HACK: 可視性狭めたい

	// CONSIDER: Leader has a LeaderStrategy のはずなので本来引数に「自分」を渡さなくてもいいはずでは？
	public void actAsLeader( Agent leader ) {
		while ( !leader.ms.solicitationList.isEmpty() ) {
			Solicitation s = leader.ms.solicitationList.remove( 0 );
			declineSolicitation( leader, s );
		}
		while ( !doneList.isEmpty() ) {
			Done d = doneList.remove( 0 );
			leader.ls.checkDoneMessage( leader, d );
		}

		if ( leader.phase == SOLICIT ) solicitAsL( leader );
		else if ( leader.phase == FORM_TEAM ) leader.ls.formTeamAsL( leader );

		evaporateDE( reliableMembersRanking );
	}

	private void declineSolicitation( Agent leader, Solicitation s ) {
		sendMessage( new ReplyToSolicitation( leader, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
	}

	public void checkDoneMessage( Agent leader, Done d ) {
		Agent from = d.getFrom();
		Subtask st = getAllocatedSubtask( d.getFrom() );

		leader.ls.renewDE( reliableMembersRanking, from, 1 );
		exceptions.remove( from );

		// タスク全体が終わったかどうかの判定と，それによる処理
		// HACK: もうちょいどうにかならんか
		// 今終わったサブタスクasが含まれるtaskを見つける
		// それによってタスク全体が終われば終了報告等をする

		Task task = leader.findTaskContainingThisSubtask( st );
		task.subtasks.remove( st );
		if ( task.subtasks.isEmpty() ) {
			from.pastTasks.remove( task );
			Task.finishTask(  );
			from.didTasksAsLeader++;
		}
	}

	private void solicitAsL( Agent leader ) {
		myTask = Task.getTask( leader );
		if ( myTask == null ) {
			leader.inactivate( 0 );
			return;
		}
		Map<Agent, Subtask > allocationMap = selectMembers( myTask.subtasks );
		repliesToCome = allocationMap.size();

		if ( allocationMap.isEmpty() ) {
			leader.inactivate( 0 );
			return;
		} else {
			leader.ls.sendSolicitations( leader, allocationMap );
		}
		Strategy.proceedToNextPhase( leader );  // 次のフェイズへ
	}

	protected Map< Agent, Subtask > selectMembers( List< Subtask > subtasks ) {
		Map< Agent, Subtask > memberCandidates = new HashMap<>();
		Agent candidate;

		for ( int i = 0; i < REBUNDUNT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: subtasks ) {
				if ( MyRandom.epsilonGreedy( Agent.ε ) ) candidate = selectMemberForASubtaskRandomly( st );
				else candidate = selectAMemberForASubtask( st );

				if ( candidate == null ) return new HashMap< >() ;

				exceptions.add( candidate );
				memberCandidates.put( candidate, st );
			}
		}
		return memberCandidates;
	}

	protected Agent selectMemberForASubtaskRandomly( Subtask st ) {
		Agent candidate;
		do {
			candidate = AgentManager.getAgentRandomly( exceptions, AgentManager.getAllAgentList() );
		} while ( ! candidate.canProcessTheSubtask( st ) );
		return candidate;
	}

	private Agent selectAMemberForASubtask( Subtask st ) {
		for ( Agent ag: reliableMembersRanking.keySet() ) {
			if ( ( ! exceptions.contains( ag ) ) && ag.canProcessTheSubtask( st ) ) return ag;
		}
		return null;
	}

	public void sendSolicitations( Agent leader, Map< Agent, Subtask > agentSubtaskMap ) {
		for ( Map.Entry<Agent, Subtask> ag_st : agentSubtaskMap.entrySet() ) {
			sendMessage( new Solicitation( leader, ag_st.getKey(), ag_st.getValue() ) );
		}
	}

	public void formTeamAsL( Agent leader ) {
		if ( replyList.size() < repliesToCome ) return;
		else repliesToCome = 0;

		Map< Subtask, Agent > mapOfSubtaskAndAgent = new HashMap<>();
		while ( !replyList.isEmpty() ) {
			ReplyToSolicitation r = replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();

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
				Agent   friend = ( Agent ) entry.getValue();
				Subtask st = (Subtask ) entry.getKey();

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

	protected void treatBetrayer( Agent betrayer ) {
		exceptions.remove( betrayer );
		renewDE( reliableMembersRanking, betrayer, 0 );
	}

	protected Pair< Agent, Agent > compareDE( Agent first, Agent second ) {
		if ( reliableMembersRanking.get( first ) >= reliableMembersRanking.get( second ) ) return new Pair<>( first, second );
		return new Pair<>( second, first );
	}

	protected boolean canExecuteTheTask( Task task, Set< Subtask > subtaskSet ) {
		// TODO: あとでサイズだけ比較するようにする
		int actual = 0;
		for ( Subtask st: subtaskSet ) if ( task.isPartOfThisTask( st ) ) actual++;
		return task.subtasks.size() == actual;
	}

	protected void apologizeToFriends( Agent failingLeader, List< Agent > friends ) {
		for( Agent friend : friends ) sendMessage( new ResultOfTeamFormation( failingLeader, friend, FAILURE, null ) );
	}

	protected boolean withinTimeWindow() {
		return Agent._coalition_check_end_time - getCurrentTime() < COALITION_CHECK_SPAN;
	}


	protected void appendAllocationHistory( Agent member, Subtask s ) {
		if ( !allocationHistory.containsKey( member ) ) {
			List temp = new ArrayList();
			allocationHistory.put( member, temp );
		}
		allocationHistory.get( member ).add( s );
	}

	protected Subtask getAllocatedSubtask( Agent member ) {
		Subtask temp = allocationHistory.get( member ).remove( 0 );
		if ( allocationHistory.get( member ).isEmpty() ) allocationHistory.remove( member );
		return temp;
	}

	public void setMemberRankingRandomly( List< Agent > agentList ) {
		List< Agent > tempList = AgentManager.generateRandomAgentList( agentList );
		for ( Agent temp: tempList ) {
			reliableMembersRanking.put( temp, INITIAL_VALUE_OF_DE );
		}
	}

	public void addMyselfToExceptions( Agent self ) {
		exceptions.add( self );
	}

	public void removeMyselfFromRanking( Agent self ) {
		reliableMembersRanking.remove( self );
	}

	public void reachReply( ReplyToSolicitation r ) {
		this.replyList.add( r );
	}

	public void reachDone( Done d ) {
		this.doneList.add( d );
	}

	protected abstract void renewDE( Map< Agent, Double > deMap, Agent target, double evaluation );


}
