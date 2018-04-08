import java.util.ArrayList;
import java.util.List;

/**
 * ProposedMethodA_2クラス
 * 信頼度更新式は最短終了応答優先
 * 役割更新機構なし
 */
public class ProposedMethodA_2 implements SetParam, Strategy {
    static final double γ = γ_r;
    static int[] min = new int[AGENT_NUM];

    ProposedMethodA_2() {
        for (int i = 0; i < AGENT_NUM; i++) {
            min[i] = Integer.MAX_VALUE;
        }
    }

    // */
    public void actAsLeader(Agent agent) {
        setPrinciple(agent);
        if (agent.phase == PROPOSITION) proposeAsL(agent);
        else if (agent.phase == REPORT) reportAsL(agent);
        else if (agent.phase == EXECUTION) execute(agent);
        decreaseDEC(agent);
    }

    public void actAsMember(Agent agent) {
        setPrinciple(agent);
        if (agent.phase == REPLY) replyAsM(agent);
        else if (agent.phase == RECEPTION) receiveAsM(agent);
        else if (agent.phase == EXECUTION) execute(agent);
        decreaseDEC(agent);
    }

    private void proposeAsL(Agent leader) {
        leader.ourTask = Manager.getTask();
        if (leader.ourTask == null) {
            leader.inactivate(0);
            return;
        }
        leader.restSubTask = leader.ourTask.subTaskNum;                       // 残りサブタスク数を設定
        leader.selectSubTask();
        leader.candidates = selectMembers(leader, leader.ourTask.subTasks);   // メッセージ送信
        leader.start = Manager.getTicks();
        leader.nextPhase();  // 次のフェイズへ
    }

    private void replyAsM(Agent member) {
        if (member.messages.size() == 0) return;     // メッセージをチェック
        member.leader = selectLeader(member, member.messages);
        if (member.leader != null) {
            member.joined = true;
//            System.out.println("ID: "+ member.id + ", my leader is " + member.leader.id );
            member.sendMessage(member, member.leader, REPLY, ACCEPT);
        }
        // どのリーダーからの要請も受けないのならinactivate
        // どっかには参加するのなら交渉2フェイズへ
        if (member.joined) {
            member.totalOffers++;
            member.start = Manager.getTicks();
            member.nextPhase();
        }
        // どこにも参加しないのであれば, 役割適正値を更新するようにする
        else {
            member.inactivate(0);
        }
// */
    }

