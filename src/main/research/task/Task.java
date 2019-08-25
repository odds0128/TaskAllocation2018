package main.research.task;

import main.research.Manager;
import main.research.SetParam;
import main.research.agent.Agent;
import main.research.others.random.MyRandom;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static main.research.Manager.getCurrentTime;
import static main.research.others.random.MyRandom.getRandomDouble;

public class Task implements SetParam {
    private static int _task_id = 0;
    private static Queue<Task> taskQueue;
    private static int disposedTasks = 0;
    private static int overflowTasks = 0;
    private static int finishedTasks = 0;

    private int task_id;
    public List<Subtask> subtasks = new ArrayList<>();
    private int deadline;

    public static void initiateTaskQueue() {
        taskQueue = new LinkedList<>();
        for (int i = 0; i < INITIAL_TASK_NUM; i++) {
            taskQueue.add(new Task( MIN_SUBTASK_NUM, MAX_SUBTASK_NUM, MIN_DEADLINE, MAX_DEADLINE ) );
        }

    }

    public Task( int minSubtask, int maxSubtask , int minDeadline, int maxDeadline ){
        task_id = _task_id++;

        int subtaskNum = MyRandom.getRandomInt(minSubtask, maxSubtask);
        generateSubtasks( subtaskNum );
        deadline = setDeadline(minDeadline, maxDeadline);
    }

	public static void addNewTasksToQueue() {
        int room = TASK_QUEUE_SIZE - taskQueue.size();    // タスクキューの空き
        double decimalPart = ADDITIONAL_TASK_NUM % 1;
        int additionalTasksNum = decideHowManyAdditionalTask( decimalPart );

        if (START_HAPPENS <= getCurrentTime() && getCurrentTime() < START_HAPPENS + BUSY_PERIOD && IS_MORE_TASKS_HAPPENS) {
            additionalTasksNum += HOW_MANY;
        }

        if (additionalTasksNum <= room) {
            if (START_HAPPENS <= getCurrentTime() && getCurrentTime() < START_HAPPENS + BUSY_PERIOD && IS_HEAVY_TASKS_HAPPENS) {
                for (int i = 0; i < additionalTasksNum; i++) taskQueue.add( new Task( 8, 11, MIN_DEADLINE, MAX_DEADLINE ) );
            }else{
                for (int i = 0; i < additionalTasksNum; i++) taskQueue.add( new Task( MIN_SUBTASK_NUM, MAX_SUBTASK_NUM, MIN_DEADLINE, MAX_DEADLINE ) );
            }
        }
        // タスクキューからタスクがはみ出そうなら, 入れるだけ入れてはみ出る分はオーバーフローとする
        else {
            int i;
            if (START_HAPPENS <= getCurrentTime() && getCurrentTime() < START_HAPPENS + BUSY_PERIOD && IS_HEAVY_TASKS_HAPPENS) {
                for (i = 0; i < room; i++) taskQueue.add( new Task( 8, 11, MIN_DEADLINE, MAX_DEADLINE ) );
            } else {
                for (i = 0; i < room; i++) taskQueue.add( new Task( MIN_SUBTASK_NUM, MAX_SUBTASK_NUM, MIN_DEADLINE, MAX_DEADLINE ) );
            }
            overflowTasks += additionalTasksNum - i;
        }
    }

    private static int decideHowManyAdditionalTask( double additionalRate ){
        if ( additionalRate != 0) {
            double random = getRandomDouble();
            if ( random < additionalRate ) {
                return ( int ) (Math.floor( ADDITIONAL_TASK_NUM ) + 1);
            }
        }
        return (int) (Math.floor( ADDITIONAL_TASK_NUM ));
    }

    public static int getFinishedTasks() {
        return finishedTasks;
    }

    public static void setFinishedTasks( int finishedTasks ) {
        Task.finishedTasks = finishedTasks;
    }

    public static int getDisposedTasks() {
        return disposedTasks;
    }

    public static void setDisposedTasks( int disposedTasks ) {
        Task.disposedTasks = disposedTasks;
    }

    public static int getOverflowTasks() {
        return overflowTasks;
    }

    public static void setOverflowTasks( int overflowTasks ) {
        Task.overflowTasks = overflowTasks;
    }

    // taskQueueにあるタスクをリーダーに渡すメソッド
    public static Task getTask() {
        Task temp = taskQueue.poll();
        return temp;
    }

    static public void disposeTask( ) {
        disposedTasks++;
    }

    public static void finishTask(  ) {
        finishedTasks++;
    }

    private void generateSubtasks( int subtaskNum ){
        for(int i = 0; i < subtaskNum; i++) {
            subtasks.add( new Subtask() );
        }
        subtasks.sort(new Subtask.SubtaskRewardComparator());
    }

    private int setDeadline( int min, int max ) {
        return MyRandom.getRandomInt(min, max);
    }

    public boolean isPartOfThisTask( Subtask st ){
    	return subtasks.contains( st );
    }

    public int getDeadline() {
        return deadline;
    }

    public static void clear(){
        _task_id = 0;
        taskQueue.clear();
        disposedTasks = 0;
        finishedTasks = 0;
        overflowTasks = 0;
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("Task ").append(task_id).append(" ( ");
        for (Subtask subtask : subtasks) str.append(subtask).append( " " );
        str.append("), ");
        return str.toString();
    }
}
