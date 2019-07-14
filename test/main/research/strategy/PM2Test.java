package main.research.strategy;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.random.MyRandom;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

class PM2Test {
    Strategy strategy = new PM2();
    List<Agent> agentList;

    @BeforeEach
    void setUp() {
        MyRandom.newSfmt(0);
        AgentManager.initiateAgents(strategy);
        agentList = AgentManager.getAgentList();
    }

}