package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Equipment;

// Run once to load data/equipment_dataset.csv (5,000 rows) into MySQL.
// readCsv() is also what the app uses when MySQL is down.
public class DatasetImporter {

    private static final String CSV_PATH = "data/equipment_dataset.csv";

    public static void main(String[] args) {
        try {
            List<Equipment> equipment = readCsv(CSV_PATH);
            System.out.println("Read " + equipment.size() + " rows from " + CSV_PATH);
            int inserted = insertAll(equipment);
            System.out.println("Imported " + inserted + " rows into MySQL.");
        } catch (IOException e) {
            System.out.println("Could not read the CSV file: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // Columns: ID, Name, Category, Location, Condition (1-5), Usage.
    public static List<Equipment> readCsv(String path) throws IOException {
        List<Equipment> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            String line = reader.readLine(); // header row
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                // Commas for a real CSV, tabs for data pasted from Excel.
                String[] cols = line.contains("\t") ? line.split("\t") : line.split(",");
                try {
                    result.add(new Equipment(
                            cols[0].trim(),
                            cols[1].trim(),
                            Equipment.Category.fromLabel(cols[2]),
                            cols[3].trim(),
                            Equipment.Condition.fromDatasetValue(Integer.parseInt(cols[4].trim())),
                            Integer.parseInt(cols[5].trim().replace(",", ""))   // Excel writes 1184 as "1,184"
                    ));
                } catch (RuntimeException ex) {
                    // Skip a bad row instead of stopping the whole import.
                    System.out.println("Skipped line " + lineNumber + ": " + ex.getMessage());
                }
            }
        }
        return result;
    }

    // Batches of 500 instead of 5,000 separate inserts.
    // INSERT IGNORE skips IDs that already exist, so running it twice is harmless.
    private static int insertAll(List<Equipment> equipment) throws SQLException {
        String sql = "INSERT IGNORE INTO equipment "
                   + "(equipment_id, name, category, location, condition_status, usage_count) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        int total = 0;
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false); // one transaction: faster, and all-or-nothing
            int count = 0;
            for (Equipment e : equipment) {
                stmt.setString(1, e.getEquipmentId());
                stmt.setString(2, e.getName());
                stmt.setString(3, e.getCategory().name());
                stmt.setString(4, e.getLocation());
                stmt.setString(5, e.getCondition().name());
                stmt.setInt(6, e.getUsageCount());
                stmt.addBatch();
                if (++count % 500 == 0) total += sum(stmt.executeBatch());
            }
            total += sum(stmt.executeBatch()); // whatever is left after the last full batch
            conn.commit();
        }
        return total;
    }

    // executeBatch() gives 1 per inserted row, 0 for a skipped one, or a negative code; count the positives.
    private static int sum(int[] results) {
        int s = 0;
        for (int r : results) if (r > 0) s += r;
        return s;
    }
}
