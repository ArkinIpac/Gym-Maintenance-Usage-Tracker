package model;

import java.time.LocalDate;

// One finished service job. Stored in the machine's own history list and in the
// "recently serviced" list. All fields are final: it records something that already happened.
public class MaintenanceRecord {

    private final String equipmentId;
    private final String note;                        // e.g. "Routine service"
    private final LocalDate dateServiced;
    private final Equipment.Condition conditionBefore;
    private final Equipment.Condition conditionAfter;

    public MaintenanceRecord(String equipmentId, String note,
                              Equipment.Condition conditionBefore, Equipment.Condition conditionAfter) {
        this.equipmentId = equipmentId;
        this.note = note;
        this.dateServiced = LocalDate.now();
        this.conditionBefore = conditionBefore;
        this.conditionAfter = conditionAfter;
    }

    public String getEquipmentId() { return equipmentId; }
    public String getNote() { return note; }
    public LocalDate getDateServiced() { return dateServiced; }
    public Equipment.Condition getConditionBefore() { return conditionBefore; }
    public Equipment.Condition getConditionAfter() { return conditionAfter; }

    // e.g. 2026-10-04 | EQ-00202 | CRITICAL -> GOOD | Routine service
    @Override
    public String toString() {
        return dateServiced + " | " + equipmentId + " | " + conditionBefore + " -> " + conditionAfter + " | " + note;
    }
}
