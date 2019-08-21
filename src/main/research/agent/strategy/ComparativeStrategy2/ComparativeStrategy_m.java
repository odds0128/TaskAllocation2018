package main.research.agent.strategy.ComparativeStrategy2;

import main.research.agent.Agent;
import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.agent.strategy.Strategy;

import java.util.Map;

public class ComparativeStrategy_m extends MemberStrategyWithRoleChange {
	@Override
	protected void renewDE( Map< Agent, Double > deMap, Agent target, double evaluation ) {
		double formerDE = deMap.get( target );
		double newDE;

		newDE = renewDEbyArbitraryReward( formerDE, evaluation );
		deMap.put( target, newDE );
		Strategy.sortReliabilityRanking( deMap );
	}
}
