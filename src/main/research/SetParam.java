package main.research;

/**
 * @author Funato
 * @version 2.0
 */

public interface SetParam {
	// 結果表示のためのパラメータ
	int  COALITION_CHECK_SPAN = 50;          // 協調関係ができているか確認するための最後の方のターム
	double THRESHOLD_FOR_COALITION = 10;
	int SNAPSHOT_TIME = 200000;

	int INITIAL_TASK_NUM = 0;       // 最初のタスク数
	int TASK_QUEUE_SIZE  = 500;     // タスクキューのサイズ

	// トーラスを考えた時に，中心が自分であるほうが考えやすいので，一片の長さは奇数にする
	int MAX_X    = 51, MAX_Y = 51;
	int MAX_DELAY = 5;

	double ADDITIONAL_TASK_NUM  = 7.5;        // タスクを追加するタイミングで, タスクキューに追加するタスクの個数(=λ)
	int RESOURCE_TYPES = 3;
	int MAX_AGENT_RESOURCE_SIZE   = 5;
	int MIN_AGENT_RESOURCE_SIZE   = 0;
	int MAX_SUBTASK_RESOURCE_SIZE = 10;
	int MIN_SUBTASK_RESOURCE_SIZE = 5;

	int REBUNDUNT_SOLICITATION_TIMES = 2;              // あるサブタスクについて要請を出すエージェントの数
	int MAX_RELIABLE_AGENTS = 1;         // メンバの信頼エージェントの上限
	int THRESHOLD_FOR_ROLE_RENEWAL  = 50;


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
