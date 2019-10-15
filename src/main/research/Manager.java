package main.research;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.agent.strategy.Strategy;

import static main.research.OutPut.*;
import static main.research.others.random.MyRandom.*;

import main.research.communication.TransmissionPath;
import main.research.grid.Grid;
import main.research.task.Task;
import main.research.task.TaskManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.time.LocalDateTime;

import static main.research.SetParam.Role.*;

public class Manager implements SetParam {
	private static final String INIT_FILE_PATH = "/resource/common.json";
	private static ObjectMapper mapper = new ObjectMapper();
	private static JsonNode jsonNode;
	private static JsonNode environmentalNode;
	private static JsonNode resultTypeNode;

	static final int executionTimes_;
	public static final int max_turn_;
	static final int writing_times_;
	static final int bin_;

	//TODO: こんな風にするならsingletonにしたほうがいいよね
	// TODO: lsとmsで分けて指定しなきゃいけないの無駄じゃない?
//	private static String package_name = "main.research.agent.strategy.reliableAgents.";
	//    private static String package_name = "main.research.agent.strategy.ComparativeStrategy1.";
//    private static String package_name = "main.research.agent.strategy.ComparativeStrategy2.";
	private static String package_name = "main.research.agent.strategy.puttingDeOcAndDelayIntoOneDimensionalValue.";
	private static String ls_name = "LeaderStrategy";      // ICA2018における提案手法役割更新あり    //    private static main.research.strategy.Strategy strategy = new ProposedMethodForSingapore();
	private static String ms_name = "MemberStrategy";

	static {
		try {
			jsonNode = mapper.readTree( new File( System.getProperty( "user.dir" ) + INIT_FILE_PATH ) );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		environmentalNode = jsonNode.get( "environment" );
		resultTypeNode    = jsonNode.get( "result_type" );
		AgentManager.setConstants( jsonNode.get( "agents" ) );
		TaskManager.setConstants( jsonNode.get( "tasks" ) );

		executionTimes_ = environmentalNode.get( "execution_times" ).asInt();
		max_turn_ = environmentalNode.get( "max_turn" ).asInt();
		writing_times_ = environmentalNode.get( "writing_times" ).asInt();
		bin_ = max_turn_ / writing_times_;
	}

	private static int turn;
	public static void main( String[] args ) {
		int num = 0;
		String strategy_name = package_name.split( "\\." )[ 4 ];
		System.out.println( strategy_name );
		System.out.println( strategy_name + ", λ=" + TaskManager.getAdditional_tasks_num_() +
			", From " + LocalDateTime.now()
		);

		// num回実験
		while ( true ) {
			initiate( num++ );                         // シード，タスク，エージェントの初期化処理

			// ターンの進行
			for ( turn = 1; turn <= max_turn_; turn++ ) {
				TaskManager.addNewTasksToQueue();
				AgentManager.JoneDoesSelectRole();
				TransmissionPath.transmit();                // 通信遅延あり
				AgentManager.actLeadersAndMembers();

				if ( turn % bin_ == 0 && resultTypeNode.get( "check_data" ).asBoolean() ) {
					aggregateAgentData( AgentManager.getAllAgentList() );
					int rmNum = Agent.countReciprocalMember( AgentManager.getAllAgentList() );
					aggregateData( TaskManager.getFinishedTasks(), TaskManager.getDisposedTasks(), TaskManager.getOverflowTasks(), rmNum, AgentManager.getAllAgentList() );
					TaskManager.reset();
				}

				// ここが1tickの最後の部分．次のtickまでにやることあったらここで．
			}
			// ↑ 一施行のカッコ．以下は次の施行までの合間で作業する部分
			int leader_num = ( int ) AgentManager.getAllAgentList().stream()
				.filter( agent -> agent.role == LEADER )
				.count();
			int member_num = ( int ) AgentManager.getAllAgentList().stream()
				.filter( agent -> agent.role == MEMBER )
				.count();
			System.out.println( "leaders:" + leader_num + ", members:" + member_num );

			// remove
			// 信頼エージェントについて
			System.out.println( "waiting: " + main.research.agent.strategy.reliableAgents.MemberStrategy.waiting );
			System.out.println( "tired of waiting: " + main.research.agent.strategy.MemberStrategyWithRoleChange.tired_of_waiting );
			System.out.println( "average de from member to leader: " + main.research.agent.strategy.reliableAgents.MemberStrategy.calculateMeanDE() );
			System.out.println( "reciprocal members: " + main.research.agent.strategy.reliableAgents.MemberStrategy.countReciprocalMembers() );
			showGrowApartDegree();

			System.out.println( "---------------------------------------------------------------------------------" );
			if ( num == executionTimes_ ) break;
			clearAll();
		}
		// ↑ 全実験の終了のカッコ．以下は後処理
		if ( resultTypeNode.get( "check_data" ).asBoolean() )       writeResults( strategy_name );
		if ( resultTypeNode.get( "check_relationships" ).asBoolean() ) writeGraphInformationX( AgentManager.getAllAgentList(), strategy_name, jsonNode.get( "others" ) );
		AgentManager.getAllAgentList().stream()
			.filter( ag -> ag.id < 10 )
			.forEach( ag -> System.out.println("id: " + ag.id + ", role: " + ag.role) );
//		showRelationsBetweenOCandDE( AgentManager.getAllAgentList() );
	}

	// 環境の準備に使うメソッド
	private static void initiate( int times ) {
		setNewSfmt( times );
		setNewRnd( times );
		TaskManager.addInitialTasksToQueue( );
		AgentManager.initiateAgents( package_name, ls_name, ms_name );
	}

	// 現在のターン数を返すメソッド
	public static int getCurrentTime() {
		return turn;
	}

	private static void clearAll() {
		TransmissionPath.clear();
		TaskManager.clear();
		Task.clear();
		Agent.clear();
		Strategy.clear();
		Grid.clear();
		AgentManager.clear();
	}
}
