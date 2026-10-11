
// EquipmentStore.java
// Part of: Arkin Ipac  (array, add/delete, sync, sorting, searching)
//          John Benedict Jesalva  (usage index / BST parts, marked below)
// Presented in: Slide 4 (Arkin Ipac) - how the 3 structures stay in sync;
//               Slides 5-8 (Arkin Ipac) - add, search, delete demo;
//               Slide 11 (John Benedict Jesalva) - sorting and searching comparison.
// What it does: Holds all 5,000 machines and keeps the array, hash table
//               and BST in sync. Sorting and searching algorithms live here.
// Used by: MainFrame, every page, MaintenanceManager, Benchmark, Main


package service;

import model.Equipment;
import structures.CustomBST;
import structures.CustomHashTable;
import structures.CustomLinkedList;

// Holds all the equipment and keeps THREE structures in sync:
//   Equipment[] array       -> master list; used for sorting, linear search, and the table on screen
//   CustomHashTable (by ID) -> O(1) average lookup by ID, e.g. "EQ-04321"
//   CustomBST (by usage)    -> ordered questions, e.g. "used between 3,000 and 3,050 times"
//
// The same Equipment object is referenced by all three (not copied), so there is only
// one version of each machine. Every add, delete, and usage change goes through this
// class, so the three structures can never disagree with each other.
//
// The sorting and searching algorithms are also here, because they work on the array.
public class EquipmentStore {

    private Equipment[] equipment;                        // the array
    private int count;                                    // how many slots are actually used
    private final CustomHashTable<String, Equipment> idIndex;
    private final CustomBST usageIndex;

    public EquipmentStore() {
        this(16);
    }

    // The hash table's starting capacity can be chosen (live change: "change capacity").
    public EquipmentStore(int hashCapacity) {
        equipment = new Equipment[8];
        count = 0;
        idIndex = new CustomHashTable<>(hashCapacity);
        usageIndex = new CustomBST();
    }

    // ---------- Add ----------
    // Array: O(1) amortized. Hash table: O(1) average. BST: O(log n) average.
    // The duplicate check uses the hash table, so it costs O(1) instead of scanning the array.
    public void addEquipment(Equipment e) {
        if (idIndex.contains(e.getEquipmentId())) {
            throw new IllegalArgumentException("Equipment ID already exists: " + e.getEquipmentId());
        }
        if (count == equipment.length) grow();
        equipment[count++] = e;
        idIndex.put(e.getEquipmentId(), e);
        usageIndex.insert(e);
    }

    // When the array is full we DOUBLE it instead of adding 1 slot.
    // Copying n items is O(n), but after doubling we get n free slots before the next copy,
    // so on average each add is still O(1) (amortized).
    private void grow() {
        Equipment[] bigger = new Equipment[equipment.length * 2];
        System.arraycopy(equipment, 0, bigger, 0, count);
        equipment = bigger;
    }

    // ---------- Delete: O(n) ----------
    // The hash table finds the object in O(1), but the array still needs O(n):
    // we find its position, then shift every item after it one step left to close the gap.
    // Removed from all three structures. Returns null if the ID doesn't exist.
    public Equipment deleteEquipment(String id) {
        Equipment target = idIndex.get(id);
        if (target == null) return null;

        int index = -1;
        for (int i = 0; i < count; i++) {
            if (equipment[i] == target) { index = i; break; }
        }
        for (int i = index; i < count - 1; i++) {   // shift left
            equipment[i] = equipment[i + 1];
        }
        equipment[--count] = null;                  // clear the old last slot

        idIndex.remove(id);
        usageIndex.delete(target);
        return target;
    }

    // ===== [Jesalva] usage index: updateUsage() keeps the BST correct =====
    // ---------- Updates ----------

    // Usage count is the BST key. If we changed it in place, the equipment would stay in the
    // node for its OLD count and BST searches would miss it. So: delete from the BST with the
    // old value, change the number, insert again with the new value.
    public void updateUsage(Equipment e, int newUsage) {
        if (newUsage < 0) throw new IllegalArgumentException("Usage count cannot be negative.");
        usageIndex.delete(e);
        e.setUsageCount(newUsage);
        usageIndex.insert(e);
    }

    // Condition is not a key in any structure, so nothing needs re-indexing.
    // It still goes through here so all changes happen in one place.
    public void updateCondition(Equipment e, Equipment.Condition newCondition) {
        e.setCondition(newCondition);
    }

    // ---------- Read access ----------

    public int getCount() { return count; }

    // Returns a copy sized to 'count', so other classes can sort or change it
    // without touching our real array.
    public Equipment[] getAll() {
        Equipment[] copy = new Equipment[count];
        System.arraycopy(equipment, 0, copy, 0, count);
        return copy;
    }

    // [Jesalva] BST range query and access to the BST
    public CustomLinkedList<Equipment> getByUsageRange(int min, int max) {
        return usageIndex.getInRange(min, max);
    }

