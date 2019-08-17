/**
 * @author Funato
 * @version 2.0
 */

package main.research.agent;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.Role.*;
import static main.research.SetParam.Principle.*;

import main.research.Manager;
import main.research.SetParam;
import main.research.others.Pair;
import main.research.others.random.*;
import main.research.agent.strategy.LeaderStrategy;
import main.research.agent.strategy.MemberStrategy;
import main.research.task.Subtask;
import main.research.task.Task;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;


public class Agent implements SetParam, Cloneable {
	public static int _id = 0;
	private static int[] resSizeArray = new int[ RESOURCE_TYPES + 1 ];

	public static int _coalition_check_end_time;
	public static double ε = INITIAL_ε;
	public LeaderStrategy ls;
	public MemberStrategy ms;

	// リーダーもメンバも持つパラメータ
	public int id;
	public Point p;
	public Role role = JONE_DOE;
	public Phase phase = SELECT_ROLE;
	public int[] resources = new int[ RESOURCE_TYPES ];
	public int[] workWithAsL = new int[ AGENT_NUM ];
	public int[] workWithAsM = new int[ AGENT_NUM ];
	public int validatedTicks = 0;
	public double e_leader = INITIAL_VALUE_OF_DSL;
	public double e_member = INITIAL_VALUE_OF_DSM;
	public Principle principle = RATIONAL;
	public int[] required = new int[ RESOURCE_TYPES ];            // そのリソースを要求するサブタスクが割り当てられた回数
	public int[][] allocated = new int[ AGENT_NUM ][ RESOURCE_TYPES ]; // そのエージェントからそのリソースを要求するサブタスクが割り当てられた回数

	// リーダーエージェントのみが持つパラメータ
	public int didTasksAsLeader = 0;
	public List< Agent > candidates;         // これからチームへの参加を要請するエージェントのリスト
	public int proposalNum = 0;            // 送ったproposalの数を覚えておく
	public List< Agent > teamMembers;        // すでにサブタスクを送っていてメンバの選定から外すエージェントのリスト
	public Task myTask;                  // 持ってきた(割り振られた)タスク
	public int replyNum = 0;
	public double threshold_for_reciprocity_as_leader;
	public List< Task > pastTasks = new ArrayList<>();
	public Map< Agent, Double > reliabilityRankingAsL = new LinkedHashMap<>( HASH_MAP_SIZE );

	// メンバエージェントのみが持つパラメータ
	public int didTasksAsMember = 0;
	public int executionTime = 0;
	public double threshold_for_reciprocity_as_member;
	// TODO: List< Pair<Agent, Subtask> にしましょ
	public List<Pair<Agent, Subtask>> mySubtaskQueue = new ArrayList<>(  );
	public int numberOfExpectedMessages = 0;                                            // 返事待ちの数
	public Map< Agent, Double > reliabilityRankingAsM = new LinkedHashMap<>( HASH_MAP_SIZE );

	public Agent( String ls_name, String ms_name ) {
		this.id = _id++;

		int resSum = Arrays.stream( resources ).sum();
		int resCount = ( int ) Arrays.stream( resources )
			.filter( res -> res > 0 )
			.count();
		setResource();
		setStrategies( ls_name, ms_name );
		threshold_for_reciprocity_as_leader = THRESHOLD_FOR_RECIPROCITY_FROM_LEADER;
		threshold_for_reciprocity_as_member = ( double ) resSum / resCount * THRESHOLD_FOR_RECIPROCITY_RATE;
		selectRole();
	}

	private void setResource() {
		int resSum;
		do {
			resSum = 0;
			for ( int i = 0; i < RESOURCE_TYPES; i++ ) {
				int rand = MyRandom.getRandomInt( MIN_AGENT_RESOURCE_SIZE, MAX_AGENT_RESOURCE_SIZE );
				resources[ i ] = rand;
				resSum += rand;
			}
		} while ( resSum == 0 );
	}

	public void setPosition( int x, int y ) {
		this.p = new Point( x, y );
	}

