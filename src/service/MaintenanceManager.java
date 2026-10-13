
// MaintenanceManager.java
// Part of: Krissy Mae Gomez  (Heap + Priority Processing)
// Presented in: Slides 5-8 (Rhobe Arkin Ipac) - flag, promote, service demo.
// What it does: The flag -> queue -> promote -> heap -> service workflow,
//               plus the history logs written after each service.
// Used by: MaintenancePanel, Main


package service;

import model.Equipment;
import model.MaintenanceRecord;
import structures.CustomHashTable;
import structures.CustomHeap;
import structures.CustomLinkedList;
import structures.CustomQueue;

// Runs the maintenance workflow in 3 steps:
//   1. Flag    -> equipment goes into the QUEUE in the order it was reported (FIFO)
//   2. Promote -> everything in the queue moves into the HEAP, which orders it by urgency
//   3. Service -> the most urgent item is taken from the heap, fixed, and a record is
//                 added to its history LINKED LIST
//
// Why both a queue and a heap? They do different jobs. The queue is the intake line:
// it keeps reports in the order they came in, like a list at the front desk. The heap is
// the technician's to-do list: it always gives the most dangerous problem first, even if
// it was reported last.
public class MaintenanceManager {

    private static final int RECENT_LIMIT = 10;   // size of the "recently serviced" list

    private final EquipmentStore store;           // all changes to equipment go through the store

    private final CustomQueue<Equipment> pendingQueue = new CustomQueue<>();
    private final CustomHeap priorityHeap = new CustomHeap();

    // IDs that are currently waiting, either in the queue or in the heap.
    // Prevents the same machine from being flagged twice. Hash table = O(1) check.
    private final CustomHashTable<String, Boolean> inWorkflow = new CustomHashTable<>();

    // Service history of each machine: ID -> linked list of records, oldest first.
    private final CustomHashTable<String, CustomLinkedList<MaintenanceRecord>> historyLogs = new CustomHashTable<>();

    // Last 10 services across ALL machines, newest first.
    // addFirst() puts the new record in front; removeLast() drops the 11th (oldest).
    private final CustomLinkedList<MaintenanceRecord> recentlyServiced = new CustomLinkedList<>();

    public MaintenanceManager(EquipmentStore store) {
        this.store = store;
    }

    // Step 1: Flag (enqueue, O(1))
    // Returns false if this machine is already waiting, so the UI can show a message.
    public boolean flagForMaintenance(Equipment e) {
        if (inWorkflow.contains(e.getEquipmentId())) return false;
        pendingQueue.enqueue(e);
        inWorkflow.put(e.getEquipmentId(), true);
        return true;
    }

    // Step 2: Promote (k dequeues + k heap inserts = O(k log n))
    // Empties the queue front to back into the heap. Returns how many were moved.
    public int promoteQueueToHeap() {
        int moved = 0;
        while (!pendingQueue.isEmpty()) {
            priorityHeap.insert(pendingQueue.dequeue());
            moved++;
        }
        return moved;
    }

    public boolean hasEquipmentReadyForService() {
        return !priorityHeap.isEmpty();
    }

    // Who will be serviced next, without removing it. Null if the heap is empty.
    public Equipment peekNext() {
        return priorityHeap.isEmpty() ? null : priorityHeap.peek();
    }

