/**
 * @author Funato
 * @version 1.1
 */

import java.util.*;

public class Agent implements SetParam{
    public static int _id = 0;
    public static int _leader_num = 0;
    public static int _member_num = 0;
    public int id;
    int    role   = WAITING;
    private int phase = SELECT_ROLE;
    private double e_leader = 0.5, e_member = 0.5;
    private int res[] = new int[RESOURCE_NUM];
    private double rel[] = new double[AGENT_NUM ];
    private ArrayList<Integer> relAgents = new ArrayList<Integer>();
    private List<Agent>  toSendAgents ;
    private List<Message> messages;
    private Task    ourTask;
    private SubTask mySubTask;
    private Random random = new Random();

    Agent( int agentType ){
        this.id = _id;
        _id++;
        setResource(agentType);
        setRel();
        setRelAg();
        selectRole();
        messages = new ArrayList<Message>();
    }

    void act1(){
        if( phase == SELECT_ROLE ) selectRole();
        else if( role == LEADER && phase == NEGOTIATION ) negotiateAsLeader();
        else if( role == MEMBER && phase == NEGOTIATION ) negotiateAsMember();
        else if( role == LEADER && phase == EXECUTION )   executeAsLeader();
        else if( role == MEMBER && phase == EXECUTION )   executeAsMember();
     }

    void act2(){
         if( role == LEADER && phase == NEGOTIATION )      negotiateAsLeader2();
         else if( role == MEMBER && phase == NEGOTIATION ) negotiateAsMember2();
    }

    /**
     * actAsLeader1メソッド
     * リーダーとしての行動を定義する.
     * まずタスクキューを見て空だったら何もしない(待機状態にするか追加されるまで待つかを実装する必要)
     * タスクを持ってこれたら自分が担当するサブタスクを選ぶ(最初に見つけたやつをやる事にする.　→ リソースの無駄のないやつを取るべきか)
     * 次にメンバエージェントのリストを見てサブタスクを割り当てる
     * 参加要請を送るまででワンセット
     */
    private void negotiateAsLeader(){
        List<Agent> candidates ;
        Message proposal;

        ourTask = Manager.getTask();
        if( ourTask == null ) return;
        mySubTask = selectSubTask(ourTask);

        candidates = Manager.getMembers();
        toSendAgents = selectMembers(ourTask, candidates);

        if( toSendAgents.size() < ourTask.subTaskNum ) {
//            System.out.println( " Break up. " );
            inactivate();
            Manager.disposeTask();
            return;
        }
//        System.out.println();

        for( Agent  toSendAgent: toSendAgents ) {
            proposal = new Message( this, toSendAgent.mySubTask );
            toSendAgent.addMessage( proposal );
        }
    }
    private void negotiateAsLeader2(){
        if( ourTask == null ) return;
        int countJoined = 0;
        for( Message message : this.messages ){
            if( message.getReply() ) countJoined++;
        }
        if( countJoined < ourTask.subTaskNum ){
            Message breakUp = new Message( BREAK_UP );
            for( Agent teamMember : toSendAgents ) {
                teamMember.addMessage( breakUp );
            }
            Manager.disposeTask();
            inactivate();
        }else {
            Message execution = new Message(EXECUTE);
            for (Agent teamMember : toSendAgents) {
                teamMember.addMessage(execution);
            }
        }
        phase = EXECUTION;
    }

    /**
     * actAsMember1メソッド
     * メンバとしての行動を定義する
     */
    private void negotiateAsMember(){
        if( messages.isEmpty() ) return;
        boolean joined = false;     // 参加するチームを決めたらtrueに
//        System.out.println( "I'm: " + this.id + ", Offers: " + messages.size() );

        for (Message message : this.messages) {
            Agent from = message.getLeader();
//            System.out.print("From: " + from.id);
            int i = 0;
            if( ! joined ) {
                for (int relLeader : relAgents) {
                    // if( まだ参加するチームを決めてなくてかつ信頼するエージェントから要請が来たら)
                    if ( from.id == relLeader ) {
                        // リーダーに受理を伝えてフラグを更新
                        Message reply = new Message(this, ACCEPT);
                        from.addMessage(reply);
                        joined = true;
//                        System.out.println(" join to " + from.id);
                        break;
                    }
                    i++;
                    if( i == relAgents.size() ){
                        Message reply = new Message(this, REJECT);
 //                       System.out.println(", Sorry:" + from.id);
                        from.addMessage(reply);
                    }
                }
            }else{
                Message reply = new Message(this, REJECT);
//                System.out.println(", Sorry:" + from.id);
                from.addMessage(reply);
            }
        }
        this.messages.clear();
    }
    private void negotiateAsMember2(){
        if( this.messages.isEmpty() ) return;
        if( this.messages.get(0).getResult() ) phase = EXECUTION;
        else inactivate();
        this.messages.clear();
    }

    private void executeAsLeader(){
        Manager.finishTask();
        if( mySubTask != null ) ourTask.subTaskNum++;
        System.out.println("We did." + id + ", Subtasks: " + ourTask.subTaskNum);
        System.out.print("ID: " + id + ", res: " );
        for( int i = 0; i < RESOURCE_NUM; i++ ) System.out.print( res[i] + " " );
        System.out.print( ", required: " );
        if( mySubTask != null ) {
            for (int i = 0; i < RESOURCE_NUM; i++) System.out.print(mySubTask.reqRes[i] + " ");
        }
        System.out.println();
        inactivate();
    }
    private void executeAsMember(){
        System.out.print("ID: " + id + ", res: " );
        for( int i = 0; i < RESOURCE_NUM; i++ ) System.out.print( res[i] + " " );
        System.out.print( ", required: " );
        for( int i = 0; i < RESOURCE_NUM; i++ ) System.out.print( mySubTask.reqRes[i] + " " );
        System.out.println();
        inactivate();
    }

