package main.research.agent;

import com.fasterxml.jackson.databind.JsonNode;
import main.research.Parameter;
import main.research.grid.Grid;
import main.research.others.random.MyRandom;

import java.util.*;

import static main.research.Parameter.Role.*;
import static main.research.others.random.MyRandom.getRandomInt;


public class AgentManager implements Parameter {
	public static int agent_num_;
	public static int time_observing_team_formation_;
	private static List< Agent > allAgentList;

	public static void setConstants( JsonNode agentNode ) {
		agent_num_ = agentNode.get( "agent_num" ).asInt();
		time_observing_team_formation_ = agentNode.get( "time_observing_team_formation" ).asInt();
		Agent.setConstants( agentNode );
	}

	public static void initiateAgents( String package_name, String ls_name, String ms_name ) {
		allAgentList = generateAgents( package_name, ls_name, ms_name );
		deployAgents();
		setReliabilityRanking();
	}

	private static List< Agent > generateAgents( String package_name, String ls_name, String ms_name ) {
		List< Agent > agentList = new ArrayList();

		for ( int i = 0; i < agent_num_; i++ ) {
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
			ag.ls.setMemberRankingRandomly( ag, allAgentList );
			ag.ms.setLeaderRankingRandomly( ag, allAgentList );
		}
	}

	public static List< Agent > generateRandomAgentList( List< Agent > agentList ) {
		List< Agent > originalList = new ArrayList<>( agentList );
		Collections.shuffle( originalList, MyRandom.getRnd() );
		return originalList;
	}

	public static Agent getAgentRandomly( List< Agent > exceptions, List< Agent > targets ) {
		int random;
		Agent candidate;

		assert exceptions.size() < agent_num_ : "Too many exceptions";

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

	private static void actLeaders( List< Agent > leaders ) {
		for ( Agent leader: leaders ) leader.actAsLeader();
	}

	private static void actMembers( List< Agent > members ) {
		for ( Agent member: members ) member.actAsMember();
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
		assert leaders.size() + members.size() == agent_num_ : "Some agents sabotage" + ( leaders.size() + members.size() );
		actLeaders( leaders );
		actMembers( members );
	}

	public static int countMembers() {
		return ( int ) allAgentList.stream()
			.filter( ag -> ag.e_member > ag.e_leader )
			.count();
	}
}