	void setReliabilityRankingRandomly( List< Agent > agentList ) {
		List< Agent > rl = generateRandomAgentList( agentList );
		for ( Agent ag: rl ) {
			this.reliabilityRankingAsL.put( ag, INITIAL_VALUE_OF_DE );
		}
		List< Agent > rm = generateRandomAgentList( agentList );
		for ( Agent ag: rm ) {
			this.reliabilityRankingAsM.put( ag, INITIAL_VALUE_OF_DE );
		}
	}

	private List< Agent > generateRandomAgentList( List< Agent > agentList ) {
		List< Agent > originalList = new ArrayList<>( agentList );
		List< Agent > randomAgentList = new ArrayList<>();

		originalList.remove( this );

		int size = originalList.size();

		int index;
		Agent ag;
		for ( int i = 1; i <= size; i++ ) {
			index = MyRandom.getRandomInt( 0, size - i );
			ag = originalList.remove( index );
			randomAgentList.add( ag );
		}
		return randomAgentList;
	}

	public void actAsLeader() {
		ls.actAsLeader( this );
	}

	public void actAsMember() {
		ms.actAsMember( this );
	}

	public void selectRole() {
		validatedTicks = Manager.getCurrentTime();
		if ( mySubtaskQueue.size() > 0 ) {
			role = MEMBER;
			this.phase = EXECUTE_SUBTASK;
		}
//		if (epsilonGreedy()) {
//			if (e_leader < e_member) {
//				role = LEADER;
//				this.phase = PROPOSITION;
//				candidates = new ArrayList<>();
//				teamMembers = new ArrayList<>();
//				preAllocations = new HashMap<>();
//				replies = new ArrayList<>();
//				results = new ArrayList<>();
//			} else if (e_member < e_leader) {
//				role = MEMBER;
//				this.phase = WAITING;
//			} else {
//// */
//				int ran = MyRandom.getRandomInt(0, 1);
//				if (ran == 0) {
//					role = LEADER;
//					this.phase = PROPOSITION;
//					candidates = new ArrayList<>();
//					teamMembers = new ArrayList<>();
//					preAllocations = new HashMap<>();
//					replies = new ArrayList<>();
//					results = new ArrayList<>();
//				} else {
//					role = MEMBER;
//					this.phase = WAITING;
//				}
//			}
//			// εじゃない時
//		} else {
		if ( e_leader > e_member ) {
			role = LEADER;
			this.phase = SOLICIT;
			candidates = new ArrayList<>();
			teamMembers = new ArrayList<>();
		} else if ( e_member > e_leader ) {
			role = MEMBER;
			this.phase = WAIT_FOR_SOLICITATION;
		} else {
			int ran = MyRandom.getRandomInt( 0, 1 );
			if ( ran == 0 ) {
				role = LEADER;
				this.phase = SOLICIT;
				candidates = new ArrayList<>();
				teamMembers = new ArrayList<>();
			} else {
				role = MEMBER;
				this.phase = WAIT_FOR_SOLICITATION;
			}
		}
		//	}
	}

	void selectRoleWithoutLearning() {
		int ran = MyRandom.getRandomInt( 0, 6 );
		if ( mySubtaskQueue.size() > 0 ) {
			role = MEMBER;
			this.phase = EXECUTE_SUBTASK;
		}
		if ( ran == 0 ) {
			role = LEADER;
			e_leader = 1;
			this.phase = SOLICIT;
			candidates = new ArrayList<>();
			teamMembers = new ArrayList<>();
		} else {
			role = MEMBER;
			e_member = 1;
			this.phase = WAIT_FOR_SOLICITATION;
		}
	}

	/**
	 * inactiveメソッド
	 * チームが解散になったときに待機状態になる.
	 */
	public void inactivate( double success ) {
		if ( role == LEADER ) {
			e_leader = e_leader * ( 1.0 - α ) + α * success;
		} else {
			e_member = e_member * ( 1.0 - α ) + α * success;
		}

		if ( role == LEADER ) {
			candidates.clear();
			teamMembers.clear();
			proposalNum = 0;
			replyNum = 0;
		}
		role = JONE_DOE;
		phase = SELECT_ROLE;
		this.validatedTicks = Manager.getCurrentTime();
	}

