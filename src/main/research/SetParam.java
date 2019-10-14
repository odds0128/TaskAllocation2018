package main.research;

/**
 * @author Funato
 * @version 2.0
 */

public interface SetParam {
	// 結果表示のためのパラメータ
	int  COALITION_CHECK_SPAN = 50;          // 協調関係ができているか確認するための最後の方のターム
	double THRESHOLD_FOR_COALITION = 10;

	// トーラスを考えた時に，中心が自分であるほうが考えやすいので，一片の長さは奇数にする
	int MAX_X    = 51, MAX_Y = 51;
	int MAX_DELAY = 5;

	double ADDITIONAL_TASK_NUM  = 7.5;        // タスクを追加するタイミングで, タスクキューに追加するタスクの個数(=λ)
	int RESOURCE_TYPES = 3;

	int REDUNDANT_SOLICITATION_TIMES = 2;              // あるサブタスクについて要請を出すエージェントの数
	int THRESHOLD_FOR_ROLE_RENEWAL  = 50;

	// リソース推定にまつわる定数
	int OC_CACHE_TIME = 100;
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
