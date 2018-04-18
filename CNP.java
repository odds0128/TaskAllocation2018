import java.util.List;

/**
 *
 */

public class CNP implements Strategy, SetParam{
    public void actAsLeader(Agent agent) {
        if (agent.phase == PROPOSITION) publicize(agent);
        else if (agent.phase == REPORT) organize(agent);
        else if (agent.phase == EXECUTION) execute(agent);
    }

    public void actAsMember(Agent agent) {
        if (Manager.getTicks() - agent.validatedTicks > ROLE_RENEWAL_TICKS) {
            agent.inactivate(0);
            return;
        }
        if (agent.phase == REPLY) bid(agent);
        else if (agent.phase == RECEPTION) waitResult(agent);
        else if (agent.phase == EXECUTION) execute(agent);
    }

    // publicizeメソッド ... 全エージェントに広報する
    private void publicize(Agent le){
        le.ourTask = Manager.getTask();
        if( le.ourTask == null ){
            le.inactivateWithNoLearning(0);
            return;
        }
        le.restSubTask = le.ourTask.subTaskNum;
        le.selectSubTask();
        for( Agent ag : Manager.getAgents() ){
            TransmissionPath.sendMessage(new Message(le, ag, PUBLICITY, le.ourTask, null));
        }
        le.nextPhase();
    }

    // bidメソッド ... リーダーに入札する
    private void bid(Agent mem){

    }

    // organizeメソッド ... 入札者の中から落札者を選びチームを編成する
    private void organize(Agent le){

    }

    // waitResultメソッド ... 入札がどうなったかを待つメソッド
    private void waitResult(Agent mem){

    }

    // executeメソッド ... 落札者とリーダーは自分の担当するサブタスクを実行する
    private void execute(Agent ag){

    }

    public List<Agent> selectMembers(Agent agent, List<SubTask> subtasks){
        return null;
    }
    public Agent selectLeader(Agent agent, List<Message> messages){
        return agent;
    }

    public void checkMessages(Agent self){
        // 現状，リーダーの広報時に自分からのメッセージが到着することがあり得ることに注意
        Message m ;
        int size = self.messages.size();
        for( int i = 0; i < size; i++ ){
            m = self.messages.remove(0);


            self.messages.add(m);
        }
    }
    public void clearStrategy(){

    }



}
