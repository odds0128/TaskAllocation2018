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

public interface Strategy {
	int time_observing_team_formation_ = AgentManager.time_observing_team_formation_;

	static void proceedToNextPhase( Agent ag ) {
		switch ( ag.phase ) {
			case WAIT_FOR_SOLICITATION:
				ag.phase = WAIT_FOR_SUBTASK;
				break;
			case SOLICIT:
				ag.phase = FORM_TEAM;
				break;
			case WAIT_FOR_SUBTASK:
				ag.phase = EXECUTE_SUBTASK;
				break;
			case FORM_TEAM:
				ag.phase = SELECT_ROLE;
				ag.role = JONE_DOE;
				break;
			case EXECUTE_SUBTASK:
				ag.phase = ag.ms.mySubtaskQueue.isEmpty() ? SELECT_ROLE : EXECUTE_SUBTASK;
				ag.role = ag.ms.mySubtaskQueue.isEmpty() ? JONE_DOE : MEMBER;
				break;
		}
		ag.validatedTicks = getCurrentTime();
	}

	default boolean withinTimeWindow() {
		return Agent._coalition_check_end_time - getCurrentTime() < time_observing_team_formation_;
	}

	default void evaporateDE( List< AgentDePair > pairList ) {
		for ( AgentDePair pair: pairList ) pair.evaporate();
	}

	default AgentDePair getPairByAgent( Agent target, List< AgentDePair > pairList ) {
		for ( AgentDePair pair: pairList ) {
			if ( pair.getAgent().equals( target ) ) return pair;
		}
		assert false : "not to come";
		return null;
	}

}