    private void reportAsL(Agent leader) {
        // 有効なReplyメッセージがなければreturn
        if (leader.replies.size() == 0) return;
        // 2017/12/06 ICARRTに揃えるために, チーム編成が成功してからサブタスクの実行指示をだすことに

        Agent from;
        for (Message reply : leader.replies) {
            leader.replyNum++;
            // ただの拒否ならそのエージェントを候補リストから外し, 信頼度を0で更新する
            if (reply.getReply() == REJECT) {
                from = reply.getFrom();
                int i = leader.inTheList(from, leader.candidates);
                assert i >= 0 : "alert: Leader got reply from a ghost.";
                leader.candidates.set(i, null);
                leader.relAgents = renewRel(leader, from, 0);
            }
            // 自分がかつて割り振ったサブタスクをまだしているのが理由なら信頼度は下げない
            else if( reply.getReply() == REJECT_FOR_DOING_YOUR_ST ){
                from = reply.getFrom();
                int i = leader.inTheList(from, leader.candidates);
                assert i >= 0 : "alert: Leader got reply from a ghost.";
                leader.candidates.set(i, null);
            }
        }
        int index;
        Agent A, B;
        List<Agent> losers = new ArrayList<>();
        List<SubTask> reallocations = new ArrayList<>();

        // if 全candidatesから返信が返ってきてタスクが実行可能なら割り当てを考えていく
        if (leader.replyNum == leader.candidates.size()) {
            for (int i = 0; i < leader.restSubTask; i++) {
                index = 2 * i;
                A = leader.candidates.get(index);
                B = leader.candidates.get(index + 1);
                // 両方ダメだったら再割り当てを考える
                if (A == null && B == null) {
                    reallocations.add(leader.ourTask.subTasks.get(i));
                    continue;
                }
                // もし両方から受理が返ってきたら, 信頼度の高い方に割り当てる
                else if (A != null && B != null) {
                    // Bの方がAより信頼度が高い場合
                    if (leader.reliabilities[A.id] < leader.reliabilities[B.id]) {
                        leader.candidates.set(index + 1, null);
                        losers.add(A);
                        leader.allocations.add(new Allocation(B, leader.ourTask.subTasks.get(i)));
                        leader.teamMembers.add(B);
                    }
                    // Aの方がBより信頼度が高い場合
                    else {
                        leader.candidates.set(index, null);
                        losers.add(B);
                        leader.allocations.add(new Allocation(A, leader.ourTask.subTasks.get(i)));
                        leader.teamMembers.add(A);
                    }
                }
                // もし片っぽしか受理しなければそいつがチームメンバーとなる
                else {
                    // Bだけ受理してくれた
                    if (A == null) {
                        leader.candidates.set(index + 1, null);
                        leader.allocations.add(new Allocation(B, leader.ourTask.subTasks.get(i)));
                        leader.teamMembers.add(B);
                    }
                    // Aだけ受理してくれた
                    else {
                        leader.candidates.set(index, null);
                        leader.allocations.add(new Allocation(A, leader.ourTask.subTasks.get(i)));
                        leader.teamMembers.add(A);
                    }
                }
            }
            // 未割り当てのサブタスクがあっても最後のチャンス
            SubTask st;
            Agent lo;
            int flag;
            if (reallocations.size() > 0 && losers.size() > 0) {
                // 未割り当てのサブタスクひとつひとつに対して
                for (int i = 0; i < reallocations.size(); i++) {
                    flag = 0;
                    st = reallocations.remove(0);
                    // 受理を返したのに競合のせいでサブタスクが割り当てられなかったいい奴らの中から割り当てを探す
                    for (int j = 0; j < losers.size(); j++) {
                        lo = losers.remove(0);
                        if (leader.calcExecutionTime(lo, st) > 0) {
                            leader.restSubTask--;
                            leader.allocations.add(new Allocation(lo, st));
                            leader.teamMembers.add(lo);
                            flag++;
                            break;
                        } else {
                            losers.add(lo);
                        }
                    }
                    // 誰にもできなかったら
                    if (flag == 0) reallocations.add(st);
                }
            }

            // 未割り当てが残っていないのならば負け犬どもに引導を渡して実行へ
            if (reallocations.size() == 0) {
                for (Agent tm : leader.teamMembers) {
                    leader.sendMessage(leader, tm, RESULT, leader.getAllocation(tm).getSubtask());
                }
                for (Agent ls : losers) {
                    leader.sendMessage(leader, ls, RESULT, null);
                }
                leader.prevTeamMember.addAll(leader.teamMembers);
//                System.out.print(leader.id + ", " + leader.prevTeamMember + "+");
//                System.out.println(leader.teamMembers);
                Manager.finishTask(leader);
                leader.nextPhase();
            }
            // 未割り当てのサブタスクが残っているにもかかわらず再割当先の候補がなければ失敗
            else {
                for (Agent tm : leader.teamMembers) {
                    leader.sendMessage(leader, tm, RESULT, null);
                }
                for (Agent ls : losers) {
                    leader.sendMessage(leader, ls, RESULT, null);
                }
                Manager.disposeTask(leader);
                leader.inactivate(0);
                return;
            }
        }
        leader.replies.clear();
    }

    private void receiveAsM(Agent member) {
        // リーダーからの返事が来るまで待つ
        if (member.messages.size() == 0) return;
        Message message;
        message = member.messages.remove(0);
        member.mySubTask = message.getSubTask();

        int rt = Manager.getTicks() - member.start;
        if (rt < min[member.id]) min[member.id] = rt;
/*
        member.totalResponseTicks += rt;
        member.meanRT = (double)member.totalResponseTicks/(double)member.totalOffers;
// */
        // サブタスクがもらえたなら信頼度をプラスに更新し, 実行フェイズへ移る.
        if (member.mySubTask != null) {
            member.start = Manager.getTicks();
            member.relAgents = renewRel(member, member.leader, 1);
            member.executionTime = member.calcExecutionTime(member, member.mySubTask);
            member.nextPhase();
        }
        // サブタスクが割り当てられなかったら信頼度を0で更新し, inactivate
        else {
            //       System.out.println(" ID: " + member.id + ", " + member.leader + " is unreliable");
            member.relAgents = renewRel(member, member.leader, -(double) rt / (double) min[member.id]);
            member.inactivate(0);
        }
    }

