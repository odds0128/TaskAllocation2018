import java.util.ArrayList;
import java.util.List;


/**
 * Edgeクラス
 * 辺の情報を格納するクラス
 */
public class Edge implements SetParam {
    List<Integer> from_id;      // エッジの根元
    List<Integer> to_id;        // エッジの先端
    List<Integer> distance;
    List<Boolean> isRecipro;    // 互恵主義かどうか

    Edge() {
        from_id = new ArrayList<>();
        to_id = new ArrayList<>();
        distance = new ArrayList<>();
        isRecipro = new ArrayList<>();
    }


    public void makeEdge(List<Agent> agents) {
        for (Agent ag : agents) {
            if (ag.e_member > ag.e_leader) {
                for (int i = 0; i < AGENT_NUM; i++) {
                    if (ag.workWithAsM[i] >= THRESHOLD_FOR_COALITION) {
                        from_id.add(ag.id);
                        to_id.add(i);
                        distance.add(Manager.distance[ag.id][i]);
                        if (ag.principle == RECIPROCAL) isRecipro.add(true);
                        else isRecipro.add(false);
                    }
                }
            }
        }
    }

    public void makeCoalitionEdges(List<Agent> agents) {
        for (Agent ag : agents) {
            if (ag.e_member > ag.e_leader) {
                for (int i = 0; i < AGENT_NUM; i++) {
                    if (ag.workWithAsM[i] >= THRESHOLD_FOR_COALITION) {
                        from_id.add(ag.id);
                        to_id.add(i);
                        if (ag.principle == RECIPROCAL) isRecipro.add(true);
                        else isRecipro.add(false);
                    }
                }
            }
        }
    }

}
