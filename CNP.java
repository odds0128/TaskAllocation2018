import java.util.List;

public class CNP implements Strategy, SetParam{
    public void actAsLeader(Agent agent){

    }
    public void actAsMember(Agent agent){

    }
    public List<Agent> selectMembers(Agent agent, List<SubTask> subtasks){
        return null;
    }
    public Agent selectLeader(Agent agent, List<Message> messages){
        return agent;
    }

    public void checkMessages(Agent self){

    }
    public void clearStrategy(){

    }



}
