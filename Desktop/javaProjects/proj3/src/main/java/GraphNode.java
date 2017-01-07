
import java.util.ArrayList;

/**
 * Created by rohansuresh on 4/15/16.
 */
public class GraphNode {
    private long id;
    private double lon;
    private double lat;
    private boolean marked = false;
    private ArrayList<GraphNode> connections = new ArrayList<>();

    public GraphNode(long name, double x, double y) {
        id = name;
        lon = x;
        lat = y;
    }

    public long getId() {
        return id;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public boolean getMarked() {
        return marked;
    }

    public ArrayList<GraphNode> getConnections() {
        return connections;
    }

    public boolean isConnection(GraphNode check) {
        for (GraphNode gn : connections) {
            if (gn.id == check.id) {
                return true;
            }
        }
        return false;
    }

    public void connect(GraphNode g) {
        connections.add(g);
    }

    public double calcDistance(double startLon, double startLat, double endLon, double endLat) {
        double lonDist = Math.abs(endLon - startLon);
        double latDist = Math.abs(endLat - startLat);
        double squared = Math.pow(lonDist, 2) + Math.pow(latDist, 2);
        return Math.sqrt(squared);
    }

    public void visit() {
        marked = true;
    }

    public void unmark() {
        marked = false;
    }
}
