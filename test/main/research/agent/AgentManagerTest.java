package main.research.agent;

import main.research.random.MyRandom;
import main.research.strategy.PM2withRoleFixed;
import main.research.strategy.Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.*;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

class AgentManagerTest {
    Strategy strategy;


    @BeforeEach
    void setUp() {
        strategy = new PM2withRoleFixed();
        MyRandom.newSfmt(0);
    }

    @Nested
    class generateAgentsのテスト {
        int agent_num = 100;

        @Test
        void generateAgentsでAgentインスタンスを100体生成できる() {
            List<Agent> actual = AgentManager.generateAgents(strategy);
            int expected = agent_num;
            assertThat( actual.size(), is(expected) );
        }

        @Test
        void generateAgentsで生成したAgentがすべて異なるidと座標を持つ() {
            List<Agent> agents = AgentManager.generateAgents(strategy);
            for (int i = 0; i < agent_num - 1; i++) {
                Agent from = agents.get(0);

                for (int j = i + 1; j < agent_num; j++) {
                    Agent target = agents.get(j);
                    assertThat( from, is( not( target ) ) );
                }
            }
        }
    }

    @Test
    void deployAgents() {
    }

    @Test
    void isDuplicateSite() {
    }
}