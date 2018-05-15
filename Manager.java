/**
 * @author Funato
 * @version 2.0
 */

import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.util.*;

public class Manager implements SetParam {
//    private static Strategy strategy = new PM2withRoleFixed();      // ICA2018における提案手法    //    private static Strategy strategy = new ProposedMethodForSingapore();
    private static Strategy strategy   = new Rational01();        // ICA2018における比較手法1
//    private static Strategy strategy = new CNP_area_restricted();   // ICA2018における比較手法2

//    private static Strategy strategy = new PM2();
//    private static Strategy strategy = new PM2withoutReciprocity();
//private static Strategy strategy = new PM2withoutReciprocalLeaderWithRoleFixed();
//    private static Strategy strategy = new PM2withoutReciprocalMemberWithRoleFixed();
//    private static Strategy strategy = new PMwithRoleFixed();
//    private static Strategy strategy = new PMwithRationalOnly();
//    private static Strategy strategy = new PMwithoutRoleRenewal();
//    private static Strategy strategy = new PMwithReallocation();
//    private static Strategy strategy   = new Rational();
//    private static Strategy strategy   = new RationalWithRoleRenewal();

    static private long _seed;
    private static Random _randSeed;

    static Queue<Task> taskQueue;
    static int[][] delays = new int[AGENT_NUM][AGENT_NUM];
    private static Agent[][] grids = new Agent[ROW][COLUMN];
    private static List<Agent> agents;
    static int disposedTasks = 0;
    static int overflowTasks = 0;
    static int finishedTasks = 0;
    static int finishedTasksInDepopulatedArea = 0;
    static int finishedTasksInPopulatedArea = 0;
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
            String currentPath = System.getProperty("user.dir");
            FileReader fr = new FileReader(currentPath + "/src/RandomSeed.txt");
            BufferedReader br = new BufferedReader(fr);
            String line;

            FileWriter fw;
            BufferedWriter bw;
            PrintWriter pw;
            int start, end;

            int num = 0;
            System.out.println(strategy.getClass().getName() + ", λ=" + ADDITIONAL_TASK_NUM  +
                    ", ε:" + INITIAL_ε + ": " + HOW_EPSILON +
                    ", XF: " + MAX_RELIABLE_AGENTS +
                    ", Role_renewal: " + THRESHOLD_FOR_ROLE_RENEWAL
            );

            if (CHECK_Eleader_Emember) {
                String fileName = strategy.getClass().getName();
                fw = new FileWriter(currentPath + "/out/role" + fileName + ".csv", false);
                bw = new BufferedWriter(fw);
                pw = new PrintWriter(bw);
                start = 0;
                end = 20;
                for (int i = start; i < end; i++) {
                    pw.print(i + ", " + " " + ", ");
                }
                pw.println();
                for (int i = start; i < end; i++) {
                    pw.print("e_leader, e_member, ");
                }
                pw.println();
            }

