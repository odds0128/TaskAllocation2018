/**
 * @author Funato
 * @version 2.0
 */

interface SetParam {
    // 環境の設定( 変更していく部分 )
    int EXECUTION_TIMES = 1;             // 実験の回数
    int MAX_TURN_NUM    = 500000;         // 一回の実験のターン数
    int WRITING_TIMES   = 5000;           // データのファイルへの出力回数
    boolean CHECK_RELATIONSHIPS    = false;  // エージェント関係の協調関係を録るか
    boolean CHECK_INTERIM_RELATIONSHIPS    = false;  // エージェント関係の協調関係のスナップショットを録るか
    boolean CHECK_INITIATION       = false;  // エージェントやタスクの初期設定を確認するか
    boolean CHECK_RESULTS          = true;  // チーム編成成功数などを確認するか
    boolean CHECK_AGENTS           = true;   // エージェントの情報を確認するか
    boolean CHECK_Eleader_Emember  = false;

    boolean IS_HEAVY_TASKS_HAPPENS = false;
    int     START_HAPPENS          = 300000;
    int     BUSY_PERIOD            = 50000;

    String HOW_EPSILON = "constant";      // 定数:constant, 線形減少:linear, 指数減少:exponential で指定
//    String HOW_EPSILON = "linear";          // 定数:constant, 線形減少:linear, 指数減少:exponential で指定
//    String HOW_EPSILON = "exponential";   // 定数:constant, 線形減少:linear, 指数減少:exponential で指定
//    double INITIAL_ε  = 0.5;
    double INITIAL_ε  = 0.05;
    double FLOOR       = 0.05;
    double DIFFERENCE  = (INITIAL_ε - FLOOR)/(MAX_TURN_NUM * 0.9);
    double RATE        = 0.99996;   // rateはマジでちゃんと計算して気をつけて思ったより早く収束するから

    // 結果表示のためのパラメータ
    int  COALITION_CHECK_SPAN = 5000;          // 協調関係ができているか確認するための最後の方のターム
    double THRESHOLD_FOR_COALITION = 10;
    int THRESHOLD_FOR_NEET = 5000;
    double THRESHOLD_FOR_RECIPROCITY_RATE = 0.2;
    int SNAPSHOT_TIME = 200000;

    int INITIAL_TASK_NUM = 0;       // 最初のタスク数
    int TASK_QUEUE_SIZE  = 100;     // タスクキューのサイズ
    int AGENT_NUM = 500;            // エージェントの数
    int ROW    = 50;                // 行数
    int COLUMN = 50;                // 列数
    int MAX_DELAY = 5;

    double ADDITIONAL_TASK_NUM  = 10.0;        // タスクを追加するタイミングで, タスクキューに追加するタスクの個数(=λ)
    int RESOURCE_TYPES = 3;
    int MAX_AGENT_RESOURCE_SIZE   = 5;
    int MIN_AGENT_RESOURCE_SIZE   = 0;
    int MAX_SUBTASK_RESOURCE_SIZE = 10;
    int MIN_SUBTASK_RESOURCE_SIZE = 5;

    int RESEND_TIMES   = 2;              // あるサブタスクについて要請を出すエージェントの数
    int MAX_RELIABLE_AGENTS = 1;         // メンバの信頼エージェントの上限
//    int THRESHOLD_FOR_ROLE_RENEWAL  = 10;
//    int THRESHOLD_FOR_ROLE_RENEWAL  = 1;
//int THRESHOLD_FOR_ROLE_RENEWAL  = 100;
    int THRESHOLD_FOR_ROLE_RENEWAL  = 50;

    // パラメータ
    double INITIAL_VALUE_OF_DEC =  0.1;
    double INITIAL_VALUE_OF_DSL =  0.5;
    double INITIAL_VALUE_OF_DSM =  0.5;
    double α = 0.05;
    double γ_r = 0.000002;
    double THRESHOLD_FOR_ROLE_RECIPROCITY   = 0.2;
    double THRESHOLD_FOR_RECIPROCITY_FROM_LEADER = 1.5;
    int BIAS = 0;
    int UNIFORM = 1;
    int AREA_LIMIT = 100; // 近い方からn体のエージェントを知っている

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
