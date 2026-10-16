package structures;

// Undirected graph of the gym floor: zones are nodes, walkways are edges.
// Stored as an adjacency list (zone -> neighbor list). With 16 zones and 20 walkways the
// graph is sparse, so this uses O(V + E) memory instead of a mostly empty 16 x 16 matrix.
public class CustomGraph {

    private final CustomHashTable<String, CustomLinkedList<String>> adjacency = new CustomHashTable<>();
    private final CustomLinkedList<String> zones = new CustomLinkedList<>();  // in the order added
    private int edgeCount;

    // Returns false if the zone already exists.
    public boolean addZone(String zone) {
        if (zone == null || zone.trim().isEmpty()) throw new IllegalArgumentException("Zone name cannot be empty.");
        if (adjacency.contains(zone)) return false;
        adjacency.put(zone, new CustomLinkedList<>());
        zones.add(zone);
        return true;
    }

    // Saved in both zones' lists since walkways go both ways. Missing zones are created.
    // Self-loops and duplicates are rejected so edgeCount stays right.
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

    // Empty list (not null) for an unknown zone, so loops over it are safe.
    public CustomLinkedList<String> getNeighbors(String zone) {
        CustomLinkedList<String> n = adjacency.get(zone);
        return n != null ? n : new CustomLinkedList<>();
    }

    // BFS, O(V + E): visits zones level by level using a queue.
    // A zone is marked visited when it's enqueued, so it can't be queued twice.
    public CustomLinkedList<String> bfs(String start) {
        CustomLinkedList<String> visitOrder = new CustomLinkedList<>();
        if (!hasZone(start)) return visitOrder;
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

    // Fewest-walkway route using BFS. Because BFS goes level by level, the first time it
    // reaches the target is along a shortest path. cameFrom remembers where each zone was
    // reached from, and we follow it back from the target. Empty list if unreachable.
    // (Edges have no distances, so hops are enough; real distances would need Dijkstra.)
    public CustomLinkedList<String> shortestPath(String start, String target) {
        CustomLinkedList<String> path = new CustomLinkedList<>();
        if (!hasZone(start) || !hasZone(target)) return path;
        CustomHashTable<String, String> cameFrom = new CustomHashTable<>();   // doubles as "visited"
        CustomQueue<String> queue = new CustomQueue<>();
        queue.enqueue(start);
        cameFrom.put(start, start);
        while (!queue.isEmpty()) {
            String zone = queue.dequeue();
            if (zone.equals(target)) break;
            for (String neighbor : getNeighbors(zone)) {
                if (!cameFrom.contains(neighbor)) {
                    cameFrom.put(neighbor, zone);
                    queue.enqueue(neighbor);
                }
            }
        }
        if (!cameFrom.contains(target)) return path;
        for (String at = target; ; at = cameFrom.get(at)) {
            path.addFirst(at);
            if (at.equals(start)) break;
        }
        return path;
    }

    // Zones within maxHops walkways of the start, each with its distance.
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
            if (d == maxHops) continue;
            for (String neighbor : getNeighbors(zone)) {
                if (!distance.contains(neighbor)) {
                    distance.put(neighbor, d + 1);
                    queue.enqueue(neighbor);
                }
            }
        }
        return result;
    }

    // DFS, O(V + E): follows one path as deep as it can before backtracking.
    // Recursive, so Java's call stack does the job of a stack.
    public CustomLinkedList<String> dfs(String start) {
        CustomLinkedList<String> visitOrder = new CustomLinkedList<>();
        if (!hasZone(start)) return visitOrder;
        dfsHelper(start, new CustomHashTable<>(), visitOrder);
        return visitOrder;
    }

    private void dfsHelper(String zone, CustomHashTable<String, Boolean> visited, CustomLinkedList<String> visitOrder) {
        visited.put(zone, true);
        visitOrder.add(zone);
        for (String neighbor : getNeighbors(zone)) {
            if (!visited.contains(neighbor)) dfsHelper(neighbor, visited, visitOrder);
        }
    }

    public boolean isReachable(String from, String to) {
        if (!hasZone(from) || !hasZone(to)) return false;
        CustomHashTable<String, Boolean> visited = new CustomHashTable<>();
        dfsHelper(from, visited, new CustomLinkedList<>());
        return visited.contains(to);
    }

    // First route DFS finds. Usually longer than shortestPath(), which is the point of
    // showing both. Empty list if unreachable.
    public CustomLinkedList<String> dfsPath(String start, String target) {
        CustomLinkedList<String> path = new CustomLinkedList<>();
        if (!hasZone(start) || !hasZone(target)) return path;
        dfsPathHelper(start, target, new CustomHashTable<>(), path);
        return path;
    }

    // If no neighbor leads to the target, this zone is a dead end: take it back off the route.
    private boolean dfsPathHelper(String zone, String target, CustomHashTable<String, Boolean> visited,
                                  CustomLinkedList<String> path) {
        visited.put(zone, true);
        path.add(zone);
        if (zone.equals(target)) return true;
        for (String neighbor : getNeighbors(zone)) {
            if (!visited.contains(neighbor) && dfsPathHelper(neighbor, target, visited, path)) return true;
        }
        path.removeLast();
        return false;
    }

    // Number of separate parts of the floor. Normally 1; closing the only walkway to the
    // Rooftop Deck makes it 2.
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
