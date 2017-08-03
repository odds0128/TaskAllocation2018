/**
 * @author Funato
 * @version 1.0
 */

import java.lang.Math;
import java.util.Random;

public class SubTask {
    public static final int resNum  = 6;
    public static final int bias    = 0;
    public static final int uniform = 1;
    int reqRes[] = new int[resNum];
    Random random = new Random();

    /**
     * コンストラクタ
     * パラメータで指定されたタイプでサブタスクを初期化する.
     * 今はそれだけ
     * @param taskType
     */
    SubTask( int taskType ){
        setResources(taskType);
    }

    /**
     * setResourcesメソッド
     * パラメータが指定するタイプのサブタスクが要求するリソースの設定.
     * @param taskType
     */
    void setResources(int taskType){
        // とりあえずuniformだけで
        if( taskType ==  bias){
        }else{
            for (int i = 0; i < resNum; i++) {
                int rand = random.nextInt(2);
                reqRes[i] = rand;
            }
        }
    }

}
