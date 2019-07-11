package main.research.agent;

import main.research.SetParam;
import main.research.grid.Coordinates;
import main.research.grid.Grid;
import main.research.random.MyRandom;
import main.research.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;


public class AgentManager implements SetParam {

    private static List<Agent> agents;


    // TODO: Agentインスタンスを生成する → 被らないように座標を設定する
    public static void initiateAgents( Strategy strategy ) {
        agents = generateAgents(strategy);
        deployAgents();
        setReliabilityRanking();
    }

    public static List<Agent> generateAgents(Strategy strategy) {
        List agentList = new ArrayList();

        for (int i = 0; i < AGENT_NUM; i++) {
            agentList.add( new Agent( strategy ) );
        }
        return agentList;
    }

    static void deployAgents() {
        agents.forEach(
                agent -> {
                    agent.p = Grid.newVacantSpot();
                    Grid.setAgentOnEnvironment(agent, agent.p.getX(), agent.p.getY() );
                }
        );
    }

    static void setReliabilityRanking() {
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
