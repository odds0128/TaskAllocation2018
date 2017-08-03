/**
 * @author Funato
 * @version 1.0
 */

import java.util.ArrayList;
import java.util.Random;

public class Task {
    public static final int resNum  = 6;
    ArrayList<SubTask> subTasks = new ArrayList<SubTask>();
    Random random = new Random();
    int subTaskNum = random.nextInt(4)+3;;

    /**
     * コンストラクタ
     * パラメータで指定されたタイプのサブタスクをランダムに生成する.
     * @param taskType
     */
    Task( int taskType ){
        setSubTasks(taskType);
    }

    /**
     * setSubTasksメソッド
     * パラメータで指定されたタイプのサブタスクを指定数作成する.
     * @param taskType
     */
    void setSubTasks(int taskType){
        for( int i = 0; i < subTaskNum; i++){
            subTasks.add(new SubTask(1));
        }
    }

    /**
     * showSubTaskメソッド
     * サブタスクの内容表示(確認用).
     * あるタスクを構成する全サブタスクの必要リソースを表示する.
     */
    void showSubTask(){
        for(SubTask subTask : subTasks){
            for( int i = 0; i < resNum; i++ ){
                System.out.print(subTask.reqRes[i]);
            }
            System.out.println();
        }
    }
}
