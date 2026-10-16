package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Equipment;

// Create, read, update and delete rows in the equipment table. The only class with SQL.
// MySQL just saves the data between runs; all searching and sorting happens in memory
// in our own structures. PreparedStatement placeholders keep user input out of the SQL.
public class EquipmentDAO {

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

    public void delete(String equipmentId) throws SQLException {
        String sql = "DELETE FROM equipment WHERE equipment_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, equipmentId);
            stmt.executeUpdate();
        }
    }

    // Enums are stored by name (e.g. "STRENGTH_MACHINE"), so valueOf() reads them back.
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
