/**
 * @author Funato
 * @version 2.0
 */

import java.util.Random;

public class SubTask implements SetParam{
    private static int _subtask_id ;
    static long _seed;
    static Random _randSeed;
    int subtask_id ;
    int reqRes;

    /**
     * コンストラクタ
     * b == true はあるタスクについて最初のサブタスク生成ということ
     * seedを引数とするこちらのコンストラクタはタームの最初のサブタスク生成時に呼び出される
     */
    SubTask( boolean b, long seed ){
        if( b ) _subtask_id = 0;
        setSeed(seed);
        this.subtask_id = _subtask_id;
        _subtask_id ++;
        setResource();
    }

    /**
     * コンストラクタ
     * 上と同様に, 同じseedを使ってサブタスクを生成する
     */
    SubTask( boolean b ){
        if( b ) _subtask_id = 0;
        this.subtask_id = _subtask_id;
        _subtask_id ++;
        setResource();
    }

    /**
     * setResourcesメソッド
     * 1~4の間で要求リソースを定義
     */
    private void setResource(){
          int rand = _randSeed.nextInt(4) + 1;
          reqRes   = rand;
    }

    static void setSeed(long seed){
        _seed = seed;
        _randSeed = new Random(_seed);
    }
    static void clearST(){
        _subtask_id = 0;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(" subtask " + subtask_id + ": " + reqRes);
        return str.toString();
    }
}
