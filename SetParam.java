/**
 * @author Funato
 * @version 2.0
 */

interface SetParam {
    // 環境の設定( 変更していく部分 )
    int EXECUTION_TIMES = 1;            // 実験の回数
    int MAX_TURN_NUM    = 1000000;       // 一回の実験のターン数
    int WRITING_TIMES   = 1000;         // データのファイルへの出力回数

    // 結果表示のためのパラメータ
    int  FINAL_TERM = 5000;          // 協調関係ができているか確認するための最後の方のターム
    double THRESHOLD_FOR_COALITION = 10;
    int THRESHOLD_FOR_NEET = 100000;
    int SNAPSHOT_TIME = 500000;

    int INITIAL_TASK_NUM = 0;       // 最初のタスク数
    int TASK_QUEUE_SIZE  = 500;     // タスクキューのサイズ. だけど現状だと意味がない
    int AGENT_NUM = 500;            // エージェントの数
    int ROW    = 50;                // 行数
    int COLUMN = 50;                // 列数

    int TASK_ADDITION_SPAN = 1;          // タスクキューにタスクを追加するスパン
    int ADDITIONAL_TASK_NUM  = 10;        // タスクを追加するタイミングで, タスクキューに追加するタスクの個数
    int RESOURCE_TYPES = 6;
    int MAX_AGENT_RESOURCE_SIZE = 1;
    int MAX_SUBTASK_RESOURSE_SIZE = 1;
    int MIN_SUBTASK_RESOURSE_SIZE = 0;

    int RESEND_TIMES   = 2;         // あるサブタスクについて要請を出すエージェントの数
    int MAX_RELIABLE_AGENTS = 5;         // 信頼エージェントの数
    int ROLE_RENEWAL_TICKS = 100;

    // パラメータ
    double INITIAL_VALUE_OF_DEC =  0.1;
    double INITIAL_VALUE_OF_DSL =  0.5;
    double INITIAL_VALUE_OF_DSM =  0.5;
    double α = 0.05;
    double ε = 0.01;
    double γ_r = INITIAL_VALUE_OF_DEC/(double)MAX_TURN_NUM;
    double THRESHOLD_FOR_DEPENDABILITY = 0.3;
    double THRESHOLD_FOR_RECIPROCITY   = 0.5;
    int BIAS = 0;
    int UNIFORM = 1;

    // タスク
    boolean RESET = true;
    boolean CONT  = false;
    boolean PROCESSING = true;

    // エージェント
    int LEADER = 1 ;
    int JONE_DOE   = 0 ;
    int MEMBER = -1;
    int RATIONAL = 0;
    int RECIPROCAL = 1;
    boolean ACCEPT = true;
    boolean REJECT = false;

    // メッセージ
    int PROPOSAL   = 1;
    int REPLY      = 2;
    int RESULT     = 3;

    // フェイズ
    int SELECT_ROLE  = 0;  // 全員
    int PROPOSITION  = 1;  // リーダー
    int WAITING      = 2;  // メンバ
    int REPORT       = 3;  // リーダー
    int RECEPTION    = 4;  // メンバ
    int EXECUTION    = 5;  // 全員
}
