import java.util.List;

public interface Strategy {

    public abstract void actAsLeader(Agent agent);
    public abstract void actAsMember(Agent agent);
    abstract List<Agent> selectMembers(Agent agent, List<SubTask> subtasks);
    abstract void selectSolicitations(Agent agent, List<Message> messages);

    void checkMessages(Agent self);
    void clearStrategy();
}
