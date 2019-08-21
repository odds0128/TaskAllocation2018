package main.research.agent.strategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentManager;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;

import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.DERenewalStrategy.withBinary;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.ReplyType.ACCEPT;
import static main.research.SetParam.ReplyType.DECLINE;
import static main.research.SetParam.ResultType.*;

import java.util.*;

public abstract class MemberStrategyWithRoleChange implements Strategy, SetParam {
    private boolean joinFlag = false;
    public Map< Agent, Double > reliableLeadersRanking = new LinkedHashMap<>( HASH_MAP_SIZE );

    private int expectedResultMessage = 0;
    List< Solicitation > solicitationList = new ArrayList<>();
    private List< ResultOfTeamFormation > resultList = new ArrayList<>();
    public List<Pair<Agent, Subtask>> mySubtaskQueue = new ArrayList<>(  );  // consider Agentとsubtaskの順番逆のがよくね

    public void actAsMember(Agent member) {
        assert mySubtaskQueue.size() <= SUBTASK_QUEUE_SIZE : "Over weight " + mySubtaskQueue.size();
        member.ms.replyToSolicitations( member, solicitationList);

        while( ! resultList.isEmpty() ) {
            ResultOfTeamFormation r = resultList.remove( 0 );
            expectedResultMessage--;
            reactToResultMessage( r );
        }

        while( ! member.ls.doneList.isEmpty() ) {
            Done d = member.ls.doneList.remove( 0 );
            member.ls.checkDoneMessage( member, d );
        }

        if      (member.phase == WAIT_FOR_SOLICITATION ) replyAsM(member);
        else if (member.phase == WAIT_FOR_SUBTASK )      receiveAsM(member);
        else if (member.phase == EXECUTE_SUBTASK )       execute(member);
        evaporateDE( reliableLeadersRanking );
    }

    public void setLeaderRankingRandomly( List< Agent > agentList ) {
        List< Agent > tempList = AgentManager.generateRandomAgentList( agentList );
        for ( Agent temp: tempList ) {
            reliableLeadersRanking.put( temp, INITIAL_VALUE_OF_DE );
        }
    }

    public void removeMyselfFromRanking( Agent self ) {
        reliableLeadersRanking.remove( self );
    }

    private void reactToResultMessage( ResultOfTeamFormation r ) {
        if( r.getResult() == SUCCESS ) {
        	Pair<Agent, Subtask > pair =  new Pair<>( r.getFrom(), r.getAllocatedSubtask() );
            mySubtaskQueue.add( pair );
        } else {
            r.getTo().ms.renewDE( reliableLeadersRanking, r.getFrom(), 0);
        }
    }

    private void replyAsM( Agent member ) {
        if( joinFlag ) {
            Strategy.proceedToNextPhase( member );
            joinFlag = false;
        } else if( getCurrentTime() - member.validatedTicks > THRESHOLD_FOR_ROLE_RENEWAL) {
            member.inactivate(0);
        }
    }

    private void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
        if(  solicitations.isEmpty() ) return;

        sortSolicitationByDEofLeader(  solicitations, reliableLeadersRanking );

        int capacity = SUBTASK_QUEUE_SIZE - mySubtaskQueue.size() - expectedResultMessage;
        while (  solicitations.size() > 0 && capacity-- > 0 ) {
            Solicitation s = MyRandom.epsilonGreedy( Agent.ε ) ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
            TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
            expectedResultMessage++;
            joinFlag = true;
        }
        while ( ! solicitations.isEmpty() ) {
            Solicitation s = solicitations.remove( 0 );
            TransmissionPath.sendMessage( new ReplyToSolicitation( member,  s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
        }
    }


    private void receiveAsM( Agent member ) {
        if( expectedResultMessage > 0 ) return;

        if ( mySubtaskQueue.isEmpty() ) {
            member.inactivate(0);
        } else {
            Subtask currentSubtask = mySubtaskQueue.get( 0 ).getValue();
            Agent currentLeader    = mySubtaskQueue.get( 0 ).getKey();

            member.allocated[currentLeader.id][currentSubtask.resType]++;
            member.executionTime = Agent.calculateExecutionTime(member, currentSubtask);
            Strategy.proceedToNextPhase(member);
        }
    }

    private void execute( Agent member ) {
        member.validatedTicks = getCurrentTime();

        if ( --member.executionTime == 0) {
            // HACK
            Pair<Agent, Subtask> pair = mySubtaskQueue.remove( 0 );
            Agent   currentLeader  = pair.getKey();
            Subtask currentSubtask = pair.getValue();

            member.required[currentSubtask.resType]++;
            TransmissionPath.sendMessage( new Done( member, currentLeader ) );
            member.ms.renewDE( reliableLeadersRanking , currentLeader, 1 );

            if ( Agent._coalition_check_end_time - getCurrentTime() < COALITION_CHECK_SPAN) {
                member.workWithAsM[currentLeader.id]++;
                member.didTasksAsMember++;
            }
            mySubtaskQueue.remove(currentSubtask);
            // consider: nextPhaseと一緒にできない？
            member.inactivate(1);
        }
    }


    private static void sortSolicitationByDEofLeader( List< Solicitation > solicitations, Map<Agent, Double> DEs ) {
        solicitations.sort( ( solicitation1, solicitation2 ) ->
            (int) ( DEs.get( solicitation2.getFrom() ) - DEs.get( solicitation1.getFrom() ) ));
    }

    static private Solicitation selectSolicitationRandomly( List< Solicitation > solicitations ) {
        int index = MyRandom.getRandomInt(0, solicitations.size() - 1);
        return solicitations.remove(index);
    }

    public void reachSolicitation( Solicitation s ) {
        this.solicitationList.add( s );
    }

    public void reachResult( ResultOfTeamFormation r ) {
        this.resultList.add(r);
    }

    protected abstract void renewDE( Map< Agent, Double > deMap, Agent target, double evaluation );
}
