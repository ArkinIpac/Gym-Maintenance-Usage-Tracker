// ApiRoutes.java
// Part of: Arkin Ipac  (Integration / web version)
// What it does: Every /api/... address the web page can call. Each method does the same
//               thing as a button in the Swing app (same store / manager calls, same
//               MySQL handling), but answers with JSON instead of updating a JTable.
// Used by: WebServer
//
// GET  = only reads data.   POST = changes something.
//
//  App      GET  /api/status                      header chips + db status
//           GET  /api/meta                        categories, conditions, zones (for dropdowns)
//  Equipment GET /api/equipment?q=&category=&offset=&limit=
//           POST /api/equipment/add | /update | /delete
//  Maint.   GET  /api/maintenance                 queue/heap stats, top 25 of heap, recent list
//           POST /api/maintenance/flag | /flag-critical | /promote | /service | /clear-heap | /clear-recent
//           GET  /api/maintenance/history?id=
//  Reports  GET  /api/sort?algo=&order=&limit=
//           GET  /api/search?id=&mode=hash|linear|binary|all     /api/search/usage?usage=
//           GET  /api/bst/tree   /api/bst/range?min=&max=   /api/bst/traverse?order=
//           GET  /api/hash/locate?id=   /api/hash/demo
//           GET  /api/graph   /api/graph/route?from=&to=&algo=bfs|dfs
//  Bench.   POST /api/benchmark      GET /api/complexity

package web;

import benchmark.Benchmark;
import db.EquipmentDAO;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;
import model.Equipment;
import model.MaintenanceRecord;
import service.EquipmentStore;
import service.GymFloorPlan;
import service.MaintenanceManager;
import structures.CustomBST;
import structures.CustomGraph;
import structures.CustomHashTable;
import structures.CustomLinkedList;

public class ApiRoutes {

    // What every handler returns: an HTTP status and a JSON body.
    public static class Response {
        final int status;
        final String body;
        Response(int status, String body) { this.status = status; this.body = body; }
    }

    private static final int HEAP_ROWS = 25;

    private final EquipmentStore store;
    private final MaintenanceManager manager;
    private final Supplier<EquipmentDAO> dao;      // get() returns null while offline
    private final String loadMessage;
    private final CustomGraph floor = GymFloorPlan.build();
    private int servicedThisSession = 0;

    private static final int ACTIVITY_LIMIT = 20;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final CustomLinkedList<String[]> activity = new CustomLinkedList<>();

    public ApiRoutes(EquipmentStore store, MaintenanceManager manager, Supplier<EquipmentDAO> dao, String loadMessage) {
        this.store = store;
        this.manager = manager;
        this.dao = dao;
        this.loadMessage = loadMessage;
        logActivity(loadMessage);
    }

    public Response handle(String method, String path, Map<String, String> p) {
        boolean post = "POST".equalsIgnoreCase(method);
        try {
            switch (path) {
                case "/api/status":                   return status();
                case "/api/overview":                 return overview();
                case "/api/meta":                     return meta();
                case "/api/equipment":                return listEquipment(p);
                case "/api/maintenance":              return maintenanceState();
                case "/api/maintenance/history":      return history(p);
                case "/api/sort":                     return sort(p);
                case "/api/search":                   return search(p);
                case "/api/search/usage":             return searchUsage(p);
                case "/api/bst/tree":                 return bstTree();
                case "/api/bst/range":                return bstRange(p);
                case "/api/bst/traverse":             return bstTraverse(p);
                case "/api/hash/locate":              return hashLocate(p);
                case "/api/hash/demo":                return hashDemo();
                case "/api/graph":                    return graph();
                case "/api/graph/route":              return route(p);
                case "/api/complexity":               return complexity();
                default: break;
            }
            if (!post) return error(405, "Use POST for " + path);
            switch (path) {
                case "/api/equipment/add":            return addEquipment(p);
                case "/api/equipment/update":         return updateEquipment(p);
                case "/api/equipment/delete":         return deleteEquipment(p);
                case "/api/maintenance/flag":         return flag(p);
                case "/api/maintenance/flag-critical":return flagAllCritical();
                case "/api/maintenance/promote":      return promote();
                case "/api/maintenance/service":      return serviceNext();
                case "/api/maintenance/clear-heap":   return clearHeap();
                case "/api/maintenance/clear-recent": return clearRecent();
                case "/api/benchmark":                return benchmark();
                default:                              return error(404, "Unknown address " + path);
            }
        } catch (IllegalArgumentException ex) {        // bad input, e.g. a negative usage count
            return error(400, ex.getMessage());
        } catch (NullPointerException ex) {            // a form field was missing
            return error(400, "Please fill in every field.");
        }
    }

