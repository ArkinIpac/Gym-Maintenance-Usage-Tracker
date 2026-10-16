package service;

import structures.CustomGraph;

// The gym floor plan: the 15 zones from the dataset plus the Entrance, joined by 20 walkways.
// The last 5 walkways add loops, so most zones have more than one route to them.
public class GymFloorPlan {

    private GymFloorPlan() { }

    public static CustomGraph build() {
        CustomGraph zones = new CustomGraph();
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
        // loops
        zones.addEdge("Cardio Zone B", "Machine Row 1");
        zones.addEdge("Machine Row 2", "Free Weight Area");
        zones.addEdge("Turf Area", "Group Class Studio");
        zones.addEdge("Entrance", "Stretching Area");
        zones.addEdge("Storage Room", "Machine Row 2");
        return zones;
    }
}
