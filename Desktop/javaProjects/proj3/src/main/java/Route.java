import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * Created by rohansuresh on 4/15/16.
 */
public class Route {

    private SearchNode sNode;
    private PriorityQueue<SearchNode> pq;

    private class SearchNode implements Comparable<SearchNode> {
        double priority;
        double distance;
        GraphNode starting;
        SearchNode prevNode;

        SearchNode(SearchNode p, GraphNode g, GraphNode end, double d) {
            prevNode = p;
            starting = g;
            distance = d;
            priority = distance
                    + starting.calcDistance(starting.getLon(), starting.getLat(),
                    end.getLon(), end.getLat());
        }

        @Override
        public int compareTo(SearchNode input) {
            if (input == null) {
                return -1;
            } else if (this.priority < input.priority) {
                return -1;
            } else if (this.priority > input.priority) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    LinkedList<Long> path;

    public Route(GraphNode initial, GraphNode goal) {
        pq = new PriorityQueue<>();
        SearchNode sn = new SearchNode(null, initial, goal, 0);
        sn.starting.visit();
        pq.add(sn);

        while (sn.starting.getId() != goal.getId()) {
            sn = pq.poll();
            sn.starting.visit();
            for (GraphNode n : sn.starting.getConnections()) {
                if (!n.getMarked()) {
                    SearchNode next = new SearchNode(sn, n, goal,
                            sn.distance + n.calcDistance(n.getLon(), n.getLat(),
                                    sn.starting.getLon(), sn.starting.getLat()));
                    pq.add(next);
                }
            }
            sNode = sn;
            makeRoute();
        }

    }


    public LinkedList<Long> makeRoute() {
        path = new LinkedList<>();
        SearchNode finalNode = sNode;
        while (finalNode != null) {
            path.addFirst(finalNode.starting.getId());
            finalNode = finalNode.prevNode;
        }
        return path;
    }

    public PriorityQueue<SearchNode> getPq() {
        return pq;
    }

    public SearchNode getSNode() {
        return sNode;
    }
}
