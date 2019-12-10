/**
 * @author Funato
 * @version 2.0
 */

package main.research.agent;

import static main.research.SetParam.Phase.*;
import static main.research.SetParam.Role.*;
import static main.research.SetParam.Principle.*;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.Manager;
import main.research.SetParam;
import main.research.agent.strategy.LeaderState;
import main.research.agent.strategy.MemberState;
import main.research.others.random.*;
import main.research.task.Subtask;
import main.research.task.Task;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;


public class Agent implements SetParam, Cloneable {
	public static double ε_, α_;
	public static int _coalition_check_end_time = Manager.max_turn_;
	public static int agent_num_ = AgentManager.agent_num_;
	public static int resource_types_;
	public LeaderState ls;
	public MemberState ms;
	public static int min_capacity_value_;
	public static int max_capacity_value_;

	public static int _id = 0;

	// リーダーもメンバも持つパラメータ
	public int id;
	private Point p;
	public Role role = JONE_DOE;
	public Phase phase = SELECT_ROLE;
	public int[] resources;
	public int[] workWithAsL = new int[ agent_num_ ];
	public int[] workWithAsM = new int[ agent_num_ ];
	public int validatedTicks = 0;
	public double e_leader ;
	public double e_member ;
	public Principle principle = RATIONAL;
	public int[] required = new int[ resource_types_ ];            // そのリソースを要求するサブタスクが割り当てられた回数
	public int[][] allocated = new int[ agent_num_ ][ resource_types_ ]; // そのエージェントからそのリソースを要求するサブタスクが割り当てられた回数

	// リーダーエージェントのみが持つパラメータ
	public int didTasksAsLeader = 0;
	public List< Task > pastTasks = new ArrayList<>();

	// メンバエージェントのみが持つパラメータ
	public int didTasksAsMember = 0;
	public int executionTime = 0;


	public static void setConstants( JsonNode parameterNode ) {
		ε_ = parameterNode.get( "parameters" ).get( "ε" ).asDouble();
		α_ = parameterNode.get( "parameters" ).get( "α" ).asDouble();
		resource_types_ = parameterNode.get( "agent" ).get( "resource_types" ).asInt();
		min_capacity_value_ = parameterNode.get( "agent" ).get( "min_capacity_value" ).asInt();
		max_capacity_value_ = parameterNode.get( "agent" ).get( "max_capacity_value" ).asInt();
	}
	public Agent( String package_name, String ls_name, String ms_name ) {
		this.id = _id++;
		setResource();
		setStrategies( package_name, ls_name, ms_name );
		selectRole();
	}

	public static boolean epsilonGreedy( ) {
		double random = MyRandom.getRandomDouble();
		return random < ε_;
	}

	private void setResource() {
		int resSum;
		resources = new int[resource_types_];
		do {
			resSum = 0;
			for ( int i = 0; i < resource_types_; i++ ) {
				int rand = MyRandom.getRandomInt( min_capacity_value_, max_capacity_value_ );
				resources[ i ] = rand;
				resSum += rand;
			}
		} while ( resSum == 0 );
	}

	public void setPosition( Point p ) {
		this.p = p;
	}

	void actAsLeader() {
		ls.actAsLeader( this );
	}

	void actAsMember() {
		ms.actAsMember( this );
	}

	void selectRole() {
		validatedTicks = Manager.getCurrentTime();
		if ( ms.mySubtaskQueue.size() > 0 ) {
			role = MEMBER;
			this.phase = EXECUTE_SUBTASK;
			executionTime = calculateExecutionTime( this, ms.mySubtaskQueue.get( 0 ).getValue() );
		}

		if( e_leader == e_member ) { selectRandomRole(); return; }
		if( epsilonGreedy( )     ) { selectReverseRole(); return; }
		if ( e_leader > e_member ) { selectLeaderRole(); return; }
		if ( e_member > e_leader ) { selectMemberRole(); return; }
	}

	private void selectLeaderRole(  ) {
		this.role  = LEADER;
		this.phase = SOLICIT;
	}

	private void selectMemberRole() {
		this.role  = MEMBER;
		this.phase = WAIT_FOR_SOLICITATION;
	}

	private void selectRandomRole() {
		int toBeLeader = MyRandom.getRandomInt( 0, 1 );
		if( toBeLeader == 1 ) selectLeaderRole();
		else selectMemberRole();
	}

	private void selectReverseRole() {
		if( e_leader > e_member ) {
			selectMemberRole();
		}else if( e_member > e_leader ){
			selectLeaderRole();
		}else{
			selectRandomRole();
		}
	}



	void selectRoleWithoutLearning() {
		int ran = MyRandom.getRandomInt( 0, 6 );
		if ( ms.mySubtaskQueue.size() > 0 ) {
			role = MEMBER;
			this.phase = EXECUTE_SUBTASK;
		}

		if ( ran == 0 ) selectLeaderRole();
		else selectMemberRole();
	}

	public void inactivate( double success ) {
		if ( role == LEADER ) {
			e_leader = e_leader * ( 1.0 - α_ ) + α_ * success;
		} else{
			e_member = e_member * ( 1.0 - α_ ) + α_ * success;
		}
		role = JONE_DOE;
		phase = SELECT_ROLE;
		principle = RATIONAL;
		this.validatedTicks = Manager.getCurrentTime();
	}

	public static int calculateExecutionTime( Agent a, Subtask st ) {
		return ( int ) Math.ceil( ( double ) st.reqRes[ st.resType ] / ( double ) a.resources[ st.resType ] );
	}

	public boolean canProcessTheSubtask( Subtask subtask) {
		return resources[ subtask.resType ] > 0;
	}

	public Task findTaskContainingThisSubtask( Subtask finishedSubtask ) {
		for ( Task t: pastTasks ) {
			if ( t.getId() == finishedSubtask.parentId ) return t;
		}
		return null;
	}

	public static void clear() {
		_id = 0;
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

	public static int countReciprocalLeader(List<Agent> agents) {
		int temp = 0;
		for ( Agent ag: agents ) {
			if ( ag.e_leader > ag.e_member && ag.principle == RECIPROCAL ) {
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
		StringBuilder str = new StringBuilder( );
		str.append( role + " No." + String.format( "%3d, ", id ) );
		if( role == LEADER ) {

		}else if( role == MEMBER ){
			str.append( "resources: " + Arrays.toString( resources ) + ", ");
		}
		return str.toString();
	}

	private void setStrategies( String package_name, String ls_name, String ms_name ) {
		Class LeaderStrategyClass;
		Class MemberStrategyClass;

		try {
			LeaderStrategyClass = Class.forName( package_name.concat( ls_name ) );
			MemberStrategyClass = Class.forName( package_name.concat( ms_name ) );
			this.ls = ( LeaderState ) LeaderStrategyClass.getDeclaredConstructor().newInstance();
			this.ms = ( MemberState ) MemberStrategyClass.getDeclaredConstructor().newInstance();
			this.ls.addMyselfToExceptions( this );
		} catch ( ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e ) {
			e.printStackTrace();
		}
	}

	public Point getP() {
		return p;
	}

	public int getX() {
		return (int) p.getX();
	}

	public int getY() {
		return (int) p.getY();
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


	public static class AgentIdComparator implements Comparator< Agent > {
		public int compare( Agent a, Agent b ) {
			return a.id >= b.id ? 1 : -1;
		}
	}

	@Override
	public boolean equals( Object o ) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;
		Agent agent = ( Agent ) o;
		return id == agent.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}
}
