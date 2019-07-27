package main.research.strategy.ComparativeStrategy;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.Message;
import main.research.strategy.LeaderStrategy;
import main.research.task.AllocatedSubtask;
import main.research.task.Subtask;
import main.research.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ProposedMethodForSingapore クラス
 * サブタスクの要求リソースに量を設けたことにより，"報酬"を適切に考える必要が生じた
 * そこで報酬はサブタスクの要求リソースに依存するものとし，信頼度更新式 e^i_j = (1-α)e^i_j + δ * α を
 * 1. j(自分)がメンバでi(相手)がリーダーの場合，
 * 　a. 成功(サブタスク割り当て)時　　　　　　　　δ = そのサブタスクの要求リソース / 実行時間
 * 　b. 失敗(サブタスクみ割り当て)時　　　　　　　δ = 0
 * 2. j(自分)がリーダーでi(相手)がメンバの場合，
 * 　a. 成功(チーム編成成功)後，終了連絡受理時　　δ = そのサブタスクの要求リソース / 応答時間(= メッセージ往復時間 + サブタスク実行時間)
 * 　b. 失敗(チーム参加要請拒否受理)時，　　　　　δ = 0
 * によって更新する．
 * 役割更新機構あり
 * メンバが互恵かどうかを気にするバージョン
 */


public class FixedLeader extends LeaderStrategy implements SetParam {

    static {
        for (int i = 0; i < AGENT_NUM; i++) {
            teamHistory[i] = new HashMap<>(HASH_MAP_SIZE);
        }
    }


    protected void proposeAsL(Agent leader) {
        leader.ourTask = Manager.getTask(leader);
        if (leader.ourTask == null) {
            inactivate(leader, 0);
            return;
        }
//        leader.ourTask.setFrom(leader);
        leader.restSubtask = leader.ourTask.subtasks.size();                       // 残りサブタスク数を設定
        leader.executionTime = -1;
        leader.candidates = selectMembers(leader, leader.ourTask.subtasks);   // メッセージ送信
        if (leader.candidates == null) {
            leader.candidates = new ArrayList<>();
            inactivate(leader, 0);
            return;
        } else {
            Agent candidate;
            for (int i = 0; i < leader.candidates.size(); i++) {
                candidate = leader.candidates.get(i);
                if (candidate != null) {
                    leader.proposalNum++;
                    leader.agentsCommunicatingWith.add(candidate);
                    leader.sendMessage(leader, candidate, PROPOSAL, leader.ourTask.subtasks.get(i % leader.restSubtask));
                }
            }
        }
        leader.phase = lPHASE2;
        leader.validatedTicks = Manager.getTicks();
    }

    protected void reportAsL(Agent leader) {
        if (leader.replies.size() != leader.proposalNum) return;
        Agent from;
        for (Message reply : leader.replies) {
            // 拒否ならそのエージェントを候補リストから外し, 信頼度を0で更新する
            if (reply.getReply() != ACCEPT) {
                from = reply.getFrom();
                int i = leader.inTheList(from, leader.candidates);
                assert i >= 0 : "alert: Leader got reply from a ghost.";
                leader.candidates.set(i, null);
                renewDE( leader, from, 0);
                sortReliabilityRanking( leader.reliabilityRankingAsM);
            }
        }
        Agent A, B;
        // if 全candidatesから返信が返ってきてタスクが実行可能なら割り当てを考えていく
        for (int indexA = 0, indexB = leader.restSubtask; indexA < leader.restSubtask; indexA++, indexB++) {
            A = leader.candidates.get(indexA);
            B = leader.candidates.get(indexB);
            // 両方ダメだったらオワコン
            if (A == null && B == null) {
                continue;
            }

            // もし両方から受理が返ってきたら, 信頼度の高い方に割り当てる
            else if (A != null && B != null) {
                // Bの方がAより信頼度が高い場合
                if (leader.reliabilityRankingAsL.get(A) < leader.reliabilityRankingAsL.get(B)) {
                    leader.preAllocations.put(B, leader.ourTask.subtasks.get(indexA));
                    leader.sendMessage(leader, A, RESULT, null);
                    leader.teamMembers.add(B);
                }
                // Aの方がBより信頼度が高い場合
                else {
                    leader.preAllocations.put(A, leader.ourTask.subtasks.get(indexA));
                    leader.sendMessage(leader, B, RESULT, null);
                    leader.teamMembers.add(A);
                }
            }
            // もし片っぽしか受理しなければそいつがチームメンバーとなる
            else {
                // Bだけ受理してくれた
                if (A == null) {
                    leader.preAllocations.put(B, leader.ourTask.subtasks.get(indexA));
                    leader.teamMembers.add(B);
                }
                // Aだけ受理してくれた
                else {
                    leader.preAllocations.put(A, leader.ourTask.subtasks.get(indexA));
                    leader.teamMembers.add(A);
                }
            }
        }
        // 未割り当てが残っていないのなら実行へ
        if (leader.teamMembers.size() == leader.ourTask.subtasks.size()) {
            leader.pastTasks.add(leader.ourTask);
            for (Agent tm : leader.teamMembers) {
                teamHistory[leader.id].put(tm, new AllocatedSubtask(leader.preAllocations.get(tm), Manager.getTicks(), leader.ourTask.task_id));
                leader.sendMessage(leader, tm, RESULT, leader.preAllocations.get(tm));

                leader.agentsCommunicatingWith.add(tm);
            }
            if (leader.executionTime < 0) {
                if (leader._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    for (Agent ag : leader.teamMembers) {
                        leader.workWithAsL[ag.id]++;
                    }
                }
                inactivate(leader, 1);
                return;
            } else {
                leader.phase = PHASE3;
                leader.validatedTicks = Manager.getTicks();
                return;
            }
        }
        // 未割り当てのサブタスクが残っていれば失敗
        else {
            for (Agent tm : leader.teamMembers) {
                leader.sendMessage(leader, tm, RESULT, null);
            }
            Manager.disposeTask(leader);
            inactivate(leader, 0);
            return;
        }
    }

