package main.research;

/**
 * @author Funato
 * @version 2.0
 */

public interface SetParam {
	// 環境の設定( 変更していく部分 )
	int EXECUTION_TIMES = 1;             // 実験の回数
	int MAX_TURN_NUM    = 100000;         // 一回の実験のターン数
	int WRITING_TIMES   = 1000;           // データのファイルへの出力回数
	boolean CHECK_RELATIONSHIPS    = false;  // エージェント関係の協調関係を録るか
	boolean CHECK_INTERIM_RELATIONSHIPS    = false;  // エージェント関係の協調関係のスナップショットを録るか
	boolean CHECK_INITIATION       = false;  // エージェントやタスクの初期設定を確認するか
	boolean CHECK_RESULTS          = true;  // チーム編成成功数などを確認するか
	boolean CHECK_AGENTS           = false;   // エージェントの情報を確認するか
	boolean CHECK_Eleader_Emember  = false;

	boolean IS_MORE_TASKS_HAPPENS =  false;
	boolean IS_HEAVY_TASKS_HAPPENS = false;
	int     START_HAPPENS          = 200000;
	int     BUSY_PERIOD            = 100000;

	String HOW_EPSILON = "constant";      // 定数:constant, 線形減少:linear, 指数減少:exponential で指定
	//    String HOW_EPSILON = "linear";          // 定数:constant, 線形減少:linear, 指数減少:exponential で指定
//    String HOW_EPSILON = "exponential";   // 定数:constant, 線形減少:linear, 指数減少:exponential で指定
//    double INITIAL_ε  = 0.5;
	double INITIAL_ε  = 0.05;
	double FLOOR       = 0.05;
	double DIFFERENCE  = (INITIAL_ε - FLOOR)/(MAX_TURN_NUM * 0.9);
	double RATE        = 0.99996;   // rateはマジでちゃんと計算して気をつけて思ったより早く収束するから

	// 結果表示のためのパラメータ
	int  COALITION_CHECK_SPAN = 50;          // 協調関係ができているか確認するための最後の方のターム
	double THRESHOLD_FOR_COALITION = 10;
	int THRESHOLD_FOR_NEET = 5000;
	double THRESHOLD_FOR_RECIPROCITY_RATE = 0.7;
	int SNAPSHOT_TIME = 200000;

	int INITIAL_TASK_NUM = 0;       // 最初のタスク数
	int TASK_QUEUE_SIZE  = 500;     // タスクキューのサイズ
	int AGENT_NUM = 500;            // エージェントの数

	// トーラスを考えた時に，中心が自分であるほうが考えやすいので，一片の長さは奇数にする
	int MAX_X    = 51, MAX_Y = 51;
	int MAX_DELAY = 5;

	double ADDITIONAL_TASK_NUM  = 7.5;        // タスクを追加するタイミングで, タスクキューに追加するタスクの個数(=λ)
	double  HOW_MANY            = 0.5 * ADDITIONAL_TASK_NUM;
	int RESOURCE_TYPES = 3;
	int MAX_AGENT_RESOURCE_SIZE   = 5;
	int MIN_AGENT_RESOURCE_SIZE   = 0;
	int MAX_SUBTASK_RESOURCE_SIZE = 10;
	int MIN_SUBTASK_RESOURCE_SIZE = 5;

	int REBUNDUNT_SOLICITATION_TIMES = 2;              // あるサブタスクについて要請を出すエージェントの数
	int MAX_RELIABLE_AGENTS = 1;         // メンバの信頼エージェントの上限
	int THRESHOLD_FOR_ROLE_RENEWAL  = 50;

	// パラメータ
	double INITIAL_VALUE_OF_DE =  0.0;
	double INITIAL_VALUE_OF_DSL =  0.5;
	double INITIAL_VALUE_OF_DSM =  0.5;
	double α = 0.05;
	double γ_r = 0.00005;
	double THRESHOLD_FOR_RECIPROCITY_FROM_LEADER = 1.5;

	int MIN_DEADLINE = 20;
	int MAX_DEADLINE = 80;

	int MIN_SUBTASK_NUM = 3;
	int MAX_SUBTASK_NUM = 6;

	// リソース推定にまつわる定数
	int CD_CACHE_TIME = 100;
	int SUBTASK_QUEUE_SIZE = 5;

	// エージェント
	enum Role {
		LEADER,
		JONE_DOE,
		MEMBER
	}

	enum Principle {
		RATIONAL,
		RECIPROCAL
	}

	// REPLYの種類
	enum ReplyType {
		ACCEPT ,
		DECLINE,
		REJECT_FOR_DOING_YOUR_ST
	}

	// RESULTの種類
	enum ResultType {
		SUCCESS,
		FAILURE
	}

	// フェイズ
	enum Phase {
		SELECT_ROLE ,              //全員
		SOLICIT,                   // リーダー
		WAIT_FOR_SOLICITATION,     // メンバ
		FORM_TEAM,                 // リーダー
		WAIT_FOR_SUBTASK,          // メンバ
		EXECUTE_SUBTASK            // メンバ
	}

	enum DERenewalStrategy {
		withBinary,
		withReward
	}
}
