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
import java.util.*;

import static main.research.SetParam.Role.*;

public class Manager implements SetParam {
    //TODO: こんな風にするならsingletonにしたほうがいいよね
    // TODO: lsとmsで分けて指定しなきゃいけないの無駄じゃない?
//        private static Strategy strategy = new PM2withRoleFixed();      // ICA2018における提案手法    //    private static main.research.strategy.Strategy strategy = new ProposedMethodForSingapore();
    private static String package_name = "main.research.agent.strategy.ProposedStrategy.";
    private static String ls_name = "ProposedStrategy_l";      // ICA2018における提案手法役割更新あり    //    private static main.research.strategy.Strategy strategy = new ProposedMethodForSingapore();
    private static String ms_name = "ProposedStrategy_m";
//    private static String package_name = "main.research.agent.strategy.ComparativeStrategy1.";
//    private static String ls_name = "ComparativeStrategy_l";
//    private static String ms_name = "ComparativeStrategy_m";
//    private static String package_name = "main.research.agent.strategy.ComparativeStrategy2.";
//    private static String ls_name = "ComparativeStrategy_l";
//    private static String ms_name = "ComparativeStrategy_m";

    private static int turn;
    private static List<Agent> snapshot = new ArrayList<>();

    public static void main(String[] args) {
        assert MAX_RELIABLE_AGENTS < AGENT_NUM : "alert0";
        assert INITIAL_TASK_NUM <= TASK_QUEUE_SIZE : "alert1";
        assert AGENT_NUM <= MAX_X * MAX_Y : "alert2";
//        assert COALITION_CHECK_SPAN < MAX_TURN_NUM : "alert3";
        assert !(IS_MORE_TASKS_HAPPENS && IS_HEAVY_TASKS_HAPPENS) : "alert4";


        try {
            int writeResultsSpan = MAX_TURN_NUM / WRITING_TIMES;

            String currentPath = System.getProperty("user.dir");

            FileWriter fw;
            BufferedWriter bw;
            PrintWriter pw;
            int start, end;

            int num = 0;
            String strategy_name = package_name.split( "\\." )[4];
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
                    if (HOW_EPSILON == "linear") Agent.renewEpsilonLinear();
                    else if (HOW_EPSILON == "exponential") Agent.renewEpsilonExponential();

                    if( getCurrentTime() == 500 || getCurrentTime() == MAX_TURN_NUM ) {
                        System.out.println( getCurrentTime() + String.format( ": %.2f", (double) TransmissionPath.sum / TransmissionPath.times ) );
                        TransmissionPath.sum   = 0;
                        TransmissionPath.times = 0;
                    }
//                    if( turn % 100 == 0 ) System.out.println( "turn: " + turn );
                    Task.addNewTasksToQueue();
                    AgentManager.JoneDoesSelectRole();

                    if (turn % writeResultsSpan == 0 && CHECK_RESULTS) {
                        OutPut.aggregateAgentData(AgentManager.getAllAgentList());
                    }
                    if (turn == SNAPSHOT_TIME && CHECK_INTERIM_RELATIONSHIPS) {
                        OutPut.writeGraphInformationX(AgentManager.getAllAgentList(), strategy_name );
//                        snapshot = takeAgentsSnapshot(AgentManager.getAgentList());
                        Agent.resetWorkHistory(AgentManager.getAllAgentList());
                    }
// */
                    TransmissionPath.transmit();                // 通信遅延あり
                    AgentManager.actLeadersAndMembers();

                    if (turn % writeResultsSpan == 0 && CHECK_RESULTS) {
                        int rmNum = Agent.countReciprocalMember(AgentManager.getAllAgentList());
                        OutPut.aggregateData( Task.getFinishedTasks(), Task.getDisposedTasks(), Task.getOverflowTasks(), rmNum, AgentManager.getAllAgentList());
                        OutPut.indexIncrement();
                        Task.setFinishedTasks( 0 );
                        Task.setDisposedTasks( 0 );
                        Task.setOverflowTasks( 0 );

                        if (CHECK_Eleader_Emember && turn % writeResultsSpan == 0) {
                            pw.print(turn + ", ");
                            for (Agent ag : AgentManager.getAllAgentList().subList(start, end)) {
                                pw.print(String.format("%.5f", ag.e_leader) + ", " + String.format("%.5f", ag.e_member) + ", ");
                            }
                            pw.println();
                        }
                    }
                    // ここが1tickの最後の部分．次のtickまでにやることあったらここで．
                }
                // ↑ 一回の実験のカッコ．以下は実験の合間で作業する部分
                if (CHECK_AGENTS) {
                  int leader_num = (int) AgentManager.getAllAgentList().stream()
                          .filter( agent -> agent.role == LEADER )
                          .count();
                  int member_num = (int) AgentManager.getAllAgentList().stream()
                          .filter( agent -> agent.role == MEMBER )
                          .count();
                    System.out.println("leaders:" + leader_num + ", members:" + member_num);
                    OutPut.aggregateDataOnce(AgentManager.getAllAgentList(), num);
                }
                if (num == EXECUTION_TIMES) break;
                clearAll();
            }
            // ↑ 全実験の終了のカッコ．以下は後処理
            if (CHECK_RESULTS) OutPut.writeResults( strategy_name );
//            main.research.OutPut.writeDelays(delays);
//            main.research.OutPut.writeReliabilities(AgentManager.getAgentList(), strategy_name);
//            main.research.OutPut.writeDelaysAndRels(delays, AgentManager.getAgentList(), strategy);
            if( CHECK_RELATIONSHIPS ) {
                OutPut.writeGraphInformationX(AgentManager.getAllAgentList(), strategy_name);
            }
// */
            if (CHECK_Eleader_Emember) pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 環境の準備に使うメソッド
    private static void initiate(int times) {
        setNewSfmt(times);
        setNewRnd(times);
		Task.initiateTaskQueue();

        AgentManager.initiateAgents( package_name, ls_name, ms_name);
        if (CHECK_INITIATION) {
            main.research.OutPut.checkAgent(AgentManager.getAllAgentList());
            main.research.OutPut.checkGrid(Grid.getGrid());
//            main.research.OutPut.checkDelay(Grid.getDelays());
//            main.research.OutPut.countDelays(Grid.getDelays());
        }
    }

    // 現在のターン数を返すメソッド
    public static int getCurrentTime() {
        return turn;
    }

    private static void clearAll() {
        snapshot = null;
        TransmissionPath.clear();
        Task.clear();
        Agent.clear();
        Strategy.clear();
        Grid.clear();
        AgentManager.clear();
    }
}
