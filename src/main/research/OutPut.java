package main.research;

import main.research.agent.AgentManager;
import main.research.agent.strategy.MemberTemplateStrategy;
import main.research.agent.strategy.OstensibleCapacity;
import main.research.agent.strategy.TemplateStrategy;
import main.research.agent.strategy.reciprocal_agents.LeaderStrategy;
import main.research.agent.strategy.reciprocal_agents.MemberStrategy;
import main.research.graph.Edge;
import main.research.task.TaskManager;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import main.research.agent.Agent;
import main.research.communication.TransmissionPath;
import main.research.graph.GraphAtAnWindow;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static main.research.Parameter.Role.LEADER;
import static main.research.Parameter.Role.MEMBER;

/**
 * OutPutクラス
 * Singletonで実装
 * 結果の画面出力とファイル出力を管理
 */
public class OutPut implements Parameter {
	private static int executionTimes_ = Manager.executionTimes_;
	private static int writing_times_ = Manager.writing_times_;
	private static int max_turn_ = Manager.max_turn_;
	private static int agent_num_ = AgentManager.agent_num_;

	private static OutPut _singleton = new OutPut();
	static String outputDirectoryPath = System.getProperty( "user.dir" ) + "/out/";

	static int index = 0;

	static int[] finishedTasksArray = new int[ writing_times_ ];
	static int[] disposedTasksArray = new int[ writing_times_ ];
	static int[] overflownTasksArray = new int[ writing_times_ ];
	static int[] messagesArray = new int[ writing_times_ ];
	static double[] communicationDelayArray = new double[ writing_times_ ];
	static int[] leaderNumArray = new int[ writing_times_ ];
	static int[] neetMembersArray = new int[ writing_times_ ];
	static int[] reciprocalLeaderArray = new int[ writing_times_ ];
	static int[] rationalistsArray = new int[ writing_times_ ];

	static int[] leadersDisposeTask = new int[ writing_times_ ];
	static int[] leadersExecuteTask = new int[ writing_times_ ];

	static double[] membersHaveNoSubtask = new double[ writing_times_ ];
	static double[] averageSubtaskQueueSizeForAllMembers = new double[ writing_times_ ];
	static double[] averageSubtaskQueueSizeForWorkingMembers = new double[ writing_times_ ];

	static int[] membersTrustedByLeaderArray = new int[ writing_times_ ];
	static double[] growApartDegreeArray = new double[ writing_times_ ];

	static int[] reciprocalMembersArray = new int[ writing_times_ ];
	static double[] idleMembersRateArray = new double[ writing_times_ ];
	static int[] finishedTasksInDepopulatedAreaArray = new int[ writing_times_ ];
	static int[] finishedTasksInPopulatedAreaArray = new int[ writing_times_ ];
	static int[] tempSubtaskExecutionTimeArray = new int[ writing_times_ ];
	static double[] taskExecutionTimeArray = new double[ writing_times_ ];
	static int subtaskExecutionCount = 0;

	static void aggregateData( List< Agent > agents, String strategy_name ) {
		neetMembersArray[ index ] += Agent.countNEETmembers( agents, max_turn_ / writing_times_ );
		leaderNumArray[ index ] += countLeader( agents );
		finishedTasksArray[ index ] += TaskManager.getFinishedTasks();
		tempMessagesArray[ index ] = TransmissionPath.getMessageNum();

		leadersDisposeTask[ index ] += TaskManager.badLeaders.size();
		leadersExecuteTask[ index ] += TaskManager.goodLeaders.size();

//		TaskManager.clearLeadersInfo();

		membersTrustedByLeaderArray[ index ] += countMembersTrustedByLeader();

		List< Agent > memberList = agents.stream().filter( ag -> ag.role == MEMBER ).collect( Collectors.toList() );
		int allMemberNum = ( int ) memberList.stream().count();
		int allSubtasksHolden = memberList.stream().mapToInt( ag -> ag.ms.mySubtaskQueue.size() ).sum();
		int tempMembersHaveNoSubtask = ( int ) memberList.stream().filter( ag -> ag.ms.mySubtaskQueue.size() == 0 ).count();
		membersHaveNoSubtask[ index ] += tempMembersHaveNoSubtask;
		averageSubtaskQueueSizeForAllMembers[ index ] += ( double ) allSubtasksHolden / allMemberNum;
		averageSubtaskQueueSizeForWorkingMembers[ index ] += ( double ) allSubtasksHolden / ( allMemberNum - tempMembersHaveNoSubtask );

		int gap = index > 0 ? tempMessagesArray[ index - 1 ] : 0;
		messagesArray[ index ] += TransmissionPath.getMessageNum() - gap;
		communicationDelayArray[ index ] += TransmissionPath.getAverageCommunicationTime();
		disposedTasksArray[ index ] += TaskManager.getDisposedTasks();
		overflownTasksArray[ index ] += TaskManager.getOverflowTasks();

		// なんか知らんけどここの有無で結果が変わるようなので注意したほうがいいかも
		if ( strategy_name.equals( "reliable_agents" ) || strategy_name.equals( "reciprocal_agents" ) ) {
			reciprocalMembersArray[ index ] += agents.stream()
				.filter( ag -> ag.role == MEMBER )
				.filter( m -> {
					MemberStrategy ms = ( MemberStrategy ) m.ms;
					return ms.principle == Principle.RECIPROCAL;
				} )
				.count();
			reciprocalLeaderArray[ index ] += agents.stream()
				.filter( ag -> ag.role == LEADER )
				.filter( l -> {
					LeaderStrategy ls = ( LeaderStrategy ) l.ls;
					return ls.principle == Principle.RECIPROCAL;
				} )
				.count();
		}
		if ( index == writing_times_ - 1 ) {
			tempMessagesArray = new int[ writing_times_ ];
		}
		idleMembersRateArray[ index ] += ( double ) MemberTemplateStrategy.idleTime / AgentManager.countMembers();
		MemberTemplateStrategy.idleTime = 0;
		indexIncrement();
	}

