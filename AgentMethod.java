/**
 * @author Funato
 * @version 2.0
 */

import java.util.*;

public abstract class AgentMethod implements SetParam, Leader, Member{
    public static int _id = 0;
    public static int _leader_num = 0;
    public static int _member_num = 0;

    public int id;
    public int role   = WAITING;
    protected int phase = SELECT_ROLE;
    protected double e_leader = INITIAL_VALUE_OF_DSL, e_member = INITIAL_VALUE_OF_DSM;
    protected double α  = LEARNING_RATE;
    protected int res[] = new int[RESOURCE_NUM];
    protected double rel[] = new double[AGENT_NUM ];
    protected ArrayList<Integer> relAgents = new ArrayList<Integer>();
    protected List<Agent>  toSendAgents ;
    protected List<Message> messages;
    protected Task    ourTask;
    protected SubTask mySubTask;
    protected Random random = new Random();

    // 初期化に必要なものたち
    /**
     * setResourceメソッド
     * 生成エージェントについてパラメータで指定されたタイプのリソースを割り当てる
     * @param agentType
     */
    /**
     * setRelメソッド
     * 信頼度と信頼するエージェントの初期化.
     * 信頼度は全員初期値0.5とし, 信頼するエージェントはランダムに選ぶ(近くのエージェントから?).
     */
    /**
     * setRelAgメソッド
     * パラメータの数だけ信頼するエージェントを定める.
     */
    /**
     * checkDupメソッド
     * 信頼するエージェントの候補が重複していないか調べる
     */
    void setResource(int agentType){
        if( agentType ==  BIAS){
        }else{
            for (int i = 0; i < RESOURCE_NUM; i++) {
                int rand = random.nextInt(2);
                res[i] = rand;
            }
        }
    }
    void setRel(){
        Arrays.fill( rel, INITIAL_VALUE_OF_DEC );
    }
    void setRelAg(){
        int temp;
        for( int i = 0; i < MAX_REL_AGENTS; i++ ) {
            temp = random.nextInt(AGENT_NUM);
            if( checkDup(temp, i) ) relAgents.add(temp);
            else i--;
        }
        Collections.sort( relAgents );
    }
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
    void selectRole(){
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
    public SubTask selectSubTask(Task ourTask){
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
    void inactivate(){
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
    void addMessage(Message message){
        messages.add(message);
    }

    /**
     * renewRelメソッド
     * 信頼度の更新
     */
    void renewRel( int reliability, Agent agent ){
        rel[agent.id] = ( 1 - α ) * rel[agent.id] + α * reliability;
    }

    /**
     * canDoメソッド
     * パラメータのサブタスクが己のリソースで実行可能かどうかを確認する
     * 可能ならばtrue, 不可能ならfalseを返す
     * @param subTask
     * @return
     */
    public boolean canDo(SubTask subTask){
        for( int i = 0; i < RESOURCE_NUM; i++ ){
            if( this.res[i] < subTask.reqRes[i] ) return false;
        }
        return true;
    }

}
