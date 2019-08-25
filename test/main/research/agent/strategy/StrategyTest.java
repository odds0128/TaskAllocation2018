package main.research.agent.strategy;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.grid.Grid;
import main.research.util.Initiation;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.*;

import java.util.Iterator;
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
            int index = rnd.nextInt(AGENT_NUM);
            sample = agentList.get(index);
        }

        @Test
        void evaporateDEを一回呼び出した結果全エージェントの全DEが初期値からγを引いたものになる() {
            Double expected_l = INITIAL_VALUE_OF_DE - γ_r;
            Double expected_m = INITIAL_VALUE_OF_DE - γ_r;

            evaporateDE(sample.ls.reliableMembersRanking);
            evaporateDE(sample.ms.reliableLeadersRanking);

            sample.ls.reliableMembersRanking.forEach(
                    (key, value) -> assertThat( value, is( expected_l ) )
            );
            sample.ms.reliableLeadersRanking.forEach(
                    (key, value) -> assertThat( value, is( expected_m ) )
            );
        }

        @Test
        void DEが0未満のエージェントがいない(){
            // 適当なやつのDEをいじってγ未満にする
            int index = rnd.nextInt(AGENT_NUM - 1 );
            Agent unreliable = null;
            Iterator<Agent> iterator = sample.ls.reliableMembersRanking.keySet().iterator();
            for( int i = 0; i <= index; i++ ) {
                unreliable = iterator.next();
            }
            sample.ls.reliableMembersRanking.replace( unreliable, (γ_r / 2.0) );
            evaporateDE(sample.ls.reliableMembersRanking);

            sample.ls.reliableMembersRanking.forEach(
                    (key, value) -> {
                        double temp = value;
                        Matcher<Double> greaterThanOrEqualToZero = greaterThanOrEqualTo( 0.0);
                        assertThat( temp, is(greaterThanOrEqualToZero) );
                    }
            );
        }
    }

    @Nested
    class sortReliabilityRankingのテスト {
        Agent sample;
        Random rnd;

        @BeforeEach
        void setUp() {
            rnd = new Random();
            int index = rnd.nextInt(AGENT_NUM);
            sample = agentList.get(index);
        }

        @Test
        void sortReliabilityRankingで降順に並び替えられる() {
            // 適当なやつのDEを大きくする
            int index = rnd.nextInt(AGENT_NUM - 1 );
            Agent reliable = null;
            Iterator<Agent> iterator = sample.ls.reliableMembersRanking.keySet().iterator();
            for( int i = 0; i <= index; i++ ) {
                reliable = iterator.next();
            }
            sample.ls.reliableMembersRanking.replace( reliable, INITIAL_VALUE_OF_DE * 10.0 );
                sample.ls.reliableMembersRanking = Strategy.getSortReliabilityRanking( sample.ls.reliableMembersRanking);
            Agent top = sample.ls.reliableMembersRanking.keySet().iterator().next();
            assertThat( top, is(reliable) );
        }
    }


    @AfterAll
    static void tearDown() {
        AgentManager.clear();
        Agent.clear();
        Grid.clear();
    }

}