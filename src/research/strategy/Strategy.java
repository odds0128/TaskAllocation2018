package research.strategy;

import research.task.SubTask;
import research.agent.Agent;

import java.util.List;

public interface Strategy {

    public abstract void actAsLeader(Agent agent);
    public abstract void actAsMember(Agent agent);
    abstract List<Agent> selectMembers(Agent agent, List<SubTask> subtasks);

    void checkMessages(Agent self);
    void clearStrategy();
}
