package main.research;

import main.research.agent.AgentDePair;
import main.research.agent.AgentManager;
import main.research.agent.strategy.CDTuple;
import main.research.agent.strategy.reliableAgents.LeaderStrategy;
import main.research.task.Task;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import main.research.agent.Agent;
import main.research.communication.TransmissionPath;
import main.research.graph.Edge;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static main.research.SetParam.Principle.RATIONAL;
import static main.research.SetParam.Principle.RECIPROCAL;
import static main.research.SetParam.Role.LEADER;

/**
 * OutPutクラス
 * Singletonで実装
 * 結果の画面出力とファイル出力を管理
 */
public class OutPut implements SetParam {
	private static int executionTimes_ = Manager.executionTimes_;
	private static int writing_times_ = Manager.writing_times_;
	private static int max_turn_ = Manager.max_turn_;
	private static int agent_num_ = AgentManager.agent_num_;

	private static OutPut _singleton = new OutPut();
    static Workbook book = null;
    static FileOutputStream fout = null;

	static int index = 0;

	static int[] finishedTasksArray = new int[ writing_times_ ];
	static int[] disposedTasksArray = new int[ writing_times_ ];
	static int[] overflownTasksArray = new int[ writing_times_ ];
	static int[] messagesArray = new int[ writing_times_ ];
	static double[] communicationDelayArray = new double[ writing_times_ ];
	static int[] leaderNumArray = new int[ writing_times_ ];
	static int[] neetMembersArray = new int[ writing_times_ ];
	static int[] reciprocalistsArray = new int[ writing_times_ ];
	static int[] rationalistsArray = new int[ writing_times_ ];
	static int[] reciprocalMembersArray = new int[ writing_times_ ];
	static int[] finishedTasksInDepopulatedAreaArray = new int[ writing_times_ ];
	static int[] finishedTasksInPopulatedAreaArray = new int[ writing_times_ ];
	static int[] tempTaskExecutionTimeArray = new int[ writing_times_ ];
	static double[] taskExecutionTimeArray = new double[ writing_times_ ];
	static int taskExecutionTimes = 0;

	static void aggregateAgentData( List< Agent > agents ) {
		neetMembersArray[ index ] += Agent.countNEETmembers( agents, max_turn_ / writing_times_ );
		leaderNumArray[ index ] += countLeader( agents );
	}

	private static int countLeader( List< Agent > agentList ) {
		return ( int ) agentList.stream()
			.filter( agent -> agent.role == LEADER )
			.count();
	}

	static int[] tempMessagesArray = new int[ writing_times_ ];

	static void aggregateData( int ft, int dt, int ot, int rm, List< Agent > agentList ) {
		finishedTasksArray[ index ] += ft;
		tempMessagesArray[ index ] = TransmissionPath.getMessageNum();
		int gap = index > 0 ? tempMessagesArray[ index - 1 ] : 0;
		messagesArray[ index ] += TransmissionPath.getMessageNum() - gap;
		communicationDelayArray[ index ] += TransmissionPath.getAverageCommunicationTime();
		disposedTasksArray[ index ] += dt;
		overflownTasksArray[ index ] += ot;
		reciprocalMembersArray[ index ] += rm;
		if ( index == writing_times_ - 1 ) {
			tempMessagesArray = new int[ writing_times_ ];
		}
		indexIncrement();
	}

	private static void indexIncrement() {
		if ( taskExecutionTimes != 0 ) {
			taskExecutionTimeArray[ index ] += ( double ) tempTaskExecutionTimeArray[ index ] / taskExecutionTimes;
		}
		tempTaskExecutionTimeArray[ index ] = 0;
		taskExecutionTimes = 0;
		index = ( index + 1 ) % writing_times_;
	}

	static void checkTask( List< Task > taskQueue ) {
		int num = taskQueue.size();
		System.out.println( "QueueSize: " + num );
		for ( int i = 0; i < num; i++ ) {
			Task temp = taskQueue.remove(0);
			System.out.println( temp );
			taskQueue.add( temp );
		}
		System.out.println( "  Remains: " + taskQueue.size() );
	}

