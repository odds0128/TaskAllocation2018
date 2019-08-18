package main.research.grid;

import main.research.agent.Agent;
import main.research.others.random.MyRandom;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static main.research.SetParam.*;

public class Grid {
    private static Agent[][] grid = new Agent[MAX_X][MAX_Y];
    private static int[][] delays = new int[AGENT_NUM][AGENT_NUM];

    public static void setAgentOnEnvironment( Agent ag ) {
        ag.setPosition( newVacantSpot() );
        int tempX = ag.getX();
        int tempY = ag.getY();

        grid[tempY][tempX] = ag;
        setDelay(ag);
    }

    private static Point newVacantSpot() {
        int tempX, tempY;
        do {
            tempX = MyRandom.getRandomInt(0, MAX_Y - 1);
            tempY = MyRandom.getRandomInt(0, MAX_X - 1);
        } while ( !isVacant( tempX, tempY ) );
        return new Point( tempX, tempY );
    }

    private static boolean isVacant(int x, int y) {
        return grid[y][x] == null;
    }

    private static List<Agent> agentList = new ArrayList<>();
    private static void setDelay( Agent from) {
        if( from.id >= AGENT_NUM ) System.out.println("clear Agent._id from");
        int delay;
        for ( Agent to: agentList ) {
            if( to.id >= AGENT_NUM ) System.out.println("clear Agent._id to");
            delay = calculateDelay( from.getP(), to.getP() );
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
    private static int calculateDelay(Point from, Point to) {
        int tillEnd = MAX_X / 2 + MAX_Y / 2;
        int minDistance = Integer.MAX_VALUE;
        int tilesX = 3, tilesY = 3;

        int fromX = (int) from.getX();
        int fromY = (int) from.getY();

        for (int i = 0; i < tilesX; i++) {
            int toX = (int) to.getX() + (i - 1) * MAX_X;

            for (int j = 0; j < tilesY; j++) {
                int toY = (int) to.getY() + (j - 1) * MAX_Y;
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

    public static int[][] getDelays() {
        return delays;
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
