
// MaintenanceRecord.java
// Part of: Jay Mark Lampa  (Queue and Linked List)
// Presented in: Slides 5-8 live demo (John Benedict Jesalva) - history list.
// What it does: One finished service job. Stored in the linked lists:
//               each machine's history and the recently serviced list.
// Used by: MaintenanceManager.serviceNext(), MaintenancePanel


package model;

import java.time.LocalDate;

// One finished service job: which equipment, what was done, when, and its condition
// before and after. MaintenanceManager creates one every time serviceNext() runs and
// stores it in two linked lists: that equipment's own history, and the "recently serviced"
// list shown on the Maintenance page.
//
// All fields are final. A record describes something that already happened,
// so it should never be edited afterwards.
public class MaintenanceRecord {

    private final String equipmentId;                 // links the record back to the equipment
    private final String note;                        // e.g. "Routine service"
    private final LocalDate dateServiced;             // set automatically, not typed in
    private final Equipment.Condition conditionBefore;
    private final Equipment.Condition conditionAfter;

    public MaintenanceRecord(String equipmentId, String note,
                              Equipment.Condition conditionBefore, Equipment.Condition conditionAfter) {
        this.equipmentId = equipmentId;
        this.note = note;
        this.dateServiced = LocalDate.now();          // the date the record was created = service date
        this.conditionBefore = conditionBefore;
        this.conditionAfter = conditionAfter;
    }

    // Getters only, no setters (see note above about final fields).
    public String getEquipmentId() { return equipmentId; }
    public String getNote() { return note; }
    public LocalDate getDateServiced() { return dateServiced; }
    public Equipment.Condition getConditionBefore() { return conditionBefore; }
    public Equipment.Condition getConditionAfter() { return conditionAfter; }

    // Example: 2026-10-04 | EQ-00202 | CRITICAL -> GOOD | Routine service
    @Override
    public String toString() {
        return dateServiced + " | " + equipmentId + " | " + conditionBefore + " -> " + conditionAfter + " | " + note;
    }
}