            // num回実験
            while ((line = br.readLine()) != null) {
                initiate(line);                         // シード，タスク，エージェントの初期化処理
                System.out.println(++num + "回目");
                if (CHECK_INITIATION) {
                    if (num == EXECUTION_TIMES) break;
                    clearAll();
                    continue;
                }

                // ターンの進行
                for (turn = 1; turn <= MAX_TURN_NUM; turn++) {
/*                    if( turn > 1000 ) {
                        System.out.println("===========================================================");
                        System.out.println("Turn: " + turn);
                    }
// */
                    // ターンの最初にεを調整する
                    // 最初は大きくしてトライアルを多くするともに，
                    // 徐々に小さくして安定させる
                    // 上が定数を引いて行くもので下が指数で減少させるもの．
                    // いずれも下限を設定できる
                    if (HOW_EPSILON == "linear") Agent.renewEpsilonLenear();
                    else if (HOW_EPSILON == "exponential") Agent.renewEpsilonExponential();

                    addNewTasksToQueue();
                    actFreeLancer();
                    assert Agent._leader_num + Agent._member_num == AGENT_NUM: "Ellegal role numbers, leaders:" + Agent._leader_num + ", members:" + Agent._member_num;
                    if (turn % writeResultsSpan == 0 && CHECK_RESULTS) {
                        OutPut.aggregateAgentData(agents);
                    }

                    if( turn == SNAPSHOT_TIME && CHECK_INTERIM_RELATIONSHIPS ){
                        OutPut.writeGraphInformationX(agents, strategy);
//                        snapshot = takeAgentsSnapshot(agents);
                        Agent.resetWorkHistory(agents);
                    }
// */
                    TransmissionPath.transmit();                // 通信遅延あり
                    checkMessage(agents);          // 要請の確認, 無効なメッセージに対する返信

                    actLeadersAndMembers();

                    if (turn % writeResultsSpan == 0 && CHECK_RESULTS) {
                        int rmNum = Agent.countReciprocalMember(agents);
                        OutPut.aggregateData(finishedTasks, disposedTasks, overflowTasks, rmNum, finishedTasksInDepopulatedArea, finishedTasksInPopulatedArea);
                        OutPut.indexIncrement();
                        finishedTasks = 0;
                        disposedTasks = 0;
                        overflowTasks = 0;
                        finishedTasksInDepopulatedArea = 0;
                        finishedTasksInPopulatedArea = 0;

                        if (CHECK_Eleader_Emember && turn % writeResultsSpan == 0) {
                            pw.print(turn + ", ");
                            for (Agent ag : agents.subList(start, end)) {
                                pw.print(String.format("%.5f", ag.e_leader) + ", " + String.format("%.5f", ag.e_member) + ", ");
                            }
                            pw.println();
                        }
                    }
                    // ここが1tickの最後の部分．次のtickまでにやることあったらここで．
                }
/*
                for( Agent agent: agents ){
                    if( agent.role == LEADER ) System.out.print( agent.id + "-l:" + agent.validatedTicks + ", ");
                    else if( agent.role == MEMBER ) System.out.print( agent.id + "-m:" + agent.validatedTicks + ", ");
                }
*/
                // ↑ 一回の実験のカッコ．以下は実験の合間で作業する部分
                if (CHECK_AGENTS) {
                    System.out.println("leaders:" + Agent._leader_num + ", members:" + Agent._member_num);
                    OutPut.aggregateDataOnce(agents, num);
                }
                if (num == EXECUTION_TIMES) break;
                clearAll();
            }
            // ↑ 全実験の終了のカッコ．以下は後処理
            System.out.println("leader role renewal: " + ProposedMethodForSingapore.leader_role_renewal / EXECUTION_TIMES);
            System.out.println("member role renewal: " + ProposedMethodForSingapore.member_role_renewal / EXECUTION_TIMES);
            if (CHECK_RESULTS) OutPut.writeResults(strategy);
            if (CHECK_AGENTS) OutPut.writeAgentsInformationX(strategy);
//            OutPut.writeDelays(delays);
//            OutPut.writeReliabilities(agents, strategy);
//            OutPut.writeDelaysAndRels(delays, agents, strategy);
            if (CHECK_RELATIONSHIPS) OutPut.writeGraphInformationX(agents, strategy);
// */
            if (CHECK_Eleader_Emember) pw.close();
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    // 環境の準備に使うメソッド
    private static void initiate(String line) {
        // シードの設定
        setSeed(line);
        // タスクキューの初期化
        taskQueue = new LinkedList<>();
        taskQueue.add(new Task(_seed));
        for (int i = 1; i < INITIAL_TASK_NUM; i++) taskQueue.add(new Task("NOT HEAVY"));

        // エージェントの初期化
        agents = generateAgents(strategy);
//
        if (CHECK_INITIATION) OutPut.checkAgent(agents);
//        OutPut.countDelays(delays);
//        OutPut.checkGrids(grids);
//        OutPut.checkDelay(delays);
//        OutPut.checkAgent(agents);
    }

