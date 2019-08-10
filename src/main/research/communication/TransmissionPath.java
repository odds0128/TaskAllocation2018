package main.research.communication; /**
 *
 */

import main.research.SetParam;
import main.research.grid.Grid;
import static main.research.SetParam.MessageType.*;
import static main.research.SetParam.ReplyType.*;

import java.util.ArrayList;
import java.util.List;

public class TransmissionPath implements SetParam {
    static double[] meanCT = new double[WRITING_TIMES + 1];

    static int messageNum = 0;
    static int communicationTime = 0;
    static int proposals = 0;
    static int replies = 0;
    static int acceptances = 0;
    static int rejects = 0;
    static int results = 0;
    static int finished = 0;
    static int failed = 0;

    private static TransmissionPath transmissionPath = new TransmissionPath();
    private static List<Message> messageQueue = new ArrayList<>();
    private static List<Integer> delays = new ArrayList<>();

    private TransmissionPath() {
    }

    public static TransmissionPath getInstance() {
        return transmissionPath;
    }

    /**
     * sendMessageメソッド
     * 送りたいメッセージとその遅延時間をパラメータとし, 格納する
     *
     * @param message
     */
    public static void sendMessage(Message message) {
        if( message.getTo() == message.getFrom() ) return;
        messageQueue.add(message);

        int temp = Grid.getDelay( message.getFrom(), message.getTo() );
        delays.add(temp);
        calcCT(temp);
        if (message.getMessageType() == PROPOSAL) proposals++;
        else if (message.getMessageType() == REPLY) {
            replies++;
            if( message.getReply() == ACCEPT ) acceptances++;
            else rejects++;
        }
        else if (message.getMessageType() == RESULT) results++;
//        System.out.println(message);
    }

    /**
     * transmissionメソッド
     * 通信時間を管理し, それが0になったメッセージを宛先のmessageリストに追加する
     */
    public static void transmit() {
        int tempI;
        Message tempM;
        int size = messageQueue.size();
        for (int i = 0; i < size; i++) {
            tempI = delays.remove(0);
            tempI--;
            tempM = messageQueue.remove(0);
            if (tempI == 0) {
                tempM.getTo().messages.add(tempM);
            } else {
                delays.add(tempI);
                messageQueue.add(tempM);
            }
        }
    }

    /**
     * calcCT(communication time) メソッド
     * あるセクションでの平均通信所要時間を計算する.
     * セクション = (総Tick数 ÷ 書き込み回数) とする.
     * 例えば合計10000ticksの施行でexcelへの書き込み回数を1000とするなら, その幅10ticks内の平均を調べる
     * @param ct
     */
    static private void calcCT(int ct){
        communicationTime += ct;
        messageNum++;
    }

    public static int getMessageNum() {
        return messageNum;
    }

    static public double getCT(){
        int ct = communicationTime, mn = messageNum;
        assert (ct > 0 && mn > 0) || (ct == 0 && mn == 0) : "ghost message was sent";
        if( ct == 0 && mn == 0 ) return 4.0;
        double temp = (double)ct/(double) mn;
        communicationTime = 0;
        messageNum = 0;
        return temp;
    }

    /**
     * transmitWithNoDelayメソッド
     * 通信所要時間を考慮しない場合のメッセージ伝送を行う
     */
    static public void transmitWithNoDelay() {
        int size = messageQueue.size();
        Message m;
        for (int i = 0; i < size; i++) {
            delays.remove(0);
            m = messageQueue.remove(0);
            m.getTo().messages.add(m);
        }
        assert delays.size() == 0 && messageQueue.size() == 0 : "transmitAlert";
    }

    public static void clearTP() {
        messageQueue.clear();
        delays.clear();
        messageNum = 0;
        communicationTime = 0;
        proposals = 0;
        replies = 0;
        acceptances = 0;
        rejects = 0;
        results = 0;
        finished = 0;
        failed = 0;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(" Messages: " + messageNum + ", Proposals: " + proposals + ", Replies: " + replies + ", Results: " + results);
        return str.toString();
    }

}
