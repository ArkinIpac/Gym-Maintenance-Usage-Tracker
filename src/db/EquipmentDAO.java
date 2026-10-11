
// EquipmentDAO.java
// Part of: Arkin Ipac  (Integration: db package)
// Presented in: Slide 4 (Arkin Ipac) - architecture, db package.
// What it does: Insert / read / update / delete equipment rows in MySQL.
//               Only used to save and load; all searching is done in memory.
// Used by: MainFrame, EquipmentPanel, MaintenancePanel, Benchmark, Main


package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Equipment;

// CRUD for the `equipment` table: Create, Read, Update, Delete.
// DAO = Data Access Object. This is the only class that contains SQL; the rest of the
// program just calls insert(), findAll(), update(), delete().
//
// MySQL is only for SAVING the data between runs. While the app is running, all searching,
// sorting and prioritizing is done by our own data structures in memory, not by SQL.
//
// Every query uses PreparedStatement with ? placeholders instead of joining strings.
// The values are sent separately from the SQL, so input like "EQ-1' OR '1'='1" is treated
// as plain text, which protects against SQL injection.
// try-with-resources closes the Connection/Statement automatically, even on an error.
public class EquipmentDAO {

    // ---------- Create ----------

    public void insert(Equipment e) throws SQLException {
        String sql = "INSERT INTO equipment (equipment_id, name, category, location, condition_status, usage_count) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getEquipmentId());
            stmt.setString(2, e.getName());
            stmt.setString(3, e.getCategory().name());
            stmt.setString(4, e.getLocation());
            stmt.setString(5, e.getCondition().name());
            stmt.setInt(6, e.getUsageCount());
            stmt.executeUpdate();
        }
    }

    // ---------- Read ----------

    public Equipment findById(String equipmentId) throws SQLException {
        String sql = "SELECT * FROM equipment WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, equipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                return null;
            }
        }
    }

    public List<Equipment> findAll() throws SQLException {
        String sql = "SELECT * FROM equipment";
        List<Equipment> results = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) results.add(mapRow(rs));
        }
        return results;
    }

    // ---------- Update ----------

    public void update(Equipment e) throws SQLException {
        String sql = "UPDATE equipment SET name = ?, category = ?, location = ?, "
                   + "condition_status = ?, usage_count = ? WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getName());
            stmt.setString(2, e.getCategory().name());
            stmt.setString(3, e.getLocation());
            stmt.setString(4, e.getCondition().name());
            stmt.setInt(5, e.getUsageCount());
            stmt.setString(6, e.getEquipmentId());
            stmt.executeUpdate();
        }
    }

    // ---------- Delete ----------

    public void delete(String equipmentId) throws SQLException {
        String sql = "DELETE FROM equipment WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, equipmentId);
            stmt.executeUpdate();
        }
    }

    // ---------- Row mapping ----------

    // Turns one database row into an Equipment object. Shared by findById() and findAll()
    // so the column mapping is written only once.
    // The enums are saved by name (e.g. "STRENGTH_MACHINE"), so valueOf() reads them back.
    private Equipment mapRow(ResultSet rs) throws SQLException {
        return new Equipment(
                rs.getString("equipment_id"),
                rs.getString("name"),
                Equipment.Category.valueOf(rs.getString("category")),
                rs.getString("location"),
                Equipment.Condition.valueOf(rs.getString("condition_status")),
                rs.getInt("usage_count")
        );
    }
}
