package main.research.agent.strategy.ProposedStrategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.agent.strategy.Strategy;

import java.util.Map;

// TODO: 中身を表したクラス名にする
public class ProposedStrategy_m extends MemberStrategyWithRoleChange implements SetParam {


	@Override
	protected void renewDE( Map< Agent, Double > deMap, Agent target, double evaluation ) {
		double formerDE = deMap.get( target );
		boolean b = evaluation > 0;
		double newDE;

		newDE = renewDEby0or1( formerDE, b );
		deMap.put( target, newDE );
	}
}