    private static void setSeed(String line) {
        _seed = Long.parseLong(line);
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

    static Agent getAgentRandomly(Agent self, List<Agent> exceptions, List<Agent> targets) {
        int random = _randSeed.nextInt(targets.size());
        Agent candidate = targets.get(random);
        while (candidate.equals(self) || self.inTheList(candidate, exceptions) > 0) {
            random = _randSeed.nextInt(targets.size());
            candidate = targets.get(random);
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
        int quartile = AGENT_NUM / 4;
        int[] density = new int[AGENT_NUM];     // エージェント周辺の密度(距離x=2以下にいるエージェントの数)
        for (int i = 0; i < AGENT_NUM; i++) {
            int temp;
            for (int j = 0; j < AGENT_NUM; j++) {
                temp = calcManhattan(agents.get(i), agents.get(j));
                delays[i][j] = temp;
                if (temp <= 2) density[i]++;
            }
            if (density[i] < min) min = density[i];
            if (density[i] > max) max = density[i];
        }
        // 過疎エージェントの探索
        for (int neighborhoods_i = min; agents_in_depopulated_area < quartile; neighborhoods_i++) {
            for (int density_i = 0; density_i < AGENT_NUM; density_i++) {
                if (density[density_i] == neighborhoods_i) {
                    agents_in_depopulated_area++;
                    agents.get(density_i).isLonely = 1;
                }
            }
        }
        // 過密エージェントの探索
        for (int neighborhoods_i = max; agents_in_populated_area < quartile; neighborhoods_i--) {
            for (int density_i = 0; density_i < AGENT_NUM; density_i++) {
                if (density[density_i] == neighborhoods_i) {
                    agents_in_populated_area++;
                    agents.get(density_i).isAccompanied = 1;
                }
            }
        }

        // エリア制限手法において，知っているエージェントを定義する
        if (strategy.getClass().getName().endsWith("_area_restricted")) {
            Agent agent;
            System.out.println("area_restricted");
            // iが起点となるエージェント
            for (int i = 0; i < AGENT_NUM; ) {
                agent = agents.get(i);
                dist++;
                assert dist <= MAX_DELAY : "alert 8: " + dist;
                // 距離に基づいて信頼エージェントを設定する
                for (int j = 0; j < AGENT_NUM; j++) {
                    // 距離がdistなら既知エージェント候補とする
                    if (delays[i][j] == dist) {
                        tempList.add(agents.get(j));
                    }
                }
                // 近い方から100体のエージェントを既知エージェントとする
                // 100体に足りてなかったら距離を広げてもう一周
                if ( tempList.size() + agent.canReach.size() < AREA_LIMIT ) {
                    agent.canReach.addAll(tempList);
                    tempList.clear();
                }
                // ピッタリだったら次のエージェントへ
                else if( tempList.size() + agent.canReach.size() == AREA_LIMIT ){
                    agent.canReach.addAll(tempList);
                    tempList.clear();
                    i++;
                    dist = 0;
                }
                // はみ出たらtempListの中からはみ出ない分だけ選んで既知エージェントとする
                else{
                    int restSize = AREA_LIMIT - agent.canReach.size();
                    for( int j = 0; j < restSize; j++ ){
                        int rand = _randSeed.nextInt(tempList.size());
                        agent.canReach.add(tempList.remove(rand));
                    }
                    tempList.clear();
                    i++;
                    dist = 0;
                }
            }
        }

        // 信頼度ランキングをランダムに初期化
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

    static int inTheList(Agent a, List<Agent> agents) {
        for (int i = 0; i < agents.size(); i++) {
            if (a == agents.get(i)) return i;
        }
        return -1;
    }

    static int calcManhattan(Agent from, Agent to) {
        int distance = Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
        return (int) Math.ceil((double) (distance * MAX_DELAY) / (double) (ROW + COLUMN - 2));
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

    static void addNewTasksToQueue() {
            int room = TASK_QUEUE_SIZE - taskQueue.size();    // タスクキューの空き
            double decimalPart = ADDITIONAL_TASK_NUM%1;
            int additionalTasksNum = (int)ADDITIONAL_TASK_NUM;

//            System.out.println(additionalTasksNum + ", " + decimalPart);

            if( decimalPart != 0 ){
                double random = _randSeed.nextDouble();
                if (random < decimalPart) {
                    additionalTasksNum++;
                }
//                System.out.println(additionalTasksNum + ", " + random);
            }



        // タスクキューに空きが十分にあるなら, 普通にぶち込む
            if (additionalTasksNum <= room) {
                if( START_HAPPENS <= turn && turn < START_HAPPENS + BUSY_PERIOD && IS_HEAVY_TASKS_HAPPENS){
                    for (int i = 0; i < additionalTasksNum; i++) taskQueue.add(new Task("HEAVY"));
                }else{
                    for (int i = 0; i < additionalTasksNum; i++) taskQueue.add(new Task("NOT HEAVY"));
                }
            }
            // タスクキューからタスクがはみ出そうなら, 入れるだけ入れてはみ出る分はオーバーフローとする
            else {
                int i;
                    if( START_HAPPENS <= turn && turn < START_HAPPENS + BUSY_PERIOD && IS_HEAVY_TASKS_HAPPENS){
                        for (i = 0; i < room; i++) taskQueue.add(new Task("HEAVY"));
                    }else{
                        for (i = 0; i < room; i++) taskQueue.add(new Task("NOT HEAVY"));
                }
                overflowTasks += additionalTasksNum - i;
            }
    }

    static void actFreeLancer() {
        List<Agent> freelancer;
        freelancer = getFreelancerList(agents);
        actRandom(freelancer, "select role");
    }

    static void actLeadersAndMembers() {
        List<Agent> leaders = new ArrayList<>();
        List<Agent> members = new ArrayList<>();
        for (Agent ag : agents) {
            if (ag.role == MEMBER) members.add(ag);
            else if (ag.role == LEADER) leaders.add(ag);
        }
        actRandom(leaders, "act as leader");            // メンバの選定,　要請の送信
        actRandom(members, "act as member");            // 要請があるメンバ達はそれに返事をする
    }

    private static void actRandom(List<Agent> agents, String command) {
        List<Agent> temp = new ArrayList<>(agents);
        int rand;
        if (command == "select role") {
            while (temp.size() != 0) {
                rand = _randSeed.nextInt(temp.size());
                temp.remove(rand).selectRole();
            }
        } else if (command == "act as member") {
            while (temp.size() != 0) {
                rand = _randSeed.nextInt(temp.size());
                temp.remove(rand).actAsMember();
            }
        } else if (command == "act as leader") {
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
        if (CHECK_RESULTS) OutPut.aggregateTaskExecutionTime(leader);
        leader.ourTask = null;
/*        if( leader.isLonely == 1 )      finishedTasksInDepopulatedArea++;
        if( leader.isAccompanied == 1 ) finishedTasksInPopulatedArea++;
// */
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

    private static List<Agent> takeAgentsSnapshot(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>();
        // この時点でのリーダーエージェントをsnapshotとして残しておく
        int size = agents.size();
        Agent ag;
        int positiveAgents = 0, mPositiveAgents = 0;

        for (Agent a : agents) {
            temp.add(a.clone());
        }

        for (int i = 0; i < size; i++) {
            ag = temp.remove(0);
            if (ag.e_leader > ag.e_member) {
                temp.add(ag);
                for (double rel : ag.reliabilities) {
                    if (rel > 0) positiveAgents++;
                }
/*                System.out.println("positiveAgents: " + positiveAgents);
                mPositiveAgents += positiveAgents;
// */
            }
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
        strategy.clearStrategy();
    }

    static public List<Agent> getAgents() {
        return agents;
    }

}
