package main.research.task; /**
 * @author Funato
 * @version 2.0
 */

import main.research.SetParam;
import main.research.agent.Agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Task implements SetParam {
    private static int _task_id = 0;
    private static long _seed;
    private static Random _randSeed ;
    public int task_id;
    static int totalSubTasks = 0;
    static int totalSubtaskNum = 0;
    public boolean flag = false;
    public List<SubTask> subTasks = new ArrayList<>();
    public int subTaskNum ;
    public int deadline = 0;
    int fromBirth = 0;
    int fromPicked = 0;

    /**
     * コンストラクタ
     * タームの最初のタスク生成時に呼び出される
     * パラメータで指定されたseedのサブタスクをランダムに生成する.
     */
    public Task(long seed){
        this.task_id = _task_id;
        _task_id++;
        setSeed(seed);
        this.subTaskNum = _randSeed.nextInt(4) + 3;
        totalSubtaskNum += subTaskNum;
        totalSubTasks++;
        setSubTasks(seed);
    }

    /**
     * setSubTasksメソッド
     * パラメータで指定されたseedのサブタスクを指定数作成する.
     * @param seed
     */
    private void setSubTasks(long seed){
        subTasks.add(new SubTask( RESET, seed) );
        for(int i = 1; i < subTaskNum; i++) subTasks.add( new SubTask(CONT) );
        Collections.sort(subTasks, new SubtaskRewardComparator() );
//        System.out.println(subTasks);
    }

    /**
     * コンストラクタ
     * 残りのタスクを生成する.
     */
    public Task( String heavy ){
        this.task_id = _task_id;
        _task_id++;
        if( heavy == "HEAVY" ) {
            this.subTaskNum = _randSeed.nextInt(3) + 8;
        }else{
            this.subTaskNum = _randSeed.nextInt(4) + 3;
        }
        totalSubtaskNum += subTaskNum;
        totalSubTasks++;
        setSubTasks();
    }
    /**
     * setSubTasksメソッド
     * 残りのサブタスクを指定数作成する.
     * @param
     */
    private void setSubTasks(){
        subTasks.add(new SubTask( RESET ) );
        for(int i = 1; i < subTaskNum; i++) subTasks.add( new SubTask(CONT) );
        Collections.sort(subTasks, new SubtaskRewardComparator() );
//        System.out.println(subTasks);
    }

    public void setFrom(Agent agent){
        for(SubTask st: subTasks){
            st.setFrom(agent);
        }
    }

    static void setSeed(long seed){
        _seed = seed;
        _randSeed = new Random(_seed);
    }
    public static void clearT(){
        _task_id = 0;
    }

    @Override
    public String toString(){
        String sep = System.getProperty("line.separator");

        StringBuilder str = new StringBuilder();
        str.append("main.research.task.Task " + task_id + "(Subtasks :" + subTaskNum +" [");
        for( int i = 0 ; i < subTasks.size() ; i++ ) str.append(subTasks.get(i));
        str.append("] ) ");
        return str.toString();
    }
}
