
// GymFloorPlan.java
// Part of: Kahlil Mendoza  (Hash Table and Graph Representation)
// Presented in: Slide 3 (Kahlil Mendoza) - the 16-zone floor plan;
//               Slides 9-10 (Arkin Ipac) - BFS / DFS on this floor.
// What it does: Builds our 16-zone, 20-walkway floor plan in one place.
// Used by: MainFrame (ReportsPanel), EquipmentPanel (zone list), Main


package service;

import structures.CustomGraph;

// Builds the gym floor-plan graph. It is defined in this one place so the console demo
// (Main) and the Swing app always use exactly the same floor.
//
// 16 nodes: the 15 zones that appear in the dataset's Location column, plus "Entrance"
//           where the technician starts.
// 20 edges: the first 15 are the main walkways (they connect everything like a tree);
//           the last 5 add loops, so many zones can be reached by more than one route.
//           Without loops there would only be one path to each zone, and "shortest path"
//           would not mean much.
public class GymFloorPlan {

    private GymFloorPlan() { }   // only has a static method, so no objects are needed

    public static CustomGraph build() {
        CustomGraph zones = new CustomGraph();
        // Main walkways
        zones.addEdge("Entrance", "Cardio Zone A");
        zones.addEdge("Entrance", "Strength Floor");
        zones.addEdge("Cardio Zone A", "Cardio Zone B");
        zones.addEdge("Cardio Zone B", "Cardio Mezzanine");
        zones.addEdge("Cardio Mezzanine", "Rooftop Deck");     // only way to the Rooftop Deck
        zones.addEdge("Strength Floor", "Machine Row 1");
        zones.addEdge("Machine Row 1", "Machine Row 2");
        zones.addEdge("Strength Floor", "Free Weight Area");
        zones.addEdge("Free Weight Area", "Squat Rack Bay");
        zones.addEdge("Squat Rack Bay", "Olympic Lifting Platform");
        zones.addEdge("Free Weight Area", "Functional Training Zone");
        zones.addEdge("Functional Training Zone", "Turf Area");
        zones.addEdge("Functional Training Zone", "Stretching Area");
        zones.addEdge("Stretching Area", "Group Class Studio");
        zones.addEdge("Group Class Studio", "Storage Room");
        // Extra walkways that create loops / alternate routes
        zones.addEdge("Cardio Zone B", "Machine Row 1");
        zones.addEdge("Machine Row 2", "Free Weight Area");
        zones.addEdge("Turf Area", "Group Class Studio");
        zones.addEdge("Entrance", "Stretching Area");
        zones.addEdge("Storage Room", "Machine Row 2");
        return zones;
    }
}
