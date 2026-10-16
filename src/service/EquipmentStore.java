package service;

import model.Equipment;
import structures.CustomBST;
import structures.CustomHashTable;
import structures.CustomLinkedList;

// Holds every machine in three structures that always stay in sync:
//   array      - master list, used for sorting, linear search and the table on screen
//   hash table - O(1) average lookup by ID
//   BST        - ordered questions by usage count
// All three point to the same Equipment objects, and every add, delete and usage change
// goes through this class, so they can't disagree. The sorts and searches live here too.
public class EquipmentStore {

    private Equipment[] equipment;
    private int count;                                    // slots actually in use
    private final CustomHashTable<String, Equipment> idIndex;
    private final CustomBST usageIndex;

    public EquipmentStore() {
        this(16);
    }

    public EquipmentStore(int hashCapacity) {
        equipment = new Equipment[8];
        count = 0;
        idIndex = new CustomHashTable<>(hashCapacity);
        usageIndex = new CustomBST();
    }

    // Array O(1) amortized, hash table O(1) average, BST O(log n) average.
    // The duplicate check uses the hash table instead of scanning the array.
    public void addEquipment(Equipment e) {
        if (idIndex.contains(e.getEquipmentId())) {
            throw new IllegalArgumentException("Equipment ID already exists: " + e.getEquipmentId());
        }
        if (count == equipment.length) grow();
        equipment[count++] = e;
        idIndex.put(e.getEquipmentId(), e);
        usageIndex.insert(e);
    }

    // Doubling (instead of adding one slot) keeps each add O(1) on average.
    private void grow() {
        Equipment[] bigger = new Equipment[equipment.length * 2];
        System.arraycopy(equipment, 0, bigger, 0, count);
        equipment = bigger;
    }

    // O(n): the hash table finds the machine instantly, but the array still has to shift
    // everything after it one step left. Returns null if the ID doesn't exist.
    public Equipment deleteEquipment(String id) {
        Equipment target = idIndex.get(id);
        if (target == null) return null;

        int index = -1;
        for (int i = 0; i < count; i++) {
            if (equipment[i] == target) { index = i; break; }
        }
        for (int i = index; i < count - 1; i++) {
            equipment[i] = equipment[i + 1];
        }
        equipment[--count] = null;

        idIndex.remove(id);
        usageIndex.delete(target);
        return target;
    }

    // Usage is the BST key, so changing it in place would leave the machine under its old
    // key. Remove, change, insert again.
    public void updateUsage(Equipment e, int newUsage) {
        if (newUsage < 0) throw new IllegalArgumentException("Usage count cannot be negative.");
        usageIndex.delete(e);
        e.setUsageCount(newUsage);
        usageIndex.insert(e);
    }

    // Everything except the ID. Input is checked first so a bad edit changes nothing.
    // Only usage needs re-indexing; the array and hash table share the same object.
    public void updateDetails(Equipment e, String name, Equipment.Category category, String location,
                              Equipment.Condition condition, int usage) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Equipment name cannot be empty.");
        if (usage < 0) throw new IllegalArgumentException("Usage count cannot be negative.");
        e.setName(name);
        e.setCategory(category);
        e.setLocation(location);
        updateCondition(e, condition);
        if (usage != e.getUsageCount()) updateUsage(e, usage);
    }

    public void updateCondition(Equipment e, Equipment.Condition newCondition) {
        e.setCondition(newCondition);
    }

    public int getCount() { return count; }

    // A copy, so callers can sort it without touching the real array.
    public Equipment[] getAll() {
        Equipment[] copy = new Equipment[count];
        System.arraycopy(equipment, 0, copy, 0, count);
        return copy;
    }

    public CustomLinkedList<Equipment> getByUsageRange(int min, int max) {
        return usageIndex.getInRange(min, max);
    }

    public CustomBST getUsageIndex() { return usageIndex; }
    public CustomHashTable<String, Equipment> getIdIndex() { return idIndex; }

    // Searching

    // Hash table, O(1) average. This is what the app uses.
    public Equipment findById(String id) {
        return idIndex.get(id);
    }

    // Linear search, O(n). Kept as the baseline for comparison.
    public Equipment linearSearchById(String id) {
        for (int i = 0; i < count; i++) {
            if (equipment[i].getEquipmentId().equals(id)) return equipment[i];
        }
        return null;
    }

    // Binary search, O(log n), on an array sorted by ID (pass in sortByIdInsertion(true)).
    // About 13 steps at most for 5,000 records. Comparing IDs as strings works because they
    // all have the same format and length.
    public Equipment binarySearchById(Equipment[] sortedById, String id) {
        int lo = 0, hi = sortedById.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;   // avoids overflow from (lo + hi)
            int cmp = sortedById[mid].getEquipmentId().compareTo(id);
            if (cmp == 0) return sortedById[mid];
            if (cmp < 0) lo = mid + 1;
            else hi = mid - 1;
        }
        return null;
    }

    // BST, O(log n) average.
    public CustomLinkedList<Equipment> findByUsage(int usageCount) {
        return usageIndex.search(usageCount);
    }

    // Sorting (always on a copy, the real array keeps its order)

    // Bubble sort by usage. O(n^2), or O(n) if a pass makes no swaps (already sorted).
    // After pass i the last i items are in place, so the inner loop gets shorter.
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
            if (!swapped) break;
        }
        return a;
    }

    // Insertion sort by ID. O(n^2) worst, but close to O(n) here because the data already
    // loads in ID order. Binary search needs this order.
    public Equipment[] sortByIdInsertion(boolean ascending) {
        Equipment[] a = getAll();
        for (int i = 1; i < a.length; i++) {
            Equipment key = a[i];
            int j = i - 1;
            while (j >= 0 && outOfOrderById(a[j], key, ascending)) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
        return a;
    }

    // Insertion sort by usage, the same job as the bubble sort so the benchmark is fair.
    // Same number of moves, but a shift is one assignment and a swap is three, so this
    // finishes about 4x faster.
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

    private boolean outOfOrderById(Equipment left, Equipment right, boolean ascending) {
        int cmp = left.getEquipmentId().compareTo(right.getEquipmentId());
        return ascending ? cmp > 0 : cmp < 0;
    }

    public Equipment[] sortByUsageCountBubble() { return sortByUsageCountBubble(true); }
    public Equipment[] sortByIdInsertion() { return sortByIdInsertion(true); }
}
