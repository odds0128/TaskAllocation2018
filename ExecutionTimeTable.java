public class ExecutionTimeTable implements SetParam {
    int[] totalExecutedSubtasks = new int[AGENT_NUM];   // あるエージェントiに割り当ててきた処理サブタスク数
    int[] totalExecutionTime    = new int[AGENT_NUM];   // iの合計処理時間
    int[] meanExecutionTime     = new int[AGENT_NUM];   // iの平均処理時間

    public void renewMET(int index, int et){
        totalExecutedSubtasks[index]++;
        totalExecutionTime[index] += et;
        meanExecutionTime[index]  = totalExecutionTime[index]/totalExecutedSubtasks[index];
    }

    public int getMET(int index){
        return meanExecutionTime[index];
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        for( int i = 0; i < AGENT_NUM; i++ ){
            str.append("ID: " + String.format("%4d", i)
                    + ", totalExecutedSubtasks: " + String.format("%3d",totalExecutedSubtasks[i])
                    + ", totalExecutionTime: "    + String.format("%3d",totalExecutionTime[i])
                    + ", meanExecutionTime: "     + String.format("%3d", meanExecutionTime[i])
            );
        }
        return str.toString();
    }
}
