
// CustomHashTable.java
// Part of: Kahlil Mendoza  (Hash Table and Graph Representation)
// Presented in: Slides 5-8 (John Benedict Jesalva) - collision demo;
//               Slide 11 (Krissy Gomez) - hash vs linear vs binary vs BST.
// What it does: Hash table with separate chaining. O(1) average lookup of
//               equipment by ID; also used inside the graph and BFS/DFS.
// Used by: EquipmentStore, MaintenanceManager, CustomGraph


package structures;

// Our own hash table using SEPARATE CHAINING.
// It is an array of "buckets". Each bucket holds a small chain (linked entries) of every
// key that landed there. If two keys land in the same bucket, that is a COLLISION, and the
// new key is simply added to the chain. Nothing gets overwritten.
//
// Where we use it:
//   - EquipmentStore: equipment ID -> Equipment, so "find EQ-04321" is O(1) on average
//   - MaintenanceManager: ID -> history list, and the "already flagged?" check
//   - CustomGraph: zone -> neighbor list (the adjacency list), and BFS/DFS visited sets
//
// Why not just search the array? Linear search checks up to 5,000 records. The hash table
// jumps straight to one bucket and checks a short chain (our longest chain is 4).
public class CustomHashTable<K, V> {

    // One key-value pair. 'next' links entries that share the same bucket.
    private class Entry {
        K key;
        V value;
        Entry next;
        Entry(K key, V value) { this.key = key; this.value = value; }
    }

    private static final int DEFAULT_CAPACITY = 16;
    private static final double MAX_LOAD_FACTOR = 0.75;   // grow once 75% full

    private Entry[] buckets;
    private int capacity;       // number of buckets
    private int size;           // number of keys stored
    private int resizeCount;    // shown on the Reports page

    public CustomHashTable() {
        this(DEFAULT_CAPACITY);
    }

    // Starting capacity can be set by the caller. A small one like 7 makes collisions
    // easy to show during the demo; it is also the "change capacity" request.
    @SuppressWarnings("unchecked")
    public CustomHashTable(int initialCapacity) {
        if (initialCapacity < 1) throw new IllegalArgumentException("Capacity must be at least 1.");
        this.capacity = initialCapacity;
        this.buckets = new CustomHashTable.Entry[capacity];   // Java can't make a generic array directly
    }

    // Hash function: turns a key into a bucket number from 0 to capacity - 1.
    // hashCode() can be negative, and a negative % gives a negative index (crash).
    // '& 0x7fffffff' clears the sign bit so the number is always positive.
    // We don't use Math.abs because Math.abs(Integer.MIN_VALUE) is still negative.
    private int indexFor(K key) {
        return (key.hashCode() & 0x7fffffff) % capacity;
    }

    // ---------- Insert: O(1) average, O(n) worst ----------
    // Worst case only happens if every key falls into the same bucket.
    public void put(K key, V value) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null.");
        int index = indexFor(key);
        Entry current = buckets[index];
        while (current != null) {                 // same key already here? update it instead
            if (current.key.equals(key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        Entry newEntry = new Entry(key, value);   // new key: insert at the FRONT of the chain (O(1))
        newEntry.next = buckets[index];
        buckets[index] = newEntry;
        size++;
        // Too full means long chains and slower lookups, so double the buckets.
        if (size > capacity * MAX_LOAD_FACTOR) resize(capacity * 2);
    }

    // ---------- Search: O(1) average, O(n) worst ----------
    // Go to the key's bucket, then walk its chain. Returns null if not found.
    public V get(K key) {
        if (key == null) return null;
        Entry current = buckets[indexFor(key)];
        while (current != null) {
            if (current.key.equals(key)) return current.value;
            current = current.next;
        }
        return null;
    }

    public boolean contains(K key) { return get(key) != null; }

    // ---------- Delete: O(1) average, O(n) worst ----------
    // Same idea as removing from a linked list:
    // if the key is the first in the chain, the bucket points to the next entry;
    // otherwise the previous entry skips over it.
    public boolean remove(K key) {
        if (key == null) return false;
        int index = indexFor(key);
        Entry current = buckets[index];
        Entry prev = null;
        while (current != null) {
            if (current.key.equals(key)) {
                if (prev == null) buckets[index] = current.next;   // first in chain
                else prev.next = current.next;                     // middle or end of chain
                size--;
                return true;
            }
            prev = current;
            current = current.next;
        }
        return false;
    }

    // ---------- Resize (rehash): O(n) ----------
    // Every key has to be put again, because its bucket is (hash % capacity) and the
    // capacity just changed. This is expensive, but it only happens after many inserts,
    // so put() is still O(1) on average (amortized).
    @SuppressWarnings("unchecked")
    public void resize(int newCapacity) {
        if (newCapacity < 1) throw new IllegalArgumentException("Capacity must be at least 1.");
        Entry[] old = buckets;
        capacity = newCapacity;
        buckets = new CustomHashTable.Entry[capacity];
        size = 0;                                  // put() below counts them again
        resizeCount++;
        for (Entry entry : old) {
            for (Entry current = entry; current != null; current = current.next) {
                put(current.key, current.value);
            }
        }
    }

    // ---------- For the collision demo on the Reports page ----------

    // Which bucket a key goes to, e.g. EQ-00001 -> bucket 8144 (capacity 8,192).
    public int bucketIndexOf(K key) { return indexFor(key); }

    // Every key in one bucket. More than one key = a collision.
    public CustomLinkedList<K> keysInBucket(int index) {
        CustomLinkedList<K> keys = new CustomLinkedList<>();
        if (index < 0 || index >= capacity) return keys;
        for (Entry current = buckets[index]; current != null; current = current.next) {
            keys.add(current.key);
        }
        return keys;
    }

    // Keys that had to share a bucket = total keys minus buckets that are in use.
    public int getCollisionCount() { return size - getUsedBucketCount(); }

    public int getUsedBucketCount() {
        int used = 0;
        for (Entry b : buckets) if (b != null) used++;
        return used;
    }

    // The longest chain is the real worst-case number of checks for one lookup right now.
    public int getLongestChain() {
        int longest = 0;
        for (Entry b : buckets) {
            int length = 0;
            for (Entry current = b; current != null; current = current.next) length++;
            longest = Math.max(longest, length);
        }
        return longest;
    }

    // ---------- Info ----------
    public int size() { return size; }
    public int getCapacity() { return capacity; }
    public int getResizeCount() { return resizeCount; }
    public double getLoadFactor() { return (double) size / capacity; }
}
