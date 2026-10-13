
// Main.java
// Part of: Arkin Ipac  (Integration)
// Presented in: Slides 5-8 backup only (John Benedict Jesalva) if the Swing
//               demo fails; Slide 14 (Krissy Gomez) - test cases T01-T10 come
//               from this output.
// What it does: Console demo of every required feature, steps 1 to 11.
//               Backup proof if the Swing demo has a problem.
// Used by: run directly (not used by the app)


import benchmark.Benchmark;
import db.DatasetImporter;
import db.EquipmentDAO;
import java.util.List;
import model.Equipment;
import model.MaintenanceRecord;
import service.EquipmentStore;
import service.GymFloorPlan;
import service.MaintenanceManager;
import structures.CustomBST;
import structures.CustomGraph;
import structures.CustomHashTable;
import structures.CustomLinkedList;

// Console demo of every required feature, numbered 1 to 11 like the project spec.
// We used its output for our test cases and traces, so the results come from the
// real 5,000-record dataset.
//
// The real program for users is the Swing app (ui.MainFrame). This class is a quick
// text-only check that each structure works.
public class Main {

    public static void main(String[] args) {

        // ===== 1. Load project data =====
        section("1. Load project data");
        List<Equipment> loaded = loadData();
        if (loaded == null) return;
        EquipmentStore store = new EquipmentStore();
        for (Equipment e : loaded) store.addEquipment(e);   // fills array + hash table + BST
        MaintenanceManager manager = new MaintenanceManager(store);
        System.out.println("Records in store: " + store.getCount());

        // ===== 2. Add, search, and delete records
        section("2. Add, search, and delete");
        Equipment added = new Equipment("EQ-09001", "Demo Rowing Machine", Equipment.Category.CARDIO,
                "Cardio Zone A", Equipment.Condition.FAIR, 150);
        store.addEquipment(added);
        System.out.println("Added:    " + added);
        System.out.println("Searched: " + store.findById("EQ-09001"));
        System.out.println("Deleted:  " + store.deleteEquipment("EQ-09001"));
        System.out.println("Count back to " + store.getCount());

        // ===== 3. Search for a missing record =====
        section("3. Missing record handled gracefully");
        Equipment missing = store.findById("EQ-99999");
        System.out.println(missing == null ? "EQ-99999 not found (no crash)." : "Unexpected: " + missing);
        System.out.println("Deleting it anyway returns: " + store.deleteEquipment("EQ-99999"));

        // ===== 5. Queue operation (done before 4 so there is history to traverse) =====
        section("5. Queue -> heap maintenance workflow");
        Equipment poorBusy = null, criticalQuiet = null;
        for (Equipment e : store.getAll()) {
            if (poorBusy == null && e.getCondition() == Equipment.Condition.POOR && e.getUsageCount() > 5000) poorBusy = e;
            if (criticalQuiet == null && e.getCondition() == Equipment.Condition.CRITICAL && e.getUsageCount() < 200) criticalQuiet = e;
        }
        Equipment third = store.findById("EQ-00001");
        System.out.println("Flag (in this order):");
        for (Equipment e : new Equipment[]{poorBusy, criticalQuiet, third}) {
            manager.flagForMaintenance(e);
            System.out.println("  enqueue " + e + "  priority=" + manager.priorityOf(e));
        }
        System.out.println("Flag EQ-00001 again -> accepted? " + manager.flagForMaintenance(third));
        System.out.println("Promoted " + manager.promoteQueueToHeap() + " items into the heap.");
        System.out.println("Service order (highest priority first, NOT flag order):");
        while (manager.hasEquipmentReadyForService()) {
            System.out.println("  " + manager.serviceNext("Console demo service"));
        }

        // ===== 4. Traverse the custom linked list =====
        section("4. Linked list traversal");
        System.out.println("Recently serviced (newest first):");
        for (MaintenanceRecord r : manager.getRecentlyServiced()) System.out.println("  " + r);
        System.out.println("History of " + criticalQuiet.getEquipmentId() + ":");
        for (MaintenanceRecord r : manager.getHistory(criticalQuiet.getEquipmentId())) System.out.println("  " + r);

        // ===== 6. BST insert, search, delete, traverse =====
        section("6. BST (small tree for tracing)");
        CustomBST demo = new CustomBST();
        int[] keys = {50, 30, 70, 20, 40, 60, 80};
        for (int k : keys) {
            demo.insert(new Equipment("T-" + k, "Trace item", Equipment.Category.ACCESSORY,
                    "Storage Room", Equipment.Condition.GOOD, k));
        }
        System.out.println("Inserted keys: 50 30 70 20 40 60 80");
        System.out.println("Inorder:   " + join(demo.inorder()));
        System.out.println("Preorder:  " + join(demo.preorder()));
        System.out.println("Postorder: " + join(demo.postorder()));
        System.out.println("Search 40: " + demo.search(40).size() + " found; search 45: " + demo.search(45).size() + " found");
        System.out.println("Delete root " + demo.deleteRoot() + " -> new root " + demo.getRootKey());
        System.out.println("Inorder after delete: " + join(demo.inorder()));
        CustomBST real = store.getUsageIndex();
        System.out.println("Real usage index: " + real.getNodeCount() + " nodes, height " + real.height()
                + ", root key " + real.getRootKey());
        System.out.println("Equipment used 3000-3010 times: " + store.getByUsageRange(3000, 3010).size());

        // ===== 7. Hash table + collision =====
        section("7. Hash table and collisions");
        CustomHashTable<String, Equipment> ids = store.getIdIndex();
        System.out.printf("Capacity %d, size %d, load %.2f, collisions %d, longest chain %d%n",
                ids.getCapacity(), ids.size(), ids.getLoadFactor(), ids.getCollisionCount(), ids.getLongestChain());
        int bucket = ids.bucketIndexOf("EQ-00001");
        System.out.println("EQ-00001 -> bucket " + bucket + " shared with: " + join(ids.keysInBucket(bucket)));
        // A tiny table with 7 buckets: put EQ-00001, then search for another ID that lands
        // in the same bucket, so we can always show a real collision.
        CustomHashTable<String, String> tiny = new CustomHashTable<>(7);
        tiny.put("EQ-00001", "Medicine Ball 1");
        int target = tiny.bucketIndexOf("EQ-00001");
        String collider = null;
        for (int i = 2; collider == null; i++) {
            String id = String.format("EQ-%05d", i);
            if (tiny.bucketIndexOf(id) == target) collider = id;
        }
        tiny.put(collider, "collides with EQ-00001");
        tiny.put("EQ-00002", "no collision");
        System.out.println("Capacity-7 table (" + tiny.size() + " keys, " + tiny.getCollisionCount() + " collision):");
        for (int b = 0; b < tiny.getCapacity(); b++) System.out.println("  bucket " + b + ": " + join(tiny.keysInBucket(b)));
        System.out.println("  get " + collider + " -> " + tiny.get(collider) + " (found by walking the chain)");
        System.out.println("  remove " + collider + " -> " + tiny.remove(collider) + ", get again -> " + tiny.get(collider));
        System.out.println("  EQ-00001 still there -> " + tiny.get("EQ-00001"));

        // ===== 8. BFS and DFS =====
        section("8. Graph: BFS and DFS");
        CustomGraph floor = GymFloorPlan.build();
        System.out.println(floor.getZoneCount() + " zones, " + floor.getEdgeCount() + " edges, "
                + floor.countComponents() + " component(s)");
        System.out.println("BFS from Entrance: " + join(floor.bfs("Entrance")));
        System.out.println("DFS from Entrance: " + join(floor.dfs("Entrance")));
        System.out.println("Fewest hops Entrance -> Olympic Lifting Platform: "
                + join(floor.shortestPath("Entrance", "Olympic Lifting Platform")));
        System.out.println("Within 1 hop of Entrance: " + join(floor.zonesWithinHops("Entrance", 1)));
        floor.removeEdge("Cardio Mezzanine", "Rooftop Deck");
        System.out.println("After removing Cardio Mezzanine - Rooftop Deck: " + floor.countComponents()
                + " components; Rooftop Deck reachable? " + floor.isReachable("Entrance", "Rooftop Deck"));

        // ===== 9. Sorting with two algorithms =====
        section("9. Sorting (top 3 of each)");
        Equipment[] byUsageDesc = store.sortByUsageCountBubble(false);
        Equipment[] byUsageAsc = store.sortByUsageCountInsertion(true);
        System.out.println("Bubble, most used first:");
        for (int i = 0; i < 3; i++) System.out.println("  " + byUsageDesc[i]);
        System.out.println("Insertion, least used first:");
        for (int i = 0; i < 3; i++) System.out.println("  " + byUsageAsc[i]);

        // ===== 10. Benchmarks and complexity =====
        section("10. Benchmarks and complexity");
        System.out.println(Benchmark.run(store.getAll()));
        System.out.println(Benchmark.complexityTable());

        // ===== 11. Invalid inputs =====
        section("11. Invalid inputs handled gracefully");
        tryInvalid("Duplicate ID", () -> store.addEquipment(new Equipment("EQ-00001", "Copy",
                Equipment.Category.CARDIO, "Cardio Zone A", Equipment.Condition.GOOD, 1)));
        tryInvalid("Empty ID", () -> new Equipment("  ", "No ID", Equipment.Category.CARDIO,
                "Cardio Zone A", Equipment.Condition.GOOD, 1));
        tryInvalid("Negative usage", () -> new Equipment("EQ-09002", "Bad", Equipment.Category.CARDIO,
                "Cardio Zone A", Equipment.Condition.GOOD, -5));
        tryInvalid("Negative heap weight", () -> manager.setPriorityWeights(-1, 1));
        tryInvalid("Unknown category text", () -> Equipment.Category.fromLabel("Swimming"));
        System.out.println("Unknown zone BFS returns " + floor.bfs("Parking Lot").size() + " zones (no crash)");
    }

