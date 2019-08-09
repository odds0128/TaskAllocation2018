package main.research.agent;

import main.research.random.MyRandom;
import main.research.agent.strategy.LeaderStrategy;
import main.research.agent.strategy.MemberStrategy;
import main.research.agent.strategy.ProposedStrategy.LeaderProposedStrategy;
import main.research.agent.strategy.ProposedStrategy.MemberProposedStrategy;
import org.junit.jupiter.api.*;

import static main.research.SetParam.AGENT_NUM;
import static org.hamcrest.CoreMatchers.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

@Tag("agent")
public class AgentManagerTest {
    private static LeaderStrategy ls;
    private static MemberStrategy ms;

    static {
        System.out.println("AgentManagerTest");
    }

    @BeforeAll
    static void setUp() {
        ls = new LeaderProposedStrategy();
        ms = new MemberProposedStrategy();
        MyRandom.newSfmt(0);
    }

    @Nested
    static class generateAgentsのテスト {
        static Object obj;
        static Method ga;

        @BeforeAll
        static void setUp() throws NoSuchMethodException {
            obj = new AgentManager();
            ga = AgentManager.class.getDeclaredMethod( "generateAgents" );
            ga.setAccessible(true);
        }

        @Test
        void generateAgentsでAgentインスタンスをAGENT_NUM体生成できる() throws InvocationTargetException, IllegalAccessException {
            List<Agent> actual = (List<Agent>) ga.invoke(obj);
            int expected = AGENT_NUM;
            assertThat( actual.size(), is(expected) );
        }

        @Test
        void generateAgentsで生成したAgentがすべて異なるidと座標を持つ() throws InvocationTargetException, IllegalAccessException {
            List<Agent> agents = (List<Agent>) ga.invoke(obj);
            for (int i = 0; i < AGENT_NUM - 1; i++) {
                Agent from = agents.get(0);

                for (int j = i + 1; j < AGENT_NUM; j++) {
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