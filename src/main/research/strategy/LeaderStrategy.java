package main.research.strategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.task.Subtask;

import java.util.*;

public abstract class LeaderStrategy implements Strategy, SetParam {
    public void actAsLeader(Agent agent) {
        if (agent.phase == PROPOSITION) proposeAsL(agent);
        else if (agent.phase == REPORT) reportAsL(agent);
        evaporateDE(agent.relRanking_l);
    }

    abstract public List<Agent> selectMembers(Agent agent, List<Subtask> subtasks);

    abstract public void checkMessages(Agent self);

    protected abstract void proposeAsL(Agent la);
    protected abstract void reportAsL(Agent la);

    protected abstract void renewDE(Agent from, Agent to, double evaluation);

}
