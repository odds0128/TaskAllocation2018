/**
 * @author Funato
 * @version 2.0
 */

package main.research.agent;


import main.research.*;
import main.research.communication.Message;
import main.research.communication.TransmissionPath;
import main.research.strategy.Strategy;
import main.research.task.SubTask;
import main.research.task.Task;
import java.util.*;

public class Agent implements SetParam , Cloneable{
    public static int _id = 0;
    public static int _leader_num = 0;
    public static int _member_num = 0;
    public static int _recipro_num = 0;
    public static int _rational_num = AGENT_NUM;
    static long _seed;
    public static Random _randSeed;
    static int[] resSizeArray = new int[RESOURCE_TYPES + 1];
    public static int _coalition_check_end_time ;
    static List<Integer> _lonelyAgents = new ArrayList<>();
    static List<Integer> _accompaniedAgents = new ArrayList<>();
    static double ε = INITIAL_ε;

    // リーダーもメンバも持つパラメータ
    public int id;
    public int x;
    public int y;
    public int role = JONE_DOE;
    public int phase = SELECT_ROLE;
    Strategy strategy;
    int resSum = 0, resCount = 0;
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
    public List<Message> messages;
    public int principle = RATIONAL;
    public int executionTime = 0;
    public int start = 0;                                          // その時のチーム参加要請を送った時刻
    int isLonely = 0;                                       // 過疎地域のエージェントだったら1
    int isAccompanied = 0;                                  // 過密地域のエージェントだったら1
    public int[]   required  = new int[RESOURCE_TYPES];            // そのリソースを要求するサブタスクが割り当てられた回数
    public int[][] allocated = new int[AGENT_NUM][RESOURCE_TYPES]; // そのエージェントからそのリソースを要求するサブタスクが割り当てられた回数
    public List<Agent> canReach = new ArrayList<>();
    int role_renewal_counter = 0;
    public List<Agent> agentsCommunicatingWith = new ArrayList<>(); // 今通信をしていて，返信を期待しているエージェントのリスト．返信が返ってきたらリストから消す

    // リーダーエージェントが持つパラメータ
    public List<Agent> candidates;         // これからチームへの参加を要請するエージェントのリスト
    public int proposalNum = 0;            // 送ったproposalの数を覚えておく
    public List<Agent> teamMembers;        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
    public Map<Agent, SubTask> preAllocations;       // サブタスクの割り当て候補を< agent, subtask >のHashMapで保持
    public List<Message> replies;
    public List<Message> results;
    public Task ourTask;                  // 持ってきた(割り振られた)タスク
    public int restSubTask;               // 残りのサブタスク数
    int index = 0;                 // 一回要請を送ったメンバにもう一度送らないようにindex管理
    public int replyNum = 0;
    int prevIndex = 0;
    int acceptances = 0;           // 今まで自分の元に帰って来た受理応答
    int untilAcceptances = 0;      // 今まで自分の元に返って来た受理応答の合計応答時間(= 往復の通信時間 + メンバの処理時間)
    double meanUA = 0;             // 今まで自分の元に返って来た受理応答の平均応答時間(=  untilAcceptances/acceptances)
    public double threshold_for_reciprocity_as_leader;
    public List<Task> pastTasks = new ArrayList<>();
    public double[] reliabilities_l = new double[AGENT_NUM];
    public List<Agent> relAgents_l = new ArrayList<>();
    public List<Agent> relRanking_l = new ArrayList<>();

    // メンバエージェントのみが持つパラメータ
    public Agent leader;
    int totalOffers = 0;            // 今まで自分が受理して来たオファー数
    int totalResponseTicks = 0;     // 受理応答からの待ち時間の合計
    double meanRT = 0;     // 受理応答からの待ち時間の平均
    public double threshold_for_reciprocity_as_member;
    public SubTask mySubTask;
    public List<SubTask> mySubTaskQueue = new ArrayList<>();       // メンバはサブタスクを溜め込むことができる(実質的に，同時に複数のチームに参加することができるようになる)
    public int tbd = 0;                                            // 返事待ちの数
    public double[] reliabilities_m = new double[AGENT_NUM];
    public List<Agent> relAgents_m = new ArrayList<>();
    public List<Agent> relRanking_m = new ArrayList<>();
    public List<Agent> myLeaders = new ArrayList<>();

    // seedが変わった(各タームの最初の)エージェントの生成
    public Agent(long seed, int x, int y, Strategy strategy) {
        setSeed(seed);
        int rand;
        this.id = _id;
        _id++;
        if (CHECK_INTERIM_RELATIONSHIPS){
            _coalition_check_end_time = SNAPSHOT_TIME;
        }else{
            _coalition_check_end_time = MAX_TURN_NUM;
        }
        this.x = x;
        this.y = y;
        this.strategy = strategy;
        setResource(UNIFORM);
        Arrays.fill(reliabilities_l, INITIAL_VALUE_OF_DEC);
        Arrays.fill(reliabilities_m, INITIAL_VALUE_OF_DEC);
        threshold_for_reciprocity_as_leader = THRESHOLD_FOR_RECIPROCITY_FROM_LEADER;
        threshold_for_reciprocity_as_member = (double)resSum/resCount * THRESHOLD_FOR_RECIPROCITY_RATE;
        if (strategy.getClass().getName().startsWith("main.research.strategy.CNP")
                || strategy.getClass().getName().startsWith("Rational")
                || strategy.getClass().getName().endsWith("RoleFixed")) {
            selectRoleWithoutLearning();
        } else {
            selectRole();
        }
        messages = new ArrayList<>();
    }

