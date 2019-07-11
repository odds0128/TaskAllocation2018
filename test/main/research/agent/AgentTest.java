package main.research.agent;

import main.research.random.MyRandom;
import main.research.strategy.PM2withRoleFixed;
import main.research.strategy.Strategy;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class AgentTest {
    List<Agent> agentList = new ArrayList<>();
    Strategy strategy = new PM2withRoleFixed();

    @BeforeEach
    void setUp() {
        MyRandom.newSfmt(0);
        agentList = AgentManager.generateAgents(strategy);
    }

    @Nested
    class setReliabilityRankingRandomlyのテスト {
        @BeforeEach
        void setUp() {
            agentList.forEach(
                    agent -> agent.setReliabilityRankingRandomly(agentList)
            );
        }

        @Test
        void setReliabilityRankingRandomlyでリーダーの場合とメンバの場合で異なるランキングが得られる() {
            List<Agent> ranking_l;
            List<Agent> ranking_m;
            boolean isFullySame = true;

            for( Agent ag: agentList ) {
                ranking_l = ag.relRanking_l;
                ranking_m = ag.relRanking_m;

                for (int i = 0; i < agentList.size() - 1; i++) {
                   if( ! ranking_l.get(i).equals(ranking_m.get(i)) ) {
                       isFullySame = false;
                       break;
                   }
                }
                assertThat( isFullySame, is( false ) );
            }
        }

        @AfterEach
        void tearDown() {
            agentList.forEach(
                    agent -> {
                        agent.relRanking_l.clear();
                        agent.relRanking_m.clear();
                    }
            );
        }

    }
}