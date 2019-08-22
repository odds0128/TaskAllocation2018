package main.research.util;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.grid.Grid;
import main.research.others.random.MyRandom;

import java.util.*;

public class Initiation {
	private static String package_name = "main.research.agent.strategy.ProposedStrategy.";
	private static String ls_name = "ProposedStrategy_l";      // ICA2018における提案手法役割更新あり    //    private static main.research.strategy.Strategy strategy = new ProposedMethodForSingapore();
	private static String ms_name = "ProposedStrategy_m";

	private static Agent[][] grid ;

	static {
		MyRandom.setNewRnd( 0 );
		AgentManager.initiateAgents( package_name, ls_name, ms_name );
		grid = Grid.getGrid();
	}

	public static List< Agent > getNewAgentList() {
		return AgentManager.getAllAgentList();
	}

	public static Agent[][] getGrid () {
		return grid;
	}
}
