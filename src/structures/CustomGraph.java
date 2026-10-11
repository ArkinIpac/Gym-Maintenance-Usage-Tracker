
// CustomGraph.java
// Part of: Kahlil Mendoza  (graph representation: adjacency list,
//                          addZone, addEdge, removeEdge)
//          Arkin Ipac  (BFS / DFS methods, marked below)
// Presented in: Slides 9-10 (Arkin Ipac) - shortest route, reachability,
//               removing a walkway (components 1 -> 2).
// What it does: The gym floor plan as a graph: zones are nodes, walkways
//               are edges. BFS = fewest-hop route, DFS = reachability.
// Used by: GymFloorPlan, ReportsPanel, Main


package structures;

// Our own UNDIRECTED graph of the gym floor.
//   Node (vertex) = a floor zone, e.g. "Cardio Zone A", "Squat Rack Bay"
//   Edge          = a walkway; a technician can walk directly between the two zones
// Undirected because a walkway works both ways.
//
// Stored as an ADJACENCY LIST: CustomHashTable<zone name, CustomLinkedList<neighbor names>>.
// Our floor has 16 zones and only 20 walkways, so it is sparse. An adjacency list uses
// O(V + E) memory. A 16 x 16 adjacency matrix would have 256 cells and most would be empty.
// The hash table also lets us get a zone's neighbors by name in O(1) on average.
//
// BFS and DFS are methods of this class because they only make sense on a graph.
public class CustomGraph {

    private final CustomHashTable<String, CustomLinkedList<String>> adjacency = new CustomHashTable<>();
    private final CustomLinkedList<String> zones = new CustomLinkedList<>();  // all zones, in the order added
    private int edgeCount;

    // ---------- Building and editing the graph ----------

    // Adds a zone with no walkways yet. Returns false if the zone already exists.
    public boolean addZone(String zone) {
        if (zone == null || zone.trim().isEmpty()) throw new IllegalArgumentException("Zone name cannot be empty.");
        if (adjacency.contains(zone)) return false;
        adjacency.put(zone, new CustomLinkedList<>());
        zones.add(zone);
        return true;
    }

    // Undirected, so the edge is saved in BOTH zones' neighbor lists.
    // Rejects an edge from a zone to itself and duplicate edges, so edgeCount stays correct.
    // If a zone doesn't exist yet, it is created.
    public boolean addEdge(String zoneA, String zoneB) {
        if (zoneA.equals(zoneB)) return false;
        addZone(zoneA);
        addZone(zoneB);
        if (adjacency.get(zoneA).contains(zoneB)) return false;
        adjacency.get(zoneA).add(zoneB);
        adjacency.get(zoneB).add(zoneA);
        edgeCount++;
        return true;
    }

    // Closes a walkway (live change: "remove one edge and rerun BFS").
    // Removed from both lists, same reason as addEdge.
    public boolean removeEdge(String zoneA, String zoneB) {
        CustomLinkedList<String> a = adjacency.get(zoneA);
        CustomLinkedList<String> b = adjacency.get(zoneB);
        if (a == null || b == null || !a.remove(zoneB)) return false;
        b.remove(zoneA);
        edgeCount--;
        return true;
    }

    public boolean hasZone(String zone) { return adjacency.contains(zone); }
    public int getZoneCount() { return zones.size(); }
    public int getEdgeCount() { return edgeCount; }
    public CustomLinkedList<String> getZones() { return zones; }

    // Unknown zone gets an empty list instead of null, so loops over it don't crash.
    public CustomLinkedList<String> getNeighbors(String zone) {
        CustomLinkedList<String> n = adjacency.get(zone);
        return n != null ? n : new CustomLinkedList<>();
    }

    // ===== [Arkin Ipac] BFS / DFS part starts here =====

    // ---------- BFS (Breadth-First Search): O(V + E) time, O(V) space ----------
    // Visits zones level by level: start, then all zones 1 walkway away, then 2 away, etc.
    //   - CustomQueue holds the zones waiting to be visited (FIFO = level by level)
    //   - CustomHashTable 'visited' makes sure each zone is added to the queue only once
    // We mark a zone as visited when we ENQUEUE it, not when we dequeue it, so the same
    // zone can't be put in the queue twice by two different neighbors.
    public CustomLinkedList<String> bfs(String start) {
        CustomLinkedList<String> visitOrder = new CustomLinkedList<>();
        if (!hasZone(start)) return visitOrder;                 // unknown zone: empty result, no crash
        CustomHashTable<String, Boolean> visited = new CustomHashTable<>();
        CustomQueue<String> queue = new CustomQueue<>();
        queue.enqueue(start);
        visited.put(start, true);
        while (!queue.isEmpty()) {
            String zone = queue.dequeue();
            visitOrder.add(zone);
            for (String neighbor : getNeighbors(zone)) {
                if (!visited.contains(neighbor)) {
                    visited.put(neighbor, true);
                    queue.enqueue(neighbor);
                }
            }
        }
        return visitOrder;
    }

