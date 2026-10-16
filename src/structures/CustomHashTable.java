package structures;

// Hash table with separate chaining: an array of buckets, each holding a chain of entries.
// Keys that land in the same bucket (a collision) just join the chain.
// Used for ID lookups, the "already flagged?" check, service histories and the graph.
public class CustomHashTable<K, V> {

    private class Entry {
        K key;
        V value;
        Entry next;
        Entry(K key, V value) { this.key = key; this.value = value; }
    }

    private static final int DEFAULT_CAPACITY = 16;
    private static final double MAX_LOAD_FACTOR = 0.75;

    private Entry[] buckets;
    private int capacity;       // number of buckets
    private int size;           // number of keys
    private int resizeCount;

    public CustomHashTable() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public CustomHashTable(int initialCapacity) {
        if (initialCapacity < 1) throw new IllegalArgumentException("Capacity must be at least 1.");
        this.capacity = initialCapacity;
        this.buckets = new CustomHashTable.Entry[capacity];   // Java can't create a generic array directly
    }

    // hashCode() can be negative; masking the sign bit keeps the index positive.
    // (Math.abs fails for Integer.MIN_VALUE.)
    private int indexFor(K key) {
        return (key.hashCode() & 0x7fffffff) % capacity;
    }

    // O(1) average, O(n) worst (every key in one bucket).
    public void put(K key, V value) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null.");
        int index = indexFor(key);
        Entry current = buckets[index];
        while (current != null) {                 // existing key: update it
            if (current.key.equals(key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        Entry newEntry = new Entry(key, value);   // new key goes at the front of the chain
        newEntry.next = buckets[index];
        buckets[index] = newEntry;
        size++;
        if (size > capacity * MAX_LOAD_FACTOR) resize(capacity * 2);
    }

    // O(1) average. Returns null when the key isn't there.
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

    // O(1) average. Same unlinking as a linked list.
    public boolean remove(K key) {
        if (key == null) return false;
        int index = indexFor(key);
        Entry current = buckets[index];
        Entry prev = null;
        while (current != null) {
            if (current.key.equals(key)) {
                if (prev == null) buckets[index] = current.next;
                else prev.next = current.next;
                size--;
                return true;
            }
            prev = current;
            current = current.next;
        }
        return false;
    }

    // O(n): every key moves, because its bucket depends on the capacity.
    // Rare enough that put() stays O(1) amortized.
    @SuppressWarnings("unchecked")
    public void resize(int newCapacity) {
        if (newCapacity < 1) throw new IllegalArgumentException("Capacity must be at least 1.");
        Entry[] old = buckets;
        capacity = newCapacity;
        buckets = new CustomHashTable.Entry[capacity];
        size = 0;                                  // put() counts them again
        resizeCount++;
        for (Entry entry : old) {
            for (Entry current = entry; current != null; current = current.next) {
                put(current.key, current.value);
            }
        }
    }

    // The methods below are for the hash table view on the Reports page.

    public int bucketIndexOf(K key) { return indexFor(key); }

    public CustomLinkedList<K> keysInBucket(int index) {
        CustomLinkedList<K> keys = new CustomLinkedList<>();
        if (index < 0 || index >= capacity) return keys;
        for (Entry current = buckets[index]; current != null; current = current.next) {
            keys.add(current.key);
        }
        return keys;
    }

    // Keys that had to share a bucket.
    public int getCollisionCount() { return size - getUsedBucketCount(); }

    public int getUsedBucketCount() {
        int used = 0;
        for (Entry b : buckets) if (b != null) used++;
        return used;
    }

    // The worst-case number of checks for one lookup right now.
    public int getLongestChain() {
        int longest = 0;
        for (Entry b : buckets) {
            int length = 0;
            for (Entry current = b; current != null; current = current.next) length++;
            longest = Math.max(longest, length);
        }
        return longest;
    }

    public int size() { return size; }
    public int getCapacity() { return capacity; }
    public int getResizeCount() { return resizeCount; }
    public double getLoadFactor() { return (double) size / capacity; }
}