    // ================= App =================

    private Response status() {
        return ok(new Json().obj()
                .put("ok", true)
                .put("count", store.getCount())
                .put("queue", manager.getPendingCount())
                .put("heap", manager.getHeapSize())
                .put("serviced", servicedThisSession)
                .put("online", dao.get() != null)
                .put("loadMessage", loadMessage)
                .end());
    }

    private Response meta() {
        Json j = new Json().obj().put("ok", true);
        j.key("categories").arr();
        for (Equipment.Category c : Equipment.Category.values()) j.val(c.name());
        j.end().key("conditions").arr();
        for (Equipment.Condition c : Equipment.Condition.values()) j.val(c.name());
        j.end().key("zones").arr();
        for (String z : floor.getZones()) j.val(z);
        return ok(j.end().end());
    }

    // ================= Overview (dashboard) =================

    // Everything the dashboard shows, in one request. One pass over the array (O(n))
    // counts conditions, categories and critical machines per zone at the same time.
    private Response overview() {
        Equipment.Condition[] conds = Equipment.Condition.values();
        Equipment.Category[] cats = Equipment.Category.values();
        int[] condCount = new int[conds.length];
        int[] catCount = new int[cats.length], catCritical = new int[cats.length];
        long[] catUsage = new long[cats.length];
        CustomHashTable<String, Integer> zoneCritical = new CustomHashTable<>();

        for (Equipment e : store.getAll()) {
            condCount[e.getCondition().ordinal()]++;
            int c = e.getCategory().ordinal();
            catCount[c]++;
            catUsage[c] += e.getUsageCount();
            if (e.getCondition() == Equipment.Condition.CRITICAL) {
                catCritical[c]++;
                Integer z = zoneCritical.get(e.getLocation());
                zoneCritical.put(e.getLocation(), z == null ? 1 : z + 1);
            }
        }

        Json j = new Json().obj().put("ok", true)
                .put("count", store.getCount())
                .put("queue", manager.getPendingCount())
                .put("heap", manager.getHeapSize())
                .put("serviced", servicedThisSession)
                .put("online", dao.get() != null);

        j.key("conditions").arr();
        for (Equipment.Condition c : conds) j.obj().put("name", c.name()).put("count", condCount[c.ordinal()]).end();
        j.end().key("categories").arr();
        for (Equipment.Category c : cats) {
            int i = c.ordinal();
            j.obj().put("name", c.name()).put("count", catCount[i]).put("critical", catCritical[i])
             .put("avgUsage", catCount[i] == 0 ? 0 : catUsage[i] / catCount[i]).end();
        }
        j.end().key("zones").arr();                       // every floor zone with its critical count
        for (String z : floor.getZones()) {
            if (z.equals("Entrance")) continue;
            Integer n = zoneCritical.get(z);
            j.obj().put("name", z).put("critical", n == null ? 0 : n).end();
        }
        j.end();

        Equipment next = manager.peekNext();
        j.key("next");
        if (next == null) j.val((String) null);
        else j.obj().put("id", next.getEquipmentId()).put("name", next.getName())
               .put("location", next.getLocation()).put("condition", next.getCondition().name())
               .put("usage", next.getUsageCount()).put("severity", next.getCondition().ordinal())
               .put("severityWeight", manager.getSeverityWeight()).put("priority", manager.priorityOf(next)).end();

        j.key("top").arr();
        for (Equipment e : topByPriority(manager.getHeapSnapshot(), 5)) {
            j.obj().put("id", e.getEquipmentId()).put("name", e.getName()).put("location", e.getLocation())
             .put("condition", e.getCondition().name()).put("priority", manager.priorityOf(e)).end();
        }
        j.end();
        activityJson(j, 6);
        return ok(j.end());
    }