	public static int calculateExecutionTime( Agent a, Subtask st ) {
		if ( a == null ) System.out.println( "Ghost trying to do subtask" );
		if ( st == null ) System.out.println( "Agent trying to do nothing" );

		if ( a.resources[ st.resType ] == 0 ) return -1;
		return ( int ) Math.ceil( ( double ) st.reqRes[ st.resType ] / ( double ) a.resources[ st.resType ] );
	}

	// consider: そもそもいるのか問題と，出来合いのメソッドがあるのでは問題

	/**
	 * inTheListメソッド
	 * 引数のエージェントが引数のリスト内にあればその索引を, いなければ-1を返す
	 */
	public static int inTheList( Object a, List List ) {
		for ( int i = 0; i < List.size(); i++ ) {
			if ( a.equals( List.get( i ) ) ) return i;
		}
		return -1;
	}

	public Task findTaskContainingThisSubtask( Subtask finishedSubtask ) {
		for ( Task t: pastTasks ) {
			if ( t.isPartOfThisTask( finishedSubtask ) ) return t;
		}
		return null;
	}

	public static void renewEpsilonLinear() {
		ε -= DIFFERENCE;
		if ( ε < FLOOR ) ε = FLOOR;
	}

	public static void renewEpsilonExponential() {
		ε = ( ε - FLOOR ) * RATE;
		ε += FLOOR;
	}

	public static void clear() {
		_id = 0;
		_coalition_check_end_time = SNAPSHOT_TIME;
		ε = INITIAL_ε;
		for ( int i = 0; i < RESOURCE_TYPES; i++ ) resSizeArray[ i ] = 0;
	}

	// 結果集計用のstaticメソッド
	public static void resetWorkHistory( List< Agent > agents ) {
		for ( Agent ag: agents ) {
			for ( int i = 0; i < AGENT_NUM; i++ ) {
				ag.workWithAsM[ i ] = 0;
				ag.workWithAsL[ i ] = 0;
			}
		}
		_coalition_check_end_time = MAX_TURN_NUM;
	}

	public static int countReciprocalMember( List< Agent > agents ) {
		int temp = 0;
		for ( Agent ag: agents ) {
			if ( ag.e_member > ag.e_leader && ag.principle == RECIPROCAL ) {
				temp++;
			}
		}
		return temp;
	}


	/**
	 * agentsの中でspan以上の時間誰からの依頼も受けずチームに参加していないメンバ数を返す．
	 *
	 * @param agents
	 * @param span
	 * @return
	 */
	public static int countNEETmembers( List< Agent > agents, int span ) {
		int neetM = 0;
		int now = Manager.getCurrentTime();
		for ( Agent ag: agents ) {
			if ( now - ag.validatedTicks > span ) {
				neetM++;
			}
		}
		return neetM;
	}

