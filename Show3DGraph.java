import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Show3DGraph extends JFrame implements SetParam{
    int layer = 0;

    /**
     * 三次元グラフのウィンドウを起動する。
     */
    public Show3DGraph(List<Agent> agents) {
        // 閉じるボタンのクリック時、ウィンドウを破棄する。
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        // ウィンドウのサイズを設定する。
        setSize(1000, 800);
        // ウィンドウを表示する。
        setVisible( true );

        // 描画アルゴリズムを実行する。
        drawFunction(agents);
    }

    //////////////////////////////////////////////
    // 描画アルゴリズム START
    //////////////////////////////////////////////

    private static final double Xmin = -1;
    private static final double Ymin = -1;
    private static final double Zmin = -1;
    private static final double Xmax = 1;
    private static final double Zmax = 1;
    private static final double Ymax = 1;

    /**
     * 描画アルゴリズムを実行する。
     */
    private void drawFunction(List<Agent> agents) {
        double x, y, z;
        boolean ok, ok1;
        double lowerhorizon[] = new double[241];
        double upperhorizon[] = new double[241];

        for ( int i = 0; i <= 240; ++i ) {
            lowerhorizon[i] = Float.MAX_VALUE;
            upperhorizon[i] = Float.MIN_VALUE;
        }

        for( Agent agent: agents ){
            if( agent.role == LEADER ){
                x = Xmin + (Xmax - Xmin)/200 * agent.x;
                y = Ymin + (Ymax - Ymin)/200 * agent.y;
                z = Zmin + (Zmax - Zmin)/200 * layer;
                draw(x,y,z);
                for( Agent member: agent.relAgents ){
                    x = Xmin + (Xmax - Xmin)/200 * member.x;
                    y = Ymin + (Ymax - Ymin)/200 * member.y;
                    z = Zmin + (Zmax - Zmin)/200 * layer;
                    draw(x,y,z);
                }
                layer ++;
            }
        }

    }

    //////////////////////////////////////////////
    // 描画アルゴリズム END
    //////////////////////////////////////////////

    // ペンの座標
    private Coordinate pen;

    // 線分のリスト
    private List<Line> lines = new ArrayList<>();

    /**
     * 線分のリストの内容を描画する。
     */
    public void paint( Graphics g ) {
        // 表示内容をクリアする。
        g.clearRect( 0, 0, 1000, 600 );
        // リスト中のすべての線分を表示する。
        // うまく表示できるように座標の補正を行う。
        for ( Line line : lines ) {
            g.drawLine( adjust( line.start().x() ), 600 - adjust( line.start().y() ), adjust( line.end().x() ),600 -  adjust( line.end().y() ) );
        }
    }

    /**
     * double型の座標値を補正し、int型にする。
     * @param d 補正前の座標値
     * @return 補正後の座標値
     */
    private int adjust( double d ) {
        return (int) ( d * 4.0 );
    }

    /**
     * 線分をリストに追加する。
     */
    private void draw( double x, double y, double z) {
        // ペンの座標から指定した座標までの線分をリストに追加する。
        Coordinate temp = new Coordinate().x( x ).y( y );
        lines.add( new Line().start( pen ).end( temp ) );
        // ペンの座標を指定した座標に変更する。
        pen = new Coordinate().x( x ).y( y );
    }
    /**
     * ペンの座標を指定した座標に変更する。
     * @param x X座標
     * @param y Y座標
     */
    private void move_( double x, double y ) {
        pen = new Coordinate().x( x ).y( y );
    }

    private Container contentPane() {
        return getContentPane();
    }
    /**
     * 座標
     */
    class Coordinate {
        private double x;	// X座標
        private double y;	// Y座標

        /**
         * X座標を設定する。
         * @param x X座標
         * @return 自分
         */
        public Coordinate x( double x ) {
            this.x = x;
            return this;
        }

        /**
         * Y座標を設定する。
         * @param y Y座標
         * @return 自分
         */
        public Coordinate y( double y ) {
            this.y = y;
            return this;
        }

        /**
         * X座標を取得する。
         * @param
         */
        public double x() {
            return x;
        }

        /**
         * Y座標を取得する。
         * @param
         */
        public double y() {
            return y;
        }
    }

    /**
     * 線分
     */
    class Line {
        private Coordinate start;	// 始点の座標
        private Coordinate end;		// 終点の座標

        /**
         * 始点の座標を設定する。
         * @param start 始点の座標
         * @return 自分
         */
        public Line start( Coordinate start ) {
            this.start = start;
            return this;
        }

        /**
         * 終点の座標を設定する。
         * @param end 終点の座標
         * @return 自分
         */
        public Line end( Coordinate end ) {
            this.end = end;
            return this;
        }

        /**
         * 始点の座標を取得する。
         * @return 始点の座標
         */
        public Coordinate start() {
            return start;
        }

        /**
         * 終点の座標を取得する。
         * @return 終点の座標
         */
        public Coordinate end() {
            return end;
        }
    }
}
