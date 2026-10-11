
// CustomHeap.java
// Part of: Krissy Mae Gomez  (Heap + Priority Processing)
// Presented in: Slides 5-8 (Arkin Ipac) - heap order in the demo;
//               Slide 12 (Arkin Ipac) - why the severity weight is 10,000.
// What it does: Max-heap that ranks flagged equipment so the most urgent
//               machine is always serviced first.
// Used by: MaintenanceManager


package structures;

import model.Equipment;

// Our own MAX-heap, stored in a plain array. The most urgent equipment is always at index 0.
//
// The array IS the tree. For the item at index i:
//   parent      = (i - 1) / 2
//   left child  = 2i + 1
//   right child = 2i + 2
// So we don't need node objects or left/right pointers.
//
// Heap rule: every parent has a priority >= its children. The array is NOT fully sorted,
// only the top (index 0) is guaranteed to be the biggest. That is all we need, because the
// technician only ever asks "what should I fix next?"
//
// Why a heap: a sorted array would cost O(n) per insert to keep it in order; scanning an
// unsorted array for the max costs O(n) per service. The heap does both in O(log n).
public class CustomHeap {

    // ---------- Priority formula ----------
    //   priority = severity * severityWeight + usageCount * usageWeight
    //   severity = condition.ordinal():  EXCELLENT 0, GOOD 1, FAIR 2, POOR 3, CRITICAL 4
    //
    // Why 10,000 for severity:
    //   Condition is the safety issue, so a worse condition must ALWAYS rank higher.
    //   The highest usage in our dataset is 5,989. One step of severity (10,000) is bigger
    //   than any possible usage difference, so usage can never push a POOR machine above a
    //   CRITICAL one. Usage only decides the order inside the same condition: between two
    //   POOR treadmills, the busier one goes first because more members use it.
    //
    //   Example: EQ-00202 CRITICAL, 194 uses -> 4 * 10,000 + 194   = 40,194
    //            EQ-00155 POOR,   5,137 uses -> 3 * 10,000 + 5,137 = 35,137  (CRITICAL wins)
    //
    // The app always uses these two values (10,000 and 1); the Maintenance page has no
    // control for changing them. They are still variables instead of numbers typed into
    // the formula, so the rule lives in one place and setWeights() can change it if the
    // gym ever decides usage should count more. Main tests that bad weights are rejected.
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

    // Returns long, not int, so the result can't overflow even with very large weights.
    // FORMULA OF THE HEAP PRIORITY
    public long priorityOf(Equipment e) {
        return e.getCondition().ordinal() * severityWeight + e.getUsageCount() * usageWeight;
    }

    // ---------- Insert: O(log n) ----------
    // Put the new item in the next free slot (bottom of the tree), then bubble it up:
    // keep swapping with its parent while it has a higher priority.
    public void insert(Equipment e) {
        if (size == items.length) grow();
        items[size] = e;
        bubbleUp(size);
        size++;
    }

    // ---------- Remove the most urgent: O(log n) ----------
    // 1. Save the root (index 0), that is our answer.
    // 2. Move the LAST item into the root so the tree has no hole.
    // 3. Bubble it down until the heap rule holds again.
    public Equipment extractMax() {
        if (size == 0) throw new IllegalStateException("Heap is empty.");
        Equipment max = items[0];
        size--;
        items[0] = items[size];
        items[size] = null;
        if (size > 0) bubbleDown(0);
        return max;
    }

    // Look at the most urgent item without removing it. O(1).
    public Equipment peek() {
        if (size == 0) throw new IllegalStateException("Heap is empty.");
        return items[0];
    }

    // ---------- Remove a specific item: O(n) find + O(log n) fix ----------
    // Used when equipment is deleted while it is still waiting in the heap.
    // A heap is not sorted, so the only way to find an item is to scan the array.
    public boolean remove(Equipment e) {
        for (int i = 0; i < size; i++) {
            if (items[i] == e) {
                size--;
                items[i] = items[size];        // fill the hole with the last item
                items[size] = null;
                if (i < size) {
                    // The moved item might be too big for this spot OR too small.
                    // Only one of these will actually move it.
                    bubbleUp(i);
                    bubbleDown(i);
                }
                return true;
            }
        }
        return false;
    }

    // ---------- Change the weights (not used by the app's screens; tested in Main) ----------
    // New weights = new priorities, so the old order may be wrong. We rebuild it with
    // HEAPIFY: bubble down every parent, starting from the last parent (size/2 - 1) up to
    // the root. Leaves (the second half of the array) have no children, so we skip them.
    // This is O(n), faster than taking everything out and inserting it again (O(n log n)).
    public void setWeights(long newSeverityWeight, long newUsageWeight) {
        if (newSeverityWeight < 0 || newUsageWeight < 0) {
            throw new IllegalArgumentException("Weights cannot be negative.");
        }
        severityWeight = newSeverityWeight;
        usageWeight = newUsageWeight;
        for (int i = size / 2 - 1; i >= 0; i--) bubbleDown(i);
    }

    public long getSeverityWeight() { return severityWeight; }
    public long getUsageWeight() { return usageWeight; }
    public boolean isEmpty() { return size == 0; }
    public int size() { return size; }

    // A copy of the array in heap order (not sorted), so the UI can display it
    // without being able to change the real heap.
    public Equipment[] snapshot() {
        Equipment[] copy = new Equipment[size];
        System.arraycopy(items, 0, copy, 0, size);
        return copy;
    }

    // ---------- Helpers ----------

    // Move item i up while it is bigger than its parent.
    private void bubbleUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (priorityOf(items[i]) <= priorityOf(items[parent])) break;   // parent is bigger: done
            swap(i, parent);
            i = parent;
        }
    }

    // Move item i down while one of its children is bigger. Always swap with the
    // BIGGER child, so the new parent is larger than both children.
    private void bubbleDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int largest = i;
            if (left < size && priorityOf(items[left]) > priorityOf(items[largest])) largest = left;
            if (right < size && priorityOf(items[right]) > priorityOf(items[largest])) largest = right;
            if (largest == i) break;                                         // already bigger than both children
            swap(i, largest);
            i = largest;
        }
    }

    private void swap(int i, int j) {
        Equipment temp = items[i];
        items[i] = items[j];
        items[j] = temp;
    }

    // Array is full: make a new one twice as big and copy everything over.
    private void grow() {
        Equipment[] bigger = new Equipment[items.length * 2];
        System.arraycopy(items, 0, bigger, 0, items.length);
        items = bigger;
    }
}
