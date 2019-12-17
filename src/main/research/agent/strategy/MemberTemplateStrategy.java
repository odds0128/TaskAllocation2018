package main.research.agent.strategy;

import main.research.Manager;
import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.AgentManager;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;

import static main.research.Manager.bin_;
import static main.research.Manager.getCurrentTime;
import static main.research.Parameter.Phase.*;
import static main.research.Parameter.ReplyType.ACCEPT;
import static main.research.Parameter.ReplyType.DECLINE;
import static main.research.Parameter.ResultType.*;
import static main.research.Parameter.Role.JONE_DOE;
import static main.research.agent.Agent.α_;
import static main.research.agent.AgentDePair.getPairByAgent;

import java.util.*;

public abstract class MemberTemplateStrategy extends TemplateStrategy implements Parameter {
	protected boolean joinFlag = false;
	public List< AgentDePair > reliableLeadersRanking = new ArrayList<>();

	public int expectedResultMessage = 0;
	public List< Pair< Agent, Subtask > > mySubtaskQueue = new ArrayList<>();  // consider Agentとsubtaskの順番逆のがよくね

	public static int idleTime = 0;

	public void actAsMember( Agent member ) {
		preprocess( member );

		if ( member.phase == WAIT_FOR_SOLICITATION ) replyAsM( member );
		else if ( member.phase == WAIT_FOR_SUBTASK ) receiveAsM( member );
		else if ( member.phase == EXECUTE_SUBTASK ) execute( member );
		evaporateDE( reliableLeadersRanking );
	}

	private void preprocess( Agent member ) {
		assert mySubtaskQueue.size() <= SUBTASK_QUEUE_SIZE : "Over weight " + mySubtaskQueue.size();

		member.ms.replyToSolicitations( member, member.solicitationList );

		if ( mySubtaskQueue.isEmpty() && getCurrentTime() % bin_ == 0 ) idleTime++;
		while ( !member.resultList.isEmpty() ) {
			ResultOfTeamFormation r = member.resultList.remove( 0 );

			expectedResultMessage--;
			member.ms.reactToResultMessage( r );
		}

		member.ls.checkAllDoneMessages( member );

		Collections.sort( reliableLeadersRanking, Comparator.comparingDouble( AgentDePair::getDe ).reversed() );
	}

	public void setLeaderRankingRandomly( Agent self, List< Agent > agentList ) {
		List< Agent > tempList = AgentManager.generateRandomAgentList( agentList );
		for ( Agent temp: tempList ) {
			reliableLeadersRanking.add( new AgentDePair( temp, Agent.initial_de_ ) );
		}
		reliableLeadersRanking.remove( self );
	}

	public void reactToResultMessage( ResultOfTeamFormation r ) {
		if ( r.getResult() == SUCCESS ) {
			Pair< Agent, Subtask > pair = new Pair<>( r.getFrom(), r.getAllocatedSubtask() );
			mySubtaskQueue.add( pair );
			r.getTo().ms.renewDE( reliableLeadersRanking, r.getFrom(), 1 );
		} else {
			r.getTo().ms.renewDE( reliableLeadersRanking, r.getFrom(), 0 );
		}
	}

	// remove
	public static int tired_of_waiting = 0;

	private void replyAsM( Agent member ) {
		if ( joinFlag ) {
			member.phase = this.nextPhase( member, true );
			joinFlag = false;
		} else if ( getCurrentTime() - member.validatedTicks > THRESHOLD_FOR_ROLE_RENEWAL ) {
			tired_of_waiting++;
			member.phase = nextPhase( member, false );
		}
	}

	public void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
		if ( solicitations.isEmpty() ) return;

		solicitations.sort( ( solicitation1, solicitation2 ) ->
			compareSolicitations( solicitation1, solicitation2, reliableLeadersRanking ) );

		int capacity = SUBTASK_QUEUE_SIZE - mySubtaskQueue.size() - expectedResultMessage;
		while ( solicitations.size() > 0 && capacity-- > 0 ) {
			Solicitation s = Agent.epsilonGreedy() ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
			expectedResultMessage++;
			joinFlag = true;
		}
		while ( !solicitations.isEmpty() ) {
			Solicitation s = solicitations.remove( 0 );
			TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}

	protected int compareSolicitations( Solicitation a, Solicitation b, List< AgentDePair > pairList ) {
		if ( getPairByAgent( a.getFrom(), pairList ).getDe() < getPairByAgent( b.getFrom(), pairList ).getDe() )
			return -1;
		else return 1;
	}

	private void receiveAsM( Agent member ) {
		if ( expectedResultMessage > 0 ) return;

		boolean canGoNext = false;
		// todo: ここに至るまでにmySubtaskQueueに放り込まれてるのはおかしくない？
		if ( !mySubtaskQueue.isEmpty() ) {
			Subtask currentSubtask = mySubtaskQueue.get( 0 ).getValue();
			Agent currentLeader = mySubtaskQueue.get( 0 ).getKey();

			member.allocated[ currentLeader.id ][ currentSubtask.resType ]++;
			member.processTime = Agent.calculateProcessTime( member, currentSubtask );
			canGoNext = true;
		}
		member.phase = nextPhase( member, canGoNext );
	}

	private void execute( Agent member ) {
		assert member.processTime >= 0 : "sabotage";
		if ( --member.processTime > 0 ) {
			member.validatedTicks = getCurrentTime();
			return;
		}
		Pair< Agent, Subtask > pair = mySubtaskQueue.remove( 0 );
		TransmissionPath.sendMessage( new Done( member, pair.getKey() ) );
		mySubtaskQueue.remove( pair.getValue() );
		member.phase = nextPhase( member, true );
	}

	protected static Solicitation selectSolicitationRandomly( List< Solicitation > solicitations ) {
		int index = MyRandom.getRandomInt( 0, solicitations.size() - 1 );
		return solicitations.remove( index );
	}


	protected abstract void renewDE( List< AgentDePair > pairList, Agent target, double evaluation );

	@Override
	protected Phase nextPhase( Agent member, boolean wasSuccess ) {
		member.validatedTicks = Manager.getCurrentTime();

		if ( !wasSuccess ) {
			member.role = inactivate( member, 0 );
			return SELECT_ROLE;
		}
		switch ( member.phase ) {
			case WAIT_FOR_SOLICITATION:
				return WAIT_FOR_SUBTASK;
			case WAIT_FOR_SUBTASK:
				return EXECUTE_SUBTASK;
			case EXECUTE_SUBTASK:
 				member.e_member = member.e_member * ( 1.0 - α_ ) + α_ * 1.0;
				if ( !mySubtaskQueue.isEmpty() ) {
					member.processTime = Agent.calculateProcessTime( member, mySubtaskQueue.get( 0 ).getValue() );
					return EXECUTE_SUBTASK;
				}
				else if ( expectedResultMessage > 0 ) {
					return WAIT_FOR_SUBTASK;
				} else {
					member.role = inactivate( member, 1 );
				}
			default:
				return SELECT_ROLE;
		}
	}

	@Override
	public Role inactivate( Agent member, double value ) {
		member.e_member = member.e_member * ( 1.0 - α_ ) + α_ * value;
		return JONE_DOE;
	}

}
