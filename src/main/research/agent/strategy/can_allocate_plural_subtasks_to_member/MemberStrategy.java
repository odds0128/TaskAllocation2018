package main.research.agent.strategy.can_allocate_plural_subtasks_to_member;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.agent.strategy.MemberTemplateStrategy;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Reply;
import main.research.communication.message.Solicitation;

import java.util.*;
import java.util.stream.Collectors;

import static main.research.Parameter.ReplyType.ACCEPT;
import static main.research.Parameter.ReplyType.DECLINE;


public class MemberStrategy extends MemberTemplateStrategy implements Parameter {
	public static final double threshold_of_reliable_leader = 0.7;

	public static int waiting = 0;
	public Principle principle;

	@Override
	public void replyTo( List< Solicitation > solicitations, Agent member ) {
		solicitations.sort( ( solicitation1, solicitation2 ) ->
			compareSolicitations( solicitation1, solicitation2, dependabilityRanking ) );

		if ( haveReliableLeader() ) {
			principle = Principle.RECIPROCAL;
			replyToReliableLeader( member, solicitations );
		} else {
			principle = Principle.RATIONAL;
			replyToOrdinaryLeaders( member, solicitations );
		}
	}

	// remove
	static int averageReliableLeadersNum = 0;
	private void replyToReliableLeader( Agent member, List< Solicitation > solicitations ) {
		int capacity = Agent.subtask_queue_size_ - mySubtaskQueue.size() - expectedResultMessage;
		// todo: 複数のリーダーを信頼するようにする

//		List<Agent> reliableLeaders = new ArrayList<>( Arrays.asList( dependabilityRanking.get( 0 ).getAgent() ) );
		List< Agent > reliableLeaders = dependabilityRanking.stream()
			.filter( dep -> dep.getValue() > threshold_of_reliable_leader )
			.map( dep -> dep.getAgent() )
			.collect( Collectors.toList() );

		for ( Agent targetRelL: reliableLeaders ) {
			List< Solicitation > solicitationsFromTargetRelL = solicitations.stream()
				.filter( s -> s.getFrom() == targetRelL )
				.collect( Collectors.toList() );

			boolean canDoAll = true;
			if( capacity < solicitationsFromTargetRelL.size() ) {
				canDoAll = false;
			} else {
				for ( Solicitation s: solicitationsFromTargetRelL ) {
					if ( !member.canProcess( s.getExpectedSubtask() ) ) {
						canDoAll = false;
						break;
					}
				}
			}
			if ( !canDoAll ) {
				declineAll( member, solicitationsFromTargetRelL );
			} else {
				for ( Solicitation s: solicitationsFromTargetRelL ) {
					accept( member, s );
				}
			}
			solicitations.removeAll( solicitationsFromTargetRelL );
		}
		declineAll( member, solicitations );
		solicitations.clear();
	}

	private boolean haveReliableLeader() {
//		return false;
		return dependabilityRanking.get( 0 ).getValue() >= threshold_of_reliable_leader;
	}

	private void accept( Agent member, Solicitation s ) {
		TransmissionPath.sendMessage( new Reply( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
		expectedResultMessage++;
	}

	private void declineAll( Agent member, List< Solicitation > solicitations ) {
		for( Solicitation s : solicitations ) {
			TransmissionPath.sendMessage( new Reply( member, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}

	private void replyToOrdinaryLeaders( Agent member, List< Solicitation > solicitationList ) {
		int capacity = Agent.subtask_queue_size_ - mySubtaskQueue.size() - expectedResultMessage;

		Map<Agent, List<Solicitation> > dealtSolicitations = dealSolicitations( solicitationList );

		if( solicitationList.size() > dealtSolicitations.size() ) {
			System.out.println( "all solicitations : \n" + solicitationList );
			System.out.println( " dealt solicitations : \n " + dealtSolicitations );
			System.out.println();
		}

		for( Map.Entry<Agent, List<Solicitation>> e : dealtSolicitations.entrySet() ) {
			boolean canDoAll = true;
			List<Solicitation> targetSolicitations = e.getValue();
			if ( capacity < targetSolicitations.size() ) {
				canDoAll = false;
			} else {
				for ( Solicitation s : targetSolicitations ) {
					if ( !member.canProcess( s.getExpectedSubtask() ) ) {
						canDoAll = false;
						break;
					}
				}
			}
			if ( !canDoAll ) {
				declineAll( member, targetSolicitations );
			} else {
				for ( Solicitation s: targetSolicitations ) {
					accept( member, s );
					capacity--;
				}
			}
		}
	}

	private Map< Agent, List< Solicitation > > dealSolicitations( List< Solicitation > solicitationList ) {
		Map<Agent, List<Solicitation> > ret = solicitationList.stream()
			.collect( Collectors.groupingBy( Solicitation::getFrom ) );
		return ret;
	}

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}

	public static double calculateMeanDE() {
		List< Agent > members = AgentManager.getAllAgentList().stream()
			.filter( ag -> ag.e_member > ag.e_leader )
			.collect( Collectors.toList() );
		new ArrayList<>();

		double sum = 0;
		int nonZeroLeaders = 0;

		for ( Agent m: members ) {
			List< Dependability > validPairsList = m.ms.dependabilityRanking.stream()
				.filter( pair -> pair.getValue() > 0 )
				.collect( Collectors.toList() );
			nonZeroLeaders += validPairsList.stream()
				.count();
			sum += validPairsList.stream()
				.mapToDouble( pair -> pair.getValue() )
				.sum();
		}
		return sum / nonZeroLeaders;
	}

	public static int countReciprocalMembers() {
		return ( int ) AgentManager.getAllAgentList().stream()
			.filter( agent -> agent.e_member > agent.e_leader )
			.filter( member -> member.ms.dependabilityRanking.get( 0 ).getValue() > threshold_of_reliable_leader )
			.count();
	}
}
