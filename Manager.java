/**
 * @author Funato
 * @version 1.0
 */

import java.util.ArrayList;

public class Manager implements SetParam {

    public static void main(String[] args) {

        // タスクの生成(最初の分)　定期的に追加される部分は別で実装
        ArrayList<Task> taskQueue = new ArrayList<Task>();
        for ( int i = 0; i < maxTasks; i++) {
            taskQueue.add(new Task(1));
        }
        // エージェントの生成
        Agent[][] agents = new Agent[row][column];

        CheckParam.checkTask( taskQueue );
        CheckParam.checkAgent( agents );
    }
}
