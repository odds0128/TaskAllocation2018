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
    private boolean reply;
    private SubTask subtask;  // チーム編成が成功したら, 割り当てるサブタスクが入る. 失敗したらnull

    Message(Agent from, Agent to, int type, Object o) {
        this.from = from;
        this.to   = to;
        this.messageType = type;
        if( type == PROPOSAL ){
            this.resType = (int) o;
        }
        if( type == REPLY ){
            this.reply       = Boolean.parseBoolean( o.toString() );
        }
        if( type == RESULT ){
            this.subtask     = (SubTask) o ;
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
    boolean getReply( ) {
        return reply;
    }
    int getResType(){ return resType; }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append(" from: " + from.id );
        str.append(", to: " + to.id);
        str.append(", type: " + messageType );
        str.append(", " + reply);
        return str.toString();
    }

}
