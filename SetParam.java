/**
 * @author Funato
 * @version 1.0
 */

interface SetParam {

    // 環境の設定
    public static final int TASK_QUEUE_SIZE  = 5;
    public static final int AGENT_NUM = 500;
    public static final int ROW    = 3;
    public static final int COLUMN = 4;
    public static final int TURN_NUM = 100;
    public static final int ADD_TASK_PER_TURN = 5;
    public static final int RESOURSE_NUM  = 6;

    // タスク
    public static final int BIAS    = 0;
    public static final int UNIFORM = 1;
    public static final boolean RESET = true;
    public static final boolean CONT = false;

    // エージェント
    public static final int LEADER = 1 ;
    public static final int WAIT   = 0 ;
    public static final int MEMBER = -1;
    public static final int MAX_REL_AGENTS = 5 ;
}