    // Shortest route by number of walkways (fewest hops), using BFS.
    // Since BFS goes level by level, the first time it reaches the target is through the
    // shortest path. 'cameFrom' stores, for each zone, the zone we reached it from.
    // At the end we follow cameFrom backwards from the target to the start, adding each
    // zone to the FRONT of the list so the route reads start -> target.
    // Returns an empty list if the target can't be reached.
    // (This counts walkways, not meters. Our edges have no distances, so BFS is enough;
    // with distances we would need Dijkstra's algorithm.)
    public CustomLinkedList<String> shortestPath(String start, String target) {
        CustomLinkedList<String> path = new CustomLinkedList<>();
        if (!hasZone(start) || !hasZone(target)) return path;
        CustomHashTable<String, String> cameFrom = new CustomHashTable<>();   // also works as 'visited'
        CustomQueue<String> queue = new CustomQueue<>();
        queue.enqueue(start);
        cameFrom.put(start, start);                 // start points to itself so we know where to stop
        while (!queue.isEmpty()) {
            String zone = queue.dequeue();
            if (zone.equals(target)) break;         // found it, no need to explore further
            for (String neighbor : getNeighbors(zone)) {
                if (!cameFrom.contains(neighbor)) {
                    cameFrom.put(neighbor, zone);
                    queue.enqueue(neighbor);
                }
            }
        }
        if (!cameFrom.contains(target)) return path;            // never reached: no route
        for (String at = target; ; at = cameFrom.get(at)) {     // walk backwards to the start
            path.addFirst(at);
            if (at.equals(start)) break;
        }
        return path;
    }

    // All zones within N walkways of the start, e.g. what a technician can reach in 2 moves.
    // Same BFS, but we store each zone's distance and stop expanding at maxHops.
    public CustomLinkedList<String> zonesWithinHops(String start, int maxHops) {
        CustomLinkedList<String> result = new CustomLinkedList<>();
        if (!hasZone(start) || maxHops < 0) return result;
        CustomHashTable<String, Integer> distance = new CustomHashTable<>();
        CustomQueue<String> queue = new CustomQueue<>();
        queue.enqueue(start);
        distance.put(start, 0);
        while (!queue.isEmpty()) {
            String zone = queue.dequeue();
            int d = distance.get(zone);
            result.add(zone + " (" + d + ")");
            if (d == maxHops) continue;             // at the limit: don't add its neighbors
            for (String neighbor : getNeighbors(zone)) {
                if (!distance.contains(neighbor)) {
                    distance.put(neighbor, d + 1);
                    queue.enqueue(neighbor);
                }
            }
        }
        return result;
    }

    // ---------- DFS (Depth-First Search): O(V + E) time, O(V) space ----------
    // Goes as far as possible down one path, and only backtracks at a dead end.
    // We use RECURSION instead of a stack class: every call waits on Java's call stack,
    // and when a call returns, that is the backtrack.
    public CustomLinkedList<String> dfs(String start) {
        CustomLinkedList<String> visitOrder = new CustomLinkedList<>();
        if (!hasZone(start)) return visitOrder;
        dfsHelper(start, new CustomHashTable<>(), visitOrder);
        return visitOrder;
    }

    // The recursive part. Mark this zone, then visit each unvisited neighbor fully
    // before moving on to the next neighbor. The base case is implicit: when every
    // neighbor is already visited, the loop does nothing and the method returns.
    private void dfsHelper(String zone, CustomHashTable<String, Boolean> visited, CustomLinkedList<String> visitOrder) {
        visited.put(zone, true);
        visitOrder.add(zone);
        for (String neighbor : getNeighbors(zone)) {
            if (!visited.contains(neighbor)) dfsHelper(neighbor, visited, visitOrder);
        }
    }

    // Can a technician get from one zone to another at all? Run DFS from 'from' and
    // check if 'to' got visited.
    public boolean isReachable(String from, String to) {
        if (!hasZone(from) || !hasZone(to)) return false;
        CustomHashTable<String, Boolean> visited = new CustomHashTable<>();
        dfsHelper(from, visited, new CustomLinkedList<>());
        return visited.contains(to);
    }

    // A route from start to target found with DFS (Arkin Ipac).
    // DFS follows one walkway as deep as it can and only backs up at a dead end, so the
    // FIRST route it finds is usually NOT the shortest. That is the difference from
    // shortestPath(), which uses BFS and always gives the fewest hops.
    // Returns an empty list if the target can't be reached.
    public CustomLinkedList<String> dfsPath(String start, String target) {
        CustomLinkedList<String> path = new CustomLinkedList<>();
        if (!hasZone(start) || !hasZone(target)) return path;
        dfsPathHelper(start, target, new CustomHashTable<>(), path);
        return path;
    }

    // Recursive part. Add this zone to the current route; if it is the target we are done.
    // Otherwise try each unvisited neighbor. If none of them leads to the target, this zone
    // is a dead end: remove it from the route again (backtrack) and return false.
    private boolean dfsPathHelper(String zone, String target, CustomHashTable<String, Boolean> visited,
                                  CustomLinkedList<String> path) {
        visited.put(zone, true);
        path.add(zone);
        if (zone.equals(target)) return true;                 // base case: reached the target
        for (String neighbor : getNeighbors(zone)) {
            if (!visited.contains(neighbor) && dfsPathHelper(neighbor, target, visited, path)) return true;
        }
        path.removeLast();                                    // dead end: backtrack
        return false;
    }

    // How many separate "islands" the floor is split into.
    // For every zone not yet visited, start a new DFS; each new start = one more component.
    // Normally 1. If we close Cardio Mezzanine - Rooftop Deck it becomes 2, because that
    // was the only walkway to the Rooftop Deck.
    public int countComponents() {
        CustomHashTable<String, Boolean> visited = new CustomHashTable<>();
        int components = 0;
        for (String zone : zones) {
            if (!visited.contains(zone)) {
                components++;
                dfsHelper(zone, visited, new CustomLinkedList<>());
            }
        }
        return components;
    }
}
