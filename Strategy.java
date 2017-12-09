import java.util.List;

public interface Strategy {

    public abstract void act(Agent agent);
    abstract List<Agent> selectMembers(Agent agent, List<SubTask> subtasks);
    abstract Agent selectLeader(Agent agent, List<Message> messages);
}
