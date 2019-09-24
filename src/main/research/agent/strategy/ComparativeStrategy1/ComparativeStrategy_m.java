package main.research.agent.strategy.ComparativeStrategy1;

import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.agent.strategy.Strategy;
import java.util.List;
import java.util.Map;

public class ComparativeStrategy_m extends MemberStrategyWithRoleChange {

	@Override
	protected void renewDE( List< AgentDePair > pairList, Agent target, double evaluation ) {
		boolean b = evaluation > 0;

		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEby0or1( b );
	}
}
