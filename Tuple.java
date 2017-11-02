public class Tuple {
    private Agent candidate;
    private SubTask subtask;
    private boolean flag = false;

    Tuple(Agent agent, SubTask subtask){
        this.candidate = agent;
        this.subtask   = subtask;
    }

    Agent getCandidate(){
        return candidate;
    }

    SubTask getSubtask(){
        return subtask;
    }

    void setFlag(){
        this.flag = true;
    }

    boolean getFlag(){
        return flag;
    }

    @Override
    public String toString(){
        return candidate.id + "← " + subtask.subtask_id ;
    }

}
