/**
 * @author Funato
 * @version 1.0
 */

import java.util.ArrayList;
import java.util.Random;

public class Agent {
    public static final int leader = 1 ;
    public static final int wait   = 0 ;
    public static final int member = -1;
    public static final int relMax = 5 ;

    int    role   = wait;
    double e_leader = 0.5, e_member = 0.5;
    int res[] = {};
    double rel[] = {};
    ArrayList<Integer> relAgents = new ArrayList<Integer>();

    /**
     * setRelメソッド
     * 信頼度と信頼するエージェントの初期化.
     * 信頼度は全員初期値0.5とし, 信頼するエージェントはランダムに選ぶ(近くのエージェントから?).
    */
    private void setRel(int n){
        for( int i = 0; i < n; i++ ) {
            rel[i] = 0.5;
        }
    }

    /**
     * setRelAgメソッド
     * パラメータの数だけ信頼するエージェントを定める.
     * @param n
     */
    private void setRelAg(int n){
    }

    /**
     * doRoleメソッド
     * eの値によって次の自分の役割を選択する.
     */
    private void doRole(){
        if( e_leader > e_member ) role = leader;
        else if( e_member > e_leader ) role = member;
        else{
            Random random = new Random();
            boolean ran = random.nextBoolean();
            if( ran ) role = leader;
            else role = member;
        }
    }
}
