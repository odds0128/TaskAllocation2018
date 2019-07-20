package main.research.strategy;

import main.research.task.AllocatedSubtask;
import main.research.agent.Agent;

import java.util.*;
import java.util.Map.Entry;

import static main.research.SetParam.*;

public interface Strategy {
    // TODO: なんじゃこりゃ．AllocatedSubtaskクラスとか絶対いらんやん
    Map<Agent, AllocatedSubtask>[] teamHistory = new HashMap[AGENT_NUM];

    void checkMessages(Agent self);

    default double renewDEby0or1( double former, boolean isPositive ){
        double multiplier = isPositive ? 1 : 0;
        return former * ( 1.0 - α ) + multiplier * α;
    }

    default double renewDEbyArbitraryReward( double former, double reward ){
        return former * ( 1.0 - α ) + reward * α;
    }

    default void evaporateDE( Map<Agent, Double> relMap ) {
        // FIXME: そもそも特定のエージェントのDEのみ保持するようにすればいいのでは？
        arrayEvaporate(relMap);
//         mapEvaporate(relMap);
    }

    // Mapがでかいとどちゃくそ遅い．Mapのせいというよりラッパークラスのせいか．こっちの方が記述は簡潔なんだけどなぁ．
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

    //TODO: 効率悪そう．DEが変わったエージェントの前後と比較して入れ替えればいいだけ．
    default Map<Agent, Double> sortReliabilityRanking(Map<Agent, Double> relMap) {
        List<Entry<Agent, Double>> entries = new ArrayList(relMap.entrySet());
        entries.sort(Strategy::compare);

        Map<Agent, Double> sortedMap = new LinkedHashMap<>(HASH_MAP_SIZE);
        for (Map.Entry<Agent, Double> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    static void clear(){
        for (int i = 0; i < AGENT_NUM; i++) {
            teamHistory[i].clear();
        }
    }

    static int compare(Entry<Agent, Double> a, Entry<Agent, Double> b) {
        return b.getValue().compareTo(a.getValue());
    }
}
