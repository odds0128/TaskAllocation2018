import java.io.*;
import java.util.*;

/**
 * OutPutクラス
 * Singletonで実装
 * 結果の画面出力とファイル出力を管理
 */
public class OutPut extends ShowGraph implements SetParam {
    static FileWriter fw;
    static BufferedWriter bw;
    static PrintWriter pw;
    static private OutPut singleton = new OutPut();
    static int zeroAgents = 0;
    static int[] means = new int[100];
    static int n ;
    static int i = 1;

/*
    public OutPut getInstance(){
        return singleton;
    }
// */

    OutPut() {
        try {
            fw = new FileWriter("/Users/r.funato/IdeaProjects/TaskAllocation/src/output" + i +  ".csv", false);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
            pw.println( "turn" + ", " + " " + "FinishedTasks" + "," + "Differences" + ", "  + "Messages" + ", " + "Reciprocal" + ", " + "Rational" + ", " + "Leader" + ", " + "Member");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    /**
     * checkTaskメソッド
     * 現在タスクキューにあるタスクのサブタスク数とその必要リソースの表示.
     *
     * @param taskQueue
     */
    static void checkTask(Queue<Task> taskQueue) {
        int num = taskQueue.size();
        System.out.println("Queuesize: " + num);
        for (int i = 0; i < num; i++) {
            Task temp = taskQueue.poll();
            System.out.println(temp);
            System.out.println(temp.subTasks);
            taskQueue.add(temp);
        }
        System.out.println("  Remains: " + taskQueue.size());
    }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(Agent[] agents) {
        int tempL = 0, tempM = 0;
        System.out.println("Total Agents is " + Agent._id);
        for( Agent ag: agents ) {
            if( ag.e_leader > ag.e_member ) tempL++;
            else tempM++;
        }
        System.out.println("Leaders is " + tempL + ", Members is " + tempM);
        List<Agent> list = Arrays.asList(agents);
        Collections.sort(list, new AgentComparator());
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.println(agents[i]);
        }
    }

    /**
     * checkAgentメソッド
     * 二次元配列の場合
     */
    static void checkGrids(Agent[][] grids) {
        System.out.println("Total Agents is " + Agent._id);
        System.out.println("Leaders is " + Agent._leader_num + ", Members is " + Agent._member_num);
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                if (grids[i][j] == null) System.out.print(" 　 ");
                else System.out.print(String.format("%3d ", grids[i][j].id));
            }
            System.out.println();
        }
    }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>(agents);
        System.out.println("Total Agents is " + Agent._id);
        System.out.print("Leaders is " + Agent._leader_num + ", Members is " + Agent._member_num + ", Resources : ");

/*        for( int i = 0; i < RESOURCE_NUM; i++ ) System.out.print( Agent.resSizeArray[i] + ", " );
        System.out.println();
        // */
/*
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.print("ID : " + i + ", ");
            for( int j = 0; j < AGENT_NUM; j++ ){
                System.out.print(String.format("%3d", Manager.distance[i][j]));
            }
            System.out.println();
        }
// */
       Collections.sort(temp, new AgentComparator());
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.println(temp.get(i));
        }
