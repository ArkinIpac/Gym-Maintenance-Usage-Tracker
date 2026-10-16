package model;

// One gym machine, e.g. EQ-00001 "Medicine Ball 1", Functional, Turf Area, Excellent, used 119 times.
// Every data structure in the project stores these objects.
public class Equipment {

    // fromLabel() turns the CSV text ("Strength Machine") into the enum name (STRENGTH_MACHINE).
    public enum Category {
        CARDIO, STRENGTH_MACHINE, FREE_WEIGHT, FUNCTIONAL, ACCESSORY;

        public static Category fromLabel(String label) {
            return Category.valueOf(label.trim().toUpperCase().replace(" ", "_"));
        }
    }

    // Ordered best to worst on purpose: the heap uses ordinal() as the severity (0 to 4),
    // so a worse condition always gets a higher priority.
    // The dataset numbers them the other way (1 = Critical, 5 = Excellent).
    public enum Condition {
        EXCELLENT, GOOD, FAIR, POOR, CRITICAL;

        public static Condition fromDatasetValue(int value) {
            switch (value) {
                case 5: return EXCELLENT;
                case 4: return GOOD;
                case 3: return FAIR;
                case 2: return POOR;
                case 1: return CRITICAL;
                default: throw new IllegalArgumentException("Invalid condition value: " + value);
            }
        }
    }

    // Final because the ID is the hash table key and the MySQL primary key.
    private final String equipmentId;

    private String name;
    private Category category;
    private String location;            // a zone name from the floor-plan graph
    private Condition condition;
    private int usageCount;             // BST key, and the tie-breaker in the heap

    // Validated here so a bad record can't get in from the form, the CSV or MySQL.
    public Equipment(String equipmentId, String name, Category category, String location,
                      Condition condition, int usageCount) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Equipment ID cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Equipment name cannot be empty.");
        }
        if (usageCount < 0) {
            throw new IllegalArgumentException("Usage count cannot be negative.");
        }
        this.equipmentId = equipmentId.trim();
        this.name = name.trim();
        this.category = category;
        this.location = location;
        this.condition = condition;
        this.usageCount = usageCount;
    }

    public String getEquipmentId() { return equipmentId; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public String getLocation() { return location; }
    public Condition getCondition() { return condition; }
    public int getUsageCount() { return usageCount; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Equipment name cannot be empty.");
        }
        this.name = name.trim();
    }
    public void setCategory(Category category) { this.category = category; }
    public void setLocation(String location) { this.location = location; }

    // Changing usage here does not update the BST. Use EquipmentStore.updateUsage() instead.
    public void setCondition(Condition condition) { this.condition = condition; }
    public void incrementUsage() { this.usageCount++; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    @Override
    public String toString() {
        return equipmentId + " - " + name + " [" + category + ", " + condition + ", used " + usageCount + "x]";
    }
}
