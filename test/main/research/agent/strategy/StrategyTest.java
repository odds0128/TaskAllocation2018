package main.research.agent.strategy;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.grid.Grid;
import main.research.random.MyRandom;
import main.research.agent.strategy.ProposedStrategy.ProposedStrategy_l;
import main.research.agent.strategy.ProposedStrategy.ProposedStrategy_m;
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
    static LeaderStrategy ls = new ProposedStrategy_l();
    static MemberStrategy ms = new ProposedStrategy_m();
    static List<Agent> agentList;

    static {
        System.out.println("StrategyTest");
    }

    @BeforeAll
    static void setUp() {
        MyRandom.newSfmt(0);
        AgentManager.initiateAgents(ls, ms);
        agentList = AgentManager.getAgentList();
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

            evaporateDE(sample.reliabilityRankingAsL);
            evaporateDE(sample.reliabilityRankingAsM);

            sample.reliabilityRankingAsL.forEach(
                    (key, value) -> assertThat( value, is( expected_l ) )
            );
            sample.reliabilityRankingAsM.forEach(
                    (key, value) -> assertThat( value, is( expected_m ) )
            );
        }

        @Test
        void DEが0未満のエージェントがいない(){
            // 適当なやつのDEをいじってγ未満にする
            int index = rnd.nextInt(AGENT_NUM - 1 );
            Agent unreliable = null;
            Iterator<Agent> iterator = sample.reliabilityRankingAsL.keySet().iterator();
            for( int i = 0; i <= index; i++ ) {
                unreliable = iterator.next();
            }
            sample.reliabilityRankingAsL.replace( unreliable, (γ_r / 2.0) );
            evaporateDE(sample.reliabilityRankingAsL);

            sample.reliabilityRankingAsL.forEach(
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
            Iterator<Agent> iterator = sample.reliabilityRankingAsL.keySet().iterator();
            for( int i = 0; i <= index; i++ ) {
                reliable = iterator.next();
            }
            sample.reliabilityRankingAsL.replace( reliable, INITIAL_VALUE_OF_DE * 10.0 );
                sample.reliabilityRankingAsL = sortReliabilityRanking( sample.reliabilityRankingAsL);
            Agent top = sample.reliabilityRankingAsL.keySet().iterator().next();
            assertThat( top, is(reliable) );
        }
    }


    // Strategyインタフェースのテストのためにかりそめの実装をする {
    @Override
    public void checkMessages(Agent self) {}

    @AfterAll
    static void tearDown() {
        AgentManager.clear();
        Agent.clear();
        Grid.clear();
    }

}