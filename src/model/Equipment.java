// Represents one gym equipment in the system.
public class Equipment {

    // Stores the unique ID of the equipment.
    private String equipmentId;

    // Stores the name of the equipment.
    private String name;

    // Stores the category of the equipment.
    private String category;

    // Stores where the equipment is located in the gym.
    private String location;

    // Stores the current condition of the equipment.
    private String condition;

    // Stores the usage level of the equipment.
    // This can later be used for BST organization and priority calculations.
    private int usageLevel;


    // Constructor used to create an Equipment object.
    public Equipment(String equipmentId, String name, String category,
                     String location, String condition, int usageLevel) {

        this.equipmentId = equipmentId;
        this.name = name;
        this.category = category;
        this.location = location;
        this.condition = condition;
        this.usageLevel = usageLevel;
    }


    // Returns the equipment ID.
    public String getEquipmentId() {
        return equipmentId;
    }

    // Returns the equipment name.
    public String getName() {
        return name;
    }

    // Returns the equipment category.
    public String getCategory() {
        return category;
    }

    // Returns the equipment location.
    public String getLocation() {
        return location;
    }

    // Returns the current equipment condition.
    public String getCondition() {
        return condition;
    }

    // Returns the equipment usage level.
    public int getUsageLevel() {
        return usageLevel;
    }


    // Updates the condition of the equipment.
    // Example: "Good" -> "Needs Maintenance"
    public void setCondition(String condition) {
        this.condition = condition;
    }

    // Updates the usage level of the equipment.
    public void setUsageLevel(int usageLevel) {
        this.usageLevel = usageLevel;
    }


    // Displays the basic information of the equipment.
    public void displayInfo() {
        System.out.println("Equipment ID: " + equipmentId);
        System.out.println("Name: " + name);
        System.out.println("Category: " + category);
        System.out.println("Location: " + location);
        System.out.println("Condition: " + condition);
        System.out.println("Usage Level: " + usageLevel);
    }
}