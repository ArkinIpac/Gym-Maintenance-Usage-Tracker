
// CustomLinkedList.java
// Part of: Jay Mark Lampa  (Queue and Linked List)
// Presented in: Slides 5-8 (John Benedict Jesalva) - history linked list.
// What it does: Singly linked list. Holds each machine's service history,
//               the recently serviced list, BST buckets, and graph neighbors.
// Used by: MaintenanceManager, CustomBST, CustomGraph


package structures;

import java.util.Iterator;
import java.util.NoSuchElementException;

// Our own singly linked list (we do not use java.util.LinkedList).
// Each node only knows the NEXT node, not the previous one.
//
// Where we use it:
//   - each equipment's maintenance history (MaintenanceManager), oldest record first
//   - the "recently serviced" list, newest first, capped at 10 (MaintenanceManager)
//   - the bucket inside each BST node, for machines with the same usage count (CustomBST)
//   - the neighbor list of each zone, plus BFS/DFS results (CustomGraph)
//
// Why a linked list here and not an array: these lists grow one item at a time and we
// add at the front or back, which is O(1) with head/tail pointers. An array would need
// resizing, and adding at the front would shift every element.
//
// It implements Iterable so we can write: for (MaintenanceRecord r : history) { ... }
public class CustomLinkedList<T> implements Iterable<T> {

    private class Node {
        T data;
        Node next;
        Node(T data) { this.data = data; }
    }

    private Node head;  // first node
    private Node tail;  // last node, kept so add() does not have to walk the whole list
    private int size;

    // ---------- Insert ----------

    // Add at the end. O(1) because we jump straight to tail.
    // Used for history logs so the oldest service stays first.
    public void add(T data) {
        Node newNode = new Node(data);
        if (head == null) {
            head = newNode;          // empty list: new node is both head and tail
        } else {
            tail.next = newNode;     // link the old last node to the new one
        }
        tail = newNode;
        size++;
    }

    // Add at the front. O(1).
    // Used for the "recently serviced" list so the newest service is shown first.
    public void addFirst(T data) {
        Node newNode = new Node(data);
        newNode.next = head;
        head = newNode;
        if (tail == null) tail = newNode;   // list was empty, so this node is also the tail
        size++;
    }

    // Search 

    // O(n): start at head and check each node. Returns -1 if not found
    // (instead of throwing), so the caller can just show "not found".
    public int indexOf(T target) {
        Node current = head;
        int index = 0;
        while (current != null) {
            if (current.data.equals(target)) return index;
            current = current.next;
            index++;
        }
        return -1;
    }

    public boolean contains(T target) {
        return indexOf(target) != -1;
    }

    // O(n): no index access like an array, so we walk 'index' steps from head.
    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index);
        Node current = head;
        for (int i = 0; i < index; i++) current = current.next;
        return current.data;
    }

    // Delete 

    // O(n): find the first node holding 'target' and unlink it. Returns false if missing.
    // We keep 'prev' while walking because, in a singly linked list, the only way to
    // remove a node is to make the node BEFORE it skip over it.
    //   Case 1: target is the head   -> head moves to the second node
    //   Case 2: target is in the middle -> prev.next skips over it
    //   Case 3: target is the tail   -> same as case 2, and tail moves back to prev
    public boolean remove(T target) {
        Node prev = null;
        Node current = head;
        while (current != null) {
            if (current.data.equals(target)) {
                if (prev == null) head = current.next;   // case 1
                else prev.next = current.next;           // case 2 and 3
                if (current == tail) tail = prev;        // case 3: fix the tail pointer
                size--;
                return true;
            }
            prev = current;
            current = current.next;
        }
        return false;
    }

    // Remove the last node. This is O(n), not O(1): the tail does not know the node
    // before it, so we walk from head to the second-to-last node. (A doubly linked list
    // would make this O(1), but we only call it once per service, on a list of 11 items.)
    // Used to keep the "recently serviced" list at 10: the oldest entry falls off the end.
    public T removeLast() {
        if (head == null) throw new NoSuchElementException("List is empty.");
        T data = tail.data;
        if (head == tail) {
            head = tail = null;                          // only one node: list becomes empty
        } else {
            Node current = head;
            while (current.next != tail) current = current.next;
            current.next = null;
            tail = current;
        }
        size--;
        return data;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    // Traversal 

    // Walks from head to tail, one node at a time. This is what for-each loops call.
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node current = head;

            public boolean hasNext() { return current != null; }

            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }
}
