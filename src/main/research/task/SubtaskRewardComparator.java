package main.research.task;

import java.util.Comparator;

public class SubtaskRewardComparator implements Comparator<Subtask> {
    public int compare(Subtask a, Subtask b ){
        int no1 = a.reqRes[a.resType];
        int no2 = b.reqRes[b.resType];

        if( no1 < no2 ) return 1;
        else return -1;
    }
}
