package main.research.agent.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.AgentManager;

import java.util.*;
import java.util.Map.Entry;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.Role.JONE_DOE;
import static main.research.SetParam.Role.MEMBER;

public abstract class Strategy {
	int time_observing_team_formation_ = AgentManager.time_observing_team_formation_;

	protected abstract void nextPhase(Agent ag);

	protected boolean withinTimeWindow() {
		return Agent._coalition_check_end_time - getCurrentTime() < time_observing_team_formation_;
	}

	void evaporateDE( List< AgentDePair > pairList ) {
		for ( AgentDePair pair: pairList ) pair.evaporate();
	}

	protected AgentDePair getPairByAgent( Agent target, List< AgentDePair > pairList ) {
		for ( AgentDePair pair: pairList ) {
			if ( pair.getAgent().equals( target ) ) return pair;
		}
		assert false : "not to come";
		return null;
	}

}
