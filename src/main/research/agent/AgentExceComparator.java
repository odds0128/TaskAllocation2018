package main.research.agent;

import java.util.Comparator;

public class AgentExceComparator implements Comparator<Agent>{
    public int compare(Agent a, Agent b ){
        double no1  = a.excellence;
        int    no11 = a.resCount;
        int    no12 = a.id;
        double no2  = b.excellence;
        int    no21 = b.resCount;
        int    no22 = b.id;

        // excellenceで比べる
        if( no1 > no2 )         return 1;
        else if( no1 < no2 )    return -1;
        // excellenceが等しいなら0でないリソースの数で比べる
        else if( no11 > no21 )  return 1;
        else if( no11 < no21 )  return -1;
        // それも一緒ならidで比べる
        else if( no12 < no22 )  return 1;
        else                    return -1;
    }
}
