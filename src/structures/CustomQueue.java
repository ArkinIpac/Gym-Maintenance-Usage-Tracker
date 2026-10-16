package structures;

// FIFO queue built from linked nodes.
// Used for pending maintenance (reports keep their order) and as the BFS frontier.
// A queue instead of a stack so an old report can't be pushed back forever by newer ones.
public class CustomQueue<T> {

    private class Node {
        T data;
        Node next;
        Node(T data) { this.data = data; }
    }

    private Node front;  // next to leave
    private Node rear;   // last added
    private int size;

    // O(1)
    public void enqueue(T data) {
        Node newNode = new Node(data);
        if (rear == null) {
            front = newNode;
        } else {
            rear.next = newNode;
        }
        rear = newNode;
        size++;
    }

    // O(1)
    public T dequeue() {
        if (front == null) throw new IllegalStateException("Queue is empty.");
        T data = front.data;
        front = front.next;
        if (front == null) rear = null;   // removed the last item, so clear rear too
        size--;
        return data;
    }

    public T peek() {
        if (front == null) throw new IllegalStateException("Queue is empty.");
        return front.data;
    }

    public boolean isEmpty() { return front == null; }
    public int size() { return size; }
}
