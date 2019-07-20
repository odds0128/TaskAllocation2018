package main.research.agent;

import main.research.random.MyRandom;
import main.research.strategy.LeaderStrategy;
import main.research.strategy.MemberStrategy;
import main.research.strategy.ProposedStrategy.LeaderProposedStrategy;
import main.research.strategy.ProposedStrategy.MemberProposedStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static main.research.SetParam.AGENT_NUM;
import static org.hamcrest.CoreMatchers.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

@Tag("agent")
public class AgentManagerTest {
    private LeaderStrategy ls;
    private MemberStrategy ms;

    @BeforeEach
    void setUp() {
        ls = new LeaderProposedStrategy();
        ms = new MemberProposedStrategy();
        MyRandom.newSfmt(0);
    }

    @Nested
    class generateAgentsのテスト {
        Object obj;
        Method ga;

        @BeforeEach
        void setUp() throws NoSuchMethodException {
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