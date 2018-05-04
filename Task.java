/**
 * @author Funato
 * @version 2.0
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Task implements SetParam{
    private static int _task_id = 0;
    private static long _seed;
    private static Random _randSeed ;
    private int task_id;
    static int totalSubTasks = 0;
    static int totalSubtaskNum = 0;
    boolean flag = false;
    List<SubTask> subTasks = new ArrayList<>();
    public int subTaskNum ;
    int fromBirth = 0;
    int fromPicked = 0;

    /**
     * コンストラクタ
     * タームの最初のタスク生成時に呼び出される
     * パラメータで指定されたseedのサブタスクをランダムに生成する.
     */
    Task( long seed ){
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
        System.out.println(subTasks);
    }

    /**
     * コンストラクタ
     * 残りのタスクを生成する.
     */
    Task( ){
        this.task_id = _task_id;
        _task_id++;
        this.subTaskNum = _randSeed.nextInt(4) + 3;
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
        System.out.println(subTasks);
    }

    static void setSeed(long seed){
        _seed = seed;
        _randSeed = new Random(_seed);
    }
    static void clearT(){
        _task_id = 0;
    }

    @Override
    public String toString(){
        String sep = System.getProperty("line.separator");

        StringBuilder str = new StringBuilder();
        str.append("Task " + task_id + "(Subtasks :" + subTaskNum +" [");
        for( int i = 0 ; i < subTasks.size() ; i++ ) str.append(subTasks.get(i));
        str.append("] ) ");
        return str.toString();
    }
}