    // ================= Equipment page =================

    // Partial-text search on ID, name or location + category filter: O(n), same as the
    // Swing table. Only one page of rows is sent back, so the browser stays fast.
    private Response listEquipment(Map<String, String> p) {
        String q = p.getOrDefault("q", "").trim().toLowerCase();
        String category = p.getOrDefault("category", "");
        int offset = Math.max(0, intParam(p, "offset", 0));
        int limit = Math.min(500, Math.max(1, intParam(p, "limit", 50)));

        Equipment[] all = store.getAll();
        Json rows = new Json().arr();
        int matched = 0;
        for (Equipment e : all) {
            if (!category.isEmpty() && !e.getCategory().name().equals(category)) continue;
            if (!q.isEmpty()
                    && !e.getEquipmentId().toLowerCase().contains(q)
                    && !e.getName().toLowerCase().contains(q)
                    && !(e.getLocation() != null && e.getLocation().toLowerCase().contains(q))) continue;
            if (matched >= offset && matched < offset + limit) rows.equipment(e);
            matched++;
        }
        rows.end();
        return ok("{\"ok\":true,\"total\":" + all.length + ",\"matched\":" + matched
                + ",\"offset\":" + offset + ",\"limit\":" + limit + ",\"rows\":" + rows + "}");
    }

    private Response addEquipment(Map<String, String> p) {
        Equipment e = new Equipment(p.get("id"), p.get("name"),
                Equipment.Category.valueOf(p.get("category")), p.get("location"),
                Equipment.Condition.valueOf(p.get("condition")), parseUsage(p.get("usage")));
        store.addEquipment(e);                          // throws on duplicate ID
        EquipmentDAO db = dao.get();
        if (db != null) {
            try {
                db.insert(e);
            } catch (Exception ex) {
                store.deleteEquipment(e.getEquipmentId());   // keep memory and database the same
                return error(500, "Database rejected " + e.getEquipmentId() + ": " + ex.getMessage());
            }
        }
        return message("Added " + e.getEquipmentId() + " - " + e.getName() + offlineNote(db));
    }

    private Response updateEquipment(Map<String, String> p) {
        Equipment e = store.findById(p.getOrDefault("id", ""));
        if (e == null) return error(404, "No equipment with ID \"" + p.get("id") + "\".");
        int usage = parseUsage(p.get("usage"));

        // Old values: to undo if MySQL refuses.
        String oldName = e.getName();
        Equipment.Category oldCategory = e.getCategory();
        String oldLocation = e.getLocation();
        Equipment.Condition oldCondition = e.getCondition();
        int oldUsage = e.getUsageCount();

        store.updateDetails(e, p.get("name"), Equipment.Category.valueOf(p.get("category")),
                p.get("location"), Equipment.Condition.valueOf(p.get("condition")), usage);
        boolean moved = manager.refreshPriority(e);     // re-place it if it's waiting in the heap

        EquipmentDAO db = dao.get();
        if (db != null) {
            try {
                db.update(e);
            } catch (Exception ex) {
                store.updateDetails(e, oldName, oldCategory, oldLocation, oldCondition, oldUsage);
                manager.refreshPriority(e);
                return error(500, "Database rejected the changes to " + e.getEquipmentId() + ": " + ex.getMessage());
            }
        }
        String extra = (oldUsage != usage ? " (BST re-indexed)" : "") + (moved ? " (heap re-ordered)" : "");
        return message("Updated " + e.getEquipmentId() + extra + offlineNote(db));
    }

