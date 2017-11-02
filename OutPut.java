import java.util.*;

public class OutPut implements SetParam{

    /**
     * checkTaskメソッド
     * 現在タスクキューにあるタスクのサブタスク数とその必要リソースの表示.
     * @param taskQueue
     */
    static void checkTask(Queue<Task> taskQueue ) {
            int num = taskQueue.size();
            System.out.println("Queuesize: " + num);
            for (int i = 0; i < num; i++) {
                Task temp = taskQueue.poll();
                System.out.println( temp );
                System.out.println(temp.subTasks);
                taskQueue.add(temp);
            }
            System.out.println("  Remains: " + taskQueue.size() );
        }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(Agent[] agents) {
        System.out.println("Total Agents is " + Agent._id );
        System.out.println("Leaders is " + Agent._leader_num +", Members is " + Agent._member_num  );
        List<Agent> list = Arrays.asList(agents);
        Collections.sort(list, new AgentComparator());
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.println(agents[i]);
        }
    }

    /**
     * checkAgentメソッド
     * 二次元配列の場合
     */
    static void checkGrids(Agent[][] grids) {
        System.out.println("Total Agents is " + Agent._id );
        System.out.println("Leaders is " + Agent._leader_num +", Members is " + Agent._member_num );
        for (int i = 0; i < ROW; i++) {
            for( int j = 0; j < COLUMN; j++ ){
                if( grids[i][j] == null ) System.out.print(" 　 ");
                else System.out.print(String.format("%3d ", grids[i][j].id) );
            }
            System.out.println();
        }
    }

    /**
     * checkAgentメソッド
     *
     * @param agents
     */
    static void checkAgent(List<Agent> agents) {
        List<Agent> temp = new ArrayList<>(agents);

/*        for (int i = 0; i < AGENT_NUM; i++) {
            for( int j = 0; j < AGENT_NUM; j++ ){
                System.out.print(String.format("%3d", Agent._distance[i][j]));
            }
            System.out.println();
        }
*/
        Collections.sort(temp, new AgentComparator());
        for (int i = 0; i < AGENT_NUM; i++) {
            System.out.println(temp.get(i) );
        }
    }

    /**
         * checkRelメソッド
         * 信頼度が正しく更新されているか確認する
         */
    static void checkRel(Agent[][] agents) {
            System.out.println("Show Reliability:");
            for (int i = 0; i < ROW; i++) {
                for (int j = 0; j < COLUMN; j++) {
                    System.out.println("id: " + (i * ROW + j) );
                    for (int k = 0; k < ROW; k++) {
                        for (int l = 0; l < COLUMN; l++) {
                            System.out.print(String.format("%.3f", agents[i][j].rel[ k * ROW + l ]) + ", ");
                        }
                        System.out.println();
                    }
                }
            }
        }

    static void showResults(){

    }

}
