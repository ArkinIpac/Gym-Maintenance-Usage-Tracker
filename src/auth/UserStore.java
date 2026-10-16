package auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import structures.CustomHashTable;
import structures.CustomLinkedList;

// All accounts, kept in our own hash table (username -> User) and saved to users.txt.
// The file holds one line per user: username|ROLE|salt|hash. It never contains a password,
// and it's listed in .gitignore so accounts stay on the machine that made them.
public class UserStore {

    private static final int MAX_FAILED = 5;
    private static final long LOCK_MILLIS = 5 * 60 * 1000;
    private static final String USERNAME_RULE = "[A-Za-z0-9._-]{3,32}";

    private final Path file;
    private final CustomHashTable<String, User> users = new CustomHashTable<>();
    private final CustomLinkedList<String> order = new CustomLinkedList<>();   // for listing

    // Used to burn the same time on unknown usernames as on real ones.
    private final String dummySalt = PasswordHasher.newSalt();
    private final String dummyHash = PasswordHasher.hash("not-a-real-password", dummySalt);

    public UserStore(String fileName) {
        this.file = Paths.get(fileName);
        load();
    }

    public boolean isEmpty() { return order.isEmpty(); }

    public User get(String username) {
        return username == null ? null : users.get(username.toLowerCase());
    }

    public CustomLinkedList<User> list() {
        CustomLinkedList<User> out = new CustomLinkedList<>();
        for (String name : order) out.add(users.get(name));
        return out;
    }

    // Only works while there are no accounts yet: this is how the first admin is made.
    public User createFirstAdmin(String username, String password) {
        if (!isEmpty()) throw new IllegalArgumentException("Setup is already done. Please sign in.");
        return addUser(username, password, Role.ADMIN);
    }

    public User addUser(String username, String password, Role role) {
        if (username == null || !username.matches(USERNAME_RULE)) {
            throw new IllegalArgumentException("Username must be 3-32 letters, numbers, dots, dashes or underscores.");
        }
        checkPassword(password);
        String key = username.toLowerCase();
        if (users.contains(key)) throw new IllegalArgumentException("That username is already taken.");
        String salt = PasswordHasher.newSalt();
        User u = new User(key, role, salt, PasswordHasher.hash(password, salt));
        users.put(key, u);
        order.add(key);
        save();
        return u;
    }

    // Returns the user, or null for a wrong username/password. The message is the same in
    // both cases, so nobody can find out which usernames exist.
    public User login(String username, String password) {
        User u = get(username);
        if (u == null) {
            PasswordHasher.matches(password == null ? "" : password, dummySalt, dummyHash);
            return null;
        }
        if (isLocked(u)) return null;
        if (password != null && PasswordHasher.matches(password, u.getSalt(), u.getPasswordHash())) {
            u.failedAttempts = 0;
            return u;
        }
        if (++u.failedAttempts >= MAX_FAILED) {
            u.lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
            u.failedAttempts = 0;
        }
        return null;
    }

    public boolean isLocked(User u) {
        return u != null && System.currentTimeMillis() < u.lockedUntil;
    }

    public void changePassword(User u, String current, String next) {
        if (current == null || !PasswordHasher.matches(current, u.getSalt(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Your current password is wrong.");
        }
        checkPassword(next);
        String salt = PasswordHasher.newSalt();
        u.setPassword(salt, PasswordHasher.hash(next, salt));
        save();
    }

    public void setRole(String username, Role role, User actingUser) {
        User u = requireUser(username);
        if (u == actingUser && role != Role.ADMIN) throw new IllegalArgumentException("You can't remove your own admin role.");
        if (u.getRole() == Role.ADMIN && role != Role.ADMIN && adminCount() == 1) {
            throw new IllegalArgumentException("There must always be at least one admin.");
        }
        u.setRole(role);
        save();
    }

    public void delete(String username, User actingUser) {
        User u = requireUser(username);
        if (u == actingUser) throw new IllegalArgumentException("You can't delete your own account.");
        if (u.getRole() == Role.ADMIN && adminCount() == 1) {
            throw new IllegalArgumentException("There must always be at least one admin.");
        }
        users.remove(u.getUsername());
        order.remove(u.getUsername());
        save();
    }

    private User requireUser(String username) {
        User u = get(username);
        if (u == null) throw new IllegalArgumentException("No user named \"" + username + "\".");
        return u;
    }

    private int adminCount() {
        int n = 0;
        for (String name : order) if (users.get(name).getRole() == Role.ADMIN) n++;
        return n;
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must be at least 8 characters.");
        if (password.length() > 128) throw new IllegalArgumentException("Password is too long.");
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] p = line.trim().split("\\|");
                if (p.length != 4) continue;
                User u = new User(p[0], Role.valueOf(p[1]), p[2], p[3]);
                users.put(u.getUsername(), u);
                order.add(u.getUsername());
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not read " + file + ": " + e.getMessage(), e);
        }
    }

    // Written to a temp file first, then moved over the real one, so a crash mid-write
    // can't leave a half-written users file.
    private void save() {
        StringBuilder sb = new StringBuilder();
        for (String name : order) {
            User u = users.get(name);
            sb.append(u.getUsername()).append('|').append(u.getRole().name()).append('|')
              .append(u.getSalt()).append('|').append(u.getPasswordHash()).append('\n');
        }
        try {
            Path tmp = Paths.get(file.toString() + ".tmp");
            Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save users: " + e.getMessage(), e);
        }
    }
}
