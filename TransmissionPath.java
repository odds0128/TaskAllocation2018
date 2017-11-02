/**
 *
 */

import java.util.ArrayList;
import java.util.List;

class TransmissionPath implements SetParam{
    private static TransmissionPath transmissionPath = new TransmissionPath();
    private static List<Message> messageQueue = new ArrayList<>();
    private static List<Integer> delay = new ArrayList<>();

    private TransmissionPath(){
    }

    public static TransmissionPath getInstance(){
        return transmissionPath;
    }

    /**
     * sendMessageメソッド
     * 送りたいメッセージとその遅延時間をパラメータとし, 格納する
     * @param message
     */
    static void sendMessage(Message message){
        messageQueue.add(message);
        delay.add(calcTicks(message));
//        System.out.println(" from: " + message.getFrom().id + ", to: " + message.getTo().id + ", distance: " + calcTicks(message));
    }

    /**
     * transmissionメソッド
     * 通信時間を管理し, それが0になったメッセージを宛先のmessageリストに追加する
     */
    static void transmit(){
        int tempI;
        Message tempM;
        int size = messageQueue.size();
        for( int i = 0; i < size; i++ ){
            tempI = delay.remove(0);
            tempI--;
            tempM = messageQueue.remove(0);
            if( tempI == 0){
                tempM.getTo().messages.add( tempM );
            }else{
                delay.add(tempI);
                messageQueue.add(tempM);
            }
        }
    }

    static int calcTicks(Message message){
            return Math.abs( message.getFrom().x - message.getTo().x ) + Math.abs( message.getFrom().y - message.getTo().y );
    }

    static void clearTP(){
        messageQueue.clear();
        delay.clear();
    }

    /**
     * transmitWithNoDelayメソッド
     * 通信所要時間を考慮しない場合のメッセージ伝送を行う
     */
    static public void transmitWithNoDelay( ){
        for( int i = 0; i < messageQueue.size(); i++ ){
            delay.remove(i);
            messageQueue.get(i).getTo().messages.add( messageQueue.remove(i) );
        }
    }
}
