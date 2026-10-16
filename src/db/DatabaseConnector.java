package db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

// The only class that knows how to reach MySQL.
// Settings come from environment variables first (DB_URL, DB_USERNAME, DB_PASSWORD, which is
// how Azure App Service passes secrets), then from db.properties (ignored by git).
// The password is never in the code. If neither is set, the app falls back to the CSV.
public class DatabaseConnector {

    private static final String SETTINGS_FILE = "db.properties";

    // rewriteBatchedStatements makes the importer's 5,000 inserts much faster.
    // connectTimeout keeps start-up quick when MySQL isn't running.
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/gym_equipment_tracker?rewriteBatchedStatements=true&connectTimeout=3000";

    private static Properties settings;

    // A fresh connection per call (an idle one can time out). Callers close it with try-with-resources.
    public static Connection getConnection() throws SQLException {
        Properties s = settings();
        return DriverManager.getConnection(
                s.getProperty("db.url", DEFAULT_URL),
                s.getProperty("db.username", "root"),
                s.getProperty("db.password", ""));
    }

    // Creates the tables the app needs if they're missing, so an existing database from
    // before accounts and service history were added keeps working without re-running schema.sql.
    public static void ensureTables() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS equipment ("
                    + " equipment_id VARCHAR(10) PRIMARY KEY,"
                    + " name VARCHAR(100) NOT NULL,"
                    + " category VARCHAR(30) NOT NULL,"
                    + " location VARCHAR(50) NOT NULL,"
                    + " condition_status VARCHAR(20) NOT NULL,"
                    + " usage_count INT NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS maintenance_log ("
                    + " log_id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " equipment_id VARCHAR(10) NOT NULL,"
                    + " note VARCHAR(200) NOT NULL,"
                    + " condition_before VARCHAR(20) NOT NULL,"
                    + " condition_after VARCHAR(20) NOT NULL,"
                    + " serviced_by VARCHAR(32) NOT NULL,"
                    + " serviced_on DATE NOT NULL,"
                    + " INDEX idx_log_equipment (equipment_id))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS app_users ("
                    + " username VARCHAR(32) PRIMARY KEY,"
                    + " role VARCHAR(10) NOT NULL,"
                    + " salt VARCHAR(64) NOT NULL,"
                    + " password_hash VARCHAR(128) NOT NULL,"
                    + " created_order INT NOT NULL DEFAULT 0)");
        }
    }

    // Environment variables win over the file, so the same build runs on a PC and on Azure.
    private static synchronized Properties settings() throws SQLException {
        if (settings != null) return settings;
        Properties s = new Properties();
        try (InputStream in = new FileInputStream(SETTINGS_FILE)) {
            s.load(in);
        } catch (IOException e) {
            // no file: fine if the environment variables are set
        }
        copyEnv(s, "DB_URL", "db.url");
        copyEnv(s, "DB_USERNAME", "db.username");
        copyEnv(s, "DB_PASSWORD", "db.password");
        if (!s.containsKey("db.password")) {
            throw new SQLException("No database settings: copy db.properties.example to db.properties "
                    + "(or set DB_URL, DB_USERNAME and DB_PASSWORD).");
        }
        settings = s;
        return s;
    }

    private static void copyEnv(Properties s, String env, String key) {
        String v = System.getenv(env);
        if (v != null && !v.isEmpty()) s.setProperty(key, v);
    }
}
