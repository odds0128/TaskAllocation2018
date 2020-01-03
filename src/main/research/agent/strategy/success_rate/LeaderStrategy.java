package main.research.agent.strategy.success_rate;

import main.research.agent.Agent;
import main.research.agent.strategy.LeaderTemplateStrategy;
import main.research.communication.message.Done;
import main.research.communication.message.Solicitation;
import main.research.task.Subtask;
import main.research.task.Task;
import main.research.task.TaskManager;

import java.util.List;

import static main.research.communication.TransmissionPath.sendMessage;


public class LeaderStrategy extends LeaderTemplateStrategy {
	@Override
	protected Agent selectMemberFor( Subtask st ) {
		for ( Dependability pair: dependabilityRanking ) {
			Agent ag = pair.getAgent();
			if ( ( !exceptions.contains( ag ) ) && ag.canProcess( st ) ) return ag;
		}
		return null;
	}


	@Override
	public void sendSolicitations( Agent leader, List< Allocation > allocationList ) {
		for ( Allocation al : allocationList ) {
			sendMessage( new Solicitation( leader, al.getAg(), al.getSt() ) );
		}
	}

	@Override
	public void checkAllDoneMessages( Agent leader ) {
		while ( !leader.doneList.isEmpty() ) {
			Done d = leader.doneList.remove( 0 );
			Agent from = d.getFrom();
			Subtask st = d.getSt();
			removeAllocationHistory( from, st );

			renewDE( dependabilityRanking, from, 1 );
			exceptions.remove( from );

			// タスク全体が終わったかどうかの判定と，それによる処理
			// HACK: もうちょいどうにかならんか
			// 今終わったサブタスクasが含まれるtaskを見つける
			// それによってタスク全体が終われば終了報告等をする

			Task task = leader.findTaskContainingThisSubtask( st );
			task.subtasks.remove( st );
			if ( task.subtasks.isEmpty() ) {
				from.pastTasks.remove( task );
				TaskManager.finishTask(leader);
				from.didTasksAsLeader++;
			}
		}
	}

	@Override
	protected void renewDE( List< Dependability > pairList, Agent target, double evaluation ) {
		boolean b = evaluation > 0;

		Dependability pair = getDeByAgent( target, pairList );
		pair.renewDEby0or1( b );
	}
}
