package main.research.agent.strategy.ProposedStrategy;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.agent.strategy.LeaderStrategy;
import main.research.communication.TransmissionPath;
import main.research.communication.message.*;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.DERenewalStrategy.*;
import static main.research.SetParam.ReplyType.*;
import static main.research.SetParam.ResultType.FAILURE;
import static main.research.SetParam.ResultType.SUCCESS;
import static main.research.agent.strategy.Strategy.*;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class ProposedStrategy_l extends LeaderStrategy implements SetParam {
	List< TeamHistoryCache > teamHistoryCache = new ArrayList<>();
	Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	Map< Agent, double[] > congestionDegreeMap = new HashMap<>();

	protected void solicitAsL( Agent leader ) {
		leader.myTask = Manager.getTask( leader );
		if ( leader.myTask == null ) { leader.inactivate( 0 ); return; }

		List<Agent> candidates = selectMembers( leader, leader.myTask.subtasks );
		repliesToCome = candidates.size();

		if ( candidates.isEmpty() ) {
			leader.inactivate( 0 );
			return;
		} else {
			sendSolicitations( leader, candidates );
		}
		proceedToNextPhase(leader);  // 次のフェイズへ
	}

	private void sendSolicitations( Agent leader, List<Agent> targets ){
		for ( int i = 0; i < targets.size(); i++ ) {
			Agent target = targets.get( i );
			if ( target != null ) {
				timeToStartCommunicatingMap.put( target, getCurrentTime() );
				// TODO: 長すぎ
				Message solicitation = new Solicitation( leader, target, leader.myTask.subtasks.get( i % leader.myTask.subtasks.size() ) );
				TransmissionPath.sendMessage( solicitation );
			}
		}
	}

	// HACK: 長すぎ
	protected void formTeamAsL( Agent leader ) {
		if ( replyList.size() < repliesToCome ) return;
		else repliesToCome = 0;

		// 拒否のやつの評価を下げる．受理の奴らだけで全ての
		while ( ! replyList.isEmpty() ) {
			ReplyToSolicitation reply = replyList.remove( 0 );
			Agent betrayer = reply.getFrom();

			if ( reply.getReplyType() != ACCEPT ) {
				exceptions.remove( betrayer );
				int i = leader.inTheList( betrayer, agentsNegotiatingWith );
				agentsNegotiatingWith.set( i, null );
				renewDE( leader.reliabilityRankingAsL, betrayer, 0, withBinary );
			}
			// hack: 現状リーダーは全員からの返信をもらってから割り当てを開始するため，早くに返信が到着したエージェントとの総通信時間が見かけ上長くなってしまう．
			// だからここではそれを訂正するために，その差分をroundTripTimeの一部として足し合わせることで混雑度の計算が狂わないようにしている
			int gap = getCurrentTime() - timeToStartCommunicatingMap.get( betrayer ) - roundTripTimeMap.get( betrayer );
			assert gap % 2 == 0 : "gap is odd.";
			int modifiedRoundTripTime = roundTripTimeMap.get( betrayer ) + gap / 2;
			roundTripTimeMap.put( betrayer, modifiedRoundTripTime );
		}

		Agent A, B;
		List<Agent> teamMembers = new ArrayList<>(  );
		Map< Agent, Subtask > preAllocations = new HashMap<>();
		// if 全candidatesから返信が返ってきてタスクが実行可能なら割り当てを考えていく
		for ( int indexA = 0, indexB = leader.myTask.subtasks.size(); indexA < leader.myTask.subtasks.size(); indexA++, indexB++ ) {
			A = agentsNegotiatingWith.get( indexA );
			B = agentsNegotiatingWith.get( indexB );
			// もし両方から受理が返ってきたら, 信頼度の高い方に割り当てる
			if ( A != null && B != null ) {
				// Bの方がAより信頼度が高い場合
				if ( leader.reliabilityRankingAsL.get( A ) < leader.reliabilityRankingAsL.get( B ) ) {
					preAllocations.put( B, leader.myTask.subtasks.get( indexA ) );
					exceptions.remove( A );
					TransmissionPath.sendMessage( new ResultOfTeamFormation( leader, A, FAILURE, null ) );
					leader.teamMembers.add( B );
				}
				// Aの方がBより信頼度が高い場合
				else {
					preAllocations.put( A, leader.myTask.subtasks.get( indexA ) );
					exceptions.remove( B );
					TransmissionPath.sendMessage( new ResultOfTeamFormation( leader, B, FAILURE, null ) );
					teamMembers.add( A );
				}
			}
			// もし片っぽしか受理しなければそいつがチームメンバーとなる
			else if ( A != null | B != null ) {
				// Bだけ受理してくれた
				if ( A == null ) {
					preAllocations.put( B, leader.myTask.subtasks.get( indexA ) );
					teamMembers.add( B );
				}
				// Aだけ受理してくれた
				if ( B == null ) {
					preAllocations.put( A, leader.myTask.subtasks.get( indexA ) );
					teamMembers.add( A );
				}
			}
		}
		// 未割り当てが残っていないのなら実行へ
		if ( teamMembers.size() == leader.myTask.subtasks.size() ) {
			for ( Agent tm: teamMembers ) {
				appendAllocationHistory( tm, preAllocations.get( tm ) );
				TransmissionPath.sendMessage( new ResultOfTeamFormation( leader, tm, SUCCESS, preAllocations.get( tm ) ) );
			}
			if ( Agent._coalition_check_end_time - getCurrentTime() < COALITION_CHECK_SPAN ) {
				for ( Agent ag: teamMembers ) {
					leader.workWithAsL[ ag.id ]++;
				}
			}
			leader.pastTasks.add( leader.myTask );
			leader.myTask = null;
			leader.inactivate( 1 );
		}
		// 未割り当てのサブタスクが残っていれば失敗
		else {
			for ( Agent tm: teamMembers ) {
				exceptions.remove( tm );
				TransmissionPath.sendMessage( new ResultOfTeamFormation( leader, tm, FAILURE, null ) );
			}
			Manager.disposeTask( leader );
			leader.inactivate( -1 );
		}
	}


	/**
	 * selectMembersメソッド
	 * 優先度の高いエージェントから(すなわち添字の若い信頼エージェントから)選択する
	 * ε-greedyを導入
	 *
	 * @param subtasks
	 */

	// TODO: HACK: Map使って作り直す．
	public List<Agent> selectMembers( Agent leader, List< Subtask > subtasks ) {
		List< Agent > memberCandidates = new ArrayList<>();
		Agent candidate = null;
		List< Subtask > skips = new ArrayList<>();  // 互恵エージェントがいるために他のエージェントに要請を送らないサブタスクを格納

		for ( int i = 0; i < subtasks.size() * RESEND_TIMES; i++ ) {
			memberCandidates.add( null );
		}

		// 一つのタスクについてRESEND_TIMES周する
		for ( int i = 0; i < RESEND_TIMES; i++ ) {
			Subtask st;
			for ( int stIndex = 0; stIndex < subtasks.size(); stIndex++ ) {
				st = subtasks.get( stIndex );

				// 一つ目のサブタスク(報酬が最も高い)から割り当てていく
				// 信頼度の一番高いやつから割り当てる
				// εの確率でランダムに割り振る
				if ( MyRandom.epsilonGreedy( Agent.ε ) ) {
					do {
						candidate = Manager.getAgentRandomly( leader, exceptions, AgentManager.getAgentList() );
					} while ( leader.calculateExecutionTime( candidate, st ) < 0 );
				} else {
					// 信頼度ランキングの上から割り当てを試みる．
					// 1. 能力的にできない
					// 2. すでにチームに参加してもらっていて，まだ終了連絡がこない
					// 3. すでに別のサブタスクを割り当てる予定がある
					// に当てはまらなければ割り当て候補とする．
					for ( Agent ag: leader.reliabilityRankingAsL.keySet() ) {
						// TODO: inTheListもcalcExecutionTimeもインスタンスに紐づかないのでstaticにするかユーティリティクラスにする．
						// もっというなら，inTheList相当のメソッドが普通にあるはず．
						if ( Agent.inTheList( ag, exceptions ) < 0 &&
							Agent.calculateExecutionTime( ag, st ) > 0 ) {
							candidate = ag;
							break;
						}
					}
				}
				// 候補が見つかれば，チーム参加要請対象者リストに入れ，参加要請を送る
				if ( candidate == null ) {
					System.out.println( "It can't be executed." );
					return new ArrayList<>();
				}
				// 候補が見つからないサブタスクがあったら直ちにチーム編成を失敗とする
				else {
					exceptions.add( candidate );
					memberCandidates.set( stIndex + i * subtasks.size(), candidate );
				}
				candidate = null;
			}
		}
		return memberCandidates;
	}

	@Override
	public void reachReply( ReplyToSolicitation r ) {
		super.reachReply( r );
		roundTripTimeMap.put( r.getFrom(), getCurrentTime() - timeToStartCommunicatingMap.get( r.getFrom() ) );
	}


	@Override
	public void checkDoneMessage( Agent leader, Done d ) {
		Agent from = d.getFrom();
		Subtask st = getAllocatedSubtask( d.getFrom() );

		int bindingTime = getCurrentTime() - timeToStartCommunicatingMap.get( from );
		renewCongestionDegreeMap( congestionDegreeMap, roundTripTimeMap, from, st, bindingTime );

		renewDE( leader.reliabilityRankingAsL, from, 1, withBinary );
		exceptions.remove( from );

		// タスク全体が終わったかどうかの判定と，それによる処理
		// HACK: もうちょいどうにかならんか
		Task task = leader.findTaskContainingThisSubtask( st );
		task.subtasks.remove( st );

		if ( task.subtasks.isEmpty() ) {
			from.pastTasks.remove( task );
			Manager.finishTask( from, task );
			from.didTasksAsLeader++;
		}
	}

	// CONSIDER: 以下２つのメソッドが果たしてstaticがいいのか?
	private static void renewCongestionDegreeMap( Map< Agent, double[] > cdm, Map< Agent, Integer > rtm, Agent target, Subtask st, int bindingTime ) {
		double[] tempArray;
		if ( cdm.containsKey( target ) ) {
			tempArray = cdm.get( target );
		} else {
			tempArray = new double[ RESOURCE_TYPES ];
		}
		int requiredResourceType = st.resType;
		tempArray[ requiredResourceType ] = calculateCongestionDegree( bindingTime, rtm.get( target ), st );
		cdm.put( target, tempArray );
	}

	private static double calculateCongestionDegree( int bindingTime, int roundTripTime, Subtask subtask ) {
		int difficulty = subtask.reqRes[ subtask.resType ];
		return difficulty / ( bindingTime - 2.0 * roundTripTime );
	}

	void clear() {
	}
}
