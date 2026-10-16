package structures;

import java.util.Iterator;
import java.util.NoSuchElementException;

// Singly linked list with head and tail pointers.
// Used for service histories, the "recently serviced" list, BST buckets and graph neighbors.
// Iterable, so it works in for-each loops.
public class CustomLinkedList<T> implements Iterable<T> {

    private class Node {
        T data;
        Node next;
        Node(T data) { this.data = data; }
    }

    private Node head;
    private Node tail;  // kept so add() doesn't walk the whole list
    private int size;

    // Add at the end, O(1). History logs use this so the oldest record stays first.
    public void add(T data) {
        Node newNode = new Node(data);
        if (head == null) {
            head = newNode;
        } else {
            tail.next = newNode;
        }
        tail = newNode;
        size++;
    }

    // Add at the front, O(1). "Recently serviced" uses this so the newest is first.
    public void addFirst(T data) {
        Node newNode = new Node(data);
        newNode.next = head;
        head = newNode;
        if (tail == null) tail = newNode;
        size++;
    }

    // O(n). Returns -1 when not found.
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

    // O(n): no random access, so we walk from the head.
    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index);
        Node current = head;
        for (int i = 0; i < index; i++) current = current.next;
        return current.data;
    }

    // O(n). Removes the first match. We track prev because the only way to unlink a node
    // in a singly linked list is to make the node before it skip over it.
    public boolean remove(T target) {
        Node prev = null;
        Node current = head;
        while (current != null) {
            if (current.data.equals(target)) {
                if (prev == null) head = current.next;
                else prev.next = current.next;
                if (current == tail) tail = prev;
                size--;
                return true;
            }
            prev = current;
            current = current.next;
        }
        return false;
    }

    // O(n): the tail doesn't know the node before it, so we walk to it.
    // Fine here: it only runs once per service, on a list of at most 11 items.
    public T removeLast() {
        if (head == null) throw new NoSuchElementException("List is empty.");
        T data = tail.data;
        if (head == tail) {
            head = tail = null;
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
