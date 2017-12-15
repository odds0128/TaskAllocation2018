import java.util.Comparator;

public class DistanceComparator implements Comparator<LearnedDistance> {
    public int compare(LearnedDistance da, LearnedDistance db ){
        assert da.getFrom() == db.getFrom() : "each leader is different";
        int dis1 = da.getDistance();
        int dis2 = db.getDistance();
        double rel1 = da.getFrom().reliabilities[da.getTarget().id];
        double rel2 = db.getFrom().reliabilities[db.getTarget().id];

        if( dis1 > dis2 ) return 1;
        else if( dis1 < dis2 ) return -1;
        else if( rel1 < rel2 ) return 1;
        else if( rel1 > rel2 ) return -1;
        else return 0;
    }
}