    // Used by the Reports page for traversals, the collision demo, and stats.
    public CustomBST getUsageIndex() { return usageIndex; }
    public CustomHashTable<String, Equipment> getIdIndex() { return idIndex; }

    // ===== [Arkin Ipac] searching and sorting =====
    // ---------- Searching ----------

    // Hash table lookup: O(1) average. This is what the app uses for normal searches.
    public Equipment findById(String id) {
        return idIndex.get(id);
    }

    // Linear search: O(n). Checks one record at a time from the start.
    // Works on unsorted data; we keep it as the baseline for comparison.
    public Equipment linearSearchById(String id) {
        for (int i = 0; i < count; i++) {
            if (equipment[i].getEquipmentId().equals(id)) return equipment[i];
        }
        return null;
    }

    // Binary search: O(log n). Only works on an array sorted by ID (ascending),
    // so the caller passes in sortByIdInsertion(true).
    // Each step checks the middle and throws away the half that can't contain the ID.
    // 5,000 records -> about 13 steps at most (2^13 = 8,192).
    // IDs are compared as Strings; this works because they all have the same
    // format and length (EQ- plus 5 digits), so "EQ-00010" < "EQ-00100".
    public Equipment binarySearchById(Equipment[] sortedById, String id) {
        int lo = 0, hi = sortedById.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;   // same as (lo + hi) / 2, but lo + hi can't overflow
            int cmp = sortedById[mid].getEquipmentId().compareTo(id);
            if (cmp == 0) return sortedById[mid];
            if (cmp < 0) lo = mid + 1;      // middle is smaller: answer is on the right
            else hi = mid - 1;              // middle is bigger: answer is on the left
        }
        return null;
    }

    // [Jesalva]
    // BST search by exact usage count: O(log n) average.
    public CustomLinkedList<Equipment> findByUsage(int usageCount) {
        return usageIndex.search(usageCount);
    }

    // ---------- Sorting (always on a COPY, the master array is never reordered) ----------

    // Bubble sort by usage count.
    // Each pass compares neighbors and swaps them if they're in the wrong order, so the
    // biggest value "bubbles" to the end. After pass i, the last i items are final, which
    // is why the inner loop stops at length - 1 - i.
    // O(n^2) average and worst. O(n) best: if a whole pass makes no swap, the array is
    // already sorted and we stop early.
    // ascending = true -> least used first; false -> most used first.
    public Equipment[] sortByUsageCountBubble(boolean ascending) {
        Equipment[] a = getAll();
        for (int i = 0; i < a.length - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < a.length - 1 - i; j++) {
                int diff = a[j].getUsageCount() - a[j + 1].getUsageCount();
                boolean outOfOrder = ascending ? diff > 0 : diff < 0;
                if (outOfOrder) {
                    Equipment temp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) break;   // early exit
        }
        return a;
    }

    // Insertion sort by equipment ID.
    // Take the next item ('key'), shift the bigger items before it one step right, and
    // drop the key into the gap. The left part of the array is always sorted.
    // O(n^2) worst, but O(n) when the data is almost sorted: our data loads in ID order,
    // so every key is already in place (4,999 comparisons, 0 shifts for 5,000 records).
    // ascending = true -> EQ-00001 first. Binary search needs this order.
    public Equipment[] sortByIdInsertion(boolean ascending) {
        Equipment[] a = getAll();
        for (int i = 1; i < a.length; i++) {
            Equipment key = a[i];
            int j = i - 1;
            while (j >= 0 && outOfOrderById(a[j], key, ascending)) {
                a[j + 1] = a[j];     // shift right to make room
                j--;
            }
            a[j + 1] = key;          // drop the key into the gap
        }
        return a;
    }

    // Insertion sort by usage count. Same key as the bubble sort, so the benchmark
    // compares both algorithms on exactly the same job.
    // Both make the same number of moves (6,241,429 at 5,000 records), but one shift is a
    // single assignment while one swap is three, so insertion sort finishes about 4x faster.
    public Equipment[] sortByUsageCountInsertion(boolean ascending) {
        Equipment[] a = getAll();
        for (int i = 1; i < a.length; i++) {
            Equipment key = a[i];
            int j = i - 1;
            while (j >= 0 && (ascending ? a[j].getUsageCount() > key.getUsageCount()
                                        : a[j].getUsageCount() < key.getUsageCount())) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
        return a;
    }

    // True if 'left' should come after 'right' in the chosen order.
    private boolean outOfOrderById(Equipment left, Equipment right, boolean ascending) {
        int cmp = left.getEquipmentId().compareTo(right.getEquipmentId());
        return ascending ? cmp > 0 : cmp < 0;
    }

    // Shortcuts with ascending as the default.
    public Equipment[] sortByUsageCountBubble() { return sortByUsageCountBubble(true); }
    public Equipment[] sortByIdInsertion() { return sortByIdInsertion(true); }
}
