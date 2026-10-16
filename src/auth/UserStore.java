package auth;

import structures.CustomHashTable;
import structures.CustomLinkedList;

// All accounts, kept in our own hash table (username -> User) and saved through a
// UserRepository (MySQL when it's up, users.txt otherwise). Passwords are never stored,
// only a salt and a PBKDF2 hash. Every change is saved straight away; if saving fails
// the change is undone, so memory and storage never disagree.
public class UserStore {

    private static final int MAX_FAILED = 5;
    private static final long LOCK_MILLIS = 5 * 60 * 1000;
    private static final String USERNAME_RULE = "[A-Za-z0-9._-]{3,32}";

    private final UserRepository repository;
    private final CustomHashTable<String, User> users = new CustomHashTable<>();
    private final CustomLinkedList<String> order = new CustomLinkedList<>();   // for listing

    // Used to burn the same time on unknown usernames as on real ones.
    private final String dummySalt = PasswordHasher.newSalt();
    private final String dummyHash = PasswordHasher.hash("not-a-real-password", dummySalt);

    public UserStore(UserRepository repository) {
        this.repository = repository;
        for (User u : repository.loadAll()) {
            users.put(u.getUsername(), u);
            order.add(u.getUsername());
        }
    }

    public boolean isEmpty() { return order.isEmpty(); }
    public int size() { return order.size(); }
    public String storageName() { return repository.describe(); }

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
        String key = username.toLowerCase();
        checkPassword(password, key);
        if (users.contains(key)) throw new IllegalArgumentException("That username is already taken.");
        String salt = PasswordHasher.newSalt();
        User u = new User(key, role, salt, PasswordHasher.hash(password, salt));
        users.put(key, u);
        order.add(key);
        try {
            save();
        } catch (RuntimeException e) {
            users.remove(key);
            order.remove(key);
            throw e;
        }
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
        if (next != null && next.equals(current)) throw new IllegalArgumentException("The new password must be different from the current one.");
        setPassword(u, next);
    }

    // For an admin helping someone who forgot their password. Also unlocks the account.
    public void resetPassword(String username, String next, User actingUser) {
        User u = requireUser(username);
        if (u == actingUser) throw new IllegalArgumentException("Use \"Your account\" to change your own password.");
        setPassword(u, next);
        u.failedAttempts = 0;
        u.lockedUntil = 0;
    }

    private void setPassword(User u, String next) {
        checkPassword(next, u.getUsername());
        String oldSalt = u.getSalt(), oldHash = u.getPasswordHash();
        String salt = PasswordHasher.newSalt();
        u.setPassword(salt, PasswordHasher.hash(next, salt));
        try {
            save();
        } catch (RuntimeException e) {
            u.setPassword(oldSalt, oldHash);
            throw e;
        }
    }

    public void setRole(String username, Role role, User actingUser) {
        User u = requireUser(username);
        if (u == actingUser && role != Role.ADMIN) throw new IllegalArgumentException("You can't remove your own admin role.");
        if (u.getRole() == Role.ADMIN && role != Role.ADMIN && adminCount() == 1) {
            throw new IllegalArgumentException("There must always be at least one admin.");
        }
        Role old = u.getRole();
        u.setRole(role);
        try {
            save();
        } catch (RuntimeException e) {
            u.setRole(old);
            throw e;
        }
    }

    public void delete(String username, User actingUser) {
        User u = requireUser(username);
        if (u == actingUser) throw new IllegalArgumentException("You can't delete your own account.");
        if (u.getRole() == Role.ADMIN && adminCount() == 1) {
            throw new IllegalArgumentException("There must always be at least one admin.");
        }
        CustomLinkedList<String> oldOrder = new CustomLinkedList<>();
        for (String name : order) oldOrder.add(name);
        users.remove(u.getUsername());
        order.remove(u.getUsername());
        try {
            save();
        } catch (RuntimeException e) {
            users.put(u.getUsername(), u);
            while (!order.isEmpty()) order.removeLast();
            for (String name : oldOrder) order.add(name);
            throw e;
        }
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

    private static void checkPassword(String password, String username) {
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must be at least 8 characters.");
        if (password.length() > 128) throw new IllegalArgumentException("Password is too long.");
        if (password.toLowerCase().contains(username.toLowerCase())) {
            throw new IllegalArgumentException("Password can't contain the username.");
        }
    }

    private void save() {
        repository.saveAll(list());
    }
}
