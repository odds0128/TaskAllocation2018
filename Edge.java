import java.util.ArrayList;
import java.util.List;


/**
 * Edgeクラス
 * 辺の情報を格納するクラス
 */
public class Edge implements SetParam {
    List<Integer> from_id;      // エッジの根元
    List<Integer> to_id;        // エッジの先端
    List<Integer> delays;
    List<Boolean> isRecipro;    // 互恵主義かどうか
    List<Integer> times;        // そのリーダーと何度仕事をしたか

    Edge() {
        from_id = new ArrayList<>();
        to_id = new ArrayList<>();
        delays = new ArrayList<>();
        isRecipro = new ArrayList<>();
        times     = new ArrayList<>();
    }

    public void makeEdge(List<Agent> agents) {
        int temp;
        for (Agent ag : agents) {
            // agがメンバの場合
            if (ag.e_member > ag.e_leader) {
                // iは相手のid
                for (int id = 0; id < AGENT_NUM; id++) {
                    temp = ag.workWithAsM[id] ;
                    if ( temp >= THRESHOLD_FOR_COALITION) {
                        from_id.add(ag.id);
                        to_id.add(id);
                        delays.add(Manager.delays[ag.id][id]);
                        times.add(temp);
                        if (ag.principle == RECIPROCAL) isRecipro.add(true);
                        else isRecipro.add(false);
                    }
                }
            }
        }
    }

    public void makeEdgesFromLeader(List<Agent> agents) {
        int temp;
        for (Agent ag : agents) {
            // agがリーダーの場合
            if (ag.e_member < ag.e_leader) {
                // idは相手のid
                for (int id = 0; id < AGENT_NUM; id++) {
                    temp = ag.workWithAsL[id] ;
                    if ( temp >= THRESHOLD_FOR_COALITION) {
                        from_id.add(ag.id);
                        to_id.add(id);
                        delays.add(Manager.delays[ag.id][id]);
                        times.add(temp);
                        if (agents.get(id).principle == RECIPROCAL) isRecipro.add(true);
                        else isRecipro.add(false);
                    }
                }
            }
        }
    }

    public void reset(){
        from_id.clear();
        to_id.clear();
        delays.clear();
        times.clear();
        isRecipro.clear();
    }

}
