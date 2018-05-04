import java.io.DataOutput;

/**
 * @author Funato
 * @version 2.0
 */

class Message implements SetParam{

    private Agent from;
    private Agent to;
    private int   messageType;
    // メッセージにサブタスクを載せること自体は悪いことじゃないことに注意. 最終的には渡さないといけない

    // 提案手法チックな手法で使う変数とコンストラクタ．引数4つ
    private SubTask proposedSubtask;
    private int reply;
    private SubTask subtask;  // チーム編成が成功したら, 割り当てるサブタスクが入る. 失敗したらnull
    private int timeSTarrived;

    Message(Agent from, Agent to, int type, Object o) {
        this.from = from;
        this.to   = to;
        this.messageType = type;
        if( type == PROPOSAL ){
            this.proposedSubtask = (SubTask) o;
        }else if( type == REPLY ){
            this.reply       = (int) o;
        }else if( type == RESULT ){
            this.subtask     = (SubTask) o ;
        }else if( type == DONE ){
            this.timeSTarrived = (int) o ;
        }
    }

    // CNPで使う変数とコンストラクタ．引数5つ
    private Task    bidTask;
    private int     bidStIndex;
    private int     estimation;
    private SubTask st;  // チーム編成が成功したら割り当てるサブタスクが入る. 失敗or割り当てなしでnull
    Message(Agent from, Agent to, int type, Object o1, Object o2){
        this.from = from;
        this.to   = to;
        this.messageType = type;
        if( type == PUBLICITY ){
            // 広報時にはタスクを載せる
            bidTask = (Task) o1;
        }else if( type == BIDDINGorNOT ){
            // どのサブタスクを選んだかはインデックスで表す
            // 入札時にはそのタスクのうちどれを何tickくらいでできるかを載せる．非入札時には0を返す
            bidStIndex = (Integer) o1;
            estimation = (Integer) o2;
        }else if( type == BID_RESULT ){
            // 落札結果報告時には落札者にサブタスクを，非落札者にnullをあげる
            st = (SubTask) o1;
        }else if( type == DONE ){
            // 終了時は終了の旨だけでいい
        }
    }

    Agent getFrom() {
        return from;
    }
    Agent getTo()   {
        return to;
    }
    int getMessageType(){
        return messageType;
    }
    SubTask getSubTask() {
        return subtask;
    }
    int getReply( ) {
        return reply;
    }
    int getResType(){ return proposedSubtask.resType; }
    SubTask getProposedSubtask(){ return proposedSubtask; }
    int getTimeSTarrived() { return timeSTarrived; }

    Task getBidTask(){
        return bidTask;
    }

    int getBidStIndex(){
        return bidStIndex;
    }

    int getEstimation(){ return estimation; }

    SubTask getSt(){ return st; }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append(" from: " + from.id );
        str.append(", to: " + to.id);
        str.append(", type: " + messageType );
        if( messageType == PROPOSAL ){
            str.append(", " + proposedSubtask.resType);
        }else if( messageType == REPLY ){
            str.append(", " + reply);
        }else if( messageType == RESULT ){
            str.append(", " + subtask);
        }else if( messageType == DONE ){
            str.append(", " + timeSTarrived);
        }
        return str.toString();
    }
}
