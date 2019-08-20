package main.research.agent.strategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.Solicitation;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.SetParam.DERenewalStrategy.withBinary;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.ReplyType.DECLINE;

import java.util.*;

public abstract class LeaderStrategy implements Strategy, SetParam {
	// TODO: Hash~ のサイズ勘案
	protected Set< Agent > exceptions = new HashSet<>();
	protected int repliesToCome = 0;            // 送ったsolicitationの数を覚えておく
	protected Task myTask;
	private Map< Agent, List< Subtask > > allocationHistory = new HashMap<>();
	public Map< Agent, Double > reliableMembersRanking = new LinkedHashMap<>( HASH_MAP_SIZE );

	protected List< ReplyToSolicitation > replyList = new ArrayList<>();
	public List< Done > doneList = new ArrayList<>(); // HACK: 可視性狭めたい

	// CONSIDER: Leader has a LeaderStrategy のはずなので本来引数に「自分」を渡さなくてもいいはずでは？
	public void actAsLeader( Agent leader ) {
		while ( !leader.ms.solicitationList.isEmpty() ) {
			Solicitation s = leader.ms.solicitationList.remove( 0 );
			declineSolicitation( leader, s );
		}
		while ( !doneList.isEmpty() ) {
			Done d = doneList.remove( 0 );
			checkDoneMessage( leader, d );
		}

		if ( leader.phase == SOLICIT ) solicitAsL( leader );
		else if ( leader.phase == FORM_TEAM ) formTeamAsL( leader );

		evaporateDE( reliableMembersRanking );
	}

	protected void declineSolicitation( Agent leader, Solicitation s ) {
		TransmissionPath.sendMessage( new ReplyToSolicitation( leader, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
	}

	public void checkDoneMessage( Agent leader, Done d ) {
		Agent from = d.getFrom();
		Subtask st = getAllocatedSubtask( d.getFrom() );

		renewDE( reliableMembersRanking, from, 1, withBinary );
		exceptions.remove( from );

		// タスク全体が終わったかどうかの判定と，それによる処理
		// HACK: もうちょいどうにかならんか
		// 今終わったサブタスクasが含まれるtaskを見つける
		// それによってタスク全体が終われば終了報告等をする

		Task task = leader.findTaskContainingThisSubtask( st );
		task.subtasks.remove( st );
		if ( task.subtasks.isEmpty() ) {
			System.out.println( "finish" );
			from.pastTasks.remove( task );
			Task.finishTask(  );
			from.didTasksAsLeader++;
		}
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

	abstract protected void solicitAsL( Agent la );

	abstract protected void formTeamAsL( Agent la );
}
