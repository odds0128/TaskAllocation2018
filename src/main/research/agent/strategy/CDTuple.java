package main.research.agent.strategy;

import main.research.Manager;
import main.research.agent.Agent;

import java.util.*;
import static main.research.SetParam.*;

public class CDTuple {
	private Agent target;
	private double[] congestionDegree;
	private int lastUpdatedTime;

	public CDTuple( Agent target, double[] congestionDegree, int lastUpdatedTime ) {
		this.target = target;
		this.congestionDegree = congestionDegree;
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public static boolean alreadyExists( Agent target, List< CDTuple > list ) {
		for( CDTuple entry: list ) {
			if ( entry.getTarget().equals( target ) ) return true;
		}
		return false;
	}

	public static void replace( Agent target, double[] tempArray, List< CDTuple > list ) {
		for( CDTuple entry : list ) {
			if( entry.getTarget().equals( target ) ) {
				entry.setCongestionDegree( tempArray );
				entry.setLastUpdatedTime( Manager.getCurrentTime() );
			}
		}
	}

	public static void updateCD( Agent target, List<CDTuple> list, int resourceType, double value ) {
		int index = searchAgent( target, list );
		list.get( index ).congestionDegree[resourceType] = value;
	}

	public static double getCD( int resourceIndex, Agent target, List< CDTuple > list ) {
		int agIndex = searchAgent( target, list );
		double[] targetCD = list.get( agIndex ).congestionDegree;
		return targetCD[resourceIndex];
	}

	public static void forgetOldCdInformation( List< CDTuple > list ) {
		int size = list.size();
		for( int i = 0; i < size; i++ ) {
			CDTuple entry = list.remove( 0 );
			if( Manager.getCurrentTime() - entry.getLastUpdatedTime() < CD_CACHE_TIME ) {
				list.add( entry );
			}
		}
	}

	private static int searchAgent( Agent wanted,  List< CDTuple > list ) {
		int index = 0;
		for( CDTuple current : list ) {
			if( current.getTarget().equals( wanted ) ) return index;
			index++;
		}
		return -1;
	}

	public static double calculateAverageCD( int resourceType, List<CDTuple> list ) {
		return list.stream()
			.mapToDouble( tuple -> tuple.congestionDegree[resourceType] )
			.average()
			.getAsDouble();
	}

	public Agent getTarget() {
		return target;
	}

	public double[] getCDArray() {
		return congestionDegree;
	}

	public int getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	public void setTarget( Agent target ) {
		this.target = target;
	}

	private void setCongestionDegree( double[] congestionDegree ) {
		this.congestionDegree = congestionDegree;
	}

	private void setLastUpdatedTime( int lastUpdatedTime ) {
		this.lastUpdatedTime = lastUpdatedTime;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(  );
		sb.append( String.format("now:%5d", Manager.getCurrentTime() ) );
		sb.append( " -> " + target + ", " + Arrays.toString( congestionDegree ) + ", " + ", Last update: " + lastUpdatedTime );

		return sb.toString();
	}
}
