package web;

import auth.Role;
import auth.SessionManager;
import auth.User;
import auth.UserStore;
import java.util.Map;

// Sign in, sign out, first-run setup, password changes and user management.
//   /api/auth/state      public: is setup needed, and who is signed in
//   /api/auth/setup      public, only while there are no accounts: creates the first admin
//   /api/auth/login      public
//   /api/auth/logout     signed in
//   /api/auth/password   signed in: change your own password
//   /api/users           admin: list, /add, /role, /delete
public class AuthRoutes {

    // A response plus an optional cookie to set (a new session, or clearing one).
    public static class Result {
        final ApiRoutes.Response response;
        final String setCookieToken;      // non-null: start this session
        final boolean clearCookie;
        Result(ApiRoutes.Response response, String setCookieToken, boolean clearCookie) {
            this.response = response;
            this.setCookieToken = setCookieToken;
            this.clearCookie = clearCookie;
        }
    }

    private final UserStore users;
    private final SessionManager sessions;
    private final ApiRoutes api;           // for the activity feed

    public AuthRoutes(UserStore users, SessionManager sessions, ApiRoutes api) {
        this.users = users;
        this.sessions = sessions;
        this.api = api;
    }

    // current is the signed-in user, or null.
    public Result handle(String method, String path, Map<String, String> p, User current, String token) {
        boolean post = "POST".equalsIgnoreCase(method);
        try {
            if (path.equals("/api/auth/state")) return plain(state(current));
            boolean listing = path.equals("/api/users");
            if (!post && !listing) return plain(error(405, "Use POST for " + path));

            switch (path) {
                case "/api/auth/setup":    return setup(p);
                case "/api/auth/login":    return login(p);
                case "/api/auth/logout":   return logout(current, token);
                default: break;
            }

            if (current == null) return plain(error(401, "Please sign in."));
            if (path.equals("/api/auth/password")) return plain(changePassword(current, p, token));

            if (!current.getRole().atLeast(Role.ADMIN)) return plain(error(403, "Only admins can manage users."));
            switch (path) {
                case "/api/users":         return plain(listUsers());
                case "/api/users/add":     return plain(addUser(p, current));
                case "/api/users/role":    return plain(setRole(p, current));
                case "/api/users/delete":  return plain(deleteUser(p, current));
                default:                   return plain(error(404, "Unknown address " + path));
            }
        } catch (IllegalArgumentException ex) {
            return plain(error(400, ex.getMessage()));
        }
    }

    private ApiRoutes.Response state(User current) {
        Json j = new Json().obj().put("ok", true).put("needsSetup", users.isEmpty());
        j.key("user");
        if (current == null) j.val((String) null);
        else userJson(j, current);
        return ok(j.end());
    }

    private Result setup(Map<String, String> p) {
        User u = users.createFirstAdmin(p.get("username"), p.get("password"));
        api.logActivity("Admin account " + u.getUsername() + " created", u.getUsername());
        return new Result(message("Welcome, " + u.getUsername() + ". Your admin account is ready."), sessions.create(u), false);
    }

    private Result login(Map<String, String> p) {
        String name = p.getOrDefault("username", "").trim();
        User u = users.login(name, p.get("password"));
        if (u == null) {
            User existing = users.get(name);
            if (users.isLocked(existing)) {
                return plain(error(429, "Too many wrong passwords. Try again in 5 minutes."));
            }
            return plain(error(401, "Wrong username or password."));
        }
        api.logActivity("Signed in", u.getUsername());
        return new Result(message("Signed in"), sessions.create(u), false);
    }

    private Result logout(User current, String token) {
        sessions.end(token);
        if (current != null) api.logActivity("Signed out", current.getUsername());
        return new Result(message("Signed out"), null, true);
    }

    // Changing your password signs you out everywhere else.
    private ApiRoutes.Response changePassword(User current, Map<String, String> p, String token) {
        users.changePassword(current, p.get("current"), p.get("next"));
        sessions.endAllFor(current.getUsername());
        api.logActivity("Changed password", current.getUsername());
        return message("Password changed. Please sign in again.");
    }

    private ApiRoutes.Response listUsers() {
        Json j = new Json().obj().put("ok", true).key("users").arr();
        for (User u : users.list()) userJson(j, u);
        return ok(j.end().end());
    }

    private ApiRoutes.Response addUser(Map<String, String> p, User current) {
        User u = users.addUser(p.get("username"), p.get("password"), parseRole(p.get("role")));
        api.logActivity("Added user " + u.getUsername() + " (" + u.getRole().name().toLowerCase() + ")", current.getUsername());
        return message("Added " + u.getUsername() + " as " + u.getRole().name().toLowerCase() + ".");
    }

    private ApiRoutes.Response setRole(Map<String, String> p, User current) {
        Role role = parseRole(p.get("role"));
        users.setRole(p.get("username"), role, current);
        sessions.endAllFor(p.get("username").toLowerCase());    // new role applies on their next sign-in
        api.logActivity("Changed " + p.get("username") + " to " + role.name().toLowerCase(), current.getUsername());
        return message(p.get("username") + " is now " + role.name().toLowerCase() + ".");
    }

    private ApiRoutes.Response deleteUser(Map<String, String> p, User current) {
        String name = p.getOrDefault("username", "");
        users.delete(name, current);
        sessions.endAllFor(name.toLowerCase());
        api.logActivity("Deleted user " + name, current.getUsername());
        return message("Deleted " + name + ".");
    }

    private static Role parseRole(String s) {
        try {
            return Role.valueOf(s == null ? "" : s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role must be admin, staff or viewer.");
        }
    }

    private static void userJson(Json j, User u) {
        j.obj().put("username", u.getUsername()).put("role", u.getRole().name()).end();
    }

    private static Result plain(ApiRoutes.Response r) { return new Result(r, null, false); }
    private static ApiRoutes.Response ok(Json j) { return new ApiRoutes.Response(200, j.toString()); }
    private static ApiRoutes.Response message(String m) {
        return ok(new Json().obj().put("ok", true).put("message", m).end());
    }
    private static ApiRoutes.Response error(int status, String m) {
        return new ApiRoutes.Response(status, new Json().obj().put("ok", false).put("message", m).end().toString());
    }
}