    private Response deleteEquipment(Map<String, String> p) {
        Equipment e = store.findById(p.getOrDefault("id", ""));
        if (e == null) return error(404, "No equipment with ID \"" + p.get("id") + "\".");
        EquipmentDAO db = dao.get();
        if (db != null) {
            try {
                db.delete(e.getEquipmentId());
            } catch (Exception ex) {
                return error(500, "Database refused to delete " + e.getEquipmentId() + ": " + ex.getMessage());
            }
        }
        manager.cancel(e);                               // if it was waiting for maintenance
        store.deleteEquipment(e.getEquipmentId());       // array + hash table + BST
        return message("Deleted " + e.getEquipmentId() + " - " + e.getName() + offlineNote(db));
    }

    // ================= Maintenance page =================

    private Response maintenanceState() {
        Equipment[] heap = manager.getHeapSnapshot();
        Equipment next = manager.peekNext();
        Json j = new Json().obj()
                .put("ok", true)
                .put("queue", manager.getPendingCount())
                .put("heap", manager.getHeapSize())
                .put("serviced", servicedThisSession)
                .put("severityWeight", manager.getSeverityWeight())
                .put("usageWeight", manager.getUsageWeight());
        j.key("next");
        if (next == null) j.val((String) null); else j.equipment(next);

        // Condition breakdown of everything in the heap, worst first.
        int[] counts = new int[Equipment.Condition.values().length];
        for (Equipment e : heap) counts[e.getCondition().ordinal()]++;
        j.key("breakdown").obj();
        for (Equipment.Condition c : Equipment.Condition.values()) j.put(c.name(), counts[c.ordinal()]);
        j.end();

        // Top 25 by priority (same selection pass as MaintenancePanel; the heap is not changed).
        j.key("top").arr();
        for (Equipment e : topByPriority(heap, HEAP_ROWS)) {
            j.obj().put("id", e.getEquipmentId()).put("name", e.getName())
             .put("condition", e.getCondition().name()).put("usage", e.getUsageCount())
             .put("location", e.getLocation()).put("priority", manager.priorityOf(e)).end();
        }
        j.end().key("queueItems").arr();
        for (Equipment e : manager.getQueueSnapshot()) j.equipment(e);
        j.end().key("recent").arr();
        for (MaintenanceRecord r : manager.getRecentlyServiced()) j.record(r);
        j.end();
        activityJson(j, 8);
        return ok(j.end());
    }

    private Equipment[] topByPriority(Equipment[] heap, int k) {
        int n = Math.min(k, heap.length);
        Equipment[] copy = heap.clone();
        for (int i = 0; i < n; i++) {
            int best = i;
            for (int j = i + 1; j < copy.length; j++) {
                if (manager.priorityOf(copy[j]) > manager.priorityOf(copy[best])) best = j;
            }
            Equipment t = copy[i]; copy[i] = copy[best]; copy[best] = t;
        }
        Equipment[] top = new Equipment[n];
        System.arraycopy(copy, 0, top, 0, n);
        return top;
    }

    private Response flag(Map<String, String> p) {
        String id = p.getOrDefault("id", "").trim();
        Equipment e = store.findById(id);               // hash table lookup, O(1)
        if (e == null) return error(404, "No equipment with ID \"" + id + "\".");
        if (!manager.flagForMaintenance(e)) return error(409, e.getEquipmentId() + " is already waiting for maintenance.");
        return message("Flagged " + e.getEquipmentId() + " (" + pretty(e.getCondition()) + ") → queue position "
                + manager.getPendingCount());
    }

    private Response flagAllCritical() {
        int added = 0;
        for (Equipment e : store.getAll()) {
            if (e.getCondition() == Equipment.Condition.CRITICAL && manager.flagForMaintenance(e)) added++;
        }
        return message("Flagged " + added + " critical items. Queue now holds " + manager.getPendingCount() + ".");
    }

    private Response promote() {
        int moved = manager.promoteQueueToHeap();
        return message(moved == 0 ? "Queue is empty, nothing to promote." : "Promoted " + moved + " items into the priority heap.");
    }

