package main.research.agent;

import main.research.agent.strategy.MemberStrategyWithRoleChange;
import main.research.agent.strategy.ProposedStrategy.ProposedStrategy_l;
import main.research.grid.Grid;
import main.research.others.random.MyRandom;
import main.research.agent.strategy.LeaderStrategyWithRoleChange;
import main.research.agent.strategy.ProposedStrategy.ProposedStrategy_m;
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
    static List<Agent> agentList = new ArrayList<>();
    static LeaderStrategyWithRoleChange ls = new ProposedStrategy_l();
    static MemberStrategyWithRoleChange ms = new ProposedStrategy_m();

    static {
        System.out.println("AgentTest");
    }

    @BeforeAll
    static void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        MyRandom.setNewSfmt(0);
        AgentManager am = new AgentManager();

        Method ga = AgentManager.class.getDeclaredMethod("generateAgents", LeaderStrategyWithRoleChange.class, MemberStrategyWithRoleChange.class);
        ga.setAccessible(true);
        agentList = (List<Agent>) ga.invoke( am, ls, ms );

        Field field = am.getClass().getDeclaredField("agents");
        field.setAccessible(true);
        field.set(am, agentList);

        Method dl = AgentManager.class.getDeclaredMethod("deployAgents");
        dl.setAccessible(true);
        dl.invoke( am );
    }


    @AfterAll
    static void tearDown() {
        AgentManager.clear();
        Agent.clear();
        Grid.clear();
    }
}