	private static int countMembersTrustedByLeader() {
		Set< Agent > trustedMemberSet = new HashSet<>();
		for ( Agent ag: AgentManager.getAllAgentList() ) {
			for ( TemplateStrategy.Dependability pair: ag.ls.dependabilityRanking ) {
				if ( pair.getValue() > LeaderStrategy.de_threshold_ ) {
					trustedMemberSet.add( pair.getAgent() );
				}
			}
		}
		return trustedMemberSet.size();
	}

	static final double de_threshold = 0.7;
	static void checkMutualRelation( List<Agent> agentList ) {
		List<Agent> leaders = agentList.stream()
			.filter( ag -> ag.role == Role.LEADER )
			.collect( Collectors.toList());
		List<Agent> members = agentList.stream()
			.filter( ag -> ag.role == Role.MEMBER )
			.collect( Collectors.toList());

	}

	static double ratioOfMutualRelationOf( Agent ag ) {
		int one_wayCount = 0;
		int mutualCount = 0;

		if( ag.role == Role.LEADER ) {
			for ( TemplateStrategy.Dependability d: ag.ls.dependabilityRanking ) {
				// leaderがd.agentを信頼している
				if ( d.getValue() > de_threshold ) {
					one_wayCount++;
					// leaderがd.agentに信頼されている
					if ( d.getAgent().ms.getDeByAgent( ag, d.getAgent().ms.dependabilityRanking ).getValue() > de_threshold ) {
						mutualCount++;
					}
				} else {
					break;
				}
			}
		} else {
			for ( TemplateStrategy.Dependability d: ag.ms.dependabilityRanking ) {
				// memberがd.agentを信頼している
				if ( d.getValue() > de_threshold ) {
					one_wayCount++;
					// memberがd.agentに信頼されている
					if ( d.getAgent().ls.getDeByAgent( ag, d.getAgent().ls.dependabilityRanking ).getValue() > de_threshold ) {
						mutualCount++;
					}
				} else {
					break;
				}
			}
		}
		return (double) mutualCount/one_wayCount;
	}

	public static void sumExecutionTime( int time ) {
		subtaskExecutionCount++;
		tempSubtaskExecutionTimeArray[ index ] += time;
	}

	private static int countLeader( List< Agent > agentList ) {
		return ( int ) agentList.stream()
			.filter( agent -> agent.role == LEADER )
			.count();
	}

	static int[] tempMessagesArray = new int[ writing_times_ ];

	private static void indexIncrement() {
		if ( subtaskExecutionCount != 0 ) {
			taskExecutionTimeArray[ index ] += ( double ) tempSubtaskExecutionTimeArray[ index ] / subtaskExecutionCount;
		}
		tempSubtaskExecutionTimeArray[ index ] = 0;
		subtaskExecutionCount = 0;
		index = ( index + 1 ) % writing_times_;
	}

