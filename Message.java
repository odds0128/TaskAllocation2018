/**
 * @author Funato
 * @version 2.0
 */

class Message implements SetParam{
    private Agent from;
    private Agent to;
    private int   messageType;
    // メッセージにサブタスクを載せること自体は悪いことじゃないことに注意. 最終的には渡さないといけない
    private int subTaskSize;
    private SubTask subtask;
    private boolean reply;
    private boolean result;

    Message(Agent from, Agent to, int type, Object o) {
        this.from = from;
        this.to   = to;
        this.messageType = type;
        if( type == PROPOSAL ){
            this.subTaskSize = Integer.parseInt( o.toString() );
        }
        if( type == REPLY ){
            this.reply       = Boolean.parseBoolean( o.toString() );
        }
        if( type == CHARGE ){
            this.subtask     = (SubTask) o ;
        }
        if( type == RESULT ){
            this.subtask     = (SubTask) o ;
            if( subtask == null ) this.result = SUCCESS;
            else this.result = FAILURE;
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
    boolean getResult() {
        return result;
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append(" from: " + from.id );
        str.append( ", type: " + messageType );
        str.append(", " + reply);
        return str.toString();
    }

}
