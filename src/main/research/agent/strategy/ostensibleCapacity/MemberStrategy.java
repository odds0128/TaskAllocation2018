package main.research.agent.strategy.ostensibleCapacity;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.MemberStrategyWithRoleChange;

import java.util.List;


// TODO: 中身を表したクラス名にする
public class MemberStrategy extends MemberStrategyWithRoleChange implements SetParam {

	@Override
	protected void renewDE( List<AgentDePair> pairList, Agent target, double evaluation ) {
		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}
}

