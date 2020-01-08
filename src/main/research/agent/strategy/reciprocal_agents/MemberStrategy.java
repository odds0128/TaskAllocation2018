package main.research.agent.strategy.reciprocal_agents;

import main.research.agent.Agent;
import main.research.agent.strategy.MemberTemplateStrategy;

import java.util.List;


public class MemberStrategy extends MemberTemplateStrategy {
	public Principle principle = Principle.RATIONAL;

	//todo : 互恵行動を入れよう！

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		boolean b = evaluation > 0;

		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEby0or1( b );
	}
}
