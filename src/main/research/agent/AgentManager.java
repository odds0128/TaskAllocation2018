package main.research.agent;

import main.research.SetParam;
import main.research.grid.Grid;
import main.research.others.random.MyRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static main.research.SetParam.Role.*;
import static main.research.others.random.MyRandom.getRandomInt;


public class AgentManager implements SetParam {
	private static List< Agent > allAgentList;

	// TODO: Agentインスタンスを生成する → 被らないように座標を設定する
	public static void initiateAgents( String package_name, String ls_name, String ms_name ) {
		allAgentList = generateAgents( package_name, ls_name, ms_name );
		deployAgents();
		setReliabilityRanking();
	}

	private static List< Agent > generateAgents( String package_name, String ls_name, String ms_name ) {
		List< Agent > agentList = new ArrayList();

		for ( int i = 0; i < AGENT_NUM; i++ ) {
			agentList.add( new Agent( package_name, ls_name, ms_name ) );
		}
		return agentList;
	}

	private static void deployAgents() {
		allAgentList.forEach(
			Grid::setAgentOnEnvironment
		);
	}

	private static void setReliabilityRanking() {
		for ( Agent ag: allAgentList ) {
			ag.ls.setMemberRankingRandomly( allAgentList );
			ag.ls.removeMyselfFromRanking( ag );
			ag.ms.setLeaderRankingRandomly( allAgentList );
			ag.ms.removeMyselfFromRanking( ag );
		}
	}

	public static List< Agent > generateRandomAgentList( List< Agent > agentList ) {
		List< Agent > originalList = new ArrayList<>( agentList );
		Collections.shuffle( originalList, MyRandom.getRnd() );
		return originalList;
	}

	public static Agent getAgentRandomly( Set< Agent > exceptions, List< Agent > targets ) {
		int random;
		Agent candidate;

		assert exceptions.size() < AGENT_NUM : "Too many exceptions";

		do {
			random = getRandomInt( 0, targets.size() - 1 );
			candidate = targets.get( random );
		} while ( exceptions.contains( candidate ) );
		return candidate;
	}


	public static List< Agent > getAllAgentList() {
		return allAgentList;
	}


	public static void clear() {
		allAgentList.clear();
	}

	public static void actRandom( List< Agent > agents, Role role ) {
		List< Agent > temp = new ArrayList<>( agents );
		int rand;
		switch ( role ) {
			case LEADER:
				while ( !temp.isEmpty() ) {
					rand = getRandomInt( 0, temp.size() - 1 );
					temp.remove( rand ).actAsLeader();
				}
				break;
			case MEMBER:
				while ( !temp.isEmpty() ) {
					rand = getRandomInt( 0, temp.size() - 1 );
					temp.remove( rand ).actAsMember();
				}
				break;
		}
		assert temp.size() == 0 : "Some agents do nothing.";
		temp.clear();
	}

	public static void JoneDoesSelectRole() {
		for ( Agent ag: allAgentList ) {
			if ( ag.role == JONE_DOE ) ag.selectRole();
		}
	}

	public static void actLeadersAndMembers() {
		List< Agent > leaders = new ArrayList<>();
		List< Agent > members = new ArrayList<>();
		for ( Agent ag: getAllAgentList() ) {
			if ( ag.role == MEMBER ) members.add( ag );
			else if ( ag.role == LEADER ) leaders.add( ag );
		}
		assert leaders.size() + members.size() == AGENT_NUM : "Some agents sabotage" + ( leaders.size() + members.size() );
		actRandom( leaders, LEADER );
		actRandom( members, MEMBER );
	}
}