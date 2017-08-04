/**
 * @author Funato
 * @version 1.0
 */

import java.util.ArrayList;

public class Manager implements SetParam {

    public static void main(String[] args) {
        int disposedTasks = 0;
        ArrayList<Task> taskQueue = new ArrayList<Task>();

        // タスクの生成
        for ( int i = 0; i < TASK_QUEUE_SIZE; i++) {
            taskQueue.add(new Task(SetParam.UNIFORM));
        }
        // エージェントの生成
        Agent[][] agents = new Agent[ROW][COLUMN];
        for ( int i = 0; i < ROW; i++) {
            for( int j = 0; j < COLUMN; j++ ) {
                agents[i][j] = new Agent();
            }
        }

        // ターンの進行
        for( int turn = 0; turn < TURN_NUM ; turn++ ){
            // 数ターン毎にタスクを追加する(タスクキューがいっぱいなら, 破棄とする)
            for( int t = 0; t < ADD_TASK_PER_TURN; t++ ){
                if( taskQueue.size() >= TASK_QUEUE_SIZE ) disposedTasks++;
                else taskQueue.add(new Task(SetParam.UNIFORM) );
            }

            // Agentが毎ターンなんかする
            for( int i = 0; i < ROW; i++ ){
                for( int j = 0; j < COLUMN; j++ ) {

                }
            }
        }

        CheckParam.checkTask( taskQueue );
        CheckParam.checkAgent( agents );
    }
}
