/**
 * @author Funato
 * @version 2.0
 */

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.*;

public class Agent implements SetParam , Cloneable{
    static int _id = 0;
    static int _leader_num = 0;
    static int _member_num = 0;
    static int _recipro_num = 0;
    static int _rational_num = AGENT_NUM;
    static long _seed;
    static Random _randSeed;
    static int[] resSizeArray = new int[RESOURCE_TYPES + 1];
    static int _coalition_check_end_time = MAX_TURN_NUM;
    static List<Integer> _lonelyAgents = new ArrayList<>();
    static List<Integer> _accompaniedAgents = new ArrayList<>();
    static double ε = INITIAL_ε;

    // リーダーもメンバも持つパラメータ
    int id;
    int x, y;
    int role = JONE_DOE;
    int phase = SELECT_ROLE;
    Strategy strategy;
    int resSum = 0, resCount = 0;
    int res[] = new int[RESOURCE_TYPES];
    double excellence;
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
    int start = 0;                                          // その時のチーム参加要請を送った時刻
    int isLonely = 0;                                       // 過疎地域のエージェントだったら1
    int isAccompanied = 0;                                  // 過密地域のエージェントだったら1
    int[]   required  = new int[RESOURCE_TYPES];            // そのリソースを要求するサブタスクが割り当てられた回数
    int[][] allocated = new int[AGENT_NUM][RESOURCE_TYPES]; // そのエージェントからそのリソースを要求するサブタスクが割り当てられた回数
    List<Agent> canReach = new ArrayList<>();
    double threshold_for_reciprocity;
    int role_renewal_counter = 0;

    // リーダーエージェントが持つパラメータ
    List<Agent> candidates;         // これからチームへの参加を要請するエージェントのリスト
    int proposalNum = 0;            // 送ったproposalの数を覚えておく
    List<Agent> teamMembers;        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
    Map<Agent, SubTask> preAllocations;       // サブタスクの割り当て候補を< agent, subtask >のHashMapで保持
    List<Message> replies;
    List<Message> results;
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
        threshold_for_reciprocity = (double)resSum/resCount * THRESHOLD_FOR_RECIPROCITY_RATE;
        if (strategy.getClass().getName().startsWith("CNP") || strategy.getClass().getName().startsWith("Rational")) {
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
        threshold_for_reciprocity = (double)resSum/resCount * THRESHOLD_FOR_RECIPROCITY_RATE;
        if (strategy.getClass().getName().startsWith("CNP") || strategy.getClass().getName().startsWith("Rational")) {
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
            while (resSum == 0) {
                for (int i = 0; i < RESOURCE_TYPES; i++) {
                    int rand = _randSeed.nextInt(MAX_AGENT_RESOURCE_SIZE - MIN_AGENT_RESOURCE_SIZE + 1) + MIN_AGENT_RESOURCE_SIZE;
                    res[i]   =  rand;
                    if( rand > 0 ) resCount++;
                    resSum += rand;
                }
            }
            excellence = (double) resSum/resCount;
        }
    }

    void actAsLeader() {
        strategy.actAsLeader(this);
    }
    void actAsMember() {
        strategy.actAsMember(this);
    }

