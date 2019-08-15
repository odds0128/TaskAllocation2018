package main.research.agent.strategy.ComparativeStrategy;

import main.research.Manager;
import main.research.SetParam;
import main.research.agent.Agent;
import main.research.communication.MessageDeprecated;
import main.research.random.MyRandom;
import main.research.agent.strategy.MemberStrategy;
import main.research.task.AllocatedSubtask;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.SetParam.DERenewalStrategy.*;
import static main.research.SetParam.MessageType.*;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.ReplyType.*;
import static main.research.SetParam.Role.*;
import static main.research.SetParam.Principle.*;

import java.util.ArrayList;
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


public class FixedMember extends MemberStrategy implements SetParam {

    protected void replyAsM(Agent member) {
        // どのリーダーからの要請も受けないのならinactivate
        // どっかには参加するのなら交渉2フェイズへ
        if ( member.messages.isEmpty() ) {
            return;
        }
        member.phase = RECEPTION;
        member.validatedTicks = Manager.getCurrentTime();
    }

    protected void receiveAsM(Agent member) {
        // サブタスクがもらえたなら実行フェイズへ移る.
        if ( ! member.mySubtaskQueue.isEmpty() ) {
            Agent currentLeader = member.mySubtaskQueue.get(0);
            Subtask currentSubtask = member.mySubtaskQueue.keySet().iterator().next();
            member.allocated[currentLeader.id][currentSubtask.resType]++;
            member.executionTime = member.calcExecutionTime(member, currentSubtask);
            member.phase = EXECUTION;
            member.validatedTicks = Manager.getCurrentTime();
        }
        // サブタスクが割り当てられなかったら信頼度を0で更新し, inactivate
        else {
            inactivate(member, 0);
        }
    }

    protected void execute(Agent ag_m) {
        ag_m.validatedTicks = Manager.getCurrentTime();
        ag_m.executionTime--;

        if ( ag_m.executionTime == 0 ) {
            Agent currentLeader = ag_m.mySubtaskQueue.get(0);
            Subtask currentSubtask = ag_m.mySubtaskQueue.keySet().iterator().next();
            ag_m.sendMessage(ag_m, currentLeader, DONE, 0);
            ag_m.required[currentSubtask.resType]++;

            renewDE( ag_m.reliabilityRankingAsM, currentLeader, 1, withReward);
            sortReliabilityRanking( ag_m.reliabilityRankingAsM);

            if (ag_m._coalition_check_end_time - Manager.getCurrentTime() < COALITION_CHECK_SPAN) {
                ag_m.workWithAsM[currentLeader.id]++;
                ag_m.didTasksAsMember++;
            }
            ag_m.mySubtaskQueue.remove(0);
            inactivate(ag_m, 1);
        }
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
        List<MessageDeprecated> others = new ArrayList<>();
        List<MessageDeprecated> solicitations = new ArrayList<>();
        MessageDeprecated message;

        // 有効なメッセージがなければリターンする
        if (member.messages.size() == 0) return;

        // TODO: メッセージの分類
        while (member.messages.size() > 0) {
            message = member.messages.remove(0);
            if (message.getMessageType() == SOLICITATION) {
//                assert message.getFrom() != member.leader : message.getFrom().id + " to " + member.id +  "Duplicated";
                // すでにそのリーダーのチームに参加している場合は落とす
                solicitations.add(message);
            } else {
                others.add(message);
            }
        }

        member.messages = others;
        int room = SUBTASK_QUEUE_SIZE - member.mySubtaskQueue.size(); // サブタスクキューの空き

        // サブタスクキューの空きがある限りsolicitationを選定する
        while ( member.numberOfExpectedMessages < room && solicitations.size() > 0) {
            // εグリーディーで選択する
            if ( MyRandom.epsilonGreedy( Agent.ε ) ) {
                MessageDeprecated target;
                Agent to;
                int index = MyRandom.getRandomInt( 0, solicitations.size() - 1 );
                target = solicitations.get(index);
                to = target.getFrom();
                member.sendMessage(member, to, REPLY, ACCEPT);
            }
            // solicitationsの中から最も信頼するリーダーのsolicitを受ける
            else {
                int index = 0;
                int solicitationNum = solicitations.size();
                MessageDeprecated message1    = solicitations.get(0);
                MessageDeprecated message2;
                Agent   tempLeader  = message1.getFrom();
                Agent   temp;

                for (int i = 1; i < solicitationNum; i++) {
                    message2 = solicitations.get(i);
                    temp     = message2.getFrom();
                    // もし暫定信頼度一位のやつより信頼度高いやついたら, 暫定のやつを断って今のやつを暫定(ryに入れる
                    if (member.reliabilityRankingAsM.get(tempLeader.id) < member.reliabilityRankingAsM.get(temp)) {
                        tempLeader = temp;
                        index = i;
                    }
                }
                if (member.principle == RATIONAL) {
                    Agent l = solicitations.remove(index).getFrom();
                    member.sendMessage(member, l, REPLY, ACCEPT);
                } else {
                    if (member.inTheList(tempLeader, (List) member.reliabilityRankingAsM.keySet()) > -1) {
                        Agent l = solicitations.remove(index).getFrom();
                        member.sendMessage(member, l, REPLY, ACCEPT);
                    } else member.sendMessage(member, tempLeader, REPLY, DECLINE);
                }
            }
            member.numberOfExpectedMessages++;
        }
        while(solicitations.size() > 0){
            message = solicitations.remove(0);
            member.sendMessage(member, message.getFrom(), REPLY, DECLINE);
        }
        // TODO: othersへの対処．(othersとしては，RESULTが考えられる)
        MessageDeprecated result ;
        Subtask allocatedSubtask;
        while( others.size() > 0 ){
            member.numberOfExpectedMessages--;
            result  = others.remove(0);
            allocatedSubtask = result.getSubtask();
            assert result.getMessageType() == RESULT: "Leader Must Confuse Someone";
            if( allocatedSubtask == null ){   // 割り当てがなかった場合
                renewDE( member.reliabilityRankingAsM, result.getFrom(), 0, withReward );
            }else{    // 割り当てられた場合
                // TODO: すでにサブタスクを持っているならそれを優先して今もらったやつはキューに入れておく
                // TODO: さもなければキューに"入れずに"自分の担当サブタスクとする
                member.mySubtaskQueue.put(allocatedSubtask, result.getFrom());
            }
        }
        assert member.mySubtaskQueue.size() <= SUBTASK_QUEUE_SIZE: member.mySubtaskQueue + " Overwork!";
    }

