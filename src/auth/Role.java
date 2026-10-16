package auth;

// What each kind of account may do. Higher roles can do everything lower ones can.
//   VIEWER - look at every page, change nothing
//   STAFF  - also flag, promote and service machines, register and edit equipment
//   ADMIN  - also delete equipment, clear lists, run benchmarks and manage users
public enum Role {
    VIEWER, STAFF, ADMIN;

    public boolean atLeast(Role needed) {
        return ordinal() >= needed.ordinal();
    }
}
