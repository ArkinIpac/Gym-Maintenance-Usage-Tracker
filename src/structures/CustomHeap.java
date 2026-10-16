package structures;

import model.Equipment;

// Max-heap stored in an array, so the most urgent machine is always at index 0.
// For index i: parent = (i - 1) / 2, left = 2i + 1, right = 2i + 2.
// Only the top is guaranteed to be the biggest, which is all "what do I fix next?" needs.
// Insert and remove are O(log n), versus O(n) for a sorted or unsorted array.
public class CustomHeap {

    // priority = severity * severityWeight + usage * usageWeight
    // Severity is condition.ordinal() (Excellent 0 ... Critical 4).
    // 10,000 is bigger than the highest usage in the data (5,989), so a worse condition
    // always wins and usage only breaks ties within the same condition.
    //   EQ-00202 Critical, 194 uses   -> 40,194
    //   EQ-00155 Poor,     5,137 uses -> 35,137
    public static final long DEFAULT_SEVERITY_WEIGHT = 10_000;
    public static final long DEFAULT_USAGE_WEIGHT = 1;

    private long severityWeight = DEFAULT_SEVERITY_WEIGHT;
    private long usageWeight = DEFAULT_USAGE_WEIGHT;

    private Equipment[] items;
    private int size;

    public CustomHeap() {
        items = new Equipment[16];
        size = 0;
    }

    // long so large weights can't overflow
    public long priorityOf(Equipment e) {
        return e.getCondition().ordinal() * severityWeight + e.getUsageCount() * usageWeight;
    }

    // O(log n): add at the bottom, then bubble up.
    public void insert(Equipment e) {
        if (size == items.length) {
            grow();
        }
        items[size] = e;
        bubbleUp(size);
        size++;
    }

    // O(log n): take the root, move the last item to the top, then bubble it down.
    public Equipment extractMax() {
        if (size == 0) {
            throw new IllegalStateException("Heap is empty.");
        }
        Equipment max = items[0];
        size--;
        items[0] = items[size];
        items[size] = null;
        if (size > 0) {
            bubbleDown(0);
        }
        return max;
    }

    public Equipment peek() {
        if (size == 0) {
            throw new IllegalStateException("Heap is empty.");
        }
        return items[0];
    }

    // O(n) to find it (a heap isn't sorted) + O(log n) to fix the order.
    // Used when a machine is deleted or edited while it's waiting.
    public boolean remove(Equipment e) {
        for (int i = 0; i < size; i++) {
            if (items[i] == e) {
                size--;
                items[i] = items[size];
                items[size] = null;
                if (i < size) {
                    // The moved item may belong higher or lower; only one of these moves it.
                    bubbleUp(i);
                    bubbleDown(i);
                }
                return true;
            }
        }
        return false;
    }

    // New weights change every priority, so rebuild the heap: bubble down each parent
    // from the last one up to the root. O(n), cheaper than re-inserting everything.
    public void setWeights(long newSeverityWeight, long newUsageWeight) {
        if (newSeverityWeight < 0 || newUsageWeight < 0) {
            throw new IllegalArgumentException("Weights cannot be negative.");
        }
        severityWeight = newSeverityWeight;
        usageWeight = newUsageWeight;
        for (int i = size / 2 - 1; i >= 0; i--) {
            bubbleDown(i);
        }
    }

    public long getSeverityWeight() {
        return severityWeight;
    }

    public long getUsageWeight() {
        return usageWeight;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    // Copy in heap order (not sorted), so the UI can't change the real heap.
    public Equipment[] snapshot() {
        Equipment[] copy = new Equipment[size];
        System.arraycopy(items, 0, copy, 0, size);
        return copy;
    }

    private void bubbleUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (priorityOf(items[i]) <= priorityOf(items[parent])) {
                break;
            }
            swap(i, parent);
            i = parent;
        }
    }

    // Swap with the bigger child so the new parent beats both children.
    private void bubbleDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int largest = i;
            if (left < size && priorityOf(items[left]) > priorityOf(items[largest])) {
                largest = left;
            }
            if (right < size && priorityOf(items[right]) > priorityOf(items[largest])) {
                largest = right;
            }
            if (largest == i) {
                break;
            }
            swap(i, largest);
            i = largest;
        }
    }

    private void swap(int i, int j) {
        Equipment temp = items[i];
        items[i] = items[j];
        items[j] = temp;
    }

    private void grow() {
        Equipment[] bigger = new Equipment[items.length * 2];
        System.arraycopy(items, 0, bigger, 0, items.length);
        items = bigger;
    }
}
