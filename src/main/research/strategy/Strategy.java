package main.research.strategy;

import main.research.task.Subtask;
import main.research.agent.Agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static main.research.SetParam.γ_r;

public interface Strategy {

    void actAsLeader(Agent agent);
    void actAsMember(Agent agent);
    List<Agent> selectMembers(Agent agent, List<Subtask> subtasks);

    void checkMessages(Agent self);
    void clearStrategy();

    // TODO: test
    default void evaporateDE( Map<Agent, Double> relMap ) {
        relMap.forEach(
                (key, value) -> {
                    double temp = value > γ_r ? value - γ_r : 0;
                    relMap.replace(key, temp);
                }
        );
    }

    // TODO: test
    default Map<Agent, Double> sortReliabilityRanking(Map<Agent, Double> relMap) {
        List<Entry<Agent, Double>> entries = new ArrayList(relMap.entrySet());
        entries.sort(Strategy::compare);

        Map<Agent, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Agent, Double> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    static int compare(Entry<Agent, Double> a, Entry<Agent, Double> b) {
        return b.getValue().compareTo(a.getValue());
    }
}