    // Step 3: Service (extractMax, O(log n))
    // After servicing, the machine is set to GOOD and its usage counter goes back to 0,
    // meaning "uses since the last service".
    public MaintenanceRecord serviceNext(String note) {
        Equipment e = priorityHeap.extractMax();
        Equipment.Condition before = e.getCondition();

        // We go through the store instead of calling e.setUsageCount(0) directly,
        // because usage is the BST key. updateUsage() removes it from its old BST node
        // and inserts it again under 0, so the BST stays correct.
        store.updateCondition(e, Equipment.Condition.GOOD);
        store.updateUsage(e, 0);

        MaintenanceRecord record = new MaintenanceRecord(e.getEquipmentId(), note, before, e.getCondition());

        // First service for this machine? Create its history list.
        CustomLinkedList<MaintenanceRecord> log = historyLogs.get(e.getEquipmentId());
        if (log == null) {
            log = new CustomLinkedList<>();
            historyLogs.put(e.getEquipmentId(), log);
        }
        log.add(record);                                   // add at the end: oldest stays first

        recentlyServiced.addFirst(record);                 // newest first
        if (recentlyServiced.size() > RECENT_LIMIT) recentlyServiced.removeLast();

        inWorkflow.remove(e.getEquipmentId());             // it can be flagged again later
        return record;
    }

    // Cancel (when equipment is deleted while waiting)
    // The heap has its own remove(). A queue can only remove from the front, so we
    // dequeue every item once and enqueue it again, except the one we're deleting.
    // Everyone else keeps their original order. O(k) for k items in the queue.
    public boolean cancel(Equipment e) {
        if (!inWorkflow.contains(e.getEquipmentId())) return false;
        boolean removed = priorityHeap.remove(e);
        int waiting = pendingQueue.size();
        for (int i = 0; i < waiting; i++) {
            Equipment item = pendingQueue.dequeue();
            if (item == e) removed = true;
            else pendingQueue.enqueue(item);
        }
        inWorkflow.remove(e.getEquipmentId());
        return removed;
    }

    // Edit: fix the heap after a machine's condition or usage changes
    // An edited machine that is waiting in the heap now has a different priority, so its
    // spot may break the heap rule. We take it out and insert it again so it bubbles to the
    // right place: O(n) to find it (a heap isn't sorted) + O(log n) to insert.
    // Machines still in the queue need nothing: the queue is ordered by report time, not priority.
    public boolean refreshPriority(Equipment e) {
        if (!priorityHeap.remove(e)) return false;    // not in the heap
        priorityHeap.insert(e);
        return true;
    }

    // Clear (reset buttons on the Maintenance page)
    // Empties the heap without servicing anything. Each item is also removed from
    // inWorkflow so it can be flagged again. k extractMax calls = O(k log k).
    public int clearHeap() {
        int cleared = 0;
        while (!priorityHeap.isEmpty()) {
            Equipment e = priorityHeap.extractMax();
            inWorkflow.remove(e.getEquipmentId());
            cleared++;
        }
        return cleared;
    }

    // Empties only the "recently serviced" list. Each machine's own history log
    // (the History button) is kept. O(k), k is at most 10.
    public void clearRecentlyServiced() {
        while (!recentlyServiced.isEmpty()) recentlyServiced.removeLast();
    }

    // Priority weights 
    // The app keeps the default weights (severity 10,000, usage 1). setPriorityWeights()
    // is only called by Main's invalid-input test; priorityOf() is used by the heap table.
    public void setPriorityWeights(long severityWeight, long usageWeight) {
        priorityHeap.setWeights(severityWeight, usageWeight);
    }

    public long priorityOf(Equipment e) { return priorityHeap.priorityOf(e); }
    public long getSeverityWeight() { return priorityHeap.getSeverityWeight(); }
    public long getUsageWeight() { return priorityHeap.getUsageWeight(); }

    // Read access

    public int getPendingCount() { return pendingQueue.size(); }
    public int getHeapSize() { return priorityHeap.size(); }
    public Equipment[] getHeapSnapshot() { return priorityHeap.snapshot(); }

    // Never returns null. A machine with no services yet gets an empty list.
    public CustomLinkedList<MaintenanceRecord> getHistory(String equipmentId) {
        CustomLinkedList<MaintenanceRecord> log = historyLogs.get(equipmentId);
        return (log != null) ? log : new CustomLinkedList<>();
    }

    public CustomLinkedList<MaintenanceRecord> getRecentlyServiced() {
        return recentlyServiced;
    }
}
