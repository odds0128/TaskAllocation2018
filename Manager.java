/**
 * @author Funato
 * @version 2.0
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Manager implements SetParam {
//    private static Strategy strategy = new ProposedMethod();
//    private static Strategy strategy = new ProposedMethod2();
// private static Strategy strategy = new ReliabilityOriented();
    private static Strategy strategy = new ReliabilityOriented2();
//    private static Strategy strategy = new ProximityOriented();
//    private static Strategy strategy = new RoundRobin();
//    private static Strategy strategy = new Distant();
//    private static Strategy strategy = new RandomStrategy();


    static private OutPut outPut = new OutPut();

    static Queue<Task> taskQueue;
    static int[][] distance = new int[AGENT_NUM][AGENT_NUM];
    private static Agent[][] grids = new Agent[ROW][COLUMN];
    private static List<Agent> agents;
    private static List<Agent> leaders;
    private static List<Agent> members;
    private static List<Agent> freelancer;
    static int turn;
    static int disposedTasks = 0;
    static int redoSubtasks = 0;
    static int overflowTasks = 0;
    static int finishedTasks = 0;
    static List<Integer> finishedTasksArray = new ArrayList<>();
    static int meanMessages = 0;
    static int processingTasks = 0;
    static double meanFinishedTasks = 0;
    static int[] meanFinishedTasksArray = new int[WRITE_NUM+1];
    static int[] meanMessagesArray = new int[WRITE_NUM];
    static int meanReciprocal = 0;
    static int meanRational = 0;
    static int meanLeader  = 0;
    static int meanMember  = 0;
    static double variance = 0;
    private static Random randSeed;

    public static void main(String[] args) {
        assert MAX_REL_AGENTS < AGENT_NUM : "alert0";
        assert INITIAL_TASK_NUM <= TASK_QUEUE_SIZE : "alert1";
        assert AGENT_NUM <= ROW * COLUMN : "alert2";
        assert MAX_PROPOSITION_NUM >= 6 : "alert6";
        assert MAX_PROPOSITION_NUM < AGENT_NUM : "alert";
        assert LAST_PERIOD < TURN_NUM : "a";

        try {
            // seedの読み込み
            FileReader fr = new FileReader("/Users/r.funato/IdeaProjects/TaskAllocation/src/RandomSeed.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;
            StringTokenizer token;

            int num = 0;
            long seed;
            System.out.println(strategy.getClass().getName());
            // num回実験
            while ((line = br.readLine()) != null) {
                int w = 0;
                if (num == EXECUTE_NUM) break;
                System.out.println(++num + "回目");
                seed = Long.parseLong(line);
                randSeed = new Random(seed);
                // タスクの生成
                taskQueue = new LinkedList<>();
                taskQueue.add(new Task(seed));
                for (int i = 1; i < INITIAL_TASK_NUM; i++) taskQueue.add(new Task());
                // エージェントの生成
                // 2017/10/19 グリッド状にランダムに設置するので何か工夫を
                agents = generateAgents(seed, strategy);
                setRelAg(agents);

//                OutPut.checkGrids(grids);
//                OutPut.checkAgent(agents);
//                OutPut.checkTask(taskQueue);

                // ターンの進行
                for (turn = 0; turn < TURN_NUM; turn++) {
/*                    System.out.println("------------------------------------------------------------------");
                    System.out.println("Turn: " + turn);
// */
                    // まず各エージェントの把握
                    // 数ターン毎にタスクを追加する(タスクキューがいっぱいなら, 破棄とする)
                    if (turn % TASK_ADD_TURN == 0) {
                        if (taskQueue.size() + TASK_ADD_NUM > TASK_QUEUE_SIZE) {
                            int i ;
                            for( i = 0; i < TASK_QUEUE_SIZE - taskQueue.size(); i++ ) taskQueue.add(new Task());
                            overflowTasks += TASK_ADD_NUM - i ;
                        }
                        else {
                            for (int i = 0; i < TASK_ADD_NUM; i++) taskQueue.add(new Task());
                        }
                    }
                    // 結果書き込み, 表示の部分
                    if ( (turn+1) % ( TURN_NUM/ WRITE_NUM) == 0 ) {
                        int temp = w % WRITE_NUM;
                        meanFinishedTasksArray[temp] += finishedTasks;
                        meanMessagesArray[temp] += TransmissionPath.messageNum;
                        w++;
                    }
// */

                    //まず役割のない奴が役割を決めてからスタート
                    freelancer = getRoleList(agents, JONE_DOE);
                    actRandom(freelancer);

                    leaders = getRoleList(agents, LEADER) ;
                    members = getRoleList(agents, MEMBER) ;

                    TransmissionPath.transmitWithNoDelay();   // 返事の送信
                    checkMessage(agents);          // 要請の確認, 無効なメッセージに対する返信

                    actRandom(leaders);            // メンバの選定,　要請の送信
                    actRandom(members);            // 要請があるメンバ達はそれに返事をする
                }
                int temp = OutPut.countReciplocalist(agents);
                meanReciprocal += temp;
                meanRational   += AGENT_NUM - temp;
                for( Agent ag: agents ) {
                    if( ag.e_leader > ag.e_member ) meanLeader++;
                    else meanMember++;
                }