    private Response serviceNext() {
        if (!manager.hasEquipmentReadyForService()) {
            return error(409, manager.getPendingCount() > 0
                    ? "The heap is empty. Promote the queue first."
                    : "Nothing to service. Flag some equipment first.");
        }
        MaintenanceRecord record = manager.serviceNext("Routine service");
        servicedThisSession++;
        Equipment e = store.findById(record.getEquipmentId());
        EquipmentDAO db = dao.get();
        String saved = "";
        if (db != null && e != null) {
            try { db.update(e); } catch (Exception ex) { saved = " (database update failed: " + ex.getMessage() + ")"; }
        } else if (db == null) {
            saved = " (offline, not saved)";
        }
        return message("Serviced " + record.getEquipmentId() + ": " + pretty(record.getConditionBefore())
                + " → " + pretty(record.getConditionAfter()) + saved);
    }

    private Response clearHeap() {
        return message("Cleared " + manager.clearHeap() + " items from the priority heap (not serviced).");
    }

    private Response clearRecent() {
        manager.clearRecentlyServiced();
        return message("Recently serviced list cleared. Machine histories are kept.");
    }

    private Response history(Map<String, String> p) {
        String id = p.getOrDefault("id", "").trim();
        if (store.findById(id) == null) return error(404, "No equipment with ID \"" + id + "\".");
        Json j = new Json().obj().put("ok", true).put("id", id).key("records").arr();
        for (MaintenanceRecord r : manager.getHistory(id)) j.record(r);
        return ok(j.end().end());
    }

    // ================= Reports page =================

    private Response sort(Map<String, String> p) {
        String algo = p.getOrDefault("algo", "bubble");
        boolean asc = "asc".equals(p.get("order"));
        int limit = Math.min(500, Math.max(0, intParam(p, "limit", 15)));
        long start = System.nanoTime();
        Equipment[] sorted = algo.equals("insertion") ? store.sortByUsageCountInsertion(asc)
                           : algo.equals("insertion-id") ? store.sortByIdInsertion(asc)
                           : store.sortByUsageCountBubble(asc);
        double ms = (System.nanoTime() - start) / 1_000_000.0;
        Json j = new Json().obj().put("ok", true).put("ms", ms).put("count", sorted.length).key("rows").arr();
        for (int i = 0; i < Math.min(limit, sorted.length); i++) j.equipment(sorted[i]);
        return ok(j.end().end());
    }

    // Times one strategy (or all three) on the same ID, like the Searching tool.
    private Response search(Map<String, String> p) {
        String id = p.getOrDefault("id", "").trim();
        String mode = p.getOrDefault("mode", "all");
        Json j = new Json().obj().put("ok", true).put("id", id).key("results").arr();
        String[] modes = mode.equals("all") ? new String[]{"hash", "linear", "binary"} : new String[]{mode};
        for (String m : modes) {
            Equipment[] sorted = m.equals("binary") ? store.sortByIdInsertion(true) : null;   // not timed
            long start = System.nanoTime();
            Equipment found = m.equals("hash") ? store.findById(id)
                            : m.equals("linear") ? store.linearSearchById(id)
                            : store.binarySearchById(sorted, id);
            long ns = System.nanoTime() - start;
            j.obj().put("mode", m).put("ns", ns).key("found");
            if (found == null) j.val((String) null); else j.equipment(found);
            j.end();
        }
        return ok(j.end().end());
    }

    private Response searchUsage(Map<String, String> p) {
        int u = intParam(p, "usage", -1);
        if (u < 0) return error(400, "Usage must be a whole number, 0 or more.");
        long start = System.nanoTime();
        CustomLinkedList<Equipment> found = store.findByUsage(u);
        long ns = System.nanoTime() - start;
        Json j = new Json().obj().put("ok", true).put("usage", u).put("ns", ns).put("count", found.size());
        j.key("path").arr();
        for (int k : store.getUsageIndex().searchPath(u)) j.val(k);
        j.end().key("rows").arr();
        int shown = 0;
        for (Equipment e : found) { if (shown++ == 25) break; j.equipment(e); }
        return ok(j.end().end());
    }

