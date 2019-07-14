/**
 * @author Funato
 * @version 2.0
 */

package main.research.agent;


import main.research.*;
import main.research.communication.Message;
import main.research.communication.TransmissionPath;
import main.research.grid.Coordinates;
import main.research.random.MyRandom;
import main.research.strategy.LeaderStrategy;
import main.research.strategy.MemberStrategy;
import main.research.strategy.Strategy;
import main.research.task.Subtask;
import main.research.task.Task;
import java.util.*;
import java.util.stream.Collectors;

public class Agent implements SetParam , Cloneable {
    public static int _id = 0;
    public static int _leader_num = 0;
    public static int _member_num = 0;
    public static int _recipro_num = 0;
    public static int _rational_num = AGENT_NUM;
    private static int[] resSizeArray = new int[RESOURCE_TYPES + 1];

    public static int _coalition_check_end_time;
    private static double ε = INITIAL_ε;
    private static LeaderStrategy sl;
    private static MemberStrategy sm;

    // リーダーもメンバも持つパラメータ
    public int id;
    public Coordinates p;
    public int role = JONE_DOE;
    public int phase = SELECT_ROLE;
    private int resSum = 0, resCount = 0;
    public int[] res = new int[RESOURCE_TYPES];
    public double excellence;
    public int didTasksAsLeader = 0;
    public int didTasksAsMember = 0;
    public int[] workWithAsL = new int[AGENT_NUM];
    public int[] workWithAsM = new int[AGENT_NUM];
    public int validatedTicks = 0;
    public boolean joined = false;
    public double e_leader = INITIAL_VALUE_OF_DSL;
    public double e_member = INITIAL_VALUE_OF_DSM;
    public List<Message> messages = new ArrayList<>();
    public int principle = RATIONAL;
    public int executionTime = 0;
    public int start = 0;                                          // その時のチーム参加要請を送った時刻
    public int[] required = new int[RESOURCE_TYPES];            // そのリソースを要求するサブタスクが割り当てられた回数
    public int[][] allocated = new int[AGENT_NUM][RESOURCE_TYPES]; // そのエージェントからそのリソースを要求するサブタスクが割り当てられた回数
    public List<Agent> canReach = new ArrayList<>();
    public List<Agent> agentsCommunicatingWith = new ArrayList<>(); // 今通信をしていて，返信を期待しているエージェントのリスト．返信が返ってきたらリストから消す

    // リーダーエージェントが持つパラメータ
    public List<Agent> candidates;         // これからチームへの参加を要請するエージェントのリスト
    public int proposalNum = 0;            // 送ったproposalの数を覚えておく
    public List<Agent> teamMembers;        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
    public Map<Agent, Subtask> preAllocations;       // サブタスクの割り当て候補を< agent, subtask >のHashMapで保持
    public List<Message> replies;
    public List<Message> results;
    public Task ourTask;                  // 持ってきた(割り振られた)タスク
    public int restSubtask;               // 残りのサブタスク数
    private int index = 0;                 // 一回要請を送ったメンバにもう一度送らないようにindex管理
    public int replyNum = 0;
    public double threshold_for_reciprocity_as_leader;
    public List<Task> pastTasks = new ArrayList<>();
    public Map<Agent, Double> relRanking_l = new LinkedHashMap<>();

    // メンバエージェントのみが持つパラメータ
    public Agent leader;
    public double threshold_for_reciprocity_as_member;
    public Subtask mySubtask;
    public List<Subtask> mySubtaskQueue = new ArrayList<>();       // メンバはサブタスクを溜め込むことができる(実質的に，同時に複数のチームに参加することができるようになる)
    public int tbd = 0;                                            // 返事待ちの数
    public Map<Agent, Double> relRanking_m = new LinkedHashMap<>();
    public List<Agent> myLeaders = new ArrayList<>();

    public Agent(LeaderStrategy sl, MemberStrategy sm) {
        this.id = _id++;
        this.sl = sl;
        this.sm = sm;

        setResource();
        threshold_for_reciprocity_as_leader = THRESHOLD_FOR_RECIPROCITY_FROM_LEADER;
        threshold_for_reciprocity_as_member = (double) resSum / resCount * THRESHOLD_FOR_RECIPROCITY_RATE;
        selectRole();

        initiateParameters();
    }

