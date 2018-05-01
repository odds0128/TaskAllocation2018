/**
 * @author Funato
 * @version 2.0
 */

interface SetParam {
    // 環境の設定( 変更していく部分 )
    int EXECUTION_TIMES = 10;             // 実験の回数
    int MAX_TURN_NUM    = 1000000;         // 一回の実験のターン数
    int WRITING_TIMES   = 100000;           // データのファイルへの出力回数
    boolean CHECK_RELATIONSHIPS = false;  // エージェント関係の協調関係を録るか
    boolean CHECK_INITIATION    = false;  // エージェントやタスクの初期設定を確認するか
    boolean CHECK_RESULTS       = false;  // チーム編成成功数などを確認するか
    boolean CHECK_AGENTS        = true;   // エージェントの情報を確認するか

    // 結果表示のためのパラメータ
    int  COALITION_CHECK_SPAN = 5000;          // 協調関係ができているか確認するための最後の方のターム
    double THRESHOLD_FOR_COALITION = 5;
    int THRESHOLD_FOR_NEET = 5000;
    int SNAPSHOT_TIME = MAX_TURN_NUM;

    int INITIAL_TASK_NUM = 0;       // 最初のタスク数
    int TASK_QUEUE_SIZE  = 500;     // タスクキューのサイズ
    int AGENT_NUM = 500;            // エージェントの数
    int ROW    = 50;                // 行数
    int COLUMN = 50;                // 列数
    int MAX_DELAY = 10;

    int TASK_ADDITION_SPAN = 1;          // タスクキューにタスクを追加するスパン
    int ADDITIONAL_TASK_NUM  = 1;        // タスクを追加するタイミングで, タスクキューに追加するタスクの個数(=λ)
    int RESOURCE_TYPES = 3;
    int MAX_AGENT_RESOURCE_SIZE   = 5;
    int MIN_AGENT_RESOURCE_SIZE   = 0;
    int MAX_SUBTASK_RESOURCE_SIZE = 100;
    int MIN_SUBTASK_RESOURCE_SIZE = 50;

    int RESEND_TIMES   = 2;              // あるサブタスクについて要請を出すエージェントの数
    int MAX_RELIABLE_AGENTS = 5;         // 信頼エージェントの数
    int ROLE_RENEWAL_TICKS = 100;

    // パラメータ
    double INITIAL_VALUE_OF_DEC =  0.1;
    double INITIAL_VALUE_OF_DSL =  0.5;
    double INITIAL_VALUE_OF_DSM =  0.5;
    double α = 0.05;
    double ε = 0.05;
    double γ_r = INITIAL_VALUE_OF_DEC/(double)MAX_TURN_NUM;
    double THRESHOLD_FOR_DEPENDABILITY = 0.5;
    double THRESHOLD_FOR_RECIPROCITY   = 0.5;
    int BIAS = 0;
    int UNIFORM = 1;

    // タスク
    boolean RESET = true;
    boolean CONT  = false;
    boolean PROCESSING = true;

    // エージェント
    int LEADER     = 1    ;
    int JONE_DOE   = 0    ;
    int MEMBER     = -1   ;
    int RATIONAL   = 0    ;
    int RECIPROCAL = 1    ;

    // メッセージの種類
    int PROPOSAL   = 1;
    int REPLY      = 2;
    int RESULT     = 3;
    int DONE       = 4;

    // CNP用のメッセージの種類
    int PUBLICITY     = 1;
    int BIDDINGorNOT  = 2;
    int BID_RESULT    = 3;
//    int DONE          = 4;

    // REPLYの種類
    int ACCEPT = 1 ;
    int REJECT = 0 ;
    int REJECT_FOR_DOING_YOUR_ST = -1 ;


    // フェイズ
    int SELECT_ROLE  = 0;  // 全員
    int PROPOSITION  = 1;  // リーダー
    int WAITING      = 2;  // メンバ
    int REPORT       = 3;  // リーダー
    int RECEPTION    = 4;  // メンバ
    int EXECUTION    = 5;  // 全員

    int PHASE0  = 0;
    int lPHASE1 = 1;
    int mPHASE1 = 2;
    int lPHASE2 = 3;
    int mPHASE2 = 4;
    int PHASE3  = 5;
}
