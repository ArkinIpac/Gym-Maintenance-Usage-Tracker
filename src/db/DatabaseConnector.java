
// DatabaseConnector.java
// Part of: Arkin Ipac  (Integration: db package)
// Presented in: Slide 4 (Arkin Ipac) - architecture, db package.
// What it does: Opens the connection to our MySQL database.
// Used by: EquipmentDAO, DatasetImporter


package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// The only class that knows HOW to connect to MySQL (URL, username, password).
// EquipmentDAO and DatasetImporter just call getConnection(), so if the database
// settings change, only this file needs editing.
//
// The username and password are NOT written in the code. They are read from
// db.properties in the project folder, so the password is never uploaded to GitHub
// (db.properties is listed in .gitignore). Each group member copies
// db.properties.example to db.properties and puts in their own MySQL password.
// If the file is missing, the app still opens: MainFrame falls back to the CSV.
public class DatabaseConnector {

    private static final String SETTINGS_FILE = "db.properties";

    // Local MySQL server on the default port 3306, database gym_equipment_tracker.
    // rewriteBatchedStatements=true lets the driver send the importer's batched INSERTs
    // as fewer, bigger statements, so loading 5,000 rows is faster.
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/gym_equipment_tracker?rewriteBatchedStatements=true";

    // Opens a new connection on every call instead of keeping one open the whole time.
    // Simpler, and a connection left idle too long can time out on the MySQL side.
    // The caller closes it with try-with-resources.
    public static Connection getConnection() throws SQLException {
        Properties settings = loadSettings();
        return DriverManager.getConnection(
                settings.getProperty("db.url", DEFAULT_URL),
                settings.getProperty("db.username", "root"),
                settings.getProperty("db.password", ""));
    }

    // Reads db.properties from the folder the app is run from (the project folder in VS Code).
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