	@Override
	public Agent clone() { //基本的にはpublic修飾子を付け、自分自身の型を返り値とする
		Agent b = null;

		try {
			b = ( Agent ) super.clone(); //親クラスのcloneメソッドを呼び出す(親クラスの型で返ってくるので、自分自身の型でのキャストを忘れないようにする)
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return b;
	}

	@Override
	public String toString() {
		String sep = System.getProperty( "line.separator" );
		StringBuilder str = new StringBuilder();
		str = new StringBuilder( String.format( "%3d", id ) );
//        str = new StringBuilder("ID:" + String.format("%3d", id) + ", " + "x: " + x + ", y: " + y + ", ");
//        str = new StringBuilder("ID:" + String.format("%3d", id) + "  " + messages );
//        str = new StringBuilder("ID: " + String.format("%3d", id) + ", " + String.format("%.3f", e_leader) + ", " + String.format("%.3f", e_member)  );
//        str = new StringBuilder("ID:" + String.format("%3d", id) + ", Resources: " + resSize + ", " + String.format("%3d", didTasksAsMember)  );
/*
        if( this.principle == RECIPROCAL ) {
            str.append(", The most reliable agent: " + relRanking.get(0).id + "← reliability: " + reliabilities[relRanking.get(0).id]);
            str.append(", the delay: " + main.research.Manager.delays[this.id][relRanking.get(0).id]);
        }
// */
		str.append( "[" );
		for ( int i = 0; i < RESOURCE_TYPES; i++ ) str.append( String.format( "%3d", resources[ i ] ) + "," );
		str.append( "]" );
// */
        /*
        if( role == LEADER ) {
            List<Agent> temp = new ArrayList<>();
            temp.addAll(candidates);
            temp.addAll(teamMembers);
            str.append( " Waiting: " );
            for( Agent ag: temp ) str.append( String.format("%3d", ag.id ) + ", ");
        }
// */
/*       if (e_member > e_leader) str.append(", member: ");
        else if (e_leader > e_member) str.append(", leader: ");
        else if (role == JONE_DOE) str.append(", free: ");
//        str.append(String.format(", %.3f", e_leader) + ", " + String.format("%.3f", e_member) + sep);

        for( int i = 0; i < AGENT_NUM; i++ ) str.append( i + ": " + String.format("%.3f", reliabilities[i] ) + ",  " );
// */
/*        str.append("e_leader: " + e_leader + ", e_member: " + e_member);
        if( role == MEMBER ) str.append(", member: ");
        else if( role == LEADER ) str.append(", leader: " );
        else if( role == JONE_DOE ) str.append(", free: ");
/*
        if( phase == REPORT ){
            str.append(" allocations: " + allocations  );
//            str.append(" teamMembers: " + teamMembers );
        }
        else if( phase == RECEPTION ) str.append(", My Leader: " + leader.id);
        else if( phase == EXECUTION ) str.append(", My Leader: " + leader.id + ", " + mySubtaskQueue.get(0) + ", resources: " + resource + ", rest:" + executionTime);

//        if (role == LEADER) str.append(", I'm a leader. ");
//        else str.append("I'm a member. ");
// */
/*
        if (relAgents.size() != 0) {
            int temp;
            str.append(sep + "   Reliable Agents:");
            for (int i = 0; i < relAgents.size(); i++) {
                temp = relAgents.get(i).id;
                str.append(String.format("%3d", temp));
            }
        }
// */
/*
        int temp;
        str.append( sep + "   Reliability Ranking:");
        for( int i = 0; i < 5; i++ ){
            temp = relRanking.get(i).id;
            str.append(String.format("%3d", temp) );
            str.append("→" + reliabilities[temp]);
        }
// */
// */
        /*
        if( role == MEMBER ) {
            str.append(",  Reliable Agents:");
            for (int i = 0; i < MAX_REL_AGENTS; i++) {
                temp = relAgents.get(i);
                str.append(String.format("%3d", temp));
            }
        }
//        */

		return str.toString();
	}

	private void setStrategies( String ls_name, String ms_name ) {
		String package_name = "main.research.agent.strategy.ProposedStrategy.";

		Class LeaderStrategyClass;
		Class MemberStrategyClass;

		try {
			LeaderStrategyClass = Class.forName( package_name.concat( ls_name ) );
			MemberStrategyClass = Class.forName( package_name.concat( ms_name ) );
			this.ls = ( LeaderStrategy ) LeaderStrategyClass.getDeclaredConstructor().newInstance();
			this.ms = ( MemberStrategy ) MemberStrategyClass.getDeclaredConstructor().newInstance();
		} catch ( ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e ) {
			e.printStackTrace();
		}
	}

	public static class AgentExcellenceComparator implements Comparator< Agent > {
		public int compare( Agent a, Agent b ) {
			Integer resCount_a = ( int ) Arrays.stream( a.resources )
				.filter( resource -> resource > 0 )
				.count();
			Integer resCount_b = ( int ) Arrays.stream( b.resources )
				.filter( resource -> resource > 0 )
				.count();

			Double excellence_a = ( double ) Arrays.stream( a.resources ).sum() / resCount_a;
			Double excellence_b = ( double ) Arrays.stream( b.resources ).sum() / resCount_b;

			int result = excellence_a.compareTo( excellence_b );
			if ( result != 0 ) return result;

			result = resCount_a.compareTo( resCount_b );
			if ( result != 0 ) return result;

			return a.id >= b.id ? 1 : -1;
		}
	}


	public static class AgentIDcomparator implements Comparator< Agent > {
		public int compare( Agent a, Agent b ) {
			return a.id >= b.id ? 1 : -1;
		}
	}

	@Override
	public boolean equals( Object o ) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;
		Agent agent = ( Agent ) o;
		return id == agent.id &&
			p.equals( agent.p );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, p );
	}
}
