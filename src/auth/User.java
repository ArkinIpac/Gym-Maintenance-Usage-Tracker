package auth;

// One account. Only the salt and hash of the password are kept.
public class User {

    private final String username;
    private Role role;
    private String salt;
    private String passwordHash;

    // Lockout after too many wrong passwords.
    int failedAttempts;
    long lockedUntil;

    public User(String username, Role role, String salt, String passwordHash) {
        this.username = username;
        this.role = role;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    public String getUsername() { return username; }
    public Role getRole() { return role; }
    String getSalt() { return salt; }
    String getPasswordHash() { return passwordHash; }

    void setRole(Role role) { this.role = role; }

    void setPassword(String salt, String passwordHash) {
        this.salt = salt;
        this.passwordHash = passwordHash;
    }
}
