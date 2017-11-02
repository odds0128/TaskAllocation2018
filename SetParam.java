/**
 * @author Funato
 * @version 2.0
 */

interface SetParam {
    // 環境の設定
    int EXECUTE_NUM = 1;
    int INITIAL_TASK_NUM = 300;
    int TASK_QUEUE_SIZE  = 1000;
    int AGENT_NUM = 80;
    int ROW    = 10;
    int COLUMN = 10;
    int TURN_NUM = 10000;
    int TASK_ADD_TURN = 10000000;

    // パラメータ
    double INITIAL_VALUE_OF_DEC =  0.1;
    double INITIAL_VALUE_OF_DSL =  0.5;
    double INITIAL_VALUE_OF_DSM =  0.5;
    double LEARNING_RATE        =  0.01;
    double ε = 0.01;
    double γ = 0.00005;
    double T_D = 0.5;
    double THRESHOLD_DEPENDABILITY = 0.5;
    double THRESHOLD_RECIPROCITY   = 0.5;

    // タスク
    boolean RESET = true;
    boolean CONT = false;
    boolean PROCESSING = true;

    // エージェント
    int LEADER = 1 ;
    int JONE_DOE   = 0 ;
    int MEMBER = -1;
    int MAX_REL_AGENTS = 20   ;
    boolean SUCCESS  = true;
    boolean FAILURE  = false;

    boolean ACCEPT = true;
    boolean REJECT = false;

    // メッセージ
    int PROPOSAL = 2;
    int REPLY    = 3;
    int CHARGE   = 5;
    int RESULT   = 7;

    // フェイズ
    int SELECT_ROLE  = 0;  // 全員
    int PROPOSITION  = 1;  // リーダー
    int WAITING      = 2;  // メンバ
    int REPORT       = 3;  // リーダー
    int RECEPTION    = 4;  // メンバ
    int EXECUTION    = 5;  // メンバ
}