    private void setResource() {
        while (resSum == 0) {
            for (int i = 0; i < RESOURCE_TYPES; i++) {
                int rand = MyRandom.getRandomInt(MIN_AGENT_RESOURCE_SIZE, MAX_AGENT_RESOURCE_SIZE);
                res[i] = rand;
                if (rand > 0) resCount++;
                resSum += rand;
            }
        }
        excellence = (double) resSum / resCount;
    }

    public void setPosition(int x, int y) {
        this.p = new Coordinates(x,y);
    }

    void setReliabilityRankingRandomly(List<Agent> agentList) {
        List<Agent> rl = generateRandomAgentList(agentList);
        this.relRanking_l = rl.stream()
                .collect( Collectors.toMap(
                        ag -> ag,
                        ag -> INITIAL_VALUE_OF_DSL
                ));

        List<Agent> rm = generateRandomAgentList(agentList);
        this.relRanking_m = rm.stream()
                .collect( Collectors.toMap(
                        ag -> ag,
                        ag -> INITIAL_VALUE_OF_DSM
                ));
    }

    private List<Agent> generateRandomAgentList( List<Agent> agentList ) {
        List<Agent> originalList = new ArrayList<>(agentList);
        List<Agent> randomAgentList = new ArrayList<>();

        originalList.remove(this);

        int size = originalList.size();

        int index;
        Agent ag;
        for (int i = 1; i <= size; i++) {
            index = MyRandom.getRandomInt( 0, size - i );
            ag = originalList.remove( index );
            randomAgentList.add(ag);
        }
        return randomAgentList;
    }

    private void initiateParameters() {
        int hashMapSize = (int) (AGENT_NUM * 1.3);
        this.relRanking_l = new LinkedHashMap<>(hashMapSize);
        this.relRanking_m = new LinkedHashMap<>(hashMapSize);
    }

    public void actAsLeader() {
        sl.actAsLeader(this);
    }

    public void actAsMember() {
        sm.actAsMember(this);
    }

    public void selectRole() {
        validatedTicks = Manager.getTicks();
        if (mySubtaskQueue.size() > 0) {
            mySubtask = mySubtaskQueue.remove(0);
            leader = mySubtask.from;
            role = MEMBER;
            this.phase = EXECUTION;
        }
//        if (epsilonGreedy()) {
//            if (e_leader < e_member) {
//                role = LEADER;
//                _leader_num++;
//                this.phase = PROPOSITION;
//                candidates = new ArrayList<>();
//                teamMembers = new ArrayList<>();
//                preAllocations = new HashMap<>();
//                replies = new ArrayList<>();
//                results = new ArrayList<>();
//            } else if (e_member < e_leader) {
//                role = MEMBER;
//                _member_num++;
//                this.phase = WAITING;
//            } else {
//// */
//                int ran = MyRandom.getRandomInt(2);
//                if (ran == 0) {
//                    role = LEADER;
//                    _leader_num++;
//                    this.phase = PROPOSITION;
//                    candidates = new ArrayList<>();
//                    teamMembers = new ArrayList<>();
//                    preAllocations = new HashMap<>();
//                    replies = new ArrayList<>();
//                    results = new ArrayList<>();
//                } else {
//                    role = MEMBER;
//                    _member_num++;
//                    this.phase = WAITING;
//                }
//            }
//        // εじゃない時
//        } else {
        if (e_leader > e_member) {
            role = LEADER;
            _leader_num++;
            this.phase = PROPOSITION;
            candidates = new ArrayList<>();
            teamMembers = new ArrayList<>();
            preAllocations = new HashMap<>();
            replies = new ArrayList<>();
            results = new ArrayList<>();
        } else if (e_member > e_leader) {
            role = MEMBER;
            _member_num++;
            this.phase = WAITING;
        } else {
            int ran = MyRandom.getRandomInt(0, 1);
            if (ran == 0) {
                role = LEADER;
                _leader_num++;
                this.phase = PROPOSITION;
                candidates = new ArrayList<>();
                teamMembers = new ArrayList<>();
                preAllocations = new HashMap<>();
                replies = new ArrayList<>();
                results = new ArrayList<>();
            } else {
                role = MEMBER;
                _member_num++;
                this.phase = WAITING;
            }
//            }
        }
    }

