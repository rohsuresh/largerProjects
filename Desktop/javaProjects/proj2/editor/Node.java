package editor;
        import javafx.scene.text.Text;

/**
 * Created by rohansuresh on 3/1/16.
 */
public class Node {
    public Node prev;
    public Text item;
    public Node next;

    public Node (Node p, Text x, Node n) {
        // parameters: Node, previous Node, next Node
        prev = p;
        item = x;
        next = n;
    }
}
