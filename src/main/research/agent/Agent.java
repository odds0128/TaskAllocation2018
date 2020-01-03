/**
 * @author Funato
 * @version 2.0
 */

package main.research.agent;

import static main.research.Parameter.Phase.*;
import static main.research.Parameter.Role.*;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.Manager;
import main.research.Parameter;
import main.research.agent.strategy.LeaderTemplateStrategy;
import main.research.agent.strategy.MemberTemplateStrategy;
import main.research.communication.message.*;
import main.research.others.random.*;
import main.research.task.Subtask;
import main.research.task.Task;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;


public class Agent implements Parameter, Cloneable {
	public static double ε_, α_, γ_;
	public static double initial_de_, initial_el_, initial_em_;
	public static int _coalition_check_end_time = Manager.max_turn_;
	public static int agent_num_ = AgentManager.agent_num_;
	public static int resource_types_;
	public static int subtask_queue_size_;
	public LeaderTemplateStrategy ls;
	public MemberTemplateStrategy ms;
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
	public double e_leader;
	public double e_member;
	public int[][] allocated = new int[ agent_num_ ][ resource_types_ ]; // そのエージェントからそのリソースを要求するサブタスクが割り当てられた回数

	public List< Solicitation > solicitationList = new ArrayList<>();
	public List< Reply > replyList = new ArrayList<>();
	public List< Result > resultList = new ArrayList<>();
	public List< Done > doneList = new ArrayList<>();

	// リーダーエージェントのみが持つパラメータ
	public int didTasksAsLeader = 0;
	public List< Task > pastTasks = new ArrayList<>();

	public static void setConstants( JsonNode agentNode ) {
		ε_ = agentNode.get( "parameters" ).get( "ε" ).asDouble();
		α_ = agentNode.get( "parameters" ).get( "α" ).asDouble();
		resource_types_ = agentNode.get( "agent" ).get( "resource_types" ).asInt();
		min_capacity_value_ = agentNode.get( "agent" ).get( "min_capacity_value" ).asInt();
		max_capacity_value_ = agentNode.get( "agent" ).get( "max_capacity_value" ).asInt();
		subtask_queue_size_ = agentNode.get( "agent" ).get( "subtask_queue_size" ).asInt();

		JsonNode parameterNode = agentNode.get( "parameters" );
		γ_ = parameterNode.get( "γ" ).asDouble();
		initial_de_ = parameterNode.get( "initial_de" ).asDouble();
		initial_el_ = parameterNode.get( "initial_el" ).asDouble();
		initial_em_ = parameterNode.get( "initial_em" ).asDouble();
	}

	public Agent( String package_name, String ls_name, String ms_name ) {
		this.id = _id++;
		setResource();
		setStrategies( package_name, ls_name, ms_name );
		selectRole();
	}

	public static boolean epsilonGreedy() {
		double random = MyRandom.getRandomDouble();
		return random < ε_;
	}

	private void setResource() {
		int resSum;
		resources = new int[ resource_types_ ];
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
		assert ms.expectedResultMessage == 0 : "Expect some result message.";
		assert ms.mySubtaskQueue.size() == 0 : "Remain some subtasks not finished.";

		if ( e_leader == e_member ) selectRandomRole();
		else if ( epsilonGreedy() ) selectReverseRole();
		else if ( e_leader > e_member ) selectLeaderRole();
		else if ( e_member > e_leader ) selectMemberRole();
	}

	private void selectLeaderRole() {
		this.role = LEADER;
		this.phase = SOLICIT;
	}

	private void selectMemberRole() {
		this.role = MEMBER;
		this.phase = WAIT_FOR_SOLICITATION;
	}

	private void selectRandomRole() {
		int toBeLeader = MyRandom.getRandomInt( 0, 1 );
		if ( toBeLeader == 1 ) selectLeaderRole();
		else selectMemberRole();
	}

	private void selectReverseRole() {
		if ( e_leader > e_member ) selectMemberRole();
		else if ( e_member > e_leader ) selectLeaderRole();
		else selectRandomRole();
	}


	public boolean canProcess( Subtask subtask ) {
		return resources[ subtask.reqResType ] > 0;
	}

	public Task findTaskContaining( Subtask finishedSubtask ) {
		// remove
		if( id == 327 && finishedSubtask.getId() == 30179 ) {
			System.out.println(pastTasks);
		}
		for ( Task t: pastTasks ) {
			if ( t.getId() == finishedSubtask.parentId ) return t;
		}
		return null;
	}

	public static void clear() {
		_id = 0;
	}

	public static int countNEETmembers( List< Agent > agents, int span ) {
		int now = Manager.getCurrentTime();
		return ( int ) agents.stream()
			.filter( ag -> now - ag.validatedTicks > span )
			.count();
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
		StringBuilder str = new StringBuilder();
		str.append( role + " No." + String.format( "%3d, ", id ) );
		return str.toString();
	}

	private void setStrategies( String package_name, String ls_name, String ms_name ) {
		Class LeaderStrategyClass;
		Class MemberStrategyClass;

		try {
			LeaderStrategyClass = Class.forName( package_name.concat( ls_name ) );
			MemberStrategyClass = Class.forName( package_name.concat( ms_name ) );
			this.ls = ( LeaderTemplateStrategy ) LeaderStrategyClass.getDeclaredConstructor().newInstance();
			this.ms = ( MemberTemplateStrategy ) MemberStrategyClass.getDeclaredConstructor().newInstance();
			this.ls.addMyselfToExceptions( this );
		} catch ( ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e ) {
			e.printStackTrace();
		}
	}

	public Point getP() {
		return p;
	}

	public int getX() {
		return ( int ) p.getX();
	}

	public int getY() {
		return ( int ) p.getY();
	}

	public void reachPost( Message m ) {
		switch ( m.getClass().getSimpleName() ) {
			case "Solicitation":
				solicitationList.add( ( Solicitation ) m );
				break;
			case "Reply":
				// todo: modify
				m.getTo().ls.reachReply( ( Reply ) m );
				break;
			case "Result":
				resultList.add( ( Result ) m );
				break;
			case "Done":
				doneList.add( ( Done ) m );
				break;
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
