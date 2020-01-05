package main.research.agent.strategy.success_rate;

import main.research.agent.Agent;
import main.research.agent.strategy.MemberTemplateStrategy;

import java.util.List;



public class MemberStrategy extends MemberTemplateStrategy {

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		boolean b = evaluation > 0;

		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEby0or1( b );
	}
}
