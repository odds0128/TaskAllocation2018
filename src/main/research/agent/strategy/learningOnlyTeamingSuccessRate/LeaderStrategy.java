package main.research.agent.strategy.learningOnlyTeamingSuccessRate;

import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.LeaderState;

import java.util.List;

public class LeaderStrategy extends LeaderState {

	@Override
	protected void renewDE( List< AgentDePair > pairList, Agent target, double evaluation ) {
		boolean b = evaluation > 0;

		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEby0or1( b );
	}
}
