package main.research.grid;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.random.MyRandom;
import main.research.strategy.LeaderStrategy;
import main.research.strategy.MemberStrategy;
import main.research.strategy.ProposedStrategy.LeaderProposedStrategy;
import main.research.strategy.ProposedStrategy.MemberProposedStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.*;

import static main.research.SetParam.AGENT_NUM;
import static org.hamcrest.MatcherAssert.assertThat;

class GridTest {
    private LeaderStrategy ls;
    private MemberStrategy ms;

    @BeforeEach
    void setUp() {
        ls = new LeaderProposedStrategy();
        ms = new MemberProposedStrategy();
        MyRandom.newSfmt(0);
    }

    @Nested
    class setAgentOnEnvironmentのテスト {
        @BeforeEach
        void setUp() {
            AgentManager.initiateAgents(ls, ms);
        }

        @Test
        void AGENT_NUM体のエージェントがみんな別の場所に配置される(){
            Agent[][] grid = Grid.getGrid();
            int actual = 0;
            int vacuous = 0;
            int expected = AGENT_NUM;
           for( Agent[] row : grid ) {
               for( Agent item : row ) {
                   if( item instanceof Agent ) {
                       actual++;
                   }
                   if( item == null ) {
                       vacuous++;
                   }
               }
           }
            System.out.println(vacuous);
           assertThat( actual, is(expected) );
        }
    }

}