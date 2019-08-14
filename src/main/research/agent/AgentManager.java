package main.research.agent;

import main.research.SetParam;
import main.research.grid.Grid;
import main.research.agent.strategy.LeaderStrategy;
import main.research.agent.strategy.MemberStrategy;

import java.util.ArrayList;
import java.util.List;


public class AgentManager implements SetParam {
    private static List<Agent> agents;

    // TODO: Agentインスタンスを生成する → 被らないように座標を設定する
    public static void initiateAgents( String ls_name, String ms_name ) {
        agents = generateAgents(ls_name, ms_name);
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
        agents.forEach(
            Grid::setAgentOnEnvironment
        );
    }

    private static void setReliabilityRanking() {
        agents.forEach(
                agent -> agent.setReliabilityRankingRandomly(agents)
        );
    }



    public static List<Agent> getAgentList() {
        return agents;
    }


    public static void clear() {
        agents.clear();
    }

}