	static void checkGrid( Agent[][] grid ) {
		System.out.println( "Total Agents is " + Agent._id );
		for ( int i = 0; i < MAX_X; i++ ) {
			for ( int j = 0; j < MAX_Y; j++ ) {
				if ( grid[ i ][ j ] == null ) System.out.print( "    " );
				else System.out.print( String.format( "%3d ", grid[ i ][ j ].id ) );
			}
			System.out.println();
		}
	}

	static void checkDelay( int[][] delays ) {
		int[] countDelay = new int[ MAX_DELAY ];
		for ( int i = 0; i < agent_num_; i++ ) {
			System.out.print( "ID: " + i + "..." );
			for ( int j = 0; j < agent_num_; j++ ) {
				System.out.print( delays[ i ][ j ] + ", " );
				if ( i != j ) {
					System.out.println( i + ", " + j + ", " + delays[ i ][ j ] );
					countDelay[ delays[ i ][ j ] - 1 ]++;
				}
			}
			System.out.println();
		}
		for ( int i = 0; i < MAX_DELAY; i++ ) {
			System.out.println( ( i + 1 ) + ", " + countDelay[ i ] / agent_num_ );
		}
	}

	static void writeResults( String st ) {
		System.out.println("called");
		String outputFilePath = _singleton.setPath( "results", st, "csv" );
		System.out.println( "writing now" );
		FileWriter fw;
		BufferedWriter bw;
		PrintWriter pw;
		try {
			fw = new FileWriter( outputFilePath, false );
			bw = new BufferedWriter( fw );
			pw = new PrintWriter( bw );

			pw.println( "turn" + ", "
					+ "FinishedTasks" + ", " + "DisposedTasks" + ", " + "OverflownTasks" + ", "
					+ "Success rate(except overflow)" + ", " + "Success rate" + ", "
					+ "CommunicationTime" + ", " + "Messages" + ", " + "ExecutionTime" + ","
					+ "Leader" + ", " // + "Member"                            + ", "
					+ "NEET Members" + ", "
					// + "Lonely leaders"                    + ", " + "Lonely members"                    + ", "
					// + "Accompanied leaders"               + ", " + "Accompanied members"               + ", "
					+ "ReciprocalLeaders" + ", " + "ReciprocalMembers" + ", "
				// + "Rational"                          + ", " + "ReciprocalMembers" + ","
				// + "FinishedTasks in depopulated area" + ", " + "FinishedTasks in populated area"   + ", "
			);
			for ( int i = 0; i < writing_times_; i++ ) {
				pw.println( ( i + 1 ) * ( max_turn_ / writing_times_ ) + ", "
					+ finishedTasksArray[ i ] / executionTimes_ + ", "
					+ disposedTasksArray[ i ] / executionTimes_ + ", "
					+ overflownTasksArray[ i ] / executionTimes_ + ", "
					+ ( double ) finishedTasksArray[ i ] / ( finishedTasksArray[ i ] + disposedTasksArray[ i ] ) + ", "
					+ ( double ) finishedTasksArray[ i ] / ( finishedTasksArray[ i ] + disposedTasksArray[ i ] + overflownTasksArray[ i ] ) + ", "
					+ ( double ) communicationDelayArray[ i ] / executionTimes_ + ", "
					+ ( double ) messagesArray[ i ] / executionTimes_ + ", "
					+ ( double ) taskExecutionTimeArray[ i ] / executionTimes_ + ", "
					+ ( double ) leaderNumArray[ i ] / executionTimes_ + ", "
					+ ( double ) neetMembersArray[ i ] / executionTimes_ + ", "
					+ ( double ) ( reciprocalistsArray[ i ] - reciprocalMembersArray[ i ] ) / executionTimes_ + ", "
					+ ( double ) reciprocalMembersArray[ i ] / executionTimes_ + ", "
				);
			}
			pw.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	static void writeAgentsSubtaskQueueSize( PrintWriter pw ) {
		pw.print( Manager.getCurrentTime() );
		List< Agent > agList = AgentManager.getAllAgentList();
		for( int i = 0; i < 10; i++ ) {
			Agent ag = agList.get( i );
			pw.print( ", " + ag.role + ", " + ag.ms.mySubtaskQueue.size() );
//		writeLeadersCD( pw, ag );
		}
		pw.println();
	}

	static void writeLeadersCD( PrintWriter pw, Agent target ) {
		for ( Agent leader: AgentManager.getAllAgentList() ) {
			if ( leader.role == LEADER ) {
				LeaderStrategy pl = ( LeaderStrategy ) leader.ls;
				boolean exists = CDTuple.alreadyExists( target, pl.getCdTupleList() );
				double temp = exists ? CDTuple.getCD( 1, target, pl.getCdTupleList() ) : -1 ;

				pw.print( ", " + temp );
			}
		}
	}

	static void writeDelays( int[][] delays ) {
		FileWriter fw;
		BufferedWriter bw;
		PrintWriter pw;
		try {
			String currentPath = System.getProperty( "user.dir" );
			Date date = new Date();
			SimpleDateFormat sdf1 = new SimpleDateFormat( ",yyyy:MM:dd,HH:mm:ss" );
			fw = new FileWriter( currentPath + "/out/results/communicationDelay=" + MAX_DELAY + sdf1.format( date ) + ".csv", false );
			bw = new BufferedWriter( fw );
			pw = new PrintWriter( bw );

			pw.print( "id" );
			for ( int i = 0; i < agent_num_; i++ ) pw.print( ", " + i );
			pw.println();
			for ( int i = 0; i < agent_num_; i++ ) {
				pw.print( i + ", " );
				for ( int j = 0; j < agent_num_; j++ ) {
					pw.print( delays[ i ][ j ] + ", " );
				}
				pw.println();
			}
			pw.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	static void writeRelationsBetweenCDandDE( List< Agent > agents ) {
		double average = 0;
		int num = 0;
		double meanLeaderCor = 0, meanMemberCor = 0;
		int leaders = 0, members = 0;

		for ( Agent ag: agents ) {
			List< CDTuple > cdTupleList = null;
			if ( ag.ls.getClass().getSimpleName().equals( "LeaderStrategy" ) ) {
				cdTupleList = LeaderStrategy.getCdSetList( ( LeaderStrategy ) ag.ls );
			} else if ( ag.ls.getClass().getSimpleName().equals( "ProposedStrategy_l" ) ) {
				cdTupleList = LeaderStrategy.getCdSetList( ( LeaderStrategy ) ag.ls );
			}
			int size = cdTupleList.size();
			if ( size == 0 || size == 1 ) continue;

			double[] CDs = new double[ size ];
			double[] DEs = new double[ size ];

			for ( int i = 0; i < size; i++ ) {
				CDTuple temp = cdTupleList.remove( 0 );
				Agent target = temp.getTarget();

				double[] tempCD = temp.getCDArray();
				CDs[ i ] = Arrays.stream( tempCD ).max().getAsDouble();

				for ( AgentDePair pair: ag.ls.reliableMembersRanking ) {
					if ( target.equals( pair.getTarget() ) ) {
						DEs[ i ] = pair.getDe();
					}
				}
			}
			PearsonsCorrelation p = new PearsonsCorrelation();
			double cor = p.correlation( CDs, DEs );
			if ( Double.isNaN( cor ) ) continue;
			if ( ag.e_leader > ag.e_member ) {
				meanLeaderCor += cor;
				leaders++;
			} else {
				meanMemberCor += cor;
				members++;
			}
			average += cor;
			num++;
		}

		System.out.println( "Average : " + average / num );
		System.out.println( "Average Leader: " + meanLeaderCor / leaders );
		System.out.println( "Average Member: " + meanMemberCor / members );
	}

	static void writeRelationsBetweenDelayAndDE( int[][] delays, List< Agent > agents, String st ) {
		double meanLeaderCor = 0, meanMemberCor = 0;
		int leaders = 0, members = 0;

		for ( Agent ag: agents ) {
			List< AgentDePair > pairList = ag.e_leader > ag.e_member ? ag.ls.reliableMembersRanking : ag.ms.reliableLeadersRanking;
			int size = pairList.size();

			double[] DEs = new double[ size ];
			double[] delay = new double[ size ];
			for ( int i = 0; i < size; i++ ) {
				AgentDePair pair = pairList.get( i );
				Agent target = pair.getTarget();

				DEs[ i ] = pair.getDe();
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

	static void writeReliabilities( List< Agent > agents, String st ) {
		FileWriter fw;
		BufferedWriter bw;
		PrintWriter pw;
		String fileName = st;
		try {
			String currentPath = System.getProperty( "user.dir" );
			Date date = new Date();
			SimpleDateFormat sdf1 = new SimpleDateFormat( ",yyyy:MM:dd,HH:mm:ss" );
			fw = new FileWriter( currentPath + "/out/results/rel" + fileName + ", λ=" + String.format( "%.2f", ADDITIONAL_TASK_NUM ) + sdf1.format( date ) + ".csv", false );
			bw = new BufferedWriter( fw );
			pw = new PrintWriter( bw );

			// 列番号入れる部分
			pw.print( " , targets" );
			for ( int i = 0; i < agent_num_; i++ ) pw.print( ", " + i + ", " );
			pw.println();

			pw.print( ", Role, " );
			for ( int i = 0; i < agent_num_; i++ ) pw.print( "             DE_l ,             DE_m, " );
			pw.println();

			for ( Agent from: agents ) {
				// column 1 : Role
				pw.print( from.id + ", " );
				if ( from.e_member > from.e_leader ) {
					pw.print( "Member, " );
				} else {
					pw.print( "Leader, " );
				}

				// column 2~3 :  DE_l(from→target), DE_m(from→target),
				for ( Agent target: agents ) {
					// 自分は飛ばす
					if ( target.equals( from ) ) {
						pw.print( " , ," );
					}
				}
				pw.println();
			}
			pw.close();
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		} catch ( IOException e2 ) {
			e2.printStackTrace();
		}
	}
	static void writeGraphInformationX( List< Agent > agents, String st ) {
		String outputFilePath = _singleton.setPath( "relationships", st, "xlsx" );
		Edge edge = new Edge();
		edge.makeEdge( agents );
		try {
			book = new SXSSFWorkbook();
			Font font = book.createFont();
			font.setFontName( "Times New Roman" );
			font.setFontHeightInPoints( ( short ) 9 );

			DataFormat format = book.createDataFormat();

			//ヘッダ文字列用のスタイル
			CellStyle style_header = book.createCellStyle();
			style_header.setBorderBottom( BorderStyle.THIN );
			OutPut.setBorder( style_header, BorderStyle.THIN );
			style_header.setFillForegroundColor( HSSFColor.HSSFColorPredefined.LIGHT_CORNFLOWER_BLUE.getIndex() );
			style_header.setFillPattern( FillPatternType.SOLID_FOREGROUND );
			style_header.setVerticalAlignment( VerticalAlignment.TOP );
			style_header.setFont( font );

			//文字列用のスタイル
			CellStyle style_string = book.createCellStyle();
			OutPut.setBorder( style_string, BorderStyle.THIN );
			style_string.setVerticalAlignment( VerticalAlignment.TOP );
			style_string.setFont( font );

			//改行が入った文字列用のスタイル
			CellStyle style_string_wrap = book.createCellStyle();
			OutPut.setBorder( style_string_wrap, BorderStyle.THIN );
			style_string_wrap.setVerticalAlignment( VerticalAlignment.TOP );
			style_string_wrap.setWrapText( true );
			style_string_wrap.setFont( font );

			//整数用のスタイル
			CellStyle style_int = book.createCellStyle();
			OutPut.setBorder( style_int, BorderStyle.THIN );
			style_int.setDataFormat( format.getFormat( "###0;-###0" ) );
			style_int.setVerticalAlignment( VerticalAlignment.TOP );
			style_int.setFont( font );

			//小数用のスタイル
			CellStyle style_double = book.createCellStyle();
			OutPut.setBorder( style_double, BorderStyle.THIN );
			style_double.setDataFormat( format.getFormat( "###0.0;-###0.0" ) );
			style_double.setVerticalAlignment( VerticalAlignment.TOP );
			style_double.setFont( font );

			//円表示用のスタイル
			CellStyle style_yen = book.createCellStyle();
			OutPut.setBorder( style_yen, BorderStyle.THIN );
			style_yen.setDataFormat( format.getFormat( "\"\\\"###0;\"\\\"-###0" ) );
			style_yen.setVerticalAlignment( VerticalAlignment.TOP );
			style_yen.setFont( font );

			//パーセント表示用のスタイル
			CellStyle style_percent = book.createCellStyle();
			OutPut.setBorder( style_percent, BorderStyle.THIN );
			style_percent.setDataFormat( format.getFormat( "0.0%" ) );
			style_percent.setVerticalAlignment( VerticalAlignment.TOP );
			style_percent.setFont( font );

			//日時表示用のスタイル
			CellStyle style_datetime = book.createCellStyle();
			OutPut.setBorder( style_datetime, BorderStyle.THIN );
			style_datetime.setDataFormat( format.getFormat( "yyyy/mm/dd hh:mm:ss" ) );
			style_datetime.setVerticalAlignment( VerticalAlignment.TOP );
			style_datetime.setFont( font );

			Row row;
			int rowNumber;
			int colNumber;

			Sheet sheet;

			for ( int i = 0; i < 3; i++ ) {
				sheet = book.createSheet();
				if ( sheet instanceof SXSSFSheet ) {
					( ( SXSSFSheet ) sheet ).trackAllColumnsForAutoSizing();
				}
				//シート名称の設定
				if ( i == 0 ) book.setSheetName( i, "nodes" );
				else if ( i == 1 ) book.setSheetName( i, "edges" );
				else if ( i == 2 ) book.setSheetName( i, "reciprocalEdges" );

				//ヘッダ行の作成
				rowNumber = 0;
				colNumber = 0;

				row = sheet.createRow( rowNumber );

				if ( i == 0 ) _singleton.writeCell( row, colNumber++, style_header, "Node id" );
				else _singleton.writeCell( row, colNumber++, style_header, "main.research.graph.Edge id" );

				if ( i == 0 ) _singleton.writeCell( row, colNumber++, style_header, "Node color" );
				else _singleton.writeCell( row, colNumber++, style_header, "Source Node id" );


				if ( i == 0 ) _singleton.writeCell( row, colNumber++, style_header, "Node shape" );
				else _singleton.writeCell( row, colNumber++, style_header, "Target Node id" );

				if ( i == 0 ) {
					_singleton.writeCell( row, colNumber++, style_header, " x-coordinate " );
					_singleton.writeCell( row, colNumber++, style_header, " y-coordinate " );
					_singleton.writeCell( row, colNumber++, style_header, " leader id" );
					_singleton.writeCell( row, colNumber++, style_header, " delay to leader " );
//                    _singleton.writeCell(row, colNumber++, style_header, " is lonely or not");
//                    _singleton.writeCell(row, colNumber++, style_header, " is accompanied or not");
					for ( int j = 0; j < RESOURCE_TYPES; j++ ) {
						_singleton.writeCell( row, colNumber++, style_header, " Resources " + j );
						_singleton.writeCell( row, colNumber++, style_header, " Required " + j );
						_singleton.writeCell( row, colNumber++, style_header, " Allocated " + j );
					}
					_singleton.writeCell( row, colNumber++, style_header, " Excellence " );
					_singleton.writeCell( row, colNumber++, style_header, "Did tasks as leader in last period" );
					_singleton.writeCell( row, colNumber++, style_header, "Did tasks as member in last period" );
					_singleton.writeCell( row, colNumber++, style_header, " e_leader" );
					_singleton.writeCell( row, colNumber++, style_header, " e_member " );
				} else {
					_singleton.writeCell( row, colNumber++, style_header, " length " );
					_singleton.writeCell( row, colNumber++, style_header, " times " );
					_singleton.writeCell( row, colNumber++, style_header, "Target Node id again" );
				}

				//ウィンドウ枠の固定
				sheet.createFreezePane( 1, 1 );

				//ヘッダ行にオートフィルタの設定
				sheet.setAutoFilter( new CellRangeAddress( 0, 0, 0, colNumber ) );

				//列幅の自動調整
				for ( int j = 0; j <= colNumber; j++ ) {
					sheet.autoSizeColumn( j, true );
				}

				//nodeシートへの書き込み
				if ( i == 0 ) {
					for ( Agent agent: agents ) {
						rowNumber++;
						colNumber = 0;
						row = sheet.createRow( rowNumber );
						_singleton.writeCell( row, colNumber++, style_int, agent.id );


						if ( agent.e_leader > agent.e_member ) {
							_singleton.writeCell( row, colNumber++, style_string, "Red" );
							_singleton.writeCell( row, colNumber++, style_string, "Circle" );
						} else if ( agent.principle == RATIONAL ) {
							_singleton.writeCell( row, colNumber++, style_string, "Green" );
							_singleton.writeCell( row, colNumber++, style_string, "Square" );
						} else if ( agent.principle == RECIPROCAL ) {
							_singleton.writeCell( row, colNumber++, style_string, "Blue" );
							_singleton.writeCell( row, colNumber++, style_string, "Triangle" );
						}

						_singleton.writeCell( row, colNumber++, style_int, agent.getX() * 10 );
						_singleton.writeCell( row, colNumber++, style_int, agent.getY() * 10 );

						for ( int j = 0; j < RESOURCE_TYPES; j++ ) {
							_singleton.writeCell( row, colNumber++, style_int, agent.resources[ j ] );
							_singleton.writeCell( row, colNumber++, style_int, agent.required[ j ] );
						}
						int resCount = ( int ) Arrays.stream( agent.resources )
							.filter( resource -> resource > 0 )
							.count();
						double excellence = ( double ) Arrays.stream( agent.resources ).sum() / resCount;
						_singleton.writeCell( row, colNumber++, style_double, excellence );
						_singleton.writeCell( row, colNumber++, style_int, agent.didTasksAsLeader );
						_singleton.writeCell( row, colNumber++, style_int, agent.didTasksAsMember );
						_singleton.writeCell( row, colNumber++, style_double, agent.e_leader );
						_singleton.writeCell( row, colNumber++, style_double, agent.e_member );


						//列幅の自動調整
						for ( int k = 0; k <= colNumber; k++ ) {
							sheet.autoSizeColumn( k, true );
						}
					}
				}
				// edgeシートへの書き込み
				else if ( i == 1 ) {
					for ( int j = 0; j < edge.from_id.size(); j++ ) {
						rowNumber++;
						colNumber = 0;
						row = sheet.createRow( rowNumber );
						_singleton.writeCell( row, colNumber++, style_int, j );
						_singleton.writeCell( row, colNumber++, style_int, edge.from_id.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.to_id.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.delays.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.times.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.to_id.get( j ) );


						//列幅の自動調整
						for ( int k = 0; k <= colNumber; k++ ) {
							sheet.autoSizeColumn( k, true );
						}
					}
				} else if ( i == 2 ) {
					for ( int j = 0; j < edge.from_id.size(); j++ ) {
						if ( edge.isRecipro.get( j ) == true ) {
							rowNumber++;
							colNumber = 0;
							row = sheet.createRow( rowNumber );
							_singleton.writeCell( row, colNumber++, style_int, j );
							_singleton.writeCell( row, colNumber++, style_int, edge.from_id.get( j ) );
							_singleton.writeCell( row, colNumber++, style_int, edge.to_id.get( j ) );
							_singleton.writeCell( row, colNumber++, style_int, edge.delays.get( j ) );
							_singleton.writeCell( row, colNumber++, style_int, edge.times.get( j ) );
							_singleton.writeCell( row, colNumber++, style_int, edge.to_id.get( j ) );
						}
						//列幅の自動調整
						for ( int k = 0; k <= colNumber; k++ ) {
							sheet.autoSizeColumn( k, true );
						}
					}
				}
			}


			// ここからリーダーからのエッジ生成
			edge.reset();
			edge.makeEdgesFromLeader( agents );
			for ( int i = 0; i < 2; i++ ) {
				sheet = book.createSheet();
				if ( sheet instanceof SXSSFSheet ) {
					( ( SXSSFSheet ) sheet ).trackAllColumnsForAutoSizing();
				}
				if ( i == 0 ) book.setSheetName( i + 3, "EdgesFromLeader" );
				else book.setSheetName( i + 3, "reciprocalEdgesFromLeader" );

				//ヘッダ行の作成
				rowNumber = 0;
				colNumber = 0;

				row = sheet.createRow( rowNumber );

				_singleton.writeCell( row, colNumber++, style_header, "main.research.graph.Edge id" );
				_singleton.writeCell( row, colNumber++, style_header, "Source Node id" );
				_singleton.writeCell( row, colNumber++, style_header, "Target Node id" );
				_singleton.writeCell( row, colNumber++, style_header, " length " );
				_singleton.writeCell( row, colNumber++, style_header, " times " );
				_singleton.writeCell( row, colNumber++, style_header, "Target Node id again" );

				//ウィンドウ枠の固定
				sheet.createFreezePane( 1, 1 );

				//ヘッダ行にオートフィルタの設定
				sheet.setAutoFilter( new CellRangeAddress( 0, 0, 0, colNumber ) );

				//列幅の自動調整
				for ( int j = 0; j <= colNumber; j++ ) {
					sheet.autoSizeColumn( j, true );
				}

				if ( i == 0 ) {
					for ( int j = 0; j < edge.from_id.size(); j++ ) {
						rowNumber++;
						colNumber = 0;
						row = sheet.createRow( rowNumber );
						_singleton.writeCell( row, colNumber++, style_int, j );
						_singleton.writeCell( row, colNumber++, style_int, edge.from_id.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.to_id.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.delays.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.times.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.to_id.get( j ) );


						//列幅の自動調整
						for ( int k = 0; k <= colNumber; k++ ) {
							sheet.autoSizeColumn( k, true );
						}
					}
				} else if ( i == 1 ) {
					for ( int j = 0; j < edge.from_id.size(); j++ ) {
						if ( edge.isRecipro.get( j ) != true ) continue;
						rowNumber++;
						colNumber = 0;
						row = sheet.createRow( rowNumber );
						_singleton.writeCell( row, colNumber++, style_int, j );
						_singleton.writeCell( row, colNumber++, style_int, edge.from_id.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.to_id.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.delays.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.times.get( j ) );
						_singleton.writeCell( row, colNumber++, style_int, edge.to_id.get( j ) );

						//列幅の自動調整
						for ( int k = 0; k <= colNumber; k++ ) {
							sheet.autoSizeColumn( k, true );
						}
					}
				}
			}


			//ファイル出力
			fout = new FileOutputStream( outputFilePath );
			book.write( fout );
		} catch ( IOException e ) {
			e.printStackTrace();
		} finally {
			if ( fout != null ) {
				try {
					fout.close();
				} catch ( IOException ignored ) {
				}
			}
			if ( book != null ) {
				try {
                    /*
                        SXSSFWorkbookはメモリ空間を節約する代わりにテンポラリファイルを大量に生成するため、
                        不要になった段階でdisposeしてテンポラリファイルを削除する必要がある
                     */
					( ( SXSSFWorkbook ) book ).dispose();
				} catch ( Exception ignored ) {
				}
			}
		}
		edge = null;
	}

	private static void setBorder( CellStyle style, BorderStyle border ) {
		style.setBorderBottom( border );
		style.setBorderTop( border );
		style.setBorderLeft( border );
		style.setBorderRight( border );
	}

	private final static String[] LIST_ALPHA = {
		"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
	};

	String setPath( String dir_name, String file_name, String extension ) {
		String currentPath = System.getProperty( "user.dir" );
		Date date = new Date();
		SimpleDateFormat sdf1 = new SimpleDateFormat( ",yyyy_MM_dd,HH_mm_ss" );
		System.out.println( "Address: " + dir_name + "/" + file_name + ",λ=" + String.format( "%.2f", ADDITIONAL_TASK_NUM ) + sdf1.format( date ) + "." + extension );
		return currentPath + "/out/" + dir_name + "/" + file_name + ",λ=" + String.format( "%.2f", ADDITIONAL_TASK_NUM ) + sdf1.format( date ) + "." + extension;
	}

	// あるrowのcolumn列にoを書き込むメソッド
	// 返り値は
	private Cell writeCell( Row row, int col_number, CellStyle style, Object o ) {
		Cell cell;
		if ( o.getClass().getName() == "java.lang.String" ) {
			cell = row.createCell( col_number++ );
			cell.setCellStyle( style );
			cell.setCellValue( o.toString() );
		} else if ( o.getClass().getName() == "java.lang.Integer" ) {
			cell = row.createCell( col_number++ );
			cell.setCellStyle( style );
			cell.setCellValue( ( int ) o );
		} else {
			cell = row.createCell( col_number++ );
			cell.setCellStyle( style );
			cell.setCellValue( ( double ) o );
		}
		return cell;
	}
}
