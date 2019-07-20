package main.research.agent;

import main.research.random.MyRandom;
import main.research.strategy.LeaderStrategy;
import main.research.strategy.MemberStrategy;
import main.research.strategy.ProposedStrategy.LeaderProposedStrategy;
import main.research.strategy.ProposedStrategy.MemberProposedStrategy;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag("agent")
public class AgentTest {
    List<Agent> agentList = new ArrayList<>();
    LeaderStrategy ls = new LeaderProposedStrategy();
    MemberStrategy ms = new MemberProposedStrategy();

    @BeforeEach
    void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        MyRandom.newSfmt(0);
        AgentManager am = new AgentManager();

        Method ga = AgentManager.class.getDeclaredMethod("generateAgents");
        ga.setAccessible(true);
        agentList = (List<Agent>) ga.invoke( am );

        Field field = am.getClass().getDeclaredField("agents");
        field.setAccessible(true);
        field.set(am, agentList);

        Method dl = AgentManager.class.getDeclaredMethod("deployAgents");
        dl.setAccessible(true);
        dl.invoke( am );
    }

    @Nested
    class setReliabilityRankingRandomlyのテスト {
        @BeforeEach
        void setUp() {
            agentList.forEach(
                    agent -> {
                        agent.setReliabilityRankingRandomly(agentList);
                    }
            );
        }

        @Test
        void setReliabilityRankingRandomlyでリーダーの場合とメンバの場合で異なるランキングが得られる() {
            List<Agent> ranking_l;
            List<Agent> ranking_m;
            boolean isFullySame = true;

            for( Agent ag: agentList ) {
                ranking_l = new ArrayList<>( ag.relRanking_l.keySet() );
                ranking_m = new ArrayList<>( ag.relRanking_m.keySet() );
                System.out.println();
                for (int i = 0; i < agentList.size() - 1; i++) {
                   if( ! ranking_l.get(i).equals(ranking_m.get(i)) ) {
                       isFullySame = false;
                       break;
                   }
                }
                assertThat( isFullySame, is( false ) );
                isFullySame = true;
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