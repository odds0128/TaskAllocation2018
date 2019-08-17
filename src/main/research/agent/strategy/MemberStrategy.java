package main.research.agent.strategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.task.Subtask;

import static main.research.SetParam.DERenewalStrategy.withBinary;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.ResultType.*;
import static main.research.agent.strategy.Strategy.*;

import java.util.*;

public abstract class MemberStrategy implements Strategy, SetParam {
    protected List< Solicitation > solicitationList = new ArrayList<>();
    private List< ResultOfTeamFormation > resultList = new ArrayList<>();

    public void actAsMember(Agent member) {
        sortReliabilityRanking(member.reliabilityRankingAsM);

        member.ms.replyToSolicitations( member, solicitationList);

        while( ! resultList.isEmpty() ) {
            ResultOfTeamFormation r = resultList.remove( 0 );
            reactToResultMessage( member, r );
        }

        if      (member.phase == WAIT_FOR_SOLICITATION ) replyAsM(member);
        else if (member.phase == WAIT_FOR_SUBTASK )      receiveAsM(member);
        else if (member.phase == EXECUTE_SUBTASK )       execute(member);
        evaporateDE(member.reliabilityRankingAsM);
    }

    protected abstract void replyToSolicitations( Agent member, List< Solicitation > solicitationList );

    private void reactToResultMessage( Agent member, ResultOfTeamFormation r) {
        if( r.getResult() == SUCCESS ) {
        	Pair<Agent, Subtask > pair =  new Pair<>( r.getFrom(), r.getAllocatedSubtask() );
            member.mySubtaskQueue.add( pair );
        } else {
            renewDE( member.reliabilityRankingAsM, r.getFrom(), 0, withBinary);
        }
    }

    public void reachSolicitation( Solicitation s ) {
        this.solicitationList.add( s );
    }

    public void reachResult( ResultOfTeamFormation r ) {
        this.resultList.add(r);
    }

    abstract protected void replyAsM(Agent ma);
    abstract protected void receiveAsM(Agent ma);
    abstract protected void execute(Agent la);
}
