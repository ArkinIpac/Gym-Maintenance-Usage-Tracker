package model;

import java.time.LocalDate;

// One finished service job. Stored in the machine's own history list, the "recently serviced"
// list and the maintenance_log table. All fields are final: it records something that already happened.
public class MaintenanceRecord {

    private final String equipmentId;
    private final String note;                        // e.g. "Routine service"
    private final LocalDate dateServiced;
    private final Equipment.Condition conditionBefore;
    private final Equipment.Condition conditionAfter;
    private final String servicedBy;                  // username of whoever pressed Service

    public MaintenanceRecord(String equipmentId, String note,
                              Equipment.Condition conditionBefore, Equipment.Condition conditionAfter,
                              String servicedBy) {
        this(equipmentId, note, conditionBefore, conditionAfter, servicedBy, LocalDate.now());
    }

    // Used when reading old records back from MySQL.
    public MaintenanceRecord(String equipmentId, String note,
                              Equipment.Condition conditionBefore, Equipment.Condition conditionAfter,
                              String servicedBy, LocalDate dateServiced) {
        this.equipmentId = equipmentId;
        this.note = note;
        this.dateServiced = dateServiced;
        this.conditionBefore = conditionBefore;
        this.conditionAfter = conditionAfter;
        this.servicedBy = servicedBy;
    }

    public String getEquipmentId() { return equipmentId; }
    public String getNote() { return note; }
    public LocalDate getDateServiced() { return dateServiced; }
    public Equipment.Condition getConditionBefore() { return conditionBefore; }
    public Equipment.Condition getConditionAfter() { return conditionAfter; }
    public String getServicedBy() { return servicedBy; }

    // e.g. 2026-10-04 | EQ-00202 | CRITICAL -> GOOD | Routine service | arkin
    @Override
    public String toString() {
        return dateServiced + " | " + equipmentId + " | " + conditionBefore + " -> " + conditionAfter
                + " | " + note + " | " + servicedBy;
    }
}
