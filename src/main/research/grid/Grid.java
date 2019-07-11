package main.research.grid;

import main.research.agent.Agent;
import main.research.random.MyRandom;

import java.util.ArrayList;
import java.util.List;

import static main.research.SetParam.*;

public class Grid {
    private static Agent[][] grid = new Agent[MAX_X][MAX_Y];
    private static int[][] delays = new int[AGENT_NUM][AGENT_NUM];

    public static Coordinates newVacantSpot() {
        int tempX, tempY;
        do {
            tempX = MyRandom.getRandomInt(0, MAX_Y - 1);
            tempY = MyRandom.getRandomInt(0, MAX_X - 1);
        } while ( !isVacant( tempX, tempY ) );
        return new Coordinates( tempX, tempY );
    }

    private static boolean isVacant(int x, int y) {
        if (grid[y][x] == null) return true;
        else return false;
    }

    public static boolean setAgentOnEnvironment( Agent ag, int x, int y ) {
        if ( grid[y][x] != null ) {
            return false;
        }
        grid[y][x] = ag;
        setDelay(ag);
        return true;
    }

    private static List<Agent> agentList = new ArrayList<>();
    private static void setDelay( Agent from) {
        int delay;
        for ( Agent to: agentList ) {
            delay = calculateDelay( from.p, to.p );
            delays[from.id][to.id] = delay;
            delays[to.id][from.id] = delay;
        }
        agentList.add(from);
        if( agentList.size() == AGENT_NUM ) {
            agentList.clear();
        }
    }

    public static int getDelay( Agent from, Agent to ) {
        return delays[from.id][to.id];
    }

    /**
     * calcurateDelayメソッド
     * エージェント間のマンハッタン距離を計算し，returnするメソッド
     * □□□
     * □■□
     * □□□
     * このように座標を拡張し，真ん中からの距離を計算，その最短距離をとることで
     * トーラス構造の距離関係を割り出す
     */
    private static int calculateDelay(Coordinates from, Coordinates to) {
        int tillEnd = MAX_X / 2 + MAX_Y / 2;
        int minDistance = Integer.MAX_VALUE;
        int tilesX = 3, tilesY = 3;

        int fromX = from.getX();
        int fromY = from.getY();

        for (int i = 0; i < tilesX; i++) {
            int toX = to.getX() + (i - 1) * MAX_X;

            for (int j = 0; j < tilesY; j++) {
                int toY = to.getY() + (j - 1) * MAX_Y;
                int tempDistance = Math.abs(fromX - toX) + Math.abs(fromY - toY);

                if (tempDistance < minDistance) {
                    minDistance = tempDistance;
                }
            }
        }
        return (int) Math.ceil((double) minDistance / tillEnd * MAX_DELAY);
    }

    public static Agent[][] getGrid() {
        return grid;
    }

    /*
    TODO: clearと言っているのにその実態は新しくコレクションを生成していると言うのがおかしい．
      実験ごとにGridオブジェクトを作り直すかなんかする．
     */
    public static void clear() {
        grid = new Agent[MAX_X][MAX_Y];
        delays = new int[AGENT_NUM][AGENT_NUM];
    }
}
