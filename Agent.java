/**
 * @author Funato
 * @version 2.0
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Agent implements SetParam {
    static private final double α = LEARNING_RATE;
    static int _id = 0;
    static int _leader_num = 0;
    static int _member_num = 0;
    static long _seed;
    static Random _randSeed;
    // リーダーもメンバも持つパラメータ
    int id;
    int x, y;
    int role = JONE_DOE;
    int phase = SELECT_ROLE;
    Strategy strategy;
    int resource = 0;
    int didTasks = 0;
    int validatedTicks = 0;
    boolean joined = false;
    double e_leader = INITIAL_VALUE_OF_DSL, e_member = INITIAL_VALUE_OF_DSM;
    double rel[] = new double[AGENT_NUM];
    List<Message> messages;
    List<Agent> relAgents = new ArrayList<>();
    // リーダーエージェントが持つパラメータ
    List<Agent> candidates;        // これからチームへの参加を要請するエージェントのリスト
    List<Agent> teamMembers;        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
    List<Tuple> allocations;       // サブタスクの割り当て候補を< agent, subtask >のリストで保持
    List<Message> replies;
    List<Message> results;
    Task ourTask;                  // 持ってきた(割り振られた)タスク
    int restSubTask;               // 残りのサブタスク数
    int index = 0;                 // 一回要請を送ったメンバにもう一度送らないようにindex管理
    // メンバエージェントのみが持つパラメータ
    Agent leader;
    SubTask mySubTask;
    int executionTime;

    // seedが変わった(各タームの最初の)エージェントの生成
    Agent(long seed, int x, int y, Strategy strategy ) {
        setSeed(seed);
        int rand;
        this.id = _id;
        _id++;
        this.x = x;
        this.y = y;
        this.strategy = strategy;
        rand = _randSeed.nextInt(4) + 1;
        resource = rand;
        Arrays.fill(rel, INITIAL_VALUE_OF_DEC);
        selectRole();
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
        rand = _randSeed.nextInt(4) + 1;
        resource = rand;
        Arrays.fill(rel, INITIAL_VALUE_OF_DEC);
        selectRole();
        messages = new ArrayList<>();
    }

    void act() {
        if( phase == SELECT_ROLE ) selectRole();
        else strategy.act(this);
    }

    /**
     * doRoleメソッド
     * eの値によって次の自分の役割を選択する.
     */
    void selectRole() {
        validatedTicks = Manager.getTicks();
        if (e_leader > e_member) {
            role = LEADER;
            _leader_num++;
            this.phase = PROPOSITION;
            candidates   = new ArrayList<>();
            teamMembers  = new ArrayList<>();
            allocations  = new ArrayList<>();
            replies      = new ArrayList<>();
            results      = new ArrayList<>();
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
                candidates   = new ArrayList<>();
                teamMembers  = new ArrayList<>();
                allocations = new ArrayList<>();
                replies      = new ArrayList<>();
                results      = new ArrayList<>();
            } else {
                role = MEMBER;
                _member_num++;
                this.phase = WAITING;
            }
        }
    }

    /**
     * inactiveメソッド
     * チームが解散になったときに待機状態になる.
     */
    void inactivate(int success, int rol) {
        if (rol == LEADER) _leader_num--;
        else _member_num--;
        if (success == 1) didTasks++;
        if (rol == LEADER) {
            ourTask = null;
            candidates.clear();
            teamMembers.clear();
            allocations.clear();
            replies.clear();
            results.clear();
            restSubTask = 0;
        }
        joined = false;
        role = JONE_DOE;
        phase = SELECT_ROLE;
        leader = null;
        mySubTask = null;
        index = 0;
        this.validatedTicks = Manager.getTicks();
        messages.clear();
        strategy.inactivate();
    }

    void sendMessage(Agent from, Agent to, int type, Object o) {
        TransmissionPath.sendMessage(new Message(from, to, type, o));
    }

    void sendNegative(Agent agent, Agent to, int type, SubTask subTask) {
        if (type == PROPOSAL) {
            sendMessage(agent, to, REPLY, REJECT);
        } else if (type == REPLY) {
            sendMessage(agent, to, CHARGE, null);
        } else if (type == CHARGE) {
            sendMessage(agent, to, RESULT, subTask);
        } else if (type == RESULT) {
        }
    }


    // 自分がリーダーの時とか正しく拒否メッセージが送れていない
    /**
     * checkMessagesメソッド
     * selfに届いたメッセージcheckListの中から,
     * 期待するタイプで期待するエージェントからの物だけを戻す.
     * それ以外はネガティブな返事をする
     */
    void checkMessages(Agent self) {
        // リーダーでPROPOSITION or メンバでEXECUTION→ 誰からのメッセージも期待していない
        if( self.phase == PROPOSITION || self.phase == EXECUTION ){
            int size = messages.size();
            Message m;
            for( int i = 0; i < size; i++ ){
                m = messages.remove(0);
                sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
        // メンバでWAITING → PROPOSALを期待している
        }else if( self.phase == WAITING ){
            int size = messages.size();
            Message m;
            for( int i = 0; i < size; i++ ){
                m = messages.remove(0);
                // PROPOSALならmessagesに追加
                if( m.getMessageType() == PROPOSAL ){
                    messages.add(m);
                // 違かったらsendNegative
                }else sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
        // メンバでRECEPTION → リーダーからのCHARGE(サブタスク割り当て)を期待している
        }else if( self.phase == RECEPTION ){
            int size = messages.size();
            Message m;
            for( int i = 0; i < size; i++ ){
                m = messages.remove(0);
                if( m.getMessageType() == CHARGE && m.getFrom()== self.leader ){
                    messages.add(m);
                // 違かったらsendNegative
                }else sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
            }
        // リーダーでREPORT → candidatesからのreply or teamMemberからのresultを期待している
        }else if( self.phase == REPORT ){
            int size = messages.size();
            Message m;
            for( int i = 0; i < size; i++ ){
                m = messages.remove(0);
                Agent from = m.getFrom();
                if( m.getMessageType() == REPLY ){
                    if ( inTheList(from, self.candidates) ) {
                        replies.add(m);
                    }else{
                        sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
                    }
                }else if(  m.getMessageType() == RESULT){
                    if ( inTheList(from, self.teamMembers) ) {
                        results.add(m);
                    }else{
                        sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
                    }
                }else{
                    sendNegative(self, m.getFrom(), m.getMessageType(), m.getSubTask());
                }
            }
        }
    }

    /**
     * inTheListメソッド
     * 引数のエージェントが引数のリスト内にあればtrueを, いなければfalseを返す
     */
    boolean inTheList(Agent a ,List<Agent> agents){
        for( int i = 0; i < agents.size(); i++ ){
            if( a == agents.get(i) ) return true;
        }
        return false;
    }

    /**
     * checkSentメソッド
     * 引数のエージェントにすでにサブタスクを割り当てていたか調べる
     * すでに割り当てていた場合は重複を避けるためにfalseを返す
     * まだ割りあてていなければtrueを返す
     * @param candidate
     * @return
     */
    boolean checkDup(Agent candidate, List<Agent> list){
        for( Agent agent : list ){
            if( candidate == agent ) return false;
        }
        return true;
    }

    /**
     * getAllocationメソッド
     * 引数のエージェントに割り当てるサブタスクを返す
     * 割り当て情報は保持
     */
    SubTask getAllocation(Agent agent) {
        int size = allocations.size();
        for (int i = 0; i < size; i++) {
            if (agent == allocations.get(i).getCandidate()) {
                return allocations.get(i).getSubtask();
            }
        }
        return null;
    }

    /**
     * removeAllocationメソッド
     * 引数のエージェントへの割り当て情報を消す
     */
    void removeAllocation(Agent agent) {
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
    void removeTeamMember(Agent agent) {
        int size = teamMembers.size();
        for (int i = 0; i < size; i++) {
            if (agent == teamMembers.get(i) ) {
                teamMembers.remove(i);
                return;
            }
        }
    }

    /**
     * nextPhaseメソッド
     * phaseの変更をする
     * 同時にvalidTimeを更新する
     */
    void nextPhase() {
        if( this.phase == PROPOSITION ) this.phase = REPORT;
        else if( this.phase == WAITING     ) this.phase = RECEPTION;
        else if( this.phase == RECEPTION   ) this.phase = EXECUTION;
        this.validatedTicks = Manager.getTicks();
    }

    static void setSeed(long seed) {
        _seed = seed;
        _randSeed = new Random(_seed);
    }

    static void clearA() {
        _id = 0;
        _leader_num = 0;
        _member_num = 0;
    }

    @Override
    public String toString() {
        String sep = System.getProperty("line.separator");
//        StringBuilder str = new StringBuilder("ID:" + String.format("%3d", id) + ", " + "x: " + x + ", y: " + y + ", ");
        StringBuilder str = new StringBuilder("ID:" + String.format("%3d", id) + "  " + messages );
//        StringBuilder str = new StringBuilder("ID: " + String.format("%3d", id) + ", " + String.format("%.3f", e_leader) + ", " + String.format("%.3f", e_member) + String.format("%5d", didTasks) );

//        str.append("e_leader: " + e_leader + ", e_member: " + e_member);
        if( role == MEMBER ) str.append(", member: ");
        else if( role == LEADER ) str.append(", leader: " );
        else if( role == JONE_DOE ) str.append(", free: ");

        if( phase == REPORT ){
            str.append(" allocations: " + allocations  );
//            str.append(" teamMembers: " + teamMembers );
        }
        else if( phase == RECEPTION ) str.append(", My Leader: " + leader.id);
        else if( phase == EXECUTION ) str.append(", My Leader: " + leader.id + ", " + mySubTask + ", resources: " + resource + ", rest:" + executionTime);

//        if (role == LEADER) str.append(", I'm a leader. ");
//        else str.append("I'm a member. ");

        int temp;
/*        str.append(",  Reliable Agents:");
        for (int i = 0; i < MAX_REL_AGENTS; i++) {
            temp = relAgents.get(i).id;
            str.append(String.format("%3d", temp));
        }
*/
        /*
        if( role == MEMBER ) {
            str.append(",  Reliable Agents:");
            for (int i = 0; i < MAX_REL_AGENTS; i++) {
                temp = relAgents.get(i);
                str.append(String.format("%3d", temp));
            }
        }
        */
        return str.toString();
    }
}
