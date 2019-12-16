package main.research.task;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.Parameter;
import main.research.others.random.MyRandom;

import java.util.Comparator;

public class Subtask implements Parameter, Cloneable {
    public static int resource_types_;
    private static int min_request_value_;
    private static int max_request_value_;

    private static int _id = 0;

    private int id;
    public int[] reqRes;
    public int resType;
    public int parentId;

    public static void setConstants( JsonNode parameterNode ) {
        resource_types_    = parameterNode.get( "resource_types" ).asInt();
        min_request_value_ = parameterNode.get( "min_request_value" ).asInt();
        max_request_value_ = parameterNode.get( "max_request_value" ).asInt();
    }

    Subtask( int parent_id ) {
        this.id = _id++;
        this.parentId = parent_id;
        setResources();
    }

    private void setResources() {
        reqRes = new int[resource_types_];
        resType = MyRandom.getRandomInt(0, resource_types_ - 1);
        reqRes[resType] = MyRandom.getRandomInt( min_request_value_, max_request_value_ );
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder( id + "[");
        for ( int i = 0; i < resource_types_ - 1; i++) {
            str.append(String.format("%2d", reqRes[i])).append(", ");
        }
        str.append( String.format( "%2d", reqRes[ resource_types_ - 1] ) ).append("]");
        return str.toString();
    }


    public static class SubtaskRewardComparator implements Comparator<Subtask> {
        public int compare(Subtask a, Subtask b ){
            int no1 = a.reqRes[a.resType];
            int no2 = b.reqRes[b.resType];

            return Integer.compare(no2, no1);
        }
    }

    @Override
    public Subtask clone() { //基本的にはpublic修飾子を付け、自分自身の型を返り値とする
        Subtask b = null;

        try {
            b = ( Subtask ) super.clone(); //親クラスのcloneメソッドを呼び出す(親クラスの型で返ってくるので、自分自身の型でのキャストを忘れないようにする)
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return b;
    }

}
