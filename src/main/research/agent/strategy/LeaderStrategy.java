package main.research.agent.strategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.task.Subtask;

import static main.research.SetParam.Phase.*;

import java.util.*;

public abstract class LeaderStrategy implements Strategy, SetParam {
    public void actAsLeader(Agent agent) {
        sortReliabilityRanking(agent.reliabilityRankingAsL);
        if (agent.phase == PROPOSITION) proposeAsL(agent);
        else if (agent.phase == REPORT) reportAsL(agent);
        evaporateDE(agent.reliabilityRankingAsL);
    }

    abstract public List<Agent> selectMembers(Agent agent, List<Subtask> subtasks);

    abstract public void checkMessages(Agent self);

    protected abstract void proposeAsL(Agent la);
    protected abstract void reportAsL(Agent la);

}
