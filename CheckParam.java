/**
 * @author Funato
 * @version 1.0
 */

import java.util.ArrayList;

public class CheckParam implements SetParam {

    /**
     * checkTaskメソッド
     * 現在タスクキューにあるタスクのサブタスク数とその必要リソースの表示.
     * @param taskQueue
     */
    static void checkTask(ArrayList<Task> taskQueue ) {
        int num = taskQueue.size();
        System.out.println("Queuesize: " + num);
        for (int i = 0; i < num; i++) {
            Task temp = taskQueue.remove(0);
            System.out.print( temp );
            System.out.println("  Remains: " + taskQueue.size());
        }
        System.out.println();
    }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(Agent[][] agents) {
        System.out.println("Total Agents is " + Agent._id );
        System.out.println("Leaders is " + Agent._leader_num );
        System.out.println("Members is " + Agent._member_num );
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                System.out.print(agents[i][j]);
            }
        }
    }
}