    private void execute(Agent agent) {
        agent.executionTime--;
        if (agent.executionTime == 0) {
            if (agent.role == LEADER) {
                if (agent._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN)
                    for (Agent ag : agent.teamMembers) agent.workWithAsL[ag.id]++;
            } else {
                agent.sendMessage(agent, agent.leader, DONE, agent.start);
                if (agent._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN)
                    agent.workWithAsM[agent.leader.id]++;
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
        List<Agent> temp = new ArrayList<>();
        Agent candidate;
        SubTask subtask;

        for (int i = 0; i / RESEND_TIMES < subtasks.size(); i++) {
            subtask = subtasks.get(i / RESEND_TIMES);
            if (leader.epsilonGreedy() ) {
                candidate = Manager.getAgentRandomly(leader, temp );
            }
            else {
                int j = 0;
                while (true) {
                    // エージェント1から全走査
                    candidate = leader.relRanking.get(j++);
                    // そいつがまだ候補に入っていなくて， さらにそのサブタスクをこなせそうなら
                    if ( leader.inTheList(candidate, temp) < 0 &&
                            leader.calcExecutionTime(candidate, subtask) > 0) {
                        break;
                    }
                }
            }
            temp.add(candidate);
            leader.sendMessage(leader, candidate, PROPOSAL, subtask.resType);
        }
        return temp;
    }

    /**
     * selectLeaderメソッド
     * メンバがどのリーダーの要請を受けるかを判断する
     * 信頼エージェントのリストにあるリーダーエージェントからの要請を受ける
     */
    // 互恵主義と合理主義のどちらかによって行動を変える
    public Agent selectLeader(Agent member, List<Message> messages) {
        int size = messages.size();
        Message message;
        Agent myLeader = null;
        Agent from;
        Agent temp;

        // 有効なメッセージがなければリターンする
        if (size == 0) return null;

        // あったらεグリーディーで選択する
        if (member.epsilonGreedy()) {
            myLeader = messages.remove(member._randSeed.nextInt(messages.size())).getFrom();
            for (int i = 0; i < size - 1; i++) {
                member.sendMessage(member, messages.remove(0).getFrom(), REPLY, REJECT);
            }
        }
        // messageキューに溜まっている参加要請を確認し, 参加するチームを選ぶ
        else {
            message = messages.remove(0);
            temp = message.getFrom();
            for (int i = 0; i < size - 1; i++) {
                message = messages.remove(0);
                from = message.getFrom();
                // もし暫定信頼度一位のやつより信頼度高いやついたら, 暫定のやつを断って今のやつを暫定(ryに入れる
                if (member.reliabilities[temp.id] < member.reliabilities[from.id]) {
                    member.sendMessage(member, temp, REPLY, REJECT);
                    temp = from;
                }
                // 暫定一位がその座を守れば挑戦者を断る
                else {
                    member.sendMessage(member, from, REPLY, REJECT);
                }
            }
            if (member.principle == RATIONAL) {
                myLeader = temp;
            } else {
                if (member.inTheList(temp, member.relAgents) > -1) {
                    myLeader = temp;
                } else member.sendMessage(member, temp, REPLY, REJECT);
            }
        }
        return myLeader;
    }

    /**
     * renewRelメソッド
     * 信頼度を更新し, 同時に信頼エージェントも更新する
     * agentのtargetに対する信頼度をevaluationによって更新し, 同時に信頼エージェントを更新する
     * evaluationは正(プラスへの信頼度更新)と0(普通にマイナスに更新)と負(ペナルティを課して更新)がある.
     * よって, この式の中で政府をいじる必要はないことに注意
     *
     * @param agent
     */
    private List<Agent> renewRel(Agent agent, Agent target, double evaluation) {
        assert evaluation <= 1 : "evaluation too big";
        assert !agent.equals(target) : "alert4";
        double temp = agent.reliabilities[target.id];
//        if( Manager.getTicks() % 10000 == 0 ) System.out.println( evaluation );
        // 信頼度の更新式
        if (agent.role == LEADER) {
            agent.reliabilities[target.id] = temp * (1.0 - α) + α * evaluation;
        } else {
            if (evaluation >= 0) agent.reliabilities[target.id] = temp * (1.0 - α) + α * evaluation;
            else agent.reliabilities[target.id] = temp * (1.0 + α * evaluation);
        }

        assert agent.reliabilities[target.id] <= 1.0 : "Illegal reliability renewal ... Turn: " + Manager.getTicks() + ", ID: " + agent.id + ", target: " + target.id + ", change: " + temp + " → " + agent.reliabilities[target.id];

        /*
         信頼エージェントの更新
         信頼度rankingを更新し, 上からMAX_REL_AGENTS分をrelAgentに放り込む
     //   */
        // 信頼度が下がった場合と上がった場合で比較の対象を変える
        // 上がった場合は順位が上のやつと比較して
        if (evaluation > 0) {
            int index = agent.inTheList(target, agent.relRanking) - 1;    // targetの現在順位の上を持ってくる
            while (index > -1) {
                // 順位が上のやつよりも信頼度が高くなったなら
                if (agent.reliabilities[agent.relRanking.get(index).id] < agent.reliabilities[target.id]) {
                    Agent tmp = agent.relRanking.get(index);
                    agent.relRanking.set(index, target);
                    agent.relRanking.set(index + 1, tmp);
                    index--;
                } else {
                    break;
                }
            }
        }
        // 下がった場合は下のやつと比較して入れ替えていく
        else {
            int index = agent.inTheList(target, agent.relRanking) + 1;    // targetの現在順位の下を持ってくる
            while (index < AGENT_NUM - 1) {
                // 順位が下のやつよりも信頼度が低くなったなら
                if (agent.reliabilities[agent.relRanking.get(index).id] > agent.reliabilities[target.id]) {
                    Agent tmp = agent.relRanking.get(index);
                    agent.relRanking.set(index, target);
                    agent.relRanking.set(index - 1, tmp);
                    index++;
                } else {
                    break;
                }
            }
        }
        List<Agent> tmp = new ArrayList<>();
        Agent ag;
        for (int j = 0; j < MAX_RELIABLE_AGENTS; j++) {
            ag = agent.relRanking.get(j);
            if (agent.reliabilities[ag.id] > THRESHOLD_FOR_DEPENDABILITY) {
                tmp.add(ag);
            } else {
                break;
            }
        }
        return tmp;
    }

    /**
     * decreaseDECメソッド
     * 過去の学習を忘れるために毎ターン信頼度を減らすメソッド
     * それに伴い, 信頼エージェントだったエージェントの信頼度が閾値を割る可能性があるので,
     * そのチェックをして必要に応じて信頼エージェントを更新する
     *
     * @param agent
     */
    private List<Agent> decreaseDEC(Agent agent) {
        double temp;
        for (int i = 0; i < AGENT_NUM; i++) {
            temp = agent.reliabilities[i] - γ;
            if (temp < 0) agent.reliabilities[i] = 0;
            else agent.reliabilities[i] = temp;
        }
        List<Agent> tmp = new ArrayList<>();
        Agent ag;
        for (int j = 0; j < MAX_RELIABLE_AGENTS; j++) {
            ag = agent.relRanking.get(j);
            if (agent.reliabilities[ag.id] > THRESHOLD_FOR_DEPENDABILITY) {
                tmp.add(ag);
            } else {
                break;
            }
        }
        return tmp;
    }

    private void setPrinciple(Agent agent) {
        if (agent.role == MEMBER) {
            if (agent.relAgents.size() > 0 && agent.e_member > THRESHOLD_FOR_RECIPROCITY) {
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
            if (agent.relAgents.size() > 0 && agent.e_leader > THRESHOLD_FOR_RECIPROCITY) {
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
            if (m.getMessageType() == DONE) {
                // prevTeamMembersから削除して
                ag.prevTeamMember.remove(m.getFrom());
                // 「リーダーとしての更新式で」信頼度を更新する
                // そのメンバがサブタスクを受け取ってからリーダーがその完了報告を受けるまでの時間
                // すなわちrt = "メンバのサブタスク実行時間 + メッセージ到達時間"
                int rt = Manager.getTicks() - m.getTimeSTarrived();
//                System.out.println(rt);
                if (rt < min[ag.id]) min[ag.id] = rt;
                ag.relAgents = renewRel(ag, m.getFrom(), (double) min[ag.id] / (double) rt);
            } else {
                ag.messages.add(m); // 違うメッセージだったら戻す
            }
        }
// */
        size =  ag.messages.size();
        if (size == 0) return;
        //        System.out.println("ID: " + self.id + ", Phase: " + self.phase + " message:  "+ self.messages);
        // リーダーでPROPOSITION or 誰でもEXECUTION → 誰からのメッセージも期待していない
        if (ag.phase == PROPOSITION || ag.phase == EXECUTION) {
            for (int i = 0; i < size; i++) {
                m = ag.messages.remove(0);
                ag.sendNegative(ag, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
            // メンバでWAITING → PROPOSALを期待している
        } else if (ag.phase == WAITING) {
            for (int i = 0; i < size; i++) {
                m = ag.messages.remove(0);
                // PROPOSALで, 要求されているリソースを自分が持つならmessagesに追加
                if (m.getMessageType() == PROPOSAL && ag.res[m.getResType()] != 0) {
                    ag.messages.add(m);
                    // 違かったらsendNegative
                } else ag.sendNegative(ag, m.getFrom(), m.getMessageType(), m.getSubTask());
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
            // メンバでRECEPTION → リーダーからのRESULT(サブタスク割り当て)を期待している
        } else if (ag.phase == RECEPTION) {
            for (int i = 0; i < size; i++) {
                m = ag.messages.remove(0);
                if (m.getMessageType() == RESULT && m.getFrom() == ag.leader) {
                    ag.messages.add(m);
                    // 違かったらsendNegative
                } else ag.sendNegative(ag, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
        }
    }

    static public void clearPM() {
        for (int i = 0; i < AGENT_NUM; i++) {
            min[i] = Integer.MAX_VALUE;
        }
    }

}