	static void writeMainResultData( String st ) {
		System.out.println( "called" );
		String outputFilePath = _singleton.setPath( "results", st, "csv" );
		System.out.println( "writing now" );
		FileWriter fw;
		BufferedWriter bw;
		PrintWriter pw;

		try {
			fw = new FileWriter( outputFilePath, false );
			bw = new BufferedWriter( fw );
			pw = new PrintWriter( bw );

			pw.println( "turn" + ","
					+ "FinishedTasks" + "," + "DisposedTasks" + "," + "OverflownTasks" + ","
					+ "Success rate(except overflow)" + "," + "Success rate" + ","
					+ "CommunicationTime" + "," + "Messages" + "," + "ExecutionTime" + ","
					+ "Leaders Num" + "," // + "Member"                            + ","
//					+ "Sabotage leaders" + "," + "Hard-work leaders" + ","
					+ "Sabotage members" + "," + "average subtasks holden for all members" + "," + "average subtasks holden for working members" + "," + "Trusted members" + ","
//					+ "Grow apart ratio" + ","
					+ "NEET Members" + ","
					// + "Lonely leaders"                    + "," + "Lonely members"                    + ","
					// + "Accompanied leaders"               + "," + "Accompanied members"               + ","
					+ "ReciprocalLeaders" + "," + "ReciprocalMembers" + ","
					+ "IdleTime" + ","
				// + "Rational"                          + "," + "ReciprocalMembers" + ","
				// + "FinishedTasks in depopulated area" + "," + "FinishedTasks in populated area"   + ","
			);
			for ( int i = 0; i < writing_times_; i++ ) {
				pw.println( ( i + 1 ) * ( max_turn_ / writing_times_ ) + ","
						+ finishedTasksArray[ i ] / executionTimes_ + ","
						+ disposedTasksArray[ i ] / executionTimes_ + ","
						+ overflownTasksArray[ i ] / executionTimes_ + ","
						+ ( double ) finishedTasksArray[ i ] / ( finishedTasksArray[ i ] + disposedTasksArray[ i ] ) + ","
						+ ( double ) finishedTasksArray[ i ] / ( finishedTasksArray[ i ] + disposedTasksArray[ i ] + overflownTasksArray[ i ] ) + ","
						+ communicationDelayArray[ i ] / executionTimes_ + ","
						+ ( double ) messagesArray[ i ] / executionTimes_ + ","
						+ taskExecutionTimeArray[ i ] / executionTimes_ + ","
						+ ( double ) leaderNumArray[ i ] / executionTimes_ + ","
//					+ leadersDisposeTask[i] / executionTimes_ + ","
//					+ leadersExecuteTask[i] / executionTimes_ + ","
						+ ( int ) membersHaveNoSubtask[ i ] / executionTimes_ + ","
						+ averageSubtaskQueueSizeForAllMembers[ i ] / executionTimes_ + ","
						+ averageSubtaskQueueSizeForWorkingMembers[ i ] / executionTimes_ + ","
						+ membersTrustedByLeaderArray[ i ] / executionTimes_ + ","
//					+ growApartDegreeArray[i] / executionTimes_ + ","
						+ ( double ) neetMembersArray[ i ] / executionTimes_ + ","
						+ ( double ) reciprocalLeaderArray[ i ] / executionTimes_ + ","
						+ ( double ) reciprocalMembersArray[ i ] / executionTimes_ + ","
						+ idleMembersRateArray[ i ] / executionTimes_ + ","
				);
			}
			pw.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	static void writeLeadersExecutionNum( String strategy_name ) {
		String path = "results/workingLeadersOf" + strategy_name;
		PrintWriter pw = newCSVPrintWriter( path );

		pw.println( "id,role,finished" );
		for ( Map.Entry< Agent, Integer > item: TaskManager.goodLeaders.entrySet() ) {
			Role role = item.getKey().e_leader > item.getKey().e_member ? LEADER : MEMBER;
			pw.println( item.getKey().id + "," + role + "," + item.getValue() );
		}
		pw.close();
	}

	static void writeInformationAsMember( String strategy_name ) {
		String path = "results/informationAsMember_" + strategy_name;
		PrintWriter pw = newCSVPrintWriter( path );

		pw.println( "id,role,excellence," +
			"holding_subtasks,solicitations_received,subtask_executions" );
		for ( Agent ag: AgentManager.getAllAgentList() ) {
			Role role = ag.e_leader > ag.e_member ? LEADER : MEMBER;
			pw.println( ag.id + "," + role + "," + getExcellence( ag ) + ","
				+ ag.ms.mySubtaskQueue.size() + "," + TransmissionPath.solicitToAgents[ag.id] + "," + ag.ms.subtaskExecution );
		}
		pw.close();
	}

	private static double getExcellence( Agent ag ) {
		int ret = 0;
		int notZero = 0;
		for ( int res: ag.resources ) {
			if ( res > 0 ) notZero++;
			ret += res;
		}
		return ( double ) ret / notZero;
	}

	static void writeDelays( int[][] delays ) {
		String path = "results/communicationDelay=" + MAX_DELAY;
		PrintWriter pw = newCSVPrintWriter( path );

		pw.print( "id" );
		for ( int i = 0; i < agent_num_; i++ ) pw.print( "," + i );
		pw.println( "," );
		for ( int i = 0; i < agent_num_; i++ ) {
			pw.print( i + "," );
			for ( int j = 0; j < agent_num_; j++ ) {
				pw.print( delays[ i ][ j ] + "," );
			}
			pw.println();
		}
		pw.close();
	}

	static PrintWriter newCSVPrintWriter( String path ) {
		try {
			Date date = new Date();
			SimpleDateFormat sdf1 = new SimpleDateFormat( ",yyyy:MM:dd,HH:mm:ss" );
			FileWriter fw = new FileWriter( outputDirectoryPath + path + sdf1.format( date ) + ".csv", false );
			BufferedWriter bw = new BufferedWriter( fw );
			return new PrintWriter( bw );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		return null;
	}

	static String graphDirectoryName;

	static void makeGraphDirectory() {
		// 出力先であるgraphディレクトリが無かったら作る
		File f = new File( outputDirectoryPath + "graph" );
		if ( !f.exists() ) f.mkdir();

		// 一意に区別できるように「日付+時刻」を名前とするディレクトリを作る
		Date date = new Date();
		SimpleDateFormat sdf1 = new SimpleDateFormat( "yyyy:MM:dd,HH:mm:ss" );
		graphDirectoryName = sdf1.format( date );
		new File( outputDirectoryPath + "graph/" + graphDirectoryName ).mkdir();

		// そしてその下に時間別のnode, edgeの情報を入れるディレクトリを作る
		new File( outputDirectoryPath + "graph/" + graphDirectoryName + "/nodes" ).mkdir();
		new File( outputDirectoryPath + "graph/" + graphDirectoryName + "/edges" ).mkdir();
	}

	static void writeNodeInformationAsCSV( int currentTicks, List< Agent > agentList ) {
		try {
			// nodeファイルを作ってそこにnode(agent)の情報を出力する
			PrintWriter pw = newCSVPrintWriter( "graph/" + graphDirectoryName + "/nodes/" + currentTicks );
			pw.print( "id," );
			pw.print( "role," );
			pw.print( "x," );
			pw.print( "y," );
			pw.println( "principle" );

			for ( Agent ag: agentList ) {
				pw.print( ag.id + "," );
				pw.print( ag.role + "," );
				pw.print( ag.getX() + "," );
				pw.print( ag.getY() + "," );
//				pw.println( ag.principle );
			}

			pw.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	static void writeGraphInformationAsCSV( int currentTick, GraphAtAnWindow graph ) {
		try {
			// graphファイルを作ってそこにedge(team formationの濃度)の情報を出力する
			PrintWriter pw = newCSVPrintWriter( "graph/" + graphDirectoryName + "/edges/" + currentTick );
			pw.println( "from," + "to," + "times" );

			for ( Map.Entry< Edge, Integer > e: graph.getGraph().entrySet() ) {
				pw.print( e.getKey().from_id + "," );
				pw.print( e.getKey().to_id + "," );
				pw.println( e.getValue() );
			}
			pw.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	static void writeRelationsBetweenDelayAndDE( int[][] delays, List< Agent > agents, String st ) {
		double meanLeaderCor = 0, meanMemberCor = 0;
		int leaders = 0, members = 0;

		for ( Agent ag: agents ) {
			List< TemplateStrategy.Dependability > pairList = ag.e_leader > ag.e_member ? ag.ls.dependabilityRanking : ag.ms.dependabilityRanking;
			int size = pairList.size();

			double[] DEs = new double[ size ];
			double[] delay = new double[ size ];
			for ( int i = 0; i < size; i++ ) {
				TemplateStrategy.Dependability pair = pairList.get( i );
				Agent target = pair.getAgent();

				DEs[ i ] = pair.getValue();
				delay[ i ] = delays[ ag.id ][ target.id ];
			}
			PearsonsCorrelation p = new PearsonsCorrelation();
			double cor = p.correlation( DEs, delay );
			if ( ag.e_leader > ag.e_member ) {
				meanLeaderCor += cor;
				leaders++;
			} else {
				meanMemberCor += cor;
				members++;
			}
			System.out.println( ag + "correlation: " + String.format( "%.3f", cor ) );
		}

		System.out.println( "Average Leader: " + meanLeaderCor / leaders );
		System.out.println( "Average Member: " + meanMemberCor / members );
	}

	String setPath( String dir_name, String file_name, String extension ) {
		Date date = new Date();
		SimpleDateFormat sdf1 = new SimpleDateFormat( ",yyyy_MM_dd,HH_mm_ss" );
		System.out.println( "Address: " + dir_name + "/" + file_name + ",λ=" + String.format( "%.2f", TaskManager.getAdditional_tasks_num_() ) + sdf1.format( date ) + "." + extension );
		return outputDirectoryPath + dir_name + "/" + file_name + ",λ=" + String.format( "%.2f", TaskManager.getAdditional_tasks_num_() ) + sdf1.format( date ) + "." + extension;
	}

}
