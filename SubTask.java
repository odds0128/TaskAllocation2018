/**
 * @author Funato
 * @version 1.1
 */

import java.util.Random;

public class SubTask implements SetParam{
    static int _subtask_id ;
    int subtask_id ;
    int reqRes[] = new int[RESOURCE_NUM];
    Random random = new Random();

    /**
     * コンストラクタ
     * パラメータで指定されたタイプでサブタスクを初期化する.
     * @param taskType
     */
    SubTask( int taskType, boolean b ){
        if( b ) _subtask_id = 0;
        this.subtask_id = _subtask_id;
        _subtask_id ++;
        setResources(taskType);
    }

    /**
     * setResourcesメソッド
     * パラメータが指定するタイプのサブタスクが要求するリソースの設定.
     * @param taskType
     */
    private void setResources(int taskType){
        // とりあえずuniformだけで
        if( taskType ==  BIAS){
        }else{
            for (int i = 0; i < RESOURCE_NUM; i++) {
                int rand = random.nextInt(2);
                reqRes[i] = rand;
            }
        }
    }

    @Override
    public String toString() {
        String str = " subtask " + subtask_id + ": " ;
        for( int i = 0; i < RESOURCE_NUM ;i++ ){
            str +=  reqRes[i] +" " ;
        }
        return str;
    }
}
