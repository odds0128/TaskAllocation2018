public class AllocatedSubTask {
    private SubTask st;
    private int     allocatedTime;
    private int     requiredResources;
    private int     taskId;

    AllocatedSubTask(SubTask st, int allocatedTime, int taskId){
        this.st = st;
        this.allocatedTime = allocatedTime;
        this.taskId = taskId;
        if( st == null  ) System.out.println("h");
        this.requiredResources = st.reqRes[st.resType];
    }

    SubTask getSt(){
        return st;
    }
    int getAllocatedTime(){
        return allocatedTime;
    }
    int getRequiredResources(){
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
