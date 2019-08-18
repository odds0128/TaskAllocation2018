package main.research.agent;

import main.research.SetParam;
import main.research.grid.Grid;
import main.research.others.random.MyRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AgentManager implements SetParam {
    private static List<Agent> agentList;

    // TODO: Agentインスタンスを生成する → 被らないように座標を設定する
    public static void initiateAgents( String ls_name, String ms_name ) {
        agentList = generateAgents(ls_name, ms_name);
        deployAgents();
        setReliabilityRanking();
    }

    private static List<Agent> generateAgents( String ls_name, String ms_name ) {
        List<Agent> agentList = new ArrayList();

        for (int i = 0; i < AGENT_NUM; i++) {
            agentList.add( new Agent(ls_name, ms_name) );
        }
        return agentList;
    }

    private static void deployAgents() {
        agentList.forEach(
            Grid::setAgentOnEnvironment
        );
    }

    private static void setReliabilityRanking() {
    	for( Agent ag: agentList ) {
    	    ag.ls.setMemberRankingRandomly( agentList ); ag.ls.removeMyselfFromRanking( ag );
    	    ag.ms.setLeaderRankingRandomly( agentList ); ag.ms.removeMyselfFromRanking( ag );
        }
    }

    public static List< Agent > generateRandomAgentList( List< Agent > agentList ) {
        List< Agent > originalList = new ArrayList<>( agentList );
        Collections.shuffle( originalList, MyRandom.getRnd() );
        return originalList;
    }


    public static List<Agent> getAgentList() {
        return agentList;
    }


    public static void clear() {
        agentList.clear();
    }

}
