/**
 * @author Funato
 * @version 1.1
 */

interface SetParam {

    // 環境の設定
    int INITIAL_TASK_NUM = 10;
    int TASK_QUEUE_SIZE  = 50;
    int AGENT_NUM = 100;
    int ROW    = 10;
    int COLUMN = 10;
    int TURN_NUM = 1000;
    int TASK_ADD_TURN = 5;
    int RESOURCE_NUM  = 6;

    // タスク
    int BIAS    = 0;
    int UNIFORM = 1;
    boolean RESET = true;
    boolean CONT = false;

    // エージェント
    int LEADER = 1 ;
    int WAITING   = 0 ;
    int MEMBER = -1;
    int MAX_REL_AGENTS = 50 ;
    boolean EXECUTE = true;
    boolean BREAK_UP = false;

    // メンバ
    boolean ACCEPT = true;
    boolean REJECT = false;

    // フェイズ
    int SELECT_ROLE = -1;
    int NEGOTIATION = 0;
    int EXECUTION = 1;
}
