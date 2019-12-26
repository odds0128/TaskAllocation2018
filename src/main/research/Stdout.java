package main.research;

import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.agent.strategy.OCTuple;
import main.research.agent.strategy.TemplateStrategy;
import main.research.agent.strategy.reliableAgents.LeaderStrategy;
import main.research.task.Task;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.util.Arrays;
import java.util.List;

import static main.research.Parameter.*;

public class Stdout {
	private static int agent_num_ = AgentManager.agent_num_;

	static void checkTask( List< Task > taskQueue ) {
		int num = taskQueue.size();
		System.out.println( "QueueSize: " + num );
		for ( int i = 0; i < num; i++ ) {
			Task temp = taskQueue.remove( 0 );
			System.out.println( temp );
			taskQueue.add( temp );
		}
		System.out.println( "  Remains: " + taskQueue.size() );
	}

	public static void checkGrid( Agent[][] grid ) {
		System.out.println( "Total Agents is " + Agent._id );
		for ( int i = 0; i < MAX_X; i++ ) {
			for ( int j = 0; j < MAX_Y; j++ ) {
				if ( grid[ i ][ j ] == null ) System.out.print( "    " );
				else System.out.print( String.format( "%3d ", grid[ i ][ j ].id ) );
			}
			System.out.println();
		}
	}

	public static void checkDelay( int[][] delays ) {
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

	static void showRelationsBetweenOCandDE( List< Agent > agents ) {
		double average = 0;
		int num = 0;
		double meanLeaderCor = 0, meanMemberCor = 0;
		int leaders = 0, members = 0;

		try {
			// TODO: 委譲によるラッパー化
			LeaderStrategy.class.getDeclaredMethod( "getCdTupleList", LeaderStrategy.class );
		} catch ( NoSuchMethodException e ) {
			return;
		}
		for ( Agent ag: agents ) {
			List< OCTuple > ocTupleList = LeaderStrategy.getOcTupleList( ( LeaderStrategy ) ag.ls );
			int size = ocTupleList.size();
			if ( size == 0 || size == 1 ) continue;

			double[] OCs = new double[ size ];
			double[] DEs = new double[ size ];

			for ( int i = 0; i < size; i++ ) {
				OCTuple temp = ocTupleList.remove( 0 );
				Agent target = temp.getTarget();

				double[] tempOC = temp.getOCArray();
				OCs[ i ] = Arrays.stream( tempOC ).max().getAsDouble();

				for ( TemplateStrategy.Dependability pair: ag.ls.reliableMembersRanking ) {
					if ( target.equals( pair.getAgent() ) ) {
						DEs[ i ] = pair.getValue();
					}
				}
			}
			PearsonsCorrelation p = new PearsonsCorrelation();
			double cor = p.correlation( OCs, DEs );
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
		System.out.println( agents.stream()
			.filter( agent -> agent.e_leader > agent.e_member )
			.count() );
		System.out.println( agents.stream()
			.filter( agent -> agent.e_leader < agent.e_member )
			.count() );
	}

}
