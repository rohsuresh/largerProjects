/**
 * Created by rohansuresh on 4/11/16.
 */

import java.util.ArrayList;

public class QuadTree {
    private Node rt;
    private ArrayList<Node> nodeList = new ArrayList<Node>();

    public QuadTree(Node tile) {
        rt = tile;
        constructTree(rt);
    }

    public Node getRt() {
        return rt;
    }

    public ArrayList<Node> getNodeList() {
        return nodeList;
    }

    private void constructTree(Node root) {

        for (int child = 0; child < 4; child++) {

            double ulx = root.getUlon();
            double uly = root.getUlat();
            double lrx = root.getLlon();
            double lry = root.getLlat();
            String fName = root.getId();

            if (fName.equals("root")) {
                fName = "";
            }

            double avgLon = (ulx + lrx) / 2;
            double avgLat = (uly + lry) / 2;


            if (child == 0) {
                fName += "1";
                lrx = avgLon;
                lry = avgLat;
            }
            if (child == 1) {
                fName += "2";
                lry = avgLat;
                ulx = avgLon;
            }
            if (child == 2) {
                fName += "3";
                uly = avgLat;
                lrx = avgLon;
            }
            if (child == 3) {
                fName += "4";
                ulx = avgLon;
                uly = avgLat;
            }
            Node toAdd = new Node(fName, ulx, uly, lrx, lry, root.getDepth() + 1);
            root.children.add(toAdd);
        }

        if (root.getDepth() + 1 < 7) {
            for (Node n : root.children) {
                constructTree(n);
            }
        }
    }


    public boolean intersects(Node root, Node query) {
        if (query.getUlon() >= root.getLlon() || root.getUlon() >= query.getLlon()) {
            return false;
        }
        if (query.getLlat() >= root.getUlat() || query.getUlat() <= root.getLlat()) {
            return false;
        }
        return true;
    }

    public int findDepth(Node r, double qDpp) {
        Node head = r;
        int depthTrack = 0;
        while (head.children.size() > 0 && depthTrack < 7) {
            double nodeDpp = (head.getLlon() - head.getUlon()) / 256;
            if (nodeDpp <= qDpp) {
                break;
            }
            head = head.children.get(0);
            depthTrack++;
        }
        return depthTrack;
    }

    public ArrayList<Node> getTiles(Node n, Node query) {
        if (intersects(n, query)) {
            if (n.getDepth() < query.getDepth()) {
                for (Node node : n.children) {
                    getTiles(node, query);
                }
            } else {
                nodeList.add(n);
            }
        }
        return nodeList;
    }

    public void clearTiles() {
        nodeList.clear();
    }

}
