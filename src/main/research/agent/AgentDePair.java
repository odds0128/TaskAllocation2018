package main.research.agent;

import static main.research.SetParam.α;
import static main.research.SetParam.γ_r;

import java.util.*;

public class AgentDePair {
	static double γ = γ_r;

	private Agent target;
	private double de;

	public AgentDePair( Agent target, double de ) {
		this.target = target;
		this.de = de;
	}

	public void evaporate() {
		double temp = de - γ;
		de = temp > 0 ? temp : 0;
	}

	public Agent getTarget() {
		return target;
	}

	public double getDe() {
		return de;
	}

	public void renewDEby0or1( boolean isPositive ){
		double multiplier = isPositive ? 1 : 0;
		de = de * ( 1.0 - α ) + multiplier * α;
	}

	public void renewDEbyArbitraryReward( double reward ){
		de = de * ( 1.0 - α ) + reward * α;
	}

	public static double searchDEofAgent( Agent stray, List<AgentDePair> list ) {
		for( AgentDePair pair : list ) {
			if( pair.target == stray ) return pair.de;
		}
		System.out.println(" something went wrong. ");
		return 0;
	}

	@Override
	public String toString() {
		return target.toString();
	}
}
