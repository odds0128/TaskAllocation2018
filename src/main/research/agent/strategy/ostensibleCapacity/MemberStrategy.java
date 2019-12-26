package main.research.agent.strategy.ostensibleCapacity;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.strategy.MemberTemplateStrategy;

import java.util.List;



// TODO: 中身を表したクラス名にする
public class MemberStrategy extends MemberTemplateStrategy implements Parameter {

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEbyArbitraryReward( evaluation );
	}
}

