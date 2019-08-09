package main.research.agent.strategy;

import main.research.SetParam;
import main.research.agent.Agent;


public abstract class MemberStrategy implements Strategy, SetParam {
    public void actAsMember(Agent agent) {
        sortReliabilityRanking(agent.reliabilityRankingAsM);
        if (agent.phase == REPLY) replyAsM(agent);
        else if (agent.phase == RECEPTION) receiveAsM(agent);
        else if (agent.phase == EXECUTION) execute(agent);
        evaporateDE(agent.reliabilityRankingAsM);
    }

    abstract public void checkMessages(Agent self);

    protected abstract void replyAsM(Agent ma);
    protected abstract void receiveAsM(Agent ma);
    protected abstract void execute(Agent la);

    protected abstract void renewDE(Agent from, Agent to, double evaluation);



}
