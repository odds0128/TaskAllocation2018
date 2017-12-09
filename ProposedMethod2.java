import java.util.ArrayList;
import java.util.List;

/**
 * ProposedMethod2クラス
 * 信頼度, 信頼エージェントの更新をする.
 * それに加えて, 合計処理時間のテーブルを持たせる
 * 合計処理時間 ≡ サブタスクの割り当てにかかる時間 + メンバのサブタスクの処理時間 + サブタスク完了報告到着までの時間
 * このテーブルにより, 処理時間の短いエージェント → 信頼度の高いエージェント　と選定していく
 */
public class ProposedMethod2 implements SetParam, Strategy {
    static final double γ = γ_p;

    // 期待処理時間テーブル ... 自分がリーダーの時の相手の期待処理時間を表す
    static ExecutionTimeTable[] etTable = new ExecutionTimeTable[AGENT_NUM];

    public void act(Agent agent) {
        assert agent.relAgents.size() <= MAX_REL_AGENTS : "alert3";
        if ((Manager.getTicks() - agent.validatedTicks) > RENEW_ROLE_TICKS ) agent.inactivate(0);
        else if (agent.role == LEADER && agent.phase == PROPOSITION) proposeAsL(agent);
        else if (agent.role == MEMBER && agent.phase == WAITING) replyAsM(agent);
        else if (agent.role == LEADER && agent.phase == REPORT) {
            reportAsL(agent);
/*            List<Agent> temp = new ArrayList<>();
            temp.addAll(agent.candidates);
            temp.addAll(agent.teamMembers);
            agent.relAgents = decreaseDEC(agent, temp);
            temp.clear();
// */
        }
        else if (agent.role == MEMBER && agent.phase == RECEPTION){
            receiveAsM(agent);
/*            List<Agent> temp = new ArrayList<>();
            temp.add(agent.leader);
            agent.relAgents = decreaseDEC(agent,  temp);
            temp.clear();
// */
        }
        else if (agent.role == MEMBER && agent.phase == EXECUTION) executeAsM(agent);
//        agent.relAgents = decreaseDEC(agent);
    }
    private void proposeAsL(Agent leader) {
        leader.ourTask = Manager.getTask();
        if (leader.ourTask == null){
            Agent._leader_num--;
            leader.candidates.clear();
            leader.teamMembers.clear();
            leader.allocations.clear();
            leader.replies.clear();
            leader.results.clear();
            leader.restSubTask = 0;
            leader.replyNum = 0;
            leader.joined = false;
            leader.role = JONE_DOE;
            leader.phase = SELECT_ROLE;
            return;
        }
        leader.restSubTask = leader.ourTask.subTaskNum;                       // 残りサブタスク数を設定
        leader.selectSubTask();
        leader.candidates = selectMembers(leader, leader.ourTask.subTasks);   // メッセージ送信
        leader.nextPhase();  // 次のフェイズへ
    }
    private void replyAsM(Agent member) {
        if (member.messages.size() == 0) return;     // メッセージをチェック
        member.leader = selectLeader(member, member.messages);
        if( member.leader != null ) {
            member.joined = true;
//            System.out.println("ID: "+ member.id + ", my leader is " + member.leader.id );
            member.sendMessage(member, member.leader, REPLY, ACCEPT);
        }
        // どのリーダーからの要請も受けないのならinactivate
        // どっかには参加するのなら交渉2フェイズへ
        if (member.joined) {
            member.nextPhase();
        }
    }


