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
//    private static Strategy strategy = new ProposedMethodWithCheating();
    private static Strategy strategy = new ReliabilityOriented();  // 信頼度の蒸発あり
//    private static Strategy strategy = new ReliabilityOrientedWithCheating();
//    private static Strategy strategy = new ProximityOriented();
//    private static Strategy strategy = new RoundRobin();

    static private OutPut outPut = new OutPut();

    static Queue<Task> taskQueue;
    static int[][] distance = new int[AGENT_NUM][AGENT_NUM];
    private static Agent[][] grids = new Agent[ROW][COLUMN];
    private static List<Agent> agents;
    private static List<Agent> leaders;
    private static List<Agent> members;
    private static List<Agent> freelancer;
    static int disposedTasks = 0;
    static int overflowTasks = 0;
    static int finishedTasks = 0;
    static int finishedTasksInDepopulatedArea = 0;
    static int processingTasks = 0;
    static int turn;
    private static Random randSeed;
    static List<Agent> snapshot = new ArrayList<>();

    public static void main(String[] args) {
        assert MAX_RELIABLE_AGENTS < AGENT_NUM : "alert0";
        assert INITIAL_TASK_NUM <= TASK_QUEUE_SIZE : "alert1";
        assert AGENT_NUM <= ROW * COLUMN : "alert2";
        assert COALLITION_CHECK_SPAN < MAX_TURN_NUM : "a";

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
                if (num == EXECUTION_TIMES) break;
                System.out.println(++num + "回目");
                seed = Long.parseLong(line);
                randSeed = new Random(seed);

                // タスクの生成
                taskQueue = new LinkedList<>();
                taskQueue.add(new Task(seed));
                for (int i = 1; i < INITIAL_TASK_NUM; i++) taskQueue.add(new Task());

                // エージェントの生成
                agents = generateAgents(seed, strategy);

                // ターンの進行
                for (turn = 1; turn <= MAX_TURN_NUM; turn++) {
/*                    System.out.println("------------------------------------------------------------------");
                    System.out.println("Turn: " + turn);
// */
                    // まずタスクキューにタスクを追加する
                    if (turn % TASK_ADDITION_SPAN == 0) {
                        int room = TASK_QUEUE_SIZE - taskQueue.size();    // タスクキューの空き
                        // タスクキューに空きが十分にあるなら, 普通にぶち込む
                        if ( ADDITIONAL_TASK_NUM <= room ) {
                            for (int i = 0; i < ADDITIONAL_TASK_NUM; i++) taskQueue.add(new Task());
                        }
                        // タスクキューからタスクがはみ出そうなら, 入れるだけ入れてはみ出る分はオーバーフローとする
                        else{
                            int i;
                            for (i = 0; i < room; i++) taskQueue.add(new Task());
                            overflowTasks += ADDITIONAL_TASK_NUM - i;
                        }
                    }
// */
                    // まず役割のない奴が役割を決めてからスタート
                    freelancer = getRoleList(agents, JONE_DOE);
                    actRandom(freelancer);

                    // フリーランサーが役割を選び終わった後で数える
                    if (turn % (MAX_TURN_NUM / WRITING_TIMES) == 0) {
                        OutPut.aggregateAgentData(agents);
                    }
/*
                    if( turn == SNAPSHOT_TIME ){
                        snapshot = takeAgentsSnapshot(agents);
                        OutPut.writeGraphInformation(agents, "interim_report");
                        Agent.resetWorkHistory(agents);
                    }
// */
                    leaders = getRoleList(agents, LEADER);
                    members = getRoleList(agents, MEMBER);

                    TransmissionPath.transmit();                // 通信遅延あり
//                    TransmissionPath.transmitWithNoDelay();   // 通信遅延なし
                    checkMessage(agents);          // 要請の確認, 無効なメッセージに対する返信

                    actRandom(leaders);            // メンバの選定,　要請の送信
                    actRandom(members);            // 要請があるメンバ達はそれに返事をする

                    if (turn % (MAX_TURN_NUM / WRITING_TIMES) == 0) {
                        int rmNum = Agent.countReciprocalMember(agents);
                        OutPut.aggregateData(finishedTasks, disposedTasks, overflowTasks, rmNum, finishedTasksInDepopulatedArea);
                        OutPut.indexIncrement();
                        finishedTasks = 0; disposedTasks = 0; overflowTasks = 0; finishedTasksInDepopulatedArea = 0;
                    }
// */
                // ここが1tickの最後の部分．次のtickまでにやることあったらここで．
                }
                // ↑ 一回の実験のカッコ．以下は実験の合間で作業する部分

                processingTasks = countProcessing();
//                OutPut.showResults(turn, agents,num);
//                OutPut.showLeaderRetirement(snapshot, agents);
                if (num == EXECUTION_TIMES) break;
                clearAll();
            }
            // ↑ 全実験の終了のカッコ．以下は後処理

//            OutPut.writeReliabilities(turn,agents);
            OutPut.writeResults();
