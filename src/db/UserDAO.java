package db;

import auth.Role;
import auth.User;
import auth.UserRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import structures.CustomLinkedList;

// Accounts in the app_users table. Only the salt and the PBKDF2 hash are stored.
public class UserDAO implements UserRepository {

    @Override
    public CustomLinkedList<User> loadAll() {
        CustomLinkedList<User> out = new CustomLinkedList<>();
        try (Connection conn = DatabaseConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM app_users ORDER BY created_order")) {
            while (rs.next()) {
                out.add(new User(rs.getString("username"), Role.valueOf(rs.getString("role")),
                        rs.getString("salt"), rs.getString("password_hash")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read accounts from MySQL.", e);
        }
        return out;
    }

    // Small table, so the simplest correct save is: in one transaction, empty it and write
    // every account again. If anything fails, rollback leaves the old accounts untouched.
    @Override
    public void saveAll(CustomLinkedList<User> users) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement del = conn.createStatement();
                 PreparedStatement ins = conn.prepareStatement(
                         "INSERT INTO app_users (username, role, salt, password_hash, created_order) VALUES (?, ?, ?, ?, ?)")) {
                del.executeUpdate("DELETE FROM app_users");
                int order = 0;
                for (User u : users) {
                    ins.setString(1, u.getUsername());
                    ins.setString(2, u.getRole().name());
                    ins.setString(3, u.getSalt());
                    ins.setString(4, u.getPasswordHash());
                    ins.setInt(5, order++);
                    ins.addBatch();
                }
                ins.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save accounts to MySQL.", e);
        }
    }

    @Override
    public String describe() { return "MySQL"; }
}
