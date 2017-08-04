/**
 * @author Funato
 * @version 1.0
 */

import java.util.ArrayList;
import java.util.Random;

public class Agent implements SetParam{
    public static int _id = 0;
    public static int _leader_num = 0;
    public static int _member_num = 0;
    int id;
    int    role   = WAIT;
    double e_leader = 0.5, e_member = 0.5;
    int res[] = {};
    double rel[] = {};
    ArrayList<Integer> relAgents = new ArrayList<Integer>();

    Agent(){
        id = _id;
        _id++;
        doRole();
    }

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
        if( e_leader > e_member ) {
            role = LEADER;
            _leader_num++;
        }else if( e_member > e_leader ){
            role = MEMBER;
            _member_num++;
        }else{
            Random random = new Random();
            boolean ran = random.nextBoolean();
            if( ran ){
                role = LEADER;
                _leader_num++;
            }else{
                role = MEMBER;
                _member_num++;
            }
        }
    }

    /**
     * inactiveメソッド
     * チームが解散になったときに待機状態になる.
     */
    private void inactivate(){
        if( role == LEADER ) _leader_num-- ;
        else _member_num-- ;
        role = WAIT;
    }

    @Override
    public String toString(){
        String str = "ID: " + id + ", ";
        if( role == LEADER ) str += "I'm a leader. \n" ;
        else str += "I'm a member. \n";
        return str;
    }
}