//                outPut.checkAgent(agents);
                // ↑ 一回の実験の終了
                processingTasks = countProcessing();
                OutPut.showResults(turn, agents);
                OutPut.showExecutionTimeTable(strategy, agents);
                OutPut.writeCoalitions(agents);
//                OutPut.writeResults(turn, agents);
//                OutPut.showFrequency(agents);
//                OutPut.checkGrids(grids);
//                OutPut.checkAgent(agents);
/*                outPut.showDistributions(grids);
                outPut.calcMeaning();
                outPut.newLine();
// */
                meanMessages += TransmissionPath.messageNum;
                finishedTasksArray.add(finishedTasks);
                if( num == EXECUTE_NUM ) break;
                clearAll();
            }
//            outPut.showGraph(agents);
            OutPut.writeResults(turn, agents);
            OutPut.writeExcels(agents);
            // ↑ 全実験の終了
            finishedTasks = 0;
            for (int temp : finishedTasksArray) { finishedTasks += temp; }
            meanFinishedTasks = (double)finishedTasks / (double)EXECUTE_NUM;
            System.out.println("Average finished tasks: " + meanFinishedTasks
                    + ", Standard Deviation: " + String.format("%.2f", calcSTDEV() )
                    + ", MeanMessages: " + meanMessages / EXECUTE_NUM
                    + ", MeanRationalists: " + meanRational/EXECUTE_NUM
                    + ", MeanReciplocalists: " + meanReciprocal/EXECUTE_NUM
                    + ", MeanLeaders: " + meanLeader/EXECUTE_NUM
                    + ", MeanMembers: " + meanMember/EXECUTE_NUM
                    + ", meanSubTasks: " + (double)Task.totalSubtaskNum/(double)Task.totalSubTasks);
//            outPut.showMeanings();
            outPut.fileClose();
