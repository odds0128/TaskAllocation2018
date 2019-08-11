package main.research.agent.strategy.ProposedStrategy;

import main.research.Manager;
import main.research.SetParam;
import main.research.agent.Agent;
import main.research.communication.Message;
import main.research.random.MyRandom;
import main.research.agent.strategy.MemberStrategy;
import main.research.task.AllocatedSubtask;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.SetParam.Role.*;
import static main.research.SetParam.Principle.*;

import static main.research.SetParam.MessageType.*;
import static main.research.SetParam.ReplyType.*;
import static main.research.SetParam.Phase.*;

import java.util.ArrayList;
import java.util.List;

// TODO: 中身を表したクラス名にする
public class MemberProposedStrategy extends MemberStrategy implements SetParam {

	protected void replyAsM(Agent member) {
		if (member.mySubtask == null) {
			if (Manager.getTicks() - member.validatedTicks > THRESHOLD_FOR_ROLE_RENEWAL) {
				member.inactivate(0);
			}
			return;     // メッセージをチェック
		}
		// どのリーダーからの要請も受けないのならinactivate
		member.phase = RECEPTION;
		member.validatedTicks = Manager.getTicks();
	}


	protected void receiveAsM(Agent member) {
		// サブタスクがもらえたなら実行フェイズへ移る.
		if (member.mySubtask != null) {
			member.myLeader = member.mySubtask.from;
			member.allocated[member.myLeader.id][member.mySubtask.resType]++;
			member.executionTime = member.calcExecutionTime(member, member.mySubtask);
			member.phase = EXECUTION;
			member.validatedTicks = Manager.getTicks();
		}
		// サブタスクが割り当てられなかったら信頼度を0で更新し, inactivate
		else {
			member.inactivate(0);
		}
	}

	protected void execute(Agent agent) {
		agent.executionTime--;
		agent.validatedTicks = Manager.getTicks();
		if (agent.executionTime == 0) {
			agent.sendMessage(agent, agent.myLeader, DONE, 0);
			agent.myLeaders.remove(agent.myLeader);
			agent.required[agent.mySubtask.resType]++;
			renewDE(agent, agent.myLeader, (double) agent.mySubtask.reqRes[agent.mySubtask.resType] / (double) agent.calcExecutionTime(agent, agent.mySubtask));
			if (agent._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
				agent.workWithAsM[agent.myLeader.id]++;
				agent.didTasksAsMember++;
			}
			// 自分のサブタスクが終わったら役割適応度を1で更新して非活性状態へ
			agent.inactivate(1);
		}
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
				if (member.haveAlreadyJoined(member, message.getFrom())) {
					member.sendMessage(member, message.getFrom(), REPLY, REJECT);
				} else {
					solicitations.add(message);
				}
			} else {
				others.add(message);
			}
		}
		member.messages = others;
		int room = SUBTASK_QUEUE_SIZE - member.mySubtaskQueue.size(); // サブタスクキューの空き

		// サブタスクキューの空きがある限りsolicitationを選定する
		while (member.tbd < room && solicitations.size() > 0) {
			// εグリーディーで選択する
			if (member.epsilonGreedy()) {
				Message target;
				Agent to;
				do {
					int index = MyRandom.getRandomInt(0, solicitations.size() - 1);
					target = solicitations.remove(index);
					to = target.getFrom();
				} while (member.haveAlreadyJoined(member, to));
				member.sendMessage(member, to, REPLY, ACCEPT);
				member.myLeaders.add(to);
			}
			// solicitationsの中から最も信頼するリーダーのsolicitを受ける
			else {
				int index = 0;
				int solicitationNum = solicitations.size();
				Message message1 = solicitations.get(0);
				Message message2;
				Agent tempLeader = message1.getFrom();
				Agent temp;

				for (int i = 1; i < solicitationNum; i++) {
					message2 = solicitations.get(i);
					temp = message2.getFrom();
					// もし暫定信頼度一位のやつより信頼度高いやついたら, 暫定のやつを断って今のやつを暫定(ryに入れる
					if (member.reliabilityRankingAsM.get(tempLeader) < member.reliabilityRankingAsM.get(temp)) {
						tempLeader = temp;
						index = i;
					}
				}
				if (member.principle == RATIONAL) {
					Agent l = solicitations.remove(index).getFrom();
					member.sendMessage(member, l, REPLY, ACCEPT);
					member.myLeaders.add(l);
				} else {
					if (member.inTheList(tempLeader, (List) member.reliabilityRankingAsM.keySet()) > -1) {
						Agent l = solicitations.remove(index).getFrom();
						member.sendMessage(member, l, REPLY, ACCEPT);
						member.myLeaders.add(l);
					} else member.sendMessage(member, tempLeader, REPLY, REJECT);
				}
			}
			member.tbd++;
		}
		while (solicitations.size() > 0) {
			message = solicitations.remove(0);
			member.sendMessage(member, message.getFrom(), REPLY, REJECT);
		}
		// othersへの対処．(othersとしては，RESULTが考えられる)
		Message result;
		Subtask allocatedSubtask;
		while (others.size() > 0) {
			member.tbd--;
			result = others.remove(0);
			allocatedSubtask = result.getSubtask();
			assert result.getMessageType() == RESULT : "Leader Must Confuse Someone";
			if (allocatedSubtask == null) {   // 割り当てがなかった場合
				renewDE(member, result.getFrom(), 0);
				member.myLeaders.remove(result.getFrom());
			} else {    // 割り当てられた場合
				// すでにサブタスクを持っているならそれを優先して今もらったやつはキューに入れておく
				// さもなければキューに"入れずに"自分の担当サブタスクとする
				if (member.mySubtask == null) {
					member.mySubtask = allocatedSubtask;
				} else {
					member.mySubtaskQueue.add(allocatedSubtask);
				}
			}
		}
		assert member.mySubtaskQueue.size() <= SUBTASK_QUEUE_SIZE : member.mySubtaskQueue + " Overwork!";
	}

	// TODO: コメントアウトで手動で切り替えるのをやめる
	@Override
	protected void renewDE(Agent from, Agent target, double evaluation) {
		assert !from.equals(target) : "alert4";

		double formerDE = from.reliabilityRankingAsM.get(target);
//		double newDE = renewDEbyArbitraryReward(formerDE, evaluation);

		boolean b = evaluation > 0;
		double newDE = renewDEby0or1(formerDE, b);

		from.reliabilityRankingAsM.put(target, newDE);
	}


	public void checkMessages(Agent ag) {
		int size = ag.messages.size();
		Message m;
		// メンバからの作業完了報告をチェックする
		for (int i = 0; i < size; i++) {
			m = ag.messages.remove(0);
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


				renewDE(ag, m.getFrom(), (double) reward / rt);
				// TODO: タスク全体が終わったかどうかの判定と，それによる処理
                /*
                1. 終わったサブタスクがどのタスクのものだったか確認する
                2. そのサブタスクを履歴から除く
                3. もしそれによりタスク全体のサブタスクが0になったら終了とみなす
                 */
				Task task = ag.identifyTask(as.getTaskId());
				task.subtasks.remove(as.getSt());
				if (task.subtasks.size() == 0) {
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


}
