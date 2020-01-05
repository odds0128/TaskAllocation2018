package main.research.agent.strategy;

import main.research.OutPut;
import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Done;
import main.research.communication.message.Reply;
import main.research.communication.message.Result;
import main.research.communication.message.Solicitation;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;

import static main.research.Manager.bin_;
import static main.research.Manager.getCurrentTime;
import static main.research.Parameter.ReplyType.ACCEPT;
import static main.research.Parameter.ReplyType.DECLINE;
import static main.research.Parameter.ResultType.*;
import static main.research.Parameter.Role.JONE_DOE;
import static main.research.agent.Agent.can_change_role_;
import static main.research.agent.Agent.α_;

import java.util.*;

public abstract class MemberTemplateStrategy extends TemplateStrategy implements Parameter {
	public int subtaskExecution = 0;
	public List< Dependability > dependabilityRanking = new ArrayList<>();

	public int expectedResultMessage = 0;
	public List< SubtaskFrom > mySubtaskQueue = new ArrayList<>();  // consider Agentとsubtaskの順番逆のがよくね
	public int currentSubtaskProcessTime;
	public static int idleTime = 0;

	public void actAsMember( Agent member ) {
		assert mySubtaskQueue.size() <= Agent.subtask_queue_size_ : "Over weight " + mySubtaskQueue.size();

		member.ls.checkAllDoneMessages( member );

		/*
		if solicitationが来ている then reply
		if resultが来ている then サブタスクキューに入れる
		if サブタスクを持っている then その残り時間をデクリメントする
		 */
		// todo: if文いらんくね
		if( ! member.solicitationList.isEmpty() ) {
			member.ms.replyTo( member.solicitationList, member );
//			replyTo( member.solicitationList, member );
		}
		if( ! member.resultList.isEmpty() ) {
			member.ms.reactTo( member.resultList, member );
			Collections.sort( dependabilityRanking, Comparator.comparingDouble( Dependability::getValue ).reversed() );
		}
		if( ! mySubtaskQueue.isEmpty() ) {
			member.validatedTicks = getCurrentTime();
			currentSubtaskProcessTime--;

			assert currentSubtaskProcessTime >= 0 : member.id + " が虚を処理している";

			if( currentSubtaskProcessTime == 0 ) {
				member.ms.finishCurrentSubtask( member );
				updateRoleValue( member, 1 );
				if( ! mySubtaskQueue.isEmpty() ) {
					currentSubtaskProcessTime = calculateProcessTime( member, mySubtaskQueue.get( 0 ).getSubtask() );
				}else if( expectedResultMessage == 0 ) {
					inactivate( member );
				}

			}
		}
		evaporateAllDependability( dependabilityRanking );
	}

	private void finishCurrentSubtask(Agent member) {
		SubtaskFrom sf = mySubtaskQueue.remove( 0 );
		TransmissionPath.sendMessage( new Done( member, sf.getLeader(), sf.getSubtask() ) );
		subtaskExecution++;
	}

	public void replyTo( List< Solicitation > solicitations, Agent member ) {
		solicitations.sort( ( solicitation1, solicitation2 ) ->
			compareSolicitations( solicitation1, solicitation2, dependabilityRanking ) );

		int capacity = Agent.subtask_queue_size_ - mySubtaskQueue.size() - expectedResultMessage;
		while ( solicitations.size() > 0 && capacity-- > 0 ) {
			Solicitation s = Agent.epsilonGreedy() ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
			TransmissionPath.sendMessage( new Reply( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
			expectedResultMessage++;
		}
		while ( !solicitations.isEmpty() ) {
			Solicitation s = solicitations.remove( 0 );
			TransmissionPath.sendMessage( new Reply( member, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}

	public void reactTo( List< Result > resultList, Agent member ) {
		if ( mySubtaskQueue.isEmpty() && getCurrentTime() % bin_ == 0 ) idleTime++;
		expectedResultMessage -= resultList.size();

		while ( !resultList.isEmpty() ) {
			Result r = resultList.remove( 0 );
			member.ms.reactTo( r );
		}

		if( expectedResultMessage == 0 && mySubtaskQueue.isEmpty()) {
			updateRoleValue( member, 0 );
			inactivate( member );
		}
	}

	public void reactTo( Result r ) {
		if ( r.getResult() == SUCCESS ) {
			SubtaskFrom sf = new SubtaskFrom( r.getAllocatedSubtask(), r.getFrom() );
			if( mySubtaskQueue.isEmpty() ) {
				currentSubtaskProcessTime = calculateProcessTime( r.getTo(), sf.getSubtask() );
			}
			mySubtaskQueue.add( sf );
			renewDE( dependabilityRanking, r.getFrom(), 1 );
		} else {
			renewDE( dependabilityRanking, r.getFrom(), 0 );
		}
	}

	protected int compareSolicitations( Solicitation a, Solicitation b, List< Dependability > pairList ) {
		if ( getDeByAgent( a.getFrom(), pairList ).getValue() < getDeByAgent( b.getFrom(), pairList ).getValue() )
			return -1;
		else return 1;
	}

	public int calculateProcessTime( Agent a, Subtask st ) {
		int res = ( int ) Math.ceil( ( double ) st.reqRes[ st.reqResType ] / ( double ) a.resources[ st.reqResType ] );
		OutPut.sumExecutionTime( res );
		return res;
	}

	protected static Solicitation selectSolicitationRandomly( List< Solicitation > solicitations ) {
		int index = MyRandom.getRandomInt( 0, solicitations.size() - 1 );
		return solicitations.remove( index );
	}

	protected abstract void renewDE( List< Dependability > pairList, Agent target, double evaluation );

	private void updateRoleValue( Agent member, double value ) {
		if( can_change_role_ ) {
			member.e_member = member.e_member * ( 1.0 - α_ ) + α_ * value;
		}
	}

	final public void inactivate( Agent member ) {
		if( can_change_role_ ) {
			member.role = JONE_DOE;
		}
	}

	// todo: イニシャライザは別んとこやる？
	public void setLeaderRankingRandomly( Agent self, List< Agent > agentList ) {
		List< Agent > tempList = AgentManager.generateRandomAgentList( agentList );
		for ( Agent temp: tempList ) {
			dependabilityRanking.add( new Dependability( temp, Agent.initial_de_ ) );
		}
		dependabilityRanking.remove( self );
	}


	public class SubtaskFrom{
		Subtask st;
		Agent l;

		public SubtaskFrom( Subtask st, Agent l ) {
			this.st = st;
			this.l  = l;
		}

		Subtask getSubtask() {
			return st;
		}

		Agent getLeader() {
			return l;
		}
	}
}
