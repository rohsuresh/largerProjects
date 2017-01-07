package editor;
import editor.Node;
import javafx.scene.text.Text;

/**
 * Created by rohansuresh on 2/28/16.
 */
public class Holder {

    public Node sentinel;
    public int size;
    public Node cursorSentinel;

    public Holder() {
        // empty linked list deque
        size = 0;
        sentinel = new Node(sentinel, null, sentinel);
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        cursorSentinel = sentinel.next;
        // in an empty list previous and next are sentinel itself
    }

    public void add(Text x) {
        if (isEmpty()) {
            addLast(x);
        } else {
            //Node beforeCursor = cursorSentinel;
            Node afterCursor = new Node(cursorSentinel, x, cursorSentinel.next);
            //System.out.println("Before:" + cursorSentinel.item);
            cursorSentinel.next = afterCursor;
            //System.out.println("New:" + cursorSentinel.next.item);
            afterCursor.prev = cursorSentinel;
            afterCursor.next = cursorSentinel.next.next;
            afterCursor.next.prev = cursorSentinel.next;
            //System.out.println("After new:" + afterCursor.next.item);
            cursorSentinel = cursorSentinel.next;
            size++;

        }
    }

    public void addLast(Text x) {
        Node oldLastNode = sentinel.prev;
        Node newLastNode = new Node(oldLastNode, x, sentinel);
        oldLastNode.next = newLastNode;
        sentinel.prev = newLastNode;
        cursorSentinel = cursorSentinel.next;
        size += 1;
    }

    public boolean isEmpty() {
        // returns true if deque is empty
        if (sentinel.next == sentinel) {
            // only sentinel is present and in structure
            size = 0;
            return true;
        } else {
            return false;
        }
    }

    public int size() {
        // returns num Nodes in deque
        // constant time
        return size;
    }

    public Text remove() {
        Node toRemove = cursorSentinel;
        toRemove.prev.next = toRemove.next;
        toRemove.next.prev = toRemove.prev;
        toRemove.next = cursorSentinel.next;
        cursorSentinel = cursorSentinel.prev;
        size--;
        return toRemove.item;
    }

    public Text get(int index) {
        // gets item at given index, doesn't change the linked list
        // use iteration
        if (index >= size) {
            return null;
        }

        int counter = 0;
        Node nodeToGet = sentinel.next;
        while (counter < index) {
            nodeToGet = nodeToGet.next;
            counter += 1;
        }
        return nodeToGet.item;
    }
}