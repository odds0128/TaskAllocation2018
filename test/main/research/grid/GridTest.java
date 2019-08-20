package main.research.grid;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.others.random.MyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;

import static main.research.SetParam.AGENT_NUM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

class GridTest {
    static {
        System.out.println( "GridTest start" );
    }
    private static String ls_name = "ProposedStrategy_l";      // ICA2018における提案手法役割更新あり    //    private static main.research.strategy.Strategy strategy = new ProposedMethodForSingapore();
    private static String ms_name = "ProposedStrategy_m";
    static List<Agent> agentList;



    @BeforeEach
    void setUp() {
        MyRandom.setNewSfmt(0);
        AgentManager.initiateAgents(ls_name, ms_name);
        agentList = AgentManager.getAllAgentList();
    }

    @Nested
    class setAgentOnEnvironmentのテスト {

        @Test
        void AGENT_NUM体のエージェントがみんな別の場所に配置される(){
            Agent[][] grid = Grid.getGrid();
            System.out.println(grid);
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