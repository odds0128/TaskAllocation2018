package main.research.task;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.random.MyRandom;

import java.util.ArrayList;
import java.util.List;

public class Task implements SetParam {
    private static int _task_id = 0;

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
        subtasks.sort(new Subtask.SubtaskRewardComparator());
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
        StringBuilder str = new StringBuilder();
        str.append("Task ").append(task_id).append("(Subtasks :").append(subtasks.size()).append(" [");
        for (Subtask subtask : subtasks) str.append(subtask);
        str.append("] ) ");
        return str.toString();
    }
}
