package main.research.agent.strategy.learningOnlyTeamingSuccessRate;

import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.MemberTemplateStrategy;

import java.util.List;

import static main.research.agent.AgentDePair.getPairByAgent;

public class MemberStrategy extends MemberTemplateStrategy {

	@Override
	protected void renewDE( List< AgentDePair > pairList, Agent target, double evaluation ) {
		boolean b = evaluation > 0;

		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEby0or1( b );
	}
}