    void selectRole() {
        validatedTicks = Manager.getTicks();
        if (epsilonGreedy()) {
            if (e_leader < e_member) {
                role = LEADER;
                _leader_num++;
                this.phase = PROPOSITION;
                candidates = new ArrayList<>();
                teamMembers = new ArrayList<>();
                preAllocations = new HashMap<>();
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
                    preAllocations = new HashMap<>();
                    replies = new ArrayList<>();
                    results = new ArrayList<>();
                } else {
                    role = MEMBER;
                    _member_num++;
                    this.phase = WAITING;
                }
            }
        // εじゃない時
        } else {
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
                int ran = _randSeed.nextInt(2);
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
            }
        }
    }
    void selectRoleWithoutLearning() {
        int ran = _randSeed.nextInt(5);
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
    void inactivate(double success) {
        if (role == LEADER) {
            e_leader = e_leader * (1.0 - α) + α * success;
            assert e_leader <= 1 && e_leader >= 0 : "Illegal adaption to role";
        } else {
            e_member = e_member * (1.0 - α) + α * success;
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
            preAllocations.clear();
            replies.clear();
            results.clear();
            restSubTask = 0;
            proposalNum = 0;
            replyNum = 0;
        } else {
            if (success == 1) didTasksAsMember++;
            _member_num--;
        }
        role_renewal_counter=0;
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
    }

    /**
     * selectSubtaskメソッド
     * リーダーが自分のやるサブタスクを選択するメソッド
     * まず自分に能力的に可能なサブタスクを探し, その中からランダムで選ぶ
     * なければ何もしない
     */
    void selectSubTask() {
        int calc = 0;
        int temp = Integer.MAX_VALUE;
        int tempIndex = -1;
        int i = 0;

        // 自分に可能なサブタスクを抽出する
        for (SubTask st : ourTask.subTasks) {
            calc = calcExecutionTime(this, st);
            if( calc > 0 && calc < temp ){
                temp = calc ;
                tempIndex = i;
            }
            i++;
        }
        // もし一つもなかったら仕方ないからなしでreturn
        if (tempIndex == -1) {
            restSubTask = ourTask.subTaskNum;
            return;
        }
        // 一個でもあったらどれかを選んで自分のサブタスクとする
        else {
            executionTime = temp;
            mySubTask = ourTask.subTasks.remove(tempIndex);
            ourTask.subTaskNum--;
            restSubTask = ourTask.subTaskNum;
        }
    }
    void sendMessage(Agent from, Agent to, int type, Object o) {
        TransmissionPath.sendMessage(new Message(from, to, type, o));
    }
    void sendNegative(Agent ag, Agent to, int type, SubTask subTask) {
        if (type == PROPOSAL) {
            // 今実行しているサブタスクをくれたリーダーが，実行中にもかかわらずまた要請を出して来たらその旨を伝える
            if( ag.phase == EXECUTION && to.equals(ag.leader) ) {
                sendMessage(ag, to, REPLY, REJECT_FOR_DOING_YOUR_ST);
            }else{
                sendMessage(ag, to, REPLY, REJECT);
            }
        } else if (type == REPLY) {
            sendMessage(ag, to, RESULT, null);
        } else if (type == RESULT) {
//            sendMessage(agent, to, SUBTASK_RESULT, subTask);
        }
    }

    /**
     * calcExecutionTimeメソッド
     * 引数のエージェントが引数のサブタスクを処理できなければ-1を返す．
     * できるのであれば，その処理時間(>0)を返す
     * @param a
     * @param st
     * @return
     */
    public int calcExecutionTime(Agent a, SubTask st) {
        if( a  == null ) System.out.println("Ghost trying to do subtask");
        if( st == null ) System.out.println("Agent trying to do nothing");

        if( a.res[st.resType] == 0 ) return -1;
        return (int) Math.ceil((double) st.reqRes[st.resType] / (double) a.res[st.resType]);
    }

    /**
     * checkMessagesメソッド
     * selfに届いたメッセージcheckListの中から,
     * 期待するタイプで期待するエージェントからの物だけを戻す.
     * それ以外はネガティブな返事をする
     */
    void checkMessages(Agent self) {
            strategy.checkMessages(self);
    }
    /**
     * inTheListメソッド
     * 引数のエージェントが引数のリスト内にあればその索引を, いなければ-1を返す
     */
    protected int inTheList(Object a, List List) {
        for (int i = 0; i < List.size(); i++) {
            if( a.equals(List.get(i)) ) return i;
        }
        return -1;
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
        else if (this.phase == REPORT){
            if( this.executionTime < 0 ){
                if (_coalition_check_end_time - Manager.getTicks() < COALITION_CHECK_SPAN) {
                    if (role == LEADER) {
                        for (Agent ag : teamMembers) workWithAsL[ag.id]++;
                    } else {
                        workWithAsM[leader.id]++;
                    }
                }
                // 自分のサブタスクが終わったら役割適応度を1で更新して非活性状態へ
                inactivate(1);
            }else {
                this.phase = EXECUTION;
            }
        }
        else if (this.phase == RECEPTION) this.phase = EXECUTION;
        this.validatedTicks = Manager.getTicks();
        this.role_renewal_counter=0;
    }
    protected boolean canDo(Agent agent, SubTask st) {
        if (agent.res[st.resType] == st.reqRes[st.resType]) return true;
        else return false;
    }

    int countZeroReliability(List<Agent> agents){
        int countZero = 0;
        for( double rel: this.reliabilities ){
            if( rel == 0 ) countZero++;
        }
        return countZero;
    }

    private static void setSeed(long seed) {
        _seed = seed;
        _randSeed = new Random(_seed);
    }

    static void renewEpsilonLenear(){
        ε -= DIFFERENCE;
        if( ε < FLOOR ) ε = FLOOR;
    }

    static void renewEpsilonExponential(){
        ε =  (ε - FLOOR) * RATE;
        ε += FLOOR;
    }

    static void clearA() {
        _id = 0;
        _leader_num = 0;
        _member_num = 0;
        _rational_num = AGENT_NUM;
        _recipro_num = 0;
        _coalition_check_end_time = SNAPSHOT_TIME;
        _lonelyAgents.clear();
        _accompaniedAgents.clear();
        ε = INITIAL_ε;
        for (int i = 0; i < RESOURCE_TYPES; i++) resSizeArray[i] = 0;
    }


    // 結果集計用のstaticメソッド
    public static void resetWorkHistory(List<Agent> agents){
        for(Agent ag: agents){
            for( int i= 0; i < AGENT_NUM; i++ ) {
                ag.workWithAsM[i] = 0;
            }
        }
        _coalition_check_end_time = MAX_TURN_NUM;
    }
    public static int countReciprocalMember(List<Agent> agents){
        int temp = 0;
        for( Agent ag: agents ){
            if( ag.e_member > ag.e_leader && ag.principle == RECIPROCAL ){
                temp++;
            }
        }
        return temp;
    }
    static void makeLonelyORAccompaniedAgentList(List<Agent> agents){
        for( Agent ag: agents ){
            if( ag.isLonely == 1 ){
                _lonelyAgents.add(ag.id);
            }else if( ag.isAccompanied == 1 ){
                _accompaniedAgents.add(ag.id);
            }
        }
//        System.out.println(_lonelyAgents.size());
//        System.out.println(_accompaniedAgents.size());
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
    static int countLeadersInPopulatedArea(List<Agent> agents){
        int temp = 0;
        for( int lag: _accompaniedAgents ){
            if( agents.get(lag).e_leader > agents.get(lag).e_member ){
                temp++;
            }
        }
        return temp;
    }

    /**
     * agentsの中でspan以上の時間誰からの依頼も受けずチームに参加していないメンバ数を返す．
     * @param agents
     * @param span
     * @return
     */
    public static int countNEETmembers(List<Agent> agents, int span){
        int neetM = 0;
        int now = Manager.getTicks();
        for( Agent ag: agents ){
                if ( now - ag.validatedTicks > span) {
                    neetM++;
                }
        }
        return neetM;
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
    @Override
    public String toString() {
        String sep = System.getProperty("line.separator");
        StringBuilder str = new StringBuilder();
        str = new StringBuilder( String.format("%3d", id));
//        str = new StringBuilder("ID:" + String.format("%3d", id) + ", " + "x: " + x + ", y: " + y + ", ");
//        str = new StringBuilder("ID:" + String.format("%3d", id) + "  " + messages );
//        str = new StringBuilder("ID: " + String.format("%3d", id) + ", " + String.format("%.3f", e_leader) + ", " + String.format("%.3f", e_member)  );
//        str = new StringBuilder("ID:" + String.format("%3d", id) + ", Resources: " + resSize + ", " + String.format("%3d", didTasksAsMember)  );
/*
        if( this.principle == RECIPROCAL ) {
            str.append(", The most reliable agent: " + relRanking.get(0).id + "← reliability: " + reliabilities[relRanking.get(0).id]);
            str.append(", the delay: " + Manager.delays[this.id][relRanking.get(0).id]);
        }
// */
        str.append("[");
        for (int i = 0; i < RESOURCE_TYPES; i++) str.append( String.format("%3d",res[i]) + "," );
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
}
