import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class ShowGraph implements SetParam {
    static Color thickBlack = new Color( 0);
    static Color thickRed = new Color(255, 0, 0, 255);
    //    static Color resourse2 = new Color(255, 0, 0, 115);
    static Color thinRed = new Color(255, 0, 0, 50);
    static Color thickBlue = new Color(0, 0, 255, 255);
    //    static Color resourse2 = new Color(255, 0, 0, 115);
    static Color thinBlue = new Color(0, 0, 255, 128);
    static Color thickGreen = new Color(0, 255, 0, 255);
    static Color thinGreen = new Color(0, 255, 0, 56);

    static private ShowGraph singleton = new ShowGraph();
    Graph<Agent, Integer> graph = new DirectedSparseGraph<>();

    ShowGraph() {
    }

    void show2DGraph(List<Agent> agents) {
        for (Agent agent : agents) {
            graph.addVertex(agent);
        }
        int i = 0;
        for (Agent agent : agents) {

            if (agent.e_member > THRESHOLD_RECIPROCITY  && agent.relAgents.size() != 0) {
                graph.addEdge(i++, agent, agent.relAgents.get(0));
            }
            // */
            if (agent.e_leader > agent.e_member ) {
                for ( int j = 0; j < MAX_REL_AGENTS; j++ ) graph.addEdge(i++, agent, agent.relRanking.get(j) );
            }
        }
//        System.out.println("Graph G = " + graph);

        Layout<Agent, Integer> layout = new StaticLayout<>(graph);
        for (Agent agent : agents) {
            layout.setLocation(agent, new Point2D.Double(agent.x * 30 + 10, agent.y * 30 + 10));
        }
        BasicVisualizationServer<Agent, Integer> panel =
                new BasicVisualizationServer<>(layout, new Dimension(1000, 800));
        panel.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<Agent, Integer>());

        Transformer<Agent, Paint> nodeFillColor = new Transformer<Agent, Paint>() {
            @Override
            public Paint transform(Agent agent) {
                if (agent.e_leader > agent.e_member) {
                    return thickRed;
                }
                else {
                    // 互恵メンバエージェントは青, 合理は緑
                    if( agent.e_member > THRESHOLD_RECIPROCITY  && agent.relAgents.size() != 0 ) return thickBlue;
                    else return thickGreen;
                }
            }
        };


        panel.getRenderContext().setVertexFillPaintTransformer(nodeFillColor);
// */
        Transformer<Agent, Shape> nodeShapeTransformer = new Transformer<Agent, Shape>() {
            @Override
            public Shape transform(Agent n) {
                return new Rectangle(-5, -5, 10, 10);
            }
        };
        panel.getRenderContext().setVertexShapeTransformer(nodeShapeTransformer);
        JFrame frame = new JFrame("Graph View: Manual Layout");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
// */
    }

    void show3DGraph(List<Agent> agents) {

    }

    int classify(Agent agent) {
        int sum = 0;
        int count = 0;
        for (int i = 0; i < RESOURCE_NUM; i++) {
            if(agent.res[i] > 3) count++;
            sum += agent.res[i];
        }
//      if( count == RESOURCE_NUM ) return 3;
        if( count > 0 ) return 3;
        else return 1;
    }

}

