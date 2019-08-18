package main.research.agent;

import main.research.others.random.MyRandom;
import main.research.agent.strategy.LeaderStrategy;
import main.research.agent.strategy.MemberStrategy;
import main.research.agent.strategy.ProposedStrategy.ProposedStrategy_l;
import main.research.agent.strategy.ProposedStrategy.ProposedStrategy_m;
import org.junit.jupiter.api.*;

import static main.research.SetParam.AGENT_NUM;
import static org.hamcrest.CoreMatchers.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class AgentManagerTest {
    static {
        System.out.println("AgentManagerTest");
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

    @Nested
    static class generateRandomAgentListのテスト {
        List <Agent> mockAgentList = new ArrayList<>(  );
        int agents = 100;
        Object obj;
        Method gral;

        @BeforeAll
        void setUp() throws NoSuchMethodException {
            for( int i = 0; i < agents; i++ ) mockAgentList.add( mock(Agent.class) );
            obj = new AgentManager();
            gral = AgentManager.class.getDeclaredMethod( "generateRandomAgentList" , List.class);
            gral.setAccessible(true);
        }

        @Test
        void 元のリストと同じものを返さない() throws InvocationTargetException, IllegalAccessException {
            List<Agent> actual = ( List< Agent > ) gral.invoke( mockAgentList );
            for( int i = 0; i < agents; i++ ) {
                assertThat( actual.get( i ), is( not ( mockAgentList.get( i ) ) ) );
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