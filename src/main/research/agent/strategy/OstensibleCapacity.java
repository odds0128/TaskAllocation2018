package main.research.agent.strategy;

import main.research.Manager;
import main.research.agent.Agent;

import java.util.*;
import static main.research.Parameter.*;

public class OstensibleCapacity {
	private Agent target;
	private double[] ostensibleCapacity;
	private int lastUpdatedTime;

	public OstensibleCapacity( Agent target, double[] ostensibleCapacity, int lastUpdatedTime ) {
		this.target = target;
		this.ostensibleCapacity = ostensibleCapacity;
		this.lastUpdatedTime = lastUpdatedTime;
	}

	public static boolean alreadyExists( Agent target, List< OstensibleCapacity > list ) {
		for( OstensibleCapacity entry: list ) {
			if ( entry.getTarget().equals( target ) ) return true;
		}
		return false;
	}

	public static void updateOC( Agent target, List< OstensibleCapacity > list, int resourceType, double value ) {
		int index = searchAgent( target, list );
		list.get( index ).ostensibleCapacity[resourceType] = value;
	}

	public static double getOC( int resourceIndex, Agent target, List< OstensibleCapacity > list ) {
		int agIndex = searchAgent( target, list );
		if( agIndex == -1 ) return -1;
		double[] targetOC = list.get( agIndex ).ostensibleCapacity;
		return targetOC[resourceIndex];
	}

	public static void forgetOldOcInformation( List< OstensibleCapacity > list ) {
		int size = list.size();
		for( int i = 0; i < size; i++ ) {
			OstensibleCapacity entry = list.remove( 0 );
			if( Manager.getCurrentTime() - entry.getLastUpdatedTime() < OC_CACHE_TIME ) {
				list.add( entry );
			}
		}
	}

	private static int searchAgent( Agent wanted,  List< OstensibleCapacity > list ) {
		int index = 0;
		for( OstensibleCapacity current : list ) {
			if( current.getTarget().equals( wanted ) ) return index;
			index++;
		}
		return -1;
	}

	public static double calculateAverageOC( int resourceType, List< OstensibleCapacity > list ) {
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
