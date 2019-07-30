package main.research.agent;

import main.research.SetParam;
import main.research.grid.Grid;
import main.research.strategy.LeaderStrategy;
import main.research.strategy.MemberStrategy;

import java.util.ArrayList;
import java.util.List;


public class AgentManager implements SetParam {
    private static List<Agent> agents;

    // TODO: Agentインスタンスを生成する → 被らないように座標を設定する
    public static void initiateAgents( LeaderStrategy ls, MemberStrategy ms ) {
        agents = generateAgents(ls, ms);
        deployAgents();
        setReliabilityRanking();
    }

    private static List<Agent> generateAgents(LeaderStrategy ls, MemberStrategy ms) {
        List<Agent> agentList = new ArrayList();

        for (int i = 0; i < AGENT_NUM; i++) {
            agentList.add( new Agent(ls, ms) );
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
