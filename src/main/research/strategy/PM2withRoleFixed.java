package main.research.strategy;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.Message;
import main.research.random.MyRandom;
import main.research.task.AllocatedSubtask;
import main.research.task.Subtask;
import main.research.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


public class PM2withRoleFixed extends AbstractStrategy implements SetParam {
    static final double γ = γ_r;
    Map<Agent, AllocatedSubtask>[] teamHistory = new HashMap[AGENT_NUM];

    public PM2withRoleFixed() {
        for (int i = 0; i < AGENT_NUM; i++) {
            teamHistory[i] = new HashMap<>();
        }
    }


    void proposeAsL(Agent leader) {
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

    void replyAsM(Agent member) {
        // どのリーダーからの要請も受けないのならinactivate
        // どっかには参加するのなら交渉2フェイズへ
        if (member.mySubtask == null) {
            return;
        }
        member.phase = mPHASE2;
        member.validatedTicks = Manager.getTicks();
    }

    void reportAsL(Agent leader) {
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
                sortReliabilityRanking( leader.relRanking_m );
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
                if (leader.relRanking_l.get(A) < leader.relRanking_l.get(B)) {
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

    void receiveAsM(Agent member) {
        // サブタスクがもらえたなら実行フェイズへ移る.
        if (member.mySubtask != null) {
            member.leader = member.mySubtask.from;
            member.allocated[member.leader.id][member.mySubtask.resType]++;
            member.executionTime = member.calcExecutionTime(member, member.mySubtask);
            member.phase = PHASE3;
            member.validatedTicks = Manager.getTicks();
        }
        // サブタスクが割り当てられなかったら信頼度を0で更新し, inactivate
        else {
            inactivate(member, 0);
        }
    }

    void execute(Agent agent) {
        agent.executionTime--;
        agent.validatedTicks = Manager.getTicks();
        if (agent.executionTime == 0) {
            if (agent.role == LEADER) {
                if (agent._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    for (Agent ag : agent.teamMembers) {
                        agent.workWithAsL[ag.id]++;
                    }
                    agent.didTasksAsLeader++;
                }
            } else {
                agent.sendMessage(agent, agent.leader, DONE, 0);
                agent.myLeaders.remove(agent.leader);
                agent.required[agent.mySubtask.resType]++;

                renewDE( agent, agent.leader, (double) agent.mySubtask.reqRes[agent.mySubtask.resType] / (double) agent.calcExecutionTime(agent, agent.mySubtask));
                sortReliabilityRanking( agent.relRanking_m );

                if (agent._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    agent.workWithAsM[agent.leader.id]++;
                    agent.didTasksAsMember++;
                }
            }
            // 自分のサブタスクが終わったら役割適応度を1で更新して非活性状態へ
            inactivate(agent, 1);
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
                    int rankingSize = leader.relRanking_l.size();
                    for( Agent ag : leader.relRanking_l.keySet() ){
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

    /**
     * selectSolicitationsメソッド
     * メンバがどのリーダーの要請を受けるかを判断する
     * 信頼エージェントのリストにあるリーダーエージェントからの要請を受ける
     *
     */
    // 互恵主義と合理主義のどちらかによって行動を変える
    // TODO: 複数のサブタスクを許容するように変更する
    public void checkSolicitationsANDAllocations(Agent member) {
        List<Message> others = new ArrayList<>();
        List<Message> solicitations = new ArrayList<>();
        Message message;

        // 有効なメッセージがなければリターンする
        if (member.messages.size() == 0) return;

        // TODO: メッセージの分類
        while (member.messages.size() > 0) {
            message = member.messages.remove(0);
            if (message.getMessageType() == PROPOSAL) {
//                assert message.getFrom() != member.leader : message.getFrom().id + " to " + member.id +  "Duplicated";
                // すでにそのリーダーのチームに参加している場合は落とす
                if( member.haveAlreadyJoined(member, message.getFrom()) ){
                    member.sendMessage(member, message.getFrom(), REPLY, REJECT);
                }else {
                    solicitations.add(message);
                }
            } else {
                others.add(message);
            }
        }

        member.messages = others;
        int room = SUBTASK_QUEUE_SIZE - member.mySubtaskQueue.size(); // サブタスクキューの空き

        // サブタスクキューの空きがある限りsolicitationを選定する
        while ( member.tbd < room && solicitations.size() > 0) {
            // εグリーディーで選択する
            if (member.epsilonGreedy()) {
                Message target;
                Agent to;
                do {
                    int index = MyRandom.getRandomInt( 0, solicitations.size() - 1 );
                    target = solicitations.remove(index);
                    to = target.getFrom();
                }while( member.haveAlreadyJoined(member, to) );
                member.sendMessage(member, to, REPLY, ACCEPT);
                member.myLeaders.add(to);
            }
            // solicitationsの中から最も信頼するリーダーのsolicitを受ける
            else {
            int index = 0;
                int solicitationNum = solicitations.size();
                Message message1    = solicitations.get(0);
                Message message2;
                Agent   tempLeader  = message1.getFrom();
                Agent   temp;

                for (int i = 1; i < solicitationNum; i++) {
                    message2 = solicitations.get(i);
                    temp     = message2.getFrom();
                    // もし暫定信頼度一位のやつより信頼度高いやついたら, 暫定のやつを断って今のやつを暫定(ryに入れる
                    if (member.relRanking_m.get(tempLeader.id) < member.relRanking_m.get(temp)) {
                        tempLeader = temp;
                        index = i;
                    }
                }
            if (member.principle == RATIONAL) {
                Agent l = solicitations.remove(index).getFrom();
                member.sendMessage(member, l, REPLY, ACCEPT);
                member.myLeaders.add(l);
            } else {
                if (member.inTheList(tempLeader, (List) member.relRanking_m.keySet()) > -1) {
                    Agent l = solicitations.remove(index).getFrom();
                    member.sendMessage(member, l, REPLY, ACCEPT);
                    member.myLeaders.add(l);
                } else member.sendMessage(member, tempLeader, REPLY, REJECT);
            }
        }
            member.tbd++;
        }
        while(solicitations.size() > 0){
            message = solicitations.remove(0);
            member.sendMessage(member, message.getFrom(), REPLY, REJECT);
        }
        // TODO: othersへの対処．(othersとしては，RESULTが考えられる)
        Message result ;
        Subtask allocatedSubtask;
        while( others.size() > 0 ){
            member.tbd--;
            result  = others.remove(0);
            allocatedSubtask = result.getSubtask();
            assert result.getMessageType() == RESULT: "Leader Must Confuse Someone";
            if( allocatedSubtask == null ){   // 割り当てがなかった場合
                renewDE(member, result.getFrom(), 0);
                member.myLeaders.remove(result.getFrom());
            }else{    // 割り当てられた場合
                // TODO: すでにサブタスクを持っているならそれを優先して今もらったやつはキューに入れておく
                // TODO: さもなければキューに"入れずに"自分の担当サブタスクとする
                if( member.mySubtask == null ){
                    member.mySubtask = allocatedSubtask;
                }else{
                    member.mySubtaskQueue.add(allocatedSubtask);
                }
            }
        }
        assert member.mySubtaskQueue.size() <= SUBTASK_QUEUE_SIZE: member.mySubtaskQueue + " Overwork!";
    }

    @Override
    void renewDE(Agent from, Agent target, double evaluation) {
        assert !from.equals(target) : "alert4";

        if (from.role == LEADER) {
            double temp = from.relRanking_l.get(target);
            // 信頼度の更新式
            temp = temp * (1.0 - α) + α * evaluation;
            sortReliabilityRanking( from.relRanking_l );
        }
        else {
            double temp = from.relRanking_m.get(target);
            // 信頼度の更新式
            temp = temp * (1.0 - α) + α * evaluation;
            sortReliabilityRanking( from.relRanking_m );
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
                sortReliabilityRanking( ag.relRanking_l );
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
            checkSolicitationsANDAllocations(ag);
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
                ag.joined = false;
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
