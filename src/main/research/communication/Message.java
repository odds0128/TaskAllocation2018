package main.research.communication;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.SetParam.MessageType.*;
import static main.research.SetParam.MessageTypeForCNP.*;

/**
 * @author Funato
 * @version 2.0
 */

public class Message implements SetParam {

    private Agent from;
    private Agent to;
    private MessageTypeInterface messageType;
    // メッセージにサブタスクを載せること自体は悪いことじゃないことに注意. 最終的には渡さないといけない

    // 提案手法チックな手法で使う変数とコンストラクタ．引数4つ
    private Subtask proposedSubtask;
    private ReplyType reply;
    private Subtask subtask;  // チーム編成が成功したら, 割り当てるサブタスクが入る. 失敗したらnull
    private int timeSTarrived;

    public Message(Agent from, Agent to, MessageType type, Object o) {
        if( to == null ) {
            System.out.println("to is null");
        }
        this.from = from;
        this.to   = to;
        this.messageType = type;
        if( type == PROPOSAL ){
            this.proposedSubtask = (Subtask) o;
        }else if( type == REPLY ){
            this.reply       = (ReplyType) o;
        }else if( type == RESULT ){
            this.subtask     = (Subtask) o ;
        }else if( type == DONE ){
            this.timeSTarrived = (int) o ;
        }
    }

    // CNPで使う変数とコンストラクタ．引数5つ
    private Task    bidTask;
    private int     bidStIndex;
    private int     estimation;
    private Subtask st;  // チーム編成が成功したら割り当てるサブタスクが入る. 失敗or割り当てなしでnull
    Message(Agent from, Agent to, MessageTypeInterface type, Object o1, Object o2){
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
            st = (Subtask) o1;
        }else if( type == DONE ){
            // 終了時は終了の旨だけでいい
        }
    }

    public Agent getFrom() {
        return from;
    }
    Agent getTo()   {
        return to;
    }
    public MessageTypeInterface getMessageType(){
        return messageType;
    }
    public Subtask getSubtask() {
        return subtask;
    }
    public ReplyType getReply() {
        return reply;
    }
    int getResType(){ return proposedSubtask.resType; }
    int getResSize(){ return proposedSubtask.reqRes[proposedSubtask.resType]; }
    Subtask getProposedSubtask(){ return proposedSubtask; }
    int getTimeSTarrived() { return timeSTarrived; }

    Task getBidTask(){
        return bidTask;
    }

    int getBidStIndex(){
        return bidStIndex;
    }

    int getEstimation(){ return estimation; }

    Subtask getSt(){ return st; }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append(" from: " + from.id );
        str.append(", to: " + to.id);
        str.append(", type: ");
        if( messageType == PROPOSAL ){
            str.append("proposal__ " + proposedSubtask.resType);
        }else if( messageType == REPLY ){
            str.append("reply__ " + reply);
        }else if( messageType == RESULT ){
            str.append("result__ " + subtask);
        }else if( messageType == DONE ){
            str.append("done__ " + timeSTarrived);
        }
        return str.toString();
    }
}
