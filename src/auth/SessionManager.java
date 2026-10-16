package auth;

import java.security.SecureRandom;
import java.util.Base64;
import structures.CustomHashTable;
import structures.CustomLinkedList;

// Signed-in sessions: a long random token (sent as an HttpOnly cookie) -> who it belongs to.
// The token is the only thing the browser holds; the server decides what it's worth.
// Sessions end after 30 minutes without activity or 8 hours in total.
public class SessionManager {

    public static final String COOKIE = "LG_SESSION";
    private static final long IDLE_MILLIS = 30 * 60 * 1000;
    private static final long MAX_MILLIS = 8 * 60 * 60 * 1000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static class Session {
        final String username;
        final long created;
        long lastSeen;
        Session(String username) {
            this.username = username;
            this.created = this.lastSeen = System.currentTimeMillis();
        }
    }

    private final CustomHashTable<String, Session> sessions = new CustomHashTable<>();
    private final CustomLinkedList<String> tokens = new CustomLinkedList<>();   // for clean-up

    // 32 random bytes: impossible to guess.
    public String create(User user) {
        removeExpired();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(user.getUsername()));
        tokens.add(token);
        return token;
    }

    // The username for a valid token, or null. Each use pushes the idle timeout back.
    public String check(String token) {
        if (token == null) return null;
        Session s = sessions.get(token);
        if (s == null) return null;
        long now = System.currentTimeMillis();
        if (now - s.lastSeen > IDLE_MILLIS || now - s.created > MAX_MILLIS) {
            end(token);
            return null;
        }
        s.lastSeen = now;
        return s.username;
    }

    public void end(String token) {
        if (token != null && sessions.remove(token)) tokens.remove(token);
    }

    // Signs a user out everywhere, e.g. after they're deleted or change their password.
    public void endAllFor(String username) {
        CustomLinkedList<String> gone = new CustomLinkedList<>();
        for (String t : tokens) {
            Session s = sessions.get(t);
            if (s != null && s.username.equals(username)) gone.add(t);
        }
        for (String t : gone) end(t);
    }

    private void removeExpired() {
        CustomLinkedList<String> all = new CustomLinkedList<>();
        for (String t : tokens) all.add(t);
        for (String t : all) check(t);
    }
}
