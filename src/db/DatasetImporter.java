
// DatasetImporter.java
// Part of: Arkin Ipac  (Integration: db package)
// Presented in: Slide 3 (Kahlil Mendoza) - the 5,000-record dataset;
//               Slide 4 (Arkin Ipac) - architecture, db package.
// What it does: One-time tool that loads the 5,000-row CSV into MySQL.
//               readCsv() is also the offline fallback when MySQL is down.
// Used by: run once before the app; MainFrame / Main / Benchmark (fallback)


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

// One-time setup tool: reads data/equipment_dataset.csv (5,000 rows) and inserts it into
// the MySQL `equipment` table. Run it once, separately from the app. After that the app
// loads from MySQL on startup. (If MySQL is down, the app falls back to readCsv().)
public class DatasetImporter {

    // Relative to the project folder, which is where VS Code runs the program from.
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

    // Turns each line of the file into an Equipment object.
    // Column order: ID, Name, Category, Location, Condition (1-5), Usage.
    public static List<Equipment> readCsv(String path) throws IOException {
        List<Equipment> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            String line = reader.readLine(); // skip the header row (ID, Name, Category, ...)
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                // A real CSV uses commas; data pasted from Excel uses tabs. Accept both.
                String[] cols = line.contains("\t") ? line.split("\t") : line.split(",");
                try {
                    result.add(new Equipment(
                            cols[0].trim(),                                          // EQ-00001
                            cols[1].trim(),                                          // Medicine Ball 1
                            Equipment.Category.fromLabel(cols[2]),                   // "Functional" -> FUNCTIONAL
                            cols[3].trim(),                                          // Turf Area
                            Equipment.Condition.fromDatasetValue(Integer.parseInt(cols[4].trim())), // 5 -> EXCELLENT
                            // replace(",", "") handles Excel's thousands separator, e.g. "1,184" -> 1184
                            Integer.parseInt(cols[5].trim().replace(",", ""))        // 119
                    ));
                } catch (RuntimeException ex) {
                    // A bad row (wrong category, missing column, not a number) is skipped
                    // and reported, instead of stopping the whole import.
                    System.out.println("Skipped line " + lineNumber + ": " + ex.getMessage());
                }
            }
        }
        return result;
    }

    // Sends the rows in batches of 500 instead of 5,000 separate INSERTs, which saves a lot
    // of back-and-forth with the database.
    // INSERT IGNORE: if the importer is run again, rows whose ID already exists are skipped
    // instead of causing a duplicate-key error.
    private static int insertAll(List<Equipment> equipment) throws SQLException {
        String sql = "INSERT IGNORE INTO equipment "
                   + "(equipment_id, name, category, location, condition_status, usage_count) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        int total = 0;
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false); // one transaction: commit once at the end (faster, all-or-nothing)
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
            total += sum(stmt.executeBatch()); // the remaining rows after the last full batch of 500
            conn.commit();
        }
        return total;
    }

    // executeBatch() returns one number per row: rows inserted, 0 if INSERT IGNORE skipped it,
    // or a negative code when the driver can't report a count. We add up only the positive ones.
    private static int sum(int[] results) {
        int s = 0;
        for (int r : results) if (r > 0) s += r;
        return s;
    }
}
