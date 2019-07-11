package main.research.task;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.random.MyRandom;

import java.util.Comparator;

public class Subtask implements SetParam {
    public int[] reqRes = new int[RESOURCE_TYPES];
    public int resType;
    public Agent from;

    Subtask() {
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

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < RESOURCE_TYPES; i++) str.append(String.format("%3d", reqRes[i])).append(", ");
        str.append("]");
        return str.toString();
    }


    public static class SubtaskRewardComparator implements Comparator<Subtask> {
        public int compare(Subtask a, Subtask b ){
            int no1 = a.reqRes[a.resType];
            int no2 = b.reqRes[b.resType];

            return Integer.compare(no2, no1);
        }
    }
}
