import java.util.Comparator;

public class AgentIDcomparator implements Comparator<Agent>{
    public int compare(Agent a, Agent b ){
        int no1 = a.id;
        int no2 = b.id;

        if( no1 > no2 ) return 1;
        else return -1;
    }
}