    void selectRoleWithoutLearning() {
        int ran = MyRandom.getRandomInt(0, 6);
        if (mySubtaskQueue.size() > 0) {
            mySubtask = mySubtaskQueue.remove(0);
            leader = mySubtask.from;
            role = MEMBER;
            _member_num++;
            this.phase = EXECUTION;
        }
        if (ran == 0) {
            role = LEADER;
            _leader_num++;
            e_leader = 1;
            this.phase = PROPOSITION;
            candidates = new ArrayList<>();
            teamMembers = new ArrayList<>();
            preAllocations = new HashMap<>();
            replies = new ArrayList<>();
            results = new ArrayList<>();
        } else {
            role = MEMBER;
            e_member = 1;
            _member_num++;
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
            _leader_num--;
            if (ourTask != null) {
                System.out.println("バカな");
                ourTask = null;
            }
            candidates.clear();
            teamMembers.clear();
            preAllocations.clear();
            replies.clear();
            results.clear();
            restSubtask = 0;
            proposalNum = 0;
            replyNum = 0;
        } else {
            _member_num--;
        }
        mySubtask = null;
        joined = false;
        role = JONE_DOE;
        phase = SELECT_ROLE;
        leader = null;
        executionTime = 0;
        this.validatedTicks = Manager.getTicks();
    }

    public void sendMessage(Agent from, Agent to, int type, Object o) {
        TransmissionPath.sendMessage(new Message(from, to, type, o));
    }

    public void sendNegative(Agent ag, Agent to, int type, Subtask subtask) {
        if (type == PROPOSAL) {
            // 今実行しているサブタスクをくれたリーダーが，実行中にもかかわらずまた要請を出して来たらその旨を伝える
            if (ag.phase == EXECUTION && to.equals(ag.leader)) {
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

        if (a.res[st.resType] == 0) return -1;
        return (int) Math.ceil((double) st.reqRes[st.resType] / (double) a.res[st.resType]);
    }

    /**
     * checkMessagesメソッド
     * selfに届いたメッセージcheckListの中から,
     * 期待するタイプで期待するエージェントからの物だけを戻す.
     * それ以外はネガティブな返事をする
     */
    public void checkMessages(Agent self) {
        if( self.role == LEADER ) {
                sl.checkMessages(self);
        }else if( self.role == MEMBER ) {
            sm.checkMessages(self);
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
        if (member.leader == target) {
            return true;
        }
        return inTheList(target, myLeaders) >= 0 ? true : false;
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

    public boolean epsilonGreedy() {
        double random = MyRandom.getRandomDouble();
        if (random < ε) {
            return true;
        }
        return false;
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
            if (this.executionTime < 0) {
                if (_coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    if (role == LEADER) {
                        for (Agent ag : teamMembers) workWithAsL[ag.id]++;
                    } else {
                        workWithAsM[leader.id]++;
                    }
                }
                // 自分のサブタスクが終わったら役割適応度を1で更新して非活性状態へ
                inactivate(1);
            } else {
                this.phase = EXECUTION;
            }
        } else if (this.phase == RECEPTION) this.phase = EXECUTION;
        this.validatedTicks = Manager.getTicks();
    }

    public static void renewEpsilonLenear() {
        ε -= DIFFERENCE;
        if (ε < FLOOR) ε = FLOOR;
    }

    public static void renewEpsilonExponential() {
        ε = (ε - FLOOR) * RATE;
        ε += FLOOR;
    }

    public static void clearA() {
        _id = 0;
        _leader_num = 0;
        _member_num = 0;
        _rational_num = AGENT_NUM;
        _recipro_num = 0;
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
        for (int i = 0; i < RESOURCE_TYPES; i++) str.append(String.format("%3d", res[i]) + ",");
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

    public static class AgentExceComparator implements Comparator<Agent> {
        public int compare(Agent a, Agent b) {
            double no1 = a.excellence;
            int no11 = a.resCount;
            int no12 = a.id;
            double no2 = b.excellence;
            int no21 = b.resCount;
            int no22 = b.id;

            // excellenceで比べる
            if (no1 > no2) return 1;
            else if (no1 < no2) return -1;
                // excellenceが等しいなら0でないリソースの数で比べる
            else if (no11 > no21) return 1;
            else if (no11 < no21) return -1;
                // それも一緒ならidで比べる
            else if (no12 < no22) return 1;
            else return -1;
        }
    }


    public static class AgentIDcomparator implements Comparator<Agent> {
        public int compare(Agent a, Agent b) {
            int no1 = a.id;
            int no2 = b.id;

            if (no1 > no2) return 1;
            else return -1;
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
