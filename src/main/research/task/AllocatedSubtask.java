package main.research.task;

public class AllocatedSubtask {
    private Subtask st;
    private int     allocatedTime;
    private int     requiredResources;
    private int     taskId;

    public AllocatedSubtask(Subtask st, int allocatedTime, int taskId){
        this.st = st;
        this.allocatedTime = allocatedTime;
        this.taskId = taskId;
        if( st == null  ) System.out.println("h");
        this.requiredResources = st.reqRes[st.resType];
    }

    public Subtask getSt(){
        return st;
    }
    public int getAllocatedTime(){
        return allocatedTime;
    }
    public int getRequiredResources(){
        return requiredResources;
    }
    public int getTaskId() {
        return taskId;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(st);
        return sb.toString();
    }
}
