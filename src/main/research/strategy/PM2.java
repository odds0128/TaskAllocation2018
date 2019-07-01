package main.research.strategy;

import main.research.*;
import main.research.agent.Agent;
import main.research.communication.Message;
import main.research.task.AllocatedSubTask;
import main.research.task.SubTask;
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


public class PM2 implements Strategy, SetParam {
    static final double γ = γ_r;
    Map<Agent, AllocatedSubTask>[] teamHistory = new HashMap[AGENT_NUM];

    PM2() {
        for (int i = 0; i < AGENT_NUM; i++) {
            teamHistory[i] = new HashMap<>();
        }
    }

    public void actAsLeader(Agent agent) {
        if (agent.phase == PROPOSITION) proposeAsL(agent);
        else if (agent.phase == REPORT) reportAsL(agent);
        else if (agent.phase == EXECUTION) execute(agent);
        agent.relAgents_l = decreaseDEC(agent);
    }

    public void actAsMember(Agent agent) {
        if (agent.phase == REPLY) replyAsM(agent);
        else if (agent.phase == RECEPTION) receiveAsM(agent);
        else if (agent.phase == EXECUTION) execute(agent);
        agent.relAgents_m = decreaseDEC(agent);
    }

    private void proposeAsL(Agent leader) {
        leader.ourTask = Manager.getTask(leader);
        if (leader.ourTask == null) {
            leader.inactivate(0);
            return;
        }
        leader.ourTask.setFrom(leader);
        leader.restSubTask = leader.ourTask.subTaskNum;                       // 残りサブタスク数を設定
        leader.executionTime = -1;
        leader.candidates = selectMembers(leader, leader.ourTask.subTasks);   // メッセージ送信
        if (leader.candidates == null) {
            leader.candidates = new ArrayList<>();
            Manager.disposeTask(leader);
            leader.inactivate(0);
            return;
        } else {
            for (int i = 0; i < leader.candidates.size(); i++) {
                if (leader.candidates.get(i) != null) {
                    leader.proposalNum++;
                    leader.sendMessage(leader, leader.candidates.get(i), PROPOSAL, leader.ourTask.subTasks.get(i % leader.restSubTask));
                }
            }
        }
        leader.nextPhase();  // 次のフェイズへ
    }

    private void replyAsM(Agent member) {
        if (member.mySubTask == null) {
            if (Manager.getTicks() - member.validatedTicks > THRESHOLD_FOR_ROLE_RENEWAL) {
                member.inactivate(0);
            }
            return;     // メッセージをチェック
        }
        // どのリーダーからの要請も受けないのならinactivate
        member.phase = mPHASE2;
        member.validatedTicks = Manager.getTicks();
    }

    private void reportAsL(Agent leader) {
        if (leader.replies.size() != leader.proposalNum) return;
        Agent from;
        for (Message reply : leader.replies) {
            // 拒否ならそのエージェントを候補リストから外し, 信頼度を0で更新する
            if (reply.getReply() != ACCEPT) {
                from = reply.getFrom();
                int i = leader.inTheList(from, leader.candidates);
                assert i >= 0 : "alert: Leader got reply from a ghost.";
                leader.candidates.set(i, null);
                leader.relAgents_l = renewRel(leader, from, 0);
            }
        }
        Agent A, B;

        // if 全candidatesから返信が返ってきてタスクが実行可能なら割り当てを考えていく
        for (int indexA = 0, indexB = leader.restSubTask; indexA < leader.restSubTask; indexA++, indexB++) {
            A = leader.candidates.get(indexA);
            B = leader.candidates.get(indexB);
            // 両方ダメだったらオワコン
            if (A == null && B == null) {
                continue;
            }
            // もし両方から受理が返ってきたら, 信頼度の高い方に割り当てる
            else if (A != null && B != null) {
                // Bの方がAより信頼度が高い場合
                if (leader.reliabilities_l[A.id] < leader.reliabilities_l[B.id]) {
                    leader.preAllocations.put(B, leader.ourTask.subTasks.get(indexA));
                    leader.sendMessage(leader, A, RESULT, null);
                    leader.teamMembers.add(B);
                }
                // Aの方がBより信頼度が高い場合
                else {
                    leader.preAllocations.put(A, leader.ourTask.subTasks.get(indexA));
                    leader.sendMessage(leader, B, RESULT, null);
                    leader.teamMembers.add(A);
                }
            }
            // もし片っぽしか受理しなければそいつがチームメンバーとなる
            else {
                // Bだけ受理してくれた
                if (A == null) {
                    leader.preAllocations.put(B, leader.ourTask.subTasks.get(indexA));
                    leader.teamMembers.add(B);
                }
                // Aだけ受理してくれた
                else {
                    leader.preAllocations.put(A, leader.ourTask.subTasks.get(indexA));
                    leader.teamMembers.add(A);
                }
            }
        }
        // 未割り当てが残っていないのなら実行へ
        if (leader.teamMembers.size() == leader.ourTask.subTaskNum) {
            for (Agent tm : leader.teamMembers) {
                teamHistory[leader.id].put(tm, new AllocatedSubTask(leader.preAllocations.get(tm), Manager.getTicks(), leader.ourTask.task_id));
                leader.sendMessage(leader, tm, RESULT, leader.preAllocations.get(tm));
            }
            if( leader.executionTime < 0 ){
                if (leader._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    for (Agent ag : leader.teamMembers) {
                        leader.workWithAsL[ag.id]++;
                    }
                }
                leader.pastTasks.add(leader.ourTask);
                leader.ourTask = null;
                leader.inactivate(1);
                return;
            }else {
                leader.nextPhase();
                return;
            }
        }
        // 未割り当てのサブタスクが残っていれば失敗
        else {
            for (Agent tm : leader.teamMembers) {
                leader.sendMessage(leader, tm, RESULT, null);
            }
            Manager.disposeTask(leader);
            leader.inactivate(-1);
            return;
        }
    }

