import java.util.Comparator;

public class DistanceComparator implements Comparator<LearnedDistance> {
    public int compare(LearnedDistance da, LearnedDistance db ){
        int no1 = da.distance;
        int no2 = db.distance;

        if( no1 > no2 ) return 1;
        else return -1;
    }
}
