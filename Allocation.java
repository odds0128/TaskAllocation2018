public class Allocation {
    private Agent candidate;
    private SubTask subtask;
    private boolean flag = false;
    private int allocationTime = 0;

    Allocation(Agent agent, SubTask subtask){
        this.candidate = agent;
        this.subtask   = subtask;
    }

    Agent getCandidate(){
        return candidate;
    }

    SubTask getSubtask(){
        return subtask;
    }

    void setAllocationTime(int time){
        this.allocationTime = time;
    }

    int getAllocationTime(){
        return allocationTime;
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