    private void receiveAsM(Agent member) {
        // サブタスクがもらえたなら実行フェイズへ移る.
        if (member.mySubTask != null) {
            member.leader = member.mySubTask.from;
            member.allocated[member.leader.id][member.mySubTask.resType]++;
            member.executionTime = member.calcExecutionTime(member, member.mySubTask);
            member.phase = PHASE3;
            member.validatedTicks = Manager.getTicks();
        }
        // サブタスクが割り当てられなかったら信頼度を0で更新し, inactivate
        else {
            member.inactivate(0);
        }
    }

    private void execute(Agent agent) {
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
                agent.required[agent.mySubTask.resType]++;
                agent.relAgents_m = renewRel(agent, agent.leader, (double) agent.mySubTask.reqRes[agent.mySubTask.resType] / (double) agent.calcExecutionTime(agent, agent.mySubTask));
                if (agent._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    agent.workWithAsM[agent.leader.id]++;
                    agent.didTasksAsMember++;
                }
            }
            // 自分のサブタスクが終わったら役割適応度を1で更新して非活性状態へ
            agent.inactivate(1);
        }
    }

    /**
     * selectMembersメソッド
     * 優先度の高いエージェントから(すなわち添字の若い信頼エージェントから)選択する
     * ε-greedyを導入
     *
     * @param subtasks
     */
    public List<Agent> selectMembers(Agent leader, List<SubTask> subtasks) {
        List<Agent> memberCandidates = new ArrayList<>();
        Agent candidate = null;
        List<SubTask> skips = new ArrayList<>();  // 互恵エージェントがいるために他のエージェントに要請を送らないサブタスクを格納

        for (SubTask st : subtasks) {
            for (int i = 0; i < RESEND_TIMES; i++) {
                memberCandidates.add(null);
            }
        }

        List<Agent> exceptions = new ArrayList<>();
        // 一つのタスクについてRESEND_TIMES周する
        for (int i = 0; i < RESEND_TIMES; i++) {
            SubTask st;
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
                        candidate = Manager.getAgentRandomly(leader, exceptions, Manager.getAgents());
                    } while (leader.calcExecutionTime(candidate, st) < 0);
                } else {
                    // 信頼度ランキングの上から割り当てを試みる．
                    // 1. 能力的にできない
                    // 2. すでにチームに参加してもらっていて，まだ終了連絡がこない
                    // 3. すでに別のサブタスクを割り当てる予定がある
                    // に当てはまらなければ割り当て候補とする．
                    int rankingSize = leader.relRanking_l.size();
                    for (int relRank = 0; relRank < rankingSize; relRank++) {
                        candidate = leader.relRanking_l.get(relRank);
                        // 上記1~3の項目を満たさないか確認する
                        // 満たしていなければ要請を送る確定の候補とし，ループから抜ける
                        if (leader.inTheList(candidate, exceptions) < 0 &&
                                leader.calcExecutionTime(candidate, st) > 0) {
                            break;
                        }
                        // 満たしていれば，候補として相応しくないので次に行く．
                        else {
                            candidate = null;
                        }
                    }
                }
                // 候補が見つかれば，チーム参加要請対象者リストに入れ，参加要請を送る
                if (!(candidate == null)) {
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
     * selectLeaderメソッド
     * メンバがどのリーダーの要請を受けるかを判断する
     * 信頼エージェントのリストにあるリーダーエージェントからの要請を受ける
     */
    // 互恵主義と合理主義のどちらかによって行動を変える
    public void checkSolicitationsANDAllocations(Agent member) {
        List<Message> others = new ArrayList<>();
        List<Message> solicitations = new ArrayList<>();
        Message message;


        // 有効なメッセージがなければリターンする
        if (member.messages.size() == 0) return;

        // メッセージの分類
        while (member.messages.size() > 0) {
            message = member.messages.remove(0);
            if (message.getMessageType() == PROPOSAL) {
                // assert message.getFrom() != member.leader : message.getFrom().id + " to " + member.id +  "Duplicated";
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
        int room = SUBTASK_QUEUE_SIZE - member.mySubTaskQueue.size(); // サブタスクキューの空き

        // サブタスクキューの空きがある限りsolicitationを選定する
        while ( member.tbd < room && solicitations.size() > 0) {
            // εグリーディーで選択する
            if (member.epsilonGreedy()) {
                Message target;
                Agent to;
                do {
                    int index = Agent._randSeed.nextInt(solicitations.size());
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
                    if (member.reliabilities_m[tempLeader.id] < member.reliabilities_m[temp.id]) {
                        tempLeader = temp;
                        index = i;
                    }
                }
                if (member.principle == RATIONAL) {
                    Agent l = solicitations.remove(index).getFrom();
                    member.sendMessage(member, l, REPLY, ACCEPT);
                    member.myLeaders.add(l);
                } else {
                    if (member.inTheList(tempLeader, member.relAgents_m) > -1) {
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
        // othersへの対処．(othersとしては，RESULTが考えられる)
        Message result ;
        SubTask allocatedSubtask;
        while( others.size() > 0 ){
            member.tbd--;
            result  = others.remove(0);
            allocatedSubtask = result.getSubTask();
            assert result.getMessageType() == RESULT: "Leader Must Confuse Someone";
            if( allocatedSubtask == null ){   // 割り当てがなかった場合
                renewRel(member, result.getFrom(), 0);
                member.myLeaders.remove(result.getFrom());
            }else{    // 割り当てられた場合
                // すでにサブタスクを持っているならそれを優先して今もらったやつはキューに入れておく
                // さもなければキューに"入れずに"自分の担当サブタスクとする
                if( member.mySubTask == null ){
                    member.mySubTask = allocatedSubtask;
                }else{
                    member.mySubTaskQueue.add(allocatedSubtask);
                }
            }
        }
        assert member.mySubTaskQueue.size() <= SUBTASK_QUEUE_SIZE: member.mySubTaskQueue + " Overwork!";
    }

    /**
     * renewRelメソッド
     * 信頼度を更新し, 同時に信頼エージェントも更新する
     * agentのtargetに対する信頼度をevaluationによって更新し, 同時に信頼エージェントを更新する
     *
     * @param agent
     */
    private List<Agent> renewRel(Agent agent, Agent target, double evaluation) {
        assert !agent.equals(target) : "alert4";

        if (agent.role == LEADER) {
            double temp = agent.reliabilities_l[target.id];
            // 信頼度の更新式
            agent.reliabilities_l[target.id] = temp * (1.0 - α) + α * evaluation;

        /*
         信頼エージェントの更新
         信頼度rankingを更新し, 上からMAX_REL_AGENTS分をrelAgentに放り込む
        //   */
            // 信頼度が下がった場合と上がった場合で比較の対象を変える
            // 上がった場合は順位が上のやつと比較して
            if (evaluation > 0) {
                int index = agent.inTheList(target, agent.relRanking_l) - 1;    // targetの現在順位の上を持ってくる
                while (index > -1) {
                    // 順位が上のやつよりも信頼度が高くなったなら
                    if (agent.reliabilities_l[agent.relRanking_l.get(index).id] < agent.reliabilities_l[target.id]) {
                        Agent tmp = agent.relRanking_l.get(index);
                        agent.relRanking_l.set(index, target);
                        agent.relRanking_l.set(index + 1, tmp);
                        index--;
                    } else {
                        break;
                    }
                }
            }
            // 下がった場合は下のやつと比較して入れ替えていく
            else {
                int index = agent.inTheList(target, agent.relRanking_l) + 1;    // targetの現在順位の下を持ってくる
                while (index < AGENT_NUM - 1) {
                    // 順位が下のやつよりも信頼度が低くなったなら
                    if (agent.reliabilities_l[agent.relRanking_l.get(index).id] > agent.reliabilities_l[target.id]) {
                        Agent tmp = agent.relRanking_l.get(index);
                        agent.relRanking_l.set(index, target);
                        agent.relRanking_l.set(index - 1, tmp);
                        index++;
                    } else {
                        break;
                    }
                }
            }
            // 信頼度を更新したら改めて信頼エージェントを設定する
            List<Agent> tmp = new ArrayList<>();
            Agent ag;
            double threshold;

            threshold = agent.threshold_for_reciprocity_as_leader;

            for (int j = 0; j < MAX_RELIABLE_AGENTS; j++) {
                ag = agent.relRanking_l.get(j);
                if (agent.reliabilities_l[ag.id] > threshold) {
                    tmp.add(ag);
                } else {
                    break;
                }
            }
            return tmp;
        } else {
            double temp = agent.reliabilities_m[target.id];


            // 信頼度の更新式
            agent.reliabilities_m[target.id] = temp * (1.0 - α) + α * evaluation;

        /*
         信頼エージェントの更新
         信頼度rankingを更新し, 上からMAX_REL_AGENTS分をrelAgentに放り込む
        //   */
            // 信頼度が下がった場合と上がった場合で比較の対象を変える
            // 上がった場合は順位が上のやつと比較して
            if (evaluation > 0) {
                int index = agent.inTheList(target, agent.relRanking_m) - 1;    // targetの現在順位の上を持ってくる
                while (index > -1) {
                    // 順位が上のやつよりも信頼度が高くなったなら
                    if (agent.reliabilities_m[agent.relRanking_m.get(index).id] < agent.reliabilities_m[target.id]) {
                        Agent tmp = agent.relRanking_m.get(index);
                        agent.relRanking_m.set(index, target);
                        agent.relRanking_m.set(index + 1, tmp);
                        index--;
                    } else {
                        break;
                    }
                }
            }
            // 下がった場合は下のやつと比較して入れ替えていく
            else {
                int index = agent.inTheList(target, agent.relRanking_m) + 1;    // targetの現在順位の下を持ってくる
                while (index < AGENT_NUM - 1) {
                    // 順位が下のやつよりも信頼度が低くなったなら
                    if (agent.reliabilities_m[agent.relRanking_m.get(index).id] > agent.reliabilities_m[target.id]) {
                        Agent tmp = agent.relRanking_m.get(index);
                        agent.relRanking_m.set(index, target);
                        agent.relRanking_m.set(index - 1, tmp);
                        index++;
                    } else {
                        break;
                    }
                }
            }
            // 信頼度を更新したら改めて信頼エージェントを設定する
            List<Agent> tmp = new ArrayList<>();
            Agent ag;
            double threshold;

            threshold = agent.threshold_for_reciprocity_as_member;

            for (int j = 0; j < MAX_RELIABLE_AGENTS; j++) {
                ag = agent.relRanking_m.get(j);
                if (agent.reliabilities_m[ag.id] > threshold) {
                    tmp.add(ag);
                } else {
                    break;
                }
            }
            return tmp;
        }
    }

    /**
     * decreaseDECメソッド
     * 過去の学習を忘れるために毎ターン信頼度を減らすメソッド
     * それに伴い, 信頼エージェントだったエージェントの信頼度が閾値を割る可能性があるので,
     * そのチェックをして必要に応じて信頼エージェントを更新する
     * @param agent
     */
    private List<Agent> decreaseDEC(Agent agent) {
        double temp;

        if (agent.role == LEADER) {
            for (int i = 0; i < AGENT_NUM; i++) {
                temp = agent.reliabilities_l[i] - γ;
                if (temp < 0) agent.reliabilities_l[i] = 0;
                else agent.reliabilities_l[i] = temp;
            }
            // 信頼度を更新したら改めて信頼エージェントを設定する
            List<Agent> tmp = new ArrayList<>();
            Agent ag;
            double threshold;
            threshold = agent.threshold_for_reciprocity_as_leader;

            for (int j = 0; j < MAX_RELIABLE_AGENTS; j++) {
                ag = agent.relRanking_l.get(j);
                if (agent.reliabilities_l[ag.id] > threshold) {
                    tmp.add(ag);
                } else {
                    break;
                }
            }
            return tmp;
        } else {
            for (int i = 0; i < AGENT_NUM; i++) {
                temp = agent.reliabilities_m[i] - γ;
                if (temp < 0) agent.reliabilities_m[i] = 0;
                else agent.reliabilities_m[i] = temp;
            }
            // 信頼度を更新したら改めて信頼エージェントを設定する
            List<Agent> tmp = new ArrayList<>();
            Agent ag;
            double threshold;
            threshold = agent.threshold_for_reciprocity_as_member;

            for (int j = 0; j < MAX_RELIABLE_AGENTS; j++) {
                ag = agent.relRanking_m.get(j);
                if (agent.reliabilities_m[ag.id] > threshold) {
                    tmp.add(ag);
                } else {
                    break;
                }
            }
            return tmp;
        }
    }
    private void setPrinciple(Agent agent) {
        if (agent.role == MEMBER) {
            if (agent.relAgents_m.size() > 0 && agent.e_member >= THRESHOLD_FOR_ROLE_RECIPROCITY) {
                if (agent.principle == RATIONAL) {
                    Agent._recipro_num++;
                    Agent._rational_num--;
                }
                agent.principle = RECIPROCAL;
            } else {
                if (agent.principle == RECIPROCAL) {
                    Agent._recipro_num--;
                    Agent._rational_num++;
                }
                agent.principle = RATIONAL;
            }
        } else if (agent.role == LEADER) {
            if (agent.relAgents_l.size() > 0 && agent.e_leader >= THRESHOLD_FOR_ROLE_RECIPROCITY) {
                if (agent.principle == RATIONAL) {
                    Agent._recipro_num++;
                    Agent._rational_num--;
                }
                agent.principle = RECIPROCAL;
            } else {
                if (agent.principle == RECIPROCAL) {
                    Agent._recipro_num--;
                    Agent._rational_num++;
                }
                agent.principle = RATIONAL;
            }
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
                AllocatedSubTask as = teamHistory[ag.id].remove(m.getFrom());
                if (as == null) {
                    System.out.println(Manager.getTicks() + ": " + m.getFrom() + " asserts he did " + ag.id + "'s subtask ");
                }

                int rt = Manager.getTicks() - as.getAllocatedTime();
                int reward = as.getRequiredResources() * 5;
                ag.relAgents_l = renewRel(ag, m.getFrom(), (double) reward / rt);

                // タスク全体が終わったかどうかの判定と，それによる処理
                /*
                1. 終わったサブタスクがどのタスクのものだったか確認する
                2. そのサブタスクを履歴から除く
                3. もしそれによりタスク全体のサブタスクが0になったら終了とみなす
                 */
                Task task = ag.identifyTask(as.getTaskId());
                task.subTasks.remove(as.getSt());
                if( task.subTasks.size() == 0 ) {
                    ag.pastTasks.remove(task);
                    Manager.finishTask(ag, task);
                    ag.didTasksAsLeader++;
                }
            } else {
                ag.messages.add(m); // 違うメッセージだったら戻す
            }
        }

        // solicitを受けるか判断する
        if (ag.role == MEMBER) {
            checkSolicitationsANDAllocations(ag);
        } else {
            size = ag.messages.size();
            if (size == 0) return;
            //        System.out.println("ID: " + self.id + ", Phase: " + self.phase + " message:  "+ self.messages);
            // リーダーでPROPOSITION or 誰でもEXECUTION → 誰からのメッセージも期待していない
            if (ag.phase == PROPOSITION || ag.phase == EXECUTION) {
                for (int i = 0; i < size; i++) {
                    m = ag.messages.remove(0);
                    ag.sendNegative(ag, m.getFrom(), m.getMessageType(), m.getSubTask());
                }
            }
            // リーダーでREPORT → REPLYを期待している
            else if (ag.phase == REPORT) {
                for (int i = 0; i < size; i++) {
                    m = ag.messages.remove(0);
                    Agent from = m.getFrom();
                    if (m.getMessageType() == REPLY && ag.inTheList(from, ag.candidates) > -1) ag.replies.add(m);
                    else ag.sendNegative(ag, m.getFrom(), m.getMessageType(), m.getSubTask());
                }
            }
        }
    }

    public void clearStrategy() {
        for (int i = 0; i < AGENT_NUM; i++) {
            teamHistory[i].clear();
        }
    }
}