    private Response bstTree() {
        CustomBST bst = store.getUsageIndex();
        Json j = new Json().obj().put("ok", true)
                .put("nodes", bst.getNodeCount())
                .put("height", bst.height())
                .put("root", bst.isEmpty() ? -1 : bst.getRootKey());
        j.key("levels").arr();
        for (int k : bst.topLevels(5)) j.val(k);       // heap-style layout, -1 = empty spot
        return ok(j.end().end());
    }

    private Response bstRange(Map<String, String> p) {
        int lo = intParam(p, "min", -1), hi = intParam(p, "max", -1);
        if (lo < 0 || hi < 0) return error(400, "Min and max must be whole numbers, 0 or more.");
        if (lo > hi) return error(400, "Min must not be greater than max.");
        CustomLinkedList<Equipment> found = store.getByUsageRange(lo, hi);
        Json j = new Json().obj().put("ok", true).put("count", found.size()).key("rows").arr();
        int shown = 0;
        for (Equipment e : found) { if (shown++ == 50) break; j.equipment(e); }
        return ok(j.end().end());
    }

    private Response bstTraverse(Map<String, String> p) {
        CustomBST bst = store.getUsageIndex();
        String order = p.getOrDefault("order", "inorder");
        CustomLinkedList<Integer> keys = order.equals("preorder") ? bst.preorder()
                                       : order.equals("postorder") ? bst.postorder() : bst.inorder();
        Json j = new Json().obj().put("ok", true).put("total", keys.size()).key("keys").arr();
        int i = 0;
        for (int k : keys) { if (i++ == 60) break; j.val(k); }
        return ok(j.end().end());
    }

    private Response hashLocate(Map<String, String> p) {
        String key = p.getOrDefault("id", "").trim();
        if (key.isEmpty()) return error(400, "Type an ID first.");
        CustomHashTable<String, Equipment> h = store.getIdIndex();
        int b = h.bucketIndexOf(key);
        Json j = new Json().obj().put("ok", true).put("id", key).put("bucket", b)
                .put("present", h.contains(key));
        hashStats(j, h);
        // The bucket and 3 neighbors on each side, for the drawing.
        j.key("buckets").arr();
        for (int i = Math.max(0, b - 3); i <= Math.min(h.getCapacity() - 1, b + 3); i++) bucket(j, h, i);
        return ok(j.end().end());
    }

    // Capacity-7 demo: finds an ID that lands in the same bucket as EQ-00001.
    private Response hashDemo() {
        CustomHashTable<String, String> tiny = new CustomHashTable<>(7);
        tiny.put("EQ-00001", "Medicine Ball 1");
        int target = tiny.bucketIndexOf("EQ-00001");
        String collider = null;
        for (int i = 2; collider == null; i++) {
            String candidate = String.format("EQ-%05d", i);
            if (tiny.bucketIndexOf(candidate) == target) collider = candidate;
        }
        tiny.put(collider, "collides with EQ-00001");
        tiny.put("EQ-00002", "no collision");
        Json j = new Json().obj().put("ok", true).put("target", target).put("collider", collider);
        j.key("buckets").arr();
        for (int i = 0; i < tiny.getCapacity(); i++) bucket(j, tiny, i);
        j.end();
        j.put("getCollider", tiny.get(collider));
        j.put("removed", tiny.remove(collider));
        j.put("stillFound", tiny.get("EQ-00001") != null);
        return ok(j.end());
    }

    private static void bucket(Json j, CustomHashTable<String, ?> h, int index) {
        j.obj().put("index", index).key("keys").arr();
        for (String k : h.keysInBucket(index)) j.val(k);
        j.end().end();
    }

    private static void hashStats(Json j, CustomHashTable<String, ?> h) {
        j.put("size", h.size()).put("capacity", h.getCapacity()).put("loadFactor", h.getLoadFactor())
         .put("collisions", h.getCollisionCount()).put("longestChain", h.getLongestChain())
         .put("usedBuckets", h.getUsedBucketCount()).put("resizes", h.getResizeCount());
    }