// */
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }


    static List<Agent> generateAgents(long seed, Strategy strategy) {
        Agent temp;
        List tempList = new ArrayList();
        int randX = randSeed.nextInt(COLUMN);
        int randY = randSeed.nextInt(ROW);
        while (checkDuplication(randX, randY) == false) {
            randX = randSeed.nextInt(COLUMN);
            randY = randSeed.nextInt(ROW);
        }
        temp = new Agent(seed, randX, randY, strategy);
        grids[temp.y][temp.x] = temp;
        tempList.add(temp);
        for (int i = 1; i < AGENT_NUM; i++) {
            while (checkDuplication(randX, randY) == false) {
                randX = randSeed.nextInt(COLUMN);
                randY = randSeed.nextInt(ROW);
            }
            temp = new Agent(randX, randY, strategy);
            grids[temp.y][temp.x] = temp;
            tempList.add(temp);
        }
        return tempList;
    }

    static Agent getAgentRandomly(Agent self, List<Agent> exceptions) {
        int random = randSeed.nextInt(agents.size());
        Agent candidate = agents.get(random);
        while (candidate.equals(self) || self.inTheList(candidate, exceptions) > 0) {
            random = randSeed.nextInt(agents.size());
            candidate = agents.get(random);
        }
        return candidate;
    }

    static boolean checkDuplication(int x, int y) {
        if (grids[y][x] == null) return true;
        else return false;
    }

    static void setRelAg(List<Agent> agents) {
        int dist = 0;
        List<Agent> tempList = new ArrayList();

        // 距離の計算
        for (int i = 0; i < AGENT_NUM; i++) {
            for (int j = 0; j < AGENT_NUM; j++) {
                distance[i][j] = calcManhattan(agents.get(i), agents.get(j));
            }
        }

//        outPut.checkGrids(grids);
//        outPut.checkAgent(agents);
        // 信頼エージェントの初期化
        // ランダム手法の場合
        if (strategy.getClass().getName() == "RandomStrategy") {
            Agent agent;
            for (int i = 0; i < AGENT_NUM; i++) {
                agent = agents.get(i);
                Agent candidate;
                // ランダムで信頼エージェントとする
                for (int j = 0; j < MAX_REL_AGENTS;){
                    candidate = getAgentRandomly(agent, agent.relAgents);
                    if( inTheList(candidate, agent.relAgents) < 0 ){
                        agent.relAgents.add(candidate);
                        j++;
                    }
                }
            }
        }
        // ランダム手法ではない場合
/*        else {
            Agent agent;
            for (int i = 0; i < AGENT_NUM; ) {
                agent = agents.get(i);
                dist++;
                assert dist < 11 : "alert 8: " + dist;
                // 距離に基づいて信頼エージェントを設定する
                for (int j = 0; j < AGENT_NUM; j++) {
                    // 距離がdistなら信頼エージェント候補とする
                    if (distance[i][j] == dist) {
                        tempList.add(agents.get(j));
                    }
                }
                agent.relRanking.addAll(tempList);
//            System.out.println("ID: " + i + "  " + agent.relRanking + ", ");
                tempList.clear();
                if (agent.relRanking.size() == AGENT_NUM - 1) {
                    // 戦略が"遠い優先"だったら遠くのやつを信頼エージェントにする
                    if (strategy.getClass().getName() == "Distant") for (int j = 0; j < MAX_REL_AGENTS; j++)
                        agent.relAgents.add(agent.relRanking.get(AGENT_NUM - j - 2));
                        // 他の戦略なら単純に近いやつを信頼エージェントにする
                    else for (int j = 0; j < MAX_REL_AGENTS; j++) agent.relAgents.add(agent.relRanking.get(j));
                    i++;
                    dist = 0;
                }
            }
        }
 // */
        else {
            Agent agent;
            for(int i = 0; i < AGENT_NUM; i++){
                List<Agent> temp = new ArrayList<>(agents);
                int rand;
                Agent ag;
                agent = agents.get(i);
                while (temp.size() != 0) {
                    rand = randSeed.nextInt(temp.size());
                    ag = temp.remove(rand);
                    if( !ag.equals(agent) ) agent.relRanking.add(ag);
                }
            }
        }

    }

    static int inTheList(Agent a, List<Agent> agents) {
        for (int i = 0; i < agents.size(); i++) {
            if (a == agents.get(i)) return i;
        }
        return -1;
    }

    static int calcManhattan(Agent from, Agent to) {
        int distance = Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
        return (int) Math.ceil((double) (distance * 10) / (double) (ROW + COLUMN - 2));
    }

    // taskQueueにあるタスクをリーダーに渡すメソッド
    static Task getTask() {
        Task temp;
        temp = taskQueue.poll();
        if (temp != null) temp.flag = PROCESSING;
        return temp;
    }

    // 現在のターン数を返すメソッド
    static int getTicks() {
        return turn;
    }

    // タスクキューからタスクがオーバーフローしたか判定するメソッド
    static boolean taskOverflows() {
        return (overflowTasks > 0);
    }

    static int countProcessing() {
        int count = 0;
        for (Agent leader : agents) {
            if (leader.role == LEADER) {
                if (leader.ourTask != null) count++;
            }
        }
        return count;
    }

    /**
     * disposeTaskメソッド
     * 引数のリーダーが持つタスクを放棄する
     * 2017/11/02 廃棄タスクはタスクキューに入れ直すことにする
     * 2017/11/25 再び破棄する方式に. 今後その方針を変える必要はおそらくない
     */
    static void disposeTask(Agent leader) {
        leader.ourTask.flag = false;
        redoSubtasks += leader.ourTask.subTaskNum - leader.restSubTask;
        disposedTasks++;
        leader.ourTask = null;
    }

    // 引数のリーダーが持つタスクの終了通知
    static void finishTask(Agent leader) {
/*        System.out.println("turn: " + turn + ", ID: " + leader.id + " did " + leader.ourTask + ", and I(" + leader + ") did " + leader.mySubTask);
        System.out.println( " Members: " + leader.teamMembers + " very good team!");
// */
//        if(leader.id == 3) System.out.println("ID: " + leader.id + ", " + leader.meanExecutedTicks);
        leader.ourTask = null;
        Manager.finishedTasks++;
    }

    static List<Agent> getRoleList(List<Agent> agents, int role){
        List<Agent> temp = new ArrayList<>();
        for( Agent ag: agents ){
            if( ag.role == role ) temp.add(ag);
        }
        return temp;
    }

    // 順番をランダムに行動させる部分
    private static void actRandom(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>(agents);
        int rand;
        while (temp.size() != 0) {
            rand = randSeed.nextInt(temp.size());
            temp.remove(rand).act();
        }
        temp.clear();
    }

    private static void checkMessage(List<Agent> agents){
        for( Agent ag: agents ) ag.checkMessages(ag);
    }

    // 次のタームに行く前にあらゆるパラメータを初期化する部分
    private static void clearAll() {
        taskQueue.clear();
        agents = null;
        disposedTasks = 0;
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

    // 実験結果の標準偏差をとる
    private static double calcSTDEV() {
        double temp = 0;
        if (finishedTasksArray.size() != EXECUTE_NUM) { return -1; }
        for (int i = 0; i < EXECUTE_NUM; i++) {
            temp += (finishedTasksArray.get(i) - meanFinishedTasks) * (finishedTasksArray.get(i) - meanFinishedTasks);
        }
        return Math.sqrt(temp / (double) EXECUTE_NUM);
    }
}