    private void reportAsL(Agent leader) {
//        System.out.println(" ID: " +leader.id + ", messages: " + leader.messages.size() );
        leader.checkMessages(leader);
        // 有効なReplyメッセージもResultメッセージもなければreturn
        if (leader.replies.size() == 0 && leader.results.size() == 0) return;
//        System.out.println(" ID: " +leader.id + ", leader.replies: " + leader.replies.size() + ", leader.results: "+ leader.results.size());
        // 2017/10/21 逐次メンバに返事してサブタスクを渡して行くことに. Rejectされたら再送
        // メッセージを順次確認. すでにサブタスクを託したメンバから終了の通知が来る可能性があることに注意
        Agent candidate, member;
        List<SubTask> resendants = new ArrayList<>();
        // leader.repliesに関して
        for (Message reply : leader.replies) {
            candidate = reply.getFrom();
            // 受諾ならteamMemberに追加してサブタスクを送る
            if (reply.getReply() == ACCEPT) {
                leader.candidates.remove(candidate);
                leader.teamMembers.add(candidate);
                setAllocationTime(leader, candidate);      // 割り当てた時刻をセットする
                leader.sendMessage(leader, candidate, RESULT, leader.getAllocation(candidate).getSubtask());
            }
            // 拒否なら信頼度を0で更新し, 送るはずだったサブタスクを再検討リストに入れ, 割り当てを消去する
            else {
                leader.relAgents = renewRel(leader, candidate, 0);
                leader.candidates.remove(candidate);
                SubTask resendant = leader.getAllocation(candidate).getSubtask();
                if (resendant == null) {
                    leader.removeAllocation(candidate);
                } else {
                    resendants.add(leader.getAllocation(candidate).getSubtask());
                    leader.removeAllocation(candidate);
                }
            }
        }
        // 残存サブタスク数が0になればタスク終了とみなす
        if (leader.restSubTask == 0) {
            Manager.finishTask(leader);
            leader.inactivate(1);
        }

        // 再検討するサブタスクがあれば再送する. ただし, 全信頼エージェントに送っているんだったらもう諦める
        if (resendants.size() > 0) {
            if (MAX_PROPOSITION_NUM - leader.index < resendants.size()) {
                Manager.disposeTask(leader);
                leader.inactivate(0);
            } else {
                leader.candidates.addAll(selectMembers(leader, resendants));
            }
// */      leader.candidates.addAll(selectMembers(leader, resendants));
        }
        leader.replies.clear();
        leader.results.clear();
        leader.validatedTicks = Manager.getTicks();
    }

    private void receiveAsM(Agent member) {
        // リーダーからの返事が来るまで待つ
        member.checkMessages(member);
        if (member.messages.size() == 0) return;
        Message message;
        message = member.messages.remove(0);
        member.mySubTask = message.getSubTask();

        int responseTime = Manager.getTicks() - member.acceptTime;
        member.totalResponseTicks += responseTime;
        member.meanResponseTicks = (double)member.totalResponseTicks/(double)member.totalOffers;

        // サブタスクがもらえたなら実行フェイズへ移る.
        if (member.mySubTask != null) {
            member.executionTime = member.calcExecutionTime(member);
            member.nextPhase();
        }
        // サブタスクが割り当てられなかったら信頼度を更新し, inactivate
        else {
            //    if( member.id == 4 ) System.out.println( member.meanResponseTicks + ", waitTime: " + responseTime);
            member.relAgents = renewRel(member, member.leader, -(double)responseTime/member.meanResponseTicks );
            member.inactivate(0);
        }
    }

