package main.research.agent.strategy.ProposedStrategy;

import main.research.*;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.agent.strategy.LeaderStrategy;
import main.research.communication.message.*;
import main.research.others.Pair;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;
import main.research.task.Task;

import static main.research.Manager.disposeTask;
import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.DERenewalStrategy.*;
import static main.research.SetParam.ReplyType.*;
import static main.research.SetParam.ResultType.FAILURE;
import static main.research.SetParam.ResultType.SUCCESS;
import static main.research.agent.strategy.Strategy.*;
import static main.research.communication.TransmissionPath.*;

import java.util.*;

// TODO: 中身を表したクラス名にする
public class ProposedStrategy_l extends LeaderStrategy implements SetParam {
	Map< Agent, Integer > timeToStartCommunicatingMap = new HashMap<>();
	Map< Agent, Integer > roundTripTimeMap = new HashMap<>();
	Map< Agent, double[] > congestionDegreeMap = new HashMap<>();

	protected void solicitAsL( Agent leader ) {
		myTask = Manager.getTask( leader );
		if ( myTask == null ) {
			leader.inactivate( 0 );
			return;
		}

		List< Agent > candidates = selectMembers( leader, myTask.subtasks );
		repliesToCome = candidates.size();

		if ( candidates.isEmpty() ) {
			leader.inactivate( 0 );
			return;
		} else {
			sendSolicitations( leader, candidates );
		}
		proceedToNextPhase( leader );  // 次のフェイズへ
	}

	private void sendSolicitations( Agent leader, List< Agent > targets ) {
		for ( int i = 0; i < targets.size(); i++ ) {
			Agent target = targets.get( i );
			if ( target != null ) {
				timeToStartCommunicatingMap.put( target, getCurrentTime() );
				// TODO: 長すぎ
				Message solicitation = new Solicitation( leader, target, myTask.subtasks.get( i % myTask.subtasks.size() ) );
				sendMessage( solicitation );
			}
		}
	}

	protected void formTeamAsL( Agent leader ) {
		if ( replyList.size() < repliesToCome ) return;
		else repliesToCome = 0;

		// remove
		if( myTask == null ){
			System.out.println( leader );
		}

		Map< Subtask, Agent > mapOfSubtaskAndAgent = new HashMap<>();
		while ( !replyList.isEmpty() ) {
			ReplyToSolicitation r = replyList.remove( 0 );
			Subtask st = r.getSubtask();
			Agent currentFrom = r.getFrom();
			updateRoundTripTime( currentFrom );

			if ( r.getReplyType() == DECLINE ) treatBetrayer( leader, r.getFrom() );
			else if ( mapOfSubtaskAndAgent.containsKey( st ) ) {
				Agent rival = mapOfSubtaskAndAgent.get( st );
				Pair winnerAndLoser = compareDE( currentFrom, rival );

				exceptions.remove( winnerAndLoser.getValue() );
				sendMessage( new ResultOfTeamFormation( leader, ( Agent ) winnerAndLoser.getValue(), FAILURE, null ) );
				mapOfSubtaskAndAgent.put( st, ( Agent ) winnerAndLoser.getKey() );
			} else {
				mapOfSubtaskAndAgent.put( st, currentFrom );
			}
		}
		if ( canExecuteTheTask( myTask, mapOfSubtaskAndAgent.keySet() ) ) {
			for ( Map.Entry entry: mapOfSubtaskAndAgent.entrySet() ) {
				Agent   friend = ( Agent ) entry.getValue();
				Subtask st = (Subtask ) entry.getKey();

				sendMessage( new ResultOfTeamFormation( leader, friend, SUCCESS, st ) );
				appendAllocationHistory( friend, st );
				if ( withinTimeWindow() ) leader.workWithAsL[ friend.id ]++;
				leader.pastTasks.add( myTask );
			}
			leader.inactivate( 1 );
		} else {
			apologizeToFriends( leader, new ArrayList<>( mapOfSubtaskAndAgent.values() ) );
			disposeTask();
			leader.inactivate( 0 );
		}
		myTask = null;
	}

	void treatBetrayer( Agent leader, Agent betrayer ) {
		exceptions.remove( betrayer );
		renewDE( reliableMembersRanking, betrayer, 0, withBinary );
	}

	// hack: 現状リーダーは全員からの返信をもらってから割り当てを開始するため，早くに返信が到着したエージェントとの総通信時間が見かけ上長くなってしまう．
	// だからここではそれを訂正するために，その差分をroundTripTimeの一部として足し合わせることで混雑度の計算が狂わないようにしている
	void updateRoundTripTime( Agent target ) {
		int gap = getCurrentTime() - timeToStartCommunicatingMap.get( target ) - roundTripTimeMap.get( target );
		assert gap % 2 == 0 : "gap is odd.";
		int modifiedRoundTripTime = roundTripTimeMap.get( target ) + gap / 2;
		roundTripTimeMap.put( target, modifiedRoundTripTime );
	}

	Pair< Agent, Agent > compareDE( Agent first, Agent second ) {
		if ( reliableMembersRanking.get( first ) >= reliableMembersRanking.get( second ) )
			return new Pair<>( first, second );
		return new Pair<>( second, first );
	}

	private boolean canExecuteTheTask( Task task, Set< Subtask > subtaskSet ) {
		// remove
		if( task == null ) {
			System.out.println( "task is null" );
		}
		// TODO: あとでサイズだけ比較するようにする
		int actual = 0;
		for ( Subtask st: subtaskSet ) if ( task.isPartOfThisTask( st ) ) actual++;
		return task.subtasks.size() == actual;
	}

	private void apologizeToFriends( Agent failingLeader, List<Agent> friends ) {
		for( Agent friend : friends ) sendMessage( new ResultOfTeamFormation( failingLeader, friend, FAILURE, null ) );
	}

	private boolean withinTimeWindow(  ) {
		return Agent._coalition_check_end_time - getCurrentTime() < COALITION_CHECK_SPAN;
	}

	// TODO: HACK: Map使って作り直す．
	public List< Agent > selectMembers( Agent leader, List< Subtask > subtasks ) {
		List< Agent > memberCandidates = new ArrayList<>();
		Agent candidate = null;

		for( int i = 0; i < subtasks.size() * REBUNDUNT_SOLICITATION_TIMES; i++ ) {
			memberCandidates.add( null );
		}

		// 一つのタスクについてRESEND_TIMES周する
		for ( int i = 0; i < REBUNDUNT_SOLICITATION_TIMES; i++ ) {
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
					for ( Agent ag: reliableMembersRanking.keySet() ) {
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

		renewDE( reliableMembersRanking, from, 1, withBinary );
		exceptions.remove( from );

		// タスク全体が終わったかどうかの判定と，それによる処理
		// HACK: もうちょいどうにかならんか
		Task task = leader.findTaskContainingThisSubtask( st );
		task.subtasks.remove( st );

		if ( task.subtasks.isEmpty() ) {
			from.pastTasks.remove( task );
			Manager.finishTask(  );
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
