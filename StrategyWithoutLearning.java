import java.util.List;

public class StrategyWithoutLearning implements Strategy, SetParam {

    public void actAsLeader(Agent agent) {
        if (agent.phase == PROPOSITION)    proposeAsL(agent);
        else if (agent.phase == REPORT)    reportAsL(agent);
        else if (agent.phase == EXECUTION) execute(agent);
    }

    public void actAsMember(Agent agent){
        if (agent.phase == REPLY)          replyAsM(agent);
        else if (agent.phase == RECEPTION) receiveAsM(agent);
        else if (agent.phase == EXECUTION) execute(agent);
    }

    void proposeAsL(Agent agent){
    }
    void reportAsL(Agent agent){
    }
    void replyAsM(Agent agent){
    }
    void receiveAsM(Agent agent){
    }
    void execute(Agent agent){
    }
    public void checkMessages(Agent ag){
    }

    @Override
    public Agent selectLeader(Agent agent, List<Message> messages) {
        return null;
    }

    @Override
    public List<Agent> selectMembers(Agent agent, List<SubTask> subtasks) {
        return null;
    }
}