    private void executeAsM(Agent member) {
        member.checkMessages(member);
        member.executionTime--;
        if (member.executionTime == 0) {
            // 自分のサブタスクが終わったら
            // リーダーに終了を通知して非活性に
//            System.out.println("ID: " + member.id + ", leader: " + member.leader.id + ", success, subtask " + member.mySubTask );
            member.inactivate(1);
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
        for (int i = 0; i < subtasks.size(); i++) {
            if (leader.epsilonGreedy()) candidate = Manager.getAgentRandomly(leader, leader.relAgents);
            else candidate = leader.relRanking.get(leader.index++ );
            subtask = subtasks.get(i);
            leader.allocations.add(new Allocation(candidate, subtask));
            temp.add(candidate);
            leader.sendMessage(leader, candidate, PROPOSAL, subtask.resType);
        }
        return temp;
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
        double temp = agent.reliabilities[target.id];

//        if(agent.id == 3 && evaluation > 0 ) System.out.println("distance: " + Manager.distance[agent.id][target.id] + ", evaluation: " + evaluation);

        // 信頼度の更新式
        if (agent.role == LEADER) {
            agent.reliabilities[target.id] = temp * (1 - α) + α * evaluation;
        } else {
            agent.reliabilities[target.id] = temp * (1 - α) + α * evaluation;
        }

        /*
         信頼エージェントの更新
         信頼度rankingを更新し, 上からMAX_REL_AGENTS分をrelAgentに放り込む
        */
        // 信頼度が下がった場合と上がった場合で比較の対象を変える
        // 上がった場合は順位が上のやつと比較して
        if (evaluation == 1) {
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
        for (int j = 0; j < MAX_REL_AGENTS; j++) {
            ag = agent.relRanking.get(j);
            if (agent.reliabilities[ag.id] > THRESHOLD_DEPENDABILITY) {
                tmp.add(ag);
            } else {
                break;
            }
        }
        if (tmp.size() == 0 || agent.e_member < THRESHOLD_RECIPROCITY) agent.principle = RATIONAL;
        else agent.principle = RECIPROCAL;
        return tmp;
    }

    /**
     * decreaseDECメソッド
     * 提案手法では, 通信を待っている間などに減らすことにする
     * したがって通信時間が短い方が相対的に信頼度が上がることになる
     * それに伴い, 信頼エージェントだったエージェントの信頼度が閾値を割る可能性があるので,
     * そのチェックをして必要に応じて信頼エージェントを更新する
     * @param agent
     */
    private List<Agent> decreaseDEC(Agent agent, List<Agent> opponents) {
        double temp;
        for (Agent ag : opponents) {
            temp = agent.reliabilities[ag.id] - γ;
            if (temp < 0) agent.reliabilities[ag.id] = 0;
            else agent.reliabilities[ag.id] = temp;
        }
        List<Agent> tmp = new ArrayList<>();
        Agent ag ;
        for (int j = 0; j < MAX_REL_AGENTS; j++){
            ag = agent.relRanking.get(j);
            if( agent.reliabilities[ag.id] > THRESHOLD_DEPENDABILITY ) {
                tmp.add( ag );
            }else{
                break;
            }
        }
        if( tmp.size() == 0 || agent.e_member < THRESHOLD_RECIPROCITY ) agent.principle = RATIONAL;
        else agent.principle = RECIPROCAL;
        return tmp;
    }

    /**
     * decreaseDECメソッド
     * 過去の学習を忘れるために毎ターン信頼度を減らすメソッド
     * それに伴い, 信頼エージェントだったエージェントの信頼度が閾値を割る可能性があるので,
     * そのチェックをして必要に応じて信頼エージェントを更新する
     * @param agent
     */
    private List<Agent> decreaseDEC(Agent agent){
        double temp;
        for( int i = 0; i < AGENT_NUM ; i++ ){
            temp = agent.reliabilities[i] - γ;
            if( temp < 0 ) agent.reliabilities[i] = 0;
            else agent.reliabilities[i] = temp;
        }
        List<Agent> tmp = new ArrayList<>();
        Agent ag ;
        for (int j = 0; j < MAX_REL_AGENTS; j++){
            ag = agent.relRanking.get(j);
            if( agent.reliabilities[ag.id] > THRESHOLD_DEPENDABILITY ) {
                tmp.add( ag );
            }else{
                break;
            }
        }
        if( tmp.size() == 0 || agent.e_member < THRESHOLD_RECIPROCITY ) agent.principle = RATIONAL;
        else agent.principle = RECIPROCAL;
        return tmp;
    }

    /**
     * renewETTableメソッド
     * ETTable = executionTimeTable
     * 任意のタイミングでmemberエージェントに割り当てたサブタスクの合計処理時間により更新
     * @param leader
     * @param member
     * @param et
     */
    private void renewETTable(Agent leader, Agent member, int et){
        etTable[leader.id].renewMET(member.id, et);
    }

    /**
     * getAllocationメソッド
     * 引数のエージェントに割り当てるサブタスクを返す
     * 割り当て情報は保持
     */
    private void setAllocationTime(Agent leader, Agent target) {
        int size = leader.allocations.size();
        for (int i = 0; i < size; i++) {
            if (target == leader.allocations.get(i).getCandidate()) {
                leader.allocations.get(i).setAllocationTime(Manager.getTicks());
            }
        }
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
        if( member.epsilonGreedy() ) {
            myLeader = messages.remove(member._randSeed.nextInt( messages.size() ) ).getFrom();
            for( int i = 0; i < size - 1; i++ ){
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
                else{
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


    public void inactivate() {
    }
}
