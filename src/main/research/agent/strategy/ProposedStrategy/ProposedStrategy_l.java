package main.research.agent.strategy.ProposedStrategy;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.Message;
import main.research.agent.strategy.LeaderStrategy;
import main.research.random.MyRandom;
import main.research.task.AllocatedSubtask;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.SetParam.MessageType.*;
import static main.research.SetParam.ReplyType.*;
import static main.research.SetParam.Phase.*;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class ProposedStrategy_l extends LeaderStrategy implements SetParam {
	List<TeamHistoryCache> teamHistoryCache = new ArrayList<>();
	Map<Agent, Integer> timeToStartCommunicatingMap = new HashMap<>();
	Map<Agent, Integer> roundTripTimeMap = new HashMap<>();
	Map<Agent, double[]> congestionDegreeMap = new HashMap<>();


	@Override
	public void actAsLeader(Agent agent){
		sortReliabilityRanking(agent.reliabilityRankingAsL);
		if (agent.phase == PROPOSITION) proposeAsL(agent);
		else if (agent.phase == REPORT) reportAsL(agent);
		evaporateDE(agent.reliabilityRankingAsL);
//		TeamHistoryCache.updateCache( teamHistoryCache );
	}

	public ProposedStrategy_l() {
		for (int i = 0; i < AGENT_NUM; i++) {
			teamHistory[i] = new HashMap<>();
		}
	}

	protected void proposeAsL(Agent leader) {
		leader.myTask = Manager.getTask(leader);
		if (leader.myTask == null) {
			leader.inactivate(0);
			return;
		}
		leader.candidates = selectMembers(leader, leader.myTask.subtasks);   // メッセージ送信

		if (leader.candidates.isEmpty()) {
			Manager.disposeTask(leader);
			leader.inactivate(0);
			return;
		}

		for (int i = 0; i < leader.candidates.size(); i++) {
			Agent candidate = leader.candidates.get(i);
			if ( candidate != null) {
				leader.proposalNum++;
				timeToStartCommunicatingMap.put( candidate, Manager.getTicks() );
				// remove
				if( leader.id == 455 && candidate.id == 203 ) {
					System.out.println( leader.id + " send proposition to 203 at " + Manager.getTicks() );
				}
				leader.sendMessage(leader, candidate, PROPOSAL, leader.myTask.subtasks.get(i % leader.myTask.subtasks.size() ) );
			}
		}
		leader.nextPhase();  // 次のフェイズへ
	}

	protected void reportAsL(Agent leader) {
		if (leader.replies.size() < leader.proposalNum) return;

		Agent from;
		for (Message reply : leader.replies) {
			// 拒否ならそのエージェントを候補リストから外し, 信頼度を0で更新する
			from = reply.getFrom();
			if (reply.getReply() != ACCEPT) {
				exceptions.remove(from);
				int i = leader.inTheList(from, leader.candidates);
				assert i >= 0 : "alert: Leader got reply from a ghost.";
				leader.candidates.set(i, null);
				renewDE(leader, from, 0);
			}
			// hack: 現状リーダーは全員からの返信をもらってから割り当てを開始するため，早くに返信が到着したエージェントとの総通信時間が見かけ上長くなってしまう．
			// だからここではそれを訂正するために，その差分をroundTripTimeの一部として足し合わせることで混雑度の計算が狂わないようにしている
			int gap = Manager.getTicks() - timeToStartCommunicatingMap.get(from) - roundTripTimeMap.get(from);
			assert gap % 2 == 0 : "gap is odd.";
			int modifiedRoundTripTime = roundTripTimeMap.get(from) + gap / 2;
			roundTripTimeMap.put( from, modifiedRoundTripTime );
			// remove
			if ( leader.id == 455 && from.id == 203 ) {
				System.out.println( leader.id + " update round trip time to " + roundTripTimeMap.get(from) + " at " + Manager.getTicks() );
			}
		}

		Agent A, B;
		Map<Agent, Subtask> preAllocations = new HashMap<>();
		// if 全candidatesから返信が返ってきてタスクが実行可能なら割り当てを考えていく
		for (int indexA = 0, indexB = leader.myTask.subtasks.size() ; indexA < leader.myTask.subtasks.size(); indexA++, indexB++ ) {
			A = leader.candidates.get(indexA);
			B = leader.candidates.get(indexB);
			// もし両方から受理が返ってきたら, 信頼度の高い方に割り当てる
			if (A != null && B != null) {
				// Bの方がAより信頼度が高い場合
				if (leader.reliabilityRankingAsL.get(A) < leader.reliabilityRankingAsL.get(B)) {
					preAllocations.put(B, leader.myTask.subtasks.get(indexA));
					exceptions.remove(A);
					leader.sendMessage(leader, A, RESULT, null);
					leader.teamMembers.add(B);
				}
				// Aの方がBより信頼度が高い場合
				else {
					preAllocations.put(A, leader.myTask.subtasks.get(indexA));
					exceptions.remove(B);
					leader.sendMessage(leader, B, RESULT, null);
					leader.teamMembers.add(A);
				}
			}
			// もし片っぽしか受理しなければそいつがチームメンバーとなる
			else if( A != null | B != null ) {
				// Bだけ受理してくれた
				if (A == null) {
					preAllocations.put(B, leader.myTask.subtasks.get(indexA));
					leader.teamMembers.add(B);
				}
				// Aだけ受理してくれた
				if (B == null){
					preAllocations.put(A, leader.myTask.subtasks.get(indexA));
					leader.teamMembers.add(A);
				}
			}
		}
		// 未割り当てが残っていないのなら実行へ
		if (leader.teamMembers.size() == leader.myTask.subtasks.size()) {
			for (Agent tm : leader.teamMembers) {
				teamHistory[leader.id].put(tm, new AllocatedSubtask(preAllocations.get(tm), Manager.getTicks(), leader.myTask.task_id));
				// remove
				if( leader.id == 455 && tm.id == 203 ) {
					System.out.println( leader.id + " assigns " + preAllocations.get(tm) + " to 203 at " + Manager.getTicks() );
				}
				leader.sendMessage(leader, tm, RESULT, preAllocations.get(tm));
			}
			if (Agent._coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
				for (Agent ag : leader.teamMembers) {
					leader.workWithAsL[ag.id]++;
				}
			}
			leader.pastTasks.add(leader.myTask);
			leader.myTask = null;
			leader.inactivate(1);
		}
		// 未割り当てのサブタスクが残っていれば失敗
		else {
			for (Agent tm : leader.teamMembers) {
				exceptions.remove(tm);
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
	List<Agent> exceptions = new ArrayList<>();
	public List<Agent> selectMembers(Agent leader, List<Subtask> subtasks) {
		List<Agent> memberCandidates = new ArrayList<>();
		Agent candidate = null;
		List<Subtask> skips = new ArrayList<>();  // 互恵エージェントがいるために他のエージェントに要請を送らないサブタスクを格納

		for (Subtask st : subtasks) {
			for (int i = 0; i < RESEND_TIMES; i++) {
				memberCandidates.add(null);
			}
		}

		// 一つのタスクについてRESEND_TIMES周する
		for (int i = 0; i < RESEND_TIMES; i++) {
			Subtask st;
			for (int stIndex = 0; stIndex < subtasks.size(); stIndex++) {
				st = subtasks.get(stIndex);
				// すでにそのサブタスクを互恵エージェントに割り当てる予定ならやめて次のサブタスクへ
				if (leader.inTheList(st, skips) >= 0) {
					continue;
				}
				// 一つ目のサブタスク(報酬が最も高い)から割り当てていく
				// 信頼度の一番高いやつから割り当てる
				// εの確率でランダムに割り振る
				if ( MyRandom.epsilonGreedy( Agent.ε ) ) {
					do {
						candidate = Manager.getAgentRandomly(leader, exceptions, AgentManager.getAgentList());
					} while (leader.calcExecutionTime(candidate, st) < 0);
				} else {
					// 信頼度ランキングの上から割り当てを試みる．
					// 1. 能力的にできない
					// 2. すでにチームに参加してもらっていて，まだ終了連絡がこない
					// 3. すでに別のサブタスクを割り当てる予定がある
					// に当てはまらなければ割り当て候補とする．
					for (Agent ag : leader.reliabilityRankingAsL.keySet()) {
						// TODO: inTheListもcalcExecutionTimeもインスタンスに紐づかないのでstaticにするかユーティリティクラスにする．
						// もっというなら，inTheList相当のメソッドが普通にあるはず．
						if (leader.inTheList(ag, exceptions) < 0 &&
							leader.calcExecutionTime(ag, st) > 0) {
							candidate = ag;
							break;
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
					return new ArrayList<>();
				}
				candidate = null;
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

				exceptions.remove( m.getFrom() );

				int now = Manager.getTicks();
				TeamHistoryCache temp = new TeamHistoryCache( now, m.getFrom(), as.getSt().resType, rt );
				teamHistoryCache.add(temp);

				int bindingTime = Manager.getTicks() - timeToStartCommunicatingMap.get(m.getFrom());
				renewCongestionDegreeMap( congestionDegreeMap, roundTripTimeMap, m.getFrom(), as.getSt(), bindingTime );
				// remove
				if(  ag.id == 455 && m.getFrom().id == 203 ) {
					String string = String.join( ", ", Integer.toString(as.getSt().reqRes[as.getSt().resType] ), Integer.toString(bindingTime), Integer.toString(roundTripTimeMap.get(m.getFrom())) );
					System.out.println(string);
					System.out.println( ag.id + " get execution message at " + Manager.getTicks() + " from 203." );

					// TODO: 距離とか考えて妥当か検証
					System.out.println("  actual: " + m.getFrom() );
					System.out.println("  expected: " + Arrays.toString( congestionDegreeMap.get(m.getFrom()) ));
				}

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
				if (m.getMessageType() == REPLY && ag.inTheList(from, ag.candidates) > -1) {
					int roundTripTime = Manager.getTicks() - timeToStartCommunicatingMap.get(from);
					roundTripTimeMap.put( from, roundTripTime );

					// remove
					if( ag.id == 455 && from.id == 203 ) {
						System.out.println( ag.id + " get " + m.getReply() + " message at " + Manager.getTicks() + " from 203." );
					}

					ag.replies.add(m);
				}
				else ag.sendNegative(ag, m.getFrom(), m.getMessageType(), m.getSubtask());
			}
		}
	}

	// TODO: 以下２つのメソッドが果たしてstaticがいいのかどうか
	private static void renewCongestionDegreeMap( Map<Agent, double[]> cdm, Map<Agent, Integer> rtm, Agent target, Subtask st, int bindingTime ) {
		double[] tempArray;
		if ( cdm.containsKey(target)) {
			tempArray = cdm.get(target);
		} else {
			tempArray = new double[RESOURCE_TYPES];
		}
		int requiredResourceType = st.resType;
		tempArray[requiredResourceType] = calculateCongestionDegree( bindingTime, rtm.get(target), st );
		cdm.put(target, tempArray);
	}

	private static double calculateCongestionDegree( int bindingTime, int roundTripTime, Subtask subtask ){
		int difficulty = subtask.reqRes[ subtask.resType ];
		return difficulty / ( bindingTime - 2.0 * roundTripTime );
	}

	void clear() {
	}
}