//            OutPut.writeGraphInformation(agents, "result");
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
        setRelAg(tempList);
        Agent.makeLonelyORAccompaniedAgentList(tempList);
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

    /**
     * setRelAgメソッド
     *
     * @param agents
     * @return agents_in_depopulation_area;
     */
    static void setRelAg(List<Agent> agents) {
        int dist = 0;
        List<Agent> tempList = new ArrayList();
        int agents_in_depopulated_area = 0, agents_in_populated_area = 0;

        // 距離の計算 & iから距離1にいるエージェントのカウント
        int min = Integer.MAX_VALUE;
        int max = 0;
        int quartile = AGENT_NUM/4;
        int[] density = new int[AGENT_NUM];     // エージェント周辺の密度(距離x=2以下にいるエージェントの数)
        for (int i = 0; i < AGENT_NUM; i++) {
            int temp ;
            for (int j = 0; j < AGENT_NUM; j++) {
                temp = calcManhattan(agents.get(i), agents.get(j));
                distance[i][j] = temp;
                if( temp <= 2 ) density[i]++;
            }
            if( density[i] < min ) min = density[i];
            if( density[i] > max ) max = density[i];
         }
        // 過疎エージェントの探索
        for (int neighborhoods_i= min; agents_in_depopulated_area < quartile; neighborhoods_i++) {
            for( int density_i = 0; density_i < AGENT_NUM; density_i++  ){
                if( density[density_i] == neighborhoods_i ){
                    agents_in_depopulated_area++;
                    agents.get(density_i).isLonely = 1;
                }
            }
        }
        // 過密エージェントの探索
        for (int neighborhoods_i= max; agents_in_populated_area < quartile; neighborhoods_i--) {
            for( int density_i = 0; density_i < AGENT_NUM; density_i++  ){
                if( density[density_i] == neighborhoods_i ){
                    agents_in_populated_area++;
                    agents.get(density_i).isAccompanied = 1;
                }
            }
        }

        // 信頼エージェントの初期化
        // ランダム手法の場合
        if (strategy.getClass().getName() == "RandomStrategy") {
            Agent agent;
            for (int i = 0; i < AGENT_NUM; i++) {
                agent = agents.get(i);
                Agent candidate;
                // ランダムで信頼エージェントとする
                for (int j = 0; j < MAX_RELIABLE_AGENTS; ) {
                    candidate = getAgentRandomly(agent, agent.relAgents);
                    if (inTheList(candidate, agent.relAgents) < 0) {
                        agent.relAgents.add(candidate);
                        j++;
                    }
                }
            }
        }
        // 学習なし手法で距離依存の場合(学習なしでは信頼度は使わないが便宜上定義する)
        else if (strategy.getClass().getName() == "RoundRobin" || strategy.getClass().getName() == "ProximityOriented") {
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
                // 距離が1のエージェントだけを信頼エージェントとする
                if (dist == 1) agent.relAgents.addAll(tempList);
                tempList.clear();
                if (agent.relRanking.size() == AGENT_NUM - 1) {
                    i++;
                    dist = 0;
                }
            }
        }
        // 学習手法の場合
        else {
            Agent agent;
            for (int i = 0; i < AGENT_NUM; i++) {
                List<Agent> temp = new ArrayList<>(agents);
                int rand;
                Agent ag;
                agent = agents.get(i);
                while (temp.size() != 0) {
                    rand = randSeed.nextInt(temp.size());
                    ag = temp.remove(rand);
                    if (!ag.equals(agent)) agent.relRanking.add(ag);
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
        disposedTasks++;
        leader.ourTask = null;
    }

    // 引数のリーダーが持つタスクの終了
    static void finishTask(Agent leader) {
        leader.ourTask = null;
        if( leader.isLonely == 1 ) finishedTasksInDepopulatedArea++;
        finishedTasks++;
    }

    static List<Agent> getRoleList(List<Agent> agents, int role) {
        List<Agent> temp = new ArrayList<>();
        for (Agent ag : agents) {
            if (ag.role == role) temp.add(ag);
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

    private static void checkMessage(List<Agent> agents) {
        for (Agent ag : agents) ag.checkMessages(ag);
    }

    // 次のタームに行く前にあらゆるパラメータを初期化する部分
    private static void clearAll() {
        taskQueue.clear();
        agents = null;
        snapshot = null;
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
        if (strategy.getClass().getName() == "ProposedMethod") ProposedMethod.clearPM();
    }

    static private List<Agent> takeAgentsSnapshot(List<Agent> agents){
        List<Agent> temp = new ArrayList<>();
        // この時点でのリーダーエージェントをsnapshotとして残しておく
        int size = agents.size();
        Agent ag;

        for( Agent a : agents ){
            temp.add( a.clone() );
        }

        for( int i = 0; i < size; i++ ){
            ag = temp.remove(0);
            if( ag.e_leader > ag.e_member ) temp.add(ag);
        }
        System.out.println("turn :" + SNAPSHOT_TIME + ", Leaders: " + temp.size());
        assert size == agents.size() : "Deep copy failed";
        return temp;
    }
}
