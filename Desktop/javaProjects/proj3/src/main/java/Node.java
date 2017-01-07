/**
 * Created by rohansuresh on 4/11/16.
 */

import java.io.File;
import java.util.ArrayList;

public class Node implements Comparable<Node> {
    private String id;
    private double ulat, ulon, llat, llon;
    //public Node nw, ne, sw, se;
    private File file;
    private int depth;
    ArrayList<Node> children = new ArrayList<>();


    public Node(String i, double lx, double ly, double rx, double ry, int d) {
        id = i;
        String s = id;
        ulon = lx;
        ulat = ly;
        llon = rx;
        llat = ry;
        depth = d;
    }

    public int compareTo(Node other) {
        if (this.ulat < other.ulat) {
            return 1;
        } else if (this.ulat > other.ulat) {
            return -1;
        } else {
            return 0;
        }
    }

    public int getDepth() {
        return depth;
    }

    public String getId() {
        return id;
    }

    public double getUlat() {
        return ulat;
    }

    public double getUlon() {
        return ulon;
    }

    public double getLlat() {
        return llat;
    }

    public double getLlon() {
        return llon;
    }

    public File getFile() {
        return file;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Node node = (Node) other;

        if (id != node.id) {
            return false;
        }
        if (Double.compare(node.ulat, ulat) != 0) {
            return false;
        }
        if (Double.compare(node.ulon, ulon) != 0) {
            return false;
        }
        if (Double.compare(node.llat, llat) != 0) {
            return false;
        }
        return Double.compare(node.llon, llon) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id.hashCode();
        temp = Double.doubleToLongBits(ulat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(ulon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(llat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(llon);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Node{"
                + "llat=" + llat
                + ", llon=" + llon
                + ", depth=" + depth
                + ", ulon=" + ulon
                + ", ulat=" + ulat
                + ", id='" + id + '\''
                + '}';
    }
}
