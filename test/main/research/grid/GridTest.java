package main.research.grid;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.util.Initiation;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;

import static main.research.SetParam.AGENT_NUM;
import static org.hamcrest.MatcherAssert.assertThat;

class GridTest {
    static {
        System.out.println( "GridTest start" );
    }

    @Nested
    class setAgentOnEnvironmentのテスト {
        List<Agent> agentList;

        @BeforeEach
        void setUp() {
        	agentList = Initiation.getNewAgentList();
        }

        @Test
        void AGENT_NUM体のエージェントがみんな別の場所に配置される(){
            Agent[][] grid = Initiation.getGrid();
            int actual = 0;
            int expected = AGENT_NUM;

            for( Agent[] row : grid ) {
                for( Agent item : row ) {
                    if( item instanceof Agent ) {
                        actual++;
                    }
                }
            }
            assertThat( actual, is(expected) );
        }

        @AfterEach
        void tearDown() {
            AgentManager.clear();
            Agent.clear();
            Grid.clear();
        }
    }

}