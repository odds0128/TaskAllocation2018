package main.research.task; /**
 * @author Funato
 * @version 2.0
 */

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.random.MyRandom;

import java.util.Comparator;

public class Subtask implements SetParam {
    private static int _subtask_id;
    int subtask_id;
    public int[] reqRes = new int[RESOURCE_TYPES];
    public int resType;
    public Agent from;

    Subtask(boolean b) {
        if (b) _subtask_id = 0;
        this.subtask_id = _subtask_id;
        _subtask_id++;
        setResources();
    }

    private void setResources() {
        resType = MyRandom.getRandomInt(0, RESOURCE_TYPES - 1);
        reqRes[resType] = MyRandom.getRandomInt( MIN_SUBTASK_RESOURCE_SIZE, MAX_SUBTASK_RESOURCE_SIZE );
    }

    void setFrom(Agent agent){
        this.from = agent;
    }

    Agent getFrom(){
        return from;
    }

    public static void clearST() {
        _subtask_id = 0;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < RESOURCE_TYPES; i++) str.append(String.format("%3d", reqRes[i]) + ", ");
        str.append("]");
        return str.toString();
    }


    public static class SubtaskRewardComparator implements Comparator<Subtask> {
        public int compare(Subtask a, Subtask b ){
            int no1 = a.reqRes[a.resType];
            int no2 = b.reqRes[b.resType];

            if( no1 < no2 ) return 1;
            else return -1;
        }
    }
}
