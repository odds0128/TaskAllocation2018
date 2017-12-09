/**
 *
 */

import java.util.ArrayList;
import java.util.List;

class TransmissionPath implements SetParam {
    static int messageNum = 0;
    static int transmitTime = 0;
    static int proposals = 0;
    static int replies = 0;
    static int acceptances = 0;
    static int rejects = 0;
    static int results = 0;
    static int finished = 0;
    static int failed = 0;

    private static TransmissionPath transmissionPath = new TransmissionPath();
    private static List<Message> messageQueue = new ArrayList<>();
    private static List<Integer> delay = new ArrayList<>();

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
    static void sendMessage(Message message) {
        messageQueue.add(message);
        int temp = Manager.distance[message.getFrom().id][message.getTo().id];
        delay.add(temp);
        transmitTime += temp;
        messageNum++;
        if (message.getMessageType() == PROPOSAL) proposals++;
        if (message.getMessageType() == REPLY) {
            replies++;
            if( message.getReply() == ACCEPT ) acceptances++;
            else rejects++;
        }
        if (message.getMessageType() == RESULT) results++;
//        System.out.println(message);
//         System.out.println(" from: " + message.getFrom().id + ", to: " + message.getTo().id + ", distance: " + calcTicks(message));
    }

    /**
     * transmissionメソッド
     * 通信時間を管理し, それが0になったメッセージを宛先のmessageリストに追加する
     */
    static void transmit() {
        int tempI;
        Message tempM;
        int size = messageQueue.size();
        for (int i = 0; i < size; i++) {
            tempI = delay.remove(0);
            tempI--;
            tempM = messageQueue.remove(0);
            if (tempI == 0) {
                tempM.getTo().messages.add(tempM);
            } else {
                delay.add(tempI);
                messageQueue.add(tempM);
            }
        }
    }

    static int calcTicks(Message message) {
        return Math.abs(message.getFrom().x - message.getTo().x) + Math.abs(message.getFrom().y - message.getTo().y);
    }

    static void clearTP() {
        messageQueue.clear();
        delay.clear();
        messageNum = 0;
        transmitTime = 0;
        proposals = 0;
        replies = 0;
        acceptances = 0;
        rejects = 0;
        results = 0;
        finished = 0;
        failed = 0;
    }

    /**
     * transmitWithNoDelayメソッド
     * 通信所要時間を考慮しない場合のメッセージ伝送を行う
     */
    static public void transmitWithNoDelay() {
        int size = messageQueue.size();
        Message m;
        for (int i = 0; i < size; i++) {
            delay.remove(0);
            m = messageQueue.remove(0);
            m.getTo().messages.add(m);
        }
        assert delay.size() == 0 && messageQueue.size() == 0 : "transmitAlert";
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(" Messages: " + messageNum + ", Proposals: " + proposals + ", Replies: " + replies + ", Results: " + results);
        return str.toString();
    }

}