    /**
     * selectMembersメソッド
     * 優先度の高いエージェントから(すなわち添字の若い信頼エージェントから)選択する
     * ε-greedyを導入
     *
     * @param subtasks
     */
    public List<Agent> selectMembers(Agent leader, List<Subtask> subtasks) {
        List<Agent> memberCandidates = new ArrayList<>();
        Agent candidate = null;
        List<Subtask> skips = new ArrayList<>();  // 互恵エージェントがいるために他のエージェントに要請を送らないサブタスクを格納

        for (Subtask st : subtasks) {
            for (int i = 0; i < RESEND_TIMES; i++) {
                memberCandidates.add(null);
            }
        }
        List<Agent> exceptions = new ArrayList<>();
        // 一つのタスクについてRESEND_TIMES周する
        for (int i = 0; i < RESEND_TIMES; i++) {
            Subtask st;
            for (int stIndex = 0; stIndex < subtasks.size(); stIndex++) {
                st = subtasks.get(stIndex);
                // すでにそのサブタスクを互恵エージェントに割り当てる予定ならやめて次のサブタスクへ
                if (leader.inTheList(st, skips) >= 0) {
                    continue;
                }
//                System.out.print(st);
                // 一つ目のサブタスク(報酬が最も高い)から割り当てていく
                // 信頼度の一番高いやつから割り当てる
                // εの確率でランダムに割り振る
                if (leader.epsilonGreedy()) {
                    do {
                        candidate = Manager.getAgentRandomly(leader, exceptions, AgentManager.getAgentList());
                    } while ( leader.calcExecutionTime(candidate, st) < 0);
                } else {
                    // 信頼度ランキングの上から割り当てを試みる．
                    // 1. 能力的にできない
                    // 2. すでにチームに参加してもらっていて，まだ終了連絡がこない
                    // 3. すでに別のサブタスクを割り当てる予定がある
                    // に当てはまらなければ割り当て候補とする．
                    int rankingSize = leader.reliabilityRankingAsL.size();
                    for( Agent ag : leader.reliabilityRankingAsL.keySet() ){
                        if( leader.inTheList( ag, exceptions ) < 0 &&
                                leader.calcExecutionTime( ag, st ) > 0 ) {
                            candidate = ag;
                            break;
                        }
                        else {
                            candidate = null;
                        }
                    }
                }
                // 候補が見つかれば，チーム参加要請対象者リストに入れ，参加要請を送る
                if (! (candidate == null) ) {
                    exceptions.add(candidate);
                    memberCandidates.set(stIndex + i * subtasks.size(), candidate);
                }
                // 候補が見つからないサブタスクがあったら直ちにチーム編成を失敗とする
                else {
                    System.out.println("It can't be executed.");
                    return null;
                }
            }
        }
        return memberCandidates;
    }