    public void checkMessages(Agent ag) {
        int size = ag.messages.size();
        MessageDeprecated m;
        // メンバからの作業完了報告をチェックする
        for (int i = 0; i < size; i++) {
            m = ag.messages.remove(0);
            if (m.getMessageType() == DONE) {
                // 「リーダーとしての更新式で」信頼度を更新する
                // そのメンバにサブタスクを送ってからリーダーがその完了報告を受けるまでの時間
                // すなわちrt = "メンバのサブタスク実行時間 + メッセージ往復時間"
                AllocatedSubtask as = teamHistory[ag.id].remove(m.getFrom());
                if (as == null) {
                    System.out.println(Manager.getCurrentTime() + ": " + m.getFrom() + " asserts he did " + ag.id + "'s subtask ");
                }
                int rt = Manager.getCurrentTime() - as.getAllocatedTime();
                int reward = as.getRequiredResources() * 5;

                renewDE( ag.reliabilityRankingAsM, m.getFrom(), (double) reward/rt , withReward);
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
        checkSolicitationsANDAllocations(ag);
    }

    void inactivate(Agent ag, int success) {
        if (ag.role == LEADER) {
            ag.phase = PROPOSITION;
            ag.teamMembers.clear();        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
            ag.myTask = null;
            ag.candidates.clear();
            ag.replyNum = 0;
            ag.replies.clear();
            ag.results.clear();
            ag.proposalNum = 0;
        } else if (ag.role == MEMBER) {
            if (ag.mySubtaskQueue.size() > 0) {
                //FIXME: どうやらサブタスクがキューから除かれずにmySubtaskとしてしまうときがある
                assert ag.mySubtaskQueue.get(0) != ag.mySubtaskQueue.get(0) : "Same subtask";
                ag.executionTime = ag.calcExecutionTime(ag, ag.mySubtaskQueue.keySet().iterator().next() );
                ag.phase = EXECUTION;
            } else {
                ag.phase = WAITING;
            }
        }
        ag.validatedTicks = Manager.getCurrentTime();
    }

    public void clearStrategy() {
        for (int i = 0; i < AGENT_NUM; i++) {
            teamHistory[i].clear();
        }
    }
}