    /**
     * setResourceメソッド
     * 生成エージェントについてパラメータで指定されたタイプのリソースを割り当てる
     * @param agentType
     */
    private void setResource(int agentType){
        if( agentType ==  BIAS){
        }else{
            for (int i = 0; i < RESOURCE_NUM; i++) {
                int rand = random.nextInt(2);
                res[i] = rand;
            }
        }
    }

    /**
     * setRelメソッド
     * 信頼度と信頼するエージェントの初期化.
     * 信頼度は全員初期値0.5とし, 信頼するエージェントはランダムに選ぶ(近くのエージェントから?).
    */
    private void setRel(){
        Arrays.fill( rel, 0.5 );
    }

    /**
     * setRelAgメソッド
     * パラメータの数だけ信頼するエージェントを定める.
     */
    private void setRelAg(){
        int temp;
        for( int i = 0; i < MAX_REL_AGENTS; i++ ) {
            temp = random.nextInt(AGENT_NUM);
            if( checkDup(temp, i) ) relAgents.add(temp);
            else i--;
        }
        Collections.sort( relAgents );
    }

    /**
     * checkDupメソッド
     * 信頼するエージェントの候補が重複していないか調べる
     */
    private boolean checkDup(int temp, int current ){
        if( temp == id ) return false;
        for( int i = 0; i < current ; i++ ){
            if( relAgents.get(i) == temp ) return false;
        }
        return true;
    }

    /**
     * doRoleメソッド
     * eの値によって次の自分の役割を選択する.
     */
    private void selectRole(){
        if( e_leader > e_member ) {
            role = LEADER;
            _leader_num++;
        }else if( e_member > e_leader ){
            role = MEMBER;
            _member_num++;
        }else{
            Random random = new Random();
            boolean ran = random.nextBoolean();
            if( ran ){
                role = LEADER;
                _leader_num++;
            }else{
                role = MEMBER;
                _member_num++;
            }
        }
        this.phase = NEGOTIATION;
    }

    /**
     * selectSubTaskメソッド
     * 自分のリソースとタスクが要求するリソースを比較して自分が担当するサブタスクを選択する
     * できるものがあれば最初に見つけたものを返し, なければnullでいいのかなぁ...
     */
    private SubTask selectSubTask(Task ourTask){
        for( SubTask temp : ourTask.subTasks ) {
            if( canDo(temp) ) {
                mySubTask = temp;
                ourTask.subTasks.remove(temp.subtask_id);
                ourTask.subTaskNum--;
                return temp;
            }
        }
        return null;
    }

    /**
     * inactiveメソッド
     * チームが解散になったときに待機状態になる.
     */
    private void inactivate(){
        if( role == LEADER ) _leader_num-- ;
        else _member_num-- ;
        role = WAITING;
        phase = SELECT_ROLE;
    }

    /**
     * addMessageメソッド
     * メッセージキューにメッセージを追加, すなわちメンバに参加要請, 実行指示, 解散指示などを送る
     * @param message
     */
    private void addMessage(Message message){
        messages.add(message);
    }

    /**
     * selectMembersメソッド
     * 候補者(能力をリーダーが知っているメンバエージェント)の中から残ったサブタスクを
     * 実際に依頼するエージェントを選定する
     * 返り値は実際に依頼するエージェントのリスト
     * @param ourTask
     * @param candidates
     * @return
     */
    private List<Agent> selectMembers(Task ourTask, List<Agent> candidates){
        List<Agent> temp = new LinkedList<>();
        boolean[] offered = new boolean[candidates.size()];
        int i ;
        for( SubTask subtask : ourTask.subTasks){
            i = 0;
            for( Agent candidate : candidates ){
                if( candidate.canDo(subtask) && offered[i] == false ){
                    candidate.mySubTask = subtask;
                    temp.add(candidate);
                    offered[i] = true;
                    break;
                }
                i++;
            }
        }
/*        System.out.print("ID: " + id + " Selected: ");
        for( Agent selected : temp ){
            System.out.print( String.format("%3d", selected.id ) + " " );
        }
*/
        return temp;
    }

    /**
     * canDoメソッド
     * パラメータのサブタスクが己のリソースで実行可能かどうかを確認する
     * 可能ならばtrue, 不可能ならfalseを返す
     * @param subTask
     * @return
     */
    private boolean canDo(SubTask subTask){
        for( int i = 0; i < RESOURCE_NUM; i++ ){
            if( this.res[i] < subTask.reqRes[i] ) return false;
        }
        return true;
    }

    @Override
    public String toString(){
        int temp;
        String str = "ID:" + String.format("%3d", id) + ", ";

        if( role == LEADER ) str += "I'm a leader. " ;
        else str += "I'm a member. ";

        str += "Resources:";
        for( int i = 0; i < RESOURCE_NUM; i++ ){
            str +=  String.format("%2d", res[i]);
        }
        if( role == MEMBER ) {
            str += ",  Relaiable Agents:";
            for (int i = 0; i < MAX_REL_AGENTS; i++) {
                temp = relAgents.get(i).intValue();
                str += String.format("%3d", temp) ;
            }
        }
        return str;
    }
}
