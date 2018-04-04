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
    private int resType;
    private int reply;
    private SubTask subtask;  // チーム編成が成功したら, 割り当てるサブタスクが入る. 失敗したらnull
    private int timeSTarrived;

    Message(Agent from, Agent to, int type, Object o) {
        this.from = from;
        this.to   = to;
        this.messageType = type;
        if( type == PROPOSAL ){
            this.resType = (int) o;
        }else if( type == REPLY ){
            this.reply       = (int) o;
        }else if( type == RESULT ){
            this.subtask     = (SubTask) o ;
        }else if( type == DONE ){
            this.timeSTarrived = (int) o ;
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
    int getResType(){ return resType; }
    int getTimeSTarrived() { return timeSTarrived; }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append(" from: " + from.id );
        str.append(", to: " + to.id);
        str.append(", type: " + messageType );
        if( messageType == PROPOSAL ){
            str.append(", " + resType);
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
