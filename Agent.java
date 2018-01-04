/**
 * @author Funato
 * @version 2.0
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Agent implements SetParam , Cloneable{
    static int _id = 0;
    static int _leader_num = 0;
    static int _member_num = 0;
    static int _recipro_num = 0;
    static int _rational_num = AGENT_NUM;
    static long _seed;
    static Random _randSeed;
    static int[] resSizeArray = new int[RESOURCE_TYPES + 1];
    static int _coalition_check_end_time = SNAPSHOT_TIME;
    static List<Integer> _lonelyAgents = new ArrayList<>();

    // リーダーもメンバも持つパラメータ
    int id;
    int x, y;
    int role = JONE_DOE;
    int phase = SELECT_ROLE;
    Strategy strategy;
    int resSize = 0;
    int res[] = new int[RESOURCE_TYPES];
    int didTasksAsLeader = 0;
    int didTasksAsMember = 0;
    int[] workWithAsL = new int[AGENT_NUM];
    int[] workWithAsM = new int[AGENT_NUM];
    int validatedTicks = 0;
    boolean joined = false;
    double e_leader = INITIAL_VALUE_OF_DSL, e_member = INITIAL_VALUE_OF_DSM;
    SubTask mySubTask;
    double[] reliabilities = new double[AGENT_NUM];
    List<Message> messages;
    List<Agent> relAgents = new ArrayList<>();
    List<Agent> relRanking = new ArrayList<>();
    int principle = RATIONAL;
    int executionTime = 0;
    int start = 0;                 // その時のチーム参加要請を送った時刻
    int isLonely = 0;              // 過疎地域のエージェントだったら1
    // リーダーエージェントが持つパラメータ
    List<Agent> candidates;         // これからチームへの参加を要請するエージェントのリスト
    List<Agent> teamMembers;        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
    List<Allocation> allocations;       // サブタスクの割り当て候補を< agent, subtask >のリストで保持
    List<Message> replies;
    List<Message> results;
    List<Agent> prevTeamMember;
    Task ourTask;                  // 持ってきた(割り振られた)タスク
    int restSubTask;               // 残りのサブタスク数
    int index = 0;                 // 一回要請を送ったメンバにもう一度送らないようにindex管理
    int replyNum = 0;
    int prevIndex = 0;
    int acceptances = 0;           // 今まで自分の元に帰って来た受理応答
    int untilAcceptances = 0;      // 今まで自分の元に返って来た受理応答の合計応答時間(= 往復の通信時間 + メンバの処理時間)
    double meanUA = 0;             // 今まで自分の元に返って来た受理応答の平均応答時間(=  untilAcceptances/acceptances)
    // メンバエージェントのみが持つパラメータ
    Agent leader;
    int totalOffers = 0;            // 今まで自分が受理して来たオファー数
    int totalResponseTicks = 0;     // 受理応答からの待ち時間の合計
    double meanRT = 0;     // 受理応答からの待ち時間の平均

    // seedが変わった(各タームの最初の)エージェントの生成
    Agent(long seed, int x, int y, Strategy strategy) {
        setSeed(seed);
        int rand;
        this.id = _id;
        _id++;
        this.x = x;
        this.y = y;
        this.strategy = strategy;
        setResource(UNIFORM);
        Arrays.fill(reliabilities, INITIAL_VALUE_OF_DEC);
        if (strategy.getClass().getName() == "RoundRobin" || strategy.getClass().getName() == "ProximityOriented") {
            selectRoleWithoutLearning();
        } else {
            selectRole();
        }
        messages = new ArrayList<>();
    }

    // 残りのエージェントの生成
    Agent(int x, int y, Strategy strategy) {
        int rand;
        this.id = _id;
        _id++;
        this.x = x;
        this.y = y;
        this.strategy = strategy;
        setResource(UNIFORM);
        Arrays.fill(reliabilities, INITIAL_VALUE_OF_DEC);
        if (strategy.getClass().getName() == "RoundRobin" || strategy.getClass().getName() == "ProximityOriented") {
            selectRoleWithoutLearning();
        } else {
            selectRole();
        }
        messages = new ArrayList<>();
    }

    void setResource(int agentType) {
        if (agentType == BIAS) {
/*            for (int i = 0; i < RESOURCE_NUM; i++) {
                int rand = random.nextInt(3) + 6;
                res[i] = rand;
            }
*/
        } else {
            while (resSize == 0) {
                for (int i = 0; i < RESOURCE_TYPES; i++) {
                    int rand = _randSeed.nextInt(MAX_AGENT_RESOURCE_SIZE + 1);
                    res[i] = rand;
                    resSize += rand;
                }
            }
            resSizeArray[resSize]++;
        }
    }

    void act() {
//        if (strategy.getClass().getName() == "RandomStrategy") strategy.act(this, action);
        if (phase == SELECT_ROLE) selectRole();
        else strategy.act(this);
    }

    /**
     * doRoleメソッド
     * eの値によって次の自分の役割を選択する.
     */
    void selectRole() {
        validatedTicks = Manager.getTicks();
        if (epsilonGreedy()) {
            if (e_leader < e_member) {
                role = LEADER;
                _leader_num++;
                this.phase = PROPOSITION;
                candidates = new ArrayList<>();
                teamMembers = new ArrayList<>();
                allocations = new ArrayList<>();
                replies = new ArrayList<>();
                results = new ArrayList<>();
            } else if (e_member < e_leader) {
                role = MEMBER;
                _member_num++;
                this.phase = WAITING;
            } else {
// */
                int ran = _randSeed.nextInt(2);
                if (ran == 0) {
                    role = LEADER;
                    _leader_num++;
                    this.phase = PROPOSITION;
                    candidates = new ArrayList<>();
                    teamMembers = new ArrayList<>();
                    allocations = new ArrayList<>();
                    replies = new ArrayList<>();
                    results = new ArrayList<>();
                } else {
                    role = MEMBER;
                    _member_num++;
                    this.phase = WAITING;
                }
            }
        } else {
            if (e_leader > e_member) {
                role = LEADER;
                _leader_num++;
                this.phase = PROPOSITION;
                candidates = new ArrayList<>();
                teamMembers = new ArrayList<>();
                allocations = new ArrayList<>();
                replies = new ArrayList<>();
                results = new ArrayList<>();
            } else if (e_member > e_leader) {
                role = MEMBER;
                _member_num++;
                this.phase = WAITING;
            } else {
                int ran = _randSeed.nextInt(2);
                if (ran == 0) {
                    role = LEADER;
                    _leader_num++;
                    this.phase = PROPOSITION;
                    candidates = new ArrayList<>();
                    teamMembers = new ArrayList<>();
                    allocations = new ArrayList<>();
                    replies = new ArrayList<>();
                    results = new ArrayList<>();
                } else {
                    role = MEMBER;
                    _member_num++;
                    this.phase = WAITING;
                }
            }
        }
    }

    void selectRoleWithoutLearning() {
        validatedTicks = Manager.getTicks();
        int ran = _randSeed.nextInt(5);
        if (ran == 0) {
            role = LEADER;
            _leader_num++;
            e_leader = 1;
            this.phase = PROPOSITION;
            candidates = new ArrayList<>();
            teamMembers = new ArrayList<>();
            allocations = new ArrayList<>();
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
    void inactivate(int success) {
        if (role == LEADER) {
            e_leader = e_leader * (1 - α) + α * success;
            assert e_leader <= 1 && e_leader >= 0 : "Illegal adaption to role";
        } else {
            e_member = e_member * (1 - α) + α * success;
            assert e_member <= 1 && e_member >= 0 : "Illegal adaption to role";
        }
        if (role == LEADER) {
            if (success == 1) {
                didTasksAsLeader++;
            }
            _leader_num--;
            if (ourTask != null) Manager.disposeTask(this);
            candidates.clear();
            teamMembers.clear();
            allocations.clear();
            replies.clear();
            results.clear();
            prevTeamMember = teamMembers;
            restSubTask = 0;
            replyNum = 0;
        } else {
            if (success == 1) didTasksAsMember++;
            _member_num--;
        }
        joined = false;
        role = JONE_DOE;
        phase = SELECT_ROLE;
        leader = null;
        mySubTask = null;
        executionTime = 0;
        if (strategy.getClass().getName() != "RoundRobin") {
            index = 0;
        } else {
            prevIndex = index % relAgents.size();
            index = prevIndex;
        }
        this.validatedTicks = Manager.getTicks();
//        System.out.println("ID: " + id + " is inactivated .");
    }

    /*
    学習しない戦略のinactivate
     */
    void inactivateWithNoLearning(int success) {
        if (role == LEADER) {
            if (success == 1) {
                didTasksAsLeader++;
            }
            if (ourTask != null) Manager.disposeTask(this);
            candidates.clear();
            teamMembers.clear();
            allocations.clear();
            replies.clear();
            results.clear();
            prevTeamMember = teamMembers;
            restSubTask = 0;
            replyNum = 0;
            role = LEADER;
            phase = PROPOSITION;
            candidates = new ArrayList<>();
            teamMembers = new ArrayList<>();
            allocations = new ArrayList<>();
            replies = new ArrayList<>();
            results = new ArrayList<>();
        } else {
            if (success == 1) didTasksAsMember++;
            role = MEMBER;
            this.phase = WAITING;
        }
        joined = false;
        leader = null;
        mySubTask = null;
        executionTime = 0;
        if (strategy.getClass().getName() != "RoundRobin") {
            index = 0;
        } else {
            prevIndex = index % relAgents.size();
            index = prevIndex;
        }
        this.validatedTicks = Manager.getTicks();
//        System.out.println("ID: " + id + " is inactivated .");
    }

    /**
     * selectSubtaskメソッド
     * リーダーが自分のやるサブタスクを選択するメソッド
     * まず自分に能力的に可能なサブタスクを探し, その中からランダムで選ぶ
     * なければ何もしない
     */
    void selectSubTask() {
        List<SubTask> temp = new ArrayList<>();
        // 自分に可能なサブタスクを抽出する
        for (SubTask st : ourTask.subTasks) {
            if (res[st.resType] == st.reqRes[st.resType]) temp.add(st);
        }
        // もし一つもなかったら仕方ないからなしでreturn
        if (temp.size() == 0) {
            temp = null;
            executionTime = 1;
            return;
        }
        // 一個でもあったらどれかを選んで自分のサブタスクとする
        else {
            int rand = _randSeed.nextInt(temp.size());
            mySubTask = temp.get(rand);
            ourTask.subTasks.remove(temp.get(rand));
            restSubTask--;
            executionTime = 1;
        }
    }

    void sendMessage(Agent from, Agent to, int type, Object o) {
        TransmissionPath.sendMessage(new Message(from, to, type, o));
    }

    void sendNegative(Agent agent, Agent to, int type, SubTask subTask) {
        if (type == PROPOSAL) {
            sendMessage(agent, to, REPLY, REJECT);
        } else if (type == REPLY) {
            sendMessage(agent, to, RESULT, null);
        } else if (type == RESULT) {
//            sendMessage(agent, to, SUBTASK_RESULT, subTask);
        }
    }

    public int calcExecutionTime(Agent agent) {
        SubTask st;
        st = agent.mySubTask;
/*        System.out.println( st );
        System.out.println( st.reqRes[st.resType] + ", " + agent.res[st.resType] );
// */
        return (int) Math.ceil((double) st.reqRes[st.resType] / (double) agent.res[st.resType]);
    }

    // 自分がリーダーの時とか正しく拒否メッセージが送れていない

    /**
     * checkMessagesメソッド
     * selfに届いたメッセージcheckListの中から,
     * 期待するタイプで期待するエージェントからの物だけを戻す.
     * それ以外はネガティブな返事をする
     */
    void checkMessages(Agent self) {
        int size = messages.size();
        Message m;
        if (size == 0) return;
//        System.out.println("ID: " + self.id + ", Phase: " + self.phase + " message:  "+ self.messages);
        // リーダーでPROPOSITION or 誰でもEXECUTION → 誰からのメッセージも期待していない
        if (self.phase == PROPOSITION || self.phase == EXECUTION) {
            for (int i = 0; i < size; i++) {
                m = messages.remove(0);
                sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
            // メンバでWAITING → PROPOSALを期待している
        } else if (self.phase == WAITING) {
            for (int i = 0; i < size; i++) {
                m = messages.remove(0);
                // PROPOSALで, 要求されているリソースを自分が持つならmessagesに追加
                if (m.getMessageType() == PROPOSAL && self.res[m.getResType()] != 0) {
                    messages.add(m);
                    // 違かったらsendNegative
                } else sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
        }
        // リーダーでREPORT → REPLYを期待している
        else if (self.phase == REPORT) {
            for (int i = 0; i < size; i++) {
                m = messages.remove(0);
                Agent from = m.getFrom();
                if (m.getMessageType() == REPLY && inTheList(from, self.candidates) > -1) replies.add(m);
                else sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
            // メンバでRECEPTION → リーダーからのRESULT(サブタスク割り当て)を期待している
        } else if (self.phase == RECEPTION) {
            for (int i = 0; i < size; i++) {
                m = messages.remove(0);
                if (m.getMessageType() == RESULT && m.getFrom() == self.leader) {
                    messages.add(m);
                    // 違かったらsendNegative
                } else sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
        }
    }

    /**
     * inTheListメソッド
     * 引数のエージェントが引数のリスト内にあればその索引を, いなければ-1を返す
     */
    protected int inTheList(Agent a, List<Agent> agents) {
        for (int i = 0; i < agents.size(); i++) {
            if (a == agents.get(i)) return i;
        }
        return -1;
    }

    /**
     * getAllocationメソッド
     * 引数のエージェントに割り当てるサブタスクを返す
     * 割り当て情報は保持
     */
    protected Allocation getAllocation(Agent agent) {
        int size = allocations.size();
        for (int i = 0; i < size; i++) {
            if (agent == allocations.get(i).getCandidate()) {
                return allocations.get(i);
            }
        }
        return null;
    }

    /**
     * removeAllocationメソッド
     * 引数のエージェントへの割り当て情報を消す
     */
    protected void removeAllocation(Agent agent) {
        int size = allocations.size();
        for (int i = 0; i < size; i++) {
            if (agent == allocations.get(i).getCandidate()) {
                allocations.remove(i);
                return;
            }
        }
    }

    /**
     * removeTeamMemberメソッド
     * 引数のエージェントへの割り当て情報を消す
     */
    protected void removeTeamMember(Agent agent) {
        int size = teamMembers.size();
        for (int i = 0; i < size; i++) {
            if (agent == teamMembers.get(i)) {
                teamMembers.remove(i);
                return;
            }
        }
    }

    protected boolean epsilonGreedy() {
        double random = _randSeed.nextDouble();
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
    protected void nextPhase() {
        if (this.phase == PROPOSITION) this.phase = REPORT;
        else if (this.phase == WAITING) this.phase = RECEPTION;
        else if (this.phase == REPORT) this.phase = EXECUTION;
        else if (this.phase == RECEPTION) this.phase = EXECUTION;
//        System.out.println(" phase : " + phase);
        this.validatedTicks = Manager.getTicks();
    }

    protected boolean canDo(Agent agent, SubTask st) {
        if (agent.res[st.resType] == st.reqRes[st.resType]) return true;
        else return false;
    }

    private static void setSeed(long seed) {
        _seed = seed;
        _randSeed = new Random(_seed);
    }

    static void clearA() {
        _id = 0;
        _leader_num = 0;
        _member_num = 0;
        _rational_num = AGENT_NUM;
        _recipro_num = 0;
        _coalition_check_end_time = SNAPSHOT_TIME;
        _lonelyAgents.clear();
        for (int i = 0; i < RESOURCE_TYPES; i++) resSizeArray[i] = 0;
    }

    @Override
    public Agent clone() { //基本的にはpublic修飾子を付け、自分自身の型を返り値とする
        Agent b = null;

       try {
            b=(Agent)super.clone(); //親クラスのcloneメソッドを呼び出す(親クラスの型で返ってくるので、自分自身の型でのキャストを忘れないようにする)
        }catch (Exception e){
            e.printStackTrace();
        }
        return b;
    }
// */

    @Override
    public String toString() {
        String sep = System.getProperty("line.separator");
        StringBuilder str;
        str = new StringBuilder("ID:" + String.format("%3d", id));
//        str = new StringBuilder("ID:" + String.format("%3d", id) + ", " + "x: " + x + ", y: " + y + ", ");
//        str = new StringBuilder("ID:" + String.format("%3d", id) + "  " + messages );
//        str = new StringBuilder("ID: " + String.format("%3d", id) + ", " + String.format("%.3f", e_leader) + ", " + String.format("%.3f", e_member)  );
//        str = new StringBuilder("ID:" + String.format("%3d", id) + ", Resources: " + resSize + ", " + String.format("%3d", didTasksAsMember)  );
/*
        if( this.principle == RECIPROCAL ) {
            str.append(", The most reliable agent: " + relRanking.get(0).id + "← reliability: " + reliabilities[relRanking.get(0).id]);
            str.append(", the distance: " + Manager.distance[this.id][relRanking.get(0).id]);
        }
// */
        /*
        str.append("[");
        for (int i = 0; i < RESOURCE_NUM; i++) str.append(res[i] + ",");
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

//        for( int i = 0; i < AGENT_NUM; i++ ) str.append( i + ": " + String.format("%.3f", reliabilities[i] ) + ",  " );
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
        else if( phase == EXECUTION ) str.append(", My Leader: " + leader.id + ", " + mySubTask + ", resources: " + resource + ", rest:" + executionTime);

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

    public static void resetWorkHistory(List<Agent> agents){
        for(Agent ag: agents){
            for( int i= 0; i < AGENT_NUM; i++ ) {
                ag.workWithAsM[i] = 0;
            }
        }
        _coalition_check_end_time = MAX_TURN_NUM;
    }

    static public int countReciprocalMember(List<Agent> agents){
        int temp = 0;
        for( Agent ag: agents ){
            if( ag.e_member > ag.e_leader && ag.principle == RECIPROCAL ){
                temp++;
            }
        }
        return temp;
    }

    static void makeLonelyAgentList(List<Agent> agents){
        for( Agent ag: agents ){
            if( ag.isLonely == 1 ){
                _lonelyAgents.add(ag.id);
            }
        }
        System.out.println(_lonelyAgents.size());
    }
    static int countLeadersInDepopulatedArea(List<Agent> agents){
        int temp = 0;
        for( int lag: _lonelyAgents ){
            if( agents.get(lag).e_leader > agents.get(lag).e_member ){
                temp++;
            }
        }
        return temp;
    }
}
