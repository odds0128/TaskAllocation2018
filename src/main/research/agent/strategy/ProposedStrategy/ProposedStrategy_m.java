package main.research.agent.strategy.ProposedStrategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.agent.strategy.Strategy;
import java.util.List;


// TODO: 中身を表したクラス名にする
public class ProposedStrategy_m extends MemberStrategyWithRoleChange implements SetParam {

	@Override
	protected void renewDE( List<AgentDePair> pairList, Agent target, double evaluation ) {
		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}
}