    private Response graph() {
        Json j = new Json().obj().put("ok", true)
                .put("zoneCount", floor.getZoneCount()).put("edgeCount", floor.getEdgeCount())
                .put("components", floor.countComponents());
        j.key("zones").arr();
        for (String z : floor.getZones()) j.val(z);
        j.end().key("edges").arr();
        for (String a : floor.getZones()) {
            for (String b : floor.getNeighbors(a)) {
                if (a.compareTo(b) < 0) j.arr().val(a).val(b).end();   // each walkway once
            }
        }
        return ok(j.end().end());
    }

    private Response route(Map<String, String> p) {
        String from = p.getOrDefault("from", ""), to = p.getOrDefault("to", "");
        if (!floor.hasZone(from) || !floor.hasZone(to)) return error(400, "Pick two zones from the list.");
        boolean dfs = "dfs".equals(p.get("algo"));
        CustomLinkedList<String> path = dfs ? floor.dfsPath(from, to) : floor.shortestPath(from, to);
        Json j = new Json().obj().put("ok", true).put("algo", dfs ? "dfs" : "bfs")
                .put("hops", path.isEmpty() ? -1 : path.size() - 1)
                .put("bfsHops", floor.shortestPath(from, to).size() - 1);
        j.key("path").arr();
        for (String z : path) j.val(z);
        j.end().key("visitOrder").arr();                // full BFS / DFS visit order from the start zone
        for (String z : dfs ? floor.dfs(from) : floor.bfs(from)) j.val(z);
        return ok(j.end().end());
    }

    // ================= Benchmarks page =================

    private Response benchmark() {
        Benchmark.Row[] rows = Benchmark.measure(store.getAll());
        Json j = new Json().obj().put("ok", true).put("savedTo", Benchmark.getResultsFile()).key("rows").arr();
        for (Benchmark.Row r : rows) {
            j.obj().put("size", r.size).put("bubbleMs", r.bubbleMs).put("insertionMs", r.insertionMs)
             .put("insertionIdMs", r.insertionIdMs).put("linearNs", r.linearNs).put("binaryNs", r.binaryNs)
             .put("hashNs", r.hashNs).put("bstNs", r.bstNs).end();
        }
        return ok(j.end().end());
    }

    private Response complexity() {
        Json j = new Json().obj().put("ok", true).key("rows").arr();
        for (String[] row : Benchmark.COMPLEXITY) {
            j.arr();
            for (String cell : row) j.val(cell);
            j.end();
        }
        return ok(j.end().end());
    }

    // ================= Helpers =================

    private static Response ok(Object json) { return new Response(200, json.toString()); }

    // Every successful change is also written to the activity feed.
    private Response message(String msg) {
        logActivity(msg);
        return ok(new Json().obj().put("ok", true).put("message", msg).end());
    }

    // Activity feed: newest first, last 20 kept. Same trick as "recently serviced":
    // addFirst() for the new entry, removeLast() to drop the oldest. Uses our own linked list.
    private void logActivity(String msg) {
        activity.addFirst(new String[]{LocalTime.now().format(CLOCK), msg});
        if (activity.size() > ACTIVITY_LIMIT) activity.removeLast();
    }

    private void activityJson(Json j, int limit) {
        j.key("activity").arr();
        int n = 0;
        for (String[] a : activity) {
            if (n++ == limit) break;
            j.obj().put("time", a[0]).put("message", a[1]).end();
        }
        j.end();
    }

    private static Response error(int status, String msg) {
        return new Response(status, new Json().obj().put("ok", false).put("message", msg).end().toString());
    }

    private static String offlineNote(EquipmentDAO db) { return db == null ? " (offline, not saved)" : ""; }

    private static int intParam(Map<String, String> p, String key, int fallback) {
        try { return Integer.parseInt(p.getOrDefault(key, "").trim().replace(",", "")); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static int parseUsage(String s) {
        try { return Integer.parseInt(s == null ? "" : s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Usage count must be a whole number, e.g. 250."); }
    }

    // CRITICAL -> Critical
    private static String pretty(Enum<?> v) {
        String s = v.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
