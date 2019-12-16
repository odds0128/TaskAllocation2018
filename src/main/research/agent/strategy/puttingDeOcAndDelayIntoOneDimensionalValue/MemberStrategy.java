package main.research.agent.strategy.puttingDeOcAndDelayIntoOneDimensionalValue;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.strategy.MemberTemplateStrategy;

import java.util.List;

import static main.research.agent.AgentDePair.getPairByAgent;


// TODO: 中身を表したクラス名にする
public class MemberStrategy extends MemberTemplateStrategy implements Parameter {

	@Override
	protected void renewDE( List<AgentDePair> pairList, Agent target, double evaluation ) {
		AgentDePair pair = getPairByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}
}

