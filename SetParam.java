/**
 * @author Funato
 * @version 2.0
 */

interface SetParam {
    // 環境の設定( 変更していく部分 )
    int EXECUTE_NUM = 20;            // 実験の回数
    int TURN_NUM    = 1000000;       // 一回の実験のターン数
    int WRITE_NUM   = 1000;         // データのファイルへの出力回数

    // 結果表示のためのパラメータ
    int LAST_PERIOD = 500;          // 協調関係ができているか確認するための最後の方のターム
    double THRESHOLD_TASK_SUCCESSES = 30;
    int THRESHOLD_NEET = 100000;

    int INITIAL_TASK_NUM = 0;       // 最初のタスク数
    int TASK_QUEUE_SIZE  = 500;     // タスクキューのサイズ. だけど現状だと意味がない
    int AGENT_NUM = 500;            // エージェントの数
    int ROW    = 50;                // 行数
    int COLUMN = 50;                // 列数

    int TASK_ADD_TURN = 1;          // タスクキューにタスクを追加するスパン
    int TASK_ADD_NUM  = 10;        // タスクを追加するタイミングで, タスクキューに追加するタスクの個数
    int RESOURCE_NUM = 6;
    int MAX_AG_RES = 1;
    int MAX_ST_RES = 1;
    int MIN_ST_RES = 0;

    int RESEND_TIMES   = 2;         // あるサブタスクについて要請を出すエージェントの数
    int MAX_REL_AGENTS = 5;         // 信頼エージェントの数
    int RENEW_ROLE_TICKS = 100;
    int HISTORY_SIZE = 50;

    // パラメータ
    double INITIAL_VALUE_OF_DEC =  0.1;
    double INITIAL_VALUE_OF_DSL =  0.5;
    double INITIAL_VALUE_OF_DSM =  0.5;
    double α = 0.05;
    double ε = 0.01;
    double γ_r = 0.00005;
    double THRESHOLD_DEPENDABILITY = 0.5;
    double THRESHOLD_RECIPROCITY   = 0.5;
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
