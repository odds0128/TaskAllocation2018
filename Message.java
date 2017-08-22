/**
 * @author Funato
 * @version 1.1
 */

public class Message {
    private Agent from;
    private SubTask subtask;
    private boolean reply;
    private boolean result;

    Message(Agent from, SubTask subtask) {
        this.from = from;
        this.subtask = subtask;
    }

    Message(Agent from, boolean reply){
        this.from = from;
        this.reply = reply;
    }

    Message(boolean result){
        this.result = result;
    }

    Agent getLeader() {
        return from;
    }

    SubTask getSubTask() {
        return subtask;
    }

    boolean getReply( ) {
        return reply;
    }

    boolean getResult( ){
        return result;
    }
}
