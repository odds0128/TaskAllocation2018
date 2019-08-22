package main.research.agent.strategy.ProposedStrategy;

import main.research.Manager;
import main.research.agent.Agent;

import java.util.*;
import static main.research.SetParam.*;

public class CDSet {
	private Agent target;
	private double[] congestionDegree;
	private int lastUpdatedTime;

	CDSet( Agent target, double[] congestionDegree, int lastUpdatedTime ) {
		this.target = target;
		this.congestionDegree = congestionDegree;
		this.lastUpdatedTime = lastUpdatedTime;
	}

	static boolean alreadyExists( Agent target, List< CDSet > list ) {
		for( CDSet entry: list ) {
			if ( entry.getTarget().equals( target ) ) return true;
		}
		return false;
	}

	public static void replace( Agent target, double[] tempArray, List< CDSet > list ) {
		for( CDSet entry : list ) {
			if( entry.getTarget().equals( target ) ) {
				entry.setCongestionDegree( tempArray );
				entry.setLastUpdatedTime( Manager.getCurrentTime() );
			}
		}
	}

	static double[] getCD( Agent target, List< CDSet > list ) {
		for( CDSet entry : list ) {
			if( entry.getTarget().equals( target ) ) return entry.getCD();
		}
		System.out.println("oh my god");
		return new double[RESOURCE_TYPES];
	}

	static void refreshMap( List< CDSet > list ) {
		int size = list.size();
		for( int i = 0; i < size; i++ ) {
			CDSet entry = list.remove( 0 );
			if( Manager.getCurrentTime() - entry.getLastUpdatedTime() < CD_CACHE_TIME ) {
				list.add( entry );
			}
		}
	}

	public Agent getTarget() {
		return target;
	}

	protected double[] getCD() {
		return congestionDegree;
	}

	protected int getLastUpdatedTime() {
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
		sb.append( String.format("%5d: ", Manager.getCurrentTime() ) );
		sb.append( " -> " + target + ", " + Arrays.toString( congestionDegree ) + ", " + ", Last update: " + lastUpdatedTime );

		return sb.toString();
	}
}
