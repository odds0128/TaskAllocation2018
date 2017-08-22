/**
 * @author Funato
 * @version 1.1
 */

import java.util.*;

public class Manager implements SetParam {

    private static Agent[][] agents;
    private static Queue<Task> taskQueue;
    private static List<Agent> leaders;
    private static List<Agent> members;
    private static List<Agent> freeAgents;
    private static int disposedTasks = 0;
    private static int finishedTasks = 0;

    public static void main(String[] args) {

        // タスクの生成
        taskQueue = new LinkedList<Task>();
        for (int i = 0; i < INITIAL_TASK_NUM; i++) {
            taskQueue.add(new Task(SetParam.UNIFORM));
        }
        // エージェントの生成
        agents = new Agent[ROW][COLUMN];
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                agents[i][j] = new Agent(UNIFORM);
            }
        }
//        CheckParam.checkTask(taskQueue);
//        CheckParam.checkAgent(agents);


        // ターンの進行
        for (int turn = 0; turn < TURN_NUM; turn++) {
//            System.out.println("Turn: " +turn);
            // 数ターン毎にタスクを追加する(タスクキューがいっぱいなら, 破棄とする)
            if (turn % TASK_ADD_TURN == 0) {
                if (taskQueue.size() >= TASK_QUEUE_SIZE) disposedTasks++;
                else taskQueue.add(new Task(SetParam.UNIFORM));
            }
            // まず各エージェントの把握
            leaders = graspAgents(LEADER);
            members = graspAgents(MEMBER);
            freeAgents = graspAgents(WAITING);
//            System.out.println("Leaders: " + leaders.size() + ", Members: "+ members.size() + ", Free: " + freeAgents.size() );
            /*
             Agentが毎ターンなんかする
             1ターン中にできることは
             待機状態の解除, 交渉, 実行 の三種類の内いずれか一つの行動
             */
            for( Agent freeAgent:freeAgents ) {
                freeAgent.act1();
            }
            for( Agent leader:leaders ) {
                leader.act1();
            }
            for( Agent member: members ){
                member.act1();
            }
            for( Agent leader:leaders ) {
                leader.act2();
            }
            for( Agent member: members ){
                member.act2();
            }
        }

//        CheckParam.checkTask(taskQueue);
        System.out.println("Disposed tasks : " + disposedTasks);
        System.out.println("finished tasks : " + finishedTasks);
    }

    public static Task getTask() {
        return taskQueue.poll();
    }

    /**
     * graspAgentsメソッド
     * パラメータで指定された役割のエージェントをリストにして返す
     * @param role
     * @return temp
     */
    static List<Agent> graspAgents(int role){
        List<Agent> temp = new LinkedList<Agent>() ;
        for( int i = 0; i < ROW; i++ ){
            for( int j = 0; j < COLUMN; j++ ){
                if( agents[i][j].role == role ){
                    temp.add( agents[i][j]);
                }
            }
        }
        return temp;
    }

    static List<Agent> getMembers(){
        return members;
    }

    static void disposeTask(){
        Manager.disposedTasks++;
    }

    static void finishTask(){
        Manager.finishedTasks++;
    }
}

