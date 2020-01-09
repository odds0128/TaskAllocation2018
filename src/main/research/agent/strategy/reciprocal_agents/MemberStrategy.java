package main.research.agent.strategy.reciprocal_agents;

import main.research.agent.Agent;
import main.research.agent.strategy.MemberTemplateStrategy;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Reply;
import main.research.communication.message.Solicitation;

import java.util.List;
import java.util.stream.Collectors;

import static main.research.Parameter.ReplyType.ACCEPT;
import static main.research.Parameter.ReplyType.DECLINE;


public class MemberStrategy extends MemberTemplateStrategy {
	public Principle principle = Principle.RATIONAL;
	private static double de_threshold_    = 0.7;
	private static int reliable_leaders_ = 4;

	@Override
	public void replyTo( List< Solicitation > solicitations, Agent member ) {
		solicitations.sort( ( solicitation1, solicitation2 ) ->
			compareSolicitations( solicitation1, solicitation2, dependabilityRanking ) );

		int capacity = Agent.subtask_queue_size_ - mySubtaskQueue.size() - expectedResultMessage;
		if( isReciprocal() ) {
			principle = Principle.RECIPROCAL;
			List<Agent> relLs = selectReliableLeaders();
			assert relLs.size() == reliable_leaders_: "Illegal reliable leaders number: " + relLs.size();

			replyReciprocallyTo( member, relLs, solicitations,capacity );
		} else {
			principle = Principle.RATIONAL;
			replyRationallyTo( member, solicitations, capacity );
		}
	}

	private void replyRationallyTo( Agent member, List< Solicitation > solicitations, int capacity ) {
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

	private void replyReciprocallyTo( Agent self, List< Agent > relLs, List< Solicitation > solicitationList, int capacity ) {
		while ( !solicitationList.isEmpty() && capacity-- > 0 ) {
			Solicitation s = solicitationList.remove( 0 );

			// 信頼エージェントからの要請のみ受理する
			if( relLs.contains( s.getFrom() ) ) {
				TransmissionPath.sendMessage( new Reply( self, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
				expectedResultMessage++;
			} else {
				TransmissionPath.sendMessage( new Reply( self, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
			}
		}
		while ( !solicitationList.isEmpty() ) {
			Solicitation s = solicitationList.remove( 0 );
			TransmissionPath.sendMessage( new Reply( self, s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
		}
	}

	private List< Agent > selectReliableLeaders() {
		return dependabilityRanking.subList( 0, reliable_leaders_ ).stream()
			.map( de -> de.getAgent() )
			.collect( Collectors.toList());
	}

	private boolean isReciprocal(  ) {
		// 信頼エージェント(DEが閾値以上のエージェント)がn体以上いたら互恵主義をとる．
		// dependabilityRankingはDEの降順に並んでいるので，インデックス(n-1)番，すなわちn体目のDEだけ確認すれば足りる．
		return dependabilityRanking.get( reliable_leaders_ - 1 ).getValue() > de_threshold_;
	}

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		boolean b = evaluation > 0;

		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEby0or1( b );
	}
}
