package main.research;

/**
 * @author Funato
 * @version 2.0
 */

public interface SetParam {
	// トーラスを考えた時に，中心が自分であるほうが考えやすいので，一片の長さは奇数にする
	int MAX_X    = 51, MAX_Y = 51;
	int MAX_DELAY = 5;

	int REDUNDANT_SOLICITATION_TIMES = 2;              // あるサブタスクについて要請を出すエージェントの数
	int THRESHOLD_FOR_ROLE_RENEWAL  = 50;

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
