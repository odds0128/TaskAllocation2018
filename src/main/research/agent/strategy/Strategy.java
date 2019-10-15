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
	double α_ = Agent.α_;
	double γ_ = LeaderStrategyWithRoleChange.γ_;
	int agent_num_ = AgentManager.agent_num_;
	int time_observing_team_formation_ = AgentManager.time_observing_team_formation_;

	default double renewDEby0or1( double former, boolean isPositive ) {
		double multiplier = isPositive ? 1 : 0;
		return former * ( 1.0 - α_ ) + multiplier * α_;
	}

	default double renewDEbyArbitraryReward( double former, double reward ) {
		return former * ( 1.0 - α_ ) + reward * α_;
	}

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

	default void evaporateDE( Map< Agent, Double > relMap ) {
		// CONSIDER: そもそも特定のエージェントのDEのみ保持するようにすればいいのでは？
//        arrayEvaporate(relMap);
//         mapEvaporate(relMap);
	}

	default boolean withinTimeWindow() {
		return Agent._coalition_check_end_time - getCurrentTime() < time_observing_team_formation_;
	}

	default void evaporateDE( List< AgentDePair > pairList ) {
		for ( AgentDePair pair: pairList ) pair.evaporate();
	}


	// CONSIDER: Mapがでかいとどちゃくそ遅い．Mapのせいというよりラッパークラスのせいか．こっちの方が記述は簡潔なんだけどなぁ．
	default void mapEvaporate( Map< Agent, Double > relMap ) {
		relMap.forEach(
			( key, value ) -> {
				double temp = value > γ_ ? value - γ_ : 0;
				relMap.replace( key, temp );
			}
		);
	}

	default void arrayEvaporate( Map< Agent, Double > relMap ) {
		double[] DEs = mapToArray( relMap );
		int zero_index = zeroDEfromHere( DEs );
		evaporateSpecifiedRangeDE( relMap, zero_index );
	}

	default double[] mapToArray( Map< Agent, Double > relMap ) {
		double[] array = new double[ agent_num_ - 1 ];
		int index = 0;
		for ( Entry< Agent, Double > e: relMap.entrySet() ) {
			array[ index++ ] = e.getValue().doubleValue();
		}
		return array;
	}

	default int zeroDEfromHere( double[] DEs ) {
		int size = agent_num_ - 1;
		for ( int i = 0; i < size; i++ ) {
			if ( DEs[ i ] == 0 ) return i;
		}
		return size;
	}

	default void evaporateSpecifiedRangeDE( Map< Agent, Double > relMap, int end_index ) {
		Iterator keyIterator = relMap.keySet().iterator();
		Iterator valueIterator = relMap.values().iterator();
		Agent key;
		double value;
		double temp;
		int i = 0;

		while ( keyIterator.hasNext() && i++ < end_index ) {
			value = ( double ) valueIterator.next();
			temp = value > γ_ ? value - γ_ : 0;
			key = ( Agent ) keyIterator.next();
			relMap.replace( key, temp );
		}
	}

	default AgentDePair getPairByAgent( Agent target, List< AgentDePair > pairList ) {
		for ( AgentDePair pair: pairList ) {
			if ( pair.getAgent().equals( target ) ) return pair;
		}
		assert false : "not to come";
		return null;
	}

	static void clear() {
	}

	static int compare( Entry< Agent, Double > a, Entry< Agent, Double > b ) {
		return b.getValue().compareTo( a.getValue() );
	}
}
