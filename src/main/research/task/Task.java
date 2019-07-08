package main.research.task; /**
 * @author Funato
 * @version 2.0
 */

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.random.MyRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Task implements SetParam {
    public static int _task_id = 0;

    public int task_id;
    public List<Subtask> subtasks = new ArrayList<>();
    public int deadline = 0;

    public Task( int minSubtask, int maxSubtask ){
        task_id = _task_id++;

        int subtaskNum = MyRandom.getRandomInt(minSubtask, maxSubtask);
        generateSubtasks( subtaskNum );
    }

    /**
     * generateSubtasksメソッド
     * サブタスクを指定数作成する.
     * @param subtaskNum ... 生成するサブタスクの個数
     */
    private void generateSubtasks( int subtaskNum ){
        for(int i = 0; i < subtaskNum; i++) {
            subtasks.add( new Subtask(CONT) );
        }
        subtasks.add( new Subtask( RESET ) );
        Collections.sort( subtasks, new SubtaskRewardComparator() );
    }

    public void setFrom(Agent agent){
        for(Subtask st: subtasks){
            st.setFrom(agent);
        }
    }

    public static void clearT(){
        _task_id = 0;
    }

    @Override
    public String toString(){
        String sep = System.getProperty("line.separator");
        StringBuilder str = new StringBuilder();
        str.append("Task " + task_id + "(Subtasks :" + subtasks.size() +" [");
        for( int i = 0 ; i < subtasks.size() ; i++ ) str.append(subtasks.get(i));
        str.append("] ) ");
        return str.toString();
    }
}
