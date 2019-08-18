package main.research;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.agent.strategy.Strategy;
import static main.research.others.random.MyRandom.*;
import main.research.communication.TransmissionPath;
import main.research.grid.Grid;
import main.research.task.Task;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static main.research.SetParam.Role.*;

public class Manager implements SetParam {
    //TODO: こんな風にするならsingletonにしたほうがいいよね
    // TODO: lsとmsで分けて指定しなきゃいけないの無駄じゃない?

//        private static Strategy strategy = new PM2withRoleFixed();      // ICA2018における提案手法    //    private static main.research.strategy.Strategy strategy = new ProposedMethodForSingapore();
    private static String ls_name = "ProposedStrategy_l";      // ICA2018における提案手法役割更新あり    //    private static main.research.strategy.Strategy strategy = new ProposedMethodForSingapore();
    private static String ms_name = "ProposedStrategy_m";

    private static Queue<Task> taskQueue;
    private static int disposedTasks = 0;
    private static int overflowTasks = 0;
    private static int finishedTasks = 0;
    private static int turn;
    private static List<Agent> snapshot = new ArrayList<>();

    public static void main(String[] args) {
        assert MAX_RELIABLE_AGENTS < AGENT_NUM : "alert0";
        assert INITIAL_TASK_NUM <= TASK_QUEUE_SIZE : "alert1";
        assert AGENT_NUM <= MAX_X * MAX_Y : "alert2";
        assert COALITION_CHECK_SPAN < MAX_TURN_NUM : "alert3";
        assert !(IS_MORE_TASKS_HAPPENS && IS_HEAVY_TASKS_HAPPENS) : "alert4";


        try {
            int writeResultsSpan = MAX_TURN_NUM / WRITING_TIMES;

            String currentPath = System.getProperty("user.dir");

            FileWriter fw;
            BufferedWriter bw;
            PrintWriter pw;
            int start, end;

            int num = 0;
            String strategy_name = ls_name.substring( 0, ( ls_name.length() - 2 ) );
            System.out.println(strategy_name);
            System.out.println( strategy_name + ", λ=" + ADDITIONAL_TASK_NUM +
                    ", ε:" + INITIAL_ε + ": " + HOW_EPSILON +
                    ", XF: " + MAX_RELIABLE_AGENTS +
                    ", Role_renewal: " + THRESHOLD_FOR_ROLE_RENEWAL +
                    ", From " + LocalDateTime.now()
            );

            if (CHECK_Eleader_Emember) {
                String fileName = strategy_name;
                fw = new FileWriter(currentPath + "/out/role" + fileName + ".csv", false);
                bw = new BufferedWriter(fw);
                pw = new PrintWriter(bw);
                start = 0;
                end = AGENT_NUM;
                for (int i = start; i < end; i++) {
                    pw.print(", " + i + ", " + " " + ", ");
                }
                pw.println();
                for (int i = start; i < end; i++) {
                    pw.print(" , e_leader, e_member, ");
                }
                pw.println();
            }

            // num回実験
            while (true) {
                initiate(num++);                         // シード，タスク，エージェントの初期化処理
                if (CHECK_INITIATION) {
                    if (num == EXECUTION_TIMES) break;
                    clearAll();
                    continue;
                }

                // ターンの進行
                for (turn = 1; turn <= MAX_TURN_NUM; turn++) {
                    // ターンの最初にεを調整する
                    // 最初は大きくしてトライアルを多くするともに，
                    // 徐々に小さくして安定させる
                    // 上が定数を引いて行くもので下が指数で減少させるもの．
                    // いずれも下限を設定できる
                    if (HOW_EPSILON == "linear") Agent.renewEpsilonLinear();
                    else if (HOW_EPSILON == "exponential") Agent.renewEpsilonExponential();

                    if( turn % 100 == 0 ) System.out.println( "turn: " + turn );
                    addNewTasksToQueue();
                    actFreeLancer();
                    if (turn % writeResultsSpan == 0 && CHECK_RESULTS) {
                        OutPut.aggregateAgentData(AgentManager.getAgentList());
                    }
//                    OutPut.checkTask(taskQueue);
                    if (turn == SNAPSHOT_TIME && CHECK_INTERIM_RELATIONSHIPS) {
                        OutPut.writeGraphInformationX(AgentManager.getAgentList(), strategy_name );
//                        snapshot = takeAgentsSnapshot(AgentManager.getAgentList());
                        Agent.resetWorkHistory(AgentManager.getAgentList());
                    }
// */
                    TransmissionPath.transmit();                // 通信遅延あり
                    actLeadersAndMembers();

                    if (turn % writeResultsSpan == 0 && CHECK_RESULTS) {
                        int rmNum = Agent.countReciprocalMember(AgentManager.getAgentList());
                        OutPut.aggregateData(finishedTasks, disposedTasks, overflowTasks, rmNum, AgentManager.getAgentList());
                        OutPut.indexIncrement();
                        finishedTasks = 0;
                        disposedTasks = 0;
                        overflowTasks = 0;

                        if (CHECK_Eleader_Emember && turn % writeResultsSpan == 0) {
                            pw.print(turn + ", ");
                            for (Agent ag : AgentManager.getAgentList().subList(start, end)) {
                                pw.print(String.format("%.5f", ag.e_leader) + ", " + String.format("%.5f", ag.e_member) + ", ");
                            }
                            pw.println();
                        }
                    }
                    // ここが1tickの最後の部分．次のtickまでにやることあったらここで．
                }
                // ↑ 一回の実験のカッコ．以下は実験の合間で作業する部分
                if (CHECK_AGENTS) {
                  int leader_num = (int) AgentManager.getAgentList().stream()
                          .filter( agent -> agent.role == LEADER )
                          .count();
                  int member_num = (int) AgentManager.getAgentList().stream()
                          .filter( agent -> agent.role == MEMBER )
                          .count();
                    System.out.println("leaders:" + leader_num + ", members:" + member_num);
                    OutPut.aggregateDataOnce(AgentManager.getAgentList(), num);
                }
                if (num == EXECUTION_TIMES) break;
                clearAll();
            }
            // ↑ 全実験の終了のカッコ．以下は後処理
            if (CHECK_RESULTS) OutPut.writeResults( strategy_name );
//            main.research.OutPut.writeDelays(delays);
//            main.research.OutPut.writeReliabilities(AgentManager.getAgentList(), strategy_name);
//            main.research.OutPut.writeDelaysAndRels(delays, AgentManager.getAgentList(), strategy);
            if (CHECK_RELATIONSHIPS) OutPut.writeGraphInformationX(AgentManager.getAgentList(), strategy_name);
// */
            if (CHECK_Eleader_Emember) pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 環境の準備に使うメソッド
    private static void initiate(int times) {
        // シードの設定
        setNewSfmt(times);
        setNewRnd(times);
        // タスクキューの初期化
        taskQueue = new LinkedList<>();
        for (int i = 0; i < INITIAL_TASK_NUM; i++) {
            taskQueue.add(new Task( MIN_SUBTASK_NUM, MAX_SUBTASK_NUM, MIN_DEADLINE, MAX_DEADLINE ) );
        }

        // エージェントの初期化
        AgentManager.initiateAgents(ls_name, ms_name);

        if (CHECK_INITIATION) {
            main.research.OutPut.checkAgent(AgentManager.getAgentList());
            main.research.OutPut.checkGrid(Grid.getGrid());
            main.research.OutPut.checkDelay(Grid.getDelays());
            main.research.OutPut.countDelays(Grid.getDelays());
        }
    }

    public static Agent getAgentRandomly(Agent self, List<Agent> exceptions, List<Agent> targets) {
        int random;
        Agent candidate;

    	do{
            random = getRandomInt(0, targets.size() - 1);
            candidate = targets.get(random);
        } while ( candidate.equals(self) || Agent.inTheList(candidate, exceptions) >= 0 );

        return candidate;
    }
    // taskQueueにあるタスクをリーダーに渡すメソッド
    public static Task getTask(Agent agent) {
        Task temp;
        temp = taskQueue.poll();
        if (temp != null) {
            temp.setFrom(agent);
        }
        return temp;
    }

    // 現在のターン数を返すメソッド
    public static int getCurrentTime() {
        return turn;
    }

    static void addNewTasksToQueue() {
        int room = TASK_QUEUE_SIZE - taskQueue.size();    // タスクキューの空き
        double decimalPart = ADDITIONAL_TASK_NUM % 1;
        int additionalTasksNum = (int) ADDITIONAL_TASK_NUM;

//            System.out.println(additionalTasksNum + ", " + decimalPart);

        if (decimalPart != 0) {
            double random = getRandomDouble();
            if (random < decimalPart) {
                additionalTasksNum++;
            }
//                System.out.println(additionalTasksNum + ", " + random);
        }

        // タスクキューに空きが十分にあるなら, 普通にぶち込む
        if (START_HAPPENS <= turn && turn < START_HAPPENS + BUSY_PERIOD && IS_MORE_TASKS_HAPPENS) {
            additionalTasksNum += HOW_MANY;
        }

        if (additionalTasksNum <= room) {
            if (START_HAPPENS <= turn && turn < START_HAPPENS + BUSY_PERIOD && IS_HEAVY_TASKS_HAPPENS) {
                for (int i = 0; i < additionalTasksNum; i++) {
                    taskQueue.add( new Task(8, 11, MIN_DEADLINE, MAX_DEADLINE ) );
                }
            }else{
                for (int i = 0; i < additionalTasksNum; i++) {
                    taskQueue.add( new Task( MIN_SUBTASK_NUM, MAX_SUBTASK_NUM, MIN_DEADLINE, MAX_DEADLINE ) );
                }
            }
        }
        // タスクキューからタスクがはみ出そうなら, 入れるだけ入れてはみ出る分はオーバーフローとする
        else {
            int i;
            if (START_HAPPENS <= turn && turn < START_HAPPENS + BUSY_PERIOD && IS_HEAVY_TASKS_HAPPENS) {
                for (i = 0; i < room; i++) {
                    taskQueue.add( new Task(8, 11, MIN_DEADLINE, MAX_DEADLINE ) );
                }
            } else {
                for (i = 0; i < room; i++) {
                    taskQueue.add( new Task( MIN_SUBTASK_NUM, MAX_SUBTASK_NUM, MIN_DEADLINE, MAX_DEADLINE ) );
                }
            }
            overflowTasks += additionalTasksNum - i;
        }
    }

    static void actFreeLancer() {
        List<Agent> freelancer;
        freelancer = getFreelancerList(AgentManager.getAgentList());
        actRandom(freelancer, "select role");
    }

    static void actLeadersAndMembers() {
        List<Agent> leaders = new ArrayList<>();
        List<Agent> members = new ArrayList<>();
        for (Agent ag : AgentManager.getAgentList()) {
            if (ag.role == MEMBER) members.add(ag);
            else if (ag.role == LEADER) leaders.add(ag);
        }
        actRandom(leaders, "act as leader");            // メンバの選定,要請の送信
        actRandom(members, "act as member");            // 要請があるメンバ達はそれに返事をする
    }

    private static void actRandom(List<Agent> agents, String command) {
        List<Agent> temp = new ArrayList<>(agents);
        int rand;
        if (command == "select role") {
            while ( ! temp.isEmpty() ) {
                rand = getRandomInt(0, temp.size() - 1);
                temp.remove(rand).selectRole();
            }
        } else if (command == "act as member") {
            while ( ! temp.isEmpty() ) {
                rand = getRandomInt(0, temp.size() - 1);
                temp.remove(rand).actAsMember();
            }
        } else if (command == "act as leader") {
            while ( ! temp.isEmpty() ) {
                rand = getRandomInt(0, temp.size() - 1);
                temp.remove(rand).actAsLeader();
            }
        }
        assert temp.size() == 0 : "Some agents do nothing.";
        temp.clear();
    }

    static public void disposeTask( ) {
        disposedTasks++;
    }

    public static void finishTask(  ) {
        finishedTasks++;
    }

    static List<Agent> getFreelancerList(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>();
        for ( Agent ag : agents ) {
            if (ag.role == JONE_DOE) temp.add(ag);
        }
        return temp;
    }

    private static void clearAll() {
        taskQueue.clear();
        snapshot = null;
        disposedTasks = 0;
        finishedTasks = 0;
        overflowTasks = 0;
        TransmissionPath.clear();
        Task.clearT();
        Agent.clear();
        Strategy.clear();
        Grid.clear();
        AgentManager.clear();
    }
}
