package main.research.strategy;

import main.research.task.SubTask;
import main.research.agent.Agent;

import java.util.List;

public interface Strategy {

    public abstract void actAsLeader(Agent agent);
    public abstract void actAsMember(Agent agent);
    abstract List<Agent> selectMembers(Agent agent, List<SubTask> subtasks);

    void checkMessages(Agent self);
    void clearStrategy();
}