    @Override
    protected void renewDE(Agent from, Agent target, double evaluation) {
        assert !from.equals(target) : "alert4";

        if (from.role == LEADER) {
            double temp = from.reliabilityRankingAsL.get(target);
            // 信頼度の更新式
            temp = temp * (1.0 - α) + α * evaluation;
            sortReliabilityRanking( from.reliabilityRankingAsL);
        }
        else {
            double temp = from.reliabilityRankingAsM.get(target);
            // 信頼度の更新式
            temp = temp * (1.0 - α) + α * evaluation;
            sortReliabilityRanking( from.reliabilityRankingAsM);
        }
    }

    public void checkMessages(Agent ag) {
        int size = ag.messages.size();
        Message m;
        // メンバからの作業完了報告をチェックする
        for (int i = 0; i < size; i++) {
            m = ag.messages.remove(0);
            ag.agentsCommunicatingWith.remove(m.getFrom());
            if (m.getMessageType() == DONE) {
                // 「リーダーとしての更新式で」信頼度を更新する
                // そのメンバにサブタスクを送ってからリーダーがその完了報告を受けるまでの時間
                // すなわちrt = "メンバのサブタスク実行時間 + メッセージ往復時間"
                AllocatedSubtask as = teamHistory[ag.id].remove(m.getFrom());
                if (as == null) {
                    System.out.println(Manager.getTicks() + ": " + m.getFrom() + " asserts he did " + ag.id + "'s subtask ");
                }
                int rt = Manager.getTicks() - as.getAllocatedTime();
                int reward = as.getRequiredResources() * 5;

                renewDE( ag, m.getFrom(), (double) reward/rt );
                sortReliabilityRanking( ag.reliabilityRankingAsL);
                // TODO: タスク全体が終わったかどうかの判定と，それによる処理
                /*
                1. 終わったサブタスクがどのタスクのものだったか確認する
                2. そのサブタスクを履歴から除く
                3. もしそれによりタスク全体のサブタスクが0になったら終了とみなす
                 */
                Task task = ag.identifyTask(as.getTaskId());
                task.subtasks.remove(as.getSt());
                if( task.subtasks.size() == 0 ) {
                    ag.pastTasks.remove(task);
                    Manager.finishTask(ag, task);
                    ag.didTasksAsLeader++;
                }

            } else {
                ag.messages.add(m); // 違うメッセージだったら戻す
            }
        }
        // TODO: solicitを受けるか判断する
        if (ag.role == MEMBER) {
        } else {
            size = ag.messages.size();
            if (size == 0) return;
            // リーダーでPROPOSITION or 誰でもEXECUTION → 誰からのメッセージも期待していない
            if (ag.phase == PROPOSITION || ag.phase == EXECUTION) {
                for (int i = 0; i < size; i++) {
                    m = ag.messages.remove(0);
                    ag.sendNegative(ag, m.getFrom(), m.getMessageType(), m.getSubtask());
                }
            }
            // リーダーでREPORT → REPLYを期待している
            else if (ag.phase == REPORT) {
                for (int i = 0; i < size; i++) {
                    m = ag.messages.remove(0);
                    Agent from = m.getFrom();
                    if (m.getMessageType() == REPLY && ag.inTheList(from, ag.candidates) > -1) ag.replies.add(m);
                    else ag.sendNegative(ag, m.getFrom(), m.getMessageType(), m.getSubtask());
                }
            }
        }
    }

    void inactivate(Agent ag, int success) {
        if (ag.role == LEADER) {
            ag.phase = lPHASE1;
            ag.teamMembers.clear();        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
            ag.ourTask = null;
            ag.candidates.clear();
            ag.replyNum = 0;
            ag.replies.clear();
            ag.results.clear();
            ag.preAllocations.clear();
            ag.restSubtask = 0;
            ag.proposalNum = 0;
        } else if (ag.role == MEMBER) {
            if (ag.mySubtaskQueue.size() > 0) {
                //FIXME: どうやらサブタスクがキューから除かれずにmySubtaskとしてしまうときがある
                assert ag.mySubtask != ag.mySubtaskQueue.get(0) : "Same subtask";
                ag.mySubtask = ag.mySubtaskQueue.remove(0);
                ag.executionTime = ag.calcExecutionTime(ag, ag.mySubtask);
                ag.leader = ag.mySubtask.from;
                ag.phase = EXECUTION;
            } else {
                ag.mySubtask = null;
				ag.phase = mPHASE1;
            }
        }
        ag.validatedTicks = Manager.getTicks();
    }

    public void clearStrategy() {
        for (int i = 0; i < AGENT_NUM; i++) {
            teamHistory[i].clear();
        }
    }
}
