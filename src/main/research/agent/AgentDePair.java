package main.research.agent;

import main.research.agent.strategy.LeaderStrategyWithRoleChange;

import java.util.*;

public class AgentDePair {
	private static final double γ  = LeaderStrategyWithRoleChange.γ_;
	private static final double α_ = Agent.α_;

	private Agent agent;
	private double de;

	public AgentDePair( Agent agent, double de ) {
		this.agent = agent;
		this.de = de;
	}

	public void evaporate() {
		double temp = de - γ;
		de = temp > 0 ? temp : 0;
	}

	public Agent getAgent() {
		return agent;
	}

	public double getDe() {
		return de;
	}

	public void renewDEby0or1( boolean isPositive ){
		double multiplier = isPositive ? 1 : 0;
		de = de * ( 1.0 - α_ ) + multiplier * α_;
	}

	public void renewDEbyArbitraryReward( double reward ){
		de = de * ( 1.0 - α_ ) + reward * α_;
	}

	public static double searchDEofAgent( Agent stray, List<AgentDePair> list ) {
		for( AgentDePair pair : list ) {
			if( pair.agent == stray ) return pair.de;
		}
		System.out.println(" something went wrong. ");
		return 0;
	}

	@Override
	public String toString() {
		return agent.toString();
	}
}
