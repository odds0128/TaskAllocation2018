import java.util.ArrayList;
import java.util.List;

/**
 * ComparativeMethod1クラス(距離志向戦略)
 * 近いエージェントを優先するエージェント
 * リーダーは信頼エージェント(通信可能範囲)から選ぶ.
 * メンバは信頼エージェントであるリーダーの要請を受ける
 */
public class ComparativeMethod1 implements Strategy, SetParam {

    public void actAsLeader(Agent agent) {
        if (agent.phase == PROPOSITION) proposeAsL(agent);
        else if (agent.phase == REPORT) reportAsL(agent);
        else if (agent.phase == EXECUTION) execute(agent);
    }

    public void actAsMember(Agent agent){
        if (agent.phase == REPLY) replyAsM(agent);
        else if (agent.phase == RECEPTION) receiveAsM(agent);
        else if (agent.phase == EXECUTION) execute(agent);
    }

    private void proposeAsL(Agent leader) {
        leader.ourTask = Manager.getTask();
        if (leader.ourTask == null) return;
        leader.restSubTask = leader.ourTask.subTaskNum;                       // 残りサブタスク数を設定
        leader.selectSubTask();
        leader.candidates = selectMembers(leader, leader.ourTask.subTasks);   // メッセージ送信
        leader.nextPhase();  // 次のフェイズへ
    }

    private void replyAsM(Agent member) {
        if (member.messages.size() == 0) return;     // メッセージをチェック
        member.leader = selectLeader(member, member.messages);
        if (member.leader != null) {
            member.joined = true;
            member.sendMessage(member, member.leader, REPLY, ACCEPT);
        }
        // どのリーダーからの要請も受けないのならinactivateWithNoLearning
        // どっかには参加するのなら交渉2フェイズへ
        if (member.joined) {
            member.nextPhase();
        }
    }

    private void reportAsL(Agent leader) {
        // 有効なReplyメッセージもResultメッセージもなければreturn
        if (leader.replies.size() == 0 && leader.results.size() == 0) return;

        Agent candidate;
        for (Message reply : leader.replies) {
            leader.replyNum++;
            candidate = reply.getFrom();
            // 受諾なら今は何もしない
            if (reply.getReply() == ACCEPT) {
                continue;
            }
            // 拒否ならそのエージェントを候補リストから外す
            else {
                int i = leader.inTheList(candidate, leader.candidates);
                if (i > 0) leader.candidates.set(i, null);
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
                // もし両方から受理が返ってきたら, (どっちも多分同じ距離にいるだろうから)ランダムに割り当てる
                else if (A != null && B != null) {
                    // Bに割り当てる
                    int rand = leader._randSeed.nextInt(2);
                    if (rand == 0) {
                        leader.candidates.set(index + 1, null);
                        losers.add(A);
                        leader.allocations.add(new Allocation(B, leader.ourTask.subTasks.get(i)));
                        leader.teamMembers.add(B);
                    } else {
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
                    st = reallocations.remove(0);
                    flag = 0;
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
                leader.inactivateWithNoLearning(0);
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
        // サブタスクがもらえたなら実行フェイズへ移る. さもなくばinactive
        if (member.mySubTask != null) {
            member.executionTime = member.calcExecutionTime(member, member.mySubTask);
            member.nextPhase();
        } else {
            member.inactivateWithNoLearning(0);
        }
    }

    private void execute(Agent agent) {
        agent.executionTime--;
        if (agent.executionTime == 0) {
            // 自分のサブタスクが終わったら役割適応度を1で更新して非活性状態へ
            agent.inactivateWithNoLearning(1);
        }
    }

    /**
     * selectMembersメソッド
     * POAgentでは近い奴からサブタスクを任せる
     *
     * @param subtasks
     */
    public List<Agent> selectMembers(Agent leader, List<SubTask> subtasks) {
        List<Agent> temp = new ArrayList<>();
        Agent candidate;
        SubTask subtask;
        for (int i = 0; i < subtasks.size() * RESEND_TIMES; i++) {
            subtask = subtasks.get(i / RESEND_TIMES);
            int j = 0;
            while (true) {
                // エージェント1から全走査
                candidate = leader.relRanking.get(j++);
                // そいつがまだ候補に入っていなくてさらにそのサブタスクをこなせそうなら
                if (leader.inTheList(candidate, temp) < 0 && leader.calcExecutionTime(candidate, subtask) > 0 ) break;
            }
            temp.add(candidate);
            leader.sendMessage(leader, candidate, PROPOSAL, subtask.resType);
        }
        return temp;
    }

    /**
     * selectLeaderメソッド
     * メンバがどのリーダーの要請を受けるかを判断する
     */
    public Agent selectLeader(Agent member, List<Message> messages) {
        int size = messages.size();
        Agent myLeader = null;

        // messageキューに溜まっている参加要請を確認し, 参加するチームを選ぶ
        for (int i = 0; i < size; i++) {
            Message message = messages.remove(0);
            Agent from = message.getFrom();
            // まだどこにも参加していないかを確認する
            if (!member.joined) {
                // リーダーに受理を伝えてフラグを更新
                myLeader = from;
                // すでにどこかに参加する予定なら残り全て拒否
            } else {
                member.sendMessage(member, from, REPLY, REJECT);
            }
        }
        return myLeader;
    }

    public void inactivateWithNoLearning() {
    }
}
