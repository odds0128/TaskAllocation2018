import java.util.ArrayList;
import java.util.List;


/**
 * Edgeクラス
 * 辺の情報を格納するクラス
 */
public class Edge implements SetParam {
    static List<Integer> from_id = new ArrayList<>();      // エッジの根元
    static List<Integer> to_id = new ArrayList<>();        // エッジの先端
    static List<Integer> distance = new ArrayList<>();
    static List<Boolean> isRecipro = new ArrayList<>();    // 互恵主義かどうか

    static public void makeEdge(List<Agent> agents) {
        for (Agent ag : agents) {
            if (ag.e_member > ag.e_leader) {
                for (int i = 0; i < AGENT_NUM; i++) {
                    if (ag.workWithAsM[i] >= THRESHOLD_FOR_COALITION) {
                        from_id.add(ag.id);
                        to_id.add(i);
                        distance.add( Manager.distance[ag.id][i] );
                        if( ag.principle == RECIPROCAL  ) isRecipro.add(true);
                        else isRecipro.add(false);
                    }
                }
            }
        }
    }

    static public void makeCoalitionEdges(List<Agent> agents) {
        for (Agent ag : agents) {
            if (ag.e_member > ag.e_leader) {
                for (int i = 0; i < AGENT_NUM; i++) {
                    if (ag.workWithAsM[i] >= THRESHOLD_FOR_COALITION) {
                        from_id.add(ag.id);
                        to_id.add(i);
                        if( ag.principle == RECIPROCAL  ) isRecipro.add(true);
                        else isRecipro.add(false);
                    }
                }
            }
        }
    }

}
