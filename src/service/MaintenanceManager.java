package service;

import model.Equipment;
import model.MaintenanceRecord;
import structures.CustomHashTable;
import structures.CustomHeap;
import structures.CustomLinkedList;
import structures.CustomQueue;

// The maintenance workflow:
//   flag    -> the machine joins the queue in the order it was reported
//   promote -> the whole queue moves into the heap, which ranks it by urgency
//   service -> the most urgent machine comes off the heap and gets a history record
// The queue is the front-desk intake list; the heap is the technician's to-do list.
public class MaintenanceManager {

    private static final int RECENT_LIMIT = 10;

    private final EquipmentStore store;           // equipment changes go through the store

    private final CustomQueue<Equipment> pendingQueue = new CustomQueue<>();
    private final CustomHeap priorityHeap = new CustomHeap();

    // IDs waiting in the queue or heap, so a machine can't be flagged twice.
    private final CustomHashTable<String, Boolean> inWorkflow = new CustomHashTable<>();

    // ID -> that machine's service records, oldest first.
    private final CustomHashTable<String, CustomLinkedList<MaintenanceRecord>> historyLogs = new CustomHashTable<>();

    // Last 10 services across all machines, newest first.
    private final CustomLinkedList<MaintenanceRecord> recentlyServiced = new CustomLinkedList<>();

    public MaintenanceManager(EquipmentStore store) {
        this.store = store;
    }

    // O(1). Returns false if the machine is already waiting.
    public boolean flagForMaintenance(Equipment e) {
        if (inWorkflow.contains(e.getEquipmentId())) return false;
        pendingQueue.enqueue(e);
        inWorkflow.put(e.getEquipmentId(), true);
        return true;
    }

    // O(k log n). Moves the queue into the heap, front to back. Returns how many moved.
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

    // Next machine to service, or null if the heap is empty.
    public Equipment peekNext() {
        return priorityHeap.isEmpty() ? null : priorityHeap.peek();
    }

    // O(log n). The machine goes back to Good and its usage resets to 0 ("uses since last service").
    public MaintenanceRecord serviceNext(String note) {
        Equipment e = priorityHeap.extractMax();
        Equipment.Condition before = e.getCondition();

        // Through the store, not e.setUsageCount(0), so the BST re-files it under 0.
        store.updateCondition(e, Equipment.Condition.GOOD);
        store.updateUsage(e, 0);

        MaintenanceRecord record = new MaintenanceRecord(e.getEquipmentId(), note, before, e.getCondition());

        CustomLinkedList<MaintenanceRecord> log = historyLogs.get(e.getEquipmentId());
        if (log == null) {
            log = new CustomLinkedList<>();
            historyLogs.put(e.getEquipmentId(), log);
        }
        log.add(record);

        recentlyServiced.addFirst(record);
        if (recentlyServiced.size() > RECENT_LIMIT) recentlyServiced.removeLast();

        inWorkflow.remove(e.getEquipmentId());             // can be flagged again later
        return record;
    }

    // Used when a waiting machine is deleted. A queue can only remove from the front,
    // so we cycle through it once and re-add everyone except this machine. O(k).
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

    // After an edit, a machine in the heap may have a new priority, so take it out and put
    // it back in. Machines in the queue don't care: the queue is ordered by report time.
    public boolean refreshPriority(Equipment e) {
        if (!priorityHeap.remove(e)) return false;
        priorityHeap.insert(e);
        return true;
    }

    // Empties the heap without servicing anything; those machines can be flagged again.
    public int clearHeap() {
        int cleared = 0;
        while (!priorityHeap.isEmpty()) {
            Equipment e = priorityHeap.extractMax();
            inWorkflow.remove(e.getEquipmentId());
            cleared++;
        }
        return cleared;
    }

    // Clears only the recent list; each machine's own history is kept.
    public void clearRecentlyServiced() {
        while (!recentlyServiced.isEmpty()) recentlyServiced.removeLast();
    }

    public void setPriorityWeights(long severityWeight, long usageWeight) {
        priorityHeap.setWeights(severityWeight, usageWeight);
    }

    public long priorityOf(Equipment e) { return priorityHeap.priorityOf(e); }
    public long getSeverityWeight() { return priorityHeap.getSeverityWeight(); }
    public long getUsageWeight() { return priorityHeap.getUsageWeight(); }

    public int getPendingCount() { return pendingQueue.size(); }

    // Copy of the queue, front first. Cycles through it once so the order is unchanged. O(k).
    public Equipment[] getQueueSnapshot() {
        int k = pendingQueue.size();
        Equipment[] out = new Equipment[k];
        for (int i = 0; i < k; i++) {
            Equipment e = pendingQueue.dequeue();
            out[i] = e;
            pendingQueue.enqueue(e);
        }
        return out;
    }
    public int getHeapSize() { return priorityHeap.size(); }
    public Equipment[] getHeapSnapshot() { return priorityHeap.snapshot(); }

    // Empty list (never null) for a machine with no services yet.
    public CustomLinkedList<MaintenanceRecord> getHistory(String equipmentId) {
        CustomLinkedList<MaintenanceRecord> log = historyLogs.get(equipmentId);
        return (log != null) ? log : new CustomLinkedList<>();
    }

    public CustomLinkedList<MaintenanceRecord> getRecentlyServiced() {
        return recentlyServiced;
    }
}
