public class AllocatedSubTask {
    private SubTask st;
    private int     allocatedTime;
    private int     requiredResources;

    AllocatedSubTask(SubTask st, int allocatedTime){
        this.st = st;
        this.allocatedTime = allocatedTime;
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


    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("AllocatedSubtask: \n");
        sb.append(" Subtask: " + st + "\n");
        sb.append(" AllocateTime: " + allocatedTime + "\n");
        sb.append(" RequiredResource: " + requiredResources + "\n");

        return sb.toString();
    }
}
