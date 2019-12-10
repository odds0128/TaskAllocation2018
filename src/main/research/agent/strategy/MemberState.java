package main.research.agent.strategy;

import main.research.SetParam;
import main.research.agent.Agent;
import main.research.agent.AgentDePair;
import main.research.agent.AgentManager;
import main.research.communication.TransmissionPath;
import main.research.communication.message.Done;
import main.research.communication.message.ReplyToSolicitation;
import main.research.communication.message.ResultOfTeamFormation;
import main.research.communication.message.Solicitation;
import main.research.others.Pair;
import main.research.others.random.MyRandom;
import main.research.task.Subtask;

import static main.research.Manager.bin_;
import static main.research.Manager.getCurrentTime;
import static main.research.SetParam.Phase.*;
import static main.research.SetParam.ReplyType.ACCEPT;
import static main.research.SetParam.ReplyType.DECLINE;
import static main.research.SetParam.ResultType.*;

import java.util.*;

public abstract class MemberState implements Strategy, SetParam {
    protected boolean joinFlag = false;
    public List< AgentDePair > reliableLeadersRanking = new ArrayList<>(  );

    protected int expectedResultMessage = 0;
    public List<Pair<Agent, Subtask>> mySubtaskQueue = new ArrayList<>(  );  // consider Agentとsubtaskの順番逆のがよくね

    public static int idleTime = 0;
    public void actAsMember(Agent member) {
        preprocess( member );

        // CONSIDER: CPU使用量馬鹿高い
        Collections.sort( reliableLeadersRanking, Comparator.comparingDouble( AgentDePair::getDe ).reversed() );
        if      (member.phase == WAIT_FOR_SOLICITATION ) replyAsM(member);
        else if (member.phase == WAIT_FOR_SUBTASK )      receiveAsM(member);
        else if (member.phase == EXECUTE_SUBTASK )       execute(member);
        evaporateDE( reliableLeadersRanking );
    }

    private void preprocess( Agent member ) {
        assert mySubtaskQueue.size() <= SUBTASK_QUEUE_SIZE : "Over weight " + mySubtaskQueue.size();

        member.ms.replyToSolicitations( member, member.solicitationList );

        if ( mySubtaskQueue.isEmpty() && getCurrentTime() % bin_ == 0 ) idleTime++;
        while ( !member.resultList.isEmpty() ) {
            ResultOfTeamFormation r = member.resultList.remove( 0 );
            expectedResultMessage--;
            member.ms.reactToResultMessage( r );
        }

        while ( !member.doneList.isEmpty() ) {
            Done d = member.doneList.remove( 0 );
            member.ls.checkDoneMessage( member, d );
        }
    }

    public void setLeaderRankingRandomly( Agent self, List< Agent > agentList ) {
        List< Agent > tempList = AgentManager.generateRandomAgentList( agentList );
        for ( Agent temp: tempList ) {
            reliableLeadersRanking.add( new AgentDePair( temp, Agent.initial_de_ ) );
        }
        reliableLeadersRanking.remove( self );
    }

    public void reactToResultMessage( ResultOfTeamFormation r ) {
        if( r.getResult() == SUCCESS ) {
        	Pair<Agent, Subtask > pair =  new Pair<>( r.getFrom(), r.getAllocatedSubtask() );
            mySubtaskQueue.add( pair );
            r.getTo().ms.renewDE( reliableLeadersRanking , r.getFrom(), 1 );
        } else {
            r.getTo().ms.renewDE( reliableLeadersRanking, r.getFrom(), 0) ;
        }
    }

    // remove
    public static int tired_of_waiting = 0;
    private void replyAsM( Agent member ) {
        if( joinFlag ) {
            Strategy.proceedToNextPhase( member );
            joinFlag = false;
        } else if( getCurrentTime() - member.validatedTicks > THRESHOLD_FOR_ROLE_RENEWAL) {
            // remove
            tired_of_waiting++;
            member.inactivate(0);
        }
    }

    public void replyToSolicitations( Agent member, List< Solicitation > solicitations ) {
        if(  solicitations.isEmpty() ) return;

        solicitations.sort( ( solicitation1, solicitation2 ) ->
            compareSolicitations( solicitation1, solicitation2, reliableLeadersRanking ) );

        int capacity = SUBTASK_QUEUE_SIZE - mySubtaskQueue.size() - expectedResultMessage;
        while (  solicitations.size() > 0 && capacity-- > 0 ) {
            Solicitation s = Agent.epsilonGreedy( ) ? selectSolicitationRandomly( solicitations ) : solicitations.remove( 0 );
            TransmissionPath.sendMessage( new ReplyToSolicitation( member, s.getFrom(), ACCEPT, s.getExpectedSubtask() ) );
            expectedResultMessage++;
            joinFlag = true;
        }
        while ( ! solicitations.isEmpty() ) {
            Solicitation s = solicitations.remove( 0 );
            TransmissionPath.sendMessage( new ReplyToSolicitation( member,  s.getFrom(), DECLINE, s.getExpectedSubtask() ) );
        }
    }

    protected int compareSolicitations( Solicitation a, Solicitation b, List< AgentDePair > pairList ) {
        if( getPairByAgent( a.getFrom(), pairList ).getDe() < getPairByAgent( b.getFrom(), pairList ).getDe() ) return -1;
        else return 1;
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

        assert member.executionTime >= 0 : "sabotage";
        if ( --member.executionTime == 0) {
            // HACK
            Pair<Agent, Subtask> pair = mySubtaskQueue.remove( 0 );
            Agent   currentLeader  = pair.getKey();
            Subtask currentSubtask = pair.getValue();

            member.required[currentSubtask.resType]++;
            TransmissionPath.sendMessage( new Done( member, currentLeader ) );

            if ( withinTimeWindow() ) {
                member.workWithAsM[currentLeader.id]++;
                member.didTasksAsMember++;
            }
            mySubtaskQueue.remove(currentSubtask);
            // consider: nextPhaseと一緒にできない？
            if( mySubtaskQueue.isEmpty() ) member.inactivate(1);
            else {
                currentSubtask = mySubtaskQueue.get( 0 ).getValue();
                currentLeader = mySubtaskQueue.get( 0 ).getKey();

                member.allocated[ currentLeader.id ][ currentSubtask.resType ]++;
                member.executionTime = Agent.calculateExecutionTime( member, currentSubtask );
            }
        }
    }

    protected static Solicitation selectSolicitationRandomly( List< Solicitation > solicitations ) {
        int index = MyRandom.getRandomInt(0, solicitations.size() - 1);
        return solicitations.remove(index);
    }


    protected abstract void renewDE( List<AgentDePair> pairList, Agent target, double evaluation );
}
