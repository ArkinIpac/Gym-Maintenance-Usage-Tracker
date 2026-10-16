package auth;

// What each kind of account may do. Higher roles can do everything lower ones can.
//   MEMBER - a gym member: look at every page, change nothing
//   STAFF  - also flag, promote and service machines, register and edit equipment, run benchmarks
//   ADMIN  - also delete equipment, clear lists and manage users
public enum Role {
    MEMBER, STAFF, ADMIN;

    public boolean atLeast(Role needed) {
        return ordinal() >= needed.ordinal();
    }

    // Reads a saved role. Accounts saved before "viewer" was renamed to "member" still load.
    public static Role parse(String s) {
        String name = s == null ? "" : s.trim().toUpperCase();
        return Role.valueOf(name.equals("VIEWER") ? "MEMBER" : name);
    }
}
