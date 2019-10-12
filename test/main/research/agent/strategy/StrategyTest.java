package main.research.agent.strategy;

import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.AgentManager;
import main.research.grid.Grid;
import main.research.util.Initiation;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import static main.research.SetParam.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

@Tag("strategy")
class StrategyTest implements Strategy {
    static {
        System.out.println("StrategyTest");
    }

    static List<Agent> agentList;

    @BeforeAll
    static void setUp() {
        agentList = Initiation.getNewAgentList();
    }

    @Nested
    class evaporateDEのテスト {
        Agent sample;
        Random rnd;

        @BeforeEach
        void setUp() {
            rnd = new Random();
            int index = rnd.nextInt( agent_num_ );
            sample = agentList.get(index);
        }

        @Test
        void evaporateDEを一回呼び出した結果全エージェントの全DEが初期値からγを引いたものになる() {
            Double expected_l = INITIAL_VALUE_OF_DE - γ_r;
            Double expected_m = INITIAL_VALUE_OF_DE - γ_r;

            evaporateDE(sample.ls.reliableMembersRanking);
            evaporateDE(sample.ms.reliableLeadersRanking);

            sample.ls.reliableMembersRanking.forEach(
                    pair -> assertThat( pair.getDe(), is( expected_l ) )
            );
            sample.ms.reliableLeadersRanking.forEach(
                    pair -> assertThat( pair.getDe(), is( expected_m ) )
            );
        }

        // TODO: ここにあんの完全におかしい．もう一個下に移す
        @Test
        void DEが0未満のエージェントがいない() throws IllegalAccessException, NoSuchFieldException {
            // 適当なやつのDEをいじってγ未満にする
            int index = rnd.nextInt( agent_num_ - 1 );
            AgentDePair targetPair = sample.ls.reliableMembersRanking.get( index );

            Class xClass    = AgentDePair.class;
            Field xNumField = xClass.getDeclaredField("de");
            xNumField.setAccessible(true);
            xNumField.setDouble( targetPair, γ_r / 2.0 ); // 次の蒸発で0未満になるような値をセット

            evaporateDE(sample.ls.reliableMembersRanking);

            for ( AgentDePair pair : sample.ls.reliableMembersRanking ) {
                Matcher<Double> greaterThanOrEqualToZero = greaterThanOrEqualTo( 0.0);
                assertThat( pair.getDe(), is(greaterThanOrEqualToZero) );
            }
        }
    }

    @AfterAll
    static void tearDown() {
        AgentManager.clear();
        Agent.clear();
        Grid.clear();
    }

}