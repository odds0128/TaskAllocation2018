import java.util.ArrayList;
import java.util.List;

/**
 * ProximityOrientedクラス(距離志向戦略)
 * 近いエージェントを優先するエージェント
 * リーダーは信頼エージェント(通信可能範囲)から選ぶ.
 * メンバは信頼エージェントであるリーダーの要請を受ける
 */
public class ProximityOriented implements Strategy, SetParam {
    public void act(Agent agent) {
        // 何にもしないエージェントがいっぱいいるのである程度サボったらリフレッシュする
        // 何も学習させないで更新するかそれとも減らすか...
//        if( ( Manager.getTicks() - validatedTicks ) > 300 ) inactivate(-1, role);
        if (agent.role == LEADER && agent.phase == PROPOSITION) proposeAsL(agent);
        else if (agent.role == MEMBER && agent.phase == WAITING) replyAsM(agent);
        else if (agent.role == LEADER && agent.phase == REPORT) reportAsL(agent);
        else if (agent.role == MEMBER && agent.phase == RECEPTION) receiveAsM(agent);
        else if (agent.role == MEMBER && agent.phase == EXECUTION) executeAsM(agent);
    }

    private void proposeAsL(Agent leader) {
        leader.checkMessages(leader);
        leader.ourTask = Manager.getTask();
        if (leader.ourTask == null) return;
        leader.restSubTask = leader.ourTask.subTaskNum;                       // 残りサブタスク数を設定
        leader.candidates = selectMembers(leader, leader.ourTask.subTasks);   // メッセージ送信
        leader.nextPhase();  // 次のフェイズへ
    }

    private void replyAsM(Agent member) {
        member.checkMessages(member);
        if (member.messages.size() == 0) return;     // メッセージをチェック
        member.leader = selectLeader(member, member.messages);
        // どのリーダーからの要請も受けないのならinactivate
        // どっかには参加するのなら交渉2フェイズへ
        if (member.joined) {
            member.nextPhase();
        } else if( member.index++ > MAX_REL_AGENTS ){
            member.inactivate(0, MEMBER);
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
                leader.sendMessage(leader, candidate, CHARGE, leader.getAllocation(candidate));
            }
            // 拒否なら送るはずだったサブタスクを再検討リストに入れ, 割り当てを消去する
            else {
                SubTask resendant = leader.getAllocation(candidate);
                if (resendant == null) {
                    leader.removeAllocation(candidate);
                } else {
                    resendants.add(leader.getAllocation(candidate));
                    leader.removeAllocation(candidate);
                }
            }
        }
        // RESULTメッセージ
        for (Message result : leader.results) {
            member = result.getFrom();
            // 失敗なら信頼度を下げ, 実行できなかったサブタスクを再検討リストに入れ, 割り当て情報を消去する
            if (result.getResult() == FAILURE) {
                resendants.add(result.getSubTask());
                leader.removeTeamMember(member);
                leader.removeAllocation(member);
                // 実行成功なら残存サブタスク数を更新
            } else {
//                System.out.println(" member: " + member.id + " did subtask " + leader.getAllocation(member) );
                leader.removeAllocation(member);
                leader.restSubTask--;
            }
        }
        // 残存サブタスク数が0になればタスク終了とみなす
        if (leader.restSubTask == 0) {
            Manager.finishTask(leader);
            leader.inactivate(1, LEADER);
        }

        // 再検討するサブタスクがあれば再送する. ただし, 全信頼エージェントに送っているんだったらもう諦める
        if (resendants.size() > 0) {
            if (MAX_REL_AGENTS - leader.index < resendants.size()) {
                Manager.disposeTask(leader);
                leader.inactivate(0, LEADER);
            } else {
                leader.candidates.addAll(selectMembers(leader, resendants));
            }
        }
        leader.replies.clear();
        leader.results.clear();
    }

    private void receiveAsM(Agent member) {
        // リーダーからの返事が来るまで待つ
        member.checkMessages(member) ;
        if( member.messages.size() == 0 )return;
        Message message;
        message = member.messages.remove(0);
        member.mySubTask = message.getSubTask();
        // サブタスクがもらえたなら実行フェイズへ移る. さもなくばinactive
        if (member.mySubTask != null) {
            member.executionTime =  (int)Math.ceil( ((double)member.mySubTask.reqRes / (double)member.resource));
            member.nextPhase();
        } else {
            member.inactivate(0, MEMBER);
        }
    }

    private void executeAsM(Agent member) {
        member.checkMessages(member);
        member.executionTime--;
        if (member.executionTime == 0) {
            // 自分のサブタスクが終わったら
            // リーダーに終了を通知して非活性に
//            System.out.println("ID: " + member.id + ", leader: " + member.leader.id + ", success, subtask " + member.mySubTask );
            member.sendMessage(member, member.leader, RESULT, null);
            member.inactivate(1, MEMBER);
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
        for ( int i = 0; i < subtasks.size(); i++ ) {
            candidate = leader.relAgents.get(leader.index++);
            subtask = subtasks.get(i);
            leader.allocations.add(new Tuple(candidate, subtask));
            temp.add(candidate);
            leader.sendMessage(leader, candidate, PROPOSAL, subtask.reqRes);
        }
        return temp;
    }

    /**
     * selectLeaderメソッド
     * メンバがどのリーダーの要請を受けるかを判断する
     */
    public Agent selectLeader(Agent member, List<Message> messages){
        int size = messages.size();
        Agent myLeader = null;

        // messageキューに溜まっている参加要請を確認し, 参加するチームを選ぶ
        for (int i = 0; i < size; i++) {
            Message message = messages.remove(0);
            Agent from = message.getFrom();
            // まだどこにも参加していないかを確認する
            if (!member.joined) {
                if( member.inTheList(from, member.relAgents) ) {
                    // リーダーに受理を伝えてフラグを更新
                    member.sendMessage(member, from, REPLY, ACCEPT);
                    member.joined = true;
                    myLeader = from;
                }else {
                    // 信頼エージェント一覧になければ断る
                    member.sendMessage(member, from, REPLY, REJECT);
                }
                // すでにどこかに参加する予定なら残り全て拒否
            } else {
                member.sendMessage(member, from, REPLY, REJECT);
            }
        }
        return myLeader;
    }

    public void inactivate() {
    }
}
