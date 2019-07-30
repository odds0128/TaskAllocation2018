package main.research.strategy.ProposedStrategy;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.Message;
import main.research.strategy.LeaderStrategy;
import main.research.task.AllocatedSubtask;
import main.research.task.Subtask;
import main.research.task.Task;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class LeaderProposedStrategy extends LeaderStrategy implements SetParam {
	List<Arrays> resource_cache = new ArrayList<>();

	public LeaderProposedStrategy() {
		for (int i = 0; i < AGENT_NUM; i++) {
			teamHistory[i] = new HashMap<>();
		}
	}

	protected void proposeAsL(Agent leader) {
		leader.ourTask = Manager.getTask(leader);
		if (leader.ourTask == null) {
			leader.inactivate(0);
			return;
		}
//        leader.ourTask.setFrom(leader);
		leader.restSubtask = leader.ourTask.subtasks.size();                       // 残りサブタスク数を設定
		leader.executionTime = -1;
		leader.candidates = selectMembers(leader, leader.ourTask.subtasks);   // メッセージ送信
		if (leader.candidates == null) {
			leader.candidates = new ArrayList<>();
			Manager.disposeTask(leader);
			leader.inactivate(0);
			return;
		} else {
			for (int i = 0; i < leader.candidates.size(); i++) {
				if (leader.candidates.get(i) != null) {
					leader.proposalNum++;
					leader.sendMessage(leader, leader.candidates.get(i), PROPOSAL, leader.ourTask.subtasks.get(i % leader.restSubtask));
				}
			}
		}
		leader.nextPhase();  // 次のフェイズへ
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
				renewDE(leader, from, 0);
				sortReliabilityRanking(leader.reliabilityRankingAsM);
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
			for (Agent tm : leader.teamMembers) {
				teamHistory[leader.id].put(tm, new AllocatedSubtask(leader.preAllocations.get(tm), Manager.getTicks(), leader.ourTask.task_id));
				leader.sendMessage(leader, tm, RESULT, leader.preAllocations.get(tm));
			}
			if (Agent._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
				for (Agent ag : leader.teamMembers) {
					leader.workWithAsL[ag.id]++;
				}
			}
			leader.pastTasks.add(leader.ourTask);
			leader.ourTask = null;
			leader.inactivate(1);
		}
		// 未割り当てのサブタスクが残っていれば失敗
		else {
			for (Agent tm : leader.teamMembers) {
				leader.sendMessage(leader, tm, RESULT, null);
			}
			Manager.disposeTask(leader);
			leader.inactivate(-1);
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
					} while (leader.calcExecutionTime(candidate, st) < 0);
				} else {
					// 信頼度ランキングの上から割り当てを試みる．
					// 1. 能力的にできない
					// 2. すでにチームに参加してもらっていて，まだ終了連絡がこない
					// 3. すでに別のサブタスクを割り当てる予定がある
					// に当てはまらなければ割り当て候補とする．
					int rankingSize = leader.reliabilityRankingAsL.size();

					for (Agent ag : leader.reliabilityRankingAsL.keySet()) {
						if (leader.inTheList(ag, exceptions) < 0 &&
							leader.calcExecutionTime(ag, st) > 0) {
							candidate = ag;
							break;
						} else {
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

	@Override
	protected void renewDE(Agent from, Agent target, double evaluation) {
		assert !from.equals(target) : "alert4";

		double formerDE = from.reliabilityRankingAsL.get(target);
//		double newDE = renewDEbyArbitraryReward(formerDE, evaluation);

		boolean b = evaluation > 0;
		double newDE = renewDEby0or1(formerDE, b);

		from.reliabilityRankingAsL.put(target, newDE);
		from.reliabilityRankingAsL = sortReliabilityRanking(from.reliabilityRankingAsL);
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
				renewDE(ag, m.getFrom(), (double) reward / rt);

				// タスク全体が終わったかどうかの判定と，それによる処理
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

		// solicitを受けるか判断する
		size = ag.messages.size();
		if (size == 0) return;
		//        System.out.println("ID: " + self.id + ", Phase: " + self.phase + " message:  "+ self.messages);
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

	void clear() {
	}
}
