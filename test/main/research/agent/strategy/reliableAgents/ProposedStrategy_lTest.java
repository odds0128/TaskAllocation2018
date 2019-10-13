package main.research.agent.strategy.reliableAgents;

import main.research.agent.Agent;
import main.research.util.Initiation;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

class ProposedStrategy_lTest {
	static {
		System.out.println("ProposedStrategy (leader) Test");
	}

	static List<Agent> agentList;

	@BeforeAll
	static void setUp() {
		agentList = Initiation.getNewAgentList();
	}
}