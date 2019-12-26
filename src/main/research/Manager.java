package main.research;

import main.research.agent.Agent;
import main.research.agent.AgentManager;

import static main.research.OutPut.*;
import static main.research.others.random.MyRandom.*;

import main.research.agent.strategy.MemberTemplateStrategy;
import main.research.communication.TransmissionPath;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.graph.GraphAtAnWindow;
import main.research.grid.Grid;
import main.research.task.Task;
import main.research.task.TaskManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static main.research.Parameter.Role.*;

public class Manager implements Parameter {
	private static final String INIT_FILE_PATH = "/resource/common.json";
	private static ObjectMapper mapper = new ObjectMapper();
	private static JsonNode jsonNode;
	private static JsonNode environmentalNode;
	private static JsonNode resultTypeNode;

	static final int executionTimes_;
	public static final int max_turn_;
	static final int writing_times_;
	public static final int bin_;

	//TODO: こんな風にするならsingletonにしたほうがいいよね
	// TODO: lsとmsで分けて指定しなきゃいけないの無駄じゃない?
	private static String package_name = "main.research.agent.strategy.reliableAgents.";
//	private static String package_name = "main.research.agent.strategy.puttingDeOcAndDelayIntoOneDimensionalValue.";
//	private static String package_name = "main.research.agent.strategy.putRewardAndDelayIntoDeCalculation.";
//	private static String package_name = "main.research.agent.strategy.learningOnlyTeamingSuccessRate.";
	private static String ls_name = "LeaderStrategy";      // ICA2018における提案手法役割更新あり    //    private static main.research.strategy.Strategy strategy = new ProposedMethodForSingapore();
	private static String ms_name = "MemberStrategy";

	static {
		System.out.println(System.getProperty( "user.dir" ));
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

//			Stdout.checkGrid( Grid.getGrid() );
			if ( resultTypeNode.get( "check_network" ).asBoolean() ) {
				OutPut.makeGraphDirectory();
			}
			GraphAtAnWindow graph = null ;

			// ターンの進行
			for ( turn = 1; turn <= max_turn_; turn++ ) {

				TaskManager.addNewTasksToQueue();
				AgentManager.JoneDoesSelectRole();

				if ( turn % bin_ == 0 ) {
					if( resultTypeNode.get( "check_data" ).asBoolean()  ) {
//						aggregateData( AgentManager.getAllAgentList(), strategy_name );
						aggregateData( AgentManager.getAllAgentList() );
						TaskManager.forget();
					}
					if( resultTypeNode.get( "check_network" ).asBoolean() ) {
						OutPut.writeNodeInformationAsCSV( turn, AgentManager.getAllAgentList() );
						OutPut.writeGraphInformationAsCSV( turn, graph );
					}
				}

				if ( turn % bin_ == 1 && resultTypeNode.get( "check_network" ).asBoolean() ) {
					graph = new GraphAtAnWindow();
					ResultOfTeamFormation.setGraph( graph );
				}

				TransmissionPath.transmit();                // 通信遅延あり
				AgentManager.actLeadersAndMembers();

				// ここが1tickの最後の部分．次のtickまでにやることあったらここで．
			}

			// ↑ 一施行のカッコ．以下は次の施行までの合間で作業する部分
			int leader_num = ( int ) AgentManager.getAllAgentList().stream()
				.filter( agent -> agent.role == LEADER )
				.count();

			List<Agent> members = AgentManager.getAllAgentList().stream()
				.filter( agent -> agent.role == MEMBER )
				.collect( Collectors.toList());
			System.out.println( "leaders:" + leader_num + ", members:" + members.size() );

			// remove
			// 信頼エージェントについて
			System.out.println( "waiting: " + main.research.agent.strategy.reliableAgents.MemberStrategy.waiting );
			System.out.println( "tired of waiting: " + MemberTemplateStrategy.tired_of_waiting );
			System.out.println( "average de from member to leader: " + main.research.agent.strategy.reliableAgents.MemberStrategy.calculateMeanDE() );
			System.out.println( "reciprocal members: " + main.research.agent.strategy.reliableAgents.MemberStrategy.countReciprocalMembers() );
//			TransmissionPath.showMessages();

//			writeSolicitationsReached( strategy_name );
//			writeSubtasksHoldenByMembers( strategy_name );
			System.out.println( "---------------------------------------------------------------------------------" );
//			TransmissionPath.showMessages();
			clearAll();
			if ( num == executionTimes_ ) break;
		}
		// ↑ 全実験の終了のカッコ．以下は後処理
		if ( resultTypeNode.get( "check_data" ).asBoolean() )  {
//			writeLeadersExecutionNum( strategy_name );
			writeMainResultData( strategy_name );
		}
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
		Grid.clear();
		AgentManager.clear();
	}
}