    // 残りのエージェントの生成
    public Agent(int x, int y, Strategy strategy) {
        int rand;
        this.id = _id;
        _id++;
        this.x = x;
        this.y = y;
        this.strategy = strategy;
        setResource(UNIFORM);
        Arrays.fill(reliabilities_l, INITIAL_VALUE_OF_DEC);
        Arrays.fill(reliabilities_m, INITIAL_VALUE_OF_DEC);
        threshold_for_reciprocity_as_leader = THRESHOLD_FOR_RECIPROCITY_FROM_LEADER;
        threshold_for_reciprocity_as_member = (double)resSum/resCount * THRESHOLD_FOR_RECIPROCITY_RATE;
        if (strategy.getClass().getName().startsWith("main.research.strategy.CNP")
                || strategy.getClass().getName().startsWith("Rational")
                || strategy.getClass().getName().endsWith("RoleFixed")
                || strategy.getClass().getName().endsWith("withoutReciprocity")
                ) {
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
            } */
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

    public void actAsLeader() {
        strategy.actAsLeader(this);
    }
    public void actAsMember() {
        strategy.actAsMember(this);
    }

    public void selectRole() {
        validatedTicks = Manager.getTicks();
        if( mySubTaskQueue.size() > 0 ){
            mySubTask = mySubTaskQueue.remove(0);
            leader = mySubTask.from;
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
//                int ran = _randSeed.nextInt(2);
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
//            }
        }
    }
    void selectRoleWithoutLearning() {
        int ran = _randSeed.nextInt(7);
        if( mySubTaskQueue.size() > 0 ){
            mySubTask = mySubTaskQueue.remove(0);
            leader = mySubTask.from;
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

            if( e_leader < 0 ) e_leader = 0.001;

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
            restSubTask = 0;
            proposalNum = 0;
            replyNum = 0;
        } else {
            _member_num--;
        }
        mySubTask = null;
        role_renewal_counter=0;
        joined = false;
        role = JONE_DOE;
        phase = SELECT_ROLE;
        leader = null;
        executionTime = 0;
        if (strategy.getClass().getName() != "RoundRobin") {
            index = 0;
        } else {
            prevIndex = index % relAgents_l.size();
            index = prevIndex;
        }
        this.validatedTicks = Manager.getTicks();
    }

    public void sendMessage(Agent from, Agent to, int type, Object o) {
        TransmissionPath.sendMessage(new Message(from, to, type, o));
    }
    public void sendNegative(Agent ag, Agent to, int type, SubTask subTask) {
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
    public void checkMessages(Agent self) {
            strategy.checkMessages(self);
    }
    /**
     * inTheListメソッド
     * 引数のエージェントが引数のリスト内にあればその索引を, いなければ-1を返す
     */
    public int inTheList(Object a, List List) {
        for (int i = 0; i < List.size(); i++) {
            if( a.equals(List.get(i)) ) return i;
        }
        return -1;
    }

    public boolean haveAlreadyJoined(Agent member, Agent target){
        if( member.leader == target ){
            return true;
        }
        return inTheList(target, myLeaders) >= 0 ? true : false;
    }

    /**
     * taskIDを元にpastTaskからTaskを同定しそれを返す
     * @param taskID ... taskのID
     */
    public Task identifyTask(int taskID){
        Task temp = null;
        for( Task t: pastTasks ){
            if( t.task_id == taskID ){
                temp = t;
                break;
            }
        }
        assert temp != null : "Did phantom task!";
        return temp;
    }

    public boolean epsilonGreedy() {
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
    public void nextPhase() {
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

    boolean isOccupied(){
        assert this.mySubTaskQueue.size() > SUBTASK_QUEUE_SIZE : "YesMan";
        if( this.mySubTaskQueue.size() == SUBTASK_QUEUE_SIZE ){
            return true;
        }
        return false;
    }

    private static void setSeed(long seed) {
        _seed = seed;
        _randSeed = new Random(_seed);
    }

    public static void renewEpsilonLenear(){
        ε -= DIFFERENCE;
        if( ε < FLOOR ) ε = FLOOR;
    }

    public static void renewEpsilonExponential(){
        ε =  (ε - FLOOR) * RATE;
        ε += FLOOR;
    }

    public static void clearA() {
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
                ag.workWithAsL[i] = 0;
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

    public static void makeLonelyORAccompaniedAgentList(List<Agent> agents){
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
            str.append(", the delay: " + main.research.Manager.delays[this.id][relRanking.get(0).id]);
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