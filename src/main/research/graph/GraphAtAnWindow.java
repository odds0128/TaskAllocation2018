package main.research.graph;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.grid.Grid;

import static main.research.SetParam.Principle.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Edgeクラス
 * 辺の情報を格納するクラス
 */
public class Network implements SetParam {
    private static final int agent_num_ = AgentManager.agent_num_;

    public List<Integer> from_id;      // エッジの根元
    public List<Integer> to_id;        // エッジの先端
    public List<Integer> times;        // そのリーダーと何度仕事をしたか

    public Network( ) {
        from_id = new ArrayList<>();
        to_id = new ArrayList<>();
        times     = new ArrayList<>();
    }

    public void makeEdge(List<Agent> agents) {
        int temp;
        int recipro = 0;
        int edges_per_hard_worker=0;
        int hard_worker_num  = 0;
        boolean hard_worker_flag = false;
        for (Agent ag : agents) {
            // agがメンバの場合
            if (ag.e_member > ag.e_leader) {
                // iは相手のid
                for ( int id = 0; id < agent_num_; id++) {
                    temp = ag.workWithAsM[id] ;
                    if ( temp >= threshold_of_frequency_of_working_together_ ) {
                        from_id.add(ag.id);
                        to_id.add(id);
                        times.add(temp);
                        hard_worker_flag = true;
                        edges_per_hard_worker++;
                        if (ag.principle == RECIPROCAL) {
                            recipro++;
                        }
                    }
                }
                if(hard_worker_flag){
                    hard_worker_num++;
                }
            }
        }
        System.out.println("Edges: " + from_id.size() + ", Reciprocal edges:" + recipro);
        System.out.println("Edges from one member: " + (double) edges_per_hard_worker/hard_worker_num);
    }

    public void makeEdgesFromLeader(List<Agent> agents) {
        int temp;
        for (Agent ag : agents) {
            // agがリーダーの場合
            if (ag.e_member < ag.e_leader) {
                // idは相手のid
                for ( int id = 0; id < agent_num_; id++) {
                    temp = ag.workWithAsL[id] ;
                    if ( temp >= threshold_of_frequency_of_working_together_ ) {
                        from_id.add(ag.id);
                        to_id.add(id);
                        times.add(temp);
                    }
                }
            }
        }
    }

    public void reset(){
        from_id.clear();
        to_id.clear();
        times.clear();
    }

}
