// 
// Equipment.java
// Part of: Arkin Ipac  (model class used by every structure)
// Presented in: Slide 3 (Kahlil Mendoza) - fields and condition scale 1-5;
//               Slide 4 (Arkin Ipac) - model package.
// What it does: One gym machine: ID, name, category, zone, condition, usage.
//               Every structure in the project stores these objects.
// Used by: every class in structures/, service/, db/ and ui/


package model;

// One piece of gym equipment, e.g. EQ-00001 "Medicine Ball 1", Functional, Turf Area,
// Excellent, used 119 times. Every data structure in the project stores Equipment objects
// (or the ID / usage count taken from them), so this is the class everything else depends on.
public class Equipment {

    // The 5 categories used in our dataset. fromLabel() turns the text in the CSV
    // ("Strength Machine") into the enum name (STRENGTH_MACHINE) by upper-casing it
    // and replacing the space with an underscore.
    public enum Category {
        CARDIO, STRENGTH_MACHINE, FREE_WEIGHT, FUNCTIONAL, ACCESSORY;

        public static Category fromLabel(String label) {
            return Category.valueOf(label.trim().toUpperCase().replace(" ", "_"));
        }
    }

    // Listed from best to worst ON PURPOSE. The heap uses ordinal() as the severity score:
    // EXCELLENT = 0, GOOD = 1, FAIR = 2, POOR = 3, CRITICAL = 4.
    // So priority = ordinal * 10,000 + usage, and a worse condition always gets a bigger number.
    //
    // The dataset uses the opposite numbering (1 = Critical, 5 = Excellent), so
    // fromDatasetValue() translates it. Anything outside 1-5 is rejected.
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

    // These never change after registration, so they are final.
    private final String equipmentId;   // key of the hash table and MySQL primary key
    private final String name;
    private final Category category;
    private final String location;      // floor zone, same name as a node in the floor-plan graph

    // Only these two change: condition after servicing, usage as the machine gets used.
    private Condition condition;
    private int usageCount;             // key of the BST, tie-breaker in the heap

    // Validation happens here so a bad record can never enter the system,
    // whether it comes from the form, the CSV, or MySQL.
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

    // Note: changing usage here does NOT update the BST. Other classes should go through
    // EquipmentStore.updateUsage(), which re-files the item under its new key.
    public void setCondition(Condition condition) { this.condition = condition; }
    public void incrementUsage() { this.usageCount++; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    @Override
    public String toString() {
        return equipmentId + " - " + name + " [" + category + ", " + condition + ", used " + usageCount + "x]";
    }
}
