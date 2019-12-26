package main.research.agent.strategy;

import main.research.agent.Agent;
import main.research.agent.AgentManager;

import java.util.*;

import static main.research.Manager.getCurrentTime;
import static main.research.Parameter.*;

public abstract class TemplateStrategy {
	int time_observing_team_formation_ = AgentManager.time_observing_team_formation_;

	protected abstract Phase nextPhase( Agent ag, boolean wasSuccess );
	protected abstract Role inactivate( Agent ag, double value );

	protected boolean withinTimeWindow() {
		return Agent._coalition_check_end_time - getCurrentTime() < time_observing_team_formation_;
	}

	void evaporateAllDependability( List< Dependability > deList ) {
		for ( Dependability de: deList ) de.evaporate();
	}

	public Dependability getDeByAgent( Agent target, List< Dependability > deList ) {
		for ( Dependability de: deList ) {
			if ( de.getAgent().equals( target ) ) return de;
		}
		assert false : "not to come";
		return null;
	}

	public class Dependability {
		private final double γ  = Agent.γ_;
		private final double α_ = Agent.α_;

		private Agent agent;
		private double value;

		public Dependability( Agent agent, double value ) {
			this.agent = agent;
			this.value = value;
		}

		public void evaporate() {
			double temp = value - γ;
			value = temp > 0 ? temp : 0;
		}

		public Agent getAgent() {
			return agent;
		}

		public double getValue() {
			return value;
		}

		public void renewDEby0or1( boolean isPositive ){
			double multiplier = isPositive ? 1 : 0;
			value = value * ( 1.0 - α_ ) + multiplier * α_;
		}

		public void renewDEbyArbitraryReward( double reward ){
			value = value * ( 1.0 - α_ ) + reward * α_;
		}

		@Override
		public String toString() {
			return agent.toString();
		}
	}
}
