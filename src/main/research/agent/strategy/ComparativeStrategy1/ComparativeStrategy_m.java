package main.research.agent.strategy.ComparativeStrategy1;

import main.research.agent.Agent;
import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.agent.strategy.Strategy;

import java.util.Map;

public class ComparativeStrategy_m extends MemberStrategyWithRoleChange {

	@Override
	protected void renewDE( Map< Agent, Double > deMap, Agent target, double evaluation ) {
		double formerDE = deMap.get( target );
		boolean b = evaluation > 0;
		double newDE;

		newDE = renewDEby0or1( formerDE, b );
		deMap.put( target, newDE );
	}
}
