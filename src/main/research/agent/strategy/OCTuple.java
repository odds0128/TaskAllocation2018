package main.research.agent.strategy;

import main.research.Manager;
import main.research.agent.Agent;

import java.util.*;
import static main.research.SetParam.*;

public class OCTuple {
	private Agent target;
	private double[] ostensibleCapacity;
	private int lastUpdatedTime;

	public OCTuple( Agent target, double[] ostensibleCapacity, int lastUpdatedTime ) {
		this.target = target;
		this.ostensibleCapacity = ostensibleCapacity;
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public static boolean alreadyExists( Agent target, List< OCTuple > list ) {
		for( OCTuple entry: list ) {
			if ( entry.getTarget().equals( target ) ) return true;
		}
		return false;
	}

	public static void replace( Agent target, double[] tempArray, List< OCTuple > list ) {
		for( OCTuple entry : list ) {
			if( entry.getTarget().equals( target ) ) {
				entry.setOstensibleCapacity( tempArray );
				entry.setLastUpdatedTime( Manager.getCurrentTime() );
			}
		}
	}

	public static void updateOC( Agent target, List<OCTuple> list, int resourceType, double value ) {
		int index = searchAgent( target, list );
		list.get( index ).ostensibleCapacity[resourceType] = value;
	}

	public static double getOC( int resourceIndex, Agent target, List< OCTuple > list ) {
		int agIndex = searchAgent( target, list );
		double[] targetOC = list.get( agIndex ).ostensibleCapacity;
		return targetOC[resourceIndex];
	}

	public static void forgetOldOcInformation( List< OCTuple > list ) {
		int size = list.size();
		for( int i = 0; i < size; i++ ) {
			OCTuple entry = list.remove( 0 );
			if( Manager.getCurrentTime() - entry.getLastUpdatedTime() < OC_CACHE_TIME ) {
				list.add( entry );
			}
		}
	}

	private static int searchAgent( Agent wanted,  List< OCTuple > list ) {
		int index = 0;
		for( OCTuple current : list ) {
			if( current.getTarget().equals( wanted ) ) return index;
			index++;
		}
		return -1;
	}

	public static double calculateAverageOC( int resourceType, List<OCTuple> list ) {
		return list.stream()
			.mapToDouble( tuple -> tuple.ostensibleCapacity[resourceType] )
			.average()
			.getAsDouble();
	}

	public Agent getTarget() {
		return target;
	}

	public double[] getOCArray() {
		return ostensibleCapacity;
	}

	public int getLastUpdatedTime() {
		return lastUpdatedTime;
	}

	public void setTarget( Agent target ) {
		this.target = target;
	}

	private void setOstensibleCapacity( double[] congestionDegree ) {
		this.ostensibleCapacity = congestionDegree;
	}

	private void setLastUpdatedTime( int lastUpdatedTime ) {
		this.lastUpdatedTime = lastUpdatedTime;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(  );
		sb.append( String.format("now:%5d", Manager.getCurrentTime() ) );
		sb.append( " -> " + target + ", " + Arrays.toString( ostensibleCapacity ) + ", " + ", Last update: " + lastUpdatedTime );

		return sb.toString();
	}
}
