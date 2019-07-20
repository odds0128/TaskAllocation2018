package main.research.strategy;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.random.MyRandom;
import main.research.strategy.ProposedStrategy.LeaderProposedStrategy;
import main.research.strategy.ProposedStrategy.MemberProposedStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import java.util.List;

@Tag("strategy")
class ProposedStrategyTest {
    LeaderStrategy ls = new LeaderProposedStrategy();
    MemberStrategy ms = new MemberProposedStrategy();
    List<Agent> agentList;

    @BeforeEach
    void setUp() {
        MyRandom.newSfmt(0);
        AgentManager.initiateAgents(ls, ms);
        agentList = AgentManager.getAgentList();
    }

}