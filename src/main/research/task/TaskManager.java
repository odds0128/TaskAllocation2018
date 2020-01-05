package main.research.task;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.Manager;
import main.research.agent.Agent;
import main.research.others.random.MyRandom;

public class TaskManager {
	private static int initial_tasks_num_;
	private static double additional_tasks_num_;
	private static int task_queue_size_;
	private static int min_subtasks_num_;
	private static int max_subtasks_num_;

	private static List< Task > tasks = new ArrayList<>();
	private static int disposedTasks = 0;
	private static int overflowTasks = 0;
	private static int finishedTasks = 0;

	public static void setConstants( JsonNode parameterNode ) {
		initial_tasks_num_   = parameterNode.get( "initial_tasks_num" ).asInt();
		additional_tasks_num_ = parameterNode.get( "additional_tasks_num" ).asDouble();
		task_queue_size_     = parameterNode.get( "task_queue_size" ).asInt();
		min_subtasks_num_    = parameterNode.get( "min_subtasks_num" ).asInt();
		max_subtasks_num_    = parameterNode.get( "max_subtasks_num" ).asInt();
		Subtask.setConstants( parameterNode.get( "subtask" ) );
	}

	public static void addInitialTasksToQueue() {
		for ( int i = 0; i < initial_tasks_num_; i++ ) tasks.add( new Task( min_subtasks_num_, max_subtasks_num_ ) );
	}

	public static void addNewTasksToQueue() {
		int room = task_queue_size_ - tasks.size();    // タスクキューの空き
		int poisson = poissonDistribution();
		int overflow = poisson > room ? poisson - room : 0;
		int additionalTasksNum = Math.min( poisson, room );

		for ( int i = 0; i < additionalTasksNum; i++ ) tasks.add( new Task( min_subtasks_num_, max_subtasks_num_ ) );
		overflowTasks += overflow;
	}

	private static int poissonDistribution() {
		double xp;
		int k = 0;
		xp = MyRandom.getRandomDouble();
		while ( xp >= Math.exp( -1 * additional_tasks_num_ ) ){
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

	public static void forget() {
		finishedTasks = 0;
		disposedTasks = 0;
		overflowTasks = 0;
	}

	public static Task popTask() {
		if ( tasks.isEmpty() ) return null;
		Task temp = tasks.remove( 0 );
		return temp;
	}

	public static Set< Agent > badLeaders = new HashSet<>(  );
	static public void disposeTask( Agent leader ) {
		if( ! badLeaders.contains( leader ) ) {
			badLeaders.add(leader);
		}
		disposedTasks++;
	}

	public static Map<Agent, Integer> goodLeaders = new LinkedHashMap<>(  );
	public static void finishTask( Agent leader ) {
		goodLeaders.merge( leader, 1, Integer::sum);
		finishedTasks++;
	}

	public static void clearLeadersInfo() {
		badLeaders.clear();
		goodLeaders.clear();
	}

	public static double getAdditional_tasks_num_() {
		return additional_tasks_num_;
	}

	public static void clear() {
		tasks.clear();
		finishedTasks = 0;
		disposedTasks = 0;
		overflowTasks = 0;
	}
}
