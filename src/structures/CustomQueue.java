
// CustomQueue.java
// Part of: Jay Mark Lampa  (Queue and Linked List)
// Presented in: Slides 5-8 (John Benedict Jesalva) - flag -> queue -> promote.
// What it does: Pending maintenance line: flagged equipment waits here (FIFO)
//               until Promote moves it into the heap. Also the BFS frontier.
// Used by: MaintenanceManager, CustomGraph.bfs()


package structures;

// Our own FIFO queue (First In, First Out), built from linked nodes.
//
// Where we use it:
//   1. Pending maintenance (MaintenanceManager): equipment waits here in the order it was
//      reported, before it is moved into the priority heap.
//   2. BFS frontier (CustomGraph): zones are visited in the order they were discovered,
//      which is what makes BFS go level by level.
//
// Why a queue and not a stack: with a stack (LIFO), the newest report would always be
// handled first and an old report could wait forever. A queue keeps it fair.
//
// front = where we remove, rear = where we add. Keeping both pointers means enqueue and
// dequeue are both O(1); we never walk the list.
public class CustomQueue<T> {

    private class Node {
        T data;
        Node next;
        Node(T data) { this.data = data; }
    }

    private Node front;  // next item to leave
    private Node rear;   // most recently added item
    private int size;

    // Add to the back. O(1).
    public void enqueue(T data) {
        Node newNode = new Node(data);
        if (rear == null) {
            front = newNode;        // empty queue: the new node is also the front
        } else {
            rear.next = newNode;    // attach behind the current last node
        }
        rear = newNode;
        size++;
    }

    // Remove from the front. O(1).
    public T dequeue() {
        if (front == null) throw new IllegalStateException("Queue is empty.");
        T data = front.data;
        front = front.next;
        // If we just removed the only item, rear still points to it. Clear it too,
        // otherwise the next enqueue would attach to a node that is no longer in the queue.
        if (front == null) rear = null;
        size--;
        return data;
    }

    // Look at the front without removing it. O(1).
    public T peek() {
        if (front == null) throw new IllegalStateException("Queue is empty.");
        return front.data;
    }

    public boolean isEmpty() { return front == null; }
    public int size() { return size; }
}
