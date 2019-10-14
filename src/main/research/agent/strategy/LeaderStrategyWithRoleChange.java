package main.research.agent.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.AgentManager;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.ReplyType.DECLINE;
import static main.research.SetParam.ResultType.FAILURE;
import static main.research.SetParam.ResultType.SUCCESS;
import static main.research.communication.TransmissionPath.sendMessage;
import static main.research.task.TaskManager.disposeTask;

import java.util.*;

public abstract class LeaderStrategyWithRoleChange implements Strategy, SetParam {
	public static double γ_;
	public static double initial_de_, initial_el_, initial_em_;

	protected Set< Agent > exceptions = new HashSet<>();
	protected int repliesToCome = 0;            // 送ったsolicitationの数を覚えておく
	protected Task myTask;
	private Map< Agent, List< Subtask > > allocationHistory = new HashMap<>();
	public List< AgentDePair > reliableMembersRanking = new ArrayList<>(  );

	protected List< ReplyToSolicitation > replyList = new ArrayList<>();
	List< Done > doneList = new ArrayList<>(); // HACK: 可視性狭めたい

	public static void setConstants( JsonNode parameterNode ) {
		γ_ = parameterNode.get( "γ" ).asDouble();
		initial_de_ = parameterNode.get( "initial_de" ).asDouble();
		initial_el_ = parameterNode.get( "initial_el" ).asDouble();
		initial_em_ = parameterNode.get( "initial_em" ).asDouble();
	}

	// question: Leader has a LeaderStrategy のはずなので本来引数に「自分」を渡さなくてもいいはずでは？
	// TODO: Leaderクラスのインスタンスメソッドにする
	public void actAsLeader( Agent leader ) {
		while ( !leader.ms.solicitationList.isEmpty() ) {
			Solicitation s = leader.ms.solicitationList.remove( 0 );
			declineSolicitation( leader, s );
		}
		while ( !doneList.isEmpty() ) {
			Done d = doneList.remove( 0 );
			leader.ls.checkDoneMessage( leader, d );
		}

		Collections.sort( reliableMembersRanking, Comparator.comparingDouble( AgentDePair::getDe ).reversed() );

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
			TaskManager.finishTask(  );
			from.didTasksAsLeader++;
		}
	}

	private void solicitAsL( Agent leader ) {
		myTask = TaskManager.popTask( );
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

		for ( int i = 0; i < REDUNDANT_SOLICITATION_TIMES; i++ ) {
			for ( Subtask st: subtasks ) {
				if ( Agent.epsilonGreedy( ) ) candidate = selectMemberForASubtaskRandomly( st );
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
		for ( AgentDePair pair : reliableMembersRanking ) {
			Agent ag = pair.getTarget();
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

		Map< Subtask, Agent > SubtaskAgentMap = new HashMap<>();
		while ( !replyList.isEmpty() ) {
			ReplyToSolicitation r = replyList.remove( 0 );
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
				Agent   friend = ( Agent ) entry.getValue();
				Subtask st = (Subtask ) entry.getKey();

				sendMessage( new ResultOfTeamFormation( leader, friend, SUCCESS, st ) );
				appendAllocationHistory( friend, st );
				if ( withinTimeWindow() ) leader.workWithAsL[ friend.id ]++;
				leader.pastTasks.add( myTask );
			}
			leader.inactivate( 1 );
		} else {
			apologizeToFriends( leader, new ArrayList<>( SubtaskAgentMap.values() ) );
			exceptions.removeAll( new ArrayList<>( SubtaskAgentMap.values() ) );
			disposeTask();
			leader.inactivate( 0 );
		}
		myTask = null;
	}

	protected Pair compareDE( Agent a, Agent b ) {
		if( getPairByAgent( a, reliableMembersRanking ).getDe() < getPairByAgent( b, reliableMembersRanking ).getDe() ) return new Pair(b, a);
		return new Pair(a, b);
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
		for( Agent friend : friends ) sendMessage( new ResultOfTeamFormation( failingLeader, friend, FAILURE, null ) );
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

	public void setMemberRankingRandomly( Agent self, List< Agent > agentList ) {
		List< Agent > tempList = AgentManager.generateRandomAgentList( agentList );
		for ( Agent temp: tempList ) {
			if( temp.equals( self ) ) continue;
			reliableMembersRanking.add( new AgentDePair( temp, initial_de_ ) );
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

	protected abstract void renewDE( List<AgentDePair> pairList, Agent target, double evaluation );


}
