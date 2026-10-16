package db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Equipment;
import model.MaintenanceRecord;

// Saves every finished service to the maintenance_log table, so service histories survive
// a restart. Read back once at start-up into the manager's hash table of linked lists.
public class MaintenanceDAO {

    public void insert(MaintenanceRecord r) throws SQLException {
        String sql = "INSERT INTO maintenance_log "
                   + "(equipment_id, note, condition_before, condition_after, serviced_by, serviced_on) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, r.getEquipmentId());
            stmt.setString(2, r.getNote());
            stmt.setString(3, r.getConditionBefore().name());
            stmt.setString(4, r.getConditionAfter().name());
            stmt.setString(5, r.getServicedBy());
            stmt.setDate(6, Date.valueOf(r.getDateServiced()));
            stmt.executeUpdate();
        }
    }

    // Oldest first, so adding them in order rebuilds each history the way it happened.
    public List<MaintenanceRecord> findAll() throws SQLException {
        String sql = "SELECT * FROM maintenance_log ORDER BY log_id";
        List<MaintenanceRecord> out = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                out.add(new MaintenanceRecord(
                        rs.getString("equipment_id"),
                        rs.getString("note"),
                        Equipment.Condition.valueOf(rs.getString("condition_before")),
                        Equipment.Condition.valueOf(rs.getString("condition_after")),
                        rs.getString("serviced_by"),
                        rs.getDate("serviced_on").toLocalDate()));
            }
        }
        return out;
    }
}
