/**
 * @author Funato
 * @version 2.0
 */

package main.research.agent;


import main.research.Manager;
import main.research.SetParam;
import main.research.communication.Message;
import main.research.communication.TransmissionPath;
import main.research.random.MyRandom;
import main.research.agent.strategy.LeaderStrategy;
import main.research.agent.strategy.MemberStrategy;
import main.research.task.Subtask;
import main.research.task.Task;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Agent implements SetParam, Cloneable {
	public static int _id = 0;
	private static int[] resSizeArray = new int[RESOURCE_TYPES + 1];

	public static int _coalition_check_end_time;
	private static double ε = INITIAL_ε;
	private LeaderStrategy ls;
	private MemberStrategy ms;

	// リーダーもメンバも持つパラメータ
	public int id;
	public Point p;
	public int role = JONE_DOE;
	public int phase = SELECT_ROLE;
	public int[] resources = new int[RESOURCE_TYPES];
	public int[] workWithAsL = new int[AGENT_NUM];
	public int[] workWithAsM = new int[AGENT_NUM];
	public int validatedTicks = 0;
	public double e_leader = INITIAL_VALUE_OF_DSL;
	public double e_member = INITIAL_VALUE_OF_DSM;
	public List<Message> messages = new ArrayList<>();
	public int principle = RATIONAL;
	public int[] required = new int[RESOURCE_TYPES];            // そのリソースを要求するサブタスクが割り当てられた回数
	public int[][] allocated = new int[AGENT_NUM][RESOURCE_TYPES]; // そのエージェントからそのリソースを要求するサブタスクが割り当てられた回数
	public List<Agent> agentsCommunicatingWith = new ArrayList<>(); // 今通信をしていて，返信を期待しているエージェントのリスト．返信が返ってきたらリストから消す

	// リーダーエージェントのみが持つパラメータ
	public int didTasksAsLeader = 0;
	public List<Agent> candidates;         // これからチームへの参加を要請するエージェントのリスト
	public int proposalNum = 0;            // 送ったproposalの数を覚えておく
	public List<Agent> teamMembers;        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
	public List<Message> replies;
	public List<Message> results;
	public Task myTask;                  // 持ってきた(割り振られた)タスク
	public int restSubtask;               // 残りのサブタスク数
	public int replyNum = 0;
	public double threshold_for_reciprocity_as_leader;
	public List<Task> pastTasks = new ArrayList<>();
	public Map<Agent, Double> reliabilityRankingAsL = new LinkedHashMap<>(HASH_MAP_SIZE);

	// メンバエージェントのみが持つパラメータ
	public int didTasksAsMember = 0;
	public Agent myLeader;
	public int executionTime = 0;
	public double threshold_for_reciprocity_as_member;
	public Subtask mySubtask;
	public List<Subtask> mySubtaskQueue = new ArrayList<>();       // メンバはサブタスクを溜め込むことができる(実質的に，同時に複数のチームに参加することができるようになる)
	public int tbd = 0;                                            // 返事待ちの数
	public Map<Agent, Double> reliabilityRankingAsM = new LinkedHashMap<>(HASH_MAP_SIZE);
	public List<Agent> myLeaders = new ArrayList<>();

	public Agent(LeaderStrategy ls, MemberStrategy ms) {
		this.id = _id++;

		int resSum = Arrays.stream(resources).sum();
		int resCount = (int) Arrays.stream(resources)
			.filter(res -> res > 0)
			.count();
		setResource();
		setStrategies(ls, ms);
		threshold_for_reciprocity_as_leader = THRESHOLD_FOR_RECIPROCITY_FROM_LEADER;
		threshold_for_reciprocity_as_member = (double) resSum / resCount * THRESHOLD_FOR_RECIPROCITY_RATE;
		selectRole();
	}

	private void setResource() {
		int resSum;
		do {
			resSum = 0;
			for (int i = 0; i < RESOURCE_TYPES; i++) {
				int rand = MyRandom.getRandomInt(MIN_AGENT_RESOURCE_SIZE, MAX_AGENT_RESOURCE_SIZE);
				resources[i] = rand;
				resSum += rand;
			}
		} while (resSum == 0);
	}

	public void setPosition(int x, int y) {
		this.p = new Point(x, y);
	}

	void setReliabilityRankingRandomly(List<Agent> agentList) {
		List<Agent> rl = generateRandomAgentList(agentList);
		for (Agent ag : rl) {
			this.reliabilityRankingAsL.put(ag, INITIAL_VALUE_OF_DE);
		}
		List<Agent> rm = generateRandomAgentList(agentList);
		for (Agent ag : rm) {
			this.reliabilityRankingAsM.put(ag, INITIAL_VALUE_OF_DE);
		}
	}

	private List<Agent> generateRandomAgentList(List<Agent> agentList) {
		List<Agent> originalList = new ArrayList<>(agentList);
		List<Agent> randomAgentList = new ArrayList<>();

		originalList.remove(this);

		int size = originalList.size();

		int index;
		Agent ag;
		for (int i = 1; i <= size; i++) {
			index = MyRandom.getRandomInt(0, size - i);
			ag = originalList.remove(index);
			randomAgentList.add(ag);
		}
		return randomAgentList;
	}

	public void actAsLeader() {
		ls.actAsLeader(this);
	}

	public void actAsMember() {
		ms.actAsMember(this);
	}

	public void selectRole() {
		validatedTicks = Manager.getTicks();
		if (mySubtaskQueue.size() > 0) {
			mySubtask = mySubtaskQueue.remove(0);
			myLeader = mySubtask.from;
			role = MEMBER;
			this.phase = EXECUTION;
		}
//		if (epsilonGreedy()) {
//			if (e_leader < e_member) {
//				role = LEADER;
//				this.phase = PROPOSITION;
//				candidates = new ArrayList<>();
//				teamMembers = new ArrayList<>();
//				preAllocations = new HashMap<>();
//				replies = new ArrayList<>();
//				results = new ArrayList<>();
//			} else if (e_member < e_leader) {
//				role = MEMBER;
//				this.phase = WAITING;
//			} else {
//// */
//				int ran = MyRandom.getRandomInt(0, 1);
//				if (ran == 0) {
//					role = LEADER;
//					this.phase = PROPOSITION;
//					candidates = new ArrayList<>();
//					teamMembers = new ArrayList<>();
//					preAllocations = new HashMap<>();
//					replies = new ArrayList<>();
//					results = new ArrayList<>();
//				} else {
//					role = MEMBER;
//					this.phase = WAITING;
//				}
//			}
//			// εじゃない時
//		} else {
			if (e_leader > e_member) {
				role = LEADER;
				this.phase = PROPOSITION;
				candidates = new ArrayList<>();
				teamMembers = new ArrayList<>();
				replies = new ArrayList<>();
				results = new ArrayList<>();
			} else if (e_member > e_leader) {
				role = MEMBER;
				this.phase = WAITING;
			} else {
				int ran = MyRandom.getRandomInt(0, 1);
				if (ran == 0) {
					role = LEADER;
					this.phase = PROPOSITION;
					candidates = new ArrayList<>();
					teamMembers = new ArrayList<>();
					replies = new ArrayList<>();
					results = new ArrayList<>();
				} else {
					role = MEMBER;
					this.phase = WAITING;
				}
			}
	//	}
	}

	void selectRoleWithoutLearning() {
		int ran = MyRandom.getRandomInt(0, 6);
		if (mySubtaskQueue.size() > 0) {
			mySubtask = mySubtaskQueue.remove(0);
			myLeader = mySubtask.from;
			role = MEMBER;
			this.phase = EXECUTION;
		}
		if (ran == 0) {
			role = LEADER;
			e_leader = 1;
			this.phase = PROPOSITION;
			candidates = new ArrayList<>();
			teamMembers = new ArrayList<>();
			replies = new ArrayList<>();
			results = new ArrayList<>();
		} else {
			role = MEMBER;
			e_member = 1;
			this.phase = WAITING;
		}
	}

	/**
	 * inactiveメソッド
	 * チームが解散になったときに待機状態になる.
	 */
	public void inactivate(double success) {
		if (role == LEADER) {
			e_leader = e_leader * (1.0 - α) + α * success;

			if (e_leader < 0) e_leader = 0.001;

//            e_member = 1.0 -e_leader;

			assert e_leader <= 1 && e_leader >= 0 : "Illegal adaption to role";
		} else {
			e_member = e_member * (1.0 - α) + α * success;

//            e_leader = 1.0 - e_member;

			assert e_member <= 1 && e_member >= 0 : "Illegal adaption to role";
		}

//        if( (e_member + e_leader) != 1.0  ) System.out.println("Illegal renewal ");

		if (role == LEADER) {
			if (myTask != null) {
				System.out.println("バカな");
				myTask = null;
			}
			candidates.clear();
			teamMembers.clear();
			replies.clear();
			results.clear();
			restSubtask = 0;
			proposalNum = 0;
			replyNum = 0;
		}
		mySubtask = null;
		role = JONE_DOE;
		phase = SELECT_ROLE;
		myLeader = null;
		executionTime = 0;
		this.validatedTicks = Manager.getTicks();
	}

	public void sendMessage(Agent from, Agent to, int type, Object o) {
		TransmissionPath.sendMessage(new Message(from, to, type, o));
	}

	public void sendNegative(Agent ag, Agent to, int type, Subtask subtask) {
		if (type == PROPOSAL) {
			// 今実行しているサブタスクをくれたリーダーが，実行中にもかかわらずまた要請を出して来たらその旨を伝える
			if (ag.phase == EXECUTION && to.equals(ag.myLeader)) {
				sendMessage(ag, to, REPLY, REJECT_FOR_DOING_YOUR_ST);
			} else {
				sendMessage(ag, to, REPLY, REJECT);
			}
		} else if (type == REPLY) {
			sendMessage(ag, to, RESULT, null);
		} else if (type == RESULT) {
//            sendMessage(agent, to, SUBTASK_RESULT, subtask);
		}
	}

	/**
	 * calcExecutionTimeメソッド
	 * 引数のエージェントが引数のサブタスクを処理できなければ-1を返す．
	 * できるのであれば，その処理時間(>0)を返す
	 *
	 * @param a
	 * @param st
	 * @return
	 */
	public int calcExecutionTime(Agent a, Subtask st) {
		if (a == null) System.out.println("Ghost trying to do subtask");
		if (st == null) System.out.println("Agent trying to do nothing");

		if (a.resources[st.resType] == 0) return -1;
		return (int) Math.ceil((double) st.reqRes[st.resType] / (double) a.resources[st.resType]);
	}

	/**
	 * checkMessagesメソッド
	 * selfに届いたメッセージcheckListの中から,
	 * 期待するタイプで期待するエージェントからの物だけを戻す.
	 * それ以外はネガティブな返事をする
	 */
	public void checkMessages(Agent self) {
		if (self.role == LEADER) {
			ls.checkMessages(self);
		} else if (self.role == MEMBER) {
			ms.checkMessages(self);
		}
	}

	/**
	 * inTheListメソッド
	 * 引数のエージェントが引数のリスト内にあればその索引を, いなければ-1を返す
	 */
	public int inTheList(Object a, List List) {
		for (int i = 0; i < List.size(); i++) {
			if (a.equals(List.get(i))) return i;
		}
		return -1;
	}

	public boolean haveAlreadyJoined(Agent member, Agent target) {
		if (member.myLeader == target) {
			return true;
		}
		return inTheList(target, myLeaders) >= 0 ? true : false;
	}


	public boolean epsilonGreedy() {
		double random = MyRandom.getRandomDouble();
		return random < ε;
	}

	/**
	 * taskIDを元にpastTaskからTaskを同定しそれを返す
	 *
	 * @param taskID ... taskのID
	 */
	public Task identifyTask(int taskID) {
		Task temp = null;
		for (Task t : pastTasks) {
			if (t.task_id == taskID) {
				temp = t;
				break;
			}
		}
		assert temp != null : "Did phantom task!";
		return temp;
	}

	/**
	 * nextPhaseメソッド
	 * phaseの変更をする
	 * 同時にvalidTimeを更新する
	 */
	public void nextPhase() {
		if (this.phase == PROPOSITION) this.phase = REPORT;
		else if (this.phase == WAITING) this.phase = RECEPTION;
		else if (this.phase == REPORT) {
			if (_coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
				if (role == LEADER) {
					for (Agent ag : teamMembers) workWithAsL[ag.id]++;
				} else {
					workWithAsM[myLeader.id]++;
				}
			}
			// 自分のサブタスクが終わったら役割適応度を1で更新して非活性状態へ
			inactivate(1);
		} else if (this.phase == RECEPTION) this.phase = EXECUTION;
		this.validatedTicks = Manager.getTicks();
	}

	public static void renewEpsilonLinear() {
		ε -= DIFFERENCE;
		if (ε < FLOOR) ε = FLOOR;
	}

	public static void renewEpsilonExponential() {
		ε = (ε - FLOOR) * RATE;
		ε += FLOOR;
	}

	public static void clear() {
		_id = 0;
		_coalition_check_end_time = SNAPSHOT_TIME;
		ε = INITIAL_ε;
		for (int i = 0; i < RESOURCE_TYPES; i++) resSizeArray[i] = 0;
	}

	// 結果集計用のstaticメソッド
	public static void resetWorkHistory(List<Agent> agents) {
		for (Agent ag : agents) {
			for (int i = 0; i < AGENT_NUM; i++) {
				ag.workWithAsM[i] = 0;
				ag.workWithAsL[i] = 0;
			}
		}
		_coalition_check_end_time = MAX_TURN_NUM;
	}

	public static int countReciprocalMember(List<Agent> agents) {
		int temp = 0;
		for (Agent ag : agents) {
			if (ag.e_member > ag.e_leader && ag.principle == RECIPROCAL) {
				temp++;
			}
		}
		return temp;
	}


	/**
	 * agentsの中でspan以上の時間誰からの依頼も受けずチームに参加していないメンバ数を返す．
	 *
	 * @param agents
	 * @param span
	 * @return
	 */
	public static int countNEETmembers(List<Agent> agents, int span) {
		int neetM = 0;
		int now = Manager.getTicks();
		for (Agent ag : agents) {
			if (now - ag.validatedTicks > span) {
				neetM++;
			}
		}
		return neetM;
	}

	@Override
	public Agent clone() { //基本的にはpublic修飾子を付け、自分自身の型を返り値とする
		Agent b = null;

		try {
			b = (Agent) super.clone(); //親クラスのcloneメソッドを呼び出す(親クラスの型で返ってくるので、自分自身の型でのキャストを忘れないようにする)
		} catch (Exception e) {
			e.printStackTrace();
		}
		return b;
	}

	@Override
	public String toString() {
		String sep = System.getProperty("line.separator");
		StringBuilder str = new StringBuilder();
		str = new StringBuilder(String.format("%3d", id));
//        str = new StringBuilder("ID:" + String.format("%3d", id) + ", " + "x: " + x + ", y: " + y + ", ");
//        str = new StringBuilder("ID:" + String.format("%3d", id) + "  " + messages );
//        str = new StringBuilder("ID: " + String.format("%3d", id) + ", " + String.format("%.3f", e_leader) + ", " + String.format("%.3f", e_member)  );
//        str = new StringBuilder("ID:" + String.format("%3d", id) + ", Resources: " + resSize + ", " + String.format("%3d", didTasksAsMember)  );
/*
        if( this.principle == RECIPROCAL ) {
            str.append(", The most reliable agent: " + relRanking.get(0).id + "← reliability: " + reliabilities[relRanking.get(0).id]);
            str.append(", the delay: " + main.research.Manager.delays[this.id][relRanking.get(0).id]);
        }
// */
		str.append("[");
		for (int i = 0; i < RESOURCE_TYPES; i++) str.append(String.format("%3d", resources[i]) + ",");
		str.append("]");
// */
        /*
        if( role == LEADER ) {
            List<Agent> temp = new ArrayList<>();
            temp.addAll(candidates);
            temp.addAll(teamMembers);
            str.append( " Waiting: " );
            for( Agent ag: temp ) str.append( String.format("%3d", ag.id ) + ", ");
        }
// */
/*       if (e_member > e_leader) str.append(", member: ");
        else if (e_leader > e_member) str.append(", leader: ");
        else if (role == JONE_DOE) str.append(", free: ");
//        str.append(String.format(", %.3f", e_leader) + ", " + String.format("%.3f", e_member) + sep);

        for( int i = 0; i < AGENT_NUM; i++ ) str.append( i + ": " + String.format("%.3f", reliabilities[i] ) + ",  " );
// */
/*        str.append("e_leader: " + e_leader + ", e_member: " + e_member);
        if( role == MEMBER ) str.append(", member: ");
        else if( role == LEADER ) str.append(", leader: " );
        else if( role == JONE_DOE ) str.append(", free: ");
/*
        if( phase == REPORT ){
            str.append(" allocations: " + allocations  );
//            str.append(" teamMembers: " + teamMembers );
        }
        else if( phase == RECEPTION ) str.append(", My Leader: " + leader.id);
        else if( phase == EXECUTION ) str.append(", My Leader: " + leader.id + ", " + mySubtask + ", resources: " + resource + ", rest:" + executionTime);

//        if (role == LEADER) str.append(", I'm a leader. ");
//        else str.append("I'm a member. ");
// */
/*
        if (relAgents.size() != 0) {
            int temp;
            str.append(sep + "   Reliable Agents:");
            for (int i = 0; i < relAgents.size(); i++) {
                temp = relAgents.get(i).id;
                str.append(String.format("%3d", temp));
            }
        }
// */
/*
        int temp;
        str.append( sep + "   Reliability Ranking:");
        for( int i = 0; i < 5; i++ ){
            temp = relRanking.get(i).id;
            str.append(String.format("%3d", temp) );
            str.append("→" + reliabilities[temp]);
        }
// */
// */
        /*
        if( role == MEMBER ) {
            str.append(",  Reliable Agents:");
            for (int i = 0; i < MAX_REL_AGENTS; i++) {
                temp = relAgents.get(i);
                str.append(String.format("%3d", temp));
            }
        }
//        */

		return str.toString();
	}

	private void setStrategies(LeaderStrategy ls, MemberStrategy ms) {
		this.ls = ls;
		this.ms = ms;
	}

	public static class AgentExcellenceComparator implements Comparator<Agent> {
		public int compare(Agent a, Agent b) {
			Integer resCount_a = (int) Arrays.stream(a.resources)
				.filter(resource -> resource > 0)
				.count();
			Integer resCount_b = (int) Arrays.stream(b.resources)
				.filter(resource -> resource > 0)
				.count();

			Double excellence_a = (double) Arrays.stream(a.resources).sum() / resCount_a;
			Double excellence_b = (double) Arrays.stream(b.resources).sum() / resCount_b;

			int result = excellence_a.compareTo(excellence_b);
			if (result != 0) return result;

			result = resCount_a.compareTo(resCount_b);
			if (result != 0) return result;

			return a.id >= b.id ? 1 : -1;
		}
	}


	public static class AgentIDcomparator implements Comparator<Agent> {
		public int compare(Agent a, Agent b) {
			return a.id >= b.id ? 1 : -1;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Agent agent = (Agent) o;
		return id == agent.id &&
			p.equals(agent.p);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, p);
	}
}
