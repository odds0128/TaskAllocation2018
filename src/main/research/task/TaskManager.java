package main.research.task;

import java.util.List;
import java.util.ArrayList;

import main.research.others.random.MyRandom;
import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

import static main.research.SetParam.*;

public class TaskManager {
	private static List< Task > tasks = new ArrayList<>();
	private static int disposedTasks = 0;
	private static int overflowTasks = 0;
	private static int finishedTasks = 0;

	public static void addNewTasksToQueue( int num ) {
		for ( int i = 0; i < num; i++ ) tasks.add( new Task( MIN_SUBTASK_NUM, MAX_SUBTASK_NUM ) );
	}

	public static void addNewTasksToQueue() {
		int room = TASK_QUEUE_SIZE - tasks.size();    // タスクキューの空き
		int poisson = poissonDistribution();
		int overflow = poisson > room ? poisson - room : 0;
		int additionalTasksNum = poisson > room ? room : poisson;

		addNewTasksToQueue( additionalTasksNum );
		overflowTasks += overflow;
	}

	private static int poissonDistribution() {
		double xp;
		int k = 0;
		xp = Math.random();
		while ( xp >= Math.exp( -1 * ADDITIONAL_TASK_NUM ) ){
			xp = xp * MyRandom.getRandomDouble();
			k = k + 1;
		}
		return ( k );
	}

	public static int getFinishedTasks() {
		return finishedTasks;
	}

	public static int getDisposedTasks() {
		return disposedTasks;
	}

	public static int getOverflowTasks() {
		return overflowTasks;
	}

	public static void reset() {
		finishedTasks = 0;
		disposedTasks = 0;
		overflowTasks = 0;
	}

	public static Task popTask() {
		if ( tasks.isEmpty() ) return null;
		Task temp = tasks.remove( 0 );
		return temp;
	}

	static public void disposeTask() {
		disposedTasks++;
	}

	public static void finishTask() {
		finishedTasks++;
	}

	public static void clear() {
		tasks.clear();
		finishedTasks = 0;
		disposedTasks = 0;
		overflowTasks = 0;
	}
}
