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
            System.out.println("Queuesize: " + taskQueue.size());
            for (int i = 0; i < maxTasks; i++) {
                Task temp = taskQueue.remove(0);
                System.out.println("number of subtasks : " + temp.subTaskNum);
                temp.showSubTask();
            }
            System.out.println("Queuesize: " + taskQueue.size());
        }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(Agent[][] agents) {
            for (int i = 0; i < row; i++) {
                for (int j = 0; j < column; j++) {
//                    System.out.print("ID: " + Agent.id + ", ");
                }
                System.out.println();
            }
        }

}
