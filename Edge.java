import java.util.ArrayList;
import java.util.List;

public class Edge implements SetParam {
    static List<Integer> from_id = new ArrayList<>();
    static List<Integer> to_id = new ArrayList<>();
    static List<Boolean> isRecipro = new ArrayList<>();

    static public void makeEdge(List<Agent> agents) {
        for (Agent ag : agents) {
            if (ag.e_member > ag.e_leader) {
                for (int i = 0; i < AGENT_NUM; i++) {
                    if (ag.workWithAsM[i] >= THRESHOLD_TASK_SUCCESSES) {
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
