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
// private static Strategy strategy = new ProposedMethod();     // 最短終了時間優先信頼度更新式
//    private static Strategy strategy = new ComparativeMethod1();   // 最短応答優先手法
//    private static Strategy strategy = new ComparativeMethod2();     // 最短応答優先信頼度更新で役割更新なし
    private static Strategy strategy = new ComparativeMethod3();   // 最短応答優先信頼度更新で役割更新あり
//    private static Strategy strategy = new RoundRobin();           // ラウンドロビン

    static private long    _seed ;
    private static Random _randSeed;

    static Queue<Task> taskQueue;
    static int[][] delays = new int[AGENT_NUM][AGENT_NUM];
    private static Agent[][] grids = new Agent[ROW][COLUMN];
    private static List<Agent> agents;
    static int disposedTasks = 0;
    static int overflowTasks = 0;
    static int finishedTasks = 0;
    static int finishedTasksInDepopulatedArea = 0;
    static int finishedTasksInPopulatedArea   = 0;
    static int turn;
    static List<Agent> snapshot = new ArrayList<>();

    public static void main(String[] args) {
        assert MAX_RELIABLE_AGENTS < AGENT_NUM : "alert0";
        assert INITIAL_TASK_NUM <= TASK_QUEUE_SIZE : "alert1";
        assert AGENT_NUM <= ROW * COLUMN : "alert2";
        assert COALITION_CHECK_SPAN < MAX_TURN_NUM : "a";

        try {
            int writeResultsSpan = MAX_TURN_NUM / WRITING_TIMES;

            // seedの読み込み
            String currentPath  = System.getProperty("user.dir");
            FileReader fr = new FileReader(currentPath + "/src/RandomSeed.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;

            int num = 0;
            System.out.println(strategy.getClass().getName() + ", λ=" + ADDITIONAL_TASK_NUM);

            // num回実験
            while ((line = br.readLine()) != null) {
                initiate(line);                         // シード，タスク，エージェントの初期化処理
                System.out.println( ++num + "回目");
                if( CHECK_INITIATION ){
                    clearAll();
                    continue;
                }
                // ターンの進行
                for (turn = 1; turn <= MAX_TURN_NUM; turn++) {
//                    System.out.println("===========================================================");
//                    System.out.println("Turn: " + turn);

                    addNewTasksToQueue();
                    actFreeLancer();
                    if (turn % writeResultsSpan == 0) {
                        OutPut.aggregateAgentData(agents);
                    }

                    if( turn == SNAPSHOT_TIME && CHECK_RELATIONSHIPS ){
                        snapshot = takeAgentsSnapshot(agents);
                        OutPut.writeGraphInformation(agents, "interim_report");
                        Agent.resetWorkHistory(agents);
                    }

                    TransmissionPath.transmit();                // 通信遅延あり
                    checkMessage(agents);          // 要請の確認, 無効なメッセージに対する返信

                    actLeadersAndMembers();

                    if (turn % writeResultsSpan == 0) {
                        int rmNum = Agent.countReciprocalMember(agents);
                        OutPut.aggregateData(finishedTasks, disposedTasks, overflowTasks, rmNum, finishedTasksInDepopulatedArea, finishedTasksInPopulatedArea);
                        OutPut.indexIncrement();
                        finishedTasks = 0; disposedTasks = 0; overflowTasks = 0; finishedTasksInDepopulatedArea = 0; finishedTasksInPopulatedArea = 0;
                    }
                // ここが1tickの最後の部分．次のtickまでにやることあったらここで．
                }
                // ↑ 一回の実験のカッコ．以下は実験の合間で作業する部分
                if (num == EXECUTION_TIMES) break;
                clearAll();
            }
            // ↑ 全実験の終了のカッコ．以下は後処理
            OutPut.writeResults(strategy);
//            OutPut.writeDelays(delays);
//            OutPut.writeReliabilities(agents, strategy);
            OutPut.writeDelaysAndRels(delays, agents, strategy);
            if( CHECK_RELATIONSHIPS ) OutPut.writeGraphInformation(agents, "graph" + strategy.getClass().getName());
// */
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }


    // 環境の準備に使うメソッド
    private static void initiate(String line){
        // シードの設定
        setSeed(line);
        // タスクキューの初期化
        taskQueue = new LinkedList<>();
        taskQueue.add(new Task(_seed));
        for (int i = 1; i < INITIAL_TASK_NUM; i++) taskQueue.add(new Task());

        // エージェントの初期化
        agents = generateAgents(strategy);
//        OutPut.countDelays(delays);
//        OutPut.checkGrids(grids);
    }
    private static void setSeed( String line ){
        _seed     = Long.parseLong(line);
        _randSeed = new Random(_seed);
    }
    private static List<Agent> generateAgents(Strategy strategy) {
        Agent temp;
        List tempList = new ArrayList();
        int randX = _randSeed.nextInt(COLUMN);
        int randY = _randSeed.nextInt(ROW);
        while (checkDuplication(randX, randY) == false) {
            randX = _randSeed.nextInt(COLUMN);
            randY = _randSeed.nextInt(ROW);
        }
        temp = new Agent(_seed, randX, randY, strategy);
        grids[temp.y][temp.x] = temp;
        tempList.add(temp);
        for (int i = 1; i < AGENT_NUM; i++) {
            while (checkDuplication(randX, randY) == false) {
                randX = _randSeed.nextInt(COLUMN);
                randY = _randSeed.nextInt(ROW);
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
        int random = _randSeed.nextInt(agents.size());
        Agent candidate = agents.get(random);
        while (candidate.equals(self) || self.inTheList(candidate, exceptions) > 0) {
            random = _randSeed.nextInt(agents.size());
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
                delays[i][j] = temp;
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
        else if (strategy.getClass().getName() == "RoundRobin" || strategy.getClass().getName() == "ComparativeMethod1") {
            Agent agent;
            for (int i = 0; i < AGENT_NUM; ) {
                agent = agents.get(i);
                dist++;
                assert dist < 11 : "alert 8: " + dist;
                // 距離に基づいて信頼エージェントを設定する
                for (int j = 0; j < AGENT_NUM; j++) {
                    // 距離がdistなら信頼エージェント候補とする
                    if (delays[i][j] == dist) {
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
                    rand = _randSeed.nextInt(temp.size());
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
        return (int) Math.ceil((double) (distance * MAX_DELAY) / (double) (ROW + COLUMN - 2) );
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

    static void addNewTasksToQueue(){
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
    }
    static void actFreeLancer(){
        List<Agent> freelancer;
        freelancer = getFreelancerList(agents);
        actRandom(freelancer, "select role");
    }
    static void actLeadersAndMembers(){
        List<Agent> leaders = new ArrayList<>();
        List<Agent> members = new ArrayList<>();
        for (Agent ag : agents) {
            if (ag.role == MEMBER)     members.add(ag);
            else if(ag.role == LEADER) leaders.add(ag);
        }
        actRandom(leaders, "act as leader");            // メンバの選定,　要請の送信
        actRandom(members, "act as member");            // 要請があるメンバ達はそれに返事をする
    }
    private static void actRandom(List<Agent> agents, String command) {
        List<Agent> temp = new ArrayList<>(agents);
        int rand;
        if( command == "select role" ) {
            while (temp.size() != 0) {
                rand = _randSeed.nextInt(temp.size());
                temp.remove(rand).selectRole();
            }
        }else if( command == "act as member" ){
            while (temp.size() != 0) {
                rand = _randSeed.nextInt(temp.size());
                temp.remove(rand).actAsMember();
            }
        }else if( command == "act as leader" ){
            while (temp.size() != 0) {
                rand = _randSeed.nextInt(temp.size());
                temp.remove(rand).actAsLeader();
            }
        }
        temp.clear();

    }

    static void disposeTask(Agent leader) {
        disposedTasks++;
        leader.ourTask = null;
    }
    static void finishTask(Agent leader) {
//         OutPut.checkTeam(leader);
        leader.ourTask = null;
        if( leader.isLonely == 1 )      finishedTasksInDepopulatedArea++;
        if( leader.isAccompanied == 1 ) finishedTasksInPopulatedArea++;
        finishedTasks++;
    }
    static List<Agent> getFreelancerList(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>();
        for (Agent ag : agents) {
            if (ag.role == JONE_DOE) temp.add(ag);
        }
        return temp;
    }
    private static void checkMessage(List<Agent> agents) {
        for (Agent ag : agents) ag.checkMessages(ag);
    }
    private static  List<Agent> takeAgentsSnapshot(List<Agent> agents){
        List<Agent> temp = new ArrayList<>();
        // この時点でのリーダーエージェントをsnapshotとして残しておく
        int size = agents.size();
        Agent ag;
        int positiveAgents = 0, mPositiveAgents = 0;

        for( Agent a : agents ){
            temp.add( a.clone() );
        }

        for( int i = 0; i < size; i++ ){
            ag = temp.remove(0);
            if( ag.e_leader > ag.e_member ){
                temp.add(ag);
                for( double  rel: ag.reliabilities ){
                    if( rel > 0 ) positiveAgents++;
                }
/*                System.out.println("positiveAgents: " + positiveAgents);
                mPositiveAgents += positiveAgents;
// */            }
        }
//        System.out.println("turn :" + SNAPSHOT_TIME + ", Leaders: " + temp.size() + ", Positive Agents: " + mPositiveAgents);
        assert size == agents.size() : "Deep copy failed";
        return temp;
    }

    private static void clearAll() {
        taskQueue.clear();
        agents = null;
        snapshot = null;
        disposedTasks = 0;
        finishedTasks = 0;
        overflowTasks = 0;
        for (int i = 0; i < AGENT_NUM; i++) {
            for (int j = 0; j < AGENT_NUM; j++) {
                delays[i][j] = 0;
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
        if (strategy.getClass().getName() == "ComparativeMethod3") ComparativeMethod3.clearPM();
    }
}
