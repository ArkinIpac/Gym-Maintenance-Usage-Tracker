package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// The only class that knows how to reach MySQL.
// The username and password come from db.properties (ignored by git), never from the code.
// Copy db.properties.example to db.properties and put in your own password.
// If the file is missing, the app falls back to the CSV.
public class DatabaseConnector {

    private static final String SETTINGS_FILE = "db.properties";

    // rewriteBatchedStatements makes the importer's 5,000 inserts much faster.
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/gym_equipment_tracker?rewriteBatchedStatements=true";

    // A fresh connection per call (an idle one can time out). Callers close it with try-with-resources.
    public static Connection getConnection() throws SQLException {
        Properties settings = loadSettings();
        return DriverManager.getConnection(
                settings.getProperty("db.url", DEFAULT_URL),
                settings.getProperty("db.username", "root"),
                settings.getProperty("db.password", ""));
    }

    // Read from the folder the app runs in (the project folder).
    private static Properties loadSettings() throws SQLException {
        Properties settings = new Properties();
        try (InputStream in = new FileInputStream(SETTINGS_FILE)) {
            settings.load(in);
        } catch (IOException e) {
            throw new SQLException("Missing " + SETTINGS_FILE
                    + ": copy db.properties.example to db.properties and put in your MySQL password.");
        }
        return settings;
    }
}
