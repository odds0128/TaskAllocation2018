package main.research.strategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.task.Subtask;

import java.util.*;
import java.util.Map.*;

public abstract class AbstractStrategy implements Strategy, SetParam {
    static final double γ = γ_r;


    public void actAsLeader(Agent agent) {
        if (agent.phase == PROPOSITION) proposeAsL(agent);
        else if (agent.phase == REPORT) reportAsL(agent);
        else if (agent.phase == EXECUTION) execute(agent);
        evaporateDE(agent.relRanking_l);
    }

    public void actAsMember(Agent agent) {
        if (agent.phase == REPLY) replyAsM(agent);
        else if (agent.phase == RECEPTION) receiveAsM(agent);
        else if (agent.phase == EXECUTION) execute(agent);
        evaporateDE(agent.relRanking_m);
    }

    public List<Agent> selectMembers(Agent agent, List<Subtask> subtasks) {
        return null;
    }

    public void checkMessages(Agent self) {

    }

    public void clearStrategy() {

    }

    abstract void proposeAsL( Agent la );
    abstract void reportAsL( Agent la );
    abstract void execute( Agent la );
    abstract void replyAsM( Agent ma );
    abstract void receiveAsM( Agent ma );

    abstract void renewDE(Agent from, Agent to, double evaluation);



}
