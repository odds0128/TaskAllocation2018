package main.research.agent.strategy;

import main.research.Parameter;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
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

	void evaporateDE( List< AgentDePair > pairList ) {
		for ( AgentDePair pair: pairList ) pair.evaporate();
	}

}
