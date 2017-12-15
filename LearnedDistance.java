import java.util.Collections;
import java.util.List;

/**
 * LearnedDistanceクラス
 * エージェントが学習した距離をその対象エージェントとともに記録する.
 * 2017/12/14
 * 距離とは厳密にはエージェント間の往復の通信時間となっている
 */
public class LearnedDistance {
    private Agent target;
    private Agent from;
    private int distance;

    LearnedDistance(Agent target, int distance, Agent from, List<LearnedDistance> ldl) {
        this.target = target;
        this.distance = distance;
        this.from = from;
        sortDT(this, ldl);
    }

    /**
     * sortDTメソッド
     * 新規に入れようとしているデータがすでに持っているものかを確認し,
     * 新規のものであればリストに入れてソートする
     *
     * @param ld  ... 新たに作ろうとしているエージェントと距離のデータの組
     * @param ldl ... 今知っているデータのリスト, 距離により降順に並んでいる
     */
    static private void sortDT(LearnedDistance ld, List<LearnedDistance> ldl) {
        // すでにリストにあればデータを消去してreturn
        for (LearnedDistance temp : ldl) {
            if (ld.target.equals(temp.target)) {
                ld = null;
                return ;
            }
        }
        // まだリストにないデータであればリストに入れ, それが適切な位置に行くようにソートする
        ldl.add(ld);
        Collections.sort(ldl, new DistanceComparator());
        return ;
    }

    public Agent getFrom() {
        return from;
    }

    public Agent getTarget() {
        return target;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("[" + target.id + "] : " + distance + ", " + from.reliabilities[target.id] + " ||| ");
        return str.toString();
    }

}
