/**
 * @author Funato
 * @version 2.0
 */

import java.util.Random;

public class SubTask implements SetParam {
    private static int _subtask_id;
    static long _seed;
    static Random _randSeed;
    int subtask_id;
    int reqRes[] = new int[RESOURCE_TYPES];
    int resSize = 0;
    int resType;
    int resentTimes = 0;

    /**
     * コンストラクタ
     * b == true はあるタスクについて最初のサブタスク生成ということ
     * seedを引数とするこちらのコンストラクタはタームの最初のサブタスク生成時に呼び出される
     */
    SubTask(boolean b, long seed) {
        if (b) _subtask_id = 0;
        setSeed(seed);
        this.subtask_id = _subtask_id;
        _subtask_id++;
        setResources(UNIFORM);
    }

    /**
     * コンストラクタ
     * 上と同様に, 同じseedを使ってサブタスクを生成する
     */
    SubTask(boolean b) {
        if (b) _subtask_id = 0;
        this.subtask_id = _subtask_id;
        _subtask_id++;
        setResources(UNIFORM);
    }

    /**
     * setResourcesメソッド
     * 要求リソース
     */
    private void setResources(int taskType) {
        // とりあえずuniformだけで
        if (taskType == BIAS) {
/*            for (int i = 0; i < RESOURCE_NUM; i++) {
                int rand = random.nextInt(3) + 6;
                res[i] = rand;
            }
*/
        } else {
            resType = _randSeed.nextInt(RESOURCE_TYPES);
            reqRes[resType] = _randSeed.nextInt(MAX_SUBTASK_RESOURCE_SIZE - MIN_SUBTASK_RESOURCE_SIZE + 1) + MIN_SUBTASK_RESOURCE_SIZE;
            assert resSize > 0 : "SubTask requires no resource.";
        }
    }

    /**
     * canResendメソッド
     * このサブタスクが再送可能回数を超えていないか判断する
     * すでに規定の回数再送されていたらfalse
     * まだ再送できるのであればtrueを返す
     *
     * @return
     */
    public boolean canResend() {
        if (this.resentTimes >= RESEND_TIMES) return false;
        else {
            resentTimes++;
            return true;
        }
    }

    static void setSeed(long seed) {
        _seed = seed;
        _randSeed = new Random(_seed);
    }

    static void clearST() {
        _subtask_id = 0;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < RESOURCE_TYPES; i++) str.append(String.format("%3d", reqRes[i]) + ", ");
        str.append("]");
        return str.toString();
    }
}
