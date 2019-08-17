package main.research.agent;

import main.research.task.Subtask;

import java.util.HashMap;
import java.util.Map;

public class Team {
	Map< Agent, Subtask > teamMap = new HashMap<>(  );

	public Team( Map< Agent, Subtask > teamMap ) {
		this.teamMap = teamMap;
	}

	public void allocate( Agent member, Subtask subtask ) {
		this.teamMap.put( member, subtask );
	}

}
