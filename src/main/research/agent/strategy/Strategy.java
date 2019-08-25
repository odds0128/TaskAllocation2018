package main.research.agent.strategy;

import main.research.agent.Agent;

import java.util.*;
import java.util.Map.Entry;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.*;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.Role.JONE_DOE;
import static main.research.SetParam.Role.MEMBER;

public interface Strategy {

    default double renewDEby0or1( double former, boolean isPositive ){
        double multiplier = isPositive ? 1 : 0;
        return former * ( 1.0 - α ) + multiplier * α;
    }

    default double renewDEbyArbitraryReward( double former, double reward ){
        return former * ( 1.0 - α ) + reward * α;
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
                ag.role  = JONE_DOE;
                break;
            case EXECUTE_SUBTASK:
                ag.phase = ag.ms.mySubtaskQueue.isEmpty() ? SELECT_ROLE : EXECUTE_SUBTASK;
                ag.role  = ag.ms.mySubtaskQueue.isEmpty() ? JONE_DOE : MEMBER;
                break;
        }
        ag.validatedTicks = getCurrentTime();
    }

    default void evaporateDE( Map<Agent, Double> relMap ) {
        // CONSIDER: そもそも特定のエージェントのDEのみ保持するようにすればいいのでは？
        arrayEvaporate(relMap);
//         mapEvaporate(relMap);
    }

    // CONSIDER: Mapがでかいとどちゃくそ遅い．Mapのせいというよりラッパークラスのせいか．こっちの方が記述は簡潔なんだけどなぁ．
    default void mapEvaporate( Map<Agent, Double> relMap ) {
        relMap.forEach(
                (key, value) -> {
                    double temp  = value > γ_r ? value - γ_r : 0;
                    relMap.replace(key, temp);
                }
        );
    }

    default void arrayEvaporate( Map<Agent, Double> relMap ) {
        double[] DEs = mapToArray(relMap);
        int zero_index = zeroDEfromHere( DEs );
        evaporateSpecifiedRangeDE( relMap, zero_index );
    }

    default double[] mapToArray( Map<Agent, Double> relMap ) {
        double[] array = new double[AGENT_NUM - 1];
        int index = 0;
        for( Entry<Agent, Double> e : relMap.entrySet() ) {
           array[index++] = e.getValue().doubleValue();
        }
        return array;
    }

    default int zeroDEfromHere( double[] DEs ) {
        int size = AGENT_NUM - 1;
        for( int i = 0; i < size; i++ ) {
            if( DEs[i] == 0 ) return i;
        }
        return size;
    }

    default void evaporateSpecifiedRangeDE( Map<Agent, Double> relMap, int end_index ) {
        Iterator keyIterator   = relMap.keySet().iterator();
        Iterator valueIterator = relMap.values().iterator();
        Agent key;
        double value;
        double temp;
        int i = 0;

        while ( keyIterator.hasNext() && i++ < end_index ){
            value = (double) valueIterator.next();
            temp  = value > γ_r ? value - γ_r : 0;
            key   = (Agent) keyIterator.next();
            relMap.replace( key, temp );
        }
    }

    static void clear(){
    }

    static int compare(Entry<Agent, Double> a, Entry<Agent, Double> b) {
        return b.getValue().compareTo(a.getValue());
    }
}
