package main.research.strategy;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.grid.Grid;
import main.research.random.MyRandom;
import main.research.strategy.ProposedStrategy.LeaderProposedStrategy;
import main.research.strategy.ProposedStrategy.MemberProposedStrategy;
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
class StrategyTest implements Strategy{
    static LeaderStrategy ls = new LeaderProposedStrategy();
    static MemberStrategy ms = new MemberProposedStrategy();
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

            evaporateDE(sample.relRanking_l);
            evaporateDE(sample.relRanking_m);

            sample.relRanking_l.forEach(
                    (key, value) -> assertThat( value, is( expected_l ) )
            );
            sample.relRanking_m.forEach(
                    (key, value) -> assertThat( value, is( expected_m ) )
            );
        }

        @Test
        void DEが0未満のエージェントがいない(){
            // 適当なやつのDEをいじってγ未満にする
            int index = rnd.nextInt(AGENT_NUM - 1 );
            Agent unreliable = null;
            Iterator<Agent> iterator = sample.relRanking_l.keySet().iterator();
            for( int i = 0; i <= index; i++ ) {
                unreliable = iterator.next();
            }
            sample.relRanking_l.replace( unreliable, (γ_r / 2.0) );
            evaporateDE(sample.relRanking_l);

            sample.relRanking_l.forEach(
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
            Iterator<Agent> iterator = sample.relRanking_l.keySet().iterator();
            for( int i = 0; i <= index; i++ ) {
                reliable = iterator.next();
            }
            sample.relRanking_l.replace( reliable, INITIAL_VALUE_OF_DE * 10.0 );
                sample.relRanking_l = sortReliabilityRanking( sample.relRanking_l );
            Agent top = sample.relRanking_l.keySet().iterator().next();
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