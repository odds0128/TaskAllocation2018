package main.research.task;

import main.research.SetParam;
import main.research.others.random.MyRandom;

import java.util.ArrayList;
import java.util.List;

public class Task implements SetParam {
    private static int _task_id = 0;

    private int id;
    public List<Subtask> subtasks = new ArrayList<>();

    Task( int minSubtask, int maxSubtask ){
        id = _task_id++;

        int subtaskNum = MyRandom.getRandomInt(minSubtask, maxSubtask);
        generateSubtasks( subtaskNum );
    }

    private void generateSubtasks( int subtaskNum ){
        for(int i = 0; i < subtaskNum; i++) {
            subtasks.add( new Subtask( this.id ) );
        }
        subtasks.sort(new Subtask.SubtaskRewardComparator());
    }

    public boolean contains( Subtask st ){
    	for( Subtask s : subtasks ) {
    	    if( st.equals( s ) ) {
    	        return true;
            }
        }
    	return false;
    }

    public int getId () {
        return id;
    }

    public static void clear(){
        _task_id = 0;
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("Task ").append( id ).append(" ( ");
        for (Subtask subtask : subtasks) str.append(subtask).append( " " );
        str.append("), ");
        return str.toString();
    }
}
