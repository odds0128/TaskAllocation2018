/**
 * @author Funato
 * @version 1.1
 */

import java.util.Queue;

class CheckParam implements SetParam {

    /**
     * checkTaskメソッド
     * 現在タスクキューにあるタスクのサブタスク数とその必要リソースの表示.
     * @param taskQueue
     */
    static void checkTask(Queue<Task> taskQueue ) {
        int num = taskQueue.size();
        System.out.println("Queuesize: " + num);
        for (int i = 0; i < num; i++) {
            Task temp = taskQueue.poll();
            System.out.print( temp );
            taskQueue.add(temp);
        }
        System.out.println("  Remains: " + taskQueue.size() );
    }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(Agent[][] agents) {
        System.out.println("Total Agents is " + Agent._id );
        System.out.println("Leaders is " + Agent._leader_num +", Members is " + Agent._member_num  );
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                System.out.println(agents[i][j]);
            }
        }
    }

}
