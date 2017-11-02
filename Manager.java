/**
 * @author Funato
 * @version 2.0
 */

import java.util.*;
import java.io.*;

public class Manager implements SetParam {

    private static Strategy strategy = new ProximityOriented();
    private static Queue<Task> taskQueue;
    private static int[][] distance = new int[AGENT_NUM][AGENT_NUM];
    private static Agent[][] grids = new Agent[ROW][COLUMN];
    private static List<Agent> agents ;
    private static int turn;
    private static int[] meanExecution = new int[100];
    private static int[] leaderNum     = new int[100];
//    private static int disposedTasks = 0;
    private static int overflowTasks = 0;
    private static int finishedTasks = 0;
    private static List<Integer> finishedTasksArray = new ArrayList<>();
    private static int processingTasks = 0;
    private static int meanFinishedTasks = 0;
    private static double variance = 0;
    private static Random randSeed ;

    public static void main(String[] args) {
        assert MAX_REL_AGENTS < AGENT_NUM           : "alert0";
        assert INITIAL_TASK_NUM <= TASK_QUEUE_SIZE  : "alert1";
        assert AGENT_NUM <= ROW*COLUMN              : "alert2";

        try{
            // seedの読み込み
            FileReader fr = new FileReader("/Users/r.funato/IdeaProjects/TaskAllocation/src/RandomSeed.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;
            StringTokenizer token;
            int    num = 0;
            long   seed;

            // num回実験
            while( ( line = br.readLine() ) != null ){
                if( num == EXECUTE_NUM ) break;
                System.out.println(num++ + "回目");
                seed = Long.parseLong(line);
                randSeed = new Random(seed);
                // タスクの生成
                taskQueue = new LinkedList<>();
                taskQueue.add(new Task(seed));
                for (int i = 1; i < INITIAL_TASK_NUM; i++) taskQueue.add(new Task() );
                // エージェントの生成
                // 2017/10/19 グリッド状にランダムに設置するので何か工夫を
                agents = generateAgents( seed, strategy);
                setRelAg(agents);

                OutPut.checkGrids(grids);
                OutPut.checkTask(taskQueue);
                OutPut.checkAgent(agents);

                // ターンの進行
                for (turn = 0; turn < TURN_NUM; turn++) {
//                    System.out.println("------------------------------------------------------------------");
//                    System.out.println("Turn: " + turn);
                    // まず各エージェントの把握
                    // 数ターン毎にタスクを追加する(タスクキューがいっぱいなら, 破棄とする)
                    if (turn % TASK_ADD_TURN == 0) {
                        if (taskQueue.size() >= TASK_QUEUE_SIZE) overflowTasks++;
                        else taskQueue.add(new Task());
                    }

                    // ターンはまず伝送路のメッセージの授受から行う.その後 Agentがランダム順で行動する
                    TransmissionPath.transmit();
                    actRandom(agents);
//                    OutPut.checkTask(taskQueue);
//                    OutPut.checkAgent(agents);
//                    System.out.println("Remaining tasks: " + taskQueue.size() + System.getProperty("line.separator"));
                }

                // 終了後, 必要なパラメータの出力
//                OutPut.showResults();
//                OutPut.checkAgent(agents);
              System.out.println("Finished tasks: " + finishedTasks + ", Overflow tasks: " + overflowTasks + ", Processing tasks: " + countProcessing() + ", resting tasks: " + taskQueue.size());
                OutPut.checkGrids(grids);

                clearAll();
            }
            br.close();
        }catch( FileNotFoundException e ){
            e.printStackTrace();
        }catch( IOException e2 ){
            e2.printStackTrace();
        }
    }


    static List<Agent> generateAgents( long seed, Strategy strategy) {
        Agent temp;
        List tempList = new ArrayList();
        int randX = randSeed.nextInt(COLUMN);
        int randY = randSeed.nextInt(ROW);
        while ( checkDuplication(randX, randY) == false ) {
            randX = randSeed.nextInt(COLUMN);
            randY = randSeed.nextInt(ROW);
        }
        temp = new Agent(seed, randX, randY, strategy);
        grids[temp.y][temp.x] = temp;
        tempList.add(temp);
        for (int i = 1; i < AGENT_NUM; i++) {
            while ( checkDuplication(randX, randY) == false ) {
                randX = randSeed.nextInt(COLUMN);
                randY = randSeed.nextInt(ROW);
            }
            temp = new Agent(randX, randY, strategy);
            grids[temp.y][temp.x] = temp;
            tempList.add(temp);
        }
        return tempList;
    }
    static boolean checkDuplication(int x, int y){
        if( grids[y][x] == null ) return true;
        else return false;
    }
    static void setRelAg(  List<Agent> agents) {
        int dist = 1;
        int rest = MAX_REL_AGENTS;
        List<Agent> tempList = new ArrayList();
        for (int i = 0; i < AGENT_NUM; i++) {
            for (int j = 0; j < AGENT_NUM; j++) {
                distance[i][j] = calcManhattan(agents.get(i), agents.get(j));
            }
        }
        // エージェントiについて
        for (int i = 0; i < AGENT_NUM; i++) {
            Agent agent = agents.get(i);
            // 他のエージェントから距離に基づいて信頼エージェントを設定する
            for (int j = 0; j < AGENT_NUM; j++) {
                // 距離がdistance以下なら信頼エージェント候補とする
                if (distance[i][j] == dist) {
                    tempList.add(agents.get(j));
                }
            }
            // 信頼エージェント数に満たなければ
            if (tempList.size() < rest) {
                agent.relAgents.addAll(tempList);
                rest -= tempList.size();
                dist++;
                i--;
                // 信頼エージェントが十分集まれば
            } else if (tempList.size() == rest) {
                agent.relAgents.addAll(tempList);
                dist = 1;
                rest = MAX_REL_AGENTS;
            } else {
                int rand;
                while (rest != 0) {
                    rand = randSeed.nextInt(rest);
                    agent.relAgents.add(tempList.remove(rand));
                    rest--;
                }
                dist = 1;
                rest = MAX_REL_AGENTS;
            }
            tempList.clear();
        }
    }
    static int calcManhattan(Agent from, Agent to) {
        return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
    }
    // taskQueueにあるタスクをリーダーに渡すメソッド
    static Task getTask() {
        Task temp;
        temp = taskQueue.poll();
        if( temp != null ) temp.flag = PROCESSING;
        return temp;
    }
    static int countProcessing(){
        int count = 0;
        for( Agent leader: agents ){
            if( leader.role == LEADER ) {
                if( leader.ourTask != null ) count++;
            }
        }
        return  count;
    }
    // 現在のターン数を返すメソッド
    static int getTicks(){
        return turn;
    }
    // タスクキューからタスクがオーバーフローしたか判定するメソッド
    static boolean taskOverflows(){
        return ( overflowTasks > 0) ;
    }
    /**
     * disposeTaskメソッド
     * 引数のリーダーが持つタスクを放棄する
     * 2017/11/02 廃棄タスクはタスクキューに入れ直すことにする
     */
    static void disposeTask(Agent leader){
        leader.ourTask.flag = false;
        taskQueue.add(leader.ourTask);
        leader.ourTask = null;
    }
    // 引数のリーダーが持つタスクの終了通知
    static void finishTask(Agent leader){
//        System.out.println("turn: " + turn + ", ID: " + leader.id + " did " + leader.ourTask );
//        System.out.println("ID: " + leader.id + ", Members: " + leader.teamMembers + " very good team!");
        leader.ourTask = null;
        Manager.finishedTasks++;
    }
    // 順番をランダムに行動させる部分
    private static void actRandom(List<Agent> agents){
        List<Agent> temp = new ArrayList<>(agents);
        int rand ;
        while( temp.size() != 0 ) {
            rand = randSeed.nextInt(temp.size());
            temp.remove(rand).act();
        }
        temp.clear();
    }
    // 次のタームに行く前にあらゆるパラメータを初期化する部分
    private static void clearAll(){
        taskQueue.clear();
        agents = null;
//        disposedTasks = 0;
        finishedTasks = 0;
        overflowTasks = 0;
        processingTasks = 0;
        for (int i = 0; i < AGENT_NUM; i++) {
            for (int j = 0; j < AGENT_NUM; j++) {
                distance[i][j] = 0;
            }
        }
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                grids[i][j] = null;
            }
        }
        TransmissionPath.clearTP();
        SubTask.clearST();
        Task.clearT();
        Agent.clearA();
    }
    // 実験結果の分散をとる
    private static double calcSTDEV(){
        double temp = 0;
        if( finishedTasksArray.size() != EXECUTE_NUM ) return -1;
        for( int i = 0; i < EXECUTE_NUM; i++ ) {
            temp += ( finishedTasksArray.get(i) - meanFinishedTasks ) * ( finishedTasksArray.get(i) - meanFinishedTasks );
        }
        return Math.sqrt( temp/(double)EXECUTE_NUM );
    }
}