// */
    }

    // ある時点でのパラメータを表示するメソッド
    static void showResults(int turn, List<Agent> agents) {
        int tempL = 0, tempM = 0;

        zeroAgents = countZeroAgent(agents);
        System.out.println(" Turn: " + turn);
        System.out.println("  Total Agents is " + Agent._id);
        int temp = countReciplocalist(agents);
        for( Agent ag: agents ) {
            if( ag.e_leader > ag.e_member ) tempL++;
            else tempM++;
        }
        System.out.println("Leaders is " + tempL + ", Members is " + tempM);
        System.out.println("  Leaders is " + tempL + ", Members is " + tempM
                + " (Rationalists: " + (AGENT_NUM - temp) + ", Reciprocalists: " + (temp) + "), "
                + "ZeroAgents: " + zeroAgents) ;
        System.out.println("  Messages  : " + TransmissionPath.messageNum
                + ", Mean TransmitTime: " + String.format( "%.2f", (double)TransmissionPath.transmitTime/(double)TransmissionPath.messageNum)
                + ", Proposals: " + TransmissionPath.proposals + ", Replies: " + TransmissionPath.replies
                + ", Acceptances: " + TransmissionPath.acceptances + ", Rejects: " + TransmissionPath.rejects);
        System.out.println( "   , Results: " + TransmissionPath.results
                + ", Finished: " + TransmissionPath.finished + ", Failed:" + TransmissionPath.failed);
        System.out.println("  Finished tasks: " + Manager.finishedTasks + ", Disposed tasks: " + Manager.disposedTasks
                + ", Overflow tasks: " + Manager.overflowTasks + ", Processing tasks: " + Manager.processingTasks
                + ", Resting tasks: " + Manager.taskQueue.size() + ", RedoSubTasks: " + Manager.redoSubtasks);
// */
    }

    static void showExecutionTimeTable(Strategy strategy, List<Agent> agents){
        if( strategy.getClass().getName() != "ProposedMethod2" ) return ;
        else{
            for( int i = 0; i < AGENT_NUM; i++ ){
                if( agents.get(i).role == LEADER ) System.out.println(ProposedMethod2.etTable[i]);
            }
        }

    }

    /**
     * writeResultsメソッド
     * ファイルへの結果書き込み
     * @param turn
     * @param agents
     */
    static void writeResults (int turn, List<Agent> agents){
        for( int i = 1; i <= WRITE_NUM; i++ ) {
            pw.print( i * ( TURN_NUM / WRITE_NUM)  + ", " + Manager.meanFinishedTasksArray[i-1]/EXECUTE_NUM + ", ");
            pw.print( (Manager.meanFinishedTasksArray[i]-Manager.meanFinishedTasksArray[i-1])/EXECUTE_NUM + ", ");
            pw.print(Manager.meanMessagesArray[i-1]/EXECUTE_NUM );
/*            pw.print(Agent._leader_num + ", " + Agent._member_num + ", ");

// */
            pw.println();
        }
        pw.println(Manager.meanReciprocal/EXECUTE_NUM + ", " + Manager.meanRational/EXECUTE_NUM );
        pw.println(Manager.meanLeader/EXECUTE_NUM + ", " + Manager.meanMember/EXECUTE_NUM );
    }

    static void writeReliabilities(List<Agent> agents){
        for( int i = 0; i < AGENT_NUM; i++ )  pw.print( ", " + i );
        pw.println();
        for( Agent ag:agents ){
            if( ag.id != 3 ) continue;
            pw.print( ag.id + ", ");
            for( int i = 0; i < AGENT_NUM; i++ ) pw.print( ag.reliabilities[i] + ", " );
            pw.println();
        }
        pw.println();
    }

    /**
     * メソッド
     * 過去にどのエージェントとどのエージェントが何回組んだかと, そのエージェントの役割を書き込む
     * メンバは負, リーダーは正
     */
    static void writeCoalitions( List<Agent> agents ){
        for( int i = 0; i < AGENT_NUM; i++ )  pw.print( ", " + i );
        pw.println();
        for( Agent ag:agents ){
            pw.print( ag.id + ", ");
            if( ag.e_leader > ag.e_member ) for( int i = 0; i < AGENT_NUM; i++ ) pw.print( (-1)*ag.workWith[i] + ", " );
            else for( int i = 0; i < AGENT_NUM; i++ ) pw.print( ag.workWith[i] + ", " );
            pw.println();
        }
        pw.println();
    }

    // 座標上でどういう風に信頼関係ができているかをみるメソッド
    static void showDistributions(Agent[][] grids){
        for( int i = 0; i < ROW/2 ; i++ ){
            for( int j = 0; j < COLUMN/2 ; j++ ){
                if( grids[i][j] != null ) {
                    if( grids[i][j].role == LEADER ) System.out.print( String.format("l%3d", grids[i][j].id));
                    else System.out.print( String.format("%4d", grids[i][j].relAgents.get(0).id));
                }else System.out.print( "    " );
            }
            System.out.println();
        }
    }
    static void calcMeaning(){
        n = 0;
        means[n++] += Agent._leader_num;
        means[n++] += Agent._member_num;
        means[n++] += TransmissionPath.messageNum;
        means[n++] += zeroAgents;
    }
    static void showMeanings(){
        for( int i = 0; i < n; i++ ){
            means[i]/=EXECUTE_NUM;
        }
        System.out.println("Averages: " );
        System.out.print("Leaders: " + means[0] + ", Members: " + means[1] + ", Messages: " + means[2] + ", ZeroAgents: " + means[3]);
        System.out.println();
        pw.println();
        pw.println("Averages" + "," + "Leaders" + ", " + "Members" + ", " + "Messages" + ", " + "ZeroAgents");
        pw.print("," + means[0] + ", " + means[1] + ", " + means[2] + ", " + means[3] );
    }

    void showGraph(List<Agent> agents){
        super.show2DGraph(agents);
    }

    // 改行するだけのメソッド
    public static void newLine(){
        pw.println();
    }

    // こなしたタスク数によって度数分布を表示
    static void showFrequency(List<Agent> agents) {
        int partition = 100;
        int range = 10;
        int max = partition * range;
        int zeroAgent = 0;
        int frequenciesMembers[] = new int[partition + 1];
        int frequenciesLeaders[] = new int[partition + 1];
        for (Agent agent : agents) {
            if (agent.didTasksAsMember == 0 && agent.didTasksAsLeader == 0) {
                zeroAgent++;
                continue;
            }
            if( agent.didTasksAsMember > max ) frequenciesMembers[partition] ++;
            else   frequenciesMembers[agent.didTasksAsMember / range]++;
            if( agent.didTasksAsLeader > max ) frequenciesLeaders[partition] ++;
            else frequenciesLeaders[agent.didTasksAsLeader / range]++;
        }
        System.out.println(" ZeroAgents: " + zeroAgent);

        int j = 0;
        System.out.println("AsMembers: ");
        for (int frequency : frequenciesMembers) {
            if (frequency == 0) continue;
            System.out.print(String.format("%4d", j) + " ~ " + String.format("%4d", j += 10) + ": " + String.format("%4d", frequency) + ": ");
            for (int i = 0; i < frequency / 10; i++) System.out.print("*");
            System.out.println();
        }
        j = 0;
        System.out.println("AsLeaders: ");
        for (int frequency : frequenciesLeaders) {
            if (frequency == 0) continue;
            System.out.print(String.format("%4d", j) + " ~ " + String.format("%4d", j += 10) + ": " + String.format("%4d", frequency) + ": ");
            for (int i = 0; i < frequency / 10; i++) System.out.print("*");
            System.out.println();
        }
    }
    static int countZeroAgent(List<Agent> agents){
        int temp = 0;
        for (Agent agent : agents) {
            if( (agent.didTasksAsMember + agent.didTasksAsLeader) == 0) temp++;
        }
        return temp;
    }
    static int countReciplocalist(List<Agent> agents){
        int temp = 0;
/*        for (Agent agent : agents) {
            if( agent.principle == RATIONAL ) temp++;
        }
// */
        for( Agent agent : agents ){
            if( agent.reliabilities[agent.relRanking.get(0).id] > THRESHOLD_DEPENDABILITY && ( agent.e_member > THRESHOLD_RECIPROCITY || agent.e_leader > THRESHOLD_RECIPROCITY) ) temp++;
        }
        return temp;
    }

    static void fileClose(){
        pw.close();
    }
}