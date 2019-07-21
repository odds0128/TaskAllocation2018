package main.research.strategy;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.random.MyRandom;
import main.research.strategy.ProposedStrategy.LeaderProposedStrategy;
import main.research.strategy.ProposedStrategy.MemberProposedStrategy;
import org.junit.jupiter.api.*;

import java.util.List;

@Tag("strategy")
class ProposedStrategyTest {
    static LeaderStrategy ls = new LeaderProposedStrategy();
    static MemberStrategy ms = new MemberProposedStrategy();
    static List<Agent> agentList;

    static {
        System.out.println("ProposedStrategyTest");
    }

    @BeforeAll
    static void setUp() {
        MyRandom.newSfmt(0);
        AgentManager.initiateAgents(ls, ms);
        agentList = AgentManager.getAgentList();
    }

    @AfterAll
    static void tearDown() {
        AgentManager.clear();
        Agent.clear();
    }
}