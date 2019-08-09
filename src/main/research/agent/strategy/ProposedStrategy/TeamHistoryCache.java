package main.research.agent.strategy.ProposedStrategy;

import main.research.Manager;
import main.research.SetParam;
import main.research.agent.Agent;

import java.util.*;

class TeamHistoryCache {
	private int cachedTime;
	private Agent agent;
	private int requiredResourceType;
	private int executionTime;

	TeamHistoryCache(int cachedTime, Agent agent, int requiredResourceType, int executionTime){
		this.cachedTime = cachedTime;
		this.agent = agent;
		this.requiredResourceType = requiredResourceType;
		this.executionTime = executionTime;
	}

	public int getCachedTime() { return cachedTime; }
	public Agent getAgent() { return agent; }
	public int getRequiredResourceType() { return requiredResourceType; }
	public int getExecutionTime() { return executionTime; }

	boolean isExpired(){
		return ( Manager.getTicks() - cachedTime)  > SetParam.RESOURCE_CACHE_TIME;
	}

	// 推測のタイミング .. cache更新時
	// すなわち，cacheから蒸発した時とcacheに追加された時
	static Map<Agent, int[]> estimateAgentResources( Map agentAndEstimatedResourcesMap ) {

		return null;
	}

	static void updateCache(List<TeamHistoryCache> cache) {
		System.out.println("Invoked");
		Iterator it = cache.iterator();
		while( it.hasNext() ) {
			TeamHistoryCache temp = (TeamHistoryCache) it.next();
			if( temp.isExpired() ) it.remove();
			else break;
		}
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Time: " + cachedTime).append(", Agent: " + agent).append(", Required: " + requiredResourceType + " - " + executionTime);
		return sb.toString();
	}
}