    // ---------- Helpers ----------

    // MySQL first; falls back to the CSV so the demo still runs without a database.
    private static List<Equipment> loadData() {
        try {
            List<Equipment> list = new EquipmentDAO().findAll();
            System.out.println("Loaded " + list.size() + " records from MySQL.");
            return list;
        } catch (Exception dbError) {
            try {
                List<Equipment> list = DatasetImporter.readCsv("data/equipment_dataset.csv");
                System.out.println("MySQL unavailable (" + dbError.getMessage() + "), loaded "
                        + list.size() + " records from CSV.");
                return list;
            } catch (Exception csvError) {
                System.out.println("Could not load data: " + csvError.getMessage());
                return null;
            }
        }
    }

    private static void section(String title) {
        System.out.println("\n===== " + title + " =====");
    }

    private static String join(CustomLinkedList<?> list) {
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (sb.length() > 0) sb.append(" -> ");
            sb.append(item);
        }
        return sb.length() == 0 ? "(empty)" : sb.toString();
    }

    // Runs something that SHOULD be rejected, and prints the error message instead of crashing.
    private static void tryInvalid(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + ": NOT rejected (unexpected)");
        } catch (IllegalArgumentException e) {
            System.out.println(label + ": rejected -> " + e.getMessage());
        }
    }
}
